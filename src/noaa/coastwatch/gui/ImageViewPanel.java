////////////////////////////////////////////////////////////////////////
/*

     File: ImageViewPanel.java
   Author: Peter Hollemans
     Date: 2003/01/18

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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import noaa.coastwatch.gui.ImageLoader;
import noaa.coastwatch.gui.ImageLoaderObserver;
import noaa.coastwatch.gui.LightTable;

/**
 * An image view panel displays a single image and allows zooming and
 * panning with the mouse.
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class ImageViewPanel
  extends JPanel {

  // Constants
  // ---------
  /** 
   * No operation mode.  No operations are performed in response to
   * the mouse.
   *
   * @see #setViewMode
   */
  public static final int NOOP_MODE = 0;

  /** 
   * Image zoom mode.  In zoom mode, the image may be zoomed by
   * clicking and dragging a zoom box.
   *
   * @see #setViewMode
   */
  public static final int ZOOM_MODE = 1;
  
  /** 
   * Image pan mode.  In pan mode, the image may be panned by
   * clicking and dragging the image.
   *
   * @see #setViewMode
   */
  public static final int PAN_MODE = 2;

  // Variables
  // ---------

  /** The buffered image to display. */
  private BufferedImage image;

  /** The buffer image graphics for drawing. */
  private Graphics2D imageGraphics;

  /** The current affine transform. */
  private AffineTransform trans;

  /** The panel dimensions. */
  private Dimension panelDims;

  /** The image loading object. */
  private ImageLoader loader;

  /** The image manipulation mode. */
  private int mode;

  /** The light table used for view manipulation. */
  private LightTable lightTable;

  /** The image panel for displaying the image. */
  private JPanel imagePanel;

  /** The ready flag, true if the image is loaded and ready. */
  private boolean ready;

  ////////////////////////////////////////////////////////////

  /** Repaints the image panel after changes in the image. */
  private void repaintImage () {

    imagePanel.repaint (imagePanel.getVisibleRect());

  } // repaintImage

  ////////////////////////////////////////////////////////////

  /** 
   * The view image loader observer handles image loading by painting
   * tiles to the view as the image is loaded.
   */
  private class LoaderObserver
    implements ImageLoaderObserver {

    // Constants
    // ---------
    /** The update frequency as a fraction of the total image size. */
    public static final double UPDATE_FRACTION = 0.2;

    // Variables
    // ---------
    /** The image pixels loaded since the last update. */
    private int pixels;

    /** The image pixel update threshold. */
    private int updatePixels;

    ////////////////////////////////////////////////////////

    public void setImageLoaded () {

      ready = true;
      if (mode != NOOP_MODE) lightTable.setActive (true);
      repaintImage();

    } // setImageLoaded

    ////////////////////////////////////////////////////////

    public void setImageTile (
      Image tile,
      int x,
      int y
    ) {

      // Save tile to buffered image
      // ---------------------------
      imageGraphics.drawImage (tile, x, y, null);

      // Draw buffered image to screen
      // -----------------------------
      pixels += tile.getWidth(null) * tile.getHeight(null);
      if (pixels >= updatePixels) {
        repaintImage();
        pixels = 0;
      } // if

    } // setImageTile

    ////////////////////////////////////////////////////////

    public void setImageDims (
      Dimension dims
    ) {

      // Create new buffered image
      // -------------------------
      if (image != null) image.flush();
      image = new BufferedImage (dims.width, dims.height,
        BufferedImage.TYPE_INT_ARGB);
      imageGraphics = image.createGraphics();

      // Calculate update pixels
      // -----------------------
      pixels = 0;
      updatePixels = (int) (dims.width * dims.height * UPDATE_FRACTION);

      // Reset the affine transform
      // --------------------------
      resetTransform (dims);

    } // setImageDims

    ////////////////////////////////////////////////////////

  } // LoaderObserver class

  ////////////////////////////////////////////////////////////

  /** Creates a new empty image view panel. */
  public ImageViewPanel () {

    super (new BorderLayout());

    // Initialize
    // ----------
    image = null;
    trans = new AffineTransform();
    panelDims = null;
    loader = null;
    mode = NOOP_MODE;
    ready = false;

    // Setup light table
    // -------------------
    imagePanel = new ImagePanel();
    lightTable = new LightTable (imagePanel);
    lightTable.addChangeListener (new DrawListener()); 
    lightTable.setActive (false);
    this.add (lightTable, BorderLayout.CENTER);

  } // ImageViewPanel constructor

  ////////////////////////////////////////////////////////////

  public void setForeground (Color color) {

    super.setForeground (color);
    if (lightTable != null) lightTable.setForeground (color);

  } // setBackground

  ////////////////////////////////////////////////////////////

  public void setBackground (Color color) {

    super.setBackground (color);
    if (lightTable != null) lightTable.setBackground (color);

  } // setBackground

  ////////////////////////////////////////////////////////////

  /** Resets the image view to standard magnification. */
  public void reset () { 

    // Check for ready state
    // ---------------------
    if (!ready) return;

    // Modify transform
    // ----------------
    resetTransform (new Dimension (image.getWidth(null), 
      image.getHeight(null)));
    repaintImage();

  } // reset

  ////////////////////////////////////////////////////////////

  /** Magnifies the view by the specified scale factor. */
  public void magnify (
    double factor
  ) {

    // Check for ready state
    // ---------------------
    if (!ready) return;

    // Translate to origin
    // -------------------
    double centerX = (getWidth()-1)/2.0;
    double centerY = (getHeight()-1)/2.0;
    trans.preConcatenate (AffineTransform.getTranslateInstance (
      -centerX, -centerY));

    // Scale and translate back
    // ------------------------
    trans.preConcatenate (AffineTransform.getScaleInstance (factor, factor));
    trans.preConcatenate (AffineTransform.getTranslateInstance (
      centerX, centerY));

    repaintImage();

  } // magnify

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the light table. */
  private class DrawListener
    implements ChangeListener {

    public void stateChanged (ChangeEvent event) {

      // Check for ready state
      // ---------------------
      if (!ready) return;

      switch (mode) {

      // Zoom in on box
      // --------------
      case ZOOM_MODE:

        // Check if box is empty
        // ---------------------
        Rectangle2D rect = (Rectangle2D) lightTable.getShape();
        int width = (int) rect.getWidth();
        int height = (int) rect.getHeight();
        if (width == 0 || height == 0) return;

        // Change transform
        // ----------------
        trans.preConcatenate (AffineTransform.getTranslateInstance (
          -rect.getCenterX(), -rect.getCenterY()));
        double s = Math.min ((double) getWidth() / width,
          (double) getHeight() / height);
        trans.preConcatenate (AffineTransform.getScaleInstance (s, s));
        trans.preConcatenate (AffineTransform.getTranslateInstance (
          (getWidth()-1)/2.0, (getHeight()-1)/2.0));

        repaintImage();
        break;

      // Pan to new area
      // ---------------
      case PAN_MODE:

        // Get line
        // --------
        Line2D line = (Line2D) lightTable.getShape();
        Point2D p1 = line.getP1();
        Point2D p2 = line.getP2();

        // Change transform
        // ----------------
        trans.preConcatenate (AffineTransform.getTranslateInstance (
          p2.getX() - p1.getX(), p2.getY() - p1.getY()));

        repaintImage();
        break;

      } // switch

    } // stateChanged

  } // DrawListener class

  ////////////////////////////////////////////////////////////

  /**
   * Resets the affine transform to fit the entire image into the
   * view using the specified dimensions.
   */
  private void resetTransform (
    Dimension imageDims
  ) {

    // Get scaling
    // -----------
    double s = Math.min ( 
      (double) getWidth() / imageDims.width,
      (double) getHeight() / imageDims.height
    );
    AffineTransform scale = AffineTransform.getScaleInstance (s, s);

    // Get translation
    // ---------------
    double tx = getWidth()/2.0 - imageDims.width*s / 2;
    double ty = getHeight()/2.0 - imageDims.height*s / 2;
    AffineTransform translate = AffineTransform.getTranslateInstance (tx, ty);

    // Create concatenated transform
    // -----------------------------
    trans = new AffineTransform();
    trans.preConcatenate (scale);
    trans.preConcatenate (translate);

  } // resetTransform

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the view image.  If the image data is not available, it is
   * fetched from the image producer and rendered to the view
   * asynchronously.  If the image data is available, it is rendered
   * immediately.
   *
   * @param viewImage the new view image, or null for no image.
   */
  public void setImage (
    Image viewImage
  ) {

    // Stop all drawing activity
    // -------------------------
    lightTable.setActive (false);

    // Stop the current loader
    // -----------------------
    if (loader != null && loader.getLoading())
      loader.stopLoading();
    if (image != null) image.flush();
      image = null;

    // Set view to blank
    // -----------------
    if (viewImage == null) {
      loader = null;
      ready = false;
    } // if

    // Create image loader
    // -------------------
    else {
      loader = new ImageLoader (viewImage, new LoaderObserver());
      ready = false;
    } // else

    // Repaint view
    // ------------
    repaintImage();

    // Start the loader
    // ----------------
    if (loader != null) loader.startLoading();

  } // setImage

  ////////////////////////////////////////////////////////////

  /**
   * The <code>ImagePanel</code> class is the actual image display
   * panel component that is a child of the drawing light table.
   */
  private class ImagePanel extends JPanel {

    public void paintComponent (
      Graphics g
    ) {
      
      // Check showing
      // -------------
      if (!isShowing()) return;

      // Call super
      // ----------
      super.paintComponent (g);

      // Change transform to fit panel
      // -----------------------------
      Dimension newPanelDims = getSize();
      if (panelDims == null || !newPanelDims.equals (panelDims)) {
        panelDims = newPanelDims;
        if (image != null) {
          Dimension imageDims = new Dimension (image.getWidth(), 
            image.getHeight());
          resetTransform (imageDims);
        } // if
      } // if
      
      // Render image
      // ------------
      if (image != null) {
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform original = g2d.getTransform();
        g2d.transform (trans);
        g2d.drawImage (image, 0, 0, null);
        g2d.setTransform (original);
      } // if

    } // paintComponent

  } // ImagePanel class

  ////////////////////////////////////////////////////////////

  /**
   * Sets the image view manipulation mode.  By default, the no
   * operation mode is in effect.
   *
   * @param mode the image mode.
   */
  public void setViewMode (
    int mode
  ) {

    this.mode = mode;
    switch (mode) {

    // Change to box zoom
    // ------------------
    case ZOOM_MODE: 
      lightTable.setDrawingMode (LightTable.BOX_ZOOM_MODE);
      setCursor (Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
      if (ready) lightTable.setActive (true);
      break;

    // Change to image pan
    // -------------------
    case PAN_MODE: 
      lightTable.setDrawingMode (LightTable.IMAGE_MODE);
      setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));
      if (ready) lightTable.setActive (true);
      break;

    default: 
      lightTable.setActive (false);
      setCursor (null);
      break;

    } // switch

  } // setViewMode

  ////////////////////////////////////////////////////////////

} // ImageViewPanel class

////////////////////////////////////////////////////////////////////////
