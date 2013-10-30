////////////////////////////////////////////////////////////////////////
/*
     FILE: EquirectangularProjection.java
  PURPOSE: Handles Equirectangular map transformations.
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
 * The <code>EquirectangularProjection</code> class performs 
 * Equirectangular map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class EquirectangularProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_origin;        // center latitude
  private double lon_center;        // Center longitude (projection center)
  private double r_major;           // major axis

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param center_lon the center longitude.
   * @param lat1 the latitude of true scale.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double center_lon,
    double lat1,
    double false_east,
    double false_north
  ) {

    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    r_major = r_maj;
    lon_center = center_lon;
    lat_origin = lat1;
    false_northing = false_north;
    false_easting = false_east;
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("EQUIRECTANGULAR");
    radius (r_major);
    cenlonmer (lon_center);
    origin (lat_origin);
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
   * @param center_lon the center longitude.
   * @param lat1 the latitude of true scale.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public EquirectangularProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_lon,              // center longitude
    double lat1,                    // latitude of true scale
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (EQRECT, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_lon, lat1, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // EquirectangularProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double sinphi, cosphi;          // sin and cos value
    double dlon;                    // delta longitude value
    double coslon;                  // cos of longitude
    double ksp;                     // scale factor
    double g;
    
    /*Forward equations
      -----------------*/
    dlon = adjust_lon (lon - lon_center);
    x[0] = false_easting + r_major*dlon*Math.cos (lat_origin);
    y[0] = false_northing + r_major*lat;
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double sinphi, cosphi;          // sin and cos value
    double dlon;                    // delta longitude value
    double coslon;                  // cos of longitude
    double ksp;                     // scale factor
    double g;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    lat[0] = y / r_major;
    if (Math.abs (lat[0]) > HALF_PI)
       {
       p_error ("Input data error","equi-inv");
       return (174);
       }
    lon[0] = adjust_lon (lon_center + x / (r_major*Math.cos (lat_origin)));
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // EquirectangularProjection

////////////////////////////////////////////////////////////////////////
