////////////////////////////////////////////////////////////////////////
/*
     FILE: SymbolSwatch.java
  PURPOSE: An icon showing a plot symbol.
   AUTHOR: Peter Hollemans
     DATE: 2008/06/20
  CHANGES: 2010/03/13, PFH, corrected Javadoc
           
  CoastWatch Software Library and Utilities
  Copyright 2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.*;
import javax.swing.*;
import jahuwaldt.plot.PlotSymbol;

/**
 * The <code>SymbolSwatch</code> class is an icon that shows a
 * plot symbol.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class SymbolSwatch 
  implements Icon { 

  // Variables
  // ---------

  /** The swatch symbol. */
  private PlotSymbol symbol;

  /** The icon size. */
  private int size;

  ////////////////////////////////////////////////////////////

  /** 
   * Create a new symbol swatch. 
   * 
   * @param symbol the symbol to display.
   * @param size the icon width and height.
   */
  public SymbolSwatch (
    PlotSymbol symbol,
    int size
  ) {

    this.symbol = symbol;
    this.size = size;

  } // SymbolSwatch

  ////////////////////////////////////////////////////////////
  
  /** Gets the icon width. */
  public int getIconWidth () { return (size); }

  ////////////////////////////////////////////////////////////

  /** Gets the icon height. */
  public int getIconHeight() { return (size); }
  
  ////////////////////////////////////////////////////////////

  /** Paints the icon to the specified graphics context. */
  public void paintIcon (Component c, Graphics g, int x, int y) { 

    // Draw line
    // ---------
    g.setColor (c.getForeground());
    symbol.setSize (size);
    symbol.draw (g, x+size/2, y+size/2);
    
  } // paintIcon

  ////////////////////////////////////////////////////////////

  /** Sets the swatch symbol. */
  public void setSymbol (PlotSymbol symbol) { this.symbol = symbol; }

  ////////////////////////////////////////////////////////////

} // SymbolSwatch class

////////////////////////////////////////////////////////////////////////
