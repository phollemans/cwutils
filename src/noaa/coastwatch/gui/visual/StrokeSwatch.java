////////////////////////////////////////////////////////////////////////
/*
     FILE: StrokeSwatch.java
  PURPOSE: An icon showing a swatch of the stroke line pattern.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/25
  CHANGES: 2006/03/19, PFH, fixed problem with stroke line endpoints
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * The <code>StrokeSwatch</code> class is an icon that shows a
 * rectangle of a given line pattern.
 */
public class StrokeSwatch 
  implements Icon { 

  // Variables
  // ---------

  /** The swatch stroke. */
  private Stroke stroke;

  /** The icon width. */
  private int width;

  /** The icon height. */
  private int height;

  ////////////////////////////////////////////////////////////

  /** 
   * Create a new stroke swatch. 
   * 
   * @param stroke the stroke to display.
   * @param width the icon width.
   * @param height the icon height.
   */
  public StrokeSwatch (
    Stroke stroke,
    int width,
    int height
  ) {

    this.stroke = stroke;
    this.width = width;
    this.height = height;

  } // StrokeSwatch

  ////////////////////////////////////////////////////////////
  
  /** Gets the icon width. */
  public int getIconWidth () { return (width); }

  ////////////////////////////////////////////////////////////

  /** Gets the icon height. */
  public int getIconHeight() { return (height); }
  
  ////////////////////////////////////////////////////////////

  /** Paints the icon to the specified graphics context. */
  public void paintIcon (Component c, Graphics g, int x, int y) { 

    // Draw line
    // ---------
    Graphics2D g2d = (Graphics2D) g;
    Stroke saved = g2d.getStroke();
    g2d.setStroke (stroke);
    g2d.setColor (c.getForeground());
    g2d.draw (new Line2D.Double (x, y+height/2, x+width-1, y+height/2));
    g2d.setStroke (saved);
    
  } // paintIcon

  ////////////////////////////////////////////////////////////

  /** Sets the swatch stroke. */
  public void setStroke (Stroke stroke) { this.stroke = stroke; }

  ////////////////////////////////////////////////////////////

} // StrokeSwatch class

////////////////////////////////////////////////////////////////////////
