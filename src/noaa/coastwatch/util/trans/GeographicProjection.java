////////////////////////////////////////////////////////////////////////
/*
     FILE: GeographicProjection.java
  PURPOSE: Handles Geographic map transformations.
   AUTHOR: Peter Hollemans
     DATE: 2012/11/02
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.geom.*;
import noaa.coastwatch.util.*;

/**
 * The <code>GeographicProjection</code> class performs 
 * Geographic map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class GeographicProjection 
  extends GCTPCStyleProjection {

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

  } // GeographicProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    if (positiveLon && lon < 0) lon = 360 + lon;
    x[0] = Math.toDegrees (lon);
    y[0] = Math.toDegrees (lat);

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
