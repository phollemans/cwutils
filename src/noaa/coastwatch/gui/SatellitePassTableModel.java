////////////////////////////////////////////////////////////////////////
/*

     File: SatellitePassTableModel.java
   Author: Peter Hollemans
     Date: 2003/01/13

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.net.ServerQuery;
import noaa.coastwatch.util.SatellitePassInfo;

/**
 * The satellite pass table model contains satellite pass data as
 * a Swing table model.
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class SatellitePassTableModel
  extends AbstractTableModel {

  // Constants
  // ---------
  /** The table columns indices. */
  private static final int SATELLITE = 0;
  private static final int SENSOR = 1;
  private static final int DATE = 2;
  private static final int TIME = 3;
  private static final int SCENE = 4;
  private static final int ORBIT = 5;
  private static final int LINES = 6;
  private static final int GROUND_STATION = 7;

  /** The time and date format strings. */
  private static final String DATE_FMT = "yyyy/MM/dd";
  private static final String TIME_FMT = "HH:mm";

  /** The column header text. */
  private static final String[] PASS_ITEMS = new String[] {
    "Satellite",
    "Sensor",
    "Date", 
    "Time",
    "Scene", 
    "Orbit", 
    "Lines", 
    "Station"
  };

  // Variables
  // ---------
  /** The key/value map of pass IDs and pass info objects. */
  private TreeMap passMap;

  /** The ordered set of pass info objects. */
  private TreeSet passSet;

  /** The protocol, host and path to use for updating the pass table model. */
  private String protocol, host, path;

  /** The current sorting column index. */
  private int sortColumn;

  /** The current update worker thread. */
  private Thread worker;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new pass table model with empty data.  The
   * <code>setSource()</code> method should be called prior to
   * performing any data updates.
   */
  public SatellitePassTableModel () {

    this ("", "", "");

  } // SatellitePassTableModel

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new pass table model based on data from the specified
   * server.
   *
   * @param protocol the communication protocol.
   * @param host the server host.
   * @param path the query script path.
   *
   * @see ServerQuery
   */
  public SatellitePassTableModel (
    String protocol,
    String host,
    String path
  ) {

    // Initialize
    // ----------
    passMap = new TreeMap();
    passSet = new TreeSet();
    this.protocol = protocol;
    this.host = host;
    this.path = path;
    sortColumn = DATE;

  } // SatellitePassTableModel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Sets the model to read data from a new data source.  The current
   * table model entries are cleared.
   *
   * @param protocol the communication protocol.
   * @param host the server host.
   * @param path the query script path.
   */
  public synchronized void setSource (
    String protocol,
    String host,
    String path
  ) {

    passMap = new TreeMap();
    passSet = new TreeSet();
    this.protocol = protocol;
    this.host = host;
    this.path = path;
    fireTableChanged (new TableModelEvent (this));

  } // setSource

  ////////////////////////////////////////////////////////////

  public int getRowCount () { return (passMap.size()); }

  ////////////////////////////////////////////////////////////

  public int getColumnCount() { return (PASS_ITEMS.length); }

  ////////////////////////////////////////////////////////////

  public String getColumnName (int index) { return (PASS_ITEMS[index]); }

  ////////////////////////////////////////////////////////////

  /** Gets the pass at the specified index. */
  public SatellitePassInfo getPass (
    int index
  ) { 

    return ((SatellitePassInfo) passSet.toArray()[index]); 

  } // getPass

  ////////////////////////////////////////////////////////////

  /**
   * Gets the pass index from the specified pass identifier.
   * 
   * @param passID the pass identifier.
   *
   * @return the pass index, or -1 if not found. 
   */
  public int getPassIndex (
    String passID
  ) {

    // Check for pass ID
    // -----------------
    if (!passMap.containsKey (passID))
      return (-1);

    // Find index
    // ----------
    List v = new ArrayList (passSet);
    for (int i = 0; i < v.size(); i++) 
      if (((OrderedPass) v.get(i)).getPassID().equals (passID)) return (i);
    return (-1);

  } // getPassIndex

  ////////////////////////////////////////////////////////////

  public Object getValueAt (
    int row,
    int column
  ) {

    // Get pass entry
    // --------------
    SatellitePassInfo pass = getPass (row);
    String entry;
    switch (column) {
    case SATELLITE: entry = pass.getSatellite().toUpperCase(); break;
    case SENSOR: entry = pass.getSensor().toUpperCase(); break;
    case SCENE: entry = pass.getSceneTime().toUpperCase(); break;
    case ORBIT: entry = pass.getOrbitType().toUpperCase(); break;
    case LINES: entry = Integer.toString (pass.getDimensions()[0]); break;
    case GROUND_STATION: entry = pass.getGroundStation(); break;
    case DATE: entry = pass.formatDate (DATE_FMT); break;
    case TIME: entry = pass.formatDate (TIME_FMT); break;
    default: entry = null; break;
    } // switch

    // Return entry
    // ------------
    if (entry == null) return ("");
    else return (entry);

  } // getValueAt

  ////////////////////////////////////////////////////////////

  /**
   * Loads a list of passes from the server.
   *
   * @param passIDs the list of pass IDs to load, or null to load all
   * pass data.
   *
   * @return a list of <code>OrderedPass</code> objects.
   */
  private List loadPasses (
    List passIDs
  ) throws IOException {

    // Create query keys
    // -----------------
    HashMap queryKeys = new HashMap();
    queryKeys.put ("query", "datasetDetails");
    queryKeys.put ("projection_type", "swath");
    queryKeys.put ("details", "file_name,satellite,sensor,date,time,scene_time,s.orbit_type,s.dim_rows,s.dim_cols,g.description,s.gcp_lats,s.gcp_lons,preview_url");

    // Add file name filter key
    // ------------------------
    if (passIDs != null && passIDs.size() != 0) {
      String matchString = "(";
      for (int i = 0; i < passIDs.size(); i++) {
        matchString += (String) passIDs.get (i);
        if (i != passIDs.size()-1) matchString += "|";
      } // for
      matchString += ")";
      queryKeys.put ("file_name", matchString);
    } // if

    // Perform server query
    // --------------------
    ServerQuery query = new ServerQuery (protocol, host, path, queryKeys);

    // Create pass set
    // ---------------
    List passes = new ArrayList();
    for (int i = 0; i < query.getResults(); i++) {
      OrderedPass op = new OrderedPass (query.getValue (i, "file_name"), 
        query, i);
      passes.add (op);      
    } // for
 
    return (passes);

  } // loadPasses

  ////////////////////////////////////////////////////////////

  /** Adds a new pass to the table. */
  private void addPass (
    OrderedPass op
  ) {
 
    // Add pass
    // --------
    passMap.put (op.getPassID(), op);
    passSet.add (op);

  } // addPass

  ////////////////////////////////////////////////////////////

  /** Removes an old pass from the table. */
  private void removePass (
    String passID
  ) {

    // Find and remove pass
    // --------------------
    if (passMap.containsKey (passID)) {
      OrderedPass op = (OrderedPass) passMap.get (passID);
      passMap.remove (passID);
      passSet.remove (op);
    } // if

  } // removePass

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the pass table by contacting the server for a pass list.
   * This method always returns immediately and the server query is
   * performed asynchronously.  If there is currently an update
   * running, no operation is performed.
   */
  public synchronized void update () {

    // Check for update worker
    // -----------------------
    if (worker != null && worker.isAlive()) return;

    // Create worker thread
    // --------------------
    worker = new Thread() {
      public void run() {

        // Create initial pass list
        // ------------------------
        List toAdd = null, toRemove = null;
        if (passMap.size() == 0) {
          try { toAdd = loadPasses (null); }
          catch (IOException e) { }
        } // if

        // Create incremental pass list
        // ----------------------------
        else {

          // Get full pass list
          // ------------------
          ServerQuery query;
          HashMap queryKeys = new HashMap();
          queryKeys.put ("query", "datasetDetails");
          queryKeys.put ("projection_type", "swath");
          queryKeys.put ("details", "file_name");
          try { query = new ServerQuery (protocol, host, path, queryKeys); }
          catch (IOException e) { return; }

          // Create hash set of pass IDs
          // ---------------------------
          HashSet passIDs = new HashSet();
          for (int i = 0; i < query.getResults(); i++) {
            String passID = query.getValue (i, "file_name");
            passIDs.add (passID);
          } // for       

          // Create list of new passes
          // -------------------------
          List newPassIDs = new ArrayList();
          Iterator iter = passIDs.iterator();
          while (iter.hasNext()) {
            String passID = (String) iter.next();
            if (!passMap.containsKey (passID)) newPassIDs.add (passID);
          } // for

          // Load new passes
          // ---------------
          if (newPassIDs.size() > 0) {
            try { toAdd = loadPasses (newPassIDs); }
            catch (IOException e) { }
          } // if

          // Create list of old passes
          // -------------------------
          List oldPassIDs = new ArrayList();
          String[] keys = (String[]) passMap.keySet().toArray (
            new String[] {});
          for (int i = 0; i < keys.length; i++) {
            String passID = keys[i];
            if (!passIDs.contains (passID)) oldPassIDs.add (passID);
          } // for
          if (oldPassIDs.size() > 0)
            toRemove = oldPassIDs;
          
        } // else

        // Update pass table
        // -----------------
        if (toAdd != null || toRemove != null) {
          final List addList = toAdd;
          final List removeList = toRemove;
          GUIServices.invokeAndWait (new Runnable() {
              public void run() { 

                // Add new passes
                // --------------
                if (addList != null) {
                  for (int i = 0; i < addList.size(); i++)
                    addPass ((OrderedPass) addList.get (i));
                } // if
                
                // Remove old passes
                // -----------------
                if (removeList != null) {
                  for (int i = 0; i < removeList.size(); i++)
                    removePass ((String) removeList.get (i));
                } // if
                
                // Signal table change
                // -------------------
                fireTableChanged (new TableModelEvent (
                  SatellitePassTableModel.this));

              } // run
            });
        } // if

      } // run
    };
    worker.start();

  } // update

  ////////////////////////////////////////////////////////////

  /**
   * Sets a new column for table sorting.
   *
   * @param column the new column to sort on.
   */
  public void setSortColumn (
    int column
  ) {

    // Set column
    // ----------
    if (sortColumn == column) return;
    sortColumn = column;
 
    // Recreate pass set
    // -----------------
    TreeSet newPassSet = new TreeSet();
    Iterator iter = passSet.iterator();
    while (iter.hasNext())
      newPassSet.add (iter.next());
    passSet = newPassSet;
    fireTableChanged (new TableModelEvent (this));

  } // setSortColumn

  ////////////////////////////////////////////////////////////

  /** 
   * An ordered pass allows the table model to store pass data with
   * sorting based on the current sorting column.
   */
  private class OrderedPass
    extends SatellitePassInfo
    implements Comparable {

    /** The UTC date string. */
    String dateString;

    /** The UTC time string. */
    String timeString;

    ////////////////////////////////////////////////////////

    /** Creates a new ordered pass based on pass ID and query. */
    public OrderedPass (
      String passID,
      ServerQuery query,
      int result
    ) { 

      super (passID, query, result);
      dateString = formatDate (DATE_FMT);
      timeString = formatDate (TIME_FMT);

    } // OrderedPass constructor

    ////////////////////////////////////////////////////////

    /** Compares this pass based on pass identifier. */
    public int compareID (
      OrderedPass pass
    ) {

      return (getPassID().compareTo (pass.getPassID()));

    } // compareID

    ////////////////////////////////////////////////////////

    /** Compares this pass based on column sorting. */
    public int compareTo (
      Object o
    ) {

      // Compare using sort column
      // -------------------------
      OrderedPass pass = (OrderedPass) o;
      int comp;
      switch (sortColumn) {
      case SATELLITE: comp = getSatellite().compareTo (pass.getSatellite());
        break;
      case SENSOR: comp = getSensor().compareTo (pass.getSensor());
        break;
      case DATE:
        comp = dateString.compareTo (pass.dateString);
        break;
      case TIME:
        comp = timeString.compareTo (pass.timeString);
        break;
      case SCENE:
        comp = getSceneTime().compareTo (pass.getSceneTime());
        break;
      case ORBIT:
        comp = getOrbitType().compareTo (pass.getOrbitType());
        break;
      case LINES:
        comp = getDimensions()[0] - pass.getDimensions()[0];
        break;
      case GROUND_STATION:
        comp = getGroundStation().compareTo (pass.getGroundStation());
        break;
      default: 
        comp = 0; 
        break;

      } // switch

      // Check if passes are equal
      // -------------------------
      if (comp == 0) return (compareID (pass));
      else return (comp);

    } // compareTo

    ////////////////////////////////////////////////////////

  } // OrderedPass class

  ////////////////////////////////////////////////////////////

} // SatellitePassTableModel class

////////////////////////////////////////////////////////////////////////
