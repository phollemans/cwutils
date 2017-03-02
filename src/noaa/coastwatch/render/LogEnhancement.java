////////////////////////////////////////////////////////////////////////
/*

     File: LogEnhancement.java
   Author: Peter Hollemans
     Date: 2002/07/21

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
package noaa.coastwatch.render;

// Imports
// -------
import noaa.coastwatch.render.EnhancementFunction;

/**
 * A log enhancement is an enhancement function formed by a logarithm
 * y = mlogx + b relationship.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class LogEnhancement
  extends EnhancementFunction {

  // Constants
  // ---------

  /** The natural log of 10. */
  private static final double LOG10 = Math.log (10);

  // Variables
  // ---------
  /** Function slope. */
  private double slope;

  /** Function y intercept. */
  private double inter;

  ////////////////////////////////////////////////////////////

  /** Computes the log base 10 of a number. */
  public static double log10 (
    double x
  ) {

    return (Math.log (x) / LOG10);

  } // log10

  ////////////////////////////////////////////////////////////

  /** 
   * Checks the log enhancement range.
   *
   * @throws ArithmeticException if one of the range values is &lt;= 0.
   */
  private void checkRange (double[] range) {

    if (range[0] <= 0 || range[1] <= 0) {
      throw new ArithmeticException ("Invalid log enhancement range [" + 
        range[0] + "," + range[1] + "] (must be positive)");
    } // if

  } // checkRange

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a log enhancement from the specified parameters.  The
   * minimum and maximum data values are mapped to [0..1].
   *
   * @param range the enhancement range as [min, max].
   */
  public LogEnhancement (
    double[] range
  ) { 
  
    super (range);
    checkRange (range);

  } // LogEnhancement constructor

  ////////////////////////////////////////////////////////////

  public void setRange (
    double[] range
  ) {

    checkRange (range);
    super.setRange (range);

  } // setRange

  ////////////////////////////////////////////////////////////

  public double getInverse (
    double normValue
  ) {

    return (Math.pow (10, (normValue - inter)/slope));

  } // getInverse

  ////////////////////////////////////////////////////////////

  /**
   * Evaluates the enhancement at the specified data value.  The
   * {@link #getValue} method should be used instead of this method
   * for evaluating enhancement values.
   *
   * @param variables a double[1] specifying the data value.
   *
   * @return the enhancement value.
   */
  public double evaluate (
    double[] variables
  ) {

    return (slope * log10 (variables[0]) + inter);

  } // evaluate

  ////////////////////////////////////////////////////////////

  /**
   * Resets the enhancement function.
   */
  protected void reset () {

    double min, max;
    if (reverse) { min = range[1]; max = range[0]; }
    else { min = range[0]; max = range[1]; }
    slope = 1.0 / (log10 (max) - log10 (min));
    inter = -slope * log10 (min);

  } // reset

  ////////////////////////////////////////////////////////////

  public String describe () {

    return ("log" + (reverse ? "-reverse" : ""));

  } // describe

  ////////////////////////////////////////////////////////////

} // LogEnhancement class

////////////////////////////////////////////////////////////////////////
