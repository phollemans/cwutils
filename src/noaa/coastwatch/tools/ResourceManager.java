////////////////////////////////////////////////////////////////////////
/*
     FILE: ResourceManager.java
  PURPOSE: To manage user-specific resources.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/18
  CHANGES: 2005/07/07, PFH, added checkResources()
           2006/02/28, PFH, added Coral Reef Watch overlays
           2006/10/27, PFH, modified base resource directory to be OS-specific
           2006/12/29, PFH, made overlay manager non-static
           2007/08/03, PFH, made feedback from checkResources() more verbose
           2007/12/19, PFH, modified comments for getOverlayManager()
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// -------
import java.util.*;
import java.io.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.gui.open.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.render.*;

/** 
 * The <code>ResourceManager</code> class stores and retrieves
 * user-specific resources related to configuration settings,
 * palettes, overlays, and so on.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ResourceManager {

  // Constants
  // ---------

  /** The user-specific base directory for resources. */
  private static final String RESOURCE_BASE = 
    System.getProperty ("user.home") + File.separator + getResourceBase();

  /** The palette resource directory. */
  private static final File PALETTES_DIR = new File (
    RESOURCE_BASE + File.separator + "palettes");

  /** The overlay resource directory. */
  private static final File OVERLAYS_DIR = new File (
    RESOURCE_BASE + File.separator + "overlays");

  /** The preferences resource directory. */
  private static final File PREFERENCES_DIR = new File (RESOURCE_BASE);

  /** The preferences file. */
  private static final String PREFERENCES_FILE = "prefs.xml";

  /** The OPeNDAP server file. */
  private static final String OPENDAP_FILE = "opendap_servers.xml";

  /** The default overlays. */
  private static final String[] OVERLAY_FILES = new String[] {
    "Atmospheric.jso",
    "Oceanographic - Cloud Analysis.jso",
    "Oceanographic.jso",
    "Oceanographic - Coral Reef Watch.jso"
  };

  // Variables
  // ---------

  /** The preferences for user-specific settings. */
  private static Preferences preferences;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the OS-specific resource base directory.
   *
   * @return the directory off the user's home directory where
   * resources should be placed.
   */
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
   * directory does not exist, it is created.  Any user-defined
   * palettes found in the directory are added to the palatte factory.
   *
   * @throws IOException if an error occurred setting up the palettes.
   *
   * @see noaa.coastwatch.render.PaletteFactory#getPredefined
   */
  public static void setupPalettes () throws IOException {

    // Create palette directory
    // ------------------------
    if (!PALETTES_DIR.exists()) {
      if (!PALETTES_DIR.mkdirs())
        throw new IOException ("Cannot create resource directory " + 
          PALETTES_DIR);
    } // if

    // Add palette files to factory
    // ----------------------------
    PaletteFactory.addPredefined (PALETTES_DIR);

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
        List groupList = new OverlayGroupManager (OVERLAYS_DIR).getGroups();
        SerializedObjectManager manager = 
          new SerializedObjectManager (OVERLAYS_DIR);
        for (Iterator iter = groupList.iterator(); iter.hasNext();) {
          try { manager.loadObject ((String) iter.next()); }
          catch (Exception e) { moveDir = true; break; }
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
      for (int i = 0; i < OVERLAY_FILES.length; i++) {
        copyStream (
          OverlayGroupManager.class.getResourceAsStream (OVERLAY_FILES[i]),
          new FileOutputStream (new File (OVERLAYS_DIR, OVERLAY_FILES[i])),
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
   * Sets up the user-specified OPeNDAP servers.
   *
   * @throws IOException if an error occurred setting up the servers.
   */
  public static void setupOpendap () throws IOException {

    // Create preferences directory
    // ----------------------------
    if (!PREFERENCES_DIR.exists()) {
      if (!PREFERENCES_DIR.mkdirs())
        throw new IOException ("Cannot create resource directory " + 
          PREFERENCES_DIR);
    } // if

    // Copy default preferences 
    // ------------------------
    File opendapFile = new File (PREFERENCES_DIR, OPENDAP_FILE);
    if (!opendapFile.exists()) {
      copyStream (
        ServerTableModel.class.getResourceAsStream (OPENDAP_FILE),
        new FileOutputStream (opendapFile),
        true
      );
    } // if

  } // setupOpendap

  ////////////////////////////////////////////////////////////
  
  /**
   * Gets the list of OPeNDAP servers from the user-specified
   * resources.
   *
   * @return the list of {@link
   * noaa.coastwatch.gui.open.ServerTableModel.Entry} objects.
   *
   * @throws RuntimeException if an error occurred setting up the
   * initial list, or reading the list from disk.
   */
  public static List getOpendapList () {

    try {

      // Setup OPeNDAP servers on disk
      // -----------------------------
      setupOpendap();

      // Get list
      // --------
      File opendapFile = new File (PREFERENCES_DIR, OPENDAP_FILE);
      InputStream stream = new FileInputStream (opendapFile);
      return (ServerTableModel.readList (stream));

    } // try

    catch (IOException e) {
      throw new RuntimeException ("Cannot read OPeNDAP server list: " + 
        e.getMessage());
    } // catch

  } // getOpendapList

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the OPeNDAP server list using the specified list of {@link
   * noaa.coastwatch.gui.open.ServerTableModel.Entry} objects.
   * Subsequent calls to <code>getOpendapList()</code> will return the
   * new list.  The list is also saved to the user-specified
   * resources.
   *
   * @throws RuntimeException if an error occurred writing the
   * list to disk.
   */
  public static void setOpendapList (
    List opendapList
  ) {

    try {
      ServerTableModel.writeList (new FileOutputStream (
        new File (PREFERENCES_DIR, OPENDAP_FILE)), opendapList);
    } // try
    catch (IOException e) {
      throw new RuntimeException ("Cannot write OPeNDAP server list: " + 
        e.getMessage());
    } // catch

  } // setOpendapList

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
      } // if

      return (preferences);

    } // try

    catch (IOException e) {
      throw new RuntimeException ("Cannot read preferences file: " + 
        e.getMessage());
    } // catch

  } // getPreferences

  ////////////////////////////////////////////////////////////

} // ResourceManager class

////////////////////////////////////////////////////////////////////////
