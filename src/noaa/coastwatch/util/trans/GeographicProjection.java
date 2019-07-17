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
import noaa.coastwatch.util.trans.GCTPCStyleProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.DataLocation;

import java.util.logging.Logger;

/**
 * The <code>GeographicProjection</code> class performs 
 * Geographic map projection calculations.
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
   * into the output column values.
   */
  private enum LongitudeRange {

    /*
    +--------------+--------------+--------------+--------------+
    |              |    *********************    |              |
    |              |    *********************    |              |
    +--------------+--------------+--------------+--------------+
  -360           -180             0             180            360
    */
    SPANS_PRIME,

    /*
    +--------------+--------------+--------------+--------------+
    |              |              |     *********************   |
    |              |              |     *********************   |
    +--------------+--------------+--------------+--------------+
  -360           -180             0             180            360
    */
    SPANS_ANTI_POSITIVE,

    /*
    +--------------+--------------+--------------+--------------+
    |    *********************    |              |              |
    |    *********************    |              |              |
    +--------------+--------------+--------------+--------------+
  -360           -180             0             180            360
    */
    SPANS_ANTI_NEGATIVE,

    /*
    +--------------+--------------+--------------+--------------+
    |                   ********************************        |
    |                   ********************************        |
    +--------------+--------------+--------------+--------------+
  -360           -180             0             180            360
    */
    SPANS_PRIME_ANTI_POSITIVE,

    /*
    +--------------+--------------+--------------+--------------+
    |       ********************************                    |
    |       ********************************                    |
    +--------------+--------------+--------------+--------------+
  -360           -180             0             180            360
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

  /** Returns true if the number n is in the range [min..max]. */
  private boolean inRange (
    double n,
    double min,
    double max
  ) {

    return (n <= max && n >= min);

  } // inRange

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
      
      // Determine longitude range type
      // ------------------------------
      if (inRange (minLon, -180, 180) && inRange (maxLon, -180, 180))
        lonRange = LongitudeRange.SPANS_PRIME;

      else if (inRange (minLon, 0, 180) && inRange (maxLon, 180, 360))
        lonRange = LongitudeRange.SPANS_ANTI_POSITIVE;

      else if (inRange (minLon, -360, -180) && inRange (maxLon, -180, 0))
        lonRange = LongitudeRange.SPANS_ANTI_NEGATIVE;

      else if (inRange (minLon, -180, 0) && inRange (maxLon, 180, 360)) {
        alpha = minLon;
        lonRange = LongitudeRange.SPANS_PRIME_ANTI_POSITIVE;
      } // else if
      
      else if (inRange (minLon, -360, -180) && inRange (maxLon, 0, 180)) {
        alpha = maxLon;
        lonRange = LongitudeRange.SPANS_PRIME_ANTI_NEGATIVE;
      } // else if

      else
        throw new IllegalStateException ("Cannot determine longitude range type");
    
      LOGGER.fine ("Geographic projection longitude range is " + lonRange);
    
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

    super.setAffine (centerLoc, pixelDims);
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

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    lat = Math.toDegrees (lat);
    lon = Math.toDegrees (lon);

    switch (lonRange) {

    case SPANS_ANTI_POSITIVE:
      if (lon < 0) lon += 360;
      break;

    case SPANS_ANTI_NEGATIVE:
      if (lon > 0) lon -= 360;
      break;

    case SPANS_PRIME_ANTI_POSITIVE:
      if (inRange (lon, -180, alpha)) lon += 360;
      break;

    case SPANS_PRIME_ANTI_NEGATIVE:
      if (inRange (lon, alpha, 180)) lon -= 360;
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

    lon[0] = Math.toRadians (x);
    lat[0] = Math.toRadians (y);

    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // GeographicProjection

////////////////////////////////////////////////////////////////////////
