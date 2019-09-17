////////////////////////////////////////////////////////////////////////
/*

     File: AzimuthalEquidistantProjection.java
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
 * The <code>AzimuthalEquidistantProjection</code> class performs 
 * Azimuthal Equidistant map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class AzimuthalEquidistantProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double cos_p12;           // cos of center latitude
  private double e,es,esp;          // eccentricity constants
  private double e0,e1,e2,e3;       // eccentricity constants
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_origin;        // center latitude
  private double lon_center;        // Center longitude (projection center)
  private double r_major;           // major axis
  private double sin_p12;           // sin of center latitude

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
    
    sin_p12 = Math.sin (center_lat); cos_p12 = Math.cos (center_lat);
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("AZIMUTHAL EQUIDISTANT");
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
  public AzimuthalEquidistantProjection (
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
    super (AZMEQD, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_lon, center_lat,
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // AzimuthalEquidistantProjection constructor

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
    double con;                     // radius of circle
    double z;                       // angle
    char mess[] = new char[80];     // error message buffer
    
    /*Forward equations
      -----------------*/
    dlon = adjust_lon (lon - lon_center);
    sinphi = Math.sin (lat); cosphi = Math.cos (lat);
    coslon = Math.cos (dlon);
    g = sin_p12*sinphi + cos_p12*cosphi*coslon;
    if (Math.abs (Math.abs (g) - 1.0) < EPSLN)
       {
       ksp = 1.0;
       if (g < 0.0)
         {
         con = 2.0*HALF_PI*r_major;
         sprintf (mess,"Point projects into a circle of radius = %12.2f", con);
         p_error (mess,"azim-for");
         return (123);
         }
       }
    else
       {
       z = Math.acos (g);
       ksp = z/ Math.sin (z);
       }
    x[0] = false_easting + r_major*ksp*cosphi*Math.sin (dlon);
    y[0] = false_northing + r_major*ksp*(cos_p12*sinphi - sin_p12*
                                    cosphi*coslon);
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
    if (rh > (2.0*HALF_PI*r_major))
       {
       p_error ("Input data error","azim-inv");
       return (125);
       }
    z = rh / r_major;
    sinz = Math.sin (z); cosz = Math.cos (z);
    lon[0] = lon_center;
    if (Math.abs (rh) <= EPSLN)
       {
       lat[0] = lat_origin;
       return (OK);
       }
    lat[0] = asinz (cosz*sin_p12 + (y*sinz*cos_p12) / rh);
    con = Math.abs (lat_origin) - HALF_PI;
    if (Math.abs (con) <= EPSLN)
       {
       if (lat_origin >= 0.0)
          {
          lon[0] = adjust_lon (lon_center + Math.atan2 (x , -y));
          return (OK);
          }
       else
          {
          lon[0] = adjust_lon (lon_center - Math.atan2 (-x , y));
          return (OK);
          }
       }
    con = cosz - sin_p12*Math.sin (lat[0]);
    if ((Math.abs (con) < EPSLN) && (Math.abs (x) < EPSLN))
       return (OK);
    temp = Math.atan2 ((x*sinz*cos_p12), (con*rh));
    lon[0] = adjust_lon (lon_center + Math.atan2 ((x*sinz*cos_p12), (con*rh)));
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // AzimuthalEquidistantProjection

////////////////////////////////////////////////////////////////////////
