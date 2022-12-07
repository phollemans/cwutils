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

import noaa.coastwatch.io.CWHDFWriter;
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

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkCollector;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.ChunkConsumer;
import noaa.coastwatch.util.chunk.GridChunkProducer;
import noaa.coastwatch.util.chunk.GridChunkConsumer;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.ChunkComputation;
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.ChunkFunction;
import noaa.coastwatch.util.chunk.PoolProcessor;
import noaa.coastwatch.util.chunk.CompositeFunction;

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
 * -p, --pedantic <br>
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
    Option keephistoryOpt = cmd.addBooleanOption ("keepHistory");
    Option versionOpt = cmd.addBooleanOption ("version");
    Option threadsOpt = cmd.addIntegerOption ("threads");
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
    String coherentOutput = (String) cmd.getOptionValue (coherentOpt);
    boolean serialOperations = (cmd.getOptionValue (serialOpt) != null);
    boolean collapseTime = (cmd.getOptionValue (collapsetimeOpt) != null);
    boolean keepHistory = (cmd.getOptionValue (keephistoryOpt) != null);
    Integer threadsObj = (Integer) cmd.getOptionValue (threadsOpt);
    int threads = (threadsObj == null ? -1 : threadsObj.intValue());

    // Check for coherent mode
    // -----------------------
/*
    if (coherentOutput != null && !(method.equals ("latest") || method.equals ("explicit"))) {
      LOGGER.severe ("Coherent mode can only be used with --method latest or --method explicit");
      ToolServices.exitWithCode (2);
      return;
    } // if
*/
    if (coherentOutput != null) {
      LOGGER.severe ("Coherent mode is temporarily disabled in this version");
      ToolServices.exitWithCode (2);
      return;
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

      // Create chunk operator
      // ---------------------
      ArrayReduction operator = null;
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
        VERBOSE.info ("Using " + maxOps + " parallel threads for processing");
      } // if

      // Loop over each composite variable
      // ---------------------------------
      for (String variableName : variableNames) {

        // Create a chunk collector for the input variable
        // -----------------------------------------------
        ChunkCollector collector = new ChunkCollector();
        DataChunk.DataType externalType = null;
        Grid prototypeGrid = null;
        for (String inputFile : inputFileList) {
          EarthDataReader reader = readerMap.get (inputFile);
          if (reader.containsVariable (variableName)) {

            // Get prototype grid
            // ------------------
            if (prototypeGrid == null)
              prototypeGrid = (Grid) reader.getVariable (variableName);;

            // Get producer for this reader
            // ----------------------------
            ChunkProducer producer = reader.getChunkProducer (variableName);

            // Check producer external type
            // ----------------------------
            if (externalType == null) externalType = producer.getExternalType();
            else if (externalType != producer.getExternalType()) {
              LOGGER.severe ("Non-matching external data types found between input files for variable " + variableName);
              ToolServices.exitWithCode (2);
              return;
            } // else if

            collector.addProducer (producer);

          } // if
        } // for
        
        // Create a chunk consumer for the output variable
        // -----------------------------------------------
        TilingScheme inputTilingScheme = prototypeGrid.getTilingScheme();
        if (inputTilingScheme != null)
          writer.setTileDims (inputTilingScheme.getTileDimensions());
        else
          writer.setTileDims (null);
        CachedGrid outputGrid = new HDFCachedGrid (prototypeGrid, writer);
        ChunkConsumer consumer = new GridChunkConsumer (outputGrid);

        ChunkingScheme outputChunkingScheme = consumer.getNativeScheme();
        int[] chunkSize = outputChunkingScheme.getChunkSize();
        VERBOSE.info ("Creating " +  variableName +
          " variable with chunk size " + chunkSize[ROW] + "x" + chunkSize[COL]);

        // Create chunk computation
        // ------------------------
        ChunkFunction function = new CompositeFunction (operator, minValid, consumer.getPrototypeChunk());
        ChunkComputation op = new ChunkComputation (collector, consumer, function);

        // Debugging
        if (LOGGER.isLoggable (Level.FINE))
          op.setTracked (true);

        // Perform chunk processing
        // ------------------------
        List<ChunkPosition> positions = new ArrayList<>();
        outputChunkingScheme.forEach (positions::add);

        if (serialOperations) {
          positions.forEach (pos -> op.perform (pos));
        } // if
        else {
          PoolProcessor processor = new PoolProcessor();
          processor.init (positions, op);
          processor.setMaxOperations (maxOps);
          processor.start();
          processor.waitForCompletion();
        } // if

        // Debugging
        if (LOGGER.isLoggable (Level.FINE)) {
          StringBuilder types = new StringBuilder();
          StringBuilder times = new StringBuilder();
          op.getTrackingData().forEach ((type, time) -> {
            types.append ((types.length() == 0 ? "" : "/") + type);
            times.append ((times.length() == 0 ? "" : "/") + String.format ("%.3f", time));
          });
          LOGGER.fine ("Computation " + types + " = " + times + " s");
        } // if

        // Flush any unwritten tiles
        // -------------------------
        outputGrid.flush();
        outputGrid.clearCache();

      } // for

      // Close input files
      // -----------------
      for (String inputFile : inputFileList)
        readerMap.get (inputFile).close();

      // Close output file
      // -----------------
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
    info.option ("-p, --pedantic", "Retain repeated metadata values");
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
