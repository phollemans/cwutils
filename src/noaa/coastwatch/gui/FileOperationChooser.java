////////////////////////////////////////////////////////////////////////
/*

     File: FileOperationChooser.java
   Author: Peter Hollemans
     Date: 2004/05/10

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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.util.Map;
import java.util.HashMap;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;

/**
 * <p>The <code>FileOperationChooser</code> class is a
 * <code>JToolBar</code> that allows the user to perform basic file
 * operations: Open, Close, Export.</p>
 *
 * <p>The operation chooser signals a change in the selected operation by
 * firing a <code>PropertyChangeEvent</code> whose property name is
 * <code>FileOperationChooser.OPERATION_PROPERTY</code>, and new value
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
public class FileOperationChooser
  extends JToolBar {

  // Constants
  // ---------

  /** The operation property. */
  public static final String OPERATION_PROPERTY = "operation";

  /** The open operation. */
  public static final String OPEN = "Open";
  private static final String OPEN_TIP = "Open new data file";

  /** The close operation. */
  public static final String CLOSE = "Close";
  private static final String CLOSE_TIP = "Close current data file";

  /** The save as operation. */
  public static final String EXPORT = "Export";
  private static final String EXPORT_TIP = "Export to image or data";

  /** The file information operation. */
  public static final String INFO = "Info";
  private static final String INFO_TIP = "Show data file information";

  // Variables
  // ---------

  /** The operation action listener. */
  private ActionListener operationAction;

  /** The show text flag. */
  private boolean showText;

  /** The static instance. */
  private static FileOperationChooser instance;

  /** The map of all buttons. */
  private Map<String, AbstractButton> buttonMap;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the closable flag.
   *
   * @param flag the closable flag, true if the close button should be enabled 
   * or false if not.
   */
  public void setClosable (
    boolean flag
  ) {

    buttonMap.get (CLOSE).setEnabled (flag);

  } // setClosable

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the savable flag.
   *
   * @param flag the savable flag, true if the save as button should be enabled 
   * or false if not.
   */
  public void setSavable (
    boolean flag
  ) {

    buttonMap.get (EXPORT).setEnabled (flag);

  } // setSavable

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the info flag.
   *
   * @param flag the info flag, true if the info button should be enabled 
   * or false if not.
   * 
   * @since 3.8.1
   */
  public void setInfo (
    boolean flag
  ) {

    buttonMap.get (INFO).setEnabled (flag);

  } // setInfo

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
  public FileOperationChooser () { this (JToolBar.HORIZONTAL, true); }

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
  public FileOperationChooser (
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
    addButton (new JButton (OPEN, GUIServices.getIcon ("file.open")), OPEN_TIP);
//    addButton (new JButton (CLOSE, GUIServices.getIcon ("file.close")), CLOSE_TIP);
    addButton (new JButton (EXPORT, GUIServices.getIcon ("file.export")), EXPORT_TIP); 
    addButton (new JButton (INFO, GUIServices.getIcon ("file.info")), INFO_TIP); 

  } // FileOperationChooser constructor

  ////////////////////////////////////////////////////////////

  private class OperationAction extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      String operation = event.getActionCommand();
      FileOperationChooser.this.firePropertyChange (OPERATION_PROPERTY, null, 
        operation);

    } // actionPerformed
  } // OperationAction class

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a static instance of this class using the default
   * constructor.
   *
   * @return the static instance of this class.
   */
  public static FileOperationChooser getInstance () {

    if (instance == null) instance = new FileOperationChooser();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    FileOperationChooser chooser = FileOperationChooser.getInstance();
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

} // FileOperationChooser class

////////////////////////////////////////////////////////////////////////
