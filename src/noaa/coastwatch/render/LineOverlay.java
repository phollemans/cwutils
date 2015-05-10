////////////////////////////////////////////////////////////////////////
/*
     FILE: LineOverlay.java
  PURPOSE: An overlay for vector specified data such as lines and shapes.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/25
  CHANGES: 2002/09/03, PFH, modified parent class, rearranged
           2002/09/19, PFH, added clip
           2002/10/03, PFH, added fill
           2002/10/10, PFH, added clip detection in polyline rendering
           2002/10/10, PFH, changed operations to use Point2D
           2002/10/23, PFH, changed default stroke to beveled joins
           2003/11/22, PFH, fixed Javadoc comments
           2004/04/04, PFH, added special serialization methods
           2006/12/26, PFH, added drop shadows for lines

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.EarthDataView;

/**
 * A vector overlay annotes a data view with vector-specified lines
 * and shapes.  The vector overlay adds the concept of a stroke to the
 * parent class.<p>
 *
 * An implementation note: The normal <code>BasicStroke</code> object
 * that would be used to define a stroke for vector overlays is not
 * serializable.  So we add some special serialization methods here to
 * handle saving and restoring stroke objects.  The alternative is to
 * create a new serializable stroke class, which may be required if
 * there are other classes that use a stroke and need serialization.
 * For now, we keep it simple.<p>
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class LineOverlay 
  extends EarthDataOverlay {

  // Variables
  // ---------

  /** The stroke to use for vector paths. */
  private Stroke stroke;

  /** The drop shadow flag, true to draw a drop shadow. */
  private boolean dropShadow;

  /** The shadow rendering flag, true if we are rendering a shadow. */
  protected boolean drawingShadow;

  ////////////////////////////////////////////////////////////

  /** Gets the drop shadow flag. */
  public boolean getDropShadow () { return (dropShadow); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the drop shadow flag.  When drop shadow mode is on, a shadow
   * is drawn behind the lines.  By default, drop shadow mode is off.
   */
  public void setDropShadow (boolean flag) { dropShadow = flag; }

  ////////////////////////////////////////////////////////////

  /** 
   * Draws a drop shadow version of this overlay.
   *
   * @param g the graphics device to draw to.
   * @param view the data for drawing.
   */
  protected void drawShadow (
    Graphics2D g,
    EarthDataView view
  ) {

    // Save settings
    // -------------
    Stroke savedStroke = stroke;
    Color savedColor = getColor();
    int savedAlpha = alpha; 
    
    // Setup for shadow drawing
    // ------------------------
    BasicStroke basic = (BasicStroke) stroke;
    stroke = new BasicStroke (
      basic.getLineWidth()+2,
      basic.getEndCap(),
      basic.getLineJoin(),
      basic.getMiterLimit(),
      basic.getDashArray(),
      basic.getDashPhase());
    setColor (getShadowColor (savedColor));
    alpha = (int) (0.50*alpha);
    drawingShadow = true;
    draw (g, view);
    drawingShadow = false;

    // Restore settings
    // ----------------
    stroke = savedStroke;
    setColor (savedColor);
    alpha = savedAlpha;

  } // drawShadow

  ////////////////////////////////////////////////////////////

  public void render (
    Graphics2D g,
    EarthDataView view
  ) {

    // Prepare graphics
    // ----------------
    if (!isPrepared (view)) {
      prepare (g, view);
      prepared = true;
      lastTrans = view.getTransform();
    } // if

    // Draw drop shadow
    // ----------------
    if (dropShadow) drawShadow (g, view);

    // Draw graphics
    // -------------
    draw (g, view);

  } // render

  ////////////////////////////////////////////////////////////

  /**
   * Gets the shadow color that will have the greatest contrast to the
   * specified foreground color.
   * 
   * @param foreground the foreground color in question.
   *
   * @return the shadow color, white if the foreground is dark or
   * black if the foreground is light.
   */
  public static Color getShadowColor (
    Color foreground
  ) {

    int gray = (int) Math.floor (
      foreground.getRed()*0.299 + 
      foreground.getGreen()*0.587 + 
      foreground.getBlue()*0.114);
    Color shadow = (gray > 128 ? Color.BLACK : Color.WHITE);

    return (shadow);

  } // getShadowColor

  ////////////////////////////////////////////////////////////

  /** Writes the vector stroke data to the output stream. */
  private void writeObject (
    ObjectOutputStream out
  ) throws IOException {

    BasicStroke basicStroke = (BasicStroke) stroke;
    out.writeFloat (basicStroke.getLineWidth());
    out.writeInt (basicStroke.getEndCap());
    out.writeInt (basicStroke.getLineJoin());
    out.writeFloat (basicStroke.getMiterLimit());
    out.writeObject (basicStroke.getDashArray());
    out.writeFloat (basicStroke.getDashPhase());

  } // writeObject

  ////////////////////////////////////////////////////////////

  /** Reads the vector stroke data from the input stream. */
  private void readObject (
    ObjectInputStream in
  ) throws IOException, ClassNotFoundException {

    stroke = new BasicStroke (
      in.readFloat(),
      in.readInt(),
      in.readInt(),
      in.readFloat(),
      (float[]) in.readObject(),
      in.readFloat()
    );

  } // readObject

  ////////////////////////////////////////////////////////////

  /** Sets the vector path stroke. */
  public void setStroke (Stroke stroke) { this.stroke = stroke; }

  ////////////////////////////////////////////////////////////

  /** Gets the vector path stroke. */
  public Stroke getStroke () { return (stroke); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new vector overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   */
  protected LineOverlay (
    Color color,
    int layer,
    Stroke stroke
  ) { 

    super (color, layer);
    this.stroke = stroke;

  } // LineOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new vector overlay.  The layer number is initialized
   * to 0, and the stroke to the default <code>BasicStroke</code> with 
   * beveled joins.
   * 
   * @param color the overlay color.
   */
  protected LineOverlay (
    Color color
  ) { 

    super (color);
    this.stroke = new BasicStroke (1.0f, BasicStroke.CAP_SQUARE, 
      BasicStroke.JOIN_BEVEL);

  } // LineOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Clips the line segment to the specified rectangle.  This method
   * implements the Cohen-Sutherland line clipping algorithm as
   * specified in:
   * <blockquote>
   *   Foley, J.D., A. van Dam, S.K. Feiner, and J.F. Hughes,
   *   "Computer Graphics: Principles and Practice, Second edition in C",
   *   Addison-Wesley, July 1997, pp 117-124, 274.
   * </blockquote>
   * @param line the line for clipping.
   * @param rect the clipping rectangle.
   *
   * @return the clipped line, or null if the line is entirely outside
   * the rectangle.  The same set of points is returned if no
   * modifications to the points were performed, or a new set of
   * points otherwise.
   */
  public static Line2D clip ( 
    Line2D line,
    Rectangle2D rect
  ) {

    // Initialize variables
    // --------------------
    double x0 = line.getX1(), x1 = line.getX2();
    double y0 = line.getY1(), y1 = line.getY2();
    double xmin = rect.getMinX(), xmax = rect.getMaxX();
    double ymin = rect.getMinY(), ymax = rect.getMaxY();

    // Compute outcodes
    // ----------------
    int outcode0 = rect.outcode (x0, y0);
    int outcode1 = rect.outcode (x1, y1);
    boolean accept = false, done = false, modified = false;
    do {

      // Check if both points inside
      // ---------------------------
      if ((outcode0 | outcode1) == 0) {
        accept = true;
        done = true;
      } // if

      // Check if both points outside on the same side
      // ---------------------------------------------
      else if ((outcode0 & outcode1) != 0) {
        done = true;
      } // else if

      // Perform clipping
      // ----------------
      else {

        // Pick one of the points outside
        // ------------------------------
        int outcodeOut = (outcode0 != 0 ? outcode0 : outcode1);       

        // Find the intersection point
        // ---------------------------
        double x, y;
        if ((outcodeOut & Rectangle2D.OUT_BOTTOM) != 0) {
          x = x0 + (x1 - x0)*(ymax - y0) / (y1 - y0);
          y = ymax;
        } // if
        else if ((outcodeOut & Rectangle2D.OUT_TOP) != 0) {
          x = x0 + (x1 - x0)*(ymin - y0) / (y1 - y0);
          y = ymin;
        } // else if
        else if ((outcodeOut & Rectangle2D.OUT_RIGHT) != 0) {
          y = y0 + (y1 - y0)*(xmax - x0) / (x1 - x0);
          x = xmax;
        } // else if
        else {
          y = y0 + (y1 - y0)*(xmin - x0) / (x1 - x0);
          x = xmin;
        } // else

        // Move outside point to clip point
        // --------------------------------
        if (outcodeOut == outcode0) {
          x0 = x; y0 = y; outcode0 = rect.outcode (x0, y0);
        } // if
        else {
          x1 = x; y1 = y; outcode1 = rect.outcode (x1, y1);
        } // else        
        modified = true;

      } // else

    } while (!done);

    // Check for accepted
    // ------------------
    if (!accept) return (null);

    // Return line
    // -----------
    if (modified) line = new Line2D.Double (x0, y0, x1, y1);
    return (line);

  } // clip

  ////////////////////////////////////////////////////////////

} // LineOverlay class

////////////////////////////////////////////////////////////////////////
