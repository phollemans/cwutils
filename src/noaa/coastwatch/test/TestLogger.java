////////////////////////////////////////////////////////////////////////
/*

     File: TestLogger.java
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

// Imports
// -------
import noaa.coastwatch.test.TerminalColors;

/**
 * The <code>TestLogger</code> object provides a singleton interface for 
 * printing testing messages.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class TestLogger implements TerminalColors {

  // Variables
  // ---------

  /** The single instance of this class. */
  private static TestLogger instance;
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates an instance of this object.
   *
   * @return the object instance.
   */
  private TestLogger () { }

  ////////////////////////////////////////////////////////////
  
  /**
   * Gets the singleton instance of this class.
   *
   * @return the singeton instance.
   */
  public static TestLogger getInstance () {
  
    if (instance == null) instance = new TestLogger();
    return (instance);
    
  } // getInstance
    
  ////////////////////////////////////////////////////////////

  /**
   * Prints a start test message for a series of class tests.
   * 
   * @param c the class that is being tested.
   */
  public void startClass (
    Class c
  ) {
  
    System.out.print (ANSI_CYAN + "*****[ " + ANSI_RESET);
    System.out.print (c.getName());
    System.out.print (ANSI_CYAN + " ]*****" + ANSI_RESET);
    System.out.println();
  
  } // startClass

  ////////////////////////////////////////////////////////////

  /**
   * Prints a start test message.
   * 
   * @param name the name of the test that is being started.
   */
  public void test (
    String name
  ) {
  
    System.out.print (ANSI_CYAN + "  TESTING --[ " + ANSI_RESET);
    System.out.print (name);
    System.out.print (ANSI_CYAN + " ]-- " + ANSI_RESET);
  
  } // test

  ////////////////////////////////////////////////////////////

  /**
   * Prints a debug message.
   * 
   * @param obj the object to print in the message.
   */
  public void debug (
    Object obj
  ) {
  
    System.out.println();
    System.out.print (ANSI_PURPLE + "    DEBUG --[ " + ANSI_RESET);
    System.out.print (obj);
    System.out.print (ANSI_PURPLE + " ]-- " + ANSI_RESET);
  
  } // debug

  ////////////////////////////////////////////////////////////

  /**
   * Prints an error message.
   * 
   * @param message the error message to print.
   */
  public void error (
    String message
  ) {
  
    System.out.print ("  " + ANSI_RED + message + ANSI_RESET);
    System.out.println();
  
  } // error

  ////////////////////////////////////////////////////////////

  /** Prints a test passed message. */
  public void passed () {
  
    System.out.print (ANSI_GREEN + "PASSED" + ANSI_RESET);
    System.out.println();
  
  } // passed

  ////////////////////////////////////////////////////////////

  /** Prints a test failed message. */
  public void failed () {
  
    System.out.print (ANSI_RED + "FAILED" + ANSI_RESET);
    System.out.println();
  
  } // passed

  ////////////////////////////////////////////////////////////

} // TestLogger class

////////////////////////////////////////////////////////////////////////
