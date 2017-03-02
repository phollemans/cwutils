////////////////////////////////////////////////////////////////////////
/*

     File: StepEnhancement.java
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
import noaa.coastwatch.render.LinearEnhancement;

/**
 * A step enhancement is an enhancement function formed by a number of
 * discrete steps.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class StepEnhancement
  extends LinearEnhancement {

  // Variables
  // ---------
  /** The enhancement value step size. */
  private double step;

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a step enhancement from the specified parameters.
   * The minimum and maximum data values are mapped to [0..1].
   *
   * @param range the enhancement range as [min, max].
   * @param step the data value step size.  The range between the
   * minimum and maximum is divided up into discrete steps of this
   * size.
   */
  public StepEnhancement (
    double[] range,
    double step
  ) { 
  
    super (range);
    this.step = Math.abs (range[1] - range[0]) / step;

  } // StepEnhancement constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a step enhancement from the specified parameters.
   * The minimum and maximum data values are mapped to the range [0..1].
   *
   * @param range the enhancement range as [min, max].
   * @param steps the number of data steps in the range.
   */
  public StepEnhancement (
    double[] range,
    int steps
  ) { 
  
    super (range);
    this.step = 1.0 / steps;

  } // StepEnhancement constructor

  ////////////////////////////////////////////////////////////

  public double evaluate (
    double[] variables
  ) {

    double linear = super.evaluate (variables);
    return (Math.floor (linear / step) * step);

  } // evaluate

  ////////////////////////////////////////////////////////////

  /** Gets the number of steps in the enhancement. */
  public int getSteps () {

    return ((int) Math.round (1.0/step));

  } // getSteps

  ////////////////////////////////////////////////////////////

  /** Indicates whether some other object is "equal to" this one. */
  public boolean equals (
    Object obj
  ) { 

    if (!super.equals (obj)) return (false);
    StepEnhancement funcObj = (StepEnhancement) obj;
    if (funcObj.step != this.step) return (false);
    return (true);

  } // equals 

  ////////////////////////////////////////////////////////////

  public String describe () {

    return ("step" + getSteps() + (reverse ? "-reverse" : ""));

  } // describe

  ////////////////////////////////////////////////////////////

} // StepEnhancement class

////////////////////////////////////////////////////////////////////////
