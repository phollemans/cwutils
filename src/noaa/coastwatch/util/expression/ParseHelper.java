////////////////////////////////////////////////////////////////////////
/*

     File: ParseHelper.java
   Author: Peter Hollemans
     Date: 2017/11/14

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
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * The <code>ParseHelper</code> class implements the {@link ParseImp} and
 * {@link EvaluateImp} interfaces with simple data structures.  All variables
 * are reported by {@link #typeOfVariable} as having type Double, and only
 * the {@link #getDoubleProperty} method is allowed to be used, all others
 * will throw an exception.  The public {@link #data} field can be used to
 * set values for all variables to be used in an evaluation.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ParseHelper implements ParseImp, EvaluateImp {

  // Variables
  // ---------

  /** The list of valid variable names. */
  private List<String> nameList;

  /** The map of variable name to index. */
  private Map<String, Integer> variableMap;

  /** The data values to use in evaluation. */
  public double[] data;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new <code>ParseHelper</code>.
   *
   * @param nameList the list of valid variable names.
   */
  public ParseHelper (
    List<String> nameList
  ) {

    this.nameList = nameList;
    variableMap = new HashMap<>();

  } // ParseHelper constructor

  ////////////////////////////////////////////////////////////

  @Override
  public int indexOfVariable (String varName) {

    Integer index = variableMap.get (varName);
    if (index == null) {
      if (nameList.contains (varName)) {
        index = variableMap.size();
        variableMap.put (varName, index);
      } // if
      else {
        index = -1;
      } // else
    } // if

    return (index);

  } // indexOfVariable

  ////////////////////////////////////////////////////////////

  @Override
  public String typeOfVariable (String varName) { return ("Double"); }

  ////////////////////////////////////////////////////////////

  public double getDoubleProperty (int varIndex) { return (data[varIndex]); }

  ////////////////////////////////////////////////////////////

} // ParseHelper class

////////////////////////////////////////////////////////////////////////
