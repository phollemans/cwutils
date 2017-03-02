////////////////////////////////////////////////////////////////////////
/*

     File: InterruptedGoodeHomolosineProjection.java
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
 * The <code>InterruptedGoodeHomolosineProjection</code> class performs 
 * Interrupted Goode Homolosine map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class InterruptedGoodeHomolosineProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double R;                 // Radius of the earth (sphere)
  private double feast[];           // False easting, one for each region
  private double lon_center[];      // Central meridians, one for each region

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r the (I) Radius of the earth (sphere).
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r
  ) {

    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    R = r;
    
    /*Initialize central meridians for each of the 12 regions
      -------------------------------------------------------*/
    lon_center = new double[12];
    lon_center[0] = -1.74532925199;                // -100.0 degrees
    lon_center[1] = -1.74532925199;                // -100.0 degrees
    lon_center[2] =  0.523598775598;        // 30.0 degrees
    lon_center[3] =  0.523598775598;        // 30.0 degrees
    lon_center[4] = -2.79252680319;                // -160.0 degrees
    lon_center[5] = -1.0471975512;                // -60.0 degrees
    lon_center[6] = -2.79252680319;                // -160.0 degrees
    lon_center[7] = -1.0471975512;                // -60.0 degrees
    lon_center[8] =  0.349065850399;        // 20.0 degrees
    lon_center[9] =  2.44346095279;                // 140.0 degrees
    lon_center[10] = 0.349065850399;        // 20.0 degrees
    lon_center[11] = 2.44346095279;                // 140.0 degrees
    
    /*Initialize false eastings for each of the 12 regions
      ----------------------------------------------------*/
    feast = new double[12];
    feast[0] = R*-1.74532925199;
    feast[1] = R*-1.74532925199;
    feast[2] = R*0.523598775598;
    feast[3] = R*0.523598775598;
    feast[4] = R*-2.79252680319;
    feast[5] = R*-1.0471975512;
    feast[6] = R*-2.79252680319;
    feast[7] = R*-1.0471975512;
    feast[8] = R*0.349065850399;
    feast[9] = R*2.44346095279;
    feast[10] = R*0.349065850399;
    feast[11] = R*2.44346095279;
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("GOODE'S HOMOLOSINE EQUAL-AREA");
    radius (r);
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
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public InterruptedGoodeHomolosineProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (GOOD, 0, rMajor, rMajor, dimensions, affine);
    long result = projinit (rMajor);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // InterruptedGoodeHomolosineProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double delta_lon;               // Delta longitude (Given longitude - center
    double theta;
    double delta_theta;
    double constant;
    int i;
    int region;
    
    /*Forward equations
      -----------------*/
    if (lat >= 0.710987989993)                     // if on or above 40 44' 11.8"
       {
       if (lon <= -0.698131700798) region = 0;   // If to the left of -40
       else region = 2;
       }
    else if (lat >= 0.0)                             // Between 0.0 and 40 44' 11.8"
       {
       if (lon <= -0.698131700798) region = 1;   // If to the left of -40
       else region = 3;
       }
    else if (lat >= -0.710987989993)                // Between 0.0 & -40 44' 11.8"
       {
       if (lon <= -1.74532925199) region = 4;          // If between -180 and -100
       else if (lon <= -0.349065850399) region = 5;        // If between -100 and -20
       else if (lon <= 1.3962634016) region = 8;        // If between -20 and 80
       else region = 9;                                // If between 80 and 180
       }
    else                            // Below -40 44'
       {
       if (lon <= -1.74532925199) region = 6;       // If between -180 and -100
       else if (lon <= -0.349065850399) region = 7;     // If between -100 and -20
       else if (lon <= 1.3962634016) region = 10;   // If between -20 and 80
       else region = 11;                            // If between 80 and 180
       }
    
    if (region==1||region==3||region==4||region==5||region==8||region==9)
       {
       delta_lon = adjust_lon (lon - lon_center[region]);
       x[0] = feast[region] + R*delta_lon*Math.cos (lat);
       y[0] = R*lat;
       }
    else
       {
       delta_lon = adjust_lon (lon - lon_center[region]);
       theta = lat;
       constant = PI*Math.sin (lat);
    
    /*Iterate using the Newton-Raphson method to find theta
      -----------------------------------------------------*/
       for (i=0;;i++)
          {
          delta_theta = -(theta + Math.sin (theta) - constant) / (1.0 + Math.cos (theta));
          theta += delta_theta;
          if (Math.abs (delta_theta) < EPSLN) break;
          if (i >= 50)
             {
             p_error ("Iteration failed to converge","goode-forward");
             return (251);
             }
          }
       theta /= 2.0;
    
       /*If the latitude is 90 deg, force the x coordinate to be
          "0 + false easting" this is done here because of precision problems
          with "Math.cos (theta)"
          ------------------------------------------------------------------*/
       if (PI / 2 - Math.abs (lat) < EPSLN)
          delta_lon = 0;
       x[0] = feast[region] + 0.900316316158*R*delta_lon*Math.cos (theta);
       y[0] = R*(1.4142135623731*Math.sin (theta) - 0.0528035274542*sign (lat));
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

    double arg;
    double theta;
    double temp;
    int region;
    
    /*Inverse equations
      -----------------*/
    if (y >= R*0.710987989993)                 // if on or above 40 44' 11.8"
       {
       if (x <= R*-0.698131700798) region = 0; // If to the left of -40
       else region = 2;
       }
    else if (y >= 0.0)                           // Between 0.0 and 40 44' 11.8"
       {
       if (x <= R*-0.698131700798) region = 1; // If to the left of -40
       else region = 3;
       }
    else if (y >= R*-0.710987989993)           // Between 0.0 & -40 44' 11.8"
       {
       if (x <= R*-1.74532925199) region = 4;     // If between -180 and -100
       else if (x <= R*-0.349065850399) region = 5; // If between -100 and -20
       else if (x <= R*1.3962634016) region = 8;  // If between -20 and 80
       else region = 9;                             // If between 80 and 180
       }
    else                            // Below -40 44' 11.8"
       {
       if (x <= R*-1.74532925199) region = 6;     // If between -180 and -100
       else if (x <= R*-0.349065850399) region = 7; // If between -100 and -20
       else if (x <= R*1.3962634016) region = 10; // If between -20 and 80
       else region = 11;                            // If between 80 and 180
       }
    x = x - feast[region];
    
    if (region==1||region==3||region==4||region==5||region==8||region==9)
       {
       lat[0] = y / R;
       if (Math.abs (lat[0]) > HALF_PI)
          {
          p_error ("Input data error","goode-inverse");
          return (252);
          }
       temp = Math.abs (lat[0]) - HALF_PI;
       if (Math.abs (temp) > EPSLN)
          {
          temp = lon_center[region] + x / (R*Math.cos (lat[0]));
          lon[0] = adjust_lon (temp);
          }
       else lon[0] = lon_center[region];
       }
    else
       {
       arg = (y + 0.0528035274542*R*sign (y)) /  (1.4142135623731*R);
       if (Math.abs (arg) > 1.0) return (IN_BREAK);
       theta = Math.asin (arg);
       lon[0] = lon_center[region]+(x/(0.900316316158*R*Math.cos (theta)));
       if (lon[0] < -(PI + EPSLN)) return (IN_BREAK);
       arg = (2.0*theta + Math.sin (2.0*theta)) / PI;
       if (Math.abs (arg) > 1.0) return (IN_BREAK);
       lat[0] = Math.asin (arg);
       }
    /*because of precision problems, long values of 180 deg and -180 deg
       may be mixed.
       ----------------------------------------------------------------*/
    if (((x < 0) && (PI - lon[0] < EPSLN)) || ((x > 0) && (PI + lon[0] < EPSLN)))
       lon[0] = -(lon[0]);
    
    /*Are we in a interrupted area?  If so, return status code of IN_BREAK.
      ---------------------------------------------------------------------*/
    if (region == 0 && (lon[0] < -(PI + EPSLN) || lon[0] > -0.698131700798))
                                                            return (IN_BREAK);
    if (region == 1 && (lon[0] < -(PI + EPSLN) || lon[0] > -0.698131700798))
                                                            return (IN_BREAK);
    if (region == 2 && (lon[0] < -0.698131700798 || lon[0] > PI + EPSLN))
                                                            return (IN_BREAK);
    if (region == 3 && (lon[0] < -0.698131700798 || lon[0] > PI + EPSLN))
                                                            return (IN_BREAK);
    if (region == 4 && (lon[0] < -(PI + EPSLN) || lon[0] > -1.74532925199))
                                                            return (IN_BREAK);
    if (region == 5 && (lon[0] < -1.74532925199 || lon[0] > -0.349065850399))
                                                            return (IN_BREAK);
    if (region == 6 && (lon[0] < -(PI + EPSLN) || lon[0] > -1.74532925199))
                                                            return (IN_BREAK);
    if (region == 7 && (lon[0] < -1.74532925199 || lon[0] > -0.349065850399))
                                                            return (IN_BREAK);
    if (region == 8 && (lon[0] < -0.349065850399 || lon[0] > 1.3962634016))
                                                            return (IN_BREAK);
    if (region == 9 && (lon[0] < 1.3962634016|| lon[0] > PI + EPSLN))
                                                            return (IN_BREAK);
    if (region ==10 && (lon[0] < -0.349065850399 || lon[0] > 1.3962634016))
                                                            return (IN_BREAK);
    if (region ==11 && (lon[0] < 1.3962634016 || lon[0] > PI + EPSLN))
                                                            return (IN_BREAK);
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // InterruptedGoodeHomolosineProjection

////////////////////////////////////////////////////////////////////////
