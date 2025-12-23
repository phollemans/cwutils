////////////////////////////////////////////////////////////////////////
/*

     File: ExpressionMaskOverlay.java
   Author: Peter Hollemans
     Date: 2006/07/10

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import java.util.HashSet;
import java.util.List;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.GridContainerOverlay;
import noaa.coastwatch.render.MaskOverlay;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.expression.ExpressionParserFactory;
import noaa.coastwatch.util.Grid;
import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;

/**
 * The <code>ExpressionMaskOverlay</code> class uses a mathematical
 * expression to compute a data mask.  If the expression evaluates to
 * true or non-zero, then the data is masked, otherwise the data is
 * left visible.  An <code>ExpressionMaskOverlay</code> is thus a more
 * general type of {@link BitmaskOverlay}.
 *
 * @see noaa.coastwatch.tools.cwmath
 *
 * @author Peter Hollemans
 * @since 3.2.1
 * 
 * @deprecated As of 4.1.5 use the {@link JavaExpressionMaskOverlay} class.
 */
@Deprecated
public class ExpressionMaskOverlay 
  extends MaskOverlay
  implements GridContainerOverlay {

  /** The serialization constant. */
  private static final long serialVersionUID = 4881065199205028892L;

  // Variables
  // ---------

  /** The reader to use for data. */
  private transient EarthDataReader reader;

  /** The list of available data variables from the reader. */
  private transient List variableList;

  /** The mask expression to use for each data location. */
  private String expression;

  /** The expression parser. */
  private transient JEP parser;

  /** The input variable names for the current expression. */
  private transient String[] inputVarNames;

  /** The input variables for the current expression. */
  private transient Grid[] inputVars;

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isUpgradable() { return (true); }

  ////////////////////////////////////////////////////////////

  @Override
  public EarthDataOverlay getUpgraded() { 

    return (JavaExpressionMaskOverlay.upgradeFrom (this, reader, variableList)); 

  } // getUpgraded

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
  public ExpressionMaskOverlay (
    Color color,
    EarthDataReader reader,
    List variableList,
    String expression
  ) { 

    super (color);
    this.reader = reader;
    this.variableList = variableList;
    setExpression (expression);

  } // ExpressionMaskOverlay constructor

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
      this.inputVarNames = new String[0];
      this.inputVars = new Grid[0];
      invalidate();
      return;
    } // if

    // Parse expression
    // ----------------
    JEP parser = ExpressionParserFactory.getInstance();
    parser.parseExpression (newExpression);
    if (parser.hasError()) {
      throw new IllegalArgumentException ("Error parsing expression: " + 
        parser.getErrorInfo());
    } // if

    // Get required variable names
    // ---------------------------
    HashSet keySet = new HashSet (parser.getSymbolTable().keySet());
    keySet.remove ("e");
    keySet.remove ("pi");
    keySet.remove ("nan");
    String[] inputVarNames = (String[]) keySet.toArray (new String[] {});
    
    // Check if required variables are available
    // -----------------------------------------
    Grid[] inputVars = new Grid[inputVarNames.length];
    for (int i = 0; i < inputVarNames.length; i++) {
      if (variableList.indexOf (inputVarNames[i]) == -1) {
        throw new IllegalArgumentException ("Cannot find input variable for "+ 
          inputVarNames[i]);
      } // if
      try { inputVars[i] = (Grid) reader.getVariable (inputVarNames[i]); } 
      catch (IOException e) { throw (new RuntimeException (e)); }
    } // for

    // Set internal values
    // -------------------
    this.expression = newExpression;
    this.parser = parser;
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
    if (isNavigated) {
      int row = (int) loc.get (Grid.ROWS);
      int col = (int) loc.get (Grid.COLS);
      for (int i = 0; i < inputVars.length; i++) {
        parser.addVariable (inputVarNames[i], 
          inputVars[i].getValue (row, col));
      } // for
    } // if
    else {
      for (int i = 0; i < inputVars.length; i++) {
        parser.addVariable (inputVarNames[i], inputVars[i].getValue (loc));
      } // for
    } // else

    // Compute expression value
    // ------------------------
    return (parser.getValue() != 0);

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
