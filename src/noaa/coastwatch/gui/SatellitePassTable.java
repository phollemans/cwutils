////////////////////////////////////////////////////////////////////////
/*

     File: SatellitePassTable.java
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
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import noaa.coastwatch.util.SatellitePassInfo;

/**
 * The satellite pass table is a Swing table that holds satellite pass
 * information for a server.  The table includes a column-based
 * sorting mechanism activated by clicking on the column header.
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class SatellitePassTable
  extends JTable {

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new pass table with empty data.
   *
   * @see noaa.coastwatch.gui.SatellitePassTableModel#setSource
   */
  public SatellitePassTable () {

    this ("", "", "");

  } // SatellitePassTable

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new pass table based on data from the specified server.
   *
   * @param protocol the communications protocol.
   * @param host the server host.
   * @param path the query script path.
   *
   * @see SatellitePassTableModel
   */
  public SatellitePassTable (
    String protocol,
    String host,
    String path
  ) {

    // Initialize
    // ----------
    super (new SatellitePassTableModel (protocol, host, path));
    setShowGrid (false);
    setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    setIntercellSpacing (new Dimension (0,0));
    setColumnSelectionAllowed (false);

    // Add column click listener
    // -------------------------
    getTableHeader().addMouseListener (new ColumnClicked());

  } // SatellitePassTable constructor

  ////////////////////////////////////////////////////////////

  /** Responds to a column click by re-sorting the pass table. */
  private class ColumnClicked 
    extends MouseAdapter {

    public void mouseClicked (MouseEvent e) {

      // Get selected row
      // ----------------
      int row = getSelectedRow();
      SatellitePassTableModel model = (SatellitePassTableModel) getModel();
      String passID = (row == -1 ? null : model.getPass(row).getPassID());

      // Set sorting column
      // ------------------
      int column = getTableHeader().columnAtPoint (e.getPoint());
      column = convertColumnIndexToModel (column);
      model.setSortColumn (column);

      // Set selected row
      // ----------------
      if (passID != null) {
        int newRow = model.getPassIndex (passID);
        if (newRow != -1)
          setRowSelectionInterval (newRow, newRow);
      } // if

    } // mouseClicked

  } // ColumnClicked class

  ////////////////////////////////////////////////////////////

} // SatellitePassTable class

////////////////////////////////////////////////////////////////////////
