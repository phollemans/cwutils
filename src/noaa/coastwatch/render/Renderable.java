////////////////////////////////////////////////////////////////////////
/*

     File: Renderable.java
   Author: Peter Hollemans
     Date: 2002/10/14

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
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * The renderable interface sets the methods for classes that render
 * to images or graphics contexts.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public interface Renderable {

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the rendered size for this renderable.
   * 
   * @param g the graphics context to be used for rendering.  This is
   * used to determine the total rendering size from font information
   * and so on.
   *
   * @return the total rendered dimensions.
   */
  public Dimension getSize (
    Graphics2D g
  );

  ////////////////////////////////////////////////////////////

  /**
   * Renders this renderable to the graphics context.
   *
   * @param g the graphics context for rendering.
   */
  public void render (
    Graphics2D g 
  );

  ////////////////////////////////////////////////////////////

} // Renderable interface

////////////////////////////////////////////////////////////////////////
