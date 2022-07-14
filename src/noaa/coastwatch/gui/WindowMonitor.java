////////////////////////////////////////////////////////////////////////
/*

     File: WindowMonitor.java
   Author: Peter Hollemans
     Date: 2002/11/29

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * <p>The window monitor class performs a system exit when it receives the
 * window closing event.  This is generally only desirable if the closing
 * window is the main application window.</p>
 *
 * <p>As an example, the following code shows the window monitor being used
 * for a frame:</p>
 * <pre>
 *   JFrame frame = new JFrame();
 *   frame.addWindowListener (new WindowMonitor());
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class WindowMonitor
  extends WindowAdapter {

  public void windowClosing (WindowEvent e) { System.exit (0); }

} // WindowMonitor class

////////////////////////////////////////////////////////////////////////
