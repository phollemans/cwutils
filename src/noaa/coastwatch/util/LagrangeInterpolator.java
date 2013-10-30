////////////////////////////////////////////////////////////////////////
/*
     FILE: LagrangeInterpolator.java
  PURPOSE: To perform Lagrangian interpolation.
   AUTHOR: Peter Hollemans
     DATE: 2003/02/06
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

/**
 * A Lagrange interpolator uses a set of known function data to
 * interpolate function values between data points.  The interpolator
 * makes use of the Lagrange polynomial method as describe in:
 * <blockquote>
 *   Burden, R.L., and J.D Faires.  "Numercial Analysis", Sixth Ed.,
 *   Brooks/Cole Publishing, 1997, pp 107-111.
 * </blockquote>
 * Briefly, the interpolator derives an (n-1)th order polynomial using
 * the n known data points (x<sub>i</sub>, y<sub>i</sub>) where i=1..n
 * with the formula:
 * <blockquote>
 *   P(x) = SUM(i=1..n) { y<sub>i</sub>L<sub>i</sub>(x) }
 * </blockquote>
 * where:
 * <blockquote>
 *   L<sub>i</sub>(x) = PRODUCT(j=1..n,j!=i) { (x-x<sub>j</sub>) / 
 *   (x<sub>i</sub> - x<sub>j</sub>) }
 * </blockquote>
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class LagrangeInterpolator
  extends Function {

  // Variables
  // ---------
  /** The polynomial constant terms. */
  private double[] a;

  /** The x values of the data points. */
  private double[] x;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new Lagrangian interpolator based on the specified
   * data.
   * 
   * @param x the x data values.
   * @param y the y function values corresponding to 
   * x<sub>1</sub>..x<sub>n</sub>.
   */
  public LagrangeInterpolator (
    double[] x,
    double[] y
  ) {

    // Calculate polynomial coefficients
    // ---------------------------------
    a = new double[x.length];
    for (int i = 0; i < x.length; i++) {
      double product = 1;
      for (int j = 0; j < x.length; j++) {
        if (i != j) product *= (x[i] - x[j]);
      } // for
      a[i] = y[i] / product;
    } // for

    // Copy x values
    // -------------
    this.x = new double[x.length];
    System.arraycopy (x, 0, this.x, 0, x.length);

  } // LagrangeInterpolator constructor

  ////////////////////////////////////////////////////////////

  public double evaluate (
    double[] variables
  ) {

    // Calcuate sum of terms
    // ---------------------
    double sum = 0;
    for (int i = 0; i < x.length; i++) {
      double product = 1;
      for (int j = 0; j < x.length; j++) {
        if (i != j) product *= (variables[0] - x[j]);
      } // for
      sum += a[i] * product;
    } // for

    return (sum);

  } // evaluate

  ////////////////////////////////////////////////////////////

} // LagrangeInterpolator class

////////////////////////////////////////////////////////////////////////
