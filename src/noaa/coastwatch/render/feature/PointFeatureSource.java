////////////////////////////////////////////////////////////////////////
/*
     FILE: PointFeatureSource.java
  PURPOSE: Supplies point feature rendering.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/22
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import noaa.coastwatch.render.feature.AbstractFeatureSource;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.PointFeatureSymbol;

/**
 * The <code>PointFeatureSource</code> class supplied and renders
 * <code>PointFeature</code> data with user-supplied plot symbols.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public abstract class PointFeatureSource 
  extends AbstractFeatureSource {

  // Variables
  // ---------
  
  /** 
   * The map of rectangles drawn by the last call to render to the point feature
   * drawn inside the rectangle.
   */
  private Map<Rectangle, PointFeature> rectToFeatureMap = new LinkedHashMap<Rectangle, PointFeature>();

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected point feature data to a graphics context.
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting Earth
   * locations to image points.
   * @param symbol the symbol to use for rendering each point feature.
   */
  public void render (
    Graphics2D g,
    EarthImageTransform trans,
    PointFeatureSymbol symbol
  ) {

    // Create or clear rectangle map
    // -----------------------------
    rectToFeatureMap.clear();

    // Loop over each point feature
    // ----------------------------
    for (Feature feature : this) {
      PointFeature pointFeature = (PointFeature) feature;
      Point2D point = trans.transform (pointFeature.getPoint());
      if (point != null) {
      
        // Draw symbol
        // -----------
        symbol.setFeature (pointFeature);
        Point center = new Point (
          (int) Math.round (point.getX()),
          (int) Math.round (point.getY())
        );
        symbol.draw (g, center.x, center.y);

        // Save rectangle
        // --------------
        int size = symbol.getSize();
        Rectangle rect = new Rectangle (center.x - size/2, center.y - size/2, size, size);
        rectToFeatureMap.put (rect, pointFeature);

      } // if
    } // for

  } // render

  ////////////////////////////////////////////////////////////

  /**
   * Gets the feature drawn in the last call to render whose symbol's 
   * rectangular extents contain the specified point.
   *
   * @param point the point for the feature query.
   *
   * @return the feature or null if no feature was drawn at the specified 
   * point. If more than one feature matches the point, the first feature 
   * rendered will be returned.
   */
  public PointFeature getFeatureAtPoint (
    Point point
  ) {
    
    PointFeature result = null;
    
    // Loop over all symbol rectangles
    // -------------------------------
    for (Rectangle rect : rectToFeatureMap.keySet()) {
      if (rect.contains (point)) {
        result = rectToFeatureMap.get (rect);
        break;
      } // if
    } // for

    return (result);

  } // getFeatureAtPoint

  ////////////////////////////////////////////////////////////

} // PointFeatureSource class

////////////////////////////////////////////////////////////////////////
