////////////////////////////////////////////////////////////////////////
/*

     File: MapProjectionFactory.java
   Author: Peter Hollemans
     Date: 2006/05/26

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
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.AlaskaConformalProjection;
import noaa.coastwatch.util.trans.AlbersConicalEqualAreaProjection;
import noaa.coastwatch.util.trans.AzimuthalEquidistantProjection;
import noaa.coastwatch.util.trans.EquidistantConicProjection;
import noaa.coastwatch.util.trans.EquirectangularProjection;
import noaa.coastwatch.util.trans.GCTPCStyleProjection;
import noaa.coastwatch.util.trans.GCTPStyleProjection;
import noaa.coastwatch.util.trans.GeneralVerticalNearsidePerspectiveProjection;
import noaa.coastwatch.util.trans.GeographicProjection;
import noaa.coastwatch.util.trans.GnomonicProjection;
import noaa.coastwatch.util.trans.HammerProjection;
import noaa.coastwatch.util.trans.HotineObliqueMercatorProjection;
import noaa.coastwatch.util.trans.InterruptedGoodeHomolosineProjection;
import noaa.coastwatch.util.trans.InterruptedMollweideProjection;
import noaa.coastwatch.util.trans.LambertAzimuthalEqualAreaProjection;
import noaa.coastwatch.util.trans.LambertConformalConicProjection;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MercatorProjection;
import noaa.coastwatch.util.trans.MillerCylindricalProjection;
import noaa.coastwatch.util.trans.MollweideProjection;
import noaa.coastwatch.util.trans.OblatedEqualAreaProjection;
import noaa.coastwatch.util.trans.OrthographicProjection;
import noaa.coastwatch.util.trans.PolarStereographicProjection;
import noaa.coastwatch.util.trans.PolyconicProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.trans.RobinsonProjection;
import noaa.coastwatch.util.trans.SinusoidalProjection;
import noaa.coastwatch.util.trans.SpaceObliqueMercatorProjection;
import noaa.coastwatch.util.trans.StatePlaneProjection;
import noaa.coastwatch.util.trans.StereographicProjection;
import noaa.coastwatch.util.trans.TransverseMercatorProjection;
import noaa.coastwatch.util.trans.UniversalTransverseMercatorProjection;
import noaa.coastwatch.util.trans.VanderGrintenProjection;
import noaa.coastwatch.util.trans.WagnerIVProjection;
import noaa.coastwatch.util.trans.WagnerVIIProjection;

/**
 * The <code>MapProjectionFactory</code> class creates instances
 * of map projections.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class MapProjectionFactory 
  implements ProjectionConstants {

  // Variables
  // ---------

  /** The static instance of this factory. */
  private static MapProjectionFactory instance;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an instance of this factory with no GCTP forcing.  This
   * is the main method that most classes should use to create a
   * {@link MapProjection} because it allows the factory to
   * possibly return a performance-enhanced pure Java version of
   * a {@link GCTPStyleProjection} object.
   */
  public static MapProjectionFactory getInstance () {

    if (instance == null) instance = new MapProjectionFactory();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** Creates a new map projection factory with no GCTP forcing. */
  protected MapProjectionFactory () { }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the positive lon flag for geographic projections.  This
   * is necessary for geographic projections that span the
   * +180/-180 longitude boundary so that earth locations with
   * longitudes in the [-180..0] range are converted to be
   * positive before applying the affine transform so that
   * positive column values result.
   *
   * @param proj the projection to set the longitude flag for.
   *
   * @see MapProjection#setPositiveLon
   */
  private void setLonFlag (
    MapProjection proj
  ) {

    if (proj.getSystem() == GEO) {
      DataLocation topRight = proj.transform (proj.transform (
        new DataLocation (0, proj.getDimensions()[Grid.COLS]-1)));
      if (topRight.get (Grid.COLS) < 0) { 
        proj.setPositiveLon (true);
      } // if
    } // if

  } // setLonFlag

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new map projection from the specified GCTP-style
   * projection and data parameters.  The {@link
   * SpheroidConstants} and {@link ProjectionConstants} class
   * should be consulted for valid parameter constants.
   *
   * @param system the map projection system.
   * @param zone the map projection zone for State Plane and UTM
   * projections.
   * @param parameters an array of 15 GCTP projection parameters.
   * @param spheroid the spheroid code or -1 for custom spheroid.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param centerLoc the earth location at the map center.
   * @param pixelDims the pixel dimensions in meters at the projection
   * reference point as <code>[height, width]</code>.
   *
   * @return the new map projection.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the projection system
   * and spheroid are incompatible or projection parameters are inconsistent.
   */
  public MapProjection create (
    int system,
    int zone,
    double[] parameters,
    int spheroid,
    int[] dimensions,
    EarthLocation centerLoc,
    double[] pixelDims
  ) throws NoninvertibleTransformException {

    // Create initial projection
    // -------------------------
    MapProjection proj = create (system, zone, parameters,
      spheroid, dimensions, new AffineTransform());

    // Modify projection for center and resolution
    // -------------------------------------------
    proj = proj.getModified (centerLoc, pixelDims);
    setLonFlag (proj);
    return (proj);

  } // create

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new map projection from the specified GCTP-style
   * projection and data parameters.  The {@link
   * SpheroidConstants} and {@link ProjectionConstants} class
   * should be consulted for valid parameter constants.
   *
   * @param system the map projection system.
   * @param zone the map projection zone for State Plane and UTM
   * projections.
   * @param parameters an array of 15 GCTP projection parameters.
   * @param spheroid the spheroid code or -1 for custom spheroid.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   *
   * @return the new map projection.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the projection system
   * and spheroid are incompatible or projection parameters are inconsistent.
   */
  public MapProjection create (
    int system,
    int zone,
    double[] parameters,
    int spheroid,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    double azimuth = 0;         // azimuth
    double alf = 0;             // SOM angle
    double angle = 0;           // rotation angle
    double lon1 = 0;            // longitude point in utm scene
    double lon2 = 0;            // 2nd longitude
    double lat1 = 0;            // 1st standard parallel
    double lat2 = 0;            // 2nd standard parallel
    double center_long = 0;     // center longitude
    double center_lat = 0;      // center latitude
    double h = 0;               // height above sphere
    double lon_origin = 0;      // longitude at origin
    double lat_origin = 0;      // latitude at origin
    double r_major = 0;         // major axis in meters
    double r_minor = 0;         // minor axis in meters
    double scale_factor = 0;    // scale factor
    double false_easting = 0;   // false easting in meters
    double false_northing = 0;  // false northing in meters
    double shape_m = 0;         // constant used for Oblated Equal Area
    double shape_n = 0;         // constant used for Oblated Equal Area
    long   start = 0;           // where SOM starts beginning or end
    double time = 0;            // SOM time
    double radius = 0;          // radius of sphere
    int tmpspheroid = 0;        // temporary spheroid for UTM
    long path = 0;              // SOM path number
    long satnum = 0;            // SOM satellite number
    long mode = 0;              // which initialization method to use A or B

    // Get spheroid shape values
    // -------------------------
    double[] rmaj = new double[1];
    double[] rmin = new double[1];
    double[] rad = new double[1];
    GCTPStyleProjection.sphdz (spheroid, parameters, rmaj, rmin, rad);
    r_major = rmaj[0];
    r_minor = rmin[0];
    radius = rad[0];

    // Create projection
    // -----------------
    GCTPCStyleProjection proj = null;
    false_easting  = parameters[6];
    false_northing = parameters[7];
    long[] iflg = new long[1];
    double S2R = Math.PI/180.0/3600.0;
    double R2D = 180.0/Math.PI;

    switch (system) {

    // Universal Transverse Mercator
    // -----------------------------
    case UTM:
      // Set Clarke 1866 spheroid if negative spheroid code
      // --------------------------------------------------
      if (spheroid < 0) {
        tmpspheroid = 0;
        GCTPStyleProjection.sphdz (tmpspheroid, parameters, rmaj, rmin, rad);
        r_major = rmaj[0];
        r_minor = rmin[0];
        radius = rad[0];
      } // if
      if (zone == 0) {
        lon1 = GCTPCStyleProjection.paksz (parameters[0], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
        lat1 = GCTPCStyleProjection.paksz (parameters[1], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
        zone = GCTPStyleProjection.calc_utm_zone (lon1 * R2D);
        if (lat1 < 0)
          zone = -zone;
      } // if
      scale_factor = .9996;
      proj = new UniversalTransverseMercatorProjection (
        r_major, r_minor, dimensions, affine, scale_factor, zone);
      break;
      
    // State Plane
    // -----------
    case SPCS:
      String nad27Path, nad83Path;
      try {
        nad27Path = IOServices.getFilePath (MapProjectionFactory.class, "nad27sp");
        nad83Path = IOServices.getFilePath (MapProjectionFactory.class, "nad83sp");
      } // try
      catch (IOException e) {
        throw new IllegalStateException ("Error finding State Plane Coordinate data files");
      } // catch
      proj = new StatePlaneProjection (dimensions, affine, zone, spheroid,
        nad27Path, nad83Path);
      break;

    // Albers
    // ------
    case ALBERS:
      lat1 = GCTPCStyleProjection.paksz (parameters[2], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat2 = GCTPCStyleProjection.paksz (parameters[3], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat_origin = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new AlbersConicalEqualAreaProjection (r_major, r_minor,
        dimensions, affine, lat1, lat2, center_long, lat_origin,
        false_easting, false_northing);
      break;

    // Lambert Conformal
    // -----------------
    case LAMCC:
      lat1 = GCTPCStyleProjection.paksz (parameters[2], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat2 = GCTPCStyleProjection.paksz (parameters[3], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat_origin = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new LambertConformalConicProjection (r_major, r_minor,
        dimensions, affine, lat1, lat2, center_long, lat_origin,
        false_easting, false_northing);
      break;

    // Mercator
    // --------
    case MERCAT:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat1 = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new MercatorProjection (r_major, r_minor, dimensions, affine,
        center_long, lat1, false_easting, false_northing);
      break;

    // Polar Stereographic
    // -------------------
    case PS:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat1  = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new PolarStereographicProjection (r_major, r_minor, dimensions,
        affine, center_long, lat1, false_easting, false_northing);
      break;

    // Polyconic
    // ---------
    case POLYC:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat_origin = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new PolyconicProjection (r_major, r_minor, dimensions, affine,
        center_long, lat_origin, false_easting, false_northing);
      break;

    // Equidistant Conic
    // -----------------
    case EQUIDC:
      lat1 = GCTPCStyleProjection.paksz (parameters[2], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat2 = GCTPCStyleProjection.paksz (parameters[3], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_long  = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat_origin = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      if (parameters[8] == 0)
        mode = 0;
      else 
        mode = 1;
      proj = new EquidistantConicProjection (r_major, r_minor, dimensions,
        affine, lat1, lat2, center_long, lat_origin, mode, false_easting,
        false_northing);
      break;

    // Transverse Mercator
    // -------------------
    case TM:
      scale_factor = parameters[2];
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat_origin = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new TransverseMercatorProjection (r_major, r_minor, dimensions,
        affine, scale_factor, center_long, lat_origin, false_easting,
        false_northing);
      break;

    // Stereographic
    // -------------
    case STEREO:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_lat = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new StereographicProjection (radius, dimensions,
        affine, center_long, center_lat, false_easting, false_northing);
      break;
      
    // Lambert Azimuthal
    // -----------------
    case LAMAZ:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_lat = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new LambertAzimuthalEqualAreaProjection (radius,
        dimensions, affine, center_long, center_lat, false_easting,
        false_northing);
      break;
      
    // Azimuthal Equidistant
    // ---------------------
    case AZMEQD:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_lat = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new AzimuthalEquidistantProjection (radius, dimensions, affine,
        center_long, center_lat, false_easting, false_northing); 
      break;
    
    // Gnomonic
    // --------
    case GNOMON:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_lat = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new GnomonicProjection (radius, dimensions, affine,
        center_long, center_lat, false_easting, false_northing);
      break;

    // Orthographic
    // ------------
    case ORTHO:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_lat = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new OrthographicProjection (radius, dimensions, affine,
        center_long, center_lat, false_easting, false_northing);
       break;
    
    // General Vertical Near-side Perspective
    // --------------------------------------
    case GVNSP:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_lat = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      h = parameters[2];
      proj = new GeneralVerticalNearsidePerspectiveProjection (radius,
        dimensions, affine, h, center_long, center_lat, false_easting,
        false_northing);
      break;
      
    // Sinusoidal
    // ----------
    case SNSOID:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new SinusoidalProjection (radius, dimensions, affine,
        center_long, false_easting, false_northing);
      break;

    // Equirectangular
    // ---------------
    case EQRECT:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      lat1   = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new EquirectangularProjection (radius, dimensions, affine,
        center_long, lat1, false_easting, false_northing);
      break;
      
    // Miller
    // ------
    case MILLER:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg) *3600*S2R;
      if (iflg[0] != 0) break;
      proj = new MillerCylindricalProjection (radius, dimensions, affine,
        center_long, false_easting, false_northing);
      break;

    // Van Der Grinten
    // ---------------
    case VGRINT:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new VanderGrintenProjection (radius, dimensions, affine,
        center_long, false_easting, false_northing);
      break;

    // Hotine Oblique Mercator
    // -----------------------
    case HOM:
      scale_factor = parameters[2];
      lat_origin = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      if (parameters[12] != 0) {
        mode = 1;
        azimuth = GCTPCStyleProjection.paksz (parameters[3], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
        lon_origin = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
      } // if
      else {
        mode = 0;
        lon1 = GCTPCStyleProjection.paksz (parameters[8], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
        lat1 = GCTPCStyleProjection.paksz (parameters[9], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
        lon2 = GCTPCStyleProjection.paksz (parameters[10], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
        lat2 = GCTPCStyleProjection.paksz (parameters[11], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
      } // else
      proj = new HotineObliqueMercatorProjection (r_major, r_minor,
        dimensions, affine, scale_factor, azimuth, lon_origin,
        lat_origin, lon1, lat1, lon2, lat2, mode, false_easting,
        false_northing);
      break;

    // Space Oblique Mercator
    // ----------------------
    case SOM:
      path = (long) parameters[3];
      satnum = (long) parameters[2];
      if (parameters[12] == 0) {
        mode = 1;
        alf = GCTPCStyleProjection.paksz (parameters[3], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
        lon1 = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
        if (iflg[0] != 0) break;
        time = parameters[8];
        start = (long) parameters[10];
      } // if
      else
        mode = 0;
      proj = new SpaceObliqueMercatorProjection (r_major, r_minor,
        dimensions, affine, satnum, path, alf, lon1, time, start, mode,
        false_easting, false_northing);
      break;

    // Hammer
    // ------
    case HAMMER:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new HammerProjection (radius, dimensions, affine,
        center_long, false_easting, false_northing);
      break;
      
    // Robinson
    // --------
    case ROBIN:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new RobinsonProjection (radius, dimensions, affine,
        center_long, false_easting, false_northing);
      break;

    // Goode Homolosine
    // ----------------
    case GOOD:
      proj = new InterruptedGoodeHomolosineProjection (radius, dimensions,
        affine);
      break;
    
    // Mollweide
    // ---------
    case MOLL:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new MollweideProjection (radius, dimensions, affine,
        center_long, false_easting, false_northing);
      break;
    
    // Interrupted Mollweide
    // ---------------------
    case IMOLL:
      proj = new InterruptedMollweideProjection (radius, dimensions, affine);
      break;
      
    // Alaska
    // ------
    case ALASKA:
      proj = new AlaskaConformalProjection (r_major, r_minor, dimensions,
        affine, false_easting, false_northing);
      break;

    // Wagner IV
    // ---------
    case WAGIV:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new WagnerIVProjection (radius, dimensions, affine,
        center_long, false_easting, false_northing);
      break;

    // Wagner VII
    // ----------
    case WAGVII:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new WagnerVIIProjection (radius, dimensions, affine,
        center_long, false_easting, false_northing);
      break;

    // Oblated Equal Area
    // ------------------
    case OBEQA:
      center_long = GCTPCStyleProjection.paksz (parameters[4], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      center_lat  = GCTPCStyleProjection.paksz (parameters[5], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      shape_m = parameters[2];
      shape_n = parameters[3];
      angle = GCTPCStyleProjection.paksz (parameters[8], iflg)*3600*S2R;
      if (iflg[0] != 0) break;
      proj = new OblatedEqualAreaProjection (radius, dimensions, affine,
        center_long, center_lat,shape_m, shape_n, angle,
        false_easting, false_northing);
      break;

    // Geographic
    // ----------
    case GEO:
      proj = new GeographicProjection (r_major, r_minor, dimensions, affine);
      break;
      
    default:
      throw new IllegalArgumentException ("Unsupported projection system: " + system);

    } // switch

    // Check projection created
    // ------------------------
    if (proj == null)
      throw new IllegalArgumentException ("Projection parameter angles invalid");
    
    setLonFlag (proj);
    proj.setParameters (parameters);
    return (proj);

  } // create

  ////////////////////////////////////////////////////////////

} // MapProjectionFactory class

////////////////////////////////////////////////////////////////////////
