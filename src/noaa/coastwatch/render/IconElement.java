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

/**
 * An icon element is a picture element that is rendered from image
 * data.  Generally, icons should be small and easily rendered.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class IconElement
  extends PictureElement {

  // Constants
  // ---------
  
  /** A predefined NOAA logo icon. */
  public static final String NOAA = "noaa.gif";

  /** A predefined NWS logo icon. */
  public static final String NWS = "nws.gif";

  /** A predefined NASA logo icon. */
  public static final String NASA = "nasa.gif";

  /** A predefined US DOC logo icon. */
  public static final String DOC = "doc.gif";

  /** A predefined NOAA 3D logo icon. */
  public static final String NOAA3D = "noaa3d.gif";

  /** A predefined NOAA Sierra style logo icon. */
  public static final String NOAA_SIERRA = "noaa_sierra.png";

  /** A predefined NWS 3D logo icon. */
  public static final String NWS3D = "nws3d.gif";

  /** A predefined NASA 3D logo icon. */
  public static final String NASA3D = "nasa3d.gif";

  /** A predefined US DOC 3D logo icon. */
  public static final String DOC3D = "doc3d.gif";

  // Variables
  // ---------
  /** The cached icon image. */
  private BufferedImage image;

  /** The area averaging rendering flag. */
  private boolean areaAveraging;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new icon element from the specified properties.
   *
   * @param position the top-left corner position of the picture.
   * @param size the preferred size of the icon (see {@link
   * PictureElement#setPreferredSize}).
   * @param name the predefined icon name.
   *
   * @throws IOException if the icon file had input errors.
   */
  public IconElement ( 
    Point2D position,
    Dimension size,
    String name
  ) throws IOException {

    // Initialize
    // ----------
    super (position, size);
    InputStream stream = getClass().getResourceAsStream (name);
    if (stream == null)
      throw new IOException ("Cannot find resource '" + name + "'");
    image = ImageIO.read (stream);
    areaAveraging = true;

  } // IconElement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new icon element from the specified properties. The
   * position and preferred size are initialized to (0,0) and null.
   *
   * @param name the predefined icon name.
   *
   * @throws IOException if the icon file had input errors.
   */
  public IconElement ( 
    String name
  ) throws IOException {

    this (new Point(), null, name);

  } // IconElement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new icon element from the specified properties.
   *
   * @param position the top-left corner position of the picture.
   * @param size the preferred size of the icon (see {@link
   * PictureElement#setPreferredSize}).
   * @param file the icon file.
   *
   * @throws IOException if the icon file had input errors.
   */
  public IconElement ( 
    Point2D position,
    Dimension size,
    File file
  ) throws IOException {

    // Initialize
    // ----------
    super (position, size);
    image = ImageIO.read (file);
    areaAveraging = true;

  } // IconElement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new icon element from the specified properties.  The
   * position and preferred size are initialized to (0,0) and null.
   *
   * @param file the icon file.
   *
   * @throws IOException if the icon file had input errors.
   */
  public IconElement ( 
    File file
  ) throws IOException {

    this (new Point(), null, file);

  } // IconElement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new icon element from the specified properties.
   *
   * @param position the top-left corner position of the picture.
   * @param size the preferred size of the icon (see {@link
   * PictureElement#setPreferredSize}).
   * @param stream the icon input stream.
   *
   * @throws IOException if the icon file had input errors.
   */
  public IconElement ( 
    Point2D position,
    Dimension size,
    InputStream stream
  ) throws IOException {

    // Initialize
    // ----------
    super (position, size);
    image = ImageIO.read (stream);
    areaAveraging = true;

  } // IconElement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new icon element from the specified properties.  The
   * position and preferred size are initialized to (0,0) and null.
   *
   * @param stream the icon input stream.
   *
   * @throws IOException if the icon file had input errors.
   */
  public IconElement ( 
    InputStream stream
  ) throws IOException {

    this (new Point(), null, stream);

  } // IconElement constructor

  ////////////////////////////////////////////////////////////

  public void render (
    Graphics2D g,
    Color foreground,
    Color background
  ) {

    // Initialize
    // ----------
    Rectangle rect = getBounds(g);

    // Draw background
    // ---------------
    if (background != null) {
      g.setColor (background);
      g.fill (rect);
    } // if

    // Draw image
    // ----------
    if (GraphicsServices.isRasterDevice (g)) {
      g.drawImage (image.getScaledInstance (rect.width, rect.height, 
        Image.SCALE_AREA_AVERAGING), rect.x, rect.y, null);
    } // if
    else {
      g.drawImage (image, rect.x, rect.y, rect.width, rect.height, null);
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
