////////////////////////////////////////////////////////////////////////
/*

     File: DataVariableTableModel.java
   Author: Peter Hollemans
     Date: 2005/06/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.awt.Container;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.util.DataVariable;

/** 
 * The <code>DataVariableTableModel</code> class models the
 * information in a list of {@link noaa.coastwatch.util.DataVariable}
 * objects.  It contains a number of columns including the variable
 * name and units.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
@Deprecated
public class DataVariableTableModel
  extends AbstractTableModel {

  // Constants
  // ---------

  /** Column index of variable name. */
  private static final int NAME_COLUMN = 0;

  /** Column index of variable units. */
  private static final int UNITS_COLUMN = 1;

  // Variables
  // ---------

  /** The list of variables for the model. */
  private List<DataVariable> variableList;

  ////////////////////////////////////////////////////////////

  /** Creates a new table model with no data. */
  public DataVariableTableModel () {

    this.variableList = new ArrayList<>();

  } // DataVariableTableModel constructor

  ////////////////////////////////////////////////////////////

  /** Creates a new table model using the varible list. */
  public DataVariableTableModel (List<DataVariable> variableList) { 

    this.variableList = variableList;

  } // DataVariableTableModel constructor

  ////////////////////////////////////////////////////////////

  /** Clears the variable list so that no data is contained. */
  public void clear () { setVariableList (new ArrayList<DataVariable>()); }

  ////////////////////////////////////////////////////////////

  /** Sets the variable list for this model. */
  public void setVariableList (List<DataVariable> variableList) {

    this.variableList = variableList;
    fireTableDataChanged();

  } // setVariableList

  ////////////////////////////////////////////////////////////

  /** Gets the number of table rows. */
  public int getRowCount () { return (variableList.size()); }

  ////////////////////////////////////////////////////////////
    
  /** Gets the number of table columns. */
  public int getColumnCount () { return (2); }

  ////////////////////////////////////////////////////////////

  /** Gets the table data value. */
  public Object getValueAt (int row, int column) {

    DataVariable var = variableList.get (row);
    switch (column) {
    case NAME_COLUMN: return (var.getName());
    case UNITS_COLUMN: return (var.getUnits());
    default: throw new IllegalArgumentException (
      "Column index " + column + " not allowed");
    } // switch

  } // getValueAt

  ////////////////////////////////////////////////////////////

  /** Gets the variable at the specified row. */
  public DataVariable getVariable (int row) { 

    return (variableList.get (row));

  } // getVariable

  ////////////////////////////////////////////////////////////

  /** Gets the table column name. */
  public String	getColumnName (int column) { 

    switch (column) {
    case NAME_COLUMN: return ("Name");
    case UNITS_COLUMN: return ("Units");
    default: throw new IllegalArgumentException (
      "Column index " + column + " not allowed");
    } // switch

  } // getColumnName

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Create table
    // ------------
    EarthDataReader reader = EarthDataReaderFactory.create (argv[0]);
    List<DataVariable> gridPreviews = new ArrayList<>();

    try {
      List<String> gridNames = reader.getAllGrids();
      for (var name : gridNames) gridPreviews.add (reader.getPreview (name));
    } // try
    catch (IOException e) { }
    AbstractTableModel model = new DataVariableTableModel (gridPreviews);
    JTable table = new JTable (model);
    JScrollPane scrollpane = new JScrollPane (table);

    // Create frame
    // ------------
    final JFrame frame = new JFrame (DataVariableTableModel.class.getName());
    frame.getContentPane().add (scrollpane);
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    frame.pack();

    // Show frame
    // ----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          frame.setVisible (true);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // DataVariableTableModel

////////////////////////////////////////////////////////////////////////
