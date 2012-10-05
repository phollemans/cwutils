////////////////////////////////////////////////////////////////////////
/*
     FILE: CrossSymbol.java
  PURPOSE: Draws a cross symbol for plotting.
   AUTHOR: Peter Hollemans
     DATE: 2008/06/19

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
import java.awt.*;

/**
 * The <code>CrossSymbol</code> class draws a cross for a point symbol.
 */
public class CrossSymbol
  extends PlotSymbol {

  ////////////////////////////////////////////////////////////

  /**
   * Creates an cross plot symbol object that has a width of 8
   * pixels, is transparent and has a border color of black.
   */
  public CrossSymbol () {}

  ////////////////////////////////////////////////////////////

  /**
   *  Draws a plot symbol consisting of a cross to the specified
   *  graphics context at the specified coordinates.
   *
   *  @param gc The graphics context where the symbol will be
   *  drawn.
   *  @param x The horizontal position of the center of the
   *  symbol.
   *  @param y The vertical position of the center of the symbol.
   */
  public void draw (
    Graphics gc, 
    int x, 
    int y
  ) {

    Color saveColor = gc.getColor();
    int width = getSize();
    int width2 = getSize()/2;
    int width4 = getSize()/4;
		
    // Draw filled symbol
    // ------------------
    Color fillColor = getFillColor();
    if (fillColor != null) {
      gc.setColor (fillColor);
      int xmw = x - width4;
      int ymw = y - width4;
      gc.fillRect (xmw, ymw, width2, width2);
    } // if

    // Draw symbol border
    // ------------------
    Color borderColor = getBorderColor();
    if (borderColor != null) {
      gc.setColor (borderColor);
      int xpw = x + width2;
      int xmw = x - width2;
      int ypw = y + width2;
      int ymw = y - width2;
      gc.drawLine (xmw, y, xpw, y);
      gc.drawLine (x, ymw, x, ypw);
    } // if
		
    gc.setColor(saveColor);

  } // draw

  ////////////////////////////////////////////////////////////
  	
} // CrossSymbol

////////////////////////////////////////////////////////////////////////
