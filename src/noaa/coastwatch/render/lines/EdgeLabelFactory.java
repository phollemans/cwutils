////////////////////////////////////////////////////////////////////////
/*
     FILE: EdgeLabelFactory.java
  PURPOSE: Creates a set of labels for a line based on view edges.
   AUTHOR: Peter Hollemans
     DATE: 2015/06/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2015, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

  /*
   * How this class works:
   *
   * o Get a list of the segments of the line closest to the edges
   *   of the image view (maximum four segments, one for each of the top,
   *   bottom, left, and right edges of the view).
   * o Sort the list of edge segments by distance from their respective
   *   edges, smallest distance first.
   * o Keep only the first two edges in the list (ie: closest to their
   *   respective edges, hopefully on either end of a line).
   * o Create a label, one for each edge, that depends on the angle
   *   that the segment makes with the edge that it's closest to.
   */

// Package
// -------
package noaa.coastwatch.render.lines;

// Imports
// -------
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.geom.Line2D;
import java.awt.Dimension;
import java.awt.Rectangle;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.render.LineOverlay;
import noaa.coastwatch.render.lines.LineLabelFactory;
import noaa.coastwatch.render.lines.EdgeSegment;
import noaa.coastwatch.render.lines.EdgeSegment.EdgeType;

/**
 * The <code>EdgeLabelFactory</code> class creates labels that
 * line up along the view edges.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class EdgeLabelFactory implements LineLabelFactory {

  // Variables
  // ---------
  
  /** The singleton instance of this class. */
  private static EdgeLabelFactory instance = null;

  ////////////////////////////////////////////////////////////

  private EdgeLabelFactory () {}

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static EdgeLabelFactory getInstance() {
  
    if (instance == null) instance = new EdgeLabelFactory();
    return (instance);
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Gets the edge segments closest to the image edges and
   * inside the image boundaries.
   *
   * @param segments the line segments for the line to label.
   * @param imageDims the image dimensions.
   *
   * @return a list of edge segments, one for each edge, or
   * null if the entire line is outside the image
   * boundaries.
   */
  private List<EdgeSegment> getEdges (
    List<Line2D> segments,
    Dimension imageDims
  ) {

    // Initialize search
    // -----------------
    EdgeSegment top = null, bottom = null, left = null, right = null;
    boolean init = false;
    Rectangle rect = new Rectangle (imageDims);

    // Loop over each segment
    // ----------------------
    for (Line2D line : segments) {

      // Copy and clip segment
      // ---------------------
      Line2D segment = LineOverlay.clip (line, rect);
      if (segment == null) continue;
      if (segment.getP1().equals (segment.getP2())) continue;

      // Initialize segment distances
      // ----------------------------
      if (!init) {
        top = new EdgeSegment (EdgeType.TOP, imageDims, segment);
        bottom = new EdgeSegment (EdgeType.BOTTOM, imageDims, segment);
        left = new EdgeSegment (EdgeType.LEFT, imageDims, segment);
        right = new EdgeSegment (EdgeType.RIGHT, imageDims, segment);
        init = true;
      } // if

      // Find closest edge segments
      // --------------------------
      else {
        EdgeSegment edge;
        edge = new EdgeSegment (EdgeType.TOP, imageDims, segment);
        if (edge.dist < top.dist) top = edge;    
        edge = new EdgeSegment (EdgeType.BOTTOM, imageDims, segment);
        if (edge.dist < bottom.dist) bottom = edge;    
        edge = new EdgeSegment (EdgeType.LEFT, imageDims, segment);
        if (edge.dist < left.dist) left = edge;    
        edge = new EdgeSegment (EdgeType.RIGHT, imageDims, segment);
        if (edge.dist < right.dist) right = edge;
      } // else

    } // while

    // Return edges
    // ------------
    List<EdgeSegment> edges = null;
    if (init) {
      edges = new LinkedList<EdgeSegment>();
      edges.add (top);
      edges.add (bottom);
      edges.add (left);
      edges.add (right);
    } // if
    
    return (edges);

  } // getEdges

  ////////////////////////////////////////////////////////////

  @Override
  public List<TextElement> create (
    List<Line2D> segments,
    String text,
    Dimension imageDims
  ) {
  
    List<TextElement> labels = null;
  
    // Get edges and sort
    // ------------------
    List<EdgeSegment> edges = getEdges (segments, imageDims);
    if (edges != null) {

      // Remove all but the first two unique segments
      // --------------------------------------------
      Collections.sort (edges);
      edges = new LinkedList<EdgeSegment> (edges.subList (0, 2));
      if (edges.get(0).line == edges.get(1).line)
        edges.remove (1);

      // Get edge segment labels
      // -----------------------
      labels = new ArrayList<TextElement>();
      for (EdgeSegment edge : edges) {
        TextElement label = edge.getLabel (text);
        if (label != null) labels.add (label);
      } // for

    } // if
    
    return (labels);
  
  } // create

  ////////////////////////////////////////////////////////////

} // EdgeLabelFactory class

////////////////////////////////////////////////////////////////////////
