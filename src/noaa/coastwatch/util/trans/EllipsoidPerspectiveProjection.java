////////////////////////////////////////////////////////////////////////
/*
     FILE: EllipsoidPerspectiveProjection.java
  PURPOSE: Performs ellipsoid perspective transformations.
   AUTHOR: Peter Hollemans
     DATE: 2005/01/14
  CHANGES: 2005/05/16, PFH, modified for in-place transform
           2006/10/16, PFH, corrected coefficient in ellipsoid matrix A

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import Jama.Matrix;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.SensorScanProjection;

/**
 * The <code>EllipsoidPerspectiveProjection</code> class simulates the
 * Earth view that a theoretical satellite would have from orbit.  The
 * satellite is equipped with a sensor that sweeps rows from top to
 * bottom and columns from left to right at user-specified stepping
 * angles.  It is assumed that the satellite is pointed at the center
 * of the Earth.  A WGS 84 Earth model is used to perform ellipsoid
 * intersection and geodetic latitude calculations.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class EllipsoidPerspectiveProjection
  extends SensorScanProjection {

  // Constants
  // ---------
 
  /** The Earth equatorial radius in meters. */
  private static final double REM = 6378137.0;

  /** The Earth equatorial radius in equator radius units. */
  private static final double RE = 1;

  /** The Earth equatorial radius squared. */
  private static final double RE2 = RE*RE;

  /** The Earth polar radius in meters. */
  private static final double RPM = 6356752.314245;

  /** The Earth polar radius in equator radius units. */
  private static final double RP = RPM/REM;

  /** The Earth polar radius squared. */
  private static final double RP2 = RP*RP;

  /** The Earth equatorial radius over polar radius, all squared. */
  private static final double RE_O_RP_2 = Math.pow (REM/RPM, 2);

  /** The Earth equatorial radius over polar radius, all to the fourth. */
  private static final double RE_O_RP_4 = Math.pow (REM/RPM, 4);

  /** The index of the x coordinate. */
  private static final int X = 0;

  /** The index of the y coordinate. */
  private static final int Y = 1;

  /** The index of the z coordinate. */
  private static final int Z = 2;

  /** The index of the row coordinate. */
  private static final int ROW = 0;

  /** The index of the column coordinate. */
  private static final int COL = 1;

  /** The unit vector in the x direction. */
  private static final double[] XHAT = new double[] {1, 0, 0};

  /** The unit vector in the y direction. */
  private static final double[] YHAT = new double[] {0, 1, 0};

  /** The unit vector in the z direction. */
  private static final double[] ZHAT = new double[] {0, 0, 1};

  /** The ellipsoid equation (x.Ax + b.x + c) matrix A. */
  private static final double[][] ELL_A = new double[][] {
    {1, 0, 0},
    {0, 1, 0},
    {0, 0, RE_O_RP_2}
  };

  /** The ellipsoid equation coefficient c. */
  private static final double ELL_C = -1;

  /** The sensor type string. */
  public static final String SENSOR_TYPE = "geostationary";

  /** The sensor type code. */
  public static final int SENSOR_CODE = 0;

  // Variables
  // ---------

  /** The satellite position in ECEF coordinates. */
  private double[] satVector;

  /** The scanner affine transform matrix for image coords to scan angles. */
  private double[][] scannerAffine;

  /** The inverse scanner affine transform matrix. */
  private double[][] scannerAffineInv;

  /** The satellite rotation matrix for satellite-fixed to Earth-fixed. */
  private double[][] satRotation;

  /** The inverse satellite rotation matrix. */
  private double[][] satRotationInv;

  /** The quadratic equation (alpha*t^2 + beta*t + gamma = 0) gamma value. */
  private double gamma;

  /** Various temporary computation arrays. */
  private double[] locVector, dirVector, surfaceNormal, dirVectorP, 
    scanAngles, imageCoords, solution, tempVector;
  private double[][] ry, rz, scanRotation;

  ////////////////////////////////////////////////////////////

  /**
   * Converts from geodetic latitude to geocentric latitude.
   * 
   * @param phiGD the geodetic latitude of the feature in radians.
   * @param height the height above the reference ellipsoid in radians.
   *
   * @return the geocentric latitude in radians.
   */
  private static double GDToGCLat (
    double phiGD,
    double height
  ) {

    double cos2_phiGD = Math.pow (Math.cos (phiGD), 2);
    double sin2_phiGD = Math.pow (Math.sin (phiGD), 2);
    double beta =  height*RP*Math.sqrt (RE_O_RP_2*cos2_phiGD + sin2_phiGD);
    return (Math.atan (((RP2 + beta)/(RE2 + beta)) * Math.tan (phiGD)));

  } // GDToGCLat

  ////////////////////////////////////////////////////////////

  /**
   * Converts from geocentric latitude to geodetic latitude.
   * 
   * @param phiGC the geocentric latitude of the feature in radians.
   * @param height the height above the reference ellipsoid in radians.
   *
   * @return the geodetic latitude in radians.
   */
  private static double GCToGDLat (
    double phiGC,
    double height
  ) {

    double tan_phiGC = Math.tan (phiGC);
    double phiGD = phiGC;
    double lastPhiGD = 0;
    do {
      double cos2_phiGD = Math.pow (Math.cos (phiGD), 2);
      double sin2_phiGD = Math.pow (Math.sin (phiGD), 2);
      double beta = height*RP*Math.sqrt (RE_O_RP_2*cos2_phiGD + sin2_phiGD);
      double tan_phiGD = ((RE2 + beta)/(RP2 + beta))*tan_phiGC;
      lastPhiGD = phiGD;
      phiGD = Math.atan (tan_phiGD);
    } while (Math.abs (lastPhiGD - phiGD) > 1e-10);

    return (phiGD);

  } // GCToGDLat

  ////////////////////////////////////////////////////////////

  /**
   * Converts geodetic coordinates to Earth-centered Earth-fixed.
   *
   * @param earthLoc the geodetic Earth location.
   * @param height the height above the reference ellipsoid in radians.
   * @param output the output coordinate array to fill.  If null, an
   * output array is created.
   *
   * @return the output coordinate array.
   */
  private static double[] GDToECEF (
    EarthLocation earthLoc, 
    double height,
    double[] output
  ) {

    if (output == null) output = new double[3];
    double theta = Math.toRadians (earthLoc.lon);
    double phi = GDToGCLat (Math.toRadians (earthLoc.lat), height);
    double[] dirVector = new double[] {
      Math.cos (theta) * Math.cos (phi),
      Math.sin (theta) * Math.cos (phi),
      Math.sin (phi)
    };
    double alpha = dot (dirVector, multiply (ELL_A, dirVector, null));
    double t = Math.sqrt (-ELL_C/alpha);
    multiply (dirVector, t + height, output);
    return (output);

  } // GDToECEF

  ////////////////////////////////////////////////////////////

  /**
   * Converts geocentric coordinates to Earth-centered Earth-fixed.
   *
   * @param earthLoc the geocentric Earth location.
   * @param radius the location radius in radians.
   * @param output the output coordinate array to fill.  If null, an
   * output array is created.
   *
   * @return the output coordinate array.
   */
  private static double[] GCToECEF (
    EarthLocation earthLoc, 
    double radius,
    double[] output
  ) {

    if (output == null) output = new double[3];
    double theta = Math.toRadians (earthLoc.lon);
    double phi = Math.toRadians (earthLoc.lat);
    output[X] = radius * Math.cos (theta) * Math.cos (phi);
    output[Y] = radius * Math.sin (theta) * Math.cos (phi);
    output[Z] = radius * Math.sin (phi);
    return (output);

  } // GCToECEF

  ////////////////////////////////////////////////////////////
  
  /**
   * Converts Earth-centered Earth-fixed coordinates to geodetic.
   *
   * @param vector the ECEF coordinate vector.
   * @param height the height above the reference ellipsoid in radians.
   * @param output the output Earth location to fill.  If null, an
   * output location is created.
   *
   * @return the output geodetic Earth location.
   */
  private static EarthLocation ECEFToGD (
    double[] vector,
    double height,
    EarthLocation output
  ) {

    if (output == null) output = new EarthLocation();
    output.lat = Math.toDegrees (GCToGDLat (
      Math.asin (vector[Z] / magnitude (vector)), height));
    output.lon = Math.toDegrees (Math.atan2 (vector[Y], vector[X]));
    return (output);

  } // ECEFToGD

  ////////////////////////////////////////////////////////////

  /**
   * Multiplies a vector by a scalar.
   * 
   * @param vector the vector to multiply.
   * @param factor the scalar multiple.
   * @param output the output vector to fill.  If null, an
   * output vector is created.
   *
   * @return the output vector.
   */
  private static double[] multiply (
    double[] vector,
    double factor,
    double[] output
  ) {

    if (output == null) output = new double[3];
    for (int i = 0; i < 3; i++)
      output[i] = vector[i]*factor;
    return (output);    

  } // multiply

  ////////////////////////////////////////////////////////////

  /**
   * Computes the vector 2-norm magnitude.
   * 
   * @param vector the vector to compute magnitude.
   *
   * @return the vector 2-norm magnitude.
   */
  private static double magnitude (
    double[] vector
  ) {

    return (Math.sqrt (dot (vector, vector)));

  } // magnitude

  ////////////////////////////////////////////////////////////

  /**
   * Normalizes a vector to unit magnitude.
   * 
   * @param vector the vector to normalize.
   * @param output the output vector to fill.  If null, an
   * output vector is created.
   *
   * @return the output vector.
   */
  private static double[] normalize (
    double[] vector,
    double[] output
  ) {

    double mag = magnitude (vector);
    return (multiply (vector, 1/mag, output));

  } // normalize

  ////////////////////////////////////////////////////////////

  /**
   * Computes the cross product of two vectors, a x b.
   * 
   * @param vectorA the first vector in the product.
   * @param vectorB the second vector in the product.
   * @param output the output vector to fill.  If null, an
   * output vector is created.
   *
   * @return the output product vector.
   */
  private static double[] cross (
    double[] vectorA,
    double[] vectorB,
    double[] output
  ) {

    if (output == null) output = new double[3];
    output[X] = vectorA[Y]*vectorB[Z] - vectorA[Z]*vectorB[Y];
    output[Y] = vectorA[Z]*vectorB[X] - vectorA[X]*vectorB[Z];
    output[Z] = vectorA[X]*vectorB[Y] - vectorA[Y]*vectorB[X];
    return (output);

  } // cross

  ////////////////////////////////////////////////////////////

  /**
   * Computes the sum two vectors, a + b.
   * 
   * @param vectorA the first vector in the sum.
   * @param vectorB the second vector in the sum.
   * @param output the output vector to fill.  If null, an
   * output vector is created.
   *
   * @return the output sum vector.
   */
  private static double[] add (
    double[] vectorA,
    double[] vectorB,
    double[] output
  ) {

    if (output == null) output = new double[3];
    for (int i = 0; i < 3; i++)
      output[i] = vectorA[i] + vectorB[i];
    return (output);    

  } // add

  ////////////////////////////////////////////////////////////

  /**
   * Computes the difference of two vectors, a - b.
   * 
   * @param vectorA the first vector in the difference.
   * @param vectorB the second vector in the difference.
   * @param output the output vector to fill.  If null, an
   * output vector is created.
   *
   * @return the output difference vector.
   */
  private static double[] subtract (
    double[] vectorA,
    double[] vectorB,
    double[] output
  ) {

    if (output == null) output = new double[3];
    for (int i = 0; i < 3; i++)
      output[i] = vectorA[i] - vectorB[i];
    return (output);    

  } // subtract

  ////////////////////////////////////////////////////////////

  /**
   * Applies a matrix transform to a vector.
   * 
   * @param matrix the matrix to apply.
   * @param vector the vector to transform.
   * @param output the output vector to fill.  If null, an
   * output vector is created.
   *
   * @return the output transformed vector.
   */
  private static double[] multiply (
    double[][] matrix, 
    double[] vector,
    double[] output
  ) {

    if (output == null) output = new double[3];
    for (int row = 0; row < 3; row++) {
      output[row] = 0;
      for (int col = 0; col < 3; col++)
        output[row] += matrix[row][col] * vector[col];
    } // for
    return (output);

  } // multiply

  ////////////////////////////////////////////////////////////

  /**
   * Computes the dot product of two vectors, a . b.
   * 
   * @param vectorA the first vector in the product.
   * @param vectorB the second vector in the product.
   *
   * @return the output dot product.
   */
  private static double dot (
    double[] vectorA, 
    double[] vectorB
  ) {

    double sum = 0;
    for (int i = 0; i < 3; i++)
      sum += vectorA[i] * vectorB[i];
    return (sum);

  } // dot

  ////////////////////////////////////////////////////////////

  /**
   * Computes a rotation matrix.
   * 
   * @param axis the axis about which the matrix should rotate: X, Y, or Z.
   * @param theta the angle to rotate in radians.  A positive angle
   * will rotate coordinates in a counter-clockwise direction (right
   * hand rule, thumb pointing along the positive axis).
   * @param output the output matrix to fill.  If null, an output
   * matrix is created.
   *
   * @return the output rotation matrix.
   */
  private static double[][] rotationMatrix (
    int axis, 
    double theta,
    double[][] output
  ) {

    if (output == null) output = new double[3][3];
    double sin_theta = Math.sin (-theta);
    double cos_theta = Math.cos (-theta);
    switch (axis) {
    case X:
      output[0][0] = 1; output[0][1] = 0; output[0][2] = 0;
      output[1][0] = 0; output[1][1] = cos_theta; output[1][2] = sin_theta;
      output[2][0] = 0; output[2][1] = -sin_theta; output[2][2] = cos_theta;
      break;
    case Y:
      output[0][0] = cos_theta; output[0][1] = 0; output[0][2] = -sin_theta;
      output[1][0] = 0; output[1][1] = 1; output[1][2] = 0;
      output[2][0] = sin_theta; output[2][1] = 0; output[2][2] = cos_theta;
      break;
    case Z:
      output[0][0] = cos_theta; output[0][1] = sin_theta; output[0][2] = 0;
      output[1][0] = -sin_theta; output[1][1] = cos_theta; output[1][2] = 0;
      output[2][0] = 0; output[2][1] = 0; output[2][2] = 1;
      break;
    } // switch
    return (output);

  } // rotationMatrix

  ////////////////////////////////////////////////////////////

  /**
   * Computes the product of two matrices, AB.
   * 
   * @param matrixA the first matrix in the product.
   * @param matrixB the second matrix in the product.
   * @param output the output matrix to fill.  If null, an
   * output matrix is created.
   *
   * @return the output product matrix.
   */
  private static double[][] multiply (
    double[][] matrixA, 
    double[][] matrixB,
    double[][] output
  ) {

    if (output == null) output = new double[3][3];
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        output[row][col] = 0;
        for (int i = 0; i < 3; i++)
          output[row][col] += matrixA[row][i] * matrixB[i][col];
      } // for
    } // for
    return (output);

  } // multiply

  ////////////////////////////////////////////////////////////

  /**
   * Computes the two solutions to the quadratic equation ax^2 + bx +
   * c = 0.
   *
   * @param a the a coefficient.
   * @param b the b coefficient.
   * @param c the c coefficient.
   * @param output the output array of 2 solutions.
   */
  private static double[] qsolve (
    double a,
    double b,
    double c,
    double[] output
  ) {

    /**
     * Note: This form of the computation helps to reduce roundoff 
     * error when sqrt(b^2 - 4ac) is approximately equal to b.
     */
    if (output == null) output = new double[2];
    double alpha = -b + (b > 0 ? -1 : 1) * Math.sqrt (b*b - 4*a*c);
    output[0] = alpha / (2*a);
    output[1] = (2*c) / alpha;
    return (output);

  } // qsolve

  ////////////////////////////////////////////////////////////

  public String getSensorType () { return (SENSOR_TYPE); }

  ////////////////////////////////////////////////////////////

  public int getSensorCode () { return (SENSOR_CODE); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new ellipsoid perspective projection.  The satellite
   * is located at the specified geocentric location and radius, and is
   * pointed towards the center of the Earth.
   *
   * @param parameters the array of sensor parameters:
   * <ol>
   *   <li>Subpoint latitude in degrees (geocentric).</li>
   *   <li>Subpoint longitude in degrees.</li>
   *   <li>Distance of satellite from center of Earth in kilometers.</li>
   *   <li>Scan step angle in row direction in radians.</li>
   *   <li>Scan step angle in column direction in radians.</li>
   * </ol>
   * @param dimensions the total grid dimensions as [rows, columns].
   */
  public EllipsoidPerspectiveProjection (
    double[] parameters,
    int[] dimensions
  ) {

    // Initialize variables
    // --------------------
    this.dims = (int[]) dimensions.clone();
    this.parameters = (double[]) parameters.clone();
    EarthLocation satLoc = new EarthLocation (parameters[0], parameters[1]);
    double radius = parameters[2];
    double[] scannerSteps = new double[] {parameters[3], parameters[4]};

    // Convert sat location and radius to ECEF (x,y,z)
    // -----------------------------------------------
    /**
     * The satellite vector is the "eye" point of the system
     * and is where the sensor is located.
     */
    satVector = GCToECEF (satLoc, radius*1000/REM, null);

    // Compute scanner affine matrix
    // -----------------------------
    /**
     * The scanner affine transforms image (row, column) to
     * sensor scan angles (rowAngle, colAngle).
     */
    scannerAffine = new double[3][3];
    scannerAffine[0][0] = scannerSteps[ROW];
    scannerAffine[0][1] = 0;
    scannerAffine[0][2] = -scannerSteps[ROW] * ((dimensions[ROW]-1)/2.0);
    scannerAffine[1][0] = 0;
    scannerAffine[1][1] = -scannerSteps[COL];
    scannerAffine[1][2] = scannerSteps[COL] * ((dimensions[COL]-1)/2.0);
    scannerAffine[2][0] = 0;
    scannerAffine[2][1] = 0;
    scannerAffine[2][2] = 1;
    scannerAffineInv = new Matrix (scannerAffine).inverse().getArrayCopy();

    // Compute satellite rotation matrix
    // ---------------------------------
    /**
     * The satellite rotation matrix rotates satellite-fixed (x',y',z')
     * coordinates to Earth-fixed (x,y,z) coordinates.
     */
    satRotationInv = new double[3][];
    satRotationInv[X] = multiply (satVector, -1, null);
    satRotationInv[Y] = cross (ZHAT, satRotationInv[X], null);
    satRotationInv[Z] = cross (satRotationInv[X], satRotationInv[Y], null);
    normalize (satRotationInv[X], satRotationInv[X]);
    normalize (satRotationInv[Y], satRotationInv[Y]);
    normalize (satRotationInv[Z], satRotationInv[Z]);
    satRotation = new Matrix (satRotationInv).inverse().getArrayCopy();

    // Compute ellipsoid intersection equation gamma coefficient
    // ---------------------------------------------------------
    gamma = ELL_C + dot (satVector, multiply (ELL_A, satVector, null));

    // Initialize temporary computation arrays
    // ---------------------------------------
    locVector = new double[3];
    dirVector = new double[3];
    tempVector = new double[3];
    surfaceNormal = new double[3];
    dirVectorP = new double[3];
    scanAngles = new double[3];
    imageCoords = new double[3];
    solution = new double[2];
    ry = new double[3][3];
    rz = new double[3][3];
    scanRotation = new double[3][3];

  } // EllipsoidPerspectiveProjection constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Prints the values contained in a vector to standard output.
   *
   * @param vector the vector to print.
   */
  private static void print (
    double[] vector
  ) {

    System.out.print (vector + " = ");
    for (int i = 0; i < vector.length; i++)
      System.out.print (vector[i] + (i != vector.length-1 ? ", " : "\n"));

  } // print

  ////////////////////////////////////////////////////////////

  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    // Convert Earth location to ECEF (x,y,z)
    // --------------------------------------
    GDToECEF (earthLoc, 0, locVector);

    // Compute direction vector
    // ------------------------
    /** 
     * The direction vector points from the satellite towards the
     * location on the surface of the ellipsoid.
     */
    subtract (locVector, satVector, dirVector);

    // Check for back-facing surface vector
    // ------------------------------------
    /**
     * If the location vector is from a part of the ellipsoid that is
     * not visible to the satellite, then the dot product of the
     * direction vector with the surface normal will be positive.
     */
    surfaceNormal[X] = locVector[X]/RE;
    surfaceNormal[Y] = locVector[Y]/RE;
    surfaceNormal[Z] = locVector[Z]/RP;
    if (dot (dirVector, surfaceNormal) > 0) {
      dataLoc.set (0, Double.NaN);
      dataLoc.set (1, Double.NaN);
      return;
    } // if

    // Rotate direction vector into satellite coordinates
    // --------------------------------------------------
    /** 
     * Once rotated, the direction vector will be relative to the
     * satellite coordinate so that we may compute the view angles.
     */
    multiply (satRotationInv, dirVector, dirVectorP);

    // Compute row and column angles
    // -----------------------------
    /**
     * Row and column angles are computed as if in spherical
     * coordinates.
     */
    scanAngles[ROW] = -Math.asin (dirVectorP[Z] / magnitude (dirVectorP)); 
    scanAngles[COL] = Math.atan2 (dirVectorP[Y], dirVectorP[X]);
    scanAngles[2] = 1;

    // Compute image row and column
    // ----------------------------
    multiply (scannerAffineInv, scanAngles, imageCoords);
    dataLoc.set (Grid.ROWS, imageCoords[ROW]);
    dataLoc.set (Grid.COLS, imageCoords[COL]);

  } // transformImpl

  ////////////////////////////////////////////////////////////

  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    // Convert image row and column to scan angles
    // -------------------------------------------
    imageCoords[ROW] = dataLoc.get (Grid.ROWS);
    imageCoords[COL] = dataLoc.get (Grid.COLS);
    imageCoords[2] = 1;
    multiply (scannerAffine, imageCoords, scanAngles);

    // Compute scan rotation matrix
    // ----------------------------
    /** 
     * The scan rotation matrix is computed by rotation around the
     * y axis to get the sensor elevation, and then rotation about the
     * z axis for the sensor azimuth.
     */
    rotationMatrix (Y, scanAngles[ROW], ry);
    rotationMatrix (Z, scanAngles[COL], rz);
    multiply (rz, ry, scanRotation);

    // Compute direction vector
    // ------------------------
    /** 
     * Given the scan rotation matrix, the scan direction is computed
     * by transforming the unit x direction of the sensor.
     */
    multiply (scanRotation, XHAT, dirVectorP);
    multiply (satRotation, dirVectorP, dirVector);

    // Compute intersection with ellipsoid
    // -----------------------------------
    /**
     * The intersection of the ray from the sensor and the ellipsoid
     * is computed using a quadratic equation in the parameter t, which is
     * the multiplier of the direction vector: x(t) = e + td.
     */
    double alpha = dot (dirVector, multiply (ELL_A, dirVector, tempVector));
    double beta = 2*dot (dirVector, multiply (ELL_A, satVector, tempVector));
    qsolve (alpha, beta, gamma, solution);
    double t = Math.min (solution[0], solution[1]);

    // Check for non-intersecting ray
    // ------------------------------
    if (Double.isNaN (t)) {
      earthLoc.setCoords (Double.NaN, Double.NaN);
    } // if

    // Convert final position vector to geodetic coordinates
    // -----------------------------------------------------
    else {
      multiply (dirVector, t, locVector);
      add (satVector, locVector, locVector);
      ECEFToGD (locVector, 0, earthLoc);
    } // else

  } // transformImpl

  ////////////////////////////////////////////////////////////

  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    int rows = 5632;
    int cols = 5632;
    EarthLocation center = new EarthLocation (10, 74);
    double scanStep = 56e-6;
    EllipsoidPerspectiveProjection trans = 
      new EllipsoidPerspectiveProjection (
      new double[] {center.lat, center.lon, 6.61*REM/1000, scanStep, scanStep},
      new int[] {rows, cols});
    DataLocation[] locArray = new DataLocation[] {
      new DataLocation ((rows-1)/2.0, (cols-1)/2.0),
      new DataLocation ((rows-1)/2.0 - 1000, (cols-1)/2.0 - 1000),
      new DataLocation ((rows-1)/2.0 - 1000, (cols-1)/2.0 + 1000),
      new DataLocation ((rows-1)/2.0 + 1000, (cols-1)/2.0 - 1000),
      new DataLocation ((rows-1)/2.0 + 1000, (cols-1)/2.0 + 1000)
    };
    for (int i = 0; i < locArray.length; i++) {
      System.out.println ("***********************************");
      System.out.println ("i = " + i);
      System.out.println ("  loc          = " + locArray[i]);
      EarthLocation earthLoc = trans.transform (locArray[i]);
      System.out.println ("  t(loc)       = " + earthLoc);
      DataLocation dataLoc = trans.transform (earthLoc);
      System.out.println ("  t^-1(t(loc)) = " + dataLoc);
    } // for

    EarthLocation testLoc = new EarthLocation (-10, -106);
    System.out.println (testLoc + " -> " + trans.transform (testLoc));

  } // main

  ////////////////////////////////////////////////////////////

} // EllipsoidPerspectiveProjection class

////////////////////////////////////////////////////////////////////////
