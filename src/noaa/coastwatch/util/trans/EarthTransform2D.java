////////////////////////////////////////////////////////////////////////
/*

     File: EarthTransform2D.java
   Author: Peter Hollemans
     Date: 2005/05/30

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.geom.AffineTransform;
import noaa.coastwatch.render.feature.LineFeature;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.PolarStereographicProjection;
import noaa.coastwatch.util.trans.SpheroidConstants;

import java.util.logging.Logger;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The <code>EarthTransform2D</code> class adds extra functionality to
 * its super class common to two-dimensional earth transforms.  It
 * also allows the use of a raster-is-point transform mode rather than
 * the default raster-is-area mode.  In raster-is-area mode, the Earth
 * location returned by {@link #transform(DataLocation,EarthLocation)}
 * refers to the center of the raster pixel area.  In raster-is-point
 * mode, the extra method {@link #transformToPoint} may be used to
 * retrieve the earth location of the point data, which may not be at
 * the center of the raster pixel area.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
@noaa.coastwatch.test.Testable
public abstract class EarthTransform2D
  extends EarthTransform {

  private static final Logger LOGGER = Logger.getLogger (EarthTransform2D.class.getName());

  // Variables
  // ---------

  /** The transform used for raster point transforms (possibly null). */
  private EarthTransform2D pointTrans;

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
   * the normal 2D transform in that it returns earth locations that
   * correspond to a raster-is-point style model rather than
   * raster-is-area.  By default, {@link #transformToPoint} returns
   * the same value as {@link #transform(DataLocation,EarthLocation)}.
   *
   * @param pointTrans the point transform to use for raster-is-point
   * coordinate transformations or null to use this transform.
   */
  public void setPointTransform (
    EarthTransform2D pointTrans
  ) {

    // FIXME: Should we do a deep copy here when the transform is cloned or
    // adjusted?

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
        upperLeft.get(ROW),
        upperLeft.get(COL)*(1-t) + lowerRight.get(COL)*t
      );
      polygon.add (transform (loc));
    } // for

    // Trace right
    // -----------
    for (int i = 0; i < segments; i++) {
      double t = (double) i/segments;
      DataLocation loc = new DataLocation (
        upperLeft.get(ROW)*(1-t) + lowerRight.get(ROW)*t,
        lowerRight.get(COL)
      );
      polygon.add (transform (loc));
    } // for

    // Trace bottom
    // ------------
    for (int i = 0; i < segments; i++) {
      double t = (double) i/segments;
      DataLocation loc = new DataLocation (
        lowerRight.get(ROW),
        lowerRight.get(COL)*(1-t) + upperLeft.get(COL)*t
      );
      polygon.add (transform (loc));
    } // for

    // Trace left
    // ----------
    for (int i = 0; i < segments; i++) {
      double t = (double) i/segments;
      DataLocation loc = new DataLocation (
        lowerRight.get(ROW)*(1-t) + upperLeft.get(ROW)*t,
        upperLeft.get(COL)
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
   * @param earthLoc the earth location to get the world axes.  The location
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
    northVector[ROW] = tipLoc.get (ROW) - baseLoc.get (ROW);
    northVector[COL] = tipLoc.get (COL) - baseLoc.get (COL);
    norm (northVector);

    // Compute east vector
    // -------------------
    EarthLocation baseEarthLoc = earthLoc.translate (0, -0.01);
    EarthLocation tipEarthLoc = earthLoc.translate (0, 0.01);
    transform (earthLoc.translate (0, -0.01), baseLoc);
    transform (earthLoc.translate (0, 0.01), tipLoc);
    eastVector[ROW] = tipLoc.get (ROW) - baseLoc.get (ROW);
    eastVector[COL] = tipLoc.get (COL) - baseLoc.get (COL);
    norm (eastVector);

  } // getWorldAxes

  ////////////////////////////////////////////////////////////

  /**
   * Converts data coordinates to geographic coordinates (raster point
   * mode).  By default, this method simply calls {@link
   * #transform(DataLocation,EarthLocation)} if no point transform has been
   * set with a call to {@link #setPointTransform}.  Otherwise, it
   * passes the data location to the stored point transform.
   *
   * @param dataLoc the data location.
   * @param earthLoc the earth location or null.  If null, an object
   * is created and returned.  If non-null, the object is simply
   * modified.
   *
   * @return the earth location.  The earth location may contain
   * <code>Double.NaN</code> if no conversion is possible.
   *
   * @see #transform(DataLocation)
   */
  public EarthLocation transformToPoint (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    EarthLocation loc;

    if (pointTrans != null) loc = pointTrans.transform (dataLoc, earthLoc);
    else loc = transform (dataLoc, earthLoc);

    return (loc);
    
  } // transformToPoint

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (EarthTransform2D.class);

    logger.test ("Framework");
    EarthTransform2D trans = new EarthTransform2D () {
      protected void transformImpl (
        DataLocation dataLoc,
        EarthLocation earthLoc
      ) {
        earthLoc.setCoords (-dataLoc.get (ROW), dataLoc.get (COL));
      }
      protected void transformImpl (
        EarthLocation earthLoc,
        DataLocation dataLoc
      ) {
        dataLoc.set (ROW, -earthLoc.lat);
        dataLoc.set (COL, earthLoc.lon);
      }
      public String describe () { return (null); }
    };
    logger.passed();
    
    logger.test ("getWorldAxes");
    double[] north = new double[2];
    double[] east = new double[2];
    EarthLocation center = new EarthLocation();
    trans.getWorldAxes (center, north, east);
    double epsilon = 1e-5;
    assert (Math.abs (north[ROW] - (-1)) < epsilon);
    assert (Math.abs (north[COL] - 0) < epsilon);
    assert (Math.abs (east[ROW] - 0) < epsilon);
    assert (Math.abs (east[COL] - 1) < epsilon);
    
    center.markInvalid();
    trans.getWorldAxes (center, north, east);
    assert (Double.isNaN (north[ROW]));
    assert (Double.isNaN (north[COL]));
    assert (Double.isNaN (east[ROW]));
    assert (Double.isNaN (east[COL]));

    MapProjection map = new PolarStereographicProjection (
      SpheroidConstants.SPHEROID_SEMI_MAJOR[12],
      SpheroidConstants.SPHEROID_SEMI_MINOR[12],
      new int[] {512, 512},
      new java.awt.geom.AffineTransform(),
      0, Math.toRadians (90), 0, 0);
    map = map.getModified (new EarthLocation (90, 0), new double[] {1, 1});
    center.setCoords (45, 45);
    map.getWorldAxes (center, north, east);
    assert (north[ROW] < 0);
    assert (north[COL] < 0);
    assert (east[ROW] < 0);
    assert (east[COL] > 0);

    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // EarthTransform2D class

////////////////////////////////////////////////////////////////////////
