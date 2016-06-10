////////////////////////////////////////////////////////////////////////
/*
     FILE: ReaderInfoPanel.java
  PURPOSE: To show information from an earth data reader.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/17
  CHANGES: 2004/11/24, PFH, extended to print transform info
           2005/02/18, PFH, modified newline insertion points
           2005/07/06, PFH, removed info tab, added showDialog() 

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.io.PrintStream;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import noaa.coastwatch.gui.PanelOutputStream;
import noaa.coastwatch.gui.WindowMonitor;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.tools.cwinfo;

/**
 * The <code>ReaderInfoPanel</code> class displays information from a 
 * <code>EarthDataReader</code> in a graphical panel.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ReaderInfoPanel
  extends JPanel {

  // Constants
  // ---------

  /** The preferred columns of text. */
  private static final int PREFERRED_COLUMNS = 80;

  /** The preferred rows of text. */
  private static final int PREFERRED_ROWS = 25;

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

    // Create panel and print streams
    // ------------------------------
    PanelOutputStream panelStream = new PanelOutputStream();
    PrintStream printStream = new PrintStream (panelStream, true);
    JPanel panel = panelStream.getPanel();
    this.add (panel, BorderLayout.CENTER);

    // Set to monospaced font
    // ----------------------
    JTextArea textArea = panelStream.getTextArea();
    Font font = textArea.getFont();
    Font monoFont = new Font ("Monospaced", font.getStyle(), font.getSize());
    textArea.setFont (monoFont);

    // Set preferred columns
    // ---------------------
    textArea.setColumns (PREFERRED_COLUMNS);
    textArea.setRows (PREFERRED_ROWS);

    // Print reader info
    // -----------------
    cwinfo.printInfo (reader, printStream);
    cwinfo.printTransform (reader, printStream, false);
    textArea.setCaretPosition (0);

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
