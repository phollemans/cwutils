////////////////////////////////////////////////////////////////////////
/*
     FILE: LatLonOverlay.java
  PURPOSE: An overlay for latitude/longitude grid lines and labels
   AUTHOR: Peter Hollemans
     DATE: 2002/07/25
  CHANGES: 2002/09/04, PFH, rearranged
           2002/10/23, PFH, added label shadows
           2002/11/29, PFH, added prepare, draw
           2002/12/12, PFH, modified grid line rendering for discontinuities
           2003/01/14, PFH, added offset constant for line labels
           2004/03/07, PFH, added various get methods
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/04/04, PFH, added serialization
           2004/08/30, PFH, added readObject() to correct problems
             when deserializing font information on different
             operating systems
           2005/02/07, PFH, modified to use EarthLocation.formatSingle()
           2005/03/21, PFH, added transparency handling
           2005/04/03, PFH, increased label offset
           2005/05/18, PFH, modified to generate EarthLocations in view datum
           2006/01/13, PFH, added check for null color
           2006/04/10, PFH, changed grid line drawing to non-antialiased lines
           2006/11/19, PFH, added light text halo for dark foreground colors
           2006/11/20, PFH, modified rendering method for shadows
           2006/12/21, PFH, moved some functionality to LabeledLineOverlay

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

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
