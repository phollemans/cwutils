////////////////////////////////////////////////////////////////////////
/*
     FILE: FileOperationChooser.java
  PURPOSE: Allows the selection of a file operation.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/10
  CHANGES: 2006/10/19, PFH, changed save as to export
           2013/02/22, PFH, added special Aqua handling
           
  CoastWatch Software Library and Utilities
  Copyright 1998-2013, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.*;

/**
 * The <code>FileOperationChooser</code> class is a
 * <code>JToolBar</code> that allows the user to perform basic file
 * operations: Open, Close, Export.<p>
 *
 * The operation chooser signals a change in the selected operation by
 * firing a <code>PropertyChangeEvent</code> whose property name is
 * <code>FileOperationChooser.OPERATION_PROPERTY</code>, and new value
 * contains an operation name from the constants in this class.<p>
 *
 * A static instance of this class is available via
 * <code>getInstance()</code> so that a single chooser may be used
 * from multiple classes, even though it is only included in one
 * layout manager.
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

  /** The close operation. */
  public static final String CLOSE = "Close";

  /** The save as operation. */
  public static final String EXPORT = "Export";

  // Variables
  // ---------

  /** The operation action listener. */
  private ActionListener operationAction;

  /** The show text flag. */
  private boolean showText;

  /** The static instance. */
  private static FileOperationChooser instance;

  /** The close button. */
  private JButton closeButton;

  /** The save as button. */
  private JButton exportButton;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the closable flag, true if the close button should enabled.
   */
  public void setClosable (
    boolean flag
  ) {

    closeButton.setEnabled (flag);

  } // setClosable

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the savable flag, true if the save as button should enabled.
   */
  public void setSavable (
    boolean flag
  ) {

    exportButton.setEnabled (flag);

  } // setSavable

  ////////////////////////////////////////////////////////////

  /** Adds the specified button to the toolbar. */
  private void addButton (
    AbstractButton button
  ) {

    // Setup button properties
    // -----------------------
    button.getModel().setActionCommand (button.getText());
    if (showText) {
      button.setHorizontalTextPosition (SwingConstants.CENTER);
      button.setVerticalTextPosition (SwingConstants.BOTTOM);
      button.setIconTextGap (0);
    } // if
    else {
      button.setToolTipText (button.getText());
      button.setText ("");
    } // else
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

    // Create buttons
    // --------------
    addButton (new JButton (OPEN, GUIServices.getIcon ("file.open")));
    closeButton = new JButton (CLOSE, GUIServices.getIcon ("file.close"));
    addButton (closeButton);
    exportButton = new JButton (EXPORT, GUIServices.getIcon ("file.export")); 
    addButton (exportButton);

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
  public static void main (String[] argc) {

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
