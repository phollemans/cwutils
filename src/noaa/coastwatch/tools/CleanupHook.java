////////////////////////////////////////////////////////////////////////
/*
     FILE: CleanupHook.java
  PURPOSE: To clean up after the Java VM exits.
   AUTHOR: Peter Hollemans
     DATE: 2005/01/30
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// -------
import java.io.*;
import java.util.*;

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

  // Variables
  // ---------

  /** The set of files to delete. */
  private Set deleteSet;

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
      System.out.println (this.getClass() + 
        ": Caught Java VM exit with cleanup required");
    } // if

    // Perform file deletions
    // ----------------------
    for (Iterator iter = deleteSet.iterator(); iter.hasNext(); ) {
      String fileName = (String) iter.next();
      File file = new File (fileName);
      if (file.exists()) {
        System.out.println (this.getClass() + ": Removing " + fileName);
        try { file.delete(); }
        catch (Exception e) { }
      } // if
    } // for

  } // run

  ////////////////////////////////////////////////////////////

} // CleanupHook

////////////////////////////////////////////////////////////////////////
