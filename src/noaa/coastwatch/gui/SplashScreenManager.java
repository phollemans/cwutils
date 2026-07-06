////////////////////////////////////////////////////////////////////////
/*

     File: SplashScreenManager.java
   Author: Peter Hollemans
     Date: 2014/09/11

  CoastWatch Software Library and Utilities
  Copyright (c) 2014 National Oceanic and Atmospheric Administration
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.SplashScreen;
import java.awt.Font;

/**
 * The <code>SplashScreenManager</code> class handles updates to the 
 * JVM-generated startup splash screen.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class SplashScreenManager {

  ////////////////////////////////////////////////////////////

  /** The default test tool name. */
  private static final String TEST_LONG_NAME = "CoastWatch Utilities";

  /** The default test version. */
  private static final String TEST_VERSION = "test mode";

  /** The default time to show the test splash in milliseconds. */
  private static final long TEST_DELAY = 5000;

  ////////////////////////////////////////////////////////////
  
  /** 
   * Updates the Java splash screen if present.  The screen is updated
   * with the name of tool and the line "Initializing ...".
   *
   * @param longName the name of the tool or null.
   */
  public static void updateSplash (
    String longName
  ) {

    updateSplash (longName, null);

  } // updateSplash

  ////////////////////////////////////////////////////////////
  
  /** 
   * Updates the Java splash screen if present.  The screen is updated
   * with the name of tool and the line "Initializing ...".
   *
   * @param longName the name of the tool or null.
   * @param version the version of the tool or null.
   * 
   * @since 3.8.1
   */
  public static void updateSplash (
    String longName,
    String version
  ) {

    SplashScreen splashScreen = SplashScreen.getSplashScreen();
    if (splashScreen != null) {
      Graphics2D g2d = splashScreen.createGraphics();
      if (g2d != null) {

        g2d.setPaintMode();
        String initText = "Initializing ";
        if (longName != null) {
          initText += longName;
          initText += " ";
        } // if
        initText += "...";

        var font = g2d.getFont();
//        g2d.setFont (font.deriveFont (Font.BOLD));

        int x = 23;
        int y = 213;
        float offset = 0.6f;
        g2d.setColor (new Color (0, 0, 0, 128));
        g2d.drawString (initText, x+offset, y+offset);
        g2d.setColor (Color.WHITE);
        g2d.drawString (initText, x, y);

        if (version != null) {
          y += (int) (font.getSize() * 1.25);
          var versionText = "Version " + version; 
          g2d.setColor (new Color (0, 0, 0, 128));
          g2d.drawString (versionText, x+offset, y+offset);
          g2d.setColor (Color.WHITE);
          g2d.drawString (versionText, x, y);
        } // if

        splashScreen.update();

      } // if
    } // if

  } // updateSplash

  ////////////////////////////////////////////////////////////

  /**
   * Tests the splash screen update.  The JVM must be started with
   * {@code -splash:<image>} for the test splash to be visible.
   *
   * @param argv the command line parameters.
   *
   * @throws InterruptedException if interrupted while pausing.
   *
   * @since 4.2.0
   */
  public static void main (
    String[] argv
  ) throws InterruptedException {

    String longName = (argv.length >= 1 ? argv[0] : TEST_LONG_NAME);
    String version = (argv.length >= 2 ? argv[1] : TEST_VERSION);
    long delay = (argv.length >= 3 ? Long.parseLong (argv[2]) : TEST_DELAY);

    updateSplash (longName, version);
    Thread.sleep (delay);

  } // main

  ////////////////////////////////////////////////////////////

} // SplashScreenManager class

////////////////////////////////////////////////////////////////////////
