////////////////////////////////////////////////////////////////////////
/*

     File: CleanupHook.java
   Author: Peter Hollemans
     Date: 2005/01/30

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.tools;

// Imports
// -------
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The <code>CleanupHook</code> class may be used by programs to clean
 * up resources after the Java VM has exited.  The hook has only one
 * instance, retrieved using the {@link #getInstance} method, and
 * registers itself to be run using the {@link
 * java.lang.Runtime#addShutdownHook} method.  Currently, the only
 * cleanup task is file deletion.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class CleanupHook 
  implements Runnable {

  private static final Logger LOGGER = Logger.getLogger (CleanupHook.class.getName());

  // Variables
  // ---------

  /** The set of files to delete. */
  private Set<String> deleteSet;

  /** The single instance of this class. */
  private static CleanupHook instance;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new hook.  Public users of this class should call the
   * {@link #getInstance} method.
   */
  private CleanupHook () {

    deleteSet = new HashSet();

  } // CleanupHook constructor

  ////////////////////////////////////////////////////////////

  /** Gets the one and only instance of this class. */
  public static CleanupHook getInstance () {

    if (instance == null) {
      instance = new CleanupHook();
      Runtime.getRuntime().addShutdownHook (new Thread (instance));
    } // if
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Adds a file to the set of files to delete upon cleanup.  If the
   * file was already scheduled for deletion, no action is performed.
   *
   * @param fileName the file name to add.
   */
  public void scheduleDelete (
    String fileName
  ) {

    deleteSet.add (fileName);

  } // scheduleDelete

  ////////////////////////////////////////////////////////////

  /**
   * Adds a file to the set of files to delete upon cleanup.  If the
   * file was already scheduled for deletion, no action is performed.
   *
   * @param file the file to add.
   */
  public void scheduleDelete (
    File file
  ) {

    deleteSet.add (file.getPath());

  } // scheduleDelete

  ////////////////////////////////////////////////////////////

  /**
   * Removes a file from the set of files to delete upon cleanup.  If
   * the file was never scheduled for deletion, no action is
   * performed.
   *
   * @param fileName the file name to remove.
   */
  public void cancelDelete (
    String fileName
  ) {

    deleteSet.remove (fileName);

  } // cancelDelete

  ////////////////////////////////////////////////////////////

  /**
   * Removes a file from the set of files to delete upon cleanup. If
   * the file was never scheduled for deletion, no action is
   * performed.
   *
   * @param file the file to remove.
   */
  public void cancelDelete (
    File file
  ) {

    deleteSet.remove (file.getPath());

  } // cancelDelete

  ////////////////////////////////////////////////////////////

  /**
   * Performs the cleanup.  This method is normally only called in
   * response to a system shutdown.
   */
  public void run () {

    // Detect if cleanup is needed
    // ---------------------------
    boolean isNeeded = !deleteSet.isEmpty();
    if (isNeeded) {

      LOGGER.warning ("Caught command exit, cleaning up ...");

      // Perform file deletions
      // ----------------------
      for (String fileName : deleteSet) {
        File file = new File (fileName);
        if (file.exists()) {
          LOGGER.warning ("Removing " + fileName);
          try { file.delete(); }
          catch (Exception e) { }
        } // if
      } // for

      // Clear the list
      // --------------
      deleteSet.clear();
      
    } // if

  } // run

  ////////////////////////////////////////////////////////////

} // CleanupHook

////////////////////////////////////////////////////////////////////////
