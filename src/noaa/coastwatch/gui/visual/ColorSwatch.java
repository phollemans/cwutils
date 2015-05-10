////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorSwatch.java
  PURPOSE: An icon showing a cwatch of color.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/23
  CHANGES: 2006/03/19, PFH, modified rectangle painting for more consistent
             behaviour across different look and feels
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * The <code>ColorSwatch</code> class is an icon that shows a
 * rectangle of a given color.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ColorSwatch 
  implements Icon { 

  // Variables
  // ---------

  /** The swatch color. */
  private Color color;

  /** The icon width. */
  private int width;

  /** The icon height. */
  private int height;

  ////////////////////////////////////////////////////////////

  /** 
   * Create a new color swatch. 
   * 
   * @param color the color to display.
   * @param width the icon width.
   * @param height the icon height.
   */
  public ColorSwatch (
    Color color,
    int width,
    int height
  ) {

    this.color = color;
    this.width = width;
    this.height = height;

  } // ColorSwatch

  ////////////////////////////////////////////////////////////
  
  /** Gets the icon width. */
  public int getIconWidth () { return (width); }

  ////////////////////////////////////////////////////////////

  /** Gets the icon height. */
  public int getIconHeight() { return (height); }
  
  ////////////////////////////////////////////////////////////

  /** Paints the icon to the specified graphics context. */
  public void paintIcon (Component c, Graphics g, int x, int y) { 

    // Paint null color
    // ----------------
    if (color == null) {
      g.setColor (c.getBackground());
      g.fillRect (x, y, width, height);
      g.setColor (c.getForeground());
      g.drawLine (x+width-1, y, x, y+height-1);
    } // if

    // Paint filled rectangle
    // ----------------------
    else {
      g.setColor (color);
      g.fillRect (x, y, width, height);
    } // else

    // Paint frame
    // -----------
    g.setColor (c.getForeground());
    g.drawLine (x, y, x+width-1, y);
    g.drawLine (x+width-1, y, x+width-1, y+width-1);
    g.drawLine (x+width-1, y+width-1, x, y+width-1);
    g.drawLine (x, y+width-1, x, y);

  } // paintIcon

  ////////////////////////////////////////////////////////////

  /** Sets the swatch color. */
  public void setColor (Color color) { this.color = color; }

  ////////////////////////////////////////////////////////////

} // ColorSwatch class

////////////////////////////////////////////////////////////////////////
