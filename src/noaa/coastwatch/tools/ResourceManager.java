////////////////////////////////////////////////////////////////////////
/*

     File: ResourceManager.java
   Author: Peter Hollemans
     Date: 2004/05/18

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
package noaa.coastwatch.tools;

// Imports
// -------
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.awt.Desktop;

import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.open.ServerTableModel;
import noaa.coastwatch.io.SerializedObjectManager;
import noaa.coastwatch.render.OverlayGroupManager;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.tools.Preferences;

import java.util.logging.Logger;
import java.util.logging.Level;

/** 
 * The <code>ResourceManager</code> class stores and retrieves
 * user-specific resources related to configuration settings,
 * palettes, overlays, and so on.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ResourceManager {

  private static final Logger LOGGER = Logger.getLogger (ResourceManager.class.getName());

  // Constants
  // ---------

  /** The user-specific base directory for resources. */
  private static final String RESOURCE_BASE = getAbsoluteResourceBase();

  /** The palette resource directory. */
  private static final String PALETTES_DIR_NAME = "palettes";
  private static final File PALETTES_DIR = new File (
    RESOURCE_BASE + File.separator + PALETTES_DIR_NAME);

  /** The overlay resource directory. */
  private static final String OVERLAYS_DIR_NAME = "overlays";
  private static final File OVERLAYS_DIR = new File (
    RESOURCE_BASE + File.separator + OVERLAYS_DIR_NAME);

  /** The preferences resource directory. */
  private static final File PREFERENCES_DIR = new File (RESOURCE_BASE);

  /** The preferences file. */
  private static final String PREFERENCES_FILE = "prefs.xml";

  /** The Java VM options file. */
  private static final String VMOPTIONS_FILE = "cwutils.vmoptions";

  // Variables
  // ---------

  /** The preferences for user-specific settings. */
  private static Preferences preferences;

  /** The overlay group file names. */
  private static String[] overlayFiles;

  ////////////////////////////////////////////////////////////

  static {

    // Find and list the serialized overlay group files in the jar file.

    var jar = ResourceManager.class.getProtectionDomain().getCodeSource().getLocation();
    var overlayFileList = new ArrayList<String>();
    try (var jarFile = new JarFile (new File (jar.toURI()))) {
      jarFile.stream()
        .filter (entry -> entry.getName().matches (".*/" + OVERLAYS_DIR_NAME + "/.*\\.jso"))
        .forEach (entry -> { 
          var fileName = entry.getName();
          fileName = fileName.substring (fileName.lastIndexOf ("/") + 1);
          overlayFileList.add (fileName);
          LOGGER.fine ("Found overlay group file " + entry.getName());
        });
    } // try
    catch (Exception e) {
      LOGGER.log (Level.FINE, "Error getting jar entries", e);
    } // catch

    overlayFiles = overlayFileList.toArray (new String[0]);

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Gets the OS-specific absolute resource base directory.
   *
   * @return the directory off the user's home directory where
   * resources should be placed.
   *
   * @since 3.3.1
   */
  private static String getAbsoluteResourceBase () {

    String base;
  
    if (GUIServices.IS_WIN) {
      base =
        System.getenv ("APPDATA") +
        File.separator + "CoastWatch";
    } // if
    else if (GUIServices.IS_MAC) {
      base =
        System.getProperty ("user.home") +
        File.separator + "Library" +
        File.separator + "Application Support" +
        File.separator + "CoastWatch";
    } // else if
    else {
      base =
        System.getProperty ("user.home") +
        File.separator + ".coastwatch";
    } // else

    return (base);

  } // getAbsoluteResourceBase

  ////////////////////////////////////////////////////////////

  /**
   * Gets the OS-specific resource base directory.
   *
   * @return the directory off the user's home directory where
   * resources should be placed.
   *
   * @deprecated As of version 3.3.1, replaced by 
   * {@link #getAbsoluteResourceBase}.
   */
  @Deprecated
  private static String getResourceBase () {

    if (GUIServices.IS_WIN)
      return ("Application Data" + File.separator + "CoastWatch");
    else if (GUIServices.IS_MAC)
      return ("Library" + File.separator + "Application Support" + 
        File.separator + "CoastWatch");
    else
      return (".coastwatch");

  } // getResourceBase

  ////////////////////////////////////////////////////////////

  /**
   * Sets up the user-specified palettes.  If the palette resource
   * directory does not exist, it is created depending on the specified
   * creation flag.  If the directory already exists and contains user-defined
   * palettes, they are added to the palatte factory.
   *
   * @param create true to create the diretory if it doesn't exist, or false
   * to not create the directory.
   *
   * @throws IOException if an error occurred setting up the palettes.
   *
   * @see noaa.coastwatch.render.PaletteFactory#getPredefined
   * @see #setupPalettes()
   *
   * @since 3.4.1
   */
  public static void setupPalettes (
    boolean create
  ) throws IOException {

    // Create palette directory
    // ------------------------
    boolean dirExists = PALETTES_DIR.exists();
    if (!dirExists && create) {
      if (!PALETTES_DIR.mkdirs())
        throw new IOException ("Cannot create resource directory " + PALETTES_DIR);
      dirExists = true;
    } // if

    // Add palette files to factory
    // ----------------------------
    if (dirExists) PaletteFactory.addPredefined (PALETTES_DIR);

  } // setupPalettes

  ////////////////////////////////////////////////////////////

  /** 
   * Sets up the user-specified palettes.  If the palette resource
   * directory does not exist, it is created.  Any user-defined
   * palettes found in the directory are added to the palatte factory.
   *
   * @throws IOException if an error occurred setting up the palettes.
   *
   * @see noaa.coastwatch.render.PaletteFactory#getPredefined
   * @see #setupPalettes(boolean)
   */
  public static void setupPalettes () throws IOException {

    setupPalettes (true);
    
  } // setupPalettes

  ////////////////////////////////////////////////////////////

  /**
   * Copies data from an input stream to an output stream.
   *
   * @param input the input stream to read.
   * @param output the output stream to write.
   * @param doClose true to close input and output after copying.
   * 
   * @throws IOException if an error occurred reading or writing data.
   */
  public static void copyStream (
    InputStream input,
    OutputStream output,
    boolean doClose
  ) throws IOException {

    byte[] buffer = new byte[512];
    int length;
    while ((length = input.read (buffer)) != -1) {
      output.write (buffer, 0, length);
    } // while
    if (doClose) { 
      input.close();
      output.close();
    } // if

  } // copy

  ////////////////////////////////////////////////////////////

  /**
   * Checks any existing resources for recoverable problems.  If
   * changes were made to the resources, a message is returned stating
   * the changes, otherwise null is returned.
   *
   * @return the result message or null for no results.
   *
   * @throws IOException if an error occurred checking the resources.
   */
  public static String checkResources () throws IOException {

    // Check overlays directory
    // ------------------------
    if (OVERLAYS_DIR.exists()) {
      boolean moveDir = false;

      // Check for old files
      // -------------------
      String[] list = OVERLAYS_DIR.list();
      if (list == null) {
        throw new IOException ("Cannot list files in overlays " +
          "directory '" + OVERLAYS_DIR + "'");
      } // if
      for (int i = 0; i < list.length; i++) {
        if (list[i].endsWith (".sogz")) { moveDir = true; break; }
      } // for

      // Check for incompatible files
      /// ---------------------------
      if (!moveDir) {
        var groupList = new OverlayGroupManager (OVERLAYS_DIR).getGroups();
        SerializedObjectManager manager = new SerializedObjectManager (OVERLAYS_DIR);
        for (var iter = groupList.iterator(); iter.hasNext();) {
          try { manager.loadObject (iter.next()); }
          catch (Exception e) { 
            LOGGER.log (Level.FINE, "Error loading group object", e);
            moveDir = true; 
            break; 
          } // catch
        } // for
      } // if

      // Move incompatible directory
      // ---------------------------
      if (moveDir) {
        File newDir = new File (OVERLAYS_DIR + "_" + new Date().getTime());
        if (!OVERLAYS_DIR.renameTo (newDir)) {
          throw new IOException (
            "An error occurred removing an incompatible overlay group\n" +
            "directory from a previous software version:\n" + 
            "    " + OVERLAYS_DIR + "\n" +
            "You must remove the directory manually and then restart."
          );
        } // if
        return (
          "The overlay groups used in this version are incompatible with\n" +
          "those saved by previous versions.  The overlay group directory:\n" +
          "    " + OVERLAYS_DIR + "\n" +
          "has been renamed to:\n" + 
          "    " + newDir + "\n" +
          "A new overlay group directory will be created and populated\n" +
          "with a default set of overlay groups."
        );
      } // if

    } // if

    return (null);

  } // checkResources

  ////////////////////////////////////////////////////////////

  /** 
   * Restores the default initial overlays.  The user resource directory
   * must already exist.
   *
   * @throws IOException if an error occurred restoring the overlays.
   * 
   * @since 3.8.1
   */
  public static void restoreOverlays () throws IOException {

    // Check if the resources directory already exists.  We need this 
    // to restore into. 
    if (!OVERLAYS_DIR.exists()) 
      throw new IOException ("Resource directory '" + OVERLAYS_DIR + "' does not exist");

    // Restore the default set of overlay groups.  We delete and overwrite any 
    // overlay groups that already exist.
    for (int i = 0; i < overlayFiles.length; i++) {
      var file = new File (OVERLAYS_DIR, overlayFiles[i]);
      if (file.exists()) { if (!file.delete()) throw new IOException ("Cannot delete file '" + file + "'"); }
      copyStream (
        OverlayGroupManager.class.getResourceAsStream (OVERLAYS_DIR_NAME + "/" + overlayFiles[i]),
        new FileOutputStream (file),
        true
      );
    } // for

  } // restoreOverlays

  ////////////////////////////////////////////////////////////

  /** 
   * Sets up the user-specified overlays.  If the overlay resource
   * directory does not exist, it is created and populated with
   * default initial overlays.
   *
   * @throws IOException if an error occurred setting up the overlays.
   */
  public static void setupOverlays () throws IOException {

    // TODO: Add overlays from the default CDAT set that the user
    // doesn't already have.  But, what happens when a user wants to
    // delete an overlay group on purpose, we don't want to put it
    // back again.  What about separate overlay groups for the system
    // and for users?  This same problem applies to palettes,
    // preferences, OPeNDAP directories, etc.  Oh darn.

    // Create overlays directory
    // -------------------------
    if (!OVERLAYS_DIR.exists()) {

      // Create overlay directory
      // ------------------------
      if (!OVERLAYS_DIR.mkdirs())
        throw new IOException ("Cannot create resource directory " + 
          OVERLAYS_DIR);

      // Copy default overlays 
      // ---------------------
      for (int i = 0; i < overlayFiles.length; i++) {
        copyStream (
          OverlayGroupManager.class.getResourceAsStream (OVERLAYS_DIR_NAME + "/" + overlayFiles[i]),
          new FileOutputStream (new File (OVERLAYS_DIR, overlayFiles[i])),
          true
        );
      } // for

    } // if

  } // setupOverlays

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an overlay group manager instance that reads overlays
   * from the user-specific resources.  User objects can keep up
   * to date on the current list of groups by listening for
   * change events from the overlay manager.
   */
  public static OverlayGroupManager getOverlayManager () {

    try {

      // Setup overlays if needed
      // ------------------------
      if (!OVERLAYS_DIR.exists())
        setupOverlays();

      // Create overlay manager
      // ----------------------
      return (new OverlayGroupManager (OVERLAYS_DIR));

    } // try

    catch (IOException e) {
      throw new RuntimeException ("Cannot create overlay manager: " + 
        e.getMessage());
    } // catch

  } // getOverlayManager

  ////////////////////////////////////////////////////////////

  /**
   * Sets up the user-specified preferences.
   *
   * @throws IOException if an error occurred setting up the
   * preferences.  
   */
  public static void setupPreferences () throws IOException {

    // Create preferences directory
    // ----------------------------
    if (!PREFERENCES_DIR.exists()) {
      if (!PREFERENCES_DIR.mkdirs())
        throw new IOException ("Cannot create resource directory " + 
          PREFERENCES_DIR);
    } // if

    // Copy default preferences 
    // ------------------------
    File prefsFile = new File (PREFERENCES_DIR, PREFERENCES_FILE);
    if (!prefsFile.exists()) {
      copyStream (
        ResourceManager.class.getResourceAsStream (PREFERENCES_FILE),
        new FileOutputStream (prefsFile),
        true
      );
    } // if

  } // setupPreferences

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the preferences using the specified object.  Subsequent
   * calls to <code>getPreferences()</code> will return the new
   * preferences.  The preferences are also saved to the
   * user-specified resources.
   *
   * @throws RuntimeException if an error occurred writing the
   * preferences to disk.
   */
  public static void setPreferences (
    Preferences preferences
  ) {

    try {
      preferences.write (new FileOutputStream (
        new File (PREFERENCES_DIR, PREFERENCES_FILE)));
      preferences.writeOptions (new FileOutputStream (
        new File (PREFERENCES_DIR, VMOPTIONS_FILE)));
    } // try
    catch (IOException e) {
      throw new RuntimeException ("Cannot write preferences file: " + 
        e.getMessage());
    } // catch
    ResourceManager.preferences = preferences;

  } // setPreferences

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a preferences instance from the user-specific resources.
   * The same preferences will be returned each time this method is
   * invoked so that user objects may share the preferences.
   *
   * @throws RuntimeException if an error occurred setting up the
   * initial preferences, or reading the preferences from disk.
   */
  public static Preferences getPreferences () { 

    try {

      // Setup preferences on disk
      // -------------------------
      setupPreferences();

      // Create preferences
      // ------------------
      if (preferences == null) {
        preferences = new Preferences (new FileInputStream (
          new File (PREFERENCES_DIR, PREFERENCES_FILE)));
        /**
         * Note that we specifically DO NOT read the vmoptions file
         * here to get the options, unlike above where we write the
         * options to a file.  That's because the vmoptions file
         * is secondary to the preferences XML file, and only needed
         * by the Java launch code to pass to the VM on startup.
         */
      } // if

      return (preferences);

    } // try

    catch (IOException e) {
      throw new RuntimeException ("Cannot read preferences file: " + 
        e.getMessage());
    } // catch

  } // getPreferences

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the default set of preferences.
   * 
   * @return the default preferences from when the software is installed.
   *
   * @throws RuntimeException if an error occurred reading the default 
   * preferences.
   * 
   * @since 4.1.5
   */
  public static Preferences getDefaultPreferences () { 

    Preferences prefs = null;

    try {
      prefs = new Preferences (ResourceManager.class.getResourceAsStream (PREFERENCES_FILE));
    } // try
    catch (IOException e) {
      throw new RuntimeException ("Cannot read preferences file: " + e.getMessage());
    } // catch

    return (prefs);

  } // getDefaultPreferences

  ////////////////////////////////////////////////////////////

  /** 
   * Shows the user resources directory in the OS native file explorer.
   * 
   * @since 3.8.1
   */
  public static void showResourcesDirectory () throws IOException {

    Desktop.getDesktop().open (PREFERENCES_DIR);

  } // showResourcesDirectory

  ////////////////////////////////////////////////////////////

} // ResourceManager class

////////////////////////////////////////////////////////////////////////
