////////////////////////////////////////////////////////////////////////
/*

     File: HammerProjection.java
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
import noaa.coastwatch.util.trans.ProjectionConstants;

/**
 * The <code>HammerProjection</code> class performs 
 * Hammer map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class HammerProjection 
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
    ptitle ("HAMMER");
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
  public HammerProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,             // (I) Center longitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (HAMMER, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // HammerProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double dlon;
    double fac;
    
    /*Forward equations
      -----------------*/
    dlon = adjust_lon (lon - lon_center);
    
    fac  = R*1.414213562 / Math.sqrt (1.0 + Math.cos (lat)*Math.cos (dlon / 2.0));
    x[0] = false_easting + fac*2.0*Math.cos (lat)*Math.sin (dlon / 2.0);
    y[0] = false_northing + fac*Math.sin (lat);
    
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
    double fac;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    fac = Math.sqrt (4.0*R*R - (x*x)/ 4.0 - y*y) / 2.0;
    lon[0] = adjust_lon (lon_center + 2.0*
                      Math.atan2 ((x*fac), (2.0*R*R - x*x/4 - y*y)));
    lat[0] = asinz (y*fac / R / R);
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // HammerProjection

////////////////////////////////////////////////////////////////////////
