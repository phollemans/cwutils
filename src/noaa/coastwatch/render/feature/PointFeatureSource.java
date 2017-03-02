////////////////////////////////////////////////////////////////////////
/*

     File: PointFeatureSource.java
   Author: Peter Hollemans
     Date: 2005/05/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Rectangle;

import java.util.Iterator;
import java.util.Map;

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

    render (g, trans, symbol, null);

  } // render

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected point feature data to a graphics context.
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting Earth
   * locations to image points.
   * @param symbol the symbol to use for rendering each point feature.
   * @param rectToFeatureMap the map of rectangle to point feature for the
   * rendering operation, or null to not save this information.  This map 
   * can later be used to easily recall what point features were drawn where.
   */
  public void render (
    Graphics2D g,
    EarthImageTransform trans,
    PointFeatureSymbol symbol,
    Map<Rectangle, PointFeature> rectToFeatureMap
  ) {

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
        if (rectToFeatureMap != null) {
          int size = symbol.getSize();
          Rectangle rect = new Rectangle (center.x - size/2, center.y - size/2, size, size);
          rectToFeatureMap.put (rect, pointFeature);
        } // if

      } // if
    } // for

  } // render

  ////////////////////////////////////////////////////////////

} // PointFeatureSource class

////////////////////////////////////////////////////////////////////////
