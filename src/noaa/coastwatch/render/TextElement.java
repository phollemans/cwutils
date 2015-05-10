////////////////////////////////////////////////////////////////////////
/*
     FILE: TextElement.java
  PURPOSE: A class to handle text strings for annotation.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/14
  CHANGES: 2002/10/03, PFH, added getLayout
           2002/10/23, PFH, added drop shadows
           2004/04/25, PFH, added getBasePoint(), getFont()
           2005/04/03, PFH, added fuzzy drop shadows
           2006/11/19, PFH, changed drop shadow style
           2007/07/16, PFH, modified render() to use Graphics2D.create/dispose

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import noaa.coastwatch.render.AnnotationElement;
import noaa.coastwatch.render.GraphicsServices;

/**
 * A text element is an annotation element the renders text strings.
 * The element specifies various text properties such as the text
 * string value, position, orientation, alignment, and so on.  The
 * text is rendered in the annotation foreground color, and a drop
 * shadow in the background color.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class TextElement
  extends AnnotationElement { 

  // Constants
  // ---------

  /** The drop shadow kernel size. */
  private static final int KERNEL_SIZE = 5;

  /** The drop shadow offset factor as a fraction of the font size. */
  private static final double SHADOW_OFFSET = 0.1;

  // Variables
  // ---------
  /** The text string. */
  private String text;

  /** The text font. */
  private Font font;

  /** The base point. */
  private Point2D base;

  /** The horizontal and vertical alignment. */
  private double[] align;

  /** The orientation angle. */
  private double angle;

  /** The cached affine transform for text positioning and rotation. */
  private AffineTransform trans;

  /** The cached text bounds. */
  private Rectangle2D bounds;

  /** The cached area corresponding to the transformed bounds. */
  private Area area;

  /** The convolution operator for the shadow. */
  private static ConvolveOp shadowConvolve;

  ////////////////////////////////////////////////////////////

  static {

    // Create shadow convolve
    // ----------------------

    /**
     * Normally, we would want the convolution coefficients to sum to
     * 1.  But in this case, we're going for a combined dilation and
     * fuzzy operator, so we allow the coefficients to saturate the
     * output pixel and we let the VM truncate the color and alpha
     * values.
     */

    float c = 1.0f/5;
    shadowConvolve = new ConvolveOp (new Kernel (5, 5, new float[] {
      c,c,c,c,c,
      c,c,c,c,c,
      c,c,c,c,c,
      c,c,c,c,c,
      c,c,c,c,c
    }));

  } // static

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a Gaussian function kernel for use in image convolution.
   *
   * @param size the kernel size in rows and columns.
   * @param stdev the standard deviation of the Gaussian function in
   * pixels.
   *
   * @return a new kernel containing Gaussian function coefficients.
   */
  private static Kernel createGaussian (
    int size,
    double stdev
  ) {

    // Compute weights
    // ---------------
    int count = size*size;
    float[] weights = new float [count];
    double alpha = 2*Math.pow (stdev, 2);
    double beta = 1/Math.sqrt (Math.PI*alpha);
    int center = size/2;
    double sum = 0;
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        double dist = Math.pow (x-center, 2) + Math.pow (y-center, 2);
        weights[y*size + x] = (float) (beta*Math.exp (-center/alpha));
        sum += weights[y*size + x];
      } // for
    } // for

    // Normalize
    // ---------
    for (int i = 0; i < count; i++)
      weights[i] /= sum;

    return (new Kernel (size, size, weights));

  } // createGaussian

  ////////////////////////////////////////////////////////////

  /**
   * Invalidates the cached text affine transform and bounds.
   */
  private void invalidate () {

    bounds = null;
    trans = null;
    area = null;

  } // invalidate

  ////////////////////////////////////////////////////////////

  /**
   * Gets the text layout object for this element.
   * 
   * @param g the graphics device for drawing.
   */
  public TextLayout getLayout (
    Graphics2D g
  ) {

    return (new TextLayout (text, font, g.getFontRenderContext()));

  } // getLayout

  ////////////////////////////////////////////////////////////

  /**
   * Prepares the text affine transform and bounds for rendering.
   *
   * @param g the graphics device for drawing.
   */
  private void prepare (
    Graphics2D g
  ) {

    // Prepare bounds
    // --------------
    TextLayout layout = new TextLayout (text, font, g.getFontRenderContext());
    bounds = layout.getBounds();
    bounds.setRect (bounds.getX()+base.getX(), bounds.getY()+base.getY(),
      bounds.getWidth(), bounds.getHeight());

    // Prepare transform
    // -----------------
    trans = new AffineTransform();
    trans.preConcatenate (AffineTransform.getTranslateInstance (
      -align[0]*bounds.getWidth(), align[1]*bounds.getHeight()));
    trans.preConcatenate (AffineTransform.getTranslateInstance (
      -base.getX(), -base.getY()));
    trans.preConcatenate (AffineTransform.getRotateInstance (
      Math.toRadians (-angle)));
    trans.preConcatenate (AffineTransform.getTranslateInstance (
      base.getX(), base.getY()));

    // Prepare area
    // ------------
    area = new Area (bounds);
    area.transform (trans);

  } // prepare

  ////////////////////////////////////////////////////////////

  /** Gets the text string. */
  public String getText () { return (text); }

  ////////////////////////////////////////////////////////////

  /** Sets the text string. */
  public void setText (String text) { this.text = text; invalidate(); }

  ////////////////////////////////////////////////////////////

  /** Sets the text base point. */
  public void setBasePoint (Point2D base) { 
    this.base = (Point2D)base.clone(); 
    invalidate();
  } // setBasePoint

  ////////////////////////////////////////////////////////////

  /** Gets the text base point. */
  public Point2D getBasePoint () { return ((Point2D) base.clone()); }

  ////////////////////////////////////////////////////////////

  /** Sets the text orientation angle. */
  public void setAngle (double angle) { this.angle = angle; invalidate(); }

  ////////////////////////////////////////////////////////////

  /** Sets the text font. */
  public void setFont (Font font) { this.font = font; invalidate(); }

  ////////////////////////////////////////////////////////////

  /** Gets the text font. */
  public Font getFont () { return (font); }

  ////////////////////////////////////////////////////////////

  /** Sets the text alignment. */
  public void setAlignment (double[] align) {
    this.align = (align == null ? new double[]{0,0} : (double[])align.clone());
    invalidate();
  } // setAlignment

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new text element with the specified string and base
   * point.  The font is initialized to the default 12 point plain
   * font, the alignment to [0,0], and the angle to 0.
   *
   * @param text the text string.
   * @param base the text base point.
   */
  public TextElement ( 
    String text,
    Point2D base
  ) {

    this (text, new Font (null, Font.PLAIN, 12), base, null, 0);

  } // TextElement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new text element with the specified properties.
   *
   * @param text the text string.
   * @param font the text font.
   * @param base the base point.
   * @param align the text alignment as [horizontal, vertical].  The
   * alignment is a set of normalized values in the range [0..1] which
   * represent where in the bounding box of the text string the base
   * point is located.  For example if the alignment is [0,0], then
   * the base point is at the lower-left corner of the text string.
   * If the alignment is [0.5, 0], then the base point is at the
   * bottom-center of the text string.  If null, the alignment is set
   * to [0,0].
   * @param angle the orientation angle.  The angle is measured in
   * degrees and represents the angle between the horizontal and the
   * text string baseline direction.  For example, and angle of 45
   * would draw text from lower-left to upper-right.
   */
  public TextElement ( 
    String text,
    Font font,
    Point2D base,
    double[] align,
    double angle
  ) {

    // Initialize variables
    // --------------------
    this.text = text;
    this.font = font;
    this.base = (Point2D) base.clone();
    this.align = (align == null ? new double[]{0,0} : (double[])align.clone());
    this.angle = angle;
    invalidate();

  } // TextElement

  ////////////////////////////////////////////////////////////

  public void render (
    Graphics2D g2,
    Color foreground,
    Color background
  ) {

    // Initialize properties
    // ---------------------
    Graphics2D g = (Graphics2D) g2.create();
    g.setFont (font);
    if (bounds == null) prepare (g);

    // Render text with shadow
    // -----------------------
    if (background != null) {

      // Draw simple drop shadow
      // -----------------------
      boolean drawFuzzy = (
        GraphicsServices.supportsAlpha (g) &&
        (g.getRenderingHint (RenderingHints.KEY_ANTIALIASING) == 
          RenderingHints.VALUE_ANTIALIAS_ON ||
        g.getRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING) == 
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      );
      if (!drawFuzzy) {
        int offset = (int) Math.max (1, 
          Math.round (font.getSize2D()*SHADOW_OFFSET));
        AffineTransform saved = g.getTransform();
        g.translate (offset, offset);
        g.transform (trans);
        g.setColor (background);
        float baseX = (float) base.getX();
        float baseY = (float) base.getY();
        g.drawString (text, baseX, baseY);
        g.setTransform (saved);
        g.transform (trans);
        g.setColor (foreground);
        g.drawString (text, baseX, baseY);
        g.setTransform (saved);
      } // if

      // Draw fuzzy translucent drop shadow
      // ----------------------------------
      else {

        // Create image for shadow
        // -----------------------
        int border = KERNEL_SIZE-1;
        Rectangle bounds = area.getBounds();
        int width = bounds.width + border*2;
        int height = bounds.height + border*2;
        int x = bounds.x - border;
        int y = bounds.y - border;
        BufferedImage letterImage = new BufferedImage (width, height,
          BufferedImage.TYPE_INT_ARGB_PRE);

        // Draw shadow image and blur
        // --------------------------
        Graphics2D letterGraphics = letterImage.createGraphics();
        letterGraphics.setColor (background);
        letterGraphics.setFont (font);
        letterGraphics.setRenderingHints (g.getRenderingHints());
        letterGraphics.translate (-x, -y);
        letterGraphics.transform (trans);
        letterGraphics.drawString (text, (float) base.getX(), 
          (float) base.getY());
        letterGraphics.dispose();
        BufferedImage letterImageBlurred = 
          shadowConvolve.filter (letterImage, null);

        // Transfer shadow image to output
        // -------------------------------
        Composite savedComposite = g.getComposite();
        g.setComposite (AlphaComposite.getInstance (AlphaComposite.SRC_OVER, 
          0.75f));
        g.drawImage (letterImageBlurred, x, y, null);
        g.setComposite (savedComposite);

        // Draw actual text to output
        // --------------------------
        letterImage = new BufferedImage (width, height,
          BufferedImage.TYPE_INT_ARGB_PRE);
        letterGraphics = letterImage.createGraphics();
        letterGraphics.setColor (foreground);
        letterGraphics.setFont (font);
        letterGraphics.setRenderingHints (g.getRenderingHints());
        letterGraphics.translate (-x, -y);
        letterGraphics.transform (trans);
        letterGraphics.drawString (text, (float) base.getX(), 
          (float) base.getY());
        letterGraphics.dispose();
        g.drawImage (letterImage, x, y, null);

      } // else

    } // if

    // Render text with no shadow
    // --------------------------
    else {
      AffineTransform saved = g.getTransform();
      g.transform (trans);
      g.setColor (foreground);
      g.drawString (text, (float) base.getX(), (float) base.getY());
      g.setTransform (saved);
    } // else

    g.dispose();

  } // render

  ////////////////////////////////////////////////////////////

  public Area getArea (
    Graphics2D g
  ) { 

    if (bounds == null) prepare (g);
    return ((Area)area.clone());

  } // getArea

  ////////////////////////////////////////////////////////////

} // TextElement class

////////////////////////////////////////////////////////////////////////
