////////////////////////////////////////////////////////////////////////
/*
     FILE: SimpleSymbol.java
  PURPOSE: Renders a point feature as a plot symbol with label.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/22
  CHANGES: 2008/06/16, PFH, added test code

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import jahuwaldt.plot.PlotSymbol;

/**
 * A <code>SimpleSymbol</code> is a <code>PointFeatureSymbol</code>
 * that renders a <code>PlotSymbol</code> and optional text label.
 */
public class SimpleSymbol
  extends PointFeatureSymbol {

  // Variables
  // ---------

  /** The symbol used for plotting. */
  private PlotSymbol symbol;

  /** The attribute for deriving the text label, or -1 for no label. */
  private int attribute = -1;

  /** The text element to use for drawing the text label or null. */
  private TextElement element = null;

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
    this.element = new TextElement ("", font, new Point(), new double[] {0, 0},
      0);

  } // SimpleSymbol constructor

  ////////////////////////////////////////////////////////////

  public int getSize() { return (symbol.getSize()); }
  public void setSize (int size) { symbol.setSize (size); }
  public void setBorderColor (Color color) { symbol.setBorderColor (color); }
  public Color getBorderColor() { return (symbol.getBorderColor()); }
  public void setFillColor (Color color) { symbol.setFillColor (color); }
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

  /** Tests this class. */
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
