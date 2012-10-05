////////////////////////////////////////////////////////////////////////
/*
     FILE: OrthographicProjection.java
  PURPOSE: To implement the orthographic map projection.
   AUTHOR: Peter Hollemans
     DATE: 2003/01/15
  CHANGES: 2005/05/16, PFH, modified for in-place transform
           2006/05/28, PFH, modified to extend GCTPProjection

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.geom.*;
import java.util.*;
import noaa.coastwatch.util.*;

/**
 * The orthographic projection class implements the orthographic
 * projection equations using pure Java code.  The Earth is assumed to
 * be a sphere of radius 6370.997 km.<p>
 *
 * <b>NOTE:</b> Only the forward transform is implemented for
 * transforming Earth location to data location.  The inverse transform
 * still calls native C code.
 */
public class OrthographicProjection 
  extends MapProjection {

  // Constants
  // ---------

  /** Epsilon value. */
  private static final double EPSLN = 1.0e-10;

  /** The standard Earth radius in meters. */
  private static final double RADIUS = SpheroidConstants.STD_RADIUS*1000;

  // Variables
  // ---------

  /** The Earth location at the projection center. */
  private EarthLocation projCenter;

  /** Precalculated projection terms. */
  private double sin_p14, cos_p14;

  /** The backface flag, true if backface projection is desired. */
  private boolean backface = false;

  /** The map projection for inverse transforms. */
  private MapProjection invProj;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the backface mode flag.  In backface mode, Earth location
   * points on the back-facing side of the orthographic projection
   * that would normally be returned as invalid data locations are
   * returned as valid.  This has the effect of showing line segments
   * on the back-facing side of the Earth as if the Earth center were
   * transparent.  By default, backface mode is off.
   */
  public void setBackface (boolean flag) { backface = flag; }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new orthographic projection using the 
   * specified projection center point.
   * 
   * @param projCenter the Earth location at the projection center.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param centerLoc the Earth location at the map center.
   * @param pixelDims the pixel dimensions in meters at the projection
   * reference point as <code>[height, width]</code>.
   *
   * @throws NoninvertibleTransformException if the map projection to data
   * coordinate affine transform is not invertible.
   */
  public OrthographicProjection (
    EarthLocation projCenter,
    int[] dimensions,
    EarthLocation centerLoc,
    double[] pixelDims
  ) throws NoninvertibleTransformException {

    super (GCTP.ORTHO, 0, GCTP.SPHERE, dimensions, new AffineTransform());

    // Initialize
    // ----------
    this.projCenter = (EarthLocation) projCenter.clone();
    sin_p14 = Math.sin (Math.toRadians (projCenter.lat));
    cos_p14 = Math.cos (Math.toRadians (projCenter.lat));
    setAffine (centerLoc, pixelDims);

    // Create GCTP projection for inverse
    // ----------------------------------
    invProj = new GCTPProjection (GCTP.ORTHO, 0, new double[] {0, 0, 0, 0, 
      GCTP.pack_angle (projCenter.lon), GCTP.pack_angle (projCenter.lat),
      0, 0, 0, 0, 0, 0, 0, 0, 0}, GCTP.SPHERE, dimensions, 
      new AffineTransform());
    invProj.setAffine (centerLoc, pixelDims);


  } // OrthographicProjection constructor

  ////////////////////////////////////////////////////////////

  public void mapTransformFor (
    double[] lonLat,
    double[] xy
  ) {

    // Convert to map coordinates
    // --------------------------
    double dlon = lonLat[0] - Math.toRadians (projCenter.lon);
    double lat = lonLat[1];
    double sinphi = Math.sin (lat);
    double cosphi = Math.cos (lat);
    double coslon = Math.cos (dlon);
    double g = sin_p14 * sinphi + cos_p14 * cosphi * coslon;
    if (backface && g < 0) g *= -1;
    if ((g > 0) || (Math.abs (g) <= EPSLN)) {
      xy[0] = RADIUS * cosphi * Math.sin (dlon);
      xy[1] = RADIUS * (cos_p14 * sinphi - sin_p14 * cosphi * coslon);
    } // if
    else {
      xy[0] = xy[1] = Double.NaN;
    } // else

  } // mapTransformFor

  ////////////////////////////////////////////////////////////

  public void mapTransformInv (
    double[] xy,
    double[] lonLat
  ) {

    invProj.mapTransformInv (xy, lonLat);

  } // mapTransformInv

  ////////////////////////////////////////////////////////////

} // OrthographicProjection class

////////////////////////////////////////////////////////////////////////
