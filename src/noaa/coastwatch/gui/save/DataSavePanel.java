////////////////////////////////////////////////////////////////////////
/*
     FILE: DataSavePanel.java
  PURPOSE: Allows the user to choose save options for exported data.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/04
  CHANGES: 2004/09/14, PFH, modified to clone EarthDataInfo
           2004/10/13, PFH, modified to use EarthTransform.getSubset()
           2005/05/20, PFH, modified to set datum on user-specified locations
           2006/11/09, PFH
           - changed write() to write(File)
           - added setVariables(List)
           2014/03/25, PFH
           - Changes: Changed to use getBounds() from getCorners()
           - Issue: API was unclear.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
import java.io.*;
import java.util.*;
import java.util.List;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.gui.*;

/** 
 * The <code>DataSavePanel</code> class is the abstract parent of all
 * data save panels, which allow the user to export Earth data to
 * various file formats.  To export data, create a new panel with the
 * <code>create()</code> method, then call the <code>write()</code>
 * method.  Generally, the save panel is enclosed in a dialog with OK
 * and Cancel options.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public abstract class DataSavePanel
  extends SavePanel {

  // Variables 
  // ---------

  /** The Earth data reader to use for data. */
  protected EarthDataReader reader;

  /** The list of variables for export. */
  protected List variableList;

  /** The current Earth view. */
  protected EarthDataView view;

  /** The subset panel used for creating data subsets. */
  protected SubsetOptionPanel subsetPanel;

  /** The variable option panel. */
  protected VariableOptionPanel variablePanel;

  ////////////////////////////////////////////////////////////

  /** Sets the list of selected variable names. */
  public void setVariables (List nameList) { 

    variablePanel.setVariables (nameList);

  } // setVariables

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if this class requires single variable selection in
   * the variable list.  By default this method returns false unless
   * overridden in the child class.
   */
  protected boolean isSingleVariable() { return (false); }

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new panel appropriate for the specified format. 
   *
   * @param reader the reader to use as a source of data.
   * @param variableList the list of available variables to export.
   * @param view the current data view.
   * @param format the file format.
   */
  public static DataSavePanel create (
    EarthDataReader reader,
    List variableList,
    EarthDataView view,
    String format
  ) {

    // Create new panel
    // ----------------
    DataSavePanel panel;
    if (format.equals ("hdf")) 
      panel = new CWHDFSavePanel (reader, variableList, view);
    else if (format.equals ("raw"))
      panel = new BinarySavePanel (reader, variableList, view);
    else if (format.equals ("txt"))
      panel = new TextSavePanel (reader, variableList, view);
    else if (format.equals ("flt"))
      panel = new ArcSavePanel (reader, variableList, view);
    else 
      throw new IllegalArgumentException ("Unsupported format: " + format);

    return (panel);

  } // create

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new image save panel.
   *
   * @param reader the reader to use as a source of data.
   * @param variableList the list of available variables to export.
   * @param view the current data view.
   */
  protected DataSavePanel (
    EarthDataReader reader,
    List variableList,
    EarthDataView view
  ) {

    // Initialize
    // ----------
    this.reader = reader;
    this.variableList = variableList;
    this.view = view;

    // Create subset option panel
    // --------------------------
    subsetPanel = createSubsetPanel();
    variablePanel = new VariableOptionPanel (variableList, isSingleVariable());

  } // DataSavePanel constructor

  ////////////////////////////////////////////////////////////

  /** Checks the panel contents. */
  public void check () {

    // Check subset
    // ------------
    if (subsetPanel.getSubsetType() != SubsetOptionPanel.SUBSET_ALL) {
      int[] start = new int[2];
      int[] dims = new int[2];
      getPanelSubset (start, dims);
      Grid grid = getDataGrid();
      if (!grid.checkSubset (start, dims))
        throw new RuntimeException ("Invalid subset specified");
    } // if

    // Check list of variables
    // -----------------------
    List variableList = variablePanel.getVariables();
    if (variableList.size() == 0) {
      throw new RuntimeException ("No variables were selected for export");
    } // if

  } // check

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the subset information specified by the subset option
   * panel. 
   *
   * @param upperLeft the upper-left data coordinate as <code>[row,
   * column]</code> (modified).
   * @param dimensions the data dimensions as <code>[rows,
   * columns]</code> (modified).
   */
  protected void getPanelSubset (
    int[] upperLeft,
    int[] dimensions
  ) {

    int type = subsetPanel.getSubsetType();
    switch (type) {

    // Get current view subset
    // -----------------------      
    case SubsetOptionPanel.SUBSET_CURRENT:
      getViewSubset (upperLeft, dimensions, null);
      break;

    // Get image coordinate-based subset
    // ---------------------------------
    case SubsetOptionPanel.SUBSET_IMAGE:
      subsetPanel.getImageCoords (upperLeft, dimensions);
      break;

    // Get geographic coordinate-based subset
    // --------------------------------------
    case SubsetOptionPanel.SUBSET_GEOGRAPHIC:

      // Get bounds
      // ----------
      EarthTransform trans = reader.getInfo().getTransform();
      Datum datum = trans.getDatum();
      EarthLocation[] bounds = new EarthLocation[2];
      bounds[0] = new EarthLocation (datum);
      bounds[1] = new EarthLocation (datum);
      subsetPanel.getGeographicCoords (bounds[0], bounds[1]);

      // Transform bounds to corners
      // ---------------------------
      DataLocation[] corners = new DataLocation[2];
      corners[0] = trans.transform (bounds[0]);
      corners[1] = trans.transform (bounds[1]);
      if (!corners[0].isValid() || !corners[1].isValid())
        throw new RuntimeException ("Invalid geographic corners specified");

      // Get truncated corners
      // ---------------------
      getTruncated (corners, upperLeft, dimensions);

      break;
      
    default: 
      throw new IllegalStateException ("Unknown subset type specified");

    } // switch

  } // getPanelSubset

  ////////////////////////////////////////////////////////////

  /**
   * Creates an information object based on the reader, taking into
   * account any subset specified.
   *
   * @return the new information object.
   */
  protected EarthDataInfo createInfo () {

    EarthDataInfo info = reader.getInfo();
    EarthDataInfo newInfo = (EarthDataInfo) info.clone();
    if (subsetPanel.getSubsetType() != SubsetOptionPanel.SUBSET_ALL) {
      int[] upperLeft = new int[2];
      int[] dimensions = new int[2];
      getPanelSubset (upperLeft, dimensions);
      DataLocation upperLeftLoc = 
        new DataLocation (upperLeft[0], upperLeft[1]);
      EarthTransform newTrans = 
        info.getTransform().getSubset (upperLeftLoc, dimensions);
      newInfo.setTransform (newTrans);
    } // if

    return (newInfo);

  } // createInfo

  ////////////////////////////////////////////////////////////

  /** Gets the data grid in the view. */
  private Grid getDataGrid () {

    Grid grid;
    if (view instanceof ColorEnhancement) 
      grid = ((ColorEnhancement) view).getGrid();
    else if (view instanceof ColorComposite)
      grid = ((ColorComposite) view).getGrids()[0];
    else 
      throw new IllegalStateException ("Unknown Earth data view");

    return (grid);

  } // getDataGrid

  ////////////////////////////////////////////////////////////

  /** Gets the full data dimensions of the grid. */
  private int[] getDataDimensions () {

    return (getDataGrid().getDimensions());

  } // getDataDimensions

  ////////////////////////////////////////////////////////////
  
  /** 
   * Gets the integer data location coordinates of the specified data
   * locations.
   *
   * @param corners the data location corners as <code>[upper-left,
   * lower-right]</code> (modified).
   * @param lowerRightLoc the lower-right data location.
   * @param upperLeft the upper-left data coordinate as <code>[row,
   * column]</code> (modified).
   * @param dimensions the data dimensions as <code>[rows,
   * columns]</code> (modified).
   */
  private void getTruncated (
    DataLocation[] corners,
    int[] upperLeft,
    int[] dimensions
  ) {

    int[] dims = getDataDimensions();
    corners[0] = corners[0].truncate (dims).round();
    corners[1] = corners[1].truncate (dims).round();
    upperLeft[0] = (int) corners[0].get (0);
    upperLeft[1] = (int) corners[0].get (1);
    dimensions[0] = ((int) corners[1].get(0)) - ((int) corners[0].get(0)) + 1;
    dimensions[1] = ((int) corners[1].get(1)) - ((int) corners[0].get(1)) + 1;

  } // getTruncated

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the subset information specified by the view.
   *
   * @param upperLeft the upper-left data coordinate as <code>[row,
   * column]</code> (modified).
   * @param dimensions the data dimensions as <code>[rows,
   * columns]</code> (modified).
   * @param bounds the data bounds as
   * <code>[upper-left, lower-right]</code> (modified) or null.
   */
  private void getViewSubset (
    int[] upperLeft,
    int[] dimensions,
    EarthLocation[] bounds
  ) {

    // Get truncated corners
    // ---------------------
    DataLocation[] corners = view.getBounds();
    getTruncated (corners, upperLeft, dimensions);

    // Get bounds
    // ----------
    if (bounds != null) {
      EarthTransform trans = reader.getInfo().getTransform();
      bounds[0] = trans.transform (corners[0]);
      bounds[1] = trans.transform (corners[1]);
    } // if

  } // getViewSubset

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a subset option panel whose parameters match the
   * view.
   */
  private SubsetOptionPanel createSubsetPanel () {

    // Get view subset
    // ---------------
    int[] upperLeft = new int[2];
    int[] dimensions = new int[2];
    EarthLocation[] bounds = new EarthLocation[2];
    getViewSubset (upperLeft, dimensions, bounds);

    return (new SubsetOptionPanel (upperLeft, dimensions, bounds));

  } // createSubsetPanel

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the list of variables from the variable option panel to
   * the writer.  The subset option panel is used to subset the
   * variable data if needed.  The writer is closed after the final
   * variable data is written.
   *
   * @param writer the writer used to export data.
   *
   * @throws IOException if an error occurred writing the data.
   */
  protected void write (
    final EarthDataWriter writer
  ) throws IOException {
  
    // Get subset parameters
    // ---------------------
    boolean isSubset = 
      (subsetPanel.getSubsetType() != SubsetOptionPanel.SUBSET_ALL);
    int[] start = new int[2];
    int[] dims = new int[2];
    if (isSubset) getPanelSubset (start, dims);

    // Add each variable to writer
    // ---------------------------
    List variableList = variablePanel.getVariables();
    for (Iterator iter = variableList.iterator(); iter.hasNext(); ) {
      String variableName = (String) iter.next();
      Grid grid = (Grid) reader.getVariable (variableName);
      if (isSubset) grid = grid.getSubset (start, dims);
      writer.addVariable (grid);
    } // for

    // Create progress monitor
    // -----------------------
    final File file = new File (writer.getDestination());
    final ProgressMonitor monitor =  new ProgressMonitor (this, 
      "Saving data to " + file.getName(), " ", 0, writer.getProgressLength());
    monitor.setMillisToPopup (1000);

    // Create progress update task
    // ---------------------------
    ActionListener task = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          monitor.setProgress (writer.getProgress());
          String name = writer.getProgressVariable();
          if (name != null) monitor.setNote ("Writing " + name);
          if (monitor.isCanceled()) writer.cancel();
        } // actionPerformed
      };
    final Timer timer = new Timer (500, task);

    // Perform data write
    // ------------------
    Thread writeThread = new Thread () {
        public void run () {

          // Attempt write and close
          // -----------------------
          try { 
            writer.flush();
            writer.close(); 
          } // try

          // Show error message
          // ------------------
          catch (IOException e) {
            final String errorMessage = 
              "An error occurred writing the file:\n" + e.toString();
            GUIServices.invokeAndWait (new Runnable() {
                public void run() {
                  JOptionPane.showMessageDialog (DataSavePanel.this,
                    errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                } // run
              });
          } // catch

          // Stop timer and close monitor dialog
          // -----------------------------------
          finally {
            timer.stop();
            if (monitor.isCanceled()) {
              if (file.exists()) file.delete();
            } // if
            else {
              GUIServices.invokeAndWait (new Runnable() {
                  public void run() {
                    monitor.close();
                  } // run
                });
            } // else
          } // finally

        } // run
      };
    writeThread.start();
    timer.start();

    // TODO: A problem can occur if the write thread is writing data
    // while another thread is attempting to read data from the same
    // file.  Partly, this is because the progress dialog does not
    // seem to be modal and allows operations in other windows.
    // Another reason is that file I/O is not always inside a
    // synchronized statement when it should be.  For example, if two
    // threads are both using the same JNI library.

  } // write

  ////////////////////////////////////////////////////////////

} // DataSavePanel class

////////////////////////////////////////////////////////////////////////
