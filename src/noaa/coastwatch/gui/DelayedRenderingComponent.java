////////////////////////////////////////////////////////////////////////
/*
     FILE: DelayedRenderingComponent.java
  PURPOSE: To define the methods for a delays rendering component.
   AUTHOR: Peter Hollemans
     DATE: 2004/06/02
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

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
 */
public interface DelayedRenderingComponent {

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if the component is in a rendering state, or false
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
