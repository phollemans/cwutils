////////////////////////////////////////////////////////////////////////
/*

     File: ParseImp.java
   Author: Peter Hollemans
     Date: 2017/11/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.expression;

// Imports
// -------
import java.util.List;

/**
 * The <code>ParseImp</code> class defines an interface for all
 * classes that help parse mathematical expressions by setting up a
 * correspondence between variable names and their type and index value.
 * The variable's index value will be used in the subsequent evaluation
 * operation to retrieve the variable value.  The variable's type is used
 * in expression parsing to determine type conversions and the expression
 * output type.
 *
 * @see EvaluateImp
 * @see ExpressionParser#evaluateToDouble
 * @see ExpressionParser#evaluateToLong
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface ParseImp {
  
  /**
   * Gets the index of the specified variable.  The index is used to recall
   * the variable value from an {@link EvaluateImp} instance.
   *
   * @param varName the variable name.
   *
   * @return the variable index, or -1 if not available.
   */
  public int indexOfVariable (String varName);

  /**
   * Gets the type of a variable as a primitive wrapper class name.
   *
   * @param varName the variable name.
   *
   * @return the variable class name: Byte, Short, Integer, Long, Float,
   * or Double.  The returned value is null if the variable type is
   * unknown.
   */
  public String typeOfVariable (String varName);

} // ParseImp interface

////////////////////////////////////////////////////////////////////////
