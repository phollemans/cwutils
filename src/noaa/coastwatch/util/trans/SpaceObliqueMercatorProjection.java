////////////////////////////////////////////////////////////////////////
/*
     FILE: SpaceObliqueMercatorProjection.java
  PURPOSE: Handles Space Oblique Mercator map transformations.
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
 * The <code>SpaceObliqueMercatorProjection</code> class performs 
 * Space Oblique Mercator map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class SpaceObliqueMercatorProjection 
  extends GCTPCStyleProjection {

  // Constants
  // ---------
  private static final double LANDSAT_RATIO = 0.5201613;

  // Variables
  // ---------
  private double false_easting;
  private double false_northing;
  private double lon_center,a,b,a2,a4,c1,c3,q,t,u,w,xj,p21,sa,ca,es,s,start;

  ////////////////////////////////////////////////////////////

  /**
   * Computes the a, b, c coefficients to convert from transform
   * latitude, longitude to Space Oblique Mercator (SOM) rectangular 
   * coordinates.
   * 
   * Mathematical analysis by John Snyder 6/82
   */
  private void som_series (
    double fb[],
    double fa2[],
    double fa4[],
    double fc1[],
    double fc3[],
    double dlam[]
  ) {

    double sd,sdsq,h,sq,fc;

    dlam[0] = dlam[0]*0.0174532925;               /* Convert dlam to radians */
    sd = Math.sin(dlam[0]);
    sdsq = sd*sd;
    s = p21*sa*Math.cos(dlam[0])*Math.sqrt((1.0+t*sdsq)/((1.0+w*sdsq)*(1.0+q*sdsq)));
    h = Math.sqrt((1.0+q*sdsq)/(1.0+w*sdsq))*(((1.0+w*sdsq)/((1.0+q*sdsq)*(1.0+
      q*sdsq)))-p21*ca);
    sq = Math.sqrt(xj*xj+s*s);
    fb[0] = (h*xj-s*s)/sq;
    fa2[0] = fb[0]*Math.cos(2.0*dlam[0]);
    fa4[0] = fb[0]*Math.cos(4.0*dlam[0]);
    fc = s*(h+xj)/sq;
    fc1[0] = fc*Math.cos(dlam[0]);
    fc3[0] = fc*Math.cos(3.0*dlam[0]);
    
  } // som_series

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_major the major axis.
   * @param r_minor the minor axis.
   * @param satnum the Landsat satellite number (1,2,3,4,5).
   * @param path the Landsat path number.
   * @param alf_in orbit inclination angle.
   * @param lon longitude of ascending orbit at equator.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   * @param time
   * @param start1
   * @param flag
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_major,
    double r_minor,
    long satnum,
    long path,
    double alf_in,
    double lon,
    double false_east,
    double false_north,
    double time,
    long   start1,
    long   flag
  ) {

    int i;
    double alf, e2c, e2s, one_es;
    double dlam[], fb[], fa2[], fa4[], fc1[], fc3[], suma2, suma4, sumc1, sumc3, sumb;
    dlam = new double[1];
    fb = new double[1];
    fa2 = new double[1];
    fa4 = new double[1];
    fc1 = new double[1];
    fc3 = new double[1];
    
    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    false_easting = false_east;
    false_northing = false_north;
    a = r_major;
    b = r_minor;
    es = 1.0 - Math.pow (r_minor/r_major, 2);
    if (flag != 0)
      {
      alf = alf_in;
      p21 = time / 1440.0;
      lon_center = lon;
      start =  start1;
      }
    else
      {
      if (satnum < 4)
        {
        alf = 99.092*D2R;
        p21=103.2669323/1440.0;
        lon_center = (128.87 - (360.0/251.0*path))*D2R;
        }
      else
        {
        alf = 98.2*D2R;
        p21=98.8841202/1440.0;
        lon_center = (129.30 - (360.0/233.0*path))*D2R;
        /*
        lon_center = (-129.30557714 - (360.0/233.0*path))*D2R;
    */
        }
      start=0.0;
      }
    
    /*Report parameters to the user (to device set up prior to this call)
      -------------------------------------------------------------------*/
    ptitle ("SPACE OBLIQUE MERCATOR");
    radius2 (a, b);
    if (flag == 0)
       {
       genrpt_long (path,     "Path Number:    ");
       genrpt_long (satnum,   "Satellite Number:    ");
       }
    genrpt (alf*R2D,       "Inclination of Orbit:    ");
    genrpt (lon_center*R2D,"Longitude of Ascending Orbit:    ");
    offsetp (false_easting, false_northing);
    genrpt (LANDSAT_RATIO, "Landsat Ratio:    ");
    
    ca = Math.cos (alf);
    if (Math.abs (ca) < 1.e-9)
      ca = 1.e-9;
    sa = Math.sin (alf);
    e2c = es*ca*ca;
    e2s = es*sa*sa;
    w = (1.0-e2c)/(1.0-es);
    w = w*w-1.0;
    one_es = 1.0-es;
    q = e2s / one_es;
    t = (e2s*(2.0-es)) / (one_es*one_es);
    u = e2c / one_es;
    xj = one_es*one_es*one_es;
    dlam[0] = 0.0;
    som_series (fb,fa2,fa4,fc1,fc3,dlam);
    suma2 = fa2[0];
    suma4 = fa4[0];
    sumb = fb[0];
    sumc1 = fc1[0];
    sumc3 = fc3[0];
    for (i = 9; i <= 81; i += 18)
       {
       dlam[0] = i;
       som_series (fb,fa2,fa4,fc1,fc3,dlam);
       suma2 = suma2+4.0*fa2[0];
       suma4 = suma4+4.0*fa4[0];
       sumb = sumb+4.0*fb[0];
       sumc1 = sumc1+4.0*fc1[0];
       sumc3 = sumc3+4.0*fc3[0];
       }
    for (i = 18; i <= 72; i += 18)
       {
       dlam[0] = i;
       som_series (fb,fa2,fa4,fc1,fc3,dlam);
       suma2 = suma2+2.0*fa2[0];
       suma4 = suma4+2.0*fa4[0];
       sumb = sumb+2.0*fb[0];
       sumc1 = sumc1+2.0*fc1[0];
       sumc3 = sumc3+2.0*fc3[0];
       }
    
    dlam[0] = 90.0;
    som_series (fb,fa2,fa4,fc1,fc3,dlam);
    suma2 = suma2+fa2[0];
    suma4 = suma4+fa4[0];
    sumb = sumb+fb[0];
    sumc1 = sumc1+fc1[0];
    sumc3 = sumc3+fc3[0];
    a2 = suma2/30.0;
    a4 = suma4/60.0;
    b = sumb/30.0;
    c1 = sumc1/15.0;
    c3 = sumc3/45.0;
    
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
   * @param satnum the Landsat satellite number (1,2,3,4,5).
   * @param path the Landsat path number.
   * @param alf_in orbit inclination angle.
   * @param lon longitude of ascending orbit at equator.
   * @param time
   * @param start1
   * @param flag
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public SpaceObliqueMercatorProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    long satnum,
    long path,
    double alf_in,
    double lon,
    double time,
    long   start1,
    long   flag,
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (SOM, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, satnum, path, alf_in,
      lon, falseEast, falseNorth, time, start1, flag);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // SpaceObliqueMercatorProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double y[],
    double x[]
  ) {

    int n, i, l;
    double delta_lon;
    double rlm, tabs, tlam, xlam, c, xlamt, ab2, sc1, ab1, xlamp, sav;
    double d, sdsq, sd, tanlg, xtan, tphi, dp, dd, ds, rlm2;
    double scl, tlamp, conv, delta_lat, radlt, radln;
    double temp;
    char   errorbuf[] = new char[80];
    
    tlamp = 0;
    tlam = 0;
    sav = 0;
    ab2 = 0;
    l = 0;
    xlamt = 0;
    tphi = 0;
    scl = 0;
    
    /*Forward equations
      -----------------*/
    conv = 1.e-7;
    delta_lat = lat;
    delta_lon= lon-lon_center;
    
    /*Test for latitude and longitude approaching 90 degrees
       ----------------------------------------------------*/
    if (delta_lat > 1.570796) delta_lat = 1.570796;
    if (delta_lat < -1.570796) delta_lat =  -1.570796;
    radlt = delta_lat;
    radln = delta_lon;
    if (delta_lat >= 0.0) tlamp = PI/2.0;
    if (start != 0.0) tlamp = 2.5*PI;
    if (delta_lat < 0.0) tlamp = 1.5*PI;
    n = 0;

    int gotoLabel = 230;
    for (;;) {
      switch (gotoLabel) {
    
      case 230:
           sav = tlamp;
           l = 0;
           xlamp = radln + p21*tlamp;
           ab1 = Math.cos (xlamp);
           if (Math.abs (ab1) < conv) xlamp = xlamp-1.e-7;
           if (ab1 >= 0.0) scl = 1.0;
           if (ab1 < 0.0) scl= -1.0;
           ab2 = tlamp - (scl)*Math.sin (tlamp)*HALF_PI;

      case 240:
           xlamt = radln+p21*sav;
           c = Math.cos (xlamt);
           if (Math.abs (c) < 1.e-7) xlamt = xlamt-1.e-7;
           xlam = (((1.0-es)*Math.tan (radlt)*sa)+Math.sin (xlamt)*ca)/c;
           tlam = Math.atan (xlam);
           tlam = tlam+ab2;
           tabs = Math.abs (sav) - Math.abs (tlam);
           if (Math.abs (tabs) < conv) { gotoLabel = 250; continue; }
           l = l+1;
           if (l > 50) { gotoLabel = 260; continue; }
           sav = tlam;
           gotoLabel = 240; continue;
    
    /*Adjust for confusion at beginning and end of landsat orbits
      -----------------------------------------------------------*/

      case 250:
           rlm = PI*LANDSAT_RATIO;
           rlm2 = rlm+2.0*PI;
           n++;
           if (n >= 3) { gotoLabel = 300; continue; }
           if (tlam > rlm && tlam < rlm2) { gotoLabel = 300; continue; }
           if (tlam < rlm) tlamp = 2.50*PI;
           if (tlam >= rlm2) tlamp = HALF_PI;
           gotoLabel = 230; continue;

      case 260:
           sprintf (errorbuf,"50 iterations without conv\n");
           p_error (errorbuf,"som-forward");
           return (214);
    
    /*tlam computed - now compute tphi
      --------------------------------*/

      case 300:
          ds = Math.sin (tlam);
          dd = ds*ds;
          dp = Math.sin (radlt);
          tphi = Math.asin (((1.0-es)*ca*dp-sa*Math.cos (radlt)*Math.sin (xlamt))/Math.sqrt (1.0-es*dp*dp));
        
      } // switch
      break;
    } // for
    
    /*compute x and y
      ---------------*/
    xtan = (PI/4.0) + (tphi/2.0);
    tanlg = Math.log (Math.tan (xtan));
    sd = Math.sin (tlam);
    sdsq = sd*sd;
    s = p21*sa*Math.cos (tlam)*Math.sqrt ((1.0+t*sdsq)/((1.0+w*sdsq)*(1.0+q*sdsq)));
    d = Math.sqrt (xj*xj+s*s);
    x[0] = b*tlam+a2*Math.sin (2.0*tlam)+a4*Math.sin (4.0*tlam)-tanlg*s/d;
    x[0] = a*x[0];
    y[0] = c1*sd+c3*Math.sin (3.0*tlam)+tanlg*xj/d;
    y[0] = a*y[0];
    
    /*Negate x & swap x, y
      -------------------*/
    temp = x[0];
    x[0]= y[0] + false_easting;
    y[0] = temp + false_northing;
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double y,
    double x,
    double lon[],
    double lat[]
  ) {

    double tlon, conv, sav, sd, sdsq, blon, dif, st, defac, actan, tlat, dd, bigk, bigk2, xlamt;
    double sl, scl, dlat, dlon, temp;
    int inumb;
    
    sl = 0;
    dlat = 0;
    scl = 0;

    /*Inverse equations. Begin inverse computation with approximation for tlon.
       Solve for transformed long.
      ---------------------------*/
    temp = y; y = x - false_easting; x= temp - false_northing;
    tlon =  x/(a*b);
    conv = 1.e-9;
    for (inumb = 0;inumb<50;inumb++)
       {
       sav = tlon;
       sd = Math.sin (tlon);
       sdsq = sd*sd;
       s = p21*sa*Math.cos (tlon)*Math.sqrt ((1.0+t*sdsq)/((1.0+w*sdsq)*(1.0+q*sdsq)));
       blon = (x/a)+(y/a)*s/xj-a2*Math.sin (2.0*tlon)-a4*Math.sin (4.0*tlon)-(s/xj)*(c1*
              Math.sin (tlon)+c3*Math.sin (3.0*tlon));
       tlon = blon/b;
       dif = tlon-sav;
       if (Math.abs (dif)<conv)break;
       }
    if (inumb >= 50)
       {
       p_error ("50 iterations without convergence","som-inverse");
       return (214);
       }
    
    /*Compute transformed lat.
      ------------------------*/
    st=Math.sin (tlon);
    defac=Math.exp (Math.sqrt (1.0+s*s/xj/xj)*(y/a-c1*st-c3*Math.sin (3.0*tlon)));
    actan=Math.atan (defac);
    tlat=2.0*(actan-(PI/4.0));
    
    /*Compute geodetic longitude
      --------------------------*/
    dd=st*st;
    if (Math.abs (Math.cos (tlon))<1.e-7) tlon=tlon-1.e-7;
    bigk=Math.sin (tlat);
    bigk2=bigk*bigk;
    xlamt=Math.atan (((1.0-bigk2/(1.0-es))*Math.tan (tlon)*ca-bigk*sa*Math.sqrt ((1.0+q*dd)
    *(1.0-bigk2)-bigk2*u)/Math.cos (tlon))/(1.0-bigk2*(1.0+u)));
    
    /*Correct inverse quadrant
      ------------------------*/
    if (xlamt>=0.0) sl=1.0;
    if (xlamt<0.0) sl= -1.0;
    if (Math.cos (tlon)>=0.0) scl=1.0;
    if (Math.cos (tlon)<0.0) scl= -1.0;
    xlamt=xlamt-((PI/2.0)*(1.0-scl)*sl);
    dlon=xlamt-p21*tlon;
    
    /*Compute geodetic latitude
      -------------------------*/
    if (Math.abs (sa)<1.e-7)dlat=Math.asin (bigk/Math.sqrt ((1.0-es)*(1.0-es)+es*bigk2));
    if (Math.abs (sa)>=1.e-7)dlat=Math.atan ((Math.tan (tlon)*Math.cos (xlamt)-ca*Math.sin (xlamt))/((1.0-es)*sa));
    lon[0] = adjust_lon (dlon+lon_center);
    lat[0] = dlat;
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // SpaceObliqueMercatorProjection

////////////////////////////////////////////////////////////////////////
