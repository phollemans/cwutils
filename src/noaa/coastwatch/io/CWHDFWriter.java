////////////////////////////////////////////////////////////////////////
/*
     FILE: CWHDFWriter.java
  PURPOSE: A class to write CoastWatch HDF format files using the JNI
           interface to the HDF library.
   AUTHOR: Peter Hollemans
     DATE: 2002/06/29
  CHANGES: 2002/07/12, PFH, added setMetaVersion
           2002/07/20, PFH, added navigation attribute
           2002/07/27, PFH, changed swath bounds data to double[]
           2002/07/29, PFH, fixed 2.x/3.x affine transform bug
           2002/11/12, PFH, removed pass type attribute
           2002/11/18, PFH, added default metadata version constant
           2002/12/03, PFH, modified for map projection changes
           2002/12/24, PFH, added updating writer constructor
           2003/08/28, PFH, added pass_type attribute for 2.x files
           2003/11/22, PFH, fixed Javadoc comments
           2004/02/16, PFH, fixed problem writing pass_type for non
             created files in which info == null
           2004/03/23, PFH, modified swath encoding to use List
           2004/06/08, PFH, moved updateNavigation() to CWHDFReader
           2004/09/09, PFH, added handling for non-satellite sources and 
             composite data sets
           2004/09/14, PFH, added writing code for multiple time periods
           2004/09/22, PFH
           - added updateHistory() 
           - modified to write user-level global/variable metadata
           2004/09/23, PFH
           - changed compatibility mode default to "false"
           - modified metadata upgrade test
           2004/09/27, PFH
           - modified to use MetadataServices.toArray()
           - corrected setVariableInfo() to write rows/cols attributes for
             any exception
           2004/09/28, PFH, modified to append history on close
           2004/09/29, PFH, added writing of extra transform metadata
           2004/09/30, PFH
           - modified to write pass_date for all metadata versions
           2005/01/18, PFH, added support for sensor scan projections
           2005/02/14, PFH, fixed inappropriate nav_affine write bug
           2005/05/05, PFH, added package version to history logging
           2005/05/18, PFH, changed "datum" to "spheroid"
           2005/07/18, PFH, modified variable creation to set fraction digits
             for unscaled float and double data
           2007/04/19, PFH, modified to use getToolVersion()
           2007/04/24, PFH, extended to allow DataProjection transform writing
           2009/06/25, PFH, modified to write the "calibrated_nt" attribute
           2010/03/30, PFH, modified constructor to close file on error

  CoastWatch Software Library and Utilities
  Copyright 1998-2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.util.*;
import java.text.*;
import java.awt.geom.*;
import ncsa.hdf.hdflib.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import noaa.coastwatch.tools.*;
import noaa.coastwatch.render.*;

/**
 * A CoastWatch HDF writer is an Earth data writer that writes
 * CoastWatch HDF format files using the HDF library class.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class CWHDFWriter 
  extends HDFWriter {

  // Constants
  // ---------
  /** The number of milliseconds in one day. */
  private final static long MSEC_PER_DAY = (1000L * 3600L * 24L);

  /** The current metadata package version. */
  public final static double CURRENT_METADATA_VERSION = 3.4;

  /** The compatibility metadata package version. */
  public final static double COMPATIBLE_METADATA_VERSION = 2.4;

  /** 
   * The compatibility mode flag.  When compatibility is on the HDF
   * metadata is written to be compatible with the older metadata
   * standard for the CoastWatch utilities version 2.3 and 2.4.
   * Programs that expect the older metadata standard such as CDAT
   * v0.7a or older require compatibility mode to be on.  Note that
   * the swath projection was not supported until version 3.1, thus
   * compatibility mode cannot be used to write files containing swath
   * data.
   */
  public static final String COMPATIBLE_MODE = "cw.compatible.mode";

  /**
   * The chunk size in kilobytes.  The chunk size is used to
   * write chunked HDF variable data.  A chunk size of 0 turns off
   * chunking.
   */
  public static final String CHUNK_SIZE = "cw.chunk.size";

  /**
   * The compression mode flag.  When compression is on the data is
   * compressed using the GZIP deflation algorithm within the HDF
   * file, resulting in a smaller file size.  Some performance
   * degradation is usually encountered when reading compressed HDF
   * datasets.
   */
  public static final String COMPRESS_MODE = "cw.compress.mode";

  /** The property defaults array. */
  public static final String[][] DEFAULTS = {
    {COMPATIBLE_MODE, "false"},
    {CHUNK_SIZE, "512"},
    {COMPRESS_MODE, "true"}
  };

  // Variables 
  // ---------
  /** The metadata version. */
  private double version;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a property value from the system properties.
   * 
   * @param name the property name.
   * 
   * @return the property value from the system properties or the
   * default value if the property is not set.  If the property is
   * unknown, null is returned.
   */
  public static String getProperty (
    String name
  ) {
  
    String value = System.getProperty (name);
    if (value == null) {
      for (int i = 0; i < DEFAULTS.length; i++) 
        if (DEFAULTS[i][0].equals (name)) value = DEFAULTS[i][1];
    } // if
    return (value);

  } // getProperty

  ////////////////////////////////////////////////////////////

  /**
   * Sets the CoastWatch HDF metadata version.  Normally the version
   * is determined from the class defaults.  For compatibility with
   * CDAT 0.7a and older, the version should be set to either 2.3 or
   * 2.4.
   *
   * @param newVersion the CWHDF metadata version.  
   */
  public void setMetaVersion (  
    double newVersion
  ) {

    version = newVersion;

  } // setMetaVersion

  ////////////////////////////////////////////////////////////

  /** Gets the default metadata version. */
  private double getDefaultMetaVersion() {

    return (getProperty (COMPATIBLE_MODE).equals ("true") ? 
     COMPATIBLE_METADATA_VERSION : CURRENT_METADATA_VERSION);

  } // getDefaultMetaVersion

  ////////////////////////////////////////////////////////////

  /** Sets the defaults for compression and chunk size. */
  private void setDefaults () {

    boolean compressMode = getProperty (COMPRESS_MODE).equals ("true");
    setCompressed (compressMode);
    int chunkSize = Integer.parseInt (getProperty (CHUNK_SIZE));
    if (chunkSize == 0) 
      setChunked (false);
    else {
      setChunked (true);
      setChunkSize (chunkSize*1024);
    } // if

  } // setDefaults

  ////////////////////////////////////////////////////////////

  /**
   * Opens an existing CWHDF file using the specified file name.  The
   * default writer settings are determined from the system properties
   * and defaults constants.
   * 
   * @param file the HDF file name.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   */  
  public CWHDFWriter (
    String file
  ) throws HDFException {

    super (file);

    try {
      setDefaults();
      this.version = CWHDFReader.getMetaVersion (sdid);
    } // try
    catch (Exception e) {
      try { close(); }
      catch (IOException e2) { }
      throw new HDFException (e.toString());
    } // catch

  } // CWHDFWriter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a CWHDF writer from the specified file.  The default
   * writer settings are determined from the system properties and
   * defaults constants.
   * 
   * @param info the Earth data info object.
   * @param file the new HDF file name.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred writing the file metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */  
  public CWHDFWriter (
    EarthDataInfo info,
    String file
  ) throws HDFException, IOException, ClassNotFoundException {

    super (info, file);
    setDefaults();
    this.version = getDefaultMetaVersion();
    setGlobalInfo();

  } // CWHDFWriter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a CWHDF writer from the specified file.  The default
   * writer settings are determined from the system properties and
   * defaults constants, except for the metadata version.
   * 
   * @param info the Earth data info object.
   * @param version the metadata version.
   * @param file the new HDF file name.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred writing the file metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */  
  public CWHDFWriter (
    EarthDataInfo info,
    double version,
    String file
  ) throws HDFException, IOException, ClassNotFoundException {

    super (info, file);
    setDefaults();
    this.version = version;
    setGlobalInfo();

  } // CWHDFWriter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Appends a command line to the file history attribute.
   * 
   * @param command the command or program name.
   * @param argv an array of command line arguments.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public void updateHistory (
    String command,
    String[] argv
  ) throws HDFException, ClassNotFoundException {

    // Append command line to history
    // ------------------------------
    String history = (String) HDFReader.getAttribute (sdid, "history");
    history = MetadataServices.append (history, 
      MetadataServices.getCommandLine (command, argv));
    setAttribute (sdid, "history", history);

  } // updateHistory

  ////////////////////////////////////////////////////////////

  protected void setGlobalInfo ()
    throws HDFException, IOException, ClassNotFoundException, 
    UnsupportedEncodingException {

    // Check version
    // -------------
    boolean needsMetadataUpgrade = (
      (version < 3.1 && info.getTransform() instanceof SwathProjection) ||
      (version < 3.2 && !(info instanceof SatelliteDataInfo)) ||
      (version < 3.2 && !info.isInstantaneous()) ||
      (version < 3.3 && info.getTransform() instanceof SensorScanProjection)
    );
    if (needsMetadataUpgrade) {
      System.err.println (this.getClass() + 
        ": Warning: Upgrading metadata version from " + version + " to " + 
        CURRENT_METADATA_VERSION);
      setMetaVersion (CURRENT_METADATA_VERSION);
    } // if

    // Set source and processing attributes
    // ------------------------------------
    if (info instanceof SatelliteDataInfo) {
      setAttribute (sdid, "satellite", 
        ((SatelliteDataInfo) info).getSatellite());
      setAttribute (sdid, "sensor", ((SatelliteDataInfo) info).getSensor());
    } // if
    else {
      setAttribute (sdid, "data_source", info.getSource());
    } // else
    setAttribute (sdid, "origin", info.getOrigin());
    setAttribute (sdid, "history", info.getHistory());
    setAttribute (sdid, "cwhdf_version", Double.toString (version));

    // Set temporal and geographic attributes
    // --------------------------------------
    List periodList = info.getTimePeriods();
    EarthTransform trans = info.getTransform();
    if (periodList.size() == 1) {
      setAttribute (sdid, "pass_type", 
        info.getSceneTime (trans.getDimensions()));
    } // if
    setPeriodList (periodList);
    setTransform (trans);

    // Set user metadata
    // -----------------
    setAttributes (sdid, info.getMetadataMap(), false);

  } // setGlobalInfo

  ////////////////////////////////////////////////////////////

  /**
   * Writes the time period list.  The date and time metadata in the HDF
   * file are converted from the equivalent TimePeriod objects.
   *
   * @param periodList the list of TimePeriod objects to write.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  private void setPeriodList (
    List periodList
  ) throws HDFException, ClassNotFoundException {

    // Create data arrays
    // ------------------
    int periods = periodList.size();
    int[] passDateArray = new int[periods];
    double[] startTimeArray = new double[periods];
    double[] extentArray = new double[periods];
    boolean needExtent = false;
    for (int i = 0; i < periods; i++) {
      TimePeriod period = (TimePeriod) periodList.get (i);
      long msec = period.getStartDate().getTime();
      long extent = period.getDuration();
      if (extent != 0) needExtent = true;
      passDateArray[i] = (int) (msec / MSEC_PER_DAY);
      startTimeArray[i] = (msec % MSEC_PER_DAY)/1000.0;
      extentArray[i] = extent/1000.0;
    } // for

    // Write attribute data
    // --------------------
    setAttribute (sdid, "pass_date", passDateArray);
    setAttribute (sdid, "start_time", startTimeArray);
    if (needExtent) setAttribute (sdid, "temporal_extent", extentArray);

  } // setPeriodList

  ////////////////////////////////////////////////////////////

  /**
   * Writes the Earth transform information.  The projection metadata
   * in the HDF file is converted from the equivalent {@link
   * EarthTransform}.
   *
   * @param trans the Earth transform to write.
   *
   * @throws HDFException if there were errors writing the HDF metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   * @throws UnsupportedEncodingException if the transform class encoding 
   * is not supported.
   * @throws IOException if an error occurred writing the file metadata.
   */
  private void setTransform (
    EarthTransform trans
  ) throws HDFException, ClassNotFoundException, 
    UnsupportedEncodingException, IOException { 

    // Write map projection
    // --------------------
    if (trans instanceof MapProjection) {
      MapProjection map = (MapProjection) trans;
      if (version >= 3) 
        setAttribute (sdid, "projection_type", map.describe());
      setAttribute (sdid, "projection", map.getSystemName()); 

      // Set GCTP paramters
      // ------------------
      setAttribute (sdid, "gctp_sys", new Integer (map.getSystem()));
      setAttribute (sdid, "gctp_zone", new Integer (map.getZone()));
      setAttribute (sdid, "gctp_parm", map.getParameters());
      setAttribute (sdid, "gctp_datum", new Integer (map.getSpheroid()));

      // Set affine transform
      // --------------------
      double[] matrix = new double[6];
      map.getAffine().getMatrix(matrix); 
      if (version >= 3) 
        setAttribute (sdid, "et_affine", matrix);
      else {
        double[] newMatrix = new double[6];
        newMatrix[0] = matrix[2];
        newMatrix[1] = matrix[0];
        newMatrix[2] = matrix[3];
        newMatrix[3] = matrix[1];
        newMatrix[4] = matrix[4] - matrix[0] - matrix[2];
        newMatrix[5] = matrix[5] - matrix[1] - matrix[3];
        setAttribute (sdid, "et_affine", newMatrix);
      } // else

    } // if

    // Write swath projection
    // ----------------------
    else if (trans instanceof SwathProjection && version >= 3) {
      SwathProjection swath = (SwathProjection) trans;
      setAttribute (sdid, "projection_type", swath.describe ());
      Object[] encoding = (Object[]) swath.getEncoding ();

      // Write partition structure info
      // ------------------------------
      byte[] structureData = SwathProjection.toBytes ((BitSet) encoding[0]);
      Line structure = new Line (
        "swath_struct", "Swath partition structure information",
        "", structureData.length, structureData, NumberFormat.getInstance (), 
        null, null);
      writeVariable (structure, false, true);

      // Write partition boundaries
      // --------------------------
      List boundsList = (List) encoding[1];
      int nBounds = boundsList.size();
      double[] boundsData = new double[nBounds*2];
      for (int i = 0; i < nBounds; i++) {
        double[] thisBound = (double[]) boundsList.get (i);
        boundsData[i*2] = thisBound[0];
        boundsData[i*2 + 1] = thisBound[1];
      } // for
      Line bounds = new Line ("swath_bounds", "Swath partition boundaries",
        "", boundsData.length, boundsData, NumberFormat.getInstance (), 
        null, null);
      writeVariable (bounds, false, true);

      // Write lat/lon estimators
      // ------------------------
      List latList = (List) encoding[2];
      List lonList = (List) encoding[3];
      int nParts = latList.size();
      double[] latData = new double[nParts*9];
      double[] lonData = new double[nParts*9];
      for (int i = 0; i < nParts; i++) {
        double[] latPartCoefs = (double[]) latList.get (i);
        double[] lonPartCoefs = (double[]) lonList.get (i);


        // TODO: If we encounter a null coefficient array here, we
        // should store NaN values as the coefficients.



        System.arraycopy (latPartCoefs, 0, latData, i*9, 9);
        System.arraycopy (lonPartCoefs, 0, lonData, i*9, 9);
      } // for
      Line latEst = new Line ("swath_lat", 
        "Swath latitude estimator coefficients", "", latData.length, latData,
        NumberFormat.getInstance (), null, null);
      Line lonEst = new Line ("swath_lon", 
        "Swath longitude estimator coefficients", "", lonData.length, lonData,
        NumberFormat.getInstance (), null, null);
      writeVariable (latEst, false, true);
      writeVariable (lonEst, false, true);
    
    } // else if

    // Write sensor scan projection
    // ----------------------------
    else if (trans instanceof SensorScanProjection && version >= 3.3) {
      SensorScanProjection scan = (SensorScanProjection) trans;
      setAttribute (sdid, "projection_type", scan.describe());
      setAttribute (sdid, "sensor_type", scan.getSensorType());
      setAttribute (sdid, "sensor_code", new Integer (scan.getSensorCode()));
      setAttribute (sdid, "sensor_parm", scan.getParameters());
    } // else if

    // Write data projection
    // ---------------------
    else if (trans instanceof DataProjection && version >= 3) {
      DataProjection data = (DataProjection) trans;
      setAttribute (sdid, "projection_type", SwathProjection.DESCRIPTION);
      writeVariable (data.getLat(), false, false);
      writeVariable (data.getLon(), false, false);
    } // else if

    // Unsupported transform
    // ---------------------
    else
      throw new UnsupportedEncodingException (
        "Unsupported Earth transform");

    // Write transform dimensions
    // --------------------------
    int[] dims = trans.getDimensions();
    setAttribute (sdid, "rows", new Integer (dims[0]));
    setAttribute (sdid, "cols", new Integer (dims[1]));

    // Write Earth polygon data
    // ------------------------
    DataLocation min = new DataLocation (0, 0);
    DataLocation max = new DataLocation (dims[Grid.ROWS]-1, 
      dims[Grid.COLS]-1);
    LineFeature polygon = ((EarthTransform2D) trans).getBoundingBox (min, max, 
      4);
    double[] latArray = new double[polygon.size()];
    double[] lonArray = new double[polygon.size()];
    int i = 0;
    for (Iterator iter = polygon.iterator(); iter.hasNext(); i++) {
      EarthLocation loc = (EarthLocation) iter.next();
      latArray[i] = loc.lat;
      lonArray[i] = loc.lon;
    } // for
    setAttribute (sdid, "polygon_latitude", latArray);
    setAttribute (sdid, "polygon_longitude", lonArray);

    // Write extra transform metadata
    // ------------------------------
    Map map = trans.getMetadataMap();
    for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      setAttribute (sdid, (String) entry.getKey(), entry.getValue());
    } // for

  } // setTransform 

  ////////////////////////////////////////////////////////////

  protected void setVariableInfo (
    int sdsid,
    DataVariable var
  ) throws HDFException, IOException, ClassNotFoundException {

    // Set dimensions
    // --------------
    String name = var.getName();
    if (var instanceof Grid) {

      // Set dimension names
      // -------------------
      int rowid = HDFLibrary.SDgetdimid (sdsid, 0);
      int colid = HDFLibrary.SDgetdimid (sdsid, 1);
      if (!HDFLibrary.SDsetdimname (rowid, "rows") ||
        !HDFLibrary.SDsetdimname (colid, "cols"))
        throw new HDFException ("Cannot set dimension names for " + name);

      // Set navigation transform
      // ------------------------
      AffineTransform nav = ((Grid) var).getNavigation();
      if (!nav.isIdentity() && version >= 3) {
        double[] matrix = new double[6];
        nav.getMatrix (matrix); 
        setAttribute (sdsid, "nav_affine", matrix);
      } // if
      else {
        /**
         * Note: If the navigation was not written in the above if
         * statement, then we don't want it to get written from the
         * metadata either, so we remove it here just in case.
         */
        var.getMetadataMap().remove ("nav_affine");
      } // else

    } // if

    // Set data strings
    // ----------------
    String coordSys;
    try { coordSys = (String) HDFReader.getAttribute (sdid, "projection"); }
    catch (Exception e) { coordSys = ""; }
    if (!HDFLibrary.SDsetdatastrs (sdsid, var.getLongName(), 
      var.getUnits(), "", coordSys))
      throw new HDFException ("Cannot set data strings for " + name);

    // Set fill value
    // --------------
    Object missing = var.getMissing ();
    if (missing != null) {
      Object fillValue = MetadataServices.toArray (missing);
      if (!HDFLibrary.SDsetfillvalue (sdsid, fillValue))
        throw new HDFException ("Cannot set fill value for " + name);
      setAttribute (sdsid, "missing_value", missing);
    } // if

    // Set calibration info
    // --------------------
    double[] scaling = var.getScaling();
    if (scaling != null) {
      int calType;
      try {
        calType = (Integer) var.getMetadataMap().get ("calibrated_nt");
      } // try
      catch (Exception e) {
        calType = 0;
      } // catch
      if (!HDFLibrary.SDsetcal (sdsid, scaling[0], 0, scaling[1], 0, calType))
        throw new HDFException ("Cannot set calibration info for " + name);
    } // if

    // Set digits
    // ----------
    Class c = var.getDataClass();
    if (scaling != null || c.equals (Float.TYPE) || c.equals (Double.TYPE)) {
      setAttribute (sdsid, "fraction_digits", 
        new Integer (var.getFormat().getMaximumFractionDigits()));
    } // if

    // Set user metadata
    // -----------------
    setAttributes (sdsid, var.getMetadataMap(), false);

  } // setVariableInfo

  ////////////////////////////////////////////////////////////

  /** 
   * Overrides the parent close to append the history before closing.
   */ 
  public void close () throws IOException {

    // Check if already closed
    // -----------------------
    if (closed) return;

    // Add to history
    // --------------
    try {
      String history;
      try { history = (String) HDFReader.getAttribute (sdid, "history"); }
      catch (HDFException e) { history = ""; }
      history = MetadataServices.append (history, 
        ToolServices.getToolVersion ("") + ToolServices.getCommandLine());
      setAttribute (sdid, "history", history);
    } // try
    catch (Exception e) { 
      throw new IOException (e.getMessage());
    } // catch

    // Call parent close
    // -----------------
    super.close();

  } // flush

  ////////////////////////////////////////////////////////////

} // CWHDFWriter class

////////////////////////////////////////////////////////////////////////
