////////////////////////////////////////////////////////////////////////
/*
     FILE: SortedTable.java
  PURPOSE: Allows a JTable to be sortable.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;

/**
 * The <code>SortedTable</code> class is a normal
 * <code>javax.swing.JTable</code> that can sort its rows when
 * the user clicks on a column in the table header.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class SortedTable
  extends JTable {

  // Constants
  // ---------

  /** The size of the sorting arrow icon. */
  private static final int ICON_SIZE = 4;

  // Variables
  // ---------

  /** The sorted table model. */
  private SortedTableModel sortedModel;

  /** The sorting icon. */
  private Icon sortIcon;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new table from the specified model.
   *
   * @param model the model to use for table data.
   */
  public SortedTable (
    TableModel model
  ) {

    // Initialize
    // ----------
    sortedModel = new SortedTableModel (model);
    sortIcon = new SortIcon();
    setModel (sortedModel);

    // Setup special header behaviour
    // ------------------------------
    JTableHeader header = getTableHeader();
    TableCellRenderer oldRenderer = header.getDefaultRenderer();
    TableCellRenderer newRenderer = new SortedHeaderRenderer (oldRenderer);
    header.setDefaultRenderer (newRenderer);
    header.addMouseListener (new ColumnListener());

  } // SortedTable constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Converts a row index in the sorted model to a row index in the
   * underlying model.
   *
   * @param row the row index to convert.
   *
   * @return the row index in the underlying table model.
   */
  public int convertRowIndexToModel (int row) { 

    return (sortedModel.convertRowIndexToModel (row));

  } // convertRowIndexToModel

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

    return (sortedModel.convertRowIndexToView (row));

  } // convertRowIndexToView

  ////////////////////////////////////////////////////////////

  /** Responds to a column click by re-sorting the table. */
  private class ColumnListener extends MouseAdapter {
    public void mouseClicked (MouseEvent e) {

      // Get new sorting column
      // ----------------------
      JTableHeader header = getTableHeader();
      int columnClicked = header.columnAtPoint (e.getPoint());
      columnClicked = convertColumnIndexToModel (columnClicked);

      // Get new sorting order
      // ---------------------
      int activeColumn = sortedModel.getSortColumn();
      boolean isAscending = sortedModel.getIsAscending();
      if (columnClicked == activeColumn)
        isAscending = !isAscending;
      
      // Set new sorting order
      // ---------------------
      sortedModel.setSort (columnClicked, isAscending);
      header.repaint();
      tableChanged (new TableModelEvent (sortedModel));

    } // mouseClicked
  } // ColumnListener class

  ////////////////////////////////////////////////////////////

  /** Renders the table header with a sorting arrow. */
  private class SortedHeaderRenderer extends DefaultTableCellRenderer {

    // Variables
    // ---------

    /** The current default renderer. */
    private TableCellRenderer oldRenderer;

    ////////////////////////////////////////////////////////
    
    /** Creates a new renderer based on the old one. */
    public SortedHeaderRenderer (TableCellRenderer renderer) {
      oldRenderer = renderer;
    } // SortedHeaderRenderer

    ////////////////////////////////////////////////////////

    /** Gets the table header renderer component. */
    public Component getTableCellRendererComponent (
      JTable table,
      Object value,
      boolean isSelected,
      boolean hasFocus,
      int row,
      int column
    ) {

      // Setup label properties
      // ----------------------
      JLabel label = (JLabel) oldRenderer.getTableCellRendererComponent (
        table, value, isSelected, hasFocus, row, column);
      label.setHorizontalAlignment (SwingConstants.LEADING);
      label.setHorizontalTextPosition (SwingConstants.LEADING);
      label.setText (" " + label.getText());
      label.setIconTextGap (10);

      // Set the label icon
      // ------------------
      if (convertColumnIndexToModel (column) == sortedModel.getSortColumn())
        label.setIcon (sortIcon);
      else
        label.setIcon (null);

      return (label);
      
    } // getTableCellRendererComponent

    ////////////////////////////////////////////////////////

  } // SortedHeaderRenderer class

  ////////////////////////////////////////////////////////////

  /** Draws an arrow icon for the table header. */
  private class SortIcon implements Icon {

    // Variables
    // ---------

    // The arrow button to use for rendering the sorting arrow. */
    private BasicArrowButton arrowButton = new BasicArrowButton(0);

    ////////////////////////////////////////////////////////

    /** Paints the sorting icon. */
    public void paintIcon (Component c, Graphics g, int x, int y) {
      int direction = sortedModel.getIsAscending() ? SwingConstants.NORTH :
        SwingConstants.SOUTH;
      arrowButton.paintTriangle (g, x, y, ICON_SIZE, direction, true);
    } // paintIcon

    ////////////////////////////////////////////////////////

    /** Gets the icon width in pixels. */
    public int getIconWidth () { return (ICON_SIZE); }

    ////////////////////////////////////////////////////////

    /** Gets the icon height in pixels. */
    public int getIconHeight() { return (ICON_SIZE/2); }

    ////////////////////////////////////////////////////////

  } // SortIcon

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    TableModel dataModel = new AbstractTableModel () {
        public int getColumnCount () { return (10); }
        public int getRowCount () { return (10);}
        public Object getValueAt (int row, int col) { 
          return (new Integer (row*col));
        } // getValueAt
      };
    JTable table = new SortedTable (dataModel);
    JScrollPane scrollpane = new JScrollPane(table);

    JFrame frame = new JFrame();
    frame.getContentPane().add (scrollpane, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible (true);

  } // main

  ////////////////////////////////////////////////////////////

} // SortedTable class

////////////////////////////////////////////////////////////////////////
