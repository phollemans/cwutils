////////////////////////////////////////////////////////////////////////
/*
     FILE: cwregister.java
  PURPOSE: To register a number of earth data variables to a master 
           projection.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/11
  CHANGES: 2002/12/19, PFH, added try/catch around getVariable
           2004/09/14, PFH, modified to clone EarthDataInfo object
           2004/09/28, PFH, modified to use ToolServices.setCommandLine()
           2004/10/05, PFH, modified to use EarthTransform.getDimensions()
           2005/01/27, PFH, added --method, --rectsize, and --overwrite options
           2005/01/30, PFH, modified to use CleanupHook class
           2005/03/15, PFH, reformatted documentation and usage note
           2006/07/13, PFH, updated for new overwrite behaviour
           2007/04/19, PFH, added version printing
           2011/09/13, XL, modified to use ACSPOInverseGridResampler for ACSPO files
           2013/03/06, PFH, modified to make ACSPO resampler usage a method subtype
           2015/11/05, PFH
           - Changes: Removed usage of ACSPO-specific registration classes.
           - Issue: In order to avoid duplicate changes/updates to
             registration algorithms, we combined ACSPO and
             regular registration classes into single classes.
           2016/06/10, PFH
           - Changes: Moved VIIRS bow-tie detection to new class.  Added options
             for using an expression or filter to determine if a source pixel
             should be used in registration.
           - We had a request from Phil Keegstra to let the user supply a
             data variable that contains a 1/0 flag for usability of the source
             pixel data.

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
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.LocationFilter;
import noaa.coastwatch.util.VIIRSBowtieFilter;
import noaa.coastwatch.util.ExpressionFilter;

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
 *   'inverse' and 'mixed'.  The inverse resampling method
 *   divides the destination into rectangles of bounded physical
 *   size (see the <b>--polysize</b> option), and computes
 *   polynomial approximations for the coordinate transforms on
 *   each rectangle in order to determine a source coordinate for
 *   each destination coordinate.  This is the default method and
 *   recommended when the source coordinate transform is smooth
 *   and continuous in the destination coordinate space such as
 *   with AVHRR LAC swath data.  The mixed resampling method
 *   divides the source into rectangles of certain dimensions
 *   (see the <b>--rectsize</b> option), computes polynomials on
 *   each rectangle similar to the inverse method, and follows
 *   with a single pixel interpolation.  This method is
 *   recommended when the source coordinate transform is
 *   discontinuous at regular intervals in the destination
 *   coordinate space, such as with MODIS swath data.</dd>
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
 *   cwregister: Reading master ws_master.hdf
 *   cwregister: Reading input 2002_318_1826_n17_mo.hdf
 *   cwregister: Creating output 2002_318_1826_n17_ws.hdf
 *   cwregister: Adding avhrr_ch2 to resampled grids
 *   GridResampler: Found 1 grid(s) for resampling
 *   GridResampler: Resampling 4788x2048 to 1024x1024
 *   GridResampler: Creating location estimators
 *   GridResampler: Computing row 0
 *   GridResampler: Computing row 100
 *   GridResampler: Computing row 200
 *   GridResampler: Computing row 300
 *   GridResampler: Computing row 400
 *   GridResampler: Computing row 500
 *   GridResampler: Computing row 600
 *   GridResampler: Computing row 700
 *   GridResampler: Computing row 800
 *   GridResampler: Computing row 900
 *   GridResampler: Computing row 1000
 *   cwregister: Closing files
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public final class cwregister {

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 3;

  /** Name of program. */
  private static final String PROG = "cwregister";

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
    Option polysizeOpt = cmd.addIntegerOption ('p', "polysize");
    Option methodOpt = cmd.addStringOption ('M', "method");
    Option rectsizeOpt = cmd.addStringOption ('r', "rectsize");
    Option overwriteOpt = cmd.addStringOption ('O', "overwrite");
    Option srcexprOpt = cmd.addStringOption ('s', "srcexpr");
    Option srcfilterOpt = cmd.addStringOption ('f', "srcfilter");
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
    String master = remain[0];
    String input = remain[1];
    String output = remain[2];

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
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

    try {

      // Get master earth info
      // ---------------------
      if (verbose) System.out.println (PROG + ": Reading master " + master);
      EarthDataInfo masterInfo = null;
      int rows = 0, cols = 0;
      EarthTransform masterTrans = null;
      try {
        EarthDataReader masterReader = EarthDataReaderFactory.create (
          master);
        masterInfo = masterReader.getInfo();
        masterTrans = masterInfo.getTransform();
        int[] dims = masterTrans.getDimensions();
        rows = dims[0];
        cols = dims[1];
        masterReader.close();
      } // try
      catch (Exception e) {
        System.err.println (PROG + ": Error reading master, aborting");        
        System.exit (2);
      } // catch

      // Get input earth info
      // --------------------
      if (verbose) System.out.println (PROG + ": Reading input " + input);
      if (method.equals ("mixed")) EarthDataReader.setDataProjection (true);
      EarthDataReader reader = EarthDataReaderFactory.create (input);
      EarthDataInfo inputInfo = reader.getInfo();

      // Create output file
      // ------------------
      if (verbose) System.out.println (PROG + ": Creating output " + output);
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
          System.err.println (PROG + ": Invalid rectangle size '" + rectsize + 
            "'");
          System.exit (2);
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
        if (srcexpr != null && srcfilter != null) {
          System.err.println (PROG + ": Cannot specify *both* source filter and expression");
          System.exit (2);
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
            System.err.println (PROG + ": Invalid source filter");
            System.exit (2);
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
          System.err.println (PROG + ": Invalid overwrite mode");
          System.exit (2);
        } // else
        mixed.setOverwriteMode (overwriteMode);
        
      } // else if

      // Invalid method
      // --------------
      else {
        System.err.println (PROG + ": Invalid registration method");
        System.exit (2);
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
          if (verbose) System.out.println (PROG + ": Adding " + 
            var.getName() + " to resampled grids");
          Grid inputGrid = (Grid) var;
          Grid outputGrid = new Grid (inputGrid, rows, cols);
          outputGrid.setNavigation (null);
          outputGrid = new HDFCachedGrid (outputGrid, writer);
          resampler.addGrid (inputGrid, outputGrid);
        } // if
        else {
          if (verbose) System.out.println (PROG + 
            ": Skipping non-grid variable " + var.getName());
        } // else 

      } // for

      // Perform resampling and close
      // ----------------------------
      resampler.perform (verbose);
      if (verbose) System.out.println (PROG + ": Closing files");
      writer.close();
      CleanupHook.getInstance().cancelDelete (output);
      reader.close();

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
"Usage: cwregister [OPTIONS] master input output\n" +
"Resamples gridded earth data to a master projection.\n" +
"\n" +
"Main parameters:\n" +
"  master                     The master projection data file name.\n" +
"  input                      The input data file name.\n" +
"  output                     The output data file name.\n" +
"\n" +
"Options:\n" +
"  -f, --srcfilter=TYPE       Filter the source pixels.  TYPE may be 'viirs'.\n" +
"  -h, --help                 Show this help message.\n" +
"  -m, --match=PATTERN        Register only variables matching the pattern.\n"+
"  -M, --method=TYPE          Set resampling method.  TYPE may be\n" +
"                              'inverse', or 'mixed'.\n" +
"  -O, --overwrite=TYPE       Set overwrite method.  TYPE may be 'always',\n" +
"                              'never', or 'closer' (advanced users).\n" +
"  -p, --polysize=KILOMETERS  Set polynomial rectangle width and height\n" +
"                              in kilometers for inverse resampling.\n" +
"  -r, --rectsize=WIDTH/HEIGHT\n" +
"                             Set rectangle size in pixels for mixed\n" +
"                              resampling.\n" +
"  -s, --srcexpr=EXPRESSION   Use only source pixels matching an expression.\n" +
"  -v, --verbose              Print verbose messages.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

  private cwregister () { }

  ////////////////////////////////////////////////////////////

} // cwregister class

////////////////////////////////////////////////////////////////////////
