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
 *   <li> Invalid command line option. </li>
 *   <li> Invalid input file name. </li>
 *   <li> Error encountered running the shell script. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p>
 *
 * </p>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.4.1
 */
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
    System.exit (0);

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
