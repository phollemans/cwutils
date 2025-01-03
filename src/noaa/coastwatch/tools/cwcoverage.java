////////////////////////////////////////////////////////////////////////
/*

     File: cwcoverage.java
   Author: Peter Hollemans
     Date: 2003/11/15

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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.render.ColorLookup;
import noaa.coastwatch.render.EarthContextElement;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.feature.LineFeature;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.SpheroidConstants;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The coverage tool creates an earth data coverage map.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p> 
 *   <!-- START NAME -->          
 *   cwcoverage - creates an earth data coverage map.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p>
 *   cwcoverage [OPTIONS] input1 [input2 ...] output <br>
 *   cwcoverage [OPTIONS] output <br>
 * </p>
 *
 * <h3>General options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h3>Output content and format options:</h3>
 *
 * <p>
 * -a, --noantialias <br>
 * -b, --background=COLOR <br>
 * -c, --center=LATITUDE/LONGITUDE <br>
 * -f, --foreground=COLOR <br>
 * -s, --size=PIXELS <br>
 * </p>
 *
 * <h3>Dataset boundary options:</h3>
 *
 * <p>
 * -H, --highlight=PATTERN <br>
 * -l, --labels=LABEL1/LABEL2/... <br>
 * -m, --map=OUTPUT <br>
 * -x, --box=COLOR <br>
 * </p>
 *
 * <h3>Ground station options:</h3>
 *
 * <p>
 * -C, --stationcolor=COLOR <br>
 * -e, --elevation=DEGREES <br>
 * -E, --height=KILOMETERS <br>
 * -L, --stationlabels=LABEL1/LABEL2/... <br>
 * -S, --stations=LAT1/LON1/LAT2/LON2/... <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p>
 * The coverage tool creates an earth data coverage map by accessing a
 * number of user-specified earth data sets and tracing the boundaries
 * onto an orthographic map projection.  The map is output as a PNG
 * graphics file.  Approximate satellite ground station coverage
 * boundaries may also be added to the map.
 * </p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt>input1 [input2 ...]</dt>
 *   <dd>The input data files name(s).  If none are specified, the
 *   output PNG image contains only map graphics and possibly ground
 *   station circles (see the <b>--stations</b> option).</dd>
 *
 *   <dt>output</dt>
 *   <dd>The output PNG file name.</dd>
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
 *   <dt> -v, --verbose </dt>
 *   <dd> Turns verbose mode on.  The current status of data
 *   rendering is printed periodically.  The default is to run
 *   quietly. </dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h3>Output content and format options:</h3>
 *
 * <dl>
 *
 *   <dt> -a, --noantialias </dt>
 *   <dd> Turns off line antialiasing.  By default, the edges of lines
 *   are smoothed using shades of the drawing color.  The use of this
 *   option can significantly reduce the size of the output file,
 *   while sacrificing visual quality. </dd>
 *
 *   <dt> -b, --background=COLOR </dt> 
 *   <dd> The map background color.  The color is specified by name or
 *   hexadecimal value.  The default is black. </dd>
 *
 *   <dt> -c, --center=LATITUDE/LONGITUDE </dt> 
 *   <dd> The map center location.  By default, the center location is
 *   determined from the data sets. </dd>
 *
 *   <dt> -f, --foreground=COLOR </dt> 
 *   <dd> The map foreground color.  The color is specified by name or
 *   hexadecimal value.  The default is a gray of 63% RGB
 *   intensity. </dd>
 *
 *   <dt> -s, --size=PIXELS </dt>
 *   <dd> The coverage map size in pixels.  By default, the map size
 *   is 512 pixels. </dd>
 *
 * </dl>
 *
 * <h3>Dataset boundary options:</h3>
 *
 * <dl>
 *
 *   <dt> -H, --highlight=PATTERN </dt>
 *   <dd> The highlighted input file matching pattern.  By default,
 *   all input files are highlighted with the box boundary fill and
 *   color.  With this option, only input files whose names match the
 *   pattern are highlighted.  The remaining non-matching input file
 *   boundaries are drawn using the foreground color. </dd>
 *
 *   <dt> -l, --labels=LABEL1/LABEL2/... </dt>
 *   <dd> The labels for each input file.  Labels are drawn at the
 *   center point of each dataset boundary.  By default, no labels are
 *   drawn. </dd>
 *
 *   <dt> -m, --map=OUTPUT </dt>
 *   <dd> The output file for HTML image map output.  The output file
 *   contains an HTML fragment with an image map and area polygons for 
 *   each input file, similar to the following:
 *   <pre>
 *  &lt;map name="coverage_map"&gt;
 *    &lt;area shape="poly" id="region_0" coords="111,64,170,63,179,132,108,134,111,64" /&gt;
 *    &lt;area shape="poly" id="region_1" coords="75,106,132,109,133,179,66,174,75,106" /&gt;
 *    &lt;area shape="poly" id="region_2" coords="121,124,183,121,192,188,119,191,121,124" /&gt;
 *  &lt;/map&gt;
 *   </pre>
 *   The map may be used in an HTML document in conjunction with the
 *   output PNG coverage image to provide users with a clickable
 *   interface for area of interest selection. </dd>
 *
 *   <dt> -x, --box=COLOR </dt> 
 *   <dd> The box boundary and fill color.  The color is specified by
 *   name or hexadecimal value.  The default is a color close to
 *   cyan. </dd>
 *
 * </dl>
 *
 * <h3>Ground station options:</h3>
 *
 * <dl>
 *
 *   <dt> -C, --stationcolor=COLOR </dt>
 *   <dd> The ground station circle color.  This option is only used
 *   in conjunction with the <b>--stations</b> option.  By default,
 *   the box color is used (see the <b>--box</b> option).</dd>
 *  
 *   <dt> -e, --elevation=DEGREES </dt>
 *   <dd> The minimum elevation of the ground station antenna above
 *   the horizon in degrees.  This option is only used in conjunction
 *   with the <b>--stations</b> option.  By default, the antenna
 *   elevation is set to 5 degrees, which approximates a NOAA HRPT
 *   tracking station with a 1.7 m diameter dish.</dd>
 *
 *   <dt> -E, --height=KILOMETERS </dt>
 *   <dd> The orbital height of the theoretical satellite above the
 *   earth surface in kilometers.  This option is only used in
 *   conjunction with the <b>--stations</b> option.  By default, the
 *   satellite orbital height is set to 846.5 km which approximates a
 *   NOAA polar orbiter.</dd>
 *
 *   <dt> -L, --stationlabels=LABEL1/LABEL2/... </dt>
 *   <dd> The ground station labels.  This option is only used in
 *   conjunction with the <b>--stations</b> option.  When specified,
 *   each ground station location is labelled with its corresponding
 *   label.  By default, no labels are drawn.</dd>
 *
 *   <dt> -S, --stations=LAT1/LON1/LAT2/LON2/... </dt>
 *   <dd> A list of ground station locations.  For each ground
 *   station, a circle is drawn on the Earth, centered at the ground
 *   station.  The circle shows the approximate area that a
 *   theoretical satellite can view from orbit while in sight of the
 *   station, ie: the ground station's real-time coverage area.  See
 *   the <b>--height</b> and <b>--elevation</b> options to control the
 *   orbital parameters of the theoretical satellite.  Note that the
 *   swath width of the satellite sensor is not taken into account in
 *   drawing the circle, so the area should be used as a conservative
 *   estimate of satellite coverage.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Unrecognized color name </li>
 *   <li> Invalid map center or station location </li>
 *   <li> Mismatch between label and file or station count </li>
 * </ul>
 *
 * <h2>Examples</h2>

 * <p> As an example, the following command shows the creation of a
 * coverage plot of the ER and SR CoastWatch regions covering the US
 * East coast:</p>
 * <pre>
 *   phollema$ cwcoverage -v --labels ER/SR 2004_155_1147_n15_er.hdf 
 *     2004_155_1147_n15_sr.hdf east_coast.png
 * 
 *   cwcoverage: Reading input 2004_155_1147_n15_er.hdf
 *   cwcoverage: Reading input 2004_155_1147_n15_sr.hdf
 *   cwcoverage: Writing east_coast.png
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public class cwcoverage {

  private static final String PROG = cwcoverage.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  /** The default plot background. */
  private static final Color BACK = Color.BLACK;

  /** The default plot foreground. */
  private static final Color FORE = new Color (160, 160, 160);

  /** The default box color. */
  private static final Color BOX_COLOR = new Color (0, 210, 236);

  /** The default map size in pixels. */
  private static final int SIZE = 512;

  /** The default ground station elevation in degrees. */
  private static final double ELEVATION = 5.0;

  /** The default satellite orbital height in kilometers. */
  private static final double HEIGHT = 846.5;

  /** The number of line segments to use for station circles. */
  private static final int STATION_SEGMENTS = 60;

  /////////////////////////////////////////////////////////////////////

  /**
   * Calculates the angle of visibility for a satellite capture station.
   * The visibility angle is the angle between a line from the center of
   * the earth to the ground station and a line from the center of the
   * earth to the satellite.
   *
   * @param radius the radius of the earth in kilometers.
   * @param height the height of the satellite above the surface in kilometers.
   * @param elev the elevation for the satellite receiver above the horizon
   * in degrees.
   *
   * @return the visibility angle in degrees.
   */
  public static double getVisibility (
    double radius,
    double height,
    double elev
  ) {
    
    return (Math.toDegrees (Math.PI/2 - Math.toRadians (elev) -
      Math.asin (radius*Math.sin(Math.toRadians (elev) + Math.PI/2) /
      (radius + height))));

  } // getVisibility

  ////////////////////////////////////////////////////////////

  /**
   * Creates a circle of earth locations centered at the
   * specified location.
   *
   * @param center the center location.
   * @param radius the circle radius in degrees.  The circle radius is
   * essentially the great circle distance between the center of the
   * circle and the radial points.
   * @param segments the number of segments around the circle.
   *
   * @return an earth vector specifying the circle points.
   */
  public static LineFeature getCircle (
    EarthLocation center,
    double radius,
    int segments
  ) {
                                                                               
    LineFeature circle = new LineFeature();
    double a = Math.toRadians (90 - center.lat);
    double c = Math.toRadians (radius);
    for (int i = 0; i <= segments; i++) {
      double B = Math.toRadians ((i*(360.0/segments)));
      double b = Math.acos (Math.cos(a)*Math.cos(c) +
        Math.sin(a)*Math.sin(c)*Math.cos(B));
      double arg = (Math.cos(c) - Math.cos(a)*Math.cos(b)) /
        (Math.sin(a)*Math.sin(b));
      if (arg > 1) arg = 1;
      else if (arg < -1) arg = -1;
      double C = Math.acos (arg);
      double lat = 90 - Math.toDegrees (b);
      double lon = center.lon + Math.toDegrees (C) * (B < Math.PI ? 1 : -1);
      circle.add (new EarthLocation (lat, lon));
    } /* for */
                                                                               
    return (circle);
                                                                               
  } // getCircle

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
    Option sizeOpt = cmd.addIntegerOption ('s', "size");
    Option boxOpt = cmd.addStringOption ('x', "box");
    Option foregroundOpt = cmd.addStringOption ('f', "foreground");
    Option backgroundOpt = cmd.addStringOption ('b', "background");
    Option centerOpt = cmd.addStringOption ('c', "center");
    Option noantialiasOpt = cmd.addBooleanOption ('a', "noantialias");
    Option highlightOpt = cmd.addStringOption ('H', "highlight");
    Option labelsOpt = cmd.addStringOption ('l', "labels");
    Option mapOpt = cmd.addStringOption ('m', "map");
    Option stationsOpt = cmd.addStringOption ('S', "stations");
    Option heightOpt = cmd.addDoubleOption ('E', "height");
    Option elevationOpt = cmd.addDoubleOption ('e', "elevation");
    Option stationcolorOpt = cmd.addStringOption ('C', "stationcolor");
    Option stationlabelsOpt = cmd.addStringOption ('L', "stationlabels");
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
    String output = remain[remain.length-1];
    String[] input = new String [remain.length-1];
    System.arraycopy (remain, 0, input, 0, input.length);

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose) VERBOSE.setLevel (Level.INFO);
    Integer sizeObj = (Integer) cmd.getOptionValue (sizeOpt);
    int size = (sizeObj == null? SIZE : sizeObj.intValue());
    String box = (String) cmd.getOptionValue (boxOpt);
    String foreground = (String) cmd.getOptionValue (foregroundOpt);
    String background = (String) cmd.getOptionValue (backgroundOpt);
    String center = (String) cmd.getOptionValue (centerOpt);
    boolean noantialias = (cmd.getOptionValue (noantialiasOpt) != null);
    String highlight = (String) cmd.getOptionValue (highlightOpt);
    String labels = (String) cmd.getOptionValue (labelsOpt);
    String map = (String) cmd.getOptionValue (mapOpt);
    String stations = (String) cmd.getOptionValue (stationsOpt);
    Double heightObj = (Double) cmd.getOptionValue (heightOpt);
    double height = (heightObj == null ? HEIGHT : 
      heightObj.doubleValue()); 
    Double elevationObj = (Double) cmd.getOptionValue (elevationOpt);
    double elevation = (elevationObj == null ? ELEVATION : 
      elevationObj.doubleValue()); 
    String stationColorName = (String) cmd.getOptionValue (stationcolorOpt);
    String stationlabels = (String) cmd.getOptionValue (stationlabelsOpt);

    // Create context map
    // ------------------
    EarthContextElement element = new EarthContextElement (new EarthLocation (0,0));
    element.setGrid (true);
    element.setEdge (true);
    element.setPreferredSize (new Dimension (size, size));
    element.setPosition (new Point (0,0));
    element.setLabelFont (new Font (null, Font.BOLD, 18));

    // Calculate satellite visibility angle
    // ------------------------------------
    double visAngle = getVisibility (SpheroidConstants.STD_RADIUS, height, elevation); 

    // Create earth area for context map
    // ---------------------------------
    EarthArea area = new EarthArea();

    try {

      // Lookup colors
      // -------------
      ColorLookup lookup = new ColorLookup();
      Color boxColor = (box == null ? BOX_COLOR : lookup.convert (box));
      Color foregroundColor = (foreground == null ? FORE : lookup.convert (foreground));
      Color backgroundColor = (background == null ? BACK : lookup.convert (background));
      Color stationColor = (stationColorName == null ? boxColor : lookup.convert (stationColorName));

      // Get region labels
      // -----------------
      String[] labelsArray = null;
      if (labels != null) {
        labelsArray = labels.split (ToolServices.SPLIT_REGEX);
        if (labelsArray.length != input.length) {
          LOGGER.severe ("Number of labels does not match number of input files");
          ToolServices.exitWithCode (2);
          return;
        } // if
      } // if
      
      // Get station coordinates
      // -----------------------
      List stationList = new ArrayList();
      if (stations != null) {
        String[] stationsArray = stations.split (ToolServices.SPLIT_REGEX);
        if (stationsArray.length % 2 != 0) {
          LOGGER.severe ("Station location list length must be a multiple of 2");
          ToolServices.exitWithCode (2);
          return;
        } // if
        int locations = stationsArray.length/2;
        for (int i = 0; i < locations; i++) {
          double lat = Double.parseDouble (stationsArray[i*2]);
          if (lat < -90 || lat > 90) {
            LOGGER.severe ("Invalid station latitude: " + lat);
            ToolServices.exitWithCode (2);
            return;
          } // if
          double lon = Double.parseDouble (stationsArray[i*2+1]);
          if (lon < -180 || lon > 180) {
            LOGGER.severe ("Invalid station longitude: " + lon);
            ToolServices.exitWithCode (2);
            return;
          } // if
          stationList.add (new EarthLocation (lat, lon));
        } // for
      } // if

      // Get station labels
      // ------------------
      String[] stationLabelsArray = null;
      if (stationlabels != null) {
        stationLabelsArray = stationlabels.split (ToolServices.SPLIT_REGEX);
        if (stationLabelsArray.length != stationList.size()) {
          LOGGER.severe ("Number of station labels does not match number of stations");
          ToolServices.exitWithCode (2);
          return;
        } // if
      } // if

      // Loop over each station
      // ----------------------
      for (int i = 0; i < stationList.size(); i++) {
        EarthLocation loc = (EarthLocation) stationList.get (i);
        element.addBoundingBox (getCircle (loc, visAngle, STATION_SEGMENTS), 
          stationColor, (stationLabelsArray == null ? null : 
          stationLabelsArray[i]));
      } // for

      // Loop over each input file
      // -------------------------
      for (int i = 0; i < input.length; i++) {

        // Open file and get the transform info to use for the bounding box
        // and the earth area
        String file = input[i];
        VERBOSE.info ("Reading input " + file);
        EarthDataReader reader = null;
        EarthDataInfo info;
        EarthTransform trans;
        try {
          reader = EarthDataReaderFactory.create (file);
          info = reader.getInfo();
          trans = info.getTransform();
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

        int[] dims = trans.getDimensions();
        int rows = dims[0];
        int cols = dims[1];

        // Determine this box color
        // ------------------------
        Color thisBoxColor;
        if (highlight != null) {
          if (file.matches (highlight)) thisBoxColor = boxColor;
          else thisBoxColor = foregroundColor;
        } // if
        else thisBoxColor = boxColor;

        // Add bounding box
        // ----------------
        DataLocation min = new DataLocation (0, 0);
        DataLocation max = new DataLocation (rows-1, cols-1);
        String label = (labelsArray != null ? labelsArray[i] : null);
        element.addBoundingBox (trans, min, max, thisBoxColor, label);

        // Add to earth area
        // -----------------
        if (center == null) {
          area.explore (trans, min, max, trans.transform (min));
          area.explore (trans, min, max, trans.transform (new DataLocation (0, cols-1)));
          area.explore (trans, min, max, trans.transform (max));
          area.explore (trans, min, max, trans.transform (new DataLocation (rows-1, 0)));
        } // if

      } // for

      // Set context using center
      // ------------------------
      if (center != null) {
        String[] centerArray = center.split (ToolServices.SPLIT_REGEX);
        if (centerArray.length != 2) {
          LOGGER.severe ("Invalid center parameters '" + center + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        double lat = Double.parseDouble (centerArray[0]);
        if (lat < -90 || lat > 90) {
          LOGGER.severe ("Invalid center latitude: " + lat);
          ToolServices.exitWithCode (2);
          return;
        } // if
        double lon = Double.parseDouble (centerArray[1]);
        if (lon < -180 || lon > 180) {
          LOGGER.severe ("Invalid center longitude: " + lon);
          ToolServices.exitWithCode (2);
          return;
        } // if
        element.setContextCenter (new EarthLocation (lat, lon));
      } // if

      // Set context using area
      // ----------------------
      else {
        element.setSizeFactor (0.5);
        element.setContextArea (area);
      } // else

      // Create image
      // ------------
      BufferedImage image = new BufferedImage (size, size, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();

      // Set antialiasing
      // ----------------
      if (noantialias) {
        g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      } // if
      else {
        g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      } // else

      // Render map
      // ----------
      element.render (g, foregroundColor, backgroundColor);
      VERBOSE.info ("Writing " + output);
      ImageIO.write (image, "png", new File (output));

      // Write image map file
      // --------------------
      if (map != null) {

        // Create map file
        // ---------------
        VERBOSE.info ("Writing " + map);
        PrintStream mapOutput = new PrintStream (new FileOutputStream (map));
        mapOutput.println ("<map name=\"coverage_map\" id=\"coverage_map\">");

        // Write area polygons
        // -------------------
        EarthImageTransform trans = element.getEarthImageTransform();
        float coords[] = new float[6];
        for (int i = 0; i < input.length; i++) {
          LineFeature polygon = element.getBoundingBox (i);
          GeneralPath path = polygon.getPath (trans);
          PathIterator iter = path.getPathIterator (null);
          mapOutput.print ("  <area shape=\"poly\" id=\"region_" + i + 
            "\" alt=\"region_" + i + "\" coords=\"");
          while (!iter.isDone()) {
            iter.currentSegment (coords);
            int x = (int) Math.round (coords[0]);
            int y = (int) Math.round (coords[1]);
            mapOutput.print (x + "," + y);
            iter.next();
            if (!iter.isDone()) mapOutput.print (",");
          } // while
          mapOutput.println ("\" />");
        } // for
	
        // End map file
        // ------------
        mapOutput.println ("</map>");
        mapOutput.close();

      } // if

    } // try
    catch (Exception e) {
      LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    ToolServices.finishExecution (PROG);

  } // main 

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cwcoverage");

    info.func ("Creates a geographic coverage map for earth data sets and ground stations");

    info.param ("output", "Output PNG file name", 1);
    info.param ("input1 [input2 ...]", "Input data file name(s)", 2);
    info.param ("output", "", 2);

    info.section ("General");
    info.option ("-h, --help", "Show help message");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    info.section ("Output content and format");
    info.option ("-a, --noantialias", "Do not smooth lines and fonts");
    info.option ("-b, --background=COLOR", "Set the map background color");
    info.option ("-c, --center=LATITUDE/LONGITUDE", "Center map on a location");
    info.option ("-f, --foreground=COLOR", "Set map foreground color");
    info.option ("-s, --size=PIXELS", "Set map height and width");

    info.section ("Dataset boundary");
    info.option ("-H, --highlight=PATTERN", "Highlight boundaries with input file names matching pattern");
    info.option ("-l, --labels=LABEL1/LABEL2/...", "Label boundaries with text");
    info.option ("-m, --map=OUTPUT","Create HTML image map file");
    info.option ("-x, --box=COLOR", "Set boundary box outline and fill color");
 
    info.section ("Ground station");
    info.option ("-C, --stationcolor=COLOR", "Set ground station outline and fill color");
    info.option ("-e, --elevation=DEGREES", "Set minimum ground station antenna elevation");
    info.option ("-E, --height=KILOMETERS", "Set satellite orbital height");
    info.option ("-L, --stationlabels=LABEL1/LABEL2/...", "Label ground stations with text");
    info.option ("-S, --stations=LAT1/LON1/LAT2/LON2/...", "Draw ground station coverage circles");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwcoverage () { }

  ////////////////////////////////////////////////////////////

} // cwcoverage class

////////////////////////////////////////////////////////////////////////
