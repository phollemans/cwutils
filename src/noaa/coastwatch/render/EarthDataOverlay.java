////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataOverlay.java
  PURPOSE: A data overlay specifies information used for the
           annotation of a data view.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/18
  CHANGES: 2002/07/23, PFH, added package, javadoc
           2002/03/09, PFH, renamed to EarthDataOverlay and rearranged
           2002/11/29, PFH, added last Earth image transform, prepare, draw
           2002/12/15, PFH, added isPrepared
           2004/03/01, PFH, added visible and name properties
           2004/03/03, PFH, changed render() to use isPrepared()
           2004/03/07, PFH, added clone() method
           2004/03/09, PFH, added protected prepared flag
           2004/03/26, PFH, added extra docs on cloning
           2004/03/29, PFH, added needsPrepare() method
           2004/04/04, PFH, added serialization
           2004/10/17, PFH, added invalidate(), serialization constant
           2005/03/21, PFH, added transparency handling
           2005/03/27, PFH, added readObject() to set alpha value 
             on deserialization
           2006/07/07, PFH, added getColors()

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;

/**
 * An Earth data overlay specifies information used for the annotation
 * of an Earth data view, for example grid lines, coastlines, symbols,
 * text, and so on.  All overlays have a color, transparency, layer
 * number, visibility flag, and name.  The layer number may be used by
 * rendering software to determine the order for rendering multiple
 * overlays.  A lower layer number indicates that the overlay should
 * be rendered first.  The visibility flag may be used to temporarily
 * hide an overlay from the rendered output.  The overlay name may be
 * used to reference the overlay in a list.
 *
 * This class implements <code>Cloneable</code> to provide a simple
 * shallow copy of the object.  If child classes have any deep mutable
 * data structures such as lists, they should override the
 * <code>clone()</code> method.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class EarthDataOverlay
  implements Comparable, Cloneable, Serializable {

  // Constants
  // ---------

  /** The serialization constant. */
  static final long serialVersionUID = -7008374236800330226L;

  // Variables
  // ---------

  /** The overlay color. */
  private Color color;

  /** The overlay layer number. */
  private int layer;

  /** The last Earth image transform used for rendering. */
  protected transient EarthImageTransform lastTrans;

  /** The overlay visibility flag. */
  private boolean isVisible;

  /** The overlay name. */
  private String name;

  /** 
   * The prepared flag, true if this overlay is ready to be drawn.
   * This flag may be used by child classes to indicate that changes
   * have occurred that require the child is be re-prepared for
   * drawing.
   */
  protected transient boolean prepared;

  /** The overlay alpha value. */
  protected int alpha = 255;

  /** 
   * The flag indicating that this overlay has a valid alpha value.
   * This flag is used by the deserialization code to detect if this
   * overlay was saved with an alpha value or not.  If true, then this
   * is a newer style of overlay with an alpha value and the value
   * should be retained after serialization.  If false, then this is
   * an older overlay without an alpha value and the value should be
   * set to the default of 255.
   */
  private boolean hasValidAlpha = true;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the overlay color.  Only opaque colors are allowed.  If the
   * color has an alpha component, the alpha value is set to 255 and
   * the overlay transparency is set using the new alpha value.  This
   * behaviour may be changed in the future to ignore the color's
   * alpha value entirely -- overlay transparency should really be set
   * by calling {@link #setTransparency}.
   *
   * @param color the new opaque overlay color.
   */
  public void setColor (
    Color color
  ) { 
    
    if (color != null && color.getAlpha() != 255) {
      this.color = new Color (color.getRGB());
      this.alpha = color.getAlpha();
    } // if
    else
      this.color = color; 

  } // setColor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the overlay color.  The overlay color is always opaque, even
   * if a call to {@link #setColor} specifies a color with an alpha
   * component.  The overlay transparency may be accessed through
   * {@link #setTransparency} or {@link #getTransparency}.
   *
   * @return the overlay color.
   */
  public Color getColor () { return (color); }

  ////////////////////////////////////////////////////////////

  /** Gets the overlay color with alpha component. */
  public Color getColorWithAlpha () { 

    return (getAlphaVersion (color));

  } // getColorWithAlpha

  ////////////////////////////////////////////////////////////

  /** Sets the overlay layer. */
  public void setLayer (int layer) { this.layer = layer; }

  ////////////////////////////////////////////////////////////

  /** Gets the overlay layer. */
  public int getLayer () { return (layer); }

  ////////////////////////////////////////////////////////////

  /** Sets the overlay name. */
  public void setName (String name) { this.name = name; }

  ////////////////////////////////////////////////////////////

  /** Gets the overlay name. */
  public String getName () { return (name); }

  ////////////////////////////////////////////////////////////

  /** Sets the overlay visibility flag. */
  public void setVisible (boolean flag) { this.isVisible = flag; }

  ////////////////////////////////////////////////////////////

  /** Gets the overlay visibility flag. */
  public boolean getVisible () { return (isVisible); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new data overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   */
  protected EarthDataOverlay (
    Color color,
    int layer
  ) { 

    setColor (color);
    this.layer = layer;
    lastTrans = null;
    name = this.getClass().getName();
    isVisible = true;

  } // EarthDataOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new data overlay.  The layer number is
   * initialized to 0.
   * 
   * @param color the overlay color.
   */
  protected EarthDataOverlay (
    Color color
  ) { 

    this (color, 0);

  } // EarthDataOverlay constructor

  ////////////////////////////////////////////////////////////

  public int compareTo (
    Object o
  ) throws ClassCastException {

    // Check for overlay
    // -----------------
    if (!(o instanceof EarthDataOverlay)) 
      throw new ClassCastException();

    // Compare layers
    // --------------
    return (this.layer - ((EarthDataOverlay) o).layer);

  } // compareTo

  ////////////////////////////////////////////////////////////

  /**
   * Gets the status of the overlay preparation. If the overlay is
   * prepared, a render call will return after almost no delay.  If
   * not, the render may require time to complete due to loading data
   * from disk or cache, converting Earth locations to screen points,
   * and so on.
   *
   * @param view the Earth data view for the next rendering operation.
   */
  public boolean isPrepared (
    EarthDataView view
  ) { 

    if (!needsPrepare()) 
      return (true);
    else 
      return (lastTrans != null && lastTrans == view.getTransform() && 
              prepared);

  } // isPrepared

  ////////////////////////////////////////////////////////////

  /** 
   * Determines if this overlay class needs to be prepared for
   * rendering.  This method returns true unless overridden in the
   * child class.
   *
   * @return true if this overlay class needs a preparation stage, or
   * false if not.
   */
  protected boolean needsPrepare () { return (true); }

  ////////////////////////////////////////////////////////////

  /** 
   * Renders the overlay graphics. 
   * 
   * @param g the graphics object for drawing.
   * @param view the Earth data view.
   */
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

    // Draw graphics
    // -------------
    draw (g, view);

  } // render
 
  ////////////////////////////////////////////////////////////

  /** 
   * Prepares the overlay graphics prior to drawing.
   * 
   * @param g the graphics object for drawing.
   * @param view the Earth data view.
   */
  protected abstract void prepare (
    Graphics2D g,
    EarthDataView view
  );
 
  ////////////////////////////////////////////////////////////

  /** 
   * Draws the overlay graphics.
   * 
   * @param g the graphics object for drawing.
   * @param view the Earth data view.
   */
  protected abstract void draw (
    Graphics2D g,
    EarthDataView view
  );
 
  ////////////////////////////////////////////////////////////

  /** Creates and returns a copy of this object. */
  public Object clone () {

    try {
      EarthDataOverlay overlay = (EarthDataOverlay) super.clone();
      return (overlay);
    } // try
    catch (CloneNotSupportedException e) {
      return (null);
    } // catch

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Invalidates the overlay.  This causes the overlay data and
   * graphics to be completely reconstructed upon the next call to
   * <code>render()</code>.  This method does nothing unless
   * overridden in the child class.
   */
  public void invalidate () {

    // do nothing

  } // invalidate

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the overlay transparency.
   *
   * @param trans the transparency level in percent from 0 to 100.  A
   * transparency of 0% is completely opaque, and 100% is completely
   * transparent.
   */
  public void setTransparency (int trans) { 

    if (trans < 0) trans = 0;
    else if (trans > 100) trans = 100;
    alpha = (int) Math.round (((100-trans)/100.0)*255);

  } // setTransparency

  ////////////////////////////////////////////////////////////

  /**
   * Gets the overlay transparency.
   * 
   * @return the transparency in the range 0 to 100 percent.
   */
  public int getTransparency () {

    int trans = (int) Math.round (100 - (alpha/255.0)*100);
    return (trans);

  } // getTransparency

  ////////////////////////////////////////////////////////////
  
  /** 
   * Gets a version of the color with the overlay transparency
   * applied.
   *
   * @param color the color to convert.
   *
   * @return a new version of the color with the alpha component set
   * to the transparency of the overlay.
   */
  public Color getAlphaVersion (
    Color color
  ) {

    if (color == null) 
      return (null);
    else 
      return (new Color (color.getRed(), color.getGreen(), color.getBlue(), 
        alpha));

  } // getAlphaVersion

  ////////////////////////////////////////////////////////////

  /** 
   * Reads the object data from the input stream.  If the overlay was
   * serialized without an alpha value, than the alpha value is set to
   * 255.
   */
  private void readObject (
    ObjectInputStream in
  ) throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    if (!hasValidAlpha) {
      alpha = 255;
      hasValidAlpha = true;
    } // if

  } // readObject

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of all the colors used by this overlay.
   * 
   * @return the list of colors.
   */
  public List getColors () {

    ArrayList colorList = new ArrayList();
    colorList.add (color);
    return (colorList);

  } // getColors

  ////////////////////////////////////////////////////////////

} // EarthDataOverlay class

////////////////////////////////////////////////////////////////////////
