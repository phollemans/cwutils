////////////////////////////////////////////////////////////////////////
/*
     FILE: WindowMonitor.java
  PURPOSE: To perform basic window monitoring tasks.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/29
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

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
 * The window monitor class performs a system exit when it receives the
 * window closing event.  This is generally only desirable if the closing
 * window is the main application window.<p>
 *
 * As an example, the following code shows the window monitor being used
 * for a frame:
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
