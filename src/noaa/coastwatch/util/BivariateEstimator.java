////////////////////////////////////////////////////////////////////////
/*

     File: BivariateEstimator.java
   Author: Peter Hollemans
     Date: 2002/05/28

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
  All rights reserved.

  Developed by: CoastWatch / OceanWatch
                Center for Satellite Applications and Research
                http://coastwatch.noaa.gov

  For conditions of distribution and use, see the accompanying
  license.txt file.

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import java.util.Arrays;
import noaa.coastwatch.util.Function;

/**
 * <p>A bivariate estimator approximates the value of a function of two
 * variables using a set of known function values.  If the function
 * values are given by</p>
 * <blockquote>
 *   <code>f<sub>1</sub> = f(x<sub>1</sub>, y<sub>1</sub>) <br>
 *   f<sub>2</sub> = f(x<sub>2</sub>, y<sub>2</sub>) <br>
 *   ... <br>
 *   f<sub>n</sub> = f(x<sub>n</sub>, y<sub>n</sub>) </code>
 * </blockquote>
 * <p>then an estimator is set up based on the
 * function values using a polynomial of user-specified degree.
 * Suppose that the desired polynomial degree is 2 so that the
 * polynomial is a quadratic.  Then a series of 9 coefficients</p>
 * <blockquote>
 *   <code>[a<sub>0</sub>, a<sub>1</sub>, ... a<sub>8</sub>]</code>
 * </blockquote>
 * <p>are calculated using the values, and the estimator will approximate
 * function values using the coefficients as:</p>
 * <blockquote>
 *   <code>f(x, y) = a<sub>0</sub> + a<sub>1</sub>x +
 *   a<sub>2</sub>x<sup>2</sup> + a<sub>3</sub>y + a<sub>4</sub>xy +
 *   a<sub>5</sub>x<sup>2</sup>y + a<sub>6</sub>y<sup>2</sup> +
 *   a<sub>7</sub>xy<sup>2</sup> +
 *   a<sub>8</sub>x<sup>2</sup>y<sup>2</sup></code>.
 * </blockquote>
 *
 * <p>Note that for estimator constuction where the number of data values
 * is exactly the required number for the polynomial degree, then LU
 * decomposition is used to obtain a least squares solution.  In the
 * case where the linear system is overdetermined (there are more data
 * values than are required) then singular value decomposition is used
 * to obtain a least squares solution.  The SVD calculation used is
 * detailed in:</p>
 * <blockquote>
 *   Watkins, David S.  <i>Fundamentals of Matrix Computations</i>,
 *   John Wiley &amp; Sons, Inc., 1991, pp 416-418.
 * </blockquote>
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class BivariateEstimator 
  extends Function {

  // Variables
  // ---------

  /** The number of terms in each polynomial. */
  private int terms;

  /** The estimator coefficient matrix created at construction time. */
  private Matrix est;

  /** The flattened coefficient matrix used for quadratic evaluation. */
  private double[] a;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a bivariate estimator using the specified function values
   * and polynomial degree.
   *
   * @param xVals the array of x values.
   * @param yVals the array of y values.
   * @param fVals the function values at <code>(x,y)</code> points.
   * @param degree the desired polynomial degree, minimum 1.
   *
   * @throws RuntimeException if the estimator matrix could not be
   * calculated.  This occurs for example when the supplied data
   * results in a singular matrix during computation.
   */
  public BivariateEstimator (
    double[] xVals,
    double[] yVals,
    double[] fVals,
    int degree
  ) {

    // Initialize dimensions
    // ---------------------
    int m = xVals.length;
    terms = degree + 1;
    int n = terms*terms;
      
    // Create A matrix
    // ---------------
    Matrix A = new Matrix (m, n);
    for (int k = 0; k < m; k++) {
      for (int i = 0; i < terms; i++) { 
        for (int j = 0; j < terms; j++) {
          double a = Math.pow (xVals[k], i) * Math.pow (yVals[k], j);
          A.set (k, i*terms + j, a);
        } // for
      } // for
    } // for

    // Create b vector
    // ---------------
    Matrix b = new Matrix (fVals, m);   

    // Solve Ax = b for x via QR
    // -------------------------
    Matrix x;
    if (m == n) {
      x = A.solve (b);
    } // if

    // Solve Ax = b for x via SVD
    // --------------------------
    else {
      SingularValueDecomposition svd = A.svd();
      Matrix c = svd.getU().transpose().times (b);
      int r = svd.rank();
      Matrix cHat = c.getMatrix (0,r-1,0,0);
      Matrix sHat = svd.getS().getMatrix(0,r-1,0,r-1);
      Matrix yHat = sHat.inverse().times (cHat);
      Matrix y = new Matrix (n, 1);
      y.setMatrix (0,r-1,0,0,yHat);
      x = svd.getV().times (y);
    } // else

    // Create estimator matrix
    // -----------------------
    est = new Matrix (terms, terms);
    for (int i = 0; i < terms; i++) {
      for (int j = 0; j < terms; j++) {
        est.set (i, j, x.get (i*terms + j, 0));
      } // for
    } // for

    // Save flattened version of matrix
    // --------------------------------
    a = est.getRowPackedCopy();

  } // BivariateEstimator constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs an estimator from the specified encoding.
   * The encoding must be a valid encoding of the estimator as
   * created by <code>getEncoding</code>.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public BivariateEstimator (
    Object obj
  ) {

    useEncoding (obj);

  } // BivariateEstimator constructor

  ////////////////////////////////////////////////////////////

  /**
   * Evaluates the expression value for a 3-term estimator (quadratic in each
   * variable).
   *
   * @param variables the input variable value array.
   */
  private double evaluateQuadratic (
    double[] variables
  ) {

    // Setup for direct evaluation
    // ---------------------------
    double x = variables[0];
    double x2 = x*x;
    double y = variables[1];
    double y2 = y*y;

    // Estimate function value f = xT E y
    // ----------------------------------
    double value =
      (a[0] + a[1]*y + a[2]*y2) +
      x*(a[3] + a[4]*y + a[5]*y2) +
      x2*(a[6] + a[7]*y + a[8]*y2);

    return (value);
    
  } // evaluate

  ////////////////////////////////////////////////////////////

  @Override
  public double evaluate (
    double[] variables
  ) {

    double value;

    // Evaluate a quadratic-based estimator
    // ------------------------------------
    if (terms == 3) {
      value = evaluateQuadratic (variables);
    } // if

    else {

      // Create polynomial vectors
      // -------------------------
      double[][] x = new double[terms][1];
      double[][] y = new double[terms][1];
      x[0][0] = 1; y[0][0] = 1;
      x[1][0] = variables[0]; y[1][0] = variables[1];
      for (int i = 2; i < terms; i++) {
        x[i][0] = variables[0] * x[i-1][0];
        y[i][0] = variables[1] * y[i-1][0];
      } // for

      // Estimate function value f = xT E y
      // ----------------------------------
      Matrix xp = new Matrix (x, terms, 1);
      Matrix yp = new Matrix (y, terms, 1);
      Matrix f = xp.transpose ().times (est.times (yp));
      value = f.get (0, 0);
      
    } // else

    return (value);

  } // evaluate

  ////////////////////////////////////////////////////////////

  /**
   * Gets an encoded representation of this estimator.  The encoding
   * may later be used to recreate the estimator without using the
   * original function data.
   *
   * @return the object encoding.  The encoding object is a
   * <code>double[]</code> array of coefficients.
   *
   * @see #useEncoding 
   */
  public Object getEncoding () {

    return (est.getColumnPackedCopy());

  } // getEncoding

  ////////////////////////////////////////////////////////////

  /**
   * Uses an encoded representation of this estimator to recreate the
   * estimator contents.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public void useEncoding (
    Object obj
  ) {

    double[] coefs = (double[]) obj;
    terms = (int) Math.round (Math.sqrt (coefs.length));
    est = new Matrix (coefs, terms);
    a = est.getRowPackedCopy();

  } // useEncoding

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    BivariateEstimator est = new BivariateEstimator (
      new double[] {0, 1, 2, 0, 1, 2, 0, 1, 2},
      new double[] {0, 0, 0, 1, 1, 1, 2, 2, 2},
      new double[] {0, 1, 2, 1, 1.41, 2.24, 2, 2.24, 2.83},
      2
    );
    double[] loc = new double[] {1,1};
    System.out.println ("est(1,1) = " + est.evaluate (loc));

    double[] f = new double[9]; 
    java.util.Arrays.fill (f, Double.NaN);
    est = new BivariateEstimator (
      new double[] {0, 1, 2, 0, 1, 2, 0, 1, 2},
      new double[] {0, 0, 0, 1, 1, 1, 2, 2, 2},
      f,
      2
    );
    System.out.println ("est(1,1) = " + est.evaluate (loc));

    f[0] = 0; f[4] = 1.41; f[8] = 2.83;
    est = new BivariateEstimator (
      new double[] {0, 1, 2, 0, 1, 2, 0, 1, 2},
      new double[] {0, 0, 0, 1, 1, 1, 2, 2, 2},
      f,
      2
    );
    System.out.println ("est(1,1) = " + est.evaluate (loc));

  } // main

  ////////////////////////////////////////////////////////////

} // BivariateEstimator class

////////////////////////////////////////////////////////////////////////
