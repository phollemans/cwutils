////////////////////////////////////////////////////////////////////////
/*
     FILE: EquidistantConicProjection.java
  PURPOSE: Handles Equidistant Conic map transformations.
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
 * The <code>EquidistantConicProjection</code> class performs 
 * Equidistant Conic map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class EquidistantConicProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double e,es,esp;          // eccentricity constants
  private double e0,e1,e2,e3;       // eccentricity constants
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double g;
  private double lat_origin;        // center latitude
  private double lon_center;        // Center longitude (projection center)
  private double ml0;               // small value m
  private double ns;
  private double r_major;           // major axis
  private double r_minor;           // minor axis
  private double rh;

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param r_min the minor axis.
   * @param lat1 the latitude of standard parallel.
   * @param lat2 the latitude of standard parallel.
   * @param center_lon the center longitude.
   * @param center_lat the center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   * @param mode the which format is present A B.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double r_min,
    double lat1,
    double lat2,
    double center_lon,
    double center_lat,
    double false_east,
    double false_north,
    long   mode
  ) {

    double temp;                    // temporary variable
    double sinphi, cosphi;          // sin and cos values
    double ms1, ms2;
    double ml1, ml2;
    
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
    
    sinphi = Math.sin (lat1); cosphi = Math.cos (lat1);
    ms1 = msfnz (e, sinphi, cosphi);
    ml1 = mlfn (e0, e1, e2, e3, lat1);
    
    /*format B
    ---------*/
    if (mode != 0)
       {
       if (Math.abs (lat1 + lat2) < EPSLN)
          {
          p_error ("Standard Parallels on opposite sides of equator","eqcon_for");
          return (81);
          }
       sinphi = Math.sin (lat2); cosphi = Math.cos (lat2);
       ms2 = msfnz (e, sinphi, cosphi);
       ml2 = mlfn (e0, e1, e2, e3, lat2);
       if (Math.abs (lat1 - lat2) >= EPSLN)
          ns = (ms1 - ms2) / (ml2 - ml1);
       else
          ns = sinphi;
       }
    else
       ns = sinphi;
    g = ml1 + ms1/ns;
    ml0 = mlfn (e0, e1, e2, e3, center_lat);
    rh = r_major*(g - ml0);
    
    /*Report parameters to the user
      -----------------------------*/
    if (mode != 0)
       {
       ptitle ("EQUIDISTANT CONIC");
       radius2 (r_major, r_minor);
       stanparl (lat1, lat2);
       cenlonmer (lon_center);
       origin (center_lat);
       offsetp (false_easting, false_northing);
       }
    else
       {
       ptitle ("EQUIDISTANT CONIC");
       radius2 (r_major, r_minor);
       stparl1 (lat1);
       cenlonmer (lon_center);
       origin (center_lat);
       offsetp (false_easting, false_northing);
       }
    
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
   * @param lat1 the latitude of standard parallel.
   * @param lat2 the latitude of standard parallel.
   * @param center_lon the center longitude.
   * @param center_lat the center latitude.
   * @param mode the which format is present A B.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public EquidistantConicProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double lat1,                    // latitude of standard parallel
    double lat2,                    // latitude of standard parallel
    double center_lon,              // center longitude
    double center_lat,              // center latitude
    long   mode,                    // which format is present A B
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (EQUIDC, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, lat1, lat2, center_lon, center_lat, 
      falseEast, falseNorth, mode);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // EquidistantConicProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double ml;
    double theta;
    double rh1;
    
    /*Forward equations
      -----------------*/
    ml = mlfn (e0, e1, e2, e3, lat);
    rh1 = r_major*(g - ml);
    theta = ns*adjust_lon (lon - lon_center);
    x[0] = false_easting  + rh1*Math.sin (theta);
    y[0] = false_northing + rh - rh1*Math.cos (theta);
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double rh1;
    double ml;
    double con;
    double theta;
    long flag[];
    
    /*Inverse equations
      -----------------*/
    flag = new long[1];
    x -= false_easting;
    y  = rh - y + false_northing;
    if (ns >= 0)
       {
       rh1 = Math.sqrt (x*x + y*y);
       con = 1.0;
       }
    else
       {
       rh1 = -Math.sqrt (x*x + y*y);
       con = -1.0;
       }
    theta = 0.0;
    if (rh1  != 0.0)
       theta = Math.atan2 (con*x, con*y);
    ml = g - rh1 / r_major;
    lat[0] = phi3z (ml, e0, e1, e2, e3, flag);
    lon[0] = adjust_lon (lon_center + theta / ns);
    
    if (flag[0] != 0)
       return (flag[0]);
    else
       return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // EquidistantConicProjection

////////////////////////////////////////////////////////////////////////
