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

  void setOpacity (float alpha);
  float getOpacity();
  void setupGraphics (Graphics graphics);

} // OpacityHelper

