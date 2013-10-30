////////////////////////////////////////////////////////////////////////
/*
     FILE: ImageLoaderObserver.java
  PURPOSE: An interface for all image loader observers.
   AUTHOR: Peter Hollemans
     DATE: 2003/01/19
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2003, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;

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
