////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthTransform.java
  PURPOSE: Abstract class to set the functionality of all transform
           subclasses.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/15
  CHANGES: 2002/05/14, PFH, added javadoc, package
           2002/06/01, PFH, made abstract, added distance function
           2002/06/04, PFH, added integer coord distance function
           2002/06/06, PFH, added getType
           2002/06/07, PFH, added toGeo for integer coords
           2002/07/25, PFH, converted to location classes
           2002/07/31, PFH, added getResolution
           2004/05/04, PFH, added createTranslated() method
           2004/09/29, PFH, modified to extend MetadataContainer
           2004/09/30, PFH, added getBoundingBox()
           2004/10/05, PFH, added getDimensions()
           2004/10/13, PFH, changed createTranslated() to getSubset()
           2005/05/16, PFH, modified for datum shifting and in-place transform
           2005/05/20, PFH
           - moved 2D-specific methods to EarthTransform2D
           - reworked getResolution() to be independent of dimension
           2006/05/26, PFH, modified to implement SpheroidConstants
           2006/05/28, PFH, moved getSpheroid() methods from GCTP class
           2014/03/25, PFH
           - Changes: Added get2DVersion()
           - Issue: In some cases, we want to use 2D data locations and the code
            assumes the transform is 2D.  But this is confusing, so we've added
            the method to avoid having to cast to EarthTransform2D in user code.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import noaa.coastwatch.util.*;

/**
 * The Earth transform class translates between data coordinates and
 * geographic coordinates in latitude and longitude degrees.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class EarthTransform
  extends MetadataContainer
  implements SpheroidConstants {

  // Constants
  // ---------
  
  /** The instance of WGS84. */
  private static final Datum WGS84 = 
    DatumFactory.create (SpheroidConstants.WGS84);

  // Variables
  // ---------

  /** Data location dimensions. */
  protected int[] dims;

  ////////////////////////////////////////////////////////////

  /**
   * Gets a version of this transform that can be used with 2D data
   * locations.
   *
   * @since 3.3.1
   *
   * @return the 2D version of this transform.
   */
  public abstract EarthTransform2D get2DVersion ();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the transform datum.  Unless overridden by the child class,
   * this method returns WGS84.
   * 
   * @return the geodetic datum.
   */
  public Datum getDatum () { return (WGS84); }

  ////////////////////////////////////////////////////////////

  /**
   * Converts data coordinates to geographic coordinates.
   *
   * @param dataLoc the data location.
   *
   * @return the Earth location.  The Earth location may contain
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
   * @param earthLoc the Earth location or null.  If null, an object
   * is created and returned.  If non-null, the object is simply
   * modified.
   *
   * @return the Earth location.  The Earth location may contain
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
   * @param earthLoc the Earth location.
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
   * @param earthLoc the Earth location.
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

  /** Gets a string describing the Earth transform type. */
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
   * Earth location (40N, 120W), and a translated transform is created
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
