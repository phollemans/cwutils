////////////////////////////////////////////////////////////////////////
/*

     File: ToolSplashWindow.java
   Author: Peter Hollemans
     Date: 2004/01/08

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import noaa.coastwatch.tools.ToolServices;

/**
 * The tool splash window class displays a borderless graphic window
 * to inform the user that the application is loading.  A number of
 * text messages are displayed on top of the splash graphic to give
 * details such as the software package name, website, version, and so
 * on.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 *
 * @deprecated As of 3.3.1, use {@link SplashScreenManager}.
 */
@Deprecated
public class ToolSplashWindow 
  extends JWindow {

  // Constants
  // ---------
  /** The splash image filename. */
  private static final String SPLASH_IMAGE = "cwf_splash.png";

  /** The annotation text position. */
  private static final int ANNOTATION_POSITION = 132;

  /** The annotation font size. */
  private static final int FONT_SIZE = 9;

  /** The splash pause time in milliseconds. */
  private static final int SPLASH_PAUSE = 3000;

  // Variables
  // ---------
  /** An instance of the splash screen. */
  private static ToolSplashWindow splash = null;

  ////////////////////////////////////////////////////////////

  /** Pauses for the splash screen. */
  public static void pauseSplash () {

    try { Thread.sleep (SPLASH_PAUSE); }
    catch (Exception e) { }

  } // pauseSplash

  ////////////////////////////////////////////////////////////

  /** 
   * Creates and shows an instance of a splash window.
   * 
   * @param app the application name.
   */
  public static void showSplash (
    String app
  ) {

    splash = new ToolSplashWindow (app);
    splash.setVisible (true);

  } // showSplash

  ////////////////////////////////////////////////////////////

  /** Hides the splash window instance if it exists. */
  public static void hideSplash () {

    if (splash != null) {
      splash.setVisible (false);
      splash = null;
    } // if

  } // hideSplash

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new splash window.
   * 
   * @param app the application name.
   */
  public ToolSplashWindow (
    String app
  ) {

    // Set background image
    // --------------------
    ImageIcon image = new ImageIcon (getClass().getResource (SPLASH_IMAGE));
    JLabel imageLabel = new JLabel (image);
    getContentPane().add (imageLabel, BorderLayout.CENTER);

    // Set location on screen
    // ----------------------
    pack();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension labelSize = imageLabel.getPreferredSize();
    setLocation (screenSize.width/2 - (labelSize.width/2),
      screenSize.height/2 - (labelSize.height/2));

    // Create annotation label
    // -----------------------
    String annotationText = ToolServices.getSplash (app);
    annotationText = annotationText.replaceAll ("\n", "<br>");
    annotationText = "<html>" + annotationText + "</html>";
    JLabel annotationLabel = new JLabel (annotationText);
    annotationLabel.setForeground (Color.white);
    annotationLabel.setFont (new Font (null, Font.PLAIN, FONT_SIZE));

    // Add annotation to glass pane
    // ----------------------------
    JPanel glass = (JPanel) getGlassPane();
    glass.setVisible (true);
    glass.setLayout (null);
    Dimension annotationSize = annotationLabel.getPreferredSize();
    annotationLabel.setSize (annotationSize);
    annotationLabel.setLocation (ANNOTATION_POSITION,
      labelSize.height - annotationSize.height - FONT_SIZE);
    glass.add (annotationLabel);

  } // ToolSplashWindow

  ////////////////////////////////////////////////////////////

  /** Creates a new splash window in a test mode. */
  public static void main (
    String[] argv
  ) {

    new ToolSplashWindow ("test mode").setVisible (true);

  } // main

  ////////////////////////////////////////////////////////////

} // ToolSplashWindow class

////////////////////////////////////////////////////////////////////////
