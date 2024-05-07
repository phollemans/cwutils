////////////////////////////////////////////////////////////////////////
/*

     File: EarthTransform.java
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
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.MetadataContainer;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.DatumFactory;
import noaa.coastwatch.util.trans.BoundaryHandler;

import static noaa.coastwatch.util.trans.SpheroidConstants.WGS84;
import static noaa.coastwatch.util.trans.SpheroidConstants.SPHEROID_SEMI_MAJOR;
import static noaa.coastwatch.util.trans.SpheroidConstants.SPHEROID_SEMI_MINOR;
import static noaa.coastwatch.util.trans.SpheroidConstants.SPHEROID_NAMES;

/**
 * The <code>EarthTransform</code> class translates between data coordinates and
 * geographic coordinates in latitude and longitude degrees.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class EarthTransform
  extends MetadataContainer {

  // Constants
  // ---------
  
  /** The instance of WGS84. */
  private static final Datum WGS84_DATUM = DatumFactory.create (WGS84);

/*
 
TODO - This is a possible location for a set of transform types, to be used
in conjunction with whatever convention replaces instanceof calls for this
class.

public enum TransformType {
  SWATH ("swath"),
  MAPPED ("mapped"),
  VECTOR ("vector"),
  SENSOR ("sensor");
  private String name;
  private TransformType (String name) { this.name = name; }
  public String toString () { return (name); }
};

We could do a Visitor pattern:

public interface EarthTransformVisitor {
  default public void visitMapProjection (MapProjection proj) { }
  default public void visitSwathProjection (SwathProjection proj) { }
  default public void visitDataProjection (DataProjection proj) { }
  default public void visitSensorScanProjection (SensorScanProjection proj) { }
  default public void visitGeoVectorProjection (GeoVectorProjection proj) { }
  default public void visitCDMGridMappedProjection (CDMGridMappedProjection proj) { }
} // EarthTransformVisitor interface

But the issue here is, that we may have more projection classes, and they
could be updated and added.  The Visitor pattern is best suited to a set
of classes that doesn't change, but which we have various concrete-specific
operations to perform.

*/

  // Variables
  // ---------

  /** Data location dimensions. */
  protected int[] dims;

  /** The boundary handler for this transform or null for none. */
  protected BoundaryHandler boundaryHandler;

  ////////////////////////////////////////////////////////////

  /**
   * Gets a version of this transform that can be used with 2D data
   * locations.
   *
   * @return the 2D version of this transform.
   *
   * @since 3.3.1
   */
  public abstract EarthTransform2D get2DVersion ();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the transform datum.  Unless overridden by the child class,
   * this method returns WGS84.
   * 
   * @return the geodetic datum.
   */
  public Datum getDatum () { return (WGS84_DATUM); }

  ////////////////////////////////////////////////////////////

  /**
   * Determines if this transform is invertible.  All transforms should
   * translate from data location to geographic location.  But to be invertible,
   * the transform must also translate from geographic location back to
   * data location.  This method returns true unless overridden by the child
   * class.
   *
   * @return true if this transform is invertible or false if not.
   *
   * @since 3.5.0
   */
  public boolean isInvertible () { return (true); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the closest integer data location to a specified geographic
   * location. 
   * 
   * @param targetEarthLoc the target earth location to locate the closest 
   * valid data location.
   * @param targetDataLoc the data location or null.  If null, an object
   * is created and returned.  If non-null, the object is simply
   * modified.
   *
   * @return the data location.  The data location may be marked invalid
   * if no closest data location can be found within the data location 
   * bounds of this transform.  
   * 
   * @since 3.8.1
   */
  public DataLocation closest ( 
    EarthLocation targetEarthLoc,
    DataLocation targetDataLoc
  ) {

    var dataLoc = transform (targetEarthLoc);
    dataLoc.round();
    if (!dataLoc.isContained (dims)) dataLoc.markInvalid();
    if (targetDataLoc == null) targetDataLoc = new DataLocation (dims.length);
    targetDataLoc.setCoords (dataLoc);

    return (targetDataLoc);

  } // closest

  ////////////////////////////////////////////////////////////

  /**
   * Gets the boundary handler for this transform that handles boundary
   * checking and splitting.
   *
   * @return the boundary handler or null if no special bounary handling
   * is performed by this transform.
   *
   * @since 3.5.1
   */
  public BoundaryHandler getBoundaryHandler() { return (boundaryHandler); }

  ////////////////////////////////////////////////////////////

  /**
   * Converts data coordinates to geographic coordinates.
   *
   * @param dataLoc the data location.
   *
   * @return the earth location.  The earth location may contain
   * <code>Double.NaN</code> if no conversion is possible.
   *
   * @see #transform(DataLocation,EarthLocation)
   */
  public EarthLocation transform (
    DataLocation dataLoc
  ) {

    return (transform (dataLoc, null));

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Converts data coordinates to geographic coordinates.
   *
   * @param dataLoc the data location.
   * @param earthLoc the earth location or null.  If null, an object
   * is created and returned.  If non-null, the object is simply
   * modified.
   *
   * @return the earth location.  The earth location may contain
   * <code>Double.NaN</code> if no conversion is possible.
   *
   * @see #transform(DataLocation)
   */
  public EarthLocation transform (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    if (earthLoc == null) earthLoc = new EarthLocation();
    transformImpl (dataLoc, earthLoc);
    earthLoc.setDatum (this.getDatum());
    return (earthLoc);

  } // transform

  ////////////////////////////////////////////////////////////

  /** 
   * Implements the data to geographic transform.
   *
   * @see #transform(DataLocation,EarthLocation)
   */
  protected abstract void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  );

  ////////////////////////////////////////////////////////////

  /**
   * Converts geographic coordinates to data coordinates.
   *
   * @param earthLoc the earth location.
   *
   * @return the data location.  The data location may contain
   * <code>Double.NaN</code> if no conversion is possible.
   *
   * @see #transform(EarthLocation,DataLocation)
   */
  public DataLocation transform (
    EarthLocation earthLoc
  ) {

    return (transform (earthLoc, null));

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Converts geographic coordinates to data coordinates.
   *
   * @param earthLoc the earth location.
   * @param dataLoc the data location or null.  If null, an object
   * is created and returned.  If non-null, the object is simply
   * modified.
   *
   * @return the data location.  The data location may contain
   * <code>Double.NaN</code> if no conversion is possible.
   *
   * @see #transform(EarthLocation)
   */
  public DataLocation transform (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    if (dataLoc == null) dataLoc = new DataLocation (dims.length);
    if (!earthLoc.getDatum().equals (this.getDatum())) {
      earthLoc = (EarthLocation) earthLoc.clone();
      earthLoc.shiftDatum (this.getDatum());
    } // if
    transformImpl (earthLoc, dataLoc);
    return (dataLoc);

  } // transform

  ////////////////////////////////////////////////////////////

  /** 
   * Implements the geographic to data transform.
   *
   * @see #transform(EarthLocation,DataLocation)
   */
  protected abstract void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  );

  ////////////////////////////////////////////////////////////

  /** Gets a string describing the earth transform type. */
  public abstract String describe ();

  ////////////////////////////////////////////////////////////

  /**
   * Calculates the distance between two data locations.
   *
   * @param loc1 the first data coordinate.
   * @param loc2 the second data coordinate.
   * 
   * @return the distance between points in kilometers.
   * 
   * @see EarthLocation#distance
   */
  public double distance (
    DataLocation loc1,
    DataLocation loc2
  ) {
  
    return (transform (loc1).distance (transform (loc2)));

  } // distance

  ////////////////////////////////////////////////////////////

  /** 
   * Creates and returns a new subset transform.  The new transform is
   * arranged with the specified new origin and dimensions.  For
   * example, if the transform maps the data location (100,100) to
   * earth location (40N, 120W), and a translated transform is created
   * with (100,100) as the new origin, then the new transform will map
   * (0,0) to (40N, 120W).  Note that not all transforms support
   * subsets.
   *
   * @param newOrigin the new data location origin.
   * @param newDims the new data location dimensions.
   *
   * @throws UnsupportedOperationException if the underlying Earth
   * transform class does not support the creation of subset
   * transforms.
   */
  public EarthTransform getSubset (
    DataLocation newOrigin,
    int[] newDims
  ) {

    /**
     * This must be overridden in the child class to work correctly.
     * Here, we simply throw an exception.
     */
    throw new UnsupportedOperationException (
      "Transform subset not implemented for " + this.getClass().getName());

  } // getSubset

  ////////////////////////////////////////////////////////////

  /**
   * Gets the projection resolution at the specified location.  The
   * calcuation uses a centered difference approach with increments of
   * 0.5 on all sides of the data location to calculate the resolution
   * in all dimensions in km/pixel.
   *
   * @param loc the data location at which to calculate the
   * resolution.
   *
   * @return the resolution in each dimension in km/pixel.
   */
  public double[] getResolution (
    DataLocation loc
  ) {

    // Prepare array of distances
    // --------------------------
    int rank = loc.getRank();
    double[] distArray = new double[rank];
    DataLocation[] locArray = new DataLocation[] { 
      (DataLocation) loc.clone(),
      (DataLocation) loc.clone()
    };

    // Compute distance in each dimension
    // ----------------------------------
    for (int i = 0; i < rank; i++) {
      double coord = loc.get (i);
      locArray[0].set (i, coord - 0.5);
      locArray[1].set (i, coord + 0.5);
      distArray[i] = distance (locArray[0], locArray[1]);
      locArray[0].set (i, coord);
      locArray[1].set (i, coord);
    } // for

    return (distArray);

  } // getResolution

  ////////////////////////////////////////////////////////////

  /** Gets the transform data location dimensions. */
  public int[] getDimensions () {

    return ((int[]) dims.clone());

  } // getDimensions

  ////////////////////////////////////////////////////////////

  /** Creates and returns a copy of this object. */
  public Object clone () {

    EarthTransform trans = (EarthTransform) super.clone();
    trans.dims = (int[]) dims.clone();
    return (trans);

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Gets the spheroid that most closely resembles the specified
   * parameters.
   *
   * @param semiMajor the spheroid semi-major axis in meters.
   * @param semiMinor the spheroid semi-minor axis in meters.
   *
   * @return the matching spheroid code or -1 if none found.
   */
  public static int getSpheroid (
    double semiMajor,
    double semiMinor
  ) {

    // Find closest matching axes
    // --------------------------
    int matchIndex = -1;
    double matchDelta = Double.MAX_VALUE;
    for (int i = 0; i < SPHEROID_SEMI_MAJOR.length; i++) {
      double delta = 
        Math.abs (SPHEROID_SEMI_MAJOR[i] - semiMajor) +
        Math.abs (SPHEROID_SEMI_MINOR[i] - semiMinor);
      if (delta < matchDelta) {
        matchDelta = delta;
        matchIndex = i;
      } // if
    } // for

    // Check for close enough match
    // ----------------------------
    if (matchDelta < 0.02) return (matchIndex);
    return (-1);

  } // getSpheroid

  ////////////////////////////////////////////////////////////

  /**
   * Gets the spheroid that matches the specified name.
   *
   * @param name the spheroid name.
   *
   * @return the matching spheroid code or -1 if none found.
   */
  public static int getSpheroid (
    String name
  ) {

    for (int i = 0; i < SPHEROID_NAMES.length; i++)
      if (name.equalsIgnoreCase (SPHEROID_NAMES[i])) return (i);

    return (-1);

  } // getSpheroid

  ////////////////////////////////////////////////////////////

} // EarthTransform class

////////////////////////////////////////////////////////////////////////
