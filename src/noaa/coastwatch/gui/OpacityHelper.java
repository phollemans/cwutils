/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.Graphics;

/**
 * The <code>OpacityHelper</code> is implemented by any class that needs 
 * to assist with storing/recalling an opacity value, and setting up a
 * graphics context with the opacity prior to rendering.
 */
public interface OpacityHelper {

  /**
   * Sets the opacity.
   *
   * @param alpha the opacity alpha value.
   */
  void setOpacity (float alpha);

  /**
   * Gets the opacity.
   *
   * @return the opacity alpha value.
   */
  float getOpacity();

  /**
   * Sets up a graphics context with the current opacity.
   *
   * @param graphics the graphics context.
   */
  void setupGraphics (Graphics graphics);

} // OpacityHelper
