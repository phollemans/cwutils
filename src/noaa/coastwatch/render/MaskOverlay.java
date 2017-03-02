////////////////////////////////////////////////////////////////////////
/*

     File: MaskOverlay.java
   Author: Peter Hollemans
     Date: 2006/07/10

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.GraphicsServices;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.TransparentOverlay;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.Grid;

/**
 * A <code>MaskOverlay</code> annotates a data view using some
 * extra information to form a mask of opaque and transparent
 * pixels at each data location.  A <code>MaskOverlay</code>
 * would most often be used to mask certain parts of the data
 * view, such as land or cloud.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public abstract class MaskOverlay
  extends EarthDataOverlay
  implements TransparentOverlay {

  // Variables
  // ---------

  /** The inverse flag. */
  private boolean inverse;

  /** The buffered image used to store mask output. */
  private transient BufferedImage image;

  ////////////////////////////////////////////////////////////

  /** Gets the inverse flag. */
  public boolean getInverse () { return (inverse); }
      
  ////////////////////////////////////////////////////////////

  /**
   * Sets the inverse flag.  When true, the sense of the mask is
   * inverted and pixels that would normally be masked are not
   * masked and vice-versa.  By default the inverse flag is set
   * to false; this is the normal mask behaviour.
   *
   * @param flag the inverse flag value.
   */
  public void setInverse (
    boolean flag
  ) {

    inverse = flag;
    updateColorModel();

  } // setInverse

  ////////////////////////////////////////////////////////////

  /** 
   * Overrides the parent method to set the mask color by
   * updating the index color model as well.
   */
  public void setColor (
    Color color
  ) {

    super.setColor (color);
    updateColorModel();

  } // setColor

  ////////////////////////////////////////////////////////////

  /** 
   * Overrides the parent method to set the mask transparency by
   * updating the index color model as well.
   */
  public void setTransparency (
    int percent
  ) {

    super.setTransparency (percent);
    updateColorModel();

  } // setColor

  ////////////////////////////////////////////////////////////

  /**
   * Updates the index color model.  If there is an image
   * available, it is converted to the new color model.
   */
  private void updateColorModel () {

    if (image != null) {
      image = new BufferedImage (createColorModel (getColorWithAlpha(), 
        inverse), image.getRaster(), false, null);
    } // if

  } // updateColorModel

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new mask overlay.  The layer number is
   * initialized to 0.  
   * 
   * @param color the overlay color.
   */
  public MaskOverlay (
    Color color
  ) { 

    super (color);

  } // MaskOverlay constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new two-color indexed color model with one color as the
   * transparent color and the other color as the specified color.
   * Which is which depends on the inverse flag.  If inverse is false,
   * the color mapping is 0 = transparent and 1 = color, otherwise it
   * is 0 = color and 1 = transparent.
   *
   * @param color the color to use for the non-transparent color.
   * @param inverse the inverse flag, true to invert the transparent
   * and non-transparent colors.
   */
  public static IndexColorModel createColorModel (
    Color color,
    boolean inverse
  ) {

    // Check for null color
    // --------------------
    if (color == null) color = Color.BLACK;

    // Create map
    // ----------
    if (!inverse) {
      return (new IndexColorModel (1, 2, 
        new byte[] {0, (byte) color.getRed()}, 
        new byte[] {0, (byte) color.getGreen()},
        new byte[] {0, (byte) color.getBlue()},
        new byte[] {0, (byte) color.getAlpha()}));
    } // if
    else {
      return (new IndexColorModel (1, 2, 
        new byte[] {(byte) color.getRed(), 0}, 
        new byte[] {(byte) color.getGreen(), 0},
        new byte[] {(byte) color.getBlue(), 0},
        new byte[] {(byte) color.getAlpha(), 0}));
    } // else

  } // createColorModel

  ////////////////////////////////////////////////////////////

  /**
   * Prepares any data structures that may be necessary for
   * computing the mask values prior to running {@link #prepare}.
   * This should be overrideen in the child class if anything
   * needs to be done.
   */
  protected void prepareData () { }

  ////////////////////////////////////////////////////////////

  /**
   * Determines if the data location should be masked.
   *
   * @param loc the data location in question.
   * @param isNavigated the navigated flag, true if the data
   * location is pre-navigated
   *
   * @return true if the data should be masked at the location or
   * false if not.
   */
  public abstract boolean isMasked (
    DataLocation loc,
    boolean isNavigated
  );

  ////////////////////////////////////////////////////////////

  /**
   * Determines if the data view is compatible with this overlay.
   * If so, then the precomputed view coordinate cache tables
   * will be used to determine data locations for each view
   * point.  If not, then each view point is transformed
   * individually using the view's image transform.
   *
   * @param view the data view in question.
   *
   * @return true if the view has compatible data coordinate
   * caches for the data used to create the mask values in the
   * {@link #isMasked} method, or false if not.
   */
  protected abstract boolean isCompatible (
    EarthDataView view
  );

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Prepare data in the mask
    // ------------------------
    prepareData();

    // Initialize properties
    // ---------------------
    ImageTransform imageTrans = view.getTransform().getImageTransform();
    Dimension imageDims = imageTrans.getImageDimensions();

    // Create new image
    // ----------------
    if (GraphicsServices.supportsBinaryWithTransparency (g)) {
      image = new BufferedImage (imageDims.width, imageDims.height, 
        BufferedImage.TYPE_BYTE_BINARY, 
        createColorModel (getColorWithAlpha(), inverse));
    } // if
    else {
      image = new BufferedImage (imageDims.width, imageDims.height, 
        BufferedImage.TYPE_BYTE_INDEXED, 
        createColorModel (getColorWithAlpha(), inverse));
    } // else
    WritableRaster raster = image.getRaster();

    // Create byte row
    // ---------------
    byte[] byteRow = new byte[imageDims.width];
    int value;

    // Render using image transform
    // ----------------------------
    if (!isCompatible (view)) {
      Point point = new Point();
      for (point.y = 0; point.y < imageDims.height; point.y++) {

        // Render line
        // -----------
        for (point.x = 0; point.x < imageDims.width; point.x++) {
          DataLocation loc = imageTrans.transform (point);
          byteRow[point.x] = (byte) (isMasked (loc, false) ? 1 : 0);
        } // for
        raster.setDataElements (0, point.y, imageDims.width, 1, byteRow);

      } // for
    } // if

    // Render using cached coordinates
    // -------------------------------
    else {
      int lastGridRow = Integer.MIN_VALUE;
      for (int y = 0; y < imageDims.height; y++) {

        // Render line
        // -----------
        if (view.rowCache[y] != lastGridRow) {
          int lastGridCol = Integer.MIN_VALUE;
          byte byteValue = 0;
          DataLocation loc = new DataLocation (2);
          for (int x = 0; x < imageDims.width; x++) {
            if (view.colCache[x] != lastGridCol) {
              loc.set (Grid.ROWS, view.rowCache[y]);
              loc.set (Grid.COLS, view.colCache[x]);
              byteValue = (byte) (isMasked (loc, true) ? 1 : 0);
              lastGridCol = view.colCache[x];
            } // if
            byteRow[x] = byteValue;
          } // for
          lastGridRow = view.rowCache[y];
        } // if
        raster.setDataElements (0, y, imageDims.width, 1, byteRow);

      } // for
    } // else

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check for null color
    // --------------------
    if (getColor() == null) return;

    // Draw image
    // ----------
    g.drawImage (image, 0, 0, null);

  } // draw

  ////////////////////////////////////////////////////////////

  /**
   * Invalidates the overlay.  This causes the mask graphics to
   * be completely reconstructed upon the next call to {@link
   * #render}.
   */
  public void invalidate () {

    prepared = false;
    image = null;

  } // invalidate

  ////////////////////////////////////////////////////////////

} // MaskOverlay class

////////////////////////////////////////////////////////////////////////
