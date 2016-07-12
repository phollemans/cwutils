////////////////////////////////////////////////////////////////////////
/*
     FILE: PolygonFeatureOverlay.java
  PURPOSE: An overlay for vector polygon data.
   AUTHOR: Peter Hollemans
     DATE: 2005/03/19
  CHANGES: 2005/03/22, PFH, added transparency handling
           2006/01/18, PFH, added check for null color

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
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.IOException;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.feature.PolygonFeatureSource;
import noaa.coastwatch.render.PolygonOverlay;

/**
 * The <code>PolygonFeatureOverlay</code> class annotes a data view with
 * earth polygons from an {@link PolygonFeatureSource}.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class PolygonFeatureOverlay 
  extends PolygonOverlay {

  // Variables
  // ---------
  /** The earth vector source. */
  private PolygonFeatureSource source;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param fillColor the fill color to use for polygon fills.
   * @param source the source for polygon data.
   */
  public PolygonFeatureOverlay (
    Color color,
    int layer,
    Stroke stroke,
    Color fillColor,
    PolygonFeatureSource source
  ) { 

    super (color, layer, stroke, fillColor);
    this.source = source;

  } // PolygonFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new overlay.  The layer number is initialized to 0
   * and the stroke to the default <code>BasicStroke</code>.
   * 
   * @param color the overlay color.
   * @param source the source for polygon data.
   */
  public PolygonFeatureOverlay (
    Color color,
    PolygonFeatureSource source
  ) { 

    super (color);
    this.source = source;

  } // PolygonFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Select data from the source
    // ---------------------------
    try { source.select (view.getArea()); }
    catch (IOException e) { return; }

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Get transform
    // -------------
    EarthImageTransform trans = view.getTransform();

    // Draw filled polygons
    // --------------------
    Color fillColor = getFillColorWithAlpha();
    if (fillColor != null) {
      g.setColor (fillColor);
      source.renderPolygons (g, trans);
    } // if

    // Draw outlines
    // -------------
    Color outlineColor = getColorWithAlpha();
    if (outlineColor != null) {
      g.setColor (outlineColor);
      g.setStroke (getStroke());
      source.renderOutlines (g, trans);
    } // if

  } // draw

  ////////////////////////////////////////////////////////////

} // PolygonFeatureOverlay class

////////////////////////////////////////////////////////////////////////
