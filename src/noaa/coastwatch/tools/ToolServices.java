////////////////////////////////////////////////////////////////////////
/*

     File: ToolServices.java
   Author: Peter Hollemans
     Date: 2002/12/17

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.io.IOServices;

/**
 * The tool services class defines various static methods relating
 * to the CoastWatch tools.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class ToolServices {

  // Constants
  // ---------
  /** The full package description. */
  public static final String PACKAGE = 
    "CoastWatch Software Library and Utilities";

  /** The short package description. */
  public static final String PACKAGE_SHORT = "cwutils";

  /** The author. */
  public static final String AUTHOR = "Peter Hollemans";

  /** The support email address. */
  public static final String SUPPORT = "coastwatch.info@noaa.gov";

  /** The software website. */
  public static final String WEBSITE = "http://coastwatch.noaa.gov";

  /** The software copyright. */
  public static final String COPYRIGHT = 
    "(c) 1998-2018 National Oceanic and Atmospheric Administration";

  /** The software copyright (short version). */
  public static final String COPYRIGHT_SHORT = COPYRIGHT;

  /** The tool parameter splitting expression. */
  public static final String SPLIT_REGEX = "[,/]";

  /** The standard error dialog instructions string. */
  public static final String ERROR_INSTRUCTIONS = 
    "If the message above is an error and not simply a warning, please copy " +
    "the text and send an email to " + SUPPORT + ", along " +
    "with a description of the actions that led to the error.  This " +
    "valuable input will help to improve future versions of the software.";

  /** The standard size of the help dialog. */
  public static final Dimension HELP_DIALOG_SIZE = new Dimension (640, 480);

  /** The space string for About style info. */
  private static final String SPACE = " ";

  // Variables
  // ---------

  /** The current tool command line. */
  private static String commandLine;

  /** The package version number. */
  private static String version;
  
  /** The current splitting regular expression. */
  private static String splitRegex = SPLIT_REGEX;

  ////////////////////////////////////////////////////////////

  /** Performs static tool settings. */
  static {

    /**
     * For now, we set the locale to US.  This is useful because there
     * are some places where we format and parse numbers in a
     * locale-specific way, and others that we don't and this can
     * cause problems, for example in parsing numbers with commas in
     * them rather than decimals.  So really, this situation should be
     * remedied by correct formatting and parsing, and by using
     * locale-specific labels.  But that would be a lot of work, so
     * for now we just use English/US conventions and hope that most
     * scientists speak English and recognized English number formats.
     */
    Locale.setDefault (Locale.US);

    // Read version number
    // -------------------
    InputStream stream = ClassLoader.getSystemResourceAsStream (
      "version.properties");
    if (stream != null) {
      Properties props = new Properties();
      try { 
        props.load (stream); 
        version = props.getProperty ("cwutils.version");
      } // try
      catch (IOException e) { }
      finally { 
        try { stream.close(); }
        catch (IOException e) { }
      } // finally
    } // if
    if (version == null) 
      throw new RuntimeException ("Cannot determine version number");

  } // static

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an about string appropriate for an about dialog box.
   * 
   * @param tool the tool or program name.
   *
   * @return the about string.
   */
  public static String getAbout (
    String tool
  ) {

    String os = 
      System.getProperty ("os.name") + " " + 
      System.getProperty ("os.version") + " " + 
      System.getProperty ("os.arch");
    String jvm = System.getProperty ("java.version") + " on " + os;
    String about = 
      "<html>" +
      "<table>" +
      "<tr><td><b>Program:</b></td><td>" + SPACE + tool + "</td></tr>" +
      "<tr><td><b>Package:</b></td><td>" + SPACE + PACKAGE + "</td></tr>" +
      "<tr><td><b>Version:</b></td><td>" + SPACE + version + "</td></tr>" +
      "<tr><td><b>Java version:</b></td><td>" + SPACE + jvm + "</td></tr>" +
      "<tr><td><b>Website:</b></td><td>" + SPACE + WEBSITE + "</td></tr>" +
      "<tr><td><b>Author:</b></td><td>" + SPACE + AUTHOR + "</td></tr>" +
      "<tr><td><b>Support:</b></td><td>" + SPACE + SUPPORT + "</td></tr>" +
      "<tr><td><b>Copyright:</b></td><td>" + SPACE + COPYRIGHT + "</td></tr>" +
      "</table>" +
      "</html>";

    return (about);

  } // getAbout

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a string appropriate for a splash screen annotation.
   * 
   * @param tool the tool or program name.
   *
   * @return the splash screen string.
   */
  public static String getSplash (
    String tool
  ) {

    String splash = 
      "Version: " + version +  
      "\nAuthor: " + AUTHOR + 
      "\nCopyright:" + COPYRIGHT_SHORT + 
      "\n\nLoading: " + tool + " ... ";

    return (splash);

  } // getSplash

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a simple version string.
   *
   * @return the version string, for example "3.2.2".
   */
  public static String getVersion () { return (version); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a tool version string.
   *
   * @param tool the tool or program name.
   *
   * @return the tool version string, for example "[cwutils 3.2.2] cwrender".
   */
  public static String getToolVersion (
    String tool
  ) {

    return ("[" + PACKAGE_SHORT + " " + version + "] " + tool);

  } // getToolVersion

  ////////////////////////////////////////////////////////////

  /** Gets a Java runtime and OS version. */
  public static String getJavaVersion () {

    return (
      "Java " + System.getProperty ("java.version") + " on " +
      System.getProperty ("os.name") + " " + 
      System.getProperty ("os.version") + " " + 
      System.getProperty ("os.arch")
    );

  } // getJavaVersion

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a full tool and Java runtime version string for
   * printing on the command line.
   *
   * @param tool the tool or program name.
   *
   * @return the full version string.
   */
  public static String getFullVersion (
    String tool
  ) {

    return (getToolVersion (tool) + "\n" + getJavaVersion());

  } // getFullVersion

  ////////////////////////////////////////////////////////////

  /**
   * Sets the current tool command line.  This method should be called
   * by all tools at the beginning of main() so that other classes may
   * retrieve the current command line via getCommandLine().  This is
   * especially important for tools that create new data files, so
   * that writers can insert the command line into the data file
   * history.
   *
   * @param command the command or program name.
   * @param argv an array of command line arguments.
   */
  public static void setCommandLine (
    String command,
    String[] argv
  ) {

    commandLine = MetadataServices.getCommandLine (command, argv);

  } // setCommandLine

  ////////////////////////////////////////////////////////////

  /** Gets the current tool command line, or null if none has been set. */
  public static String getCommandLine () { return (commandLine); }

  ////////////////////////////////////////////////////////////

  /**
   * Starts a memory monitor task that repeatedly outputs the status
   * of memory usage to standard output until the VM exits.
   */
  public static void startMemoryMonitor () {

    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
      public void run() {
        Runtime runtime = Runtime.getRuntime();
        long bytesPerMegabyte = 1024*1024;
        System.out.println ("Free memory:  " + runtime.freeMemory() / bytesPerMegabyte + " Mb");
        System.out.println ("Total memory: " + runtime.totalMemory() / bytesPerMegabyte + " Mb");
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println ("Used memory:  " + usedMemory / bytesPerMegabyte + " Mb");
        System.out.println ("Max memory:   " + runtime.maxMemory() / bytesPerMegabyte + " Mb");
        System.out.println ("--------------");
      } // run
    };
    timer.schedule (task, 0, 5000);
  
  } // startMemoryMonitor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current command line parameter splitting regular expression.
   *
   * @return the current splitting expression.
   */
  public static String getSplitRegex () { return (splitRegex); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the current command line parameter splitting regular expression.
   *
   * @param expr the new splitting expression.
   */
  public static void setSplitRegex (String expr) { splitRegex = expr; }

  ////////////////////////////////////////////////////////////

  /**
   * Checks the setup of tool services.
   *
   * @return an error string if any issues encountered, or null if none.
   */
  public static String checkSetup() {

    String err = null;
    
    err = IOServices.checkSetup();
    if (err == null) {
      // Check other service setups here ...
    } // if

    return (err);

  } // checkSetup

  ////////////////////////////////////////////////////////////

  private ToolServices () { }

  ////////////////////////////////////////////////////////////

} // ToolServices class

////////////////////////////////////////////////////////////////////////
