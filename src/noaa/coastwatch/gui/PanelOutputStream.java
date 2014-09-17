////////////////////////////////////////////////////////////////////////
/*
     FILE: PanelOutputStream.java
  PURPOSE: To display an output stream in a GUI window.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/16
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import javax.swing.*;
import java.io.*;

/**
 * The <code>PanelOutputStream</code> class extends
 * <code>java.io.OutputStream</code> to display output in a Swing
 * <code>JPanel</code>.  The most common use is to create a
 * <code>PanelOutputStream</code> to be used in combination with a
 * <code>java.io.PrintStream</code> object:
 * <pre>
 *   PanelOutputStream panelStream = new PanelOutputStream();
 *   PrintStream printStream = new PrintStream (panelStream, true);
 *   JPanel panel = panelStream.getPanel();
 *   ...
 *   printStream.println ("Hello, world!");
 * </pre>
 * The output stream has a special method <code>getPanel()</code> that
 * retrieves a <code>javax.swing.JPanel</code> object that may be used
 * to display the output.  The retrieved <code>JPanel</code> is simply
 * a Swing text area inside a scrollable pane.  The text area is set
 * to non-editable.<p>
 *
 * In general, as output is appended to the text area, the scroll
 * panel scrolls to the bottom so that the new output is visible.  To
 * explicitly set the caret position of the text area, you can do
 * something like this:
 * <pre>
 *   PanelOutputStream panelStream = new PanelOutputStream();
 *   JTextArea textArea = panelStream.getTextArea(); 
 *   ...
 *   textArea.setCaretPosition (0);
 * </pre>
 * The text area is also useful for setting specific fonts, for example
 * a fixed space font rather than the default proportional space font.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class PanelOutputStream
  extends OutputStream {

  // Variables
  // ---------

  /** The Swing text area for output. */
  private JTextArea textArea;

  /** The Swing panel created by this object. */
  private JPanel panel;

  ////////////////////////////////////////////////////////////

  /** Creates a new panel output stream. */
  public PanelOutputStream () {

    // Create text area and panel
    // --------------------------
    textArea = new JTextArea();
    textArea.setEditable (false);
    JScrollPane scrollPane = new JScrollPane (textArea);
    panel = new JPanel (new BorderLayout());
    panel.add (scrollPane, BorderLayout.CENTER);

  } // PanelOutputStream constructor

  ////////////////////////////////////////////////////////////

  /** Gets the associated output panel. */
  public JPanel getPanel () { return (panel); }

  ////////////////////////////////////////////////////////////

  /** Gets the associated text area. */
  public JTextArea getTextArea () { return (textArea); }

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the specified byte to this output stream.  See the general
   * contract for <code>java.io.OutputStream.write(int)</code>.
   */
  public void write (
    int b
  ) throws IOException {

    textArea.append (new String (new byte[] {(byte) (b & 0xff)}));

  } // write

  ////////////////////////////////////////////////////////////

  /** 
   * Writes <code>b.length</code> bytes from the specified byte array
   * to this output stream. See the general contract for
   * <code>java.io.OutputStream.write(byte[])</code>.
   */
  public void write (
    byte b[]
  ) throws IOException {

    textArea.append (new String (b));

  } // write

  ////////////////////////////////////////////////////////////

  /** 
   * Writes <code>len</code> bytes from the specified byte array
   * starting at offset <code>off</code> to this output stream.  See
   * the general contract for
   * <code>java.io.OutputStream.write(byte[],int,int)</code>.
   */
  public void write (
    byte b[], 
    int off, 
    int len
  ) throws IOException {

    textArea.append (new String (b, off, len));

  } // write

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    // Create panel and print streams
    // ------------------------------
    final PanelOutputStream panelStream = new PanelOutputStream();
    final PrintStream printStream = new PrintStream (panelStream, true);
    JPanel panel = panelStream.getPanel();
    panel.setPreferredSize (new Dimension (300, 200));

    // Set font
    // --------
    JTextArea textArea = panelStream.getTextArea();
    Font font = textArea.getFont();
    Font monoFont = new Font ("Monospaced", font.getStyle(), font.getSize());
    textArea.setFont (monoFont);
    
    // Add panel to frame
    // ------------------
    final JFrame frame = new JFrame (PanelOutputStream.class.getName());
    frame.addWindowListener (new WindowMonitor());
    frame.setContentPane (panel);
    frame.pack();

    // Show frame and test printing
    // ----------------------------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          frame.setVisible (true);
          printStream.println ("Hello, world!");
          for (int i = 0; i < 80; i++)
            printStream.print (i%10);
          printStream.println();
          for (int i = 0; i < 20; i++)
            printStream.println ("Line " + (i+1));
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // PanelOutputStream class

////////////////////////////////////////////////////////////////////////
