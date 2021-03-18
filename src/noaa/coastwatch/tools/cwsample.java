////////////////////////////////////////////////////////////////////////
/*

     File: cwsample.java
   Author: Peter Hollemans
     Date: 2002/01/26

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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.SimpleParser;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.EarthTransform;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The sampling tool extracts data values at specified earth
 * locations.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p> 
 *   <!-- START NAME -->          
 *   cwsample - extracts data values at specified earth locations.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p>cwsample [OPTIONS] input output</p>
 *
 * <h3>Sampling type options:</h3>
 *
 * <p>
 * -s, --sample=LATITUDE/LONGITUDE <br>
 * -S, --samples=FILE <br>
 * </p>
 *
 * <h3>General options:</h3>
 *
 * <p>
 * -d, --dec=DECIMALS <br>
 * -D, --delimit=STRING <br>
 * -h, --help <br>
 * -H, --header <br>
 * -i, --imagecoords <br>
 * -m, --match=PATTERN <br>
 * -M, --missing=VALUE <br>
 * -n, --nocoords <br>
 * -R, --reverse <br>
 * -V, --variable=NAME1[/NAME2/...] <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p>
 * The sampling tool extracts data values at specified Earth
 * locations from 2D data variables.  A sample point may be specified
 * on the command line using geographic coordinates, or multiple
 * sample points may be specified using a data file.  A number of 2D
 * data variables may be sampled simultaneously.  The sampled values
 * are printed as ASCII text to the output file, one line per sample
 * point.  Various options are available to modify the output decimals
 * places, delimiters, and so on.
 * </p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> input </dt>
 *   <dd> The input data file name. </dd>
 *
 *   <dt> output </dt>
 *   <dd> The output text file name.  If the output file name is '-',
 *   output is sent to standard output (normally the terminal).  In this case,
 *   the end of the options must be indicated with a lone '--' (see the
 *   examples) or the '-' output file name will be interpreted as an option. </dd>
 *
 * </dl>
 *
 * <h3>Sampling type options:</h3>
 *
 * <dl>
 *
 *   <dt> -s, --sample=LATITUDE/LONGITUDE </dt>
 *   <dd> The sample point for a single sampling operation.  The point
 *   is specified in terms of earth location latitude and longitude in
 *   the range [-90..90] and [-180..180] with a datum matching that of the
 *   input file. </dd>
 *
 *   <dt> -S, --samples=FILE </dt>
 *   <dd> The file name containing a list of sample points for
 *   performing multiple sampling operations.  The file must be an
 *   ASCII text file containing sample points as latitude / longitude
 *   pairs, one pair per line, with values separated by spaces or
 *   tabs.  The points are specified in terms of earth location
 *   latitude and longitude in the range [-90..90] and
 *   [-180..180] with a datum matching that of the input file. </dd>
 *
 * </dl>
 *
 * <h3>General options:</h3>
 *
 * <dl>
 *
 *   <dt> -d, --dec=DECIMALS </dt>
 *   <dd> The number of decimal places for printed geographic
 *   coordinate values.  The default is 6 decimals. </dd>
 *
 *   <dt> -D, --delimit=STRING </dt>
 *   <dd> The value delimiter string.  By default, values are
 *   separated with a single space character. </dd>
 *
 *   <dt> -h, --help </dt>
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -H, --header </dt>
 *   <dd> Specifies that a one line header should be written.  The
 *   header is written before any data and consists of the output
 *   column names.  By default no header is written. </dd>
 *
 *   <dt> -i, --imagecoords </dt>
 *   <dd> Specifies that image coordinates (row and column) should be
 *   printed for each output line.  The default is to print only
 *   geographic coordinates. </dd>
 *
 *   <dt> -m, --match=PATTERN </dt>
 *   <dd> The variable name matching pattern.  If specified, the
 *   pattern is used as a regular expression to match variable names.
 *   Only variables matching the pattern will be sampled.  By default,
 *   no pattern matching is performed and all variables are sampled
 *   unless the <b>--variable</b> option is used.  Note that either
 *   <b>--variable</b> or <b>--match</b> may be specified, but not
 *   both.</dd>
 *
 *   <dt> -M, --missing=VALUE </dt>
 *   <dd> The output value for missing or out of range data.  The
 *   default is to print 'NaN' for missing values. </dd>
 *
 *   <dt> -n, --nocoords </dt>
 *   <dd> Turns geographic coordinate printing off.  By default, each
 *   output line has the form 'latitude longitude value(s)' but with no
 *   coordinates, each line simply contains the data value(s). </dd>
 *
 *   <dt> -R, --reverse </dt>
 *   <dd> Specifies that coordinates should be printed in reverse
 *   order, 'longitude latitude'.  The default is 'latitude
 *   longitude'. </dd>
 *
 *   <dt> -V, --variable=NAME1[/NAME2/...] </dt>
 *   <dd> The variable names to sample.  If specified, the variable
 *   sample values are printed in columns in exactly the same order as
 *   they are listed.  This option is different from the
 *   <b>--match</b> option because it (i) specifies the column order,
 *   where as <b>--match</b> orders the columns as the variables are
 *   encountered in the file, and (ii) does not support pattern
 *   matching; all variable names must be specified exactly.  Without
 *   this option or the <b>--match</b> option, all variables are
 *   sampled.  Note that either <b>--variable</b> or <b>--match</b>
 *   may be specified, but not both.</dd>
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
 *   <li> Invalid sample coordinates file format </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> In the example below, a sample points file named
 * <code>sample_locs.txt</code> was set up to follow the 93 W
 * longitude line at regular 0.2 degree intervals as follows:</p>
 * <pre>
 *   28 -93
 *   28.2 -93
 *   28.4 -93
 *   28.6 -93
 *   28.8 -93
 *   29 -93
 *   29.2 -93
 *   29.4 -93
 *   29.6 -93
 *   29.8 -93
 *   30 -93
 * </pre>
 * <p>and a Gulf of Mexico data file sampled for SST and cloud data along
 * this line with output to the terminal screen:</p>
 * <pre>
 *   phollema$ cwsample --header --match '(sst|cloud)' --samples sample_locs.txt
 *     -- 2002_325_1546_n17_mr.hdf -
 *
 *   latitude longitude sst cloud
 *   28 -93 25.24 0
 *   28.2 -93 25.24 0
 *   28.4 -93 24.78 0
 *   28.6 -93 23.84 0
 *   28.8 -93 22.72 0
 *   29 -93 21.37 0
 *   29.2 -93 20.06 0
 *   29.4 -93 19.29 0
 *   29.6 -93 18.16 0
 *   29.8 -93 17.57 6
 *   30 -93 17.48 22
 * </pre>
 * <p>Another example shows the sampling of one SST value as in the case
 * of comparison with a single buoy measurement with output to the
 * terminal screen:</p>
 * <pre>
 *   phollema$ cwsample --header --match sst --sample 28.8/-93 -- 2002_325_1546_n17_mr.hdf -
 *
 *   latitude longitude sst
 *   28.8 -93 22.72
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class cwsample {

  private static final String PROG = cwsample.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  
  // Constants
  // ------------
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
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option sampleOpt = cmd.addStringOption ('s', "sample");
    Option samplesOpt = cmd.addStringOption ('S', "samples");
    Option missingOpt = cmd.addDoubleOption ('M', "missing");
    Option headerOpt = cmd.addBooleanOption ('H', "header");
    Option decOpt = cmd.addIntegerOption ('d', "dec");
    Option nocoordsOpt = cmd.addBooleanOption ('n', "nocoords");
    Option reverseOpt = cmd.addBooleanOption ('R', "reverse");
    Option delimitOpt = cmd.addStringOption ('D', "delimit");
    Option imagecoordsOpt = cmd.addBooleanOption ('i', "imagecoords");
    Option variableOpt = cmd.addStringOption ('V', "variable");
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
    String input = remain[0];
    String output = remain[1];

    // Set defaults
    // ------------
    String match = (String) cmd.getOptionValue (matchOpt);
    String sample = (String) cmd.getOptionValue (sampleOpt);
    String samples = (String) cmd.getOptionValue (samplesOpt);
    Double missingObj = (Double) cmd.getOptionValue (missingOpt);
    double missing = (missingObj == null ? Double.NaN : 
      missingObj.doubleValue());
    boolean header = (cmd.getOptionValue (headerOpt) != null);
    Integer decObj = (Integer) cmd.getOptionValue (decOpt);
    int dec = (decObj == null ? 6 : decObj.intValue());
    boolean nocoords = (cmd.getOptionValue (nocoordsOpt) != null);
    boolean reverse = (cmd.getOptionValue (reverseOpt) != null);
    String delimit = (String) cmd.getOptionValue (delimitOpt);
    if (delimit == null) delimit = " ";
    boolean imagecoords = (cmd.getOptionValue (imagecoordsOpt) != null);
    String variable = (String) cmd.getOptionValue (variableOpt);

    // Check for sample option
    // -----------------------
    if (sample == null && samples == null) {
      LOGGER.severe ("At least one sampling option must be specified");
      ToolServices.exitWithCode (2);
      return;
    } // if

    // Check variable and match options
    // --------------------------------
    if (match != null && variable != null) {
      LOGGER.severe ("Cannot specify both --variable and --match options");
      ToolServices.exitWithCode (2);
      return;
    } // if

    try {

      // Open input file
      // ---------------
      EarthDataReader reader = EarthDataReaderFactory.create (input);
      EarthTransform trans = reader.getInfo().getTransform();
      if (!trans.isInvertible()) {
        LOGGER.severe ("Detected a non-invertible transform in input file");
        ToolServices.exitWithCode (2);
        return;
      } // if
      Datum datum = trans.getDatum();

      // Get single sample location
      // --------------------------
      List locations = new ArrayList();
      if (sample != null) {
        String[] sampleArray = sample.split (ToolServices.SPLIT_REGEX);
        if (sampleArray.length != 2) {
          LOGGER.severe ("Invalid sample '" + sample + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        double lat = Double.parseDouble (sampleArray[0]);
        double lon = Double.parseDouble (sampleArray[1]);
        locations.add (new EarthLocation (lat, lon, datum));
      } // if

      // Get multiple sample locations
      // -----------------------------
      else if (samples != null) {
        try {
          SimpleParser parser = new SimpleParser (new BufferedReader (
            new InputStreamReader (new FileInputStream (new File (samples)))));
          do {
            double lat = parser.getNumber();
            double lon = parser.getNumber();
            locations.add (new EarthLocation (lat, lon, datum));
          } while (!parser.eof());
        } // try
        catch (IOException e) {
          LOGGER.log (Level.SEVERE, "Error parsing sample points file", e);
          ToolServices.exitWithCode (2);
          return;
        } // catch
      } // else if

      // Create output file
      // ------------------
      PrintStream outStream;
      if (output.equals ("-")) outStream = System.out;
      else outStream = new PrintStream (new FileOutputStream (output));

      // Get variables by name
      // ---------------------
      List variables = new ArrayList();
      if (variable != null) {
        String[] varNameArray = variable.split (ToolServices.SPLIT_REGEX);
        for (int i = 0; i < varNameArray.length; i++) {
          DataVariable var = reader.getPreview (varNameArray[i]);
          if (var.getRank() != 2) {
            LOGGER.severe ("Cannot sample variable '" + varNameArray[i] + "', rank is not 2");
            ToolServices.exitWithCode (2);
            return;
          } // if
          variables.add (reader.getVariable (varNameArray[i]));
        } // for
      } // if

      // Get variables by pattern or file order
      // --------------------------------------
      else {
        for (int i = 0; i < reader.getVariables(); i++) {
          DataVariable var = reader.getPreview(i);
          if (var.getRank() != 2) continue;
          String varName = var.getName();
          if (match != null && !varName.matches (match)) continue;
          variables.add (reader.getVariable(i));
        } // for      
        if (variables.size() == 0) {
          LOGGER.severe ("No matching variables found");
          ToolServices.exitWithCode (2);
          return;
        } // if
      } // else

      // Create array of variables from list
      // -----------------------------------
      DataVariable[] varArray = (DataVariable[]) variables.toArray (
        new DataVariable[] {});

      // Create format 
      // -------------
      String formatPattern = "0.";
      for (int i = 0; i < dec; i++) formatPattern += "#";
      DecimalFormat format = new DecimalFormat (formatPattern);

      // Print header
      // ------------
      if (header) {
        if (!nocoords) {
          if (reverse) 
            outStream.print ("longitude" + delimit + "latitude" + delimit);
          else
            outStream.print ("latitude" + delimit + "longitude" + delimit);
        } // if
        if (imagecoords)
          outStream.print ("row" + delimit + "column" + delimit);
        for (int i = 0; i < varArray.length; i++) {
          outStream.print (varArray[i].getName());
          if (i < varArray.length-1) outStream.print (delimit);
        } // for
        outStream.println();
      } // if

      // Loop over each location
      // -----------------------
      for (Iterator iter = locations.iterator(); iter.hasNext(); ) {

        // Print earth location
        // --------------------
        EarthLocation geoLoc = (EarthLocation) iter.next();
        if (!nocoords) {
          double[] coords = new double[] {(reverse ? geoLoc.lon : geoLoc.lat),
            (reverse ? geoLoc.lat : geoLoc.lon)};
          outStream.print (format.format (coords[0]));
          outStream.print (delimit);
          outStream.print (format.format (coords[1]));
          outStream.print (delimit);
        } // if

        // Print data location
        // -------------------
        DataLocation dataLoc = trans.transform (geoLoc);
        if (imagecoords) {
          int row = (int) Math.round (dataLoc.get (Grid.ROWS));
          int col = (int) Math.round (dataLoc.get (Grid.COLS));
          outStream.print (row + delimit + col + delimit);
        } // if

        // Print variable values
        // ---------------------
        for (int i = 0; i < varArray.length; i++) {
          double value = varArray[i].getValue (dataLoc);
          if (Double.isNaN (value)) {
            if (Double.isNaN (missing))
              outStream.print ("NaN");
            else
              outStream.print (varArray[i].format (missing));
          } // if
          else
            outStream.print (varArray[i].format (value));
          if (i < varArray.length-1) outStream.print (delimit);
        } // for
        outStream.println();

      } // for


      // TODO: Should we explicitly close the reader here?  An issue was found
      // in cwrender that if the reader wasn't explicitly closed, it was
      // assumed by the garbage collector that it wasn't being used anymore
      // even though structures internal to the reader were being accessed.
      // The reader is not closed explicitly in cwstats either, and possibly
      // other places.


    } // try

    catch (OutOfMemoryError | Exception e) {
      ToolServices.warnOutOfMemory (e);
      LOGGER.log (Level.SEVERE, "Aborting", e);
      ToolServices.exitWithCode (2);
      return;
    } // catch

    ToolServices.finishExecution (PROG);

  } // main

  ////////////////////////////////////////////////////////////
  
  private static void usage () { System.out.println (getUsage()); }
  
  ////////////////////////////////////////////////////////////
  
  /** Gets the usage info for this tool. */
  private static UsageInfo getUsage () {
    
    UsageInfo info = new UsageInfo ("cwsample");
    
    info.func ("Extracts data values at specified earth locations");

    info.param ("input", "Input data file");
    info.param ("output", "Output text file name or '-' for stdout");

    info.section ("Sampling type");
    info.option ("-s, --sample=LATITUDE/LONGITUDE", "Sample single location");
    info.option ("-S, --samples=FILE", "Sample multiple locations");

    info.section ("General");
    info.option ("-d, --dec=DECIMALS", "Set decimal places in geographic coordinates");
    info.option ("-D, --delimit=STRING", "Set column delimiter");
    info.option ("-h, --help", "Show help message");
    info.option ("-H, --header", "Print output table header");
    info.option ("-i, --imagecoords", "Print image row/column coordinates");
    info.option ("-m, --match=PATTERN", "Sample only variables matching regular expression");
    info.option ("-M, --missing=VALUE", "Set value to print for missing data");
    info.option ("-n, --nocoords", "Do not print geographic coordinates");
    info.option ("-R, --reverse", "Reverse order of geographic coordinates");
    info.option ("-V, --variable=NAME1[/NAME2/...]", "Sample only variables listed");
    info.option ("--version", "Show version information");
    
    return (info);
    
  } // usage

  ////////////////////////////////////////////////////////////

  private cwsample () { }

  ////////////////////////////////////////////////////////////

} // cwsample

////////////////////////////////////////////////////////////////////////

