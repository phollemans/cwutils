////////////////////////////////////////////////////////////////////////
/*
     FILE: TransformableImageComponent.java
  PURPOSE: To define the methods for a transformable image component.
   AUTHOR: Peter Hollemans
     DATE: 2004/06/02
  CHANGES: 2006/12/14, PFH, added getImageAffine()

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.geom.*;

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
