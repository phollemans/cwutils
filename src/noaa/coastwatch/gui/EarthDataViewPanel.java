////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataViewPanel.java
   Author: Peter Hollemans
     Date: 2002/12/04

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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.Cursor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.Font;

import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.BorderFactory;
import javax.swing.event.MouseInputAdapter;
import javax.swing.Timer;
import javax.swing.AbstractAction;
import javax.swing.Action;

import noaa.coastwatch.gui.DelayedRenderingComponent;
import noaa.coastwatch.gui.TransformableImageComponent;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>EarthDataViewPanel</code> class displays an onscreen
 * version of an <code>EarthDataView</code> object.  The panel has
 * enhanced functionality over the <code>RenderablePanel</code> class
 * with resizing behaviour and mouse events.  The panel also supplies
 * a tracking bar that may be used to display the mouse cursor
 * position within the panel.<p>
 *
 * Changes to the view should be performed using panel methods
 * wherever possible rather than the corresponding view methods, since
 * a rendering loop may be active in the panel.  When the view must be
 * modified directly, users should call the panel
 * <code>stopRendering()</code> method before making the change.<p>
 *
 * The panel signals a change in rendering status by firing a
 * <code>RENDERING_PROPERTY</code> change event with value true
 * when rendering is in progress or false if not.  This can help
 * with progress monitors.
 *
 * @see noaa.coastwatch.render.EarthDataView
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class EarthDataViewPanel
  extends JPanel
  implements DelayedRenderingComponent, TransformableImageComponent {

  // Constants
  // ---------

  /** 
   * The delay in milliseconds between successive repaints during a
   * delayed rendering operation.
   */
  private static final int DELAY = 333;

  /** The rendering status property for property change events. */
  public static final String RENDERING_PROPERTY = "renderingStatus";

  // Variables
  // ---------

  /** The earth data view for rendering. */
  private EarthDataView view;

  /** 
   * The origin point for view graphics.  The origin is the top-left
   * corner of actual view graphics within the view panel.  If the
   * aspect ratio of the view is not the same as the view panel, it is
   * necessary for the origin to be somewhere along the top edge or
   * the left edge of the view panel.
   */
  private Point origin;

  /** 
   * The panel dimensions. These are the width and height of the
   * actual view panel onscreen.  When the width and height change, it
   * may be necessary to resize the view graphics to fit the panel.
   */
  private Dimension panelDims;

  /** 
   * The view dimensions. These are the height and width of the view
   * graphics.  The origin of the view graphics is shifted with
   * respect to the view panel by the origin.
   */
  private Dimension viewDims;

  /** 
   * The default cursor.  Generally, the default cursor is set to be
   * an arrow, but when some view drawing mode is set such as a zoom
   * box mode or pan mode, the cursor would be different.  The default
   * cursor may also change temporarily when the view is being drawn
   * to indicate drawing progress.
   */
  private Cursor defaultCursor;

  /** 
   * The buffered view panel image.  The buffer image is used to
   * double-buffer the view to the screen.  The default Swing
   * double-buffering is turned off so that the panel itself can
   * handle the double-buffering.  This is important so that the panel
   * can override double-buffering when view reconstruction requires
   * some delay time, and the progress of the reconstruction should be
   * shown during that delay.
   */
  private BufferedImage bufferImage;

  /**
   * The static view mode flag, true if the view dimensions should
   * never be set based on the panel dimensions.  See the
   * <code>setStaticView()</code> method for details.
   */
  private boolean isStaticView;

  /** 
   * The worker thread that is currently executing a rendering
   * loop.
   */
  private Thread worker;

  /** 
   * The resize flag, true just after a panel resize event.  Any
   * repainting code should reset this flag back to false once the
   * resizing has been dealt with.
   */
  private boolean wasResized;

  /**
   * The rendering lock used to stop rendering.  When stopRendering()
   * is called, the running thread acquires the rendering lock and
   * then waits until it is notified.  Notification happens when
   * rendering is complete and the rendering worker thread is done.
   */
  private Object renderingLock = new Object();

  /** 
   * The rendering flag, true if we are in a delayed rendering mode or
   * false if not.
   */
  private boolean isRendering;

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if the view panel is being rendered, or false if
   * not. 
   */
  public synchronized boolean isRendering () { 

    return (isRendering);

  } // isRendering

  ////////////////////////////////////////////////////////////

  /** 
   * Stops the current rendering thread if it is active.  If there is
   * no active rendering taking place, no operation is performed. 
   */
  public synchronized void stopRendering () { 

    synchronized (renderingLock) {

      // Check if we are rendering
      // -------------------------
      if (!isRendering()) { 
        return;
      } // if

      // Stop rendering and wait for the worker to finish
      // ------------------------------------------------
      view.stopRendering();
      try { renderingLock.wait(); }
      catch (InterruptedException e) {
        throw new RuntimeException ("Interrupted waiting for rendering stop");
      } // catch

    } // synchronized

  } // stopRendering

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the earth data view.
   *
   * @return the data view.
   */
  public EarthDataView getView () { return (view); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the earth data view.
   *
   * @param view the new data view.
   */
  public void setView (EarthDataView view) { 

    stopRendering();
    this.view = view; 
    view.setProgress (true);
    view.setChanged();
    if (viewDims != null && !view.getSize (null).equals (viewDims)) {
      origin = null;
      viewDims = null;
      panelDims = null;
      wasResized = true;
    } // if

  } // setView

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the panel dimensions, view dimensions, and the origin
   * point for view graphics.
   */
  private void updateDimensions () {

    viewDims = view.getSize (null);
    panelDims = getSize();
    origin = new Point ((panelDims.width - viewDims.width) / 2, 
                        (panelDims.height - viewDims.height) / 2);

  } // updateDimensions

  ////////////////////////////////////////////////////////////

  /**
   * Sets the magnification of the view around the current center
   * location so that the data:screen pixel magnification is 1:1.
   * This method should be used rather than manipulating the
   * <code>EarthDataView()</code> object directly.  
   *
   * @see #magnify(double)
   */
  public void unityMagnify () {

    stopRendering();
    try {
      DataLocation center = view.getCenter();
      view.reset();
      view.setCenter (center);
      view.setSize (getSize());
      updateDimensions();
    } // try
    catch (NoninvertibleTransformException e) { 
      e.printStackTrace(); 
    } // catch

  } // unityMagnify

  ////////////////////////////////////////////////////////////

  /**
   * Magnifies the view around the current center location.  This
   * method should be used for magnification of the view rather than
   * calling the <code>EarthDataView.magnify()</code> method directly
   * on the view itself.
   *
   * @param factor the factor by which to magnify.  Factors greater
   * than 1 increase the magnification and factors less than 1
   * decrease the magnification.
   *
   * @see noaa.coastwatch.render.EarthDataView#magnify(double)
   */
  public void magnify (
    double factor
  ) {

    stopRendering();
    try {
      view.magnify (factor);
      if (!isStaticView) view.setSize (getSize());
      updateDimensions();
    } // try
    catch (NoninvertibleTransformException e) { 
      e.printStackTrace(); 
    } // catch

  } // magnify

  ////////////////////////////////////////////////////////////

  /**
   * Magnifies the specified rectangle to occupy the entire view.  The
   * center data location is changed to match the center of the
   * specified rectangle.  The scaling factor is changed so that the
   * maximum dimension of the rectangle fits within the view
   * dimensions.  This method should be used for magnification of the
   * view rather than calling the <code>EarthDataView.magnify()</code>
   * method directly on the view itself.
   *
   * @param rect the magnification rectangle in panel-relative
   * coordinates.
   *
   * @see noaa.coastwatch.render.EarthDataView#magnify(Point,Point)
   */
  public void magnify (
    Rectangle rect
  ) {

    // Get corner points
    // -----------------
    Point upperLeft = (Point) translate (new Point (rect.x, rect.y));
    Point lowerRight = (Point) translate (new Point (rect.x + rect.width, 
      rect.y + rect.height));

    // Magnify view
    // ------------
    try {
      view.magnify (upperLeft, lowerRight);
      if (!isStaticView) view.setSize (getSize());
      updateDimensions();
    } // try
    catch (NoninvertibleTransformException e) { 
      e.printStackTrace(); 
    } // catch

  } // magnify

  ////////////////////////////////////////////////////////////

  /**
   * Sets the view center to a new data location.  The image
   * dimensions and image scaling factor are unaffected. This method
   * should be used for recentering of the view rather than calling
   * the <code>EarthDataView.setCenter()</code> method directly on the
   * view itself.
   *
   * @param center the new center data location in panel-relative
   * coordinates.
   *
   * @see noaa.coastwatch.render.EarthDataView#setCenter(DataLocation)
   */
  public void setCenter (
    Point2D center
  ) {

    stopRendering();
    try {
      view.setCenter (view.getTransform().getImageTransform().transform (
        translate (center)));
      if (!isStaticView) view.setSize (getSize());
      updateDimensions();
    } // try
    catch (NoninvertibleTransformException e) { 
      e.printStackTrace(); 
    } // catch

  } // setCenter

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the static view mode flag.  In this mode, any resizing of
   * the panel window or calls to magnify, recenter, or reset the view
   * panel keep the view image size the same.  When off, these
   * operations dyamically change the view size based on the size of
   * the panel window.  The "on" mode is more appropriate when the
   * view panel is being used to display exactly the same width and
   * height in pixels of data area every time the view panel is
   * modified.  The "off" mode is better for maximizing the use of the
   * view panel window to show as much data as possible.  By default,
   * static view mode is off.
   *
   * @param flag the static view flag, true for static view mode.
   */
  public void setStaticView (
    boolean flag
  ) {

    isStaticView = flag;

  } // setStaticView

  ////////////////////////////////////////////////////////////

  /** 
   * Resizes the view to match the new view panel dimensions.  The
   * view shows approximately the same data as before the resize.  Any
   * rendering that may be taking place is stopped, and the buffer
   * image is recreated.
   *
   * @see noaa.coastwatch.render.EarthDataView#resize
   */
  private void resize () {

    // TODO: There may be a better way to perform resizing.  As it is,
    // if the window is made smaller, the view scaling factor is
    // changed accordingly so that approximately the same view
    // contents appear.  But when made bigger, the scaling factor is
    // kept the same, and a larger area around the center is shown.

    stopRendering();
    try { 
      view.resize (getSize());
      if (!isStaticView) view.setSize (getSize());
      updateDimensions();
      bufferImage = new BufferedImage (panelDims.width, panelDims.height, 
        BufferedImage.TYPE_INT_RGB);
    } // try
    catch (NoninvertibleTransformException e) { 
      e.printStackTrace(); 
    } // catch

  } // resize

  ////////////////////////////////////////////////////////////

  /** 
   * Creates and starts a worker thread that renders the view contents
   * to the buffer image.  A timer is used to periodically repaint the
   * panel to show the user the progress of rendering.
   */
  private synchronized void startRendering () {

    // Create repaint timer
    // --------------------
    final Timer timer = new Timer (DELAY, new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          repaint();
        } // actionPerformed
      });

    // Blank existing buffer image immediately
    // ---------------------------------------
    Graphics bufferGraphics = bufferImage.getGraphics();
    bufferGraphics.setColor (getBackground());
    bufferGraphics.fillRect (0, 0, bufferImage.getWidth(), 
      bufferImage.getHeight());
    bufferGraphics.dispose();

    // Create and start worker thread
    // ------------------------------
    worker = new Thread () {
        public void run () {

          // Setup for rendering
          // -------------------
          SwingUtilities.invokeLater (new Runnable() {
              public void run () {
                setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
                firePropertyChange (RENDERING_PROPERTY, false, true);
              } // run
            });
          timer.start();

          // Perform rendering
          // -----------------
          renderToImageBuffer();

          // Clean up
          // --------
          timer.stop();
          SwingUtilities.invokeLater (new Runnable() {
              public void run () {
                setCursor (defaultCursor);
                firePropertyChange (RENDERING_PROPERTY, true, false);
                repaint();
              } // run
            });
          isRendering = false;
          synchronized (renderingLock) { renderingLock.notifyAll(); }

        } // run
      };
    isRendering = true;
    worker.start();

  } // startRendering

  ////////////////////////////////////////////////////////////

  /** Renders the view to the image buffer. */
  private void renderToImageBuffer () {

    Graphics2D bufferGraphics = bufferImage.createGraphics();
    Graphics2D subsetGraphics = (Graphics2D) bufferGraphics.create (
      origin.x, origin.y, viewDims.width, viewDims.height);
    subsetGraphics.setRenderingHint (RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);
    view.render (subsetGraphics);
    subsetGraphics.dispose();
    bufferGraphics.dispose();
    
  } // renderImageBuffer

  ////////////////////////////////////////////////////////////

  @Override
  public void paintComponent (
    Graphics g
  ) {

    // Check if we are showing
    // -----------------------
    if (!isShowing()) return;

    // Check for resize
    // ----------------
    if (wasResized || bufferImage == null || origin == null || 
      viewDims == null) { 
      resize(); 
      wasResized = false; 
    } // if

    // Re-render the buffer
    // --------------------
    if (!isRendering() && view.isPrepared() && view.isChanged()) {
      renderToImageBuffer();
    } // if

    // Paint the component
    // -------------------
    super.paintComponent (g);
    if (isRendering() || view.isPrepared()) {
      g.drawImage (bufferImage, 0, 0, null);
    } // if
    else {
      startRendering();
    } // else

  } // paintComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the default cursor.
   *
   * @return the default cursor.
   */
  public Cursor getDefaultCursor () { return (defaultCursor); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the default cursor.
   *
   * @param cursor the new default cursor to use.
   */
  public void setDefaultCursor (Cursor cursor) { 

    defaultCursor = cursor; 
    setCursor (defaultCursor);

  } // setDefaultCursor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new earth data view panel using the specified view.
   *
   * @param view the earth data view to display.
   */
  public EarthDataViewPanel (
    EarthDataView view
  ) {

    // Initialize
    // ----------
    setView (view);
    defaultCursor = Cursor.getDefaultCursor();
    setDoubleBuffered (false);
    wasResized = true;

    // Add detection for panel resizing
    // --------------------------------
    /*
    addComponentListener (new ComponentAdapter() {
        public void componentResized (ComponentEvent e) {
          wasResized = true;
        } // componentResized
      });
    */

    // Add detection for showing changed
    // ---------------------------------
    addHierarchyListener (new HierarchyListener () {
        public void hierarchyChanged (HierarchyEvent e) {
          boolean showingChanged = 
            (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0;
          if (showingChanged && !isShowing() && isRendering()) {
            stopRendering();
          } // if
        } // hierarchyChanged
      });

    // Add overlay metadata display
    // ----------------------------
    MouseInputAdapter handler = new OverlayMetadataMouseHandler();
    addMouseListener (handler);
    addMouseMotionListener (handler);

  } // EarthDataViewPanel constructor

  ////////////////////////////////////////////////////////////

  /** Handles mouse events for showing overlay metadata. */
  private class OverlayMetadataMouseHandler extends MouseInputAdapter {

    /** The timer for the metadata popup. */
    private Timer popupTimer;

    /** The popup that shows metadata from an overlay layer. */
    private Popup metadataPopup;
    
    /** The metadata showing in the popup. */
    private Map<String, Object> lastMetadataMap;

    ////////////////////////////////////////////////////////

    /**
     * Hides the metadata display if the popup is showing.
     */
    public void hideMetadata() {

      if (popupTimer != null && popupTimer.isRunning())
        popupTimer.stop();
      else if (metadataPopup != null) {
        metadataPopup.hide();
        metadataPopup = null;
        lastMetadataMap = null;
      } // if
    
    } // hideMetadata

    ////////////////////////////////////////////////////////

    /**
     * Shows the metadata popup if the cursor is over a point with metadata.
     * If not, then any showing metadata popup is hidden.
     *
     * @param point the point to query the view overlays for metadata.
     */
    public void showMetadata (
      Point point
    ) {
    
      // Check for rendering
      // -------------------
      if (isRendering()) {
        hideMetadata();
      } // if

      // Check origin exists
      // -------------------
      else if (origin != null) {

        // Get transforms
        // --------------
        EarthImageTransform trans = view.getTransform();
        ImageTransform imageTrans = trans.getImageTransform();
        EarthTransform earthTrans = trans.getEarthTransform();

        // Check point is in image bounds
        // ------------------------------
        final Point screenPoint = (Point) point.clone();
        point.translate (-origin.x, -origin.y);
        boolean isInBounds = new Rectangle (view.getSize (null)).contains (point);

        // If out of bounds, hide metadata
        // -------------------------------
        if (!isInBounds) {
          hideMetadata();
        } // if

        // Otherwise show metadata
        // -----------------------
        else {

          // Search for metadata to show
          // ---------------------------
          Map<String, Object> metadataMap = null;
          for (EarthDataOverlay overlay : view.getOverlays()) {
            if (overlay.hasMetadata()) {
              metadataMap = overlay.getMetadataAtPoint (point);
              if (metadataMap != null) break;
            } // if
          } // for
          
          // Check if metadata found
          // -----------------------
          if (metadataMap == null) {
            hideMetadata();
          } // if
          
          // Create popup if not the same metadata
          // -------------------------------------
          else if (!metadataMap.equals (lastMetadataMap)) {
          
            // Create popup action
            // -------------------
            final Map<String, Object> thisMetadataMap = metadataMap;
            Action showPopupAction = new AbstractAction() {
              public void actionPerformed (ActionEvent e) {

                // Format metadata
                // ---------------
                StringBuilder buffer = new StringBuilder ("<html>");
                for (String name : thisMetadataMap.keySet()) {
                  buffer.append (name + " = " + thisMetadataMap.get (name) + "<br>");
                } // for
                buffer.append ("</html>");

                // Create label with metadata
                // --------------------------
                JLabel label = new JLabel (buffer.toString());
                Font labelFont = label.getFont();
                label.setFont (labelFont.deriveFont (labelFont.getSize2D() - 2));
                label.setBorder (BorderFactory.createEmptyBorder (10, 10, 10, 10));

                // Set popup position
                // ------------------
                Component parent = EarthDataViewPanel.this;
                Point parentTopLeft = parent.getLocationOnScreen();
                hideMetadata();
                Point popupTopLeft = new Point (
                  parentTopLeft.x + screenPoint.x + 10,
                  parentTopLeft.y + screenPoint.y + 10
                );

                // TODO: If we get the bounds in this way, it seems that in a
                // multimonitor setup with the desktop being extended, the
                // bounds include the whole extended virtual desktop and not
                // just the physical screen that the panel is currently on.
                // We might want to correct this in the future.

                Rectangle bounds = getGraphicsConfiguration().getBounds();
                Dimension labelDims = label.getPreferredSize();
                if (popupTopLeft.y + labelDims.height > bounds.height)
                  popupTopLeft.y = bounds.height - labelDims.height - 10;
                
                // Create and show popup
                // ---------------------
                metadataPopup = PopupFactory.getSharedInstance().getPopup (parent,
                  label, popupTopLeft.x, popupTopLeft.y);
                metadataPopup.show();
                
                // Save metadata
                // -------------
                lastMetadataMap = thisMetadataMap;
                
              } // actionPerformed
            };
          
            // Start new popup timer
            // ---------------------
            if (popupTimer != null && popupTimer.isRunning())
              popupTimer.stop();
            popupTimer = new Timer (500, showPopupAction);
            popupTimer.setRepeats (false);
            popupTimer.start();
          
          } // else if
        
        } // else

      } // else if
    
    } // showMetadata

    ////////////////////////////////////////////////////////

    @Override
    public void mouseExited (MouseEvent e) { hideMetadata(); }

    ////////////////////////////////////////////////////////
    
    @Override
    public void mouseMoved (MouseEvent e) { showMetadata (e.getPoint()); }

    ////////////////////////////////////////////////////////
    
/*
    @Override
    public void mouseDragged (MouseEvent e) { showMetadata (e.getPoint()); }
*/

  } // OverlayMetadataMouseHandler class

  ////////////////////////////////////////////////////////////

  @Override
  public void setBounds (Rectangle r) {

    wasResized = (!r.equals (getBounds()));
    super.setBounds (r);

  } // setBounds

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the view panel so that one dimension of the data grid
   * fits, centered within the panel.  This method should be used to
   * maximize the use of the view area so that no data edges are
   * visible.
   *
   * @throws UnsupportedOperationException if the static view mode
   * flag is on.
   *
   * @see #setStaticView
   */
  public void fitReset () {

    // Check operation
    // ---------------
    if (isStaticView) 
      throw new UnsupportedOperationException();
    
    // Perform fit
    // -----------
    stopRendering();
    try {
      view.reset();
      Dimension panelDims = getSize();
      Dimension imageDims = view.getSize (null);
      double factor = Math.max (
        (double) panelDims.width / imageDims.width,
        (double) panelDims.height / imageDims.height
      );
      view.resize (factor);
      view.setSize (panelDims);
      updateDimensions();
    } // try
    catch (NoninvertibleTransformException e) { 
      e.printStackTrace(); 
    } // catch

  } // fitReset

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the view panel so that the entire data grid fits, centered
   * within the panel.  This method should be used for resetting the
   * view rather than calling the <code>EarthDataView.reset()</code>
   * method directly on the view itself.
   */
  public void reset () {

    stopRendering();
    try {
      view.reset();
      view.resize (getSize());
      if (!isStaticView) view.setSize (getSize());
      updateDimensions();
    } // try
    catch (NoninvertibleTransformException e) { 
      e.printStackTrace(); 
    } // catch

  } // reset

  ////////////////////////////////////////////////////////////

  /**
   * Gets the image affine transform in the view.
   *
   * @return the image affine transform or null for no transform.
   *
   * @see EarthDataView#getImageAffine
   */
  public AffineTransform getImageAffine () {

    return (view.getImageAffine());

  } // getImageAffine

  ////////////////////////////////////////////////////////////

  /**
   * Sets the image affine transform in the view.  This method should
   * be used for modifying the view rather than calling the
   * <code>EarthDataView.setImageAffine()</code> method directly on
   * the view itself.
   *
   * @param affine the image affine transform, or null for no
   * transform.
   *
   * @see EarthDataView#setImageAffine
   */
  public void setImageAffine (
    AffineTransform affine
  ) {

    stopRendering();
    view.setImageAffine (affine);

  } // setImageAffine

  ////////////////////////////////////////////////////////////

  /** 
   * Translates the specified panel-relative point to a view-relative
   * point.  Depending on the view dimensions, the view may be offset
   * from (0,0) inside the panel.
   *
   * @param point the point for translation.
   *
   * @return the translated point.
   */
  public Point2D translate (
    Point2D point
  ) {

    Point2D newPoint = (Point2D) point.clone();
    newPoint.setLocation (point.getX() - origin.x,
      point.getY() - origin.y);
    return (newPoint);

  } // translate

  ////////////////////////////////////////////////////////////

  /**
   * Gets the affine transform that translates view panel coordinates
   * to data location coordinates.
   *
   * @return the affine transform.
   */
  public AffineTransform getAffine () {

    // Create image affine
    // -------------------
    AffineTransform affine = null;
    ImageTransform imageTrans = view.getTransform().getImageTransform();
    try { 
      affine = imageTrans.getAffine().createInverse(); 
    } // try
    catch (NoninvertibleTransformException e) {
      throw new IllegalStateException ("Cannot generate image affine inverse");
    } // catch

    // Adjust for origin
    // -----------------
    affine.concatenate (AffineTransform.getTranslateInstance (
      -origin.getX(), -origin.getY()));

    return (affine);

  } // getAffine

  ////////////////////////////////////////////////////////////

  /**
   * The track bar class supplies mouse position tracking for an Earth
   * data view panel.  The track bar is configurable and can show the
   * cursor position as latitude/longitude and row/column, and the
   * data value at the cursor position.
   */
  public class TrackBar
    extends JPanel {

    // Constants
    // ---------
    /** The number of text fields in total. */
    private final static int MAX_FIELDS = 5;

    /** The indices of various fields. */
    private final static int LAT_FIELD = 0;
    private final static int LON_FIELD = 1;
    private final static int ROW_FIELD = 2;
    private final static int COL_FIELD = 3;
    private final static int VALUE_FIELD = 4;

    // Variables
    // ---------
    /** The text fields. */
    private JTextField[] textFields;

    ////////////////////////////////////////////////////////

    /**
     * Creates a track bar with the specified fields.
     *
     * @param earthLoc the earth location field flag, true for
     * latitude and longitude display.
     * @param dataLoc the data location field flag, true for data row
     * and column display.
     * @param dataValue the data value field flag, true for data value
     * display.
     * @param terse the terse mode flag, true for short labels.
     */
    public TrackBar (
      boolean earthLoc,
      boolean dataLoc,
      boolean dataValue,
      boolean terse
    ) {

      // Create layout
      // -------------
      setLayout (new BoxLayout (this, BoxLayout.X_AXIS));

      // Add earth location fields
      // -------------------------
      textFields = new JTextField[MAX_FIELDS];
      if (earthLoc) {
        for (int i = 0; i < 2; i++) {
          this.add (Box.createHorizontalStrut (5));
          if (terse)
            this.add (new JLabel (i == 0 ? "Lat:" : "Lon:"));
          else 
            this.add (new JLabel (i == 0 ? "Latitude:" : "Longitude:"));
          textFields[i+LAT_FIELD] = new JTextField();
          textFields[i+LAT_FIELD].setEditable (false);
          this.add (Box.createHorizontalStrut (5));
          this.add (textFields[i+LAT_FIELD]);
        } // for
      } // if

      // Add data location fields
      // ------------------------
      if (dataLoc) {
        for (int i = 0; i < 2; i++) {
          this.add (Box.createHorizontalStrut (5));
          if (terse)
            this.add (new JLabel (i == 0 ? "Row:" : "Col:"));
          else 
            this.add (new JLabel (i == 0 ? "Row:" : "Column:"));
          textFields[i+ROW_FIELD] = new JTextField();
          textFields[i+ROW_FIELD].setEditable (false);
          this.add (Box.createHorizontalStrut (5));
          this.add (textFields[i+ROW_FIELD]);
        } // for
      } // if

      // Add data value field
      // --------------------
      if (dataValue) {
        this.add (Box.createHorizontalStrut (5));
        if (terse)
          this.add (new JLabel ("Val:"));
        else
          this.add (new JLabel ("Value:"));
        textFields[VALUE_FIELD] = new JTextField();
        textFields[VALUE_FIELD].setEditable (false);
        this.add (Box.createHorizontalStrut (5));
        this.add (textFields[VALUE_FIELD]);
      } // if

      // Add mouse listener
      // ------------------
      MouseHandler handler = new MouseHandler();
      EarthDataViewPanel.this.addMouseListener (handler);
      EarthDataViewPanel.this.addMouseMotionListener (handler);

    } // TrackBar

    ////////////////////////////////////////////////////////

    /** Blanks the active text fields. */
    private void blank () {

      for (int i = 0; i < MAX_FIELDS; i++) 
        if (textFields[i] != null) textFields[i].setText("");

    } // blank

    ////////////////////////////////////////////////////////

    /** Updates the active text fields using the specified point. */
    private void update (
      Point p
    ) {

      // Check for rendering
      // -------------------
      if (isRendering()) {
        blank();
        return;
      } // if

      // Get transforms
      // --------------
      EarthImageTransform trans = view.getTransform();
      ImageTransform imageTrans = trans.getImageTransform();
      EarthTransform earthTrans = trans.getEarthTransform();
      
      // Check if location is valid
      // --------------------------
      if (origin == null) return;
      p.translate (-origin.x, -origin.y);
      boolean valid = new Rectangle (view.getSize (null)).contains (p);

      // If valid, get data and earth locations
      // --------------------------------------
      DataLocation dataLoc = null;
      EarthLocation earthLoc = null;
      if (valid && imageTrans != null && earthTrans != null) {            
        dataLoc = imageTrans.transform (p);
        earthLoc = earthTrans.transform (dataLoc);
      } // if

      // If not valid, blank and return
      // ------------------------------
      else {
        blank();
        return;
      } // if

      // Update data location
      // --------------------
      if (textFields[ROW_FIELD] != null) {
        boolean isContained = 
          dataLoc.isContained (earthTrans.getDimensions());
        String row, col;
        if (isContained) {
          row = Integer.toString ((int) Math.round (dataLoc.get(Grid.ROWS)));
          col = Integer.toString ((int) Math.round (dataLoc.get(Grid.COLS)));
        } // if
        else {
          row = "";
          col = "";
        } // else
        textFields[ROW_FIELD].setText (row);
        textFields[COL_FIELD].setText (col);
      } // if

      // Update earth location
      // ---------------------
      if (textFields[LAT_FIELD] != null) {
        String lat, lon;
        if (earthLoc.isValid()) {
          lat = earthLoc.formatSingle (EarthLocation.LAT);
          lon = earthLoc.formatSingle (EarthLocation.LON);
        } // if
        else {
          lat = "";
          lon = "";
        } // else
        textFields[LAT_FIELD].setText (lat);
        textFields[LON_FIELD].setText (lon);
      } // if

      // Update value
      // ------------
      if (textFields[VALUE_FIELD] != null) {
        String value;
        if (view instanceof ColorEnhancement) {
          DataVariable var = ((ColorEnhancement) view).getGrid();
          double doubleValue = var.getValue (dataLoc);
          if (Double.isNaN (doubleValue)) value = "";
          else value = var.format (doubleValue);
        } // if
        else {
          value = "";
        } // else
        textFields[VALUE_FIELD].setText (value);
      } // if

    } // update

    ////////////////////////////////////////////////////////

    /** Handles mouse events. */
    private class MouseHandler 
      extends MouseInputAdapter {

      public void mouseExited (MouseEvent e) { blank(); }
      public void mouseMoved (MouseEvent e) { update (e.getPoint()); }
      public void mouseDragged (MouseEvent e) { update (e.getPoint()); }

    } // MouseHandler class

    ////////////////////////////////////////////////////////

  } // TrackBar class

  ////////////////////////////////////////////////////////////

} // EarthDataViewPanel class

////////////////////////////////////////////////////////////////////////
