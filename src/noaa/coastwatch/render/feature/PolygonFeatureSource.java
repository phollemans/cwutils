////////////////////////////////////////////////////////////////////////
/*

     File: PolygonFeatureSource.java
   Author: Peter Hollemans
     Date: 2003/05/11

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
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.feature.LineFeatureSource;
import noaa.coastwatch.render.feature.PolygonFeature;

import java.util.logging.Logger;

/**
 * The <code>PolygonFeatureSource</code> extends the
 * <code>LineFeatureSource</code> class to render filled polygons as
 * well as lines.  Polygon features are maintained in a separate list,
 * so that line features and polygon features may be rendered
 * independently.
 *
 * @author Peter Hollemans
 * @since 3.1.4
 */
public abstract class PolygonFeatureSource
  extends LineFeatureSource {

  private static final Logger LOGGER = Logger.getLogger (PolygonFeatureSource.class.getName());
    
  // Variables
  // ---------

  /** The currently selected list of polygon features. */
  protected List<PolygonFeature> polygonList;

  ////////////////////////////////////////////////////////////

  /** Creates a new source with an empty list of polygons. */
  protected PolygonFeatureSource () { 

    polygonList = new ArrayList<>();

  } // PolygonFeatureSource constructor

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected polygon data to a graphics context.
   * This method differs from the {@link LineFeatureSource#render}
   * method in three ways:
   * <ol>
   *
   *   <li>Groups of polygons are rendered as a batch using a non-zero
   *   winding rule (see the {@link java.awt.geom.PathIterator} class
   *   for details).  This is done so that hierarchical levels of
   *   polygons (polygons contained within other polygons) whose
   *   winding orders are different are rendered correctly.</li>
   *
   *   <li>Groups of polygons that should be rendered as a batch are
   *   separated by a polygon of length zero.</li>
   *
   *   <li>Polygons that are detected to be discontinuous are not rendered,
   *   and all polygons in their group are also ignored.  This helps
   *   to avoid strange polygon rendering results.</li>
   *
   * </ol>
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting earth
   * locations to image points.
   */
  public void renderPolygons (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    // Iterate over polygons
    // ---------------------
    GeneralPath path = new GeneralPath (GeneralPath.WIND_NON_ZERO);
    int actualFillCount = 0;
    int discontinuousCount = 0;
    Iterator<PolygonFeature> iter = polygonList.iterator();

    while (iter.hasNext()) {

      // Check for end of polygon group
      // ------------------------------
      PolygonFeature polygon = iter.next();
      if (polygon.size() == 0) {
        g.fill (path);
        actualFillCount++;
        path.reset();
      } // if

      else {

        // Check for usable polygon path
        // -----------------------------
        GeneralPath polygonPath = polygon.getPath (trans);
        Point2D point = polygonPath.getCurrentPoint();
        if (point != null) {

          // Discard current group of polygons if one is discontinuous
          // ---------------------------------------------------------
          if (polygon.isDiscontinuous()) {
            discontinuousCount++;
            while (iter.hasNext()) {
              polygon = iter.next();
              if (polygon.size() == 0) break;
            } // while
            path.reset();
          } // if

          // Add polygon to path
          // -------------------
          else {
            path.append (polygonPath, false);
            path.closePath();
          } // else

        } // if

      } // else

    } // while

    // Perform final fill
    // ------------------
    g.fill (path);
    actualFillCount++;

    LOGGER.fine ("Rendered " + actualFillCount + " filled polygons");
    LOGGER.fine ("Filtered out " + discontinuousCount + " discontinuous polygons");

  } // renderPolygons

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected polygon data to a graphics context as just
   * the polygon outlines.
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting earth
   * locations to image points.
   */
  public void renderOutlines (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    int outlineCount = 0;
    
    for (Iterator<PolygonFeature> iter = polygonList.iterator(); iter.hasNext(); ) {
      PolygonFeature polygon = iter.next();
      if (polygon.getPath (trans).getCurrentPoint() != null) {
        polygon.renderOutline (g, trans);
        outlineCount++;
      } // if
    } // for

    LOGGER.fine ("Rendered " + outlineCount + " polygon outlines");
    
  } // renderOutlines

  ////////////////////////////////////////////////////////////

} // PolygonFeatureSource class

////////////////////////////////////////////////////////////////////////
