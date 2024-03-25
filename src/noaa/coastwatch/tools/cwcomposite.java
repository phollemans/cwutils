////////////////////////////////////////////////////////////////////////
/*

     File: cwcomposite.java
   Author: Peter Hollemans
     Date: 2003/04/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;
import java.text.NumberFormat;

import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.CWHDFReader;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.CachedGrid;
import noaa.coastwatch.io.HDFCachedGrid;
import noaa.coastwatch.io.tile.TilingScheme;

import noaa.coastwatch.tools.CleanupHook;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.ArrayReduction;
import noaa.coastwatch.util.MinReduction;
import noaa.coastwatch.util.MaxReduction;
import noaa.coastwatch.util.MeanReduction;
import noaa.coastwatch.util.GeoMeanReduction;
import noaa.coastwatch.util.MedianReduction;
import noaa.coastwatch.util.LastReduction;
import noaa.coastwatch.util.MetadataServices;

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.DataChunk.DataType;
import noaa.coastwatch.util.chunk.ChunkCollector;
import noaa.coastwatch.util.chunk.CompositeMapApplicationCollector;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.ChunkConsumer;
import noaa.coastwatch.util.chunk.GridChunkProducer;
import noaa.coastwatch.util.chunk.GridChunkConsumer;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.ChunkComputation;
import noaa.coastwatch.util.chunk.ChunkComputationHelper;
import noaa.coastwatch.util.chunk.ChunkOperation;
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.ChunkFunction;
import noaa.coastwatch.util.chunk.PoolProcessor;
import noaa.coastwatch.util.chunk.CompositeFunction;
import noaa.coastwatch.util.chunk.CompositeMapFunction;
import noaa.coastwatch.util.chunk.CompositeMapApplicationFunction;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

import java.util.logging.Logger;
import java.util.logging.Level;

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
 *   cwcomposite [OPTIONS] --inputs=FILE output
 * </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -c, --coherent=VARIABLE1[/VARIABLE2[...]] <br>
 * -h, --help <br>
 * -k, --keephistory <br>
 * -m, --match=PATTERN <br>
 * -M, --method=TYPE <br>
 * -o, --optimal=VARIABLE/TYPE <br>
 * -p, --pedantic <br>
 * -S, --savemap <br>
 * --serial <br>
 * --threads=MAX <br>
 * -t, --collapsetime <br>
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
 *   <dt> --inputs=FILE </dt>
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
 *   <dd>Turns on coherent mode (can only be used with <b>--method
 *   latest</b>, <b>--method explicit</b>, or <b>--method optimal</b>).  
 *   In coherent mode, the output
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
 *   <dt>-k, --keephistory</dt>
 *
 *   <dd>Turns keep history mode on.  In keep history mode, the processing
 *   commands used to create each input file are combined together, along
 *   with the composite command.  The combined history of all processing commands
 *   from all input files can be very large and in many cases overflows the maximum
 *   length for history metadata in the output file, so by default only the
 *   composite command is written to the output file history.</dd>
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
 *     <li>optimal - Finds the best pixel from the set of input files
 *     based on the minimum or maximum of an optimization variable.
 *     See the <b>--optimal</b> option for more details.  This method
 *     turns on coherent mode as specified by the <b>--coherent</b> 
 *     option.</li>
 * 
 *   </ul>
 *   The default is to compute the mean value.</dd>
 *   
 *   <dt>-o, --optimal=VARIABLE/TYPE</dt>
 *   
 *   <dd>The optimization variable and type to use for optimal compositing (see
 *   <b>--method optimal</b> above).
 *   The type may be either 'min' or 'max'.  Optimal compositing is done by 
 *   searching for the input file whose data value is either minimum or 
 *   maximum.  The output values for all variables at a given pixel location are 
 *   then guaranteed to originate from this same input file.  By default,
 *   the input files are searched for a satellite zenith angle variable and optimized
 *   by selecting the minimum satellite zenith angle.</dd>
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
 *   <dt>-S, --savemap</dt>
 *
 *   <dd>Saves the source index mapping to the output file in coherent mode.  
 *   The variable 'source_index' is written as a 16-bit signed integer, 
 *   and for each destination pixel in the output, represents the 0-based index 
 *   of the input file used to copy variable values into the output file.
 *   An attribute is written to the mapping variable specifying, in index order,
 *   the names of the input files used.  See the <b>--coherent</b> option
 *   above for details on coherent mode.</dd>
 *
 *   <dt>--serial</dt>
 *
 *   <dd>Turns on serial processing mode.  By default the program will
 *   use multiple processors in parallel to process chunks of data.</dd>
 *
 *   <dt>--threads=MAX</dt>
 *
 *   <dd>Specifies the maximum number of threads for parallel processing. By
 *   default the program will automatically detect the maximum number of
 *   threads possible.</dd>
 *
 *   <dt>-t, --collapsetime</dt>
 *
 *   <dd>Specifies that the time metadata in the output file
 *   should be simplified and collapsed into a single period with start
 *   date/time, and end date/time. By default the output file contains
 *   the full time period metadata with the time periods from all of the input
 *   files preserved.</dd>
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
 *   <li> Invalid command line option </li>
 *   <li> Invalid input or output file names </li>
 *   <li> Unsupported input file format </li>
 *   <li> Input file earth transforms do not match </li>
 *   <li> No matching variables found </li>
 *   <li> Unsupported composite method </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the combination of 24 hours of Himawari-8 NetCDF
 * SST data files into one median composite file, using a maximum
 * heap size of 8 Gb and at least 5 valid pixels per median calculation:</p>
 * <pre>
 *   phollema$ cwcomposite -J-Xmx8g -v --method median --valid=5
 *     --match sea_surface_temperature 2019*.nc composite.hdf
 *   [INFO] Checking input file information
 *   [INFO] Checking input file [1/24] 20190410000000-AHI_H08.nc
 *   [INFO] Adding sea_surface_temperature to composite output variables
 *   [INFO] Checking input file [2/24] 20190410010000-AHI_H08.nc
 *   [INFO] Checking input file [3/24] 20190410020000-AHI_H08.nc
 *   [INFO] Checking input file [4/24] 20190410030000-AHI_H08.nc
 *   [INFO] Checking input file [5/24] 20190410040000-AHI_H08.nc
 *   [INFO] Checking input file [6/24] 20190410050000-AHI_H08.nc
 *   [INFO] Checking input file [7/24] 20190410060000-AHI_H08.nc
 *   [INFO] Checking input file [8/24] 20190410070000-AHI_H08.nc
 *   [INFO] Checking input file [9/24] 20190410080000-AHI_H08.nc
 *   [INFO] Checking input file [10/24] 20190410090000-AHI_H08.nc
 *   [INFO] Checking input file [11/24] 20190410100000-AHI_H08.nc
 *   [INFO] Checking input file [12/24] 20190410110000-AHI_H08.nc
 *   [INFO] Checking input file [13/24] 20190410120000-AHI_H08.nc
 *   [INFO] Checking input file [14/24] 20190410130000-AHI_H08.nc
 *   [INFO] Checking input file [15/24] 20190410140000-AHI_H08.nc
 *   [INFO] Checking input file [16/24] 20190410150000-AHI_H08.nc
 *   [INFO] Checking input file [17/24] 20190410160000-AHI_H08.nc
 *   [INFO] Checking input file [18/24] 20190410170000-AHI_H08.nc
 *   [INFO] Checking input file [19/24] 20190410180000-AHI_H08.nc
 *   [INFO] Checking input file [20/24] 20190410190000-AHI_H08.nc
 *   [INFO] Checking input file [21/24] 20190410200000-AHI_H08.nc
 *   [INFO] Checking input file [22/24] 20190410210000-AHI_H08.nc
 *   [INFO] Checking input file [23/24] 20190410220000-AHI_H08.nc
 *   [INFO] Checking input file [24/24] 20190410230000-AHI_H08.nc
 *   [INFO] Creating output file composite.hdf
 *   [INFO] Total grid size is 5500x5500
 *   [INFO] Found 8 processor(s) to use
 *   [INFO] Creating sea_surface_temperature variable with chunk size 1834x1834
  * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.4
 */
public final class cwcomposite {

  private static final String PROG = cwcomposite.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

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
    CmdLineParser cmd = new CmdLineParser();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option collapsetimeOpt = cmd.addBooleanOption ('t', "collapsetime");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option methodOpt = cmd.addStringOption ('M', "method");
    Option validOpt = cmd.addIntegerOption ('V', "valid");
    Option pedanticOpt = cmd.addBooleanOption ('p', "pedantic");
    Option inputsOpt = cmd.addStringOption ('i', "inputs");
    Option coherentOpt = cmd.addStringOption ('c', "coherent");
    Option serialOpt = cmd.addBooleanOption ("serial");
    Option keephistoryOpt = cmd.addBooleanOption ('k', "keephistory");
    Option versionOpt = cmd.addBooleanOption ("version");
    Option threadsOpt = cmd.addIntegerOption ("threads");
    Option optimalOpt = cmd.addStringOption ('o', "optimal");
    Option savemapOpt = cmd.addBooleanOption ('S', "savemap");

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
    String[] remainingArgs = cmd.getRemainingArgs();
    if (remainingArgs.length < NARGS) {
      LOGGER.warning ("At least " + NARGS + " argument(s) required");
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // if
    String output = remainingArgs[remainingArgs.length-1];

    // Set defaults
    // ------------
    boolean verbosePrinting = (cmd.getOptionValue (verboseOpt) != null);
    if (verbosePrinting) VERBOSE.setLevel (Level.INFO);
    String match = (String) cmd.getOptionValue (matchOpt);
    String method = (String) cmd.getOptionValue (methodOpt);
    if (method == null) method = "mean";
    Integer validObj = (Integer) cmd.getOptionValue (validOpt);
    int minValid = (validObj == null ? 1 : validObj.intValue());
    boolean pedanticOutput = (cmd.getOptionValue (pedanticOpt) != null);
    String inputs = (String) cmd.getOptionValue (inputsOpt);
    String coherent = (String) cmd.getOptionValue (coherentOpt);
    boolean serialOperations = (cmd.getOptionValue (serialOpt) != null);
    boolean collapseTime = (cmd.getOptionValue (collapsetimeOpt) != null);
    boolean keepHistory = (cmd.getOptionValue (keephistoryOpt) != null);
    Integer threadsObj = (Integer) cmd.getOptionValue (threadsOpt);
    int threads = (threadsObj == null ? -1 : threadsObj.intValue());
    String optimal = (String) cmd.getOptionValue (optimalOpt);
    boolean saveMap = (cmd.getOptionValue (savemapOpt) != null);

    // Check for coherent mode
    // -----------------------
    boolean coherentMode = false;
    if (method.equals ("optimal")) coherentMode = true;
    else if (coherent != null) coherentMode = true;

    List<String> coherentVars = null;
    if (coherent != null) {
      if (!(method.equals ("latest") || method.equals ("explicit") || method.equals ("optimal"))) {
        LOGGER.severe ("Coherent mode cannot by used with method '" + method + "'");
        ToolServices.exitWithCode (2);
        return;
      } // if
      else coherentVars = Arrays.asList (coherent.split (ToolServices.getSplitRegex()));
    } // if

    // Read input filenames from file
    // ------------------------------
    List<String> inputFileList;
    if (inputs != null) {
      inputFileList = new ArrayList<>();
      try {
        BufferedReader reader;
        if (inputs.equals ("-"))
          reader = new BufferedReader (new InputStreamReader (System.in));
        else
          reader = new BufferedReader (new FileReader (inputs));
        while (reader.ready()) {
          String fileName = reader.readLine().trim();
          inputFileList.add (fileName);
        } // while
        reader.close();
      } // try
      catch (IOException e) {
        LOGGER.log (Level.SEVERE, "Error parsing inputs file", e);
        ToolServices.exitWithCode (2);
        return;
      } // catch
    } // if

    // Get input filenames from command line
    // -------------------------------------
    else {
      inputFileList = Arrays.asList (remainingArgs).subList (0, remainingArgs.length-1);
    } // else

    // Check input file count
    // ----------------------
    if (inputFileList.size() == 0) {
      LOGGER.severe ("At least one input file must be specified");
      ToolServices.exitWithCode (2);
      return;
    } // if

    try {

      // Loop over each input file
      // -------------------------
      EarthTransform earthTransform = null;
      TreeSet<String> variableNames = new TreeSet<>();
      List<EarthDataInfo> inputInfoList = new ArrayList<>();
      Map<String, EarthDataReader> readerMap = new HashMap<>();
      VERBOSE.info ("Checking input file information");
      int inputNumber = 1;
      for (String inputFile : inputFileList) {

        // Open file
        // ---------
        VERBOSE.info ("Checking input file [" + inputNumber + "/" + inputFileList.size() + "] " + inputFile);
        EarthDataReader reader = EarthDataReaderFactory.create (inputFile);
        readerMap.put (inputFile, reader);
  
        // Get info and add to list
        // ------------------------
        EarthDataInfo info = reader.getInfo();
        if (!keepHistory) info.clearHistory();
        inputInfoList.add (info);

        // Check earth transform matches
        // -----------------------------
        EarthTransform readerTransform = info.getTransform();
        if (earthTransform == null)
          earthTransform = readerTransform;
        else if (!earthTransform.equals (readerTransform)) {
          LOGGER.severe ("Non-matching earth transforms detected between inputs " + inputFile + " and " + inputFileList.get (0));
          ToolServices.exitWithCode (2);
          return;
        } // else if

        // Get grid names
        // --------------
        for (String variableName : reader.getAllGrids()) {
          boolean nameValid = (match == null ? true : variableName.matches (match));
          if (nameValid) {
            if (!variableNames.contains (variableName)) {
              VERBOSE.info ("Adding " + variableName + " to composite output variables");
              variableNames.add (variableName);
            } // if
          } // if
        } // for

        inputNumber++;

      } // for

      // Check variable names
      // --------------------
      if (variableNames.size() == 0) {
        LOGGER.severe ("No valid composite variables found");
        ToolServices.exitWithCode (2);
        return;
      } // if

      // Sort input files by date for latest type composite
      // --------------------------------------------------
      if (method.equals ("latest")) {
        List<Entry<String, EarthDataInfo>> entryList = new ArrayList<>();
        for (int i = 0; i < inputFileList.size(); i++)
          entryList.add (new SimpleEntry (inputFileList.get (i), inputInfoList.get (i)));
        entryList.sort (Comparator.comparing (Entry::getValue, Comparator.comparing (EarthDataInfo::getDate)));
        inputFileList = entryList.stream().map (Entry::getKey).collect (Collectors.toList());
      } // if

      // Create output file
      // ------------------
      VERBOSE.info ("Creating output file " + output);
      EarthDataInfo outputInfo = inputInfoList
        .stream()
        .reduce (pedanticOutput ? EarthDataInfo::appendWithDuplicates : EarthDataInfo::appendWithoutDuplicates)
        .get();
      if (collapseTime) outputInfo.collapseTimePeriods();
      CleanupHook.getInstance().scheduleDelete (output);
      CWHDFWriter writer = new CWHDFWriter (outputInfo, output);

      // Report computation properties
      // -----------------------------
      int[] dims = earthTransform.getDimensions();
      VERBOSE.info ("Total grid size is " + dims[ROW] + "x" + dims[COL]);

      int maxOps = 0;
      if (!serialOperations) {
        int processors = Runtime.getRuntime().availableProcessors();
        if (threads < 0) maxOps = processors;
        else maxOps = Math.min (threads, processors);
        if (maxOps < 1) maxOps = 1;
        VERBOSE.info ("Using max " + maxOps + " parallel threads for processing");
      } // if

      // If operating in coherent mode, we first need to create the map of
      // input file index for each pixel.  Then we transfer the data 
      // into the output file after that.
      EarthDataReader coherentMapReader = null;
      ChunkProducer mapChunkProducer = null;
      if (coherentMode) {

        ChunkCollector coherentCollector = new ChunkCollector();
        Map<IntArray,Integer> chunkSizeMap = new HashMap<>();

        // Start by setting up the chunk producers and comparator for the 
        // optimization if needed.
        String optVarName = null;
        Comparator<Double> optComparator = null;
        var coherentVarNames = new ArrayList<String>();
        if (method.equals ("optimal")) {

          // If we've been given the optimal option, then extract the variable
          // name and the comparator type.
          if (optimal != null) {
            String[] optArray = optimal.split (ToolServices.getSplitRegex());
            if (optArray.length != 2) {
              LOGGER.severe ("Invalid optimal option '" + optimal + "'");
              ToolServices.exitWithCode (2);
              return;
            } // if
            optVarName = optArray[0];
            var optType = optArray[1];
            if (optType.equals ("max")) optComparator = (a,b) -> Double.compare (a, b);
            else if (optType.equals ("min")) optComparator = (a,b) -> Double.compare (b, a);
            else {
              LOGGER.severe ("Invalid optimization type '" + optType + "', specify either min or max");
              ToolServices.exitWithCode (2);
              return;
            } // else
          } // if         

          // If we haven't been given an optimal option, try looking for the 
          // satellite zenith angle, and set the comparator to select the
          // minimum angle as the optimal.
          else {
            var reader = readerMap.get (inputFileList.get (0));
            optVarName = reader.findVariable (List.of ("satellite zenith", "sat zenith", "sensor zenith"), 0.8);
            if (optVarName == null) {
              LOGGER.severe ("Cannot locate satellite zenith angle data, use --optimal option");
              ToolServices.exitWithCode (2);
              return;
            } // if
            VERBOSE.info ("Using variable " + optVarName + " with optimization type min");
            optComparator = (a,b) -> Double.compare (b, a);
          } // else

          // Now create chunk producers for the optimization using the 
          // variable data from each input file.  We need all the files to
          // contain optimization data.
          for (String inputFile : inputFileList) {
            EarthDataReader reader = readerMap.get (inputFile);
            if (!reader.containsVariable (optVarName)) {
              LOGGER.severe ("Optimization variable " + optVarName + " not found in input file " + inputFile);
              ToolServices.exitWithCode (2);
              return;
            } // if
            else {
              var producer = reader.getChunkProducer (optVarName);
              coherentCollector.addProducer (producer);

              // Add the chunk size to a map here -- we'll use it later to
              // optimize the coherent map chunk size.
              var scheme = producer.getNativeScheme();
              if (scheme != null) {
                var size = scheme.getChunkSize();
                var key = new IntArray (size);
                chunkSizeMap.compute (key, (k,v) -> (v == null) ? 0 : v + 1);
              } // if

            } // else
          } // for
          coherentVarNames.add (optVarName);

        } // if

        // Next, set up the chunk producers for the priority variables.
        if (coherentVars != null) {
          for (String coherentVar : coherentVars) {
            for (String inputFile : inputFileList) {
              EarthDataReader reader = readerMap.get (inputFile);
              if (!reader.containsVariable (coherentVar)) {
                LOGGER.severe ("Coherent mode variable " + coherentVar + " not found in input file " + inputFile);
                ToolServices.exitWithCode (2);
                return;
              } // if
              else {
                ChunkProducer producer = reader.getChunkProducer (coherentVar);
                coherentCollector.addProducer (producer);

                // Add the chunk size to a map here -- we'll use it later to
                // optimize the coherent map chunk size.
                var scheme = producer.getNativeScheme();
                if (scheme != null) {
                  var size = scheme.getChunkSize();
                  var key = new IntArray (size);
                  chunkSizeMap.compute (key, (k,v) -> (v == null) ? 0 : v + 1);
                } // if

              } // else
            } // for
            coherentVarNames.add (coherentVar);
          } // for
        } // if

        // If the savemap option is used, we write the coherent map data 
        // directly into the output file.  Otherwise, we create a temporary 
        // file here to hold the map data and schedule the temporary file 
        // for deletion.
        CWHDFWriter coherentMapWriter;
        String tempFileName;
        if (saveMap) {
          tempFileName = null;
          coherentMapWriter = writer;
        } // if
        else {
          tempFileName = File.createTempFile ("map", ".hdf").getPath();
          CleanupHook.getInstance().scheduleDelete (tempFileName);
          LOGGER.fine ("Creating temporary integer map file " + tempFileName);
          coherentMapWriter = new CWHDFWriter (outputInfo, tempFileName);
        } // else

        // Set up the tile size in the writer so that it matches the most
        // commonly used tile size from the input files.
        var intArray = chunkSizeMap.entrySet().stream().max (Comparator.comparing (Entry::getValue)).get().getKey();
        coherentMapWriter.setTileDims (intArray.data);

        // In the file, create a new short integer grid and also the 
        // chunk consumer that we'll use to run the computation.
        String coherentMapVarName = "source_index";
        var mapGrid = new Grid (coherentMapVarName, "Composite source input index", 
          null, dims[Grid.ROWS], dims[Grid.COLS], new short[0], 
          NumberFormat.getIntegerInstance(), null, Short.MIN_VALUE);
        String inputFiles = null;
        for (var file : inputFileList) inputFiles = MetadataServices.append (inputFiles, file);
        mapGrid.getMetadataMap().put ("input_files", inputFiles);
        var cachedMapGrid = new HDFCachedGrid (mapGrid, coherentMapWriter);
        var mapConsumer = new GridChunkConsumer (cachedMapGrid);
        var scheme = mapConsumer.getNativeScheme();
        int[] chunkSize = scheme.getChunkSize();
        if (saveMap) {
          VERBOSE.info ("Creating " + coherentMapVarName +
            " variable in output file with chunk size " + chunkSize[ROW] + "x" + chunkSize[COL]);
        } // if
        else {
          VERBOSE.info ("Creating " + coherentMapVarName +
            " variable in temporary cache file with chunk size " + chunkSize[ROW] + "x" + chunkSize[COL]);
        } // if

        // Perform the computation to get the integer coherent mapping.
        VERBOSE.info ("Computing coherent integer map from inputs " + Arrays.toString (coherentVarNames.toArray()));
        int chunkCount = inputFileList.size();
        int coherentVarCount = coherentVars == null ? 0 : coherentVars.size();
        var mapFunction = new CompositeMapFunction (chunkCount, optComparator, coherentVarCount);
        var op = new ChunkComputation (coherentCollector, mapConsumer, mapFunction);
        ChunkComputationHelper.getInstance().run (op, scheme, serialOperations, maxOps, VERBOSE);

        // If we're saving the coherent map, then create a new reader from
        // the output file.  Otherwise, close the map writer and re-open as a 
        // reader so that we can use it as a chunk producer for the integer 
        // map for the next stage when we apply the map to the input chunks.
        cachedMapGrid.flush();
        cachedMapGrid.clearCache();
        coherentMapWriter.close();
        if (saveMap) {
          writer = new CWHDFWriter (output);
          coherentMapReader = new CWHDFReader (writer);
        } // if
        else {
          coherentMapReader = new CWHDFReader (tempFileName);
        } // else
        mapChunkProducer = coherentMapReader.getChunkProducer (coherentMapVarName);

      } // if

      // When operating in non-coherent mode, we need to set the array operator
      // here that will be used in the function for compositing each chunk of 
      // data.
      ArrayReduction operator = null;
      boolean valueOrderMethod = false;
      if (!coherentMode) {
        if (method.equals ("mean")) operator = new MeanReduction();
        else if (method.equals ("geomean")) operator = new GeoMeanReduction();
        else if (method.equals ("median")) operator = new MedianReduction();
        else if (method.equals ("min")) operator = new MinReduction();
        else if (method.equals ("max")) operator = new MaxReduction();
        else if (method.equals ("latest")) operator = new LastReduction();
        else if (method.equals ("explicit")) operator = new LastReduction();
        else {
          LOGGER.severe ("Unsupported composite method '" + method + "'");
          ToolServices.exitWithCode (2);
          return;
        } // else
        valueOrderMethod = (
          method.equals ("latest") || 
          method.equals ("explicit")
        );
      } // if

      // Now we loop over each variable.  In the case of arithmetic or ordering
      // operators, we can tolerate having variables exist in some data files
      // and not in others.  For the coherent mode though, we need all
      // compositing variables available in each file.  This is so that we
      // have coinsistency between the indices generated in the integer map,
      // and the indices of the chunks obtained from each input file.
      for (String variableName : variableNames) {

        // Start by creating a chunk collector.  In coherent mode, the first
        // chunk for the function will be the integer map chunk, and then
        // all the input variable chunks.  For arithmetic operators, we only
        // need the input variable chunks.
        ChunkCollector collector;
        if (coherentMode) {
          collector = new CompositeMapApplicationCollector();
          collector.addProducer (mapChunkProducer);
        } // if 
        else {
          collector = new ChunkCollector();
        } // else

        // Create and add a chunk producer for this variable for 
        // each input file.  Along the way, we need to save the external 
        // type so we can check if any variable has a different external
        // type -- that would completely sabotage the composite.
        DataChunk.DataType externalType = null;
        Grid protoGrid = null;
        for (String inputFile : inputFileList) {
          EarthDataReader reader = readerMap.get (inputFile);

          // Cancel the operation entirely if we find a missing variable
          // in coherent mode.
          if (!reader.containsVariable (variableName)) {
            if (coherentMode) {
              LOGGER.severe ("Input file " + inputFile + " does not contain variable " + variableName);
              LOGGER.severe ("In coherent mode, all input files must contain all composited variables");
              LOGGER.severe ("To specify a subset of variables, use the --match option");
              ToolServices.exitWithCode (2);
              return;
            } // if
          } // if

          else {
            if (protoGrid == null) protoGrid = (Grid) reader.getVariable (variableName);
            ChunkProducer producer = reader.getChunkProducer (variableName);
            if (externalType == null) externalType = producer.getExternalType();
            else if (externalType != producer.getExternalType()) {
              LOGGER.severe ("Non-matching external data types found between input files for variable " + variableName);
              ToolServices.exitWithCode (2);
              return;
            } // else if
            collector.addProducer (producer);
          } // else

        } // for
        
        // Warn the user about compositing variables that are integer types
        // using an arithmetic type of operator.  This could mean that mask
        // or quality variables are being averaged for example.  
        if (!coherentMode && !valueOrderMethod && !(externalType == DataType.FLOAT || externalType == DataType.DOUBLE)) {
          LOGGER.warning ("Composite method '" + method + "' used with variable " + variableName + " of external type " + externalType);
          LOGGER.warning ("For mask or quality flag variables, this may not be what you want");
        } // if

        // Create a new variable in the output file to hold the composite
        // data, and a chunk consumer to use in the operation.  Also configure
        // the output chunking scheme to match the input scheme if possible. 
        TilingScheme inputTilingScheme = protoGrid.getTilingScheme();
        if (inputTilingScheme != null)
          writer.setTileDims (inputTilingScheme.getTileDimensions());
        else
          writer.setTileDims (null);
        CachedGrid outputGrid = new HDFCachedGrid (protoGrid, writer);
        ChunkConsumer consumer = new GridChunkConsumer (outputGrid);

        // Find out what the output chunking scheme actually is and report
        // on the variable that we are about to composite.
        ChunkingScheme outputChunkingScheme = consumer.getNativeScheme();
        int[] chunkSize = outputChunkingScheme.getChunkSize();
        VERBOSE.info ("Creating " +  variableName +
          " variable with chunk size " + chunkSize[ROW] + "x" + chunkSize[COL]);

        // In coherent mode, create a chunk function to map the 
        // input data values into the output variable using the integer map.
        // Otherwise create a chunk function to composite the data
        // with an arimetic-based function.
        ChunkFunction function = null;
        DataChunk protoChunk = consumer.getPrototypeChunk();
        if (coherentMode)
          function = new CompositeMapApplicationFunction (inputFileList.size(), protoChunk);
        else 
          function = new CompositeFunction (operator, minValid, protoChunk);

        // Create and run a new chunk computation to composite the data.
        ChunkComputation op = new ChunkComputation (collector, consumer, function);
        ChunkComputationHelper.getInstance().run (op, outputChunkingScheme, serialOperations, maxOps, VERBOSE);

        // Flush any unwritten tiles in the output grid and clear its
        // cache since we won't be using it anymore.  This helps to keep the
        // overall memory footprint only as large as needed for each individual
        // variable composite.

        // TODO: Should we also do this for the input grids?

        outputGrid.flush();
        outputGrid.clearCache();

      } // for

      // Close the input files and the coherent map reader if applicable.
      for (String inputFile : inputFileList)
        readerMap.get (inputFile).close();
      if (coherentMapReader != null) coherentMapReader.close();

      // Close the output file.
      writer.close();
      CleanupHook.getInstance().cancelDelete (output);

    } // try

    catch (OutOfMemoryError | Exception e) {
      ToolServices.warnOutOfMemory (e);
      LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    ToolServices.finishExecution (PROG);

  } // main

  ////////////////////////////////////////////////////////////

  /** Holds an integer array for use as a map key. */
  private static class IntArray {

    public int[] data;

    public IntArray (int[] data) { this.data = data; }

    @Override
    public boolean equals (Object obj) {
      boolean result = false;
      if (obj instanceof IntArray) {
        result = Arrays.equals (this.data, ((IntArray) obj).data);
      } // if
      return (result);
    } // equals

    @Override
    public int hashCode() {
      return (Arrays.hashCode (data));
    } // hashCode

    @Override
    public String toString () {
      return ("IntArray" + Arrays.toString (data));
    } // toString


  } // IntArray

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cwcomposite");

    info.func ("Combines a time series of earth data");

    info.param ("input [input2 ...]", "Input data file(s)", 1);
    info.param ("output", "Output data file", 1);

    info.param ("--inputs=FILE", "Text file of input data file(s)", 2);
    info.param ("output", "Output data file", 2);

    info.option ("-c, --coherent=VAR1[/VAR2[...]]", "Use coherent mode with variables");
    info.option ("-h, --help", "Show help message");
    info.option ("-k, --keephistory", "Retain all input file history metadata");
    info.option ("-m, --match=PATTERN", "Composite only variables matching regular expression");
    info.option ("-M, --method=TYPE", "Set composite type");
    info.option ("-o, --optimal=VARIABLE/TYPE", "Set optimization variable and type");
    info.option ("-p, --pedantic", "Retain repeated metadata values");
    info.option ("-S, --savemap", "Save source index mapping in coherent mode");
    info.option ("--serial", "Perform serial operations");
    info.option ("--threads=MAX", "Set maximum number of parallel threads");
    info.option ("-t, --collapsetime", "Collapse and simplify time metadata");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("-V, --valid=COUNT", "Set minimum valid values");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwcomposite () { }

  ////////////////////////////////////////////////////////////

} // cwcomposite class

////////////////////////////////////////////////////////////////////////
