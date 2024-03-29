////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataExporter.java
   Author: Peter Hollemans
     Date: 2004/05/01

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import noaa.coastwatch.gui.FileSavePanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.SimpleFileFilter;
import noaa.coastwatch.gui.save.DataSavePanel;
import noaa.coastwatch.gui.save.ImageSavePanel;
import noaa.coastwatch.gui.save.SavePanel;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.ColorComposite;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.LatLonOverlay;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;

/** 
 * The <code>EarthDataExporter</code> class allows the user to
 * choose a file name and parameters for saving earth locatable data.
 * The data may be saved in the form of a rendered image in a PNG,
 * GIF, JPEG, GeoTIFF, of PDF file, or as exported data in a CoastWatch
 * HDF, binary raster, text, or ArcGIS binary grid file.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class EarthDataExporter 
  extends JPanel {

  // Constants
  // ---------


// TODO: Where would we put the GeoTIFF 32-bit float data saving format?


  /** The supported file formats. */
  private static final String[] FORMATS = new String[] {
    "PNG image (.png)", 
    "GIF image (.gif)", 
    "JPEG image (.jpg)", 
    "GeoTIFF image (.tif)", 
    "PDF document (.pdf)", 
    "CoastWatch HDF (.hdf)",
    "NetCDF-3 (.nc)",
    "NetCDF-4 (.nc4)",
    "Binary raster (.raw)", 
    "Text file (.txt)", 
    "ArcGIS binary grid (.flt)"
  };

  /** The default extension for each format. */
  private static final String[] EXTENSIONS = new String[] {
    "png",
    "gif",
    "jpg",
    "tif",
    "pdf",
    "hdf",
    "nc",
    "nc4",
    "raw",
    "txt",
    "flt"
  };

  // Variables
  // ---------

  /** The earth data view for rendered images. */
  private EarthDataView view;

  /** The earth data information to use for legends. */
  private EarthDataInfo info;

  /** The data reader for exported data. */
  private EarthDataReader reader;

  /** The list of variable names for export. */
  private List variableList;

  /** The combo box for file format. */
  private JComboBox formatCombo;

  /** The save dialog created by the showDialog() method. */
  private JDialog saveDialog;

  /** The save options panel. */
  private SavePanel optionPanel;

  /** The option dialog. */
  private JDialog optionDialog;

  /** The file chooser instance. */
  private static JFileChooser fileChooser;

  /** The save panel for current file name. */
  private FileSavePanel savePanel;

  ////////////////////////////////////////////////////////////

  static {

    // Create file chooser
    // -------------------
    fileChooser = new JFileChooser();
    SimpleFileFilter imageFilter = new SimpleFileFilter (
      new String[] {"png", "gif", "jpg", "tif", "pdf"}, "Image files");
    fileChooser.addChoosableFileFilter (imageFilter);
    SimpleFileFilter dataFilter = new SimpleFileFilter (
      new String[] {"hdf", "nc", "nc4", "raw", "txt", "flt"}, "Data files");
    fileChooser.addChoosableFileFilter (dataFilter);
    fileChooser.setDialogTitle ("Select");
    fileChooser.setDialogType (JFileChooser.SAVE_DIALOG);
    fileChooser.setApproveButtonText ("OK");
    fileChooser.setFileFilter (imageFilter);

  } // static

  ////////////////////////////////////////////////////////////

  /** Gets a new option panel for the current format. */
  private SavePanel getOptionPanel () {

    // Get current format
    // ------------------
    String format = EXTENSIONS[formatCombo.getSelectedIndex()];

    // Create image option panel
    // -------------------------
    SavePanel panel;
    if (format.equals ("png") || format.equals ("gif") ||
        format.equals ("jpg") || format.equals ("tif") || 
        format.equals ("pdf")) {
      panel = ImageSavePanel.create (view, info, format);
    } // if

    // Create data option panel
    // ------------------------
    else {
      panel = DataSavePanel.create (reader, variableList, view, format);
      List nameList = new ArrayList();
      if (view instanceof ColorEnhancement) 
        nameList.add (((ColorEnhancement) view).getGrid().getName());
      else if (view instanceof ColorComposite) {
        Grid[] grids = ((ColorComposite) view).getGrids();
        for (int i = 0; i < grids.length; i++) 
          nameList.add (grids[i].getName());
      } // else if
      else {
        throw new IllegalArgumentException ("Unsupported view class: " + 
          view.getClass().getName());
      } // else
      ((DataSavePanel) panel).setVariables (nameList);
    } // else

    return (panel);

  } // getOptionPanel

  ////////////////////////////////////////////////////////////

  /** Shows a set of format-specific options for saving. */
  private void showOptions () {

    // Create new option panel
    // -----------------------
    if (optionPanel == null) optionPanel = getOptionPanel();

    // Create option dialog
    // --------------------
    Action okAction = GUIServices.createAction ("OK", new Runnable() {
        public void run () {

          // Check for error
          // ---------------
          try { optionPanel.check(); }
          catch (Exception e) {
            JOptionPane.showMessageDialog (optionDialog,
              "An error has been detected in the options:\n" +
              e.toString() + "\n" + 
              "Please correct the problem and try again.", 
              "Error", JOptionPane.ERROR_MESSAGE);
            return;
          } // catch

          // Dispose of dialog
          // -----------------
          optionDialog.dispose();

        } // run
      });
    
    optionDialog = GUIServices.createDialog (
      saveDialog, "Options", true, optionPanel,
      null, new Action[] {okAction},
      new boolean[] {false}, true);

    // Show option dialog
    // ------------------
    optionDialog.setVisible (true);

  } // showOptions

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new exporter panel.
   *
   * @param view the earth data view to save.  The view will be used
   * to render an image if the user selects an image type save, or
   * will be used for its bounding rectangle if the user chooses to
   * export data.
   * @param info the earth data information to use for the
   * legends.
   * @param reader the data reader to use for exporting data.  The
   * reader is also used to generate an initial file name based on the
   * data source.
   * @param variableList the list of variables to show for exporting
   * data.
   */
  public EarthDataExporter (
    EarthDataView view,
    EarthDataInfo info,
    EarthDataReader reader,
    List variableList
  ) {

    // Initialize
    // ----------
    this.view = view;
    this.info = info;
    this.reader = reader;
    this.variableList = variableList;

    // Create panel
    // ------------
    setLayout (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 5);

    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    this.add (new JLabel ("Format:"), gc);

    formatCombo = new JComboBox (FORMATS);
    formatCombo.addItemListener (new ItemListener () {
      public void itemStateChanged (ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
          File saveFile = savePanel.getSaveFile();
          String name = saveFile.getName().replaceFirst ("\\.[^.]*$", 
            "." + EXTENSIONS[formatCombo.getSelectedIndex()]);
          savePanel.setSaveFile (new File (saveFile.getParentFile(), name));
          optionPanel = null;
        } // if
      } // itemSelected
    });
    GUIServices.setConstraints (gc, 1, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    this.add (formatCombo, gc);

    JButton optionButton = GUIServices.getTextButton ("Options...");
    optionButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          showOptions();
        } // actionPerformed
      });
    GUIServices.setConstraints (gc, 2, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    this.add (optionButton, gc);

    savePanel = new FileSavePanel (fileChooser);
    GUIServices.setConstraints (gc, 0, 1, 3, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    this.add (savePanel, gc);

    GUIServices.setConstraints (gc, 0, 2, 3, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    this.add (Box.createVerticalStrut (10), gc);

    // Set initial output file
    // -----------------------
    File sourceFile = new File (reader.getSource());
    File parent = sourceFile.getParentFile();
    if (parent == null || !parent.exists())
      parent = GUIServices.getFileChooser().getCurrentDirectory();
    savePanel.setSaveFile (new File (parent, 
      sourceFile.getName().replaceFirst ("\\.[^.]*$", ".png")));

  } // EarthDataExporter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Shows the exporter in a dialog window.
   *
   * @param parent the parent component to use for showing dialogs.
   */
  public void showDialog (
    Component parent
  ) { 

    // Create OK action
    // ----------------
    final Frame frame = JOptionPane.getFrameForComponent (parent);
    Action okAction = GUIServices.createAction ("OK", new Runnable () {
        public void run () { 
          try { 
            if (optionPanel == null) optionPanel = getOptionPanel();
            optionPanel.write (savePanel.getSaveFile()); 
          } // try
          catch (Exception e) {
            JOptionPane.showMessageDialog (frame, 
              "An error occurred writing the file:\n" + 
              e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
          } // catch
          saveDialog.dispose();
        } // run
      });

    // Create cancel action
    // --------------------
    Action cancelAction = GUIServices.createAction ("Cancel", null);

    // Create chooser dialog
    // ---------------------
    Component[] controls = new Component[] {
      GUIServices.getHelpButton (EarthDataExporter.class),
      Box.createHorizontalGlue()
    };
    saveDialog = GUIServices.createDialog (frame, "Export", true,
      this, controls, new Action[] {okAction, cancelAction}, 
      new boolean[] {false, true}, true);

    // Set position to end after visible
    // ---------------------------------
    /*
    saveDialog.addComponentListener (new ComponentAdapter () {
      public void componentShown (ComponentEvent e) {
        saveField.setCaretPosition (saveField.getText().length());
      } // componentShown
    });
    */

    // Show chooser
    // ------------
    saveDialog.setVisible (true);

  } // perform

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    // Get reader
    // ----------
    EarthDataReader reader = null;
    try { reader = EarthDataReaderFactory.create (argv[0]); }
    catch (Exception e) { e.printStackTrace(); System.exit (1); }

    // Create view
    // -----------
    List variableList = null;
    EarthDataView view = null;
    try { 
      variableList = reader.getAllGrids();
      view = new ColorEnhancement (
        reader.getInfo().getTransform(), 
        (Grid) reader.getVariable ("avhrr_ch4"),
        PaletteFactory.create ("HSL256"),
        new LinearEnhancement (new double[] {-60, 45})
      );
      view.resizeMaxAspect (512);
      view.addOverlay (new CoastOverlay (Color.WHITE));
      view.addOverlay (new LatLonOverlay (Color.WHITE));
    } // try
    catch (Exception e) { e.printStackTrace(); System.exit (1); }

    // Create operation
    // ----------------
    final EarthDataExporter exporter = new EarthDataExporter (view, 
      reader.getInfo(), reader, variableList);
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          exporter.showDialog (null);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // EarthDataExporter

////////////////////////////////////////////////////////////////////////
