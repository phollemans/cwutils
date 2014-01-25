////////////////////////////////////////////////////////////////////////
/*
     FILE: UnivariateEstimator.java
  PURPOSE: To perform estimation of a function of one variables.
   AUTHOR: Peter Hollemans
     DATE: 2002/06/03
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import Jama.*;

/**
 * A univariate estimator approximates the value of a function of one
 * variable using a set of known function values.  If the function
 * values are given by <code>[x<sub>1</sub>, x<sub>2</sub>,
 * ... x<sub>n</sub>]</code> then an estimator is set up based on the
 * function values using a polynomial of user-specified degree.
 * Suppose that the desired polynomial degree is 2 so that the
 * polynomial is a quadratic.  Then a series of 3 coefficients
 * <code>a<sub>0</sub>, a<sub>1</sub>, a<sub>2</sub></code> are
 * calculated using the values, and the estimator will approximate
 * function values using the coefficients as <code>f(x) =
 * a<sub>0</sub> + a<sub>1</sub>x + a<sub>2</sub>x<sup>2</sup></code>.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class UnivariateEstimator 
  extends Function {

  // Variables
  // ---------
  /** Polynomial terms. */
  private int terms;

  /** Estimator matrix. */
  private Matrix est;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a univariate estimator using the specified function values
   * and polynomial degree.
   *
   * @param x the array of x values.
   * @param f the function values at <code>x</code> points.
   * @param degree the desired polynomial degree.
   */
  public UnivariateEstimator (
    double[] x,
    double[] f,
    int degree
  ) {
      
    // Create A matrix
    // ---------------
    int values = x.length;
    terms = degree + 1;
    Matrix A = new Matrix (values, terms);
    for (int k = 0; k < values; k++)
      for (int i = 0; i < terms; i++)
        A.set(k, i, Math.pow (x[k], i));

    // Create b vector
    // ---------------
    Matrix b = new Matrix (f, values);   

    // Solve Az = b for z
    // ------------------
    Matrix z  = A.solve (b);

    // Create estimator matrix
    // -----------------------
    est = z.transpose ();

  } // UnivariateEstimator constructor

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
  public UnivariateEstimator (
    Object obj
  ) {

    useEncoding (obj);

  } // UnivariateEstimator constructor

  ////////////////////////////////////////////////////////////

  public double evaluate (
    double[] variables
  ) {

    // Create polynomial vectors
    // -------------------------
    Matrix xp = new Matrix (terms, 1);
    for (int i = 0; i < terms; i++)
      xp.set (i, 0, Math.pow (variables[0], i));

    // Estimate function value f = xT E y
    // ----------------------------------
    Matrix f = est.times (xp);
    return (f.get (0,0));

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

    return (est.getColumnPackedCopy ());

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
    terms = coefs.length;
    est = new Matrix (coefs, 1);

  } // useEncoding

  ////////////////////////////////////////////////////////////

} // UnivariateEstimator class

////////////////////////////////////////////////////////////////////////
