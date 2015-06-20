////////////////////////////////////////////////////////////////////////
/*
     FILE: LineLabelFactory.java
  PURPOSE: Creates a set of labels for a line.
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
import java.awt.geom.Line2D;
import java.awt.Dimension;
import noaa.coastwatch.render.TextElement;

/**
 * The <code>LineLabelFactory</code> interface is implemented by classes
 * that need to produce a set of labels for a line composed of line
 * segments.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public interface LineLabelFactory {

  /**
   * Creates a set of line labels.
   *
   * @param segments the line segments for the line to label.
   * @param text the label for the line.
   * @param imageDims the image dimensions for the box whose edges 
   * contain the entire line and all its segments.  This would typically be
   * the view.
   *
   * @return the set of labels created by the factory, or null if no segments
   * are inside the image boundaries.
   */
  public List<TextElement> create (
    List<Line2D> segments,
    String text,
    Dimension imageDims
  );

} // LineLabelFactory interface


////////////////////////////////////////////////////////////////////////


