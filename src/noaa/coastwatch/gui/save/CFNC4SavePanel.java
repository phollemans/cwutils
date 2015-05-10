////////////////////////////////////////////////////////////////////////
/*
     FILE: CFNC4SavePanel.java
  PURPOSE: Allows the user to choose CF NetCDF 4 save options.
   AUTHOR: Peter Hollemans
     DATE: 2015/04/13
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2015, USDOC/NOAA/NESDIS CoastWatch

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
import noaa.coastwatch.gui.save.DataSavePanel;
import noaa.coastwatch.io.CFNC4Writer;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.util.EarthDataInfo;

/** 
 * The <code>CFNC4SavePanel</code> class allows the user to select save
 * options for CF NetCDF 4 export files.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class CFNC4SavePanel
  extends DataSavePanel {

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the data as a CoastWatch HDF file.
   *  
   * @param file the output file to write.
   *
   * @throws IOException if an error occurred writing to the file.
   */
  public void write (File file) throws IOException {

    // Create info object
    // ------------------
    EarthDataInfo info = createInfo();

    // Create output file
    // ------------------
    CFNC4Writer writer;
    try {
      writer = new CFNC4Writer (info, file.getPath());
    } // try
    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch
    write (writer);

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new CF NetCDF save panel.
   *
   * @param reader the reader to use as a source of data.
   * @param variableList the list of available variables to export.
   * @param view the current data view.
   */
  public CFNC4SavePanel (
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

  } // CFNC4SavePanel constructor

  ////////////////////////////////////////////////////////////

} // CFNC4SavePanel class

////////////////////////////////////////////////////////////////////////
