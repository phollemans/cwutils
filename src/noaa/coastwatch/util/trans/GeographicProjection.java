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
import java.util.List;

import noaa.coastwatch.util.trans.GCTPCStyleProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.trans.BoundaryHandler;

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

  // Variables
  // ---------
  
  /** The longitude range for this projection. */
  private LongitudeRange lonRange;
  
  /** The angle at the edge of the projection. */
  private double alpha;

  ////////////////////////////////////////////////////////////

  /**
   * Updates the internal longitude range.
   *
   * @param minLon the minimum longitude value (outside edge).
   * @param maxLon the maximum longitude value (outside edge).
   *
   * @since 3.6.1
   */
  private void updateLongitudeRange (
    double minLon,
    double maxLon
  ) {

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

    // Create boundary handler
    // -----------------------
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

    double nextBoundary = boundary + 360;
    List<List<EarthLocation>> lineList = List.of (
      List.of (new EarthLocation (90, boundary), new EarthLocation (-90, boundary)),
      List.of (new EarthLocation (90, nextBoundary), new EarthLocation (-90, nextBoundary))
    );
    
    boundaryHandler = new BoundaryHandler ((a, b) -> isBoundaryCut (a, b), lineList);
    
    LOGGER.fine ("Longitude range type is " + lonRange + " with alpha = " + alpha);
    
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
    if (!affine.isIdentity()) setup();

  } // GeographicProjection constructor

  ////////////////////////////////////////////////////////////

  /**
   * Sets up the affine and longitude type to be internally consistent.
   *
   * @since 3.6.1
   */
  private void setup() throws NoninvertibleTransformException {
  
    double[] matrix = new double[6];
    inverseAffine.getMatrix (matrix);
    double[] pixelDims = new double[] {
      -matrix[1],
      matrix[2]
    };
    double centerLon = matrix[4] + pixelDims[1]*(dims[1]-1)/2.0;
    double centerLat = matrix[5] - pixelDims[0]*(dims[0]-1)/2.0;
    EarthLocation centerLoc = new EarthLocation (centerLat, centerLon);
    
    setAffine (centerLoc, pixelDims);
  
  } // setup

  ////////////////////////////////////////////////////////////

  @Override
  protected void setAffine (
    EarthLocation centerLoc,
    double[] pixelDims
  ) throws NoninvertibleTransformException {
  
    /*
     * We override here to not convert the lat/lon values
     * into radians before sending to the forward transform.  Also
     * to convert the center longitude to the new range type before
     * computing the affine transform.
     */

    // Update the longitude range type
    // -------------------------------
    double widthInDegrees = pixelDims[1]*((dims[1]-1)/2.0 + 0.5);
    int sign = pixelDims[1] > 0 ? 1 : -1;
    double startLon = centerLoc.lon - sign*widthInDegrees;
    double endLon = centerLoc.lon + sign*widthInDegrees;

    if (startLon < -360) { startLon += 360; endLon += 360; }
    double lonSpan = endLon - startLon;
    if (lonSpan > 360) endLon = startLon + 360;

    updateLongitudeRange (startLon, endLon);

    // Compute affine values
    // ---------------------
    
    // x     | a  c  e | R
    // y  =  | b  d  f | C
    // 1     | 0  0  1 | 1

    // x = a.R + c.C + e
    // y = b.R + d.C + f

    // a = 0 (x is independent of row)
    // c = px (x1-x0=px for adjacent columns, x varies linearly with column)
    // c.C + e = x
    // e = x - c.C for given x and C
    // Let: x = centerLon, C = (columns-1)/2, then
    // e = centerLon - px*(columns-1)/2
    
    // b = -py (y varies linearly with row but in reverse order)
    // d = 0 (y is independent of column)
    // b.R + f = y
    // f = y - b.R for given y and R
    // Let: y = centerLat, R = (rows-1)/2, then
    // f = centerLat + py*(rows-1)/2

    inverseAffine = new AffineTransform (
      0,                                                          // a
      -pixelDims[0],                                              // b
      pixelDims[1],                                               // c
      0,                                                          // d
      lonAdjust (centerLoc.lon - pixelDims[1]*(dims[1]-1)/2.0),   // e
      centerLoc.lat + pixelDims[0]*(dims[0]-1)/2.0                // f
    );
    forwardAffine = inverseAffine.createInverse();

  } // setAffine

  ////////////////////////////////////////////////////////////

  @Override
  public EarthTransform getSubset (
    DataLocation newOrigin,
    int[] newDims
  ) {

    GeographicProjection proj =
      (GeographicProjection) super.getSubset (newOrigin, newDims);
    try { proj.setup(); }
    catch (NoninvertibleTransformException e) {
      LOGGER.severe ("Internal error with non-invertible affine");
      throw (new IllegalStateException (e));
    } // catch
    
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
    try { proj.setup(); }
    catch (NoninvertibleTransformException e) {
      LOGGER.severe ("Internal error with non-invertible affine");
      throw (new IllegalStateException (e));
    } // catch
    
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

  /**
   * Adjusts the longitude value to be within the range dictated by the
   * longitude range type for this projection.
   *
   * @param lon the longitude value to adjust.
   *
   * @return the adjusted longitude value.
   *
   * @since 3.5.1
   */
  private double lonAdjust (
    double lon
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

    return (lon);
  
  } // lonAdjust

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    x[0] = lonAdjust (lon);
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

  /** The geographic implementation of the boundary cut test. */
  public boolean isBoundaryCut (
    EarthLocation a,
    EarthLocation b
  ) {
  
    // We find out here which of the two locations is the eastern location
    // and which one is the western.  We define east and west as being related
    // to the shortest longitude path, ie: the one that takes much less
    // than 180 degrees of longitude to span the distance between the points.
  
    EarthLocation east, west;
    if (a.isEast (b)) {
      east = a;
      west = b;
    } // if
    else {
      east = b;
      west = a;
    } // else
  
    // We adjust the longitudes here to the same range and then check what
    // we expect to be true, which is that if the shortest path stays strictly
    // within the range, the west value should be strictly less than the east
    // value.  If not, we must have crossed over the range boundary and wrapped
    // around.  If the longitudes are identical, we say that no boundary was
    // crossed.
  
    double westLon = lonAdjust (west.lon);
    double eastLon = lonAdjust (east.lon);
    boolean boundaryCut = (westLon > eastLon);

    return (boundaryCut);

  } // isBoundaryCut

  ////////////////////////////////////////////////////////////

} // GeographicProjection

////////////////////////////////////////////////////////////////////////
