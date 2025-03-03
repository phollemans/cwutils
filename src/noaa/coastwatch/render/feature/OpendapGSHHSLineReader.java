////////////////////////////////////////////////////////////////////////
/*

     File: OpendapGSHHSLineReader.java
   Author: Peter Hollemans
     Date: 2006/06/26

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
  All rights reserved.

  Developed by: CoastWatch / OceanWatch
                Center for Satellite Applications and Research
                http://coastwatch.noaa.gov

  For conditions of distribution and use, see the accompanying
  license.txt file.

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import noaa.coastwatch.render.feature.BinnedGSHHSLineReader;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DConnect2;
import opendap.dap.DDS;
import opendap.dap.DVector;
import opendap.dap.DataDDS;
import opendap.dap.NoSuchVariableException;
import opendap.dap.PrimitiveVector;

/**
 * The <code>OpendapGSHHSLineReader</code> extends
 * <code>BinnedGSHHSLineReader</code> to read data from an OPeNDAP-enabled
 * binned data file.  Some optimizations are made for network
 * connections.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class OpendapGSHHSLineReader
  extends BinnedGSHHSLineReader {

  // Constants
  // ---------

  /** The array of variable names that will be accessed. */
  private static final String VAR_NAMES[] = new String[] {
    "Hierarchial_level_of_a_segment",
    "N_points_for_a_segment",
    "Relative_longitude_from_SW_corner_of_bin",
    "Relative_latitude_from_SW_corner_of_bin"
  };

  /** The segment info id. */
  private static final int SEGMENT_LEVEL_ID = 0;

  /** The segment area id. */
  private static final int SEGMENT_POINTS_ID = 1;

  /** The point dx id. */
  private static final int DX_ID = 2;

  /** The point dy id. */
  private static final int DY_ID = 3;

  // Variables
  // ---------

  /** The server and path to the data file. */
  private String path;

  /** The data connection. */
  private DConnect2 connect;

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
    if (!binDataCache.containsKey (Integer.valueOf (index)))
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
    BinData binData = (BinData) binDataCache.get (Integer.valueOf (nextBin));
    if (binData == null)
      throw new IOException ("Bin cache does not contain bin " + nextBin);

    switch (sdsid) {

    // Get segment level data
    // ----------------------
    case SEGMENT_LEVEL_ID:
      System.arraycopy (binData.segmentLevel, 0, data, 0, count[0]);
      break;

    // Get segment points data
    // -----------------------
    case SEGMENT_POINTS_ID:
      System.arraycopy (binData.segmentPoints, 0, data, 0, count[0]);
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
   * The <code>BinData</code> class holds data segment information,
   * segment area, and segment dx, dy points for a bin. 
   */
  private static class BinData {

    /** The raw segment level for each segment in a bin. */
    public short[] segmentLevel;

    /** The raw segment points for each segment. */
    public short[] segmentPoints;

    /** The dx for each segment point in a bin. */
    public short[] dx;

    /** The dy for each segment point in a bin. */
    public short[] dy;

    /** Creates a new bin data object. */
    public BinData (
      short[] segmentLevel,
      short[] segmentPoints, 
      short[] dx, 
      short[] dy
    ) {

      this.segmentLevel = segmentLevel;
      this.segmentPoints = segmentPoints;
      this.dx = dx;
      this.dy = dy;

    } // BinData constructor

  } // BinData class

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

      DataSpec segmentLevelSpec = new DataSpec (
        "Hierarchial_level_of_a_segment",
        firstReadSegment, readSegments, new short[readSegments]);
      specList.add (segmentLevelSpec);

      DataSpec segmentPointsSpec = new DataSpec (
        "N_points_for_a_segment",
        firstReadSegment, readSegments, new short[readSegments]);
      specList.add (segmentPointsSpec);

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
        short[] segmentLevel = new short[numSegments[i]];
        short[] segmentPoints = new short[numSegments[i]];
        short[] dx = new short[binPoints];
        short[] dy = new short[binPoints];
        System.arraycopy (segmentLevelSpec.data, 
          firstBinSegment-firstReadSegment, segmentLevel, 0, numSegments[i]);
        System.arraycopy (segmentPointsSpec.data, 
          firstBinSegment-firstReadSegment, segmentPoints, 0, numSegments[i]);
        System.arraycopy (dxSpec.data, 
          firstBinPoint-firstReadPoint, dx, 0, binPoints);
        System.arraycopy (dySpec.data, 
          firstBinPoint-firstReadPoint, dy, 0, binPoints);
        binDataCache.put (Integer.valueOf (i), 
          new BinData (segmentLevel, segmentPoints, dx, dy));

      } // for

    } // read

    ////////////////////////////////////////////////////////

  } // BinSequenceReader class

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

    // Create list of sequence readers
    // -------------------------------
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
            binDataCache.containsKey (Integer.valueOf (firstReadBin)))
          firstReadBin++;
        else 
          break;
      } // while
      while (lastReadBin >= firstReadBin) {
        if (numSegments[lastReadBin] == 0 ||
          binDataCache.containsKey (Integer.valueOf (lastReadBin))) 
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

    connect = new DConnect2 (path + "/" + name, true);
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
      DConnect2 newConnect = new DConnect2 (connect.URL(), true);
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
   * Creates a new reader from the database name.  By default,
   * there is no minimum area for polygon selection and no
   * polygons are selected.
   * 
   * @param path the OPeNDAP server path as http://server/path.
   * @param name the database name.  Several predefined database
   * names are available from {@link
   * BinnedGSHHSReaderFactory#getDatabaseName}.
   *
   * @throws IOException if an error occurred reading the file.
   */
  public OpendapGSHHSLineReader (
    String path,
    String name
  ) throws IOException {

    this.path = path;
    init (name);

  } // OpendapGSHHSLineReader constructor

  ////////////////////////////////////////////////////////////

} // OpendapGSHHSLineReader class

////////////////////////////////////////////////////////////////////////
