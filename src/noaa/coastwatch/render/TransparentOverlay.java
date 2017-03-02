////////////////////////////////////////////////////////////////////////
/*

     File: TransparentOverlay.java
   Author: Peter Hollemans
     Date: 2006/07/10

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
