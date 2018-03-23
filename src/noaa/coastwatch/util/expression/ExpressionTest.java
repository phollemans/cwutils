
////////////////////////////////////////////////////////////////////////
/*

     File: ExpressionTest.java
   Author: Peter Hollemans
     Date: 2017/11/13

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
import noaa.coastwatch.util.expression.ExpressionParser;
import noaa.coastwatch.util.expression.ExpressionParser.ResultType;
import noaa.coastwatch.util.expression.ParseImp;
import noaa.coastwatch.util.expression.EvaluateImp;

/**
 * The <code>ExpressionTest</code> class test a parser with a given set of
 * inputs and expected result.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ExpressionTest implements Runnable {

  // Variables
  // ---------
  private String expression;
  private ExpressionParser parser;
  private String[] names;
  private double[] values;
  private double result;
  private boolean isNot;
  private boolean isCorrect;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new expression test.
   *
   * @param expression the expression to parse.
   * @param parser the parser implementation to use for the expression.
   * @param names the names of the variables available to the expression.
   * @param values the values of the variables.
   * @param result the expected result.
   */
  public ExpressionTest (
    String expression,
    ExpressionParser parser,
    String[] names,
    double[] values,
    double result
  ) {

    this.expression = expression;
    this.parser = parser;
    this.names = names;
    this.values = values;
    this.result = result;
    this.isNot = false;
  
  } // ExpressionTest constructor

  ////////////////////////////////////////////////////////////

  @Override
  public String toString () { return (expression); }

  ////////////////////////////////////////////////////////////

  @Override
  public void run () {

    ParseImp parseImp = new ParseImp () {
      public int indexOfVariable (String varName) {
        for (int i = 0; i < names.length; i++) if (names[i].equals (varName)) return (i);
        return (-1);
      } // indexOfVariable
      public String typeOfVariable (String varName) {
        int index = indexOfVariable (varName);
        if (index == -1) return (null);
        else return ("Double");
      } // typeOfVariable
    };
    EvaluateImp evalImp = new EvaluateImp () {
      public double getDoubleProperty (int varIndex) { return (values[varIndex]); }
    };
    parser.init (parseImp);

    parser.parse (expression);
    ResultType resultType = parser.getResultType();
    double result;
    if (resultType.equals (ResultType.BOOLEAN))
      result = (parser.evaluateToBoolean (evalImp) ? 1.0 : 0.0);
    else
      result = parser.evaluateToDouble (evalImp);

    if (Double.isNaN (this.result)) {
      isCorrect = Double.isNaN (result);
    } // if
    else
      isCorrect = (this.result == result);

    if (isNot) isCorrect = !isCorrect;
  
  } // run

  ////////////////////////////////////////////////////////////

  /**
   * Gets the result is correct flag.
   *
   * @return true if the result was correct after running the test
   * or false if not.
   */
  public boolean isCorrect() { return (isCorrect); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the opposite of this test.
   *
   * @return a test that will return true if this test returns false
   * and vice-versa.
   */
  public ExpressionTest not () {
  
    this.isNot = !this.isNot;
    return (this);
  
  } // not

  ////////////////////////////////////////////////////////////

} // ExpressionTest class

////////////////////////////////////////////////////////////////////////

