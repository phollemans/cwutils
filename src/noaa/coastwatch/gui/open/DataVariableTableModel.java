////////////////////////////////////////////////////////////////////////
/*
     FILE: DataVariableTableModel.java
  PURPOSE: Models variable information.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/22
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.*;

/** 
 * The <code>DataVariableTableModel</code> class models the
 * information in a list of {@link noaa.coastwatch.util.DataVariable}
 * objects.  It contains a number of columns including the variable
 * name and units.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
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
  private List variableList;

  ////////////////////////////////////////////////////////////

  /** Creates a new table model with no data. */
  public DataVariableTableModel () {

    this.variableList = new ArrayList();

  } // DataVariableTableModel constructor

  ////////////////////////////////////////////////////////////

  /** Creates a new table model using the varible list. */
  public DataVariableTableModel (List variableList) { 

    this.variableList = variableList;

  } // DataVariableTableModel constructor

  ////////////////////////////////////////////////////////////

  /** Clears the variable list so that no data is contained. */
  public void clear () { setVariableList (new ArrayList()); }

  ////////////////////////////////////////////////////////////

  /** Sets the variable list for this model. */
  public void setVariableList (List variableList) {

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

    DataVariable var = (DataVariable) variableList.get (row);
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

    return ((DataVariable) variableList.get (row));

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

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    // Create table
    // ------------
    EarthDataReader reader = EarthDataReaderFactory.create (argv[0]);
    List gridNames = reader.getAllGrids();
    List gridPreviews = new ArrayList();
    for (Iterator iter = gridNames.iterator(); iter.hasNext();)
      gridPreviews.add (reader.getPreview ((String) iter.next()));
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
