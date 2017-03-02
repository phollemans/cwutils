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
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";

} // TerminalColors interface

////////////////////////////////////////////////////////////////////////
