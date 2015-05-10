////////////////////////////////////////////////////////////////////////
/*
     FILE: FileTable.java
  PURPOSE: Displays directory listing information.
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
import java.awt.Container;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.open.DirectoryLister;
import noaa.coastwatch.gui.open.LocalDirectoryLister;

/**
 * The <code>FileTable</code> uses a {@link FileTableModel} object to
 * display a directory listing.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class FileTable
  extends JTable {

  // Constants
  // ---------
  
  /** The icon for directory entries. */
  private static final Icon DIR_ICON = GUIServices.getIcon ("filetable.dir");

  /** The icon for file entries. */
  private static final Icon FILE_ICON = GUIServices.getIcon ("filetable.file");

  // Variables
  // ---------

  /** The render for the size column. */
  private static SizeRenderer sizeRenderer = new SizeRenderer();

  /** The render for the name column. */
  private static NameRenderer nameRenderer = new NameRenderer();

  ////////////////////////////////////////////////////////////

  /** Creates a new empty file table. */
  public FileTable () { 

    super (new FileTableModel());

    // Setup table
    // -----------
    setShowGrid (false);
    setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    for (int i = 0; i < 3; i++) {
      TableColumn column = getColumnModel().getColumn (i);
      switch (i) {
      case FileTableModel.NAME_COLUMN: column.setPreferredWidth (240); break;
      case FileTableModel.SIZE_COLUMN: column.setPreferredWidth (70); break;
      case FileTableModel.DATE_COLUMN: column.setPreferredWidth (130); break;
      } // switch
    } // for

  } // FileTable constructor

  ////////////////////////////////////////////////////////////

  /** Formats the file size as a right-justified label. */
  static class SizeRenderer extends DefaultTableCellRenderer {

    public SizeRenderer () {
      setHorizontalAlignment (SwingConstants.RIGHT);
    } // SizeRenderer constructor

  } // SizeRenderer class

  ////////////////////////////////////////////////////////////

  /** Formats the file name as an icon and label. */
  static class NameRenderer extends DefaultTableCellRenderer {

    public void setValue(Object value) {

      // Set icon
      // --------
      String name = (String) value;
      boolean isDir = name.startsWith ("[DIR] ");
      boolean isEmpty = name.equals (FileTableModel.EMPTY.getName());
      setIcon (isDir ? DIR_ICON : !isEmpty ? FILE_ICON : null);

      // Set text
      // --------
      if (isDir) name = name.substring (6, name.length());
      setText (name);

    } // setValue

  } // NameRenderer class

  ////////////////////////////////////////////////////////////

  /** Gets a renderer for the specified cell. */
  public TableCellRenderer getCellRenderer (int row, int column) {

    switch (convertColumnIndexToModel (column)) {
    case FileTableModel.SIZE_COLUMN: return (sizeRenderer);
    case FileTableModel.NAME_COLUMN: return (nameRenderer);
    default: return super.getCellRenderer (row, column);
    } // switch

  } // getCellRenderer

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Create table
    // ------------
    DirectoryLister lister = new LocalDirectoryLister();
    lister.setDirectory (argv[0]);
    FileTable table = new FileTable();
    ((FileTableModel) table.getModel()).setEntryList (lister.getEntries());
    JScrollPane scrollpane = new JScrollPane (table);

    // Create frame
    // ------------
    final JFrame frame = new JFrame (FileTableModel.class.getName());
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

} // FileTable class

////////////////////////////////////////////////////////////////////////
