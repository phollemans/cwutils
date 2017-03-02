////////////////////////////////////////////////////////////////////////
/*

     File: LineCollection.java
   Author: Peter Hollemans
     Date: 2015/06/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2015 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render.lines;

// Imports
// -------

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.render.lines.LabeledLine;
import noaa.coastwatch.render.lines.LineLabelFactory;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;

/**
 * The <code>LineCollection</code> class holds an ordered set of
 * line objects.  Each line encodes information about which line it
 * is and what line segments it contains.  The line collection is
 * used as a convenient interface for adding segments to a set of
 * lines, and then iterating over the lines for rendering.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class LineCollection
  extends TreeMap<String,LabeledLine> {

  ////////////////////////////////////////////////////////////

  /** Creates a new empty collection of lines. */
  public LineCollection () { }

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a line segment to the collection.  The segment is added to
   * the appropriate line object.  If the line does not yet exist,
   * it is created.
   *
   * @param textLabel the line text label, used as a key in the
   * collection.
   * @param trans the earth image transform used for
   * translating earth locations to image points.
   * @param start the starting earth location.
   * @param end the ending earth location.
   */
  public void addSegment (
    String textLabel,
    EarthImageTransform trans,
    EarthLocation start,
    EarthLocation end
  ) {

    // Add new line
    // ------------
    if (!this.containsKey (textLabel)) {
      LabeledLine line = new LabeledLine (textLabel);
      line.addSegment (start, end, trans);
      if (line.getSegmentCount() != 0) this.put (textLabel, line);
    } // if

    // Add to existing line
    // --------------------
    else {
      LabeledLine line = this.get (textLabel);
      line.addSegment (start, end, trans);
    } // else

  } // addSegment

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a line segment to the collection.  The segment is added to
   * the appropriate line object.  If the line does not yet exist,
   * it is created.
   *
   * @param textLabel the line text label, used as a key in the
   * collection.
   * @param trans the earth image transform used for
   * translating data locations to image points.
   * @param start the starting data location.
   * @param end the ending Earthdata location.
   */
  public void addSegment (
    String textLabel,
    EarthImageTransform trans,
    DataLocation start,
    DataLocation end
  ) {

    // Add new line
    // ------------
    if (!this.containsKey (textLabel)) {
      LabeledLine line = new LabeledLine (textLabel);
      line.addSegment (start, end, trans);
      if (line.getSegmentCount() != 0) this.put (textLabel, line);
    } // if

    // Add to existing line
    // --------------------
    else {
      LabeledLine line = this.get (textLabel);
      line.addSegment (start, end, trans);
    } // else

  } // addSegment

  ////////////////////////////////////////////////////////////

  /** 
   * Renders this collection of lines.
   * 
   * @param g the graphics device for rendering.
   */
  public void render (
    Graphics2D g
  ) {

    // Loop over each line and render
    // ------------------------------
    for (LabeledLine line : this.values())
      line.render (g);

  } // render

  ////////////////////////////////////////////////////////////

  /**
   * Gets the labels for this collection of lines.
   * 
   * @param imageDims the image dimensions.
   * @param labelFactory the label factory to use for creating labels.
   *
   * @return a list of text elements for the line labels.
   */
  public List<TextElement> getLabels (
    Dimension imageDims,
    LineLabelFactory labelFactory
  ) {

    // Loop over each line and get labels
    // ----------------------------------
    List<TextElement> labels = new ArrayList<TextElement>();
    for (LabeledLine line : this.values()) {
      List<TextElement> lineLabels = line.getLabels (imageDims, labelFactory);
      if (lineLabels != null) labels.addAll (lineLabels);
    } // while

    return (labels);

  } // getLabels

  ////////////////////////////////////////////////////////////

} // LineCollection class

////////////////////////////////////////////////////////////////////////

