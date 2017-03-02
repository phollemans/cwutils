////////////////////////////////////////////////////////////////////////
/*

     File: DelayedRenderingComponent.java
   Author: Peter Hollemans
     Date: 2004/06/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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

/**
 * The <code>DelayedRenderingComponent</code> interface may be used by
 * graphical components that wish to implement a delayed rendering
 * strategy.  If the component is currently in a rendering state,
 * users of the interface may stop the rendering by calling the
 * <code>stopRendering()</code> method.  Generally, delayed rendering
 * is only applicable if the component draws itself using a separate
 * rendering thread.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public interface DelayedRenderingComponent {

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the rendering state.
   * 
   * @return true if the component is in a rendering state, or false
   * if not.
   */
  public boolean isRendering ();

  ////////////////////////////////////////////////////////////

  /** 
   * Stops the current rendering thread if it is active.  If there is
   * no active rendering taking place, no operation is performed.
   */
  public void stopRendering ();

  ////////////////////////////////////////////////////////////

} // DelayedRenderingComponent class

////////////////////////////////////////////////////////////////////////
