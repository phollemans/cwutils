/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * The <code>TranslucentComponent</code> interface identifies a component
 * whose painting opacity may be controlled by an {@link OpacityHelper}.
 *
 * <p>Implementations store the helper and use it during painting to set
 * up the graphics context before drawing.  Components that share the same
 * helper can therefore fade together.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public interface TranslucentComponent {

  /**
   * Sets the opacity helper.
   *
   * @param helper the opacity helper, or null for normal painting opacity.
   */
  void setHelper (OpacityHelper helper);

} // TranslucentComponent class

