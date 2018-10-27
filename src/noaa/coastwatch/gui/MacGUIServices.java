////////////////////////////////////////////////////////////////////////
/*

     File: MacGUIServices.java
   Author: Peter Hollemans
     Date: 2006/04/14

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import com.apple.eawt.AppEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 * The <code>MacGUIServices</code> class defines various static
 * methods relating to the Mac graphical user interfaces.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 *
 * @deprecated Since version 3.4.1, this class is not needed and the
 * functionality is replaced by cross platform functionality in
 * {@link GUIServices#addOpenFileListener}.
 */
@Deprecated
public class MacGUIServices {

  ////////////////////////////////////////////////////////////

  /**
   * Adds a listener that executes when a file is opened on a Mac via
   * the Finder (for example by double-clicking).
   *
   * @param listener the listener to call.
   */
  public static void addOpenFileListener (
    final ActionListener listener
  ) {

    Application.getApplication().setOpenFileHandler (
      new OpenFilesHandler() {
        public void openFiles (AppEvent.OpenFilesEvent e) {
          List<File> files = e.getFiles();
          if (!files.isEmpty()) {
            String name = files.get (0).toString();
            listener.actionPerformed (new ActionEvent (this, 0, name));
          } // if
        } // openFiles
      });

    Application.getApplication().setQuitHandler (
      new QuitHandler() {
        public void handleQuitRequestWith (
          AppEvent.QuitEvent e,
          QuitResponse response
        ) {
          response.performQuit();
        } // handleQuitRequestWith
      });

  } // addOpenFileListener

  ////////////////////////////////////////////////////////////

  private MacGUIServices () { }

  ////////////////////////////////////////////////////////////

} // MacGUIServices class

////////////////////////////////////////////////////////////////////////
