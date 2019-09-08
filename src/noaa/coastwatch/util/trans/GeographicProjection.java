////////////////////////////////////////////////////////////////////////
/*

     File: GeographicProjection.java
   Author: Peter Hollemans
     Date: 2012/11/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2012 National Oceanic and Atmospheric Administration
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
import java.awt.geom.NoninvertibleTransformException;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import noaa.coastwatch.util.trans.GCTPCStyleProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.DataLocation;

import java.util.logging.Logger;

/**
 * The <code>GeographicProjection</code> class performs 
 * geographic map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class GeographicProjection 
  extends GCTPCStyleProjection {

  private static final Logger LOGGER = Logger.getLogger (GeographicProjection.class.getName());

  // Constants
  // ---------
  
  /**
   * The longitude range types for geographic projections.
   * These are used to determine how to translate input longitudes
   * into the output column values.  The [ bracket in the following
   * indicates a hard limit, and ) bracket indicates a soft limit.
   * For example if longitude is in the range [-180, 180) that means
   * longitude is >= -180 and < 180.
   */
  private enum LongitudeRange {

    /*
    
    +--------------+--------------+--------------+--------------+
    |              |    *********************    |              |
    |              |    *********************    |              |
    +--------------+--------------+--------------+--------------+
  -360           -180             0             180            360

    Longitudes span the prime meridian
    Longitudes translated to [-180, 180) before translation to column

    */
    SPANS_PRIME,

    /*
    
    +--------------+--------------+--------------+--------------+
    |              |              |     *********************   |
    |              |              |     *********************   |
    +--------------+--------------+--------------+--------------+
  -360           -180             0             180            360
  
    Longitudes span the positive anti-meridian
    Longitudes translated to [0, 360) before translation to column

    */
    SPANS_ANTI_POSITIVE,

    /*
    
    +--------------+--------------+--------------+--------------+
    |    *********************    |              |              |
    |    *********************    |              |              |
    +--------------+--------------+--------------+--------------+
  -360           -180             0             180            360

    Longitudes span the negative anti-meridian
    Longitudes translated to [-360, 0) before translation to column

    */
    SPANS_ANTI_NEGATIVE,

    /*
    
    +--------------+--------------+--------------+--------------+
    |                   ********************************        |
    |                   ********************************        |
    +--------------+----+---------+--------------+--------------+
  -360           -180 alpha       0             180            360

    Longitudes span both the prime and anti-meridian on the positive side
    Longitudes translated to [alpha, alpha+360) before translation to column

    */
    SPANS_PRIME_ANTI_POSITIVE,

    /*
    
    +--------------+--------------+--------------+--------------+
    |       ********************************                    |
    |       ********************************                    |
    +--------------+--------------+--------+-----+--------------+
  -360           -180             0      alpha  180            360

    Longitudes span both the prime and anti-meridian on the negative side
    Longitudes translated to [alpha-360, alpha) before translation to column

    */
    SPANS_PRIME_ANTI_NEGATIVE

  } // LongitudeRange

  /** The nudge value for boundary splits. */
  private static final double BOUNDARY_NUDGE = 1.0e-10;

  // Variables
  // ---------
  
  /** The longitude range for this projection. */
  private LongitudeRange lonRange;
  
  /** The angle at the edge of the projection. */
  private double alpha;

  /** The geometry to use for splitting lines and polygons. */
  private Geometry splitter;

  ////////////////////////////////////////////////////////////

  /**
   * Updates the internal longitude range using the current affine
   * transform.
   *
   * @since 3.5.1
   */
  private void updateLongitudeRange () {

    // Check affine is initialized
    // ---------------------------
    if (forwardAffine.isIdentity()) {
      lonRange = LongitudeRange.SPANS_PRIME;
    } // if
    
    else {
    
      // Get minimum and maximum longitude
      // ---------------------------------
      double[] xyStart = new double[] {-0.5, -0.5};
      inverseAffine.transform (xyStart, 0, xyStart, 0, 1);
      double[] xyEnd = new double[] {dims[0] - 0.5, dims[1] - 0.5};
      inverseAffine.transform (xyEnd, 0, xyEnd, 0, 1);
      double minLon = Math.min (xyStart[0], xyEnd[0]);
      double maxLon = Math.max (xyStart[0], xyEnd[0]);

      // Adjust longitude span if greater than 360
      // -----------------------------------------
      double lonSpan = maxLon - minLon;
      if (lonSpan > 360) maxLon = minLon + 360;

      LOGGER.fine ("Minimum longitude = " + minLon);
      LOGGER.fine ("Maximum longitude = " + maxLon);

      // Determine longitude range type
      // ------------------------------
      if (minLon >= -180 && minLon <= 180 && maxLon >= -180 && maxLon <= 180)
        lonRange = LongitudeRange.SPANS_PRIME;

      else if (minLon >= 0 && minLon <= 180 && maxLon >= 180 && maxLon <= 360)
        lonRange = LongitudeRange.SPANS_ANTI_POSITIVE;

      else if (minLon >= -360 && minLon <= -180 && maxLon >= -180 && maxLon <= 0)
        lonRange = LongitudeRange.SPANS_ANTI_NEGATIVE;

      else if (minLon <= 0 && maxLon >= 180) {
        alpha = minLon;
        lonRange = LongitudeRange.SPANS_PRIME_ANTI_POSITIVE;
      } // else if
      
      else if (minLon <= -180 && maxLon >= 0) {
        alpha = maxLon;
        lonRange = LongitudeRange.SPANS_PRIME_ANTI_NEGATIVE;
      } // else if

      else
        throw new IllegalStateException ("Unsupported longitude range type in geographic projection");

      // Create splitting geometry
      // -------------------------
      double boundary;
      switch (lonRange) {

      case SPANS_PRIME:
        boundary = 180;
        break;

      case SPANS_ANTI_POSITIVE:
      case SPANS_ANTI_NEGATIVE:
        boundary = 0;
        break;

      case SPANS_PRIME_ANTI_POSITIVE:
      case SPANS_PRIME_ANTI_NEGATIVE:
        boundary = alpha;
        break;

      default:
        throw new IllegalStateException ("Unsupported longitude range type in geographic projection");

      } // switch

      GeometryFactory factory = new GeometryFactory();

      Polygon poly = factory.createPolygon (new Coordinate[] {
        new Coordinate (boundary - BOUNDARY_NUDGE, 90),
        new Coordinate (boundary + BOUNDARY_NUDGE, 90),
        new Coordinate (boundary + BOUNDARY_NUDGE, -90),
        new Coordinate (boundary - BOUNDARY_NUDGE, -90),
        new Coordinate (boundary - BOUNDARY_NUDGE, 90)
      });
      double nextBoundary = boundary + 360;
      Polygon nextPoly = factory.createPolygon (new Coordinate[] {
        new Coordinate (nextBoundary - BOUNDARY_NUDGE, 90),
        new Coordinate (nextBoundary + BOUNDARY_NUDGE, 90),
        new Coordinate (nextBoundary + BOUNDARY_NUDGE, -90),
        new Coordinate (nextBoundary - BOUNDARY_NUDGE, -90),
        new Coordinate (nextBoundary - BOUNDARY_NUDGE, 90)
      });
      splitter = poly.union (nextPoly);

      LOGGER.fine ("Longitude range type is " + lonRange + " with alpha = " + alpha);
    
    } // else
  
  } // updateLongitudeRange

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a map projection from the specified projection and
   * affine transform.   The {@link SpheroidConstants} and
   * {@link ProjectionConstants} class should be consulted for
   * valid parameter constants.
   *
   * @param rMajor the semi-major axis in meters.
   * @param rMinor the semi-minor axis in meters.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   */
  public GeographicProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    super (GEO, 0, rMajor, rMinor, dimensions, affine);
    updateLongitudeRange();

  } // GeographicProjection constructor

  ////////////////////////////////////////////////////////////

  @Override
  protected void setAffine (
    EarthLocation centerLoc,
    double[] pixelDims
  ) throws NoninvertibleTransformException {

    /*
     * We override here to not convert the lat/lon values
     * into radians before sending to the forward transform.
     */

    // Get map coordinates of new center point
    // ---------------------------------------
    double[] lonLat = new double[] {centerLoc.lon, centerLoc.lat};
    double[] xy = new double[2];
    mapTransformFor (lonLat, xy);
    
    // Create new affines
    // ------------------
    inverseAffine = new AffineTransform (
      0,
      -pixelDims[0],
      pixelDims[1],
      0,
      xy[0] - pixelDims[1]*(dims[1]-1)/2,
      xy[1] + pixelDims[0]*(dims[0]-1)/2
    );
    forwardAffine = inverseAffine.createInverse();

    // Update the longitude range type
    // -------------------------------
    updateLongitudeRange();

  } // setAffine

  ////////////////////////////////////////////////////////////

  @Override
  public EarthTransform getSubset (
    DataLocation newOrigin,
    int[] newDims
  ) {

    GeographicProjection proj =
      (GeographicProjection) super.getSubset (newOrigin, newDims);
    proj.updateLongitudeRange();
    
    return (proj);

  } // getSubset

  ////////////////////////////////////////////////////////////

  @Override
  public MapProjection getSubset (
    int[] start,
    int[] stride,
    int[] length
  ) {

    GeographicProjection proj =
      (GeographicProjection) super.getSubset (start, stride, length);
    proj.updateLongitudeRange();
    
    return (proj);

  } // getSubset

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    /*
     * We override here so that we perform no conversion to and from
     * radians.  This just loses precision of the original lat/lon values
     * in degrees and causes problems in the boundary split code.
     */
     
    double[] xy = dataLoc.getCoords();
    inverseAffine.transform (xy, 0, xy, 0, 1);
    double[] lonLat = new double[2];
    mapTransformInv (xy, lonLat);
    earthLoc.setCoords (lonLat[1], lonLat[0]);

  } // transformImpl

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    /*
     * We override here so that we perform no conversion to and from
     * radians.  This just loses precision of the original lat/lon values
     * in degrees and causes problems in the boundary split code.
     */
    
    double[] lonLat = new double[] {earthLoc.lon, earthLoc.lat};
    double[] xy = new double[2];
    mapTransformFor (lonLat, xy);
    forwardAffine.transform (xy, 0, xy, 0, 1);
    dataLoc.setCoords (xy);

  } // transformImpl

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    switch (lonRange) {

    case SPANS_PRIME:                   // Lon now in [-180, 180)
      break;

    case SPANS_ANTI_POSITIVE:           // Lon now in [0, 360)
      if (lon < 0) lon += 360;
      break;

    case SPANS_ANTI_NEGATIVE:           // Lon now in [-360, 0)
      if (lon >= 0) lon -= 360;
      break;

    case SPANS_PRIME_ANTI_POSITIVE:     // Lon now in [alpha, alpha+360)
      if (lon < alpha) lon += 360;
      break;

    case SPANS_PRIME_ANTI_NEGATIVE:     // Lon now in [alpha-360, alpha)
      if (lon >= alpha) lon -= 360;
      break;

    } // switch

    x[0] = lon;
    y[0] = lat;

    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    lon[0] = x;
    lat[0] = y;

    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

  @Override
  public boolean hasBoundaryCheck () { return (true); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isBoundaryCut (
    EarthLocation a,
    EarthLocation b
  ) {
  
    EarthLocation east, west;
    if (a.isEast (b)) {
      east = a;
      west = b;
    } // if
    else {
      east = b;
      west = a;
    } // else
  
    boolean boundaryCut = false;
  
    switch (lonRange) {

    case SPANS_PRIME:
      boundaryCut = (west.lon > east.lon);
      break;

    case SPANS_ANTI_POSITIVE:
      boundaryCut = (west.lon < 0 && east.lon >= 0);
      break;

    case SPANS_ANTI_NEGATIVE:
      boundaryCut = (west.lon < 0 && east.lon >= 0);
      break;

    case SPANS_PRIME_ANTI_POSITIVE:
      boundaryCut = (west.lon < alpha && east.lon >= alpha);
      break;

    case SPANS_PRIME_ANTI_NEGATIVE:
      boundaryCut = (west.lon < alpha && east.lon >= alpha);
      break;

    } // switch

    return (boundaryCut);

  } // isBoundaryCut

  ////////////////////////////////////////////////////////////

  @Override
  public Geometry getBoundarySplitter () { return (splitter); }

  ////////////////////////////////////////////////////////////

} // GeographicProjection

////////////////////////////////////////////////////////////////////////
