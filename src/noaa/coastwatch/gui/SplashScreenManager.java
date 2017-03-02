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
   * @param longName the name of the tool.
   */
  public static void updateSplash (
    String longName
  ) {

    SplashScreen splashScreen = SplashScreen.getSplashScreen();
    if (splashScreen != null) {
      Graphics2D g = splashScreen.createGraphics();
      if (g != null) {
        g.setPaintMode();
        g.setColor (Color.WHITE);
        g.drawString (longName, 176, 178);
        g.drawString ("Initializing ...", 176, 195);
        splashScreen.update();
      } // if
    } // if

  } // updateSplash

  ////////////////////////////////////////////////////////////

} // SplashScreenManager class

////////////////////////////////////////////////////////////////////////
