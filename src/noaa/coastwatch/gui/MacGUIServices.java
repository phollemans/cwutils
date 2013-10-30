////////////////////////////////////////////////////////////////////////
/*
     FILE: MacGUIServices.java
  PURPOSE: A class to perform various static Mac GUI-related functions.
   AUTHOR: Peter Hollemans
     DATE: 2006/04/14
  CHANGES: 2012/08/30, PFH, updated for OS X 10.5 and up

  CoastWatch Software Library and Utilities
  Copyright 1998-2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.util.*;
import java.io.*;
import java.awt.event.*;
import com.apple.eawt.*;

/**
 * The <code>MacGUIServices</code> class defines various static
 * methods relating to the Mac graphical user interfaces.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
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
