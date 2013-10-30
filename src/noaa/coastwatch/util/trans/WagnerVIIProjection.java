////////////////////////////////////////////////////////////////////////
/*
     FILE: WagnerVIIProjection.java
  PURPOSE: Handles Wagner VII map transformations.
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
 * The <code>WagnerVIIProjection</code> class performs 
 * Wagner VII map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class WagnerVIIProjection 
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
    ptitle ("WAGNER VII");
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
  public WagnerVIIProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,             // (I) Center longitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (WAGVII, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // WagnerVIIProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double delta_lon;               // Delta longitude (Given longitude - center
    double sin_lon, cos_lon;
    double s, c0, c1;
    
    /*Forward equations
      -----------------*/
    delta_lon = adjust_lon (lon - lon_center);
    sin_lon = Math.sin (delta_lon/3.0);
    cos_lon = Math.cos (delta_lon/3.0);
    s = 0.90631*Math.sin (lat);
    c0 = Math.sqrt (1-s*s);
    c1 = Math.sqrt (2.0 / (1.0 + c0*cos_lon));
    x[0] = 2.66723*R*c0*c1*sin_lon + false_easting;
    y[0] = 1.24104*R*s*c1 + false_northing;
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double temp;                    // Re-used temporary variable
    double t1, t2, p, c;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    t1 = x / 2.66723;
    t2 = y / 1.24104;
    t1*= t1;
    t2*= t2;
    p = Math.sqrt (t1 + t2);
    c = 2.0*asinz (p / (2.0*R));
    lat[0] = asinz (y*Math.sin (c) / (1.24104*0.90631*p));
    lon[0] = adjust_lon (lon_center + 3.0*Math.atan2 (x*Math.tan (c), 2.66723*p));
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // WagnerVIIProjection

////////////////////////////////////////////////////////////////////////
