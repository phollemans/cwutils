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
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;
import ucar.ma2.Array;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.units.DateUnit;

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
 *   <li>Upper-right lat/lon at (mc-1,0)</li>
 *   <li>Lower-left lat/lon at (0,mr-1)</li>
 *   <li>Lower-right lat/lon at (mc-1,mr-1)</li>
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
 *   <li> Invalid command line option. </li>
 *   <li> Invalid input file name. </li>
 *   <li> Unsupported input file format. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows an information dump of a
 * CoastWatch HDF file from the West Coast:</p>
 * <pre>
 *   phollema$ cwinfo 2002_197_1100_n16_wn.hdf
 *
 *   Contents of file 2002_197_1100_n16_wn.hdf
 *   
 *   Global information:
 *     Satellite:        noaa-16
 *     Sensor:           avhrr
 *     Date:             2002/07/16 JD 197
 *     Time:             11:00:08 UTC
 *     Pass type:        night
 *     Projection type:  mapped
 *     Map projection:   mercator
 *     Map affine:       0 -1469.95 1469.95 0 -15012623.67 6367109.52 
 *     Origin:           USDOC/NOAA/NESDIS CoastWatch
 *   
 *   Variable information:
 *     Variable       Type    Dimensions  Units          Scale     Offset    
 *     avhrr_ch3      short   1024x1024   temp_deg_c     0.01      0         
 *     avhrr_ch4      short   1024x1024   temp_deg_c     0.01      0         
 *     avhrr_ch5      short   1024x1024   temp_deg_c     0.01      0         
 *     sst            short   1024x1024   temp_deg_c     0.01      0         
 *     cloud          byte    1024x1024   -              -         -         
 *     sat_zenith     short   1024x1024   -              0.0001    0         
 *     graphics       byte    1024x1024   -              -         -         
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Mark Robinson
 * @since 3.1.0
 */
public final class cwinfo {

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  /** Name of program. */
  private static final String PROG = "cwinfo";

  /** Default date format. */
  public static final String DATE_FMT = "yyyy/MM/dd 'JD' DDD";

  /** Default time format. */
  public static final String TIME_FMT = "HH:mm:ss 'UTC'";

  /** Default date/time format. */
  public static final String DATE_TIME_FMT = "yyyy/MM/dd HH:mm:ss 'UTC'";

  /** Java primitve 1D array class types. */
  private static final Class[] JAVA_TYPES = {
    Boolean.TYPE,
    Byte.TYPE,
    Character.TYPE,
    Short.TYPE,
    Integer.TYPE,
    Long.TYPE,
    Float.TYPE,
    Double.TYPE
  };

  /** Primitive array class names. */
  private static final String[] JAVA_NAMES = {
    "boolean",
    "byte",
    "char",
    "short",
    "int",
    "long",
    "float",
    "double"
  };

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
    Option transformOpt = cmd.addBooleanOption ('t', "transform");
    Option coordOpt = cmd.addBooleanOption ('c', "coord");
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
    if (cmd.getRemainingArgs().length < NARGS) {
      System.err.println (PROG + ": At least " + NARGS + 
        " argument(s) required");
      usage ();
      System.exit (1);
    } // if
    String input = cmd.getRemainingArgs()[0];

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    boolean transform = (cmd.getOptionValue (transformOpt) != null);
    boolean coord = (cmd.getOptionValue (coordOpt) != null);

    // Setup verbose mode in factory
    // -----------------------------
    EarthDataReaderFactory.setVerbose (verbose);

    // Open file
    // ---------
    EarthDataReader reader = null;
    //    SwathProjection.setNullMode (true);
    try {
      reader = EarthDataReaderFactory.create (input);
    } // try
    catch (Exception e) {
      System.err.println (PROG + ": " + e.getMessage());      
      System.exit (2);
    } // catch

    // Print information
    // -----------------
    printInfo (reader, System.out);
    if (transform) printTransform (reader, System.out, false);
    if (coord) printCoordSystems (reader, System.out);

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints earth transform data from the specified file.  The Earth
   * transform data includes pixel resolution, total width and height,
   * and latitude and longitude data for selected locations.
   *
   * @param reader the earth data reader object to use.
   * @param stream the output stream for printing.
   * @param useEdges true to use actual edges for location values,
   * false to use center of edge pixels.
   */
  public static void printTransform (
    EarthDataReader reader,
    PrintStream stream,
    boolean useEdges
  ) {

    // Get info
    // --------
    EarthDataInfo info = reader.getInfo();
    EarthTransform trans = info.getTransform();
    int[] dims = trans.getDimensions();
    int rows = dims[0];
    int cols = dims[1];

    // Create map
    // ----------
    Map valueMap = new LinkedHashMap();

    // Add distance info
    // -----------------
    double centerRow = (rows-1)/2.0;
    double centerCol = (cols-1)/2.0;
    double pixelWidth = trans.distance (
      new DataLocation (centerRow, centerCol-0.5),
      new DataLocation (centerRow, centerCol+0.5));
    valueMap.put ("Pixel width", String.format ("%.4f km", pixelWidth));
    double pixelHeight = trans.distance (
      new DataLocation (centerRow-0.5, centerCol),
      new DataLocation (centerRow+0.5, centerCol));
    valueMap.put ("Pixel height", String.format ("%.4f km", pixelHeight));
    double width = trans.distance (
      new DataLocation (centerRow, 0),
      new DataLocation (centerRow, cols-1));
    valueMap.put ("Total width", String.format ("%.4f km", width));
    double height = trans.distance (
      new DataLocation (0, centerCol),
      new DataLocation (rows-1, centerCol));
    valueMap.put ("Total height", String.format ("%.4f km", height));

    // Add location info
    // -----------------
    EarthLocation earthLoc;
    earthLoc = trans.transform (new DataLocation (centerRow, centerCol));
    valueMap.put ("Center", earthLoc.isValid() ? earthLoc.format() : "Invalid");
    earthLoc = trans.transform (new DataLocation (0, 0));
    valueMap.put ("Upper-left", earthLoc.isValid() ? earthLoc.format() : "Invalid");
    earthLoc = trans.transform (new DataLocation (0, cols-1));
    valueMap.put ("Upper-right", earthLoc.isValid() ? earthLoc.format() : "Invalid");
    earthLoc = trans.transform (new DataLocation (rows-1, 0));
    valueMap.put ("Lower-left", earthLoc.isValid() ? earthLoc.format() : "Invalid");
    earthLoc = trans.transform (new DataLocation (rows-1, cols-1));
    valueMap.put ("Lower-right", earthLoc.isValid() ? earthLoc.format() : "Invalid");

    // Print data
    // ----------
    stream.format ("Earth location information:\n");
    for (Iterator iter = valueMap.keySet().iterator(); iter.hasNext(); ) {
      String key = (String) iter.next();
      String value = (String) valueMap.get (key);
      stream.format ("  %-20s %s\n", key + ":", value);
    } // for
    stream.println();

  } // printTransform

  ////////////////////////////////////////////////////////////

  /** Prints a single coordinate system axes. */
  private static void printCoordSystem (
    EarthDataReader reader, 
    CoordinateSystem system,
    PrintStream stream
  ) throws IOException {                                        

    // Print system header
    // -------------------
    List<CoordinateAxis> axes = system.getCoordinateAxes();
    int rank = axes.size();
    stream.print ("  ");
    for (int i = 0; i < rank; i++)
      stream.print (axes.get (i).getAxisType() + (i != rank-1 ? " " : ""));
    stream.print (" (");
    for (int i = 0; i < rank; i++)
      stream.print (axes.get (i).getSize() + (i != rank-1 ? "x" : ""));
    stream.println (")");

    // Print each axis
    // ---------------
    for (CoordinateAxis axis : axes) {

      // Print basic axis info
      // ---------------------
      AxisType axisType = axis.getAxisType();
      String units = axis.getUnitsString();
      int size = (int) axis.getSize();
      stream.format ("    %s (%s):\n", axisType, units);

      // Create date unit if applicable
      // ------------------------------
      DateUnit dateUnit = null;
      if (axisType == AxisType.Time) {
        try { dateUnit = new DateUnit (units); }
        catch (Exception e) { }
      } // if

      // Detect geographic axis
      // ----------------------
      boolean isGeo = (
        axisType == AxisType.GeoX || 
        axisType == AxisType.GeoY ||
        axisType == AxisType.Lat ||
        axisType == AxisType.Lon ||
        axisType == AxisType.RadialAzimuth ||
        axisType == AxisType.RadialDistance ||
        axisType == AxisType.RadialElevation
      );

      // Print coordinates for simple axis
      // ---------------------------------
      if (axis.getRank() == 1) {
        Array data = axis.read();
        if (isGeo) {
          for (int i : new int[] {0, size-1})
            stream.format ("    %-5d %s\n", i, data.getObject (i));
        } // if
        else {
          int coordIndex = 0;
          while (data.hasNext()) {
            Object value = data.next();
            if (dateUnit != null) {
              Date date = dateUnit.makeDate (((Number) value).doubleValue());
              value = DateFormatter.formatDate (date, DATE_TIME_FMT);
            } // if
            stream.format ("    %-5d %s\n", coordIndex, value);
            coordIndex++;
          } // while
        } // else
      } // if

    } // for

    // Print variables
    // ---------------
    List<String> varList = reader.getVariablesForSystem (system);
    stream.println ("    Variables:");
    for (String var : varList) 
      stream.println ("    " + var);

  } // printCoordSystem

  ////////////////////////////////////////////////////////////

  /**
   * Prints Common Data Model (CDM) style coordinate system
   * information including level and time index information.
   *
   * @param reader the earth data reader object to use.
   * @param stream the output stream for printing.
   */
  public static void printCoordSystems (
    EarthDataReader reader,
    PrintStream stream
  ) throws IOException {

    // Print header
    // ------------
    List<CoordinateSystem> systemList = reader.getCoordinateSystems();
    stream.println ("Coordinate system information:\n");

    // Print each system
    // -----------------
    for (CoordinateSystem system : systemList) {
      printCoordSystem (reader, system, stream);
      stream.println();
    } // for

  } // printCoordSystems

  ////////////////////////////////////////////////////////////

  /**
   * Prints the information from the specified file.
   *
   * @param reader the earth data reader object to use.
   * @param stream the output stream for printing.
   */
  public static void printInfo (
    EarthDataReader reader,
    PrintStream stream
  ) {

    try {
      stream.println ("Contents of file " + reader.getSource());
      stream.println();
      printGlobal (reader, stream);
      printVariables (reader, stream);
    } catch (IOException e) {
      e.printStackTrace();
    } // catch

  } // printInfo

  ////////////////////////////////////////////////////////////

  /**
   * Prints the global file information.
   *
   * @param reader the earth data reader object to use.
   * @param stream the output stream for printing.
   *
   * @throws IOException if an error occurred printing to the stream.
   */
  public static void printGlobal (
    EarthDataReader reader,
    PrintStream stream
  ) throws IOException {

    // Create map of attributes to values
    // ----------------------------------
    Map valueMap = new LinkedHashMap();

    // Add data source info
    // --------------------
    EarthDataInfo info = reader.getInfo();
    if (info instanceof SatelliteDataInfo) {
      SatelliteDataInfo satInfo = (SatelliteDataInfo) info;
      valueMap.put ("Satellite", 
        MetadataServices.format (satInfo.getSatellite(), ", "));
      valueMap.put ("Sensor", 
        MetadataServices.format (satInfo.getSensor(), ", "));
    } // if
    else {
      valueMap.put ("Data source", 
        MetadataServices.format (info.getSource(), ", "));
    } // else

    // Add single time info
    // --------------------
    if (info.isInstantaneous()) {
      Date startDate = info.getStartDate();
      valueMap.put ("Date", DateFormatter.formatDate (startDate, DATE_FMT));
      valueMap.put ("Time", DateFormatter.formatDate (startDate, TIME_FMT));
      valueMap.put ("Scene time", reader.getSceneTime());
    } // if

    // Add time range info
    // -------------------
    else {
      Date startDate = info.getStartDate();
      Date endDate = info.getEndDate();
      String startDateString = DateFormatter.formatDate (startDate, DATE_FMT);
      String endDateString = DateFormatter.formatDate (endDate, DATE_FMT);
      String startTimeString = DateFormatter.formatDate (startDate, TIME_FMT);
      String endTimeString = DateFormatter.formatDate (endDate, TIME_FMT);
      if (startDateString.equals (endDateString)) {
        valueMap.put ("Date", startDateString);
        valueMap.put ("Start time", startTimeString);
        valueMap.put ("End time", endTimeString);
      } // if
      else {
        valueMap.put ("Start date", startDateString);
        valueMap.put ("Start time", startTimeString);
        valueMap.put ("End date", endDateString);
        valueMap.put ("End time", endTimeString);
      } // else
    } // else

    // Add earth transform info
    // ------------------------
    EarthTransform trans = info.getTransform();
    valueMap.put ("Projection type", (trans == null ? "unknown" : 
      trans.describe()));

    // Add map projection info
    // -----------------------    
    if (trans instanceof MapProjection) {
      MapProjection map = (MapProjection) trans;
      valueMap.put ("Map projection", map.getSystemName());
      String affineVals = "";      
      DecimalFormat form = new DecimalFormat ("0.##");
      double[] matrix = new double[6];
      map.getAffine().getMatrix (matrix);
      for (int i = 0; i < 6; i++)
        affineVals += form.format (matrix[i]) + " ";
      valueMap.put ("Map affine", affineVals);
      valueMap.put ("Spheroid", map.getSpheroidName());
    } // if

    // Add other info
    // --------------
    valueMap.put ("Origin",
      MetadataServices.format (info.getOrigin(), ", "));
    valueMap.put ("Format", reader.getDataFormat());
    valueMap.put ("Reader ident", reader.getClass().getName());

    // Print attribute and value lists
    // -------------------------------
    stream.format ("Global information:\n");
    for (Iterator iter = valueMap.keySet().iterator(); iter.hasNext(); ) {
      String key = (String) iter.next();
      String value = (String) valueMap.get (key);
      stream.format ("  %-20s %s\n", key + ":", value);
    } // for
    stream.println();

  } // printGlobal

  ////////////////////////////////////////////////////////////

  /**
   * Gets the variable data type name.
   *
   * @param var the data variable.
   * 
   * @return a user-friendly type name.
   */
  private static String getType (
    DataVariable var
  ) {

    // Find appropriate type name
    // --------------------------
    Class dataClass = var.getDataClass();
    String typeName = null;
    for (int i = 0; i < JAVA_TYPES.length && typeName == null; i++) {
      if (JAVA_TYPES[i].equals (dataClass))
        typeName = JAVA_NAMES[i];
    } // for

    // Check for unknown type
    // ----------------------
    if (typeName == null) return ("unknown");

    // Add unsigned prefix
    // -------------------
    if (var.getUnsigned()) typeName = "u" + typeName;
    return (typeName);

  } // getType

  ////////////////////////////////////////////////////////////

  /**
   * Prints the variable information.
   *
   * @param reader the earth data reader object to use.
   * @param stream the output stream for printing.
   *
   * @throws IOException if an error occurred printing to the stream.
   */
  public static void printVariables (
    EarthDataReader reader,
    PrintStream stream
  ) throws IOException {

    // Check variable count
    // --------------------
    int vars = reader.getVariables();
    if (vars == 0)
      return;
    
    // Get variable info
    // -----------------
    int maxNameLength = 14;
    int maxUnitsLength = 14;
    List<String[]> varInfoList = new ArrayList<String[]>();
    for (int i = 0; i < vars; i++) {

      // Get preview
      // -----------
      DataVariable var = null;
      try { var = reader.getPreview (i); }
      catch (Exception e) {
        e.printStackTrace();
        continue;
      } // catch
      
      // Create new info array
      // ---------------------
      String[] infoArray = new String[6];
      int index = 0;
      infoArray[index++] = var.getName();
      infoArray[index++] = getType (var);
      varInfoList.add (infoArray);
      
      // Get dimensions
      // --------------
      int[] dims = var.getDimensions();
      String dimValues = "";
      for (int dim = 0; dim < dims.length; dim++)
        dimValues += dims[dim] + (dim < dims.length-1 ? "x" : "");
      infoArray[index++] = dimValues;

      // Get units
      // ---------
      String units = var.getUnits ();
      if (units.equals ("")) units = "-";
      infoArray[index++] = units;

      // Get scale, offset
      // -----------------
      double[] scaling = var.getScaling();
      String scale, offset;
      if (scaling == null) {
        scale = "-";
        offset = "-";
      } // if
      else {
        DecimalFormat fmt = new DecimalFormat ("0.######");
        scale = fmt.format (scaling[0]);
        offset = fmt.format (scaling[1]);
      } // else
      infoArray[index++] = scale;
      infoArray[index++] = offset;

      // Save maximum lengths
      // --------------------
      int nameLength = var.getName().length();
      if (nameLength > maxNameLength) maxNameLength = nameLength;
      int unitsLength = units.length();
      if (unitsLength > maxUnitsLength) maxUnitsLength = unitsLength;

    } // for

    // Print variable info
    // -------------------
    stream.format ("Variable information:\n");
    String varFormatLine = "  %-" + maxNameLength + "s %-7s %-11s %-" + maxUnitsLength + "s %-9s %-9s\n";
    stream.format (varFormatLine, "Variable", "Type", "Dimensions", "Units", "Scale", "Offset");
    for (String[] infoArray : varInfoList) {
      stream.format (varFormatLine, infoArray[0], infoArray[1], infoArray[2],
        infoArray[3], infoArray[4], infoArray[5]);
    } // for
    stream.println();

  } // printVariables

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwinfo [OPTIONS] input\n" +
"Dumps earth data information in a display-friendly format.\n" +
"\n" +
"Main parameters:\n" +
"  input                      The input data file name.\n" +
"\n" +
"Options:\n" +
"  -h, --help                 Show this help message.\n" +
"  -t, --transform            Print earth transform information.\n" +
"  -c, --coord                Print CDM coordinate system information.\n" +
"  -v, --verbose              Print verbose messages.\n" +
"  --version                  Show version information.\n"
    );
    
  } // usage

  ////////////////////////////////////////////////////////////

  private cwinfo () { }

  ////////////////////////////////////////////////////////////

} // cwinfo class

////////////////////////////////////////////////////////////////////////
