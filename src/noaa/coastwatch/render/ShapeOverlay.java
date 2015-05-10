////////////////////////////////////////////////////////////////////////
/*
     FILE: ShapeOverlay.java
  PURPOSE: An overlay for generic shapes.
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
import noaa.coastwatch.render.LineOverlay;

/**
 * The <code>ShapeOverlay</code> class may be used to render a list of
 * generic shapes.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ShapeOverlay 
  extends LineOverlay {

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
   * Creates a new shape overlay with the specified color.
   * The layer number is initialized to 0, and the stroke to the
   * default <code>BasicStroke</code>.
   */
  public ShapeOverlay (
    Color color
  ) {

    // Initialize
    // ----------
    super (color);
    shapeList = new LinkedList();

  } // ShapeOverlay constructor

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
    Color color = getColorWithAlpha();
    if (color == null) return;
    g.setColor (color);
    g.setStroke (getStroke());

    // Draw shapes
    // -----------
    for (Iterator iter = shapeList.iterator(); iter.hasNext(); ) {
      Shape shape = at.createTransformedShape ((Shape) iter.next());
      g.draw (shape);
    } // for

  } // draw

  ////////////////////////////////////////////////////////////

} // ShapeOverlay class

////////////////////////////////////////////////////////////////////////
