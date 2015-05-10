////////////////////////////////////////////////////////////////////////
/*
     FILE: PlotSymbolFactory.java
  PURPOSE: To manage a set of predefined written color palettes.
   AUTHOR: Peter Hollemans
     DATE: 2008/06/16

  CoastWatch Software Library and Utilities
  Copyright 1998-2008, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import jahuwaldt.plot.PlotSymbol;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The <code>PlotSymbolFactory</code> class supplies plot symbols
 * for scatter plots and point feature plots.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class PlotSymbolFactory { 

  // Constants
  // ---------

  /** The table of plot symbol names. */
  private final static String[] SYMBOL_NAMES = {
    "Cross",
    "X",
    "Square",
    "Circle",
    "Triangle Up",
    "Triangle Down",
    "Diamond"

    //    "BoxLL",
    //    "BoxLR",
    //    "BoxUL",
    //    "BoxUR",
    //    "Circle",
    //    "Diamond",
    //    "RTriangle1",
    //    "RTriangle2",
    //    "RTriangle3",
    //    "RTriangle4",
    //    "Square",
    //    "TabD",
    //    "TabL",
    //    "TabR",
    //    "TabUp",
    //    "ThinRect1",
    //    "ThinRect2",
    //    "Triangle1",
    //    "Triangle2",
    //    "Triangle3",
    //    "Triangle4",
    //    "X"

  };

  // Variables
  // ---------

  /** The map of symbol name to class. */
  private static LinkedHashMap<String,String> nameMap =
    new LinkedHashMap<String,String>();

  /** The cache of plot symbols. */
  private static HashMap<String,PlotSymbol> symbolCache = 
    new HashMap<String,PlotSymbol>();

  ////////////////////////////////////////////////////////////

  static {

    // Create map of symbol name to class
    // ----------------------------------
    nameMap.put ("Cross", "noaa.coastwatch.render.CrossSymbol");
    nameMap.put ("X", "noaa.coastwatch.render.XSymbol");
    nameMap.put ("Square", "jahuwaldt.plot.SquareSymbol");
    nameMap.put ("Circle", "jahuwaldt.plot.CircleSymbol");
    nameMap.put ("Triangle Up", "jahuwaldt.plot.Triangle1Symbol");
    nameMap.put ("Triangle Down", "jahuwaldt.plot.Triangle3Symbol");
    nameMap.put ("Diamond", "jahuwaldt.plot.DiamondSymbol");

  } // static

  ////////////////////////////////////////////////////////////

  /** Gets the list of plot symbol names. */
  public static Iterator<String> getSymbolNames () {

    return (Arrays.asList (SYMBOL_NAMES).iterator());

  } // getSymbolNames

  ////////////////////////////////////////////////////////////

  /**
   * Gets a plot symbol.
   *
   * @param name the plot symbol name.
   *
   * @return the plot symbol or null if no symbol was found with
   * the specified name.  The same plot symbol is returned for
   * any given call to this method, therefore methods should set
   * up the plot symbol prior to rendering.
   */
  public static PlotSymbol create (
    String name
  ) {

    PlotSymbol symbol = symbolCache.get (name);
    if (symbol == null) {
      try {
        symbol = (PlotSymbol) Class.forName (nameMap.get (
          name)).getConstructor().newInstance();
      } // try
      catch (Exception e) { }
      if (symbol != null) symbolCache.put (name, symbol);
    } // if
    return (symbol);

  } // create

  ////////////////////////////////////////////////////////////

} // PlotSymbolFactory class

////////////////////////////////////////////////////////////////////////
