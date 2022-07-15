////////////////////////////////////////////////////////////////////////
/*

     File: ReaderMetadataPanel.java
   Author: Peter Hollemans
     Date: 2022/04/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2022 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui;

// Imports
// -------

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.JEditorPane;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.gui.WindowMonitor;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.HTMLReportFormatter;

import java.util.logging.Logger;

/**
 * The <code>ReaderMetadataPanel</code> class displays metadata from an
 * <code>EarthDataReader</code> in a graphical panel.
 *
 * @author Peter Hollemans
 * @since 3.7.1
 */
public class ReaderMetadataPanel extends JPanel {

  private static final Logger LOGGER = Logger.getLogger (ReaderMetadataPanel.class.getName());

  private static int ATT_SCOPE = 0;
  private static int ATT_NAME = 1;
  private static int ATT_TYPE = 2;
  private static int ATT_VALUE = 3;
  private static int COLUMNS = 4;
  private static final String[] COLUMN_NAMES = new String[] {"Scope", "Attribute Name", "Type", "Value"};

  /** The metadata to show in the table. */
  private List<String[]> metadataList;

  /** The filtered metadata to show in the table. */
  private List<String[]> filteredList;

  /** The panel showing the HTML. */
  private JEditorPane editor;

  ////////////////////////////////////////////////////////////

  private void setFilter (String text) {

    if (text == null || text.equals (""))
      this.filteredList = metadataList;
    else {
      this.filteredList = metadataList.stream().filter (att -> 
        att[ATT_NAME].indexOf (text) != -1 || att[ATT_VALUE].indexOf (text) != -1
      ).collect (Collectors.toList());
    } // else

    updateEditor();

  } // setFilter 

  ////////////////////////////////////////////////////////////

  private void updateEditor () {

    // First, we create a version of the metadata organized by section.
    Map<String, List<String[]>> sectionMap = new LinkedHashMap<>();
    filteredList.forEach (att -> {
      var scope = att[ATT_SCOPE];
      var section = sectionMap.get (scope);
      if (section == null) { section = new ArrayList<>(); sectionMap.put (scope, section); }
      section.add (Arrays.copyOfRange (att, 1, 4));
    });

    // Now, create an HTML table with a section for each scope of metadata 
    // values.
    var formatter = HTMLReportFormatter.create();
    formatter.setBorderColor (UIManager.getColor ("Label.background"));
    formatter.start();
    formatter.table (Arrays.copyOfRange (COLUMN_NAMES, 1, 4), sectionMap);
    formatter.end();

    editor.setText (formatter.getContent());
    editor.setCaretPosition (0);

  } // updateEditor

  ////////////////////////////////////////////////////////////

  private String getType (Object value) {

    var valueClass = value.getClass();
    var isArray = valueClass.isArray();
    var className = (isArray ? valueClass.getComponentType().toString() : valueClass.toString());
    var type = className.substring (className.lastIndexOf ('.')+1).toLowerCase();
    if (type.equals ("integer")) type = "int";
    if (isArray) type += "[]";
    return (type);

  } // getType

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new panel showing metadata from the specified
   * reader.
   *
   * @param reader the reader to show.
   */
  public ReaderMetadataPanel (
    EarthDataReader reader
  ) {

    super (new BorderLayout());

    // Set up an initial list of all metadata, using the maps retrieved from
    // the reader.
    this.metadataList = new ArrayList<>();
    var global = "Global";
    reader.getRawMetadata().forEach ((key, value) -> {  
      metadataList.add (new String[] {global, (String) key, getType (value), MetadataServices.toString (value)});
    });
    for (int i = 0; i < reader.getVariables(); i++) {
      try { 
        var map = reader.getRawMetadata (i);
        var name = "Variable " + reader.getName (i);
        map.forEach ((key, value) -> {  
          metadataList.add (new String[] {name, (String) key, getType (value), MetadataServices.toString (value)});
        });
      } // try
      catch (IOException e) { LOGGER.warning ("Error reading metadata at variable index " + i + ": " + e.toString()); }
    } // for
    this.filteredList = this.metadataList;

    // Create an HTML display panel to list the attributes' scope, name, 
    // type, and values.
    this.editor = new JEditorPane();
    editor.setContentType ("text/html");
    editor.putClientProperty (JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    editor.setEditable (false);
    var scroll = new JScrollPane (editor);
    this.add (scroll, BorderLayout.CENTER);
    var colWidths = new int[] {
      GUIServices.getLabelWidth (10),
      GUIServices.getLabelWidth (30),
      GUIServices.getLabelWidth (8),
      GUIServices.getLabelWidth (80)
    };
    var dims = new Dimension (
      colWidths[1]+colWidths[2]+colWidths[3],
      GUIServices.getLabelHeight() * 30
    );
    editor.setPreferredSize (dims);
    updateEditor();



    // This was a previous iteration of the table display, but lacked the
    // ability to easily display the full width of long attribute values.
    // The UI was nicer looking though, and included a floating table header.

    // // Create a table with four columns to display the attributes' scope, name, 
    // // type, and values.
    // this.model = new AbstractTableModel() {
    //   public int getColumnCount() { return (4); }
    //   public int getRowCount() { return (filteredList.size()); }
    //   public Object getValueAt (int row, int col) { return (filteredList.get (row)[col]); }
    //   public String getColumnName (int columnIndex) { return (COLUMN_NAMES[columnIndex]); }
    // };
    // var table = new JTable (model);
    // var scroll = new JScrollPane (table);
    // this.add (scroll, BorderLayout.CENTER);

    // var maxValueLength = metadataList.stream().mapToInt (att -> att[ATT_VALUE].length()).max().orElse (0);
    // maxValueLength = Math.max (80, Math.min (maxValueLength, 300));
    // var colWidths = new int[] {
    //   getLabelWidth (10),
    //   getLabelWidth (30),
    //   getLabelWidth (8),
    //   getLabelWidth (maxValueLength)
    // };
    // for (int i = 0; i < 4; i++) table.getColumnModel().getColumn (i).setPreferredWidth (colWidths[i]);
    // var dims = new Dimension (
    //   colWidths[0]+colWidths[1]+colWidths[2]+getLabelWidth (80), 
    //   table.getRowHeight() * 40
    // );
    // table.setPreferredScrollableViewportSize (dims);
    // table.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);




    // Create a filtering mechanism that allows you to look for strings 
    // in either the attribute name or value.
    var panel = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 5));
    this.add (panel, BorderLayout.NORTH);
    panel.add (new JLabel ("Filter by keyword:"));
    var field = new JTextField (20);
    field.getDocument().addDocumentListener (new DocumentListener () {
      private void changed () { setFilter (field.getText()); }
      public void insertUpdate (DocumentEvent e) { changed(); }
      public void removeUpdate(DocumentEvent e) { changed(); }
      public void changedUpdate(DocumentEvent e) { changed(); }
    });
    panel.add (field);
    var button = new JButton ("Clear");
    button.addActionListener (event -> field.setText (""));
    panel.add (button);

  } // ReaderMetadataPanel

  ////////////////////////////////////////////////////////////

  /**
   * Creates a dialog showing the metadata panel and close button.
   *
   * @param component the parent component for the dialog.
   * @param title the dialog window title.
   */
  public void showDialog (
    Component component,
    String title
  ) {

    // Create info dialog
    // ------------------
    JOptionPane optionPane = new JOptionPane (this,
      JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
      null, new String [] {"Close"});
    JDialog dialog = optionPane.createDialog (component, title);
    dialog.setResizable (true);
    dialog.setModal (false);
    dialog.setVisible (true);

  } // showDialog

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

    // Add panel to frame
    // ------------------
    final JFrame frame = new JFrame (ReaderMetadataPanel.class.getName());
    frame.addWindowListener (new WindowMonitor());
    frame.setContentPane (new ReaderMetadataPanel (reader));
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

} // ReaderMetadataPanel class

////////////////////////////////////////////////////////////////////////
