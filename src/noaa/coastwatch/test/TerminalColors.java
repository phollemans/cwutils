////////////////////////////////////////////////////////////////////////
/*
     FILE: TerminalColors.java
  PURPOSE: Hold various ANSI terminal color constants.
   AUTHOR: Peter Hollemans
     DATE: 2016/01/01
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2016, USDOC/NOAA/NESDIS CoastWatch

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
