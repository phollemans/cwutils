////////////////////////////////////////////////////////////////////////
/*
     FILE: VariableChooser.java
  PURPOSE: Selects a variable from a data reader.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/17
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;

/**
 * The <code>VariableChooser</code> is a simple panel that displays a
 * list of variable names and allows the user to select one of the
 * variables.
 *
 * The chooser signals a change in the selected variable by
 * firing a <code>PropertyChangeEvent</code> whose property name is
 * <code>VariableChooser.VARIABLE_PROPERTY</code>, and new value
 * contains the variable name.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VariableChooser
  extends JPanel {

  // Constants
  // ---------

  /** The variable property. */
  public static final String VARIABLE_PROPERTY = "variable";

  /** The combo box. */
  private JComboBox combo;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new variable chooser panel.  The first variable is
   * initially selected.
   *
   * @param variableList the list of variables to make available.
   */  
  public VariableChooser (
    List variableList
  ) {

    super (new FlowLayout (FlowLayout.LEFT, 5, 2));

    // Create label and combo box
    // --------------------------
    this.add (new JLabel ("Variable:"));
    combo = new JComboBox (variableList.toArray());
    this.add (combo);
    combo.addActionListener (new SelectionListener());

  } // VariableChooser

  ////////////////////////////////////////////////////////////

  /** Gets the current variable name. */
  public String getVariable () { 

    return ((String) combo.getSelectedItem()); 

  } // getVariable

  ////////////////////////////////////////////////////////////

  /** 
   * Fires a property change event when the combo selection changes.
   */
  private class SelectionListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      VariableChooser.this.firePropertyChange (VARIABLE_PROPERTY, null, 
        getVariable());

    } // actionPerformed
  } // SelectionListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    // Get reader
    // ----------
    List gridList = null;
    try { 
      EarthDataReader reader = 
        EarthDataReaderFactory.create (argv[0]); 
      gridList = reader.getAllGrids();
    } // try
    catch (Exception e) { e.printStackTrace(); System.exit (1); }
    noaa.coastwatch.gui.TestContainer.showFrame (
      new VariableChooser (gridList));

  } // main

  ////////////////////////////////////////////////////////////

} // VariableChooser

////////////////////////////////////////////////////////////////////////
