////////////////////////////////////////////////////////////////////////
/*

     File: SymbolSwatch.java
   Author: Peter Hollemans
     Date: 2008/06/20

  CoastWatch Software Library and Utilities
  Copyright (c) 2008 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.Component;
import java.awt.Graphics;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
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

  // Constants
  // ---------
  
  /** The size of the border around the icon. */
  private static final int BORDER_SIZE = 3;

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

    // Draw symbol
    // -----------
    Graphics2D g2d = (Graphics2D) g;
    Stroke saved = g2d.getStroke();

    g2d.setStroke (new BasicStroke (1.2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
    g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    symbol.setSize (size - BORDER_SIZE*2);
    symbol.setBorderColor (c.getForeground());
    symbol.setFillColor (null);
    symbol.draw (g2d, x+size/2, y+size/2);

    g2d.setStroke (saved);

  } // paintIcon

  ////////////////////////////////////////////////////////////

  /** Sets the swatch symbol. */
  public void setSymbol (PlotSymbol symbol) { this.symbol = symbol; }

  ////////////////////////////////////////////////////////////

} // SymbolSwatch class

////////////////////////////////////////////////////////////////////////
