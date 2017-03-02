////////////////////////////////////////////////////////////////////////
/*

     File: BinarySavePanel.java
   Author: Peter Hollemans
     Date: 2004/05/05

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
import noaa.coastwatch.gui.save.BinaryOptionPanel;
import noaa.coastwatch.gui.save.DataSavePanel;
import noaa.coastwatch.io.BinaryWriter;
import noaa.coastwatch.io.ByteWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.FloatWriter;
import noaa.coastwatch.io.ShortWriter;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.util.EarthDataInfo;

/** 
 * The <code>BinarySavePanel</code> class allows the user to select save
 * options for binary export files.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class BinarySavePanel
  extends DataSavePanel {

  // Variables 
  // ---------

  /** The binary option panel. */
  private BinaryOptionPanel binaryPanel;

  ////////////////////////////////////////////////////////////

  /** Checks the panel contents. */
  public void check () {

    super.check();

    // Check scaling
    // -------------
    int type = binaryPanel.getDataType();
    if (type != BinaryOptionPanel.TYPE_FLOAT) {
      if (binaryPanel.getScalingType() == BinaryOptionPanel.SCALING_RANGE) {
        double[] range = new double[2];
        binaryPanel.getRange (range);
      } // if
      else {
        double[] scaling = new double[3];
        binaryPanel.getScaling (scaling);
      } // else
    } // if

    // Check missing
    // -------------
    double missing = binaryPanel.getMissing();

  } // check

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the data as a binary file.
   *  
   * @param file the output file to write.
   *
   * @throws IOException if an error occurred writing to the file.
   */
  public void write (File file) throws IOException {

    // Create info object
    // ------------------
    EarthDataInfo info = createInfo();

    // Create writer based on data type
    // --------------------------------
    BinaryWriter writer = null;
    int type = binaryPanel.getDataType();
    switch (type) {
    case BinaryOptionPanel.TYPE_UBYTE: 
      writer = new ByteWriter (info, file.getPath());
      break;
    case BinaryOptionPanel.TYPE_SHORT: 
      writer = new ShortWriter (info, file.getPath());
      break;
    case BinaryOptionPanel.TYPE_FLOAT: 
      writer = new FloatWriter (info, file.getPath());
      break;
    } // switch

    // Set scaling
    // -----------
    if (type != BinaryOptionPanel.TYPE_FLOAT) {
      if (binaryPanel.getScalingType() == BinaryOptionPanel.SCALING_RANGE) {
        double[] range = new double[2];
        binaryPanel.getRange (range);
        writer.setRange (range[0], range[1]);
      } // if
      else {
        double[] scaling = new double[3];
        binaryPanel.getScaling (scaling);
        writer.setScaling (scaling);
      } // else
    } // if

    // Set missing
    // -----------
    writer.setMissing (new Double (binaryPanel.getMissing()));

    // Set byte order
    // --------------
    int order = binaryPanel.getByteOrder();
    switch (order) {
    case BinaryOptionPanel.ORDER_HOST:
      writer.setOrder (BinaryWriter.HOST);
      break;
    case BinaryOptionPanel.ORDER_MSB:
      writer.setOrder (BinaryWriter.MSB);
      break;
    case BinaryOptionPanel.ORDER_LSB:
      writer.setOrder (BinaryWriter.LSB);
      break;
    } // switch

    // Set header
    // ----------
    writer.setHeader (binaryPanel.getHeader());

    // Write data
    // ----------
    write (writer);

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new binary save panel.
   *
   * @param reader the reader to use as a source of data.
   * @param variableList the list of available variables to export.
   * @param view the current data view.
   */
  public BinarySavePanel (
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
    binaryPanel = new BinaryOptionPanel();
    this.add (binaryPanel, gc);

  } // BinarySavePanel constructor

  ////////////////////////////////////////////////////////////

} // BinarySavePanel class

////////////////////////////////////////////////////////////////////////
