////////////////////////////////////////////////////////////////////////
/*
     FILE: MapProjectionFactory.java
  PURPOSE: Creates map projection objects.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/26
  CHANGES: 2006/10/05, PFH, added call to setPositiveLon for GEO projections
           2007/07/13, PFH, added check for spheroid/system compatibility

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
import java.util.*;
import noaa.coastwatch.util.*;

/**
 * The <code>MapProjectionFactory</code> class creates instances
 * of map projections.
 */
public class MapProjectionFactory 
  implements ProjectionConstants {

  // Variables
  // ---------
  
  /** The GCTP forcing flag, true to force a GCTP projection instance. */
  private boolean forceGctp;

  /** The static instance of this factory. */
  private static MapProjectionFactory instance;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an instance of this factory with no GCTP forcing.  This
   * is the main method that most classes should use to create a
   * {@link MapProjection} because it allows the factory to
   * possibly return a performance-enhanced pure Java version of
   * a {@link GCTPStyleProjection} object.
   */
  public static MapProjectionFactory getInstance () {

    if (instance == null) instance = new MapProjectionFactory();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** Creates a new map projection factory with no GCTP forcing. */
  protected MapProjectionFactory () { }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new map projection factory.
   *
   * @param forceGctp the GCTP flag, true to the factory to return
   * projections that use the native method based {@link
   * GCTPProjection} for the underlying object, or false to return a
   * pure Java object if an implementation is available.
   */
  public MapProjectionFactory (
    boolean forceGctp
  ) {

    this.forceGctp = forceGctp;

  } // MapProjectionFactory constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the positive lon flag for geographic projections.  This
   * is necessary for geographic projections that span the
   * +180/-180 longitude boundary so that earth locations with
   * longitudes in the [-180..0] range are converted to be
   * positive before applying the affine transform so that
   * positive column values result.
   *
   * @param proj the projection to set the longitude flag for.
   *
   * @see MapProjection#setPositiveLon
   */
  private void setLonFlag (
    MapProjection proj
  ) {

    if (proj.getSystem() == GEO) {
      DataLocation topRight = proj.transform (proj.transform (
        new DataLocation (0, proj.getDimensions()[Grid.COLS]-1)));
      if (topRight.get (Grid.COLS) < 0) { 
        proj.setPositiveLon (true);
      } // if
    } // if

  } // setLonFlag

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new map projection from the specified GCTP-style
   * projection and data parameters.  The {@link
   * SpheroidConstants} and {@link ProjectionConstants} class
   * should be consulted for valid parameter constants.
   *
   * @param system the map projection system.
   * @param zone the map projection zone for State Plane and UTM
   * projections.
   * @param parameters an array of 15 GCTP projection parameters.
   * @param spheroid the spheroid code or -1 for custom spheroid.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param centerLoc the Earth location at the map center.
   * @param pixelDims the pixel dimensions in meters at the projection
   * reference point as <code>[height, width]</code>.
   *
   * @return the new map projection.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the projection system
   * and spheroid are incompatible.
   */
  public MapProjection create (
    int system,
    int zone,
    double[] parameters,
    int spheroid,
    int[] dimensions,
    EarthLocation centerLoc,
    double[] pixelDims
  ) throws NoninvertibleTransformException {

    // Create initial projection
    // -------------------------
    MapProjection proj = create (system, zone, parameters,
      spheroid, dimensions, new AffineTransform());

    // Modify projection for center and resolution
    // -------------------------------------------
    proj = proj.getModified (centerLoc, pixelDims);

    setLonFlag (proj);
    return (proj);

  } // create

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new map projection from the specified GCTP-style
   * projection and data parameters.  The {@link
   * SpheroidConstants} and {@link ProjectionConstants} class
   * should be consulted for valid parameter constants.
   *
   * @param system the map projection system.
   * @param zone the map projection zone for State Plane and UTM
   * projections.
   * @param parameters an array of 15 GCTP projection parameters.
   * @param spheroid the spheroid code or -1 for custom spheroid.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   *
   * @return the new map projection.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the projection system
   * and spheroid are incompatible.
   */
  public MapProjection create (
    int system,
    int zone,
    double[] parameters,
    int spheroid,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    MapProjection proj = null;

    // Check for spheroid support in projection system
    // -----------------------------------------------
    double[] rMajor = new double[1];
    double[] rMinor = new double[1];
    double[] radius = new double[1];
    GCTPStyleProjection.sphdz (spheroid, parameters, rMajor, rMinor, radius);
    boolean isSphere = (rMajor[0] == rMinor[0]);
    if (!isSphere && !GCTP.supportsSpheroid (system)) {
      throw new IllegalArgumentException (
        "Projection system does not support generic spheroid");
    } // if

    // Create GCTP style projection
    // ----------------------------
    if (!forceGctp) {

      // Determine if we have an implementation
      // --------------------------------------
      boolean haveImpl;
      switch (system) {
      case MERCAT:
      // case PS:
      // case GEO:
        haveImpl = true;
        break;
      default: 
        haveImpl = false;
      } // switch        

      // If so, continue on
      // ------------------
      if (haveImpl) {

        // Get common parameter values
        // ---------------------------
        double falseEast = parameters[6];
        double falseNorth = parameters[7];

        // Create projection
        // -----------------
        switch (system) {

        case MERCAT:
          proj = new MercatorProjection (rMajor[0], rMinor[0], dimensions,
            affine, 
            Math.toRadians (GCTPStyleProjection.unpack_angle (parameters[5])),
            Math.toRadians (GCTPStyleProjection.unpack_angle (parameters[4])));
          break;

        } // switch        

        // Set false east/north
        // --------------------
        if (proj != null) {
          ((GCTPStyleProjection) proj).setFalse (falseEast, falseNorth);
        } // if

      } // if

    } // if

    // Fall back on GCTP projection
    // ----------------------------
    if (proj == null) {
      proj = new GCTPProjection (system, zone, parameters, spheroid, 
        dimensions, affine);
    } // if

    setLonFlag (proj);
    return (proj);

  } // create

  ////////////////////////////////////////////////////////////

} // MapProjectionFactory class

////////////////////////////////////////////////////////////////////////
