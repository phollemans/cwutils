////////////////////////////////////////////////////////////////////////
/*

     File: cwangles.java
   Author: Peter Hollemans
     Date: 2002/12/23

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
import java.text.NumberFormat;
import noaa.coastwatch.io.CWHDFReader;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.HDFCachedGrid;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SolarZenith;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * <p>The angles tool computes earth location and solar angles for an
 * earth data file.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 *
 * <p>
 *   <!-- START NAME -->
 *   cwangles - computes earth location and solar angles.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>cwangles [OPTIONS] input</p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -f, --float <br>
 * -d, --double <br>
 * -h, --help <br>
 * -l, --location <br>
 * -s, --scale=FACTOR/OFFSET <br> 
 * -u, --units=TYPE <br>
 * -v, --verbose <br>
 * -z, --sunzenith <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p>The angles tool computes earth location and solar angles for an
 * earth data file.  Angles may be computed as scaled integer or
 * floating point values, and in radians, degrees, or cosine.  The
 * earth location values computed refer to the center of each
 * pixel.</p>
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
 *   <dt>-f, --float</dt>
 *   <dd>Specifies that data should be stored as 32-bit floating point
 *   values with no scaling.  The default is to store as 16-bit signed
 *   integers with a scaling factor of 0.01.</dd>
 *
 *   <dt>-d, --double</dt>
 *   <dd>Specifies that data should be stored as 64-bit floating point
 *   values with no scaling.  The default is to store as 16-bit signed
 *   integers with a scaling factor of 0.01.</dd>
 *   
 *   <dt>-h, --help</dt>
 *   <dd>Prints a brief help message.</dd>
 *
 *   <dt>-l, --location</dt>
 *   <dd>Specifies that earth location latitude and longitude data
 *   should be computed.</dd>
 *
 *   <dt>-s, --scale=FACTOR/OFFSET</dt>
 *   <dd>The data scale factor and offset.  Data values are scaled to
 *   integers using the factor and offset under the equation:<br>
 *   <pre>
 *     integer = value/factor + offset
 *   </pre>
 *   The default factor is 0.01 and offset is 0.  This option is
 *   ignored if <b>--float</b> or <b>--double</b> is used.</dd>
 *
 *   <dt>-v, --verbose</dt>
 *   <dd>Turns verbose mode on.  The current status of data
 *   computation is printed periodically.  The default is to run
 *   quietly.</dd>
 *
 *   <dt>-u, --units=TYPE</dt>
 *   <dd>The units type.  Valid units are 'deg' for degrees, 'rad'
 *   for radians, or 'cos' for cosine of the angle.  The default is to
 *   compute angles in degrees.</dd>
 *
 *   <dt>-z, --sunzenith</dt>
 *   <dd>Specifies that solar zenith angle data should be
 *   computed.</dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 *
 * <p>0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option. </li>
 *   <li> Invalid input file name. </li>
 *   <li> No angle computations specified. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <p>
 * The following shows the computation of latitude and longitude data
 * for a CoastWatch HDF product file:</p>
 * <pre> 
 *   phollema$ cwangles --float --location 2002_361_1049_n16_ax.hdf
 *
 *   cwangles: Reading input 2002_361_1049_n16_ax.hdf
 *   cwangles: Creating latitude variable
 *   cwangles: Creating longitude variable
 *   cwangles: Calculating angles
 *   cwangles: Computing row 0
 *   cwangles: Computing row 100
 *   cwangles: Computing row 200
 *   cwangles: Computing row 300
 *   cwangles: Computing row 400
 *   cwangles: Computing row 500
 *   cwangles: Computing row 600
 *   cwangles: Computing row 700
 *   cwangles: Computing row 800
 *   cwangles: Computing row 900
 *   cwangles: Computing row 1000
 * </pre>
 * <p>Another example below shows the computation of solar zenith angle,
 * stored as the cosine and scaled to integer data by 0.0001:</p>
 * <pre> 
 *   phollema$ cwangles -v --sunzenith --units cos --scale 0.0001/0 test_angles.hdf
 *
 *   cwangles: Reading input test_angles.hdf
 *   cwangles: Creating sun_zenith variable
 *   cwangles: Calculating angles
 *   cwangles: Computing row 0
 *   cwangles: Computing row 100
 *   cwangles: Computing row 200
 *   cwangles: Computing row 300
 *   cwangles: Computing row 400
 *   cwangles: Computing row 500
 *   cwangles: Computing row 600
 *   cwangles: Computing row 700
 *   cwangles: Computing row 800
 *   cwangles: Computing row 900
 *   cwangles: Computing row 1000
 *   cwangles: Computing row 1100
 *   cwangles: Computing row 1200
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public final class cwangles {

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  /** Name of program. */
  private static final String PROG = "cwangles";

  /** The units constants. */
  private static final int DEGREES = 0;
  private static final int RADIANS = 1;
  private static final int COSINE = 2;

  ////////////////////////////////////////////////////////////

  /** Converts a value in degrees to the specified units. */
  private static double convertValue (
    double deg,
    int units
  ) {

    switch (units) {
    case DEGREES: return (deg);
    case RADIANS: return (Math.toRadians (deg));
    case COSINE: return (Math.cos (Math.toRadians (deg)));
    default: return (deg);
    } // switch

  } // convertValue

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) throws IOException {

    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option floatOpt = cmd.addBooleanOption ('f', "float");
    Option doubleOpt = cmd.addBooleanOption ('d', "double");
    Option locationOpt = cmd.addBooleanOption ('l', "location");
    Option sunzenithOpt = cmd.addBooleanOption ('z', "sunzenith");
    Option unitsOpt = cmd.addStringOption ('u', "units");
    Option scaleOpt = cmd.addStringOption ('s', "scale");
    Option versionOpt = cmd.addBooleanOption ("version");
    try { cmd.parse (argv); }
    catch (OptionException e) {
      System.err.println (PROG + ": " + e.getMessage());
      usage();
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
      usage();
      System.exit (1);
    } // if
    String input = remain[0];

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    boolean floatData = (cmd.getOptionValue (floatOpt) != null);
    boolean doubleData = (cmd.getOptionValue (doubleOpt) != null);
    boolean location = (cmd.getOptionValue (locationOpt) != null);
    boolean sunzenith = (cmd.getOptionValue (sunzenithOpt) != null);
    String unitsObj = (String) cmd.getOptionValue (unitsOpt);
    if (unitsObj == null) unitsObj = "deg";
    String scale = (String) cmd.getOptionValue (scaleOpt);
    if (scale == null) scale = "0.01,0";

    // Check angles specified
    // ----------------------
    if (!location && !sunzenith) {
      System.err.println (PROG + ": Must specify angles to compute");
      usage();
      System.exit (1);
    } // if

    // Check scaling
    // -------------
    String[] scaleArray = scale.split (ToolServices.SPLIT_REGEX);
    if (scaleArray != null && scaleArray.length != 2) {
      System.err.println (PROG + ": Invalid scale '" + scale + "'");
      System.exit (1);
    } // if

    // Check units
    // -----------
    int units = -1;
    if (unitsObj.equals ("deg")) units = DEGREES;
    else if (unitsObj.equals ("rad")) units = RADIANS;
    else if (unitsObj.equals ("cos")) units = COSINE;
    else { 
      System.err.println (PROG + ": Invalid units '" + unitsObj + "'");
      System.exit (1);
    } // else

    CWHDFWriter writer = null;
    try {

      // Get grid specifications
      // -----------------------
      if (verbose) System.out.println (PROG + ": Reading input " + input);
      CWHDFReader reader = new CWHDFReader (input);
      EarthDataInfo info = reader.getInfo();
      EarthTransform trans = info.getTransform();
      SolarZenith sz = new SolarZenith (info.getDate());
      int[] dims = trans.getDimensions();
      int rows = dims[0];
      int cols = dims[1];
      reader.close();

      // Instantiate variables according to number format
      // ------------------------------------------------
      double[] scaling;
      Number missing;
      int digits;
      Object data;
      
      if (floatData) {
        scaling = null;
        missing = Float.valueOf (Float.NaN);
        digits = 6;
        data = new float[] {};
      } // if
      else if (doubleData) {
        scaling = null;
        missing = Double.valueOf (Double.NaN);
        digits = 10;
        data = new double[] {};
      } // else if
      else {
        scaling = new double[] {
          Double.parseDouble (scaleArray[0]),
          Double.parseDouble (scaleArray[1])
        };
        missing = Short.valueOf (Short.MIN_VALUE);
        digits = DataVariable.getDecimals (
          Double.toString (Short.MAX_VALUE*scaling[0]));
        data = new short[] {};
      } // if
      
      // Create decimal format
      // ---------------------
      NumberFormat format = NumberFormat.getInstance();
      format.setMaximumFractionDigits (digits);

      // Create units string
      // -------------------
      String unitsStr = (units == DEGREES ? "degrees" : units == RADIANS ?
        "radians" : "cosine");

      // Open writer
      // -----------
      writer = new CWHDFWriter (input);

      // Create location grids
      // ---------------------
      Grid latGrid = null;
      Grid lonGrid = null;
      if (location) {
        if (verbose) 
          System.out.println (PROG + ": Creating latitude variable");
        latGrid = new HDFCachedGrid (
          new Grid ("latitude", "Geographic latitude",
          unitsStr, rows, cols, data, format, scaling, missing), writer);
        if (verbose) 
          System.out.println (PROG + ": Creating longitude variable");
        lonGrid = new HDFCachedGrid (
          new Grid ("longitude", "Geographic longitude",
          unitsStr, rows, cols, data, format, scaling, missing), writer);
      } // if

      // Create solar zenith grid
      // ------------------------
      Grid sunzenithGrid = null;
      if (sunzenith) {
        if (verbose) 
          System.out.println (PROG + ": Creating sun_zenith variable");
        sunzenithGrid = new HDFCachedGrid (
          new Grid ("sun_zenith", "Solar zenith angle",
          unitsStr, rows, cols, data, format, scaling, missing), writer);
      } // if

      // Calculate grid values
      // ---------------------
      if (verbose) System.out.println (PROG + ": Calculating angles");
      for (int i = 0; i < rows; i++) {
        if (verbose && i%100 == 0)
          System.out.println (PROG + ": Computing row " + i);
        for (int j = 0; j < cols; j++) {
          DataLocation dataLoc = new DataLocation (i, j);
          EarthLocation earthLoc = trans.transform (dataLoc);
          if (location) {
            latGrid.setValue (dataLoc, convertValue (earthLoc.lat, units));
            lonGrid.setValue (dataLoc, convertValue (earthLoc.lon, units));
          } // if
          if (sunzenith) {
            sunzenithGrid.setValue (dataLoc, 
              convertValue (sz.getSolarZenith (earthLoc), units));
          } // if
        } // for
      } // for

      // Close writer
      // ------------
      writer.close();
 
    } // try
    catch (Exception e) {
      e.printStackTrace();
      writer.close();
      System.exit (2);
    } // catch

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwangles [OPTIONS] input\n" +
"Computes earth location and solar angles for an earth data file.\n" +
"\n" +
"Main parameters:\n" +
"  input                      The input data file name.\n" +
"\n" +
"Options:\n" +
"  -f, --float                Write angles as 32-bit floating-point values.\n"+
"  -d, --double               Write angles as 64-bit floating-point values.\n"+
"  -h, --help                 Show this help message.\n" +
"  -l, --location             Compute earth latitude/longitude values.\n" +
"  -s, --scale=FACTOR/OFFSET  Set scale factor and offset for integer data\n" +
"                              storage.\n" + 
"  -u, --units=TYPE           Set angle units.  TYPE may be 'deg', 'rad',\n" +
"                              or 'cos'.\n" +
"  -v, --verbose              Print verbose messages.\n" +
"  -z, --sunzenith            Compute solar zenith angle values.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

  private cwangles () { }

} // cwangles class

////////////////////////////////////////////////////////////////////////
