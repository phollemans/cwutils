////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthTransform2D.java
  PURPOSE: Performs two-dimensional Earth transform computations.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/30
  CHANGES: 2014/03/05, PFH
           - Changes: Added getWorldAxes(), isOrientable() and private norm() 
             methods with test code.
           - Issue: We needed a way to detect at the EarthTransform level
             the orientation of north and east vectors in order to create
             an orientation affine transform for display.  This seems like
             the logical place to put that code.  Also, the isOrientable() 
             method provides a hint to users of whether or not the transform
             should be oriented differently for display.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

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
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
@noaa.coastwatch.test.Testable
public abstract class EarthTransform2D
  extends EarthTransform {

  // Variables
  // ---------

  /** The transform used for raster point transforms. */
  private EarthTransform2D pointTrans = this;

  ////////////////////////////////////////////////////////////

  public EarthTransform2D get2DVersion () { return (this); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the orientation hint.  If a transform is orientable, then
   * it may be better for the user to have it rotated for display so 
   * that northerly locations occur where data locations have smaller
   * row values and southerly locations occur where data locations have
   * larger row values.  If a transform is not orientable, then it should
   * be presented to the user exactly as-is.
   *
   * @return true if the transform is orientable, or false if not.  Unless
   * overridden by the subclass, this method returns true.
   *
   * @since 3.3.1
   */
  public boolean isOrientable () { return (true); }

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

  /** Normalizes the 2D vector. */
  private void norm (double[] v) {

    double mag = Math.sqrt (v[0]*v[0] + v[1]*v[1]);
    if (mag != 0) {
      for (int i = 0; i < 2; i++) v[i] /= mag;
    } // if

  } // norm

  ////////////////////////////////////////////////////////////

  /**
   * Gets the world axes for this transform at the specified location.
   * The world axes are the north-pointing unit vector and east-pointing
   * unit vector in data coordinates.
   *
   * @param earthLoc the Earth location to get the world axes.  The location
   * must transform to a valid data location, or the output vectors may contain
   * NaN values.
   * @param northVector the north-pointing vector array of length 2 (modified).
   * @param eastVector the east-pointing vector array of length 2 (modified).
   *
   * @since 3.3.1
   */
   public void getWorldAxes (
     EarthLocation earthLoc,
     double[] northVector,
     double[] eastVector
   ) {

    DataLocation baseLoc = new DataLocation (2);
    DataLocation tipLoc = new DataLocation (2);
   
    // Compute north vector
    // --------------------
    transform (earthLoc.translate (-0.01, 0), baseLoc);
    transform (earthLoc.translate (0.01, 0), tipLoc);
    northVector[Grid.ROWS] = tipLoc.get (Grid.ROWS) - baseLoc.get (Grid.ROWS);
    northVector[Grid.COLS] = tipLoc.get (Grid.COLS) - baseLoc.get (Grid.COLS);
    norm (northVector);

    // Compute east vector
    // -------------------
    transform (earthLoc.translate (0, -0.01), baseLoc);
    transform (earthLoc.translate (0, 0.01), tipLoc);
    eastVector[Grid.ROWS] = tipLoc.get (Grid.ROWS) - baseLoc.get (Grid.ROWS);
    eastVector[Grid.COLS] = tipLoc.get (Grid.COLS) - baseLoc.get (Grid.COLS);
    norm (eastVector);

  } // getWorldAxes

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

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    System.out.print ("Testing getWorldAxes ... ");
    
    EarthTransform2D trans = new EarthTransform2D () {
      protected void transformImpl (
        DataLocation dataLoc,
        EarthLocation earthLoc
      ) {
        earthLoc.setCoords (-dataLoc.get (Grid.ROWS), dataLoc.get (Grid.COLS));
      }
      protected void transformImpl (
        EarthLocation earthLoc,
        DataLocation dataLoc
      ) {
        dataLoc.set (Grid.ROWS, -earthLoc.lat);
        dataLoc.set (Grid.COLS, earthLoc.lon);
      }
      public String describe () { return (null); }
    };
    double[] north = new double[2];
    double[] east = new double[2];
    EarthLocation center = new EarthLocation();
    trans.getWorldAxes (center, north, east);
    double epsilon = 1e-5;
    assert (Math.abs (north[Grid.ROWS] - (-1)) < epsilon);
    assert (Math.abs (north[Grid.COLS] - 0) < epsilon);
    assert (Math.abs (east[Grid.ROWS] - 0) < epsilon);
    assert (Math.abs (east[Grid.COLS] - 1) < epsilon);
    
    center.markInvalid();
    trans.getWorldAxes (center, north, east);
    assert (Double.isNaN (north[Grid.ROWS]));
    assert (Double.isNaN (north[Grid.COLS]));
    assert (Double.isNaN (east[Grid.ROWS]));
    assert (Double.isNaN (east[Grid.COLS]));

    MapProjection map = new PolarStereographicProjection (
      SpheroidConstants.SPHEROID_SEMI_MAJOR[12],
      SpheroidConstants.SPHEROID_SEMI_MINOR[12],
      new int[] {512, 512},
      new java.awt.geom.AffineTransform(),
      0, Math.toRadians (90), 0, 0);
    map = map.getModified (new EarthLocation (90, 0), new double[] {1, 1});
    center.setCoords (45, 45);
    map.getWorldAxes (center, north, east);
    assert (north[Grid.ROWS] < 0);
    assert (north[Grid.COLS] < 0);
    assert (east[Grid.ROWS] < 0);
    assert (east[Grid.COLS] > 0);

    System.out.println ("OK");

  } // main

  ////////////////////////////////////////////////////////////

} // EarthTransform2D class

////////////////////////////////////////////////////////////////////////
