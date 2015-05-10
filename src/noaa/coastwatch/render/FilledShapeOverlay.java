////////////////////////////////////////////////////////////////////////
/*
     FILE: FilledShapeOverlay.java
  PURPOSE: An overlay for generic filled shapes.
   AUTHOR: Peter Hollemans
     DATE: 2004/04/24
  CHANGES: 2005/03/22, PFH, added transparency handling

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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.PolygonOverlay;

/**
 * The <code>FilledShapeOverlay</code> class may be used to render a
 * list of generic filled shapes.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class FilledShapeOverlay 
  extends PolygonOverlay {

  // Variables
  // ---------

  /** The list of shapes to draw. */
  private List shapeList;
  
  ////////////////////////////////////////////////////////////

  /** 
   * Adds a new shape to the list.  The shape coordinates are considered
   * to be in the data coordinate reference frame.
   *
   * @param shape the new shape to add to the list.
   */
  public void addShape (
    Shape shape
  ) {

    shapeList.add (shape);

  } // addShape

  ////////////////////////////////////////////////////////////

  /** Gets an iterator over the list of shapes. */
  public Iterator getShapeIterator () { return (shapeList.iterator()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Removes a shape from the list.
   *
   * @param shape the shape to remove from the list.
   */
  public void removeShape (
    Shape shape
  ) {

    shapeList.remove (shape);

  } // removeShape

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new shape overlay with the specified color.  The layer
   * number is initialized to 0, and the stroke to the default
   * <code>BasicStroke</code>, and the fill color to null.
   */
  public FilledShapeOverlay (
    Color color
  ) {

    // Initialize
    // ----------
    super (color);
    setFillColor (null);
    shapeList = new LinkedList();

  } // FilledShapeOverlay constructor

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

    // Setup for drawing
    // -----------------
    AffineTransform at = view.getTransform().getImageTransform().getAffine();
    g.setStroke (getStroke());

    // Draw shapes
    // -----------
    Color color = getColorWithAlpha();
    Color fillColor = getFillColorWithAlpha();
    for (Iterator iter = shapeList.iterator(); iter.hasNext(); ) {
      Shape shape = at.createTransformedShape ((Shape) iter.next());
      if (fillColor != null) {
        g.setColor (fillColor);
        g.fill (shape);
      } // if
      if (color != null) {
        g.setColor (color);
        g.draw (shape);
      } // if
    } // for

  } // draw

  ////////////////////////////////////////////////////////////

} // FilledShapeOverlay class

////////////////////////////////////////////////////////////////////////
