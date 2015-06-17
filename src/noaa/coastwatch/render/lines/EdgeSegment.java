////////////////////////////////////////////////////////////////////////
/*
     FILE: EdgeSegment.java
  PURPOSE: Holds information about an edge segment of a line.
   AUTHOR: Peter Hollemans
     DATE: 2015/06/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2015, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render.lines;

// Imports
// -------
import java.awt.geom.Line2D;
import java.awt.Dimension;
import noaa.coastwatch.render.TextElement;

/**
 * The <code>EdgeSegment</code> class holds information about a
 * segment which is close to an image edge.  The edge type,
 * distance, and segment points are stored.
 *
 * @since 3.3.1
 */
public class EdgeSegment
  implements Comparable<EdgeSegment> {

  // Constants
  // ---------

  /** The types of edges possible for edge segments. */
  public enum EdgeType {
    TOP,        // The top edge
    BOTTOM,     // The bottom edge
    LEFT,       // The left edge
    RIGHT       // The right edge
  }

  /** The line label offset as a fraction of the label size. */
  private final static double OFFSET = 0.2;

  // Variables
  // ---------

  /** The segment edge type. */
  public EdgeType type;

  /** The distance to the segment edge. */
  public double dist;

  /** The segment points. */
  public Line2D line;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new edge segment from the specified parameters.
   * The distance to the edge is calculated.
   *
   * @param type the segment type.
   * @param imageDims the image dimensions.
   * @param line the segment line.
   */
  public EdgeSegment (
    EdgeType type,
    Dimension imageDims,
    Line2D line
  ) {     

    // Initialize
    // ----------
    this.line = line;
    this.type = type;

    // Calculate distance
    // ------------------
    switch (type) {
    case TOP: 
      dist = Math.min (line.getY1(), line.getY2()); 
      break;
    case BOTTOM: 
      dist = imageDims.height - 1 - Math.max (line.getY1(), line.getY2()); 
      break;
    case LEFT: 
      dist = Math.min (line.getX1(), line.getX2()); 
      break;
    case RIGHT:
      dist = imageDims.width - 1 - Math.max (line.getX1(), line.getX2()); 
      break;
    } // switch

  } // EdgeSegment constructor

  ////////////////////////////////////////////////////////////

  @Override
  public int compareTo (EdgeSegment edge) {

    return (Double.compare (this.dist, edge.dist));

  } // compareTo

  ////////////////////////////////////////////////////////////

  /** 
   * Orients this edge segment with respect to its edge.  The
   * edge segment points are rearranged so that this edge
   * segment starts at its edge and ends towards the center of
   * the image.
   */
  public void orient () {

    if (type == EdgeType.TOP && line.getY1() > line.getY2())
      line = new Line2D.Double (line.getP2(), line.getP1());

    else if (type == EdgeType.BOTTOM && line.getY1() < line.getY2())
      line = new Line2D.Double (line.getP2(), line.getP1());

    else if (type == EdgeType.LEFT && line.getX1() > line.getX2())
      line = new Line2D.Double (line.getP2(), line.getP1());

    else if (type == EdgeType.RIGHT && line.getX1() < line.getX2())
      line = new Line2D.Double (line.getP2(), line.getP1());

  } // orient

  ////////////////////////////////////////////////////////////

  /**
   * Gets a label for this edge segment using the specified
   * text.
   *
   * @param text the text string for the label, or null if the
   * label is not valid.
   */
  public TextElement getLabel (
    String text
  ) {
  
    // Orient the segment points
    // -------------------------
    orient();

    // Create the label
    // ----------------
    double a, b, angle;
    TextElement element = null;
    switch (type) {
      
    case TOP:
      a = line.getX2() - line.getX1();
      b = line.getY2() - line.getY1();
      angle = Math.toDegrees (Math.asin (b / Math.sqrt (a*a + b*b)));
      if (angle >= 10) {
        if (a <= 0) element = new TextElement (text, TextElement.DEFAULT_FONT, line.getP1(),
          new double[] {1+OFFSET, 1+OFFSET}, angle);
        else element = new TextElement (text, TextElement.DEFAULT_FONT, line.getP1(),
          new double[] {-OFFSET,1+OFFSET}, -angle);
      } // if
      break;

    case BOTTOM:
      a = line.getX2() - line.getX1();
      b = line.getY1() - line.getY2();
      angle = Math.toDegrees (Math.asin (b / Math.sqrt (a*a + b*b)));
      if (angle >= 10) {
        if (a < 0) element = new TextElement (text, TextElement.DEFAULT_FONT, line.getP1(),
          new double[] {1+OFFSET,-OFFSET}, -angle);
        else element = new TextElement (text, TextElement.DEFAULT_FONT, line.getP1(),
          new double[] {-OFFSET,-OFFSET}, angle);
      } // if
      break;

    case LEFT:
      a = line.getY2() - line.getY1();
      b = line.getX2() - line.getX1();
      angle = Math.toDegrees (Math.acos (b / Math.sqrt (a*a + b*b)));
      if (angle <= 80) {
        if (a < 0) element = new TextElement (text, TextElement.DEFAULT_FONT, line.getP1(),
          new double[] {-OFFSET,1+OFFSET}, angle);
        else element = new TextElement (text, TextElement.DEFAULT_FONT, line.getP1(),
          new double[] {-OFFSET,-OFFSET}, -angle);
      } // if
      break;

    case RIGHT:
      a = line.getY2() - line.getY1();
      b = line.getX1() - line.getX2();
      angle = Math.toDegrees (Math.acos (b / Math.sqrt (a*a + b*b)));
      if (angle <= 80) {
        if (a <= 0) element = new TextElement (text, TextElement.DEFAULT_FONT, line.getP1(),
          new double[] {1+OFFSET,1+OFFSET}, -angle);
        else element = new TextElement (text, TextElement.DEFAULT_FONT, line.getP1(),
          new double[] {1+OFFSET,-OFFSET}, angle);
      } // if
      break;
      
    } // switch

    return (element);

  } // getLabel

  ////////////////////////////////////////////////////////////

} // EdgeSegment class

////////////////////////////////////////////////////////////////////////
