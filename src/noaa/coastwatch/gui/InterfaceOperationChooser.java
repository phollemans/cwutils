/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.util.Map;
import java.util.HashMap;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;

/**
 * <p>The <code>InterfaceOperationChooser</code> class is a
 * <code>JToolBar</code> that allows the user to perform basic interface change
 * operations.</p>
 *
 * <p>A static instance of this class is available via
 * <code>getInstance()</code> so that a single chooser may be used
 * from multiple classes, even though it is only included in one
 * layout manager.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class InterfaceOperationChooser extends JToolBar {

  // Constants
  // ---------

  /** The operation property. */
  public static final String OPERATION_PROPERTY = "operation";

  /** The controls operation. */
  public static final String CONTROLS = "Controls";
  private static final String CONTROLS_TIP = "Show/hide control tabs";

  /** The full screen operation. */
  public static final String FULLSCREEN = "Fullscreen";
  private static final String FULLSCREEN_TIP = "Enter full screen presentation mode";

  // Variables
  // ---------

  /** The operation action listener. */
  private ActionListener operationAction;

  /** The show text flag. */
  private boolean showText;

  /** The static instance. */
  private static InterfaceOperationChooser instance;

  /** The map of all buttons. */
  private Map<String, AbstractButton> buttonMap;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the visiblity of the toolbar text labels.
   * 
   * @param flag the new text visibility flag value.
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
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.setToolTipText (tip);
    if (!showText) button.setText ("");
    button.addActionListener (operationAction);
    if (GUIServices.IS_AQUA) button.setBorderPainted (false);
//    button.setFocusable (false);

    // Add button
    // ----------
    this.add (button);

  } // addButton

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new horizontal operation chooser with text and icons.
   * By default, the chooser is set to be not floatable.
   */
  public InterfaceOperationChooser () { this (JToolBar.HORIZONTAL, true); }

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
  public InterfaceOperationChooser (
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
    operationAction = new OperationAction();
    this.showText = showText;
    this.buttonMap = new HashMap<>();

    // Create buttons
    // --------------
    addButton (new JButton (CONTROLS, GUIServices.getIcon ("interface.control.tabs")), CONTROLS_TIP);
    addButton (new JButton (FULLSCREEN, GUIServices.getIcon ("interface.fullscreen")), FULLSCREEN_TIP);

  } // FileOperationChooser constructor

  ////////////////////////////////////////////////////////////

  private class OperationAction extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      String operation = event.getActionCommand();
      InterfaceOperationChooser.this.firePropertyChange (OPERATION_PROPERTY, null, operation);

    } // actionPerformed
  } // OperationAction class

  ////////////////////////////////////////////////////////////

  /** Sets the enabled status of the chooser buttons. */
  public void setEnabled (boolean flag) {

    buttonMap.forEach ((k,button) -> button.setEnabled (flag));

  } // setEnabled

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a static instance of this class using the default
   * constructor.
   *
   * @return the static instance of this class.
   */
  public static InterfaceOperationChooser getInstance () {

    if (instance == null) instance = new InterfaceOperationChooser();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    var chooser = InterfaceOperationChooser.getInstance();
    chooser.addPropertyChangeListener (
      FileOperationChooser.OPERATION_PROPERTY,
      new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          System.out.println ("got new value = " + event.getNewValue());
        } // propertyChange
      });
    noaa.coastwatch.gui.TestContainer.showFrame (chooser);

  } // main

  ////////////////////////////////////////////////////////////

} // InterfaceOperationChooser class

////////////////////////////////////////////////////////////////////////
