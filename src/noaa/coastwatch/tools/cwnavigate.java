////////////////////////////////////////////////////////////////////////
/*
     FILE: cwnavigate.java
  PURPOSE: To add navigation corrections to CoastWatch HDF files.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/21
  CHANGES: 2004/01/23, PFH, modified to use SPLIT_REGEX and updated docs
           2004/06/09, PFH, modified to use new reader-based nav update
           2004/10/05, PFH, modified to use EarthTransform.getDimensions()
           2005/03/15, PFH, reformatted documentation and usage note
           2005/04/23, PFH, added ToolServices.setCommandLine()
           2005/09/19, PFH, added extra verbose output
           2007/04/19, PFH, added version printing
           2012/12/04, PFH, added call to canUpdateNavigation for reader

  CoastWatch Software Library and Utilities
  Copyright 2002-2012, USDOC/NOAA/NESDIS CoastWatch

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
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * <p>The navigation tool adds navigation corrections to 2D variables
 * in an Earth data file.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwnavigate - adds navigation corrections to Earth data.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>
 *   cwnavigate [OPTIONS] {-t, --trans=ROWS/COLS} input<br>
 *   cwnavigate [OPTIONS] {-r, --rotate=ANGLE} input<br>
 *   cwnavigate [OPTIONS] {-a, --affine=A/B/C/D/E/F} input<br>
 *   cwnavigate [OPTIONS] {-R, --reset} input<br>
 * </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -m, --match=PATTERN <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p>The navigation tool adds navigation corrections to 2D variables
 * in an Earth data file by setting navigation transform
 * parameters.  The most basic navigation transform consists of
 * additive translation in the row and column data coordinates.  As an
 * example of translation, the following diagram shows coastlines in
 * the Earth image data as a '.' (period) symbol and coastlines
 * derived from a GIS database as a '*' (star).  Translation has been
 * used to correct the position of the image data:</p>
 * <pre>
 *        ***                              ***
 *      ... ***         *****                ***         *****
 *        ... ***     **...                    ***     **
 *          ... *   .**          ----&gt;           *    **
 *            . *  **                            *  **
 *            . ****        row trans = 1        ****
 *            ....          col trans = -2
 * </pre>
 *
 * <p>A more generic navigation transform consists of a translation
 * combined with a rotation or shear.  To represent generic
 * navigation transforms, an affine transform matrix is used to
 * convert "desired" data coordinates to "actual" data coordinates as
 * follows:</p>
 * <pre>
 *   |row'|   |a  c  e|  |row|
 *   |    |   |       |  |   |
 *   |col'| = |b  d  f|  |col|
 *   |    |   |       |  |   |   
 *   | 1  |   |0  0  1|  | 1 | 
 * </pre>
 * <p>where [a..f] are the affine transform matrix coefficients and
 * (row',col') is the actual data coordinates at which the desired
 * data value for (row,col) may be found.</p>
 *
 * <p>To apply a navigation transform to a 2D variable, the existing
 * navigation transform is read and the new transform is applied to
 * it using matrix multiplication to create a combined transform.  As
 * an example, suppose that T1 is the initial navigation transform.
 * The application of an additional transform T2 results in a new
 * transform that is equivalent to:</p>
 * <pre>
 *   T2 (T1 (row, col))
 * </pre>
 *
 * <p>A navigation transform can be applied to a subset of 2D
 * variables, or all variables in the file.  Note that satellite
 * channel data or channel-derived variables should be corrected with
 * navigation but GIS-derived variables such as coastline and lat/lon
 * grid graphics should not be corrected.  Setting the navigation
 * transform simply establishes a mapping between desired and actual
 * data coordinates -- it does not change the gridded data values
 * themselves.  Once a navigation transform has been set, other
 * CoastWatch tools in this package will take the transform into
 * account when reading the data. </p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> -t, --trans=ROWS/COLS </dt>
 *   <dd> The translation transform to apply.  The actual row and
 *   column coordinates are calculated by adding the specified row and
 *   column translation to the desired coordinates. </dd>
 *
 *   <dt> -r, --rotate=ANGLE </dt>
 *   <dd> The rotation transform to apply.  The actual row and column
 *   coordinates are calculated by rotating the desired row and column
 *   coordinates about the data center by the specified angle in
 *   degrees.  Positive angles rotate counter-clockwise while negative
 *   angles rotate clockwise.</dd>
 *
 *   <dt> -a, --affine=A/B/C/D/E/F </dt> <dd> The explicit affine
 *   transform to apply.  The coefficients are used to form the affine
 *   transform matrix (see the Description section above) which is
 *   applied to the existing transform using matrix
 *   multiplication. </dd>
 *
 *   <dt> -R, --reset </dt>
 *   <dd> Specifies that the existing navigation transform should be
 *   reset to the identity. Under the identity transform, no
 *   navigation correction is performed.</dd>
 *   
 *   <dt> input </dt>
 *   <dd> The input data file name.  The navigation corrections are
 *   applied to the input file in-situ.  For CoastWatch HDF files, the
 *   corrections are applied to individual variables.  For CoastWatch
 *   IMGMAP files, corrections are applied to the global attributes
 *   and the <b>--match</b> option has no effect.  No other file
 *   formats are supported.</dd>
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt> -h, --help </dt>
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -m, --match=PATTERN </dt>
 *   <dd> The variable name matching pattern.  If specified, the
 *   pattern is used as a regular expression to match variable names.
 *   Only variables matching the pattern will be navigated.  By
 *   default, no pattern matching is performed and all variables are
 *   navigated. </dd>
 *
 *   <dt> -v, --verbose </dt>
 *   <dd> Turns verbose mode on.  The status of navigation
 *   correction is printed periodically.  The default is to run
 *   quietly. </dd>
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
 *   <li> Invalid input file name. </li>
 *   <li> Unsupported input file format. </li>
 *   <li> Unsupported navigation correction. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <p>The following example shows the navigation correction of a NOAA-15
 * CoastWatch HDF data file from the Gulf of Mexico:</p>
 * <pre>
 *   phollema$ cwnavigate --trans -3/3 -v --match '(avhrr.*|cloud|sst)' 
 *     2002_328_1326_n15_ml.hdf
 *
 *   cwnavigate: Reading input 2002_328_1326_n15_ml.hdf
 *   cwnavigate: Applying navigation correction to avhrr_ch1
 *   cwnavigate: Applying navigation correction to avhrr_ch2
 *   cwnavigate: Applying navigation correction to avhrr_ch4
 *   cwnavigate: Applying navigation correction to cloud
 *   cwnavigate: Applying navigation correction to sst
 * </pre>
 * <p>Another example below shows the navigation correction of a NOAA-15
 * CoastWatch IMGMAP data file from the US east coast:</p>
 * <pre>
 *   phollema$ cwnavigate --trans -2/1 -v 2002_326_1330_n15_er_c2.cwf
 *
 *   cwnavigate: Reading input 2002_326_1330_n15_er_c2.cwf
 *   cwnavigate: Applying navigation correction
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public final class cwnavigate {

  // Constants
  // ------------
  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  /** Name of program. */
  private static final String PROG = "cwnavigate";

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
    Option transOpt = cmd.addStringOption ('t', "trans");
    Option rotateOpt = cmd.addDoubleOption ('r', "rotate");
    Option affineOpt = cmd.addStringOption ('a', "affine");
    Option resetOpt = cmd.addBooleanOption ('R', "reset");
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
    String input = remain[0];

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    String match = (String) cmd.getOptionValue (matchOpt);
    String trans = (String) cmd.getOptionValue (transOpt);
    Double rotate = (Double) cmd.getOptionValue (rotateOpt);
    String affine = (String) cmd.getOptionValue (affineOpt);
    boolean reset = (cmd.getOptionValue (resetOpt) != null);

    try {

      // Open input file
      // ---------------
      if (verbose) System.out.println (PROG + ": Reading input " + input);
      EarthDataReader reader = EarthDataReaderFactory.create (input);

      // Check file format
      // ----------------- 
    if (!reader.canUpdateNavigation()) {
        System.err.println (PROG + ": Unsupported file format for " + input);
        System.exit (2);
      } // if       

      // Create translation transform
      // ----------------------------
      AffineTransform newNav = null;
      if (trans != null) {
        String[] transArray = trans.split (ToolServices.SPLIT_REGEX);
        if (transArray.length != 2) {
          System.err.println (PROG + ": Invalid translation '" + trans + "'");
          System.exit (2);
        } // if
        double rows = Double.parseDouble (transArray[0]);
        double cols = Double.parseDouble (transArray[1]);
        newNav = AffineTransform.getTranslateInstance (rows, cols);
      } // if

      // Create rotation transform
      // -------------------------
      else if (rotate != null) {
        double theta = Math.toRadians (rotate.doubleValue());
        EarthDataInfo info = reader.getInfo();
        int[] dims = info.getTransform().getDimensions();
        int rows = dims[0];
        int cols = dims[1];
        newNav = AffineTransform.getRotateInstance (
          theta, (rows-1)/2.0, (cols-1)/2.0);
      } // else if

      // Create generic transform
      // ------------------------
      else if (affine != null) {
        String[] affineArray = affine.split (ToolServices.SPLIT_REGEX);
        if (affineArray.length != 6) {
          System.err.println (PROG + ": Invalid affine '" + affine + "'");
          System.exit (2);
        } // if
        double[] matrix = new double[6];
        for (int i = 0; i < 6; i++) 
          matrix[i] =  Double.parseDouble (affineArray[i]);
        newNav = new AffineTransform (matrix);
      } // else

      // Create reset transform
      // ----------------------
      else if (reset) { newNav = null; }

      // Report error
      // ------------
      else {
        System.err.println (PROG + ": At least one navigation option" +
          " must be specified");
        System.exit (2);
      } // else

      // Get variable names
      // ------------------
      List variables = new ArrayList();
      for (int i = 0; i < reader.getVariables(); i++) {
        DataVariable var = reader.getPreview(i);
        if (var.getRank() != 2) continue;
        String varName = var.getName();
        if (match != null && !varName.matches (match)) continue;
        variables.add (varName);
        if (verbose) 
          System.out.println (PROG + ": Adding variable " + varName + 
            " to correction list");
      } // for      

      // Apply navigation
      // ----------------
      if (verbose)
        System.out.println (PROG + ": Applying navigation correction");
      reader.updateNavigation (variables, newNav);
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
"Usage: cwnavigate [OPTIONS] {-t, --trans=ROWS/COLS} input\n" +
"       cwnavigate [OPTIONS] {-r, --rotate=ANGLE} input\n" +
"       cwnavigate [OPTIONS] {-a, --affine=A/B/C/D/E/F} input\n" +
"       cwnavigate [OPTIONS] {-R, --reset} input\n" +
"Adds navigation corrections to 2D variables in an Earth data file\n" +
"by setting navigation transform parameters.\n" +
"\n" +
"Main parameters:\n" +
"  -t, --trans=ROWS/COLS      Apply translation correction.\n" +
"  -r, --rotate=ANGLE         Apply rotation correction.\n" +
"  -a, --affine=A/B/C/D/E/F   Apply generic affine correction.\n" +
"  -R, --reset                Reset correction to identity.\n" +
"  input                      The input data file name.\n" +
"\n" +
"Options:\n" +
"  -h, --help                 Show this help message.\n" +
"  -m, --match=PATTERN        Apply correction to variables matching the\n" +
"                              pattern.\n" +
"  -v, --verbose              Print verbose messages.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

  private cwnavigate () { }

  ////////////////////////////////////////////////////////////

} // cwnavigate

////////////////////////////////////////////////////////////////////////
