////////////////////////////////////////////////////////////////////////
/*
     FILE: CatalogEntryTableModel.java
  PURPOSE: Holds catalog query entries.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.browse;

// Imports
// -------
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import noaa.coastwatch.net.CatalogQueryAgent.Entry;
import noaa.coastwatch.util.*;

/**
 * The <code>CatalogEntryTableModel</code> holds a list of {@link
 * noaa.coastwatch.net.CatalogQueryAgent.Entry} objects as Swing
 * table data.
 */
public class CatalogEntryTableModel
  extends AbstractTableModel {

  // Constants
  // ---------

  /** The total number of data columns. */
  private static final int COLUMNS = 5;

  /** The table columns indices. */
  private static final int DATE = 0;
  private static final int TIME = 1;
  private static final int SOURCE = 2;
  private static final int COVER = 3;
  private static final int SCENE = 4;

  /** The time and date format strings. */
  private static final String DATE_FMT = "yyyy/MM/dd";
  private static final String TIME_FMT = "HH:mm";

  // Variables
  // ---------

  /** The list of catalog entry. */
  private List entryList;

  ////////////////////////////////////////////////////////////

  /** Creates a new model with empty entry list. */
  public CatalogEntryTableModel () { 

    entryList = new ArrayList();

  } // CatalogEntryTableModel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the catalog entry list.
   *
   * @param entryList the new entry list.
   */
  public void setEntryList (
    List entryList
  ) {

    this.entryList = entryList;
    fireTableChanged (new TableModelEvent (this));

  } // setEntryList

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an entry from the current list.
   * 
   * @param index the index of the entry to get.
   *
   * @return the catalog entry.
   */
  public Entry getEntry (
    int index
  ) {

    return ((Entry) entryList.get (index));

  } // getEntry

  ////////////////////////////////////////////////////////////

  /** Gets the number of rows in the table. */
  public int getRowCount () { return (entryList.size()); }

  ////////////////////////////////////////////////////////////

  /** Gets the number of columns in the table. */
  public int getColumnCount() { return (COLUMNS); }

  ////////////////////////////////////////////////////////////

  /** Gets the column name at the specified index. */
  public String getColumnName (int index) { 

    switch (index) {
    case DATE: return ("Date"); 
    case TIME: return ("Time");
    case SOURCE: return ("Source"); 
    case COVER: return ("Coverage");
    case SCENE: return ("Scene"); 
    default: throw new IllegalArgumentException ("Invalid column: " + index);
    } // switch

  } // getColumnName

  ////////////////////////////////////////////////////////////

  /** Gets the table data value at the specified indices. */
  public Object getValueAt (
    int row,
    int column
  ) {

    Entry entry = (Entry) entryList.get (row);
    switch (column) {
    case DATE: return (DateFormatter.formatDate (entry.startDate, DATE_FMT));
    case TIME: return (DateFormatter.formatDate (entry.startDate, TIME_FMT));
    case SOURCE: return (entry.dataSource);
    case COVER: return (new Integer (entry.coverage));
    case SCENE: return (entry.sceneTime);
    default: throw new IllegalArgumentException ("Invalid column: " + column);
    } // switch

  } // getValueAt

  ////////////////////////////////////////////////////////////

} // CatalogEntryTableModel class

////////////////////////////////////////////////////////////////////////
