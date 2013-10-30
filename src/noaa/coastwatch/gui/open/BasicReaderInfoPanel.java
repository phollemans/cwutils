////////////////////////////////////////////////////////////////////////
/*
     FILE: BasicReaderInfoPanel.java
  PURPOSE: To show basic information from an Earth data reader.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/23
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
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import noaa.coastwatch.tools.*;
import com.braju.format.Format;

/**
 * The <code>BasicReaderInfoPanel</code> class displays basic
 * information from a <code>EarthDataReader</code> in a graphical
 * panel.  A details checkbox may be used to display the raw metadata
 * from the reader.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class BasicReaderInfoPanel
  extends JPanel {

  // Constants
  // ---------

  /** The default date format. */
  private static final String DATE_FMT = cwinfo.DATE_FMT;

  /** The UTC time format. */
  private static final String TIME_FMT = cwinfo.TIME_FMT;

  // Variables
  // ---------
  
  /** The Swing text area for output. */
  private JTextArea textArea;

  /** The string for normal display. */
  private String basicInfoString;

  /** The string for detailed display. */
  private String detailedInfoString;

  /** The details mode check box. */
  private JCheckBox detailsCheckBox;

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

    // Create text area and panel
    // --------------------------
    textArea = new JTextArea();
    textArea.setEditable (false);
    textArea.setColumns (45);
    JScrollPane scrollPane = new JScrollPane (textArea);
    this.add (scrollPane, BorderLayout.CENTER);

    // Set font and columns
    // --------------------
    Font font = textArea.getFont();
    Font monoFont = new Font ("Monospaced", font.getStyle(), font.getSize());
    textArea.setFont (monoFont);

    // Create details checkbox
    // -----------------------
    detailsCheckBox = new JCheckBox ("Show detailed metadata");
    detailsCheckBox.addItemListener (new ItemListener () {
        public void itemStateChanged (ItemEvent event) {
          updateTextArea();
        } // itemStateChanged
      });
    this.add (detailsCheckBox, BorderLayout.SOUTH);

  } // BasicReaderInfoPanel constructor

  ////////////////////////////////////////////////////////////

  /** Updates the text area after a change. */
  private void updateTextArea () {

    // Check for null text
    // -------------------
    if (basicInfoString == null) { textArea.setText (""); return; }

    // Create text to display
    // ----------------------
    StringBuffer buffer = new StringBuffer();
    buffer.append (basicInfoString);
    if (detailsCheckBox.isSelected()) {
      buffer.append ("\n----- Detailed metadata -----\n\n");
      buffer.append (detailedInfoString);
    } // if
    textArea.setText (buffer.toString());
    textArea.setCaretPosition (0);

  } // updateTextArea

  ////////////////////////////////////////////////////////////

  /** Clears the information displayed by this panel. */
  public void clear () {

    basicInfoString = null;
    detailedInfoString = null;
    updateTextArea();

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

    // Create map of attributes to values
    // ----------------------------------
    Map valueMap = new LinkedHashMap();

    // Add data source info
    // --------------------
    EarthDataInfo info = reader.getInfo();
    if (info instanceof SatelliteDataInfo) {
      SatelliteDataInfo satInfo = (SatelliteDataInfo) info;
      valueMap.put ("Satellite", 
        MetadataServices.format (satInfo.getSatellite(), ", "));
      valueMap.put ("Sensor", 
        MetadataServices.format (satInfo.getSensor(), ", "));
    } // if
    else {
      valueMap.put ("Data source", 
        MetadataServices.format (info.getSource(), ", "));
    } // else

    // Add single time info
    // --------------------
    if (info.isInstantaneous()) {
      Date startDate = info.getStartDate();
      valueMap.put ("Date", DateFormatter.formatDate (startDate, DATE_FMT));
      valueMap.put ("Time", DateFormatter.formatDate (startDate, TIME_FMT));
    } // if

    // Add time range info
    // -------------------
    else {
      Date startDate = info.getStartDate();
      Date endDate = info.getEndDate();
      String startDateString = DateFormatter.formatDate (startDate, DATE_FMT);
      String endDateString = DateFormatter.formatDate (endDate, DATE_FMT);
      String startTimeString = DateFormatter.formatDate (startDate, TIME_FMT);
      String endTimeString = DateFormatter.formatDate (endDate, TIME_FMT);
      if (startDateString.equals (endDateString)) {
        valueMap.put ("Date", startDateString);
        valueMap.put ("Start time", startTimeString);
        valueMap.put ("End time", endTimeString);
      } // if
      else {
        valueMap.put ("Start date", startDateString);
        valueMap.put ("Start time", startTimeString);
        valueMap.put ("End date", endDateString);
        valueMap.put ("End time", endTimeString);
      } // else
    } // else

    // Add Earth transform info
    // ------------------------
    EarthTransform trans = info.getTransform();
    String projection;
    if (trans == null) projection = "unknown";
    else {
      projection = trans.describe();
      if (trans instanceof MapProjection)
        projection += " / " + ((MapProjection) trans).getSystemName();
    } // else
    valueMap.put ("Projection", projection);

    // Add other info
    // --------------
    valueMap.put ("Origin", MetadataServices.format (info.getOrigin(), ", "));
    valueMap.put ("Format", reader.getDataFormat());

    // Create basic info string
    // ------------------------
    StringBuffer buffer = new StringBuffer();
    for (Iterator iter = valueMap.keySet().iterator(); iter.hasNext(); ) {
      String key = (String) iter.next();
      String value = (String) valueMap.get (key);
      buffer.append (Format.sprintf ("%-12s %s\n", 
        new Object[] {key + ":", value}));
    } // for
    basicInfoString = buffer.toString();

    // Create detailed info string
    // ---------------------------
    Map metadata = reader.getRawMetadata();
    if (metadata.size() == 0) 
      detailedInfoString = "(No detailed metadata available.)";
    else {
      buffer.setLength (0);
      for (Iterator iter = metadata.keySet().iterator(); iter.hasNext(); ) {
        String key = (String) iter.next();
        Object value = metadata.get (key);
        buffer.append (key);
        buffer.append (" = ");
        buffer.append (MetadataServices.toString (value));
        buffer.append ("\n");
    } // for
      detailedInfoString = buffer.toString();
    } // else

    // Update text
    // -----------
    updateTextArea();

  } // setReader

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
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
