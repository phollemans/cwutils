////////////////////////////////////////////////////////////////////////
/*
     FILE: MercatorProjection.java
  PURPOSE: Handles Mercator map transformations.
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
 * The <code>MercatorProjection</code> class performs 
 * Mercator map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class MercatorProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double e,es;              // eccentricity constants
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_origin;        // center latitude
  private double lon_center;        // Center longitude (projection center)
  private double m1;                // small value m
  private double r_major;           // major axis
  private double r_minor;           // minor axis

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param r_min the minor axis.
   * @param center_lon the center longitude.
   * @param center_lat the center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double r_min,
    double center_lon,
    double center_lat,
    double false_east,
    double false_north
  ) {

    double temp;                    // temporary variable
    
    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    r_major = r_maj;
    r_minor = r_min;
    lon_center = center_lon;
    lat_origin = center_lat;
    false_northing = false_north;
    false_easting = false_east;
    
    temp = r_minor / r_major;
    es = 1.0 - Math.pow (temp, 2);
    e = Math.sqrt (es);
    m1 = Math.cos (center_lat)/(Math.sqrt (1.0 - es*Math.sin (center_lat)*Math.sin (center_lat)));
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("MERCATOR");
    radius2 (r_major, r_minor);
    cenlonmer (lon_center);
    origin (lat_origin);
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
   * @param rMinor the semi-minor axis in meters.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   * @param center_lon the center longitude.
   * @param center_lat the center latitude.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public MercatorProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double center_lon,              // center longitude
    double center_lat,              // center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (MERCAT, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, center_lon, center_lat, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // MercatorProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double ts;                      // small t value
    double sinphi;                  // sin value
    
    /*Forward equations
      -----------------*/
    if (Math.abs (Math.abs (lat) - HALF_PI)  <= EPSLN)
       {
       p_error ("Transformation cannot be computed at the poles","mer-forward");
       return (53);
       }
    else
       {
       sinphi = Math.sin (lat);
       ts = tsfnz (e, lat, sinphi);
       x[0] = false_easting + r_major*m1*adjust_lon (lon - lon_center);
       y[0] = false_northing - r_major*m1*Math.log (ts);
       }
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double ts;                      // small t value
    double sin_phii;                // sin value
    long flag[];                    // error flag
    
    /*Inverse equations
      -----------------*/
    flag = new long[1];
    x -= false_easting;
    y -= false_northing;
    ts = Math.exp (-y/(r_major*m1));
    lat[0] = phi2z (e, ts, flag);
    if (flag[0] != 0)
       return (flag[0]);
    lon[0] = adjust_lon (lon_center + x/(r_major*m1));
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // MercatorProjection

////////////////////////////////////////////////////////////////////////
