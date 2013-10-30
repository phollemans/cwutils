////////////////////////////////////////////////////////////////////////
/*
     FILE: OpendapGSHHSReader.java
  PURPOSE: To provide GSHHS coastline data from an OPeNDAP connection.
   AUTHOR: Peter Hollemans
     DATE: 2006/06/09
  CHANGES: 2008/02/18, PFH, modified to use opendap.dap classes

  CoastWatch Software Library and Utilities
  Copyright 2006-2008, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import opendap.dap.*;

/**
 * The <code>OpendapGSHHSReader</code> extends
 * <code>BinnedGSHHSReader</code> to read data from an OPeNDAP-enabled
 * binned data file.  Some optimizations are made for network
 * connections.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class OpendapGSHHSReader
  extends BinnedGSHHSReader {

  // Constants
  // ---------

  /** The array of variable names that will be accessed. */
  private static final String VAR_NAMES[] = new String[] {
    "Embedded_npts_levels_exit_entry_for_a_segment",
    "Ten_times_the_km_squared_area_of_the_parent_polygon_of_a_segmen",
    "Relative_longitude_from_SW_corner_of_bin",
    "Relative_latitude_from_SW_corner_of_bin"
  };

  /** The segment info id. */
  private static final int SEGMENT_INFO_ID = 0;

  /** The segment area id. */
  private static final int SEGMENT_AREA_ID = 1;

  /** The point dx id. */
  private static final int DX_ID = 2;

  /** The point dy id. */
  private static final int DY_ID = 3;

  // Variables
  // ---------

  /** The server and path to the data file. */
  private String path;

  /** The data connection. */
  private DConnect connect;

  /** The cache of bin data. */
  private Map binDataCache = Collections.synchronizedMap (new HashMap());

  /** The next bin for data access. */
  private int nextBin;

  ////////////////////////////////////////////////////////////

  protected void setBinHint (
    int index
  ) {

    // Make sure bin cache contains data
    // ---------------------------------
    if (!binDataCache.containsKey (new Integer (index)))
      throw new RuntimeException ("Bin cache does not contain bin " + index);

    // Set next bin index
    // ------------------
    nextBin = index;

  } // setBinHint

  ////////////////////////////////////////////////////////////

  protected void readData (
    int sdsid,
    int[] start, 
    int[] count,
    Object data
  ) throws IOException {

    // Get cached bin data
    // -------------------
    BinData binData = (BinData) binDataCache.get (new Integer (nextBin));
    if (binData == null)
      throw new IOException ("Bin cache does not contain bin " + nextBin);

    switch (sdsid) {

    // Get segment info data
    // ---------------------
    case SEGMENT_INFO_ID:
      System.arraycopy (binData.segmentInfo, 0, data, 0, count[0]);
      break;

    // Get segment area data
    // ---------------------
    case SEGMENT_AREA_ID:
      System.arraycopy (binData.segmentArea, 0, data, 0, count[0]);
      break;

    // Get point dx data
    // -----------------
    case DX_ID:
      System.arraycopy (binData.dx, 0, data, 0, count[0]);
      break;

    // Get point dy data
    // -----------------
    case DY_ID:
      System.arraycopy (binData.dy, 0, data, 0, count[0]);
      break;

    default:
      throw new IOException ("Unknown variable id: " + sdsid);

    } // switch

  } // readData

  ////////////////////////////////////////////////////////////

  protected void readData (
    String var,
    int[] start, 
    int[] count,
    Object data
  ) throws IOException {

    throw new IOException ("Unsupported method call");

  } // readData

  ////////////////////////////////////////////////////////////

  /**
   * The <code>BinSequenceReader</code> is a special class that
   * holds the information necessary to read a sequence of bins
   * into the cache.  Because a sequence of bins requires a
   * network fetch, this class may be used to read sequences of
   * bins in parallel to speed up the network fetch process.
   */
  private class BinSequenceReader {

    /** The first bin in the sequence. */
    private int firstReadBin;

    /** The last bin in the sequence. */
    private int lastReadBin;

    ////////////////////////////////////////////////////////

    /** Creates a new sequence reader. */
    public BinSequenceReader (
      int firstReadBin,
      int lastReadBin
    ) {

      this.firstReadBin = firstReadBin;
      this.lastReadBin = lastReadBin;

    } // BinSequenceReader constructor

    ////////////////////////////////////////////////////////

    /** Performs the sequence read. */
    public void read () {

      // Get bounds of required bin data
      // -------------------------------
      int firstReadSegment = firstSegment[firstReadBin];
      int lastReadSegment = 
        firstSegment[lastReadBin] + numSegments[lastReadBin] - 1;
      int readSegments = lastReadSegment - firstReadSegment + 1;
      int firstReadPoint = segmentStart[firstReadSegment];
      int lastReadPoint = (
        lastReadSegment == (totalSegments-1) ? 
        totalPoints-1 :
        segmentStart[lastReadSegment+1] - 1
      );
      int readPoints = lastReadPoint - firstReadPoint + 1;

      // Read sequence of bins
      // ---------------------
      List specList = new ArrayList();

      DataSpec segmentInfoSpec = new DataSpec (
        "Embedded_npts_levels_exit_entry_for_a_segment", 
        firstReadSegment, readSegments, new int[readSegments]);
      specList.add (segmentInfoSpec);

      DataSpec segmentAreaSpec = new DataSpec (
        "Ten_times_the_km_squared_area_of_the_parent_polygon_of_a_segmen",
        firstReadSegment, readSegments, new int[readSegments]);
      specList.add (segmentAreaSpec);

      DataSpec dxSpec = new DataSpec (
        "Relative_longitude_from_SW_corner_of_bin",
        firstReadPoint, readPoints, new short[readPoints]);
      specList.add (dxSpec);

      DataSpec dySpec = new DataSpec (
        "Relative_latitude_from_SW_corner_of_bin",
        firstReadPoint, readPoints, new short[readPoints]);
      specList.add (dySpec);

      try { readData (specList); }
      catch (IOException e) { 
        throw new RuntimeException ("Bin access failed:" + e.getMessage());
      } // catch

      // Save data to bin cache
      // ----------------------
      for (int i = firstReadBin; i <= lastReadBin; i++) {

        // Check for empty bin
        // -------------------
        if (numSegments[i] == 0) continue;

        // Get bounds of bin data
        // ----------------------
        int firstBinSegment = firstSegment[i];
        int lastBinSegment = firstSegment[i] + numSegments[i] - 1;
        int firstBinPoint = segmentStart[firstSegment[i]];
        int lastBinPoint = (
          lastBinSegment == (totalSegments-1) ? 
          totalPoints-1 :
          segmentStart[lastBinSegment+1] - 1
        );
        int binPoints = lastBinPoint - firstBinPoint + 1;

        // Create bin cache entry
        // ----------------------
        int[] segmentInfo = new int[numSegments[i]];
        int[] segmentArea = new int[numSegments[i]];
        short[] dx = new short[binPoints];
        short[] dy = new short[binPoints];
        System.arraycopy (segmentInfoSpec.data, 
          firstBinSegment-firstReadSegment, segmentInfo, 0, numSegments[i]);
        System.arraycopy (segmentAreaSpec.data, 
          firstBinSegment-firstReadSegment, segmentArea, 0, numSegments[i]);
        System.arraycopy (dxSpec.data, 
          firstBinPoint-firstReadPoint, dx, 0, binPoints);
        System.arraycopy (dySpec.data, 
          firstBinPoint-firstReadPoint, dy, 0, binPoints);
        binDataCache.put (new Integer (i), 
          new BinData (segmentInfo, segmentArea, dx, dy));

      } // for

    } // read

    ////////////////////////////////////////////////////////

  } // BinSequenceReader class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>BinData</code> class holds data segment information,
   * segment area, and segment dx, dy points for a bin. 
   */
  private static class BinData {

    /** The raw segment information for each segment in a bin. */
    public int[] segmentInfo;

    /** The raw segment area for each segment. */
    public int[] segmentArea;

    /** The dx for each segment point in a bin. */
    public short[] dx;

    /** The dy for each segment point in a bin. */
    public short[] dy;

    /** Creates a new bin data object. */
    public BinData (
      int[] segmentInfo, 
      int[] segmentArea, 
      short[] dx, 
      short[] dy
    ) {

      this.segmentInfo = segmentInfo;
      this.segmentArea = segmentArea;
      this.dx = dx;
      this.dy = dy;

    } // BinData constructor

  } // BinData class

  ////////////////////////////////////////////////////////////

  protected void setBinListHint (
    List indexList
  ) {

    // Find runs of sequential bins
    // ----------------------------
    int bins = indexList.size();
    boolean[] startSequence = new boolean[bins];
    for (int i = 0; i < bins; i++) {
      if (i == 0) startSequence[i] = true;
      else {
        int thisBin = ((Integer) indexList.get (i)).intValue();
        int lastBin = ((Integer) indexList.get (i-1)).intValue();
        startSequence[i] = (thisBin != (lastBin+1));
      } // else
    } // for

    // Read each sequential run
    // ------------------------
    int bin = 0;
    List readerList = new ArrayList();
    while (bin < bins) {

      // Get bin sequence bounds
      // -----------------------
      int firstReadBin = ((Integer) indexList.get (bin)).intValue();
      int sequenceLength = 0;
      do { bin++; sequenceLength++; }
      while (bin < bins && !startSequence[bin]);
      int lastReadBin = firstReadBin + sequenceLength - 1;

      // Trim bounds based on cached bin data
      // ------------------------------------
      while (firstReadBin <= lastReadBin) {
        if (numSegments[firstReadBin] == 0 ||
            binDataCache.containsKey (new Integer (firstReadBin)))
          firstReadBin++;
        else 
          break;
      } // while
      while (lastReadBin >= firstReadBin) {
        if (numSegments[lastReadBin] == 0 ||
          binDataCache.containsKey (new Integer (lastReadBin))) 
          lastReadBin--;
        else 
          break;
      } // while
      if (firstReadBin > lastReadBin) continue;

      // Create reader
      // -------------
      readerList.add (new BinSequenceReader (firstReadBin, lastReadBin));

    } // while

    // Perform bin sequence reads in parallel
    // --------------------------------------
    Executor exec = Executors.newCachedThreadPool();
    final CountDownLatch latch = new CountDownLatch (readerList.size());
    for (Iterator iter = readerList.iterator(); iter.hasNext();) {
      final BinSequenceReader reader = (BinSequenceReader) iter.next();
      exec.execute (new Runnable() {
          public void run () {
            reader.read();
            latch.countDown();
          } // run
        });
    } // for
    try { latch.await(); }
    catch (InterruptedException e) { 
      throw new RuntimeException ("Interrupted waiting for bin sequence read");
    } // catch

  } // setBinListHint

  ////////////////////////////////////////////////////////////

  protected int selectData (
    String var
  ) throws IOException {

    for (int i = 0; i < VAR_NAMES.length; i++) {
      if (VAR_NAMES[i].equals (var)) return (i);
    } // for

    throw new IOException ("Cannot access variable " + var);

  } // selectData

  ////////////////////////////////////////////////////////////

  protected int openFile (
    String name
  ) throws IOException {

    connect = new DConnect (path + "/" + name, true);
    return (0);

  } // openFile 

  ////////////////////////////////////////////////////////////

  /**
   * The <code>DataSpec</code> class holds data specification for a
   * variable read request.
   */
  private static class DataSpec {

    /** The variable name for stat to retrieve. */
    public String var;

    /** The starting data coordinate. */
    public int start;

    /** The count of data values. */
    public int count;

    /** The destination data array. */
    public Object data;

    /** Creates a new data spec. */
    public DataSpec (String var, int start, int count, Object data) {
      this.var = var;
      this.start = start;
      this.count = count;
      this.data = data;
    } // DataSpec constructor

  } // DataSpec class 

  ////////////////////////////////////////////////////////////

  /**
   * Reads the data specified in the list.
   *
   * @param specList the list of data reading specifications.
   *
   * @throws IOException if an error occurred reading the data.
   */
  private void readData (
    List specList
  ) throws IOException {

    // Build constraint expression
    // ---------------------------
    StringBuffer constraint = new StringBuffer();
    constraint.append ("?");
    for (Iterator iter = specList.iterator(); iter.hasNext();) {
      DataSpec spec = (DataSpec) iter.next();
      constraint.append (spec.var + "[" + spec.start + ":1:" + 
        (spec.start+spec.count-1) + "]");
      if (iter.hasNext())
        constraint.append (",");
    } // for

    // Get data from server
    // --------------------
    DataDDS dds;
    try { 
      DConnect newConnect = new DConnect (connect.URL(), true);
      dds = newConnect.getData (constraint.toString(), null); 
    } // try
    catch (Exception e) {
      throw new IOException ("Error getting data: " + e.getMessage());
    } // catch

    // Copy data into destination arrays
    // ---------------------------------
    for (Iterator iter = specList.iterator(); iter.hasNext();) {
      DataSpec spec = (DataSpec) iter.next();
      PrimitiveVector vector;
      try {
        vector = ((DVector) dds.getVariable (spec.var)).getPrimitiveVector();
      } // try
      catch (NoSuchVariableException e) {
        throw new IOException ("Error getting variable " + spec.var);
      } // catch
      System.arraycopy (vector.getInternalStorage(), 0, spec.data, 0, 
        Array.getLength (spec.data));
    } // for

  } // readData

  ////////////////////////////////////////////////////////////

  protected void getGlobalData () throws IOException {

    // Get data descriptor object
    // --------------------------
    DDS dds;
    try { dds = connect.getDDS(); }
    catch (Exception e) { 
      throw new IOException ("Error getting OPeNDAP descriptor data: " + 
        e.getMessage());
    } // catch

    // Get dimensions from DDS
    // -----------------------
    try {
      totalBins = ((DArray) dds.getVariable (
        "N_segments_in_a_bin")).getFirstDimension().getSize();
      totalSegments = ((DArray) dds.getVariable (
        "Id_of_first_point_in_a_segment")).getFirstDimension().getSize();
      totalPoints = ((DArray) dds.getVariable (
        "Relative_longitude_from_SW_corner_of_bin"
        )).getFirstDimension().getSize();
    } // try
    catch (NoSuchVariableException e) {
      throw new IOException ("Error getting variable: " + e.getMessage());
    } // catch

    // Create read spec list
    // ---------------------
    ArrayList specList = new ArrayList();

    DataSpec binSizeSpec = new DataSpec ("Bin_size_in_minutes", 
      0, 1, new int[1]);
    specList.add (binSizeSpec);

    DataSpec lonBinsSpec = new DataSpec ("N_bins_in_360_longitude_range", 
      0, 1, new int[1]);
    specList.add (lonBinsSpec);

    DataSpec latBinsSpec = 
      new DataSpec ("N_bins_in_180_degree_latitude_range", 0, 1, new int[1]);
    specList.add (latBinsSpec);

    firstSegment = new int[totalBins];
    specList.add (new DataSpec ("Id_of_first_segment_in_a_bin", 
      0, totalBins, firstSegment));

    numSegments = new short[totalBins];
    specList.add (new DataSpec ("N_segments_in_a_bin", 
      0, totalBins, numSegments));

    binInfo = new short[totalBins];
    specList.add (new DataSpec ("Embedded_node_levels_in_a_bin", 
      0, totalBins, binInfo));

    segmentStart = new int[totalSegments];
    specList.add (new DataSpec ("Id_of_first_point_in_a_segment", 
      0, totalSegments, segmentStart));

    // Reader data spec list
    // ---------------------
    readData (specList);

    // Assign data values
    // ------------------
    binSize = Array.getInt (binSizeSpec.data, 0) / 60.0;
    multiplier = binSize / 65535.0;
    lonBins = Array.getInt (lonBinsSpec.data, 0);
    latBins = Array.getInt (latBinsSpec.data, 0);

  } // getGlobalData

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new binned GSHHS reader from the database name.  By
   * default, there is no minimum area for polygon selection and no
   * polygons are selected.
   * 
   * @param path the OPeNDAP server path as http://server/path.
   * @param name the database name.  Several predefined database
   * names are available from {@link
   * BinnedGSHHSReaderFactory#getDatabaseName}.
   *
   * @throws IOException if an error occurred reading the file.
   */
  public OpendapGSHHSReader (
    String path,
    String name
  ) throws IOException {

    this.path = path;
    init (name);

  } // OpendapGSHHSReader constructor

  ////////////////////////////////////////////////////////////

} // OpendapGSHHSReader class

////////////////////////////////////////////////////////////////////////
