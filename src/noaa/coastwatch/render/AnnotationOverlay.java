////////////////////////////////////////////////////////////////////////
/*
     FILE: AnnotationOverlay.java
  PURPOSE: An overlay for annotation elements.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/15
  CHANGES: 2002/11/29, PFH, added prepare, draw
           2004/03/28, PFH, added setReference, replaced Vector with
             ArrayList
           2004/03/29, PFH, added needsPrepare() method
           2005/03/22, PFH, added transparency handling

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import noaa.coastwatch.util.*;

/**
 * An annotation overlay annotes a data view with annotation elements.
 * Annotation elements may be based in the <i>image reference
 * frame</i> or the <i>data reference frame</i>.  With image
 * referencing, the annotation location and size follow the image
 * coordinates and do not change when the view window location and
 * size changes.  With data referencing, the annotation follows the
 * data coordinates and changes with the view window and size.
 */
public class AnnotationOverlay 
  extends PolygonOverlay {

  // Constants
  // ---------
  /** The image reference frame. */
  public static final int IMAGE = 0;

  /** The data reference frame. */
  public static final int DATA = 1;

  // Variables
  // ---------
  /** The list of annotation elements. */
  protected List elements;

  /** The annotation reference frame. */
  private int reference;

  ////////////////////////////////////////////////////////////

  /** Adds an annotation element to the list. */
  public void addElement (AnnotationElement element) { elements.add(element); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the annotation element reference frame. 
   *
   * @param frame the reference frame, either <code>IMAGE</code> and
   * <code>DATA</code>.
   */
  public void setReference (
    int frame
  ) {

    this.reference = frame;

  } // setReference

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new annotation overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param fillColor the fill color to use for polygon fills.
   * @param reference the annotation reference frame.  The options are
   * <code>IMAGE</code> and <code>DATA</code>.
   */
  public AnnotationOverlay (
    Color color,
    int layer,
    Stroke stroke,
    Color fillColor,
    int reference
  ) { 

    super (color, layer, stroke, fillColor);
    this.reference = reference;
    elements = new ArrayList();

  } // AnnotationOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new annotation overlay.  The layer number is
   * initialized to 0, the stroke to the default
   * <code>BasicStroke</code>, the fill color to null, and the
   * reference to the image reference frame.
   * 
   * @param color the overlay color.
   */
  public AnnotationOverlay (
    Color color
  ) { 

    super (color);
    setFillColor (null);
    this.reference = IMAGE;
    elements = new ArrayList();

  } // AnnotationOverlay constructor

  ////////////////////////////////////////////////////////////

  /** Returns false as this class needs no preparation. */
  protected boolean needsPrepare () { return (false); }

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // do nothing

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Apply data reference transform
    // ------------------------------
    AffineTransform saved = null;
    if (reference == DATA) {
      saved = g.getTransform();
      AffineTransform at = view.getTransform().getImageTransform().getAffine();
      g.transform (at);
    } // if

    // Render elements
    // ---------------
    Iterator iter = elements.iterator();
    g.setStroke (getStroke());
    while (iter.hasNext()) {
      AnnotationElement element = (AnnotationElement) iter.next();
      element.render (g, getColorWithAlpha(), getFillColorWithAlpha());
    } // while

    // Restore transform
    // -----------------
    if (reference == DATA)
      g.setTransform (saved);

  } // draw

  ////////////////////////////////////////////////////////////

} // AnnotationOverlay class

////////////////////////////////////////////////////////////////////////
