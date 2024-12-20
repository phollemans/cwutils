////////////////////////////////////////////////////////////////////////
/*

     File: Legend.java
   Author: Peter Hollemans
     Date: 2002/09/26

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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * A legend is used to annotate data with descriptive elements.  A
 * legend may have a desired size, font, and color.  The actual size
 * of the legend may vary from the desired size.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class Legend {

  // Constants
  // ---------
  
  /** The default space size between legend elements. */
  public static final int SPACE_SIZE = 5;

  /** The default stroke for line drawing. */
  public static final Stroke DEFAULT_STROKE = 
    new BasicStroke (1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);

  // Variables
  // ---------
  
  /** The preferred dimensions for the legend. */
  protected Dimension preferredSize;

  /** The legend font. */
  protected Font font;

  /** The legend foreground and background colors. */
  protected Color fore, back;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the foreground color.
   *
   * @param color the foreground color to be used for drawing legend 
   * elements.
   */
  public void setForeground (Color color) { this.fore = color; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the background color.
   *
   * @param color the background colour to be used for drawing the legend 
   * background, or null if no background is to be drawn.
   */
  public void setBackground (Color color) { this.back = color; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the preferred size of the legend.  The actual size of the
   * legend may or may not match the preferred size.  Use {@link
   * #getSize} to determine the actual rendered size.  If the
   * preferred size is null, then the class determines an optimal
   * size.
   *
   * @param size the preferred legend size, or null for none.
   */
  public void setPreferredSize (
    Dimension size
  ) {

    this.preferredSize = (size == null ? null : (Dimension) size.clone());

  } // setPreferredSize 

  ////////////////////////////////////////////////////////////

  /**
   * Renders the legend at the specified coordinates in the graphics 
   * device.
   *
   * @param g the graphics device for rendering.
   * @param x the x coordinate of the top-left corner. 
   * @param y the y coordinate of the top-left corner.
   */
  public abstract void render (
    Graphics2D g,
    int x,
    int y
  );

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the actual required legend size.
   *
   * @param g the graphics device that the legend will be rendered on.
   */
  public abstract Dimension getSize (
    Graphics2D g
  );

  ////////////////////////////////////////////////////////////

  /**
   * Sets the legend font.
   *
   * @param font the legend font, or null to use the default font face, 
   * plain style, 12 point.
   */
  public void setFont (
    Font font
  ) {

    this.font = (font == null ? new Font (null, Font.PLAIN, 12) : font);

  } // setFont

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new legend with the specified properties.
   *
   * @param dim the dimensions for the legend, or null to detect the size
   * of the legend from the subclass {@link #getSize} method.
   * @param font the font the use for the legend text.
   * @param fore the forground color to use for text and lines.
   * @param back the background colour for filling behind the legend contents.
   */
  protected Legend (
    Dimension dim,
    Font font,
    Color fore,
    Color back
  ) {

    setPreferredSize (dim);
    setFont (font);
    setForeground (fore);
    setBackground (back);

  } // Legend constructor

  ////////////////////////////////////////////////////////////

} // Legend class

////////////////////////////////////////////////////////////////////////
