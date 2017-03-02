////////////////////////////////////////////////////////////////////////
/*

     File: SimpleSymbol.java
   Author: Peter Hollemans
     Date: 2005/05/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Iterator;
import javax.swing.JPanel;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.render.PlotSymbolFactory;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.PointFeatureSymbol;
import noaa.coastwatch.render.TextElement;

/**
 * A <code>SimpleSymbol</code> is a <code>PointFeatureSymbol</code>
 * that renders a <code>PlotSymbol</code> and optional text label.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class SimpleSymbol
  extends PointFeatureSymbol
  implements Cloneable {

  // Variables
  // ---------

  /** The symbol used for plotting. */
  private PlotSymbol symbol;

  /** The attribute for deriving the text label, or -1 for no label. */
  private int attribute = -1;

  /** The text element to use for drawing the text label or null. */
  private TextElement element = null;

  ////////////////////////////////////////////////////////////

  @Override
  public Object clone () {

    SimpleSymbol copy = (SimpleSymbol) super.clone();
    if (element != null)
      copy.element = new TextElement ("", element.getFont(), new Point(), new double[] {0, 0}, 0);
    return (copy);

  } // clone

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the plot symbol.
   *
   * @return the plot symbol.
   */
  public PlotSymbol getPlotSymbol () { return (symbol); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the plot symbol.
   *
   * @param symbol the plot symbol.
   */
  public void setPlotSymbol (PlotSymbol symbol) { this.symbol = symbol; }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new simple symbol with no text label. 
   *
   * @param symbol the plot symbol to use for plotting.
   */
  public SimpleSymbol (
    PlotSymbol symbol
  ) {

    this.symbol = symbol;

  } // SimpleSymbol constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new simple symbol with a text label.
   *
   * @param symbol the plot symbol to use for plotting.
   * @param attribute the feature attribute index to use for deriving
   * the text label.
   * @param font the font to use for the text label.
   */
  public SimpleSymbol (
    PlotSymbol symbol,
    int attribute,
    Font font
  ) {

    this.symbol = symbol;
    this.attribute = attribute;
    this.element = new TextElement ("", font, new Point(), new double[] {0, 0}, 0);

  } // SimpleSymbol constructor

  ////////////////////////////////////////////////////////////

  @Override
  public int getSize() { return (symbol.getSize()); }

  ////////////////////////////////////////////////////////////

  @Override
  public void setSize (int size) { symbol.setSize (size); }

  ////////////////////////////////////////////////////////////

  @Override
  public void setBorderColor (Color color) { symbol.setBorderColor (color); }

  ////////////////////////////////////////////////////////////

  @Override
  public Color getBorderColor() { return (symbol.getBorderColor()); }

  ////////////////////////////////////////////////////////////

  @Override
  public void setFillColor (Color color) { symbol.setFillColor (color); }

  ////////////////////////////////////////////////////////////

  @Override
  public Color getFillColor() { return (symbol.getFillColor()); }

  ////////////////////////////////////////////////////////////

  public void draw (
    Graphics gc, 
    int x, 
    int y
  ) {

    // Draw symbol
    // -----------
    symbol.draw (gc, x, y);

    // Draw label
    // ----------
    if (attribute != -1) {
      String text = feature.getAttribute (attribute).toString();
      element.setText (text);
      int size = symbol.getSize();
      Point base = new Point (x+size, y-size);
      element.setBasePoint (base);
      element.render ((Graphics2D) gc, symbol.getBorderColor(), Color.BLACK);
    } // if

  } // draw

  ////////////////////////////////////////////////////////////

  @Override
  public String toString () {

    StringBuffer buffer = new StringBuffer();
    buffer.append ("SimpleSymbol[");
    buffer.append ("symbol=" + symbol + ",");
    buffer.append ("size=" + symbol.getSize());
    buffer.append ("]");

    return (buffer.toString());

  } // toString

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
    
    final int symbolSize = 20;
    final int symbolStride = symbolSize*3/2;
    javax.swing.JPanel panel = new javax.swing.JPanel() {
        public void paintComponent (Graphics g) {
          super.paintComponent (g);
          int x = symbolStride, y = symbolStride;
          for (java.util.Iterator<String> iter = 
            PlotSymbolFactory.getSymbolNames(); iter.hasNext(); ) {
            String name = iter.next();
            SimpleSymbol symbol = new SimpleSymbol (
              PlotSymbolFactory.create (name));
            symbol.setSize (symbolSize);
            symbol.setBorderColor (g.getColor());
            
            for (Color fill : new Color[] {null, getForeground()}) {
              symbol.setFillColor (fill);
              System.out.println ("Drawing " + name + " symbol" + 
                (fill == null ? "" : " (filled)"));
              symbol.draw (g, x, y);
              x += symbolStride;
              if ((x + symbolStride) > 300) { 
                x = symbolStride; y += symbolStride; 
              } // if
            } // for

          } // for
        } // paintComponent
      };
    panel.setPreferredSize (new java.awt.Dimension (300, 200));
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // SimpleSymbol class

////////////////////////////////////////////////////////////////////////
