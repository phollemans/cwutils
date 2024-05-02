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
        var text = "Initializing ";
        if (longName != null) {
          text += longName;
          text += " ";
        } // if
        if (version != null) {
          text += "version " + version; 
          text += " ";
        } // if
        text += "...";

        var font = g2d.getFont();
        g2d.setFont (font.deriveFont (Font.BOLD));

        int x = 36;
        int y = 250;
        float offset = 0.8f;

        g2d.setColor (Color.BLACK);
        g2d.drawString (text, x+offset, y+offset);

        g2d.setColor (Color.WHITE);
        g2d.drawString (text, x, y);

        splashScreen.update();
      } // if
    } // if

  } // updateSplash

  ////////////////////////////////////////////////////////////

} // SplashScreenManager class

////////////////////////////////////////////////////////////////////////
