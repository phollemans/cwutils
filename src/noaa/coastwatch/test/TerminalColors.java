////////////////////////////////////////////////////////////////////////
/*

     File: TerminalColors.java
   Author: Peter Hollemans
     Date: 2016/01/01

  CoastWatch Software Library and Utilities
  Copyright (c) 2016 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.test;

/** 
 * The <code>TerminalColors</code> class is an interface that
 * hold various ANSI terminal color constants, which can be used to
 * change the color of messages printed to standard output and error.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public interface TerminalColors {

  // Constants
  // ---------

  /** ANSI escape sequence to reset terminal colors. */
  public static final String ANSI_RESET = "\u001B[0m";

  /** ANSI escape sequence for black terminal text. */
  public static final String ANSI_BLACK = "\u001B[30m";

  /** ANSI escape sequence for red terminal text. */
  public static final String ANSI_RED = "\u001B[31m";

  /** ANSI escape sequence for green terminal text. */
  public static final String ANSI_GREEN = "\u001B[32m";

  /** ANSI escape sequence for yellow terminal text. */
  public static final String ANSI_YELLOW = "\u001B[33m";

  /** ANSI escape sequence for blue terminal text. */
  public static final String ANSI_BLUE = "\u001B[34m";

  /** ANSI escape sequence for purple terminal text. */
  public static final String ANSI_PURPLE = "\u001B[35m";

  /** ANSI escape sequence for cyan terminal text. */
  public static final String ANSI_CYAN = "\u001B[36m";

  /** ANSI escape sequence for white terminal text. */
  public static final String ANSI_WHITE = "\u001B[37m";

} // TerminalColors interface

////////////////////////////////////////////////////////////////////////
