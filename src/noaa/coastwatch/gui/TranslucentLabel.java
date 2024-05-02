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
 * 
 * 
 * 
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class TranslucentLabel extends JLabel implements TranslucentComponent {

  private OpacityHelper helper;

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

  @Override
  public void setHelper (OpacityHelper helper) { this.helper = helper; }

} // TranslucentLabel class
