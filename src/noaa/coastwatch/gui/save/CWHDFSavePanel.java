////////////////////////////////////////////////////////////////////////
/*
     FILE: CWHDFSavePanel.java
  PURPOSE: Allows the user to choose CoastWatch WHDF save options.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/04
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
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.List;
import noaa.coastwatch.io.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.tools.*;
import noaa.coastwatch.util.*;

/** 
 * The <code>CWHDFSavePanel</code> class allows the user to select save
 * options for CoastWatch HDF export files.
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
