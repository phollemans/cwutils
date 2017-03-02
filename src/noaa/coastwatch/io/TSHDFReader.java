////////////////////////////////////////////////////////////////////////
/*

     File: TSHDFReader.java
   Author: Peter Hollemans
     Date: 2002/06/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.io;

// Imports
// -------
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import hdf.hdflib.HDFException;
import noaa.coastwatch.io.HDFLib;
import noaa.coastwatch.io.CachedGrid;
import noaa.coastwatch.io.HDFReader;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.trans.SwathProjection;

/**
 * <p>A <code>TSHDFReader</code> reads SeaSpace TeraScan HDF format
 * files using the HDF library class.  TeraScan HDF files are created
 * using the TeraScan command 'tdftohdf'.  This class has been tested
 * using TeraScan versions 3.1 and 3.2 -- see the <a
 * href="http://www.seaspace.com">SeaSpace web site</a> for
 * information on TeraScan and current versions.</p>
 *
 * <p>Currently only a subset of TeraScan datasets are supported.  In
 * particular:</p>
 * <ul>
 *
 *   <li> Sensor scan data with pre-computed double precision latitude
 *   and longitude variables in degrees.</li>
 *
 *   <li> Mapped projection data in 'mercator', 'emercator',
 *   'polarstereo', and 'rectangular' projections.  The spheroid used
 *   is detected automatically from the TeraScan spheroid
 *   parameters.</li>
 *
 * </ul>
 *
 * <p>When a TeraScan HDF dataset is accessed, only attributes that
 * are required by the reader class are actually read -- for example
 * the date and time information, the projection, the variable scaling
 * factors, and so on.  Other TeraScan user-defined attributes are
 * ignored.  To make up for this problem, there is a mechanism that
 * forces the user-defined attributes to be read into the attribute
 * maps held by the reader (accessed via
 * <code>EarthDataReader.getInfo().getMetadataMap()</code>) and by the
 * variables (accessed by <code>DataVariable.getMetadataMap()</code>).
 * If the user defines a string attribute named
 * <code>import_atts</code> (using <code>setattr</code> for example)
 * which contains a slash-separated list of attribute names, then
 * those attributes listed will be imported into the metadata maps.
 * The <code>import_atts</code> attribute can be defined for a
 * dataset, or for a variable and it will be handled accordingly.
 * This mechanism allows tools like cwimport to import attributes from
 * TeraScan-produced datasets and preserve the user-defined attributes
 * that are of value to the user.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class TSHDFReader
  extends HDFReader {

  // Constants
  // ---------
  /** Swath polynomial size in kilometers. */
  public static final double SWATH_POLY_SIZE = 100.0;

  /** TeraScan projection names. */
  private static final String[] PROJ_NAMES = new String[] {
    "sensor_scan",
    "stereographic",
    "rectangular",
    "orthographic",
    "polarstereo",
    "equidist_azim",
    "mercator",
    "utm",
    "polyconic",
    "albers_conic",
    "lambert_conic",
    "lambert_azim",
    "mollweide",
    "cylindrical",
    "perspective",
    "emercator",
    "georect",
    "normalized_geo"
  };

  /** The data format description. */
  private static final String DATA_FORMAT = "TeraScan HDF";

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (DATA_FORMAT); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a TSHDF reader from the specified file.
   * 
   * @param file the file name to read.
   *
   * @throws IOException if an error opening or reading the file
   * metadata.
   */  
  public TSHDFReader (
    String file
  ) throws IOException {

    super (file);

  } // TSHDFReader constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a set of attributes from the SDID or SDSID that have been
   * marked for import in the TeraScan HDF file.  The attributes are
   * placed into the map.
   *
   * @param sdid the HDF SDID or SDSID to read from.
   * @param attMap the attribute map to add attribute values to.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  private void getImportAttributes (
    int sdid,
    Map attMap
  ) throws HDFException, ClassNotFoundException {
    
    // Get import attribute
    // --------------------
    String importAtt;
    try { importAtt = (String) getAttribute (sdid, "import_atts"); }
    catch (Exception e) { return; }

    // Get list of attributes
    // ----------------------
    String[] importArray = importAtt.split ("/");
    getAttributes (sdid, Arrays.asList (importArray), attMap);

  } // getImportAttributes

  ////////////////////////////////////////////////////////////

  protected EarthDataInfo getGlobalInfo () throws HDFException, 
    IOException, NoninvertibleTransformException, ClassNotFoundException {

    // Get simple attributes
    // ---------------------
    String sat;
    try { sat = (String) getAttribute (sdid, "satellite"); }
    catch (HDFException e) { sat = "unknown"; }
    String sensor;
    try { sensor = (String) getAttribute (sdid, "sensor_name"); }
    catch (HDFException e) { sensor = "unknown"; }
    String origin;
    try { origin = (String) getAttribute (sdid, "origin"); }
    catch (HDFException e) { origin = "SeaSpace TeraScan"; }
    String history;
    try { history = (String) getAttribute (sdid, "history"); }
    catch (HDFException e) { history = ""; }

    // Get date and transform
    // ----------------------
    Date date = getDate();
    EarthTransform transform = getTransform();

    // Create info object and add global dimensions
    // --------------------------------------------
    EarthDataInfo info = new SatelliteDataInfo (sat, sensor, date,
      transform, origin, history);

    // Get imported attributes (if any)
    // --------------------------------
    getImportAttributes (sdid, info.getMetadataMap());

    // Return info object
    // ------------------
    return (info);

  } // getGlobalInfo

  //////////////////////////////////////////////////////////////////////

  public DataVariable getPreview (
    int index
  ) throws IOException {

    // Get variable preview from super
    // -------------------------------
    DataVariable var = super.getPreview (index);

    // Add any import attributes needed
    // --------------------------------
    try {
      int sdsid = HDFLib.getInstance().SDselect (sdid, HDFLib.getInstance().SDnametoindex (sdid, 
        variables[index]));
      if (sdsid < 0) 
        throw new HDFException ("Cannot access variable at index " + index);
      getImportAttributes (sdsid, var.getMetadataMap());
      HDFLib.getInstance().SDendaccess (sdsid);
    } // try
    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

    return (var);

  } // getPreview

  //////////////////////////////////////////////////////////////////////

  /**
   * Reads the date and time.  The date and time metadata in the HDF
   * file are converted into the equivalent <code>Date</code>.
   *
   * @return a new date based on the HDF file data.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  private Date getDate () 
    throws HDFException, ClassNotFoundException {

    // Read the date and time
    // ----------------------
    int passDate;
    double startTime;
    try {
      passDate = ((Integer) getAttribute (sdid, "pass_date")).intValue();
      startTime = ((Double) getAttribute (sdid, "start_time")).doubleValue();
    } // try
    catch (HDFException e) { return (new Date (0)); }

    // Unpack TeraScan date
    // --------------------
    /**
     * Note: TeraScan stores the date as an integer value in the form
     * YYMMDD where YY is the last two digits of the year.
     */
    int year = passDate / 10000;
    int month = (passDate - year*10000) / 100;
    int day = passDate - year*10000 - month*100;
    if (year < 100) {
      if (year < 70) year += 2000;
      else year += 1900;
    } // if

    // Unpack TeraScan time
    // --------------------
    /**
     * Note: TeraScan stores the time of day as a double value in the
     * form HHMMSS.MMM where HH is 24-hour time and MMM is the number
     * of milliseconds.
     */
    int hour = (int) (startTime / 1e4);
    int minute = (int) ((startTime - hour*1e4) / 1e2);
    int second = (int) (startTime - hour*1e4 - minute*1e2);
    int millisecond = (int) (startTime - (int)startTime) * 1000;
      
    // Create date object
    // ------------------
    Calendar cal = new GregorianCalendar (year, month-1, day, hour, minute, 
      second);
    cal.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
    return (new Date (cal.getTimeInMillis() + millisecond));

  } // getDate

  //////////////////////////////////////////////////////////////////////

  /**
   * Reads the earth transform information.  The projection metadata
   * in the HDF file is converted into the equivalent {@link
   * MapProjection} or {@link SwathProjection}.
   *
   * @return an earth transform based on the HDF file data.
   *
   * @throws HDFException if there were errors reading the HDF metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   * @throws NoninvertibleTransformException if the map projection object
   * could not be initialized.
   */
  private EarthTransform getTransform () 
    throws HDFException, ClassNotFoundException,
    NoninvertibleTransformException {

    // Get projection
    // --------------
    String projection;
    try {
      projection = (String) getAttribute (sdid, "projection_name");
    } // try
    catch (HDFException e) {
      int projectionIndex = ((Integer) getAttribute (sdid, 
        "projection")).intValue();
      projection = PROJ_NAMES[projectionIndex];
    } // catch
    
    // Create swath
    // ------------
    if (projection.equals ("sensor_scan")) {
      try {
        DataVariable lat = getVariable ("latitude");
        int cols = lat.getDimensions()[Grid.COLS];
        ((CachedGrid) lat).setTileDims (new int[] {1, cols});
        DataVariable lon = getVariable ("longitude");
        ((CachedGrid) lon).setTileDims (new int[] {1, cols});
        return (new SwathProjection (lat, lon, SWATH_POLY_SIZE, 
          new int[] {cols, cols}));
      } // try
      catch (Exception e) {
        return (null);
      } // catch
    } // if

    // Create map
    // ----------
    else {

      // Create affine
      // -------------
      double[] a = (double[]) getAttribute (sdid, "et_affine");
      double[] matrix = new double[6];
      matrix[0] = a[1]/(a[1]*a[2] - a[0]*a[3]);
      matrix[1] = a[3]/(a[0]*a[3] - a[1]*a[2]);
      matrix[2] = -a[0]/(a[1]*a[2] - a[0]*a[3]);
      matrix[3] = -a[2]/(a[0]*a[3] - a[1]*a[2]);
      matrix[4] = (a[1] - a[0] - a[4]*a[1] + a[5]*a[0]) /
        (a[1]*a[2] - a[0]*a[3]);
      matrix[5] = (a[3] - a[2] - a[4]*a[3] + a[5]*a[2]) / 
        (a[0]*a[3] - a[1]*a[2]);
      for (int i = 0; i < 6; i++) matrix[i] *= 1e3;
      AffineTransform affine = new AffineTransform (matrix);

      // Initialize GCTP parameters
      // --------------------------
      int system = 0;
      int zone = 0;
      double[] parameters = new double[15];

      // Get spheroid parameters
      // -----------------------
      double semiMajor = ((Double) getAttribute (sdid,
        "equator_radius")).doubleValue() * 1000;
      double flattening = ((Double) getAttribute (sdid,
        "flattening")).doubleValue();

      // Calculate semi-minor axis
      // -------------------------
      /*
       * Some formulas are useful here:
       *   invFlattening = major / (major - minor)
       *   flattening = (major - minor) / major = 1 - minor/major
       *   eccentricity^2 = 1 - (minorAxis / majorAxis)^2
       */
      double semiMinor = (1 - flattening)*semiMajor;

      // Get spheroid
      // ------------
      int spheroid = EarthTransform.getSpheroid (semiMajor, semiMinor);
      if (spheroid == -1) 
        throw new HDFException ("Cannot determine spheroid from parameters");

      // Equirectangular
      // ---------------
      if (projection.equals ("rectangular")) {
        system = GCTP.EQRECT;
        double clon = ((Double) getAttribute (sdid, 
          "center_lon")).doubleValue();
        parameters[4] = GCTP.pack_angle (clon);
        double clat = ((Double) getAttribute (sdid, 
          "center_lat")).doubleValue();
        parameters[5] = GCTP.pack_angle (clat);
      } // if

      // Polar stereographic
      // -------------------
      else if (projection.equals ("polarstereo")) {
        system = GCTP.PS;
        double plat = ((Double) getAttribute (sdid, 
          "proj_param")).doubleValue ();
        parameters[5] = GCTP.pack_angle (plat);
      } // else if

      // Mercator
      // --------
      else if (projection.equals ("mercator") || 
        projection.equals ("emercator")) {
        system = GCTP.MERCAT;
        double clon = ((Double) getAttribute (sdid, 
          "center_lon")).doubleValue();
        parameters[4] = GCTP.pack_angle (clon);
        double clat = ((Double) getAttribute (sdid, 
          "center_lat")).doubleValue();
        parameters[5] = GCTP.pack_angle (clat);
      } // else if

      // Unknown
      // -------
      else return (null);

      // Get dimensions
      // --------------
      int rows = getVariableDimensions (sdid, "line")[0];
      int cols = getVariableDimensions (sdid, "sample")[0];

      // Return the projection
      // ---------------------
      return (MapProjectionFactory.getInstance().create (system, 
        zone, parameters, spheroid, new int[] {rows, cols}, affine));

    } // else

  } // getTransform

  //////////////////////////////////////////////////////////////////////

} // TSHDFReader class

////////////////////////////////////////////////////////////////////////
