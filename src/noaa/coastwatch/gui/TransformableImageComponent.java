////////////////////////////////////////////////////////////////////////
/*

     File: TransformableImageComponent.java
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

// Imports
// -------
import java.awt.geom.AffineTransform;

/**
 * The <code>TransformableImageComponent</code> interface may be
 * implemented by graphical components that paint using an image.  The
 * image transform may be modified when painting using the
 * <code>setImageAffine()</code> method.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public interface TransformableImageComponent {

  ////////////////////////////////////////////////////////////

  /**
   * Sets the image affine transform.  The transform will be used to
   * transform the image during the next painting phase.
   *
   * @param affine the image affine transform, or null for no
   * transform.
   */
  public void setImageAffine (
    AffineTransform affine
  );

  ////////////////////////////////////////////////////////////

  /**
   * Gets the image affine transform in the view.
   *
   * @return the image affine transform or null for no transform.
   */
  public AffineTransform getImageAffine ();

  ////////////////////////////////////////////////////////////

} // TransformableImageComponent class

////////////////////////////////////////////////////////////////////////
