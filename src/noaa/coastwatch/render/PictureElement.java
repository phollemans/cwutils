////////////////////////////////////////////////////////////////////////
/*
     FILE: PictureElement.java
  PURPOSE: A class to handle pictures and graphics for annotation.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/28
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Dimension;
import java.awt.geom.Point2D;
import noaa.coastwatch.render.AnnotationElement;

/**
 * A picture element is an annotation element the renders graphics and
 * shapes.  The element specifies various picture properties such as
 * the position and size.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class PictureElement
  extends AnnotationElement { 

  // Variables
  // ---------
  /** The top-left corner position. */
  protected Point2D position;

  /** The preferred dimensions. */
  protected Dimension preferred;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the picture position. 
   * 
   * @param position the top-left corner position of the picture.
   */
  public void setPosition (Point2D position) {

    this.position = (Point2D) position.clone();

  } // setPosition

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the preferred size of the picture.  The actual
   * picture size may be different than the preferred size.  The
   * picture aspect ratio is kept the same, so that the actual picture
   * fits into the preferred size rectangle.
   *
   * @param size the preferred picture size, or null if the picture size
   * should be determined by the element.
   */
  public void setPreferredSize (
    Dimension size
  ) {

    this.preferred = (size == null ? null : (Dimension) size.clone());

  } // setPreferredSize 

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new picture element with the specified properties.  The
   * actual picture size may be different than the preferred size.
   * The picture aspect ratio is kept the same, so that the actual
   * picture fits into the preferred size rectangle.
   *
   * @param position the top-left corner position of the picture.
   * @param size the preferred picture size, or null if the picture size
   * should be determined by the element.
   */   
  protected PictureElement ( 
    Point2D position,
    Dimension size
  ) {

    setPosition (position);
    setPreferredSize (size);

  } // PictureElement constructor

  ////////////////////////////////////////////////////////////

} // PictureElement class

////////////////////////////////////////////////////////////////////////
