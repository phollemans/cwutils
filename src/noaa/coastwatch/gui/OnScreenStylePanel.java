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
 * 
 * 
 * 
 * @since 3.8.1
 */
public class OnScreenStylePanel extends JPanel {

  private int diameter = 20;
  private boolean border = true;

  ////////////////////////////////////////////////////////////

  public OnScreenStylePanel () { this (BoxLayout.X_AXIS); }

  ////////////////////////////////////////////////////////////

  public OnScreenStylePanel (boolean border) { this(); this.border = border; }

  ////////////////////////////////////////////////////////////

  public OnScreenStylePanel (int axis) { 

    setLayout (new BoxLayout (this, axis));
    setOpaque (false);
    setBackground (new Color (30, 30, 30));

  } // OnScreenStylePanel

  ////////////////////////////////////////////////////////////

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

