////////////////////////////////////////////////////////////////////////
/*

     File: CWHDFSavePanel.java
   Author: Peter Hollemans
     Date: 2004/05/04

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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.save.DataSavePanel;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.util.EarthDataInfo;

/** 
 * The <code>CWHDFSavePanel</code> class allows the user to select save
 * options for CoastWatch HDF export files.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class CWHDFSavePanel
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
    CWHDFWriter writer;
    try {
      writer = new CWHDFWriter (info, file.getPath());
    } // try
    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch
    write (writer);

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new CoastWatch HDF save panel.
   *
   * @param reader the reader to use as a source of data.
   * @param variableList the list of available variables to export.
   * @param view the current data view.
   */
  public CWHDFSavePanel (
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

  } // CWHDFSavePanel constructor

  ////////////////////////////////////////////////////////////

} // CWHDFSavePanel class

////////////////////////////////////////////////////////////////////////
