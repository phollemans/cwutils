////////////////////////////////////////////////////////////////////////
/*

     File: UpdateAgent.java
   Author: Peter Hollemans
     Date: 2004/06/18

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
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import noaa.coastwatch.net.UpdateCheck;

/**
 * The <code>UpdateAgent</code> class performs an update check on the
 * network, then pops up an informational dialog if the software is
 * out of date.  The update check is started asynchronously when the
 * window is made visible for the first time so that no delay occurs.
 *
 * @see noaa.coastwatch.net.UpdateCheck
 *
 * @author Peter Hollemans
 * @since 3.1.7
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
