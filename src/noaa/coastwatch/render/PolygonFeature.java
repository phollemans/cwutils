////////////////////////////////////////////////////////////////////////
/*
     FILE: PolygonFeature.java
  PURPOSE: A class for holding polygons of Eath location data.
   AUTHOR: Peter Hollemans
     DATE: 2003/05/09
  CHANGES: 2003/11/22, PFH, added more Javadoc
           2005/03/21, PFH, added renderOutline()
           2005/04/29, PFH, modified to extend LineFeature

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import noaa.coastwatch.util.*;

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
   * @param trans the Earth image transform for converting Earth
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
   * @param trans the Earth image transform for converting Earth
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
