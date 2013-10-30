////////////////////////////////////////////////////////////////////////
/*
     FILE: FileTableModel.java
  PURPOSE: Models directory listing information.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/27
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
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.gui.open.DirectoryLister.Entry;

/**
 * The <code>FileTableModel</code> uses a list of {@link
 * DirectoryLister.Entry} objects to present a view of a directory and
 * its subdirectories and files.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class FileTableModel 
  extends AbstractTableModel {

  // Constants
  // ---------

  /** Column index of file name. */
  public static final int NAME_COLUMN = 0;

  /** Column index of file size. */
  public static final int SIZE_COLUMN = 1;

  /** Column index of file modified date. */
  public static final int DATE_COLUMN = 2;

  /** Format of file size strings. */
  private static final DecimalFormat SIZE_FMT = new DecimalFormat ("0.#");

  /** The local timezone. */
  private static final TimeZone ZONE = TimeZone.getDefault();

  /** The date/time format. */
  private static final String DATE_FMT = "yyyy/MM/dd HH:mm";

  /** The empty directory entry. */
  public static final Entry EMPTY = 
    new Entry ("[empty]", new Date (0), 0, false);

  // Variables
  // ---------

  /** The list of directory entries. */
  private List entryList;

  ////////////////////////////////////////////////////////////

  /** Creates a new file table model with an empty entry list. */
  public FileTableModel () { clear(); }

  ////////////////////////////////////////////////////////////

  /** Clears the entry list so that no data is contained. */
  public void clear () { 
    
    this.entryList = new ArrayList();
    fireTableDataChanged();

  } // clear

  ////////////////////////////////////////////////////////////

  /** Sets the entry list for this model. */
  public void setEntryList (List entryList) {
      
    this.entryList = entryList;
    if (entryList.size() == 0) entryList.add (EMPTY);
    fireTableDataChanged();
    
  } // setEntryList

  ////////////////////////////////////////////////////////////

  /** Gets the number of directory entries. */
  public int getRowCount () { return (entryList.size()); }

  ////////////////////////////////////////////////////////////

  /** Gets the number of directory entry columns. */
  public int getColumnCount () { return (3); }

  ////////////////////////////////////////////////////////////

  /** Gets the directory entry at the specified row. */
  public Entry getEntry (int row) {

    return ((Entry) entryList.get (row));

  } // getEntry

  ////////////////////////////////////////////////////////////

  /** Gets the directory entry value. */
  public Object getValueAt (int row, int column) {
    
    Entry entry = getEntry (row);

    switch (column) {
      
    case NAME_COLUMN: 
      if (entry.isDirectory()) return ("[DIR] " + entry.getName());
      else return (entry.getName());

    case SIZE_COLUMN: 
      long size = entry.getSize();
      if (entry == EMPTY) return ("");
      if (size == 0 && entry.isDirectory()) return ("");
      float sizeFloat = size;
      int mult = 1;
      while (sizeFloat > 1024) {
        sizeFloat /= 1024;
        mult++;
      } // while
      String sizeStr = SIZE_FMT.format (sizeFloat);
      switch (mult) {
      case 2: sizeStr += "KB"; break;
      case 3: sizeStr += "MB"; break; 
      case 4: sizeStr += "GB"; break;
      case 5: sizeStr += "TB"; break;
      } // switch
      return (sizeStr);
      
    case DATE_COLUMN: 
      Date modified = entry.getModified();
      if (modified.getTime() <= 0) return ("");
      else return (DateFormatter.formatDate (modified, DATE_FMT, ZONE));

    default: throw new IllegalArgumentException (
      "Column index " + column + " not allowed");

    } // switch

  } // getValueAt

  ////////////////////////////////////////////////////////////

  /** Gets the table column name. */
  public String getColumnName (int column) { 

    switch (column) {
    case NAME_COLUMN: return ("Name");
    case SIZE_COLUMN: return ("Size");
    case DATE_COLUMN: return ("Modified");
    default: throw new IllegalArgumentException (
      "Column index " + column + " not allowed");
    } // switch

  } // getColumnName

  ////////////////////////////////////////////////////////////

} // FileTableModel class

////////////////////////////////////////////////////////////////////////
