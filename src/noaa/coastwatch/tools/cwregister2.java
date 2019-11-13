////////////////////////////////////////////////////////////////////////
/*

     File: cwregister2.java
   Author: Peter Hollemans
     Date: 2019/02/04

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
import java.io.FileNotFoundException;
import java.io.File;

import java.text.NumberFormat;

import java.util.List;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.Arrays;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.OptionException;

import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.HDFCachedGrid;
import noaa.coastwatch.tools.CleanupHook;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.SpheroidConstants;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.ChunkConsumer;
import noaa.coastwatch.util.chunk.GridChunkProducer;
import noaa.coastwatch.util.chunk.GridChunkConsumer;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.PoolProcessor;
import noaa.coastwatch.util.chunk.ResamplingOperation;
import noaa.coastwatch.util.chunk.ChunkOperation;
import noaa.coastwatch.util.chunk.SyntheticIntChunkProducer;

import noaa.coastwatch.util.DirectResamplingMapFactory;
import noaa.coastwatch.util.BucketResamplingMapFactory;
import noaa.coastwatch.util.ResamplingMapFactory;
import noaa.coastwatch.util.ResamplingDiagnostic;
import noaa.coastwatch.util.ResamplingDiagnostic.DiagnosticInfo;
import noaa.coastwatch.util.ResamplingSourceImp;
import noaa.coastwatch.util.sensor.SensorSourceImpFactory;
import noaa.coastwatch.util.GridDataResamplingMapFactory;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The new registration tool resamples gridded earth data to a master
 * projection using a revised set of high accuracy algorithms.</p>
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwregister2 - resamples gridded earth data to a master projection using
 *   a revised set of high accuracy algorithms.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p>cwregister2 [OPTIONS] input output</p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -c, --clobber <br>
 * -d, --diagnostic <br>
 * -D, --diagnostic-long <br>
 * -M, --master=FILE <br>
 * -m, --match=PATTERN <br>
 * -p, --proj=SYSTEM <br>
 * -S, --savemap <br>
 * --serial <br>
 * -t, --tiledims=ROWS/COLS <br>
 * -u, --usemap=FILE[/ROW_VAR/COL_VAR] <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The new registration tool resamples gridded earth data to a master
 * projection using a revised set of high accuracy algorithms.  The new tool
 * replaces the old <b>cwregister</b> tool in order to simplify and
 * streamline registration.  Specifically, the new tool improves registration
 * by:</p>
 *
 * <ol type="i">
 *
 *   <li>selecting the most appropriate resampling algorithm automatically
 *   rather than making the user choose,</li>
 *
 *   <li>automatically creating an optimal master projection for the input
 *   file,</li>
 *
 *   <li>performing diagnostics on the accuracy of the resampling
 *   algorithm,</li>
 *
 *   <li>handling terrain-corrected data with sensor-specific algorithms,
 *   and</li>
 *
 *   <li>performing registration on 2D tiles of data in parallel.</li>
 *
 *  </ol>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt>input</dt>
 *   <dd>The input data file name.</dd>
 *
 *   <dt>output</dt>
 *   <dd>The output data file name.</dd>
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt>-h, --help</dt>
 *
 *   <dd>Prints a brief help message.</dd>
 *
 *   <dt>-c, --clobber</dt>
 *
 *   <dd>Turns on clobber mode, in which the output file is overwritten even
 *   if it already exists.  The default is to check if an output file exists
 *   and not overwrite it if so.</dd>
 *
 *   <dt>-d, --diagnostic</dt>
 *
 *   <dd>Turns on diagnostic mode, in which the output file resampling accuracy
 *   is checked and compared to the optimal resampling.  The default is to
 *   run without diagnostics because diagnostic mode is much slower.  The
 *   diagnostics calculated are:
 *   <ul>
 *
 *     <li>Distance - distance in kilometers from the destination pixel center
 *     to the corresponding source pixel center.</li>
 *
 *     <li>Distance error - the value of <b>(dist - opt)</b> where <b>dist</b>
 *     is the distance in kilometers between the destination pixel and mapped
 *     source pixel (see above) and <b>opt</b> is the distance in kilometers
 *     between the destination pixel and optimal source pixel.  The optimal
 *     source pixel is the source pixel whose center has the minimum
 *     physical distance from the destination pixel center out of all pixels
 *     in the input file.  Thus the value of <b>(dist - opt)</b> is always
 *     positive since <b>dist</b> &gt;= <b>opt</b>.</li>
 *
 *     <li>Normalized performance metric - normalized value calculated as
 *     <b>1 - (dist - opt)/(dist + opt)</b> where <b>dist</b> and <b>opt</b>
 *     are defined above.  The normalized metric has a range of [0..1] where
 *     0 is the least optimal resampling and 1 is the most optimal.</li>
 *
 *     <li>% of suboptimal destination pixels.  A suboptimal
 *     destination pixel is one where the source pixel mapped to it wasn't
 *     the optimal source pixel.</li>
 *
 *   </ul>
 *   Diagnostic mode also turns on verbose mode.</dd>
 *
 *   <dt>-D, --diagnostic-long</dt>
 *
 *   <dd>Turns on long form diagnostic mode.  In addition to the diagnostics
 *   mentioned above, each suboptimal remapping is printed: destination
 *   coordinates, source coordinates, optimal source coordinates, and distance
 *   error.</dd>
 *
 *   <dt>-M, --master=FILE</dt>
 *
 *   <dd>The master projection file name.  The master file is not modified,
 *   only accessed for earth transform parameters.  By default an optimal output
 *   projection is determined automatically from the input file if no master
 *   file is specified.</dd>
 *
 *   <dt>-m, --match=PATTERN</dt>
 *
 *   <dd>The variable name matching pattern.  The pattern is a regular
 *   expression that determines which variables to register, otherwise all
 *   variables are registered.</dd>
 *
 *   <dt>-p, --proj=SYSTEM</dt>
 *
 *   <dd>The projection system to use for a master.  This can be 'geo'
 *   (geographic), 'ortho' (orthographic), 'mercator', or 'polar' (polar
 *   stereographic).  An optimal destination master is created using the
 *   specified projection system, and is set up with its center location,
 *   resolution, and extents matching the input file.  If no projection
 *   system is specified and no master file is provided, the default is
 *   to create an orthographic projection.</dd>
 *
 *   <dt>-S, --savemap</dt>
 *
 *   <dd>Saves the row and column mapping to the output file.  These can be
 *   used in a later registration run with <b>--usemap</b> to speed up the
 *   registration for input files that always have the same source coordinates.
 *   The variables 'source_row' and 'source_col' are added to the output
 *   file, and specify for each destination pixel, which source row and column
 *   they were mapped from.</dd>
 *
 *   <dt>--serial</dt>
 *
 *   <dd>Turns on serial processing mode.  By default the program will
 *   use multiple processors in parallel to process chunks of data.</dd>
 *
 *   <dt>-t, --tiledims=ROWS/COLS</dt>
 *
 *   <dd>Specifies the 2D tile size to use in the output file.  This is
 *   the size the HDF chunks will be written as.  The default is 512 by 512
 *   tiles for all variables.</dd>
 *
 *   <dt>-u, --usemap=FILE[/ROW_VAR/COL_VAR]</dt>
 *
 *   <dd>Uses the file to load previously saved row and column mapping
 *   variables.  The names of the variables are optional, and default to
 *   'source_row' and 'source_col'.  The data can be from a previous run of
 *   <b>cwregister2</b>, or can be user-generated from some other method --
 *   if so the variables should be saved as <b>int32</b> type.</dd>
 *
 *   <dt>-v, --verbose</dt>
 *
 *   <dd>Runs in verbose mode, printing the current status of data registration
 *   rather than running silently.</dd>
 *
 *   <dt>--version</dt>
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Invalid master, input or output file names </li>
 *   <li> Unsupported master or input file format </li>
 *   <li> Output file already exists </li>
 *   <li> Invalid tile dimensions </li>
 *   <li> No variables found for registration </li>
 *   <li> Projection system specified is unsupported </li>
 *   <li> Error during registration computation </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p>The following shows the registration of a VIIRS level 2 granule
 * to an optimal Mercator projection wih diagnostic output:</p>
 * <pre>
 *   phollema$ cwregister2 --diagnostic --proj mercator --clobber
 *     VRSLCW.B2018157.213433.hdf VRSLCW.B2018157.213433.mercator.hdf
 *   [INFO] Opening input file VRSLCW.B2018157.213433.hdf
 *   [INFO] Creating optimal destination transform
 *   [INFO] Creating output file VRSLCW.B2018157.213433.mercator.hdf
 *   [INFO] Creating output variable latitude
 *   [INFO] Creating output variable longitude
 *   [INFO] Creating output variable rel_azimuth
 *   [INFO] Creating output variable sat_zenith
 *   [INFO] Creating output variable sun_zenith
 *   [INFO] Creating output variable EV_BandM8
 *   [INFO] Creating output variable EV_BandM6
 *   [INFO] Creating output variable EV_BandM11
 *   [INFO] Creating output variable EV_BandM1
 *   [INFO] Creating output variable EV_BandM5
 *   [INFO] Creating output variable EV_BandM2
 *   [INFO] Creating output variable EV_BandM10
 *   [INFO] Creating output variable EV_BandM4
 *   [INFO] Creating output variable EV_BandM3
 *   [INFO] Creating output variable EV_BandM7
 *   [INFO] Creating output variable edgemask
 *   [INFO] Initializing resampling map factory
 *   [INFO] Source has size 768x3200
 *   [INFO] Destination has size 1378x4032
 *   [INFO] Found 8 processor(s) to use
 *   [INFO] Processing 384 chunks of size 512x512
 *   [INFO] Performing diagnostic
 *   [INFO] Diagnostic summary statistics
 *     Distance (km)       min = 0.002940, max = 1.265793, avg = 0.376542
 *     Distance error (km) min = 0.000000, max = 0.001918, avg = 0.000000
 *     Norm perf metric    min = 0.998856, max = 1.000000, avg = 1.000000
 *   [INFO] Found 22 suboptimal of 28851 samples (0.08%)
 *   [INFO] Closing files
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public final class cwregister2 {

  private static final String PROG = cwregister2.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 2;

  ////////////////////////////////////////////////////////////

  /**
   * Gets a map projection that covers the entire extents of the source
   * transform area.  The returned map projection attempts to match the
   * source transform resolution and center point as closely as possible.
   *
   * @param sourceTrans the source transform to match.
   * @param type the map projection type: geo, ortho, mercator, or polar.
   *
   * @return the optimal map projection of the given type.
   */
  private static EarthTransform getOptimalProjection (
    EarthTransform sourceTrans,
    String type
  ) {
  
    // Estimate source resolution at center
    // ------------------------------------
    int[] sourceDims = sourceTrans.getDimensions();
    DataLocation centerDataLoc = new DataLocation (sourceDims[ROW]/2, sourceDims[COL]/2);

    EarthLocation centerEarthLoc = sourceTrans.transform (centerDataLoc);
    EarthLocation rightEarthLoc = sourceTrans.transform (centerDataLoc.translate (0, 1));
    EarthLocation bottomEarthLoc = sourceTrans.transform (centerDataLoc.translate (1, 0));

    double horizRes = centerEarthLoc.distance (rightEarthLoc);
    if (Double.isNaN (horizRes) || horizRes == 0) horizRes = Double.MAX_VALUE;

    double vertRes = centerEarthLoc.distance (bottomEarthLoc);
    if (Double.isNaN (vertRes) || vertRes == 0) vertRes = Double.MAX_VALUE;

    double res = Math.max (horizRes, vertRes);

    if (res == Double.MAX_VALUE)
      throw new IllegalStateException ("Cannot determine source transform resolution");

    LOGGER.fine ("Projection center is " + centerEarthLoc.format (EarthLocation.RAW));
    LOGGER.fine ("Projection resolution is " + res + " km");

    // Create projection
    // -----------------
    double[] parameters = new double[15];
    int system;
    int spheroid = SpheroidConstants.WGS84;
    
    if (type.equals ("geo")) {
      system = ProjectionConstants.GEO;
      res = Math.toDegrees (res / SpheroidConstants.STD_RADIUS);
    } // if
    else {
      if (type.equals ("ortho")) {
        system = ProjectionConstants.ORTHO;
        spheroid = SpheroidConstants.SPHERE;
      } // if
      else if (type.equals ("mercator")) system = ProjectionConstants.MERCAT;
      else if (type.equals ("polar")) system = ProjectionConstants.PS;
      else throw new IllegalArgumentException ("Unsupported projection type " + type);
      parameters[4] = GCTP.pack_angle (centerEarthLoc.lon);
      parameters[5] = GCTP.pack_angle (centerEarthLoc.lat);
      res = res*1000;
    } // else
    
    EarthTransform destTrans;
    try {
      destTrans = MapProjectionFactory.getInstance().create (
        system,
        0,
        parameters,
        spheroid,
        new int[] {100, 100},
        centerEarthLoc,
        new double[] {res, res}
      );
    } // try
    catch (Exception e) {
      throw new RuntimeException ("Error initializing optimal map projection");
    } // catch

    LOGGER.fine ("Projection is " + destTrans.getClass().getName());

    // Get extents
    // -----------
    double[] minDataLoc = new double[] {Double.MAX_VALUE, Double.MAX_VALUE};
    double[] maxDataLoc = new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE};
    DataLocation sourceDataLoc = new DataLocation (2);
    DataLocation destDataLoc = new DataLocation (2);
    EarthLocation earthLoc = new EarthLocation();
    boolean isValid = false;

    for (int i = 0; i < sourceDims[ROW]; i++) {
      sourceDataLoc.set (ROW, i);
      for (int j = 0; j < sourceDims[COL]; j++) {
        sourceDataLoc.set (COL, j);
        sourceTrans.transform (sourceDataLoc, earthLoc);
        if (earthLoc.isValid()) {
          destTrans.transform (earthLoc, destDataLoc);
          if (destDataLoc.isValid()) {
            double row = destDataLoc.get (ROW);
            double col = destDataLoc.get (COL);
            if (row < minDataLoc[ROW]) minDataLoc[ROW] = row;
            else if (row > maxDataLoc[ROW]) maxDataLoc[ROW] = row;
            if (col < minDataLoc[COL]) minDataLoc[COL] = col;
            else if (col > maxDataLoc[COL]) maxDataLoc[COL] = col;
            isValid = true;
          } // if
        } // if
        if (i > 0 && i < sourceDims[ROW]-1) j += sourceDims[COL]-2;
      } // for
    } // for
    
    if (!isValid) throw new RuntimeException ("Cannot find extents for optimal map projection");
    
    LOGGER.fine ("Min data location " + Arrays.toString (minDataLoc));
    LOGGER.fine ("Max data location " + Arrays.toString (maxDataLoc));
    
    // Shift projection to actual extents origin
    // -----------------------------------------
    int dims[] = new int[] {
      (int) Math.ceil (maxDataLoc[ROW] - minDataLoc[ROW]),
      (int) Math.ceil (maxDataLoc[COL] - minDataLoc[COL])
    };
    DataLocation originDataLoc = new DataLocation (
      Math.floor (minDataLoc[ROW]),
      Math.floor (minDataLoc[COL])
    );
    destTrans = destTrans.getSubset (originDataLoc, dims);

    LOGGER.fine ("Projection dims " + Arrays.toString (destTrans.getDimensions()));

    return (destTrans);
  
  } // getOptimalProjection

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
    Option clobberOpt = cmd.addBooleanOption ('c', "clobber");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option masterOpt = cmd.addStringOption ('M', "master");
    Option serialOpt = cmd.addBooleanOption ("serial");
    Option diagnosticOpt = cmd.addBooleanOption ('d', "diagnostic");
    Option diagnosticlongOpt = cmd.addBooleanOption ('D', "diagnostic-long");
    Option tiledimsOpt = cmd.addStringOption ('t', "tiledims");
    Option projOpt = cmd.addStringOption ('p', "proj");
    Option savemapOpt = cmd.addBooleanOption ('S', "savemap");
    Option usemapOpt = cmd.addStringOption ('u', "usemap");
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
    boolean performDiagnostic = (cmd.getOptionValue (diagnosticOpt) != null);
    boolean performDiagnosticLong = (cmd.getOptionValue (diagnosticlongOpt) != null);
    if (performDiagnosticLong) performDiagnostic = true;
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose || performDiagnostic) VERBOSE.setLevel (Level.INFO);
    String match = (String) cmd.getOptionValue (matchOpt);
    String tiledims = (String) cmd.getOptionValue (tiledimsOpt);
    if (tiledims == null) tiledims = "512/512";
    boolean saveMap = (cmd.getOptionValue (savemapOpt) != null);
    String usemap = (String) cmd.getOptionValue (usemapOpt);
    String master = (String) cmd.getOptionValue (masterOpt);
    String proj = (String) cmd.getOptionValue (projOpt);
    if (proj == null) proj = "ortho";
    boolean clobberOutput = (cmd.getOptionValue (clobberOpt) != null);
    boolean serialOperations = (cmd.getOptionValue (serialOpt) != null);

    // Check output
    // ------------
    if (new File (output).exists() && !clobberOutput) {
      LOGGER.severe ("Output file already exists and --clobber not specified");
      ToolServices.exitWithCode (2);
      return;
    } // if

    try {

      // Access input file and get transform
      // -----------------------------------
      VERBOSE.info ("Opening input file " + input);
      EarthDataReader.setDataProjection (true);
      EarthDataReader reader = EarthDataReaderFactory.create (input);
      EarthDataInfo inputInfo = reader.getInfo();
      EarthTransform sourceTrans = inputInfo.getTransform();

      // Get destination from master
      // ---------------------------
      EarthTransform destTrans;
      if (master != null) {

        VERBOSE.info ("Accessing master file for destination transform data");
        try {
          EarthDataReader masterReader = EarthDataReaderFactory.create (master);
          destTrans = masterReader.getInfo().getTransform();
          masterReader.close();
        } // try
        catch (FileNotFoundException fnf) {
          LOGGER.severe (fnf.getMessage());
          ToolServices.exitWithCode (2);
          return;
        } // catch
        catch (Exception e) {
          LOGGER.log (Level.SEVERE, "Error reading master file", e);
          ToolServices.exitWithCode (2);
          return;
        } // catch

      } // if
      
      // Get destination from optimal projection
      // ---------------------------------------
      else {
        VERBOSE.info ("Creating optimal destination transform");
        destTrans = getOptimalProjection (sourceTrans, proj);
      } // else

      int[] destDims = destTrans.getDimensions();
      int destRows = destDims[ROW];
      int destCols = destDims[COL];

      LOGGER.fine ("Source reader " + reader.getClass().getName());
      LOGGER.fine ("Source transform " + sourceTrans.getClass().getName());

      // Create output file
      // ------------------
      VERBOSE.info ("Creating output file " + output);
      EarthDataInfo outputInfo = (EarthDataInfo) inputInfo.clone();
      outputInfo.setTransform (destTrans);
      CleanupHook.getInstance().scheduleDelete (output);
      CWHDFWriter writer = new CWHDFWriter (outputInfo, output);

      // Set tile dimensions
      // -------------------
      String[] tiledimsArray = tiledims.split (ToolServices.SPLIT_REGEX);
      if (tiledimsArray.length != 2) {
        LOGGER.severe ("Invalid tile dimensions '" + tiledims + "'");
        ToolServices.exitWithCode (2);
        return;
      } // if
      int[] tileDims = new int[] {
        Integer.parseInt (tiledimsArray[0]),
        Integer.parseInt (tiledimsArray[1])
      };
      LOGGER.fine ("Tile dimensions " + Arrays.toString (tileDims));
      writer.setTileDims (tileDims);
      
      List<ChunkProducer> producerList = new ArrayList<>();
      List<ChunkConsumer> consumerList = new ArrayList<>();
     
      // Loop over each variable
      // -----------------------
      int variables = reader.getVariables();
      for (int i = 0; i < variables; i++) {

        // Check for name match
        // --------------------
        String varName = reader.getName (i);
        if (match != null && !varName.matches (match))
          continue;

        // Get data variable
        // -----------------
        DataVariable var = null;
        try {
          var = reader.getVariable (i);
        } // try
        catch (Exception e) {
          continue;
        } // catch

        // Create output grid
        // ------------------
        if (var instanceof Grid) {
          VERBOSE.info ("Creating output variable " + var.getName());
          Grid inputGrid = (Grid) var;
          Grid outputGrid = new Grid (inputGrid, destRows, destCols);
          outputGrid.setNavigation (null);
          outputGrid = new HDFCachedGrid (outputGrid, writer);
          producerList.add (new GridChunkProducer (inputGrid));
          consumerList.add (new GridChunkConsumer (outputGrid));
        } // if

      } // for

      // Check for empty list
      // --------------------
      if (producerList.isEmpty()) {
        LOGGER.severe ("No variables found for registration");
        ToolServices.exitWithCode (2);
        return;
      } // if

      // Create source coordinate map variables
      // --------------------------------------
      if (saveMap) {

        ChunkingScheme sourceScheme = producerList.get (0).getNativeScheme();

        VERBOSE.info ("Creating mapping variable source_row");
        ChunkProducer sourceRowChunkProducer = new SyntheticIntChunkProducer (sourceScheme,
          (row, col) -> row);
        Grid sourceRowGrid = new Grid (
          "source_row",
          "Row from source coordinate system",
          "",
          destRows,
          destCols,
          new int[0],
          NumberFormat.getIntegerInstance(),
          null,
          Integer.MIN_VALUE
        );
        sourceRowGrid = new HDFCachedGrid (sourceRowGrid, writer);

        producerList.add (sourceRowChunkProducer);
        consumerList.add (new GridChunkConsumer (sourceRowGrid));

        VERBOSE.info ("Creating mapping variable source_col");
        ChunkProducer sourceColChunkProducer = new SyntheticIntChunkProducer (sourceScheme,
          (row, col) -> col);
        Grid sourceColGrid = new Grid (
          "source_col",
          "Column from source coordinate system",
          "",
          destRows,
          destCols,
          new int[0],
          NumberFormat.getIntegerInstance(),
          null,
          Integer.MIN_VALUE
        );
        sourceColGrid = new HDFCachedGrid (sourceColGrid, writer);

        producerList.add (sourceColChunkProducer);
        consumerList.add (new GridChunkConsumer (sourceColGrid));

      } // if

      // Check diagnotic request
      // -----------------------
      if (performDiagnostic) {
        if (sourceTrans.isInvertible()) {
          LOGGER.warning ("Found invertible source transform, no diagnostic to perform");
          performDiagnostic = false;
        } // if
      } // if

      // Access saved map file
      // ---------------------
      ResamplingSourceImp imp = null;
      ResamplingMapFactory mapFactory = null;
      if (usemap != null) {

        VERBOSE.info ("Accessing previously saved resampling map data");

        // Get file and variable names
        // ---------------------------
        String[] usemapArray = usemap.split (ToolServices.SPLIT_REGEX);
        if (usemapArray.length != 1 && usemapArray.length != 3) {
          LOGGER.severe ("Invalid map specification '" + usemap + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        String mapFileName = usemapArray[0];
        String sourceRowVarName, sourceColVarName;
        if (usemapArray.length == 3) {
          sourceRowVarName = usemapArray[1];
          sourceColVarName = usemapArray[2];
        } // if
        else {
          sourceRowVarName = "source_row";
          sourceColVarName = "source_col";
        } // else

        // Access mapping data
        // -------------------
        try {
          EarthDataReader mapReader = EarthDataReaderFactory.create (mapFileName);
          Grid sourceRowGrid = (Grid) mapReader.getVariable (sourceRowVarName);
          Grid sourceColGrid = (Grid) mapReader.getVariable (sourceColVarName);
          mapFactory = new GridDataResamplingMapFactory (sourceRowGrid, sourceColGrid);
        } // try
        catch (FileNotFoundException fnf) {
          LOGGER.log (Level.SEVERE, fnf.getMessage());
          ToolServices.exitWithCode (2);
          return;
        } // catch
        catch (Exception e) {
          LOGGER.log (Level.SEVERE, "Error reading map file", e);
          ToolServices.exitWithCode (2);
          return;
        } // catch
      
        // Create sensor imp for diagnostic
        // --------------------------------
        if (performDiagnostic)
          imp = SensorSourceImpFactory.create (sourceTrans);
      
      } // if

      else {

        VERBOSE.info ("Initializing resampling map factory");
        
        // For invertible transforms, use a direct resampling
        // --------------------------------------------------
        if (sourceTrans.isInvertible()) {
          LOGGER.fine ("Detected an invertible transform, performing direct resampling");
          mapFactory = new DirectResamplingMapFactory (sourceTrans, destTrans);
        } // if

        // For non-invertible transforms, use bucket resampling
        // ----------------------------------------------------
        else {
          LOGGER.fine ("Detected a non-invertible transform, performing bucket resampling");
          imp = SensorSourceImpFactory.create (sourceTrans);
          mapFactory = new BucketResamplingMapFactory (sourceTrans, destTrans, imp);
        } // else

      } // else

      // Create diagnostic
      // -----------------
      ResamplingDiagnostic diagnostic = null;
      if (performDiagnostic) {
        diagnostic = new ResamplingDiagnostic (sourceTrans, imp, destTrans, mapFactory, 0.01);
        mapFactory = diagnostic;
      } // if

      // Setup resampling
      // ----------------
      ChunkingScheme scheme = consumerList.get (0).getNativeScheme();
      List<ChunkPosition> positions = new ArrayList<>();
      scheme.forEach (positions::add);

      int[] sourceDims = sourceTrans.getDimensions();
      VERBOSE.info ("Source has size " + sourceDims[ROW] + "x" + sourceDims[COL]);
      int[] chunkingDims = scheme.getDims();
      VERBOSE.info ("Destination has size " + chunkingDims[ROW] + "x" + chunkingDims[COL]);

      if (!serialOperations) {
        int processors = Runtime.getRuntime().availableProcessors();
        VERBOSE.info ("Found " + processors + " processor(s) to use");
      } // if

      int[] chunkSize = scheme.getChunkSize();
      VERBOSE.info ("Processing " + (positions.size() * consumerList.size()) +
        " chunks of size " + chunkSize[ROW] + "x" + chunkSize[COL]);

      // Perform resampling
      // ------------------
      ChunkOperation op = new ResamplingOperation (producerList, consumerList, mapFactory);

      if (serialOperations) {
        positions.forEach (pos -> op.perform (pos));
      } // if
      else {
        PoolProcessor processor = new PoolProcessor();
        processor.init (positions, op);
        processor.start();
        processor.waitForCompletion();
      } // if

      // Perform diagnostic
      // ------------------
      if (performDiagnostic && diagnostic != null) {

        VERBOSE.info ("Performing diagnostic");
        diagnostic.complete();
        DoubleSummaryStatistics distStats = diagnostic.getDistStats();
        DoubleSummaryStatistics distErrStats = diagnostic.getDistErrorStats();
        DoubleSummaryStatistics omegaStats = diagnostic.getOmegaStats();
        
        VERBOSE.info ("Diagnostic summary statistics\n" +
          String.format ("  Distance (km)       min = %.6f, max = %.6f, avg = %.6f\n",
            distStats.getMin(), distStats.getMax(), distStats.getAverage()) +
          String.format ("  Distance error (km) min = %.6f, max = %.6f, avg = %.6f\n",
            distErrStats.getMin(), distErrStats.getMax(), distErrStats.getAverage()) +
          String.format ("  Norm perf metric    min = %.6f, max = %.6f, avg = %.6f",
            omegaStats.getMin(), omegaStats.getMax(), omegaStats.getAverage())
        );
        
        int suboptimalCount = diagnostic.getSuboptimalCount();
        int sampleCount = diagnostic.getSampleCount();
        double suboptimalPercent = ((double) suboptimalCount) / sampleCount * 100;
        VERBOSE.info (String.format ("Found %d suboptimal of %d samples (%.2f%%)", suboptimalCount, sampleCount, suboptimalPercent));

        if (performDiagnosticLong) {
          List<DiagnosticInfo> suboptimalList = diagnostic.getSuboptimalDiagnosticList();
          for (int i = 0; i < suboptimalList.size(); i++) {
            DiagnosticInfo info = suboptimalList.get (i);
            VERBOSE.info (
              String.format ("Suboptimal diagnostic sample [%3d/%3d]\n", (i+1), suboptimalCount) +
              String.format ("  dest    = %10s\n", Arrays.toString (info.destCoords)) +
              String.format ("  source  = %10s, dist = %.6f km\n", Arrays.toString (info.sourceCoords), info.actualDist) +
              String.format ("  optimal = %10s, dist = %.6f km\n", Arrays.toString (info.optimalSourceCoords), info.optimalDist) +
              String.format ("  error   = %.6f km", info.getDistanceError())
            );
          } // for
        } // if
        
      } // if

      // Close files
      // -----------
      VERBOSE.info ("Closing files");
      writer.close();
      CleanupHook.getInstance().cancelDelete (output);
      reader.close();

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

    UsageInfo info = new UsageInfo ("cwregister2");

    info.func ("Resamples gridded earth data to a master projection using " +
      "a revised set of high accuracy algorithms");

    info.param ("input", "Input data file");
    info.param ("output", "Output data file");

    info.option ("-c, --clobber", "Overwrite previous output file");
    info.option ("-d, --diagnostic", "Perform resampling diagnostics");
    info.option ("-D, --diagnostic-long", "Perform diagnostics in long form");
    info.option ("-h, --help", "Show help message");
    info.option ("-M, --master=FILE", "Use file for output projection");
    info.option ("-m, --match=PATTERN", "Register only variables matching regular expression");
    info.option ("-p, --proj=SYSTEM", "Set output projection system");
    info.option ("-S, --savemap", "Save resampling map");
    info.option ("--serial", "Perform serial operations");
    info.option ("-t, --tiledims=ROWS/COLS", "Set written tile dimensions");
    info.option ("-u, --usemap=FILE[/ROW_VAR/COL_VAR]", "Use precomputed remapping");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwregister2 () { }

  ////////////////////////////////////////////////////////////

} // cwregister2 class

////////////////////////////////////////////////////////////////////////

