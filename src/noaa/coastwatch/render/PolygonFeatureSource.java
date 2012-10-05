////////////////////////////////////////////////////////////////////////
/*
     FILE: PolygonFeatureSource.java
  PURPOSE: An abstract class for Earth polygon data source methods.
   AUTHOR: Peter Hollemans
     DATE: 2003/05/11
  CHANGES: 2003/05/22, PFH, added test for degenerate polygon path
           2003/12/10, PFH, changed class name from PolygonFeatureReader
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2005/03/21, PFH
           - added more docs for renderPolygons()
           - added renderOutlines()
           2005/05/03, PFH, modified for changes in LineFeatureSource
           2005/05/27, PFH, changed disjoint to discontinuous

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;

/**
 * The <code>PolygonFeatureSource</code> extends the
 * <code>LineFeatureSource</code> class to render filled polygons as
 * well as lines.  Polygon features are maintained in a separate list,
 * so that line features and polygon features may be rendered
 * independently.
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
   * @param trans the Earth image transform for converting Earth
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
   * @param trans the Earth image transform for converting Earth
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
