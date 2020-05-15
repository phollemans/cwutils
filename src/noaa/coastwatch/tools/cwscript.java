////////////////////////////////////////////////////////////////////////
/*

     File: cwscript.java
   Author: Peter Hollemans
     Date: 2018/07/04

  CoastWatch Software Library and Utilities
  Copyright (c) 2018 National Oceanic and Atmospheric Administration
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
import java.util.Arrays;

import bsh.Interpreter;
import bsh.EvalError;

import java.awt.Window;

/**
 * <p>The script tool runs a shell script written in BeanShell syntax.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwscript - runs a shell script written in BeanShell.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p> cwscript input [ARGUMENTS]</p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The script tool runs a shell script written in the
 * <a href="http://beanshell.org">BeanShell</a> language, which
 * is a simplified variant of Java.  All of the CoastWatch API is available
 * to the code using import statments.  The arguments passed on the
 * command line of the tool are available in the shell script by
 * accessing the <code>String[] args</code> array starting with
 * <code>args[0]</code> as the first argument.</p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt>input</dt>
 *   <dd>The input shell script file.</dd>
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
 *   <li> Error encountered running the shell script </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p>The following script prints the start time of a data file:</p>
 * <pre>
 * import noaa.coastwatch.io.EarthDataReaderFactory;
 *
 * reader = EarthDataReaderFactory.create (args[0]);
 * print (reader.getInfo().getStartDate());
 * </pre>
 * <p>A sample run on a CoastWatch HDF file:</p>
 * <pre>
 * phollema$ cwscript start_time.bsh 2018_217_0309_n18_wj.hdf
 * Sat Aug 04 20:09:27 PDT 2018
 * </pre>
 * <p>Another example script prints out the latitude and longitude values
 * along the satellite subpoint of a data file:</p>
 * <pre>
 * import noaa.coastwatch.io.EarthDataReaderFactory;
 * import noaa.coastwatch.util.DataLocation;
 *
 * input = args[0];
 * reader = EarthDataReaderFactory.create (input);
 * lat = reader.getVariable ("latitude");
 * lon = reader.getVariable ("longitude");
 * dims = lat.getDimensions();
 *
 * rows = dims[0];
 * cols = dims[1];
 * print (rows);
 * print (cols);
 *
 * for (int i = 0; i &lt; rows; i++) {
 *   loc = new DataLocation (i, cols/2);
 *   print (lat.getValue (loc) + " " + lon.getValue (loc));
 * } // for
 * </pre>
 * <p>The output from running on a NOAA 1b format Metop-2 FRAC file:</p>
 * <pre>
 * phollema$ cwscript sat_subpoint.bsh NSS.FRAC.M2.D10206.S1053.E1235.B1953132.SV
 * 4733
 * 2048
 * 66.71659851074219 -1.3313000202178955
 * 66.7074966430664 -1.341499924659729
 * 66.69829559326172 -1.351599931716919
 * 66.68930053710938 -1.3616999387741089
 * 66.68009948730469 -1.3717999458312988
 * 66.6709976196289 -1.3819999694824219
 * 66.66189575195312 -1.3919999599456787
 * ...
 * </pre>
 * <p>The example script below shows how to use the new <i>noaa.coastwatch.util.chunk</i>
 * API to retrieve chunks of data from a data variable in any file:</p>
 * <pre>
 * import noaa.coastwatch.io.EarthDataReader;
 * import noaa.coastwatch.io.EarthDataReaderFactory;
 * import noaa.coastwatch.util.chunk.DataChunk;
 * import noaa.coastwatch.util.chunk.ChunkPosition;
 * import noaa.coastwatch.util.chunk.GridChunkProducer;
 *
 * reader = EarthDataReaderFactory.create (args[0]);
 * lat = reader.getVariable ("latitude");
 * producer = new GridChunkProducer (lat);
 * pos = new ChunkPosition (2);
 * pos.start[0] = 0;
 * pos.start[1] = 0;
 * pos.length[0] = pos.length[1] = 16;
 * chunk = producer.getChunk (pos);
 *
 * print (chunk);
 * </pre>
 * <p> Running this script on a CoastWatch HDF format VIIRS granule produces:</p>
 * <pre>
 * phollema$ cwscript chunk_type.bsh VXSRCW.B2018205.180733.hdf
 * noaa.coastwatch.util.chunk.FloatChunk@edf4efb
 * </pre>
 * <p>Graphics windows can also be created from a script, using the
 * <b>cwgscript</b> launcher which properly sets up a graphics environment.
 * The following script run with <b>cwgscript</b> displays a view of a variable
 * in an input file:</p>
 * <pre>
 * import java.awt.Color;
 * import javax.swing.JFrame;
 *
 * import noaa.coastwatch.gui.EarthDataViewFactory;
 * import noaa.coastwatch.gui.EarthDataViewPanel;
 * import noaa.coastwatch.gui.GUIServices;
 * import noaa.coastwatch.gui.WindowMonitor;
 * import noaa.coastwatch.io.EarthDataReader;
 * import noaa.coastwatch.io.EarthDataReaderFactory;
 * import noaa.coastwatch.render.CoastOverlay;
 * import noaa.coastwatch.render.LatLonOverlay;
 *
 * file = args[0];
 * var = args[1];
 *
 * // Set up the view
 * reader = EarthDataReaderFactory.create (file);
 * view = EarthDataViewFactory.create (reader, var);
 * view.addOverlay (new CoastOverlay (Color.WHITE));
 * view.addOverlay (new LatLonOverlay (Color.WHITE));
 * view.resizeHeight (600);
 *
 * // Create a panel
 * panel = new EarthDataViewPanel (view);
 * panel.setPreferredSize (view.getSize (null));
 *
 * // Create a graphics frame to show
 * frame = new JFrame (file);
 * frame.setContentPane (panel);
 * frame.addWindowListener (new WindowMonitor());
 * frame.pack();
 *
 * // Show the frame onscreen
 * GUIServices.showFrame (frame);
 * </pre>
 * <p>A sample call on a NetCDF file to display SST data is as follows:</p>
 * <pre>
 * phollema$ cwgscript view.bsh ncdcOisst2Agg_7a1d_a943_b8c6.nc sst
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.4.1
 */
 
// TODO: LOGGING

public final class cwscript {

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  /** Name of program. */
  private static final String PROG = "cwscript";

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.
   */
  public static void main (String argv[]) throws EvalError, IOException {

    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
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
      usage();
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
      System.err.println (PROG + ": At least " + NARGS + " argument(s) required");
      usage();
      System.exit (1);
    } // if
    String input = cmd.getRemainingArgs()[0];

    // Run interpreter
    // ---------------
    Interpreter interpreter = new Interpreter();
    String[] scriptArgs = cmd.getRemainingArgs();
    scriptArgs = Arrays.copyOfRange (scriptArgs, 1, scriptArgs.length);
    interpreter.set ("args", scriptArgs);
    try { interpreter.source (input); }
    catch (Exception e) {
      System.err.println (PROG + ": " + e.getMessage());
    } // catch

    // Check if the script created any windows
    // ---------------------------------------
    Window[] windows = Window.getWindows();
    boolean hasWindows = (windows.length != 0);

    if (!hasWindows) System.exit (0);

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwscript input [ARGUMENTS]\n" +
"Runs a shell script written in BeanShell.\n" +
"\n" +
"Main parameters:\n" +
"  input                      The input shell script file.\n" +
"\n" +
"Options:\n" +
"  -h, --help                 Show this help message.\n" +
"  --version                  Show version information.\n"
    );
    
  } // usage

  ////////////////////////////////////////////////////////////

  private cwscript () { }

  ////////////////////////////////////////////////////////////

} // cwscript

////////////////////////////////////////////////////////////////////////
