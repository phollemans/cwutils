////////////////////////////////////////////////////////////////////////
/*
     FILE: GCTP.java
  PURPOSE: To allow access to GCTP transformations from Java
           via the native GCTP library in C.
   AUTHOR: Mark Robinson
     DATE: 2002/04/15
  CHANGES: 2002/05/15, PFH, added javadoc, package
           2002/06/01, PFH, added STD_RADIUS
           2002/12/02, PFH, added Requirements inner class
           2002/12/09, PFH, modified pack_angle, added unpack_angle
           2003/03/05, PFH, modified documentation on requirements
           2004/01/12, PFH, added getSpheroid()
           2004/02/10, PFH, added getSpheroid(String), getProjection(String)
           2004/09/05, PFH, corrected spelling of sinusoidal
           2005/04/10, PFH, added supportsSpheroid()
           2005/05/18, PFH
           - added flattening constants
           - changed "datum" to "spheroid"
           2006/05/26, PFH, removed spheroid and projection constants/methods
           2012/10/13, PFH, deprecated native methods and library
           2013/09/29, PFH
           - change: added the isSupportedSpheroid() method
           - issue: cwmaster was allowing unsupported spheroids for the 
             state plane system, so using this new method provided more 
             information about compatible spheroids
           
  CoastWatch Software Library and Utilities
  Copyright 1998-2013, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * The static routines in the General Cartographic Transformations
 * Package (GCTP) class handle the transformation of data coordinates
 * between common Earth projection systems.  The class is simply a
 * Java wrapper for the GCTP package in C.  See the <a
 * href="http://edcwww.cr.usgs.gov/pub/software/gctpc">GCTPC</a>
 * package documentation for full documentation on routine parameters
 * and usage.
 * 
 * The native methods of GCTP are no longer supported.  Use
 * the {@link noaa.coastwatch.util.trans.MapProjectionFactory} to create
 * and work with map projections.  The GCTP parameter related methods
 * in this class are still useful for creating arrays of valid 
 * parameters to pass to the map projection factory creation methods.
 *
 * @author Mark Robinson
 * @since 3.1.0
 */
public class GCTP 
  implements SpheroidConstants, ProjectionConstants {

  // Units constants
  // ---------------
  /** Radians units code. */
  public static final int RADIAN      = 0;

  /** US Feet units code. */
  public static final int FEET        = 1;

  /** Meters units code. */
  public static final int METER       = 2;

  /** Arc Seconds units code. */
  public static final int SECOND      = 3;

  /** Decimal degrees units code. */
  public static final int DEGREE      = 4;

  /** International Feet units code. */
  public static final int INT_FEET    = 5;

  /**
   * State Plane table units code. The STPLN_TABLE unit value is
   * specifically used for State Plane -- if units equals STPLN_TABLE
   * and Datum is NAD83 -- actual units are retrieved from a table
   * according to the zone.  If Datum is NAD27 -- actual units will be
   * feet.  An error will occur with this unit if the projection is
   * not State Plane.
   */
  public static final int STPLN_TABLE = 6;

  // Error constants
  // ---------------
  /** Print errors to the terminal. */
  public static final int TERM = 0;

  /** Print errors to a file. */
  public static final int FILE = 1;

  /** Print errors to both file and terminal. */
  public static final int BOTH = 2;

  /** Do not print error messages. */
  public static final int NEITHER = -1;

  // Variables
  // ---------
  /** The hash table of requirement descriptions. */
  private static Hashtable reqDesc;

  /** The hash table of requirement units. */
  private static Hashtable reqUnits;

  ///////////////////////////////////////////////////////////////

  /** Initializes static elements. */
  static {

    // Load shared library
    // -------------------
    /**
     * We remove this call so that the code no longer requires the library
     * and uses the pure Java classes.
     */
    //System.loadLibrary ("GCTP");

    // Create descriptive requirements table
    // -------------------------------------
    reqDesc = new Hashtable();
    reqDesc.put ("Lon/Z", "Longitude point or zero");
    reqDesc.put ("Lat/Z", "Latitude point or zero");
    reqDesc.put ("SMajor", "Semi-major axis of ellipsoid or zero");
    reqDesc.put ("SMinor", "Semi-minor axis of ellipsoid or zero");
    reqDesc.put ("Sphere", "Radius of reference sphere or zero");
    reqDesc.put ("STDPAR", "Latitude of standard parallel");
    reqDesc.put ("STDPR1", "Latitude of first standard parallel");
    reqDesc.put ("STDPR2", "Latitude of second standard parallel");
    reqDesc.put ("CentMer", "Longitude of central meridian");
    reqDesc.put ("OriginLat", "Latitude of projection origin");
    reqDesc.put ("FE", "False easting");
    reqDesc.put ("FN", "False northing");
    reqDesc.put ("TrueScale", "Latitude of true scale");
    reqDesc.put ("LongPol", "Longitude down below pole of map");
    reqDesc.put ("FactorMer", "Scale factor at central meridian");
    reqDesc.put ("FactorCent", "Scale factor at center of projection");
    reqDesc.put ("CentLon", "Longitude of center of projection");
    reqDesc.put ("CentLat", "Latitude of center of projection");
    reqDesc.put ("Height", "Height of perspective point");
    reqDesc.put ("Long1", "Longitude of first point on center line");
    reqDesc.put ("Long2", "Longitude of second point on center line");
    reqDesc.put ("Lat1", "Latitude of first point on center line");
    reqDesc.put ("Lat2", "Latitude of second point on center line" );
    reqDesc.put ("AziAng", "Azimuth angle east of north of center line");
    reqDesc.put ("AzmthPt", "Longitude of central meridian azimuth point");
    reqDesc.put ("IncAng", "Inclination of orbit at ascending node");
    reqDesc.put ("AscLong", "Longitude of ascending orbit at equator");
    reqDesc.put ("PSRev", "Period of sat revolution");
    reqDesc.put ("LRat", "Landsat ratio");
    reqDesc.put ("PFlag", "End of path flag for Landsat");
    reqDesc.put ("Satnum", "Landsat satellite number");
    reqDesc.put ("Path", "Landsat path number");
    reqDesc.put ("Shapem", "Oval shape parameter m");
    reqDesc.put ("Shapen", "Oval shape parameter n");
    reqDesc.put ("Angle", "Oval rotation angle");
    reqDesc.put ("zero", "Parameter mode flag, must be zero");
    reqDesc.put ("one", "Parameter mode flag, must be one");

    // Create descriptive requirements table
    // -------------------------------------
    reqUnits = new Hashtable();
    reqUnits.put ("Lon/Z", "degrees");
    reqUnits.put ("Lat/Z", "degrees");
    reqUnits.put ("SMajor", "meters");
    reqUnits.put ("SMinor", "meters");
    reqUnits.put ("Sphere", "meters");
    reqUnits.put ("STDPAR", "degrees");
    reqUnits.put ("STDPR1", "degrees");
    reqUnits.put ("STDPR2", "degrees");
    reqUnits.put ("CentMer", "degrees");
    reqUnits.put ("OriginLat", "degrees");
    reqUnits.put ("FE", "meters");
    reqUnits.put ("FN", "meters");
    reqUnits.put ("TrueScale", "degrees");
    reqUnits.put ("LongPol", "degrees");
    reqUnits.put ("FactorMer", "");
    reqUnits.put ("FactorCent", "");
    reqUnits.put ("CentLon", "degrees");
    reqUnits.put ("CentLat", "degrees");
    reqUnits.put ("Height", "meters");
    reqUnits.put ("Long1", "degrees");
    reqUnits.put ("Long2", "degrees");
    reqUnits.put ("Lat1", "degrees");
    reqUnits.put ("Lat2", "degrees");
    reqUnits.put ("AziAng", "degrees");
    reqUnits.put ("AzmthPt", "degrees");
    reqUnits.put ("IncAng", "degrees");
    reqUnits.put ("AscLong", "degrees");
    reqUnits.put ("PSRev", "minutes");
    reqUnits.put ("LRat", "");
    reqUnits.put ("PFlag", "");
    reqUnits.put ("Satnum", "");
    reqUnits.put ("Path", "");
    reqUnits.put ("Shapem", "");
    reqUnits.put ("Shapen", "");
    reqUnits.put ("Angle", "degrees");
    reqUnits.put ("zero", "");
    reqUnits.put ("one", "");

  } // static

  ///////////////////////////////////////////////////////////////

  /**
   * The GCTP requirements class helps determine the various
   * parameters required for each projection.  A requirements object
   * may be used to list the required parameters based on their
   * position in the parameters array.
   */
  public static class Requirements {

    // Constants
    // ---------
    /** The maximum number of requirement parameters. */
    public final static int MAX_PARAMETERS = 15;

    // Variables
    // ---------
    /** The list of requirements. */
    private String[] list;

    ///////////////////////////////////////////////////////////

    /** 
     * Creates a new requirements object from the specified list.
     * This constructor is meant for use by the GCTP class itself.
     * Users should obtain a requirements object using the {@link
     * GCTP#getRequirements} method.
     *
     * @param list the array of requirements as descriptive strings.
     * The array may contain empty string values for unrequired
     * parameters indices.  If the length of the array is less than
     * the maximum number of required parameters, the remaining
     * parameters are assumed to be unrequired.
     */
    public Requirements (
      String[] list
    ) {

      this.list = list;

    } // Requirements constructor

    ///////////////////////////////////////////////////////////

    /**
     * Determines if the specified parameter is required.
     *
     * @param index the parameter index in the range
     * [0..MAX_PARAMETERS-1].
     *
     * @return true if required or false if not.
     */
    public boolean isRequired (
      int index
    ) {

      if (index < 0 || index > list.length-1 || list[index].equals (""))
        return (false);
      else
        return (true);

    } // isRequired

    ///////////////////////////////////////////////////////////

    /** 
     * Gets the requirement short description for the specified
     * parameter index.  The short description is a variable name
     * style code for the required parameter.
     *
     * @param index the parameter index in the range
     * [0..MAX_PARAMETERS-1].
     *
     * @return the parameter short description, or "" if the parameter
     * is not required.
     */
    public String getShortDescription (
      int index
    ) {

      if (!isRequired (index)) return ("");
      else return (list[index]);

    } // getShortDescription

    ///////////////////////////////////////////////////////////

    /** 
     * Gets the requirement description for the specified parameter
     * index. 
     *
     * @param index the parameter index in the range
     * [0..MAX_PARAMETERS-1].
     *
     * @return the parameter description, or "" if the parameter is 
     * not required.
     */
    public String getDescription (
      int index
    ) {

      if (!isRequired (index)) return ("");
      else return ((String) reqDesc.get (list[index]));

    } // getDescription

    ///////////////////////////////////////////////////////////

    /** 
     * Gets the requirement units for the specified parameter
     * index. 
     *
     * @param index the parameter index in the range
     * [0..MAX_PARAMETERS-1].
     *
     * @return the parameter units, or "" if the parameter is 
     * not required.
     */
    public String getUnits (
      int index
    ) {

      if (!isRequired (index)) return ("");
      else return ((String) reqUnits.get (list[index]));

    } // getUnits

    ///////////////////////////////////////////////////////////

    /** Gets the total number of required parameters. */
    public int getParameters () {   

      // Count required parameters
      // -------------------------
      int count = 0;
      for (int i = 0; i < MAX_PARAMETERS; i++) {
        if (isRequired (i)) count++;        
      } // for
      return (count);

    } // getParameters

    ///////////////////////////////////////////////////////////

  } // Requirements class

  ///////////////////////////////////////////////////////////////

  /**
   * Determines if the projection system supports the specified
   * spheroid Earth model.  This is the preferred method now over
   * {@link #supportsSpheroid} because it gives more specific
   * information about spheroids supported.  This routine should be
   * used prior to creating a map projection with the 
   * {@link noaa.coastwatch.util.trans.MapProjectionFactory} methods,
   * to make sure that no error with be thrown if an incompatible
   * projection system and spheroid model are attempted.
   *
   * @param spheroid the spheroid to check for support.
   * @param system the projection system to check for support.
   *
   * @return true if the projection system supports the specified spheroid,
   * or false if not.
   */
  public static boolean isSupportedSpheroid (
    int spheroid,
    int system
  ) {

    // Check the parameters
    // --------------------
    if (spheroid < 0 || spheroid > MAX_SPHEROIDS-1)
      throw new IllegalArgumentException();
    if (system < 0 || system > MAX_PROJECTIONS-1)
      throw new IllegalArgumentException();

    // Look for compatibility
    // ----------------------
    boolean isSupported = false;
    switch (system) {

    /** 
     * These systems support all spheroids.
     */
    case GEO:
    case UTM: 
    case ALBERS:
    case LAMCC:
    case MERCAT:
    case PS:
    case POLYC:
    case EQUIDC:
    case TM:
    case HOM:
    case SOM:
    case ALASKA:
      isSupported = true;
      break;

    /**
     * The State Plane only supports CLARKE1866 and GRS1980.
     */
    case SPCS:
      isSupported = (spheroid == CLARKE1866 || spheroid == GRS1980);
      break;

    /** 
     * The rest of the systems only support SPHERE.
     */
    case STEREO:
    case LAMAZ:
    case AZMEQD:
    case GNOMON:
    case ORTHO:
    case GVNSP:
    case SNSOID:
    case EQRECT:
    case MILLER:
    case VGRINT:
    case ROBIN:
    case GOOD:
    case MOLL:
    case IMOLL:
    case HAMMER:
    case WAGIV:
    case WAGVII:
    case OBEQA:
      isSupported = (spheroid == SPHERE);
      break;

    } // switch

    return (isSupported);

  } // isSupportedSpheroid

  ///////////////////////////////////////////////////////////////
 
  /**
   * Determines if the projection system supports a generic
   * spheroid Earth model, or only a perfect sphere.
   * 
   * @param system the projection system code.
   *
   * @return true if the projection system supports a generic
   * spheroid, or false if not.
   */
  public static boolean supportsSpheroid (
    int system
  ) {

    switch (system) {

    case GEO:
    case UTM: 
    case SPCS: // only CLARKE1866 and GRS1980 allowed!
    case ALBERS:
    case LAMCC:
    case MERCAT:
    case PS:
    case POLYC:
    case EQUIDC:
    case TM:
    case HOM:
    case SOM:
    case ALASKA:
      return (true);

    case STEREO:
    case LAMAZ:
    case AZMEQD:
    case GNOMON:
    case ORTHO:
    case GVNSP:
    case SNSOID:
    case EQRECT:
    case MILLER:
    case VGRINT:
    case ROBIN:
    case GOOD:
    case MOLL:
    case IMOLL:
    case HAMMER:
    case WAGIV:
    case WAGVII:
    case OBEQA:
      return (false);

    default: 
      throw new IllegalArgumentException();
    } // switch

  } // supportsSpheroid

  ///////////////////////////////////////////////////////////////

  /** 
   * Gets the requirements for the specified projection.
   * 
   * @param system the projection system code.
   *
   * @return the parameter requirements or null if the projection
   * system is invalid.  Generally the requirements array only
   * contains one element.  If there is more than one way to specify
   * parameters for the projection system, the array will contain each
   * set of requirements.
   */
  public static Requirements[] getRequirements (
    int system
  ) {

    // Create projection requirements list
    // -----------------------------------
    String[] list = null;
    String[] list2 = null;    
    switch (system) {

    case GEO: 
      list = new String[] {}; 
      break;

    case UTM:
      list = new String[] {"Lon/Z", "Lat/Z"}; 
      break;

    case SPCS:
      list = new String[] {}; 
      break;

    case ALBERS:
      list = new String[] {"SMajor", "SMinor", "STDPR1", "STDPR2", 
        "CentMer", "OriginLat", "FE", "FN"};
      break;

    case LAMCC:
      list = new String[] {"SMajor", "SMinor", "STDPR1", "STDPR2", 
        "CentMer", "OriginLat", "FE", "FN"}; 
      break;

    case MERCAT:
      list = new String[] {"SMajor", "SMinor", "", "", "CentMer", 
        "TrueScale", "FE", "FN"}; 
      break;

    case PS:
      list = new String[] {"SMajor", "SMinor", "", "", "LongPol", 
        "TrueScale", "FE", "FN"}; 
      break;

    case POLYC:
      list = new String[] {"SMajor", "SMinor", "", "", "CentMer", 
        "OriginLat", "FE", "FN"}; 
      break;

    case EQUIDC:
      list = new String[] {"SMajor", "SMinor", "STDPAR", "", "CentMer", 
        "OriginLat", "FE", "FN", "zero"}; 
      list2 = new String[] {"SMajor", "SMinor", "STDPR1", "STDPR2", 
        "CentMer", "OriginLat", "FE", "FN", "one"}; 
      break;

    case TM:
      list = new String[] {"SMajor", "SMinor", "FactorMer", "", "CentMer", 
        "OriginLat", "FE", "FN"}; 
      break;

    case STEREO:
      list = new String[] {"Sphere", "", "", "", "CentLon", "CentLat", 
        "FE", "FN"};
      break;

    case LAMAZ:
      list = new String[] {"Sphere", "", "", "", "CentLon", "CentLat", 
        "FE", "FN"};
      break;

    case AZMEQD:
      list = new String[] {"Sphere", "", "", "", "CentLon", "CentLat", 
        "FE", "FN"};
      break;

    case GNOMON:
      list = new String[] {"Sphere", "", "", "", "CentLon", "CentLat", 
        "FE", "FN"};
      break;

    case ORTHO:
      list = new String[] {"Sphere", "", "", "", "CentLon", "CentLat", 
        "FE", "FN"};
      break;

    case GVNSP:
      list = new String[] {"Sphere", "", "Height", "", "CentLon", "CentLat",
        "FE", "FN"};
      break;

    case SNSOID:
      list = new String[] {"Sphere", "", "", "", "CentMer", "", "FE", "FN"};
      break;

    case EQRECT:
      list = new String[] {"Sphere", "", "", "", "CentMer", "TrueScale", 
        "FE", "FN"};
      break;

    case MILLER:
      list = new String[] {"Sphere", "", "", "", "CentMer", "", "FE", "FN"};
      break;

    case VGRINT:
      list = new String[] {"Sphere", "", "", "", "CentMer", "OriginLat", 
        "FE", "FN"};
      break;

    case HOM:
      list = new String[] {"SMajor", "SMinor", "FactorCent", "", "", 
        "OriginLat", "FE", "FN", "Long1", "Lat1", "Long2", "Lat2", "zero"};
      list2 = new String[] {"SMajor", "SMinor", "FactorCent", "AziAng", 
        "AzmthPt", "OriginLat", "FE", "FN", "", "", "", "", "one"};
      break;

    case ROBIN:
      list = new String[] {"Sphere", "", "", "", "CentMer", "", "FE", "FN"};
      break;

    case SOM:
      list = new String[] {"SMajor", "SMinor", "", "IncAng", "AscLong", "", 
        "FE", "FN", "PSRev", "LRat", "PFlag", "", "zero"};
      list2 = new String[] {"SMajor", "SMinor", "Satnum", "Path", "", "", 
        "FE", "FN", "", "", "", "", "one"};
      break;

    case ALASKA:
      list = new String[] {"SMajor", "SMinor", "", "", "", "", "FE", "FN"};
      break;

    case GOOD:
      list = new String[] {"Sphere"};
      break;

    case MOLL:
      list = new String[] {"Sphere", "", "", "", "CentMer", "", "FE", "FN"};
      break;

    case IMOLL:
      list = new String[] {"Sphere"};
      break;

    case HAMMER:
      list = new String[] {"Sphere", "", "", "", "CentMer", "", "FE", "FN"};
      break;

    case WAGIV:
      list = new String[] {"Sphere", "", "", "", "CentMer", "", "FE", "FN"};
      break;

    case WAGVII:
      list = new String[] {"Sphere", "", "", "", "CentMer", "", "FE", "FN"};
      break;

    case OBEQA:
      list = new String[] {"Sphere", "", "Shapem", "Shapen", "CentLon", 
        "CentLat", "FE", "FN", "Angle"};
      break;

    default:
      break;

    } // switch

    // Create projection requirements
    // ------------------------------
    if (list == null) return (null);
    else if (list2 == null) 
      return (new Requirements[] {new Requirements (list)});
    else 
      return (new Requirements[] {new Requirements (list),
        new Requirements (list2)});

  } // getRequirements

  ///////////////////////////////////////////////////////////////

  /**
   * Performs a single coordinate transform.  Both input and output
   * coordinate parameters are specified.
   *
   * @param input_coord the data coordinate to transform.  Input
   * coordinate values are either <code>[x, y]</code> in a projection
   * coordinate system or <code>[lon, lat]</code> for geographic
   * coordinates.
   * @param input_system the projection type of the input coordinates.
   * The projection type must be a valid GCTP projection code
   * constant.
   * @param input_zone the zone of the input coordinates if the input
   * system is State Plane or UTM.
   * @param input_parameters an array of 15 parameters for the input
   * projection system.
   * @param input_units the input coordinate units.  The input units
   * must be a valid GCTP units code constant.
   * @param input_spheroid the input coordinate spheroid.  The input
   * spheroid must be a valid GCTP spheroid code constant.
   * @param error_message_flag a flag to determine the destination of
   * error messages.  The value must be <code>TERM</code>,
   * <code>FILE</code>, <code>BOTH</code>, or <code>NEITHER</code>.
   * See the GCTP constants for details.
   * @param error_file the file name for error messages.
   * @param jpr a flag to determine the destination of projection
   * parameter messages.  The value must be <code>TERM</code>,
   * <code>FILE</code>, <code>BOTH</code>, or <code>NEITHER</code>.
   * See the GCTP constants for details.
   * @param pfile the file name for projection parameter messages.
   * @param output_system the projection type of the output
   * coordinates.  The projection type must be a valid GCTP projection
   * code constant.
   * @param output_zone the zone of the output coordinates if the
   * output system is State Plane or UTM.
   * @param output_parameters an array of 15 parameters for the output
   * projection system.
   * @param output_unit the output coordinate units.  The output units
   * must be a valid GCTP units code constant.
   * @param output_spheroid the output coordinate spheroid.  The output
   * spheroid must be a valid GCTP spheroid code constant.
   * @param NAD1927_zonefile the file containing NAD 1927 State Plane
   * zone parameters.
   * @param NAD1983_zonefile the file containing NAD 1983 State Plane
   * zone parameters.
   *
   * @return the transformed output coordinates.  Coordinates are
   * either <code>[x, y]</code> in a projection coordinate system or
   * <code>[lon, lat]</code> for geographic coordinates.
   *
   * @deprecated The native methods of GCTP are no longer supported.  Use
   * the {@link noaa.coastwatch.util.trans.MapProjectionFactory} to create 
   * and work with map projections.
   */
  @Deprecated
  public static double[] gctp (
    double input_coord[],
    int input_system,
    int input_zone,
    double input_parameters[],
    int input_units,
    int input_spheroid,
    int error_message_flag,
    String error_file,
    int jpr,
    String pfile,
    int output_system,
    int output_zone,
    double output_parameters[],
    int output_unit,
    int output_spheroid,
    String NAD1927_zonefile,
    String NAD1983_zonefile
  ) {
  
   throw new IllegalStateException();
    
  } // gctp

  ////////////////////////////////////////////////////////////

  /**
   * Initializes the forward transformation projection parameters.
   * Subsequent calls to <code>forward</code> will use the specified
   * projection parameters.<p>
   *
   * Refer to the {@link #gctp} routine output parameters for details.
   *
   * @deprecated The native methods of GCTP are no longer supported.  Use
   * the {@link noaa.coastwatch.util.trans.MapProjectionFactory} to create 
   * and work with map projections.
   */
  @Deprecated
  public static void init_forward (
    int output_system,
    int output_zone,
    double output_parameters[],
    int output_spheroid,
    String NAD1927_zonefile,
    String NAD1983_zonefile
  ) {
  
   throw new IllegalStateException();
    
  } // init_forward

  ////////////////////////////////////////////////////////////

  /**
   * Initializes the inverse transformation projection parameters.
   * Subsequent calls to <code>inverse</code> will use the specified
   * projection parameters.<p>
   *
   * Refer to the {@link #gctp} routine input parameters for details.
   *
   * @deprecated The native methods of GCTP are no longer supported.  Use
   * the {@link noaa.coastwatch.util.trans.MapProjectionFactory} to create 
   * and work with map projections.
   */
  @Deprecated
  public static void init_inverse (
    int input_system,
    int input_zone,
    double input_parameters[],
    int input_spheroid,
    String NAD1927_zonefile,
    String NAD1983_zonefile
  ) {
  
   throw new IllegalStateException();
    
  } // init_inverse

  ////////////////////////////////////////////////////////////

  /**
   * Transforms coordinates from geographic to projection system.
   *
   * @param pos the coordinates to transform as <code>[lon,
   * lat]</code> in radians.
   * @param output_system the projection type of the output
   * coordinates.  The projection type must be a valid GCTP projection
   * code constant.
   *
   * @return the transformed output coordinates as <code>[x, y]</code>.
   *
   * @throws Exception if the forward transform failed.  Usually, this
   * is because some geographic coordinates have no valid map
   * coordinates.
   *
   * @see #inverse
   *
   * @deprecated The native methods of GCTP are no longer supported.  Use
   * the {@link noaa.coastwatch.util.trans.MapProjectionFactory} to create 
   * and work with map projections.
   */
  @Deprecated
  public static double[] forward (
    double[] pos,
    int output_system
  ) {
  
   throw new IllegalStateException();
    
  } // forward

  ////////////////////////////////////////////////////////////

  /**
   * Transforms coordinates from projection system to geographic.
   *
   * @param pos the coordinates to transform in <code>[x, y]</code>.
   * @param input_system the projection type of the input
   * coordinates.  The projection type must be a valid GCTP projection
   * code constant.
   *
   * @return the transformed output coordinates as <code>[lon, lat]</code>
   * in radians.
   *
   * @throws Exception if the inverse transform failed.
   *
   * @see #forward
   *
   * @deprecated The native methods of GCTP are no longer supported.  Use
   * the {@link noaa.coastwatch.util.trans.MapProjectionFactory} to create 
   * and work with map projections.
   */
  @Deprecated
  public static double[] inverse (
    double[] pos,
    int input_system
  ) {
  
   throw new IllegalStateException();
    
  } // inverse

  ////////////////////////////////////////////////////////////

  /**
   * Packs an angle in radians to DDDMMMSSS.SS format.
   * 
   * @param angle the angle in radians.
   *
   * @return the packed angle in DDDMMMSSS.SS format.
   *
   * @deprecated The native methods of GCTP are no longer supported.  Use
   * the {@link noaa.coastwatch.util.trans.MapProjectionFactory} to create 
   * and work with map projections.
   */
  @Deprecated
  private static double pakr2dm (
    double angle
  ) {
  
   throw new IllegalStateException();
    
  } // pakr2dm

  ////////////////////////////////////////////////////////////

  /**
   * Packs an angle in degrees to DDDMMMSSS.SS format.
   * 
   * @param angle the angle in degrees.
   *
   * @return the packed angle in DDDMMMSSS.SS format.
   */
  public static double pack_angle (
    double angle
  ) {

    return (GCTPStyleProjection.pack_angle (angle));

  } // pack_angle

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks an angle in DDDMMMSSS.SS format to degrees.
   * 
   * @param angle the packed angle in DDDMMMSSS.SS format.
   *
   * @return the angle in degrees.
   */
  public static double unpack_angle (
    double angle
  ) {

    return (GCTPStyleProjection.unpack_angle (angle));

  } // unpack_angle

  ////////////////////////////////////////////////////////////

  private GCTP () { }

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    for (int system = 0; system < MAX_PROJECTIONS ; system++) {
      System.out.println (PROJECTION_NAMES[system]);
      for (int spheroid = 0; spheroid < MAX_SPHEROIDS; spheroid++) {
        if (GCTP.isSupportedSpheroid (spheroid, system))
          System.out.println ("  " + SPHEROID_NAMES[spheroid]);
      } // for
    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // GCTP class

////////////////////////////////////////////////////////////////////////
