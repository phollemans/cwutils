////////////////////////////////////////////////////////////////////////
/*

     File: AnnotationOverlay.java
   Author: Peter Hollemans
     Date: 2002/09/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.render.AnnotationElement;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.PolygonOverlay;

/**
 * An annotation overlay annotes a data view with annotation elements.
 * Annotation elements may be based in the <i>image reference
 * frame</i> or the <i>data reference frame</i>.  With image
 * referencing, the annotation location and size follow the image
 * coordinates and do not change when the view window location and
 * size changes.  With data referencing, the annotation follows the
 * data coordinates and changes with the view window and size.
 *
 * @author Peter Hollemans
 * @since 3.1.1
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
