////////////////////////////////////////////////////////////////////////
/*

     File: GCTPProjection.java
   Author: Peter Hollemans
     Date: 2002/04/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.trans.MapProjection;

/**
 * The <code>GCTPProjection</code> class implements Earth
 * transform calculations for many common 2D map projections
 * using the native GCTP transformation library.  See the General
 * Cartographic Transformations Package ({@link GCTP}) class for
 * details on the projections constants and parameters.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 *
 * @deprecated The native methods of GCTP are no longer supported.  Use
 * the {@link noaa.coastwatch.util.trans.MapProjectionFactory} to create
 * and work with map projections rather than this class.
 */
@Deprecated
public class GCTPProjection 
  extends MapProjection
  implements Cloneable {

  // Variables
  // ---------

  /** The GCTP projection parameters array. */
  private double[] parameters;

  /** The projection object currently initialized for forward transforms. */
  private static GCTPProjection forwardObject;

  /** The projection object currently initialized for inverse transforms. */
  private static GCTPProjection inverseObject;

  ////////////////////////////////////////////////////////////

  /** Gets the GCTP projection parameters. */
  public double[] getParameters () { return ((double[]) parameters.clone ()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Checks that the spheroid and projection system are compatible
   * and throws an error if not.
   *
   * @param system the projection system code to check.
   * @param spheroid the spheroid code to check.
   *
   * @throws IllegalArgumentException if the spheroid and system are
   * incompatible.
   */
  private static void checkSpheroid (
    int system,
    int spheroid
  ) {

    // TODO: Delay this check for now, as it breaks a few things that
    // we don't want broken.

    /*
    if (spheroid != GCTP.SPHERE && !GCTP.supportsSpheroid (system)) {
      throw new IllegalArgumentException ("GCTP does not support a " +
        GCTP.SPHEROID_NAMES[spheroid] + " spheroid with the " + 
        GCTP.PROJECTION_NAMES[system] + " projection system");
    } // if
    */

  } // checkSpheroid

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a map projection from the specified projection and
   * affine transform.  The {@link SpheroidConstants} and
   * {@link ProjectionConstants} class should be consulted for
   * valid parameter constants.
   *
   * @param system the map projection system.
   * @param zone the map projection zone for State Plane and UTM
   * projections.
   * @param parameters an array of 15 GCTP projection parameters.
   * @param spheroid the spheroid code.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   *
   * @throws NoninvertibleTransformException if the map projection to data
   * coordinate affine transform is not invertible.  
   */
  public GCTPProjection (
    int system,
    int zone,
    double[] parameters,
    int spheroid,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    super (system, zone, spheroid, dimensions, affine);

    // Check for unsupported spheroid
    // ------------------------------
    checkSpheroid (system, spheroid);

    // Initialize variables
    // --------------------
    this.parameters = (double[]) (parameters.clone());
    forwardObject = null;
    inverseObject = null;



    // TODO: Need to do something here about the datum if the
    // spheroid code is negative.


  } // GCTPProjection constructor

  ////////////////////////////////////////////////////////////

  public void mapTransformInv (
    double[] xy,
    double[] lonLat
  ) {

    // Check for initialization
    // ------------------------
    if (this != inverseObject && system != GEO) {
      GCTP.init_inverse (system, zone, parameters, spheroid, "", "");
      inverseObject = this;
    } // if

    // Perform conversion
    // ------------------
    if (system == GEO) {
      lonLat[0] = Math.toRadians (xy[0]);
      lonLat[1] = Math.toRadians (xy[1]);
    } // if
    else {
      try {
        double[] newLonLat = GCTP.inverse (xy, system);
        lonLat[0] = newLonLat[0];
        lonLat[1] = newLonLat[1];
      } // try
      catch (Exception e) { 
        lonLat[0] = Double.NaN;
        lonLat[1] = Double.NaN;
      } // catch
    } // else

  } // mapTransformInv

  ////////////////////////////////////////////////////////////

  public void mapTransformFor (
    double[] lonLat,
    double[] xy
  ) {

    // Check for initialization
    // ------------------------
    if (this != forwardObject && system != GEO) {
      GCTP.init_forward (system, zone, parameters, spheroid, "", "");
      forwardObject = this;
    } // if

    // Perform conversion
    // ------------------
    try {
      double[] newXY = GCTP.forward (lonLat, system);
      xy[0] = newXY[0];
      xy[1] = newXY[1];
    } // try
    catch (Exception e) {
      xy[0] = Double.NaN;
      xy[1] = Double.NaN;
    } // catch

  } // mapTransformFor

  ////////////////////////////////////////////////////////////

  public Object clone () {

    GCTPProjection proj = (GCTPProjection) super.clone();
    proj.parameters = (double[]) (parameters.clone());
    return (proj);

  } // clone

 ////////////////////////////////////////////////////////////

} // GCTPProjection class

////////////////////////////////////////////////////////////////////////
