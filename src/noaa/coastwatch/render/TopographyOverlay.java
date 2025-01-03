////////////////////////////////////////////////////////////////////////
/*

     File: TopographyOverlay.java
   Author: Peter Hollemans
     Date: 2004/03/04

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

// TODO: Something goes wrong when two topography overlays are
// simultaneously being editted graphically, parts of one show up
// in the other.  Possibly a cloning issue?  Difficult to
// duplicate/reproduce.

// TODO: Another problem, when contouring near 0 E, the rendering
// routine goes into an infinite loop.

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import noaa.coastwatch.render.ContourGenerator;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.LineOverlay;
import noaa.coastwatch.util.EarthArea;

/**
 * A topography overlay annotates an earth data view with topography
 * lines.  The topography lines are rendered from ETOP05 elevation
 * data using the {@link noaa.coastwatch.render.ContourGenerator}
 * class.  ETOP05 data has a spatial resolution of 5 minutes and a
 * digitization accuracy of 1 meter.  Information on ETOP05 may be
 * found at:
 * <blockquote>
 *   http://www.ngdc.noaa.gov/mgg/global/global.html<br>
 *   http://sis.agr.gc.ca/cansis/nsdb/ecostrat/elevation.html
 * </blockquote>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class TopographyOverlay 
  extends LineOverlay {
 
  // Constants
  // ---------

  /** The default topography contour levels. */
  public static final int[] TOPO_LEVELS = new int[] {
    200, 500, 1000, 2000, 3000};

  /** The default bathymetry contour levels. */
  public static final int[] BATH_LEVELS = new int[] {-2000, -200};

  /** The topography digitization accuracy in meters. */
  private static final double TOPOGRAPHY_ACCURACY = 1.0;

  // Variables
  // ---------

  /** The contour generator used for topography lines. */
  private transient ContourGenerator topo;

  /** The contour levels for contouring. */
  private int[] levels;

  ////////////////////////////////////////////////////////////

  /** Reads the object data from the input stream. */
  private void readObject (
    ObjectInputStream in
  ) throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    topo = getSource();
    setTopoLevels (levels);

  } // readObject

  ////////////////////////////////////////////////////////////

  /** Gets the topography levels. */
  public int[] getLevels () { return (levels); }

  ////////////////////////////////////////////////////////////

  /** Sets the levels in the contour generator. */
  private void setTopoLevels (int[] levels) {

    double[] dblLevels = new double[levels.length];
    for (int i = 0; i < levels.length; i++)
      dblLevels[i] = levels[i];
    topo.setLevels (dblLevels); 

  } // setTopoLevels

  ////////////////////////////////////////////////////////////

  /** Sets the topography levels. */
  public void setLevels (int[] levels) { 

    if (!Arrays.equals (this.levels, levels)) {
      this.levels = levels;
      prepared = false;
      if (topo != null) setTopoLevels (levels);
    } // if

  } // setLevels

  ////////////////////////////////////////////////////////////

  /** Gets the source for topographic contours. */
  private ContourGenerator getSource () throws IOException {

    // Get an instance of the topography data
    var topo = Topography.getInstance();

    // Create the contour generator
    ContourGenerator generator = new ContourGenerator (topo.getElevation(), topo.getTransform());
    generator.setLevelNudge (TOPOGRAPHY_ACCURACY/2);
    return (generator);

  } // getSource

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new topography overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   *
   * @throws IOException if a problem occurs accessing the topography data.
   */
  public TopographyOverlay (
    Color color,
    int layer,
    Stroke stroke
  ) throws IOException { 

    // Initialize
    // ----------
    super (color, layer, stroke);
    topo = getSource();

  } // TopographyOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new topography overlay.  The layer number is
   * initialized to 0, and the stroke to the default
   * <code>BasicStroke</code>.
   *
   * @param color the overlay color.
   *
   * @throws IOException if a problem occurs accessing the topography data.
   */
  public TopographyOverlay (
    Color color
  ) throws IOException { 

    super (color);
    topo = getSource();

  } // TopographyOverlay constructor

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check if topographic contour regeneration required
    // --------------------------------------------------
    EarthArea viewArea = view.getArea();
    EarthArea selectedArea = topo.getArea();
    if (viewArea.equals (selectedArea) && prepared) return;

    // Select topographic data
    // -----------------------
    try { topo.select (viewArea); }
    catch (IOException e) { e.printStackTrace(); }

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check for null color
    // --------------------
    if (getColor() == null) return;

    // Draw topography lines
    // ---------------------
    g.setColor (getColorWithAlpha());
    g.setStroke (getStroke());
    topo.render (g, view.getTransform());

  } // draw

  ////////////////////////////////////////////////////////////

} // TopographyOverlay class

////////////////////////////////////////////////////////////////////////
