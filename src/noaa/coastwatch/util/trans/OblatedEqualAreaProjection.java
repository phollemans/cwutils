////////////////////////////////////////////////////////////////////////
/*
     FILE: OblatedEqualAreaProjection.java
  PURPOSE: Handles Oblated Equal Area map transformations.
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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import noaa.coastwatch.util.trans.GCTPCStyleProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;

/**
 * The <code>OblatedEqualAreaProjection</code> class performs 
 * Oblated Equal Area map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class OblatedEqualAreaProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double R;
  private double cos_lat_o;
  private double false_easting;
  private double false_northing;
  private double lat_o;
  private double lon_center;
  private double m;
  private double n;
  private double sin_lat_o;
  private double theta;

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r the earth radius.
   * @param center_long the center longitude.
   * @param center_lat the center latitude.
   * @param shape_m the oval shape parameter m.
   * @param shape_n the oval shape parameter n.
   * @param angle the oval rotation angle.
   * @param false_east
   * @param false_north
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r,
    double center_long,
    double center_lat,
    double shape_m,
    double shape_n,
    double angle,
    double false_east,
    double false_north
  ) {

    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    R = r;
    lon_center = center_long;
    lat_o = center_lat;
    m = shape_m;
    n = shape_n;
    theta = angle;
    false_easting = false_east;
    false_northing = false_north;
    
    /*Report parameters to the user (to device set up prior to this call)
      -------------------------------------------------------------------*/
    ptitle ("OBLATED EQUAL-AREA");
    radius (R);
    cenlon (lon_center);
    cenlat (lat_o);
    genrpt (m,"Parameter m:      ");
    genrpt (n,"Parameter n:      ");
    genrpt (theta,"Theta:      ");
    offsetp (false_easting, false_northing);
    
    /*Calculate the sine and cosine of the latitude of the center of the map
       and store in static storage for common use.
      -------------------------------------------*/
    sin_lat_o = Math.sin (lat_o); cos_lat_o = Math.cos (lat_o);
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
   * @param center_long the center longitude.
   * @param center_lat the center latitude.
   * @param shape_m the oval shape parameter m.
   * @param shape_n the oval shape parameter n.
   * @param angle the oval rotation angle.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public OblatedEqualAreaProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,
    double center_lat,
    double shape_m,
    double shape_n,
    double angle,
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (OBEQA, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, center_lat, shape_m, shape_n, angle, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // OblatedEqualAreaProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double delta_lon;
    double sin_delta_lon;
    double cos_delta_lon;
    double sin_lat;
    double cos_lat;
    double z;
    double sin_z;
    double cos_z;
    double Az;
    double sin_Az;
    double cos_Az;
    double temp;                    // Re-used temporary variable
    double x_prime;
    double y_prime;
    double M;
    double N;
    double diff_angle;
    double sin_diff_angle;
    double cos_diff_angle;
    
    /*Forward equations
      -----------------*/
    delta_lon = lon - lon_center;
    sin_lat = Math.sin (lat); cos_lat = Math.cos (lat);
    sin_delta_lon = Math.sin (delta_lon); cos_delta_lon = Math.cos (delta_lon);
    z = Math.acos (sin_lat_o*sin_lat + cos_lat_o*cos_lat*cos_delta_lon);
    Az = Math.atan2 (cos_lat*sin_delta_lon , cos_lat_o*sin_lat - sin_lat_o*
            cos_lat*cos_delta_lon) + theta;
    sin_Az = Math.sin (Az); cos_Az = Math.cos (Az);
    temp = 2.0*Math.sin (z / 2.0);
    x_prime = temp*sin_Az;
    y_prime = temp*cos_Az;
    M = Math.asin (x_prime / 2.0);
    temp = y_prime / 2.0*Math.cos (M) / Math.cos (2.0*M / m);
    N = Math.asin (temp);
    y[0] = n*R*Math.sin (2.0*N / n) + false_easting;
    x[0] = m*R*Math.sin (2.0*M / m)*Math.cos (N) / Math.cos (2.0*N / n) + false_northing;
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double z;
    double sin_z;
    double cos_z;
    double Az;
    double sin_Az;
    double cos_Az;
    double temp;                    // Re-used temporary variable
    double x_prime;
    double y_prime;
    double M;
    double N;
    double diff_angle;
    double sin_diff_angle;
    double cos_diff_angle;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    N = (n / 2.0)*Math.asin (y / (n*R));
    temp = x / (m*R)*Math.cos (2.0*N / n) / Math.cos (N);
    M = (m / 2.0)*Math.asin (temp);
    x_prime = 2.0*Math.sin (M);
    y_prime = 2.0*Math.sin (N)*Math.cos (2.0*M / m) / Math.cos (M);
    temp = Math.sqrt (x_prime*x_prime + y_prime*y_prime) / 2.0;
    z = 2.0*Math.asin (temp);
    Az = Math.atan2 (x_prime, y_prime);
    diff_angle = Az - theta;
    sin_diff_angle = Math.sin (diff_angle); cos_diff_angle = Math.cos (diff_angle);
    sin_z = Math.sin (z); cos_z = Math.cos (z);
    lat[0] = Math.asin (sin_lat_o*cos_z + cos_lat_o*sin_z*cos_diff_angle);
    lon[0] = adjust_lon (lon_center + Math.atan2 ((sin_z*sin_diff_angle), (cos_lat_o*
                     cos_z - sin_lat_o*sin_z*cos_diff_angle)));
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // OblatedEqualAreaProjection

////////////////////////////////////////////////////////////////////////
