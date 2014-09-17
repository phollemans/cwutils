////////////////////////////////////////////////////////////////////////
/*
     FILE: NavigationPointSavePanel.java
  PURPOSE: Saves navigation points.
   AUTHOR: Peter Hollemans
     DATE: 2006/12/19
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.nav;

// Imports
// -------
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.*;
import java.util.List;
import noaa.coastwatch.render.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.gui.*;

/** 
 * The <code>NavigationPointSavePanel</code> class allows the user to
 * choose a file name and parameters for saving a list of navigation
 * points.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class NavigationPointSavePanel 
  extends JPanel {

  // Constants
  // ---------

  /** The supported file formats. */
  private static final String[] FORMATS = new String[] {
    "XML data (.xml)",
    "CSV data (.csv)"
  };

  /** The default extension for each format. */
  private static final String[] EXTENSIONS = new String[] {
    "xml",
    "csv",
  };

  // Variables
  // ---------

  /** The data reader for saving data. */
  private EarthDataReader reader;

  /** The combo box for file format. */
  private JComboBox formatCombo;

  /** The file chooser instance. */
  private static JFileChooser fileChooser;

  /** The save panel for current file name. */
  private FileSavePanel savePanel;

  /** The list of variable names. */
  private JList variableNameList;

  /** The table of point data. */
  private JTable pointTable;

  ////////////////////////////////////////////////////////////

  static {

    // Create file chooser
    // -------------------
    fileChooser = new JFileChooser();
    SimpleFileFilter navFilter = new SimpleFileFilter (
      new String[] {"xml", "csv"}, "Navigation data");
    fileChooser.addChoosableFileFilter (navFilter);
    fileChooser.setDialogTitle ("Select");
    fileChooser.setDialogType (JFileChooser.SAVE_DIALOG);
    fileChooser.setApproveButtonText ("OK");
    fileChooser.setFileFilter (navFilter);

  } // static

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new save panel.
   *
   * @param reader the data reader to use for saving data.  The reader
   * is also used to generate an initial file name based on the data
   * source.
   * @param variableList the list of variables to show for saving
   * data.  
   * @param pointList the list of navigation points to save.
   */
  public NavigationPointSavePanel (
    EarthDataReader reader,
    List<String> variableList,
    List<NavigationPoint> pointList
  ) {

    // Initialize
    // ----------
    this.reader = reader;
    setLayout (new BorderLayout());
    
    // Create top panel
    // ----------------
    Box topPanel = Box.createHorizontalBox();
    this.add (topPanel, BorderLayout.CENTER);

    JPanel pointPanel = new JPanel (new BorderLayout());
    pointPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Navigation Points"));
    pointTable = new NavigationPointTable (pointList);
    Dimension tableSize = new Dimension (0, pointTable.getRowHeight()*10);
    for (int i = 0; i < pointTable.getColumnCount(); i++) {
      tableSize.width += pointTable.getColumnModel().getColumn (i
        ).getPreferredWidth();
    } // for
    pointTable.setPreferredScrollableViewportSize (tableSize);
    pointPanel.add (new JScrollPane (pointTable), BorderLayout.CENTER);
    topPanel.add (pointPanel);

    JPanel varPanel = new JPanel (new BorderLayout());
    varPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Variables"));
    variableNameList = new JList (variableList.toArray());
    variableNameList.setPrototypeCellValue ("WWWWWWWWWWWW");
    varPanel.add (new JScrollPane (variableNameList), BorderLayout.CENTER);
    topPanel.add (varPanel);

    // Create bottom panel
    // -------------------
    JPanel bottomPanel = new JPanel (new GridBagLayout());
    this.add (bottomPanel, BorderLayout.SOUTH);

    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 5);
    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    bottomPanel.add (new JLabel ("Format:"), gc);

    formatCombo = new JComboBox (FORMATS);
    formatCombo.addItemListener (new ItemListener () {
      public void itemStateChanged (ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
          File saveFile = savePanel.getSaveFile();
          String name = saveFile.getName().replaceFirst ("\\.[^.]*$", 
            "." + EXTENSIONS[formatCombo.getSelectedIndex()]);
          savePanel.setSaveFile (new File (saveFile.getParentFile(), name));
        } // if
      } // itemSelected
    });
    GUIServices.setConstraints (gc, 1, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    bottomPanel.add (formatCombo, gc);

    savePanel = new FileSavePanel (fileChooser);
    GUIServices.setConstraints (gc, 0, 1, 3, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    bottomPanel.add (savePanel, gc);

    GUIServices.setConstraints (gc, 0, 2, 3, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    bottomPanel.add (Box.createVerticalStrut (10), gc);

    // Set initial output file
    // -----------------------
    File sourceFile = new File (reader.getSource());
    File parent = sourceFile.getParentFile();
    if (parent == null || !parent.exists())
      parent = GUIServices.getFileChooser().getCurrentDirectory();
    savePanel.setSaveFile (new File (parent, 
      sourceFile.getName().replaceFirst ("\\.[^.]*$", ".xml")));

    // Select all navigation points
    // ----------------------------
    pointTable.setRowSelectionInterval (0, pointTable.getRowCount()-1);

  } // NavigationPointSavePanel constructor

  ////////////////////////////////////////////////////////////

  /** Performs a save using the current panel settings. */
  private void save () throws IOException {

    // Get list of points
    // ------------------
    List<NavigationPoint> savePointList = new ArrayList<NavigationPoint>();
    int[] rows = pointTable.getSelectedRows();
    NavigationPointTableModel model = 
      (NavigationPointTableModel) pointTable.getModel();
    for (int i = 0; i < rows.length; i++)
      savePointList.add (model.getPoint (i));

    // Get list of variables
    // ---------------------
    List<String> saveVarList = new ArrayList<String>();
    int[] indices = variableNameList.getSelectedIndices();
    for (int i = 0; i < indices.length; i++) {
      saveVarList.add ((String) variableNameList.getModel().getElementAt (
        indices[i]));
    } // for

    // Create point writer
    // -------------------
    String format = EXTENSIONS[formatCombo.getSelectedIndex()];
    NavigationPointWriter writer;
    if (format.equals ("xml"))
      writer = new XMLPointWriter (reader, saveVarList, savePointList);
    else if (format.equals ("csv"))
      writer = new CSVPointWriter (reader, saveVarList, savePointList);
    else
      throw new IllegalStateException();

    // Write the data
    // --------------
    writer.write (new FileOutputStream (savePanel.getSaveFile()));

  } // save

  ////////////////////////////////////////////////////////////

  /**
   * Shows the save panel in a dialog window.
   *
   * @param parent the parent component to use for showing dialogs.
   */
  public void showDialog (
    final Component parent
  ) { 

    // Create dialog reference
    // -----------------------
    final JDialog[] dialog = new JDialog[1];

    // Create OK action
    // ----------------
    Action okAction = GUIServices.createAction ("OK", new Runnable () {
        public void run () { 
          try { save(); }
          catch (Exception e) {
            JOptionPane.showMessageDialog (parent, 
              "An error occurred writing the file:\n" + 
              e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
          } // catch
          dialog[0].dispose();
        } // run
      });

    // Create cancel action
    // --------------------
    Action cancelAction = GUIServices.createAction ("Cancel", null);

    // Create chooser dialog
    // ---------------------
    Component[] controls = new Component[] {
      GUIServices.getHelpButton (NavigationPointSavePanel.class),
      Box.createHorizontalGlue()
    };
    dialog[0] = GUIServices.createDialog (parent, "Save", 
      true, this, controls, new Action[] {okAction, cancelAction}, 
      new boolean[] {false, true}, true);

    // Show chooser
    // ------------
    dialog[0].setVisible (true);

  } // showDialog

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Get reader
    // ----------
    EarthDataReader reader = EarthDataReaderFactory.create (argv[0]); 
    List<String> variableList = reader.getAllGrids();

    // Create panel
    // ------------
    List<NavigationPoint> pointList = NavigationPointWriter.getTestPoints (
      reader.getInfo().getTransform(), 20);
    final NavigationPointSavePanel panel = new NavigationPointSavePanel (
      reader, variableList, pointList);
    
    // Show dialog
    // -----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          panel.showDialog (null);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // NavigationPointSavePanel

////////////////////////////////////////////////////////////////////////
