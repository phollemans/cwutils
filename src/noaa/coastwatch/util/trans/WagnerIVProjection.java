////////////////////////////////////////////////////////////////////////
/*
     FILE: WagnerIVProjection.java
  PURPOSE: Handles Wagner IV map transformations.
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
 * The <code>WagnerIVProjection</code> class performs 
 * Wagner IV map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class WagnerIVProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double R;                 // Radius of the earth (sphere)
  private double false_easting;     // x offset
  private double false_northing;    // y offset
  private double lon_center;        // Center longitude (projection center)

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r the (I) Radius of the earth (sphere).
   * @param center_long the (I) Center longitude.
   * @param false_east the x offset.
   * @param false_north the y offset.
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
    ptitle ("WAGNER IV");
    radius (r);
    cenlon (center_long);
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
  public WagnerIVProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,             // (I) Center longitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (WAGIV, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // WagnerIVProjection constructor

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
    long i;
    
    /*Forward equations
      -----------------*/
    delta_lon = adjust_lon (lon - lon_center);
    theta = lat;
    con = 2.9604205062*Math.sin (lat);
    
    /*Iterate using the Newton-Raphson method to find theta
      -----------------------------------------------------*/
    for (i=0;;i++)
       {
       delta_theta = -(theta + Math.sin (theta) - con) / (1.0 + Math.cos (theta));
       theta += delta_theta;
       if (Math.abs (delta_theta) < EPSLN) break;
       if (i >= 30) p_error ("Iteration failed to converge","wagneriv-forward");
       }
    theta /= 2.0;
    x[0] = 0.86310*R*delta_lon*Math.cos (theta) + false_easting;
    y[0] = 1.56548*R*Math.sin (theta) + false_northing;
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
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    theta = Math.asin (y /  (1.56548*R));
    lon[0] = adjust_lon (lon_center + (x / (0.86310*R*Math.cos (theta))));
    lat[0] = Math.asin ((2.0*theta + Math.sin (2.0*theta)) / 2.9604205062);
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // WagnerIVProjection

////////////////////////////////////////////////////////////////////////
