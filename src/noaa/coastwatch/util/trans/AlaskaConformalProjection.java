////////////////////////////////////////////////////////////////////////
/*
     FILE: AlaskaConformalProjection.java
  PURPOSE: Handles Alaska Conformal map transformations.
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
 * The <code>AlaskaConformalProjection</code> class performs 
 * Alaska Conformal map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class AlaskaConformalProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double acoef[];
  private double bcoef[];
  private double cos_p26;
  private double e;
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lat_center;        // center latitude
  private double lon_center;        // Center longitude (projection center)
  private double r_major;           // major axis
  private double r_minor;           // minor axis
  private double sin_p26;
  private int n;

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the Major axis.
   * @param r_min the Minor axis.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double r_min,
    double false_east,
    double false_north
  ) {

    int i;
    double temp;
    double es;
    double chi;
    double esphi;
    
    /*Place parameters in static storage for common use
      -------------------------------------------------*/
    r_major = r_maj;
    r_minor = r_min;
    false_easting = false_east;
    false_northing = false_north;
    lon_center = -152.0*D2R;
    lat_center = 64.0*D2R;
    n = 6;
    
    es = .006768657997291094;
    e = Math.sqrt (es);
    
             acoef = new double[n+1];
             acoef[1]= 0.9945303;
             acoef[2]= 0.0052083;
             acoef[3]= 0.0072721;
             acoef[4]= -0.0151089;
             acoef[5]= 0.0642675;
             acoef[6]= 0.3582802;
             bcoef = new double[n+1];
             bcoef[1]= 0.0;
             bcoef[2]= -.0027404;
             bcoef[3]= 0.0048181;
             bcoef[4]= -0.1932526;
             bcoef[5]= -0.1381226;
             bcoef[6]= -0.2884586;
    
    esphi = e*Math.sin (lat_center);
    chi = 2.0*Math.atan (Math.tan ((HALF_PI + lat_center)/2.0)*
                Math.pow (((1.0 - esphi)/(1.0 + esphi)),(e/2.0))) - HALF_PI;
    sin_p26 = Math.sin (chi); cos_p26 = Math.cos (chi);
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("ALASKA CONFORMAL");
    radius2 (r_major, r_minor);
    cenlon (lon_center);
    cenlat (lat_center);
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
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public AlaskaConformalProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (ALASKA, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // AlaskaConformalProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double dlon;
    double sinlon, coslon;
    double sinphi, cosphi;
    double esphi;
    double g;
    double s;
    double xp;
    double yp;
    double ar;
    double ai;
    double br;
    double bi;
    double arn = 0;
    double ain = 0;
    double chi;
    double r;
    int j;
    
    /*Forward equations
      -----------------*/
    dlon = adjust_lon ( lon - lon_center);
    
    /*caluclate x' and y' for Oblique Stereographic Proj for LAT/LONG
    ----------------------------------------------------------------*/
    sinlon = Math.sin (dlon); coslon = Math.cos (dlon);
    esphi = e*Math.sin (lat);
    chi = 2.0*Math.atan (Math.tan ((HALF_PI + lat) / 2.0)*
                Math.pow (((1.0 - esphi) / (1.0 + esphi)),(e/2.0))) - HALF_PI;
    sinphi = Math.sin (chi); cosphi = Math.cos (chi);
    g = sin_p26*sinphi + cos_p26*cosphi*coslon;
    s = 2.0 / (1.0 + g);
    xp = s*cosphi*sinlon;
    yp = s*(cos_p26*sinphi - sin_p26*cosphi*coslon);
    
    /*Use Knuth algorithm for summing complex terms, to convert
       Oblique Stereographic to Modified-Stereographic coord
    ----------------------------------------------------------*/
    r = xp + xp;
    s = xp*xp + yp*yp;
    ar = acoef[n];
    ai = bcoef[n];
    br = acoef[n -1];
    bi = bcoef[n -1];
    for (j =2; j <= n; j++)
       {
       arn = br + r*ar;
       ain = bi + r*ai;
       if (j < n)
          {
          br = acoef[n - j] - s*ar;
          bi = bcoef[n - j] - s*ai;
          ar = arn;
          ai = ain;
          }
       }
    br = -s*ar;
    bi = -s*ai;
    ar = arn;
    ai = ain;
    x[0] = (xp*ar - yp*ai + br)*r_major + false_easting;
    y[0] = (yp*ar + xp*ai + bi)*r_major + false_northing;
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double dlon;
    double sinlon, coslon;
    double esphi;
    double r;
    double s;
    double br;
    double bi;
    double ai;
    double ar;
    double ci;
    double cr;
    double di;
    double dr;
    double arn = 0;
    double ain = 0;
    double crn;
    double cin;
    double fxyr;
    double fxyi;
    double fpxyr;
    double fpxyi;
    double xp, yp;
    double den;
    double dxp;
    double dyp;
    double ds;
    double z;
    double cosz;
    double sinz;
    double rh;
    double chi;
    double dphi;
    double phi;
    int j;
    int nn;
    
    /*Inverse equations
      -----------------*/
    x = (x - false_easting) / r_major;
    y = (y - false_northing) / r_major;
    xp = x;
    yp = y;
    nn = 0;
    
    /*Use Knuth algorithm for summing complex terms, to convert Modified-
       Stereographic conformal to Oblique Stereographic coordinates.
    --------------------------------------------------------------------*/
    do
      {
      r = xp + xp;
      s = xp*xp + yp*yp;
      ar = acoef[n];
      ai = bcoef[n];
      br = acoef[n -1];
      bi = bcoef[n - 1];
      cr = (double) (n)*ar;
      ci = (double) (n)*ai;
      dr = (double) (n -1)*br;
      di = (double) (n -1)*bi;
    
      for (j = 2; j <= n; j++)
          {
          arn = br + r*ar;
          ain = bi + r*ai;
          if (j < n)
            {
            br = acoef[n -j] - s*ar;
            bi = bcoef[n - j] - s*ai;
            ar = arn;
            ai = ain;
            crn = dr  + r*cr;
            cin = di  + r*ci;
            dr = (double) (n - j)*acoef[n -j] - s*cr;
            di = (double) (n - j)*bcoef[n -j] - s*ci;
            cr = crn;
            ci = cin;
            }
          }
      br = -s*ar;
      bi = -s*ai;
      ar = arn;
      ai = ain;
      fxyr = xp*ar - yp*ai + br - x;
      fxyi = yp*ar + xp*ai + bi - y;
      fpxyr = xp*cr - yp*ci + dr;
      fpxyi = yp*cr + xp*ci + ci;
      den = fpxyr*fpxyr + fpxyi*fpxyi;
      dxp = -(fxyr*fpxyr + fxyi*fpxyi) / den;
      dyp = -(fxyi*fpxyr - fxyr*fpxyi) / den;
      xp = xp + dxp;
      yp = yp + dyp;
      ds = Math.abs (dxp) + Math.abs (dyp);
      nn++;
      if (nn > 20)
         {
         p_error ("Too many iterations in inverse","alcon-inv");
         return (235);
         }
      }
    while (ds > EPSLN);
    
    /*convert Oblique Stereographic coordinates to LAT/LONG
    ------------------------------------------------------*/
    rh = Math.sqrt (xp*xp + yp*yp);
    z = 2.0*Math.atan (rh / 2.0);
    sinz = Math.sin (z); cosz = Math.cos (z);
    lon[0] = lon_center;
    if (Math.abs (rh) <= EPSLN)
       {
       lat[0] = lat_center;
       return (OK);
       }
    chi = asinz (cosz*sin_p26 + (yp*sinz*cos_p26) / rh);
    nn = 0;
    phi = chi;
    do
      {
      esphi = e*Math.sin (phi);
      dphi = 2.0*Math.atan (Math.tan ((HALF_PI + chi) / 2.0)*
             Math.pow (((1.0 + esphi) / (1.0 - esphi)),(e / 2.0))) - HALF_PI - phi;
      phi += dphi;
      nn++;
      if (nn > 20)
         {
         p_error ("Too many iterations in inverse","alcon-inv");
         return (236);
         }
      }
    while (Math.abs (dphi) > EPSLN);
    
    lat[0] = phi;
    lon[0] = adjust_lon (lon_center + Math.atan2 ((xp*sinz), (rh*cos_p26*cosz - yp*
                       sin_p26*sinz)));
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // AlaskaConformalProjection

////////////////////////////////////////////////////////////////////////
