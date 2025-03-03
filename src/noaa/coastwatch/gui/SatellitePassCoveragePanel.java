////////////////////////////////////////////////////////////////////////
/*

     File: SatellitePassCoveragePanel.java
   Author: Peter Hollemans
     Date: 2003/01/16

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;
import noaa.coastwatch.gui.EarthContextPanel;
import noaa.coastwatch.render.EarthContextElement;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.SatellitePassInfo;
import noaa.coastwatch.util.SolarZenith;

/**
 * The satellite pass coverage panel displays a graphical
 * representaion of a satellite pass on an annotated Earth globe.  The
 * globe may be manipulated by dragging the mouse to change the
 * projection angles.
 *
 * @see SatellitePassInfo
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class SatellitePassCoveragePanel
  extends EarthContextPanel {

  // Constants
  // ---------
  /** The daytime polygon color. */
  private static final Color DAY_COLOR = new Color (64, 64, 0);

  /** The nighttime polygon color. */
  private static final Color NIGHT_COLOR = null;

  /** The bounding box polygon color. */
  private static final Color BOX_COLOR = new Color (0, 210, 236);

  /** The foregound color. */
  private static final Color FOREGROUND_COLOR = new Color (160, 160, 160);

  /** The background color. */
  private static final Color BACKGROUND_COLOR = Color.BLACK;

  /** The current pass. */
  private SatellitePassInfo pass;

  /** The mouse motion and button handler. */
  private MouseHandler handler;

  ////////////////////////////////////////////////////////////

  /** Gets the currently displayed pass. */
  public SatellitePassInfo getPass () { return (pass); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the active satellite pass used to display pass coverage. 
   * 
   * @param pass the satellite pass information, or null for no pass
   * coverage information.
   */
  public void setPass (
    SatellitePassInfo pass
  ) { 

    // Set current pass
    // ----------------
    this.pass = pass;

    // Set context element to blank
    // ----------------------------
    if (pass == null) {
      element.setSolarZenith (null);
      element.removeAllBoundingBoxes();
    } // if

    // Modify element details
    // ----------------------
    else {
      element.setSolarZenith (new SolarZenith (pass.getDate()));
      element.removeAllBoundingBoxes();
      element.addBoundingBox (pass.getCoveragePolygon(), BOX_COLOR, null);
      element.setContextCenter (pass.getCenter());
      handler.reset();
    } // else

    // Update image
    // ------------
    repaint (getVisibleRect());

  } // setPass

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new empty satellite pass coverage panel.
   */
  public SatellitePassCoveragePanel () {

    // Initialize
    // ----------
    pass = null;
    element.setGrid (true);
    setBackground (BACKGROUND_COLOR);
    setForeground (FOREGROUND_COLOR);
    element.setSolarZenithFill (DAY_COLOR, NIGHT_COLOR);

    // Add mouse handler
    // -----------------
    handler = new MouseHandler();
    addMouseListener (handler);
    addMouseMotionListener (handler);

  } // SatellitePassCoveragePanel constructor

  ////////////////////////////////////////////////////////////

  /** Handles mouse events. */
  private class MouseHandler 
    extends MouseInputAdapter {

    // Variables
    // ---------
    /** The mouse motion mode. */
    private boolean motion;

    /** The base point for mouse motion. */
    private Point base;

    /** The context element projection center point. */
    private EarthLocation center;

    ////////////////////////////////////////////////////////

    /** Resets the handler to the context center. */
    public void reset () { center = element.getCenter(); }

    ////////////////////////////////////////////////////////
    
    /** Starts active center point adjustment. */
    public void mousePressed (
      MouseEvent e
    ) { 

      if (!motion) {
        base = e.getPoint();
        if (center == null) reset();
        motion = true; 
      } // if

    } // mousePressed

    ////////////////////////////////////////////////////////
    
    /** Adjusts the context element center point. */
    public void mouseDragged (
      MouseEvent e
    ) { 

      if (motion) {

        // Get point offset
        // ----------------
        Point point = e.getPoint();
        int dx = point.x - base.x;
        int dy = point.y - base.y;
        base = point;

        // Calculate new projection center
        // -------------------------------
        double radius = getWidth()/2.0;
        double dlat = ((double) dy/radius) * 60.0;
        double dlon = ((double) -dx/radius) * 60.0;
        center.lat += dlat;
        if (center.lat > 90) center.lat = 90;
        else if (center.lat < -90) center.lat = -90;
        center.lon += dlon;
        if (center.lon > 180) center.lon -= 360;
        else if (center.lon < -180) center.lon += 360;
        element.setContextCenter (center);

        // Repaint window
        // --------------
        repaint (getVisibleRect());

      } // if
  
    } // mouseDragged

    ////////////////////////////////////////////////////////
    
    /** Ends active center point adjustment. */
    public void mouseReleased (
      MouseEvent e
    ) { 

      if (motion) motion = false;

    } // mouseReleased

    ////////////////////////////////////////////////////////

  } // MouseHandler class

  ////////////////////////////////////////////////////////////

} // SatellitePassCoveragePanel class

////////////////////////////////////////////////////////////////////////
