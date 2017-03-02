////////////////////////////////////////////////////////////////////////
/*

     File: VisualArray.java
   Author: Peter Hollemans
     Date: 2004/02/29

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
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;

/**
 * The <code>VisualArray</code> class represents an array as a list
 * with a text field, and add/remove buttons.  When the add button is
 * pressed, the contents of the text field are added to the list.
 * When the remove button is pressed, the highlighted items in the
 * list are removed.  The visual array supports arrays of primitive
 * types, and <code>String</code> objects.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualArray 
  extends AbstractVisualObject {

  // Variables
  // ---------

  /** The panel holding the list, field, and buttons. */
  private JPanel panel;

  /** The text field used for value input. */
  private JTextField field;

  /** The list holding the array values. */
  private JList list;

  /** The data class of the array elements. */
  private Class dataClass;

  /** The data class of the array elements stored in the list. */
  private Class listDataClass;

  /** The constructor for converting text to list data values. */
  private Constructor listDataConstructor;

  /** The add button. */
  private JButton addButton;

  /** The remove button. */
  private JButton removeButton;

  ////////////////////////////////////////////////////////////

  /** Creates a new visual array object using the specified array. */
  public VisualArray (
    Object array
  ) {                     

    // Get data class
    // --------------
    dataClass = array.getClass().getComponentType();

    // Convert array to object array
    // -----------------------------
    Object[] objectArray = new Object[Array.getLength(array)];
    for (int i = 0; i < objectArray.length; i++)
      objectArray[i] = Array.get (array, i);

    // Get list data class
    // -------------------
    if (objectArray.length > 0)
      listDataClass = objectArray[0].getClass();
    else if (Object.class.isAssignableFrom (dataClass))
      listDataClass = dataClass;
    else {
      Object sourceArray = Array.newInstance (dataClass, 1);
      Object[] destArray = new Object[1];
      destArray[0] = Array.get (sourceArray, 0);
      listDataClass = destArray[0].getClass();
    } // else

    // Get list data type constructor
    // ------------------------------
    try {
      listDataConstructor = listDataClass.getConstructor (
        new Class[] {String.class});
    } // try
    catch (NoSuchMethodException e) {
      throw (new IllegalArgumentException (
        "Array element class has no String based constructor"));
    } // catch

    // Create main panel
    // -----------------
    panel = new JPanel (new BorderLayout());
    
    // Create list model
    // -----------------
    DefaultListModel model = new DefaultListModel();
    for (int i = 0; i < objectArray.length; i++)
      model.addElement (objectArray[i]);

    // Create list
    // -----------
    list = new JList (model);
    list.addListSelectionListener (new ListListener());
    list.setVisibleRowCount (6);
    panel.add (new JScrollPane (list), BorderLayout.CENTER);

    // Add delete key listener to list
    // -------------------------------
    list.addKeyListener (new KeyAdapter() {
        public void keyPressed (KeyEvent event) {
          if (removeButton.isEnabled() && 
              event.getKeyCode() == KeyEvent.VK_DELETE)
            removeButton.doClick();
        } // keyTyped
      });

    // Create control panel
    // --------------------
    //    Box controlPanel = Box.createHorizontalBox();
    JPanel controlPanel = new JPanel (new GridBagLayout());
    panel.add (controlPanel, BorderLayout.SOUTH);

    // Create text field
    // -----------------
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (2, 2, 2, 2);
    field = new JTextField (12);
    field.setEditable (true);
    field.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          addButton.doClick();
        } // actionPerformed
      });
    //    controlPanel.add (field);
    controlPanel.add (field, gc);

    // Create add button
    // -----------------
    GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
      GridBagConstraints.NONE, 0, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    addButton = GUIServices.getIconButton ("list.add");
    GUIServices.setSquare (addButton);
    addButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          addEntry();
        } // actionPerformed
      });
    addButton.setToolTipText ("Add");
    //    controlPanel.add (addButton);
    controlPanel.add (addButton, gc);

    // Create remove button
    // --------------------
    removeButton = GUIServices.getIconButton ("list.remove");
    GUIServices.setSquare (removeButton);
    removeButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          removeEntry();
        } // actionPerformed
      });
    removeButton.setEnabled (false);
    removeButton.setToolTipText ("Remove");
    //    controlPanel.add (removeButton);
    controlPanel.add (removeButton, gc);

  } // VisualArray constructor

  ////////////////////////////////////////////////////////////

  /** Handles list selection events. */
  private class ListListener implements ListSelectionListener {
    public void valueChanged (ListSelectionEvent event) {

      removeButton.setEnabled (list.getSelectedIndices().length != 0);

    } // valueChanged
  } // ListListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Adds an entry to the list using the text field contents and fires
   * a property change event.  If the text field cannot be converted
   * to an object value, no operation is performed.
   */
  private void addEntry () {

    // Create list data object
    // -----------------------
    Object value;
    try {
      value = listDataConstructor.newInstance (
        new Object[] {field.getText()});
    } // try
    catch (Exception e) { return; }

    // Add entry to list
    // -----------------
    DefaultListModel model = (DefaultListModel) list.getModel();
    model.addElement (value);
    list.ensureIndexIsVisible (model.getSize() - 1);

    // Clear text field
    // ----------------
    field.setText ("");

    // Fire property change
    // --------------------
    firePropertyChange();

  } // addEntry

  ////////////////////////////////////////////////////////////

  /** 
   * Removes one or more entries from the list using the highlighted
   * list entries and fires a property change event.
   */
  private void removeEntry () {

    // Remove selected items
    // ---------------------
    Object[] values = list.getSelectedValuesList().toArray();
    DefaultListModel model = (DefaultListModel) list.getModel();
    for (int i = 0; i < values.length; i++)
      model.removeElement (values[i]);

    // Fire property change
    // --------------------
    firePropertyChange();

  } // removeEntry

  ////////////////////////////////////////////////////////////

  /** Gets the panel used to represent the array. */
  public Component getComponent () { return (panel); }

  ////////////////////////////////////////////////////////////

  /** Gets the array value. */
  public Object getValue () { 

    // Create array
    // ------------
    Object[] objectArray = ((DefaultListModel)list.getModel()).toArray();
    Object array = Array.newInstance (dataClass, objectArray.length);
    for (int i = 0; i < objectArray.length; i++)
      Array.set (array, i, objectArray[i]);

    return (array); 

  } // getValue

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    panel.add (new VisualArray (new double[] {1.2, 2.3}).getComponent());
    panel.add (new VisualArray (new String[] {"hello", "world"}).getComponent());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualArray class

////////////////////////////////////////////////////////////////////////
