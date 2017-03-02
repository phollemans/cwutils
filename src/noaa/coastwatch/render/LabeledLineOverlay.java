////////////////////////////////////////////////////////////////////////
/*

     File: LabeledLineOverlay.java
   Author: Peter Hollemans
     Date: 2006/12/20

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.LineOverlay;
import noaa.coastwatch.render.lines.LineCollection;
import noaa.coastwatch.render.lines.LineLabelFactory;
import noaa.coastwatch.render.lines.EdgeLabelFactory;
import noaa.coastwatch.render.lines.CenteredLabelFactory;
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

  // Constants
  // ---------

  /** The serialization constant. */
  private static final long serialVersionUID = 4102557195004157599L;

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

  @Override
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

      // Determine which factory to use for labels
      // -----------------------------------------
      EarthImageTransform trans = view.getTransform();
      Dimension imageDims = trans.getImageTransform().getImageDimensions();
      List<Point2D> pointList = new ArrayList<Point2D>();
      pointList.add (new Point2D.Double (0, 0));
      pointList.add (new Point2D.Double ((imageDims.width-1)/2, 0));
      pointList.add (new Point2D.Double (imageDims.width-1, 0));
      pointList.add (new Point2D.Double (imageDims.width-1, (imageDims.height-1)/2));
      pointList.add (new Point2D.Double (imageDims.width-1, imageDims.height-1));
      pointList.add (new Point2D.Double ((imageDims.width-1)/2, imageDims.height-1));
      pointList.add (new Point2D.Double (0, imageDims.height-1));
      pointList.add (new Point2D.Double (0, (imageDims.height-1)/2));
      boolean foundValid = false;
      for (Point2D point : pointList) {
        if (trans.transform (point).isValid()) {
          foundValid = true;
          break;
        } // if
      } // for
      LineLabelFactory labelFactory;
      if (foundValid) labelFactory = EdgeLabelFactory.getInstance();
      else labelFactory = CenteredLabelFactory.getInstance();

      // Create initial label collection
      // -------------------------------
      lineLabels = new ArrayList<TextElement>();
      lineLabels.addAll (lineCollection.getLabels (imageDims, labelFactory));

      // Set label font
      // --------------
      for (TextElement label : lineLabels) {
        label.setFont (font);
      } // for

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

  @Override
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
