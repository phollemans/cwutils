////////////////////////////////////////////////////////////////////////
/*

     File: cwinfo.java
   Author: Mark Robinson
     Date: 2002/04/15

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
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import java.util.logging.Logger;
import java.util.logging.Level;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.ReaderSummaryProducer;
import noaa.coastwatch.io.ReaderSummaryProducer.SummaryTable;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.TextReportFormatter;

/**
 * <p>The information utility dumps earth data information in a
 * display-friendly format.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwinfo - prints earth data file information.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p> cwinfo [OPTIONS] input </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -t, --transform <br>
 * -c, --coord <br>
 * -e, --edge <br>
 * -l, --locFormat=TYPE <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The information utility dumps earth data
 * information in a display-friendly format.  The global earth
 * information is printed such as satellite name, sensor, date, and
 * earth transform information.  The name of each variable is printed
 * along with its data type, dimensions, scaling factor, and so on.
 * For more detailed printing of generic HDF file contents, use the
 * HDF hdp command. </p>
 *
 * <p>When the <b>--transform</b> option is used, various additional
 * earth transform information is printed.  Let nc and nr be the x and
 * y coordinate dimensions respectively, and mc=(nc-1)/2, mr=(nr-1)/2
 * be the midpoint coordinates.  Note that indexing is zero-based and
 * coordinates refer to the pixel center.  Then the following
 * information is computed:</p>
 * <ul>
 *   <li>Pixel width at (mc,mr)</li>
 *   <li>Pixel height at (mc,mr)</li>
 *   <li>Total width from (0,mr) to (nc-1,mr)</li>
 *   <li>Total height from (mc,0) to (mc,nr-1)</li>
 *   <li>Center lat/lon at (mc,mr)</li>
 *   <li>Upper-left lat/lon at (0,0)</li>
 *   <li>Upper-right lat/lon at (nc-1,0)</li>
 *   <li>Lower-left lat/lon at (0,nr-1)</li>
 *   <li>Lower-right lat/lon at (nc-1,nr-1)</li>
 * </ul>
 *
 * <p>When the <b>--coord</b> option is used, Common Data Model
 * coordinate systems are printed if available.  Generally this
 * style of coordinate system information is only available for
 * files read by the NetCDF Java library.</p>
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
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt> -h, --help </dt>
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -t, --transform </dt>
 *   <dd> Specifies that additional earth transform information should
 *   also be printed.  The default is to show only global and variable
 *   information. </dd>
 *
 *   <dt> -c, --coord </dt>
 *   <dd> Specifies that Common Data Model coordinate system
 *   information should also be printed.  The default is to show
 *   only global and variable information. </dd>
 *
 *   <dt> -e, --edge </dt>
 *   <dd> Specifies that transform information should print the coordinates of
 *   the extreme edges of the corner pixels.  The default is to print 
 *   the coordinates of the center of corner pixels. </dd>
 *
 *   <dt> -l, --locFormat=TYPE </dt>
 *   <dd> Specifies the format style for geographic coordinates printed by the
 *   <b>--transform</b> option.  Valid values are:
 *   <ul>
 *     <li>D - Integer degrees, eg: '124 W'.</li>
 *     <li>DD - 2-digit degrees, eg: '124.36 W'.</li>
 *     <li>DDDD - 4-digit degrees, eg: '124.3600 W'.</li>
 *     <li>DDMM - Degrees, minutes, eg: '124 21.60 W'.</li>
 *     <li>DDMMSS - Degrees, minutes, seconds, eg: '124 21 36.00 W'.</li>
 *     <li>RAW - Decimal degrees up to the full 64-bit precision, eg: '-124.36003592404',
 *     in the range [-90,90] for latitude and [-180,180] for longitude.</li>
 *   </ul>
 *   The default is 'DDDD'.</dd>
 *
 *   <dt> -v, --verbose </dt>
 *   <dd> Turns verbose mode on.  The current status of automatic file
 *   identification is printed.  This output is useful when trying to
 *   understand why a certain file is not being recognized by this and
 *   other tools.  The default is to run quietly. </dd>
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
 *   <li> Invalid input file name </li>
 *   <li> Unsupported input file format </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows an information dump of a CoastWatch HDF file:</p>
 * <pre>
 *   phollema$ cwinfo --transform 2019_320_0511_m01_wj.hdf
 *
 *   Contents of file 2019_320_0511_m01_wj.hdf
 *
 *   Global information:
 *     Satellite:           metop-1
 *     Sensor:              avhrr
 *     Date:                2019/11/16 JD 320
 *     Time:                05:11:42 UTC
 *     Scene time:          night
 *     Projection type:     mapped
 *     Transform ident:     noaa.coastwatch.util.trans.MercatorProjection
 *     Map projection:      Mercator
 *     Map affine:          0 -1470 1470 0 -13397799.15 3887086.51
 *     Spheroid:            WGS 84
 *     Origin:              USDOC/NOAA/NESDIS CoastWatch
 *     Format:              CoastWatch HDF version 3.4
 *     Reader ident:        noaa.coastwatch.io.CWHDFReader

 *   Variable information:
 *     Variable       Type    Dimensions  Units          Scale     Offset
 *     avhrr_ch3      short   1024x1024   celsius        0.01      0
 *     avhrr_ch4      short   1024x1024   celsius        0.01      0
 *     avhrr_ch5      short   1024x1024   celsius        0.01      0
 *     cloud          ubyte   1024x1024   -              1         0
 *     graphics       ubyte   1024x1024   -              1         0
 *     sat_zenith     short   1024x1024   degrees        0.01      0
 *     sst            short   1024x1024   celsius        0.01      0
 *
 *   Earth location information:
 *     Pixel width:                   1.3054 km
 *     Pixel height:                  1.3123 km
 *     Total width:                   1334.7650 km
 *     Total height:                  1340.7132 km
 *     Center:                        27.2500 N, 113.6000 W
 *     Upper-left (pixel center):     33.1139 N, 120.3545 W
 *     Upper-right (pixel center):    33.1139 N, 106.8455 W
 *     Lower-left (pixel center):     21.0565 N, 120.3545 W
 *     Lower-right (pixel center):    21.0565 N, 106.8455 W
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Mark Robinson
 * @since 3.1.0
 */
public final class cwinfo {

  private static final String PROG = cwinfo.class.getName();
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
  public static void main (String argv[]) throws IOException {

    ToolServices.startExecution (PROG);
    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option transformOpt = cmd.addBooleanOption ('t', "transform");
    Option coordOpt = cmd.addBooleanOption ('c', "coord");
    Option edgeOpt = cmd.addBooleanOption ('e', "edge");
    Option locFormatOpt = cmd.addStringOption ('l', "locFormat");
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
    if (cmd.getRemainingArgs().length < NARGS) {
      LOGGER.warning ("At least " + NARGS + " argument(s) required");
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // if
    String input = cmd.getRemainingArgs()[0];

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    boolean transform = (cmd.getOptionValue (transformOpt) != null);
    boolean coord = (cmd.getOptionValue (coordOpt) != null);
    Boolean edgeObj = (Boolean) cmd.getOptionValue (edgeOpt);
    boolean edge = (edgeObj == null ? false : edgeObj.booleanValue());
    String locFormatObj = (String) cmd.getOptionValue (locFormatOpt);
    int locFormat = EarthLocation.DDDD;
    if (locFormatObj != null) {
      if (locFormatObj.equals ("D"))
        locFormat = EarthLocation.D;
      else if (locFormatObj.equals ("DD"))
        locFormat = EarthLocation.DD;
      else if (locFormatObj.equals ("DDDD"))
        locFormat = EarthLocation.DDDD;
      else if (locFormatObj.equals ("DDMM"))
        locFormat = EarthLocation.DDMM;
      else if (locFormatObj.equals ("DDMMSS"))
        locFormat = EarthLocation.DDMMSS;
      else if (locFormatObj.equals ("RAW"))
        locFormat = EarthLocation.RAW;
      else {
        LOGGER.severe ("Invalid location format '" + locFormatObj + "'");
        ToolServices.exitWithCode (2);
        return;
      } // else
    } // if

    // Setup verbose mode in factory
    // -----------------------------
    EarthDataReaderFactory.setVerbose (verbose);

    // Open the reader and print the requested information
    EarthDataReader reader = null;
    try {
      //    SwathProjection.setNullMode (true);
      reader = EarthDataReaderFactory.create (input);
      printSummary (reader, System.out, edge, locFormat, transform, coord);
      reader.close();
      reader = null;
    } // try

    catch (OutOfMemoryError | Exception e) {
      ToolServices.warnOutOfMemory (e);
      LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    finally {
      try { if (reader != null) reader.close(); }
      catch (Exception e) { LOGGER.log (Level.SEVERE, "Error closing resources", e); }
    } // finally

    ToolServices.finishExecution (PROG);

  } // main

  ////////////////////////////////////////////////////////////

  private static void printMap (Map<String, String> map, PrintStream stream) {

    int maxKey = map.keySet().stream().mapToInt (key -> key.length()).max().orElse (0);
    var fmt = "  %-" + (maxKey+1) + "s     %s\n";
    map.forEach ((key, value) -> stream.format (fmt, key + ":", value));

  } // printMap

  ////////////////////////////////////////////////////////////

  private static void printTable (SummaryTable table, PrintStream stream) {

    int cols = table.columnNames.length;
    int[] columnSizes = new int[cols];
    for (int i = 0; i < cols; i++) {
      var index = i;
      columnSizes[i] = Math.max (table.columnNames[i].length(), table.rowList.stream().mapToInt (row -> row[index].length()).max().orElse (0));
    } // for

    var buf = new StringBuffer();
    buf.append ("  ");
    for (int i = 0; i < cols; i++) buf.append ("%-" + columnSizes[i] + "s   ");
    buf.append ("\n");
    var fmt = buf.toString();

    stream.format (fmt, (Object[]) table.columnNames);
    table.rowList.forEach (row -> stream.format (fmt, (Object[]) row));

  } // printTable

  ////////////////////////////////////////////////////////////

  /**
   * Prints earth transform data from the specified file.  
   *
   * @param reader the reader object to use.
   * @param stream the output stream for printing.
   * @param transform the transform flag, true to print detailed earth location
   * data.  The earth transform data includes pixel resolution, total width 
   * and height, and latitude and longitude data for selected locations.
   */
  public static void printSummary (
    EarthDataReader reader,
    PrintStream stream,
    boolean transform
  ) {

    printSummary (reader, stream, false, EarthLocation.DDDD, transform, false);

  } // printSummary

  ////////////////////////////////////////////////////////////

  /**
   * Prints earth transform data from the specified file.  
   *
   * @param reader the reader object to use.
   * @param stream the output stream for printing.
   * @param useEdges true to use actual edges for location values,
   * false to use center of edge pixels.
   * @param locFormat the Earth location format code,
   * see {@link EarthLocation#format}.
   * @param transform the transform flag, true to print detailed earth location
   * data.  The earth transform data includes pixel resolution, total width 
   * and height, and latitude and longitude data for selected locations.
   * @param coord the coord flag, true to print detailed coordinate system
   * data.  
   */
  public static void printSummary (
    EarthDataReader reader,
    PrintStream stream,
    boolean useEdges,
    int locFormat,
    boolean transform,
    boolean coord
  ) {

    try {

      var producer = ReaderSummaryProducer.getInstance();
      var summary = producer.create (reader, useEdges, locFormat);
      var formatter = TextReportFormatter.create();
      producer.report (summary, formatter, true, true, transform, coord);
      stream.print (formatter.getContent());

    } catch (IOException e) {
      LOGGER.warning (e.getMessage());
    } // catch

  } // printSummary

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  private static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cwinfo");

    info.func ("Dumps earth data information in a display-friendly format");

    info.param ("input", "Input data file");

    info.option ("-h, --help", "Show help message");
    info.option ("-t, --transform", "Print earth transform info");
    info.option ("-c, --coord", "Print CDM coordinate info");
    info.option ("-e, --edge", "Print edge of pixels in transform");
    info.option ("-l, --locFormat=TYPE", "Set output format for locations");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    return (info);

  } // usage

  ////////////////////////////////////////////////////////////

  private cwinfo () { }

  ////////////////////////////////////////////////////////////

} // cwinfo class

////////////////////////////////////////////////////////////////////////
