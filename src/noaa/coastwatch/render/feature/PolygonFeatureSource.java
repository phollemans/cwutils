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

  // Variables
  // ---------

  /** The currently selected list of polygon features. */
  protected List polygonList;

  ////////////////////////////////////////////////////////////

  /** Creates a new source with an empty list of polygons. */
  protected PolygonFeatureSource () { 

    polygonList = new ArrayList();

  } // PolygonFeatureSource constructor

  ////////////////////////////////////////////////////////////

  /** Gets an iterator over the polygon features. */
  public Iterator polygonIterator () { return (polygonList.iterator()); }

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected polygon data to a graphics context.
   * This method differs from the {@link LineFeatureSource#render}
   * method in three ways:
   * <ol>
   *
   *   <li>Groups of polygons are rendered as a batch using a non-zero
   *   winding rule (see the {@link java.awt.geom.GeneralPath} class
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
   * @param trans the earth image transform for converting Earth
   * locations to image points.
   */
  public void renderPolygons (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    // Loop over each polygon
    // ----------------------
    GeneralPath path = new GeneralPath (GeneralPath.WIND_NON_ZERO);
    for (Iterator iter = polygonIterator(); iter.hasNext(); ) {
      PolygonFeature polygon = (PolygonFeature) iter.next();

      // Flush path to graphics
      // ----------------------
      if (polygon.size() == 0) {
        g.fill (path);
        path = new GeneralPath (GeneralPath.WIND_NON_ZERO);
        continue;
      } // if

      // Check for degenerate polygon path
      // ---------------------------------
      GeneralPath polygonPath = polygon.getPath (trans);
      Point2D point = polygonPath.getCurrentPoint();
      if (point == null) continue;

      // Check for discontinuous polygon
      // -------------------------------
      if (polygon.isDiscontinuous()) {
        do {
          polygon = (PolygonFeature) iter.next();
        } while (polygon.size() != 0);
        path = new GeneralPath (GeneralPath.WIND_NON_ZERO);
        continue;
      } // if

      // Add polygon to path
      // -------------------
      path.append (polygon.getPath (trans), false);
      path.closePath();

    } // for

    // Perform final fill
    // ------------------
    g.fill (path);

  } // renderPolygons

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected polygon data to a graphics context as just
   * the polygons outlines.
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting Earth
   * locations to image points.
   */
  public void renderOutlines (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    for (Iterator iter = polygonList.iterator(); iter.hasNext(); )
      ((PolygonFeature) iter.next()).renderOutline (g, trans);

  } // renderOutlines

  ////////////////////////////////////////////////////////////

} // PolygonFeatureSource class

////////////////////////////////////////////////////////////////////////