////////////////////////////////////////////////////////////////////////
/*
     FILE: LabeledLineOverlay.java
  PURPOSE: Draws lines and labels on a data view.
   AUTHOR: Peter Hollemans
     DATE: 2006/12/20
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.LineOverlay;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;

/**
 * A <code>LabeledLineOverlay</code> renders lines and labels on
 * an {@link EarthDataView}.  A labeled line is any line that has
 * a labeled value and some connected set of points to display
 * such as a line of latitude/longitude or a data contour line.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public abstract class LabeledLineOverlay 
  extends LineOverlay {

  // Variables
  // ---------

  /** A flag to indicate whether labels should be drawn. */
  private boolean drawLabels;

  /** The font for labels. */
  private Font font;

  /** The collection of lines for rendering. */
  private transient LineCollection lineCollection;

  /** The collection of labels for rendering. */
  private transient List<TextElement> lineLabels;

  /** The text drop shadow flag, true to draw a drop shadow for text. */
  private boolean textDropShadow = true;

  ////////////////////////////////////////////////////////////

  /** 
   * Reads the object data from the input stream. In most cases there
   * are no problems when deserializing the overlay.  But when going
   * between operating systems with different fonts, the font may be
   * deserialized incorrectly and needs to be reset.
   */
  private void readObject (
    ObjectInputStream in
  ) throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    font = new Font (font.getName(), font.getStyle(), font.getSize());

  } // readObject

  ////////////////////////////////////////////////////////////

  /** Gets the text drop shadow flag. */
  public boolean getTextDropShadow () { return (textDropShadow); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the text drop shadow flag.  When text drop shadow mode is
   * on, a shadow is drawn behind the text labels.  By default, text
   * drop shadow mode is on.
   */
  public void setTextDropShadow (boolean flag) { textDropShadow = flag; }

  ////////////////////////////////////////////////////////////

  /** Sets the line labels flag. */
  public void setDrawLabels (boolean drawLabels) { 

    if (this.drawLabels != drawLabels) {
      this.drawLabels = drawLabels; 
      prepared = false;
    } // if

  } // setDrawLabels

  ////////////////////////////////////////////////////////////

  /** Gets the line labels flag. */
  public boolean getDrawLabels () { return (drawLabels); }

  ////////////////////////////////////////////////////////////

  /** Sets the line labels font. */
  public void setFont (Font font) { 

    if (!this.font.equals (font)) {
      this.font = font;
      prepared = false;
    } // if

  } // setFont

  ////////////////////////////////////////////////////////////

  /** Gets the line labels font. */
  public Font getFont () { return (font); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new labeled line overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param drawLabels the line labels flag, true if labels
   * should be drawn.
   * @param font the line labels font.  The labels font may
   * be null if no labels are to be drawn.
   */
  public LabeledLineOverlay (
    Color color,
    int layer,
    Stroke stroke,
    boolean drawLabels,
    Font font
  ) { 

    super (color, layer, stroke);
    this.drawLabels = drawLabels;
    this.font = font;

  } // LabeledLineOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new labeled line overlay.  The layer number
   * is initialized to 0, the stroke to the default
   * <code>BasicStroke</code>, labels to true, and the font to
   * the default font face, plain style, 12 point.
   *
   * @param color the overlay color.
   */
  public LabeledLineOverlay (
    Color color
  ) { 

    super (color);
    this.drawLabels = true;
    this.font = new Font (null, Font.PLAIN, 12);

  } // LabeledLineOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * The <code>LabeledLine</code> class holds the line label text
   * and segment information for one labeled line on the earth.
   */
  private class LabeledLine 
    implements Comparable<LabeledLine> {

    // Variables
    // ---------

    /** The line label text. */
    private String labelText;

    /** The list of line segments as Line2D objects. */
    private List<Line2D> lineSegments;

    ////////////////////////////////////////////////////////

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

    ////////////////////////////////////////////////////////

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

    ////////////////////////////////////////////////////////

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

    ////////////////////////////////////////////////////////

    /** Gets the number of segments in this line. */
    public int getSegmentCount () { return (lineSegments.size()); }

    ////////////////////////////////////////////////////////

    public int compareTo (LabeledLine line) {

      return (this.labelText.compareTo (line.labelText));

    } // compareTo

    ////////////////////////////////////////////////////////

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

    ////////////////////////////////////////////////////////

    /**
     * Gets the edge segments closest to the image edges and
     * inside the image boundaries.
     *
     * @param imageDims the image dimensions.
     *
     * @return a list of edge segments, one for each edge, or
     * null if the entire line is outside the image
     * boundaries.
     */
    public List<EdgeSegment> getEdges (
      Dimension imageDims
    ) {

      // Initialize search
      // -----------------
      EdgeSegment top = null, bottom = null, left = null, right = null;
      boolean init = false;
      Rectangle rect = new Rectangle (imageDims);

      // Loop over each segment
      // ----------------------
      for (Line2D line : lineSegments) {

        // Copy and clip segment
        // ---------------------
        Line2D segment = clip (line, rect);
        if (segment == null) continue;
        if (segment.getP1().equals (segment.getP2())) continue;

        // Initialize segment distances
        // ----------------------------
        if (!init) {
          top = new EdgeSegment (EdgeSegment.TOP, imageDims, segment);
          bottom = new EdgeSegment (EdgeSegment.BOTTOM, imageDims, segment);
          left = new EdgeSegment (EdgeSegment.LEFT, imageDims, segment);
          right = new EdgeSegment (EdgeSegment.RIGHT, imageDims, segment);
          init = true;
        } // if

        // Find closest edge segments
        // --------------------------
        else {
          EdgeSegment edge;
          edge = new EdgeSegment (EdgeSegment.TOP, imageDims, segment);
          if (edge.dist < top.dist) top = edge;    
          edge = new EdgeSegment (EdgeSegment.BOTTOM, imageDims, segment);
          if (edge.dist < bottom.dist) bottom = edge;    
          edge = new EdgeSegment (EdgeSegment.LEFT, imageDims, segment);
          if (edge.dist < left.dist) left = edge;    
          edge = new EdgeSegment (EdgeSegment.RIGHT, imageDims, segment);
          if (edge.dist < right.dist) right = edge;    
        } // else

      } // while

      // Return edges
      // ------------
      if (!init) return (null);
      List<EdgeSegment> edges = new LinkedList<EdgeSegment>();
      edges.add (top);
      edges.add (bottom);
      edges.add (left);
      edges.add (right);
      return (edges);

    } // getEdges

    ////////////////////////////////////////////////////////

    /**
     * Gets the line labels.  A line is given two labels for
     * annotation of each edge of the image boundaries that the
     * line encounters.
     *
     * @param imageDims the image dimensions.
     *
     * @return an array of text elements for line labels, or null
     * if no segments are inside the image boundaries.
     */
    public List<TextElement> getLabels (
      Dimension imageDims
    ) {

      // Get edges and sort
      // ------------------
      List<EdgeSegment> edges = getEdges (imageDims);
      if (edges == null) return (null);
      Collections.sort (edges);

      // Remove all but the first two unique segments
      // --------------------------------------------
      edges = new LinkedList<EdgeSegment> (edges.subList (0, 2));
      if (edges.get(0).line == edges.get(1).line)
        edges.remove (1);

      // Get edge segment labels
      // -----------------------
      List<TextElement> labels = new ArrayList<TextElement>();
      for (EdgeSegment edge : edges) {
        TextElement label = (edge.getLabel (labelText));
        if (label != null) labels.add (label);
      } // for

      return (labels);

    } // getLabels

    ////////////////////////////////////////////////////////

  } // LabeledLine class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>EdgeSegment</code> class holds information about a
   * segment which is close to an image edge.  The edge,
   * distance, and segment points are stored.
   */
  private class EdgeSegment
    implements Comparable<EdgeSegment> {

    // Constants
    // ---------

    /** Top edge. */
    public final static int TOP = 0;

    /** Bottom edge. */
    public final static int BOTTOM = 1;
    
    /** Left edge. */
    public final static int LEFT = 2;

    /** Right edge. */
    public final static int RIGHT = 3;

    /** The line label offset as a fraction of the label size. */
    private final static double OFFSET = 0.2;

    // Variables
    // ---------

    /** The segment edge type. */
    public int type;

    /** The distance to the segment edge. */
    public double dist;

    /** The segment points. */
    public Line2D line;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new edge segment from the specified parameters.
     * The distance to the edge is calculated.
     *
     * @param type the segment type.
     * @param imageDims the image dimensions.
     * @param line the segment line.
     */
    public EdgeSegment (
      int type,
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

    ////////////////////////////////////////////////////////

    public int compareTo (EdgeSegment edge) {

      return (Double.compare (this.dist, edge.dist));

    } // compareTo

    ////////////////////////////////////////////////////////

    /** 
     * Orients this edge segment with respect to its edge.  The
     * edge segment points are rearranged so that this edge
     * segment starts at its edge and ends towards the center of
     * the image.
     */
    public void orient () {

      if (type == TOP && line.getY1() > line.getY2())
        line = new Line2D.Double (line.getP2(), line.getP1());

      else if (type == BOTTOM && line.getY1() < line.getY2())
        line = new Line2D.Double (line.getP2(), line.getP1());

      else if (type == LEFT && line.getX1() > line.getX2())
        line = new Line2D.Double (line.getP2(), line.getP1());

      else if (type == RIGHT && line.getX1() < line.getX2())
        line = new Line2D.Double (line.getP2(), line.getP1());

    } // orient

    ////////////////////////////////////////////////////////

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
      if (type == TOP) {
        double a = line.getX2() - line.getX1();
        double b = line.getY2() - line.getY1();
        double angle = Math.toDegrees (Math.asin (b / Math.sqrt (a*a + b*b)));
        if (angle < 10) return (null);
        if (a <= 0) return (new TextElement (text, font, line.getP1(), 
          new double[] {1+OFFSET, 1+OFFSET}, angle));
        else return (new TextElement (text, font, line.getP1(),
          new double[] {-OFFSET,1+OFFSET}, -angle));
      } // if

      else if (type == BOTTOM) {
        double a = line.getX2() - line.getX1();
        double b = line.getY1() - line.getY2();
        double angle = Math.toDegrees (Math.asin (b / Math.sqrt (a*a + b*b)));
        if (angle < 10) return (null);
        if (a < 0) return (new TextElement (text, font, line.getP1(),
          new double[] {1+OFFSET,-OFFSET}, -angle));
        else return (new TextElement (text, font, line.getP1(),
          new double[] {-OFFSET,-OFFSET}, angle));
      } // else if

      else if (type == LEFT) {
        double a = line.getY2() - line.getY1();
        double b = line.getX2() - line.getX1();
        double angle = Math.toDegrees (Math.acos (b / Math.sqrt (a*a + b*b)));
        if (angle > 80) return (null);
        if (a < 0) return (new TextElement (text, font, line.getP1(),
          new double[] {-OFFSET,1+OFFSET}, angle));
        else return (new TextElement (text, font, line.getP1(),
          new double[] {-OFFSET,-OFFSET}, -angle));
      } // else if

      else { // if (type == RIGHT)
        double a = line.getY2() - line.getY1();
        double b = line.getX1() - line.getX2();
        double angle = Math.toDegrees (Math.acos (b / Math.sqrt (a*a + b*b)));
        if (angle > 80) return (null);
        if (a <= 0) return (new TextElement (text, font, line.getP1(),
          new double[] {1+OFFSET,1+OFFSET}, -angle));
        else return (new TextElement (text, font, line.getP1(), 
          new double[] {1+OFFSET,-OFFSET}, angle));
      } // else

    } // getLabel

    ////////////////////////////////////////////////////////

  } // EdgeSegment class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>LineCollection</code> class holds an ordered set of
   * line objects.  Each line encodes information about which line it
   * is and what line segments it contains.  The line collection is
   * used as a convenient interface for adding segments to a set of
   * lines, and then iterating over the lines for rendering.
   */
  protected class LineCollection
    extends TreeMap<String,LabeledLine> {

    /////////////////////////////////////////////////////////

    /** Creates a new empty collection of lines. */
    public LineCollection () { }

    /////////////////////////////////////////////////////////

    /** 
     * Adds a line segment to the collection.  The segment is added to
     * the appropriate line object.  If the line does not yet exist,
     * it is created.
     *
     * @param textLabel the line text label, used as a key in the
     * collection.
     * @param trans the Earth image transform used for
     * translating Earth locations to image points.
     * @param start the starting Earth location.
     * @param end the ending Earth location.
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

    /////////////////////////////////////////////////////////

    /** 
     * Adds a line segment to the collection.  The segment is added to
     * the appropriate line object.  If the line does not yet exist,
     * it is created.
     *
     * @param textLabel the line text label, used as a key in the
     * collection.
     * @param trans the Earth image transform used for
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

    /////////////////////////////////////////////////////////

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

    /////////////////////////////////////////////////////////

    /**
     * Gets the labels for this collection of lines.
     * 
     * @param imageDims the image dimensions.
     *
     * @return a list of text elements for the line labels.
     */
    public List<TextElement> getLabels (
      Dimension imageDims
    ) {

      // Loop over each line and get labels
      // ----------------------------------
      List<TextElement> labels = new ArrayList<TextElement>();
      for (LabeledLine line : this.values()) {
        List<TextElement> lineLabels = line.getLabels (imageDims);
        if (lineLabels != null) labels.addAll (lineLabels);
      } // while

      return (labels);

    } // getLabels

    /////////////////////////////////////////////////////////

  } // LineCollection class

  ////////////////////////////////////////////////////////////

  /**
   * Gets the collection of lines for this overlay.  This method
   * should be implemented by the subclass and is called by
   * {@link #prepare}.
   *
   * @param view the earth view for line rendering.
   *
   * @return the collection of lines to render.
   */
  protected abstract LineCollection getLines (
    EarthDataView view
  );

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {
 
    // Get lines
    // ---------
    lineCollection = getLines (view);

    // Create labels
    // -------------
    if (drawLabels) {

      // Create initial label collection
      // -------------------------------
      Dimension imageDims = 
        view.getTransform().getImageTransform().getImageDimensions();
      lineLabels = new ArrayList<TextElement>();
      lineLabels.addAll (lineCollection.getLabels (imageDims));

      // Filter overlapping and out of bounds labels
      // -------------------------------------------
      Rectangle imageBounds = new Rectangle (imageDims);
      Area renderedArea = new Area();
      List<TextElement> renderedLabels = new ArrayList<TextElement>();
      for (TextElement label : lineLabels) {

        // Check image bounds
        // ------------------
        Area labelArea = label.getArea(g);
        Rectangle labelBounds = labelArea.getBounds();
        if (!imageBounds.contains (labelBounds)) continue;

        // Check overlap
        // -------------
        Area testArea = (Area) labelArea.clone();
        testArea.intersect (renderedArea);
        if (!testArea.isEmpty()) continue;

        // Save rendered label
        // -------------------
        renderedArea.add (labelArea);
        renderedLabels.add (label);

      } // for

      // Save only rendered labels
      // -------------------------
      lineLabels = renderedLabels;

    } // if

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {
 
    // Check for null color
    // --------------------
    if (getColor() == null) return;
 
    // Initialize properties
    // ---------------------
    g.setColor (getColorWithAlpha());
    g.setStroke (getStroke());

    // Render lines
    // ------------
    Object strokeHint = g.getRenderingHint (RenderingHints.KEY_STROKE_CONTROL);
    g.setRenderingHint (RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_NORMALIZE);
    Object aliasHint = g.getRenderingHint (RenderingHints.KEY_ANTIALIASING);
    g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
      RenderingHints.VALUE_ANTIALIAS_OFF);
    lineCollection.render (g);
    if (strokeHint != null) 
      g.setRenderingHint (RenderingHints.KEY_STROKE_CONTROL, strokeHint);
    if (aliasHint != null) 
      g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, aliasHint);

    // Render labels
    // -------------
    if (drawLabels && !drawingShadow) {

      // Get fore/back label colors
      // --------------------------
      Color fore = getColorWithAlpha();
      Color back = (textDropShadow ? 
        getAlphaVersion (getShadowColor (fore)) : null);

      // Loop over each label and render
      // -------------------------------
      for (TextElement label : lineLabels)
        label.render (g, fore, back);

    } // if

  } // draw

  ////////////////////////////////////////////////////////////

} // LabeledLineOverlay class

////////////////////////////////////////////////////////////////////////
