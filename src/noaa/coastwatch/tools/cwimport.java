////////////////////////////////////////////////////////////////////////
/*
     FILE: cwimport.java
  PURPOSE: To import satellite data files to CoastWatch HDF format.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/09
  CHANGES: 2002/07/22, PFH, fixed problem reading coordinate variables
           2002/07/23, PFH, added chunk, match options, fixed NARGS value
           2002/08/25, PFH, rearranged remaining options parsing
           2002/11/05, PFH, modified setChunkSize for bytes
           2002/11/11, PFH, changed some option names for easier reading
           2002/11/16, PFH, modified verbose messages
           2002/11/18, PFH, removed writer options, changed documentation
           2003/02/23, PFH, added NOAA 1b import
           2004/09/28, PFH, modified to use ToolServices.setCommandLine()
           2005/01/30, PFH, modified to use CleanupHook class
           2005/03/14, PFH, reformatted documentation and usage note
           2007/04/10, PFH, added --copy option
           2007/04/19, PFH, added version printing

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// --------
import java.io.*;
import java.util.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import jargs.gnu.*;
import jargs.gnu.CmdLineParser.*;

/**
 * <p>The import tool translates Earth data into CoastWatch HDF format.</p> 
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwimport - translates Earth data into CoastWatch HDF.
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
 * -h, --help <br>
 * -m, --match=PATTERN <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The import tool translates Earth data into
 * CoastWatch HDF format.  Multiple input files may be specified, but
 * must have matching Earth transforms and dates.  The utility loops
 * over all input files and creates a single CoastWatch HDF output
 * file.  The utility does not handle multiple variables with the same
 * name -- if a variable in an input file is encountered with the same
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
 *   matching dates and Earth transforms.  The currently supported
 *   input formats are CoastWatch HDF, CoastWatch IMGMAP, TeraScan
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
 *   <li> Invalid command line option. </li>
 *   <li> Invalid input or output file names. </li>
 *   <li> Unsupported input file format. </li>
 *   <li> Input file dates or Earth transforms do not match. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the import of several .cwf files
 * to CoastWatch HDF with verbose mode on:</p>
 * <pre>
 *   phollema$ cwimport --verbose 2002_214_2057_n16_wv_*.cwf 2002_214_2057_n16_wv.hdf
 *
 *   cwimport: Reading file [1/2], 2002_214_2057_n16_wv_c2.cwf
 *   cwimport: Writing avhrr_ch2
 *   cwimport: Writing graphics
 *   cwimport: Reading file [2/2], 2002_214_2057_n16_wv_c4.cwf
 *   cwimport: Writing avhrr_ch4
 *   cwimport: Writing graphics
 *   cwimport: Variable 'graphics' already exists, skipping
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public final class cwimport {

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 2;

  /** Name of program. */
  private static final String PROG = "cwimport";

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) {

    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option copyOpt = cmd.addBooleanOption ('c', "copy");
    Option versionOpt = cmd.addBooleanOption ("version");
    try { cmd.parse (argv); }
    catch (OptionException e) {
      System.err.println (PROG + ": " + e.getMessage());
      usage ();
      System.exit (1);
    } // catch

    // Print help message
    // ------------------
    if (cmd.getOptionValue (helpOpt) != null) {
      usage ();
      System.exit (0);
    } // if  

    // Print version message
    // ---------------------
    if (cmd.getOptionValue (versionOpt) != null) {
      System.out.println (ToolServices.getFullVersion (PROG));
      System.exit (0);
    } // if  

    // Get remaining arguments
    // -----------------------
    String[] remain = cmd.getRemainingArgs();
    if (remain.length < NARGS) {
      System.err.println (PROG + ": At least " + NARGS + 
        " argument(s) required");
      usage ();
      System.exit (1);
    } // if
    String output = remain[remain.length-1];
    String[] input = remain;
    int inputCount = remain.length - 1;

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    String match = (String) cmd.getOptionValue (matchOpt);
    boolean copy = (cmd.getOptionValue (copyOpt) != null);

    try {

      // Open first input file
      // ---------------------
      if (verbose) System.out.println (PROG + ": Reading input " + input[0]);
      EarthDataReader reader = EarthDataReaderFactory.create (
        input[0]);
      EarthDataInfo info = reader.getInfo ();
      Date date = reader.getInfo().getDate();
      EarthTransform trans = reader.getInfo().getTransform();

      // Open/check output file
      // ----------------------
      CWHDFWriter writer;
      if (copy) {
        File outputFile = new File (output);
        if (!outputFile.exists()) {
          System.err.println (PROG + ": Output file " + output + 
            " does not exist");
          System.exit (2);
        } // if          
        if (verbose) System.out.println (PROG + ": Opening output " + output);
        CWHDFReader outputReader = new CWHDFReader (output);
        EarthTransform outputTransform = outputReader.getInfo().getTransform();
        outputReader.close();
        if (!trans.equals (outputTransform)) {
          System.err.println (PROG + 
            ": Earth transforms do not match for " + input[0] + " and " + 
            output);
          System.exit (2);
        } // if
        writer = new CWHDFWriter (output);
      } // if

      // Create new output file
      // ----------------------
      else {
        if (verbose) System.out.println (PROG + ": Creating output " + output);
        CleanupHook.getInstance().scheduleDelete (output);
        writer = new CWHDFWriter (info, output);
      } // else
     
      // Loop over each input file
      // -------------------------
      for (int k = 0; k < inputCount; k++) {
        if (verbose)
          System.out.println (PROG + ": Reading file [" + (k+1) + "/" +
            + inputCount + "], " + input[k]);

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
            if (verbose)
              System.out.println (PROG + ": Writing " + varName); 
            writer.addVariable (var);
            writer.flush(); 
          } // try
          catch (IOException e) { 
            if (verbose) 
              System.out.println (PROG + ": " + e.getMessage() +", skipping");
          } // catch

        } // for

        // Close input
        // -----------
        reader.close();

        // Open new input file
        // -------------------
        if (k != inputCount-1) {
          reader = EarthDataReaderFactory.create (input[k+1]);
          Date newDate = reader.getInfo().getDate();
          EarthTransform newTrans = reader.getInfo().getTransform();
          if (!date.equals (newDate)) {
            System.err.println (PROG + ": Dates do not match for " +
              input[k+1] + " and " + input[0]);
            System.exit (2);
          } // if
          if (!trans.equals (newTrans)) {
            System.err.println (PROG + ": Earth transforms do not match for " +
              input[k+1] + " and " + input[0]);
            System.exit (2);
          } // if
        } // if

      } // for

      // Close output file
      // -----------------
      writer.close();
      CleanupHook.getInstance().cancelDelete (output);
    
    } // try
    catch (Exception e) {
      e.printStackTrace ();
      System.exit (2);
    } // catch

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwimport [OPTIONS] input1 [input2 ...] output\n" +
"Translates Earth data into CoastWatch HDF format.\n" +
"\n" +
"Main parameters:\n" +
"  input1 [input2 ...]        The input data file name(s).\n" +
"  output                     The output data file name.\n" +
"\n" +
"Options:\n" +
"  -c, --copy                 Copy data without overwriting output file.\n" +
"  -h, --help                 Show this help message.\n" +
"  -m, --match=PATTERN        Import variables matching the pattern.\n" +
"  -v, --verbose              Print verbose messages.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

  private cwimport () { }

  ////////////////////////////////////////////////////////////

} // cwimport class

////////////////////////////////////////////////////////////////////////
