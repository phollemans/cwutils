////////////////////////////////////////////////////////////////////////
/*

     File: LightTable.java
   Author: Peter Hollemans
     Date: 2002/12/13

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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.Timer;

import noaa.coastwatch.gui.EarthDataViewPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.TransformableImageComponent;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.util.DataLocation;

/**
 * A <code>LightTable</code> is a container that places an
 * invisible drawing table on top of another component and allows
 * that component to become a drawing surface.  When activated,
 * the <code>LightTable</code> listens for mouse events on the
 * surface and draws rubber-band lines according to one of
 * various drawing modes.  It then reports the results of the
 * drawing operation via a change event.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class LightTable 
  extends JLayeredPane {

  // Constants
  // ---------

  public enum Mode {

    /** The non-drawing mode. */
    NONE,

    /**
     * The point drawing mode.  In this mode, a point is drawn by
     * clicking once.
     */
    POINT,

    /**
     * The line drawing mode.  In this mode, a line is drawn by pressing
     * at one end of the line, dragging to the other end of the line,
     * and releasing.
     */
    LINE,

    /**
     * The polyline drawing mode.  In this mode, a multiple segment
     * polyline is drawn by clicking at each segment start and end
     * point.  The polyline is finished by double-clicking or right
     * clicking on the final point.
     */
    POLYLINE,

    /**
     * The box drawing mode.  In this mode, a box is created by pressing
     * at one corner, dragging to the diagonally opposite corner, and
     * releasing.
     */
    BOX,

    /**
     * The zoom box drawing mode.  In this mode, a box is created by
     * pressing at one corner, dragging to the diagonally opposite
     * corner, and releasing.  The display indicates a "zoom" operation
     * by dimming the area outside of the zoom box.
     */
    BOX_ZOOM,

    /**
     * The circle drawing mode.  In this mode, a circle is drawn by
     * pressing at the circle center, dragging to a radius point, and
     * releasing.
     */
    CIRCLE,

    /**
     * The general path drawing mode.  In this mode, a general path is
     * used to create the graphics for rubber-banding.  The general path
     * is drawn when the mouse button is pressed, redrawn while the
     * mouse is dragged, then erased when the mouse button is released.
     */
    GENERAL_PATH,

    /**
     * The image drawing mode.  In this mode, the component background
     * itself is used to create the graphics for rubber-banding.  The
     * image moves continuously with the dragging mouse.
     */
    IMAGE,

    /**
     * The image translation mode.  In this mode, the component must be
     * a <code>TransformableImageComponent</code> and the component is
     * used to create the graphics for rubber-banding by calling its
     * <code>setImageAffine()</code> method.  The image translates
     * continuously with the dragging mouse.
     */
    IMAGE_TRANSLATE,

    /**
     * The image rotation mode.  In this mode, the component must be a
     * <code>TransformableImageComponent</code> and the component is
     * used to create the graphics for rubber-banding by calling its
     * <code>setImageAffine()</code> method.  The image rotates
     * continuously with the dragging mouse.
     */
    IMAGE_ROTATE
    
  } // Mode enum

  /** The radius of the point selection crosshairs. */
  private static final int CROSSHAIR_RADIUS = 10;

  /** The stroke used for drawing the shape line. */
  private static final Stroke LINE_STROKE = new BasicStroke (1);

  /** The stroke used for drawing the line shadow. */
  private static final Stroke SHADOW_STROKE = new BasicStroke (3);

  /** The color for the shape line. */
  private static final Color LINE_COLOR = Color.WHITE;

  /** The color for the line shadow. */
  private static final Color SHADOW_COLOR = new Color (0, 0, 0, 150);

  /** The color for outside the zoom drag area. */
  private static final Color OUT_ZOOM_COLOR = new Color (0, 0, 0, 128);

  /** The color for inside the zoom drag area. */
  private static final Color IN_ZOOM_COLOR = new Color (0, 0, 0, 0);

  // Variables
  // ---------

  /** The base component for drawing. */
  private JComponent component;

  /** The glass pane for the table. */
  private JPanel glassPane;

  /** The dragging flag, true if we are currently dragging. */
  private boolean dragging;

  /** The current drawing mode. */
  private Mode drawingMode;

  /** The base point for the current drag. */
  private Point basePoint;

  /** The most recently drawn shape for rendering the glass pane. */
  private Shape tableShape;

  /** The active flag, true if the light table is responding to events. */
  private boolean active;

  /** The list of change listeners. */
  private List<ChangeListener> listeners;

  /** The most recently drawn shape for returning to the user. */
  private Shape userShape;

  /** The poly mode flag, true if the drawing mode uses multiple segments. */
  private boolean isPolyMode;

  /** The image mode flag, true if the drawing mode uses images. */
  private boolean isImageMode;

  /** The general path forming the current polyline. */
  private GeneralPath polyline;

  /** The polyline started flag, true if a polyline is started. */
  private boolean polyStarted;

  /** The initial image affine transform when dragging started. */
  private AffineTransform initialAffine;
  



  private double wheelRotationTotal;
  private Timer wheelRotationTimer;
  private Point wheelRotationPoint;



  ////////////////////////////////////////////////////////////

  /** Gets the component used as a base layer for this table. */
  public JComponent getComponent () { return (component); }
  
  ////////////////////////////////////////////////////////////

  @Override
  public void doLayout () {

    Rectangle bounds = new Rectangle (0, 0, getWidth(), getHeight());
    component.setBounds (bounds);
    glassPane.setBounds (bounds);
    super.doLayout();

  } // doLayout

  ////////////////////////////////////////////////////////////

  /**
   * The <code>GlassPane</code> is the actual panel used for
   * drawing lines and annnotations in the layered pane.  It
   * floats over top of the base component and receives mouse
   * events when active.
   */
  private class GlassPane extends JPanel {
    public void paintComponent (Graphics g) {
      
      if (isOpaque()) super.paintComponent (g);

      // Check table shape
      // -----------------
      if (tableShape == null) return;
      
      // Set antialias hint
      // ------------------
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
        RenderingHints.VALUE_ANTIALIAS_ON);

      // Draw zoom box
      // -------------
      if (drawingMode == Mode.BOX_ZOOM) {
        Area insideZoom = new Area (tableShape);
        Area outsideZoom = 
          new Area (new Rectangle (0, 0, getWidth(), getHeight()));
        outsideZoom.subtract (insideZoom);
        g2.setColor (OUT_ZOOM_COLOR);
        g2.fill (outsideZoom);
        g2.setColor (IN_ZOOM_COLOR);
        g2.fill (insideZoom);
      } // if

      // Draw shape
      // ----------
      g2.setStroke (SHADOW_STROKE);
      g2.setColor (SHADOW_COLOR);
      if (polyline != null) g2.draw (polyline);
      g2.draw (tableShape);
      g2.setStroke (LINE_STROKE);
      g2.setColor (LINE_COLOR);
      if (polyline != null) g2.draw (polyline);
      g2.draw (tableShape);

      g2.dispose();

    } // paintComponent
  } // GlassPane class

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new table using the specified component as the
   * base layer.  The stroke is set to the default basic stroke
   * and the drawing mode to point mode.  By default the table is
   * inactive.
   *
   * @param component the component to use as a base layer.
   */
  public LightTable (
    JComponent component
  ) {

    // Initialize
    // ----------
    this.component = component;
    add (component, new Integer (0));
    drawingMode = Mode.POINT;
    dragging = false;
    active = false;
    listeners = new ArrayList<ChangeListener>();
    setBackground (component.getBackground());
    setOpaque (true);

    // Create glass pane
    // -----------------
    glassPane = new GlassPane();
    glassPane.setOpaque (false);
    glassPane.setBackground (new Color (0, 0, 0, 0));
    add (glassPane, new Integer (1));
    glassPane.setVisible (false);
    MouseHandler handler = new MouseHandler();
    glassPane.addMouseListener (handler);
    glassPane.addMouseMotionListener (handler);
    glassPane.addMouseWheelListener (event -> wheelRotation (event));
    MouseEventForwarder forwarder = new MouseEventForwarder();
    glassPane.addMouseListener (forwarder);
    glassPane.addMouseMotionListener (forwarder);
    
  } // LightTable constructor

  ////////////////////////////////////////////////////////////

  private void fireZoomEvent () {

/*
    System.out.println ("Rotation point = " + wheelRotationPoint);
    System.out.println ("Rotation total = " + wheelRotationTotal);
*/


    wheelRotationPoint = null;
    wheelRotationTimer = null;
    wheelRotationTotal = 0;





  
  } // fireZoomEvent

  ////////////////////////////////////////////////////////////

  private void wheelRotation (MouseWheelEvent event) {

    // Create or restart the timer
    // ---------------------------
    if (wheelRotationTimer == null) {
      wheelRotationTimer = new Timer (300, action -> fireZoomEvent());
      wheelRotationTimer.setRepeats (false);
    } // if
    else
      wheelRotationTimer.restart();

    // Save the rotation data
    // ----------------------
    wheelRotationPoint = event.getPoint();
    wheelRotationTotal += event.getPreciseWheelRotation();





/*

    Dimension size = component.getSize();
    Point center = wheelRotationPoint

    double baseAngle = Math.atan2 (basePoint.x - center.x,
      -(basePoint.y - center.y));
    double pointAngle = Math.atan2 (p.x - center.x, -(p.y - center.y));
    double theta = pointAngle - baseAngle;

    AffineTransform scaleAffine =
      AffineTransform.getScaleInstance (theta, center.x, center.y);

    if (initialAffine != null) rotAffine.concatenate (initialAffine);

    ((TransformableImageComponent) component).setImageAffine (rotAffine);
    component.repaint();



*/






  } // wheelRotation

  ////////////////////////////////////////////////////////////

  public void setForeground (Color color) {

    super.setForeground (color);
    if (component != null) component.setForeground (color);

  } // setBackground

  ////////////////////////////////////////////////////////////

  public void setBackground (Color color) {

    super.setBackground (color);
    if (component != null) component.setBackground (color);

  } // setBackground

  ////////////////////////////////////////////////////////////

  /** 
   * Overrides the parent to add the listener to the base layer
   * component. 
   */
  public void addMouseListener (MouseListener l) { 

    component.addMouseListener (l); 

  } // addMouseListener

  ////////////////////////////////////////////////////////////

  /** 
   * Overrides the parent to add the listener to the base layer
   * component.
   */
  public void addMouseMotionListener (MouseMotionListener l) { 

    component.addMouseMotionListener (l); 

  } // addMouseMotionListener

  ////////////////////////////////////////////////////////////

  /**
   * Removes the specified change listener from the listeners list.
   */
  public void removeChangeListener (
    ChangeListener listener
  ) {

    listeners.remove (listener);

  } // removeChangeListener

  ////////////////////////////////////////////////////////////
  
  /**
   * Adds a change listener to the listeners list.  A change event
   * is fired when the light table detects the finished drawing.
   * The finished shape may be retrieved using <code>getShape</code>.
   */
  public void addChangeListener (
    ChangeListener listener
  ) {

    listeners.add (listener);

  } // addChangeListener

  ////////////////////////////////////////////////////////////

  /** Gets the light table activity mode. */
  public boolean getActive () { return (active); }

  ////////////////////////////////////////////////////////////

  /** Sets the light table active or inactive. */
  public void setActive (boolean flag) { 

    active = flag; 
    glassPane.setVisible (active);
  
  } // setActive

  ////////////////////////////////////////////////////////////

  /** Forwards mouse events to the component. */
  private class MouseEventForwarder extends MouseInputAdapter {

    public void mousePressed (MouseEvent e) { component.dispatchEvent (e); }
    public void mouseDragged (MouseEvent e) { component.dispatchEvent (e); }
    public void mouseReleased (MouseEvent e) { component.dispatchEvent (e); }
    public void mouseClicked (MouseEvent e) { component.dispatchEvent (e); }
    public void mouseMoved (MouseEvent e) { component.dispatchEvent (e); }
    public void mouseExited (MouseEvent e) { component.dispatchEvent (e); }
    public void mouseEntered (MouseEvent e) { component.dispatchEvent (e); }

  } // MouseEventForwarder

  ////////////////////////////////////////////////////////////

  /** Handles mouse events. */
  private class MouseHandler extends MouseInputAdapter {

    // Event handlers for simple press/drag/release drawing modes
    // ----------------------------------------------------------
    public void mousePressed (MouseEvent e) { 
      if (active && !isPolyMode) 
        start (truncate (e.getPoint())); 
    } // mousePressed
    public void mouseDragged (MouseEvent e) { 
      if (active && !isPolyMode) 
        update (truncate (e.getPoint()));
    } // mouseDragged
    public void mouseReleased (MouseEvent e) { 
      if (active && !isPolyMode) 
        finish (truncate (e.getPoint())); 
    } // mouseReleased

    // Event handlers for polyline click/click/click drawing modes
    // -----------------------------------------------------------
    public void mouseClicked (MouseEvent e) { 
      if (active && isPolyMode) {
        if (!polyStarted)
          startPoly (truncate (e.getPoint()));
        else {
          if (e.getClickCount() == 2 || SwingUtilities.isRightMouseButton (e))
            finishPoly (truncate (e.getPoint()));
          else
            appendPoly (truncate (e.getPoint()));
        } // else
      } // if
    } // mouseClicked
    
    public void mouseMoved (MouseEvent e) { 
      if (active && isPolyMode && polyStarted) 
        update (truncate (e.getPoint()));
    } // mouseMoved

  } // MouseHandler class

  ////////////////////////////////////////////////////////////

  /**
   * Truncates the specified point at the component border.
   */
  private Point truncate (
    Point p
  ) {

    Dimension dims = component.getSize();
    Point newPoint = new Point (
      Math.max (0, Math.min (dims.width-1, p.x)),
      Math.max (0, Math.min (dims.height-1, p.y))
    );
    return (newPoint);

  } // truncate

  ////////////////////////////////////////////////////////////

  /** Starts a new polyline. */
  private void startPoly (
    Point p
  ) {

    start (p);
    tableShape = polyline = new GeneralPath();
    polyline.moveTo (p.x, p.y);
    polyStarted = true;

  } // startPoly

  ////////////////////////////////////////////////////////////

  /** Appends a new point to the polyline. */
  private void appendPoly (
    Point p
  ) {

    // Add point to polyline
    // ---------------------
    polyline.lineTo (p.x, p.y);
    glassPane.repaint();

    // Prepare for next segment
    // ------------------------
    basePoint = p;

  } // appendPoly

  ////////////////////////////////////////////////////////////

  /** Finishes the polyline. */
  private void finishPoly (
    Point p
  ) {

    // Add point to polyline
    // ---------------------
    Point2D currentPoint = polyline.getCurrentPoint();
    if (!currentPoint.equals (p)) polyline.lineTo (p.x, p.y);

    // Set user shape
    // --------------
    userShape = polyline;

    // Repaint with empty shape
    // ------------------------
    tableShape = null;
    glassPane.repaint();

    // Clean up
    // --------
    dragging = false;
    basePoint = null;
    polyline = null;
    polyStarted = false;

    // Fire change event
    // -----------------
    ChangeEvent event = new ChangeEvent (this);
    for (ChangeListener listener : listeners) listener.stateChanged (event);

  } // finishPoly

  ////////////////////////////////////////////////////////////

  /** Starts the drawing using the specified point as a base. */
  private void start (
    Point p
  ) { 

    // Check for dragging
    // ------------------
    if (dragging) return;

    // Create new table shape
    // ----------------------
    if (!isImageMode) {
      tableShape = getShape (p, p);
    } // if

    // Save initial affine
    // -------------------
    if (drawingMode == Mode.IMAGE_TRANSLATE || drawingMode == Mode.IMAGE_ROTATE) {
      initialAffine = ((TransformableImageComponent) component).getImageAffine();
    } // if

    // Store drawing state
    // -------------------
    basePoint = p;
    dragging = true;

  } // start

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the cursor appropriate for the current drawing mode.
   *
   * @return the appropriate cursor.
   */
  public Cursor getCursor () {

    int cursor;
    
    switch (drawingMode) {
    case POINT:
    case LINE:
    case POLYLINE:
    case BOX_ZOOM:
    case BOX:
    case CIRCLE:
      cursor = Cursor.CROSSHAIR_CURSOR;
      break;
    case GENERAL_PATH:
    case IMAGE:
      cursor = Cursor.MOVE_CURSOR;
      break;
    case IMAGE_TRANSLATE:
    case IMAGE_ROTATE:
      cursor = Cursor.HAND_CURSOR;
      break;
    default:
      cursor = Cursor.DEFAULT_CURSOR;
    } // switch

    return (Cursor.getPredefinedCursor (cursor));

  } // getCursor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a shape based on the current drawing mode.
   *
   * @param p1 the first shape point, the base point.
   * @param p2 the second shape point.
   *
   * @return the new shape.
   */
  private Shape getShape (
    Point p1,
    Point p2
  ) {

    Shape s = null;
    switch (drawingMode) {

    // Shape is a point
    // ---------------
    case POINT:
      GeneralPath path = new GeneralPath();
      path.moveTo (p2.x - CROSSHAIR_RADIUS, p2.y);
      path.lineTo (p2.x - CROSSHAIR_RADIUS/2, p2.y);
      path.moveTo (p2.x + CROSSHAIR_RADIUS/2, p2.y);
      path.lineTo (p2.x + CROSSHAIR_RADIUS, p2.y);
      path.moveTo (p2.x, p2.y - CROSSHAIR_RADIUS);
      path.lineTo (p2.x, p2.y - CROSSHAIR_RADIUS/2);
      path.moveTo (p2.x, p2.y + CROSSHAIR_RADIUS/2);
      path.lineTo (p2.x, p2.y + CROSSHAIR_RADIUS);
      s = path;
      break;

    // Shape is a line
    // ---------------
    case LINE:
    case POLYLINE:
      s = new Line2D.Float (p1, p2);
      break;

    // Shape is a box
    // --------------      
    case BOX_ZOOM:
    case BOX:
      s = new Rectangle (
        Math.min (p1.x, p2.x), 
        Math.min (p1.y, p2.y),
        Math.abs (p1.x - p2.x),
        Math.abs (p1.y - p2.y)
      );
      break;

    // Shape is a circle
    // -----------------      
    case CIRCLE:
      int radius = (int) p1.distance (p2);
      s = new Ellipse2D.Float (
        p1.x - radius,
        p1.y - radius,
        radius*2,
        radius*2
      );
      break;

    // Shape is a general path
    // -----------------------
    case GENERAL_PATH:
      s = null;
      break;

    } // switch

    return (s);

  } // getShape

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a user shape based on the current drawing mode.  The user
   * shape differs from the drawn shape in some cases.
   *
   * @param p1 the first shape point, the base point.
   * @param p2 the second shape point.
   *
   * @return the user shape.
   */
  private Shape getUserShape (
    Point p1,
    Point p2
  ) {

    Shape s = null;
    switch (drawingMode) {

    case POINT:
      s = new Line2D.Float (p2, p2);
      break;

    case GENERAL_PATH:
    case IMAGE:
    case IMAGE_TRANSLATE:
    case IMAGE_ROTATE:
      s = new Line2D.Float (p1, p2);
      break;

    default:
      s = getShape (p1, p2);
      break;

    } // switch

    return (s);

  } // getUserShape

  ////////////////////////////////////////////////////////////

  /** Gets the drawing mode. */
  public Mode getDrawingMode () { return (drawingMode); }

  ////////////////////////////////////////////////////////////

  /** Sets the drawing mode to the specified mode. */
  public void setDrawingMode (Mode mode) {

    // Check mode
    // ----------
    switch (mode) {
    case IMAGE_TRANSLATE:
    case IMAGE_ROTATE:
      if (!(component instanceof TransformableImageComponent))
        throw new IllegalArgumentException ("Component is not transformable");
      break;
    } // switch

    // Set mode
    // --------
    drawingMode = mode; 
    isPolyMode = (drawingMode == Mode.POLYLINE);
    isImageMode = (
      drawingMode == Mode.IMAGE ||
      drawingMode == Mode.IMAGE_TRANSLATE ||
      drawingMode == Mode.IMAGE_ROTATE
    );

  } // setDrawingMode

  ////////////////////////////////////////////////////////////

  /** Updates the drawing using the specified point. */
  private void update (Point p) { 

    // Check for dragging
    // ------------------
    if (!dragging) return;

    // Redraw image
    // ------------
    if (isImageMode) {
      switch (drawingMode) {

      // Perform translation of entire image
      // -----------------------------------
      case IMAGE:
        Rectangle bounds = component.getBounds();
        component.setBounds (p.x - basePoint.x, p.y - basePoint.y,
          bounds.width, bounds.height);
        repaint();
        break;

      // Translate using the image affine
      // -------------------------------
      case IMAGE_TRANSLATE:
        AffineTransform transAffine = AffineTransform.getTranslateInstance (
          p.x - basePoint.x, p.y - basePoint.y);
        if (initialAffine != null) transAffine.concatenate (initialAffine);
        ((TransformableImageComponent) component).setImageAffine (transAffine);
        component.repaint();
        break;
        
      // Rotate using the image affine
      // -----------------------------
      case IMAGE_ROTATE:
        Dimension size = component.getSize();
        Point center = new Point (size.width/2, size.height/2);
        double baseAngle = Math.atan2 (basePoint.x - center.x, 
          -(basePoint.y - center.y));
        double pointAngle = Math.atan2 (p.x - center.x, -(p.y - center.y));
        double theta = pointAngle - baseAngle;
        AffineTransform rotAffine = 
          AffineTransform.getRotateInstance (theta, center.x, center.y);
        if (initialAffine != null) rotAffine.concatenate (initialAffine);
        ((TransformableImageComponent) component).setImageAffine (rotAffine);
        component.repaint();
        break;

      } // switch
    } // if

    // Redraw shape
    // ------------
    else {
      tableShape = getShape (basePoint, p);
      glassPane.repaint();
    } // else

  } // update

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the current user shape.  The shape differs depending on the
   * drawing mode as follows:
   * <ul>
   *   <li> <code>Mode.POINT = Line2D</code> </li>
   *   <li> <code>Mode.LINE = Line2D</code> </li>
   *   <li> <code>Mode.POLYLINE = GeneralPath</code> </li>
   *   <li> <code>Mode.BOX = Rectangle2D</code> </li>
   *   <li> <code>Mode.BOX_ZOOM = Rectangle2D</code> </li>
   *   <li> <code>Mode.CIRCLE = Ellipse2D</code> </li>
   *   <li> <code>Mode.GENERAL_PATH = Line2D</code> </li>
   *   <li> <code>Mode.IMAGE = Line2D</code> </li>
   *   <li> <code>Mode.IMAGE_TRANSLATE = Line2D</code> </li>
   *   <li> <code>Mode.IMAGE_ROTATE = Line2D</code> </li>
   * </ul>
   *
   * @return the current user shape or null if not available.
   */
  public Shape getShape () { return (userShape); }

  ////////////////////////////////////////////////////////////

  /** Finishes the drawing using the specified point. */
  private void finish (Point p) { 

    // Check for dragging
    // ------------------
    if (!dragging) return;

    // Get user shape
    // --------------
    userShape = getUserShape (basePoint, p);

    // Restore component bounds
    // ------------------------
    if (drawingMode == Mode.IMAGE) {
      component.setBounds (0, 0, getWidth(), getHeight());
      component.repaint();
    } // if

    // Set empty shape
    // ---------------
    tableShape = null;
    glassPane.repaint();

    // Clean up
    // --------
    dragging = false;
    basePoint = null;

    // Fire change event
    // -----------------
    ChangeEvent event = new ChangeEvent (this);
    for (ChangeListener listener : listeners) listener.stateChanged (event);

  } // finish

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    // Create drawing area
    // -------------------
    JPanel panel = new JPanel (new BorderLayout());
    JPanel drawingArea = new JPanel() {
        public void paintComponent (Graphics g) {
          super.paintComponent (g);
          g.setColor (new Color (0, 0, 128));
          Dimension dims = getSize();
          g.fillRect (dims.width/4, dims.height/4, 
                      dims.width/2, dims.height/2);
          g.setColor (new Color (180, 180, 180));
          g.fillRect (dims.width/2, dims.height/2, 
                      dims.width/3, dims.height/3);
        } // paintComponent
      };
    drawingArea.setBackground (Color.BLACK);

    // Create light table
    // ------------------
    final LightTable table = new LightTable (drawingArea);
    table.setActive (true);
    table.setPreferredSize (new Dimension (320, 200));
    table.addChangeListener (new ChangeListener() {
        public void stateChanged (ChangeEvent event) {
          Shape s = table.getShape();
          System.out.println ("Got shape = " + s);
        } // stateChanged
      });
    panel.add (table, BorderLayout.CENTER);

    // Create drawing mode controls
    // ----------------------------
    JPanel buttons = new JPanel();
    panel.add (buttons, BorderLayout.SOUTH);
    Map<String,Mode> map = new HashMap<>();
    map.put ("Point", Mode.POINT);
    map.put ("Line", Mode.LINE);
    map.put ("Polyline", Mode.POLYLINE);
    map.put ("Box", Mode.BOX);
    map.put ("Box Zoom", Mode.BOX_ZOOM);
    map.put ("Circle", Mode.CIRCLE);
    //    map.put ("Path", Mode.GENERAL_PATH);
    map.put ("Image", Mode.IMAGE);
    JComboBox combo = new JComboBox (map.keySet().toArray());
    combo.setSelectedItem ("Point");
    combo.addActionListener (event -> table.setDrawingMode (map.get (combo.getSelectedItem())));
    buttons.add (combo);

    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // LightTable class

////////////////////////////////////////////////////////////////////////
