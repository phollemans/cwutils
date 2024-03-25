
////////////////////////////////////////////////////////////////////////
/*

     File: JELParser.java
   Author: Peter Hollemans
     Date: 2017/11/08

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
import java.util.ArrayList;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;

import noaa.coastwatch.util.expression.ExpressionParser;
import noaa.coastwatch.util.expression.ParseImp;
import noaa.coastwatch.util.expression.EvaluateImp;
import noaa.coastwatch.util.expression.ExpressionTest;
import noaa.coastwatch.util.EarthLocation;

import java.util.logging.Logger;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The <code>JELParser</code> class parses expressions using the Java
 * Expressions Library (JEL) by Konstantin L. Metlov available from
 * https://www.gnu.org/software/jel.  The syntax follows the
 * Java Language Specification and includes the full set of java.lang.Math
 * static methods and other useful constants and methods.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
@noaa.coastwatch.test.Testable
public class JELParser implements ExpressionParser {

  private static final Logger LOGGER = Logger.getLogger (JELParser.class.getName());

  // Variables
  // ---------

  /** The parse implementation to be used for variable indices and types. */
  private ParseImp parseImp;

  /** The JEL library for compiling. */
  private Library jelLib;

  /** The compiled JEL expression. */
  private CompiledExpression jelObj;

  /** The list of variables names in the expression. */
  private List<String> variableList;

  /** The currently compiled JEL expression as a string. */
  private String jelExpression;

  ////////////////////////////////////////////////////////////

  /** Handles variable name mapping for the JEL compiler. */
  private class VariableMap extends DVMap {

    /** Registers the variable in the variable list. */
    private void registerVariable (String varName) {
      if (!variableList.contains (varName))
        variableList.add (varName);
    } // registerVariable

    @Override
    public String getTypeName (String name) { return (parseImp.typeOfVariable (name)); }

    @Override
    public Object translate (String name) {
      registerVariable (name);
      return (parseImp.indexOfVariable (name));
    } // translate

  } // VariableMap class

  ////////////////////////////////////////////////////////////

  @Override
  public void init (ParseImp parseImp) {

    this.parseImp = parseImp;

    // Set up JEL library
    // ------------------
    Class[] staticClasses = new Class[] {
      Math.class,
      ExtrasLibrary.class
    };
    Class[] dynamicClasses = new Class[] {EvaluateImp.class};
    VariableMap varMap = new VariableMap();
    jelLib = new Library (staticClasses, dynamicClasses, null, varMap, null);

    try { jelLib.markStateDependent ("random", null); }
    catch (CompilationException e) {
      throw new RuntimeException ("Failed to set up random method");
    } // catch

  } // init

  ////////////////////////////////////////////////////////////

  @Override
  public void parse (String expr) {

    LOGGER.fine ("Parsing Java expression '" + expr + "'");

    // Parse the expression
    // --------------------
    variableList = new ArrayList<>();
    try { jelObj = Evaluator.compile (expr, jelLib); }
    catch (CompilationException e) {
      throw new RuntimeException (
        "Error parsing expression '" + expr +
        "' at column " + e.getColumn() + ", " + e.getMessage()
      );
    } // catch
    jelExpression = expr;

  } // parse

  ////////////////////////////////////////////////////////////

  @Override
  public void adapt (ResultType type) {

    // Check for valid expression
    // --------------------------
    if (jelExpression == null) throw new IllegalStateException();

    // Add a cast to expression and re-parse
    // -------------------------------------
    String cast = "(" + type.toString().toLowerCase() + ")";
    ResultType currentResultType = getResultType();
    String adapted;
    if (currentResultType == ResultType.BOOLEAN)
      adapted = cast + " ((" + jelExpression + ") ? 1 : 0)";
    else
      adapted = cast + " (" + jelExpression + ")";

    parse (adapted);
  
  } // adapt

  ////////////////////////////////////////////////////////////

  @Override
  public ResultType getResultType() {

    ResultType type;
    switch (jelObj.getType()) {
      case 0: type = ResultType.BOOLEAN; break;
      case 1: type = ResultType.BYTE; break;
      case 3: type = ResultType.SHORT; break;
      case 4: type = ResultType.INT; break;
      case 5: type = ResultType.LONG; break;
      case 6: type = ResultType.FLOAT; break;
      case 7: type = ResultType.DOUBLE; break;
      default:
        throw new RuntimeException ("Unsupported expression result type: " + jelObj.getTypeC());
    } // switch
  
    return (type);
    
  } // getResultType
  
  ////////////////////////////////////////////////////////////

  @Override
  public List<String> getVariables() { return (variableList); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getVariableType (String name) { return (parseImp.typeOfVariable (name)); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object evaluate (EvaluateImp evalImp) {

    Object resultValue;
    Object[] dynamicObjects = new Object[] {evalImp};
    try { resultValue = jelObj.evaluate (dynamicObjects); }
    catch (Throwable t) { throw new RuntimeException (t); }

    return (resultValue);

  } // evaluate

  ////////////////////////////////////////////////////////////

  @Override
  public boolean evaluateToBoolean (EvaluateImp evalImp) {

    boolean resultValue;
    Object[] dynamicObjects = new Object[] {evalImp};
    try { resultValue = jelObj.evaluate_boolean (dynamicObjects); }
    catch (Throwable t) { throw new RuntimeException (t); }

    return (resultValue);

  } // evaluateToBoolean
  
  ////////////////////////////////////////////////////////////

  @Override
  public byte evaluateToByte (EvaluateImp evalImp) {
  
    byte resultValue;
    Object[] dynamicObjects = new Object[] {evalImp};
    try { resultValue = jelObj.evaluate_byte (dynamicObjects); }
    catch (Throwable t) { throw new RuntimeException (t); }

    return (resultValue);
  
  } // evaluateToByte

  ////////////////////////////////////////////////////////////

  @Override
  public short evaluateToShort (EvaluateImp evalImp) {
  
    short resultValue;
    Object[] dynamicObjects = new Object[] {evalImp};
    try { resultValue = jelObj.evaluate_short (dynamicObjects); }
    catch (Throwable t) { throw new RuntimeException (t); }

    return (resultValue);

  } // evaluateToShort

  ////////////////////////////////////////////////////////////

  @Override
  public int evaluateToInt (EvaluateImp evalImp) {
  
    int resultValue;
    Object[] dynamicObjects = new Object[] {evalImp};
    try { resultValue = jelObj.evaluate_int (dynamicObjects); }
    catch (Throwable t) { throw new RuntimeException (t); }

    return (resultValue);
  
  } // evaluateToInt

  ////////////////////////////////////////////////////////////

  @Override
  public long evaluateToLong (EvaluateImp evalImp) {

    long resultValue;
    Object[] dynamicObjects = new Object[] {evalImp};
    try { resultValue = jelObj.evaluate_long (dynamicObjects); }
    catch (Throwable t) { throw new RuntimeException (t); }

    return (resultValue);

  } // evaluateToLong

  ////////////////////////////////////////////////////////////

  @Override
  public float evaluateToFloat (EvaluateImp evalImp) {

    float resultValue;
    Object[] dynamicObjects = new Object[] {evalImp};
    try { resultValue = jelObj.evaluate_float (dynamicObjects); }
    catch (Throwable t) { throw new RuntimeException (t); }

    return (resultValue);
  
  } // evaluateToFloat

  ////////////////////////////////////////////////////////////

  @Override
  public double evaluateToDouble (EvaluateImp evalImp) {
  
    double resultValue;
    Object[] dynamicObjects = new Object[] {evalImp};
    try { resultValue = jelObj.evaluate_double (dynamicObjects); }
    catch (Throwable t) { throw new RuntimeException (t); }

    return (resultValue);

  } // evaluateToDouble

  ////////////////////////////////////////////////////////////

  /**
   * Implements a number of additional constants and methods for
   * JEL expressions to use.
   */
  public static class ExtrasLibrary {
  
    /** The Not-a-Number value as a double. */
    public static double NaN = Double.NaN;
  
    /** Computes the inverse hyperbolic sin of a value. */
    public static double asinh (double x) { return (Math.log (x + Math.sqrt (x*x + 1.0))); }

    /** Computes the inverse hyperbolic cosine of a value. */
    public static double acosh (double x) { return (Math.log (x + Math.sqrt (x*x - 1.0))); }

    /** Computes the inverse hyperbolic tangent of a value. */
    public static double atanh (double x) { return (0.5*Math.log ((x + 1.0) / (x - 1.0))); }

    /** Computes a sum of values. */
    public static double sum (double[] values) {
      double sum = 0;
      for (int i = 0; i < values.length; i++) sum += values[i];
      return (sum);
    } // sum

    /** Determines if a double is the NaN value. */
    public static boolean isNaN (double value) { return (Double.isNaN (value)); }

    /** Computes the physical distance between two locations in kilometers. */
    public static double dist (double lat1, double lon1, double lat2, double lon2) {
      return (EarthLocation.distance (lat1, lon1, lat2, lon2));
    } // dist

    /**
     * Gets the index of the minimum value in the array, or -1 if there is no
     * minimum (ie: all values are NaN).
     */
    public static int indexOfMin (double[] values) {

      double min = Double.MAX_VALUE;
      int index = -1;
      
      for (int i = 0; i < values.length; i++) {
        if (values[i] < min) {
          min = values[i];
          index = i;
        } // if
      } // for
      
      return (index);

    } // indexOfMin

    /**
     * Gets the index of the maximum value in the array, or -1 if there is no
     * maximum (ie: all values are NaN).
     */
    public static int indexOfMax (double[] values) {

      double max = -Double.MAX_VALUE;
      int index = -1;
      
      for (int i = 0; i < values.length; i++) {
        if (values[i] > max) {
          max = values[i];
          index = i;
        } // if
      } // for
      
      return (index);

    } // indexOfMax

    /** Gets the specified value from the array, or NaN if the index is -1. */
    public static double getValue (int index, double[] array) {
      return (index == -1 ? Double.NaN : array[index]);
    } // getValue

    /** Gets the specified value from the array, or NaN if the index is -1. */
    public static float getValue (int index, float[] array) {
      return (index == -1 ? Float.NaN : array[index]);
    } // getValue

  } // ExtrasLibrary class

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (JELParser.class);

    List<ExpressionTest> testList = new ArrayList<>();

    double x = 3;
    double y = 7;
    double z = 11;
    double a = 0.5;
    double b = Double.NaN;
    String[] variables = new String[] {"x", "y", "z", "a", "b"};
    JELParser parser = new JELParser();
    double[] values = new double[] {x, y, z, a, b};
    testList.add (new ExpressionTest ("pow (x, y)", parser, variables, values, Math.pow (x, y)));
    testList.add (new ExpressionTest ("-x", parser, variables, values, -x));
    testList.add (new ExpressionTest ("y % x", parser, variables, values, y % x));
    testList.add (new ExpressionTest ("y / x", parser, variables, values, y / x));
    testList.add (new ExpressionTest ("x * y", parser, variables, values, x*y));
    testList.add (new ExpressionTest ("x + y", parser, variables, values, x+y));
    testList.add (new ExpressionTest ("x + y * z", parser, variables, values, x + y*z));
    testList.add (new ExpressionTest ("x * y + z", parser, variables, values, x*y + z));
    testList.add (new ExpressionTest ("x - y", parser, variables, values, x-y));
    testList.add (new ExpressionTest ("x <= y", parser, variables, values, 1));
    testList.add (new ExpressionTest ("x >= y", parser, variables, values, 0));
    testList.add (new ExpressionTest ("x < y", parser, variables, values, 1));
    testList.add (new ExpressionTest ("x > y", parser, variables, values, 0));
    testList.add (new ExpressionTest ("x != y", parser, variables, values, 1));
    testList.add (new ExpressionTest ("x == y", parser, variables, values, 0));
    testList.add (new ExpressionTest ("x > y && y < z", parser, variables, values, 0));
    testList.add (new ExpressionTest ("x > y || y < z", parser, variables, values, 1));
    testList.add (new ExpressionTest ("sin (a)", parser, variables, values, Math.sin (a)));
    testList.add (new ExpressionTest ("cos (a)", parser, variables, values, Math.cos (a)));
    testList.add (new ExpressionTest ("tan (a)", parser, variables, values, Math.tan (a)));
    testList.add (new ExpressionTest ("asin (a)", parser, variables, values, Math.asin (a)));
    testList.add (new ExpressionTest ("acos (a)", parser, variables, values, Math.acos (a)));
    testList.add (new ExpressionTest ("atan (a)", parser, variables, values, Math.atan (a)));
    testList.add (new ExpressionTest ("asinh (x)", parser, variables, values, Math.log(x + Math.sqrt(x*x + 1.0))));
    testList.add (new ExpressionTest ("acosh (x)", parser, variables, values, Math.log(x + Math.sqrt(x*x - 1.0))));
    testList.add (new ExpressionTest ("atanh (x)", parser, variables, values, 0.5*Math.log( (x + 1.0) / (x - 1.0) )));
    testList.add (new ExpressionTest ("abs (-x)", parser, variables, values, Math.abs (-x)));
    testList.add (new ExpressionTest ("random()", parser, variables, values, 0).not());
    testList.add (new ExpressionTest ("sqrt (x)", parser, variables, values, Math.sqrt (x)));
    testList.add (new ExpressionTest ("sum (x, y, z)", parser, variables, values, x+y+z));
    testList.add (new ExpressionTest ("E", parser, variables, values, Math.E));
    testList.add (new ExpressionTest ("PI", parser, variables, values, Math.PI));
    testList.add (new ExpressionTest ("NaN", parser, variables, values, Double.NaN));
    testList.add (new ExpressionTest ("isNaN (b)", parser, variables, values, 1));

    testList.forEach (test -> {
      logger.test ("parse \"" + test + "\"");
      test.run();
      assert (test.isCorrect());
      logger.passed();
    });

  } // main

  ////////////////////////////////////////////////////////////

} // JELParser class

////////////////////////////////////////////////////////////////////////

