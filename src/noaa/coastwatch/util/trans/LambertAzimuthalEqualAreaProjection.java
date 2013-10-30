////////////////////////////////////////////////////////////////////////
/*
     FILE: LambertAzimuthalEqualAreaProjection.java
  PURPOSE: Handles Lambert Azimuthal Equal Area map transformations.
   AUTHOR: Peter Hollemans
     DATE: 2012/11/02
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.geom.*;
import noaa.coastwatch.util.*;

/**
 * The <code>LambertAzimuthalEqualAreaProjection</code> class performs 
 * Lambert Azimuthal Equal Area map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class LambertAzimuthalEqualAreaProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double R;                 // Radius of the earth (sphere)
  private double cos_lat_o;         // Cosine of the center latitude
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_center;        // Center latitude (projection center)
  private double lon_center;        // Center longitude (projection center)
  private double sin_lat_o;         // Sine of the center latitude

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r the (I) Radius of the earth (sphere).
   * @param center_long the (I) Center longitude.
   * @param center_lat the (I) Center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r,
    double center_long,
    double center_lat,
    double false_east,
    double false_north
  ) {

    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    R = r;
    lon_center = center_long;
    lat_center = center_lat;
    false_easting = false_east;
    false_northing = false_north;
    sin_lat_o = Math.sin (center_lat); cos_lat_o = Math.cos (center_lat);
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("LAMBERT AZIMUTHAL EQUAL-AREA");
    radius (r);
    cenlon (center_long);
    cenlat (center_lat);
    offsetp (false_easting, false_northing);
    return (OK);

  } // projinit

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a map projection from the specified projection and
   * affine transform.   The {@link SpheroidConstants} and
   * {@link ProjectionConstants} class should be consulted for
   * valid parameter constants.
   *
   * @param rMajor the semi-major axis in meters.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   * @param center_long the (I) Center longitude.
   * @param center_lat the (I) Center latitude.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public LambertAzimuthalEqualAreaProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,             // (I) Center longitude
    double center_lat,              // (I) Center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (LAMAZ, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, center_lat, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // LambertAzimuthalEqualAreaProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double delta_lon;               // Delta longitude (Given longitude - center
    double sin_delta_lon;           // Sine of the delta longitude
    double cos_delta_lon;           // Cosine of the delta longitude
    double sin_lat;                 // Sine of the given latitude
    double cos_lat;                 // Cosine of the given latitude
    double g;                       // temporary varialbe
    double ksp;                     // height above elipsiod
    char mess[] = new char[60];
    
    /*Forward equations
      -----------------*/
    delta_lon = adjust_lon (lon - lon_center);
    sin_lat = Math.sin (lat); cos_lat = Math.cos (lat);
    sin_delta_lon = Math.sin (delta_lon); cos_delta_lon = Math.cos (delta_lon);
    g = sin_lat_o*sin_lat + cos_lat_o*cos_lat*cos_delta_lon;
    if (g == -1.0)
       {
       sprintf (mess, "Point projects to a circle of radius = %lf\n", 2.0*R);
       p_error (mess, "lamaz-forward");
       return (113);
       }
    ksp = R*Math.sqrt (2.0 / (1.0 + g));
    x[0] = ksp*cos_lat*sin_delta_lon + false_easting;
    y[0] = ksp*(cos_lat_o*sin_lat - sin_lat_o*cos_lat*cos_delta_lon) +
            false_northing;
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double Rh;
    double z;                       // Great circle dist from proj center to given point
    double sin_z;                   // Sine of z
    double cos_z;                   // Cosine of z
    double temp;                    // Re-used temporary variable
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    Rh = Math.sqrt (x*x + y*y);
    temp = Rh / (2.0*R);
    if (temp > 1)
       {
       p_error ("Input data error", "lamaz-inverse");
       return (115);
       }
    z = 2.0*asinz (temp);
    sin_z = Math.sin (z); cos_z = Math.cos (z);
    lon[0] = lon_center;
    if (Math.abs (Rh) > EPSLN)
       {
       lat[0] = asinz (sin_lat_o*cos_z + cos_lat_o*sin_z*y / Rh);
       temp = Math.abs (lat_center) - HALF_PI;
       if (Math.abs (temp) > EPSLN)
          {
          temp = cos_z - sin_lat_o*Math.sin (lat[0]);
          if (temp!=0.0)lon[0]=adjust_lon (lon_center+Math.atan2 (x*sin_z*cos_lat_o, temp*Rh));
          }
       else if (lat_center < 0.0) lon[0] = adjust_lon (lon_center - Math.atan2 (-x, y));
       else lon[0] = adjust_lon (lon_center + Math.atan2 (x, -y));
       }
    else lat[0] = lat_center;
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // LambertAzimuthalEqualAreaProjection

////////////////////////////////////////////////////////////////////////
