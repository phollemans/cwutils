////////////////////////////////////////////////////////////////////////
/*
     FILE: VariableChooser.java
  PURPOSE: Selects a variable from a data reader.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/17
  CHANGES: 2014/11/11, PFH
           - Changes: Added dispose() method.
           - Issue: We had problems with garbage collecting all the memory when
             a data analysis panel was closed, and it turned out that releasing
             panels here and removing action listeners from the combo solved
             most of the memory problems.

  CoastWatch Software Library and Utilities
  Copyright 2004-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;

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
   * Disposes of any resources used by this chooser. 
   * 
   * @since 3.3.1
   */
  public void dispose () {

    /**
     * We do the following because it was found that retaining references
     * in this class were transitively holding references to the other
     * panels and data caches that were associated with this class.
     */
    removeAll();
    for (ActionListener listener : combo.getActionListeners())
      combo.removeActionListener (listener);
    combo = null;

  } // dispose
  
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
