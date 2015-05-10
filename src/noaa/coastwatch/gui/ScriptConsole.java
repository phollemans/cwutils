////////////////////////////////////////////////////////////////////////
/*
     FILE: ScriptConsole.java
  PURPOSE: To provide a scripting interface to the CoastWatch Utilities.
   AUTHOR: Peter Hollemans
     DATE: 2015/05/01
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2015, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import bsh.util.JConsole;
import bsh.Interpreter;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.UIManager;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.border.Border;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;

/**
 * The <code>ScriptConsole</code> can be shown and hidden to allow a scripting
 * interface to the CoastWatch Utilities using Beanshell syntax.  Refer to 
 * the Beanshell documentation, Java API, and CoastWatch Utilities API for 
 * details on valid commands and classes.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class ScriptConsole {

  /** The singleton instance of this class. */
  private static ScriptConsole instance = null;

  /** The frame used to display the scripting console. */
  private JFrame scriptFrame;

  ////////////////////////////////////////////////////////////

  private ScriptConsole () {
  
    // Create content panel
    // --------------------
    JPanel contentPanel = new JPanel (new BorderLayout());
    contentPanel.setBorder ((Border) UIManager.get ("OptionPane.border"));

    // Add console
    // -----------
    JConsole console = new JConsole();
    contentPanel.add (console, BorderLayout.CENTER);

    // Add close button
    // ----------------
    Box buttonBox = Box.createHorizontalBox();
    buttonBox.setBorder ((Border) UIManager.get ("OptionPane.buttonAreaBorder"));
    buttonBox.add (Box.createHorizontalGlue());
    JButton closeButton = new JButton ("Close");
    closeButton.addActionListener (new ActionListener () {
      public void actionPerformed (ActionEvent event) {
        scriptFrame.setVisible (false);
      } // actionPerformed
    });
    buttonBox.add (closeButton);
    contentPanel.add (buttonBox, BorderLayout.SOUTH);

    // Create frame
    // ------------
    scriptFrame = new JFrame ("Script Console");
    scriptFrame.setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);
    scriptFrame.setContentPane (contentPanel);
    scriptFrame.pack();
    scriptFrame.setSize (new Dimension (640, 480));

    // Create interpreter
    // ------------------
    Interpreter interpreter = new Interpreter (console);
    interpreter.setExitOnEOF (false);
    Thread thread = new Thread (interpreter);
    thread.start();
  
  } // ScriptConsole constructor

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static ScriptConsole getInstance () {
  
    if (instance == null) instance = new ScriptConsole();
    return (instance);
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Shows the console relative to the specified parent component.
   *
   * @param parent the component to use for the placement of the console, or
   * null for relative to the screen.
   */
  public void showRelativeTo (
    Component parent
  ) {

    // Show script frame
    // -----------------
    if (!scriptFrame.isVisible()) {
      scriptFrame.setLocationRelativeTo (parent);
      SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          scriptFrame.setVisible (true);
        } // run
      });
    } // if
    
  } // showRelativeTo

  ////////////////////////////////////////////////////////////

} // ScriptConsole class

////////////////////////////////////////////////////////////////////////
