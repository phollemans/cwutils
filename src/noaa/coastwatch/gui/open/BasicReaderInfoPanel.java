////////////////////////////////////////////////////////////////////////
/*

     File: BasicReaderInfoPanel.java
   Author: Peter Hollemans
     Date: 2005/06/23

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Dimension;

import java.util.List;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.UIManager;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.tools.cwinfo;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.gui.GUIServices;

import static noaa.coastwatch.util.MetadataServices.DATE_FMT;
import static noaa.coastwatch.util.MetadataServices.TIME_FMT;

/**
 * The <code>BasicReaderInfoPanel</code> class displays basic
 * information from a <code>EarthDataReader</code> in a graphical
 * panel.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
@Deprecated
public class BasicReaderInfoPanel
  extends JPanel {

  // Variables
  // ---------
  
  /** The list of [name,value] entries to show in the table. */
  private List<String[]> dataList;
  
  /** The table data model with two columns. */
  private AbstractTableModel dataModel;
  
  ////////////////////////////////////////////////////////////

  /** Creates a new info panel initialized with the specified reader. */
  public BasicReaderInfoPanel (EarthDataReader reader) {

    this();
    setReader (reader);

  } // BasicReaderInfoPanel constructor

  ////////////////////////////////////////////////////////////

  /** Creates a new info panel. */
  public BasicReaderInfoPanel () {

    super (new BorderLayout());

    // We create a data list and model here that simply returns the [0]
    // element as the first column and the [1] element as the second column.
    // Then when the reader is updated, the data list gets updated and the
    // table is refreshed.
    dataList = new ArrayList<>();
    dataModel = new AbstractTableModel() {
      public int getColumnCount() { return (2); }
      public int getRowCount() { return (dataList.size()); }
      public Object getValueAt (int row, int col) { return (dataList.get (row)[col]); }
    };
    var table = new JTable (dataModel);
    table.setTableHeader (null);
    table.setShowGrid (true);
    table.setGridColor (UIManager.getColor ("Panel.background"));
    var scrollPane = new JScrollPane (table);
    this.add (scrollPane, BorderLayout.CENTER);

    // Set the table viewport size large enough to accomodate two columns
    // and eight rows.
    int keyWidth = GUIServices.getLabelWidth (15);
    int valWidth = GUIServices.getLabelWidth (35);
    table.getColumnModel().getColumn (0).setPreferredWidth (keyWidth);
    table.getColumnModel().getColumn (1).setPreferredWidth (valWidth);
    var dims = new Dimension (keyWidth + valWidth, table.getRowHeight() * 8);
    table.setPreferredScrollableViewportSize (dims);

  } // BasicReaderInfoPanel

  ////////////////////////////////////////////////////////////

  /** Clears the information displayed by this panel. */
  public void clear () {

    dataList.clear();
    dataModel.fireTableDataChanged();

  } // clear

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the reader for information displayed by this panel.
   *
   * @param reader the new reader.  The information displayed is
   * modified based on data from the reader.
   */
  public void setReader (
    EarthDataReader reader
  ) {


//    // Create detailed info string
//    // ---------------------------
//    Map metadata = reader.getRawMetadata();
//    if (metadata.size() == 0)
//      detailedInfoString = "(No detailed metadata available.)";
//    else {
//      buffer.setLength (0);
//      for (Iterator iter = metadata.keySet().iterator(); iter.hasNext(); ) {
//        String key = (String) iter.next();
//        Object value = metadata.get (key);
//        buffer.append (key);
//        buffer.append (" = ");
//        buffer.append (MetadataServices.toString (value));
//        buffer.append ("\n");
//    } // for
//      detailedInfoString = buffer.toString();
//    } // else
//



    dataList.clear();
    
    // Add in the data source information, or if from a satellite, the satellite
    // and sensor information.
    var info = reader.getInfo();
    if (info instanceof SatelliteDataInfo) {
      SatelliteDataInfo satInfo = (SatelliteDataInfo) info;
      dataList.add (new String[] {"Satellite", MetadataServices.format (satInfo.getSatellite(), ", ")});
      dataList.add (new String[] {"Sensor", MetadataServices.format (satInfo.getSensor(), ", ")});
    } // if
    else {
      dataList.add (new String[] {"Data source", MetadataServices.format (info.getSource(), ", ")});
    } // else

    // Add in when the data was recorded.
    if (info.isInstantaneous()) {
      var startDate = info.getStartDate();
      dataList.add (new String[] {"Date", DateFormatter.formatDate (startDate, DATE_FMT)});
      dataList.add (new String[] {"Time", DateFormatter.formatDate (startDate, TIME_FMT)});
    } // if
    else {
      var startDate = info.getStartDate();
      var endDate = info.getEndDate();
      var startDateString = DateFormatter.formatDate (startDate, DATE_FMT);
      var endDateString = DateFormatter.formatDate (endDate, DATE_FMT);
      var startTimeString = DateFormatter.formatDate (startDate, TIME_FMT);
      var endTimeString = DateFormatter.formatDate (endDate, TIME_FMT);
      if (startDateString.equals (endDateString)) {
        dataList.add (new String[] {"Date", startDateString});
        dataList.add (new String[] {"Start time", startTimeString});
        dataList.add (new String[] {"End time", endTimeString});
      } // if
      else {
        dataList.add (new String[] {"Start date", startDateString});
        dataList.add (new String[] {"Start time", startTimeString});
        dataList.add (new String[] {"End date", endDateString});
        dataList.add (new String[] {"End time", endTimeString});
      } // else
    } // else

    // Add in the projection information.
    var trans = info.getTransform();
    String projection;
    if (trans == null) projection = "Unknown";
    else {
      projection = trans.describe();
      if (trans instanceof MapProjection)
        projection += " / " + ((MapProjection) trans).getSystemName();
    } // else
    dataList.add (new String[] {"Projection", projection});

    // Add in the data origina and format.
    dataList.add (new String[] {"Origin", MetadataServices.format (info.getOrigin(), ", ")});
    dataList.add (new String[] {"Format", reader.getDataFormat()});

    dataModel.fireTableDataChanged();

  } // setReader

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Create frame
    // ------------
    EarthDataReader reader = EarthDataReaderFactory.create (argv[0]);
    final JFrame frame = new JFrame (BasicReaderInfoPanel.class.getName());
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    frame.setContentPane (new BasicReaderInfoPanel (reader));
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

} // BasicReaderInfoPanel class

////////////////////////////////////////////////////////////////////////
