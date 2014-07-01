////////////////////////////////////////////////////////////////////////
/*
     FILE: MapProjection.java
  PURPOSE: To perform common 2D map projection calculations.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/26
  CHANGES: 2014/03/25, PFH
           - Changes: Added isOrientable() override to return false.
           - Issue: By default EarthTransform objects are orientable for
             display, but some map projections shouldn't be oriented, so
             by default we return false here and let individual projections
             decide.

  CoastWatch Software Library and Utilities
  Copyright 2006-2014, USDOC/NOAA/NESDIS CoastWatch

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
 * The <code>MapProjection</code> class is the abstract parent of
 * all map projections that implement coordinate conversions for
 * the projection systems listed in {@link ProjectionConstants}.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public abstract class MapProjection 
  extends EarthTransform2D
  implements ProjectionConstants {

  // Constants
  // ---------

  /** Projection description string. */
  public final static String DESCRIPTION = "mapped";  

  // Variables
  // ---------

  /** The map projection system. */
  protected int system;

  /** The map projection system zone. */
  protected int zone;

  /** The map projection spheroid code. */
  protected int spheroid;

  /** 
   * Data <code>[row, column]</code> to map <code>[x, y]</code> affine
   * transform.
   */
  protected AffineTransform inverseAffine;

  /**
   * Map <code>[x, y]</code> to data <code>[row, column]</code> affine
   * transform. 
   */
  protected AffineTransform forwardAffine;

  /** The map projection datum. */
  protected Datum datum;

  /** 
   * The positive longitude flag for geographic projections, true to
   * convert longitudes to positive only in calls to
   * transform(EarthLocation).
   */
  protected boolean positiveLon;

  ////////////////////////////////////////////////////////////

  public boolean isOrientable () { return (false); }

  ////////////////////////////////////////////////////////////

  /** Gets the projection system name. */
  public String getSystemName () { return (PROJECTION_NAMES[system]); }

  ////////////////////////////////////////////////////////////

  /** Gets the projection system. */
  public int getSystem () { return (system); }

  ////////////////////////////////////////////////////////////

  /** Gets the projection zone. */
  public int getZone () { return (zone); }

  ////////////////////////////////////////////////////////////

  /** Gets the projection spheroid name. */
  public String getSpheroidName () { 

    return (spheroid == -1 ? "User defined" : SPHEROID_NAMES[spheroid]);

  } // getSpheroidName

  ////////////////////////////////////////////////////////////

  /** Gets the projection spheroid code. */
  public int getSpheroid () { return (spheroid); }

  ////////////////////////////////////////////////////////////

  /** Gets the projection datum. */
  public Datum getDatum () { return (datum); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data <code>[row, column]</code> to map <code>[x,
   * y]</code> affine transform.
   */
  public AffineTransform getAffine () { 

    return ((AffineTransform) inverseAffine.clone ()); 

  } // getAffine

  ////////////////////////////////////////////////////////////

  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets a modified version of this map projection.
   *
   * @param centerLoc the new earth center location of the map
   * center.
   * @param pixelDims the new pixel dimensions in meters at the
   * projection reference point as <code>[height, width]</code>.
   *
   * @return the modified version of this projection.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   */
  public MapProjection getModified (
    EarthLocation centerLoc, 
    double[] pixelDims
  ) throws NoninvertibleTransformException {

    MapProjection proj = (MapProjection) this.clone();
    proj.setAffine (centerLoc, pixelDims);
    return (proj);

  } // getModified

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a map projection from the specified projection and
   * affine transform.   The {@link SpheroidConstants} and
   * {@link ProjectionConstants} class should be consulted for
   * valid parameter constants.
   *
   * @param system the map projection system.
   * @param zone the map projection zone for State Plane and UTM
   * projections.
   * @param spheroid the spheroid code or -1 for user-defined.
   * If the spheroid is user-defined, the datum should be set
   * manually by the subclass.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   */
  protected MapProjection (
    int system,
    int zone,
    int spheroid,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    // Initialize variables
    // --------------------
    this.system = system;
    this.zone = zone;
    if (spheroid < 0) spheroid = -1;
    this.spheroid = spheroid;
    this.datum = DatumFactory.create (spheroid);
    this.dims = (int[]) dimensions.clone();

    // Set affine transforms
    // ---------------------
    inverseAffine = (AffineTransform) affine.clone();
    forwardAffine = inverseAffine.createInverse();

  } // MapProjection constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the affines for this projection based on center and pixel
   * dimensions.
   *
   * @param centerLoc the Earth location at the map center.
   * @param pixelDims the pixel dimensions in meters at the projection
   * reference point as <code>[height, width]</code>.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   */
  protected void setAffine (
    EarthLocation centerLoc,
    double[] pixelDims
  ) throws NoninvertibleTransformException {

    // Get map coordinates of new center point
    // ---------------------------------------
    double[] lonLat = new double[] {
      Math.toRadians (centerLoc.lon),
      Math.toRadians (centerLoc.lat)
    };
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

  } // setAffine

  ////////////////////////////////////////////////////////////

  /**
   * Gets the pixel size indicated by the affine transform.  The
   * returned pixel size is only valid for square pixels.
   *
   * @return the pixel size in meters.
   */
  public double getPixelSize () {

    double matrix[] = new double[6];
    inverseAffine.getMatrix (matrix);
    return (Math.sqrt (matrix[0]*matrix[0] + matrix[1]*matrix[1]));

  } // getPixelSize

  ////////////////////////////////////////////////////////////

  /**
   * Gets the pixel dimensions.  The returned pixel dimensions are
   * only valid for non-rotation data to map affine transforms.
   *
   * @return the pixel dimensions in meters at the projection
   * reference point as <code>[height, width]</code>.
   */
  public double[] getPixelDimensions () {

    double[] matrix = new double[6];
    inverseAffine.getMatrix (matrix);
    double[] pixelDims = new double[] {-matrix[1], matrix[2]};
    return (pixelDims);

  } // getPixelDimensions

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a subset version of this map projection. 
   * 
   * @see EarthTransform#getSubset
   */
  public EarthTransform getSubset (
    DataLocation newOrigin,
    int[] newDims
  ) {

    // Create exact clone
    // ------------------
    MapProjection proj = (MapProjection) this.clone();

    // Translate affine transform
    // --------------------------
    AffineTransform trans = AffineTransform.getTranslateInstance (
      -newOrigin.get (0), -newOrigin.get (1));
    proj.forwardAffine.preConcatenate (trans);
    AffineTransform inverseTrans = AffineTransform.getTranslateInstance (
      newOrigin.get (0), newOrigin.get (1));
    proj.inverseAffine.concatenate (inverseTrans);

    // Set new dimensions
    // ------------------
    proj.dims = (int[]) newDims.clone();

    return (proj);

  } // getSubset

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a subset version of this map projection.
   *
   * @param start the 2D starting data coordinates.
   * @param stride the 2D data stride.
   * @param length the total number of values in each dimension.
   *
   * @return the subset and/or subsampled transform.
   */
  public MapProjection getSubset (
    int[] start,
    int[] stride,
    int[] length
  ) {

    MapProjection newProj = (MapProjection) this.clone();
    newProj.inverseAffine.concatenate (
      AffineTransform.getTranslateInstance (start[0], start[1]));
    newProj.inverseAffine.concatenate (
      AffineTransform.getScaleInstance (stride[0], stride[1]));
    try { newProj.forwardAffine = newProj.inverseAffine.createInverse(); }
    catch (NoninvertibleTransformException e) {
      throw new RuntimeException ("Cannot invert affine transform");
    } // catch
    newProj.dims = length;

    return (newProj);

  } // getSubset

  ////////////////////////////////////////////////////////////

  public Object clone () {

    MapProjection proj = (MapProjection) super.clone();
    proj.inverseAffine = (AffineTransform) inverseAffine.clone();
    proj.forwardAffine = (AffineTransform) forwardAffine.clone();
    /**
     * Don't clone the datum -- it's immutable like a string.
     */
    return (proj);

  } // clone

  ////////////////////////////////////////////////////////////

  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    // Perform conversion
    // ------------------
    double[] xy = dataLoc.getCoords();
    inverseAffine.transform (xy, 0, xy, 0, 1);
    double[] lonLat = new double[2];
    mapTransformInv (xy, lonLat);
    earthLoc.setCoords (
      Math.toDegrees (lonLat[1]), 
      Math.toDegrees (lonLat[0])
    );

  } // transformImpl

  ////////////////////////////////////////////////////////////

  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    // Perform conversion
    // ------------------
    double[] lonLat = new double[] {
      Math.toRadians (earthLoc.lon), 
      Math.toRadians (earthLoc.lat)
    };
    double[] xy = new double[2];
    mapTransformFor (lonLat, xy); 
    forwardAffine.transform (xy, 0, xy, 0, 1);
    dataLoc.setCoords (xy);

  } // transformImpl

  ////////////////////////////////////////////////////////////

  /**
   * Performs a forward map transformation from (latitude,
   * longitude) coordinates to map (x, y).
   * 
   * @param lonLat the longitude and latitude in radians.
   * @param xy the x and y in meters or Double.NaN if the
   * transform could not be computed (modified).
   */
  public abstract void mapTransformFor (
    double[] lonLat,
    double[] xy
  );

  ////////////////////////////////////////////////////////////

  /**
   * Performs an inverse map transformation from map (x, y)
   * coordinates to (latitude, longitude).
   * 
   * @param xy the x and y in meters.
   * @param lonLat the longitude and latitude in radians or Double.NaN
   * if the transform could not be computed (modified).
   */
  public abstract void mapTransformInv (
    double[] xy,
    double[] lonLat
  );

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the positive longitude flag for geographic projections.
   * When true, longitudes are converted to be positive only in calls
   * to <code>transform(EarthLocation)</code>.  By default, the
   * positive longitude flag is false and longitudes are not modified.
   *
   * @param flag the positive longitude flag.
   */
  public void setPositiveLon (
    boolean flag
  ) {

    positiveLon = flag;

  } // setPositiveLon

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a set of GCTP-style projection parameters if available.
   *
   * @return the parameters array.
   *
   * @throws UnsupportedOperationException if the map projection
   * properties cannot be represented using GCTP parameters.
   */
  public double[] getParameters () {

    throw new UnsupportedOperationException ("No GCTP parameters available");

  } // getParameters

  ////////////////////////////////////////////////////////////

  /**
   * Gets the projection system that matches the specified name.
   *
   * @param name the projection system name.
   *
   * @return the matching projection code or -1 if none found.
   */
  public static int getProjection (
    String name
  ) {

    for (int i = 0; i < PROJECTION_NAMES.length; i++)
      if (name.equalsIgnoreCase (PROJECTION_NAMES[i])) return (i);

    return (-1);

  } // getProjection

  ////////////////////////////////////////////////////////////

  /**
   * Performs a comparison of double value arrays, checking that
   * values are "almost" equal, within a small relative error.
   *
   * @param arrayA the first array for comparison.
   * @param arrayB the second array for comparison.
   *
   * @return true if the arrays are equal or almost equal, or
   * false if not.
   */
  private static boolean almostEquals (
    double[] arrayA,
    double[] arrayB
  ) {

    // Check lengths
    // -------------
    if (arrayA.length != arrayB.length) return (false);

    // Check each value
    // ----------------
    for (int i = 0; i < arrayA.length; i++) {
      if (arrayA[i] != arrayB[i]) {
        double relative = Math.abs ((arrayA[i] - arrayB[i]) / 
          Math.max (arrayA[i], arrayB[i]));
        if (relative > 1e-10) return (false);
      } // if
    } // for  

    // Assume equal
    // ------------
    return (true);
 
  } // almostEquals

  ////////////////////////////////////////////////////////////

  /**
   * Compares the specified object with this map projection for
   * equality.  The GCTP-style projection parameters and affine
   * transforms are compared value by value.
   *
   * @param obj the object to be compared for equality.
   *
   * @return true if the map projections are equivalent, or false
   * if not.
   *
   * @see #getParameters
   */
  public boolean equals (
    Object obj
  ) {

    // Check object instance
    // ---------------------
    if (!(obj instanceof MapProjection)) return (false);

    // Check GCTP parameters
    // ---------------------
    MapProjection map = (MapProjection) obj;
    if (this.system != map.system) return (false);
    if (this.zone != map.zone) return (false);
    if (!almostEquals (this.getParameters(), map.getParameters())) 
      return (false);
    if (this.spheroid != map.spheroid) return (false);

    // Check affine transforms
    // -----------------------
    double[] matrixA = new double[6];
    double[] matrixB = new double[6];
    this.forwardAffine.getMatrix (matrixA);
    map.forwardAffine.getMatrix (matrixB);
    if (!almostEquals (matrixA, matrixB)) return (false);
    this.inverseAffine.getMatrix (matrixA);
    map.inverseAffine.getMatrix (matrixB);
    if (!almostEquals (matrixA, matrixB)) return (false);

    return (true);

  } // equals

  ////////////////////////////////////////////////////////////

} // MapProjection class

////////////////////////////////////////////////////////////////////////
