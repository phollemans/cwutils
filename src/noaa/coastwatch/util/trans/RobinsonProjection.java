////////////////////////////////////////////////////////////////////////
/*

     File: RobinsonProjection.java
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
 * The <code>RobinsonProjection</code> class performs 
 * Robinson map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class RobinsonProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double R;                 // Radius of the earth (sphere)
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lon_center;        // Center longitude (projection center)
  private double pr[];
  private double xlr[];

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

    int i;
    
    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    R = r;
    lon_center = center_long;
    false_easting = false_east;
    false_northing = false_north;
    
             pr = new double[21];
             xlr = new double[21];
             pr[1]= -0.062;
             xlr[1]=0.9986;
             pr[2]=0.0;
             xlr[2]=1.0;
             pr[3]=0.062;
             xlr[3]=0.9986;
             pr[4]=0.124;
             xlr[4]=0.9954;
             pr[5]=0.186;
             xlr[5]=0.99;
             pr[6]=0.248;
             xlr[6]=0.9822;
             pr[7]=0.31;
             xlr[7]=0.973;
             pr[8]=0.372;
             xlr[8]=0.96;
             pr[9]=0.434;
             xlr[9]=0.9427;
             pr[10]=0.4958;
             xlr[10]=0.9216;
             pr[11]=0.5571;
             xlr[11]=0.8962;
             pr[12]=0.6176;
             xlr[12]=0.8679;
             pr[13]=0.6769;
             xlr[13]=0.835;
             pr[14]=0.7346;
             xlr[14]=0.7986;
             pr[15]=0.7903;
             xlr[15]=0.7597;
             pr[16]=0.8435;
             xlr[16]=0.7186;
             pr[17]=0.8936;
             xlr[17]=0.6732;
             pr[18]=0.9394;
             xlr[18]=0.6213;
             pr[19]=0.9761;
             xlr[19]=0.5722;
             pr[20]=1.0;
             xlr[20]=0.5322;
    
             for (i = 0; i < 21; i++)
                xlr[i]*= 0.9858;
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("ROBINSON");
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
  public RobinsonProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,             // (I) Center longitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (ROBIN, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // RobinsonProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double dlon;
    double p2;
    int ip1;
    
    /*Forward equations
      -----------------*/
    dlon = adjust_lon (lon - lon_center);
    p2 = Math.abs (lat / 5.0 / .01745329252);
    ip1 = (int) (p2 - EPSLN);
    
    /*Stirling's interpolation formula (using 2nd Diff.)
    ---------------------------------------------------*/
    p2 -= (double) ip1;
    x[0] = R*(xlr[ip1 + 2] + p2*(xlr[ip1 + 3] - xlr[ip1 + 1]) / 2.0 +
              p2*p2*(xlr[ip1 + 3] - 2.0*xlr[ip1 + 2] + xlr[ip1 + 1])/2.0)*
              dlon + false_easting;
    
    if (lat >= 0)
       y[0] = R*(pr[ip1 + 2] + p2*(pr[ip1 + 3] - pr[ip1 +1]) / 2.0 + p2*p2*
                (pr[ip1 + 3] - 2.0*pr[ip1 + 2] + pr[ip1 + 1]) / 2.0)*PI / 2.0 +
                false_northing;
    else
       y[0] = -R*(pr[ip1 + 2] + p2*(pr[ip1 + 3] - pr[ip1 +1]) / 2.0 + p2*p2*
                 (pr[ip1 + 3] - 2.0*pr[ip1 + 2] + pr[ip1 + 1]) / 2.0)*PI / 2.0 +
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

    double yy;
    double p2;
    double u, v, t, c;
    double phid;
    double temp;
    double y1;
    int ip1;
    int i;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    
    yy = 2.0*y / PI / R;
    phid = yy*90.0;
    p2 = Math.abs (phid / 5.0);
    ip1 = (int) (p2 - EPSLN);
    if (ip1 == 0)
       ip1 = 1;
    
    /*Stirling's interpolation formula as used in forward transformation is
       reversed for first estimation of LAT. from rectangular coordinates. LAT.
       is then adjusted by iteration until use of forward series provides correct
       value of Y within tolerance.
    ---------------------------------------------------------------------------*/
    for (i = 0;;)
       {
       u = pr[ip1 + 3] - pr[ip1 + 1];
       v = pr[ip1 + 3] - 2.0*pr[ip1 + 2] + pr[ip1 + 1];
       t = 2.0*(Math.abs (yy) - pr[ip1 + 2]) / u;
       c = v / u;
       p2 = t*(1.0 - c*t*(1.0 - 2.0*c*t));
    
       if ((p2 >= 0.0) || (ip1 == 1))
          {
          if (y >= 0)
             phid = (p2 + (double) ip1 )*5.0;
          else
             phid = -(p2 + (double) ip1 )*5.0;
    
          do
            {
            p2 = Math.abs (phid / 5.0);
            ip1 = (int) (p2 - EPSLN);
            p2 -= (double) ip1;
    
            if (y >= 0)
               y1 = R*(pr[ip1 +2] + p2*(pr[ip1 + 3] - pr[ip1 +1]) / 2.0 + p2
    *p2*(pr[ip1 + 3] - 2.0*pr[ip1 + 2] + pr[ip1 + 1])/2.0)
    *PI / 2.0;
            else
               y1 = -R*(pr[ip1 +2] + p2*(pr[ip1 + 3] - pr[ip1 +1]) / 2.0 + p2
    *p2*(pr[ip1 + 3] - 2.0*pr[ip1 + 2] + pr[ip1 + 1])/2.0)
    *PI / 2.0;
            phid += -180.0*(y1 - y) / PI / R;
            i++;
            if (i > 75)
               {
               p_error ("Too many iterations in inverse","robinv-conv");
               return (234);
               }
            }
          while (Math.abs (y1 - y) > .00001);
          break;
          }
       else
          {
          ip1 -= 1;
          if (ip1 < 0)
               {
               p_error ("Too many iterations in inverse","robinv-conv");
               return (234);
               }
          }
       }
    lat[0]  = phid*.01745329252;
    
    /*calculate  LONG. using final LAT. with transposed forward Stirling's
       interpolation formula.
    ---------------------------------------------------------------------*/
    lon[0] = lon_center + x / R / (xlr[ip1 + 2] + p2*(xlr[ip1 + 3] - xlr[ip1 + 1])
                          / 2.0 + p2*p2*(xlr[ip1 + 3] - 2.0*xlr[ip1 + 2] +
                          xlr[ip1 + 1]) / 2.0);
    lon[0] = adjust_lon (lon[0]);
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // RobinsonProjection

////////////////////////////////////////////////////////////////////////
