////////////////////////////////////////////////////////////////////////
/*
     FILE: GraphicsServices.java
  PURPOSE: A class to perform various static graphics functions.
   AUTHOR: Peter Hollemans
     DATE: 2002/10/09
  CHANGES: 2004/08/30, PFH, added supportsBinaryWithTransparency()
           2005/03/27, PFH, modified isRasterDevice() for PDF
           2006/05/03, PFH, enclosed PDF classes in try/catch statements
           2006/05/28, PFH, modified PDF class tests for applet performance
           2006/11/18, PFH, added drawRect()

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import com.lowagie.text.pdf.PdfGraphics2D;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;

/**
 * The graphics services class defines various static methods relating
 * to the java.awt.Graphics2D class.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class GraphicsServices {

  // Variables
  // ---------

  /** The PDF flag, true if we know about the PDF classes. */
  private static boolean havePdf;

  ////////////////////////////////////////////////////////////

  /** Tests for the <code>PdfGraphics2D</code> class. */
  static {

    try {
      Class pdfClass = Class.forName ("com.lowagie.text.pdf.PdfGraphics2D");
      havePdf = true;
    } // try
    catch (ClassNotFoundException e) {
      havePdf = false;
    } // catch

  } // static

  ////////////////////////////////////////////////////////////

  /** 
   * Determines if the graphics device supports colors with an alpha
   * component.
   */
  public static boolean supportsAlpha (
    Graphics2D g
  ) {

    // TODO: We could add an option here that returns true for PDF
    // alpha support as long as the user has specified that newer PDF
    // files may be generated.  Transparency support was added to PDF
    // in version 1.4 (corresponding to Acrobat Reader 5) although not
    // all PDF readers seem to support it correctly (try xpdf and ggv
    // for various levels of support).  Acrobat Reader 6 definitely
    // supports alpha rendering, but is not available under Linux.  So
    // for now, we aim for the greatest compatibility and "claim" that
    // PDF does not support alpha.

    if (havePdf) {
      if (g instanceof PdfGraphics2D) return (false);
      else return (true);
    } // if
    else {
      /*
       * If we get here, it means we don't know about the PDF
       * classes.  So, we conclude that transparency is supported.
       */
      return (true);
    } // else

  } // supportsAlpha

  ////////////////////////////////////////////////////////////

  /**
   * Determines if the graphics device is raster-based as opposed to
   * vector-based.
   */
  public static boolean isRasterDevice (
    Graphics2D g
  ) {

    // Check for PDF
    // -------------
    if (havePdf) {
      if (g instanceof PdfGraphics2D) return (false);
    } // if

    /*
     * If we get here, it means we don't know about the PDF
     * classes.  So, we keep going and check the device type.
     */

    // Check for raster device
    // -----------------------
    int deviceType;
    try { deviceType = g.getDeviceConfiguration().getDevice().getType(); }
    catch (Exception e) { return (false); }
    return (deviceType == GraphicsDevice.TYPE_IMAGE_BUFFER ||
            deviceType == GraphicsDevice.TYPE_RASTER_SCREEN);

  } // isRasterDevice

  ////////////////////////////////////////////////////////////

  /**
   * Determines if the graphics device supports a binary mask with a
   * transparent color.  If so, BufferedImage objects may be created
   * with a BufferedImage.TYPE_BYTE_BINARY type and 2 color model in
   * which one of the colors is transparent.  The resulting mask image
   * may be drawn over top of existing graphics content and the
   * existing graphics will show through where the mask is set to be
   * transparent.  This does not work with all Java implementations,
   * in which case a BufferedImage.TYPE_BYTE_INDEXED type must be used
   * which requires more space in memory.
   */
  public static boolean supportsBinaryWithTransparency (
    Graphics2D g
  ) {

    // Check for MacOS X
    // -----------------
    String osName = System.getProperty ("os.name").toLowerCase(); 
    boolean isMac = osName.startsWith ("mac os x");
    if (isMac) return (false);
    else return (true);

  } // supportsBinaryWithTransparency

  ////////////////////////////////////////////////////////////

  /**
   * Draws a rectangle using a series of lines.
   *
   * @param g the graphics device to draw to.
   * @param rect the rectangle to draw.
   */
  public static void drawRect (
    Graphics g,
    Rectangle rect
  ) {

    /**
     * Why do we even need this routine?  The alternative is to use
     * Graphics2D.draw(Shape).  The problem is that on the Mac (as of
     * OS 10.4.8 at least), rendering a rectangle shape to an
     * antialiased output device even with stroke normalization turned
     * on makes a fuzzy rectangle.  So we defer to this rectangle
     * drawing routine to give us normalized rectangle sides.
     */

    int x1 = rect.x;
    int x2 = rect.x + rect.width;
    int y1 = rect.y;
    int y2 = rect.y + rect.height;
    g.drawLine (x1, y1, x2, y1);
    g.drawLine (x2, y1, x2, y2);
    g.drawLine (x2, y2, x1, y2);
    g.drawLine (x1, y2, x1, y1);

  } // drawRect

  ////////////////////////////////////////////////////////////

  private GraphicsServices () { }

  ////////////////////////////////////////////////////////////

} // GraphicsServices class

////////////////////////////////////////////////////////////////////////
