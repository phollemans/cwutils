////////////////////////////////////////////////////////////////////////
/*
     FILE: Renderable.java
  PURPOSE: A class to set up renderable image operations.
   AUTHOR: Peter Hollemans
     DATE: 2002/10/14
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
