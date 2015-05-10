////////////////////////////////////////////////////////////////////////
/*
     FILE: NavigationPointTable.java
  PURPOSE: Displays navigation point data in a table form.
   AUTHOR: Peter Hollemans
     DATE: 2006/12/19
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
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * The <code>NavigationPointTable</code> class is a table that
 * displays navigation point data.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class NavigationPointTable
  extends JTable {

  // Constants
  // ---------

  /** The point comment for manual navigation. */
  public static final String MANUAL_COMMENT = "Manual";

  /** The point comment for automatic navigation successful. */
  public static final String AUTO_OK_COMMENT = "Auto OK";

  /** The point comment for automatic navigation failed. */
  public static final String AUTO_FAIL_COMMENT = "Auto failed";

  /** The label color for automatic navigation OK. */
  private static final Color AUTO_OK_COLOR = new Color (0, 128, 0);

  /** The label color for automatic navigation failed. */
  private static final Color AUTO_FAIL_COLOR = new Color (128, 0, 0);

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a point table.
   * 
   * @param pointList the list of points to display initially, or null
   * for none.
   */
  public NavigationPointTable (
    List<NavigationPoint> pointList
  ) {

    // Create data model
    // -----------------
    TableModel dataModel = (pointList == null ? 
      new NavigationPointTableModel() : 
      new NavigationPointTableModel (pointList)
    );
    setModel (dataModel);

    // Setup properties
    // ----------------
    this.setShowGrid (false);
    this.setSelectionMode (ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    this.setIntercellSpacing (new Dimension (0, 0));
    this.setColumnSelectionAllowed (false);
    this.getColumnModel().getColumn (4).setCellRenderer (
      new StatusColumnRenderer());

    // Set column sizes
    // ----------------
    int largeColSize = 
      (int) new JLabel ("000.0000WWW").getPreferredSize().getWidth();
    int smallColSize = 
      (int) new JLabel ("00000W").getPreferredSize().getWidth();
    for (int i = 0; i < 5; i++) {
      TableColumn col = this.getColumnModel().getColumn (i);
      if (i == 2 || i == 3) {
        col.setPreferredWidth (smallColSize);
      } // if
      else {
        col.setPreferredWidth (largeColSize);
      } // else
    } // for

  } // NavigationPointTable constructor

  //////////////////////////////////////////////////////////////////////

  /** Renders table cell data using colors. */
  private class StatusColumnRenderer extends DefaultTableCellRenderer {
    public void setValue (Object value) {

      setText ((String) value);
      if (value.equals (AUTO_OK_COMMENT)) 
        setForeground (AUTO_OK_COLOR);
      else if (value.equals (AUTO_FAIL_COMMENT)) 
        setForeground (AUTO_FAIL_COLOR);
      else 
        setForeground (NavigationPointTable.this.getForeground());

    } // setValue
  } // StatusColumnRenderer class

  ////////////////////////////////////////////////////////////

} // NavigationPointTable class

////////////////////////////////////////////////////////////////////////
