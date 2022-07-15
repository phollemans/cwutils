/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.render;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;

import com.jhlabs.image.GaussianFilter;

// Testing
import java.io.File;
import javax.imageio.ImageIO;

/**
 * The <code>ShadowGenerator</code> class creates drop shadows from the 
 * non-transparent pixels in an image.
 * 
 * @author Peter Hollemans
 * @since 3.7.1
 */
public class ShadowGenerator {

  private static ShadowGenerator instance;

  protected ShadowGenerator() { }

  public static ShadowGenerator getInstance() {
    if (instance == null) instance = new ShadowGenerator();
    return (instance);
  } // getInstance

  /////////////////////////////////////////////////////////////////

  /** 
   * Creates a new shadow image from the pixels of a source image.
   * 
   * @param source the source image to use.
   * @param size the size of the shadow blur in pixels.
   * @param color the color of the shadow.
   * @param alpha the alpha transparency 
   * 
   * @return the shadow image.  The shadow is centered in the image, and a
   * border of 2*size pixels added on all sides.  The total size is 4*size pixels
   * larger in each dimension than the source image.
   */
  public BufferedImage createShadow (
    BufferedImage source, 
    int size, 
    Color color, 
    float alpha
  ) {

    // Create a new larger image of just the non-transparent pixels in the 
    // center.  This allows space for the fuzzy shadow pixels around the outside.

    int width = source.getWidth();
    int height = source.getHeight();

    int shadowWidth = width + (size*4);
    int shadowHeight = height + (size*4);

    var shadow = new BufferedImage (shadowWidth, shadowHeight, BufferedImage.TYPE_INT_ARGB);
    var g2 = shadow.createGraphics();

    g2.drawImage (source, size*2, size*2, null);
    g2.setComposite (AlphaComposite.getInstance (AlphaComposite.SRC_IN, alpha));
    g2.setColor (color);
    g2.fillRect (0, 0, shadowWidth, shadowHeight);
    g2.dispose();

    // Now perform the Gaussian blur to the shadow image.

    GaussianFilter filter = new GaussianFilter (size*2);
    shadow = filter.filter (shadow, null);

    return (shadow);

  } // createShadow

  /////////////////////////////////////////////////////////////////

  public static void main (String[] argv) throws Exception {

    var source = ImageIO.read (new File (argv[0]));
    int size = Math.max (2, source.getWidth()/30);
    var shadow = ShadowGenerator.getInstance().createShadow (source, size, Color.BLACK, 0.5f);
    var g2 = shadow.createGraphics();
    g2.drawImage (source, size*2, size*2, null);
    g2.dispose();
    ImageIO.write (shadow, "png", new File (argv[1]));

  } // main

  /////////////////////////////////////////////////////////////////

} // ShadowGenerator class

