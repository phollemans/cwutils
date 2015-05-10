////////////////////////////////////////////////////////////////////////
/*
     FILE: GnomonicProjection.java
  PURPOSE: Handles Gnomonic map transformations.
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
 * The <code>GnomonicProjection</code> class performs 
 * Gnomonic map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class GnomonicProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double R;                 // Radius of the earth (sphere)
  private double cos_p13;           // Cosine of the center latitude
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_center;        // Center latitude (projection center)
  private double lon_center;        // Center longitude (projection center)
  private double sin_p13;           // Sine of the center latitude

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r the (I) Radius of the earth (sphere).
   * @param center_long the (I) Center longitude.
   * @param center_lat the (I) Center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r,
    double center_long,
    double center_lat,
    double false_east,
    double false_north
  ) {

    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    R = r;
    lon_center = center_long;
    lat_center = center_lat;
    false_easting = false_east;
    false_northing = false_north;
    sin_p13 = Math.sin (center_lat); cos_p13 = Math.cos (center_lat);
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("GNOMONIC");
    radius (r);
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
  public GnomonicProjection (
    double rMajor,
    int[] dimensions,
    AffineTransform affine,
    double center_long,             // (I) Center longitude
    double center_lat,              // (I) Center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (GNOMON, 0, rMajor, rMajor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, center_long, center_lat, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // GnomonicProjection constructor

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
    g = sin_p13*sinphi + cos_p13*cosphi*coslon;
    if (g <= 0.0)
       {
       p_error ("Point projects into infinity","gnomfor-conv");
       return (133);
       }
    ksp = 1.0 / g;
    x[0] = false_easting + R*ksp*cosphi*Math.sin (dlon);
    y[0] = false_northing + R*ksp*(cos_p13*sinphi - sin_p13*cosphi*
                    coslon);
    
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
    double z, sinz, cosz;
    double con;
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    rh = Math.sqrt (x*x + y*y);
    z = Math.atan (rh / R);
    sinz = Math.sin (z); cosz = Math.cos (z);
    lon[0] = lon_center;
    
    if (Math.abs (rh) <= EPSLN)
      {
      lat[0] = lat_center;
      return (OK);
      }
    lat[0] = asinz (cosz*sin_p13 + (y*sinz*cos_p13) / rh);
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
    con = cosz - sin_p13*Math.sin (lat[0]);
    if ((Math.abs (con) < EPSLN) && (Math.abs (x) < EPSLN))
       return (OK);
    lon[0] = adjust_lon (lon_center + Math.atan2 ((x*sinz*cos_p13), (con*rh)));
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // GnomonicProjection

////////////////////////////////////////////////////////////////////////
