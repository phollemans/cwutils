////////////////////////////////////////////////////////////////////////
/*
     FILE: InterruptedMollweideProjection.java
  PURPOSE: Handles Interrupted Mollweide map transformations.
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
import noaa.coastwatch.util.trans.GCTPStyleProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;

/**
 * The <code>InterruptedMollweideProjection</code> class performs 
 * Interrupted Mollweide map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class InterruptedMollweideProjection 
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
    
    /*Initialize central meridians for each of the 6 regions
      ------------------------------------------------------*/
    lon_center = new double[6];
    lon_center[0] = 1.0471975512;                // 60.0 degrees
    lon_center[1] = -2.96705972839;                // -170.0 degrees
    lon_center[2] = -0.523598776;                // -30.0 degrees
    lon_center[3] =  1.57079632679;                // 90.0 degrees
    lon_center[4] = -2.44346095279;                // -140.0 degrees
    lon_center[5] = -0.34906585;                // -20.0 degrees
    
    /*Initialize false eastings for each of the 6 regions
      ---------------------------------------------------*/
    feast = new double[6];
    feast[0] = R*-2.19988776387;
    feast[1] = R*-0.15713484;
    feast[2] = R*2.04275292359;
    feast[3] = R*-1.72848324304;
    feast[4] = R*0.31426968;
    feast[5] = R*2.19988776387;
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("INTERRUPTED MOLLWEIDE EQUAL-AREA");
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
  public InterruptedMollweideProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (IMOLL, 0, rMajor, rMajor, dimensions, affine);
    long result = projinit (rMajor);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // InterruptedMollweideProjection constructor

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
    double con;
    int i;
    int region;
    
    /*Forward equations
      -----------------*/
    /*Note:  PI has been adjusted so that the correct region will be assigned
       when lon = 180 deg.
       ----------------------------------------------------------------------*/
    if (lat >= 0.0)
       {
       if (lon >= 0.34906585 && lon < 1.91986217719) region = 0;
       else if
         ((lon >= 1.919862177 && lon <= (PI + 1.0E-14)) ||
                                     (lon >= (-PI - 1.0E-14) && lon < -1.745329252))
            region=1;
       else region = 2;
       }
    else
       {
       if (lon >= 0.34906585 && lon < 2.44346095279) region = 3;
       else if
         ((lon >= 2.44346095279 && lon <= (PI +1.0E-14)) ||
                                    (lon >= (-PI - 1.0E-14) && lon<-1.2217304764))
            region=4;
       else region = 5;
       }
    
    delta_lon = adjust_lon (lon - lon_center[region]);
    theta = lat;
    con = PI*Math.sin (lat);
    
    /*Iterate using the Newton-Raphson method to find theta
      -----------------------------------------------------*/
    for (i=0;;i++)
          {
          delta_theta = -(theta + Math.sin (theta) - con) / (1.0 + Math.cos (theta));
          theta += delta_theta;
          if (Math.abs (delta_theta) < EPSLN) break;
          if (i >= 50) p_error ("Iteration failed to converge","IntMoll-forward");
          }
    theta /= 2.0;
    
    /*If the latitude is 90 deg, force the x coordinate to be "0 + false easting"
       this is done here because of percision problems with "Math.cos (theta)"
       --------------------------------------------------------------------------*/
    if (PI / 2 - Math.abs (lat) < EPSLN)
       delta_lon = 0;
    x[0] = feast[region] + 0.900316316158*R*delta_lon*Math.cos (theta);
    y[0] = R*1.4142135623731*Math.sin (theta);
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double theta;
    double temp;
    int region;
    
    /*Inverse equations
      -----------------*/
    if (y >= 0.0)
       {
       if (x <= R*-1.41421356248) region = 0;
       else if (x <= R*0.942809042) region = 1;
       else region = 2;
       }
    else
       {
       if (x <= R*-0.942809042) region = 3;
       else if (x <= R*1.41421356248) region = 4;
       else region = 5;
       }
    x = x - feast[region];
    
    theta = Math.asin (y / (1.4142135623731*R));
    lon[0] = adjust_lon (lon_center[region] + (x / (0.900316316158*R*Math.cos (theta))));
    lat[0] = Math.asin ((2.0*theta + Math.sin (2.0*theta)) / PI);
    
    /*Are we in a interrupted area?  If so, return status code of IN_BREAK.
      ---------------------------------------------------------------------*/
    if (region == 0 && (lon[0] < 0.34906585 || lon[0] > 1.91986217719))return (IN_BREAK);
    if (region == 1 && ((lon[0] < 1.91986217719 && lon[0] > 0.34906585) ||
                  (lon[0] > -1.74532925199 && lon[0] < 0.34906585))) return (IN_BREAK);
    if (region == 2 && (lon[0] < -1.745329252 || lon[0] > 0.34906585)) return (IN_BREAK);
    if (region == 3 && (lon[0] < 0.34906585 || lon[0] > 2.44346095279))return (IN_BREAK);
    if (region == 4 && ((lon[0] < 2.44346095279 && lon[0] > 0.34906585) ||
                  (lon[0] > -1.2217304764 && lon[0] < 0.34906585))) return (IN_BREAK);
    if (region == 5 && (lon[0] < -1.2217304764 || lon[0]> 0.34906585))return (IN_BREAK);
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // InterruptedMollweideProjection

////////////////////////////////////////////////////////////////////////
