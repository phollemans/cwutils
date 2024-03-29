////////////////////////////////////////////////////////////////////////
/*

     File: VariableOptionPanel.java
   Author: Peter Hollemans
     Date: 2004/05/04

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
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;

/** 
 * The <code>VariableOptionPanel</code> class allows the user to
 * choose a subset from a list of variables.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VariableOptionPanel
  extends JPanel {

  // Variables
  // ---------

  /** The list of variable names. */
  private JList variableList;

  ////////////////////////////////////////////////////////////

  /** Gets the list of selected variable names. */
  public List getVariables () { 

    return (variableList.getSelectedValuesList());

  } // getVariables

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the list of selected variable names.
   *
   * @param nameList the list of variable names to select.  If
   * the variable list is in single selection mode, only the
   * first name from the list is selected.
   */
  public void setVariables (
    List<String> nameList
  ) { 

    // Check for single selection only
    // -------------------------------
    boolean isSingle = (variableList.getSelectionMode() == 
      ListSelectionModel.SINGLE_SELECTION);

    // Get list of variables
    // ---------------------
    ListModel listModel = variableList.getModel();
    List<String> variablesInList = new ArrayList<String>();
    for (int i = 0; i < listModel.getSize(); i++)
      variablesInList.add ((String) listModel.getElementAt (i));

    // Select each variable
    // --------------------
    for (String varName : nameList) {
      int index = variablesInList.indexOf (varName);
      if (index != -1) {
        variableList.addSelectionInterval (index, index);
        if (isSingle) break;
      } // if
    } // for

  } // setVariables

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new variable option panel. 
   *
   * @param variableNames the list of variables names to choose from.
   * @param isSingle the single selection flag, true to allow only
   * single selections.
   */
  public VariableOptionPanel (
    List variableNames,
    boolean isSingle
  ) {

    // Initialize
    // ----------
    super (new GridBagLayout());
    setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), "Variable Options"),
      new EmptyBorder (4, 4, 4, 4)
    ));
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 0);

    // Add variable list
    // -----------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    this.add (new JLabel ("Export variables:"));

    GUIServices.setConstraints (gc, 0, 1, 2, 1, 
      GridBagConstraints.BOTH, 1, 1);
    variableList = new JList (variableNames.toArray());
    if (isSingle) 
      variableList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    this.add (new JScrollPane (variableList), gc);
    variableList.setVisibleRowCount (5);

  } // VariableOptionPanel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    JPanel panel = new VariableOptionPanel (Arrays.asList (new String[] {
      "variable_name_1",
      "variable_name_2",
      "variable_name_3",
      "variable_name_4"
    }), true);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VariableOptionPanel class

////////////////////////////////////////////////////////////////////////
