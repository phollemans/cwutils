////////////////////////////////////////////////////////////////////////
/*

     File: ImageLoaderObserver.java
   Author: Peter Hollemans
     Date: 2003/01/19

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.awt.Dimension;
import java.awt.Image;

/**
 * An image loading observer is capable of drawing small tiles of an
 * image as it is being loaded by an image loader.
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public interface ImageLoaderObserver {

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the image dimensions.  The loader observer should make the
   * necessary arrangements for loading an image of this size.
   *
   * @param dims the new image dimensions.
   */
  public void setImageDims (
    Dimension dims
  );

  ////////////////////////////////////////////////////////////

  /**
   * Sets a tile in the image.  The loader observer should update
   * itself with the new image tile.
   *
   * @param tile the new image tile.
   * @param x the top-left x coordinate of the tile within the image.
   * @param y the top-left y coordinate of the tile within the image.
   */
  public void setImageTile (
    Image tile,
    int x,
    int y
  );

  ////////////////////////////////////////////////////////////

  /**
   * Signals that the image is finished loading.
   */
  public void setImageLoaded ();

  ////////////////////////////////////////////////////////////

} // ImageLoaderObserver interface

////////////////////////////////////////////////////////////////////////
