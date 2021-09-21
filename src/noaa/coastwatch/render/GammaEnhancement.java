////////////////////////////////////////////////////////////////////////
/*

     File: GammaEnhancement.java
   Author: Peter Hollemans
     Date: 2018/08/20

  CoastWatch Software Library and Utilities
  Copyright (c) 2018 National Oceanic and Atmospheric Administration
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
 * A gamma enhancement is an enhancement function formed by a power law
 * y = (mx + b)^g relationship where g is the gamma value, set to 1/2.2 by default.
 * This equation normalizes the value of x within the range using a linear
 * enhancement, and then applies the power to encode the normalized value,
 * typically for display of a grayscale intensity value on a screen.  Since
 * computer screens normally have a gamma correction value of 2.2, this makes
 * the x value intensity look correct, when x represents an intensity such
 * as albedo.
 *
 * @author Peter Hollemans
 * @since 3.4.1
 */
public class GammaEnhancement
  extends EnhancementFunction {

  // Variables
  // ---------

  /** Function slope. */
  private double slope;

  /** Function y intercept. */
  private double inter;

  /** Function gamma value. */
  private double gamma;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a gamma enhancement from the data value range.
   * The minimum and maximum data values are mapped to [0..1].
   *
   * @param range the enhancement range as [min, max].
   */
  public GammaEnhancement (
    double[] range
  ) {
  
    super (range);
    gamma = 1.0/2.2;

  } // GammaEnhancement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the gamma value for this function.
   *
   * @return the gamma value used in the normalization.
   *
   * @since 3.7.0
   */
  public double getGamma () { return (gamma); }

  ////////////////////////////////////////////////////////////

  @Override
  public double getInverse (
    double normValue
  ) {

    return ((Math.pow (normValue, 1.0/gamma) - inter)/slope);

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

    return (Math.pow (slope*variables[0] + inter, gamma));

  } // evaluate

  ////////////////////////////////////////////////////////////

  @Override
  protected void reset () {

    double min, max;
    if (reverse) { min = range[1]; max = range[0]; }
    else { min = range[0]; max = range[1]; }
    slope = 1.0 / (max - min);
    inter = -slope*min;

  } // reset

  ////////////////////////////////////////////////////////////

  public String describe () {

    return ("gamma" + (reverse ? "-reverse" : ""));

  } // describe

  ////////////////////////////////////////////////////////////

} // GammaEnhancement class

////////////////////////////////////////////////////////////////////////

