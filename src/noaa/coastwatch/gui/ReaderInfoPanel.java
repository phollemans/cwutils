////////////////////////////////////////////////////////////////////////
/*

     File: ReaderInfoPanel.java
   Author: Peter Hollemans
     Date: 2004/02/17

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
package noaa.coastwatch.gui;

// Imports
// -------

import java.util.Map;
import java.util.List;
import java.io.IOException;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import noaa.coastwatch.gui.WindowMonitor;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.ReaderSummaryProducer;
import noaa.coastwatch.io.ReaderSummaryProducer.Summary;
import noaa.coastwatch.io.ReaderSummaryProducer.SummaryTable;
import noaa.coastwatch.util.HTMLReportFormatter;

import java.util.logging.Logger;

/**
 * The <code>ReaderInfoPanel</code> class displays information from a 
 * <code>EarthDataReader</code> in a graphical panel.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ReaderInfoPanel extends JPanel {

  private static final Logger LOGGER = Logger.getLogger (ReaderInfoPanel.class.getName());

  /** The panel showing the HTML. */
  private JEditorPane editor;

  ////////////////////////////////////////////////////////////

  private void updateEditor (EarthDataReader reader) {

    try { 

      var producer = ReaderSummaryProducer.getInstance();
      var formatter = HTMLReportFormatter.create();
      formatter.setBorderColor (UIManager.getColor ("Label.background"));
      producer.report (producer.create (reader), formatter);

      editor.setText (formatter.getContent());
      editor.setCaretPosition (0);

    } catch (IOException e) {
      LOGGER.warning (e.getMessage());
    } // catch

  } // updateEditor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new panel showing information from the specified
   * reader.
   * 
   * @param reader the reader to show.
   */
  public ReaderInfoPanel (
    EarthDataReader reader
  ) {

    super (new BorderLayout());

    this.editor = new JEditorPane();
    editor.setContentType ("text/html");
    editor.putClientProperty (JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    editor.setEditable (false);
    var scroll = new JScrollPane (editor);
    this.add (scroll, BorderLayout.CENTER);

    var dims = new Dimension (
      GUIServices.getLabelWidth (100),
      GUIServices.getLabelHeight() * 30
    );
    editor.setPreferredSize (dims);

    updateEditor (reader);

  } // ReaderInfoPanel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a dialog showing the reader panel and Close button.
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
    final JFrame frame = new JFrame (ReaderInfoPanel.class.getName());
    frame.addWindowListener (new WindowMonitor());
    frame.setContentPane (new ReaderInfoPanel (reader));
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

} // ReaderInfoPanel class

////////////////////////////////////////////////////////////////////////
