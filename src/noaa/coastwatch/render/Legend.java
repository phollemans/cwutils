////////////////////////////////////////////////////////////////////////
/*
     FILE: Legend.java
  PURPOSE: A class to set up the functionality of legend classes.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/26
  CHANGES: 2004/10/08, PFH, added DEFAULT_STROKE 

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;

/**
 * A legend is used to annotate data with descriptive elements.  A
 * legend may have a desired size, font, and color.  The actual size
 * of the legend may vary from the desired size.
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
  /** The preferred dimensions. */
  protected Dimension preferred;

  /** The legend font. */
  protected Font font;

  /** The legend foreground and background colors. */
  protected Color fore, back;

  ////////////////////////////////////////////////////////////

  /** Sets the color to be used for drawing legend elements. */
  public void setForeground (Color color) { this.fore = color; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the color to be used for drawing the legend background.  If
   * the background color is null, no background is drawn.
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

    this.preferred = (size == null ? null : (Dimension) size.clone());

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

  /** Gets the actual rendered legend size. */
  public abstract Dimension getSize (
    Graphics2D g
  );

  ////////////////////////////////////////////////////////////

  /**
   * Sets the legend font.  If null, the font is set to the default
   * font face, plain style, 12 point.
   */
  public void setFont (
    Font font
  ) {

    this.font = (font == null ? new Font (null, Font.PLAIN, 12) : font);

  } // setFont

  ////////////////////////////////////////////////////////////

  /** Creates a new legend with the specified size, font, and colors. */
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
