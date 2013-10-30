////////////////////////////////////////////////////////////////////////
/*
     FILE: HotineObliqueMercatorProjection.java
  PURPOSE: Handles Hotine Oblique Mercator map transformations.
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
 * The <code>HotineObliqueMercatorProjection</code> class performs 
 * Hotine Oblique Mercator map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class HotineObliqueMercatorProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double al;
  private double azimuth;
  private double bl;
  private double d;
  private double e,es;              // eccentricity constants
  private double el,u;
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_origin;        // center latitude
  private double lon_origin;        // center longitude
  private double r_major;           // major axis
  private double r_minor;           // minor axis
  private double scale_factor;      // scale factor
  private double sin_p20,cos_p20;   // sin and cos values
  private double sinaz,cosaz;
  private double singam,cosgam;
  private double ts;

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param r_min the minor axis.
   * @param scale_fact the scale factor.
   * @param azimuth the azimuth east of north.
   * @param lon_orig the longitude of origin.
   * @param lat_orig the center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   * @param lon1 the fist point to define central line.
   * @param lat1 the fist point to define central line.
   * @param lon2 the second point to define central line.
   * @param lat2 the second point to define central line.
   * @param mode the which format type A or B.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double r_min,
    double scale_fact,
    double azimuth,
    double lon_orig,
    double lat_orig,
    double false_east,
    double false_north,
    double lon1,
    double lat1,
    double lon2,
    double lat2,
    long   mode
  ) {

    double temp;                    // temporary variable
    double con, com;
    double ts;
    double ts1, ts2;
    double h, l;
    double j, p, dlon;
    double f = 0, g, gama;
    double sinphi, cosphi;
    
    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    r_major = r_maj;
    r_minor = r_min;
    scale_factor = scale_fact;
    lat_origin = lat_orig;
    false_northing = false_north;
    false_easting = false_east;
    
    temp = r_minor / r_major;
    es = 1.0 - Math.pow (temp, 2);
    e = Math.sqrt (es);
    
    sin_p20 = Math.sin (lat_origin); cos_p20 = Math.cos (lat_origin);
    con = 1.0 - es*sin_p20*sin_p20;
    com = Math.sqrt (1.0 - es);
    bl = Math.sqrt (1.0 + es*Math.pow (cos_p20,4.0)/(1.0 - es));
    al = r_major*bl*scale_factor*com / con;
    if (Math.abs (lat_origin) < EPSLN)
       {
       ts = 1.0;
       d = 1.0;
       el = 1.0;
       }
    else
       {
       ts = tsfnz (e, lat_origin, sin_p20);
       con = Math.sqrt (con);
       d = bl*com / (cos_p20*con);
       if ((d*d - 1.0) > 0.0)
          {
          if (lat_origin >= 0.0)
             f = d + Math.sqrt (d*d - 1.0);
          else
             f = d - Math.sqrt (d*d - 1.0);
          }
       else
          f = d;
       el = f*Math.pow (ts, bl);
       }
    
    /*Report parameters to the user that are the same for both formats
      ---------------------------------------------------------------*/
    ptitle ("OBLIQUE MERCATOR (HOTINE)");
    radius2 (r_major, r_minor);
    genrpt (scale_factor,"Scale Factor at C. Meridian:    ");
    offsetp (false_easting, false_northing);
    
    if (mode != 0)
       {
       g = .5*(f - 1.0/f);
       gama = asinz (Math.sin (azimuth) / d);
       lon_origin = lon_orig - asinz (g*Math.tan (gama))/bl;
    
       /*Report parameters common to format B
       -------------------------------------*/
       genrpt (azimuth*R2D,"Azimuth of Central Line:    ");
       cenlon (lon_origin);
       cenlat (lat_origin);
    
       con = Math.abs (lat_origin);
       if ((con > EPSLN) && (Math.abs (con - HALF_PI) > EPSLN))
          {
          singam = Math.sin (gama); cosgam = Math.cos (gama);
          sinaz = Math.sin (azimuth); cosaz = Math.cos (azimuth);
          if (lat_origin >= 0)
             u =  (al / bl)*Math.atan (Math.sqrt (d*d - 1.0)/cosaz);
          else
             u =  -(al / bl)*Math.atan (Math.sqrt (d*d - 1.0)/cosaz);
          }
       else
          {
          p_error ("Input data error","omer-init");
          return (201);
          }
       }
    else
       {
       sinphi = Math.sin (lat1);
       ts1 = tsfnz (e, lat1, sinphi);
       sinphi = Math.sin (lat2);
       ts2 = tsfnz (e, lat2, sinphi);
       h = Math.pow (ts1, bl);
       l = Math.pow (ts2, bl);
       f = el/h;
       g = .5*(f - 1.0/f);
       j = (el*el - l*h)/(el*el + l*h);
       p = (l - h) / (l + h);
       dlon = lon1 - lon2;
       if (dlon < -PI)
          lon2 = lon2 - 2.0*PI;
       if (dlon > PI)
          lon2 = lon2 + 2.0*PI;
       dlon = lon1 - lon2;
       lon_origin = .5*(lon1 + lon2) - Math.atan (j*Math.tan (.5*bl*dlon)/p)/bl;
       dlon  = adjust_lon (lon1 - lon_origin);
       gama = Math.atan (Math.sin (bl*dlon)/g);
       azimuth = asinz (d*Math.sin (gama));
    
       /*Report parameters common to format A
       -------------------------------------*/
       genrpt (lon1*R2D,"Longitude of First Point:    ");
       genrpt (lat1*R2D,"Latitude of First Point:    ");
       genrpt (lon2*R2D,"Longitude of Second Point:    ");
       genrpt (lat2*R2D,"Latitude of Second Point:    ");
    
       if (Math.abs (lat1 - lat2) <= EPSLN)
          {
          p_error ("Input data error","omer-init");
          return (202);
          }
       else
          con = Math.abs (lat1);
       if ((con <= EPSLN) || (Math.abs (con - HALF_PI) <= EPSLN))
          {
          p_error ("Input data error","omer-init");
          return (202);
          }
       else
       if (Math.abs (Math.abs (lat_origin) - HALF_PI) <= EPSLN)
          {
          p_error ("Input data error","omer-init");
          return (202);
          }
    
       singam = Math.sin (gama); cosgam = Math.cos (gama);
       sinaz = Math.sin (azimuth); cosaz = Math.cos (azimuth);
       if (lat_origin >= 0)
          u =  (al/bl)*Math.atan (Math.sqrt (d*d - 1.0)/cosaz);
       else
          u = -(al/bl)*Math.atan (Math.sqrt (d*d - 1.0)/cosaz);
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
   * @param scale_fact the scale factor.
   * @param azimuth the azimuth east of north.
   * @param lon_orig the longitude of origin.
   * @param lat_orig the center latitude.
   * @param lon1 the fist point to define central line.
   * @param lat1 the fist point to define central line.
   * @param lon2 the second point to define central line.
   * @param lat2 the second point to define central line.
   * @param mode the which format type A or B.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public HotineObliqueMercatorProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double scale_fact,              // scale factor
    double azimuth,                 // azimuth east of north
    double lon_orig,                // longitude of origin
    double lat_orig,                // center latitude
    double lon1,                    // fist point to define central line
    double lat1,                    // fist point to define central line
    double lon2,                    // second point to define central line
    double lat2,                    // second point to define central line
    long   mode,                    // which format type A or B
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (HOM, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, scale_fact, azimuth, lon_orig,
      lat_orig, falseEast, falseNorth, lon1, lat1, lon2, lat2, mode);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // HotineObliqueMercatorProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double theta;                   // angle
    double sin_phi, cos_phi;// sin and cos value
    double b;                       // temporary values
    double c, t, tq;                // temporary values
    double con, n, ml;              // cone constant, small m
    double q, us, vl;
    double ul, vs;
    double s;
    double dlon;
    double ts1;
    
    /*Forward equations
      -----------------*/
    sin_phi = Math.sin (lat);
    dlon = adjust_lon (lon - lon_origin);
    vl = Math.sin (bl*dlon);
    if (Math.abs (Math.abs (lat) - HALF_PI) > EPSLN)
       {
       ts1 = tsfnz (e, lat, sin_phi);
       q = el / (Math.pow (ts1, bl));
       s = .5*(q - 1.0 / q);
       t = .5*(q + 1.0/ q);
       ul = (s*singam - vl*cosgam) / t;
       con = Math.cos (bl*dlon);
       if (Math.abs (con) < .0000001)
          {
          us = al*bl*dlon;
          }
       else
          {
          us = al*Math.atan ((s*cosgam + vl*singam) / con)/bl;
          if (con < 0)
             us = us + PI*al / bl;
          }
       }
    else
       {
       if (lat >= 0)
          ul = singam;
       else
          ul = -singam;
       us = al*lat / bl;
       }
    if (Math.abs (Math.abs (ul) - 1.0) <= EPSLN)
       {
       p_error ("Point projects into infinity","omer-for");
       return (205);
       }
    vs = .5*al*Math.log ((1.0 - ul)/(1.0 + ul)) / bl;
    us = us - u;
    x[0] = false_easting + vs*cosaz + us*sinaz;
    y[0] = false_northing + us*cosaz - vs*sinaz;
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double delta_lon;               // Delta longitude (Given longitude - center
    double theta;                   // angle
    double delta_theta;             // adjusted longitude
    double sin_phi, cos_phi;// sin and cos value
    double b;                       // temporary values
    double c, t, tq;                // temporary values
    double con, n, ml;              // cone constant, small m
    double vs, us, q, s, ts1;
    double vl, ul, bs;
    double dlon;
    long flag[];
    
    /*Inverse equations
      -----------------*/
    x -= false_easting;
    y -= false_northing;
    flag = new long[1];
    vs = x*cosaz - y*sinaz;
    us = y*cosaz + x*sinaz;
    us = us + u;
    q = Math.exp (-bl*vs / al);
    s = .5*(q - 1.0/q);
    t = .5*(q + 1.0/q);
    vl = Math.sin (bl*us / al);
    ul = (vl*cosgam + s*singam)/t;
    if (Math.abs (Math.abs (ul) - 1.0) <= EPSLN)
       {
       lon[0] = lon_origin;
       if (ul >= 0.0)
          lat[0] = HALF_PI;
       else
          lat[0] = -HALF_PI;
       }
    else
       {
       con = 1.0 / bl;
       ts1 = Math.pow ((el / Math.sqrt ((1.0 + ul) / (1.0 - ul))), con);
       lat[0] = phi2z (e, ts1, flag);
       if (flag[0] != 0)
          return (flag[0]);
       con = Math.cos (bl*us /al);
       theta = lon_origin - Math.atan2 ((s*cosgam - vl*singam) , con)/bl;
       lon[0] = adjust_lon (theta);
       }
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // HotineObliqueMercatorProjection

////////////////////////////////////////////////////////////////////////
