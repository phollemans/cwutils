////////////////////////////////////////////////////////////////////////
/*

     File: VanderGrintenProjection.java
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
 * The <code>VanderGrintenProjection</code> class performs 
 * Vander Grinten map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class VanderGrintenProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double R;                 // Radius of the earth (sphere)
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lon_center;        // Center longitude (projection center)

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r the (I) Radius of the earth (sphere).
   * @param center_long the (I) Center longitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r,
    double center_long,
    double false_east,
    double false_north
  ) {

    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    R = r;
    lon_center = center_long;
    false_easting = false_east;
    false_northing = false_north;
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("VAN DER GRINTEN");
    radius (r);
    cenlon (center_long);
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
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public VanderGrintenProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,             // (I) Center longitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (VGRINT, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // VanderGrintenProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double dlon;
    double theta;
    double al, asq;
    double g, gsq;
    double m, msq;
    double con;
    double costh, sinth;
    double temp;
    
    /*Forward equations
      -----------------*/
    dlon = adjust_lon (lon  - lon_center);
    
    if (Math.abs (lat) <= EPSLN)
       {
       x[0] = false_easting  + R*dlon;
       y[0] = false_northing;
       return (OK);
       }
    theta = asinz (2.0*Math.abs (lat / PI));
    if ((Math.abs (dlon) <= EPSLN) || (Math.abs (Math.abs (lat) - HALF_PI) <= EPSLN))
       {
       x[0] = false_easting;
       if (lat >= 0)
          y[0] = false_northing + PI*R*Math.tan (.5*theta);
       else
          y[0] = false_northing + PI*R*(-Math.tan (.5*theta));
       return (OK);
       }
    al = .5*Math.abs ((PI / dlon) - (dlon / PI));
    asq = al*al;
    sinth = Math.sin (theta); costh = Math.cos (theta);
    g = costh / (sinth + costh - 1.0);
    gsq = g*g;
    m = g*(2.0 / sinth - 1.0);
    msq = m*m;
    con = PI*R*(al*(g - msq) + Math.sqrt (asq*(g - msq)*(g - msq) - (msq + asq)
       *(gsq - msq))) / (msq + asq);
    if (dlon < 0)
       con = -con;
    x[0] = false_easting + con;
    con = Math.abs (con / (PI*R));
    if (lat >= 0)
       y[0] = false_northing + PI*R*Math.sqrt (1.0 - con*con - 2.0*al*con);
    else
       y[0] = false_northing - PI*R*Math.sqrt (1.0 - con*con - 2.0*al*con);
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double dlon;
    double xx, yy, xys, c1, c2, c3;
    double al, asq;
    double a1;
    double m1;
    double con;
    double th1;
    double d;
    
    /*inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    con = PI*R;
    xx = x / con;
    yy = y / con;
    xys = xx*xx + yy*yy;
    c1 = -Math.abs (yy)*(1.0 + xys);
    c2 = c1 - 2.0*yy*yy + xx*xx;
    c3 = -2.0*c1 + 1.0 + 2.0*yy*yy + xys*xys;
    d = yy*yy / c3 + (2.0*c2*c2*c2 / c3 / c3 / c3 - 9.0*c1*c2 / c3 /c3)
        / 27.0;
    a1 = (c1 - c2*c2 / 3.0 / c3) / c3;
    m1 = 2.0*Math.sqrt ( -a1 / 3.0);
    if (Math.abs (m1) < EPSLN)
       lat[0] = 0;
    else
      {
      con = ((3.0*d) / a1) / m1;
      if (Math.abs (con) > 1.0)
        {
        if (con >= 0.0)
            con = 1.0;
        else
            con = -1.0;
        }
      th1 = Math.acos (con) / 3.0;
      if (y >= 0)
        lat[0] = (-m1*Math.cos (th1 + PI / 3.0) - c2 / 3.0 / c3)*PI;
      else
        lat[0] = -(-m1*Math.cos (th1 + PI / 3.0) - c2 / 3.0 / c3)*PI;
      }
    if (Math.abs (xx) < EPSLN)
       {
       lon[0] = lon_center;
       return (OK);
       }
    lon[0] = adjust_lon (lon_center + PI*(xys - 1.0 + Math.sqrt (1.0 + 2.0*
                     (xx*xx - yy*yy) + xys*xys)) / 2.0 / xx);
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // VanderGrintenProjection

////////////////////////////////////////////////////////////////////////
