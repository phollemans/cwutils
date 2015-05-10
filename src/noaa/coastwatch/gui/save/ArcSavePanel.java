////////////////////////////////////////////////////////////////////////
/*
     FILE: ArcSavePanel.java
  PURPOSE: Allows the user to choose ArcGIS save options.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/05
  CHANGES: 2006/11/09, PFH, changed write() to write(File)

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.save.ArcOptionPanel;
import noaa.coastwatch.gui.save.DataSavePanel;
import noaa.coastwatch.io.ArcWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.util.EarthDataInfo;

/** 
 * The <code>ArcSavePanel</code> class allows the user to select save
 * options for ArcGIS export files.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ArcSavePanel
  extends DataSavePanel {

  // Variables 
  // ---------

  /** The ArcGIS option panel. */
  private ArcOptionPanel arcPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true since an ArcGIS binary grid file may container only
   * one variable.
   */
  protected boolean isSingleVariable() { return (true); }

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the data as an ArcGIS binary grid file.
   *  
   * @param file the output file to write.
   *
   * @throws IOException if an error occurred writing to the file.
   */
  public void write (File file) throws IOException {

    // Create writer
    // -------------
    EarthDataInfo info = createInfo();
    ArcWriter writer = new ArcWriter (info, file.getPath());

    // Set header
    // ----------
    writer.setHeader (arcPanel.getHeader());

    // Write data
    // ----------
    write (writer);

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new ArcGIS save panel.
   *
   * @param reader the reader to use as a source of data.
   * @param variableList the list of available variables to export.
   * @param view the current data view.
   */
  public ArcSavePanel (
    EarthDataReader reader,
    List variableList,
    EarthDataView view
  ) {

    // Initialize
    // ----------
    super (reader, variableList, view);
    setLayout (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();

    // Create panels
    // -------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.BOTH, 1, 1);
    this.add (variablePanel, gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    this.add (subsetPanel, gc);

    GUIServices.setConstraints (gc, 0, 2, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    arcPanel = new ArcOptionPanel();
    this.add (arcPanel, gc);

  } // ArcSavePanel constructor

  ////////////////////////////////////////////////////////////

} // ArcSavePanel class

////////////////////////////////////////////////////////////////////////
