////////////////////////////////////////////////////////////////////////
/*

     File: ViewOperationChooser.java
   Author: Peter Hollemans
     Date: 2004/02/19

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

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import noaa.coastwatch.gui.FullScreenToolBar;
import noaa.coastwatch.gui.GUIServices;

/**
 * <p>The <code>ViewOperationChooser</code> class is a
 * <code>JToolBar</code> that allows the user to select view transform
 * operations using a set of toolbar buttons.  There are seven view
 * transform operations in total: Magnify, Shrink, 1:1, Zoom, Pan,
 * Recenter, and Reset, and Fit.  The Magnify, Shrink, 1:1, Reset, and Fit
 * operations are designed to be non-interactive, single-click modes.
 * The user would click these buttons once, then the view should
 * change according to the operation.  The Zoom, Pan, and Recenter
 * operations are interactive in the sense that once selected,
 * additional input must be obtained from the user in order the change
 * the view transform.</p>
 *
 * <p>The operation chooser signals a change in the selected operation by
 * firing a <code>PropertyChangeEvent</code> whose property name is
 * <code>ViewOperationChooser.OPERATION_PROPERTY</code>, and new value
 * contains an operation name from the constants in this class.</p>
 *
 * <p>A static instance of this class is available via
 * <code>getInstance()</code> so that a single chooser may be used
 * from multiple classes, even though it is only included in one
 * layout manager.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ViewOperationChooser
  extends JToolBar {

  // Constants
  // ---------

  /** The operation property. */
  public static final String OPERATION_PROPERTY = "operation";

  /** The magnify operation. */
  public static final String MAGNIFY = "Magnify";
  private static final String MAGNIFY_TIP = "Zoom in";

  /** The shrink operation. */
  public static final String SHRINK = "Shrink";
  private static final String SHRINK_TIP = "Zoom out";

  /** The 1:1 operation. */
  public static final String ONE_TO_ONE = "Actual";
  private static final String ONE_TO_ONE_TIP = "Zoom to actual size";

  /** The zoom operation. */
  public static final String ZOOM = "Zoom";
  private static final String ZOOM_TIP = "Zoom to selection";

  /** The pan operation. */
  public static final String PAN = "Pan";
  private static final String PAN_TIP = "Shift view center";

  /** The recenter operation. */
  public static final String RECENTER = "Recenter";
  private static final String RECENTER_TIP = "Recenter view";

  /** The reset operation. */
  public static final String RESET = "Reset";
  private static final String RESET_TIP = "Fit image to window";

  /** The fit operation. */
  public static final String FIT = "Fit";
  private static final String FIT_TIP = "Fill window";

  /** The close operation (full screen only). */
  public static final String CLOSE = "Close";
  private static final String CLOSE_TIP = "Exit full screen";

  // Variables
  // ---------

  /** The button group for tool buttons. */
  private ButtonGroup group;

  /** The operation action listener. */
  private ActionListener operationAction;

  /** The show text flag. */
  private boolean showText;

  /** The static instance. */
  private static ViewOperationChooser instance;

  /** The invisible button used for turning off the button group. */
  private JToggleButton hidden;

  /** The map of all buttons. */
  private Map<String, AbstractButton> buttonMap;

  /** The fullscreen toolbar for this chooser. */
  private FullScreenToolBar fsToolbar;

  ////////////////////////////////////////////////////////////

  /** Deactivates the view chooser so that no operation is selected. */
  public void deactivate () { hidden.setSelected (true); }

  ////////////////////////////////////////////////////////////

  /** Sets the enabled status of the chooser buttons. */
  public void setEnabled (boolean flag) {

    buttonMap.forEach ((k,button) -> button.setEnabled (flag));

  } // setEnabled

  ////////////////////////////////////////////////////////////

  /**
   * Sets the visiblity of the toolbar text labels.
   * 
   * @param flag the new text visibility flag value.
   * 
   * @since 3.8.1
   */
  public void setShowText (boolean flag) {

    if (this.showText != flag) {
      this.showText = flag;
      buttonMap.forEach ((text,button) -> { button.setText (showText ? text : ""); });
    } // if

  } // setShowText

  ////////////////////////////////////////////////////////////

  /** Adds the specified button to the toolbar. */
  private void addButton (
    AbstractButton button,
    String tip
  ) {

    var text = button.getText();
    buttonMap.put (text, button);

    // Setup button properties
    // -----------------------
    button.getModel().setActionCommand (text);
    if (button instanceof JToggleButton)
      group.add (button);
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.setToolTipText (tip);
    if (!showText) button.setText ("");
    button.addActionListener (operationAction);
    if (GUIServices.IS_AQUA) button.setBorderPainted (false);

    // Add button
    // ----------
    this.add (button);

  } // addButton

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new horizontal operation chooser with text and icons.
   * By default, the chooser is set to be not floatable.
   */
  public ViewOperationChooser () { this (JToolBar.HORIZONTAL, true); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new operation chooser.  By default, the chooser is set
   * to be not floatable.
   *
   * @param orientation the toolbar orientation, either
   * <code>HORIZONTAL</code> or <code>VERTICAL</code>.
   * @param showText the show text flag.  If true, text is shown below
   * the icon in each button.  If false, no text is shown, but a tool
   * tip is set for the button.
   */
  public ViewOperationChooser (
    int orientation,
    boolean showText
  ) {

    // Setup toolbar
    // -------------
    this.setFloatable (false);
    if (orientation == HORIZONTAL)
      this.setLayout (new GridLayout (1, 0, 2, 2));
    else
      this.setLayout (new GridLayout (0, 1, 2, 2));

    // Set variables
    // -------------
    group = new ButtonGroup();
    operationAction = new OperationAction();
    this.showText = showText;
    buttonMap = new HashMap<>();

    // Create buttons
    // --------------
    addButton (new JButton (MAGNIFY, GUIServices.getIcon ("view.magnify")), MAGNIFY_TIP);
    addButton (new JButton (SHRINK, GUIServices.getIcon ("view.shrink")), SHRINK_TIP);
    addButton (new JButton (ONE_TO_ONE, GUIServices.getIcon ("view.one_to_one")), ONE_TO_ONE_TIP);
    addButton (new JToggleButton (ZOOM, GUIServices.getIcon ("view.zoom")), ZOOM_TIP);
    addButton (new JToggleButton (PAN, GUIServices.getIcon ("view.pan")), PAN_TIP);
    addButton (new JToggleButton (RECENTER, GUIServices.getIcon ("view.recenter")), RECENTER_TIP);
    addButton (new JButton (FIT, GUIServices.getIcon ("view.fit")), FIT_TIP);
    addButton (new JButton (RESET, GUIServices.getIcon ("view.reset")), RESET_TIP);

    // Create hidden button
    // --------------------
    hidden = new JToggleButton();
    group.add (hidden);

  } // ViewOperationChooser

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the selected operation, or null if no operation is selected.
   */
  public String getOperation () { 

    ButtonModel model = group.getSelection();
    if (model != null)
      return (model.getActionCommand());
    else
      return (null);

  } // getOperation

  ////////////////////////////////////////////////////////////

  private class OperationAction extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      String operation = event.getActionCommand();
      ViewOperationChooser.this.firePropertyChange (OPERATION_PROPERTY, null, 
        operation);

    } // actionPerformed
  } // OperationAction class

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a static instance of this class using the default
   * constructor. 
   */
  public static ViewOperationChooser getInstance () {

    if (instance == null) instance = new ViewOperationChooser();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a button based on its text label.
   *
   * @param text the text label to search for.
   *
   * @return the button with the specified text or null if not found.
   */
  private AbstractButton getButton (
    String text
  ) {    

    return (buttonMap.get (text));

  } // getButton

  ////////////////////////////////////////////////////////////

  /**
   * Creates a full screen version of the chooser with a subset of the
   * view operations, and an additional exit button.
   *
   * @return the full screen mode chooser.
   */
  public FullScreenToolBar getFullScreenChooser () {

    // Create toolbar
    // --------------
    if (fsToolbar == null) {
      fsToolbar = new FullScreenToolBar();
      fsToolbar.addButton (getButton (MAGNIFY), GUIServices.getIcon ("view.magnify.fullscreen"), MAGNIFY_TIP);
      fsToolbar.addButton (getButton (SHRINK), GUIServices.getIcon ("view.shrink.fullscreen"), SHRINK_TIP);
      fsToolbar.addButton (getButton (ONE_TO_ONE), GUIServices.getIcon ("view.one_to_one.fullscreen"), ONE_TO_ONE_TIP);
      fsToolbar.addButton (getButton (ZOOM), GUIServices.getIcon ("view.zoom.fullscreen"), ZOOM_TIP);
      fsToolbar.addButton (getButton (PAN), GUIServices.getIcon ("view.pan.fullscreen"), PAN_TIP);
      fsToolbar.addSeparator();
      JButton closeButton = new JButton (CLOSE, GUIServices.getIcon ("view.close.fullscreen"));
      closeButton.getModel().setActionCommand (CLOSE);
      closeButton.addActionListener (operationAction);
      fsToolbar.addButton (closeButton, CLOSE_TIP);
    } // if
    
    return (fsToolbar);

  } // getFullScreenChooser

  ////////////////////////////////////////////////////////////

  /**
   * Performs an operation programatically, rather than having to
   * wait for the user to click a button on the chooser.
   *
   * @param operation the operation to perform.
   *
   * @see #getOperation
   */
  public void performOperation (
    String operation
  ) {

    AbstractButton button = getButton (operation);
    if (button != null) button.doClick();

  } // performOperation

  ////////////////////////////////////////////////////////////

} // ViewOperationChooser class

////////////////////////////////////////////////////////////////////////
