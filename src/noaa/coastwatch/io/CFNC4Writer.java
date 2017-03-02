////////////////////////////////////////////////////////////////////////
/*

     File: CFNC4Writer.java
   Author: Peter Hollemans
     Date: 2015/04/08

  CoastWatch Software Library and Utilities
  Copyright (c) 2015 National Oceanic and Atmospheric Administration
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
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import noaa.coastwatch.io.EarthDataWriter;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.GeoVectorProjection;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.trans.SpheroidConstants;
import noaa.coastwatch.util.trans.SwathProjection;

import edu.ucar.ral.nujan.netcdf.NhDimension;
import edu.ucar.ral.nujan.netcdf.NhException;
import edu.ucar.ral.nujan.netcdf.NhFileWriter;
import edu.ucar.ral.nujan.netcdf.NhGroup;
import edu.ucar.ral.nujan.netcdf.NhVariable;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * <p>A CF NetCDF 4 writer creates NetCDF 4 format files with CF
 * metadata using the Nujan NetCDF 4 writing library.</p>
 *
 * <p>Some implementation notes:</p>
 *
 * <ol>
 *
 * <li>The Nujan NetCDF 4 writing library does not support all unsigned data
 * types.  If unsigned 16-bit or 32-bit data is present (as specified by
 * the {@link DataVariable#getUnsigned} flag), it is written as
 * signed data of the next largest NetCDF data type.</li>
 *
 * <li>As much as possible, the CF metadata written to the NetCDF
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
 * it.</li>
 *
 * </ol>
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class CFNC4Writer
  extends EarthDataWriter {

  // Constants
  // ---------
  
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

  /** The square tile size. */
  private static final int TILE_SIZE = 512;
  
  /** The compression level used for tile data (0-9). */
  private static final int COMPRESSION_LEVEL = 6;
  
  // Variables
  // ---------
  
  /** The NetCDF writeable file. */
  private NhFileWriter ncFileWriter;

  /** The write queue with data to write. */
  private Queue<WriteQueueEntry> writeQueue;
  
  /** The list of tile positions to write for the main variables. */
  private List<TilePosition> tilePositions;
  
  /** The chunk lengths to use for the main variables. */
  private int[] chunkLengths;
  
  /** Flag to signify that the file is closed. */
  private boolean closed;

  /** The dimensions for the main variables. */
  private NhDimension[] ncDims;

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
   * Creates a new NetCDF 4 file from the specified info and file
   * name.  The required CF data variables and attributes are
   * written to the file.  No extra DCS or CW attributes are written.
   *
   * @param info the earth data info object.
   * @param file the new NetCDF file name.
   *
   * @throws IOException if an error occurred creating the file
   * or writing the initial data.
   *
   * @since 3.3.1
   */
  public CFNC4Writer (
    EarthDataInfo info,
    String file
  ) throws IOException {

    this (info, file, false, false);

  } // CFNC4Writer constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new NetCDF 4 file from the specified info and file
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
  public CFNC4Writer (
    EarthDataInfo info,
    String file,
    boolean writeDcs,
    boolean writeCw
  ) throws IOException {

    super (file);

    // Create new file
    // ---------------
    closed = true;
    try { ncFileWriter = new NhFileWriter (file, NhFileWriter.OPT_OVERWRITE); }
    catch (NhException e) { throw new IOException (e.toString()); }
    closed = false;

    // Setup
    // -----
    this.info = info;
    this.writeCw = writeCw;
    this.writeQueue = new LinkedList<WriteQueueEntry>();

    // Write initial data structures
    // -----------------------------
    try { 
      writeCFInfo(); 
      if (writeDcs) writeDCSInfo();
      if (writeCw) writeCWInfo();
    } // try
    catch (NhException e) { throw new IOException (e.toString()); }

  } // CFNC4Writer constructor

  ////////////////////////////////////////////////////////////

  /** Writes CF metadata for the specified map projection. */
  private void writeMapProjection (MapProjection map) throws IOException, NhException {

    // Set projection parameters
    // -------------------------
    double[] params = map.getParameters();
    Map<String,Object> attributes = new LinkedHashMap<String,Object>();
    switch (map.getSystem()) {

    case ProjectionConstants.ALBERS:
      attributes.put ("grid_mapping_name", "albers_conical_equal_area");
      double[] albersStdPara = new double[] {
        GCTP.unpack_angle (params[2]),
        GCTP.unpack_angle (params[3])
      };
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
      double[] lamCCStdPara = new double[] {
        GCTP.unpack_angle (params[2]),
        GCTP.unpack_angle (params[3])
      };
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
    NhVariable coordVar = ncFileWriter.getRootGroup().findVariable (COORD_REF);
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      String attName = entry.getKey();
      Object attValue = entry.getValue();
      int attType;
      if (attValue instanceof String)
        attType = NhVariable.TP_STRING_VAR;
      else if (attValue.getClass().isArray())
        attType = NhVariable.TP_DOUBLE;
      else if (attValue instanceof Double)
        attType = NhVariable.TP_DOUBLE;
      else
        throw new IllegalStateException ("Illegal attribute type: " + attValue.getClass().getName());
      coordVar.addAttribute (attName, attType, attValue);
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
  private static boolean inRange (
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
   * The <code>WriteQueueEntry</code> class holds information about a pending
   * entry in the data write queue.
   */
  private interface WriteQueueEntry {
  
    /**
     * Writes the data held by this entry to the file.
     *
     * @throws IOException if some error occurred in the write operation.
     */
    public void write () throws IOException;

  } // WriteQueueEntry interface

  ////////////////////////////////////////////////////////////

  /**
   * The <code>DataEntry</code> class holds specific data to be written to
   * the file.
   */
  private class DataEntry implements WriteQueueEntry {

    // Variables
    // ---------
    
    /** The variable name to write. */
    private String varName;
    
    /** The starting index for the write. */
    private int[] start;
    
    /** The data to write. */
    private Object data;
    
    ////////////////////////////////////////////////////////
    
    /**
     * Create an entry with the specified properties.
     *
     * @param varName the variable name to write data to for this entry.
     * @param the starting index, same dimensions as the variable.
     * @param the data to write, must be compatible with NetCDF data types
     * and match the variable named.
     */
    public DataEntry (
      String varName,
      int[] start,
      Object data
    ) {

      this.varName = varName;
      this.start = (start == null ? null : (int[]) start.clone());
      this.data = data;

    } // DataEntry

    ////////////////////////////////////////////////////////

    @Override
    public void write () throws IOException {
    
      NhVariable ncVar = ncFileWriter.getRootGroup().findVariable (varName);
      try { ncVar.writeData (start, data, true); }
      catch (NhException e) { throw new IOException (e.toString()); }

    } // write

    ////////////////////////////////////////////////////////

  } // DataEntry class

  ////////////////////////////////////////////////////////////

  /**
   * Gets latitude and longitude data values from an {@link EarthTransform}
   * object for a specified set of data coordinate bounds.
   *
   * @param trans the transform to use for extracting latitude/longitude data.
   * @param start the starting data coordinates as [row, column].
   * @param count the number of data values in each direction to copy data as
   * [rows, columns].
   * @param latData the output latitude data array, or null to ignore latitude
   * data.
   * @param lonData the output longitude data array, or null to ignore longitude
   * data.
   */
  private void getTransformData (
    EarthTransform trans,
    int[] start,
    int[] count,
    double[] latData,
    double[] lonData
  ) {

    // Initialize location values
    // --------------------------
    DataLocation dataLoc = new DataLocation (0, 0);
    EarthLocation earthLoc = new EarthLocation (0, 0);

    // Loop over each row and column
    // -----------------------------
    for (int row = 0; row < count[Grid.ROWS]; row++) {
      dataLoc.set (Grid.ROWS, row + start[Grid.ROWS]);
      for (int col = 0; col < count[Grid.COLS]; col++) {
        dataLoc.set (Grid.COLS, col + start[Grid.COLS]);

        // Compute geographic location
        // ---------------------------
        trans.transform (dataLoc, earthLoc);
        int index = row*count[Grid.COLS] + col;
        if (latData != null) latData[index] = earthLoc.lat;
        if (lonData != null) lonData[index] = earthLoc.lon;

      } // for
    } // for

  } // getTransformData
  
  ////////////////////////////////////////////////////////////

  /**
   * Writes CF metadata and data variables to the file, using
   * information from the info object.
   */
  private void writeCFInfo () throws IOException, NhException {

    // Write main global attributes
    // ----------------------------
    NhGroup root = ncFileWriter.getRootGroup();
    root.addAttribute ("Conventions", NhVariable.TP_STRING_VAR, "CF-1.4");
    root.addAttribute ("source", NhVariable.TP_STRING_VAR, getSource().trim());
    String origin = MetadataServices.collapse (info.getOrigin());
    root.addAttribute ("institution", NhVariable.TP_STRING_VAR, origin);
    TimeZone zone = TimeZone.getDefault();


    // TODO: Should we prefix any current history attribute data?
    

    String history =
      "[" + 
      DateFormatter.formatDate (new Date(), DATE_FMT, zone) + " " +
      ToolServices.PACKAGE_SHORT + "-" +
      ToolServices.getVersion() + " " + 
      System.getProperty ("user.name") + "] " +
      ToolServices.getCommandLine();
    root.addAttribute ("history", NhVariable.TP_STRING_VAR, history);


    // TODO: How can we add title, references, and comment?


    // Get earth transform
    // -------------------
    /**
     * We need to create different CF structures depending on the
     * type of earth transform.  The main transforms supported
     * are swath and map projection (1D and 2D coordinate axes).
     */
    final EarthTransform trans = info.getTransform();
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
    NhVariable coordVar = root.addVariable (
      COORD_REF,
      NhVariable.TP_INT,
      new NhDimension[0],
      null,
      null,
      0
    );

    // Create swath structures
    // -----------------------
    if (isSwathProj) {
      ncDims = new NhDimension[] {
        root.addDimension (TIME_DIM, 1),
        root.addDimension (LEVEL_DIM, 1),
        root.addDimension (LINE_DIM, rows),
        root.addDimension (SAMPLE_DIM, cols)
      };
      coordVar.addAttribute ("grid_mapping_name", NhVariable.TP_STRING_VAR, "latitude_longitude");
    } // if

    // Create 1D map projection structures
    // -----------------------------------
    else if (isMapped1D) {
      ncDims = new NhDimension[] {
        root.addDimension (TIME_DIM, 1),
        root.addDimension (LEVEL_DIM, 1),
        root.addDimension (LAT_DIM, rows),
        root.addDimension (LON_DIM, cols)
      };
      coordVar.addAttribute ("grid_mapping_name", NhVariable.TP_STRING_VAR, "latitude_longitude");
    } // else if

    // Create 2D map projection structures
    // -----------------------------------
    else if (isMapped2D) {

      // Create dimensions
      // -----------------
      MapProjection map = (MapProjection) trans;
      ncDims = new NhDimension[] {
        root.addDimension (TIME_DIM, 1),
        root.addDimension (LEVEL_DIM, 1),
        root.addDimension (ROW_DIM, rows),
        root.addDimension (COLUMN_DIM, cols)
      };

      // Create coordinate metadata
      // --------------------------
      writeMapProjection (map);

      // Compute map coordinate arrays
      // -----------------------------
      AffineTransform affine = map.getAffine();
      int maxDim = Math.max (rows, cols);
      double[] rowCol = new double[maxDim*2];
      double[] xy = new double[maxDim*2];
      for (int i = 0; i < maxDim; i++) { rowCol[i*2] = i; rowCol[i*2+1] = i; }
      affine.transform (rowCol, 0, xy, 0, maxDim);
      double[] projXData = new double[cols];
      for (int i = 0; i < cols; i++) projXData[i] = xy[i*2];
      double[] projYData = new double[rows];
      for (int i = 0; i < rows; i++) projYData[i] = xy[i*2 + 1];

      // Create x coordinate array
      // -------------------------
      NhVariable projXVar = root.addVariable (
        PROJ_X_VAR,
        NhVariable.TP_DOUBLE,
        new NhDimension[] {ncDims[COLUMN_INDEX]},
        null,
        null,
        0
      );
      projXVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, "projection_x_coordinate");
      projXVar.addAttribute ("units", NhVariable.TP_STRING_VAR, "m");
      writeQueue.add (new DataEntry (PROJ_X_VAR, null, projXData));

      // Create y coordinate array
      // -------------------------
      NhVariable projYVar = root.addVariable (
        PROJ_Y_VAR,
        NhVariable.TP_DOUBLE,
        new NhDimension[] {ncDims[ROW_INDEX]},
        null,
        null,
        0
      );
      projYVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, "projection_y_coordinate");
      projYVar.addAttribute ("units", NhVariable.TP_STRING_VAR, "m");
      writeQueue.add (new DataEntry (PROJ_Y_VAR, null, projYData));

    } // else if

    // Throw error for unsupported transform/projection
    // ------------------------------------------------
    else
      throw new IOException ("Unsupported earth transform type");

    // Create tile position list for main variables
    // --------------------------------------------
    TilingScheme scheme = new TilingScheme (dims, new int[] {TILE_SIZE, TILE_SIZE});
    tilePositions = new ArrayList<TilePosition>();
    int[] tileCounts = scheme.getTileCounts();
    for (int tileRow = 0; tileRow < tileCounts[TilingScheme.ROWS]; tileRow++)
      for (int tileCol = 0; tileCol < tileCounts[TilingScheme.COLS]; tileCol++)
        tilePositions.add (scheme.new TilePosition (tileRow, tileCol));

    // Set chunk lengths for main variables
    // ------------------------------------
    chunkLengths = new int[4];
    chunkLengths[TIME_INDEX] = 1;
    chunkLengths[LEVEL_INDEX] = 1;
    chunkLengths[ROW_INDEX] = (rows > TILE_SIZE ? TILE_SIZE : rows);
    chunkLengths[COLUMN_INDEX] = (cols > TILE_SIZE ? TILE_SIZE : cols);

    // Create ellipsoid parameters
    // ---------------------------
    Datum datum = trans.getDatum();
    coordVar.addAttribute ("semi_major_axis", NhVariable.TP_DOUBLE, datum.getAxis());
    coordVar.addAttribute ("inverse_flattening", NhVariable.TP_DOUBLE, 1.0/datum.getFlat());
    coordVar.addAttribute ("longitude_of_prime_meridian", NhVariable.TP_DOUBLE, 0.0);

    // Create lat/lon 1D data vectors
    // ------------------------------
    if (isMapped1D) {
    
      // Create latitude vector
      // ----------------------
      NhVariable latVar = root.addVariable (
        LAT_VAR,
        NhVariable.TP_DOUBLE,
        new NhDimension[] {ncDims[ROW_INDEX]},
        null,
        null,
        0
      );
      latVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, "latitude");
      latVar.addAttribute ("units", NhVariable.TP_STRING_VAR, "degrees_north");
      double[] latData = new double[rows];
      getTransformData (trans, new int[] {0, 0}, new int[] {rows, 1}, latData, null);
      for (int i = 0; i < rows; i++) addToLatBounds (latData[i]);
      writeQueue.add (new DataEntry (LAT_VAR, null, latData));

      // Create longitude vector
      // -----------------------
      NhVariable lonVar = root.addVariable (
        LON_VAR,
        NhVariable.TP_DOUBLE,
        new NhDimension[] {ncDims[COLUMN_INDEX]},
        null,
        null,
        0
      );
      lonVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, "longitude");
      lonVar.addAttribute ("units", NhVariable.TP_STRING_VAR, "degrees_east");
      double[] lonData = new double[cols];
      getTransformData (trans, new int[] {0, 0}, new int[] {1, cols}, null, lonData);
      for (int j = 0; j < cols; j++) addToLonBounds (lonData[j]);
      writeQueue.add (new DataEntry (LON_VAR, null, lonData));

    } // if
    
    // Create lat/lon 2D data arrays
    // -----------------------------
    else {

      // TODO: What if the original data for the swath came from something like
      // MODIS or VIIRS where the lines were scanned in sets?  If we write out
      // the data as smooth like the swath projection creates, then it's not
      // true to the original data.
      
      // Create latitude array
      // ---------------------
      final NhVariable latVar = root.addVariable (
        LAT_VAR,
        NhVariable.TP_DOUBLE,
        new NhDimension[] {ncDims[ROW_INDEX], ncDims[COLUMN_INDEX]},
        new int[] {chunkLengths[ROW_INDEX], chunkLengths[COLUMN_INDEX]},
        null,
        COMPRESSION_LEVEL
      );
      latVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, "latitude");
      latVar.addAttribute ("units", NhVariable.TP_STRING_VAR, "degrees_north");

      // Create longitude array
      // ----------------------
      final NhVariable lonVar = root.addVariable (
        LON_VAR,
        NhVariable.TP_DOUBLE,
        new NhDimension[] {ncDims[ROW_INDEX], ncDims[COLUMN_INDEX]},
        new int[] {chunkLengths[ROW_INDEX], chunkLengths[COLUMN_INDEX]},
        null,
        COMPRESSION_LEVEL
      );
      lonVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, "longitude");
      lonVar.addAttribute ("units", NhVariable.TP_STRING_VAR, "degrees_east");

      // Write lat/lon tile data
      // -----------------------
      writeQueue.add (new WriteQueueEntry () {
        public void write () throws IOException {

          // Loop over each tile position
          // ----------------------------
          for (TilePosition pos : tilePositions) {

            // Compute data for tile
            // ---------------------
            int[] tileStart = pos.getStart();
            int[] tileDims = pos.getDimensions();
            int values = tileDims[TilingScheme.ROWS]*tileDims[TilingScheme.COLS];
            double[] latData = new double[values];
            double[] lonData = new double[values];
            getTransformData (trans, tileStart, tileDims, latData, lonData);

            // Write data for tile
            // -------------------
            try {
              latVar.writeData (tileStart, latData, true);
              lonVar.writeData (tileStart, lonData, true);
            } // try
            catch (NhException e) { throw new IOException (e.toString()); }

          } // for

        } // write
      });

      // Add lat/lon data to bounds
      // --------------------------
      for (TilePosition pos : tilePositions) {
        int[] tileStart = pos.getStart();
        int[] tileDims = pos.getDimensions();
        int values = tileDims[TilingScheme.ROWS]*tileDims[TilingScheme.COLS];
        double[] latData = new double[values];
        double[] lonData = new double[values];
        getTransformData (trans, tileStart, tileDims, latData, lonData);
        for (int i = 0; i < values; i++) {
          addToLatBounds (latData[i]);
          addToLonBounds (lonData[i]);
        } // for
      } // for

    } // else

    // Create time variable
    // --------------------
    NhVariable timeVar = root.addVariable (
      TIME_VAR,
      NhVariable.TP_DOUBLE,
      new NhDimension[] {ncDims[TIME_INDEX]},
      null,
      null,
      0
    );
    timeVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, "time");
    timeVar.addAttribute ("units", NhVariable.TP_STRING_VAR, "seconds since 1970-01-01 00:00:00 UTC");

    // Write instantaneous time data
    // -----------------------------
    if (info.isInstantaneous()) {
      Date startDate = info.getStartDate();
      double[] timeData = new double[] {startDate.getTime()/1000.0};
      writeQueue.add (new DataEntry (TIME_VAR, null, timeData));
    } // if

    // Write time span data
    // --------------------
    else {

      // TODO: Add an option here for climatology?

      // Write time as half way between start and end
      // --------------------------------------------
      Date startDate = info.getStartDate();
      double startTime = startDate.getTime()/1000.0;
      Date endDate = info.getEndDate();
      double endTime = endDate.getTime()/1000.0;
      double time = (startTime + endTime)/2;
      double[] timeData = new double[] {time};
      writeQueue.add (new DataEntry (TIME_VAR, null, timeData));

      // Create and write time bounds variable
      // -------------------------------------
      timeVar.addAttribute ("bounds", NhVariable.TP_STRING_VAR, TIME_BOUNDS_VAR);
      NhDimension nvalsDim = root.addDimension (NVALS_DIM, 2);
      NhVariable timeBoundsVar = root.addVariable (
        TIME_BOUNDS_VAR,
        NhVariable.TP_DOUBLE,
        new NhDimension[] {ncDims[TIME_INDEX], nvalsDim},
        null,
        null,
        0
      );
      double[] timeBoundsData = new double[] {startTime, endTime};
      writeQueue.add (new DataEntry (TIME_BOUNDS_VAR, null, timeBoundsData));

    } // else

    // Create level variable
    // ---------------------
    NhVariable levelVar = root.addVariable (
      LEVEL_VAR,
      NhVariable.TP_DOUBLE,
      new NhDimension[] {ncDims[LEVEL_INDEX]},
      null,
      null,
      0
    );
    levelVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, "height");
    levelVar.addAttribute ("units", NhVariable.TP_STRING_VAR, "m");
    levelVar.addAttribute ("positive", NhVariable.TP_STRING_VAR, "up");
    double[] levelData = new double[] {0};
    writeQueue.add (new DataEntry (LEVEL_VAR, null, levelData));

  } // writeCFInfo

  ////////////////////////////////////////////////////////////

  /**
   * Writes DCS global metadata to the file, using information
   * from the info object.
   */
  private void writeDCSInfo () throws IOException, NhException {

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
    NhGroup root = ncFileWriter.getRootGroup();
    String origin = MetadataServices.collapse (info.getOrigin());
    root.addAttribute ("dcs:createInstitution", NhVariable.TP_STRING_VAR, origin);

    // Write creation and acquisition dates
    // ------------------------------------
    root.addAttribute ("dcs:createDateTime", NhVariable.TP_STRING_VAR,
      DateFormatter.formatDate (new Date(), ISO_DATE_FMT));
    root.addAttribute ("dcs:acquisitionStartDateTime", NhVariable.TP_STRING_VAR,
      DateFormatter.formatDate (info.getStartDate(), ISO_DATE_FMT));
    root.addAttribute ("dcs:acquisitionEndDateTime", NhVariable.TP_STRING_VAR,
      DateFormatter.formatDate (info.getEndDate(), ISO_DATE_FMT));

    // Write sensor/satellite names
    // ----------------------------
    String sensor = satInfo.getSensor();
    sensor = MetadataServices.collapse (sensor).replaceAll ("\n", ",");
    root.addAttribute ("dcs:sensor", NhVariable.TP_STRING_VAR, sensor);
    String satellite = satInfo.getSatellite();
    satellite = MetadataServices.collapse (satellite).replaceAll ("\n", ",");
    root.addAttribute ("dcs:sensorPlatform", NhVariable.TP_STRING_VAR, satellite);

    // Write earth location info
    // -------------------------
    root.addAttribute ("dcs:mapProjection", NhVariable.TP_STRING_VAR, map.getSystemName());
    root.addAttribute ("dcs:geodeticDatum", NhVariable.TP_STRING_VAR,
      map.getDatum().getDatumName());
    root.addAttribute ("dcs:northernLatitude", NhVariable.TP_DOUBLE, getNorthBound());
    root.addAttribute ("dcs:southernLatitude", NhVariable.TP_DOUBLE, getSouthBound());
    root.addAttribute ("dcs:easternLongitude", NhVariable.TP_DOUBLE, getEastBound());
    root.addAttribute ("dcs:westernLongitude", NhVariable.TP_DOUBLE, getWestBound());

    // Write observed property
    // -----------------------
    root.addAttribute ("dcs:observedProperty", NhVariable.TP_STRING_VAR, "Unknown");
    root.addAttribute ("dcs:observedPropertyAlgorithm", NhVariable.TP_STRING_VAR, "Unknown");
    root.addAttribute ("dcs:processingLevel", NhVariable.TP_STRING_VAR, "Level 3");

  } // writeDCSInfo

  ////////////////////////////////////////////////////////////

  /**
   * Writes CW global metadata to the file, using information
   * from the info object.
   */
  private void writeCWInfo () throws IOException, NhException {

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
    NhGroup root = ncFileWriter.getRootGroup();
    for (String attName : attributeArray) {
      String attValue = (String) info.getMetadataMap().get (attName);
      if (attValue != null)
        root.addAttribute ("cw:" + attName, NhVariable.TP_STRING_VAR, attValue);
    } // for

    // Copy polygon points
    // -------------------
    double[] latPoints = 
      (double[]) info.getMetadataMap().get ("polygon_latitude"); 
    double[] lonPoints = 
      (double[]) info.getMetadataMap().get ("polygon_longitude"); 
    if (latPoints != null && lonPoints != null) {
      root.addAttribute ("cw:polygon_latitude", NhVariable.TP_DOUBLE, latPoints);
      root.addAttribute ("cw:polygon_longitude", NhVariable.TP_DOUBLE, lonPoints);
    } // if

  } // writeCWInfo

  ////////////////////////////////////////////////////////////

  /**
   * Gets a NetCDF 4 type from a Java class type.  This can be used to determine
   * which NetCDF 4 type variable or attribute can be used to encode the value
   * of a Java primitive or String value.
   *
   * @param dataClass thr Java class type to convert.
   * @param isUnsigned true if the numerical values for the specified
   * Java class are to be interpreted as unsigned, or false if not. This
   * is currently only useful for byte, short, or int data.
   *
   * @return the corresponding NetCDF 4 type, or -1 if the type is unknown.
   */
  private static int getType (
    Class dataClass,
    boolean isUnsigned
  ) {

    int type = -1;
    if (isUnsigned) {
      if (dataClass.equals (Byte.TYPE) || dataClass.equals (Byte.class))
        type = NhVariable.TP_UBYTE;
      else if (dataClass.equals (Short.TYPE) || dataClass.equals (Short.class))
        type = NhVariable.TP_INT;
      else if (dataClass.equals (Integer.TYPE) || dataClass.equals (Integer.class))
        type = NhVariable.TP_LONG;
    } // if
    else {
      if (dataClass.equals (Byte.TYPE) || dataClass.equals (Byte.class))
        type = NhVariable.TP_SBYTE;
      else if (dataClass.equals (Short.TYPE) || dataClass.equals (Short.class))
        type = NhVariable.TP_SHORT;
      else if (dataClass.equals (Integer.TYPE) || dataClass.equals (Integer.class))
        type = NhVariable.TP_INT;
      else if (dataClass.equals (Long.TYPE) || dataClass.equals (Long.class))
        type = NhVariable.TP_LONG;
      else if (dataClass.equals (Float.TYPE) || dataClass.equals (Float.class))
        type = NhVariable.TP_FLOAT;
      else if (dataClass.equals (Double.TYPE) || dataClass.equals (Double.class))
        type = NhVariable.TP_DOUBLE;
      else if (dataClass.equals (Character.TYPE) || dataClass.equals (Character.class))
        type = NhVariable.TP_CHAR;
      else if (dataClass.equals (String.class))
        type = NhVariable.TP_STRING_VAR;
    } // else
    
    return (type);

  } // getType

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new data variable in the file and queues its data for writing.
   *
   * @param var the variable to create.
   *
   * @throws IOException if creating the variable or its attributes failed.
   * @throws NhException if an error occurred in the NetCDF 4 layer.
   */
  private void createVariable (
    final DataVariable var
  ) throws IOException, NhException {

    // Check rank
    // ----------
    if (!(var instanceof Grid))
      throw new IOException ("Unsupported variable type:" + var.getClass());

    // Get data type
    // -------------
    NhGroup root = ncFileWriter.getRootGroup();
    Class dataClass = var.getDataClass();
    final boolean isUnsigned = var.getUnsigned();
    final int dataType = getType (dataClass, isUnsigned);
    if (dataType == -1)
      throw new IOException ("Unsupported variable data type: " + dataClass +
        " with " + (isUnsigned ? "unsigned" : "signed") + " values");

    // Get missing value
    // -----------------
    Object missing = var.getMissing();

    // Convert unsigned missing to next wider type
    // -------------------------------------------
    if (missing != null) {
      if (isUnsigned) {
        if (dataType == NhVariable.TP_INT)
          missing = (((Short) missing).shortValue()) & 0xffff;
        else if (dataType == NhVariable.TP_LONG)
          missing = (((Integer) missing).intValue()) & 0xffffffffL;
      } // if
    } // if

    // Create variable
    // ---------------
    String varName = var.getName();
    final NhVariable ncVar = root.addVariable (
      varName,
      dataType,
      ncDims,
      chunkLengths,
      missing,
      COMPRESSION_LEVEL
    );
    ncVar.addAttribute ("missing_value", dataType, missing);

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
      if (dataType == NhVariable.TP_FLOAT) {
        scaleFactor = (Float) scaleFactor;
        addOffset = (Float) addOffset;
      } // if
      int packingDataType = getType (scaleFactor.getClass(), false);
      ncVar.addAttribute ("scale_factor", packingDataType, scaleFactor);
      ncVar.addAttribute ("add_offset", packingDataType, addOffset);
    } // if

    // Set standard and long name
    // --------------------------
    String standard = (String) var.getMetadataMap().get ("standard_name");
    if (standard != null) 
      ncVar.addAttribute ("standard_name", NhVariable.TP_STRING_VAR, standard);
    String longName = var.getLongName();
    if (longName != null && !longName.equals ("") && !longName.equals (varName))
      ncVar.addAttribute ("long_name", NhVariable.TP_STRING_VAR, longName);

    // Set units
    // ---------
    String units = var.getUnits();
    if (units != null && !units.equals (""))
      ncVar.addAttribute ("units", NhVariable.TP_STRING_VAR, units);

    // Set coordinate info
    // -------------------
    
    // TODO: Should we add extra variables here?  eg: "time level x y lat lon" ?
    
    
    ncVar.addAttribute ("coordinates", NhVariable.TP_STRING_VAR, "lat lon");


    // TODO: What about when cell_methods should indicate a climatology?


    String rasterType = (String) info.getMetadataMap().get ("raster_type");
    if (rasterType == null || !rasterType.equals ("RasterPixelIsPoint"))
      ncVar.addAttribute ("cell_methods", NhVariable.TP_STRING_VAR, "area: mean");
    ncVar.addAttribute ("grid_mapping", NhVariable.TP_STRING_VAR, COORD_REF);

    // Set ancillary variables
    // -----------------------
    String ancillary = getMultiValuedAtt (new String[] {
      "quality_mask", 
      "direction_variable"
      }, var.getMetadataMap());
    if (ancillary != null)
      ncVar.addAttribute ("ancillary_variables", NhVariable.TP_STRING_VAR, ancillary);


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
      ncVar.addAttribute ("source", NhVariable.TP_STRING_VAR, source);

    // TODO: This is a pretty rough cut for getting the source
    // attribute right!  Also, what about references?

    // Write navigation affine
    // -----------------------
    if (writeCw) {
      AffineTransform nav = ((Grid) var).getNavigation();
      if (!nav.isIdentity()) {
        double[] matrix = new double[6];
        nav.getMatrix (matrix); 
        ncVar.addAttribute ("cw:nav_affine", NhVariable.TP_DOUBLE, matrix);
      } // if
    } // if

    // Add variable data to write queue
    // --------------------------------
    writeQueue.add (new WriteQueueEntry () {
      public void write () throws IOException {

        // Loop over each tile position
        // ----------------------------
        for (TilePosition pos : tilePositions) {

          // Get data for tile
          // -----------------
          int[] tileStart = pos.getStart();
          int[] tileDims = pos.getDimensions();
          int[] start = new int[] {0, 0, tileStart[TilingScheme.ROWS], tileStart[TilingScheme.COLS]};
          Object data = ((Grid) var).getData (tileStart, tileDims);
          if (isUnsigned) data = convertUnsignedData (data, dataType);

          // Write tile
          // ----------
          try { ncVar.writeData (start, data, true); }
          catch (NhException e) { throw new IOException (e.toString()); }

        } // for

      } // write
    });

  } // createVariable

  ////////////////////////////////////////////////////////////

  /** 
   * Converts unsigned data to the next largest signed data type.
   *
   * @param data the data array to convert.
   * @param dataType the NetCDF 4 data type to convert to.
   *
   * @return the new converted data array.
   *
   * @throws IOException if the data type to convert ot is not
   * supported.
   */
  private static Object convertUnsignedData (
    Object data,
    int dataType
  ) throws IOException {

    Object newData;

    // Do nothing to byte data
    // -----------------------
    if (dataType == NhVariable.TP_UBYTE) {
      newData = data;
    } // if

    // Convert ushort to int
    // ---------------------
    else if (dataType == NhVariable.TP_INT) {
      short[] shortData = (short[]) data;
      int[] intData = new int[shortData.length];
      for (int i = 0; i < shortData.length; i++)
        intData[i] = shortData[i] & 0xffff;
      newData = intData;
    } // else if

    // Convert uint to long
    // --------------------
    else if (dataType == NhVariable.TP_LONG) {
      int[] intData = (int[]) data;
      long[] longData = new long[intData.length];
      for (int i = 0; i < intData.length; i++)
        longData[i] = intData[i] & 0xffffffffL;
      newData = longData;
    } // else if

    // Unsupported destination type
    // ----------------------------
    else {
      throw new IOException ("Unsupported destination data type: " +
        NhVariable.nhTypeNames[dataType]);
    } // else
    
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

  @Override
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
      DataVariable var = variables.remove (0);
      writeVariableName = var.getName();
      try { createVariable (var); }
      catch (NhException e) { throw new IOException (e.toString()); }

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

  @Override
  public void close () throws IOException {

    // Check for already closed
    // ------------------------
    if (!closed) {
    
      // Flush and process write queue
      // -----------------------------
      flush();
      try {
        ncFileWriter.endDefine();
        WriteQueueEntry entry;
        while ((entry = writeQueue.poll()) != null) entry.write();
        ncFileWriter.close();
      } // try
      catch (NhException e) { throw new IOException (e.toString()); }

      // Mark as closed
      // --------------
      closed = true;
      
    } // if

  } // close

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (CFNC4Writer.class);

    // Create transforms
    // -----------------
    logger.test ("Framework");
    List<EarthTransform> transforms = new ArrayList<EarthTransform>();
    transforms.add (MapProjectionFactory.getInstance().create (
      ProjectionConstants.MERCAT,
      0,
      new double[15],
      SpheroidConstants.WGS84,
      new int[] {512, 512},
      new EarthLocation (48, -125),
      new double[] {2000, 2000}
    ));
    transforms.add (MapProjectionFactory.getInstance().create (
      ProjectionConstants.GEO,
      0,
      new double[15],
      SpheroidConstants.WGS84,
      new int[] {512, 512},
      new EarthLocation (48, -125),
      new double[] {0.02, 0.02}
    ));

    double[] latData = new double[512*512];
    double[] lonData = new double[512*512];
    for (int i = 0; i < 512; i++) {
      for (int j = 0; j < 512; j++) {
        latData[i*512 + j] = 48 - 2.56 + i*0.01;
        lonData[i*512 + j] = -125 - 2.56 + j*0.01;
      } // for
    } // for
    Grid latVar = new Grid (
      "lat",
      "Latitude",
      "degrees",
      512, 512,
      latData,
      new java.text.DecimalFormat ("0"),
      null,
      Double.NaN
    );
    Grid lonVar = new Grid (
      "lon",
      "Longitude",
      "degrees",
      512, 512,
      lonData,
      new java.text.DecimalFormat ("0"),
      null,
      Double.NaN
    );
    transforms.add (new SwathProjection (latVar, lonVar, 100, new int[] {512, 512}));

    // Create variable
    // ---------------
    short[] data = new short[512*512];
    for (int i = 0; i < 512; i++)
      for (int j = 0; j < 512; j++)
        data[i*512 + j] = (short) (Math.cos (-500 + Math.sqrt ((256-i)*(256-i) + (256-j)*(256-j))/40)*4500);
    Grid var = new Grid (
      "sst",
      "Sea surface temperature",
      "degrees_Celsius",
      512, 512,
      data,
      new java.text.DecimalFormat ("0"),
      new double[] {0.01, 0},
      Short.MIN_VALUE
    );

    logger.passed();

    for (EarthTransform trans : transforms) {

      String ncFileName = "/tmp/test." + trans.getClass().getName() + ".nc4";
      logger.test ("constructor, addVariable, flush, close (" +
        ncFileName + ")");

      // Create info
      // -----------
      SatelliteDataInfo info = new SatelliteDataInfo (
        "petros-1",
        "java-19",
        Arrays.asList (
          new TimePeriod (new Date (0), 12*60*1000),
          new TimePeriod (new Date (12*60*1000 + 90*60*1000), 12*60*1000)
        ),
        trans,
        "Petros RS Inc.",
        "Created by unit test on " + new Date()
      );

      // Write file
      // ----------
      CFNC4Writer writer = new CFNC4Writer (
        info,
        ncFileName,
        (trans instanceof MapProjection),
        true
      );
      writer.addVariable (var);
      writer.flush();
      writer.close();

      File ncFile = new File (ncFileName);
      assert (ncFile.exists());
      assert (ncFile.length() != 0);
  //    ncFile.delete();
  //    assert (!ncFile.exists());

      logger.passed();
      
    } // for
    
  } // main

  ////////////////////////////////////////////////////////////

} // CFNC4Writer class

////////////////////////////////////////////////////////////////////////
