////////////////////////////////////////////////////////////////////////
/*

     File: cwimport.java
   Author: Peter Hollemans
     Date: 2002/07/09

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
// --------
import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.OptionException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import noaa.coastwatch.io.CWHDFReader;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.tools.CleanupHook;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The import tool translates earth data into CoastWatch HDF format.</p> 
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwimport - translates earth data into CoastWatch HDF.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p> cwimport [OPTIONS] input1 [input2 ...] output </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -c, --copy <br>
 * -g, --nogroup <br>
 * -h, --help <br>
 * -m, --match=PATTERN <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The import tool translates earth data into
 * CoastWatch HDF format.  Multiple input files may be specified, but
 * must have matching earth transforms and dates.  The utility loops
 * over all input files and creates a single CoastWatch HDF output
 * file.  The utility does not handle multiple variables with the same
 * name &#8212; if a variable in an input file is encountered with the same
 * name as an existing variable from a previous input file, the new
 * variable is skipped.  Options are available to alter verbosity and
 * variable name matching. </p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> input1 [input2 ...] </dt>
 *   <dd> The input data file name(s).  At least one input file is
 *   required.  If multiple files are specified, they must have
 *   matching dates and earth transforms.  The currently supported
 *   input formats are CoastWatch HDF, NetCDF 3/4 with CF metadata, TeraScan
 *   HDF, and NOAA 1b format GAC/LAC/HRPT AVHRR. </dd>
 *
 *   <dt> output </dt>
 *   <dd> The output data file name. </dd> 
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt> -c, --copy </dt>
 *
 *   <dd> Turns on copy mode.  In copy mode, variables from the
 *   input files are copied into an existing output file.  The
 *   default is to create a new output file and populate it with
 *   data.  Copy mode is especially useful for copying variables
 *   from one CoastWatch HDF file to another.</dd>
 *
 *   <dt>-g, --nogroup</dt>
 *
 *   <dd>Turns on removal of the group path in variable names.  If variable
 *   names contain a leading group path ending with '/', the group path is
 *   removed.</dd>
 *
 *   <dt> -h, --help </dt>
 * 
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -m, --match=PATTERN </dt>
 * 
 *   <dd> The variable name matching pattern.  If specified, the
 *   pattern is used as a regular expression to match variable names.
 *   Only variables matching the pattern will be imported.  By
 *   default, no pattern matching is performed and all variables are
 *   imported. </dd>
 *
 *   <dt> -v, --verbose </dt>
 *
 *   <dd> Turns verbose mode on.  The current status of data
 *   conversion is printed periodically.  The default is to run
 *   quietly. </dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Invalid input or output file names </li>
 *   <li> Unsupported input file format </li>
 *   <li> Input file dates or earth transforms do not match </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the import of a NetCDF file to CoastWatch HDF 
 * with verbose mode on:</p>
 * <pre>
 *   phollema$ cwimport --verbose --match analysed_sst 
 *     20220621-GHRSST-Blended-v02.0-fv01.0.nc 2022_06_21_night_sst.hdf
 *   [INFO] Reading input 20220621-GHRSST-Blended-v02.0-fv01.0.nc
 *   [INFO] Creating output 2022_06_21_night_sst.hdf
 *   [INFO] Reading file [1/1], 20220621-GHRSST-Blended-v02.0-fv01.0.nc
 *   [INFO] Writing analysed_sst
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
 public final class cwimport {

  private static final String PROG = cwimport.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 2;

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) {

    ToolServices.startExecution (PROG);
    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option copyOpt = cmd.addBooleanOption ('c', "copy");
    Option nogroupOpt = cmd.addBooleanOption ('g', "nogroup");
    Option versionOpt = cmd.addBooleanOption ("version");
    try { cmd.parse (argv); }
    catch (OptionException e) {
      LOGGER.warning (e.getMessage());
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // catch

    // Print help message
    // ------------------
    if (cmd.getOptionValue (helpOpt) != null) {
      usage();
      ToolServices.exitWithCode (0);
      return;
    } // if  

    // Print version message
    // ---------------------
    if (cmd.getOptionValue (versionOpt) != null) {
      System.out.println (ToolServices.getFullVersion (PROG));
      ToolServices.exitWithCode (0);
      return;
    } // if  

    // Get remaining arguments
    // -----------------------
    String[] remain = cmd.getRemainingArgs();
    if (remain.length < NARGS) {
      LOGGER.warning ("At least " + NARGS + " argument(s) required");
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // if
    String output = remain[remain.length-1];
    String[] input = remain;
    int inputCount = remain.length - 1;

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose) VERBOSE.setLevel (Level.INFO);
    String match = (String) cmd.getOptionValue (matchOpt);
    boolean copy = (cmd.getOptionValue (copyOpt) != null);
    boolean nogroup = (cmd.getOptionValue (nogroupOpt) != null);

    // We set the reader to default to reading a swath projection as a data
    // projection to preserve the sensor geometery as much as possible if there
    // are level 2 files
    EarthDataReader.setDataProjection (true);

    // We have the reader and writer declared outside the try statement so
    // we can close them later if there's an error that doesn't result in
    // exiting the Java VM.
    EarthDataReader[] readers = new EarthDataReader[input.length];
    CWHDFWriter writer = null;
    EarthDataReader reader = null;

    try {

      // Open first input file
      // ---------------------
      VERBOSE.info ("Reading input " + input[0]);
      reader = EarthDataReaderFactory.create (input[0]);
      EarthDataInfo info = reader.getInfo();
      Date date = reader.getInfo().getDate();
      EarthTransform trans = reader.getInfo().getTransform();

      // Open/check output file
      // ----------------------
      if (copy) {
        File outputFile = new File (output);
        if (!outputFile.exists()) {
          LOGGER.severe ("Output file " + output + " does not exist");
          ToolServices.exitWithCode (2);
          return;
        } // if          
        VERBOSE.info ("Opening output " + output);
        CWHDFReader outputReader = new CWHDFReader (output);
        EarthTransform outputTransform = outputReader.getInfo().getTransform();
        outputReader.close();
        if (!trans.equals (outputTransform)) {
          LOGGER.severe ("Earth transforms do not match for " + input[0] + " and " + output);
          ToolServices.exitWithCode (2);
          return;
        } // if
        writer = new CWHDFWriter (output);
      } // if

      // Create new output file
      // ----------------------
      else {
        VERBOSE.info ("Creating output " + output);
        CleanupHook.getInstance().scheduleDelete (output);
        writer = new CWHDFWriter (info, output);
      } // else
     
      // Loop over each input file
      // -------------------------
      for (int k = 0; k < inputCount; k++) {
        VERBOSE.info ("Reading file [" + (k+1) + "/" + inputCount + "], " + input[k]);

        // Loop over each variable
        // -----------------------
        for (int i = 0; i < reader.getVariables(); i++) {

          // Check for name match
          // --------------------
          String varName = reader.getName(i);
          if (match != null && !varName.matches (match))
            continue;

          // Get variable and flush
          // ----------------------
          try {
            DataVariable var = reader.getVariable (i);

            if (nogroup) {
              String outputName = IOServices.stripGroup (varName);
              var.setName (outputName);
              varName = outputName;
            } // if

            VERBOSE.info ("Writing " + varName); 
            writer.addVariable (var);
            writer.flush(); 
          } // try
          catch (IOException e) { 
            VERBOSE.info (e.getMessage() + ", skipping");
          } // catch

        } // for

        // Close input
        // -----------
        reader.close();
        reader = null;

        // Open new input file
        // -------------------
        if (k != inputCount-1) {
          reader = EarthDataReaderFactory.create (input[k+1]);
          Date newDate = reader.getInfo().getDate();
          EarthTransform newTrans = reader.getInfo().getTransform();
          if (!date.equals (newDate)) {
            LOGGER.severe ("Dates do not match for " + input[k+1] + " and " + input[0]);
            ToolServices.exitWithCode (2);
            return;
          } // if
          if (!trans.equals (newTrans)) {
            LOGGER.severe ("Earth transforms do not match for " + input[k+1] + " and " + input[0]);
            ToolServices.exitWithCode (2);
            return;
          } // if
        } // if

      } // for

      // Close output file
      // -----------------
      writer.close();
      writer = null;
      CleanupHook.getInstance().cancelDelete (output);
    
    } // try

    catch (OutOfMemoryError | Exception e) {
      ToolServices.warnOutOfMemory (e);
      LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    finally {
      try {
        if (reader != null) reader.close();
        if (writer != null) writer.close();
      } // try
      catch (Exception e) { LOGGER.log (Level.SEVERE, "Error closing resources", e); }
    } // finally

    ToolServices.finishExecution (PROG);

  } // main

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cwimport");

    info.func ("Translates earth data into CoastWatch HDF format");

    info.param ("input1 [input2 ...]", "Input data file(s)");
    info.param ("output", "Output data file");

    info.option ("-c, --copy", "Copy data without overwriting output file");
    info.option ("-g, --nogroup", "Remove group path from variable names");
    info.option ("-h, --help", "Show help message");
    info.option ("-m, --match=PATTERN", "Import only variables matching regular expression");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwimport () { }

  ////////////////////////////////////////////////////////////

} // cwimport class

////////////////////////////////////////////////////////////////////////
