////////////////////////////////////////////////////////////////////////
/*

     File: cwstats.java
   Author: Peter Hollemans
     Date: 2002/05/25

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
import com.braju.format.Format;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.OptionException;

import java.awt.Shape;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.SimpleParser;
import noaa.coastwatch.render.Subregion;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.VariableStatisticsGenerator;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.Datum;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The statistics utility calculates a number of statistics for each
 * variable in an earth data file.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p> 
 *   <!-- START NAME -->
 *   cwstats - calculates earth data file statistics.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p> cwstats [OPTIONS] input </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -i, --region=LAT/LON/RADIUS <br>
 * -l, --limit=STARTROW/STARTCOL/ENDROW/ENDCOL <br>
 * -m, --match=PATTERN <br>
 * -p, --polygon=FILE <br>
 * -s, --stride=N <br>
 * -S, --sample=FACTOR <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The statistics utility calculates a number of
 * statistics for each variable in an earth data file:</p>
 * <ul>
 *   <li> Count - the count of total data values sampled </li>
 *   <li> Valid - the number of valid (not missing) data values </li>
 *   <li> Min - the minimum data value </li>
 *   <li> Max - the maximum data value </li>
 *   <li> Mean - the average data value </li>
 *   <li> Stdev - the standard deviation from the mean </li>
 *   <li> Median - the median data value </li>
 * </ul> 
 * <p>To speed up the statistics calculations, a subset of the data values
 * in each variable may be specified using either the <b>--stride</b>
 * or <b>--sample</b> options, and one of the <b>--limit</b>, <b>--region</b>, 
 * or <b>--polygon</b> options.  The <b>--match</b> option may also be used 
 * to limit the statistics calculations to a subset of the variables.</p>
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
 *   <dt> -i, --region=LAT/LON/RADIUS</dt>
 *   <dd> The sampling region for each two-dimensional variable.  The
 *   region is specified by the center latitude and longitude in
 *   degrees, and the radius from the center in kilometers.  Only data
 *   within the rectangle specified by the center and radius is
 *   sampled.  By default, all data is sampled.  Only one of the
 *   <b>--region</b>, <b>--limit</b>, or <b>--polygon</b> options may be
 *   specified.</dd>
 *
 *   <dt> -l, --limit=STARTROW/ENDROW/STARTCOL/ENDCOL</dt> 
 *   <dd> The sampling limits for each two-dimensional variable in
 *   image coordinates.  Only data between the limits is sampled.  By
 *   default, all data is sampled.  Only one of the
 *   <b>--region</b>, <b>--limit</b>, or <b>--polygon</b> options may be
 *   specified.</dd>
 *
 *   <dt> -m, --match=PATTERN </dt>
 *   <dd> The variable name matching pattern.  If specified, the
 *   pattern is used as a regular expression to match variable names.
 *   Only variables matching the pattern are included in the
 *   calculations.  By default, no pattern matching is performed and
 *   all variables are included. </dd>
 *
 *   <dt> -p, --polygon=FILE </dt>
 *   <dd> The file name containing a list of polygon vertex points to use for
 *   constraining the statistics calculation.  The file must be an
 *   ASCII text file containing vertex points as latitude / longitude
 *   pairs, one pair per line, with values separated by spaces or
 *   tabs.  The points are specified in terms of earth location
 *   latitude and longitude in the range [-90..90] and
 *   [-180..180] with a datum matching that of the input file.  Only data 
 *   inside the polygon is sampled.  By default, all data is sampled.  
 *   Only one of the <b>--region</b>, <b>--limit</b>, or <b>--polygon</b> 
 *   options may be specified.</dd>
 *
 *   <dt> -s, --stride=N </dt>
 *   <dd> The sampling frequency for each variable dimension.  The
 *   default is to sample all data values (stride = 1). </dd>
 *
 *   <dt> -S, --sample=FACTOR </dt>
 *   <dd> The sampling factor for each variable.  The sampling factor
 *   is a value in the range [0..1] that specifies the number of data
 *   values sampled as a fraction of the total number of data values.
 *   To sample 1 percent of all data values, the sample factor would
 *   be 0.01.  The default is to sample all data values (factor =
 *   1). </dd>
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
 *   <li> Error reading input data values </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows a statistics calculation on a
 * CoastWatch HDF file from the Great Lakes:</p>
 * <pre>
 *   phollema$ cwstats 2002_197_1719_n16_gr.hdf
 *
 *   Variable       Count     Valid     Min       Max       Mean       Stdev     
 *   avhrr_ch1      1048576   483728    3.49      74.36     13.059646  11.371605 
 *   avhrr_ch2      1048576   483728    1.97      71.35     18.520041  9.844144  
 *   avhrr_ch3a     1048576   483728    0.53      52.84     14.664213  8.88201   
 *   avhrr_ch4      1048576   483728    -44.8     31.55     11.052207  13.683309 
 *   avhrr_ch5      1048576   483728    -45.48    27.05     7.978351   13.185983 
 *   sst            1048576   483728    -44.51    51.43     20.166333  16.714169 
 *   cloud          1048576   1048576   0         127       23.24175   37.179013 
 *   sat_zenith     1048576   483728    0.36      0.7       0.466376   0.077153  
 *   sun_zenith     1048576   483728    0.87      0.95      0.907019   0.022209  
 *   rel_azimuth    1048576   483728    -0.58     -0.33     -0.465731  0.058149  
 *   graphics       1048576   1048576   0         14        6.84576    2.931459  
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
 
public final class cwstats {

  private static final String PROG = cwstats.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);

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
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option strideOpt = cmd.addIntegerOption ('s', "stride");
    Option sampleOpt = cmd.addDoubleOption ('S', "sample");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option limitOpt = cmd.addStringOption ('l', "limit");
    Option polygonOpt = cmd.addStringOption ('p', "polygon");
    Option regionOpt = cmd.addStringOption ('i', "region");
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
    Integer strideObj = (Integer) cmd.getOptionValue (strideOpt);
    int stride = (strideObj == null ? 1 : strideObj.intValue());
    Double sampleObj = (Double) cmd.getOptionValue (sampleOpt);
    double sample = (sampleObj == null ? Double.NaN : sampleObj.doubleValue());
    String match = (String) cmd.getOptionValue (matchOpt);
    String limit = (String) cmd.getOptionValue (limitOpt);
    String region = (String) cmd.getOptionValue (regionOpt);
    String polygon = (String) cmd.getOptionValue (polygonOpt);

    // Get limits
    // ----------
    DataLocation start = null, end = null;
    if (limit != null) {
      String[] limitArray = limit.split (ToolServices.SPLIT_REGEX);
      if (limitArray.length != 4) {
        LOGGER.severe ("Invalid limit '" + limit + "'");
        ToolServices.exitWithCode (2);
        return;
      } // if
      start = new DataLocation (Double.parseDouble (limitArray[0]),
        Double.parseDouble (limitArray[1]));
      end = new DataLocation (Double.parseDouble (limitArray[2]),
        Double.parseDouble (limitArray[3]));
    } // if

    EarthDataReader reader = null;

    try {

      // Open file
      // ---------
      try {
        reader = EarthDataReaderFactory.create (input);
      } // try
      catch (Exception e) {
        LOGGER.severe (e.getMessage());
        ToolServices.exitWithCode (1);
        return;
      } // catch

      // Get region limits
      // -----------------
      if (region != null) {
        String[] regionArray = region.split (ToolServices.SPLIT_REGEX);
        if (regionArray.length != 3) {
          LOGGER.severe ("Invalid region '" + region + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        EarthLocation centerLoc = new EarthLocation (
          Double.parseDouble (regionArray[0]),
          Double.parseDouble (regionArray[1])
        );
        double radius = Double.parseDouble (regionArray[2]);
        Subregion subregion = new Subregion (centerLoc, radius);
        start = new DataLocation (2);
        end = new DataLocation (2);
        boolean success = subregion.getLimits (reader.getInfo().getTransform(),
          start, end);
        if (!success) {
          LOGGER.severe ("Error getting region limits");
          ToolServices.exitWithCode (2);
          return;
        } // if
      } // if

      // Get polygon point data
      // ----------------------
      Shape polygonShape = null;
      if (polygon != null) {

        // Read points
        // -----------
        List<EarthLocation> locations = new ArrayList<EarthLocation>();
        EarthTransform trans = reader.getInfo().getTransform();
        Datum datum = trans.getDatum();
        try {
          SimpleParser parser = new SimpleParser (new BufferedReader (
            new InputStreamReader (new FileInputStream (new File (polygon)))));
          do {
            double lat = parser.getNumber();
            double lon = parser.getNumber();
            locations.add (new EarthLocation (lat, lon, datum));
          } while (!parser.eof());
        } // try
        catch (IOException e) {
          LOGGER.severe ("Error parsing polygon points file");
          ToolServices.exitWithCode (2);
          return;
        } // catch

        // Convert to data location path
        // -----------------------------
        Iterator<EarthLocation> iter = locations.iterator();
        if (iter.hasNext()) {
          DataLocation dataLoc = new DataLocation (2);
          trans.transform (iter.next(), dataLoc);
          Path2D path = new Path2D.Double();
          path.moveTo (dataLoc.get (0), dataLoc.get (1));
          iter.forEachRemaining (earthLoc -> {
            trans.transform (earthLoc, dataLoc);
            path.lineTo (dataLoc.get (0), dataLoc.get (1));
          });
          path.closePath();
          polygonShape = path;
        } // if

      } // if

      // Print stats
      // -----------
      printStats (reader, start, end, polygonShape, stride, sample, match);

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

  /**
   * Prints the variable statistics information.
   *
   * @param reader the earth data reader object to use.
   * @param start the starting data location, or null for the
   * beginning of the data.
   * @param end the ending data location, or null for the end of the
   * data.
   * @param polygon the polygon to use for constraining the statistics
   * calculation, or null to use the values of start and end.
   * @param stride the sampling stride for each variable.
   * @param sample the sampling factor for each variable, or
   * <code>Double.NaN</code> if the sampling stride should be used.
   * @param match the variable name matching pattern, or null for
   * all variables.
   *
   * @see DataVariable#getStatistics(DataLocation,DataLocation,int[])
   * @see DataVariable#getStatistics(DataLocation,DataLocation,double)
   * @see DataVariable#getStatistics(int[])
   * @see DataVariable#getStatistics(double)
   */
  public static void printStats (
    EarthDataReader reader,
    DataLocation start,
    DataLocation end,
    Shape polygon,
    int stride,
    double sample,
    String match
  ) {

    // Check variable count
    // --------------------
    int vars = reader.getVariables ();
    if (vars == 0)
      return;

    // Get limit rank
    // --------------
    int limitRank = 0;
    if (start != null && end != null) {
      limitRank = start.getRank();
      if (end.getRank() != limitRank)
        throw new RuntimeException ("Start/end limits have different ranks");
    } // if

    // Print stats header
    // ------------------
    Format.printf ("%-14s %-9s %-9s %-10s %-10s %-10s %-10s %-10s\n",
      new Object[] {"Variable", "Count", "Valid", "Min", "Max", "Mean", 
      "Stdev", "Median"});

    // Loop over each variable
    // -----------------------
    for (int i = 0; i < vars; i++) {

      // Check for name match
      // --------------------
      String varName = reader.getName(i);
      if (match != null && !varName.matches (match))
        continue;

      // Get variable
      // ------------
      DataVariable var = null;
      try { var = reader.getVariable (i); }
      catch (Exception e) { continue; }

      // Check variable and limits rank
      // ------------------------------
      int varRank = var.getRank();
      boolean useLimits = (limitRank == varRank);

      // Calculate statistics
      // --------------------
      DataLocationConstraints lc = new DataLocationConstraints();
      if (useLimits) {
        lc.start = start;
        lc.end = end;
      } // if
      else {
        lc.dims = var.getDimensions();
      } // else
      if (Double.isNaN (sample)) {
        int[] strideArray = new int[varRank];
        Arrays.fill (strideArray, stride);
        lc.stride = strideArray;
      } // if
      else {
        lc.fraction = sample;
      } // else
      if (polygon != null && varRank == 2)
        lc.polygon = polygon;
      Statistics stats = VariableStatisticsGenerator.getInstance().generate (var, lc);

      // Print statistics
      // ----------------
      DecimalFormat fmt = new DecimalFormat ("0.######");
      int valid = stats.getValid();
      Object[] params = new Object[] {
        var.getName(),
        Integer.toString (stats.getValues()),
        Integer.toString (valid),
        (valid == 0 ? "NaN" : var.format (stats.getMin())),
        (valid == 0 ? "NaN" : var.format (stats.getMax())),
        (valid == 0 ? "NaN" : fmt.format (stats.getMean())),
        (valid == 0 ? "NaN" : fmt.format (stats.getStdev())),
        (valid == 0 ? "NaN" : fmt.format (stats.getMedian()))
       };
      Format.printf ("%-14s %-9s %-9s %-10s %-10s %-10s %-10s %-10s\n", 
        params);

    } // for

  } // printStats

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cwstats");

    info.func ("Calculates statistics for variables in an earth data file");

    info.param ("input", "Input data file name");

    info.option ("-h, --help", "Show help message");
    info.option ("-i, --region=LAT/LON/RADIUS", "Set geographic location and radius");
    info.option ("-l, --limit=STARTROW/STARTCOL/ENDROW/ENDCOL", "Set data coordinate bounds");
    info.option ("-m, --match=PATTERN", "Set variable matching regular expression");
    info.option ("-p, --polygon=FILE", "Set polygon vertex point file");
    info.option ("-s, --stride=N", "Sample every Nth value in each dimension");
    info.option ("-S, --sample=FACTOR", "Sample fraction of data, factor range [0..1]");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwstats () { }

  ////////////////////////////////////////////////////////////

} // cwstats class

////////////////////////////////////////////////////////////////////////
