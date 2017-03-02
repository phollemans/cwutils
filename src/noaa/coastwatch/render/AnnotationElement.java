////////////////////////////////////////////////////////////////////////
/*

     File: AnnotationElement.java
   Author: Peter Hollemans
     Date: 2002/09/15

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
import java.awt.Rectangle;
import java.awt.geom.Area;

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
