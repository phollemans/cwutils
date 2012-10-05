////////////////////////////////////////////////////////////////////////
/*
     FILE: LightTable.java
  PURPOSE: Handles common drawing operations.
   AUTHOR: Peter Hollemans
     DATE: 2002/12/13
  CHANGES: 2004/03/28, PFH, added getDrawingMode(), getActive(), getCursor()
           2004/04/05, PFH, changed Vector to ArrayList, added polyline mode
           2004/06/02, PFH, added handling for delayed rendering components
           2004/06/07, PFH, added image translate and rotate modes
           2006/12/14, PFH, modified for new setImageAffine() behaviour
           2007/07/27, PFH, reimplemented using JLayoutPane
           2011/05/16, XL, added a popup menu to copy/paste between earthDataView's

  CoastWatch Software Library and Utilities
  Copyright 2004-2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.List;
import java.io.IOException;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;

/**
 * A <code>LightTable</code> is a container that places an
 * invisible drawing table on top of another component and allows
 * that component to become a drawing surface.  When activated,
 * the <code>LightTable</code> listens for mouse events on the
 * surface and draws rubber-band lines according to one of
 * various drawing modes.  It then reports the results of the
 * drawing operation via a change event.
 */
public class LightTable 
  extends JLayeredPane {

  // Constants
  // ---------
  /** 
   * The point drawing mode.  In this mode, a point is drawn by
   * clicking once.
   *
   * @see #setDrawingMode
   */
  public static final int POINT_MODE = 0;

  /**
   * The line drawing mode.  In this mode, a line is drawn by pressing
   * at one end of the line, dragging to the other end of the line,
   * and releasing.
   *
   * @see #setDrawingMode
   */
  public static final int LINE_MODE = 1;

  /**
   * The polyline drawing mode.  In this mode, a multiple segment
   * polyline is drawn by clicking at each segment start and end
   * point.  The polyline is finished by double-clicking or right
   * clicking on the final point.
   *
   * @see #setDrawingMode
   */
  public static final int POLYLINE_MODE = 2;

  /** 
   * The box drawing mode.  In this mode, a box is created by pressing
   * at one corner, dragging to the diagonally opposite corner, and
   * releasing.
   *
   * @see #setDrawingMode
   */
  public static final int BOX_MODE = 3;

  /** 
   * The zoom box drawing mode.  In this mode, a box is created by
   * pressing at one corner, dragging to the diagonally opposite
   * corner, and releasing.  The display indicates a "zoom" operation
   * by dimming the area outside of the zoom box.
   *
   * @see #setDrawingMode
   */
  public static final int BOX_ZOOM_MODE = 103;

  /**
   * The circle drawing mode.  In this mode, a circle is drawn by
   * pressing at the circle center, dragging to a radius point, and
   * releasing.
   *
   * @see #setDrawingMode
   */
  public static final int CIRCLE_MODE = 4;

  /**
   * The general path drawing mode.  In this mode, a general path is
   * used to create the graphics for rubber-banding.  The general path
   * is drawn when the mouse button is pressed, redrawn while the
   * mouse is dragged, then erased when the mouse button is released.
   *
   * @see #setDrawingMode
   */
  public static final int GENERAL_PATH_MODE = 5;

  /**
   * The image drawing mode.  In this mode, the component background
   * itself is used to create the graphics for rubber-banding.  The
   * image moves continuously with the dragging mouse.
   *
   * @see #setDrawingMode
   */
  public static final int IMAGE_MODE = 6;

  /**
   * The image translation mode.  In this mode, the component must be
   * a <code>TransformableImageComponent</code> and the component is
   * used to create the graphics for rubber-banding by calling its
   * <code>setImageAffine()</code> method.  The image translates
   * continuously with the dragging mouse.
   *
   * @see #setDrawingMode
   */
  public static final int IMAGE_TRANSLATE_MODE = 7;

  /**
   * The image rotation mode.  In this mode, the component must be a
   * <code>TransformableImageComponent</code> and the component is
   * used to create the graphics for rubber-banding by calling its
   * <code>setImageAffine()</code> method.  The image rotates
   * continuously with the dragging mouse.
   *
   * @see #setDrawingMode
   */
  public static final int IMAGE_ROTATE_MODE = 8;

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
  private int drawingMode;

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
  
  /** The popup menu for copy/paste between earthDataView's */
  private JPopupMenu popupMenu;
  
  /** The data flavor for copy/paste functions */
  private DataFlavor df = new DataFlavor(double[].class, "Double Array");

  ////////////////////////////////////////////////////////////

  /** Gets the component used as a base layer for this table. */
  public JComponent getComponent () { return (component); }

  ////////////////////////////////////////////////////////////
  
  /**
   * The transferable EarthDataView for copy/paste function on the 
   * local clipboard
   */
  private class EarthDataViewProperty implements Transferable{
	  private double[] data;
	  
	  public EarthDataViewProperty(double[] data2Transfer){
		  data = data2Transfer;
	  }
	  public DataFlavor[] getTransferDataFlavors(){
		  DataFlavor[] dfs = {LightTable.this.df};
		  return dfs;
	  }
	  public boolean isDataFlavorSupported(DataFlavor flavor){
		  if(flavor.match(LightTable.this.df))
			  return true;
		  else
			  return false;
	  }
	  public Object getTransferData(DataFlavor flavor)
      throws UnsupportedFlavorException,
             IOException{
		  if(isDataFlavorSupported(flavor))
			  return data;
		  else
			  return null;
	  }
  }

  ////////////////////////////////////////////////////////////
  
  /**
   * The action to copy an EarthDataView scale info to 
   * the local clipboard
   * 
   * @author xiaoming
   *
   */
  private class CopyScaleAction 
	extends AbstractAction implements ClipboardOwner{
	  public CopyScaleAction(){
		  super("copy scale");
	  }
	  public void actionPerformed(ActionEvent e) {
		  	EarthDataView edv = ((EarthDataViewPanel)(LightTable.this.component)).getView();
		  	DataLocation dl = edv.getCenter();
	  	  	double scale = edv.getScale();
	  	  	double y = dl.get(0);
	  	  	double x = dl.get(1);
	  		double[] toClipboard = new double[]{y,x,scale};
	  		EarthDataViewProperty scaleInfo = new EarthDataViewProperty(toClipboard);
	  	    Clipboard clipboard = GUIServices.getCDATClipboard();
	  	    clipboard.setContents( scaleInfo, this );
	  }
	  public boolean isEnabled(){
		  return true;
	  }
	  public void lostOwnership( Clipboard aClipboard, Transferable aContents) {
	  	     //do nothing
	  }
  }
  
  ////////////////////////////////////////////////////////////
  
  /**
   * The action to get the scale info from the local clipboard
   * and paste to the current EarthDataView
   * 
   * @author xiaoming
   *
   */
  private class PasteScaleAction 
	extends AbstractAction{
	  public PasteScaleAction(){
		  super("paste scale");
	  }
	  public void actionPerformed(ActionEvent e) {
		  System.out.print("Action for paste");
		  double[] result;
		  Clipboard clipboard = GUIServices.getCDATClipboard();
		  Transferable contents = clipboard.getContents(null);
		  boolean hasTransferableText =
		      (contents != null) &&
		  contents.isDataFlavorSupported(LightTable.this.df);
		  if ( hasTransferableText ) {
		      try {
		        result = (double[])contents.getTransferData(df);
		  		DataLocation center = new DataLocation(result[0], result[1]);
		  		double scale = result[2];
		  		EarthDataView view = ((EarthDataViewPanel)(LightTable.this.component)).getView();
		  		view.setCenterAndScale(center, scale);
		      }
		      catch (Exception ex){
		        System.out.println(ex);
		        ex.printStackTrace();
		      }
		  }
		  
	  }
	  public boolean isEnabled(){
		  Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		  Transferable contents = clipboard.getContents(null);
		  boolean hasTransferableText =
		      (contents != null) &&
		  contents.isDataFlavorSupported(LightTable.this.df);
		  return true;
	  }
  }
  
  ////////////////////////////////////////////////////////////
  
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
  private class GlassPane
    extends JPanel {
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
      if (drawingMode == BOX_ZOOM_MODE) {
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
    drawingMode = POINT_MODE;
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
    MouseEventForwarder forwarder = new MouseEventForwarder();
    glassPane.addMouseListener (forwarder);
    glassPane.addMouseMotionListener (forwarder);
    
    popupMenu = new JPopupMenu();
    //JMenuItem menuItem = new JMenuItem(new SetScaleAction());
    //popupMenu.add(menuItem);
    JMenuItem menuItem = new JMenuItem(new CopyScaleAction());
    popupMenu.add(menuItem);
    menuItem = new JMenuItem(new PasteScaleAction());
    popupMenu.add(menuItem);
    add(popupMenu);

  } // LightTable constructor

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
  private class MouseHandler 
    extends MouseInputAdapter {

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
      } 
      else if (active && drawingMode != POINT_MODE
    		  && e.getButton()!=e.BUTTON1){
          	popupMenu.show(e.getComponent(), e.getX(), e.getY());
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
    if (drawingMode == IMAGE_TRANSLATE_MODE || 
        drawingMode == IMAGE_ROTATE_MODE) {
      initialAffine = 
        ((TransformableImageComponent) component).getImageAffine();
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
    case POINT_MODE: cursor = Cursor.CROSSHAIR_CURSOR; break;
    case LINE_MODE: cursor = Cursor.CROSSHAIR_CURSOR; break;
    case POLYLINE_MODE: cursor = Cursor.CROSSHAIR_CURSOR; break;
    case BOX_ZOOM_MODE:
    case BOX_MODE: cursor = Cursor.CROSSHAIR_CURSOR; break;
    case CIRCLE_MODE: cursor = Cursor.CROSSHAIR_CURSOR; break;
    case GENERAL_PATH_MODE: cursor = Cursor.MOVE_CURSOR; break;
    case IMAGE_MODE: cursor = Cursor.MOVE_CURSOR; break;
    case IMAGE_TRANSLATE_MODE: cursor = Cursor.HAND_CURSOR; break;
    case IMAGE_ROTATE_MODE: cursor = Cursor.HAND_CURSOR; break;
    default: cursor = Cursor.DEFAULT_CURSOR;
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
    case POINT_MODE: 
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
    case LINE_MODE:
    case POLYLINE_MODE:
      s = new Line2D.Float (p1, p2);
      break;

    // Shape is a box
    // --------------      
    case BOX_ZOOM_MODE:
    case BOX_MODE:
      s = new Rectangle (
        Math.min (p1.x, p2.x), 
        Math.min (p1.y, p2.y),
        Math.abs (p1.x - p2.x),
        Math.abs (p1.y - p2.y)
      );
      break;

    // Shape is a circle
    // -----------------      
    case CIRCLE_MODE:
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
    case GENERAL_PATH_MODE:
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

    case POINT_MODE: 
      s = new Line2D.Float (p2, p2);
      break;

    case GENERAL_PATH_MODE:
    case IMAGE_MODE:
    case IMAGE_TRANSLATE_MODE:
    case IMAGE_ROTATE_MODE:
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
  public int getDrawingMode () { return (drawingMode); }

  ////////////////////////////////////////////////////////////

  /** Sets the drawing mode to the specified mode. */
  public void setDrawingMode (int mode) { 

    // Check mode
    // ----------
    switch (mode) {
    case IMAGE_TRANSLATE_MODE:
    case IMAGE_ROTATE_MODE:
      if (!(component instanceof TransformableImageComponent))
        throw new IllegalArgumentException ("Component is not transformable");
      break;
    } // switch

    // Set mode
    // --------
    drawingMode = mode; 
    isPolyMode = (drawingMode == POLYLINE_MODE);
    isImageMode = (
      drawingMode == IMAGE_MODE ||
      drawingMode == IMAGE_TRANSLATE_MODE ||
      drawingMode == IMAGE_ROTATE_MODE
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
      case IMAGE_MODE:
        Rectangle bounds = component.getBounds();
        component.setBounds (p.x - basePoint.x, p.y - basePoint.y,
          bounds.width, bounds.height);
        repaint();
        break;

      // Translate using the image affine
      // -------------------------------
      case IMAGE_TRANSLATE_MODE:
        AffineTransform transAffine = AffineTransform.getTranslateInstance (
          p.x - basePoint.x, p.y - basePoint.y);
        if (initialAffine != null) transAffine.concatenate (initialAffine);
        ((TransformableImageComponent) component).setImageAffine (transAffine);
        component.repaint();
        break;
        
      // Rotate using the image affine
      // -----------------------------
      case IMAGE_ROTATE_MODE:
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
   *   <li> <code>POINT_MODE = Line2D</code> </li>
   *   <li> <code>LINE_MODE = Line2D</code> </li>
   *   <li> <code>POLYLINE_MODE = GeneralPath</code> </li>
   *   <li> <code>BOX_MODE = Rectangle2D</code> </li>
   *   <li> <code>BOX_ZOOM_MODE = Rectangle2D</code> </li>
   *   <li> <code>CIRCLE_MODE = Ellipse2D</code> </li>
   *   <li> <code>GENERAL_PATH_MODE = Line2D</code> </li>
   *   <li> <code>IMAGE_MODE = Line2D</code> </li>
   *   <li> <code>IMAGE_TRANSLATE_MODE = Line2D</code> </li>
   *   <li> <code>IMAGE_ROTATE_MODE = Line2D</code> </li>
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
    if (drawingMode == IMAGE_MODE) {
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

  /** Tests this class. */
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
    final Map map = new HashMap();
    map.put ("Point", new Integer (POINT_MODE));
    map.put ("Line", new Integer (LINE_MODE));
    map.put ("Polyline", new Integer (POLYLINE_MODE));
    map.put ("Box", new Integer (BOX_MODE));
    map.put ("Box Zoom", new Integer (BOX_ZOOM_MODE));
    map.put ("Circle", new Integer (CIRCLE_MODE));
    //    map.put ("Path", new Integer (GENERAL_PATH_MODE));
    map.put ("Image", new Integer (IMAGE_MODE));
    final JComboBox combo = new JComboBox (map.keySet().toArray());
    combo.setSelectedItem ("Point");
    combo.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent e) {
          table.setDrawingMode (((Integer) map.get (
            combo.getSelectedItem())).intValue());
        } // actionPerformed
      });
    buttons.add (combo);

    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // LightTable class

////////////////////////////////////////////////////////////////////////
