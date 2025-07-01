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
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.io.IOServices;

import java.util.logging.Logger;

/**
 * The tool services class defines various static methods relating
 * to the CoastWatch tools.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class ToolServices {

  private static final Logger LOGGER = Logger.getLogger (ToolServices.class.getName());

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
  public static final String WEBSITE = "https://coastwatch.noaa.gov";

  /** The software copyright. */
  public static final String COPYRIGHT = 
    "(c) 1998-2025 National Oceanic and Atmospheric Administration";

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

  /** The system exit mode, true to perform an ectual System.exit() call. */
  private static boolean isSystemExit = true;

  /** The last exit code reported. */
  private static int exitCode;
  
  /** The tool starting time when startExec() was called. */
  private static Map<String, Long> startTimeMap = new HashMap<>();
  
  ////////////////////////////////////////////////////////////

  /**
   * Logs a starting time for an executable task.
   *
   * @param name the name of the task starting.
   *
   * @since 3.5.0
   */
  public static void startExecution (
    String name
  ) {
  
    LOGGER.fine ("Started execution of " + name);
    startTimeMap.put (name, System.currentTimeMillis());

  } // startExecution

  ////////////////////////////////////////////////////////////

  /**
   * Logs a finishing time for a executable task.
   *
   * @param name the name of the task finishing.
   *
   * @since 3.5.0
   */
  public static void finishExecution (
    String name
  ) {

    LOGGER.fine ("Finished execution of " + name);
    long startTime = startTimeMap.get (name);
    long elapsedMillis = System.currentTimeMillis() - startTime;
    LOGGER.fine (String.format ("Elapsed time = %.3f s", elapsedMillis*1e-3));;

    startTimeMap.remove (name);

  } // finishExecution

  ////////////////////////////////////////////////////////////

  /**
   * Sets the system exit flag.
   *
   * @param flag the exit flag, true to actually exit the system on a call to
   * {@link #exitWithCode} or false to not.
   *
   * @since 3.5.0
   */
  public static void setSystemExit (
    boolean flag
  ) {

    isSystemExit = flag;

  } // setSystemExit

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

    tool = getClassName (tool);
    return (getToolVersion (tool) + "\n" + getJavaVersion());

  } // getFullVersion

  ////////////////////////////////////////////////////////////

  /**
   * Gets the final class name without any package qualifier.
   *
   * @param name the name with possible package prefix.
   *
   * @return the class name without package qualifier.
   */
  private static String getClassName (
    String name
  ) {

    if (name.contains (".")) {
      int lastDot = name.lastIndexOf (".");
      name = name.substring (lastDot + 1, name.length());
    } // if

    return (name);
  
  } // getClassName

  ////////////////////////////////////////////////////////////

  /**
   * Sets the current tool command line.  This method should be called
   * by all tools at the beginning of main() so that other classes may
   * retrieve the current command line via getCommandLine().  This is
   * especially important for tools that create new data files, so
   * that writers can insert the command line into the data file
   * history.
   *
   * @param command the command or program name.  If this is a class name,
   * only the final part of the class name is retained as the program name.
   * @param argv an array of command line arguments.
   */
  public static void setCommandLine (
    String command,
    String[] argv
  ) {

    command = getClassName (command);
    commandLine = MetadataServices.getCommandLine (command, argv);

    LOGGER.fine ("Command line was " + commandLine);

    // Check to see if a memory monitor is requested in the system properties.
    var memoryOpt = System.getProperty ("cw.memory.monitor", "false");
    var interval = -1;
    if (!memoryOpt.equals ("false")) {
      if (memoryOpt.equals ("true")) interval = 5;
      else {
        try { interval = Integer.parseInt (memoryOpt); }
        catch (Exception e) { }
      } // else
    } // if
    if (interval != -1) startMemoryMonitor (interval);

  } // setCommandLine

  ////////////////////////////////////////////////////////////

  /** Gets the current tool command line, or null if none has been set. */
  public static String getCommandLine () { return (commandLine); }

  ////////////////////////////////////////////////////////////

  /**
   * Starts a memory monitor task that repeatedly outputs the status
   * of memory usage to standard output until the VM exits.
   * 
   * @param interval the interval in seconds for the memory status
   * message.
   * 
   * @since 3.8.1
   */
  public static void startMemoryMonitor (int interval) {

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
    timer.schedule (task, 0, interval*1000);

  } // startMemoryMonitor

  ////////////////////////////////////////////////////////////

  /**
   * Starts a memory monitor task that repeatedly outputs the status
   * of memory usage to standard output until the VM exits.
   */
  public static void startMemoryMonitor () {

    startMemoryMonitor (5);
  
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

  /**
   * Performs an exit of a tool with the specified code.  The exit
   * may be a system exit, or a soft exit depending on the exit mode.  The
   * default is to actually perform a system exit.
   *
   * @param code the code to use for exiting.
   *
   * @since 3.5.0
   *
   * @see #setSystemExit
   */
  public static void exitWithCode (int code) {
  
    CleanupHook.getInstance().run();
    LOGGER.fine ("Exiting with code " + code);

    if (isSystemExit) {
      System.exit (code);
    } // if
    else {
      exitCode = code;
    } // else
  
  } // exitWithCode

  ////////////////////////////////////////////////////////////

  /**
   * Logs a warning to the user if the throwable or any of its causes
   * is an OutOfMemory error.
   *
   * @param th the throwable to check.
   *
   * @since 3.5.1
   */
  public static void warnOutOfMemory (
    Throwable th
  ) {
  
    // Check for out of memory
    // -----------------------
    boolean isOutOfMemory = false;
    do {
      if (th instanceof OutOfMemoryError) {
        isOutOfMemory = true;
        break;
      } // if
      else {
        th = th.getCause();
      } // else
    } while (th != null);

    // Log warning
    // -----------
    if (isOutOfMemory) {
      long maxHeap = Runtime.getRuntime().maxMemory() / 1024 / 1024;
      long newMaxHeap = maxHeap*2;
      LOGGER.warning ("Caught dynamic memory allocation error, heap space is completely used");
      LOGGER.warning ("Maximum heap size for this run set at " + maxHeap + " MB");
      LOGGER.warning ("Rerun command with option -J-Xmx" + newMaxHeap + "m to double maximum heap");
    } // if
  
  } // warnOutOfMemory
  
  ////////////////////////////////////////////////////////////

  /**
   * Shortens the stack trace contained in a throwable error so that it
   * ends at the mention of a specific class.  If the thowable has a non-null
   * cause, it is modified as well.
   *
   * @param error the throwable to check.
   * @param prefix the prefix of the class name to look for.  Any stack trace
   * elements after the last one that contains the prefix are removed.
   * 
   * @return the throwable error passed in, for convenience.
   *
   * @since 3.7.1
   */
  public static Throwable shortTrace (
    Throwable error,
    String prefix
  ) {

    var trace = error.getStackTrace();
    int last = trace.length-1;
    while (last >= 0) { 
      if (trace[last].getClassName().startsWith (prefix)) break; 
      last--; 
    } // while

    error.setStackTrace (Arrays.copyOf (trace, last+1));
    var cause = error.getCause();
    if (cause != null) shortTrace (cause, prefix);

    return (error);

  } // shortTrace

  ////////////////////////////////////////////////////////////

  private ToolServices () { }

  ////////////////////////////////////////////////////////////

} // ToolServices class

////////////////////////////////////////////////////////////////////////
