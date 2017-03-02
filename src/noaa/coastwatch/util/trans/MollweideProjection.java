////////////////////////////////////////////////////////////////////////
/*

     File: MollweideProjection.java
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
 * The <code>MollweideProjection</code> class performs 
 * Mollweide map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class MollweideProjection 
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
    false_easting = false_east;
    false_northing = false_north;
    R = r;
    lon_center = center_long;
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("MOLLWEIDE");
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
  public MollweideProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,             // (I) Center longitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (MOLL, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // MollweideProjection constructor

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
    
    /*Forward equations
      -----------------*/
    delta_lon = adjust_lon (lon - lon_center);
    theta = lat;
    con = PI*Math.sin (lat);
    
    /*Iterate using the Newton-Raphson method to find theta
      -----------------------------------------------------*/
    for (i=0;;i++)
       {
       delta_theta = -(theta + Math.sin (theta) - con)/ (1.0 + Math.cos (theta));
       theta += delta_theta;
       if (Math.abs (delta_theta) < EPSLN) break;
       if (i >= 50)
         {
         p_error ("Iteration failed to converge","Mollweide-forward");
         return (241);
         }
       }
    theta /= 2.0;
    
    /*If the latitude is 90 deg, force the x coordinate to be "0 + false easting"
       this is done here because of precision problems with "Math.cos (theta)"
       --------------------------------------------------------------------------*/
    if (PI/2 - Math.abs (lat) < EPSLN)
       delta_lon =0;
    x[0] = 0.900316316158*R*delta_lon*Math.cos (theta) + false_easting;
    y[0] = 1.4142135623731*R*Math.sin (theta) + false_northing;
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
    double arg;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    arg = y /  (1.4142135623731*R);
    
    /*Because of division by zero problems, 'arg' can not be 1.0.  Therefore
       a number very close to one is used instead.
       -------------------------------------------------------------------*/
    if (Math.abs (arg) > 0.999999999999) arg=0.999999999999;
    theta = Math.asin (arg);
    lon[0] = adjust_lon (lon_center + (x / (0.900316316158*R*Math.cos (theta))));
    if (lon[0] < (-PI)) lon[0]= -PI;
    if (lon[0] > PI) lon[0]= PI;
    arg = (2.0*theta + Math.sin (2.0*theta)) / PI;
    if (Math.abs (arg) > 1.0)arg=1.0;
    lat[0] = Math.asin (arg);
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // MollweideProjection

////////////////////////////////////////////////////////////////////////
