////////////////////////////////////////////////////////////////////////
/*

     File: GeneralVerticalNearsidePerspectiveProjection.java
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
 * The <code>GeneralVerticalNearsidePerspectiveProjection</code> class performs 
 * General Vertical Nearside Perspective map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class GeneralVerticalNearsidePerspectiveProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double R;                 // Radius of the earth (sphere)
  private double cos_p15;           // Cosine of the center latitude
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_center;        // Center latitude (projection center)
  private double lon_center;        // Center longitude (projection center)
  private double p;                 // Height above sphere
  private double sin_p15;           // Sine of the center latitude

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r the (I) Radius of the earth (sphere).
   * @param h the height above sphere.
   * @param center_long the (I) Center longitude.
   * @param center_lat the (I) Center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r,
    double h,
    double center_long,
    double center_lat,
    double false_east,
    double false_north
  ) {

    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    R = r;
    p = 1.0 + h / R;
    lon_center = center_long;
    lat_center = center_lat;
    false_easting = false_east;
    false_northing = false_north;
    sin_p15 = Math.sin (center_lat); cos_p15 = Math.cos (center_lat);
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("GENERAL VERTICAL NEAR-SIDE PERSPECTIVE");
    radius (r);
    genrpt (h,"Height of Point Above Surface of Sphere:   ");
    cenlon (center_long);
    cenlat (center_lat);
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
   * @param h the height above sphere.
   * @param center_long the (I) Center longitude.
   * @param center_lat the (I) Center latitude.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public GeneralVerticalNearsidePerspectiveProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double h,                       // height above sphere
    double center_long,             // (I) Center longitude
    double center_lat,              // (I) Center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (GVNSP, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, h, center_long, center_lat, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // GeneralVerticalNearsidePerspectiveProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double dlon;
    double sinphi, cosphi;
    double coslon;
    double g;
    double ksp;
    
    /*Forward equations
      -----------------*/
    dlon = adjust_lon (lon - lon_center);
    sinphi = Math.sin (lat); cosphi = Math.cos (lat);
    coslon = Math.cos (dlon);
    g = sin_p15*sinphi + cos_p15*cosphi*coslon;
    if (g < (1.0/ p))
       {
       p_error ("Point cannot be projected","gvnsp-for");
       return (153);
       }
    ksp = (p - 1.0)/(p - g);
    x[0] = false_easting + R*ksp*cosphi*Math.sin (dlon);
    y[0] = false_northing + R*ksp*(cos_p15*sinphi - sin_p15*cosphi*coslon);
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double rh;
    double r;
    double con;
    double com;
    double z, sinz, cosz;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    rh = Math.sqrt (x*x + y*y);
    r  = rh / R;
    con = p - 1.0;
    com = p + 1.0;
    if (r > Math.sqrt (con/com))
       {
       p_error ("Input data error","gvnsp-for");
       return (155);
       }
    sinz = (p - Math.sqrt (1.0 - (r*r*com) / con)) / (con / r + r/con);
    z = asinz (sinz);
    sinz = Math.sin (z); cosz = Math.cos (z);
    lon[0] = lon_center;
    if (Math.abs (rh) <= EPSLN)
       {
       lat[0] = lat_center;
       return (OK);
       }
    lat[0] = asinz (cosz*sin_p15 + ( y*sinz*cos_p15)/rh);
    con = Math.abs (lat_center) - HALF_PI;
    if (Math.abs (con) <= EPSLN)
       {
       if (lat_center >= 0.0)
          {
          lon[0] = adjust_lon (lon_center + Math.atan2 (x, -y));
          return (OK);
          }
       else
          {
          lon[0] = adjust_lon (lon_center - Math.atan2 (-x, y));
          return (OK);
          }
       }
    con = cosz - sin_p15*Math.sin (lat[0]);
    if ((Math.abs (con) < EPSLN) && (Math.abs (x) < EPSLN))
       return (OK);
    lon[0]  = adjust_lon (lon_center + Math.atan2 ((x*sinz*cos_p15), (con*rh)));
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // GeneralVerticalNearsidePerspectiveProjection

////////////////////////////////////////////////////////////////////////
