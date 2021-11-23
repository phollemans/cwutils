
////////////////////////////////////////////////////////////////////////
/*

     File: JavaExpressionMaskOverlay.java
   Author: Peter Hollemans
     Date: 2021/11/12

  CoastWatch Software Library and Utilities
  Copyright (c) 2021 National Oceanic and Atmospheric Administration
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
import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.expression.ExpressionParserFactory;
import noaa.coastwatch.util.expression.ExpressionParserFactory.ParserStyle;
import noaa.coastwatch.util.expression.ExpressionParser;
import noaa.coastwatch.util.expression.ExpressionParser.ResultType;
import noaa.coastwatch.util.expression.ParseHelper;
import noaa.coastwatch.util.Grid;

/**
 * The <code>JavaExpressionMaskOverlay</code> class uses a mathematical
 * expression in Java syntax to compute a data mask.  If the expression
 * evaluates to true, then the data is masked, otherwise the data is
 * left visible.  A <code>JavaExpressionMaskOverlay</code> is thus a more
 * general type of {@link BitmaskOverlay}.
 *
 * @see noaa.coastwatch.tools.cwmath
 * @see ExpressionMaskOverlay
 *
 * @author Peter Hollemans
 * @since 3.7.1
 */
public class JavaExpressionMaskOverlay
  extends MaskOverlay
  implements GridContainerOverlay {

  // Variables
  // ---------

  /** The reader to use for data. */
  private transient EarthDataReader reader;

  /** The list of available data variables from the reader. */
  private transient List<String> variableList;

  /** The mask expression to use for each data location. */
  private String expression;

  /** The expression parser. */
  private transient ExpressionParser parser;

  /** The expression parser helper. */
  private transient ParseHelper helper;

  /** The input variable names for the current expression. */
  private transient String[] inputVarNames;

  /** The input variables for the current expression. */
  private transient Grid[] inputVars;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data source for grid data.  The reader and variable list
   * must contain data grids with the current set of expression
   * variables.
   *
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   */
  public void setDataSource (
    EarthDataReader reader,
    List variableList
  ) {

    this.reader = reader;
    this.variableList = variableList;
    String lastExpression = expression;
    this.expression = null;
    setExpression (lastExpression);

  } // setDataSource

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new overlay.  The layer number is initialized
   * to 0.
   *
   * @param color the overlay color.
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   * @param expression the mask expression.  Variables names in
   * the expression must have corresponding grids in the list.
   */
  public JavaExpressionMaskOverlay (
    Color color,
    EarthDataReader reader,
    List<String> variableList,
    String expression
  ) {

    super (color);
    this.reader = reader;
    this.variableList = variableList;
    setExpression (expression);

  } // JavaExpressionMaskOverlay

  ////////////////////////////////////////////////////////////

  /**
   * Sets the expression used by the mask.
   *
   * @param newExpression the new math expression.
   *
   * @throws IllegalArgumentException is the expression has a
   * parsing error or contains variables that are not available
   * in the current list of variables.
   */
  public void setExpression (
    String newExpression
  ) {

    // Check for the same expression
    // -----------------------------
    newExpression = newExpression.trim();
    if (newExpression.equals (expression)) return;

    // Handle the empty expression
    // ---------------------------
    if (newExpression.equals ("")) {
      this.expression = "";
      this.parser = null;
      this.helper = null;
      this.inputVarNames = new String[0];
      this.inputVars = new Grid[0];
      invalidate();
      return;
    } // if

    // Parse expression
    // ----------------
    helper = new ParseHelper (variableList);
    parser = ExpressionParserFactory.getFactoryInstance().create (ParserStyle.JAVA);
    parser.init (helper);
    parser.parse (newExpression);
    var resultType = parser.getResultType();
    if (resultType != ResultType.BOOLEAN) {
      throw new IllegalArgumentException ("Illegal expression result type '" +
        resultType + "', expecting a boolean result");
    } // if
    
    // Check if required variables are available
    // -----------------------------------------
    String[] inputVarNames = (String[]) parser.getVariables().toArray (new String[] {});
    Grid[] inputVars = new Grid[inputVarNames.length];
    for (int i = 0; i < inputVarNames.length; i++) {
      if (variableList.indexOf (inputVarNames[i]) == -1)
        throw new IllegalArgumentException ("Cannot find input variable for " + inputVarNames[i]);
      try { inputVars[i] = (Grid) reader.getVariable (inputVarNames[i]); }
      catch (IOException e) { throw (new RuntimeException (e)); }
    } // for

    // Set internal values
    // -------------------
    this.expression = newExpression;
    this.inputVarNames = inputVarNames;
    this.inputVars = inputVars;
    invalidate();

  } // setExpression

  ////////////////////////////////////////////////////////////

  /** Gets the current expression. */
  public String getExpression () { return (expression); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isMasked (
    DataLocation loc,
    boolean isNavigated
  ) {

    // Check for an active expression parser
    // -------------------------------------
    if (parser == null) return (false);

    // Put values in parser
    // --------------------
    helper.data = new double[inputVars.length];
    if (isNavigated) {
      int row = (int) loc.get (Grid.ROWS);
      int col = (int) loc.get (Grid.COLS);
      for (int i = 0; i < inputVars.length; i++) {
        helper.data[i] = inputVars[i].getValue (row, col);
      } // for
    } // if
    else {
      for (int i = 0; i < inputVars.length; i++) {
        helper.data[i] = inputVars[i].getValue (loc);
      } // for
    } // else

    // Compute expression value
    // ------------------------
    return (parser.evaluateToBoolean (helper));

  } // isMasked

  ////////////////////////////////////////////////////////////

  @Override
  protected boolean isCompatible (
    EarthDataView view
  ) {

    for (int i = 0; i < inputVars.length; i++) {
      if (!view.hasCompatibleCaches (inputVars[i])) return (false);
    } // for
    return (true);
  
  } // isCompatible

  ////////////////////////////////////////////////////////////

  @Override
  public List<Grid> getGridList () { return (Arrays.asList (inputVars)); }

  ////////////////////////////////////////////////////////////

} // ExpressionMaskOverlay class

////////////////////////////////////////////////////////////////////////
