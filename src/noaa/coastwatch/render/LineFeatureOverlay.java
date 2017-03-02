////////////////////////////////////////////////////////////////////////
/*

     File: LineFeatureOverlay.java
   Author: Peter Hollemans
     Date: 2002/09/06

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
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.IOException;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.feature.LineFeatureSource;
import noaa.coastwatch.render.LineOverlay;

/**
 * The <code>LineFeatureOverlay</code> class annotates a data view
 * with line features from a line feature source.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class LineFeatureOverlay 
  extends LineOverlay {

  // Variables
  // ---------
  /** The earth vector source. */
  private LineFeatureSource source;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new earth vector overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param source the source for vector data.
   */
  public LineFeatureOverlay (
    Color color,
    int layer,
    Stroke stroke,
    LineFeatureSource source
  ) { 

    super (color, layer, stroke);
    this.source = source;

  } // LineFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new earth vector overlay.  The layer number is
   * initialized to 0 and the stroke to the default
   * <code>BasicStroke</code>.
   * 
   * @param color the overlay color.
   * @param source the source for vector data.
   */
  public LineFeatureOverlay (
    Color color,
    LineFeatureSource source
  ) { 

    super (color);
    this.source = source;

  } // LineFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Select data from the source
    // ---------------------------
    try { source.select (view.getArea()); }
    catch (IOException e) {
      throw new RuntimeException (e);
    } // catch

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check for null color
    // --------------------
    if (getColor() == null) return;

    // Initialize properties
    // ---------------------
    g.setColor (getColorWithAlpha());
    g.setStroke (getStroke());

    // Render the data
    // ---------------
    source.render (g, view.getTransform());

  } // draw

  ////////////////////////////////////////////////////////////

} // LineFeatureOverlay class

////////////////////////////////////////////////////////////////////////
