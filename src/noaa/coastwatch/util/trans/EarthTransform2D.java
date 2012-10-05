////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthTransform2D.java
  PURPOSE: Performs two-dimensional Earth transform computations.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/30
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;

/**
 * The <code>EarthTransform2D</code> class adds extra functionality to
 * its super class common to two-dimensional Earth transforms.  It
 * also allows the use of a raster-is-point transform mode rather than
 * the default raster-is-area mode.  In raster-is-area mode, the Earth
 * location returned by {@link #transform(DataLocation,EarthLocation)}
 * refers to the center of the raster pixel area.  In raster-is-point
 * mode, the extra method {@link #transformToPoint} may be used to
 * retrieve the Earth location of the point data, which may not be at
 * the center of the raster pixel area.
 */
public abstract class EarthTransform2D
  extends EarthTransform {

  // Variables
  // ---------

  /** The transform used for raster point transforms. */
  private EarthTransform2D pointTrans = this;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data to geographic transform used when {@link
   * #transformToPoint} is called.  This transform is different from
   * the normal 2D transform in that it returns Earth locations that
   * correspond to a raster-is-point style model rather than
   * raster-is-area.  By default, {@link #transformToPoint} returns
   * the same value as {@link #transform(DataLocation,EarthLocation)}.
   *
   * @param pointTrans the point transform to use for raster-is-point
   * coordinate transformations.
   */
  public void setPointTransform (
    EarthTransform2D pointTrans
  ) {

    this.pointTrans = pointTrans;

  } // setPointTransform

  ////////////////////////////////////////////////////////////

  /**
   * Gets a polygon bounding box for this transform using the
   * specified data coordinate limits.
   *
   * @param upperLeft the upper-left corner of the data window.
   * @param lowerRight the lower-right corner of the data window.
   * @param segments the number of segments along each side of the
   * data window.
   */
  public LineFeature getBoundingBox (
    DataLocation upperLeft,
    DataLocation lowerRight,
    int segments
  ) {

    // Create new polygon
    // ------------------
    LineFeature polygon = new LineFeature();

    // Trace top
    // ---------
    for (int i = 0; i < segments; i++) {
      double t = (double) i/segments;
      DataLocation loc = new DataLocation (
        upperLeft.get(Grid.ROWS),
        upperLeft.get(Grid.COLS)*(1-t) + lowerRight.get(Grid.COLS)*t
      );
      polygon.add (transform (loc));
    } // for

    // Trace right
    // -----------
    for (int i = 0; i < segments; i++) {
      double t = (double) i/segments;
      DataLocation loc = new DataLocation (
        upperLeft.get(Grid.ROWS)*(1-t) + lowerRight.get(Grid.ROWS)*t,
        lowerRight.get(Grid.COLS)
      );
      polygon.add (transform (loc));
    } // for

    // Trace bottom
    // ------------
    for (int i = 0; i < segments; i++) {
      double t = (double) i/segments;
      DataLocation loc = new DataLocation (
        lowerRight.get(Grid.ROWS),
        lowerRight.get(Grid.COLS)*(1-t) + upperLeft.get(Grid.COLS)*t
      );
      polygon.add (transform (loc));
    } // for

    // Trace left
    // ----------
    for (int i = 0; i < segments; i++) {
      double t = (double) i/segments;
      DataLocation loc = new DataLocation (
        lowerRight.get(Grid.ROWS)*(1-t) + upperLeft.get(Grid.ROWS)*t,
        upperLeft.get(Grid.COLS)
      );
      polygon.add (transform (loc));
    } // for

    // Add final point
    // ---------------
    polygon.add (transform (upperLeft));

    return (polygon);

  } // getBoundingBox

  ////////////////////////////////////////////////////////////

  /**
   * Converts data coordinates to geographic coordinates (raster point
   * mode).  By default, this method simply calls {@link
   * #transform(DataLocation,EarthLocation)} if no point transform is
   * set with a call to {@link #setPointTransform}.  Otherwise, it
   * passes the data location to the stored point transform.
   *
   * @param dataLoc the data location.
   * @param earthLoc the Earth location or null.  If null, an object
   * is created and returned.  If non-null, the object is simply
   * modified.
   *
   * @return the Earth location.  The Earth location may contain
   * <code>Double.NaN</code> if no conversion is possible.
   *
   * @see #transform(DataLocation)
   */
  public EarthLocation transformToPoint (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    return (pointTrans.transform (dataLoc, earthLoc));

  } // transformToPoint

  ////////////////////////////////////////////////////////////

} // EarthTransform2D class

////////////////////////////////////////////////////////////////////////
