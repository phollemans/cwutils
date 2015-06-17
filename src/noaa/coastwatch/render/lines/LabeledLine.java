////////////////////////////////////////////////////////////////////////
/*
     FILE: LabeledLine.java
  PURPOSE: Holds line label and segment information.
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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.LineOverlay;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.render.lines.LineLabelFactory;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;

/**
 * The <code>LabeledLine</code> class holds the line label text
 * and segment information for one labeled line on the earth.
 * 
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class LabeledLine
  implements Comparable<LabeledLine> {

  // Variables
  // ---------

  /** The line label text. */
  private String labelText;

  /** The list of line segments as Line2D objects. */
  private List<Line2D> lineSegments;

  ////////////////////////////////////////////////////////////

  /**
   * Creates an empty line with the specified label text.
   *
   * @param labelText the text for labeling the line.
   */
  public LabeledLine (
    String labelText
  ) {
  
    this.labelText = labelText;
    lineSegments = new ArrayList<Line2D>();

  } // LabeledLine constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a segment to the line.  The starting and ending
   * locations are translated to image points, and only added
   * if the points are valid.
   *
   * @param start the starting data location.
   * @param end the ending data location.
   * @param trans the transform for converting from data location to
   * image location.
   */
  public void addSegment (
    DataLocation start,
    DataLocation end,
    EarthImageTransform trans
  ) {

    // Convert segment points
    // ----------------------
    Point2D p1 = trans.getImageTransform().transform (start);
    Point2D p2 = trans.getImageTransform().transform (end);

    // Add segment if valid
    // --------------------
    if (p1 != null && p2 != null) {
      lineSegments.add (new Line2D.Double (p1, p2));
    } // if

  } // addSegment

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a segment to the line.  The starting and ending
   * locations are translated to image points, and only added
   * if the points are valid.
   *
   * @param start the starting Earth location.
   * @param end the ending Earth location.
   * @param trans the transform for converting from earth
   * location to image location.
   */
  public void addSegment (
    EarthLocation start,
    EarthLocation end,
    EarthImageTransform trans
  ) {

    // Convert segment points
    // ----------------------
    Point2D p1 = trans.transform (start);
    Point2D p2 = trans.transform (end);

    // Add segment if valid
    // --------------------
    if (p1 != null && p2 != null 
      && !trans.isDiscontinuous (start, end, p1, p2)) {
      lineSegments.add (new Line2D.Double (p1, p2));
    } // if

  } // addSegment

  ////////////////////////////////////////////////////////////

  /** Gets the number of segments in this line. */
  public int getSegmentCount () { return (lineSegments.size()); }

  ////////////////////////////////////////////////////////////

  @Override
  public int compareTo (LabeledLine line) {

    return (this.labelText.compareTo (line.labelText));

  } // compareTo

  ////////////////////////////////////////////////////////////

  /** 
   * Renders this line.
   * 
   * @param g the graphics device for rendering.
   */
  public void render (
    Graphics2D g
  ) {

    for (Line2D line : lineSegments) 
      g.draw (line);

  } // render

  ////////////////////////////////////////////////////////////

  /**
   * Gets the line labels.  A line is given labels based on the type
   * of factory used.
   *
   * @param imageDims the image dimensions.
   * @param labelFactory the label factory to use for creating labels.
   *
   * @return an array of text elements for line labels, or null
   * if no segments are inside the image boundaries.
   */
  public List<TextElement> getLabels (
    Dimension imageDims,
    LineLabelFactory labelFactory
  ) {

    return (labelFactory.create (lineSegments, labelText, imageDims));

  } // getLabels

  ////////////////////////////////////////////////////////////

} // LabeledLine class

////////////////////////////////////////////////////////////////////////
