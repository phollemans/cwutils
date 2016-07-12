////////////////////////////////////////////////////////////////////////
/*
     FILE: GIFWriter.java
  PURPOSE: Writes GIF format image files.
   AUTHOR: Peter Hollemans
     DATE: 2005/03/28
  CHANGES: 2016/02/22, PFH
           - Changes: Refactored the encode() method.
           - Issue: We wanted to have access to the quantization step from
             outside the class.

  CoastWatch Software Library and Utilities
  Copyright 1998-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import com.fmsware.gif.NeuQuant;
import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import no.geosoft.cc.io.GifEncoder;

/**
 * The <code>GIFWriter</code> class writes non-interlaced GIF87a
 * images from a rendered Java image.  If more than 256 colors are
 * found, an optimal 256 color map is generated prior to writing the
 * file.  The following web sites were used for neural network color
 * quantization and GIF encoding source code:
 * <ul>
 *   <li>http://www.fmsware.com</li>
 *   <li>http://geosoft.no</li>
 * </ul>
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class GIFWriter {

  // Variables
  // ---------

  /** The file output stream for writing. */
  private OutputStream output;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new writer using the specified output stream.
   * 
   * @param output the output stream for writing.
   */
  public GIFWriter (
    OutputStream output
  ) {

    this.output = output;

  } // GIFWriter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Determines if an image needs to be quantized before writing to
   * a GIF file.
   *
   * @param image the image to check.
   *
   * @return true if the image needs to be quantized prior to encoding, or 
   * false if not.
   *
   * @see #getQuantizedImage
   */
  public static boolean needsQuantization (
    BufferedImage image
  ) {

    // Check for indexed color model
    // -----------------------------
    boolean needsQuantization = true;
    ColorModel model = image.getColorModel();
    if (model instanceof IndexColorModel) {
      int mapSize = ((IndexColorModel) model).getMapSize();
      if (mapSize <= 256) needsQuantization = false;
    } // if

    // Check image colors
    // ------------------
    else {
      int width = image.getWidth();
      int height = image.getHeight();
      Set colorSet = new HashSet (256);
      image_loop: for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          Integer color = new Integer (image.getRGB (x, y));
          colorSet.add (color);
          if (colorSet.size() > 256) {
            needsQuantization = true;
            break image_loop;
          }  // if
        } // for
      } // for
    } // else

    return (needsQuantization);

  } // needsQuantization
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a GIF ready image by quantizing an image with a colour map that is
   * greater than 256 colours in size to an image with a 256 colour map.  
   * Some colours in the original are approximated in the quantized image.
   *
   * @param image the image to make GIF ready.
   *
   * @return the quantized image.
   *
   * @see #needsQuantization
   */
  public static BufferedImage getQuantizedImage (
    BufferedImage image
  ) {
  
    // Create byte data array
    // ----------------------
    int width = image.getWidth();
    int height = image.getHeight();
    int length = width*height*3;
    byte[] byteData = new byte[length];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int color = image.getRGB (x, y);
        int index = (y*width + x)*3;
        byteData[index] = (byte) ((color & 0xff0000) >>> 16);
        byteData[index+1] = (byte) ((color & 0xff00) >>> 8);
        byteData[index+2] = (byte) (color & 0xff);
      } // for
    } // for

    // Create quantized color map
    // --------------------------
    NeuQuant quantizer = new NeuQuant (byteData, length, 10);
    byte[] colorMap = quantizer.process();

    // Create new image
    // ----------------
    IndexColorModel colorModel = new IndexColorModel (8, 256, colorMap, 0, 
      false);
    image = new BufferedImage (width, height, 
      BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    WritableRaster raster = image.getRaster();
    byte[] byteRow = new byte[width];
    int index = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        byteRow[x] = (byte) quantizer.map (byteData[index] & 0xff, 
          byteData[index+1] & 0xff, byteData[index+2] & 0xff);
        index += 3;
      } // for
      raster.setDataElements (0, y, width, 1, byteRow);
    } // for

    return (image);

  } // getQuantizedImage

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a GIF file to the output stream using the specified image
   * data.
   *
   * @param image the image to write.
   *
   * @throws IOException if an error occurred writing to the output
   * stream.
   */
  public void encode (
    BufferedImage image
  ) throws IOException {

    // Get quantized image
    // -------------------
    if (needsQuantization (image))
      image = getQuantizedImage (image);

    // Write GIF image
    // ---------------
    try { 
      GifEncoder encoder = new GifEncoder (image);
      encoder.write (output);
    } // try
    catch (AWTException e) {
      throw new IOException ("Got AWTException: " + e.toString());
    } // catch

  } // encode

  ////////////////////////////////////////////////////////////

} // GIFWriter class

////////////////////////////////////////////////////////////////////////
