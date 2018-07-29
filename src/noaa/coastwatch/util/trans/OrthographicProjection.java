////////////////////////////////////////////////////////////////////////
/*

     File: OrthographicProjection.java
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
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.GCTPCStyleProjection;
import noaa.coastwatch.util.trans.GCTPStyleProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.trans.SpheroidConstants;

/**
 * The <code>OrthographicProjection</code> class performs 
 * Orthographic map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class OrthographicProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double cos_p14;           // cos of center latitude
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_origin;        // center latitude
  private double lon_center;        // Center longitude (projection center)
  private double r_major;           // major axis
  private double sin_p14;           // sin of center latitude

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param center_lon the center longitude.
   * @param center_lat the center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double center_lon,
    double center_lat,
    double false_east,
    double false_north
  ) {

    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    r_major = r_maj;
    lon_center = center_lon;
    lat_origin = center_lat;
    false_northing = false_north;
    false_easting = false_east;
    
    sin_p14 = Math.sin (center_lat); cos_p14 = Math.cos (center_lat);
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("ORTHOGRAPHIC");
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
   * @param center_lat the center latitude.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public OrthographicProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_lon,              // center longitude
    double center_lat,              // center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (ORTHO, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_lon, center_lat, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // OrthographicProjection constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new orthographic projection using the 
   * specified projection center point.  The Earth is assumed to
   * be a sphere of radius 6370.997 km.
   * 
   * @param projCenter the earth location at the projection center.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param centerLoc the earth location at the map center.
   * @param pixelDims the pixel dimensions in meters at the projection
   * reference point as <code>[height, width]</code>.
   *
   * @throws NoninvertibleTransformException if the map projection to data
   * coordinate affine transform is not invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public OrthographicProjection (
    EarthLocation projCenter,
    int[] dimensions,
    EarthLocation centerLoc,
    double[] pixelDims
  ) throws NoninvertibleTransformException {

    this (SpheroidConstants.STD_RADIUS*1000, dimensions, new AffineTransform(),
      Math.toRadians (centerLoc.lon), Math.toRadians (centerLoc.lat), 0, 0);
    setAffine (centerLoc, pixelDims);

  } // OrthographicProjection constructor

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
    sinphi = Math.sin (lat); cosphi = Math.cos (lat);
    coslon = Math.cos (dlon);
    g = sin_p14*sinphi + cos_p14*cosphi*coslon;
    ksp = 1.0;
    if ((g > 0) || (Math.abs (g) <= EPSLN))
      {
      x[0] = false_easting + r_major*ksp*cosphi*Math.sin (dlon);
      y[0] = false_northing + r_major*ksp*(cos_p14*sinphi -
                                             sin_p14*cosphi*coslon);
      }
    else
      {
      p_error ("Point can not be projected","orth-for");
      return (143);
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

    double rh;                      // height above ellipsoid
    double z;                       // angle
    double sinz, cosz;              // sin of z and cos of z
    double temp;
    double con;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    rh = Math.sqrt (x*x + y*y);
    if (rh > r_major + .0000001)
       {
       p_error ("Input data error","orth-inv");
       return (145);
       }
    z = asinz (rh / r_major);
    sinz = Math.sin (z); cosz = Math.cos (z);
    lon[0] = lon_center;
    if (Math.abs (rh) <= EPSLN)
       {
       lat[0] = lat_origin;
       return (OK);
       }
    lat[0] = asinz (cosz*sin_p14 + (y*sinz*cos_p14)/rh);
    con = Math.abs (lat_origin) - HALF_PI;
    if (Math.abs (con) <= EPSLN)
       {
       if (lat_origin >= 0)
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
    con = cosz - sin_p14*Math.sin (lat[0]);
    if ((Math.abs (con) >= EPSLN) || (Math.abs (x) >= EPSLN))
       lon[0] = adjust_lon (lon_center + Math.atan2 ((x*sinz*cos_p14), (con*rh)));
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // OrthographicProjection

////////////////////////////////////////////////////////////////////////
