/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;
import javax.swing.Icon;

/**
 * The <code>TranslucentButton</code> class is a button whose painting
 * opacity may be controlled by an {@link OpacityHelper}.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 * @serial exclude
 */
public class TranslucentButton extends JButton implements TranslucentComponent {

  private OpacityHelper helper;

  /**
   * Creates a new translucent button.
   *
   * @param icon the button icon.
   */
  public TranslucentButton (Icon icon) {
    super (icon);
    setOpaque (false);
  } // TranslucentButton

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

} // TranslucentButton class
