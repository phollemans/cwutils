////////////////////////////////////////////////////////////////////////
/*
     FILE: PolyconicProjection.java
  PURPOSE: Handles Polyconic map transformations.
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
import noaa.coastwatch.util.trans.ProjectionConstants;

/**
 * The <code>PolyconicProjection</code> class performs 
 * Polyconic map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class PolyconicProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double e,es,esp;          // eccentricity constants
  private double e0,e1,e2,e3;       // eccentricity constants
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_origin;        // center latitude
  private double lon_center;        // Center longitude (projection center)
  private double ml0;               // small value m
  private double r_major;           // major axis
  private double r_minor;           // minor axis

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param r_min the minor axis.
   * @param center_lon the center longitude.
   * @param center_lat the center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double r_min,
    double center_lon,
    double center_lat,
    double false_east,
    double false_north
  ) {

    double temp;                    // temporary variable
    
    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    r_major = r_maj;
    r_minor = r_min;
    lon_center = center_lon;
    lat_origin = center_lat;
    false_northing = false_north;
    false_easting = false_east;
    
    temp = r_minor / r_major;
    es = 1.0 - Math.pow (temp, 2);
    e = Math.sqrt (es);
    e0 = e0fn (es);
    e1 = e1fn (es);
    e2 = e2fn (es);
    e3 = e3fn (es);
    ml0 = mlfn (e0, e1, e2, e3, lat_origin);
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("POLYCONIC");
    radius2 (r_major, r_minor);
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
   * @param rMinor the semi-minor axis in meters.
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
  public PolyconicProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double center_lon,              // center longitude
    double center_lat,              // center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (POLYC, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, center_lon, center_lat, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // PolyconicProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double sinphi, cosphi;          // sin and cos value
    double al;                      // temporary values
    double c;                       // temporary values
    double con, ml;                 // cone constant, small m
    double ms;                      // small m
    
    /*Forward equations
      -----------------*/
    con = adjust_lon (lon - lon_center);
    if (Math.abs (lat) <= .0000001)
       {
       x[0] = false_easting + r_major*con;
       y[0] = false_northing - r_major*ml0;
       }
    else
       {
       sinphi = Math.sin (lat); cosphi = Math.cos (lat);
       ml = mlfn (e0, e1, e2, e3, lat);
       ms = msfnz (e, sinphi, cosphi);
       con*= sinphi;
       x[0] = false_easting + r_major*ms*Math.sin (con)/sinphi;
       y[0] = false_northing + r_major*(ml - ml0 + ms*(1.0 - Math.cos (con))/sinphi);
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

    double al;                      // temporary values
    double b;                       // temporary values
    double c[] = new double[1];     // temporary values
    long iflg;                      // error flag
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    al = ml0 + y/r_major;
    iflg = 0;
    if (Math.abs (al) <= .0000001)
       {
       lon[0] = x/r_major + lon_center;
       lat[0] = 0.0;
       }
    else
       {
       b = al*al + (x/r_major)*(x/r_major);
       iflg = phi4z (es, e0, e1, e2, e3, al, b, c, lat);
       if (iflg != OK)
          return (iflg);
       lon[0] = adjust_lon ((asinz (x*c[0] / r_major) / Math.sin (lat[0])) + lon_center);
       }
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // PolyconicProjection

////////////////////////////////////////////////////////////////////////
