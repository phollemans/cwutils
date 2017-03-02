////////////////////////////////////////////////////////////////////////
/*

     File: LatLonOverlay.java
   Author: Peter Hollemans
     Date: 2002/07/25

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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.util.Iterator;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.LabeledLineOverlay;
import noaa.coastwatch.render.lines.LineCollection;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>LatLonOverlay</code> class annotates an {@link
 * EarthDataView} view with latitude/longitude grid lines and
 * labels.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class LatLonOverlay 
  extends LabeledLineOverlay {

  // Constants
  // ---------

  /** The serialization constant. */
  private static final long serialVersionUID = -6424130357334364832L;

  /** The allowed automatic grid increment values. */
  private static final int[] GRID_INCS = {1, 2, 5, 10, 15, 20, 30};

  /** The default number of desired grid lines. */
  private final static int GRID_LINES = 5;

  // Variables
  // ---------

  /** The grid increment in degrees. */
  private int inc;

  /** The manual grid increment flag. */
  private boolean manualIncrement;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the manual grid increment mode flag.  When off, the grid
   * increment is calculated automatically at rendering time for an
   * optimal number of grid lines.
   */
  public void setManualIncrement (boolean flag) { 

    if (manualIncrement != flag) {
      manualIncrement = flag; 
      prepared = false;
    } // if

  } // setManualIncrement

  ////////////////////////////////////////////////////////////

  /** Gets the manual grid increment mode flag. */
  public boolean getManualIncrement () { return (manualIncrement); }

  ////////////////////////////////////////////////////////////

  /** Sets the grid increment in degrees. */
  public void setIncrement (int inc) { 

    if (this.inc != inc) {
      this.inc = inc; 
      if (manualIncrement) prepared = false;
    } // if

  } // setIncrement

  ////////////////////////////////////////////////////////////

  /** Gets the current grid increment value in degrees. */
  public int getIncrement () { return (inc); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new grid overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param manualIncrement the manual increment flag.
   * @param inc the latitude and longitude grid increment in degrees.
   * @param drawLabels the grid labels flag, true if grid labels
   * should be drawn.
   * @param font the grid labels font.  The labels font may be
   * null if no labels are to be drawn.
   */
  public LatLonOverlay (
    Color color,
    int layer,
    Stroke stroke,
    boolean manualIncrement,
    int inc,
    boolean drawLabels,
    Font font
  ) { 

    super (color, layer, stroke, drawLabels, font);
    this.manualIncrement = manualIncrement;
    this.inc = inc;

  } // LatLonOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new grid overlay.  The layer number is
   * initialized to 0, the stroke to the default
   * <code>BasicStroke</code>, the grid increment mode to
   * automatic, labels to true, and the font to the default font
   * face, plain style, 12 point.
   *
   * @param color the overlay color.
   */
  public LatLonOverlay (
    Color color
  ) { 

    super (color);
    this.manualIncrement = false;
    this.inc = 1;

  } // LatLonOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Computes a grid increment value based on the earth area for
   * grid line annotation.
   *
   * @param area the earth area for grid lines.
   */
  private int getIncrementValue (
    EarthArea area
  ) {

    // Get maximum geographic extents
    // ------------------------------
    int[] extremes = area.getExtremes();
    int extent = Math.min (extremes[0] - extremes[1], 
      extremes[2] - extremes[3]);

    // Search for the best increment
    // -----------------------------
    double lines = (double) extent / GRID_INCS[0];
    int index = 0;
    for (int i = 1; i < GRID_INCS.length; i++) {
      double newLines = (double) extent / GRID_INCS[i];
      if (Math.abs (newLines - GRID_LINES) < Math.abs (lines - GRID_LINES)) { 
        lines = newLines;
        index = i;
      } // if
    } // for

    return (GRID_INCS[index]);

  } // getIncrementValue

  ////////////////////////////////////////////////////////////

  @Override
  protected LineCollection getLines (
    EarthDataView view
  ) {
 
    // Compute grid increment
    // ----------------------
    EarthArea area = view.getArea();
    int gridInc = (!manualIncrement ? getIncrementValue (area) : inc);

    // Create grid lines
    // -----------------
    EarthImageTransform trans = view.getTransform();
    Datum datum = trans.getEarthTransform().getDatum();
    Iterator iter = area.getIterator();
    LineCollection lines = new LineCollection();
    while (iter.hasNext()) {
      int[] square = (int[]) iter.next();
      EarthLocation corner = new EarthLocation (square[0], square[1], datum);
      if (square[0]%gridInc == 0) {
        lines.addSegment (EarthLocation.formatSingle (square[0], 
          EarthLocation.D, EarthLocation.LAT).replaceFirst (" ", "\u00b0"), 
          trans, corner, corner.translate (0,1));
      } // if
      if (square[1]%gridInc == 0) {
        lines.addSegment (EarthLocation.formatSingle (square[1], 
          EarthLocation.D, EarthLocation.LON).replaceFirst (" ", "\u00b0"), 
          trans, corner, corner.translate (1,0));
      } // if
    } // while

    return (lines);

  } // getLines

  ////////////////////////////////////////////////////////////

} // LatLonOverlay class

////////////////////////////////////////////////////////////////////////
