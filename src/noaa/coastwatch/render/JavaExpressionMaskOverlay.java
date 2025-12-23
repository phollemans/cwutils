
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

  private static final long serialVersionUID = 2548077101262448802L;

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

  /** The legacy emulation mode. */
  private transient boolean emulated;

  ////////////////////////////////////////////////////////////

  /** 
   * Upgrades an expression mask overlay to this new class.
   * 
   * @param overlay the existing expression mask instance.
   * @param reader the data reader to use as the source.
   * @param variableList the variable list to use from the reader.
   * 
   * @return an upgraded instance of the expression mask.
   */
  public static JavaExpressionMaskOverlay upgradeFrom (
    ExpressionMaskOverlay overlay,
    EarthDataReader reader,
    List variableList
  ) {

    // Translate the legacy expression to Java syntax
    var helper = new ParseHelper (variableList);
    var emulationParser = ExpressionParserFactory.getFactoryInstance().create (ParserStyle.LEGACY_EMULATED);
    emulationParser.init (helper);
    var newExpression = emulationParser.translate (overlay.getExpression());
    newExpression = newExpression.replaceFirst ("^\\((.*)\\)$", "$1");

    // Construct the upgraded overlay and transfer properties
    var upgraded = new JavaExpressionMaskOverlay (
      overlay.getColor(),
      reader,
      variableList,
      newExpression      
    );
    upgraded.setLayer (overlay.getLayer());
    upgraded.setVisible (overlay.getVisible());
    upgraded.setName (overlay.getName());
    upgraded.setInverse (overlay.getInverse());
    upgraded.alpha = overlay.alpha;

    return (upgraded);

  } // upgradeFrom

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
   * Constructs a new overlay with optional emulation parsing.  The layer 
   * number is initialized to 0.
   *
   * @param color the overlay color.
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   * @param expression the mask expression.  Variables names in
   * the expression must have corresponding grids in the list.
   * @param emulated the emulation flag, true to parse expressions and
   * emulate the legacy syntax using the new Java parser, of false
   * to directly use Java style syntax parsing.
   *
   * @since 4.1.5
   */
  public JavaExpressionMaskOverlay (
    Color color,
    EarthDataReader reader,
    List<String> variableList,
    String expression,
    boolean emulated
  ) {

    super (color);
    this.reader = reader;
    this.variableList = variableList;
    this.emulated = emulated;
    setExpression (expression);

  } // JavaExpressionMaskOverlay

   ////////////////////////////////////////////////////////////

  /**
   * Set the legacy emulation flag, by default false.
   * 
   * @param emulated the emulation flag, true to parse expressions and
   * emulate the legacy syntax using the new Java parser, of false
   * to directly use Java style syntax parsing.
   *
   * @since 4.1.5
   */
  public void setLegacyEmulated (boolean emulated) {

    this.emulated = emulated;

  } // setLegacyEmulated

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
    if (emulated) {
      ExpressionParser emulationParser = ExpressionParserFactory.getFactoryInstance().create (ParserStyle.LEGACY_EMULATED);
      emulationParser.init (this.helper);
      newExpression = emulationParser.translate (newExpression);
    } // if
    parser = ExpressionParserFactory.getFactoryInstance().create (ParserStyle.JAVA);
    parser.init (helper);
    parser.parse (newExpression);
    var resultType = parser.getResultType();
    if (resultType != ResultType.BOOLEAN) {
      throw new IllegalArgumentException ("Illegal expression result type '" +
        resultType + "', expecting a boolean result");
    } // if
    
    // Get required variables as grids
    String[] inputVarNames = (String[]) parser.getVariables().toArray (new String[] {});
    Grid[] inputVars = new Grid[inputVarNames.length];
    for (int i = 0; i < inputVarNames.length; i++) {
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
