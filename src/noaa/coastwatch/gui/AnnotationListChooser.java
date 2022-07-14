////////////////////////////////////////////////////////////////////////
/*

     File: AnnotationListChooser.java
   Author: Peter Hollemans
     Date: 2004/04/13

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
  All rights reserved.

  Developed by: CoastWatch / OceanWatch
                Center for Satellite Applications and Research
                http://coastwatch.noaa.gov

  For conditions of distribution and use, see the accompanying
  license.txt file.

*/
////////////////////////////////////////////////////////////////////////

// TODO: It would be nice if annotation positions could be modified
// after they are placed.  

// TODO: There needs to be some way to annotate using a symbol
// such as a crosshair, dot, square, triangle, etc.

// TODO: Is there a way of inserting an "arrow mode" so that lines,
// polylines, and curves can have arrows on one or both ends?

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import noaa.coastwatch.gui.AbstractOverlayListPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.gui.visual.VisualColor;
import noaa.coastwatch.gui.visual.VisualFont;
import noaa.coastwatch.gui.visual.VisualInteger;
import noaa.coastwatch.gui.visual.VisualStroke;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.FilledShapeOverlay;
import noaa.coastwatch.render.ShapeOverlay;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.render.TextOverlay;

/**
 * <p>The <code>AnnotationListChooser</code> class is a panel that allows
 * the user to manipulate a list of annotations.  The user may add a
 * new line, polyline, curve, text, box, circle, polygon or closed
 * curve, and edit the annotation visibility, name, color, linestyle,
 * or fill color.</p>
 *
 * <p>The chooser signals a change in the annotation overlay list by
 * firing a property change event whose property name is given by
 * <code>ANNOTATION_LIST_PROPERTY</code>.  See the {@link
 * AbstractOverlayListPanel} class for details on how the property
 * change events should be interpreted.</p>
 *
 * <p>Annotations require that extra information be provided from the
 * user object.  The chooser signals that it requires input for an
 * annotation by firing an action event whose action command specifies
 * the type of input required as:</p>
 * <ul>
 *   <li> <code>LINE_COMMAND</code> </li>
 *   <li> <code>POLYLINE_COMMAND</code> </li>
 *   <li> <code>BOX_COMMAND</code> </li>
 *   <li> <code>POLYGON_COMMAND</code> </li>
 *   <li> <code>CIRCLE_COMMAND</code> </li>
 *   <li> <code>CURVE_COMMAND</code> </li>
 *   <li> <code>TEXT_COMMAND</code> </li>
 * </ul>
 * <p>The user object should perform some operation to obtain the
 * annotation input information, and then pass it to the
 * <code>addAnnotation()</code> method.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class AnnotationListChooser
  extends JPanel
  implements TabComponent {

  // Constants
  // ---------

  /** The annotattion list property. */
  public static final String ANNOTATION_LIST_PROPERTY = 
    AbstractOverlayListPanel.OVERLAY_PROPERTY;

  /** The line annotation command. */
  public static final String LINE_COMMAND = "Line";

  /** The polyline annotation command. */
  public static final String POLYLINE_COMMAND = "Polyline";

  /** The box annotation command. */
  public static final String BOX_COMMAND = "Box";

  /** The polygon annotation command. */
  public static final String POLYGON_COMMAND = "Polygon";

  /** The circle annotation command. */
  public static final String CIRCLE_COMMAND = "Circle";

  /** The curve annotation command. */
  public static final String CURVE_COMMAND = "Curve";

  /** The text annotation command. */
  public static final String TEXT_COMMAND = "Text";

  /** The annotation list tooltip. */
  private static final String ANNOTATION_LIST_TOOLTIP = "Annotations";

  /** The annotation list panel title. */
  private static final String ANNOTATION_TITLE = "Annotation";

  // Variables
  // ---------

  /** The annotation list panel. */
  private AnnotationListPanel listPanel;

  /** The list of action listeners. */
  private List actionListeners;

  /** The last annotation command executed. */
  private String annotationCommand;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the last annotation command executed.
   *
   * @return the last annotation added, or null if no annotation has been added.
   */
  public String getAnnotationCommand () { return (annotationCommand); }

  ////////////////////////////////////////////////////////////

  /** 
   * Adds the specified listener for receiving annotation input action
   * commands.
   *
   * @param listener the listener to add.
   */
  public void addAnnotationActionListener (
    ActionListener listener
  ) {

    actionListeners.add (listener);

  } // addAnnotationActionListener

  ////////////////////////////////////////////////////////////

  /** Creates a new annotation list chooser. */
  public AnnotationListChooser () {

    super (new BorderLayout());

    // Create main panel
    // -----------------
    listPanel = new AnnotationListPanel();
    listPanel.addPropertyChangeListener (
      AnnotationListPanel.SELECTION_PROPERTY,
      new PropertyChangeListener() {
        public void propertyChange (PropertyChangeEvent event) {
          EarthDataOverlay overlay = (EarthDataOverlay) event.getNewValue();

          // TODO: Change the overlay look when selected.  A selected
          // overlay should have a bounding box around the shape.

        } // propertyChange
      });
    this.add (listPanel, BorderLayout.CENTER);

    // Create list of annotation action listeners
    // --------------------------------------
    actionListeners = new ArrayList();

  } // AnnotationListChooser constructor

  ////////////////////////////////////////////////////////////

  /** Gets the annotation list chooser tooltip. */
  public String getToolTip () { return (ANNOTATION_LIST_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  /** Gets the annotation list chooser title. */
  public String getTitle () { return (null); }

  ////////////////////////////////////////////////////////////

  /** Gets the annotation list tab icon. */
  public Icon getIcon () {

    return (GUIServices.getIcon ("annotation.tab"));

  } // getIcon

  ////////////////////////////////////////////////////////////

  /** Redirects overlay property listeners to the list panel. */
  public void addPropertyChangeListener (
    String propertyName,
    PropertyChangeListener listener
  ) {

    if (propertyName.equals (AbstractOverlayListPanel.OVERLAY_PROPERTY))
      listPanel.addPropertyChangeListener (propertyName, listener);
    else
      super.addPropertyChangeListener (propertyName, listener);

  } // addPropertyChangeListener

  ////////////////////////////////////////////////////////////

  /** Gets the currently active drawing color. */
  private Color getDrawingColor () { 

    return ((Color) listPanel.visualColor.getValue()); 

  } // getDrawingColor

  ////////////////////////////////////////////////////////////

  /** Gets the currently active fill color. */
  private Color getFillColor () { 

    return ((Color) listPanel.visualFill.getValue()); 

  } // getFillColor

  ////////////////////////////////////////////////////////////

  /** Gets the currently active font. */
  private Font getTextFont () { 

    return ((Font) listPanel.visualFont.getValue()); 

  } // getTextFont

  ////////////////////////////////////////////////////////////

  /** Gets the currently active stroke. */
  private Stroke getLineStroke () { 

    return ((Stroke) listPanel.visualStroke.getValue());

  } // getLineStroke

  ////////////////////////////////////////////////////////////

  /** Gets the currently active transparency. */
  private int getTransparency () { 

    return (((Integer) listPanel.visualTransparency.getValue()).intValue());

  } // getTransparency

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a new annotation.  A new overlay is created for the
   * annotation and the overlay added to the list.
   *
   * @param shape the new data annotation shape to add.  The shape is
   * considered to be in data coordinates.
   */
  public void addAnnotation (
    Shape shape
  ) {

    EarthDataOverlay overlay = null;

    // Create shape overlay
    // --------------------
    if (
      annotationCommand.equals (AnnotationListChooser.LINE_COMMAND) ||
      annotationCommand.equals (AnnotationListChooser.POLYLINE_COMMAND) ||
      annotationCommand.equals (AnnotationListChooser.CURVE_COMMAND)
    ) {
      ShapeOverlay shapeOverlay = new ShapeOverlay (getDrawingColor());
      shapeOverlay.setStroke (getLineStroke());
      shapeOverlay.addShape (shape);
      overlay = shapeOverlay;
    } // if

    // Create filled shape overlay
    // ---------------------------
    else if (
      annotationCommand.equals (AnnotationListChooser.BOX_COMMAND) ||
      annotationCommand.equals (AnnotationListChooser.POLYGON_COMMAND) ||
      annotationCommand.equals (AnnotationListChooser.CIRCLE_COMMAND)
    ) {
      FilledShapeOverlay filledOverlay = new FilledShapeOverlay (
        getDrawingColor());
      filledOverlay.setStroke (getLineStroke());
      filledOverlay.setFillColor (getFillColor());
      filledOverlay.addShape (shape);
      overlay = filledOverlay;
    } // if

    // Create text overlay
    // -------------------
    else if (annotationCommand.equals (AnnotationListChooser.TEXT_COMMAND)) {
      String text = (String) JOptionPane.showInputDialog (
        this,
        "Enter the text to use for annotation:",
        "Enter annotation text",
        JOptionPane.QUESTION_MESSAGE
      );
      if (text == null || text.length() == 0) return;
      TextOverlay textOverlay = new TextOverlay (getDrawingColor());
      Rectangle2D bounds = shape.getBounds2D();
      Point2D base = new Point2D.Double (bounds.getMinX(), bounds.getMinY());
      TextElement element = new TextElement (text, base);
      textOverlay.addElement (element);
      textOverlay.setFont (getTextFont());
      textOverlay.setTextDropShadow (true);
      overlay = textOverlay;
    } // else if

    // Check for valid overlay
    // -----------------------
    if (overlay == null)
      throw new IllegalStateException ("Cannot create annotation overlay");

    // Set name and add to list
    // ------------------------
    overlay.setName (annotationCommand + listPanel.getOverlayCount (
      annotationCommand));
    overlay.setTransparency (getTransparency());
    listPanel.addOverlay (overlay);

  } // addAnnotation

  ////////////////////////////////////////////////////////////

  /** Deactivates the annotation chooser so that no annotation is selected. */
  public void deactivate () { listPanel.hidden.setSelected (true); }

  ////////////////////////////////////////////////////////////
  
  /** Implements annotation list buttons and title. */
  private class AnnotationListPanel 
    extends AbstractOverlayListPanel {

    // Variables
    // ---------

    /** The invisible button used for turning off the button group. */
    public JToggleButton hidden;

    /** The visual drawing color chooser. */
    public VisualColor visualColor;

    /** The visual line style chooser. */
    public VisualStroke visualStroke;

    /** The visual fill color chooser. */
    public VisualColor visualFill;

    /** The visual font chooser. */
    public VisualFont visualFont;

    /** The visual transparency chooser. */
    public VisualInteger visualTransparency;

    ////////////////////////////////////////////////////////

    /** Creates a new annotation list panel. */
    public AnnotationListPanel () {

      setBaseLayer (2000);

    } // AnnotationListPanel constructor

    ////////////////////////////////////////////////////////

    /** Creates the overlay list add buttons. */
    protected List getAddButtons () {

      // Create button list and listener
      // -------------------------------
      List buttons = new LinkedList();
      ActionListener buttonListener = new AnnotationListener();

      // Create buttons
      // --------------
      ButtonGroup group = new ButtonGroup();
      JToggleButton button;

      button = GUIServices.getIconToggle ("annotation.line");
      button.setActionCommand (LINE_COMMAND);
      button.addActionListener (buttonListener);
      button.setToolTipText (LINE_COMMAND);
      buttons.add (button);
      group.add (button);

      button = GUIServices.getIconToggle ("annotation.polyline");
      button.setActionCommand (POLYLINE_COMMAND);
      button.addActionListener (buttonListener);
      button.setToolTipText (POLYLINE_COMMAND);
      buttons.add (button);
      group.add (button);

      button = GUIServices.getIconToggle ("annotation.box");
      button.setActionCommand (BOX_COMMAND);
      button.addActionListener (buttonListener);
      button.setToolTipText (BOX_COMMAND);
      buttons.add (button);
      group.add (button);

      button = GUIServices.getIconToggle ("annotation.polygon");
      button.setActionCommand (POLYGON_COMMAND);
      button.addActionListener (buttonListener);
      button.setToolTipText (POLYGON_COMMAND);
      buttons.add (button);
      group.add (button);

      button = GUIServices.getIconToggle ("annotation.circle");
      button.setActionCommand (CIRCLE_COMMAND);
      button.addActionListener (buttonListener);
      button.setToolTipText (CIRCLE_COMMAND);
      buttons.add (button);
      group.add (button);

      button = GUIServices.getIconToggle ("annotation.curve");
      button.setActionCommand (CURVE_COMMAND);
      button.addActionListener (buttonListener);
      button.setToolTipText (CURVE_COMMAND);
      buttons.add (button);
      group.add (button);

      button = GUIServices.getIconToggle ("annotation.text");
      button.setActionCommand (TEXT_COMMAND);
      button.addActionListener (buttonListener);
      button.setToolTipText (TEXT_COMMAND);
      buttons.add (button);
      group.add (button);

      hidden = new JToggleButton();
      group.add (hidden);

      return (buttons);

    } // getAddButtons

    ////////////////////////////////////////////////////////
    
    /** 
     * Creates a custom panel showing the default annotation overlay
     * properties. 
     */
    protected JPanel getCustomPanel () {

      // Create panel
      // ------------
      JPanel panel = new JPanel (new GridBagLayout());
      panel.setBorder (new TitledBorder (new EtchedBorder(), 
        "Drawing Defaults"));
      GridBagConstraints gc = new GridBagConstraints();
      gc.anchor = GridBagConstraints.WEST;

      // Add drawing color chooser
      // -------------------------
      GUIServices.setConstraints (gc, 0, 0, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gc.insets = new Insets (2, 0, 2, 10);
      panel.add (new JLabel ("Drawing color:"), gc);
      gc.insets = new Insets (2, 0, 2, 0);

      visualColor = new VisualColor (Color.WHITE);
      GUIServices.setConstraints (gc, 1, 0, 1, 1, 
        GridBagConstraints.NONE, 1, 0);
      panel.add (visualColor.getComponent(), gc);

      // Add line style chooser
      // ----------------------
      GUIServices.setConstraints (gc, 0, 1, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gc.insets = new Insets (2, 0, 2, 10);
      panel.add (new JLabel ("Line style:"), gc);
      gc.insets = new Insets (2, 0, 2, 0);

      visualStroke = new VisualStroke (new BasicStroke());
      GUIServices.setConstraints (gc, 1, 1, 1, 1, 
        GridBagConstraints.NONE, 1, 0);
      panel.add (visualStroke.getComponent(), gc);

      // Add fill color chooser
      // ----------------------
      GUIServices.setConstraints (gc, 0, 2, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gc.insets = new Insets (2, 0, 2, 10);
      panel.add (new JLabel ("Fill color:"), gc);
      gc.insets = new Insets (2, 0, 2, 0);

      visualFill = new VisualColor (null);
      GUIServices.setConstraints (gc, 1, 2, 1, 1, 
        GridBagConstraints.NONE, 1, 0);
      panel.add (visualFill.getComponent(), gc);

      // Add font chooser
      // ----------------
      GUIServices.setConstraints (gc, 0, 3, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gc.insets = new Insets (2, 0, 2, 10);
      panel.add (new JLabel ("Text font:"), gc);
      gc.insets = new Insets (2, 0, 2, 0);

      visualFont = new VisualFont (new Font (null, Font.PLAIN, 12));
      GUIServices.setConstraints (gc, 1, 3, 1, 1, 
        GridBagConstraints.NONE, 1, 0);
      panel.add (visualFont.getComponent(), gc);

      // Add transparency chooser
      // ------------------------
      GUIServices.setConstraints (gc, 0, 4, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gc.insets = new Insets (2, 0, 2, 10);
      panel.add (new JLabel ("Transparency:"), gc);
      gc.insets = new Insets (2, 0, 2, 0);

      visualTransparency = new VisualInteger (Integer.valueOf (0));
      visualTransparency.setRestrictions (new int[] {0, 100, 5});
      GUIServices.setConstraints (gc, 1, 4, 1, 1, 
        GridBagConstraints.NONE, 1, 0);
      panel.add (visualTransparency.getComponent(), gc);

      return (panel);

    } // getCustomPanel

    ////////////////////////////////////////////////////////

    /** Gets the annotation list title. */
    protected String getTitle () { return (ANNOTATION_TITLE); }

    ////////////////////////////////////////////////////////

    /** Gets the annotation button panel title. */
    protected String getButtonTitle () { return (ANNOTATION_TITLE + " Tool"); }

    ////////////////////////////////////////////////////////

    /** Handles annotation add events. */
    private class AnnotationListener implements ActionListener {
      public void actionPerformed (ActionEvent event) {

        annotationCommand = event.getActionCommand();
        for (Iterator iter = actionListeners.iterator(); iter.hasNext(); )
          ((ActionListener) iter.next()).actionPerformed (event);

      } // actionPerformed
    } // AnnotationListener class

    ////////////////////////////////////////////////////////

  } // AnnotationListPanel class

  ////////////////////////////////////////////////////////////

} // AnnotationListChooser class

////////////////////////////////////////////////////////////////////////
