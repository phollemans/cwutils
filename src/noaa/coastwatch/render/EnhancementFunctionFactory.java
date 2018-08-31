////////////////////////////////////////////////////////////////////////
/*

     File: EnhancementFunctionFactory.java
   Author: Peter Hollemans
     Date: 2005/02/04

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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

/**
 * The <code>EnhancementFunctionFactory</code> creates enhancement
 * functions using a simple set of specifications.  This is useful
 * when receiving input from the user or a file, and the input must be
 * used to create a function.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class EnhancementFunctionFactory {

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new enhancement function based on a set of
   * specifications.
   *
   * @param functionType the type of function desired.  Supported
   * function types are 'linear', 'log', and 'stepN' where N is the
   * number of steps in the function, for example 'step10'.  Each type
   * may be extended with '-reverse' to indicate a reversal of the
   * range, ie: equivalent to calling {@link
   * EnhancementFunction#setReverse} after creation.
   * @param range the function range as [min, max].
   *
   * @return the new enhancement function.
   *
   * @throws IllegalArgumentException if the function type is not supported.
   */
  public static EnhancementFunction create (
    String functionType,
    double[] range
  ) {

    // Create function
    // ---------------
    EnhancementFunction function;
    if (functionType.startsWith ("linear"))
      function = new LinearEnhancement (range);
    else if (functionType.startsWith ("log"))
      function = new LogEnhancement (range);
    else if (functionType.startsWith ("step")) {
      function = new StepEnhancement (range, getSteps (functionType));
    } // else if
    else if (functionType.startsWith ("gamma")) {
      function = new GammaEnhancement (range);
    } // else if
    else {
      throw new IllegalArgumentException ("Invalid function '" + 
        functionType + "'");
    } // else

    // Set reverse
    // -----------
    if (functionType.endsWith ("-reverse"))
      function.setReverse (true);

    return (function);

  } // create

  ////////////////////////////////////////////////////////////

  /** Gets the number of steps for a step function type. */
  private static int getSteps (String functionType) {

    int start = 4;
    int end = functionType.indexOf ("-");
    if (end < 0) end = functionType.length();
    int steps = Integer.parseInt (functionType.substring (start, end));
    return (steps);

  } // getSteps

  ////////////////////////////////////////////////////////////

  /**
   * Converts an enhancement function to a new function type.  The new
   * function has the same range as the source function.
   *
   * @param source the source function to be converted.
   * @param functionType the new function type to convert to, either 'linear',
   * 'log', or 'stepN' where N is the step count.
   *
   * @return the new converted enhancement function.
   *
   * @throws IllegalArgumentException if the function type is not supported.
   *
   * @see #create
   */
  public static EnhancementFunction convert (
    EnhancementFunction source,
    String functionType
  ) {

    return (create (functionType + (source.getReverse() ? "-reverse" : ""),
      source.getRange()));

  } // convert

  ////////////////////////////////////////////////////////////

} // EnhancementFunctionFactory class

////////////////////////////////////////////////////////////////////////
