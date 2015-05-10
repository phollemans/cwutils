////////////////////////////////////////////////////////////////////////
/*
     FILE: LinearEnhancement.java
  PURPOSE: A class to implement linear data enhancements.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/21
  CHANGES: 2002/09/26, PFH, added getInverse
           2002/10/10, PFH, added reverse, stats constructor
           2002/10/22, PFH, changed class structure
           2005/02/05, PFH, added describe()

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import noaa.coastwatch.render.EnhancementFunction;

/**
 * A linear enhancement is an enhancement function formed by a linear
 * y = mx + b relationship.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class LinearEnhancement
  extends EnhancementFunction {

  // Variables
  // ---------
  /** Function slope. */
  private double slope;

  /** Function y intercept. */
  private double inter;

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a linear enhancement from the data value range.
   * The minimum and maximum data values are mapped to [0..1].
   *
   * @param range the enhancement range as [min, max].
   */
  public LinearEnhancement (
    double[] range
  ) { 
  
    super (range);

  } // LinearEnhancement constructor

  ////////////////////////////////////////////////////////////

  public double getInverse (
    double normValue
  ) {

    return ((normValue - inter)/slope);

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

    return (slope*variables[0] + inter);

  } // evaluate

  ////////////////////////////////////////////////////////////

  /**
   * Resets the enhancement function.
   */
  protected void reset () {

    double min, max;
    if (reverse) { min = range[1]; max = range[0]; }
    else { min = range[0]; max = range[1]; }
    slope = 1.0 / (max - min);
    inter = -slope*min;

  } // reset

  ////////////////////////////////////////////////////////////

  public String describe () {

    return ("linear" + (reverse ? "-reverse" : ""));

  } // describe

  ////////////////////////////////////////////////////////////

} // LinearEnhancement class

////////////////////////////////////////////////////////////////////////
