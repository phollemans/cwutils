////////////////////////////////////////////////////////////////////////
/*
     FILE: TransparentOverlay.java
  PURPOSE: A marker interface for transparent overlays.
   AUTHOR: Peter Hollemans
     DATE: 2006/07/10
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

/**
 * The <code>TransparentOverlay</code> interface is a marker
 * interface for {@link EarthDataOverlay} classes that use a
 * combination of opaque and transparent pixels.  Some code may
 * need to make special arrangements for handling the
 * transparency aspect of the overlays for certain output
 * devices.
 *
 * @see GraphicsServices#supportsAlpha
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public interface TransparentOverlay {

} // TransparentOverlay interface

////////////////////////////////////////////////////////////////////////
