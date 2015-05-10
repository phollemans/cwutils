////////////////////////////////////////////////////////////////////////
/*
     FILE: GCTPStyleProjection.java
  PURPOSE: Provides constants and functions for GCTP classes.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/28
  CHANGES: 2012/10/30, PFH, added full set of support functions
           2012/11/05, PFH, added handling of spheres in constructor
           2013/09/23, PFH
           - changes: added setDatum
           - issue: state plane needs the datum set after init

  CoastWatch Software Library and Utilities
  Copyright 2006-2013, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.DatumFactory;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.SpheroidConstants;

/**
 * The <code>GCTPStyleProjection</code> class provides various static
 * constants and functions for classes that perform GCTP style
 * projection computations.  The functions are based directly on those
 * from the GCTPC package.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public abstract class GCTPStyleProjection 
  extends MapProjection {

  // Constants
  // ---------

  /** The value of PI. */
  public static final double PI = Math.PI;

  /** The value of PI/2. */
  public static final double HALF_PI = Math.PI/2;

  /** The value of 2 PI. */
  public static final double TWO_PI = 2*Math.PI;

  /** The epsilon value for checking if numbers are very close. */
  public static final double EPSLN = 1.0e-10;
  
  /** Conversion from radians to degrees. */
  public static final double R2D = 180.0/PI;

  /** A useless quirky companion constant to go along with R2D. */
  public static final double C3P = 0.49238743984;

  /** Conversion from degrees to radians. */
  public static final double D2R = PI/180.0;

  /** A constant for the adjust_lon function. */
  private static final double MAXLONG = 2147483647;

  /** A constant for the adjust_lon function. */
  private static final double DBLLONG = 4.61168601e18;

  /** The max iterations for the adjust_lon function. */
  private static final int MAX_VAL = 4;
  
  // Variables
  // ---------

  /** The false easting value. */
  protected double falseEast;

  /** The false northing value. */
  protected double falseNorth;

  /** The eccentricity value for the spheroid. */
  protected double ec;

  /** The eccentricity value squared for the spheroid. */
  protected double ec2;

  /** The semi-major axis in meters. */
  protected double rMajor;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the datum for this projection and pre-computes values 
   * needed and matching spheroid code.
   *
   * @param datum the new datum to use.
   */
  protected void setDatum (
    Datum datum
  ) {
  
    this.datum = datum;
    this.ec2 = datum.getE2();
    this.ec = Math.sqrt (this.ec2);
    this.rMajor = datum.getAxis();
    double rMinor = Math.sqrt (1 - this.ec2) * this.rMajor;
    this.spheroid = getSpheroid (this.rMajor, rMinor);

  } // setDatum

  ////////////////////////////////////////////////////////////

  /**
   * Creates a GCTP style projection.
   *
   * @param system the map projection system.
   * @param zone the map projection zone for State Plane and UTM
   * projections.
   * @param rMajor the semi-major axis in meters.
   * @param rMinor the semi-minor axis in meters.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   */
  public GCTPStyleProjection (
    int system,
    int zone,
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    super (system, zone, 0, dimensions, affine);

    // Set actual datum
    // ----------------
    spheroid = getSpheroid (rMajor, rMinor);
    if (spheroid != -1)
      datum = DatumFactory.create (spheroid);
    else {
      double invFlat = (rMajor == rMinor ?
        Double.POSITIVE_INFINITY :
        rMajor/(rMajor - rMinor));
      datum = new Datum ("User defined", "User defined", rMajor, invFlat,
        0, 0, 0);
    } // else
    
    // Precompute values
    // -----------------
    ec2 = datum.getE2();
    ec = Math.sqrt (ec2);
    this.rMajor = datum.getAxis();

  } // GCTPStyleProjection constructor

  ////////////////////////////////////////////////////////////

  /**
   * Sets the false east and north values.  By default, these are
   * both set to zero.
   * 
   * @param falseEast the false easting value in meters.
   * @param falseNorth the false northing value in meters.
   */
  public void setFalse (
    double falseEast,
    double falseNorth
  ) {

    this.falseEast = falseEast;
    this.falseNorth = falseNorth;

  } // setFalse

  ////////////////////////////////////////////////////////////

  /**
   * Computes asin and eliminates roundoff error.
   *
   * @param con the value to compute the arc sine for.
   * 
   * @return the arc sine of the value, in the range of -pi/2 
   * through pi/2.
   */
  public static double asinz (
    double con
  ) {

    if (Math.abs (con) > 1.0) {
      if (con > 1.0)
        con = 1.0;
      else
        con = -1.0;
    } // if

    return (Math.asin (con));

  } // asinz

  ////////////////////////////////////////////////////////////

  /**
   * Computes the constant small m which is the radius of
   * a parallel of latitude, phi, divided by the semimajor axis.
   *
   * @param eccent the spheroid eccentricity.
   * @param sinphi sine of the latitude angle.
   * @param cosphi cosine of the latitude angle.
   *
   * @return the value m.
   */
  public static double msfnz (
    double eccent,
    double sinphi,
    double cosphi
  ) {

    double con = eccent*sinphi;
    return ((cosphi / (Math.sqrt (1.0 - con*con))));

  } // msfnz

  ////////////////////////////////////////////////////////////

  /**
   * Computes the constant small q which is the radius of a
   * parallel of latitude, phi, divided by the semimajor axis.
   *
   * @param eccent the spheroid eccentricity.
   * @param sinphi sine of the latitude angle.
   * @param cosphi cosine of the latitude angle.
   *
   * @return the value q.
   */
  public static double qsfnz (
    double eccent,
    double sinphi,
    double cosphi
  ) {

    if (eccent > 1.0e-7) {
      double con = eccent*sinphi;
      return ((1.0 - eccent*eccent) * (sinphi/(1.0 - con*con) - (.5/eccent)*
        Math.log((1.0 - con)/(1.0 + con))));
    } // if
    else
      return (2.0*sinphi);

  } // qsfnz

  ////////////////////////////////////////////////////////////

  /**
   * Computes the value phi1, the latitude for the inverse of the
   * Albers Conical Equal-Area projection.
   *
   * @param eccent the eccentricity angle in radians.
   * @param qs the angle in radians.
   *
   * @return the latitude angle in radians or Double.NaN if the
   * iteration did not converge.
   */
  public static double phi1z (
    double eccent,
    double qs
  ) {

    double eccnts;
    double dphi;
    double con;
    double com;
    double sinpi;
    double cospi;
    double phi;
    long i;

    phi = asinz (.5 * qs);
    if (eccent < EPSLN)
      return (phi);
    eccnts = eccent * eccent;
    for (i = 1; i <= 25; i++) {
      sinpi = Math.sin (phi);
      cospi = Math.cos (phi);
      con = eccent * sinpi;
      com = 1.0 - con * con;
      dphi = .5 * com * com / cospi * (qs / (1.0 - eccnts) - sinpi / com +
        .5 / eccent * Math.log ((1.0 - con) / (1.0 + con)));
      phi = phi + dphi;
      if (Math.abs (dphi) <= 1e-7)
        return (phi);
    } // for

    return (Double.NaN);

  } // phi1z

  ////////////////////////////////////////////////////////////

  /**
   * Computes the latitude angle, phi2, for the inverse of the
   * Lambert Conformal Conic and Polar Stereographic projections.
   *
   * @param eccent the spheroid eccentricity.
   * @param ts the constant value t.
   *
   * @return the latitude angle in radians or Double.NaN if the
   * iteration did not converge.
   */
  public static double phi2z (
    double eccent,
    double ts
  ) {

    double eccnth = 0.5*eccent;
    double phi = HALF_PI - 2*Math.atan (ts);
    for (int i = 0; i <= 15; i++) {
      double sinpi = Math.sin (phi);
      double con = eccent*sinpi;
      double dphi = HALF_PI - 2*Math.atan (ts*(Math.pow(((1 - con)/(1 + con)),
        eccnth))) - phi;
      phi += dphi;
      if (Math.abs (dphi) <= EPSLN)
        return (phi);
    } // for

    return (Double.NaN);

  } // phi2z

  ////////////////////////////////////////////////////////////

  /**
   * Computes the latitude, phi3, for the inverse of the Equidistant
   * Conic projection.
   *
   * @param ml the constant ml.
   * @param e0 the constant e0.
   * @param e1 the constant e1.
   * @param e2 the constant e2.
   * @param e3 the constant e3.
   *
   * @return the latitude angle in radians or Double.NaN if the
   * iteration did not converge.
   */
  public static double phi3z (
    double ml,
    double e0,
    double e1,
    double e2,
    double e3
  ) {

    double phi;
    double dphi;
    long i;

    phi = ml;
    for (i = 0; i < 15; i++) {
      dphi = (ml + e1 * Math.sin(2.0 * phi) - e2 * Math.sin (4.0 * phi) + e3 *
        Math.sin (6.0 * phi)) / e0 - phi;
      phi += dphi;
      if (Math.abs (dphi) <= .0000000001)
        return (phi);
    } // for

   return (Double.NaN);

  } // phi3z

  ////////////////////////////////////////////////////////////

  /**
   * Computes phi4, the latitude for the inverse of the
   * Polyconic projection.
   *
   * @param eccent the eccentricity squared.
   * @param e0 the constant e0.
   * @param e1 the constant e1.
   * @param e2 the constant e0.
   * @param e3 the constant e0.
   * @param a the constant e0.
   * @param b the constant e0.
   * @param c the constant c (modified).
   *
   * @return the latitude angle in radians or Double.NaN if the
   * iteration did not converge.
   */
  public static double phi4z (
    double eccent,
    double e0,
    double e1,
    double e2,
    double e3,
    double a,
    double b,
    double c[]
  ) {

    double sinphi;
    double sin2ph;
    double tanphi;
    double ml;
    double mlp;
    double con1;
    double con2;
    double con3;
    double phi;
    double dphi;
    long i;

    phi = a;
    for (i = 1; i <= 15; i++) {
      sinphi = Math.sin (phi);
      tanphi = Math.tan (phi);
      c[0] = tanphi * Math.sqrt (1.0 - eccent * sinphi * sinphi);
      sin2ph = Math.sin (2.0 * phi);
      ml = e0 * phi - e1 * sin2ph + e2 * Math.sin (4.0 * phi) - e3 *
        Math.sin (6.0 * phi);
      mlp = e0 - 2.0 * e1 * Math.cos (2.0 * phi) + 4.0 * e2 *
       Math.cos (4.0 * phi) - 6.0 * e3 * Math.cos (6.0 * phi);
      con1 = 2.0 * ml + c[0] * (ml * ml + b) - 2.0 * a *  (c[0] * ml + 1.0);
      con2 = eccent * sin2ph * (ml * ml + b - 2.0 * a * ml) / (2.0 * c[0]);
      con3 = 2.0 * (a - ml) * (c[0] * mlp - 2.0 / sin2ph) - 2.0 * mlp;
      dphi = con1 / (con2 + con3);
      phi += dphi;
      if (Math.abs (dphi) <= .0000000001)
        return (phi);
    } // for
    
    return (Double.NaN);

  } // phi4z
  
  ////////////////////////////////////////////////////////////

  /**
   * Converts the 2 digit alternate packed DMS format (+/-)DDDMMSS.SSS
   * to 3 digit standard packed DMS format (+/-)DDDMMMSSS.SSS.
   *
   * @param pak the angle in alternate packed DMS format.
   *
   * @return the angle packed in 3 digit format.
   */
  public static double pakcz (
    double pak
  ) {

    double con;
    double secs;
    long degs, mins;
    char sgna;

    sgna = ' ';
    if (pak < 0.0)
      sgna = '-';
    con = Math.abs (pak);
    degs = (long) ((con / 10000.0) + .001);
    con =  con  - degs * 10000;
    mins = (long) ((con / 100.0) + .001);
    secs = con  - mins * 100;
    con = (double) (degs) * 1000000.0 + (double) (mins) * 1000.0 + secs;
    if (sgna == '-')
      con = - con;

    return (con);

  } // pakcz

  ////////////////////////////////////////////////////////////

  /** 
   * Converts radians to 3 digit packed DMS format (+/-)DDDMMMSSS.SSS.
   *
   * @param pak the angle in radians.
   *
   * @return the angle packed in 3 digits format.
   */
  public static double pakr2dm (
    double pak
  ) {

    double con;
    double secs;
    long degs, mins;
    char sgna;

    sgna = ' ';
    pak *= R2D;
    if (pak < 0.0)
      sgna = '-';
    con = Math.abs (pak);
    degs = (long) (con);
    con =  (con  - degs) * 60;
    mins = (long) con;
    secs = (con  - mins) * 60;
    con = (double) (degs) * 1000000.0 + (double) (mins) * 1000.0 + secs;
    if (sgna == '-')
      con = - con;

    return (con);

  } // pakr2dm
  
  ////////////////////////////////////////////////////////////
  
  /**
   * Computes the constant e0 which is used
   * in a series for calculating the distance along a meridian.
   *
   * @param x the eccentricity squared.
   *
   * @return the constant e0.
   */
  public static double e0fn (
    double x
  ) {
  
    return (1.0-0.25*x*(1.0+x/16.0*(3.0+1.25*x)));

  } // e0fn
  
  ////////////////////////////////////////////////////////////
  
  /**
   * Computes the constant e1 which is used
   * in a series for calculating the distance along a meridian.
   *
   * @param x the eccentricity squared.
   *
   * @return the constant e1.
   */
  public static double e1fn (
    double x
  ) {

    return (0.375*x*(1.0+0.25*x*(1.0+0.46875*x)));

  } // e1fn

  ////////////////////////////////////////////////////////////

  /**
   * Computes the constant e2 which is used
   * in a series for calculating the distance along a meridian.
   *
   * @param x the eccentricity squared.
   *
   * @return the constant e2.
   */
  public static double e2fn (
    double x
  ) {

    return (0.05859375*x*x*(1.0+0.75*x));

  } // e2fn

  ////////////////////////////////////////////////////////////

  /**
   * Computes the constant e3 which is used
   * in a series for calculating the distance along a meridian.
   *
   * @param x the eccentricity squared.
   *
   * @return the constant e3.
   */
  public static double e3fn (
    double x
  ) {

    return (x*x*x*(35.0/3072.0));

  } // e3fn

  ////////////////////////////////////////////////////////////

  /** 
   * Computes the constant e4 used in the Polar Stereographic
   * projection.
   *
   * @param x the eccentricity.
   *
   * @return the constant e4.
   */
  public static double e4fn (
    double x
  ) {

    double con;
    double com;
    con = 1.0 + x;
    com = 1.0 - x;
    return (Math.sqrt ((Math.pow (con, con))*(Math.pow (com, com))));

  } // e4fn
  
  ////////////////////////////////////////////////////////////
  
  /** 
   * Computes the value of M which is the distance along a meridian
   * from the Equator to latitude phi.
   *
   * @param e0 the constant e0.
   * @param e1 the constant e1.
   * @param e2 the constant e0.
   * @param e3 the constant e0.
   * @param phi the latitude value in radians.
   * 
   * @return the distance value M.
   */
  public static double mlfn (
    double e0,
    double e1,
    double e2,
    double e3,
    double phi
  ) {

    return (e0*phi - e1*Math.sin (2.0*phi) + e2*Math.sin (4.0*phi) -
      e3*Math.sin (6.0*phi));

  } // mlfn

  ////////////////////////////////////////////////////////////

  /**
   * Calculates the UTM zone number.
   *
   * @param lon the longitude in degrees.
   *
   * @return the UTM zone number.
   */
  public static int calc_utm_zone (
    double lon
  ) {

    return ((int)(((lon + 180.0) / 6.0) + 1.0));

  } // calc_utm_zone

  ////////////////////////////////////////////////////////////

  /**
   * Gets the sign of a value.
   *
   * @param x the value to get the sign for.
   *
   * @return the sign of the value, -1 if the value is less than zero,
   * or 1 otherwise.
   */
  public static int sign (
    double x
  ) {

    if (x < 0)
      return (-1);
    else
      return (1);

  } // sign

  ////////////////////////////////////////////////////////////

  /** 
   * Computes the constant small t for use in the forward computations
   * in the Lambert Conformal Conic and the Polar Stereographic
   * projections.
   *
   * @param eccent the eccentricity of the spheroid.
   * @param phi the latitude phi in radians.
   * @param sinphi the sine of the latitude.
   *
   * @return the contant term t.
   */
  public static double tsfnz (
    double eccent,
    double phi,
    double sinphi
  ) {
  
    double con = eccent * sinphi;
    double com = .5 * eccent; 
    con = Math.pow (((1.0 - con) / (1.0 + con)), com);
    return (Math.tan (0.5 * (HALF_PI - phi))/con);

  } // tsfnz

  ////////////////////////////////////////////////////////////

  /**
   * Adjusts a longitude angle to be in the range [-180 .. 180] in
   * radians.
   *
   * @param x the input longitude in radians.
   *
   * @return the adjusted longitude in radians.
   */
  public static double adjust_lon (
    double x
  ) { 

    long temp;
    long count = 0;
    for (;;) {
      if (Math.abs(x) <= PI)
        break;
      else if (((long) Math.abs (x/PI)) < 2)
        x = x - (sign (x)*TWO_PI);
      else if (((long) Math.abs (x/TWO_PI)) < MAXLONG)
        x = x - (((long) (x/TWO_PI))*TWO_PI);
      else if (((long) Math.abs (x/(MAXLONG * TWO_PI))) < MAXLONG)
        x = x - (((long) (x/(MAXLONG * TWO_PI))) * (TWO_PI * MAXLONG));
      else if (((long) Math.abs (x/(DBLLONG * TWO_PI))) < MAXLONG)
        x = x - (((long) (x/(DBLLONG * TWO_PI))) * (TWO_PI * DBLLONG));
      else
        x = x - (sign (x)*TWO_PI);
      count++;
      if (count > MAX_VAL)
        break;
    } // for

    return (x);

  } // adjust_lon

  ////////////////////////////////////////////////////////////

  /**
   * Gets the semi-major, semi-minor, and radius axes lengths based on
   * the parameters and spheroid code.  The following algorithm is
   * used.  If the spheroid code is negative, the first two values in
   * the parameter array <code>parm</code> are used to define the
   * values as follows:<p>
   * 
   * <ul>
   * 
   *   <li>If <code>parm[0]</code> is a non-zero value and
   *   <code>parm[1]</code> is greater than one, the semimajor
   *   axis and radius are set to <code>parm[0]</code> and the
   *   semiminor axis is set to <code>parm[1]</code>.</li>
   * 
   *   <li>If <code>parm[0]</code> is nonzero and
   *   <code>parm[1]</code> is greater than zero but less than or
   *   equal to one, the semimajor axis and radius are set to
   *   <code>parm[0]</code> and the semiminor axis is computed
   *   from the eccentricity squared value <code>parm[1]</code>.
   *   This algorithm is given below.</li>
   *
   *   <li>If <code>parm[0]</code> is nonzero and
   *   <code>parm[1]</code> is equal to zero, the semimajor axis,
   *   radius, and semiminor axis are set to
   *   <code>parm[0]</code>.</li>
   * 
   *   <li>If <code>parm[0]</code> equals zero and
   *   <code>parm[1]</code> is greater than zero, the default
   *   Clarke 1866 is used to assign values to the semimajor
   *   axis, radius and semiminor axis.</li>
   *
   *   <li>If <code>parm[0]</code> and <code>parm[1]</code>
   *   equals zero, the semimajor axis and radius are set to
   *   6370997.0 and the semiminor axis is set to zero.</li>
   * 
   * </ul>
   *
   * If a spheroid code is zero or greater, the semimajor and
   * semiminor axis are defined by the spheroid code arrays in the
   * {@link SpheroidConstants} interface, and the radius is set to
   * 6370997.0.  If the spheroid code is greater than
   * <code>MAX_SPHEROIDS-1</code> the default spheroid, Clarke 1866,
   * is used to define the semimajor and semiminor axis and radius is
   * set to 6370997.0.<p>
   *
   * The algorithm to define the semiminor axis using the eccentricity
   * squared value is as follows:<p>
   * 
   * <pre>
   *   semiminor = sqrt(1.0 - ES) * semimajor
   * </pre>
   *
   * where <code>ES</code> is the eccentricity squared.
   *
   * @param isph the spheroid code number.
   * @param parm the projection parameter array.
   * @param r_major the semi-major axis value in meters (modified).
   * @param r_minor the semi-minor axis value in meters (modified).
   * @param radius the radius value in meters (modified).
   */
  public static void sphdz (
    int isph,
    double[] parm,
    double[] r_major,
    double[] r_minor,
    double[] radius
  ) {

    // Spheroid is negative, get axes from parameters
    // ----------------------------------------------
    if (isph < 0) {
      double t_major = Math.abs (parm[0]);
      double t_minor = Math.abs (parm[1]);

      // Use only parameter values to assign axes
      // ----------------------------------------
      if (t_major > 0) {

        // Assign semi-major and semi-minor directly
        // -----------------------------------------
        if (t_minor > 1) {
          r_major[0] = t_major;
          r_minor[0] = t_minor;
          radius[0] = t_major;
        } // if

        // Assign using semi-major and eccentricity squared
        // ------------------------------------------------
        else if (t_minor > 0) {
          r_major[0] = t_major;
          radius[0] = t_major;
          r_minor[0] = (Math.sqrt (1.0 - t_minor)) * t_major;
        } // else if 

        // Assign using semi-major only
        // ----------------------------
        else {
          r_major[0] = t_major;
          radius[0] = t_major;
          r_minor[0] = t_major;
        } // else

      } // if

      // Assign Clarke 1866 axes
      // -----------------------
      else if (t_minor > 0) {
        r_major[0] = SPHEROID_SEMI_MAJOR[CLARKE1866];
        radius[0] = SPHEROID_SEMI_MAJOR[CLARKE1866];
        r_minor[0] = SPHEROID_SEMI_MINOR[CLARKE1866];
      } // else if

      // Assign sphere axes
      // ------------------
      else {
        r_major[0] = SPHEROID_SEMI_MAJOR[SPHERE];
        radius[0] = SPHEROID_SEMI_MAJOR[SPHERE];
        r_minor[0] = SPHEROID_SEMI_MAJOR[SPHERE];
      } // else

    } // if

    // Spheroid is positive, get axes from arrays
    // ------------------------------------------
    else {

      // Check spheroid code
      // -------------------
      int jsph = isph;
      if (jsph > (MAX_SPHEROIDS-1)) jsph = CLARKE1866;

      // Assign radius values
      // --------------------
      r_major[0] = SPHEROID_SEMI_MAJOR[jsph];
      r_minor[0] = SPHEROID_SEMI_MINOR[jsph];
      radius[0] = SPHEROID_SEMI_MAJOR[SPHERE];

    } // else
    
  } // sphdz

  ////////////////////////////////////////////////////////////

  /**
   * Packs an angle in degrees to DDDMMMSSS.SS format.
   * 
   * @param angle the angle in degrees.
   *
   * @return the packed angle in DDDMMMSSS.SS format.
   */
  public static double pack_angle (
    double angle
  ) {

    int degrees = (int) angle;
    int minutes = (int) (angle*60 - degrees*60);
    double seconds = angle*3600 - degrees*3600 - minutes*60;
    return (degrees*1000000 + minutes*1000 + seconds);

  } // pack_angle

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks an angle in DDDMMMSSS.SS format to degrees.
   * 
   * @param angle the packed angle in DDDMMMSSS.SS format.
   *
   * @return the angle in degrees.
   */
  public static double unpack_angle (
    double angle
  ) {

    int degrees = ((int) angle)/1000000;
    int minutes = ((int) angle)/1000 - degrees*1000;
    double seconds = angle - degrees*1000000 - minutes*1000;
    return (degrees + minutes/60.0 + seconds/3600.0);

  } // unpack_angle

  ////////////////////////////////////////////////////////////

} // GCTPStyleProjection

////////////////////////////////////////////////////////////////////////
