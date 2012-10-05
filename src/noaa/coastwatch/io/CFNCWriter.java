////////////////////////////////////////////////////////////////////////
/*
     FILE: CFNCWriter.java
  PURPOSE: A class to write NetCDF/CF format files.
   AUTHOR: Peter Hollemans
     DATE: 2010/02/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.util.*;
import java.awt.geom.*;
import ucar.nc2.*;
import ucar.ma2.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import noaa.coastwatch.tools.*;

/**
 * A CF NetCDF writer creates NetCDF format files with CF
 * metadata using the Unidata Java NetCDF library.
 *
 * Some implementation notes:
 *
 * 1) The Java NetCDF library documentation mentions that it can
 * be extremely I/O intensive to put the file back into define
 * mode (via NetcdfFileWriteable.setRedefineMode (true)), because
 * it may require that the entire file be re-written to
 * accomodate the new header information.  To avoid this issue,
 * we reserve 64 kb of extra space for header information.
 * **NOTE** Currently, there is a bug in the NetCDF library that
 * makes this option not work correctly.  Waiting for it to be
 * fixed.  For now, we have to suffer the performance loss.
 *
 * 2) The NetCDF 3 data model does not allow for unsigned data
 * types.  If unsigned variable data is present (as specified by
 * the {@link DataVariable#getUnsigned} flag), it is written as
 * signed data of the next largest NetCDF data type.  This rule
 * applies for 16-bit and 32-bit integer data, but for 8-bit
 * bytes, we use the NetCDF byte type and indicated unsignedness
 * using the valid_range attribute as recommended by the CF
 * conventions.
 *
 * 3) As much as possible, the CF metadata written to the NetCDF
 * file conforms to the examples laid out in the document
 * "Encoding CoastWatch Satellite Data in NetCDF using the CF
 * Metadata Conventions", Peter Hollemans, February 2010.  Some
 * of the metadata attributes require that CWHDF style attributes
 * be available in the input stream.  This will generally not be
 * the case for non-CW input files.  For now, this is how we
 * handle things because the CW data model does not handle all
 * attributes as full properties of the model (such as date and
 * time, these are fully recognized properties of the model) and
 * so must rely on the generic metadata container attributes to
 * populate attributes in the CF output stream.  For example, the
 * CF attribute "ancillary_variables" is populated using the
 * CWHDF attributes "quality_mask" and "direction_variable" but
 * the CW data model does not include the concept of ancillary
 * variables explicitly, even though the CWHDF format supports
 * it.
 *
 */
public class CFNCWriter
  extends EarthDataWriter {

  // Constants
  // ---------
  /** Extra bytes to put into the file header. */
  private static final int EXTRA_HEADER_BYTES = 64*1024;

  /** History date format. */
  private static final String DATE_FMT = "yyyy-MM-dd HH:mm:ss z";

  /** ISO date format. */
  private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** The time dimension index in the dimensions array. */
  private static final int TIME_INDEX = 0;

  /** The level dimension index in the dimensions array. */
  private static final int LEVEL_INDEX = 1;

  /** The row dimension index in the dimensions array. */
  private static final int ROW_INDEX = 2;

  /** The column dimension index in the dimensions array. */
  private static final int COLUMN_INDEX = 3;

  /** The name of the coordinate reference variable. */
  private static final String COORD_REF = "coord_ref";

  /** The name of the time variable. */
  private static final String TIME_VAR = "time";

  /** The name of the level variable. */
  private static final String LEVEL_VAR = "level";

  /** The name of the x projection variable. */
  private static final String PROJ_X_VAR = "x";
  
  /** The name of the y projection variable. */
  private static final String PROJ_Y_VAR = "y";
  
  /** The name of the latitude variable. */
  private static final String LAT_VAR = "lat";

  /** The name of the longitude variable. */
  private static final String LON_VAR = "lon";

  /** The name of the time bounds variable. */
  private static final String TIME_BOUNDS_VAR = "time_bounds";

  /** The name of the time dimension. */
  private static final String TIME_DIM = TIME_VAR;

  /** The name of the level dimension. */
  private static final String LEVEL_DIM = LEVEL_VAR;

  /** The name of the line dimension. */
  private static final String LINE_DIM = "line";

  /** The name of the sample dimension. */
  private static final String SAMPLE_DIM = "sample";

  /** The name of the latitude dimension. */
  private static final String LAT_DIM = "lat";

  /** The name of the longitude dimension. */
  private static final String LON_DIM = "lon";

  /** The name of the row dimension. */
  private static final String ROW_DIM = "row";

  /** The name of the column dimension. */
  private static final String COLUMN_DIM = "column";

  /** The name of the n values dimension. */
  private static final String NVALS_DIM = "n_vals";

  /** The threshold for comparing longitude bounds. */
  private static final double LON_EPSILON = 0.5;

  /** The threshold for comparing scaling factor and offsets to 1 and 0. */
  private static final double SCALE_EPSILON = 1e-10;

  /** The enumeration of boundary crossing cases. */
  private static enum BoundCase {
    POLAR,
    PACIFIC,
    ATLANTIC,
    NORMAL
  };

  // Variables
  // ---------
  /** The netCDF writeable file. */
  private NetcdfFileWriteable ncFile;
  
  /** Flag to signify that the file is closed. */
  private boolean closed;

  /** The dimensions for the main variables. */
  private Dimension[] ncDims;

  /** The latitude bounds as [min,max]. */
  private double[] latBounds = 
    new double[] {Double.MAX_VALUE, -Double.MAX_VALUE};

  /** The longitude bounds as [min,max]. */
  private double[] lonBounds = 
    new double[] {Double.MAX_VALUE, -Double.MAX_VALUE};

  /** The longitude bounds as [min,max] in the 0..360 coordinate system. */
  private double[] lonBounds360 = 
    new double[] {Double.MAX_VALUE, -Double.MAX_VALUE};

  /** The CW metadata flag, true to write CW metadata. */
  private boolean writeCw;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new NetCDF file from the specified info and file
   * name.  The required CF data variables and attributes are
   * written to the file.
   *
   * @param info the earth data info object.
   * @param file the new NetCDF file name.
   * @param writeDcs DCS metadata flag, true to write DCS
   * attributes.
   * @param writeCw CW metadata flag, true to write CW
   * attributes.
   *
   * @throws IOException if an error occurred creating the file
   * or writing the initial data.
   */
  public CFNCWriter (
    EarthDataInfo info,
    String file,
    boolean writeDcs,
    boolean writeCw
  ) throws IOException {

    super (file);

    // Create new file
    // ---------------
    closed = true;
    ncFile = NetcdfFileWriteable.createNew (file, false);
    //ncFile.setExtraHeaderBytes (EXTRA_HEADER_BYTES);
    // TODO: This call causes writing problems later on.
    closed = false;

    // Setup
    // -----
    this.info = info;
    this.writeCw = writeCw;

    // Write initial data structures
    // -----------------------------
    try { 
      writeCFInfo(); 
      if (writeDcs) writeDCSInfo();
      if (writeCw) writeCWInfo();
    } // try
    catch (InvalidRangeException e) { throw new IOException (e.toString()); } 

  } // CFNCWriter

  ////////////////////////////////////////////////////////////

  /** Writes CF metadata for the specified map projection. */
  private void writeMapProjection (MapProjection map) throws IOException {

    // Set projection parameters
    // -------------------------
    double[] params = map.getParameters();
    Map<String,Object> attributes = new LinkedHashMap<String,Object>();
    switch (map.getSystem()) {

    case ProjectionConstants.ALBERS:
      attributes.put ("grid_mapping_name", "albers_conical_equal_area");
      Array albersStdPara = Array.factory (double.class, new int[] {2}, 
        new double[] {GCTP.unpack_angle( params[2]), 
        GCTP.unpack_angle (params[3])});
      attributes.put ("standard_parallel", albersStdPara);
      attributes.put ("longitude_of_central_meridian", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin", 
        GCTP.unpack_angle (params[5]));
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    case ProjectionConstants.AZMEQD:
      attributes.put ("grid_mapping_name", "azimuthal_equidistant");
      attributes.put ("longitude_of_projection_origin", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin", 
        GCTP.unpack_angle (params[5]));
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    case ProjectionConstants.LAMAZ:
      attributes.put ("grid_mapping_name", "lambert_azimuthal_equal_area");
      attributes.put ("longitude_of_projection_origin", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin", 
        GCTP.unpack_angle (params[5]));
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    case ProjectionConstants.LAMCC:
      attributes.put ("grid_mapping_name", "lambert_conformal_conic");
      Array lamCCStdPara = Array.factory (double.class, new int[] {2}, 
        new double[] {GCTP.unpack_angle (params[2]), 
        GCTP.unpack_angle (params[3])});
      attributes.put ("standard_parallel", lamCCStdPara);
      attributes.put ("longitude_of_central_meridian", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin",
        GCTP.unpack_angle (params[5]));
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    case ProjectionConstants.GEO:
      break;

    case ProjectionConstants.MERCAT:
      attributes.put ("grid_mapping_name", "mercator");
      attributes.put ("longitude_of_projection_origin", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("standard_parallel", GCTP.unpack_angle (params[5]));
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    case ProjectionConstants.ORTHO:
      attributes.put ("grid_mapping_name", "orthographic");
      attributes.put ("longitude_of_projection_origin", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin", 
        GCTP.unpack_angle (params[5]));
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;
      
    case ProjectionConstants.PS:
      attributes.put ("grid_mapping_name", "polar_stereographic");
      attributes.put ("straight_vertical_longitude_from_pole", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin", 
        params[5] < 0 ? -90.0 : +90.0);
      attributes.put ("standard_parallel", GCTP.unpack_angle (params[5]));
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    case ProjectionConstants.STEREO:
      attributes.put ("grid_mapping_name", "stereographic");
      attributes.put ("longitude_of_projection_origin", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin", 
        GCTP.unpack_angle (params[5]));
      attributes.put ("scale_factor_at_projection_origin", 1.0);
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    case ProjectionConstants.TM:
      attributes.put ("grid_mapping_name", "transverse_mercator");
      attributes.put ("scale_factor_at_central_meridian", params[2]);
      attributes.put ("longitude_of_central_meridian", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin", 
        GCTP.unpack_angle (params[5]));
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    case ProjectionConstants.GVNSP:
      attributes.put ("grid_mapping_name", "vertical_perspective");
      attributes.put ("longitude_of_projection_origin", 
        GCTP.unpack_angle (params[4]));
      attributes.put ("latitude_of_projection_origin", 
        GCTP.unpack_angle (params[5]));
      attributes.put ("perspective_point_height", params[2]);
      attributes.put ("false_easting", params[6]);
      attributes.put ("false_northing", params[7]);
      break;

    default:
      throw new IOException ("Unsupported map projection: " + 
        map.getSystemName());

    } // switch

    // Write all parameters
    // --------------------
    for (Map.Entry<String,Object> entry : attributes.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof String)
        ncFile.addVariableAttribute (COORD_REF, entry.getKey(), (String) value);
      else if (value instanceof Array)
        ncFile.addVariableAttribute (COORD_REF, entry.getKey(), (Array) value);
      else if (value instanceof Double)
        ncFile.addVariableAttribute (COORD_REF, entry.getKey(), (Double) value);
    } // for

  } // writeMapProjection

  ////////////////////////////////////////////////////////////

  /** Gets a formatted source string. */
  private String getSource () throws IOException {

    String source;

    // Handle satellite data source
    // ----------------------------
    if (info instanceof SatelliteDataInfo) {
      SatelliteDataInfo satInfo = (SatelliteDataInfo) info;
      String satellite = satInfo.getSatellite();
      String sensor = satInfo.getSensor();
      String[] satValueArray = satellite.split (MetadataServices.SPLIT_STRING);
      String[] senValueArray = sensor.split (MetadataServices.SPLIT_STRING);

      // Deal with satellite/sensor of the same count
      // --------------------------------------------
      if (satValueArray.length == senValueArray.length) {
        source = "";
        for (int i = 0; i < satValueArray.length; i++) {
          source = source + 
            satValueArray[i].replaceAll (" ", "_").replaceAll ("-", "") + "_" +
            senValueArray[i].replaceAll (" ", "_").replaceAll ("-", "") + 
            MetadataServices.SPLIT_STRING;
        } // for
        source = MetadataServices.collapse (source);
        source = source.replaceAll ("\n", " ");
      } // if

      // Deal with one sensor, multiple satellites
      // -----------------------------------------
      else if (senValueArray.length == 1 && satValueArray.length > 1) {
        source = "";
        for (int i = 0; i < senValueArray.length; i++) {
          source = source + 
            satValueArray[i].replaceAll (" ", "_").replaceAll ("-", "") + "_" +
            senValueArray[0].replaceAll (" ", "_").replaceAll ("-", "") + 
            MetadataServices.SPLIT_STRING;
        } // for
        source = MetadataServices.collapse (source);
        source = source.replaceAll ("\n", " ");
      } // else if

      // Unable to create source from a complex set of attribute values
      // --------------------------------------------------------------
      else {
        throw new IOException ("Cannot create data source attribute");
      } // else

    } // if

    // Handle earth data source
    // ------------------------
    else {
      source = MetadataServices.collapse (info.getSource());
      source = source.replaceAll (" ", "_").replaceAll ("-", 
        "").replaceAll ("\n", " ");
    } // else
    
    return (source.toUpperCase());

  } // getSource

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new longitude point to the longitude bounds array.
   *
   * @param lon the new longitude to add.
   */
  private void addToLonBounds (
    double lon
  ) {

    lonBounds[0] = Math.min (lonBounds[0], lon);
    lonBounds[1] = Math.max (lonBounds[1], lon);
    double lon360 = (lon < 0 ? lon + 360 : lon);
    lonBounds360[0] = Math.min (lonBounds360[0], lon360);
    lonBounds360[1] = Math.max (lonBounds360[1], lon360);

  } // addToLonBounds

  ////////////////////////////////////////////////////////////

  /** Gets the northern bound of the earth transform. */
  private double getNorthBound () { return (latBounds[1]); }

  ////////////////////////////////////////////////////////////

  /** Gets the southern bound of the earth transform. */
  private double getSouthBound () { return (latBounds[0]); }

  ////////////////////////////////////////////////////////////

  /** Returns true if the number n is in the range [min..max]. */
  private boolean inRange (
    double n, 
    double min,
    double max
  ) {

    return (n <= max && n >= min);

  } // inRange

  ////////////////////////////////////////////////////////////

  /**
   * Gets the longitude bounds condition.
   *
   * @return
   * BoundCase.POLAR if both 180 and 360 lines were crossed (polar coverage),
   * BoundCase.PACIFIC if the 180 line was crossed (Pacific coverage),
   * BoundCase.ATLANTIC if the 0 line was crossed (Atlantic coverage),
   * BoundCase.NORMAL if no discontinuities were crossed.
   */
  private BoundCase getLonBoundCondition () {

    double lon180Span = lonBounds[1] - lonBounds[0];
    double lon360Span = lonBounds360[1] - lonBounds360[0];

    // Bounds crossed no boundary
    // --------------------------
    if ((inRange (lonBounds[0], 0, 180) && inRange (lonBounds[1], 0, 180)) ||
        (inRange (lonBounds[0], -180, 0) && inRange (lonBounds[1], -180, 0)))
      return (BoundCase.NORMAL);

    // Bounds crossed both 0 and 180 line
    // ----------------------------------
    else if (Math.abs (lon360Span - lon180Span) < LON_EPSILON)
      return (BoundCase.POLAR);

    // Bounds crossed 0 line
    // ---------------------
    else if (lon360Span > lon180Span)
      return (BoundCase.ATLANTIC);

    // Bounds crossed 180 line
    // -----------------------
    else
      return (BoundCase.PACIFIC);

  } // getLonBoundCondition

  ////////////////////////////////////////////////////////////

  /** Gets the eastern bound of the earth transform. */
  private double getEastBound () { 

    switch (getLonBoundCondition()) {
    case POLAR: 
      return (180);
    case PACIFIC: 
      return (lonBounds360[0] > 180 ? lonBounds360[0] - 360 : lonBounds360[0]);
    case ATLANTIC:
    case NORMAL: 
      return (lonBounds[1]);
    default: 
      throw new IllegalStateException();
    } // switch

  } // getEastBound

  ////////////////////////////////////////////////////////////

  /** Gets the western bound of the earth transform. */
  private double getWestBound () { 

    switch (getLonBoundCondition()) {
    case POLAR: 
      return (-180);
    case PACIFIC: 
      return (lonBounds360[1] > 180 ? lonBounds360[1] - 360 : lonBounds360[1]);
    case ATLANTIC:
    case NORMAL: 
      return (lonBounds[0]);
    default: 
      throw new IllegalStateException();
    } // switch

  } // getWestBound

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new latitude point to the latitude bounds array.
   *
   * @param lat the new latitude to add.
   */
  private void addToLatBounds (
    double lat
  ) {

    latBounds[0] = Math.min (latBounds[0], lat);
    latBounds[1] = Math.max (latBounds[1], lat);

  } // addToLatBounds

  ////////////////////////////////////////////////////////////

  /**
   * Writes CF metadata and data variables to the file, using
   * information from the info object.
   */
  private void writeCFInfo () throws IOException, InvalidRangeException {

    // Write main global attributes
    // ----------------------------
    ncFile.addGlobalAttribute ("Conventions", "CF-1.4");
    ncFile.addGlobalAttribute ("source", getSource());
    String origin = MetadataServices.collapse (info.getOrigin());
    ncFile.addGlobalAttribute ("institution", origin);
    TimeZone zone = TimeZone.getDefault();
    String history = 
      "[" + 
      DateFormatter.formatDate (new Date(), DATE_FMT, zone) + " " +
      ToolServices.PACKAGE_SHORT + "-" +
      ToolServices.getVersion() + " " + 
      System.getProperty ("user.name") + "] " +
      ToolServices.getCommandLine();
    ncFile.addGlobalAttribute ("history", history);

    // TODO: How can we add title, references, and comment?


    // Get earth transform
    // -------------------
    /**
     * We need to create different CF structures depending on the
     * type of earth transform.  The main transforms supported
     * are swath and map projection (1D and 2D coordinate axes).
     */
    EarthTransform trans = info.getTransform();
    int[] dims = trans.getDimensions();
    int rows = dims[0];
    int cols = dims[1];
    boolean isSwathProj = (trans instanceof SwathProjection);
    boolean isMapProj = (trans instanceof MapProjection);
    boolean isGeoVector = (trans instanceof GeoVectorProjection);
    boolean isMapped1D = (isGeoVector || (isMapProj && 
      ((MapProjection) trans).getSystem() == ProjectionConstants.GEO));
    boolean isMapped2D = (isMapProj && !isMapped1D);

    // Create coordinate reference variable
    // ------------------------------------
    ncFile.addVariable (COORD_REF, DataType.INT, new Dimension[0]);

    // Create swath structures
    // -----------------------
    if (isSwathProj) {
      ncDims = new Dimension[] {
        ncFile.addDimension (TIME_DIM, 1),
        ncFile.addDimension (LEVEL_DIM, 1), 
        ncFile.addDimension (LINE_DIM, rows),
        ncFile.addDimension (SAMPLE_DIM, cols)
      };
      ncFile.addVariableAttribute (COORD_REF, 
        "grid_mapping_name", "latitude_longitude");
      ncFile.create();
      ncFile.setRedefineMode (true);
    } // if

    // Create 1D map projection structures
    // -----------------------------------
    else if (isMapped1D) {
      ncDims = new Dimension[] {
        ncFile.addDimension (TIME_DIM, 1),
        ncFile.addDimension (LEVEL_DIM, 1), 
        ncFile.addDimension (LAT_DIM, rows),
        ncFile.addDimension (LON_DIM, cols)
      };
      ncFile.addVariableAttribute (COORD_REF, 
        "grid_mapping_name", "latitude_longitude");
      ncFile.create();
      ncFile.setRedefineMode (true);
    } // else if

    // Create 2D map projection structures
    // -----------------------------------
    else if (isMapped2D) {

      // Create dimensions
      // -----------------
      MapProjection map = (MapProjection) trans;
      ncDims = new Dimension[] {
        ncFile.addDimension (TIME_DIM, 1),
        ncFile.addDimension (LEVEL_DIM, 1), 
        ncFile.addDimension (ROW_DIM, rows),
        ncFile.addDimension (COLUMN_DIM, cols)
      };

      // Write coordinate metadata
      // -------------------------
      writeMapProjection (map);

      // Compute map coordinate arrays
      // -----------------------------
      AffineTransform affine = map.getAffine();
      int maxDim = Math.max (rows, cols);
      double[] rowCol = new double[maxDim*2];
      double[] xy = new double[maxDim*2];
      for (int i = 0; i < maxDim; i++) { rowCol[i*2] = i; rowCol[i*2+1] = i; }
      affine.transform (rowCol, 0, xy, 0, maxDim);
      double[] x = new double[cols];
      for (int i = 0; i < cols; i++) x[i] = xy[i*2];
      double[] y = new double[rows];
      for (int i = 0; i < rows; i++) y[i] = xy[i*2 + 1];

      // Write x coordinate array
      // ------------------------
      ncFile.addVariable (PROJ_X_VAR, DataType.DOUBLE, 
        new Dimension[] {ncDims[COLUMN_INDEX]});
      ncFile.addVariableAttribute (PROJ_X_VAR, 
        "standard_name", "projection_x_coordinate");
      ncFile.addVariableAttribute (PROJ_X_VAR, "units", "m");
      int[] start = new int[] {0};
      int[] count = new int[] {cols};
      ncFile.create();
      ncFile.write (PROJ_X_VAR, start, Array.factory (double.class, count, x));
      ncFile.setRedefineMode (true);

      // Write y coordinate array
      // ------------------------
      ncFile.addVariable (PROJ_Y_VAR, DataType.DOUBLE, 
        new Dimension[] {ncDims[ROW_INDEX]});
      ncFile.addVariableAttribute (PROJ_Y_VAR, 
        "standard_name", "projection_y_coordinate");
      ncFile.addVariableAttribute (PROJ_Y_VAR, "units", "m");
      count[0] = rows;
      ncFile.setRedefineMode (false);
      ncFile.write (PROJ_Y_VAR, start, Array.factory (double.class, count, y));
      ncFile.setRedefineMode (true);

    } // else if

    // Throw error for unsupported transform/projection
    // ------------------------------------------------
    else
      throw new IOException ("Unsupported earth transform type");

    // Write ellipsoid parameters
    // --------------------------
    Datum datum = trans.getDatum();
    ncFile.addVariableAttribute (COORD_REF, 
      "semi_major_axis", datum.getAxis());
    ncFile.addVariableAttribute (COORD_REF, 
      "inverse_flattening", 1.0/datum.getFlat());
    ncFile.addVariableAttribute (COORD_REF, 
      "longitude_of_prime_meridian", 0.0);

    // Write lat/lon 1D data
    // ---------------------
    if (isMapped1D) {

      Dimension[] latDims = new Dimension[] {ncDims[ROW_INDEX]}; 
      ncFile.addVariable (LAT_VAR, DataType.DOUBLE, latDims);
      ncFile.addVariableAttribute (LAT_VAR, "standard_name", "latitude");
      ncFile.addVariableAttribute (LAT_VAR, "units", "degrees_north");

      Dimension[] lonDims = new Dimension[] {ncDims[COLUMN_INDEX]}; 
      ncFile.addVariable (LON_VAR, DataType.DOUBLE, lonDims);
      ncFile.addVariableAttribute (LON_VAR, "standard_name", "longitude");
      ncFile.addVariableAttribute (LON_VAR, "units", "degrees_east");

      int[] start = new int[] {0};
      int[] count = new int[] {rows};

      double[] lat = new double[rows];
      Array latArray = Array.factory (double.class, count, lat);
      DataLocation dataLoc = new DataLocation (0, 0);
      EarthLocation earthLoc = new EarthLocation (0, 0); 
      for (int row = 0; row < rows; row++) {
        dataLoc.set (Grid.ROWS, row);
        trans.transform (dataLoc, earthLoc);
        lat[row] = earthLoc.lat;
        addToLatBounds (lat[row]);
      } // for
      ncFile.setRedefineMode (false);
      ncFile.write (LAT_VAR, start, latArray); 

      double[] lon = new double[cols];
      Array lonArray = Array.factory (double.class, count, lon);
      dataLoc = new DataLocation (0, 0);
      for (int col = 0; col < cols; col++) {
        dataLoc.set (Grid.COLS, col);
        trans.transform (dataLoc, earthLoc);
        lon[col] = earthLoc.lon;
        addToLonBounds (lon[col]);
      } // for
      ncFile.setRedefineMode (false);
      ncFile.write (LON_VAR, start, lonArray); 
      ncFile.setRedefineMode (true);

    } // if
    
    // Write lat/lon 2D data
    // ---------------------
    else {

      Dimension[] coordDims = new Dimension[] {ncDims[ROW_INDEX], 
        ncDims[COLUMN_INDEX]};

      ncFile.addVariable (LAT_VAR, DataType.DOUBLE, coordDims);
      ncFile.addVariableAttribute (LAT_VAR, "standard_name", "latitude");
      ncFile.addVariableAttribute (LAT_VAR, "units", "degrees_north");

      ncFile.addVariable (LON_VAR, DataType.DOUBLE, coordDims);
      ncFile.addVariableAttribute (LON_VAR, "standard_name", "longitude");
      ncFile.addVariableAttribute (LON_VAR, "units", "degrees_east");

      int[] start = new int[] {0, 0};
      int[] count = new int[] {1, cols};
      double[] lat = new double[cols];
      double[] lon = new double[cols];
      Array latArray = Array.factory (double.class, count, lat);
      Array lonArray = Array.factory (double.class, count, lon);
      ncFile.setRedefineMode (false);

      DataLocation dataLoc = new DataLocation (0, 0);
      EarthLocation earthLoc = new EarthLocation (0, 0); 
      for (int row = 0; row < rows; row++) {
        dataLoc.set (Grid.ROWS, row);
        for (int col = 0; col < cols; col++) {
          dataLoc.set (Grid.COLS, col);
          trans.transform (dataLoc, earthLoc);
          lat[col] = earthLoc.lat;
          lon[col] = earthLoc.lon;
          addToLatBounds (lat[col]);
          addToLonBounds (lon[col]);
        } // for
        start[0] = row;
        ncFile.write (LAT_VAR, start, latArray); 
        ncFile.write (LON_VAR, start, lonArray); 
      } // for
      ncFile.setRedefineMode (true);

    } // else

    // Write time data
    // ---------------
    ncFile.addVariable (TIME_VAR, DataType.DOUBLE, 
      new Dimension [] {ncDims[TIME_INDEX]});
    ncFile.addVariableAttribute (TIME_VAR, "standard_name", "time");
    ncFile.addVariableAttribute (TIME_VAR, "units", 
      "seconds since 1970-01-01 00:00:00 UTC");
    ncFile.setRedefineMode (false);
    Array timeArray = Array.factory (DataType.DOUBLE, new int[] {1});
    if (info.isInstantaneous()) {
      Date startDate = info.getStartDate();
      timeArray.setDouble (0, startDate.getTime()/1000.0);
      int[] start = new int[] {0};
      ncFile.write (TIME_VAR, start, timeArray);
    } // if
    else {

      // TODO: Add an option here for climatology?

      Date startDate = info.getStartDate();
      double startTime = startDate.getTime()/1000.0;
      Date endDate = info.getEndDate();
      double endTime = endDate.getTime()/1000.0;
      double time = (startTime + endTime)/2;
      timeArray.setDouble (0, time);
      int[] start = new int[] {0};
      ncFile.write (TIME_VAR, start, timeArray);
      ncFile.setRedefineMode (true);
      ncFile.addVariableAttribute (TIME_VAR, "bounds", TIME_BOUNDS_VAR);
      Dimension nvalsDim = ncFile.addDimension (NVALS_DIM, 2);
      ncFile.addVariable (TIME_BOUNDS_VAR, DataType.DOUBLE, 
        new Dimension[] {ncDims[TIME_INDEX], nvalsDim});
      ncFile.setRedefineMode (false);
      Array timeBoundsArray = Array.factory (DataType.DOUBLE, new int[] {1, 2});
      timeBoundsArray.setDouble (0, startTime);
      timeBoundsArray.setDouble (1, endTime);
      start = new int[] {0, 0};
      ncFile.write (TIME_BOUNDS_VAR, start, timeBoundsArray);
    } // else
    ncFile.setRedefineMode (true);

    // Write level data
    // ----------------
    ncFile.addVariable (LEVEL_VAR, DataType.DOUBLE, 
      new Dimension [] {ncDims[LEVEL_INDEX]});
    ncFile.addVariableAttribute (LEVEL_VAR, "standard_name", "height");
    ncFile.addVariableAttribute (LEVEL_VAR, "units", "m");
    ncFile.addVariableAttribute (LEVEL_VAR, "positive", "up");
    ncFile.setRedefineMode (false);
    Array levelArray = Array.factory (DataType.DOUBLE, new int[] {1});
    levelArray.setDouble (0, 0);
    int[] start = new int[] {0};
    ncFile.write (LEVEL_VAR, start, levelArray);
    ncFile.setRedefineMode (true);

    // Flush all data
    // --------------
    ncFile.flush();

  } // writeCFInfo

  ////////////////////////////////////////////////////////////

  /**
   * Writes DCS global metadata to the file, using information
   * from the info object.
   */
  private void writeDCSInfo () throws IOException, InvalidRangeException {

    // Check conditions for writing DCS
    // --------------------------------
    if (!(info instanceof SatelliteDataInfo))
      throw new IOException ("No satellite sensor metadata found");
    SatelliteDataInfo satInfo = (SatelliteDataInfo) info;
    if (!(info.getTransform() instanceof MapProjection))
      throw new IOException ("No map projection metadata found");
    MapProjection map = (MapProjection) info.getTransform();

    // Write origin
    // ------------
    String origin = MetadataServices.collapse (info.getOrigin());
    ncFile.addGlobalAttribute ("dcs:createInstitution", origin);

    // Write creation and acquisition dates
    // ------------------------------------
    ncFile.addGlobalAttribute ("dcs:createDateTime", 
      DateFormatter.formatDate (new Date(), ISO_DATE_FMT));
    ncFile.addGlobalAttribute ("dcs:acquisitionStartDateTime",
      DateFormatter.formatDate (info.getStartDate(), ISO_DATE_FMT));
    ncFile.addGlobalAttribute ("dcs:acquisitionEndDateTime",
      DateFormatter.formatDate (info.getEndDate(), ISO_DATE_FMT));

    // Write sensor/satellite names
    // ----------------------------
    String sensor = satInfo.getSensor();
    sensor = MetadataServices.collapse (sensor).replaceAll ("\n", ",");
    ncFile.addGlobalAttribute ("dcs:sensor", sensor);
    String satellite = satInfo.getSatellite();
    satellite = MetadataServices.collapse (satellite).replaceAll ("\n", ",");
    ncFile.addGlobalAttribute ("dcs:sensorPlatform", satellite);

    // Write earth location info
    // -------------------------
    ncFile.addGlobalAttribute ("dcs:mapProjection", map.getSystemName());
    ncFile.addGlobalAttribute ("dcs:geodeticDatum", 
      map.getDatum().getDatumName());
    ncFile.addGlobalAttribute ("dcs:northernLatitude", getNorthBound());
    ncFile.addGlobalAttribute ("dcs:southernLatitude", getSouthBound());
    ncFile.addGlobalAttribute ("dcs:easternLongitude", getEastBound());
    ncFile.addGlobalAttribute ("dcs:westernLongitude", getWestBound());

    // Write observed property
    // -----------------------
    ncFile.addGlobalAttribute ("dcs:observedProperty", "Unknown");
    ncFile.addGlobalAttribute ("dcs:observedPropertyAlgorithm", "Unknown");   
    ncFile.addGlobalAttribute ("dcs:processingLevel", "Level 3");

    ncFile.setRedefineMode (false);
    ncFile.setRedefineMode (true);

  } // writeDCSInfo

  ////////////////////////////////////////////////////////////

  /**
   * Writes CW global metadata to the file, using information
   * from the info object.
   */
  private void writeCWInfo () throws IOException, InvalidRangeException {

    // Copy string attributes
    // ----------------------
    String[] attributeArray = new String[] {
      "orbit_type",
      "station_code",
      "station_name",
      "pass_type",
      "region_code",
      "region_name"
    };
    for (String attName : attributeArray) {
      String attValue = (String) info.getMetadataMap().get (attName);
      if (attValue != null)
        ncFile.addGlobalAttribute ("cw:" + attName, attValue);
    } // for

    // Copy polygon points
    // -------------------
    double[] latPoints = 
      (double[]) info.getMetadataMap().get ("polygon_latitude"); 
    double[] lonPoints = 
      (double[]) info.getMetadataMap().get ("polygon_longitude"); 
    if (latPoints != null && lonPoints != null) {
      ncFile.addGlobalAttribute ("cw:polygon_latitude", 
        Array.factory (double.class, new int[] {latPoints.length}, latPoints));
      ncFile.addGlobalAttribute ("cw:polygon_longitude", 
        Array.factory (double.class, new int[] {lonPoints.length}, lonPoints));
    } // if

    ncFile.setRedefineMode (false);
    ncFile.setRedefineMode (true);

  } // writeCWInfo

  ////////////////////////////////////////////////////////////

  /**
   * Writes a data variable to the file.
   *
   * @param var the variable to write.
   *
   * @throws IOException if creating the variable or writing the
   * data failed.
   * @throws InvalidRangeException if writing the variable data
   * failed.
   */
  private void writeVariable (
    DataVariable var
  ) throws IOException, InvalidRangeException { 

    // Check rank
    // ----------
    if (!(var instanceof Grid))
      throw new IOException ("Unsupported variable type:" + var.getClass());

    // Create variable
    // ---------------
    Class dataClass = var.getDataClass();
    boolean isUnsigned = var.getUnsigned();
    DataType dataType = DataType.getType (dataClass);
    if (dataType == null)
      throw new IOException ("Unsupported data type: " + dataClass);
    if (isUnsigned) {
      if (dataType == DataType.BYTE)
        ; // do nothing
      else if (dataType == DataType.SHORT)
        dataType = DataType.INT;
      else if (dataType == DataType.INT)
        dataType = DataType.LONG;
      else 
        throw new IOException ("Unsupported unsigned data type: " + dataClass);
    } // if
    String varName = var.getName();
    ncFile.addVariable (varName, dataType, ncDims); 

    // Set packing info
    // ----------------
    double[] scaling = var.getScaling();
    if (scaling != null && 
      (Math.abs (scaling[0] - 1) < SCALE_EPSILON && 
      Math.abs (scaling[1]) < SCALE_EPSILON))
      scaling = null;
    if (scaling != null) {
      Number scaleFactor = scaling[0];
      Number addOffset = -scaling[0]*scaling[1];
      if (dataType == DataType.FLOAT) {
        scaleFactor = (Float) scaleFactor;
        addOffset = (Float) addOffset;
      } // if
      ncFile.addVariableAttribute (varName, "scale_factor", scaleFactor);
      ncFile.addVariableAttribute (varName, "add_offset", addOffset);
    } // if

    // Set missing info
    // ----------------
    Object missing = var.getMissing();
    if (missing != null) {
      if (isUnsigned) {
        if (dataType == DataType.SHORT)
          missing = DataType.unsignedShortToInt ((Short) missing);
        else if (dataType == DataType.INT)
          missing = DataType.unsignedIntToLong ((Integer) missing);
      } // if
      ncFile.addVariableAttribute (varName, "missing", (Number) missing);
    } // if
    if (dataType == DataType.BYTE) {
      ncFile.addVariableAttribute (varName, "valid_range", 
        Array.factory (DataType.INT, new int[] {2}, new int[] {0, 255}));
    } // if

    // Set standard and long name
    // --------------------------
    String standard = (String) var.getMetadataMap().get ("standard_name");
    if (standard != null) 
      ncFile.addVariableAttribute (varName, "standard_name", standard);
    String longName = var.getLongName();
    if (longName != null && !longName.equals ("") && !longName.equals (varName))
      ncFile.addVariableAttribute (varName, "long_name", longName);

    // Set units
    // ---------
    String units = var.getUnits();
    if (units != null && !units.equals (""))
      ncFile.addVariableAttribute (varName, "units", units);

    // Set coordinate info
    // -------------------
    ncFile.addVariableAttribute (varName, "coordinates", "lat lon");

    // TODO: What about when cell_methods should indicate a climatology?

    String rasterType = (String) info.getMetadataMap().get ("raster_type");
    if (rasterType == null || !rasterType.equals ("RasterPixelIsPoint"))
      ncFile.addVariableAttribute (varName, "cell_methods", "area: mean");
    ncFile.addVariableAttribute (varName, "grid_mapping", COORD_REF);

    // Set ancillary variables
    // -----------------------
    String ancillary = getMultiValuedAtt (new String[] {
      "quality_mask", 
      "direction_variable"
      }, var.getMetadataMap());
    if (ancillary != null)
      ncFile.addVariableAttribute (varName, "ancillary_variables", ancillary);

    // TODO: We currently have no way to associate variables that
    // don't have this extra metadata.


    // Set source
    // ----------
    String source = getMultiValuedAtt (new String[] {
      "sst_equation_day",
      "sst_equation_night", 
      "processing_algorithm",
      "atmospheric_correction"
      }, var.getMetadataMap());
    if (source != null)
      ncFile.addVariableAttribute (varName, "source", source);

    // TODO: This is a pretty rough cut for getting the source
    // attribute right!  Also, what about references?

    // Write navigation affine
    // -----------------------
    if (writeCw) {
      AffineTransform nav = ((Grid) var).getNavigation();
      if (!nav.isIdentity()) {
        double[] matrix = new double[6];
        nav.getMatrix (matrix); 
        ncFile.addVariableAttribute (varName, "cw:nav_affine", 
          Array.factory (double.class, new int[] {6}, matrix));
      } // if
    } // if

    // Write data
    // ----------
    int[] dims = var.getDimensions();
    int[] start = new int[] {0, 0, 0, 0};
    int[] count = new int[] {1, 1, 1, dims[Grid.COLS]};
    int[] dataStart = new int[] {0, 0};
    int[] dataCount = new int[] {1, dims[Grid.COLS]};
    ncFile.setRedefineMode (false);
    try {
      for (int row = 0; row < dims[Grid.ROWS]; row++) {
        dataStart[Grid.ROWS] = row;
        start[2] = row;
        Object data = ((Grid) var).getData (dataStart, dataCount);
        if (isUnsigned) data = convertUnsignedData (data, dataType);
        Array dataArray = Array.factory (dataType, count, data);
        ncFile.write (varName, start, dataArray);
      } // for
    } // try
    finally {
      ncFile.setRedefineMode (true);
    } // finally

    // Flush to the file
    // -----------------
    ncFile.flush();


  } // writeVariable

  ////////////////////////////////////////////////////////////

  /** 
   * Converts unsigned data to the next largest signed data type.
   *
   * @param data the data array to convert.
   * @param dataType the NetCDF data type to convert to.
   *
   * @return the new converted data array.
   * @throws IOException if the data type to convert ot is not
   * supported.
   */
  private static Object convertUnsignedData (
    Object data,
    DataType dataType
  ) throws IOException {

    Object newData;

    // Do nothing to byte data
    // -----------------------
    if (dataType == DataType.BYTE) {
      newData = data;
    } // if

    // Convert ushort to int
    // ---------------------
    else if (dataType == DataType.INT) {
      short[] shortData = (short[]) data;
      int[] intData = new int[shortData.length];
      for (int i = 0; i < shortData.length; i++)
        intData[i] = DataType.unsignedShortToInt (shortData[i]);
      newData = intData;
    } // else if

    // Convert uint to long
    // --------------------
    else if (dataType == DataType.LONG) {
      int[] intData = (int[]) data;
      long[] longData = new long[intData.length];
      for (int i = 0; i < intData.length; i++)
        longData[i] = DataType.unsignedIntToLong (intData[i]);
      newData = longData;
    } // else if

    // Unsupported destination type
    // ----------------------------
    else 
      throw new IOException ("Unsupported destination data type: " + dataType);

    return (newData);

  } // convertUnsignedData

  ////////////////////////////////////////////////////////////

  /**
   * Gets a multiple-value CF attribute string.
   *
   * @param candidateAray the array of attribute names to use as
   * candidates for source strings in the value map.
   * @param valueMap the map of attribute names to values.  The
   * map will be searched for the list of candidates.
   *
   * @return the composite CF style attribute resulting from the
   * candidates, or null if no candidates were found.
   */
  private static String getMultiValuedAtt (
    String[] candidateArray,
    Map valueMap
  ) {
                                   
    // Build value list
    // ----------------
    List<String> valueList = new ArrayList<String>();
    for (String search : candidateArray) {
      String value = (String) valueMap.get (search);
      if (value != null) valueList.add (value);
    } // for
    
    // Build attribute value
    // ---------------------
    if (valueList.size() != 0) {
      String att = "";
      for (String value : valueList) {
        value = value.replaceAll ("[ /]", "_");
        value = value.replaceAll ("[^\\w]", "");
        value = value.toLowerCase();
        att = att + " " + value;
      } // for
      att = att.trim();
      return (att);
    } // if
    else {
      return (null);
    } // else

  } // getMultiValuedAtt

  ////////////////////////////////////////////////////////////
  
  public void flush () throws IOException {

    // Check for canceled
    // ------------------
    if (isCanceled) return;

    // Initialize progress counters
    // ----------------------------
    synchronized (this) {
      writeProgress = 0;
      writeVariables = 0;
    } // synchronized

    // Loop over each variable
    // -----------------------
    while (variables.size() != 0) {

      // Write variable
      // --------------
      DataVariable var = (DataVariable) variables.remove (0);
      writeVariableName = var.getName();
      try { writeVariable (var); }
      catch (InvalidRangeException e) { throw new IOException (e.toString()); }

      // Update progress
      // ---------------
      synchronized (this) {
        writeProgress = 0;
        writeVariables++;
      } // synchronized

      // Check for canceled
      // ------------------
      if (isCanceled) return;

    } // while

  } // flush

  ////////////////////////////////////////////////////////////

  public void close () throws IOException {

    // Flush and close
    // ---------------
    if (closed) return;
    flush();
    ncFile.close();
    closed = true;

  } // close

  ////////////////////////////////////////////////////////////

} // CFNCWriter class

////////////////////////////////////////////////////////////////////////
