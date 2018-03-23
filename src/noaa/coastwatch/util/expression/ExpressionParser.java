////////////////////////////////////////////////////////////////////////
/*

     File: ExpressionParser.java
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
import org.w3c.dom.Document;
import noaa.coastwatch.util.expression.ParseImp;
import noaa.coastwatch.util.expression.EvaluateImp;

/**
 * The <code>ExpressionParser</code> class defines an interface for all
 * classes that parse and evaluate mathematical expressions.  The expression
 * parser must initialize, parse, and then evaluate in that order.  The list
 * of variables is only available via {@link #getVariables} after parsing
 * is complete.<p>
 *
 * Expressions evaluate to a primitive type given by the {@link #getResultType}
 * method.  The result can be retrieved using the {@link #evaluate} method
 * to a primitive wrapper, or the extra overhead of creating a wrapped
 * primitive can be avoided by using one of the special primitive evaluate
 * methods to directly retrieve the result.  If the result type does not match
 * the method called, zero is returned (or false for a boolean).
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface ExpressionParser {

  /** The enumeration of the possible expression result types. */
  public enum ResultType {
    BOOLEAN,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE
  }; // ResultType enum

  /**
   * Determines if this parser instance is thread-safe.
   *
   * @return true is this parser instance is thread-safe, or false if not.
   */
  default public boolean isThreadSafe() { return (true); }

  /**
   * Initializes this parser with the specified implementation.
   *
   * @param parseImp the parser implementation to use for variable information
   * during parsing.
   */
  public void init (ParseImp parseImp);

  /**
   * Parses the specified expression.
   *
   * @param expr the expression to parse.
   *
   * @throws RuntimeException if an error occurred parsing the expression.
   */
  public void parse (String expr);

  /**
   * Adapts the parsed expression to a specific result type.
   *
   * @param type the result type to adapt the expression.  The result
   * type of the parser is set to the specified type.
   *
   * @throws UnsupportedOperationException if the result type of the parser
   * cannot be adapted.
   */
  default public void adapt (ResultType type) { throw new UnsupportedOperationException(); }

  /**
   * Translates the specified expression to Java Language syntax if possible.
   *
   * @param expr the expression to translate.
   *
   * @throws UnsupportedOperationException if the expression cannot be
   * translated, or expression translation is not supported.
   */
  default public String translate (String expr) { throw new UnsupportedOperationException(); }

  /**
   * Gets a document tree corresponding to the parsed expression.
   *
   * @return the document tree.
   */
  default public Document getParseTree () { throw new UnsupportedOperationException(); }

  /**
   * Gets the result data type.
   *
   * @return the result data type.
   */
  public ResultType getResultType();

  /**
   * Gets the list of variables used in the expression.
   *
   * @return the list of variable names.
   */
  public List<String> getVariables();

  /**
   * Evaluates the expression to a primitive wrapper object.
   *
   * @param evalImp the evalutation implementation that provides variable
   * values.
   *
   * @return the result value in a wrapper.
   */
  public Object evaluate (EvaluateImp evalImp);

  /**
   * Evaluates the expression to a boolean value.
   *
   * @param evalImp the evalutation implementation that provides variable
   * values.
   *
   * @return the boolean valued result, or false if the result type is not a
   * boolean.
   */
  default public boolean evaluateToBoolean (EvaluateImp evalImp) { return (false); }

  /**
   * Evaluates the expression to a byte value.
   *
   * @param evalImp the evalutation implementation that provides variable
   * values.
   *
   * @return the byte valued result, or zero if the result is not a byte.
   */
  default public byte evaluateToByte (EvaluateImp evalImp) { return ((byte) 0); }

  /**
   * Evaluates the expression to a short value.
   *
   * @param evalImp the evalutation implementation that provides variable
   * values.
   *
   * @return the short valued result, or zero if the result is not a short.
   */
  default public short evaluateToShort (EvaluateImp evalImp) { return ((short) 0); }

  /**
   * Evaluates the expression to an int value.
   *
   * @param evalImp the evalutation implementation that provides variable
   * values.
   *
   * @return the int valued result, or zero if the result is not an int.
   */
  default public int evaluateToInt (EvaluateImp evalImp) { return (0); }

  /**
   * Evaluates the expression to a long value.
   *
   * @param evalImp the evalutation implementation that provides variable
   * values.
   *
   * @return the long valued result, or zero if the result is not a long.
   */
  default public long evaluateToLong (EvaluateImp evalImp) { return (0L); }

  /**
   * Evaluates the expression to a float value.
   *
   * @param evalImp the evalutation implementation that provides variable
   * values.
   *
   * @return the float valued result, or zero if the result is not a float.
   */
  default public float evaluateToFloat (EvaluateImp evalImp) { return (0.0f); }

  /**
   * Evaluates the expression to a double value.
   *
   * @param evalImp the evalutation implementation that provides variable
   * values.
   *
   * @return the double valued result, or zero if the result is not a double.
   */
  default public double evaluateToDouble (EvaluateImp evalImp) { return (0.0); }

} // ExpressionParser interface

////////////////////////////////////////////////////////////////////////
