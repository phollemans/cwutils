////////////////////////////////////////////////////////////////////////
/*
     FILE: MercatorProjection.java
  PURPOSE: Handles Mercator map transformations.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/28
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

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
 * The <code>MercatorProjection</code> class performs Mercator map
 * projection calculations.
 */
public class MercatorProjection 
  extends GCTPStyleProjection {

  // Variables
  // ---------

  /** The latitude of the projection origin in radians. */
  private double latOrigin;

  /** The longitude of the projection center in radians. */
  private double lonCenter;

  /** The mercator computation constant. */
  private double m1;

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
   * @param latOrigin the latitude of true scale in radians.
   * @param lonCenter the center longitude of the map in radians.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   */
  public MercatorProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double latOrigin,
    double lonCenter
  ) throws NoninvertibleTransformException {

    super (MERCAT, 0, rMajor, rMinor, dimensions, affine);

    // Initialize
    // ----------
    this.latOrigin = latOrigin;
    this.lonCenter = lonCenter;

    // Precompute values
    // -----------------
    double sinLat = Math.sin (latOrigin);
    double cosLat = Math.cos (latOrigin);
    m1 = cosLat/Math.sqrt (1 - ec2*sinLat*sinLat);

  } // MercatorProjection constructor

  ////////////////////////////////////////////////////////////

  public void mapTransformFor (
    double[] lonLat,
    double[] xy
  ) {

    double lon = lonLat[0];
    double lat = lonLat[1];
    if (Math.abs (Math.abs (lat) - HALF_PI) <= EPSLN)
      xy[0] = xy[1] = Double.NaN;
    else {
      double sinphi = Math.sin (lat);
      double ts = tsfnz (ec, lat, sinphi);
      xy[0] = falseEast +  rMajor*m1*adjust_lon (lon - lonCenter);
      xy[1] = falseNorth - rMajor*m1*Math.log (ts);
    } // else

  } // mapTransformFor

  ////////////////////////////////////////////////////////////

  public void mapTransformInv (
    double[] xy,
    double[] lonLat
  ) {

    double x = xy[0];
    double y = xy[1];
    x -= falseEast;
    y -= falseNorth;
    double ts = Math.exp (-y/(rMajor * m1));
    double lat = phi2z (ec, ts);
    if (Double.isNaN (lat)) {
      lonLat[0] = lonLat[1] = Double.NaN;
    } // if
    else {
      double lon = adjust_lon (lonCenter + x/(rMajor * m1));
      lonLat[0] = lon;
      lonLat[1] = lat;
    } // else

  } // mapTransformInv

  ////////////////////////////////////////////////////////////

  public double[] getParameters () {

    double[] params = new double[15];
    if (spheroid == -1) {
      params[0] = rMajor;
      params[1] = ec2;
    } // if
    params[4] = pack_angle (Math.toDegrees (lonCenter));
    params[5] = pack_angle (Math.toDegrees (latOrigin));
    params[6] = falseEast;
    params[7] = falseNorth;

    return (params);

  } // getParameters

  ////////////////////////////////////////////////////////////

} // MercatorProjection

////////////////////////////////////////////////////////////////////////
