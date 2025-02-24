/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.tools;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.OptionException;

import noaa.coastwatch.io.EarthGridSet;
import noaa.coastwatch.io.EarthGridSetFactory;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.DataColorScale;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.render.EnhancementFunctionFactory;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.IconElementFactory;
import noaa.coastwatch.render.EarthContextElement;
import noaa.coastwatch.render.TextOverlay;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.render.ColorLookup;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.movtool.MetadataEditor;
import org.jcodec.containers.mp4.boxes.MetaValue;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Font;

import java.io.IOException;
import java.io.File;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.time.Instant;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The animate tool creates animations of earth data.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 *
 * <p>
 *   <!-- START NAME -->          
 *   cwanimate - creates earth data animations.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>
 *   cwanimate [OPTIONS] input [input2 ...] [output]
 * </p>
 * 
 * <h3>Options:</h3>
 *
 * <p>
 * -a, --axis NAME/INDEX | NAME/START/END[/STEP] <br>
 * -c, --colormap PALETTE/MIN/MAX[/FUNCTION] <br>
 * -C, --credit TEXT <br>
 * -h, --help <br>
 * -H, --height PIXELS <br>
 * -l, --list-vars <br>
 * -L, --logo NAME | FILE <br>
 * -q, --query-var VARIABLE <br>
 * -r, --rate FPS <br>
 * -t, --title TEXT <br>
 * -u, --units UNITS <br>
 * -V, --variable VARIABLE <br>
 * -v, --verbose <br>
 * --version <br>
 * -W, --width PIXELS <br>
 * -z, --zoom LATITUDE/LONGITUDE[/SCALE] <br>
 * </p>
 *
 * <h2>Description</h2>
 * 
 * <p>
 * The animate tool creates a movie out of multiple frames of earth data.  You
 * can specify either a single file or URL to a THREDDS / DODS / ERDDAP server
 * or multiple files on the local file system as input.  The data is scaled
 * using a color map and output with standard annotations to an output MP4 file.
 * The defaut is to search for a time axis in the input for animation, but other
 * axes can also be used.  Before animating, you can query the input for
 * variables and axes:
 * </p>
 * <pre>
 *   $ cwanimate --list-vars input.nc4
 *   $ cwanimate --query-var sst input.nc4
 * </pre>
 * <p>The output MP4 file contains metadata including the software version used
 * and the command line parameters, so that you can create new animations based
 * on existing ones.  Use the exiftool on Linux or Mac to query the output 
 * file metadata.</p>
 * 
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> input [input2 ...] </dt>
 *   <dd> Either (i) a single input dataset specified as a local file name or URL
 *   containing a set of 2D data grids with additional time, level, or some other 
 *   axis, or (ii) a list of input datasets each containing a single timestep
 *   of 2D data grids.</dd>
 *
 *   <dt> [output] </dt>
 *   <dd> The output movie file name. </dd>
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt>-a, --axis NAME/INDEX | NAME/START/END[/STEP]</dt>
 *   <dd>The axis name and index/range for animation.  Only one axis can be specified 
 *   with a range and optional step value to animate, and all other axes with a single 
 *   index value.  If an axis name is not specfied, the zero index is assumed 
 *   for that axis.</dd>
 *
 *   <dt>-c, --colormap PALETTE/MIN/MAX[/FUNCTION]</dt>
 *   <dd>The color map assignment for the data, consisting of the palette name
 *   (see cwrender), minimum data value, maximum data value, and 
 *   optionally a function type: 'linear' (default), 'log', 'stepN', or 
 *   'gamma'.</dd>
 *
 *   <dt>-C, --credit TEXT</dt>
 *   <dd>The data credit text, default is the origin/institution metadata value.</dd>
 * 
 *   <dt>-h, --help</dt>
 *   <dd>Prints a brief help message.</dd>
 *
 *   <dt>-H, --height PIXELS</dt>
 *   <dd>The height of the output in pixels, default is 720.</dd>
 *
 *   <dt>-l, --list-vars</dt>
 *   <dd>Lists the variables in the input data that are available to animate.</dd>
 *
 *   <dt>-L, --logo NAME | FILE</dt>
 *   <dd>The logo on the plot, default is the NOAA logo.  The logo can either
 *   be a name like in cwrender, or a custom PNG, GIF, or JPEG file.</dd>
 *
 *   <dt>-q, --query-var VARIABLE</dt>
 *   <dd>Performs a query of the specified variable in the input data and prints 
 *   out the axis information.</dd>
 *
 *   <dt>-r, --rate FPS</dt>
 *   <dd>The frame rate of the output in frames per second, default is 15.</dd>
 *
 *   <dt>-t, --title TEXT</dt>
 *   <dd>The title text, default is the variable long name metadata value.</dd>
 * 
 *   <dt>-u, --units UNITS</dt>
 *   <dd>The units for the color map, default is to read from the input file.</dd>
 *
 *   <dt>-V, --variable VARIABLE</dt>
 *   <dd>The variable data to animate.</dd>
 *
 *   <dt>-v, --verbose</dt>
 *   <dd>Turns verbose mode on.  The current status of creating the data 
 *   animation is printed periodically.  The default is to run
 *   quietly.</dd>
 *
 *   <dt>--version</dt>
 *   <dd>Prints the software version.</dd>
 *
 *   <dt>-W, --width PIXELS</dt>
 *   <dd>The width of the output in pixels, default is 1280.</dd>
 *
 * 	 <dt>-z, --zoom LATITUDE/LONGITUDE[/SCALE]</dt>
 *   <dd>Zooms and recenters the view on a new latitude and longitude point.
 *   The optional zoom scale is either a screen:data pixel factor (eg: 0.5, 1, 2), 
 *   a screen pixel size in kilometres (eg: '5km'), or a GIS map zoom 
 *   level (eg: 'L5').  The default zoom scale is 1:1 screen:data pixels.</dd>
 * 
 * </dl>
 * 
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Invalid input or output file names </li>
 *   <li> Unsupported input file format </li>
 *   <li> Input file earth transforms do not match </li>
 *   <li> Variable name not found </li>
 *   <li> No variable specified to animate </li>
 *   <li> Axis name not found </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <ul>
 * 
 *   <li>Create a chlorophyll-a animation centered on the Gulf of Mexico 
 *   from a time series of NetCDF files:
 *   <pre> 
 *     $ cwanimate -v --zoom 24.7/-87.7/L5 --variable chlor_a 
 *     --colormap NCCOS-chla/0.01/64/log dineof/*.nc output_dineof_chlor_a_gom.mp4
 *   </pre>
 *   </li>
 * 
 *   <li>Similar to above, but for monthly Hawaii data over a three year period 
 *   and from a THREDDS server:
 *   <pre>
 *     $ cwanimate -v --zoom 20.6/-156.9/L7 --axis time/0/36 --variable chlor_a 
 *     --colormap Turbo/0.01/1/log 
 *     https://oceanwatch.pifsc.noaa.gov/thredds/dodsC/noaa_snpp_chla/monthly 
 *     output_chlor_a_hawaii.mp4
 *   </pre> 
 *   </li>
 * 
 *   <li>Create a sea surface temperature animation centered on the Gulf of
 *   Mexico from a NetCDF 4 file containing multiple time steps downloaded 
 *   from a THREDDS server using the NetCDF Subset Service:
 *   <pre>
 *     $ cwanimate -v --title "SST Analysis Gulf of Mexico" --zoom 24.7/-87.7/L6 
 *     --variable sst_analysis_night_only --colormap Turbo/8/35 
 *     CW_BLENDED_NIGHT_SST_cwblendednightsst.nc4 output_sst_gom.mp4
 *   </pre>
 *   </li>
 * 
 * </ul>
 * 
 * 
 * 
 * 
 * 
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 4.0.1
 */
public final class cwanimate {

  private static final String PROG = cwanimate.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.
   */
  public static void main (String argv[]) {

    ToolServices.startExecution (PROG);
    ToolServices.setCommandLine (PROG, argv);

    // Parse the command line parameters
    CmdLineParser cmd = new CmdLineParser();
    Option axisOpt = cmd.addStringOption ('a', "axis");
    Option colormapOpt = cmd.addStringOption ('c', "colormap");
    Option creditOpt = cmd.addStringOption ('C', "credit");
    Option heightOpt = cmd.addIntegerOption ('H', "height");
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option listOpt = cmd.addBooleanOption ('l', "list-vars");
    Option logoOpt = cmd.addStringOption ('L', "logo");
    Option queryOpt = cmd.addStringOption ('q', "query-var");
    Option rateOpt = cmd.addIntegerOption ('r', "rate");
    Option titleOpt = cmd.addStringOption ('t', "title");
    Option unitsOpt = cmd.addStringOption ('u', "units");
    Option variableOpt = cmd.addStringOption ('V', "variable");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option versionOpt = cmd.addBooleanOption ("version");
    Option widthOpt = cmd.addIntegerOption ('W', "width");
    Option zoomOpt = cmd.addStringOption ('z', "zoom");

    try { cmd.parse (argv); }
    catch (OptionException e) {
      LOGGER.warning (e.getMessage());
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // catch

    // Print help or version messages
    if (cmd.getOptionValue (helpOpt) != null) {
      usage();
      ToolServices.exitWithCode (0);
      return;
    } // if
    if (cmd.getOptionValue (versionOpt) != null) {
      System.out.println (ToolServices.getFullVersion (PROG));
      ToolServices.exitWithCode (0);
      return;
    } // if

    // Check for options that will make it so that the output file
    // is not specified
    var listVars = (cmd.getOptionValue (listOpt) != null);
    var queryVar = (String) cmd.getOptionValue (queryOpt);
    int minArgs = (listVars || (queryVar != null)) ? 1 : 2;

    // Get the required remaining arguments
    String[] remainingArgs = cmd.getRemainingArgs();
    if (remainingArgs.length < minArgs) {
      LOGGER.warning ("At least " + minArgs + " argument(s) required");
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // if

    // Turn on verbose printing
    boolean verbosePrinting = (cmd.getOptionValue (verboseOpt) != null);
    if (verbosePrinting) VERBOSE.setLevel (Level.INFO);

    // Open input grids
    EarthGridSet gridSet = null;
    try {
			if (minArgs == remainingArgs.length) {
				var inputFileName = remainingArgs[0];
				VERBOSE.info ("Accessing single input " + inputFileName);
      	gridSet = EarthGridSetFactory.createFromSingleSource (inputFileName);
			} // if
			else {
		    var inputFileList = Arrays.asList (remainingArgs);
		    if (minArgs == 2) inputFileList = inputFileList.subList (0, remainingArgs.length-1);
 				VERBOSE.info ("Accessing time series of inputs " + inputFileList.get (0) + " ...");
				gridSet = EarthGridSetFactory.createFromTimeSeries (inputFileList);
			} // else
		} // try
    catch (IOException e) {
      LOGGER.log (Level.SEVERE, "Error reading input dataset(s)", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    // List the variables found to animate
    var variables = gridSet.getVariables();
    if (listVars) {
			System.out.println ("Variables found:");
	    for (var varName : variables)
  			System.out.println (varName);
  		return;
  	} // if

    // Query a specific variable for its axis values
    if (queryVar != null) {
    	if (!variables.contains (queryVar)) {
	      LOGGER.severe ("Query variable " + queryVar + " not found in input");
	      ToolServices.exitWithCode (2);
	      return;
    	} // if
    	System.out.println ("Axes for " + queryVar + ":");
    	var axes = gridSet.getAxes (queryVar);
    	for (var axis : axes) {
    		System.out.println (axis.getName() + " (" + axis.getUnits() + "), " + axis.getSize() + " values:");
    		for (int i = 0; i < axis.getSize(); i++)
    			System.out.println ("[" + i + "] = " + axis.getValue (i));
    	} // for
    	return;
    } // if

    // Get the output file name
    var outputFileName = remainingArgs[remainingArgs.length-1];

    // Check that the user specified a variable to animate
    var animateVar = (String) cmd.getOptionValue (variableOpt);
    if (animateVar == null) {
      LOGGER.severe ("No variable specified, use --variable option");
      ToolServices.exitWithCode (2);
      return;
  	} // if

  	else {

  		// Check that the animation variable exists
	    if (!variables.contains (animateVar)) {
	      LOGGER.severe ("Animation variable " + animateVar + " not found in input");
	      ToolServices.exitWithCode (2);
	      return;
	    } // if

	    // Get the axes and create a map from axis name to index
	    var axes = gridSet.getAxes (animateVar);
	    var axisIndexMap = new HashMap<String, Integer>();
	    for (int i = 0; i < axes.size(); i++) axisIndexMap.put (axes.get (i).getName(), i);

	    // Initialize the list of axis indices that we'll use for animation.
	    // These are the indices that are use for retrieval from the grid set.
	    var axisIndexList = new ArrayList<Integer>();
	    for (int i = 0; i < axes.size(); i++) axisIndexList.add (0);
	    int animationAxisListIndex = -1;
	    int animationAxisEnd = -1;
	    int animationAxisStep = 1;

	    // Get the list of axis option values
	    var axisSpecVals = cmd.getOptionValues (axisOpt);
	    var axisSpecList = new ArrayList<String>();
	    for (var val : axisSpecVals) axisSpecList.add (val.toString());

	    for (var axisSpec : axisSpecList) {

	    	// For this option, check if the named axis exists in our list
        var specArray = axisSpec.split (ToolServices.getSplitRegex());
       	String name = specArray[0];
      	if (!axisIndexMap.containsKey (name)) {
		      LOGGER.severe ("Axis " + name + " not found");
		      ToolServices.exitWithCode (2);
		      return;
      	} // if

      	// Get the index along the axis that the user requested and
      	// store it in our list of axis indices
      	int axisIndex = axisIndexMap.get (name);
       	int axisValue = Integer.parseInt (specArray[1]);
       	axisIndexList.set (axisIndex, axisValue);

       	// Get the index along the axis for the end of the animation
       	if (specArray.length >= 3) {
       		animationAxisListIndex = axisIndex;
       		animationAxisEnd = Integer.parseInt (specArray[2]);
       	} // if

       	// Get the step value along the animation axis
       	if (specArray.length == 4) {
					animationAxisStep = Integer.parseInt (specArray[3]);
					if (animationAxisStep < 1) {
			      LOGGER.severe ("Animation step value must be >= 1");
			      ToolServices.exitWithCode (2);
			      return;
					} // if
       	} // if

	    } // for

	    // Check that we found an animation axis provided by the user.  If not,
	    // search for a time axis and assume a start/end of the full axis range.
	    if (animationAxisListIndex == -1) {
	    	for (int i = 0; i < axes.size(); i++) {
					var axis = axes.get (i);
					if (axis.getAxisType().equals ("Time")) {
         		animationAxisListIndex = i;
         		animationAxisEnd = axis.getSize() - 1;
				    VERBOSE.info ("Found time axis for animation with " + axis.getSize() + " steps");
					} // if
				} // for
			} // if
			if (animationAxisListIndex == -1) {
	      LOGGER.severe ("No animation range specified and no time axis found, use --axis with a range");
	      ToolServices.exitWithCode (2);
	      return;
	    } // if

			// Report the axes used for the animation
	    VERBOSE.info ("Creating animation of " + animateVar + " with axes:");
	    for (int i = 0; i < axes.size(); i++) {
	    	var axis = axes.get (i);
	    	var axisIndex = axisIndexList.get (i);
	    	String spec;
	    	if (i == animationAxisListIndex) 
	    		spec = "[" + axisIndex + " -> " + animationAxisEnd + "] = " + axis.getValue (axisIndex) + " -> " + axis.getValue (animationAxisEnd);
	    	else
	    		spec = "[" + axisIndex + "] = " + axis.getValue (axisIndex);
	    	VERBOSE.info (axis.getName() + " (" + axis.getUnits() + "): " + spec);
	    } // for

	    // Get the color palette, range, and function
	    var trans = gridSet.getTransform (animateVar);
    	var grid = gridSet.accessGrid (animateVar, axisIndexList);
	    var unitsSpec = (String) cmd.getOptionValue (unitsOpt);
	    if (unitsSpec != null) grid.convertUnits (unitsSpec);
	    var colormapSpec = (String) cmd.getOptionValue (colormapOpt);
	    if (colormapSpec == null) {
	      LOGGER.severe ("No variable colormap specified, use --colormap option");
	      ToolServices.exitWithCode (2);
	      return;
	    } // if
			var colormapSpecArray = colormapSpec.split (ToolServices.getSplitRegex());
	    var palette = PaletteFactory.create (colormapSpecArray[0]);
	    var min = Double.parseDouble (colormapSpecArray[1]);
	    var max = Double.parseDouble (colormapSpecArray[2]);
	    var functionType = colormapSpecArray.length == 4 ? colormapSpecArray[3] : "linear";
	    var function = EnhancementFunctionFactory.create (functionType, new double[] {min, max});

	    // Parse the height and width options
	    var heightSpec = (Integer) cmd.getOptionValue (heightOpt);
	    var height = heightSpec == null ? 720 : heightSpec.intValue();
	    var widthSpec = (Integer) cmd.getOptionValue (widthOpt);
	    var width = widthSpec == null ? -1 : widthSpec.intValue();

	    ColorEnhancement view = null;
	    try {

  	    // Create the color enhancement for the data view
      	view = new ColorEnhancement (trans, grid, palette, function);

	      // Zoom in and resize on a specified area
	      var zoomSpec = (String) cmd.getOptionValue (zoomOpt);
	      if (zoomSpec != null) {

	      	// First get the center location requested and test it
	        var zoomSpecArray = zoomSpec.split (ToolServices.getSplitRegex());
		      double lat = Double.parseDouble (zoomSpecArray[0]);
		      if (lat < -90 || lat > 90) {
		        LOGGER.severe ("Invalid zoom latitude: " + lat);
		        ToolServices.exitWithCode (2);
		        return;
		      } // if
		      double lon = Double.parseDouble (zoomSpecArray[1]);
		      if (lon < -180 || lon > 180) {
		        LOGGER.severe ("Invalid zoom longitude: " + lon);
		        ToolServices.exitWithCode (2);
		        return;
		      } // if
		      var center = new EarthLocation (lat, lon, trans.getDatum());
	        var dataLoc = trans.transform (center);
	        if (dataLoc == null) {
	          LOGGER.severe ("Zoom location " + center.format() + " has no valid data location");
	          ToolServices.exitWithCode (2);
	          return;
	        } // if
			    VERBOSE.info ("Centering output on " + center.format());

	        // Now parse the scale factor if it's provided
		      double factor = 1;
		      if (zoomSpecArray.length == 3) {
		      	var factorSpec = zoomSpecArray[2];

		      	// Parse as a number of kilometers and then compute from the data
		      	// resolution
		      	if (factorSpec.endsWith ("km")) {
		      		var kmPerPixel = Double.parseDouble (factorSpec.replaceAll ("km", ""));
		      		var dataResArray = trans.getResolution (dataLoc);
		      		var dataRes = Math.max (dataResArray[0], dataResArray[1]);
		      		factor = dataRes / kmPerPixel;
					    VERBOSE.info ("Setting data view to " + kmPerPixel + " km/pixel");
		      	} // if

		      	// Parse as a zoom level that indicates a predetermined map scale:
		      	//
						// Zoom Level 	Scale (1:x)				Resolution (meters/pixel)
						// 0						1:500,000,000			156,543
						// 1						1:250,000,000			78,271
						// 2						1:150,000,000			39,135
						// 3						1:70,000,000			19,568
						// 4						1:35,000,000			9,784
						// 5						1:15,000,000			4,892
						// 6						1:10,000,000			2,446
						// 7						1:5,000,000				1,223
						// 8						1:2,500,000				611
						// 9						1:1,000,000				305
						// 10						1:500,000					152
		      	else if (factorSpec.startsWith ("L")) {
		      		var zoomLevel = Integer.parseInt (factorSpec.replaceAll ("L", ""));
							var initKmPerPixel = 156.54303;
							var kmPerPixel = initKmPerPixel / Math.pow (2, zoomLevel);
		      		var dataResArray = trans.getResolution (dataLoc);
		      		var dataRes = Math.max (dataResArray[0], dataResArray[1]);
		      		factor = dataRes / kmPerPixel;
					    VERBOSE.info ("Setting data view to zoom level " + zoomLevel + ", " + kmPerPixel + " km/pixel");
		      	} // else if 

		      	// Parse as a factor dfirectly from the user -- we assume the user
		      	// knows the data resolution in this case
		      	else {
							factor = Double.parseDouble (factorSpec);
					    VERBOSE.info ("Setting data view magnification to " + factor);
		      	} // else

		      } // if

		      // Magnify the view around the center point and using the scale factor
	        view.magnify (dataLoc, factor);

	        // Adjust the height and width of the view
	        if (height > 0 && width > 0) 
	        	view.setSize (new Dimension (width, height));
	        else if (height > 0)
	        	view.setSize (new Dimension ((int) Math.round (height/0.5625), height));
	        else if (width > 0)
	        	view.setSize (new Dimension (width, (int) Math.round (width*0.5625)));

				} // if

				// Resize the full data view 
				else {
	        if (height > 0 && width > 0) 
	        	view.resize (new Dimension (width, height));
	        else if (height > 0)
	        	view.resizeHeight (height);
	        else if (width > 0)
	        	view.resizeWidth (width);
				} // else

			} // try

      catch (Exception e) {
	      LOGGER.log (Level.SEVERE, "Error creating and sizing data view", ToolServices.shortTrace (e, "noaa.coastwatch"));
	      ToolServices.exitWithCode (2);
	      return;
      } // catch

      // Add the coastline and the missing color
      var coast = new CoastOverlay (Color.BLACK);
      coast.setDropShadow (true);
      coast.setFillColor (noaa.coastwatch.render.ColorLookup.getInstance().getColor ("land"));
      view.addOverlay (coast);
      view.setMissingColor (new Color (64, 64 ,64));

    	try {

    		// Create the video encoder and create the image that we'll render to
    		var rateSpec = (Integer) cmd.getOptionValue (rateOpt);
    		var rate = rateSpec == null ? 15 : rateSpec.intValue();
				var encoder = AWTSequenceEncoder.createSequenceEncoder (new File (outputFileName), rate);
				var viewDims = view.getSize (null);
        var image = new BufferedImage (viewDims.width, viewDims.height, BufferedImage.TYPE_3BYTE_BGR);
        var graphics = (Graphics2D) image.createGraphics();
        graphics.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		    // Create a label at the top
		    var topLabelAreaWidth = viewDims.width;
		    var topLabelAreaHeight = 30;
		    var topLabelBox = new Rectangle (0, 0, topLabelAreaWidth, topLabelAreaHeight);
		    var topLabelFont = new Font ("Dialog", Font.BOLD, 20); 
		    var titleSpec = (String) cmd.getOptionValue (titleOpt);
		    var topLabelText = titleSpec != null ? titleSpec : grid.getLongName();
				var topLabel = new TextElement (topLabelText, topLabelFont, 
					new Point (topLabelBox.x + topLabelBox.width/2, topLabelBox.y + topLabelBox.height/2), 
					new double[] {0.5, 0.35}, 0);

		    // Create a label at the bottom
		    var bottomLabelAreaWidth = viewDims.width;
		    var bottomLabelAreaHeight = 22;
		    var bottomLabelBox = new Rectangle (0, viewDims.height-bottomLabelAreaHeight, bottomLabelAreaWidth, bottomLabelAreaHeight);
		    var bottomLabelFont = new Font ("Dialog", Font.PLAIN, 15); 
		    var creditSpec = (String) cmd.getOptionValue (creditOpt);
		    var bottomLabelText = "Data: " + (creditSpec != null ? creditSpec : gridSet.getOrigin());
				var bottomLabel = new TextElement (bottomLabelText, bottomLabelFont, 
					new Point (bottomLabelBox.x + bottomLabelBox.width/2, bottomLabelBox.y + bottomLabelBox.height/2), 
					new double[] {0.5, 0.35}, 0);

	      // Create a color scale
				var scale = new DataColorScale (function, palette, animateVar, grid.getUnits());
				scale.setLegendAxis (DataColorScale.HORIZONTAL_AXIS);
				scale.setForeground (Color.WHITE);
				scale.setBackground (null);
				var scaleDims = scale.getSize (graphics);
				scale.setPreferredSize (new Dimension (
					Math.max (viewDims.width/3, scaleDims.width),
				 	scaleDims.height
				));
				scaleDims = scale.getSize (graphics);
				var scaleBoxWidth = scaleDims.width + 30;
				var scaleBoxHeight = scaleDims.height + 10;
				var scaleBox = new RoundRectangle2D.Double (
					viewDims.width/2 - scaleBoxWidth/2, viewDims.height - bottomLabelAreaHeight - 15 - scaleBoxHeight,
					scaleBoxWidth, scaleBoxHeight,
					20, 20
				);
				var scalePos = new Point ((int) (scaleBox.x + 15), (int) (scaleBox.y + 5));
				var boxColor = new Color (30, 30, 30, 128);
				var boxOutlineColor = new Color (255, 255, 255, 128);

				// Create a logo icon
				var logoSpec = (String) cmd.getOptionValue (logoOpt);
				var factory = IconElementFactory.getInstance();
				var iconName = logoSpec == null ? factory.getDefaultIcon() : logoSpec;
				var logoIcon = factory.create (iconName);
				logoIcon.setShadow (true);
				var logoSize = new Dimension ((int) scaleBox.height, (int) scaleBox.height);
				logoIcon.setPreferredSize (logoSize);
				var logoBounds = logoIcon.getBounds (graphics);
				var logoPos = new Point (
					viewDims.width - 30 - logoBounds.width,
				  (int) scaleBox.y
				);
				logoIcon.setPosition (logoPos);

				// Create a context element
				var context = getContext (view);
				Shape contextShape = null;
				if (context != null) {
		      context.setPreferredSize (logoSize);
		      var contextBounds = context.getBounds (graphics);
		      var contextPos = new Point (
		      	30,
		      	(int) scaleBox.y 
		      );
		      context.setPosition (contextPos);
		      contextShape = new Ellipse2D.Double (
		      	contextPos.x - 5, contextPos.y - 5,
		      	contextBounds.width + 10, contextBounds.height + 10
		      );
		      context.setLandColor (new Color (177, 177, 177, 128));
		    } // if

        // Set up the axis we'll use for animation and initialize the
        // frame count that we expect to create
	    	int animationAxisIndex = axisIndexList.get (animationAxisListIndex);
	    	var animationAxis = axes.get (animationAxisListIndex);
	    	boolean animationComplete = false;
	    	int frame = 1;
	    	int frames = (animationAxisEnd - animationAxisIndex)/animationAxisStep + 1;

	    	// Create the axis slider elements
	    	var axisSliderWidth = viewDims.width/3;
	    	var axisSliderHeight = 10;
	    	var axisSliderBar = new RoundRectangle2D.Double (
	    		viewDims.width/2 - axisSliderWidth/2, topLabelAreaHeight + 20,
	    		axisSliderWidth, axisSliderHeight,
	    		10, 10
	    	);
	    	var axisSliderKnobSize = 18;
	    	var axisSliderKnobColor = new Color (205, 96, 0);
				var axisSliderKnobStroke = new BasicStroke (4.0f);	    	
	    	var axisSliderKnobStartX = axisSliderBar.x + axisSliderKnobSize/3;
	    	var axisSliderKnobEndX = axisSliderBar.x + axisSliderWidth - axisSliderKnobSize/3 - axisSliderKnobSize;
	    	var axisSliderKnobIncrementX = (axisSliderKnobEndX - axisSliderKnobStartX) / (double) (frames-1);
	    	var axisSliderKnob = new Ellipse2D.Double (
	    		axisSliderKnobStartX, axisSliderBar.y + axisSliderHeight/2 - axisSliderKnobSize/2,
	    		axisSliderKnobSize, axisSliderKnobSize
	    	);
		    var axisSliderLabelFont = new Font ("Dialog", Font.BOLD, 18); 
		    var axisSliderLabelStartIndex = animationAxisIndex;
		    var axisSliderLabelEndIndex = animationAxisIndex + (frames-1)*animationAxisStep;
		    var axisSliderLabelIndex = axisSliderLabelStartIndex;
		    var axisSliderLabelText = formatAxisValue (animationAxis, axisSliderLabelIndex);
				var axisSliderLabel = new TextElement (axisSliderLabelText, axisSliderLabelFont, 
					new Point ((int) (axisSliderKnob.x + axisSliderKnob.width/2), (int) (axisSliderKnob.y + axisSliderKnob.height + 10)), 
					new double[] {0.5, 1}, 0);
				var axisLabel = new TextElement (animationAxis.getAxisType() + ":", axisSliderLabelFont, 
					new Point ((int) (axisSliderBar.x - 15), (int) (axisSliderBar.y + axisSliderBar.height/2)), 
					new double[] {1, 0.5}, 0);

	    	do {

	    		VERBOSE.info ("Encoding frame [" + frame + "/" + frames + "]: " + animationAxis.getValue (animationAxisIndex));
					frame++;

					// Render the data view to the frame 
	    		view.render (graphics);

	    		// Add in the color scale
	    		graphics.setColor (boxColor);
	    		graphics.fill (scaleBox);
	    		graphics.setColor (boxOutlineColor);
	    		graphics.draw (scaleBox);
	    		scale.render (graphics, scalePos.x, scalePos.y);

	    		// Draw the logo
			    logoIcon.render (graphics, null, null);

					// Draw the context legend
			    if (context != null) {
		    		graphics.setColor (boxColor);
		    		graphics.fill (contextShape);
		    		graphics.setColor (boxOutlineColor);
		    		graphics.draw (contextShape);
		    		graphics.clip (contextShape);
			      context.render (graphics, new Color (255, 255, 255, 180), null);
		    		graphics.setClip (null);
			    } // if

			    // Draw the top and bottom labels
			    graphics.setColor (boxColor);
			    graphics.fill (topLabelBox);
			    graphics.fill (bottomLabelBox);
			    topLabel.render (graphics, Color.WHITE, null);
			    bottomLabel.render (graphics, Color.WHITE, null);

			    // Draw the axis slider and label
			    graphics.setColor (boxColor);
			    graphics.fill (axisSliderBar);
			    graphics.setColor (boxOutlineColor);
			    graphics.draw (axisSliderBar);
			    graphics.setColor (axisSliderKnobColor);
			    graphics.fill (axisSliderKnob);
			    graphics.setColor (boxColor);
			    var stroke = graphics.getStroke();
			    graphics.setStroke (axisSliderKnobStroke);
			    graphics.draw (axisSliderKnob);
			    graphics.setStroke (stroke);
			    axisSliderLabel.render (graphics, Color.WHITE, Color.BLACK);
			    axisLabel.render (graphics, Color.WHITE, Color.BLACK);

			    // Encode the image we just created for this frame
	    		encoder.encodeImage (image);

	    		// Increment to the next axis step and test for completion.  If
	    		// we're not complete, update the grid for the data view.
					animationAxisIndex += animationAxisStep;
					animationComplete = (animationAxisIndex > animationAxisEnd);
					if (!animationComplete) {

	      		axisIndexList.set (animationAxisListIndex, animationAxisIndex);
	      		gridSet.releaseGrid (grid);
	      		grid = gridSet.accessGrid (animateVar, axisIndexList);
      	    if (unitsSpec != null) grid.convertUnits (unitsSpec);
	      		view.setGrid (grid);

						axisSliderKnob.x += axisSliderKnobIncrementX;
						axisSliderLabelIndex += animationAxisStep;
		        axisSliderLabelText = formatAxisValue (animationAxis, axisSliderLabelIndex);
					  axisSliderLabel = new TextElement (axisSliderLabelText, axisSliderLabelFont, 
					    new Point ((int) (axisSliderKnob.x + axisSliderKnob.width/2), (int) (axisSliderKnob.y + axisSliderKnob.height + 10)), 
					    new double[] {0.5, 1}, 0);

	      	} // if

		    } while (!animationComplete);

		    // Finish encoding the animation
		    VERBOSE.info ("Finishing encoding");
		    encoder.finish();

		    // Write the metadata for encoder, author, etc
		   	VERBOSE.info ("Writing metadata");
				var editor = MetadataEditor.createFrom (new File (outputFileName));
				var meta = editor.getKeyedMeta();
				meta.put ("encoder", MetaValue.createString (ToolServices.PACKAGE + " version " + ToolServices.getVersion()));
				meta.put ("author", MetaValue.createString (System.getProperty ("user.name")));
				meta.put ("comment", MetaValue.createString ("Command line was " + ToolServices.getCommandLine()));
				editor.save (false);

	   	} catch (IOException e) {
	      LOGGER.log (Level.SEVERE, "Error encoding image sequence", ToolServices.shortTrace (e, "noaa.coastwatch"));
	      ToolServices.exitWithCode (2);
	      return;
	   	} // catch

  	} // else

  } // main

  ////////////////////////////////////////////////////////////

 	private static String formatAxisValue (
 		EarthGridSet.Axis axis,
 		int index
 	) {

 		String value;
	  if (axis.getAxisType().equals ("Time")) {
			var formatter = DateTimeFormatter.ofPattern ("yyyy/MM/dd").withZone (ZoneOffset.UTC);					
	  	value = formatter.format ((Instant) axis.getValue (index));
	  } // if
	  else {
			var units = axis.getUnits();		    	
	  	value = axis.getValue (index) + " " + units;
	  } // else

	  return (value);

 	} // formatAxisValue

  ////////////////////////////////////////////////////////////

  /**
   * Gets a context element for a data view.
   * 
   * @param view the data view for the context.
   * 
   * @return the context or null if the area shown by the view is too
   * large.
   */
  private static EarthContextElement getContext (
  	EarthDataView view
  ) {

  	EarthContextElement context = null;

    if (view.getArea().getCoverage() < 0.25) {
	    var trans = view.getTransform();
      var imageTrans = trans.getImageTransform();
      var earthTrans = trans.getEarthTransform();
			var viewSize = view.getSize (null);
      var upperLeft = imageTrans.transform (new Point (0,0));
      var lowerRight = imageTrans.transform (new Point (viewSize.width-1, viewSize.height-1));
      context = new EarthContextElement (earthTrans, upperLeft, lowerRight);
      context.addBoundingBox (earthTrans, upperLeft, lowerRight, Color.RED, null);
    } // if

    return (context);

  } // getContext

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cwanimate");

    info.func ("Creates animations of earth data");

    info.param ("input [input2 ...]", "Input data file(s)");
    info.param ("[output]", "Output movie file");

		info.option ("-a, --axis NAME/INDEX | NAME/START/END[/STEP]", "Set axis values");
		info.option ("-c, --colormap PALETTE/MIN/MAX[/FUNCTION]", "Set color map and range");
    info.option ("-C, --credit TEXT", "Set data credit text");
    info.option ("-h, --help", "Show help message");
		info.option ("-H, --height PIXELS", "Set output height");
    info.option ("-l, --list-vars", "List variables in input");
    info.option ("-L, --logo NAME | FILE", "Set output logo");
    info.option ("-q, --query-var VARIABLE", "Prints axis extents");
    info.option ("-r, --rate FPS", "Set frame rate");
    info.option ("-t, --title TEXT", "Set title text");
    info.option ("-u, --units UNITS", "Set color map units");
    info.option ("-V, --variable VARIABLE", "Set variable to animate");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");
		info.option ("-W, --width PIXELS", "Set output width");
		info.option ("-z, --zoom LATITUDE/LONGITUDE[/SCALE]", "Set center and map scale");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwanimate () { }

  ////////////////////////////////////////////////////////////

} // cwanimate class

