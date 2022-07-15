////////////////////////////////////////////////////////////////////////
/*

     File: IconElement.java
   Author: Peter Hollemans
     Date: 2002/09/28

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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

// Imports
// -------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import noaa.coastwatch.render.GraphicsServices;
import noaa.coastwatch.render.PictureElement;

import java.util.logging.Logger;

/**
 * An icon element is a picture element that is rendered from image
 * data.  Generally, icons should be small and easily rendered.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class IconElement extends PictureElement {

  private static final Logger LOGGER = Logger.getLogger (IconElement.class.getName());

  /** The cached icon image. */
  private BufferedImage image;

  /** The shadow flag, true to draw a drop shadow. */
  private boolean shadow;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the drop shadow flag.
   * 
   * @param flag the drop shadow flag, true to draw a drop shadow or false 
   * to not.  By default the drop shadow is not drawn.
   * 
   * @since 3.7.1
   */
  public void setShadow (boolean flag) { shadow = flag; }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new icon element from the specified image stream.  The
   * position and preferred size are initialized to (0,0) and null.
   *
   * @param stream the icon image input stream.
   * 
   * @return the new icon element.
   *
   * @throws IOException if an error occurred reading the icon image.
   * 
   * @since 3.7.1
   */
  public static IconElement create (
    InputStream stream
  ) throws IOException {

    var element = new IconElement();
    element.image = ImageIO.read (stream);
    return (element);

  } // create

  ////////////////////////////////////////////////////////////

  protected IconElement () {

    super (new Point(), null);

  } // IconElement

  ////////////////////////////////////////////////////////////

  public void render (
    Graphics2D g,
    Color foreground,
    Color background
  ) {

    // Initialize
    // ----------
    Rectangle rect = getBounds (g);

    // Draw background
    // ---------------
    if (background != null) {
      g.setColor (background);
      g.fill (rect);
    } // if

    // When drawing the image to a raster device, it's best to scale the image
    // first and create the drop shadow if needed after scaling.  This is not
    // the recommended way to scale an image (see StackOverflow for the
    // opinions) but it works well enough.

    if (GraphicsServices.isRasterDevice (g)) {

      var scaled = image.getScaledInstance (rect.width, rect.height, Image.SCALE_AREA_AVERAGING);

      if (shadow) {

        var buffer = new BufferedImage (scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        var g2 = buffer.getGraphics();
        g2.drawImage (scaled, 0, 0, null);
        g2.dispose();

        int size = Math.max (2, buffer.getWidth()/30);
        var shadowImage = ShadowGenerator.getInstance().createShadow (buffer, size, Color.BLACK, 0.5f);
        g.drawImage (shadowImage, rect.x-size*2, rect.y-size*2, null);

      } // if

      g.drawImage (scaled, rect.x, rect.y, null);

    } // if

    // Otherwise if drawing to a non-raster, just create the drop shadow 
    // if needed and then render both shadow and icon together.

    else {

      if (shadow) {

        int size = Math.max (2, image.getWidth()/30);
        var shadowImage = ShadowGenerator.getInstance().createShadow (image, size, Color.BLACK, 0.5f);
        var g2 = shadowImage.getGraphics();
        g2.drawImage (image, size*2, size*2, null);
        g2.dispose();

        int offset = Math.round ((((float) size*2)/image.getWidth()) * rect.width);
        g.drawImage (shadowImage, rect.x-offset, rect.y-offset, rect.width+2*offset, rect.height+2*offset, null);

      } // if

      else {
        g.drawImage (image, rect.x, rect.y, rect.width, rect.height, null);
      } // else

    } // else

  } // render

  ////////////////////////////////////////////////////////////

  public Area getArea (
    Graphics2D g
  ) {

    // Create area
    // -----------
    int width = image.getWidth();
    int height = image.getHeight();
    if (preferred != null) {
      double scale = Math.min ((double) preferred.width/width, 
        (double) preferred.height/height);
      width = (int) Math.round (width*scale);
      height = (int) Math.round (height*scale);
    } // else
    return (new Area (new Rectangle ((int) Math.round (position.getX()), 
      (int) Math.round (position.getY()), width, height)));

  } // getArea

  ////////////////////////////////////////////////////////////

} // IconElement class

////////////////////////////////////////////////////////////////////////
