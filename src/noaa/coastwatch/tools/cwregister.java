////////////////////////////////////////////////////////////////////////
/*

     File: cwregister.java
   Author: Peter Hollemans
     Date: 2002/11/11

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
import java.io.FileNotFoundException;

import java.text.NumberFormat;

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
import noaa.coastwatch.util.GridResampler;
import noaa.coastwatch.util.InverseGridResampler;
import noaa.coastwatch.util.MixedGridResampler;
import noaa.coastwatch.util.DirectGridResampler;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.LocationFilter;
import noaa.coastwatch.util.VIIRSBowtieFilter;
import noaa.coastwatch.util.ExpressionFilter;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The registration tool resamples gridded earth data to a master
 * projection.</p>
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwregister - resamples gridded earth data to a master projection.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p>cwregister [OPTIONS] master input output</p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -f, --srcfilter=TYPE <br>
 * -h, --help <br>
 * -m, --match=PATTERN <br>
 * -M, --method=TYPE <br>
 * -O, --overwrite=TYPE <br>
 * -p, --polysize=KILOMETERS <br>
 * -r, --rectsize=WIDTH/HEIGHT <br>
 * -s, --srcexpr=EXPRESSION <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The register tool resamples gridded earth data to
 * a master projection.  A master projection specifies the translation
 * between grid row and column coordinates and earth latitude and
 * longitude coordinates.  The master projection file is any valid
 * earth data file from which a set of row and column dimensions
 * and earth transform parameters may be extracted.  This includes
 * standard CoastWatch product files as well as master files created
 * using the <b>cwmaster</b> tool. </p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt>master</dt>
 *   <dd>The master projection file name.  Note that the master file
 *   is not altered in any way.  It is simply accessed in order to
 *   determine grid and earth transform parameters.</dd>
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
 *   <dt>-f, --srcfilter=TYPE</dt>
 *
 *   <dd>Specifies a filter used to determine whether source pixels
 *   at a given location should be used in registration.  The only filter 
 *   type currently supported is 'viirs', which filters out pixels from the
 *   VIIRS sensor that have been omitted due to bow-tie overlap.  By default 
 *   all valid source pixels are used in registration.  See also the
 *   <b>--srcexpr</b> option to filter using an expression.</dd>
 *
 *   <dt>-m, --match=PATTERN</dt>
 *
 *   <dd>The variable name matching pattern.  If specified, the
 *   pattern is used as a regular expression to match variable names.
 *   Only variables in the input file matching the pattern will be
 *   registered.  By default, no pattern matching is performed and all
 *   variables are registered.</dd>
 *
 *   <dt>-M, --method=TYPE</dt>
 *
 *   <dd>The registration resampling method.  Valid methods are
 *   'inverse', 'mixed', and 'direct':
 *   <ul>
 *
 *     <li> <b>Inverse</b> - This method divides the destination grid into a
 *     series of rectangles of physical size no larger than that specified by
 *     the <b>--polysize</b> option (100 km by default), and computes polynomial
 *     coefficients for approximating the coordinate transform within each
 *     rectangle.  Each polynomial approximation computes a source coordinate
 *     in the source grid for each destination coordinate in the destination
 *     grid rectangle.  This is the default method of registration and is
 *     recommended when the source earth location data is smooth and continuous
 *     such as with AVHRR LAC swath data.</li>
 *
 *     <li> <b>Mixed</b> - This method divides the source grid into rectangles
 *     of a size specified by the  <b>--rectsize</b> option (50x50 pixels by
 *     default), and computes polynomial coefficients on each rectangle that
 *     are used to approximate a source coordinate in the source grid rectangle
 *     for each destination coordinate in the corresponding destination grid
 *     rectangle.  The method follows up with a single pixel interpolation to
 *     fill in pixels that fell between destination rectangles bounds.  This
 *     method is recommended when the source earth location data is
 *     discontinuous at regular intervals, such as with MODIS swath data.  In
 *     this case, the <b>--rectsize</b> option must be used to specify rectangles
 *     that are compatible with the discontinuity.  For example if the source
 *     earth location data is discontinuous at 16 pixel intervals along the
 *     row direction, then the rectangle size should be set to 16x16.</li>
 *
 *     <li> <b>Direct</b> - This method is the simplest and performs a direct
 *     lookup of the source coordinate in the source grid for each destination
 *     coordinate in the destination grid.  This method is available mainly
 *     as a comparison for testing the accuracy and speed of the other methods,
 *     since it is expected to run more slowly but have the highest accuracy.
 *     Note that the direct method cannot currently be used if the source grid
 *     contains discontinuous earth location data (such as MODIS swath data).</li>
 *
 *   </ul></dd>
 *
 *   <dt>-O, --overwrite=TYPE</dt>
 *
 *   <dd>ADVANCED USERS ONLY.  Sets the overwrite policy for
 *   'mixed' mode resampling: either 'always' (the default),
 *   'never', or 'closer'.  If during resampling, more than one
 *   source pixel maps to a single destination pixel, this option
 *   is used to determine if the new value should overwrite the
 *   old value.  By default, the new value always overwrites the
 *   destination pixel (the 'always' mode).  If 'never' is
 *   specified, the first written value is never overwritten.  If
 *   'closer' is specified, the destination pixel is only
 *   overwritten if the source pixel is closer in physical
 *   location to the destination than any previous source
 *   pixel.</dd>
 *
 *   <dt>-p, --polysize=KILOMETERS</dt>
 *
 *   <dd>The polynomial approximation rectangle size in kilometers.
 *   This option is only used by the inverse resampling method (see
 *   the <b>--method</b> option). The inverse resampling method
 *   employs a polynomial approximation to speed up the calculation of
 *   data locations.  The polynomial rectangle size determines the
 *   maximum allowed size of the resampling rectangles in the
 *   destination.  The default polynomial size is 100 km, which
 *   introduces an error of less than 0.15 km for AVHRR LAC data.</dd>
 *
 *   <dt>-r, --rectsize=WIDTH/HEIGHT</dt>
 *
 *   <dd>The polynomial approximation rectangle size in pixels.  This
 *   option is only used by the mixed resampling method (see the
 *   <b>--method</b> option).  The mixed resampling method employs a
 *   polynomial approximation to speed up the calculation of data
 *   locations.  The polynomial rectangle size determines the exact
 *   dimensions of the resampling rectangles in the source.  The
 *   default polynomial rectangle size is 50/50, which introduces
 *   only a small error for AVHRR LAC data.</dd>
 *
 *   <dt>-s, --srcexpr=EXPRESSION</dt>
 *   
 *   <dd>Specifies that an expression should be used to
 *   determine if pixels from the source should be used in
 *   registration.  If the result of the expression is true (in the case of a
 *   boolean result) or non-zero (in the case of a numerical
 *   result), the source pixel at the given location is used, otherwise it is
 *   ignored. The syntax for the expression is identical to the right-hand-side
 *   of a <b>cwmath</b> expression (see the <b>cwmath</b> tool manual
 *   page).  By default all valid source pixels are used in registration.</dd>
 *
 *   <dt>-v, --verbose</dt>
 *
 *   <dd>Turns verbose mode on.  The current status of data
 *   conversion is printed periodically.  The default is to run
 *   quietly.</dd>
 *
 *   <dt>--version</dt>
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option. </li>
 *   <li> Invalid master, input or output file names. </li>
 *   <li> Unsupported master or input file format. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p>
 * The following shows the registration of NOAA-17 AVHRR channel 2
 * swath data to a southern California master:</p>
 * <pre>
 *   phollema$ cwregister -v --match avhrr_ch2 ws_master.hdf 2002_318_1826_n17_mo.hdf
 *     2002_318_1826_n17_ws.hdf
 *
 *   [INFO] Reading master ws_master.hdf
 *   [INFO] Reading input 2002_318_1826_n17_mo.hdf
 *   [INFO] Creating output 2002_318_1826_n17_ws.hdf
 *   [INFO] Adding avhrr_ch2 to resampled grids
 *   [INFO] Found 1 grid(s) for resampling
 *   [INFO] Resampling 4788x2048 to 1024x1024
 *   [INFO] Creating location estimators
 *   [INFO] Computing row 0
 *   [INFO] Computing row 100
 *   [INFO] Computing row 200
 *   [INFO] Computing row 300
 *   [INFO] Computing row 400
 *   [INFO] Computing row 500
 *   [INFO] Computing row 600
 *   [INFO] Computing row 700
 *   [INFO] Computing row 800
 *   [INFO] Computing row 900
 *   [INFO] Computing row 1000
 *   [INFO] Closing files
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public final class cwregister {

  private static final String PROG = cwregister.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 3;

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) {

    ToolServices.startExecution (PROG);
    ToolServices.setCommandLine (PROG, argv);

    LOGGER.warning ("This program is DEPRECATED -- as of version 3.5.0, use cwregister2");

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option polysizeOpt = cmd.addIntegerOption ('p', "polysize");
    Option methodOpt = cmd.addStringOption ('M', "method");
    Option rectsizeOpt = cmd.addStringOption ('r', "rectsize");
    Option overwriteOpt = cmd.addStringOption ('O', "overwrite");
    Option srcexprOpt = cmd.addStringOption ('s', "srcexpr");
    Option srcfilterOpt = cmd.addStringOption ('f', "srcfilter");
    Option savemapOpt = cmd.addBooleanOption ('S', "savemap");
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
    String master = remain[0];
    String input = remain[1];
    String output = remain[2];

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose) VERBOSE.setLevel (Level.INFO);
    String match = (String) cmd.getOptionValue (matchOpt);
    Integer polysizeObj = (Integer) cmd.getOptionValue (polysizeOpt);
    int polysize = (polysizeObj == null ? 100 : polysizeObj.intValue());
    String method = (String) cmd.getOptionValue (methodOpt);
    if (method == null) method = "inverse";
    String rectsize = (String) cmd.getOptionValue (rectsizeOpt);
    if (rectsize == null) rectsize = "50/50";
    String overwrite = (String) cmd.getOptionValue (overwriteOpt);
    if (overwrite == null) overwrite = "always";
    String srcexpr = (String) cmd.getOptionValue (srcexprOpt);
    String srcfilter = (String) cmd.getOptionValue (srcfilterOpt);
    boolean saveMap = (cmd.getOptionValue (savemapOpt) != null);

    // Detect method subtype
    // ---------------------
    /** 
     * We do this primarily to support ACSPO VIIRS mixed mode resampling, in
     * which the VIIRS bowtie overlap pixels are set to missing, and so are 
     * detected and filled properly.  We currently don't document the ACSPO 
     * method subtype, as it's for NOAA users who know that the option exists.
     */
    String[] methodArray = method.split ("-");
    String methodSubtype = "";
    if (methodArray.length == 2) {
      method = methodArray[0];
      methodSubtype = methodArray[1];
    } // if

    try {

      // Get master earth info
      // ---------------------
      VERBOSE.info ("Reading master " + master);
      EarthDataInfo masterInfo = null;
      int rows = 0, cols = 0;
      EarthTransform masterTrans = null;
      try {
        EarthDataReader masterReader = EarthDataReaderFactory.create (master);
        masterInfo = masterReader.getInfo();
        masterTrans = masterInfo.getTransform();
        int[] dims = masterTrans.getDimensions();
        rows = dims[0];
        cols = dims[1];
        masterReader.close();
      } // try
      catch (FileNotFoundException fnf) {
        LOGGER.log (Level.SEVERE, fnf.getMessage());
        ToolServices.exitWithCode (2);
        return;
      } // catch
      catch (Exception e) {
        LOGGER.log (Level.SEVERE, "Error reading master file " + master, e);
        ToolServices.exitWithCode (2);
        return;
      } // catch

      // Get input earth info
      // --------------------
      VERBOSE.info ("Reading input " + input);
      if (method.equals ("mixed")) EarthDataReader.setDataProjection (true);
      EarthDataReader reader = EarthDataReaderFactory.create (input);
      EarthDataInfo inputInfo = reader.getInfo();

      // Create output file
      // ------------------
      VERBOSE.info ("Creating output " + output);
      EarthDataInfo outputInfo = (EarthDataInfo) inputInfo.clone();
      outputInfo.setTransform (masterTrans);
      CleanupHook.getInstance().scheduleDelete (output);
      CWHDFWriter writer = new CWHDFWriter (outputInfo, output);

      // Create inverse resampler
      // ------------------------
      GridResampler resampler = null;
      if (method.equals ("inverse")) {
        resampler = new InverseGridResampler (inputInfo.getTransform(),
          masterTrans, polysize);
      } // if

      // Create mixed resampler
      // ----------------------
      else if (method.equals ("mixed")) {

        // Get rectangle size
        // ------------------
        String[] rectsizeArray = rectsize.split (ToolServices.SPLIT_REGEX);
        if (rectsizeArray.length != 2) {
          LOGGER.severe ("Invalid rectangle size '" + rectsize + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        int rectWidth = Integer.parseInt (rectsizeArray[0]);
        int rectHeight = Integer.parseInt (rectsizeArray[1]);
        
        // Create resampler
        // ----------------
        MixedGridResampler mixed = new MixedGridResampler (inputInfo.getTransform(),
          masterTrans, rectWidth, rectHeight);
        resampler = mixed;
        
        // Add location filter
        // -------------------
        if (methodSubtype.equals ("acspo")) {
          srcfilter = "viirs";
          LOGGER.warning ("Detected deprecated method subtype 'acspo', activating source filter 'viirs'.");
          LOGGER.warning ("In the future, use --srcfilter viirs to avoid this warning.");
        } // if
        
        if (srcexpr != null && srcfilter != null) {
          LOGGER.severe ("Cannot specify *both* source filter and expression");
          ToolServices.exitWithCode (2);
          return;
        } // if

        LocationFilter filter = null;
        if (srcexpr != null) {
          filter = new ExpressionFilter (reader, srcexpr);
        } // if
        else if (srcfilter != null) {
          if (srcfilter.equals ("viirs")) {
            filter = VIIRSBowtieFilter.getInstance();
          } // if
          else {
            LOGGER.severe ("Invalid source filter");
            ToolServices.exitWithCode (2);
            return;
          } // else
        } // else if

        if (filter != null) mixed.setSourceFilter (filter);

        // Set overwrite mode
        // ------------------
        int overwriteMode = -1;
        if (overwrite.equals ("always"))
          overwriteMode = MixedGridResampler.OVERWRITE_ALWAYS;
        else if (overwrite.equals ("never")) 
          overwriteMode = MixedGridResampler.OVERWRITE_NEVER;
        else if (overwrite.equals ("closer"))
          overwriteMode = MixedGridResampler.OVERWRITE_IF_CLOSER;
        else {
          LOGGER.severe ("Invalid overwrite mode");
          ToolServices.exitWithCode (2);
          return;
        } // else
        mixed.setOverwriteMode (overwriteMode);
        
      } // else if


      // TODO: We should move the creation of the specific resampler into
      // a factory.


      // Create direct resampler
      // -----------------------
      else if (method.equals ("direct")) {
        resampler = new DirectGridResampler (inputInfo.getTransform(),
          masterTrans);
      } // else if

      // Invalid method
      // --------------
      else {
        LOGGER.severe ("Invalid registration method");
        ToolServices.exitWithCode (2);
        return;
      } // else
     
      // Loop over each variable
      // -----------------------
      int variables = reader.getVariables();
      for (int i = 0; i < variables; i++) {

        // Check for name match
        // --------------------
        String varName = reader.getName(i);
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

        // Add grid to resampler
        // ---------------------
        if (var instanceof Grid) {
          VERBOSE.info ("Adding " + var.getName() + " to resampled grids");
          Grid inputGrid = (Grid) var;
          Grid outputGrid = new Grid (inputGrid, rows, cols);
          outputGrid.setNavigation (null);
          outputGrid = new HDFCachedGrid (outputGrid, writer);
          resampler.addGrid (inputGrid, outputGrid);
        } // if
        else {
          VERBOSE.info ("Skipping non-grid variable " + var.getName());
        } // else 

      } // for

      // Create source coordinate map variables
      // --------------------------------------
      if (saveMap) {

        VERBOSE.info ("Creating mapping variable source_row");
        int[] sourceDims = inputInfo.getTransform().getDimensions();
        Grid sourceRowInputGrid = new Grid (
          "source_row",
          "Row from source coordinate system",
          "",
          sourceDims[ROW],
          sourceDims[COL],
          new int[0],
          NumberFormat.getIntegerInstance(),
          null,
          Integer.MIN_VALUE
        ) {
          public double getValue (int sourceRow, int sourceCol) { return (sourceRow); }
        };
        Grid sourceRowOutputGrid = new Grid (sourceRowInputGrid, rows, cols);
        sourceRowOutputGrid = new HDFCachedGrid (sourceRowOutputGrid, writer);
        resampler.addGrid (sourceRowInputGrid, sourceRowOutputGrid);

        VERBOSE.info ("Creating mapping variable source_col");
        Grid sourceColInputGrid = new Grid (
          "source_col",
          "Column from source coordinate system",
          "",
          sourceDims[ROW],
          sourceDims[COL],
          new int[0],
          NumberFormat.getIntegerInstance(),
          null,
          Integer.MIN_VALUE
        ) {
          public double getValue (int sourceRow, int sourceCol) { return (sourceCol); }
        };
        Grid sourceColOutputGrid = new Grid (sourceColInputGrid, rows, cols);
        sourceColOutputGrid = new HDFCachedGrid (sourceColOutputGrid, writer);
        resampler.addGrid (sourceColInputGrid, sourceColOutputGrid);

      } // if

      // Perform resampling and close
      // ----------------------------
      resampler.perform (verbose);
      VERBOSE.info ("Closing files");
      writer.close();
      CleanupHook.getInstance().cancelDelete (output);
      reader.close();

    } // try
    catch (Exception e) {
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

    UsageInfo info = new UsageInfo ("cwregister");

    info.func ("Resamples gridded earth data to a master projection");

    info.param ("master", "Master projection file");
    info.param ("input", "Input data file");
    info.param ("output", "Output data file");

    info.option ("-f, --srcfilter=TYPE", "Filter source pixels using filter type");
    info.option ("-h, --help", "Show help message");
    info.option ("-m, --match=PATTERN", "Register only variables matching pattern");
    info.option ("-M, --method=TYPE", "Use resampling algorithm");
    info.option ("-O, --overwrite=TYPE", "Overwrite destination pixels upon condition");
    info.option ("-p, --polysize=KILOMETERS", "Set rectangle size for inverse method");
    info.option ("-r, --rectsize=WIDTH/HEIGHT", "Set rectangle size for mixed method");
    info.option ("-s, --srcexpr=EXPRESSION", "Filter source pixels using expression");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwregister () { }

  ////////////////////////////////////////////////////////////

} // cwregister class

////////////////////////////////////////////////////////////////////////
