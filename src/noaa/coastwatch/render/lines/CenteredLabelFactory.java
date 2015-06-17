////////////////////////////////////////////////////////////////////////
/*
     FILE: CenteredLabelFactory.java
  PURPOSE: Creates a set of labels for a line based on view center.
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
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.render.LineOverlay;
import noaa.coastwatch.render.lines.LineLabelFactory;

/**
 * The <code>CenteredLabelFactory</code> class creates labels that
 * line up along the view edges.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class CenteredLabelFactory implements LineLabelFactory {

  // Constants
  // ---------

  /** The line label offset as a fraction of the label size. */
  private final static double OFFSET = 0.2;

  // Variables
  // ---------
  
  /** The singleton instance of this class. */
  private static CenteredLabelFactory instance = null;

  ////////////////////////////////////////////////////////////

  private CenteredLabelFactory () {}

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static CenteredLabelFactory getInstance() {
  
    if (instance == null) instance = new CenteredLabelFactory();
    return (instance);
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  @Override
  public List<TextElement> create (
    List<Line2D> segments,
    String text,
    Dimension imageDims
  ) {
  
    List<TextElement> labels = null;

    // Loop over each segment
    // ----------------------
    Line2D centerSegment = null;
    double minDist = Double.MAX_VALUE;
    Point2D centerPoint = new Point2D.Double (imageDims.width/2, imageDims.height/2);
    for (Line2D segment : segments) {

      // Save segment closest to center
      // ------------------------------
      double segmentDist = segment.ptSegDist (centerPoint);
      if (segmentDist < minDist) {
        minDist = segmentDist;
        centerSegment = segment;
      } // if

    } // for

    // Create label
    // ------------
    if (centerSegment != null) {
      double x = centerSegment.getX2() - centerSegment.getX1();
      double y = centerSegment.getY1() - centerSegment.getY2();
      double angle = Math.toDegrees (Math.atan2 (y, x));
      Point2D basePoint = new Point2D.Double (
        (centerSegment.getX1() + centerSegment.getX2())/2,
        (centerSegment.getY1() + centerSegment.getY2())/2
      );
      if (angle > 90) angle = angle - 180;
      else if (angle < -90) angle = angle + 180;
      labels = new ArrayList<TextElement>();
      labels.add (new TextElement (text, TextElement.DEFAULT_FONT, basePoint,
        new double[] {0.5, 1+OFFSET}, angle));
    } // if
    
    return (labels);
  
  } // create

  ////////////////////////////////////////////////////////////

} // CenteredLabelFactory class

////////////////////////////////////////////////////////////////////////
