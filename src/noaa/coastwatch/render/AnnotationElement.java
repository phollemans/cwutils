////////////////////////////////////////////////////////////////////////
/*
     FILE: AnnotationElement.java
  PURPOSE: A class to handle annotation elements such as text, shapes, 
           symbols, etc.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;

/**
 * An annotation element handles the specific information for
 * annotation of data with one symbol or string.  Elements may be
 * grouped together to compose a data annotation overlay.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class AnnotationElement { 

  ////////////////////////////////////////////////////////////

  /** 
   * Renders the element graphics.
   * 
   * @param g the graphics object for drawing.
   * @param foreground the foreground element color.
   * @param background the background element color or null for no
   * background.
   */
  public abstract void render (
    Graphics2D g,
    Color foreground,
    Color background
  );
 
  ////////////////////////////////////////////////////////////

  /**
   * Gets the element bounding area.
   *
   * @param g the graphics object for drawing.
   */
  public abstract Area getArea (
    Graphics2D g
  );

  ////////////////////////////////////////////////////////////

  /**
   * Gets the element bounding rectangle.
   *
   * @param g the graphics object for drawing.
   */
  public Rectangle getBounds (
    Graphics2D g
  ) {

    return (getArea(g).getBounds());

  } // getBounds

  ////////////////////////////////////////////////////////////

} // AnnotationElement class

////////////////////////////////////////////////////////////////////////
