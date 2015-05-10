////////////////////////////////////////////////////////////////////////
/*
     FILE: UpdateCheck.java
  PURPOSE: To check for updates to the CWF software online.
   AUTHOR: Peter Hollemans
     DATE: 2004/06/18
  CHANGES: 2004/11/18, PFH, modified to generate unique user ID
           2007/06/19, PFH, updated to use ToolServices.getVersion()

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.net;

// Imports
// -------
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.prefs.Preferences;
import noaa.coastwatch.tools.ToolServices;

/**
 * The <code>UpdateCheck</code> class checks for updates of the
 * CoastWatch utilities software package online.  The check generates
 * a message if an update is available.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class UpdateCheck {
  
  // Constants
  // ---------

  /** The server URL to check for updates. */
  private static final String SERVER_URL = 
    "http://coastwatch.noaa.gov/cw_cwfv3.html";

  /** The update string. */
  private static final String UPDATE_STRING = "cwf_update=";

  /** The update persistant cache key. */
  private static final String UPDATE_KEY = "cwf_update_id";

  // Variables
  // ---------

  /** The update message, or null if none exists. */
  private String message;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the update message, or null if there is no update message
   * available.
   */
  public String getMessage () { return (message); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a user-persistent ID to use for making update requests.
   */
  private int getUserID () {

    // Check for ID in persistent cache
    // --------------------------------
    Preferences prefs = Preferences.userNodeForPackage (this.getClass());
    int id = prefs.getInt (UPDATE_KEY, 0);

    // Create ID if not found
    // ----------------------
    if (id == 0) {
      String idString = 
        System.getProperty ("os.name") +
        System.getProperty ("os.arch") +
        System.getProperty ("os.version") +
        System.getProperty ("user.name") +
        new Date().toString();
      id = idString.hashCode();
      prefs.putInt (UPDATE_KEY, id);
    } // if

    return (id);

  } // getUserID

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new update check which checks a pre-defined server for
   * a software update.
   *
   * @param tool the name of the tool to check for an update.
   */
  public UpdateCheck (
    String tool
  ) {
                            
    try {

      // Create reader from URL
      // ----------------------
      URL url = new URL (SERVER_URL);
      InputStream in = url.openStream();
      BufferedReader reader = new BufferedReader (new InputStreamReader (in));
      String line;

      // Search for update URL
      // ---------------------
      String updateURL = null;
      while ((line = reader.readLine ()) != null) {
        if (line.indexOf (UPDATE_STRING) != -1) {
          updateURL = line.replaceAll (".*" + UPDATE_STRING + "\"(.*)\".*",
            "$1");
          break;
        } // if
      } // while
      reader.close();
      if (updateURL == null) return;

      // Get update message
      // ------------------
      updateURL = updateURL +
        "?os=" + URLEncoder.encode (System.getProperty ("os.name"), "UTF-8") +
        "&arch=" + URLEncoder.encode (System.getProperty ("os.arch"), "UTF-8")+
        "&tool=" + URLEncoder.encode (tool, "UTF-8") +
        "&version=" + URLEncoder.encode (ToolServices.getVersion(), "UTF-8") +
        "&userid="+URLEncoder.encode (Integer.toString (getUserID()), "UTF-8");
      StringBuffer messageBuffer = new StringBuffer();
      reader = new BufferedReader (
        new InputStreamReader (new URL (updateURL).openStream()));
      while ((line = reader.readLine ()) != null) {
        messageBuffer.append (line);
        messageBuffer.append ("\n");
      } // while
      reader.close();

      message = messageBuffer.toString();

    } catch (Exception e) {
      /**
       * Do nothing here.  If there is some problem with the
       * connection or the server is not responding, we'll just assume
       * that there are no updates available and leave the message
       * null.
       */
    } // catch

  } // UpdateCheck constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {

    UpdateCheck check = new UpdateCheck ("test");
    System.out.println ("message = \n" + check.getMessage());

  } // main

  ////////////////////////////////////////////////////////////

} // UpdateCheck class

////////////////////////////////////////////////////////////////////////
