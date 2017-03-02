////////////////////////////////////////////////////////////////////////
/*

     File: PolygonFeature.java
   Author: Peter Hollemans
     Date: 2003/05/09

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
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.feature.LineFeature;

/**
 * The <code>PolygonFeature</code> class is a {@link LineFeature} with
 * extra properties and rendering algorithms specific to polygons.
 * Polygons are rendered as filled where as lines are rendered as a
 * series of segments only.  Polygons have an inherent winding
 * direction which may be used to determine what is inside the polygon
 * and what is outside.  The winding direction is not used by the
 * rendering algorithm, but rather is a convenience for the user so
 * that polygons of different winding directions may be grouped
 * together but treated differently depending on their direction.
 *
 * @author Peter Hollemans
 * @since 3.1.4
 */
public class PolygonFeature 
  extends LineFeature {

  // Constants
  // ---------
  /** The clockwise winding direction. */
  public static final int CLOCKWISE = 0;

  /** The counter-clockwise winding direction. */
  public static final int COUNTER_CLOCKWISE = 1;

  // Variables
  // ---------
  /** The winding direction. */
  private int direction;

  ////////////////////////////////////////////////////////////

  /** Gets the winding direction. */
  public int getDirection () { return (direction); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new empty polygon feature with no attributes.
   *
   * @param direction the winding direction, either clockwise or
   * counter-clockwise.
   */
  public PolygonFeature (
    int direction
  ) { 

    this.direction = direction;

  } // PolygonFeature

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new empty polygon feature with attributes.
   *
   * @param direction the winding direction, either clockwise or
   * counter-clockwise.
   * @param attributeArray the array of feature attributes.
   */
  public PolygonFeature (
    int direction,
    Object[] attributeArray
  ) { 

    super (attributeArray);
    this.direction = direction;

  } // PolygonFeature

  ////////////////////////////////////////////////////////////

  /**
   * Renders the outline of this polygon feature to a graphics
   * context.  This method allows the user to render the polygon as if
   * it was line data with no filling applied.  It simply calls the
   * overridden {@link LineFeature#render} method.
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting Earth
   * locations to image points.
   */
  public void renderOutline (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    super.render (g, trans);

  } // renderOutline

  ////////////////////////////////////////////////////////////

  /**
   * Renders this polygon feature to a graphics context.
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting Earth
   * locations to image points.
   */
  public void render (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    GeneralPath path = getPath (trans);
    g.fill (path);

  } // render

  ////////////////////////////////////////////////////////////

} // PolygonFeature class

////////////////////////////////////////////////////////////////////////
