////////////////////////////////////////////////////////////////////////
/*
     FILE: UpdateAgent.java
  PURPOSE: To show an update dialog when the software is out of date.
   AUTHOR: Peter Hollemans
     DATE: 2004/06/18
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
import java.awt.event.*;
import javax.swing.*;
import noaa.coastwatch.net.*;

/**
 * The <code>UpdateAgent</code> class performs an update check on the
 * network, then pops up an informational dialog if the software is
 * out of date.  The update check is started asynchronously when the
 * window is made visible for the first time so that no delay occurs.
 *
 * @see noaa.coastwatch.net.UpdateCheck
 */
public class UpdateAgent
  extends WindowAdapter {

  // Variables
  // ---------

  /** The tool name for the update check. */
  private String tool;

  ////////////////////////////////////////////////////////////

  /** Creates a new update agent for the specified tool name. */
  public UpdateAgent (
    String tool
  ) {

    this.tool = tool;

  } // UpdateAgent

  ////////////////////////////////////////////////////////////

  /**
   * Creates an update check in response to the window being opened
   * for the first time.
   *
   * @param event the window open event.
   */
  public void windowOpened (
    WindowEvent event
  ) { 

    final Window window = event.getWindow();
    Thread worker = new Thread () {
      public void run () {

        // Get update message
        // ------------------
        UpdateCheck check = new UpdateCheck (tool);
        final String message = check.getMessage();

        // Show dialog
        // -----------
        if (message != null && message.indexOf ("No update") == -1) {
          SwingUtilities.invokeLater (new Runnable () {
              public void run () {
                JOptionPane.showMessageDialog (window, message,
                  "Update", JOptionPane.INFORMATION_MESSAGE);
              } // run
            });
        } // if

      } // run
    };
    worker.start();

  } // windowOpened

  ////////////////////////////////////////////////////////////

} // UpdateAgent class

////////////////////////////////////////////////////////////////////////
