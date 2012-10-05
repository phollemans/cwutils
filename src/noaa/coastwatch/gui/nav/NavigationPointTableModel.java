////////////////////////////////////////////////////////////////////////
/*
     FILE: NavigationPointTableModel.java
  PURPOSE: Provides table data for a set of navigation points.
   AUTHOR: Peter Hollemans
     DATE: 2006/12/12
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.nav;

// Imports
// -------
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import noaa.coastwatch.util.*;

/**
 * The <code>NavigationPointTableModel</code> class provides data
 * from a list of {@link NavigationPoint} objects.  The comment
 * field of the navigation points is used to indicate the latest
 * status of navigation and should be set accordingly.
 */
public class NavigationPointTableModel
  extends AbstractTableModel {

  // Constants
  // ---------

  /** The names of each column. */
  private static final String[] COLUMN_NAMES = {
    "Latitude",
    "Longitude",
    "Row",
    "Col",
    "Status"
  };

  // Variables
  // ---------

  /** The list of navigation points. */
  private List<NavigationPoint> pointList;

  ////////////////////////////////////////////////////////////

  /** Creates a new table using an empty point list. */
  public NavigationPointTableModel () {

    this.pointList = new ArrayList<NavigationPoint>();

  } // NavigationPointTableModel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new table using the specified point list.
   *
   * @param pointList the initial point list data.
   */
  public NavigationPointTableModel (
    List<NavigationPoint> pointList
  ) {

    this.pointList = new ArrayList<NavigationPoint> (pointList);

  } // NavigationPointTableModel constructor

  ////////////////////////////////////////////////////////////

  /** Adds a point to the list. */
  public void addPoint (NavigationPoint point) { 

    pointList.add (point); 
    fireTableRowsInserted (pointList.size()-1, pointList.size()-1);

  } // addPoint

  ////////////////////////////////////////////////////////////

  /** Gets a point from the list. */
  public NavigationPoint getPoint (int row) { return (pointList.get (row)); }

  ////////////////////////////////////////////////////////////

  /** Gets the list of navigation points. */
  public List<NavigationPoint> getPointList () {

    return (new ArrayList<NavigationPoint> (pointList));

  } // getPointList

  ////////////////////////////////////////////////////////////

  /** Removes a point from the list. */
  public NavigationPoint removePoint (int row) { 

    NavigationPoint point = pointList.remove (row);
    fireTableRowsDeleted (row, row);
    return (point);
  
  } // removePoint

  ////////////////////////////////////////////////////////////

  /** Removes all points from the list. */
  public void clear () {

    int rows = pointList.size();
    if (rows > 0) {
      pointList.clear();
      fireTableRowsDeleted (0, rows-1);
    } // if
  
  } // clear

  ////////////////////////////////////////////////////////////

  /** Notifies listeners that a point has changed. */
  public void changePoint (NavigationPoint point) {

    int row = pointList.indexOf (point);
    if (row != -1)
      fireTableRowsUpdated (row, row);

  } // changePoint

  ////////////////////////////////////////////////////////////
  
  public int getRowCount() { return (pointList.size()); }

  ////////////////////////////////////////////////////////////

  public int getColumnCount() { return (5); }

  ////////////////////////////////////////////////////////////

  public String getColumnName (int column) { return (COLUMN_NAMES[column]); }

  ////////////////////////////////////////////////////////////

  public Object getValueAt (
    int row, 
    int column
  ) {

    NavigationPoint point = pointList.get (row);
    switch (column) {
    case 0: return (point.getEarthLoc().formatSingle (EarthLocation.DDDD, EarthLocation.LAT));
    case 1: return (point.getEarthLoc().formatSingle (EarthLocation.DDDD, EarthLocation.LON));    
    case 2: return ((int) point.getDataLoc().get (Grid.ROWS));
    case 3: return ((int) point.getDataLoc().get (Grid.COLS));
    case 4: return (point.getComment());
    default: throw new IllegalArgumentException ("Invalid column number");
    } // switch

  } // getValueAt

  ////////////////////////////////////////////////////////////

} // NavigationPointTableModel class

////////////////////////////////////////////////////////////////////////
