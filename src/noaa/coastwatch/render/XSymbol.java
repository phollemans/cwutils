////////////////////////////////////////////////////////////////////////
/*

     File: XSymbol.java
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
package noaa.coastwatch.render;

// Imports
// -------
import jahuwaldt.plot.PlotSymbol;
import jahuwaldt.plot.PlotSymbol;
import java.awt.Color;
import java.awt.Graphics;

/**
 * The <code>XSymbol</code> class draws an X for a point symbol.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class XSymbol
  extends PlotSymbol {

  /** The point array for drawing the filled version. */
  private int[] xPoints = new int[4];
  private int[] yPoints = new int[4];

  ////////////////////////////////////////////////////////////

  /**
   * Creates an X plot symbol object that has a width of 8
   * pixels, is transparent and has a border color of black.
   */
  public XSymbol () {}

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
    int width4 = (int) Math.round (getSize()/4 * 1.414);
		
    // Draw filled symbol
    // ------------------
    Color fillColor = getFillColor();
    if (fillColor != null) {
      gc.setColor (fillColor);
      xPoints[0] = x;           yPoints[0] = y - width4;
      xPoints[1] = x - width4;	yPoints[1] = y;
      xPoints[2] = x;           yPoints[2] = y + width4;
      xPoints[3] = x + width4;	yPoints[3] = y;
      gc.fillPolygon (xPoints, yPoints, 4);
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
      gc.drawLine(xmw, ymw, xpw, ypw);
      gc.drawLine(xpw, ymw, xmw, ypw);
    } // if
		
    gc.setColor(saveColor);

  } // draw

  ////////////////////////////////////////////////////////////
  	
} // XSymbol

////////////////////////////////////////////////////////////////////////
