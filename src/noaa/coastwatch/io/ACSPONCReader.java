////////////////////////////////////////////////////////////////////////
/*
     FILE: ACSPONCReader.java
  PURPOSE: Reads ACSPO data through the NetCDF 4 interface.
   AUTHOR: X. Liu
     DATE: 2011/01/25
  CHANGES: 2013/02/05, PFH, updated to handle latest v2.2 metadata
           2013/02/12, PFH, added support for cached grids to getActualVariable
           2013/05/13, PFH, fixed date parsing
           2014/08/08, PFH
           - Changes: Updated to use the TileCachedGrid and NCTileSource classes
             in getActualVariable().  Also fixed missing value for unsigned byte
             data variables.
           - Issue: The NCCachedGrid class uses a per-variable cache of limited
             flexibility, and we needed to be able to handle larger chunk sizes
             than it allowed for with tracking of total cache size across variables.
             The TileCachedGrid makes this possible.  We were getting chunk size
             too large messages when reading certain files.  Also, the missing value for
             byte data should only be applied to signed byte types, not unsigned
             according to Yury Kihai via email July 8, 2014.  We were seeing 
             issues with nighttime data where the cloud bitmask data for SST 
             was showing clear in situations where it should have been masked,
             because the value was being ignored as missing data.
           2015/04/30, PFH
           - Changes: Added new getGeoTransform() method.
           - Issues: We needed support for the Normalized Geostationary 
             Projection, or in our case the Ellipsoid Perspective Projection 
             transform class.
 
 
  CoastWatch Software Library and Utilities
  Copyright 1998-2015, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import noaa.coastwatch.io.NCReader;
import noaa.coastwatch.io.tile.NCTileSource;
import noaa.coastwatch.io.tile.TileCachedGrid;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.SwathProjection;
import noaa.coastwatch.util.trans.EllipsoidPerspectiveProjection;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/** 
 * The <code>ACSPONCReader</code> class reads Java NetCDF accessible
 * datasets and uses the ACSPO metadata conventions to parse
 * the attribute and variable data.
 *
 * @author Xiaoming Liu
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class ACSPONCReader
  extends NCReader {

  // Constants
  // ---------

  /** The data format description. */
  private String DATA_FORMAT;
  
  /** Swath polynomial size in kilometers. */
  public static final double SWATH_POLY_SIZE = 100.0;

  /** The latitude variable name. */
  private static final String LAT_VAR = "latitude";

  /** The longitude variable name. */
  private static final String LON_VAR = "longitude";

  /** ISO date format. */
  private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ssX";

  // Variables
  // ---------
  
  /** The data format description. */
  private String dataFormat;

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (dataFormat); }

  ////////////////////////////////////////////////////////////
  
  /** Gets the earth data info object. */
  private EarthDataInfo getGlobalInfo () throws IOException {

    // Get simple attributes
    // ---------------------
    String sat = (String) getAttribute ("SATELLITE");
    String sensor = (String) getAttribute ("SENSOR");
    String origin = "USDOC/NOAA/NESDIS";
    
    String processor = (String) getAttribute ("PROCESSOR");
    dataFormat = processor + " NetCDF 4";

    String created = (String) getAttribute ("CREATED");
    String history = "[" + processor + "] " + created;

    // Get time period list and transform
    // ----------------------------------
    List periodList = getPeriodList();
    EarthTransform transform = getTransform();

    // Create info object
    // ------------------
    EarthDataInfo info = new SatelliteDataInfo (sat, sensor, periodList, transform, 
        origin, history); // else

    return (info);

  } // getGlobalInfo

  ////////////////////////////////////////////////////////////

  /** Gets the list of time periods. */
  private List getPeriodList () throws IOException {

    // Read data
    // ---------
    String startCoverage = (String) getAttribute ("TIME_COVERAGE_START");
    String endCoverage = (String) getAttribute ("TIME_COVERAGE_END");
    
    // Create date objects
    // -------------------
    Date startDate, endDate;
    try {
      startDate = DateFormatter.parseDate (startCoverage, ISO_DATE_FMT);
      endDate = DateFormatter.parseDate (endCoverage, ISO_DATE_FMT);
    } // try
    catch (ParseException e) {
      throw new UnsupportedEncodingException ("Cannot parse start/end dates");
    } // catch
    
    // Create period list
    // ------------------
    List periodList = new ArrayList();
    periodList.add (new TimePeriod (startDate, endDate.getTime() - 
      startDate.getTime()));

    return (periodList); 
	   
  } // getPeriodList

  ////////////////////////////////////////////////////////////

  /** Gets the earth transform information. */
  private EarthTransform getTransform () throws IOException {

    EarthTransform trans;
    
    // Check for AHI sensor
    // --------------------
    String sensor = (String) getAttribute ("SENSOR");
    if (sensor.equals ("AHI")) {
      trans = getGeoTransform();
    } // if

    // Otherwise assume a polar orbiter-like sensor
    // --------------------------------------------
    else {
      trans = getSwathTransform();
    } // else

    return (trans);

  } // getTransform

  ////////////////////////////////////////////////////////////

  /** Gets the earth transform information for geostationary projection. */
  private EarthTransform getGeoTransform () throws IOException {

    /**
     * This next part gets and checks the attributes.  In some cases, we
     * expect attributes to have certain values and if they don't, we
     * issue a message.  The attribute values look something like this:
     *
     * Sub_Lon = 140.7
     * Dist_Virt_Sat = 42164.0
     * Dist_Real_Sat = 42159.69555133015
     * Earth_Radius_Equator = 6378.137
     * Earth_Radius_Polar = 6356.7523
     * CFAC = 20466275
     * LFAC = 20466275
     * COFF = 2750.5
     * LOFF = 2750.5
     */

    // Get projection attributes
    // -------------------------
    double subpointLon = ((Number) getAttribute ("Sub_Lon")).doubleValue();
    double satDist = ((Number) getAttribute ("Dist_Virt_Sat")).doubleValue();
    int columnFactor = ((Number) getAttribute ("CFAC")).intValue();
    int lineFactor = ((Number) getAttribute ("LFAC")).intValue();


    // TODO: Check these are correct for what we expect.  Our implementation
    // doesn't use these extra parameter values, they're assumed to be the same
    // for every data file.  The ellipsoid should always be WGS84 and the COFF/LOFF
    // should always indicate the center of the pixel line and column to be
    // the center of the projection.
    
    double eqRadius = ((Number) getAttribute ("Earth_Radius_Equator")).doubleValue();
    double polarRadius = ((Number) getAttribute ("Earth_Radius_Polar")).doubleValue();
    double columnOffset = ((Number) getAttribute ("COFF")).doubleValue();
    double lineOffset = ((Number) getAttribute ("LOFF")).doubleValue();

    
    // Create projection
    // -----------------
    DataVariable prototype = getPreview (0);
    int[] dims = prototype.getDimensions();
    EarthTransform trans = new EllipsoidPerspectiveProjection (
      // Subpoint latitude in degrees (geocentric).
      // Subpoint longitude in degrees.
      // Distance of satellite from center of Earth in kilometers.
      // Scan step angle in row direction in radians.
      // Scan step angle in column direction in radians.
      new double[] {
        0,
        subpointLon,
        satDist,
        Math.toRadians (65536.0/lineFactor),
        Math.toRadians (65536.0/columnFactor)
      },
      dims
    );

    return (trans);
    
  } // getGeoTransform

  ////////////////////////////////////////////////////////////

  /** Gets the earth transform information for swath projection. */
  private EarthTransform getSwathTransform () throws IOException {
	   
    // Create swath
    // ------------
    EarthTransform trans = null;
    DataVariable lat = null, lon = null;
    try {
      lat = getVariable (LAT_VAR);
      int cols = lat.getDimensions()[Grid.COLS];
      lon = getVariable (LON_VAR);
      if (dataProjection) {
        trans = new DataProjection (lat, lon);
      } // if
      else {
        trans = new SwathProjection (lat, lon, SWATH_POLY_SIZE, 
          new int[] {cols, cols});
      } // else
    } // try
    catch (Exception e) {
      System.err.println (this.getClass() + 
        ": Warning: Problems encountered using earth location data");
      e.printStackTrace();
      if (lat != null && lon != null) {
        System.err.println (this.getClass() + 
          ": Warning: Falling back on data-only projection, " +
          "earth location reverse lookup will not function");
        trans = new DataProjection (lat, lon);
      } // if
    } // catch

    return (trans);
	   
  } // getSwathTransform

  ////////////////////////////////////////////////////////////

  /** Gets the list of variable names. */
  private String[] getVariableNames () {

    // Create list of 2D non-coordinate variables
    // ------------------------------------------
    List<String> nameList = new ArrayList<String>();
    for (Variable var : dataset.getReferencedFile().getVariables()) {
      if (var.getRank() == 2 && !var.isCoordinateVariable())
        nameList.add (var.getShortName());
    } // for
    
    return ((String[]) nameList.toArray (new String[]{}));

  } // getVariableNames

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new reader from the specified file. 
   * 
   * @param name the file name or URL to read.
   *
   * @throws IOException if an error occurred reading the metadata.
   */
  public ACSPONCReader (
    String name
  ) throws IOException {

    super (name);
    variables = getVariableNames();
    info = getGlobalInfo();

  } // ACSPONCReader constructor

  ////////////////////////////////////////////////////////////

  protected DataVariable getPreviewImpl (
    int index
  ) throws IOException { 

    // Access variable
    // ---------------
    Variable var = dataset.getReferencedFile().findVariable (variables[index]);
    if (var == null)
      throw new IOException ("Cannot access variable at index " + index);

    // Get variable info
    // -----------------
    int varDims[] = var.getShape();
    String name = var.getShortName();
    Class varClass = var.getDataType().getPrimitiveClassType();
    boolean isUnsigned = var.isUnsigned();
    
    // Create fake data array
    // ----------------------
    Object data = Array.newInstance (varClass, 1);

    // Get data strings
    // ----------------
    String longName = (String) getAttribute (var, "Description");
    if (longName == null) longName = "";
    String units = (String) getAttribute (var, "UNITS");
    if (units == null) units = "";
    if (units.equals ("none")) units = "";

    // Set missing value and print format
    // ----------------------------------
    Object missing;
    NumberFormat format;
    if (varClass.equals (Byte.TYPE)) {
      if (isUnsigned)
        missing = null;
      else
        missing = getAttribute ("MISSING_VALUE_INT1");
      format = new DecimalFormat ("0");
    } // if
    else if (varClass.equals (Short.TYPE)) {
      missing = getAttribute ("MISSING_VALUE_INT2");
      format = new DecimalFormat ("0");
    } // else if
    else if (varClass.equals (Integer.TYPE)) {
      missing = getAttribute ("MISSING_VALUE_INT4");
      format = new DecimalFormat ("0");
    } // else if
    else if (varClass.equals (Float.TYPE)) {
      missing = getAttribute ("MISSING_VALUE_REAL4");
      format = new DecimalFormat ("0.######");
    } // else if
    else {
      throw new UnsupportedEncodingException (
        "Unsupported variable class " + varClass + " for " + name);
    } // else
    
    // Check for non-standard missing value
    // ------------------------------------
    Object bathMissing = getAttribute ("Bathymetry_Missing_Value");
    if (bathMissing != null) missing = bathMissing;
    
    // Create variable
    // ---------------
    DataVariable dataVar = new Grid (name, longName, units, varDims[0],
      varDims[1], data, format, null, missing);
    dataVar.setUnsigned (isUnsigned);
      
    // Return the new variable
    // -----------------------
    return (dataVar);

  } // getPreviewImpl

  ////////////////////////////////////////////////////////////

  protected DataVariable getActualVariable (
    int index
  ) throws IOException {

    // Get a variable preview
    // ----------------------
    DataVariable varPreview = getPreview (index);

    // Create tile cached grid
    // -----------------------
    DataVariable dataVar;
    if (varPreview instanceof Grid) {
      Grid grid = (Grid) varPreview;
      NCTileSource source = new NCTileSource (dataset.getReferencedFile(),
        grid.getName(), Grid.ROWS, Grid.COLS, new int[] {0, 0});
      TileCachedGrid cachedGrid = new TileCachedGrid (grid, source);
      dataVar = cachedGrid;
    } // if
    
    // Fill preview with data
    // ----------------------
    else {
      Variable var = dataset.getReferencedFile().findVariable (variables[index]);
      if (var == null)
        throw new IOException ("Cannot access variable at index " + index);
      Object data = var.read().getStorage();
      varPreview.setData (data);
      dataVar = varPreview;
    } // else

    return (dataVar);

  } // getActualVariable

  ////////////////////////////////////////////////////////////

} // ACSPONCReader class

////////////////////////////////////////////////////////////////////////

