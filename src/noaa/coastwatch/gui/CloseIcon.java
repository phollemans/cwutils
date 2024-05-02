////////////////////////////////////////////////////////////////////////
/*

     File: CloseIcon.java
   Author: Peter Hollemans
     Date: 2017/10/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;

/**
 * The <code>CloseIcon</code> renders an icon for a close button in one of
 * three modes: NORMAL, HOVER, and PRESSED.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 * 
 * @deprecated As of 3.8.1 use the {@link IconFactory} class.
 */
@Deprecated
public class CloseIcon implements Icon {

  /**
   * The mode constants for the icon rendering.  NORMAL renders just an "x",
   * HOVER renders an "x" and a square for when the mouse cursor is hovering
   * over the icon, and PRESSED for when the icon is pressed.
   */
  public enum Mode {NORMAL, HOVER, PRESSED};

  /**
   * The shape constants for the icon highlight rendeing.  SQUARE renders a 
   * square, ROUNDED renders a rounded rectangle, and CIRCLE renders a circle.
   */
  public enum Shape {SQUARE, ROUNDED, CIRCLE};

  /** The mode for icon rendering. */
  private Mode mode;
  
  /** The shape of the icon highlight. */
  private Shape shape;

  /** The size of the icon in pixels. */
  private int size;
    
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new icon.
   *
   * @param mode the mode for icon rendering.
   * @param shape the shape for icon highlight rendering.
   * @param size the icon size in pixels.
   */
  public CloseIcon (
    Mode mode,
    Shape shape,
    int size
  ) {
  
    this.mode = mode;
    this.shape = shape;
    this.size = size;
    
  } // CloseIcon constructor
  
  ////////////////////////////////////////////////////////////

  @Override
  public int getIconWidth() { return (size); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getIconHeight() { return (size); }

  ////////////////////////////////////////////////////////////

  @Override
  public void paintIcon (
    Component comp,
    Graphics g,
    int x,
    int y
  ) {

    Graphics2D g2d = (Graphics2D) g;

    // Setup the rendering
    // -------------------
    g2d.setStroke (new BasicStroke (1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
    g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Draw the highlight
    // ------------------
    Color fore = comp.getForeground();

    // if (!mode.equals (Mode.NORMAL)) {
    //   int alpha = (mode.equals (Mode.HOVER) ? 32 : 64);
    //   g2d.setColor (new Color (fore.getRed(), fore.getGreen(), fore.getBlue(), alpha));
    //   int diameter = size/4;
    //   g2d.fillRoundRect (x, y, size, size, diameter, diameter);
    // } // if

    var intensity = (int) (fore.getRed()*0.2989 + fore.getGreen()*0.5870 + fore.getBlue()*0.1140);
    var light = (intensity > 128);
    int alpha = 0;
    switch (mode) {
    case NORMAL: alpha = (light ? 64 : 0); break;
    case HOVER: alpha = (light ? 92 : 32); break;
    case PRESSED: alpha = (light ? 115 : 64); break;
    } // switch
    g2d.setColor (new Color (fore.getRed(), fore.getGreen(), fore.getBlue(), alpha));
    switch (shape) {
    case SQUARE: g2d.fillRect (x, y, size, size); break;
    case ROUNDED: g2d.fillRoundRect (x, y, size, size, size/4, size/4); break;
    case CIRCLE: g2d.fillOval (x, y, size, size); break;
    } // switch
    
    // Draw the "x"
    // ------------
    g2d.setColor (fore);
    int inc = (int) Math.round (size/3.0);
    g2d.drawLine (x+inc, y+inc, x+size-inc, y+size-inc);
    g2d.drawLine (x+size-inc, y+inc, x+inc, y+size-inc);

  } // paintIcon

  ////////////////////////////////////////////////////////////

} // CloseIcon class

////////////////////////////////////////////////////////////////////////


