////////////////////////////////////////////////////////////////////////
/*

     File: SortedTableModel.java
   Author: Peter Hollemans
     Date: 2006/05/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * A <code>SortedTableModel</code> can be used to provide row
 * sorting services for any
 * <code>javax.swing.table.TableModel</code>.  The new model
 * simply wraps the old one and translates row indicies through
 * the wrapper so as to make it appear that the table is sorted
 * according to the values held in a specific column of the
 * table.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class SortedTableModel
  implements TableModel {

  // Variables
  // ---------

  /** The current sorting column, or -1 for unsorted. */
  private int sortColumn;

  /** The current sorting order, true if sorting is ascending. */
  private boolean isAscending;

  /** The internal table model upon which this one is based. */
  private TableModel model;

  /** The mapping table from displayed row index to actual row index. */
  private int[] rowMap;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a sorted model using the specified unsorted model
   * for its data.  The table is initially unsorted so that rows
   * in the sorted table model are in exactly the same order as
   * rows in the unsorted model.
   *
   * @param model the model to use for row and column data.
   */
  public SortedTableModel (
    TableModel model
  ) {

    // Initialize
    // ----------
    this.model = model;
    setSort (-1, true);

    // Reset sorting on new data
    // -------------------------
    model.addTableModelListener (new TableModelListener() {
        public void tableChanged (TableModelEvent e) { 
          setSort (sortColumn, isAscending);
        } // tableChanged
      });

  } // SortedTableModel

  ////////////////////////////////////////////////////////////

  /** 
   * Set the sorting column and order.
   * 
   * @param sortColumn the new sorting column, or -1 for no sorting.
   * @param isAscending the new sorting order, true for
   * ascending.
   */
  public void setSort (
    int sortColumn,
    boolean isAscending
  ) {

    this.sortColumn = sortColumn;
    this.isAscending = isAscending;

    // Check for non-sorting
    // ---------------------
    if (sortColumn == -1) {
      rowMap = new int[model.getRowCount()];
      for (int i = 0; i < rowMap.length; i++)
        rowMap[i] = i;
    } // if

    // Create row sorting map
    // ----------------------
    else {
      rowMap = new int[model.getRowCount()];
      List rowList = new ArrayList();
      for (int i = 0; i < rowMap.length; i++)
        rowList.add (new TableRow (model.getValueAt (i, sortColumn), i));
      Collections.sort (rowList);
      if (!isAscending) Collections.reverse (rowList);
      Iterator iter = rowList.iterator();
      for (int i = 0; i < rowMap.length; i++) {
        TableRow row = (TableRow) iter.next();
        rowMap[i] = row.row;
      } // for
    } // else

  } // setSort

  ////////////////////////////////////////////////////////////

  /** 
   * Bundles a table cell value and row together for sorting
   * purposes. 
   */
  private class TableRow implements Comparable {
    public Object value;
    public int row;
    public TableRow (Object value, int row) { 
      this.value = value; this.row = row; 
    } // TableRow constructor
    public int compareTo (Object o) { 
      return (((Comparable) value).compareTo (((TableRow) o).value));
    } // compareTo
  } // TableRow class

  ////////////////////////////////////////////////////////////

  /** Gets the current sorting column, or -1 for no sorting. */
  public int getSortColumn () { return (sortColumn); }

  ////////////////////////////////////////////////////////////

  /** Gets the current sorting order. */
  public boolean getIsAscending () { return (isAscending); }

  ////////////////////////////////////////////////////////////

  /** 
   * Converts a row index in the sorted model to a row index in the
   * underlying model.
   *
   * @param row the row index to convert.
   *
   * @return the row index in the underlying table model.
   */
  public int convertRowIndexToModel (int row) { return (rowMap[row]); }

  ////////////////////////////////////////////////////////////

  /** 
   * Converts a row index in the underlying model to a row index in the
   * sorted model.
   *
   * @param row the row index to convert.
   *
   * @return the row index in the sorted table model.
   */
  public int convertRowIndexToView (int row) { 

    for (int i = 0; i < rowMap.length; i++)
      if (rowMap[i] == row) return (i);
    return (-1);

  } // convertRowIndexToView

  ////////////////////////////////////////////////////////////

  /*
   * These method implementations just pass on the call to the
   * underlying table model.
   */

  public int getRowCount () { 
    return (model.getRowCount()); 
  }

  public int getColumnCount () { 
    return (model.getColumnCount()); 
  }

  public String getColumnName (int columnIndex) { 
    return (model.getColumnName (columnIndex)); 
  }

  public Class getColumnClass (int columnIndex) { 
    return (model.getColumnClass (columnIndex)); 
  }

  public boolean isCellEditable (int rowIndex, int columnIndex) {
    return (model.isCellEditable (rowMap[rowIndex], columnIndex));
  }   

  public Object getValueAt (int rowIndex, int columnIndex) {
    return (model.getValueAt (rowMap[rowIndex], columnIndex));
  } 

  public void setValueAt (Object aValue, int rowIndex, int columnIndex) {
    model.setValueAt (aValue, rowMap[rowIndex], columnIndex);
  }

  public void addTableModelListener (TableModelListener l) {
    model.addTableModelListener (l);
  }

  public void removeTableModelListener (TableModelListener l) {
    model.removeTableModelListener (l);
  }

  ////////////////////////////////////////////////////////////

} // SortedTableModel class

////////////////////////////////////////////////////////////////////////
