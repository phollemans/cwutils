/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BoxLayout;

/**
 * The <code>TranslucentPanel</code> class is an on-screen style panel
 * with helper-controlled opacity.
 *
 * <p>If an opacity helper is set, the helper prepares the graphics
 * context before the panel background is painted.  This allows the panel
 * to share opacity behavior with child translucent controls that use the
 * same helper.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.1
 * @serial exclude
 */
public class TranslucentPanel extends OnScreenStylePanel implements TranslucentComponent {

  private OpacityHelper helper;

  /**
   * Creates a new translucent panel with a horizontal box layout.
   */
  public TranslucentPanel () { this (BoxLayout.X_AXIS); }

  /**
   * Creates a new translucent panel with the specified box layout axis.
   *
   * @param axis the box layout axis.
   */
  public TranslucentPanel (int axis) {
    super (axis);
    setOpaque (false);
  } // TranslucentPanel

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

  /**
   * Gets the opacity helper.
   *
   * @return the opacity helper, or null if none is set.
   */
  public OpacityHelper getHelper () { return (helper); }

} // TranslucentPanel class
