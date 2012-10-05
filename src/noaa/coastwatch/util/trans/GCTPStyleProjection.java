////////////////////////////////////////////////////////////////////////
/*
     FILE: MercatorProjection.java
  PURPOSE: Handles Mercator map transformations.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/28
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

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
 * The <code>GCTPStyleProjection</code> class provides various static
 * constants and functions for classes that perform GCTP style
 * projection computations.  The functions are based directly on those
 * from the GCTPC package.
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
    else
      datum = new Datum ("User defined", "User defined", rMajor, 
        rMajor/(rMajor - rMinor), 0, 0, 0);

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
      return(-1);
    else
      return(1);

  } // sign

  ////////////////////////////////////////////////////////////

  /**
   * Computes the latitude angle, phi2, for the inverse of the
   * Lambert Conformal Conic and Polar Stereographic projections.
   *
   * @param eccent the spheroid eccentricity.
   * @param ts the constant value t.
   *
   * @return the latitude angle in radians or Double.NaN is the
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
