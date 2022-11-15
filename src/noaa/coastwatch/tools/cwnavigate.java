////////////////////////////////////////////////////////////////////////
/*

     File: cwnavigate.java
   Author: Peter Hollemans
     Date: 2002/11/21

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
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The navigation tool adds navigation corrections to 2D variables
 * in an earth data file.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwnavigate - adds navigation corrections to earth data.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>
 *   cwnavigate [OPTIONS] input
 * </p>
 * 
 * <h3>Correction options:</h3>
 *
 * <p>
 *   -t, --trans=ROWS/COLS <br>
 *   -r, --rotate=ANGLE <br>
 *   -a, --affine=A/B/C/D/E/F <br>
 *   -R, --reset <br>
 * </p>
 *
 * <h3>General options:</h3>
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
 * in an earth data file by setting navigation transform
 * parameters.  The most basic navigation transform consists of
 * additive translation in the row and column data coordinates.  As an
 * example of translation, the following diagram shows coastlines in
 * the earth image data as a '.' (period) symbol and coastlines
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
 * data coordinates &#8212; it does not change the gridded data values
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
 *   <dt> input </dt>
 *   <dd> The input data file name.  The navigation corrections are
 *   applied to the input file in-situ.  For CoastWatch HDF files, the
 *   corrections are applied to individual variables.  No other file
 *   formats are supported.</dd>
 *
 * </dl>
 *
 * <h3>Correction options:</h3>
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
 * </dl>
 * 
 * <h3>General options:</h3>
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
 *   <li> Invalid command line option </li>
 *   <li> Invalid input file name </li>
 *   <li> Unsupported input file format </li>
 *   <li> Unsupported navigation correction </li>
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
 * CoastWatch HDF data file from the US east coast:</p>
 * <pre>
 *   phollema$ cwnavigate --trans -2/1 -v 2002_326_1330_n15_er_c2.hdf
 *
 *   cwnavigate: Reading input 2002_326_1330_n15_er_c2.hdf
 *   cwnavigate: Applying navigation correction
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
 public final class cwnavigate {

  private static final String PROG = cwnavigate.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ------------

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

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose) VERBOSE.setLevel (Level.INFO);
    String match = (String) cmd.getOptionValue (matchOpt);
    String trans = (String) cmd.getOptionValue (transOpt);
    Double rotate = (Double) cmd.getOptionValue (rotateOpt);
    String affine = (String) cmd.getOptionValue (affineOpt);
    boolean reset = (cmd.getOptionValue (resetOpt) != null);

    EarthDataReader reader = null;
    try {

      // Open input file
      // ---------------
      VERBOSE.info ("Reading input " + input);
      reader = EarthDataReaderFactory.create (input);

      // Check file format
      // ----------------- 
      if (!reader.canUpdateNavigation()) {
        LOGGER.severe ("Unsupported file format for " + input);
        ToolServices.exitWithCode (2);
        return;
      } // if       

      // Create translation transform
      // ----------------------------
      AffineTransform newNav = null;
      if (trans != null) {
        String[] transArray = trans.split (ToolServices.SPLIT_REGEX);
        if (transArray.length != 2) {
          LOGGER.severe ("Invalid translation '" + trans + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        double rows = Double.parseDouble (transArray[0]);
        double cols = Double.parseDouble (transArray[1]);
        newNav = AffineTransform.getTranslateInstance (rows, cols);
        VERBOSE.info ("Using translation transform");
      } // if

      // Create rotation transform
      // -------------------------
      else if (rotate != null) {
        double theta = Math.toRadians (rotate.doubleValue());
        EarthDataInfo info = reader.getInfo();
        int[] dims = info.getTransform().getDimensions();
        int rows = dims[0];
        int cols = dims[1];
        newNav = AffineTransform.getRotateInstance (theta, (rows-1)/2.0, (cols-1)/2.0);
        VERBOSE.info ("Using rotation transform");
      } // else if

      // Create generic transform
      // ------------------------
      else if (affine != null) {
        String[] affineArray = affine.split (ToolServices.SPLIT_REGEX);
        if (affineArray.length != 6) {
          LOGGER.severe ("Invalid affine '" + affine + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        double[] matrix = new double[6];
        for (int i = 0; i < 6; i++) 
          matrix[i] =  Double.parseDouble (affineArray[i]);
        newNav = new AffineTransform (matrix);
        VERBOSE.info ("Using explicit affine transform");
      } // else

      // Create reset transform
      // ----------------------
      else if (reset) { 
        newNav = null; 
        VERBOSE.info ("Using a null transform (reset mode)");        
      } // else if

      // Report error
      // ------------
      else {
        LOGGER.severe ("At least one navigation option must be specified");
        ToolServices.exitWithCode (2);
        return;
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
        VERBOSE.info ("Adding variable " + varName + " to correction list");
      } // for      

      // Apply navigation
      // ----------------
      VERBOSE.info ("Applying navigation correction");
      reader.updateNavigation (variables, newNav);
      reader.close();
      reader = null;

    } // try
    catch (Exception e) {
      LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    finally {
      try {
        if (reader != null) reader.close();
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

    UsageInfo info = new UsageInfo ("cwnavigate");

    info.func ("Adds navigation corrections to earth data");

    info.param ("input", "Input data file");

    info.section ("Correction");
    info.option ("-t, --trans=ROWS/COLS", "Apply translation correction");
    info.option ("-r, --rotate=ANGLE", "Apply rotation correction");    
    info.option ("-a, --affine=A/B/C/D/E/F", "Apply generic affine correction");
    info.option ("-R, --reset", "Reset correction to identity");

    info.section ("General");
    info.option ("-h, --help", "Show help message");
    info.option ("-m, --match=PATTERN", "Correct only variables matching regular expression");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwnavigate () { }

  ////////////////////////////////////////////////////////////

} // cwnavigate

////////////////////////////////////////////////////////////////////////
