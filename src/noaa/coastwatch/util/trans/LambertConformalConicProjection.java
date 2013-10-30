////////////////////////////////////////////////////////////////////////
/*
     FILE: LambertConformalConicProjection.java
  PURPOSE: Handles Lambert Conformal Conic map transformations.
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
 * The <code>LambertConformalConicProjection</code> class performs 
 * Lambert Conformal Conic map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class LambertConformalConicProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double center_lat;        // cetner latitude
  private double center_lon;        // center longituted
  private double e;                 // eccentricity
  private double es;                // eccentricity squared
  private double f0;                // flattening of ellipsoid
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double ns;                // ratio of angle between meridian
  private double r_major;           // major axis
  private double r_minor;           // minor axis
  private double rh;                // height above ellipsoid

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param r_min the minor axis.
   * @param lat1 the first standard parallel.
   * @param lat2 the second standard parallel.
   * @param c_lon the center longitude.
   * @param c_lat the center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double r_min,
    double lat1,
    double lat2,
    double c_lon,
    double c_lat,
    double false_east,
    double false_north
  ) {

    double sin_po;                  // sin value
    double cos_po;                  // cos value
    double con;                     // temporary variable
    double ms1;                     // small m 1
    double ms2;                     // small m 2
    double temp;                    // temporary variable
    double ts0;                     // small t 0
    double ts1;                     // small t 1
    double ts2;                     // small t 2
    r_major = r_maj;
    r_minor = r_min;
    false_northing = false_north;
    false_easting = false_east;
    
    /*Standard Parallels cannot be equal and on opposite sides of the equator
    ------------------------------------------------------------------------*/
    if (Math.abs (lat1+lat2) < EPSLN)
       {
       p_error ("Equal latitudes for St. Parallels on opposite sides of equator",
               "lamcc-for");
       return (41);
       }
    
    temp = r_minor / r_major;
    es = 1.0 - Math.pow (temp, 2);
    e = Math.sqrt (es);
    
    center_lon = c_lon;
    center_lat = c_lat;
    sin_po = Math.sin (lat1); cos_po = Math.cos (lat1);
    con = sin_po;
    ms1 = msfnz (e, sin_po, cos_po);
    ts1 = tsfnz (e, lat1, sin_po);
    sin_po = Math.sin (lat2); cos_po = Math.cos (lat2);
    ms2 = msfnz (e, sin_po, cos_po);
    ts2 = tsfnz (e, lat2, sin_po);
    sin_po = Math.sin (center_lat);
    ts0 = tsfnz (e, center_lat, sin_po);
    
    if (Math.abs (lat1 - lat2) > EPSLN)
        ns = Math.log (ms1/ms2)/ Math.log (ts1/ts2);
    else
        ns = con;
    f0 = ms1 / (ns*Math.pow (ts1, ns));
    rh = r_major*f0*Math.pow (ts0, ns);
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("LAMBERT CONFORMAL CONIC");
    radius2 (r_major, r_minor);
    stanparl (lat1, lat2);
    cenlonmer (center_lon);
    origin (c_lat);
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
   * @param lat1 the first standard parallel.
   * @param lat2 the second standard parallel.
   * @param c_lon the center longitude.
   * @param c_lat the center latitude.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public LambertConformalConicProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double lat1,                    // first standard parallel
    double lat2,                    // second standard parallel
    double c_lon,                   // center longitude
    double c_lat,                   // center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (LAMCC, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, lat1, lat2, c_lon, c_lat, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // LambertConformalConicProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double con;                     // temporary angle variable
    double rh1;                     // height above ellipsoid
    double sinphi;                  // sin value
    double theta;                   // angle
    double ts;                      // small value t
    
    con  = Math.abs ( Math.abs (lat) - HALF_PI);
    if (con > EPSLN)
      {
      sinphi = Math.sin (lat);
      ts = tsfnz (e, lat, sinphi);
      rh1 = r_major*f0*Math.pow (ts, ns);
      }
    else
      {
      con = lat*ns;
      if (con <= 0)
        {
        p_error ("Point can not be projected","lamcc-for");
        return (44);
        }
      rh1 = 0;
      }
    theta = ns*adjust_lon (lon - center_lon);
    x[0] = rh1*Math.sin (theta) + false_easting;
    y[0] = rh - rh1*Math.cos (theta) + false_northing;
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double rh1;                     // height above ellipsoid
    double con;                     // sign variable
    double ts;                      // small t
    double theta;                   // angle
    long flag[];                    // error flag
    
    flag = new long[1];
    x -= false_easting;
    y = rh - y + false_northing;
     if (ns > 0)
        {
        rh1 = Math.sqrt (x*x + y*y);
        con = 1.0;
        }
     else
        {
        rh1 = -Math.sqrt (x*x + y*y);
        con = -1.0;
        }
     theta = 0.0;
     if (rh1 != 0)
        theta = Math.atan2 ((con*x),(con*y));
     if ((rh1 != 0) || (ns > 0.0))
        {
        con = 1.0/ns;
        ts = Math.pow ((rh1/(r_major*f0)), con);
        lat[0] = phi2z (e, ts, flag);
        if (flag[0] != 0)
           return (flag[0]);
        }
     else
        lat[0] = -HALF_PI;
     lon[0] = adjust_lon (theta/ns + center_lon);
     return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // LambertConformalConicProjection

////////////////////////////////////////////////////////////////////////
