////////////////////////////////////////////////////////////////////////
/*

     File: TestContainer.java
   Author: Peter Hollemans
     Date: 2002/12/02

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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.Container;
import java.lang.reflect.Constructor;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import noaa.coastwatch.gui.WindowMonitor;

/**
 * The test container class simply creates a <code>JFrame</code> and
 * sets the content pane to the command line specified container.  For
 * example:
 * <pre> 
 *   java noaa.coastwatch.gui.TestContainer noaa.coastwatch.gui.ProjectionChooser
 * </pre>
 *
 * @see javax.swing.JFrame
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class TestContainer {

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) {

    // Check command line
    // ------------------
    if (argv.length != 1) {
      System.out.println ("Usage: TestContainer class");
      System.exit (1);
    } // if

    // Get container class
    // -------------------
    String className = argv[0];
    Container container = null;
    try {
      Class classObj = Class.forName (className);
      Constructor constructor = classObj.getConstructor (new Class[] {});
      container = (Container) constructor.newInstance ((Object[]) null);
    } // try
    catch (Exception e) {
      e.printStackTrace();
      System.exit (2);
    } // catch

    // Show container in frame
    // -----------------------
    showFrame (container);

  } // main

  ////////////////////////////////////////////////////////////

  /** Puts the specified container into a frame and displays it. */
  public static void showFrame (
    Container container
  ) {

    // Create frame
    // ------------
    final JFrame frame = new JFrame ("TestContainer: " + 
      container.getClass().getName());
    frame.addWindowListener (new WindowMonitor());
    frame.setContentPane (container);
    frame.pack();

    // Show frame
    // ----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          GUIServices.centerOnScreen (frame);
          frame.setVisible (true);
        } // run
      });

  } // showFrame

  ////////////////////////////////////////////////////////////

} // TestContainer class

////////////////////////////////////////////////////////////////////////
