////////////////////////////////////////////////////////////////////////
/*

     File: cwgraphics.java
   Author: Peter Hollemans
     Date: 2003/05/28

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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import noaa.coastwatch.io.CWHDFReader;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.HDFCachedGrid;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.LatLonOverlay;
import noaa.coastwatch.render.PoliticalOverlay;
import noaa.coastwatch.render.SolidBackground;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The graphics tool creates earth data annotation graphics.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p> 
 *   <!-- START NAME -->          
 *   cwgraphics - creates earth data annotation graphics.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p>
 *   cwgraphics [OPTIONS] input <br> 
 *   cwgraphics [OPTIONS] input output
 * </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -c, --coast=PLANE <br>
 * -g, --grid=PLANE <br>
 * -h, --help <br>
 * -l, --land=PLANE <br>
 * -p, --political=PLANE <br>
 * -v, --verbose <br>
 * -V, --variable=NAME <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The graphics tool creates earth data annotation
 * graphics in the form of a byte-valued variable.  Each output byte
 * in the new variable contains 8 bits, one for each of 8 possible
 * graphics planes numbered 1 to 8 from the least significant bit to
 * the most significant bit.  The graphics planes are independent of
 * one another and encode a bitmask for graphical data annotation,
 * where a bit value of 0 is interpreted as 'off' and a bit value of 1
 * as 'on'.  In this way, 8 separate binary bitmasks may be encoded
 * into one byte value.  For example a pixel with graphics planes 2,
 * 3, and 4 on is encoded as:</p>
 * <pre>
 *    Binary value  = 00001110
 *    Decimal value = 14
 * </pre>
 * <p>Following the standard convention for graphics planes in CoastWatch
 * product files, the default behaviour places latitude/longitude grid
 * graphics in plane 2, coastline graphics in plane 3, and land mask
 * graphics in plane 4.  Coastlines are derived from GSHHS coastline
 * data, and land polygons are filled GSHHS polygons (see the
 * <a href="http://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html">GSHHS website</a>).
 * The default output variable name is 'graphics'.  These defaults may be changed
 * using command line options to alter the planes used for each type
 * of annotation, to exclude or add some types of annotation, and to
 * change the output variable name.</p>
 *
 * <p>Once the graphics planes are created, they may be used as overlay
 * graphics for rendered earth data images.  The graphics byte
 * data may be exported using the cwexport tool for use in other
 * software packages, or may be used in the cwrender tool with the
 * <b>--bitmask</b> option.</p>
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
 *   <dd> The output file name.  If the output file name is not
 *   specified, the input file name is used and the new variable must
 *   not already exist in the input file. </dd>
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt> -c, --coast=PLANE </dt>
 *   <dd> The coastline graphics plane.  The default is plane 3.  If
 *   the plane value is 0, no coast graphics are rendered. </dd>
 *
 *   <dt> -g, --grid=PLANE </dt>
 *   <dd> The grid line graphics plane.  The default is plane 2.  If
 *   the plane value is 0, no grid graphics are rendered. </dd>
 *
 *   <dt> -h, --help </dt>
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -l, --land=PLANE </dt>
 *   <dd> The land mask graphics plane.  The default is plane 4.  If
 *   the plane value is 0, no land graphics are rendered. </dd>
 *
 *   <dt> -p, --political=PLANE </dt>
 *   <dd> The political line graphics plane.  There is no default 
 *   plane for political lines, as they are normally excluded from
 *   rendering. </dd>
 *
 *   <dt> -v, --verbose </dt>
 *   <dd> Turns verbose mode on.  The current status of data
 *   rendering is printed periodically.  The default is to run
 *   quietly. </dd>
 *
 *   <dt> -V, --variable=NAME </dt>
 *   <dd> The output variable name.  The default name is
 *   'graphics'. </dd>
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
 *   <li> Output variable already exists in input file </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the creation of a standard set of graphics
 * planes using cwgraphics.  The file being acted upon is a CoastWatch
 * HDF file created using the graphical cwmaster tool:</p>
 * <pre>
 *   phollema$ cwgraphics -v bc_coast.hdf
 
 *   [INFO] Reading input bc_coast.hdf
 *   [INFO] Creating graphics variable
 *   [INFO] Rendering overlay at plane 2
 *   [INFO] Rendering overlay at plane 3
 *   [INFO] Rendering overlay at plane 4
 * </pre>
 * <p>Another example below shows the alteration of the default options.
 * Only coastline and political line graphics are rendered to plane
 * 1, and the output variable is named 'geography':</p>
 * <pre>
 *   phollema$ cwgraphics -v --land 0 --grid 0 --coast 1 --political 1 
 *     --variable geography bc_coast.hdf
 *
 *   [INFO] Reading input bc_coast.hdf
 *   [INFO] Creating geography variable
 *   [INFO] Rendering overlay at plane 1
 *   [INFO] Rendering overlay at plane 1
 *   [INFO] Rendering overlay at plane 1
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.5
 */
public class cwgraphics {

  private static final String PROG = cwgraphics.class.getName();
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
    Option gridOpt = cmd.addIntegerOption ('g', "grid");
    Option coastOpt = cmd.addIntegerOption ('c', "coast");
    Option landOpt = cmd.addIntegerOption ('l', "land");
    Option politicalOpt = cmd.addIntegerOption ('p', "political");
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
      System.exit (0);
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
    String output = ((remain.length == 1) ? input : remain[1]);

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose) VERBOSE.setLevel (Level.INFO);
    Integer gridObj = (Integer) cmd.getOptionValue (gridOpt);
    int grid = (gridObj == null? 2 : gridObj.intValue());
    Integer coastObj = (Integer) cmd.getOptionValue (coastOpt);
    int coast = (coastObj == null? 3 : coastObj.intValue());
    Integer landObj = (Integer) cmd.getOptionValue (landOpt);
    int land = (landObj == null? 4 : landObj.intValue());
    Integer politicalObj = (Integer) cmd.getOptionValue (politicalOpt);
    int political = (politicalObj == null? 0 : politicalObj.intValue());
    String variable = (String) cmd.getOptionValue (variableOpt);
    if (variable == null) variable = "graphics";

    try {

      // Open files
      // ----------
      EarthDataReader reader;
      CWHDFWriter writer;
      EarthDataInfo info;
      if (input.equals (output)) {
        VERBOSE.info ("Reading input " + input);
        writer = new CWHDFWriter (input);
        reader = new CWHDFReader (writer);
        info = reader.getInfo();     
      } // if
      else {
        VERBOSE.info ("Reading input " + input);
        reader = EarthDataReaderFactory.create (input);
        info = reader.getInfo();
        VERBOSE.info ("Creating output " + output);
        writer = new CWHDFWriter (info, output);
      } // else

      // Get dimensions
      // --------------
      int[] dims = info.getTransform().getDimensions();
      int rows = dims[ROW];
      int cols = dims[COL];

      // Create output variable
      // ----------------------
      VERBOSE.info ("Creating " + variable + " variable");
      NumberFormat format = NumberFormat.getInstance();
      format.setMaximumFractionDigits (0);
      Grid gridVar = new Grid (variable, "graphics overlay planes", null, rows, cols,
        new byte[0], format, null, Byte.valueOf ((byte)0));
      gridVar.setUnsigned (true);
      Grid outputVar = new HDFCachedGrid (gridVar, writer);

      // Create solid background view
      // ----------------------------
      EarthDataView view = new SolidBackground (info.getTransform(),
        new int[] {rows, cols}, Color.BLACK);

      // Create buffered image
      // ---------------------
      BufferedImage image = new BufferedImage (cols, rows,
        BufferedImage.TYPE_BYTE_GRAY);
      Graphics2D g = image.createGraphics();
      Raster rast = image.getRaster();
      int[] pixelRow = new int[cols];
      Point imagePoint = new Point();
      int[] dataCoord = new int[2];
      view.computeCaches (null);

      // Create overlay and bit lists
      // ------------------------------
      List overlays = new ArrayList();
      List bits = new ArrayList();
      if (grid > 0) {
        overlays.add (new LatLonOverlay (Color.WHITE));
        bits.add (Integer.valueOf (grid));
      } // if
      if (coast > 0) { 
        CoastOverlay coastOverlay = new CoastOverlay (Color.WHITE);
        coastOverlay.setSmallPolygons (true);
        overlays.add (coastOverlay);
        bits.add (Integer.valueOf (coast));
      } // if
      if (land > 0) {
        CoastOverlay coastOverlay = new CoastOverlay (Color.WHITE);
        coastOverlay.setSmallPolygons (true);
        coastOverlay.setFillColor (Color.WHITE);
        overlays.add (coastOverlay);
        bits.add (Integer.valueOf (land));
      } // if
      if (political > 0) {
        PoliticalOverlay poliOverlay = new PoliticalOverlay (Color.WHITE);
        poliOverlay.setState (true);
        overlays.add (poliOverlay);
        bits.add (Integer.valueOf (political));
      } // if

      // Render overlays
      // ---------------
      for (int k = 0; k < overlays.size(); k++) {
        EarthDataOverlay overlay = (EarthDataOverlay) overlays.get (k);
        int bit = ((Integer) bits.get (k)).intValue();
        int bitValue = 1 << (bit-1);
        VERBOSE.info ("Rendering overlay at plane " + bit);
        view.addOverlay (overlay);
        view.render (g);
        for (int i = 0; i < rows; i++) {
          rast.getPixels (0, i, cols, 1, pixelRow);
          for (int j = 0; j < cols; j++) {
            imagePoint.setLocation (j, i);
            view.transform (imagePoint, dataCoord);
            int existingByte = (int) outputVar.getValue (dataCoord[ROW], dataCoord[COL]);
            int graphicsByte = (pixelRow[j] != 0 ? bitValue : 0);
            int newByte = existingByte | graphicsByte;
            outputVar.setValue (dataCoord[ROW], dataCoord[COL], newByte);
          } // for
        } // for
        view.removeOverlay (overlay);
      } // for

      // Close files
      // -----------
      reader.close();
      writer.close();

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

    UsageInfo info = new UsageInfo ("cwgraphics");

    info.func ("Creates earth data annotation graphics");

    info.param ("input", "Input data file", 1);
    info.param ("input", "Input data file", 2);
    info.param ("output", "Output data file", 2);

    info.option ("-c, --coast=PLANE", "Set coastline graphics plane");
    info.option ("-g, --grid=PLANE", "Set latitude/longitude grid graphics plane");
    info.option ("-h, --help", "Show this help message");
    info.option ("-l, --land=PLANE", "Set land mask graphics plane");
    info.option ("-p, --political=PLANE", "Set political line graphics plane");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("-V, --variable=NAME", "Set output variable name");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwgraphics () { }

  ////////////////////////////////////////////////////////////

} // cwgraphics

////////////////////////////////////////////////////////////////////////

