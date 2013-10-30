////////////////////////////////////////////////////////////////////////
/*
     FILE: GCTPProjection.java
  PURPOSE: To perform GCTP map projection calculations.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/15
  CHANGES: 2002/05/15, PFH, added javadoc, package
           2002/07/11, PFH, added equals method
           2002/07/25, PFH, converted to location classes
           2002/10/02, PFH, added try/catch around GCTP calls
           2002/10/03, PFH, added getPixelSize
           2002/12/02, PFH, removed projection names
           2002/12/03, PFH, modified GCTP variable names, get methods,
             and constructor
           2002/12/04, PFH, added Cloneable interface, getDatumName,
             getPixelDimensions
           2002/12/11, PFH, added checks for GCTP.GEO in transform methods
           2003/01/15, PFH, allowed access to affine from subclasses
           2003/03/13, PFH, corrected commenting
           2003/11/22, PFH, fixed Javadoc comments
           2004/05/04, PFH, modified clone(), added createTranslated() method
           2004/10/05, PFH
           - modified clone() for new superclass
           - modified to have only dimensions-based constructor
           2004/10/13, PFH
           - added setPositiveLon()
           - changed createTranslated() to getSubset()
           2004/12/13, PFH, modified equals() to use almostEquals()
           2005/05/16, PFH, modified for in-place transform
           2005/05/18, PFH, changed "datum" to "spheroid" and added actual
             datum-related functionality
           2005/05/27, PFH, added check for unsupported spheroid
           2005/05/20, PFH, now extends 2D transform
           2005/08/01, PFH, added getSubset()
           2005/05/26, PFH, separated into MapProjection and GCTPProjection

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import noaa.coastwatch.util.*;
import java.awt.geom.*;
import java.util.*;

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
    if (system == GEO) {
      xy[0] = Math.toDegrees (lonLat[0]);
      xy[1] = Math.toDegrees (lonLat[1]);
      if (positiveLon && xy[0] < 0) xy[0] = 360 + xy[0];
    } // if
    else {
      try {
        double[] newXY = GCTP.forward (lonLat, system);
        xy[0] = newXY[0];
        xy[1] = newXY[1];
      } // try
      catch (Exception e) {
        xy[0] = Double.NaN;
        xy[1] = Double.NaN;
      } // catch
    } // else        

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
