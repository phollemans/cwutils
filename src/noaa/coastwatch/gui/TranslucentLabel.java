/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JLabel;
import javax.swing.Icon;

/**
 * The <code>TranslucentLabel</code> class is a label whose painting
 * opacity may be controlled by an {@link OpacityHelper}.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 * @serial exclude
 */
public class TranslucentLabel extends JLabel implements TranslucentComponent {

  private OpacityHelper helper;

  /**
   * Creates a new translucent label.
   *
   * @param icon the label icon.
   */
  public TranslucentLabel (Icon icon) {
    super (icon);
    setOpaque (false);
  } // TranslucentLabel

  @Override 
  protected void paintComponent (Graphics g) {
    Graphics2D g2d = (Graphics2D) g.create();
    if (helper != null) helper.setupGraphics (g2d);
    super.paintComponent (g2d);
    g2d.dispose();
  } // paintComponent

  /**
   * Sets the opacity helper.
   *
   * @param helper the opacity helper, or null for normal painting opacity.
   */
  @Override
  public void setHelper (OpacityHelper helper) { this.helper = helper; }

} // TranslucentLabel class
