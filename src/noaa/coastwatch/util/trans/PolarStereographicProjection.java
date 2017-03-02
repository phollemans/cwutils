////////////////////////////////////////////////////////////////////////
/*

     File: PolarStereographicProjection.java
   Author: Peter Hollemans
     Date: 2012/11/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2012 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.trans.GCTPCStyleProjection;
import noaa.coastwatch.util.trans.GCTPStyleProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;

/**
 * The <code>PolarStereographicProjection</code> class performs 
 * Polar Stereographic map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class PolarStereographicProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double center_lat;        // center latitude
  private double center_lon;        // center longitude
  private double e4;                // e4 calculated from eccentricity
  private double e;                 // eccentricity
  private double es;                // eccentricity squared
  private double fac;               // sign variable
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double ind;               // flag variable
  private double mcs;               // small m
  private double r_major;           // major axis
  private double r_minor;           // minor axis
  private double tcs;               // small t

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param r_min the minor axis.
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
    double c_lon,
    double c_lat,
    double false_east,
    double false_north
  ) {

    double temp;                    // temporary variable
    double con1;                    // temporary angle
    double sinphi;                  // sin value
    double cosphi;                  // cos value
    
    r_major = r_maj;
    r_minor = r_min;
    false_northing = false_north;
    false_easting = false_east;
    temp = r_minor / r_major;
    es = 1.0 - Math.pow (temp, 2);
    e = Math.sqrt (es);
    e4 = e4fn (e);
    center_lon = c_lon;
    center_lat = c_lat;
    
    if (c_lat < 0)
       fac = -1.0;
    else
       fac = 1.0;
    ind = 0;
    if (Math.abs (Math.abs (c_lat) - HALF_PI) > EPSLN)
       {
       ind = 1;
       con1 = fac*center_lat;
       sinphi = Math.sin (con1); cosphi = Math.cos (con1);
       mcs = msfnz (e, sinphi, cosphi);
       tcs = tsfnz (e, con1, sinphi);
       }
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("POLAR STEREOGRAPHIC");
    radius2 (r_major, r_minor);
    cenlon (center_lon);
    offsetp (false_east, false_north);
    
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
  public PolarStereographicProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double c_lon,                   // center longitude
    double c_lat,                   // center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (PS, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, c_lon, c_lat, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // PolarStereographicProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double con1;                    // adjusted longitude
    double con2;                    // adjusted latitude
    double rh;                      // height above ellipsoid
    double sinphi;                  // sin value
    double ts;                      // value of small t
    
    con1 = fac*adjust_lon (lon - center_lon);
    con2 = fac*lat;
    sinphi = Math.sin (con2);
    ts = tsfnz (e, con2, sinphi);
    if (ind != 0)
       rh = r_major*mcs*ts / tcs;
    else
       rh = 2.0*r_major*ts / e4;
    x[0] = fac*rh*Math.sin (con1) + false_easting;
    y[0] = -fac*rh*Math.cos (con1) + false_northing;;
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double rh;                      // height above ellipsiod
    double ts;                      // small value t
    double temp;                    // temporary variable
    long flag[];                    // error flag
    
    flag = new long[1];
    x = (x - false_easting)*fac;
    y = (y - false_northing)*fac;
    rh = Math.sqrt (x*x + y*y);
    if (ind != 0)
      ts = rh*tcs/(r_major*mcs);
    else
      ts = rh*e4 / (r_major*2.0);
    lat[0] = fac*phi2z (e, ts, flag);
    if (flag[0] != 0)
       return (flag[0]);
    if (rh == 0)
       lon[0] = fac*center_lon;
    else
       {
       temp = Math.atan2 (x, -y);
       lon[0] = adjust_lon (fac*temp + center_lon);
       }
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // PolarStereographicProjection

////////////////////////////////////////////////////////////////////////
