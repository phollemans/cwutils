////////////////////////////////////////////////////////////////////////
/*
     FILE: ImageLoader.java
  PURPOSE: A class to handle asynchronous image loading.
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
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;
import javax.swing.SwingUtilities;

/**
 * The image loader class is used to render an image asychronously
 * from an image producer.  Each loader is coupled with an observer
 * object that displays the image data as it is being loaded.
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class ImageLoader
  implements ImageConsumer {

  // Variables
  // ---------
  /** The image for loading. */
  private Image image;

  /** The current image producer. */
  private ImageProducer producer;

  /** The current image dimensions. */
  private Dimension imageDims;

  /** The image loader observer. */
  private ImageLoaderObserver observer;

  ////////////////////////////////////////////////////////////////

  /** Gets the image dimensions. */
  public Dimension getDims () { 
  
    return (imageDims == null ? null : (Dimension) imageDims.clone()); 

  } // getDims

  ////////////////////////////////////////////////////////////

  /** Gets the image loading status. */
  public boolean getLoading () { return (producer != null); }

  ////////////////////////////////////////////////////////////

  /** Creates a new image loader for the specified image. */
  public ImageLoader (
    Image image,
    ImageLoaderObserver observer
  ) {

    this.image = image;
    producer = null;
    imageDims = null;
    this.observer = observer;

  } // ImageLoader

  ////////////////////////////////////////////////////////////

  /** Starts image loading. */
  public void startLoading () {   

    if (producer != null) return;
    producer = image.getSource();
    producer.startProduction (this);

  } // startLoading

  ////////////////////////////////////////////////////////////

  /** Stops image loading. */
  public void stopLoading () {

    if (producer != null) {
      producer.removeConsumer (this);
      producer = null;
    } // if
    observer.setImageLoaded();

  } // stopLoading

  ////////////////////////////////////////////////////////////

  public void imageComplete (
    int status
  ) { 

    stopLoading();

  } // imageComplete

  ////////////////////////////////////////////////////////////

  public void setColorModel (
    ColorModel model
  ) { }

  ////////////////////////////////////////////////////////////

  public void setDimensions (
    int width, 
    int height
  ) { 

    imageDims = new Dimension (width, height);
    observer.setImageDims (imageDims);

  } // setDimensions

  ////////////////////////////////////////////////////////////

  public void setHints (
    int hintflags
  ) { }

  ////////////////////////////////////////////////////////////

  public void setPixels (
    final int x, 
    final int y, 
    int w, 
    int h, 
    ColorModel model, 
    byte[] pixels, 
    int off, 
    int scansize
  ) {

    // Set image tile
    // --------------
    final Image tile = Toolkit.getDefaultToolkit().createImage (
      new MemoryImageSource (w, h, model, pixels, off, scansize));
    try {
      SwingUtilities.invokeAndWait (new Runnable() {
        public void run() { 
          observer.setImageTile (tile, x, y);
        } // run
      });
    } catch (Exception e) { }

  } // setPixels

  ////////////////////////////////////////////////////////////

  public void setPixels (
    final int x, 
    final int y, 
    int w, 
    int h, 
    ColorModel model, 
    int[] pixels, 
    int off, 
    int scansize
  ) { 

    // Set image tile
    // --------------
    final Image tile = Toolkit.getDefaultToolkit().createImage (
      new MemoryImageSource (w, h, model, pixels, off, scansize));
    try {
      SwingUtilities.invokeAndWait (new Runnable() {
        public void run() { 
          observer.setImageTile (tile, x, y);
        } // run
      });
    } catch (Exception e) { }

  } // setPixels

  ////////////////////////////////////////////////////////////
 
  public void setProperties (
    Hashtable props
  ) { }

  ////////////////////////////////////////////////////////////

} // ImageLoader class

////////////////////////////////////////////////////////////////////////


