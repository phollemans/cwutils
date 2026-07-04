/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.geom.RoundRectangle2D;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.JPanel;
import javax.swing.BoxLayout;

/**
 * The <code>OnScreenStylePanel</code> class draws a semi-transparent
 * dark background for controls displayed over a view.
 *
 * <p>The panel is non-opaque and uses a <code>BoxLayout</code>.  The
 * background is painted using the current background color with a fixed
 * alpha value.  Depending on the constructor, the painted area is either
 * a rounded rectangle with a border or a plain rectangle without a
 * border.</p>
 *
 * @since 3.8.1
 * @serial exclude
 */
public class OnScreenStylePanel extends JPanel {

  private int diameter = 20;
  private boolean border = true;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new on-screen style panel with a horizontal box layout.
   */
  public OnScreenStylePanel () { this (BoxLayout.X_AXIS); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new on-screen style panel with a horizontal box layout and the
   * specified border mode.
   *
   * @param border the border mode, true to paint a rounded border or false to
   * paint a rectangular background without a border.
   */
  public OnScreenStylePanel (boolean border) { this(); this.border = border; }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new on-screen style panel with the specified box layout axis.
   *
   * @param axis the box layout axis.
   */
  public OnScreenStylePanel (int axis) { 

    setLayout (new BoxLayout (this, axis));
    setOpaque (false);
    setBackground (new Color (30, 30, 30));

  } // OnScreenStylePanel

  ////////////////////////////////////////////////////////////

  /**
   * Sets the rounded corner diameter.
   *
   * @param diameter the rounded corner diameter.
   */
  public void setDiameter (int diameter) { this.diameter = diameter; }

  ////////////////////////////////////////////////////////////

  @Override 
  protected void paintComponent (Graphics g) {

    super.paintComponent (g);

    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    var size = getSize();
    var insets = getInsets();
    Shape shape;
    if (border) {
      shape = new RoundRectangle2D.Double (
        insets.left, insets.top, 
        size.width-1-(insets.left+insets.right), size.height-1-(insets.top+insets.bottom), 
        diameter, diameter
      );
    } // if
    else {
      shape = new Rectangle (
        insets.left, insets.top, 
        size.width-(insets.left+insets.right), size.height-(insets.top+insets.bottom) 
      );
    } // if

    var back = getBackground();
    var fill = new Color (back.getRed(), back.getGreen(), back.getBlue(), 192);
    g2d.setPaint (fill);
    g2d.fill (shape);

    if (border) {
      var line = new Color (back.getRed()*4, back.getGreen()*4, back.getBlue()*4, 192);
      g2d.setColor (line);
      g2d.setStroke (new BasicStroke (1.0f));
      g2d.draw (shape);
    } // if

    g2d.dispose();      

  } // paintComponent

  ////////////////////////////////////////////////////////////

} // OnScreenStylePanel class
