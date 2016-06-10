////////////////////////////////////////////////////////////////////////
/*
     FILE: cwcomposite.java
  PURPOSE: To combine earth data files by variable.
   AUTHOR: Peter Hollemans
     DATE: 2003/04/06
  CHANGES: 2004/09/28, PFH
           - modified to use EarthDataInfo.append()
           - modified to use ToolServices.setCommandLine()
           - added --pedantic option
           2004/09/29, PFH, added --inputs option
           2004/11/12, PFH, added stdin option for inputs file
           2005/01/30, PFH, modified to use CleanupHook class
           2005/03/15, PFH, reformatted documentation and usage note
           2006/07/10, PFH, added --coherent option
           2007/04/19, PFH, added version printing
           2008/07/30, HG, added computing geometric mean in computeComposite 
             method
           2016/06/09, PFH, updated documentation on method types

  CoastWatch Software Library and Utilities
  Copyright 1998-2016, USDOC/NOAA/NESDIS CoastWatch

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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.HDFCachedGrid;
import noaa.coastwatch.tools.CleanupHook;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * <p>The composite tool combines a time series of earth data.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwcomposite - combines a time series of earth data.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p>
 *   cwcomposite [OPTIONS] input [input2 ...] output<br>
 *   cwcomposite [OPTIONS] {-i, --inputs=FILE} output
 * </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -c, --coherent=VARIABLE1[/VARIABLE2[...]] <br>
 * -h, --help <br>
 * -m, --match=PATTERN <br>
 * -M, --method=TYPE <br>
 * -p, --pedantic <br>
 * -v, --verbose <br>
 * -V, --valid=COUNT <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p> The composite tool combines a time series of earth data.
 * Data variables are combined on a pixel-by-pixel basis using
 * one of several statistical or temporal methods: mean, geometric mean, median,
 * minimum, maximum, explicit or latest.  The input files must have
 * matching earth transforms but may have different dates.  The
 * composite tool may be used, for example, to combine a number
 * of sea-surface-temperature datasets into one in order to
 * obtain a mean SST for a certain region and help eliminate
 * cloud.  Another use is to combine datasets from different
 * regions that are registered to the same earth transform to
 * create a mosaic.  The output dataset is constructed using
 * metadata from each input dataset so that it properly reflects
 * the different input dataset dates and other metadata. </p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> input [input2 ...] </dt>
 *   <dd> The input data file names.  At least one input file is required,
 *   unless the <b>--inputs</b> option is used.  If multiple files
 *   are specified, they must have matching earth transforms. </dd>
 *
 *   <dt> -i, --inputs=FILE </dt>
 *   <dd> The file name containing a list of input data files.  The file
 *   must be an ASCII text file containing input file names, one per
 *   line.  If multiple files are listed, they must have matching
 *   earth transforms.  If the inputs file name is '-', input is read
 *   from standard input.</dd>
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
 *   <dt>-c, --coherent=VARIABLE1[/VARIABLE2[...]]</dt>
 *
 *   <dd>Turns on coherent mode (only valid with <b>--method
 *   latest or --method explicit</b>).  In coherent mode, the output
 *   values for all variables at a given pixel location are 
 *   guaranteed to originate from the same input file.  The specified
 *   variable list is used to prioritize variables to check for a 
 *   valid latest/last value.  If there are no valid values for the 
 *   first variable at a given location, then the next variable is
 *   checked and so on until the latest/last valid value is found.
 *   This mode is useful for when data variables and their
 *   respective quality flags should be kept together during a
 *   composite operation.  Without this option, the 'latest' and
 *   'explicit' composite methods may select the latest/last valid 
 *   data value from one input file, and the latest/last valid 
 *   quality flag from another input file for a given location.</dd>
 *
 *   <dt>-h, --help</dt>
 *
 *   <dd>Prints a brief help message.</dd>
 *
 *   <dt>-m, --match=PATTERN</dt>
 *
 *   <dd>The variable name matching pattern.  If specified, the
 *   pattern is used as a regular expression to match variable names.
 *   Only variables matching the pattern will be present in the
 *   output.  By default, no pattern matching is performed and all
 *   variables are combined.</dd>
 *
 *   <dt>-M, --method=TYPE</dt>
 *
 *   <dd>The composite method.  Valid methods are:
 *   <ul>
 *
 *     <li>mean - Computes the arithmetic mean or average value (sum of
 *     values over n)</li>
 *
 *     <li>geomean - Computes the geometric mean (nth root of product
 *     of values)</li>
 *
 *     <li>median - Finds the median value (middle value of n values)</li>
 *
 *     <li>min - Finds the minimum value</li>
 *
 *     <li>max - Finds the maximum value</li>
 *
 *     <li>latest - Finds the most recent valid value (latest in time
 *     according to the data time stamp)</li>
 *
 *     <li>explicit - Finds the last valid value in the set of input
 *     files, according to the explicit order given on the command line.
 *     This would yield the same results as the 'latest' method if the
 *     files were listed in chronological order on the command line.</li>
 *
 *   </ul>
 *   The default is to compute the mean value.</dd>
 *   
 *   <dt>-p, --pedantic</dt>
 *
 *   <dd>Turns pedantic mode on.  In pedantic mode, metadata from
 *   each input file is combined exactly such that composite
 *   attributes in the output file may contain repeated values.  When
 *   pedantic mode is off, composite attributes are collapsed so that
 *   only unique values appear.  By default, pedantic mode is
 *   off.</dd>
 *
 *   <dt>-v, --verbose</dt>
 *
 *   <dd>Turns verbose mode on.  The current status of data
 *   combination is printed periodically.  The default is to run
 *   quietly.</dd>
 *
 *   <dt>-V, --valid=COUNT</dt>
 *
 *   <dd>The minimum number of valid values required to form an
 *   aggregate function.  By default, only one value per pixel is
 *   required.  If the actual number of valid values is below this
 *   threshold, the output value is set to invalid.</dd>
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
 *   <li> Input file earth transforms do not match. </li>
 *   <li> No matching variables found. </li>
 *   <li> Unsupported composite method. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the combination of several Earth
 * datasets into one using the 'latest' composite method:</p>
 * <pre>
 *   phollema$ cwcomposite -v --method latest 2003_097_1428_n17_wi_na.hdf
 *     2003_097_1607_n17_wi_na.hdf 2003_097_1751_n17_mo_na.hdf
 *     2003_097_1931_n17_mo_na.hdf 2003_097_n17_na.hdf
 *
 *  cwcomposite: Reading input 2003_097_1428_n17_wi_na.hdf
 *  cwcomposite: Adding avhrr_ch1 to composite variables
 *  cwcomposite: Adding avhrr_ch2 to composite variables
 *  cwcomposite: Adding avhrr_ch4 to composite variables
 *  cwcomposite: Reading input 2003_097_1607_n17_wi_na.hdf
 *  cwcomposite: Reading input 2003_097_1751_n17_mo_na.hdf
 *  cwcomposite: Reading input 2003_097_1931_n17_mo_na.hdf
 *  cwcomposite: Creating output 2003_097_n17_na.hdf
 *  cwcomposite: Writing avhrr_ch1
 *  cwcomposite: Writing avhrr_ch2
 *  cwcomposite: Writing avhrr_ch4
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.4
 */
public final class cwcomposite {

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  /** Name of program. */
  private static final String PROG = "cwcomposite";

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
    CmdLineParser cmd = new CmdLineParser();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option methodOpt = cmd.addStringOption ('M', "method");
    Option validOpt = cmd.addIntegerOption ('V', "valid");
    Option pedanticOpt = cmd.addBooleanOption ('p', "pedantic");
    Option inputsOpt = cmd.addStringOption ('i', "inputs");
    Option coherentOpt = cmd.addStringOption ('c', "coherent");
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

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    String match = (String) cmd.getOptionValue (matchOpt);
    String method = (String) cmd.getOptionValue (methodOpt);
    if (method == null) method = "mean";
    Integer validObj = (Integer) cmd.getOptionValue (validOpt);
    int valid = (validObj == null? 1 : validObj.intValue());
    boolean pedantic = (cmd.getOptionValue (pedanticOpt) != null);
    String inputs = (String) cmd.getOptionValue (inputsOpt);
    String coherent = (String) cmd.getOptionValue (coherentOpt);

    // Check for coherent mode
    // -----------------------
    if (coherent != null && 
        !(method.equals ("latest") || method.equals("explicit"))) {
      System.err.println (PROG + ": Coherent mode can only be used with " +
        "--method latest or --method explicit");
      System.exit (2);
    } // if

    // Read inputs from file
    // ---------------------
    String[] input;
    if (inputs != null) {
      List fileNameList = new ArrayList();
      try {
        BufferedReader reader;
        if (inputs.equals ("-"))
          reader = new BufferedReader (new InputStreamReader (System.in));
        else
          reader = new BufferedReader (new FileReader (inputs));
        while (reader.ready()) {
          String fileName = reader.readLine().trim();
          fileNameList.add (fileName);
        } // while
        reader.close();
      } // try
      catch (IOException e) {
        System.err.println (PROG + ": Error parsing inputs file");
        e.printStackTrace();
        System.exit (2);
      } // catch
      input = (String[]) fileNameList.toArray (new String[] {});
    } // if

    // Get inputs from command line
    // ----------------------------
    else {
      input = new String [remain.length-1];
      System.arraycopy (remain, 0, input, 0, input.length);
    } // else

    // Check input file count
    // ----------------------
    if (input.length == 0) {
      System.err.println (PROG + ": At least one input file" +
        " must be specified");
      System.exit (2);
    } // if

    try {

      // Loop over each input file
      // -------------------------
      EarthDataReader[] readers = new EarthDataReader[input.length];
      EarthTransform trans = null;
      TreeSet variableNames = new TreeSet();
      for (int i = 0; i < input.length; i++) {

        // Open file
        // ---------
        if (verbose) System.out.println (PROG + ": Reading input " + input[i]);
        readers[i] = EarthDataReaderFactory.create (input[i]);

        // Check transform
        // ---------------
        if (i == 0)
          trans = readers[i].getInfo().getTransform();
        else if (!trans.equals (readers[i].getInfo().getTransform())) {
          System.err.println (PROG + ": Earth transforms do not match for " +
            input[i] + " and " + input[0]);
          System.exit (2);
        } // else if

        // Get variable names
        // ------------------
        for (int j = 0; j < readers[i].getVariables(); j++) {
          DataVariable var = readers[i].getPreview (j);
          if (var instanceof Grid) {
            String varName = var.getName();
            if (match != null && !varName.matches (match)) continue;
            if (variableNames.contains (varName)) continue;
            if (verbose) System.out.println (PROG + ": Adding " + varName +
              " to composite variables");
            variableNames.add (varName);
          } // if
        } // for

      } // for

      // Check variable names
      // --------------------
      if (variableNames.size() == 0) {
        System.err.println (PROG + ": No valid variables found in input");
        System.exit (2);
      } // if

      // Sort readers by date if the latest method is specified
      // ------------------------------------------------------
      if (method.equals ("latest")) {
        Arrays.sort (readers, new ReaderComparator());
      } // if

      // Create output file
      // ------------------
      EarthDataInfo info = readers[0].getInfo();
      for (int i = 1; i < readers.length; i++)
        info = info.append (readers[i].getInfo(), pedantic);
      if (verbose) System.out.println (PROG + ": Creating output " + output);
      CleanupHook.getInstance().scheduleDelete (output);

      // TODO: There is a problem here when the history of the output
      // file is too large.  The HDF library complains when there are
      // more than 65535 values.  The HDFWriter class currently
      // truncates at 65535, but it would be nice if there was a terse
      // mode that simply output the input file names rather than
      // truncating the history.

      CWHDFWriter writer = new CWHDFWriter (info, output);

      // Run in coherent mode
      // --------------------
      if (coherent != null) {

        // Check coherent variables
        // ------------------------
        String[] coherentVarNames = coherent.split (ToolServices.SPLIT_REGEX);
        for (int i = 0; i < coherentVarNames.length; i++) {
          if (!variableNames.contains (coherentVarNames[i])) {
            System.err.println (PROG + ": Cannot find coherent mode " +
              "variable '" + coherentVarNames[i] + "' among input variables");
            System.exit (2);
          } // if
        } // for

        // Get all input variables
        // -----------------------
        int numVars = variableNames.size();
        DataVariable[][] inputVarArrays = new DataVariable[numVars][];
        int[] coherentVarIndicies = new int[coherentVarNames.length];
        int varNameIndex = 0, coherentVarNameIndex = 0;
        for (Iterator iter = variableNames.iterator(); iter.hasNext();) {
          String varName = (String) iter.next();
          DataVariable[] vars = getInputVars (readers, varName, false);
          inputVarArrays[varNameIndex] = vars;
          if (varName.equals (coherentVarNames[coherentVarNameIndex])) {
            coherentVarIndicies[coherentVarNameIndex] = varNameIndex;
            coherentVarNameIndex++;
          } // if
          varNameIndex++;
        } // for

        // Create output variables
        // -----------------------
        DataVariable[] outputVars = new DataVariable[numVars];
        for (int i = 0; i < numVars; i++) {
          DataVariable inputVar = null;
          for (int j = 0; j < readers.length; j++) {
            if (inputVarArrays[i][j] != null) {
              inputVar = inputVarArrays[i][j];
              break;
            } // if
          } // for
          outputVars[i] = new HDFCachedGrid ((Grid) inputVar, writer);
        } // for

        // Compute composite
        // -----------------
        if (verbose) {
          System.out.println (PROG + ": Computing coherent composite using " +
            coherent);
        } // if
        computeCoherentComposite (inputVarArrays, coherentVarIndicies,
          outputVars);
        for (int i = 0; i < numVars; i++)
          ((HDFCachedGrid) outputVars[i]).flush();

      } // if

      // Run in normal mode
      // ------------------
      else {

        // Loop over each variable
        // -----------------------
        for (Iterator iter = variableNames.iterator(); iter.hasNext();) {

          // Get input variables
          // -------------------
          String varName = (String) iter.next();
          DataVariable[] vars = getInputVars (readers, varName, true);

          // Create composite variable
          // -------------------------
          HDFCachedGrid var = new HDFCachedGrid ((Grid) vars[0], writer);
          if (verbose) System.out.println (PROG + ": Writing "+var.getName());
          computeComposite (vars, var, method, valid);
          var.flush();

        } // for

      } // else

      // Close files
      // -----------
      writer.close();
      CleanupHook.getInstance().cancelDelete (output);
      for (int i = 0; i < readers.length; i++) readers[i].close();

    } // try
    catch (Exception e) {
      e.printStackTrace();
      System.exit (2);
    } // catch

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Gets an array of input variables of the same name from the
   * input files.
   *
   * @param readers the data reader to get variables from.
   * @param varName the variable name to read.
   * @param condense the condense flag, true to condense null
   * variables when the variable does not exist in the input
   * file, or false to preserve nulls.
   *
   * @return the array of input variables.
   *
   * @throws IOException if an error occurred getting the
   * variable from a reader.
   */
  private static DataVariable[] getInputVars (
    EarthDataReader[] readers,
    String varName,
    boolean condense
  ) throws IOException {

    // Get variable list
    // -----------------
    ArrayList varList = new ArrayList();
    for (int i = 0; i < readers.length; i++) {
      int index = readers[i].getIndex (varName);
      if (index == -1) {
        if (!condense) varList.add (null);
      } // if
      else {
        varList.add (readers[i].getVariable (index));
      } // else
    } // for

    return ((DataVariable[]) varList.toArray (new DataVariable[] {}));

  } // getInputVars

  ////////////////////////////////////////////////////////////

  /**
   * A reader comparator compares two earth data readers based on
   * date.  This is useful for sorting a set of readers into ascending
   * order by date.
   */
  private static class ReaderComparator
    implements Comparator {

    ////////////////////////////////////////////////////////

    /** Compares two readers by date. */
    public int compare (
      Object o1,
      Object o2
    ) {

      Date d1 = ((EarthDataReader) o1).getInfo().getDate();
      Date d2 = ((EarthDataReader) o2).getInfo().getDate();
      return (d1.compareTo (d2));

    } // compare

    ////////////////////////////////////////////////////////

  } // ReaderComparator

  ////////////////////////////////////////////////////////////

  /**
   * Computes the coherent composite output variables from the
   * specified input variables and coherent indices.
   *
   * @param inputVarArrays the array of input variable arrays of
   * size [variables][readers].
   * @param coIndicies the coherent variables to use as
   * indicies into the input variable arrays.
   * @param outputvars the output variables to write to.
   */
  private static void computeCoherentComposite (
    DataVariable[][] inputVarArrays,
    int[] coIndicies,
    DataVariable[] outputVars
  ) {

    // Initialize
    // ----------
    int[] dims = outputVars[0].getDimensions();
    DataLocation start = new DataLocation (dims.length);
    DataLocation end = new DataLocation (dims.length);
    for (int i = 0; i < dims.length; i++) end.set (i, dims[i]-1);
    int[] stride = new int[dims.length];
    Arrays.fill (stride, 1);
    DataLocation loc = (DataLocation) start.clone();
    int coVars = coIndicies.length;
    int readers = inputVarArrays[0].length;

    // Loop over each location
    // -----------------------
    do {

      // Get reader with latest valid value
      // ----------------------------------
      int reader = -1;
      coIndexLoop: for (int coIndex = 0; coIndex < coVars; coIndex++) {
        for (int rIndex = readers-1; rIndex >= 0; rIndex--) {
          DataVariable var = inputVarArrays[coIndicies[coIndex]][rIndex];
          if (var != null && !Double.isNaN (var.getValue (loc))) {
            reader = rIndex;
            break coIndexLoop;
          } // if
        } // for
      } // for

      // Write missing value to output
      // -----------------------------
      if (reader == -1) {
        for (int i = 0; i < outputVars.length; i++)
          outputVars[i].setValue (loc, Double.NaN);
      } // if

      // Write actual value to output
      // ----------------------------
      else {
        for (int i = 0; i < outputVars.length; i++) {
          outputVars[i].setValue (loc,
            inputVarArrays[i][reader].getValue (loc));
        } // for
      } // else

    } while (loc.increment (stride, start, end));

  } // computeCoherentComposite

  ////////////////////////////////////////////////////////////

  /**
   * Computes the composite variable from the specified inputs and
   * method.
   *
   * @param inputVars the input variables.  The variables are assumed to be
   * sorted in ascending temporal order unless using the 'explicit' method.
   * @param outputVar the output variable.
   * @param method the composite method.
   * @param valid the minimum number of valid values for aggregate methods.
   *
   * @throws UnsupportedOperationException is the composite method is not
   * supported.
   */
  private static void computeComposite (
    DataVariable[] inputVars,
    DataVariable outputVar,
    String method,
    int valid
  ) {

    // Initialize
    // ----------
    int[] dims = outputVar.getDimensions();
    DataLocation start = new DataLocation (dims.length);
    DataLocation end = new DataLocation (dims.length);
    for (int i = 0; i < dims.length; i++) end.set (i, dims[i]-1);
    int[] stride = new int[dims.length];
    Arrays.fill (stride, 1);
    DataLocation loc = (DataLocation) start.clone();

    // Compute minimum
    // ---------------
    if (method.equals ("min")) {
      do {
        double min = Double.MAX_VALUE;
        for (int j = 0; j < inputVars.length; j++) {
          double val = inputVars[j].getValue (loc);
          if (val < min) min = val;
        } // for
        if (min == Double.MAX_VALUE) min = Double.NaN;
        outputVar.setValue (loc, min);
      } while (loc.increment (stride, start, end));
    } // if

    // Compute maximum
    // ---------------
    else if (method.equals ("max")) {
      do {
        double max = -Double.MAX_VALUE;
        for (int j = 0; j < inputVars.length; j++) {
          double val = inputVars[j].getValue (loc);
          if (val > max) max = val;
        } // for
        if (max == -Double.MAX_VALUE) max = Double.NaN;
        outputVar.setValue (loc, max);
      } while (loc.increment (stride, start, end));
    } // else if

    // Compute latest
    // --------------
    else if (method.equals ("latest") || method.equals("explicit")) {
      do {
        double latest = Double.NaN;
        for (int j = inputVars.length-1; j >= 0; j--) {
          double val = inputVars[j].getValue (loc);
          if (!Double.isNaN (val)) { latest = val; break; }
        } // for
        outputVar.setValue (loc, latest);
      } while (loc.increment (stride, start, end));
    } // else if

    // Compute mean
    // ------------
    else if (method.equals ("mean")) {
      do {
        double sum = 0;
        int values = 0;
        for (int j = 0; j < inputVars.length; j++) {
          double val = inputVars[j].getValue (loc);
          if (!Double.isNaN (val)) {
            sum += val;
            values++;
          } // if
        } // for
        double mean = (values != 0 && values >= valid ? sum/values :
          Double.NaN);
        outputVar.setValue (loc, mean);
      } while (loc.increment (stride, start, end));
    } // else if

    // Compute geometric mean
    // ----------------------
    else if (method.equals ("geomean")) {
      do {
        double sum = 0;
        int values = 0;
        for (int j = 0; j < inputVars.length; j++) {
          double val = inputVars[j].getValue (loc);
          if (val > 0) {
            sum += Math.log(val);
            values++;
          } // if
        } // for
        double mean = (values != 0 && values >= valid ? Math.exp (sum/values) :
          Double.NaN);
        outputVar.setValue (loc, mean);
      } while (loc.increment (stride, start, end));
    } // else if

    // Compute median
    // --------------
    else if (method.equals ("median")) {
      double[] valueArray = new double[inputVars.length];
      do {
        int values = 0;
        for (int j = 0; j < inputVars.length; j++) {
          double val = inputVars[j].getValue (loc);
          if (!Double.isNaN (val)) { valueArray[values] = val; values++; }
        } // for
        double median;
        if (values != 0 && values >= valid) {
          Arrays.sort (valueArray, 0, values);
          if (values%2 == 0)
            median = (valueArray[values/2 - 1] + valueArray[values/2]) / 2;
          else
            median = valueArray[(values+1)/2 - 1];
        } // if
        else
          median = Double.NaN;
        outputVar.setValue (loc, median);
      } while (loc.increment (stride, start, end));
    } // else if

    // Report unsupported method
    // -------------------------
    else
      throw new UnsupportedOperationException (
        "Unsupported composite method '" + method + "'");

  } // computeComposite

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwcomposite [OPTIONS] input [input2 ...] output\n" +
"       cwcomposite [OPTIONS] {-i, --inputs=FILE} output\n" +
"Combines a time series of earth data.\n" +
"\n" +
"Main parameters:\n" +
"  input [input2 ...]         The input data file name(s).\n" +
"  -i, --inputs=FILE          The text file containing input file name(s)\n" +
"                              or '-' for stdin.\n" +
"  output                     The output data file name.\n" +
"\n" +
"Options:\n" +
"  -c, --coherent=VARIABLE1[/VARIABLE2[...]]\n" +
"                             Perform coherent mode composite.\n" +
"  -h, --help                 Show this help message.\n" +
"  -m, --match=PATTERN        Compute only variable names matching the\n" +
"                              pattern.\n" +
"  -M, --method=TYPE          Set composite method.  TYPE may be 'mean',\n" +
"                              'median', 'min', 'max', 'explicit' or \n" +
"                              'latest'.\n" +
"  -p, --pedantic             Retain all repeated metadata values.\n" +
"  -v, --verbose              Print verbose messages.\n" +
"  -V, --valid=COUNT          Set minimum valid value count to compute\n" +
"                              composite at each pixel.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

  private cwcomposite () { }

  ////////////////////////////////////////////////////////////

} // cwcomposite class

////////////////////////////////////////////////////////////////////////

