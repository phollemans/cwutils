/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Font;
import java.awt.Composite;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.UIManager;

// Testing

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import noaa.coastwatch.gui.WindowMonitor;

import java.util.logging.Logger;

/**
 * The <code>IconFactory</code> renders various icons for buttons that require
 * a normal, hover, and pressed mode.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class IconFactory {

  private static final Logger LOGGER = Logger.getLogger (IconFactory.class.getName());

  /**
   * The mode constants for the icon rendering.  NORMAL renders just the icon
   * shape.  HOVER renders a highlighted version of the icon for whern the
   * mouse cursor is opver it, and PRESSED renders a pressed icon.
   */
  public enum Mode {NORMAL, HOVER, PRESSED};

  /** The purpose constants for the icon rendering. */
  public enum Purpose {
    CLOSE_SQUARE, 
    CLOSE_ROUNDED, 
    CLOSE_CIRCLE,
    SEPARATOR_VERTICAL,
    SEPARATOR_HORIZONTAL
  };

  private static IconFactory instance;

  ////////////////////////////////////////////////////////////

  protected IconFactory () { }

  ////////////////////////////////////////////////////////////

  public static IconFactory getInstance() { 

    if (instance == null) instance = new IconFactory();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  public Icon createIcon (
    Purpose purpose,
    Mode mode,
    int size
  ) {

    var icon = new Icon () {

      @Override
      public int getIconWidth() { return (size); }

      @Override
      public int getIconHeight() { return (size); }

      @Override
      public void paintIcon (
        Component comp,
        Graphics g,
        int x,
        int y
      ) {

        switch (purpose) {

        case CLOSE_SQUARE:
        case CLOSE_ROUNDED:
        case CLOSE_CIRCLE:
          paintCloseIcon (comp, g, x, y, size, purpose, mode);
          break;

        case SEPARATOR_VERTICAL:
        case SEPARATOR_HORIZONTAL:
          paintSeparatorIcon (comp, g, x, y, size, purpose, mode);
          break;

        default:
          throw new IllegalStateException ("Case not handled for purpose " + purpose);

        } // switch

      } // paintIcon

    };

    return (icon);

  } // createIcon

  ////////////////////////////////////////////////////////////

  private void paintSeparatorIcon (
    Component comp,
    Graphics g,
    int x,
    int y,
    int size,
    Purpose purpose,
    Mode mode
  ) {

    Graphics2D g2d = (Graphics2D) g;

    g2d.setStroke (new BasicStroke (1.5f*size/16.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
    g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
    g2d.setColor (comp.getForeground());
//    int inc = (int) Math.round (size/4.0);
    int inc = 0;

    switch (purpose) {
    case SEPARATOR_VERTICAL: g2d.drawLine (x+size/2, y+inc, x+size/2, y+size-inc); break;
    case SEPARATOR_HORIZONTAL: g2d.drawLine (x+inc, y+size/2, x+size-inc, y+size/2); break;
    } // switch

  } // paintSeparatorIcon

  ////////////////////////////////////////////////////////////

  private void paintCloseIcon (
    Component comp,
    Graphics g,
    int x,
    int y,
    int size,
    Purpose purpose,
    Mode mode
  ) {

    Graphics2D g2d = (Graphics2D) g;

    // Set up to render with antialiasing for smoothness and a 1 pixel 
    // stroke.
    g2d.setStroke (new BasicStroke (1.5f*size/16.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
    g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // First draw the highlight color using the foreground as a hint.  For 
    // light foregrounds we use a slightly different set of alpha values
    // than for dark foregrounds.
    Color fore = comp.getForeground();
    var intensity = (int) (fore.getRed()*0.2989 + fore.getGreen()*0.5870 + fore.getBlue()*0.1140);
    var light = (intensity > 128);
    int alpha = 0;
    switch (mode) {
    case NORMAL: alpha = (light ? 64 : 0); break;
    case HOVER: alpha = (light ? 92 : 32); break;
    case PRESSED: alpha = (light ? 115 : 64); break;
    } // switch
    g2d.setColor (new Color (fore.getRed(), fore.getGreen(), fore.getBlue(), alpha));
    Shape shape = null;
    switch (purpose) {
    case CLOSE_SQUARE: shape = new Rectangle2D.Double (x, y, size, size); break;
    case CLOSE_ROUNDED: shape = new RoundRectangle2D.Double (x, y, size, size, size/4, size/4); break;
    case CLOSE_CIRCLE: shape = new Ellipse2D.Double (x, y, size, size); break;
    } // switch
    g2d.fill (shape);
    
    // Draw the x closing symbol in the foreground color.
    g2d.setColor (fore);
    int inc = (int) Math.round (size/3.0);
    g2d.drawLine (x+inc, y+inc, x+size-inc, y+size-inc);
    g2d.drawLine (x+size-inc, y+inc, x+inc, y+size-inc);

  } // paintCloseIcon

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    int size = 48;

    var panel = new JPanel (new BorderLayout());
    panel.setBorder (BorderFactory.createEmptyBorder (20, 20, 20, 20));
    panel.setBackground (Color.BLUE);

    var box = new OnScreenStylePanel (BoxLayout.X_AXIS);
    box.add (Box.createHorizontalStrut (10));
    for (var purpose : Purpose.values()) {
      var button = GUIServices.createOnScreenStyleButton (size, purpose);
      button.setForeground (Color.WHITE);
      box.add (button);
    } // for
    box.add (Box.createHorizontalStrut (10));
    panel.add (box, BorderLayout.CENTER);

    JFrame frame = new JFrame (IconFactory.class.getName());
    frame.addWindowListener (new WindowMonitor());
    frame.setContentPane (panel);
    frame.pack();

    SwingUtilities.invokeLater (() -> { frame.setVisible (true); });

  } // main

  ////////////////////////////////////////////////////////////

} // IconFactory class

////////////////////////////////////////////////////////////////////////


