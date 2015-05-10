////////////////////////////////////////////////////////////////////////
/*
     FILE: UniversalTransverseMercatorProjection.java
  PURPOSE: Handles Universal Transverse Mercator map transformations.
   AUTHOR: Peter Hollemans
     DATE: 2012/11/02
  CHANGES: 2013/09/23, PFH
           - change: modified call to super to pass actual zone (not zero)
           - issue: cwmaster reverting to 0 for zone on Apply

  CoastWatch Software Library and Utilities
  Copyright 2012-2013, USDOC/NOAA/NESDIS CoastWatch

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
 * The <code>UniversalTransverseMercatorProjection</code> class performs 
 * Universal Transverse Mercator map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class UniversalTransverseMercatorProjection 
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
  private double scale_factor;      // scale factor
  private long ind;                 // sphere flag value

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param r_min the minor axis.
   * @param scale_fact the scale factor.
   * @param zone the zone number.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double r_min,
    double scale_fact,
    long   zone
  ) {

    double temp;                    // temporary variable
    
    if ((Math.abs (zone) < 1) || (Math.abs (zone) > 60))
       {
       p_error ("Illegal zone number","utm-forint");
       return (11);
       }
    r_major = r_maj;
    r_minor = r_min;
    scale_factor = scale_fact;
    lat_origin = 0.0;
    lon_center = ((6*Math.abs (zone)) - 183)*D2R;
    false_easting = 500000.0;
    false_northing = (zone < 0) ? 10000000.0 : 0.0;
    
    temp = r_minor / r_major;
    es = 1.0 - Math.pow (temp, 2);
    e = Math.sqrt (es);
    e0 = e0fn (es);
    e1 = e1fn (es);
    e2 = e2fn (es);
    e3 = e3fn (es);
    ml0 = r_major*mlfn (e0, e1, e2, e3, lat_origin);
    esp = es / (1.0 - es);
    
    if (es < .00001)
       ind = 1;
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("UNIVERSAL TRANSVERSE MERCATOR (UTM)");
    genrpt_long (zone,   "Zone:     ");
    radius2 (r_major, r_minor);
    genrpt (scale_factor,"Scale Factor at C. Meridian:     ");
    cenlonmer (lon_center);
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
   * @param zone the zone number.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public UniversalTransverseMercatorProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double scale_fact,              // scale factor
    long   zone                     // zone number
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (UTM, (int) zone, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, scale_fact, zone);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // UniversalTransverseMercatorProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double delta_lon;               // Delta longitude (Given longitude - center
    double theta;                   // angle
    double delta_theta;             // adjusted longitude
    double sin_phi, cos_phi;// sin and cos value
    double al, als;                 // temporary values
    double b;                       // temporary values
    double c, t, tq;                // temporary values
    double con, n, ml;              // cone constant, small m
    
    /*Forward equations
      -----------------*/
    delta_lon = adjust_lon (lon - lon_center);
    sin_phi = Math.sin (lat); cos_phi = Math.cos (lat);
    
    /*This part was in the fortran code and is for the spherical form
    ----------------------------------------------------------------*/
    if (ind != 0)
      {
      b = cos_phi*Math.sin (delta_lon);
      if ((Math.abs (Math.abs (b) - 1.0)) < .0000000001)
         {
         p_error ("Point projects into infinity","utm-for");
         return (93);
         }
      else
         {
         x[0] = .5*r_major*scale_factor*Math.log ((1.0 + b)/(1.0 - b));
         con = Math.acos (cos_phi*Math.cos (delta_lon)/Math.sqrt (1.0 - b*b));
         if (lat < 0)
            con = - con;
         y[0] = r_major*scale_factor*(con - lat_origin);
         return (OK);
         }
      }
    
    al  = cos_phi*delta_lon;
    als = Math.pow (al, 2);
    c   = esp*Math.pow (cos_phi, 2);
    tq  = Math.tan (lat);
    t   = Math.pow (tq, 2);
    con = 1.0 - es*Math.pow (sin_phi, 2);
    n   = r_major / Math.sqrt (con);
    ml  = r_major*mlfn (e0, e1, e2, e3, lat);
    
    x[0] = scale_factor * n * al * (1.0 + als / 6.0 * (1.0 - t + c + als / 20.0 *
      (5.0 - 18.0 * t + (t*t) + 72.0 * c - 58.0 * esp))) + false_easting;

    y[0] = scale_factor * (ml - ml0 + n * tq * (als * (0.5 + als / 24.0 *
      (5.0 - t + 9.0 * c + 4.0 * (c*c) + als / 30.0 * (61.0 - 58.0 * t
      + (t*t) + 600.0 * c - 330.0 * esp))))) + false_northing;
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double con, phi;                // temporary angles
    double delta_phi;               // difference between longitudes
    int i;                         // counter variable
    double sin_phi, cos_phi, tan_phi;// sin cos and tangent values
    double c, cs, t, ts, n, r, d, ds;// temporary variables
    double f, h, g, temp;           // temporary variables
    int max_iter = 6;                        // maximun number of iterations
    
    /*fortran code for spherical form
    --------------------------------*/
    if (ind != 0)
       {
       f = Math.exp (x/(r_major*scale_factor));
       g = .5*(f - 1/f);
       temp = lat_origin + y/(r_major*scale_factor);
       h = Math.cos (temp);
       con = Math.sqrt ((1.0 - h*h)/(1.0 + g*g));
       lat[0] = asinz (con);
       if (temp < 0)
         lat[0] = -lat[0];
       if ((g == 0) && (h == 0))
         {
         lon[0] = lon_center;
         return (OK);
         }
       else
         {
         lon[0] = adjust_lon (Math.atan2 (g, h) + lon_center);
         return (OK);
         }
       }
    
    /*Inverse equations
      -----------------*/
    x = x - false_easting;
    y = y - false_northing;
    
    con = (ml0 + y / scale_factor) / r_major;
    phi = con;
    for (i=0;;i++)
       {
       delta_phi=((con + e1*Math.sin (2.0*phi) - e2*Math.sin (4.0*phi) + e3*Math.sin (6.0*phi))
                   / e0) - phi;
    /*
       delta_phi = ((con + e1*Math.sin (2.0*phi) - e2*Math.sin (4.0*phi)) / e0) - phi;
    */
       phi += delta_phi;
       if (Math.abs (delta_phi) <= EPSLN) break;
       if (i >= max_iter)
          {
          p_error ("Latitude failed to converge","UTM-INVERSE");
          return (95);
          }
       }
    if (Math.abs (phi) < HALF_PI)
       {
       sin_phi = Math.sin (phi); cos_phi = Math.cos (phi);
       tan_phi = Math.tan (phi);
       c    = esp*Math.pow (cos_phi, 2);
       cs   = Math.pow (c, 2);
       t    = Math.pow (tan_phi, 2);
       ts   = Math.pow (t, 2);
       con  = 1.0 - es*Math.pow (sin_phi, 2);
       n    = r_major / Math.sqrt (con);
       r    = n*(1.0 - es) / con;
       d    = x / (n*scale_factor);
       ds   = Math.pow (d, 2);
       lat[0] = phi - (n*tan_phi*ds / r)*(0.5 - ds / 24.0*(5.0 + 3.0*t +
              10.0*c - 4.0*cs - 9.0*esp - ds / 30.0*(61.0 + 90.0*t +
              298.0*c + 45.0*ts - 252.0*esp - 3.0*cs)));
       lon[0] = adjust_lon (lon_center + (d*(1.0 - ds / 6.0*(1.0 + 2.0*t +
              c - ds / 20.0*(5.0 - 2.0*c + 28.0*t - 3.0*cs + 8.0*esp +
              24.0*ts))) / cos_phi));
       }
    else
       {
       lat[0] = HALF_PI*sign (y);
       lon[0] = lon_center;
       }
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // UniversalTransverseMercatorProjection

////////////////////////////////////////////////////////////////////////
