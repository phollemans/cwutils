////////////////////////////////////////////////////////////////////////
/*

     File: ExpressionFilter.java
   Author: Peter Hollemans
     Date: 2016/06/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2016 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util;

// Includes
// --------
import noaa.coastwatch.util.LocationFilter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.ExpressionParserFactory;
import noaa.coastwatch.util.Grid;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import org.nfunk.jep.JEP;

/**
 * The <code>ExpressionFilter</code> class detects locations
 * whose data variable values satisfy a mathematical expression.  If the 
 * expression evaluates to true at the given location, the filter returns 
 * true, otherwise it returns false.
 *
 * @see noaa.coastwatch.tools.cwmath
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class ExpressionFilter implements LocationFilter {

  // Variables
  // ---------

  /** The expression to use for each data location. */
  private String expression;

  /** The expression parser. */
  private transient JEP parser;

  /** The input variable names for the expression. */
  private String[] inputVarNames;

  /** The input variables for the expression. */
  private Grid[] inputVars;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new filter.
   *
   * @param reader the reader to use for data variables.
   * @param expression the mask expression.  Variables names in
   * the expression must have corresponding grids in the reader.
   */
  public ExpressionFilter (
    EarthDataReader reader,
    String expression
  ) { 

    // Parse expression
    // ----------------
    parser = ExpressionParserFactory.getInstance();
    parser.parseExpression (expression);
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
    inputVarNames = (String[]) keySet.toArray (new String[] {});
    
    // Check if required variables are available
    // -----------------------------------------
    inputVars = new Grid[inputVarNames.length];
    List gridList = null;
    try { gridList = reader.getAllGrids(); }
    catch (IOException e) { throw new RuntimeException (e); }
    for (int i = 0; i < inputVarNames.length; i++) {
      if (!gridList.contains (inputVarNames[i])) {
        throw new IllegalArgumentException ("Cannot find input variable for " +
          inputVarNames[i]);
      } // if
      try { inputVars[i] = (Grid) reader.getVariable (inputVarNames[i]); } 
      catch (IOException e) { throw (new RuntimeException (e)); }
    } // for

  } // ExpressionFilter constructor

  ////////////////////////////////////////////////////////////

  @Override
  public boolean useLocation (DataLocation loc) {

    // Get data values for expression
    // ------------------------------
    for (int i = 0; i < inputVars.length; i++) {
      parser.addVariable (inputVarNames[i], inputVars[i].getValue (loc));
    } // for
  
    // Evaluate expression
    // -------------------
    return (parser.getValue() != 0);
  
  } // useLocation

  ////////////////////////////////////////////////////////////

} // ExpressionFilter class

////////////////////////////////////////////////////////////////////////
