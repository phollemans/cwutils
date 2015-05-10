////////////////////////////////////////////////////////////////////////
/*
     FILE: GeoVectorProjection.java
  PURPOSE: Geographic vector based projection.
   AUTHOR: Peter Hollemans
     DATE: 2005/08/05
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform2D;

/**
 * The <code>GeoVectorProjection</code> class uses arrays of latitude
 * and longitude data to transform coordinates.  It is assumed that
 * the projection may be described by two 1D arrays: one for latitude
 * and one for longitude.  Each row in data coordinates has all the
 * same latitude, and each column has all the same longitude or
 * vice-versa.  Since the projection is based on discrete data values,
 * it is similar to swath in that the transformation of data
 * coordinates outside the data dimensions returns invalid Earth
 * locations.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class GeoVectorProjection 
  extends EarthTransform2D {

  // Constants
  // ---------

  /** Projection description string. */
  public final static String DESCRIPTION = "cylindrical";  

  // Variables
  // ---------

  /** The array of latitude locations (degrees). */
  private double[] latArray;

  /** The array of longitude locations (degrees). */
  private double[] lonArray;

  /** The data location index associated with latitude. */
  private int latLocIndex;

  /** The data location index associated with longitude. */
  private int lonLocIndex;

  ////////////////////////////////////////////////////////////

  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new projection.
   * 
   * @param latArray the array of latitude locations in degrees
   * (should be monotonic).
   * @param lonArray the array of longitude locations in degrees
   * (should be monotonic).
   * @param latLocIndex the data location index associated with
   * latitude.
   * @param lonLocIndex the data location index associated with
   * longitude.
   */
  public GeoVectorProjection (
    double[] latArray,
    double[] lonArray,
    int latLocIndex,
    int lonLocIndex
  ) {

    // Initialize
    // ----------
    this.latArray = (double[]) latArray.clone();
    this.lonArray = (double[]) lonArray.clone();
    this.latLocIndex = latLocIndex;
    this.lonLocIndex = lonLocIndex;

  } // GeoVectorProjection constructor

  ////////////////////////////////////////////////////////////

  /**
   * Finds the fractional index of a value in an array.
   * 
   * @param array the data array to search.
   * @param value the value to search for.
   * 
   * @return the fractional index, or -1 if the value cannot be found.
   */
  private static double getIndex (
    double[] array,
    double value
  ) {

    for (int i = 0; i < array.length-1; i++) {
      if (array[i] <= value && array[i+1] >= value) {
        return (i + (value - array[i])/(array[i+1] - array[i]));
      } // if
    } // for

    return (-1);

  } // getIndex

  ////////////////////////////////////////////////////////////

  /**
   * Finds the interpolated value of a fractional index.
   * 
   * @param array the data array to search.
   * @param index the index to interpolate.
   * 
   * @return the interpolated value, or Double.NaN if the index is
   * outside the array bounds.
   */
  private static double getValue (
    double[] array,
    double index
  ) {

    int lower = (int) Math.floor (index);
    if (lower < 0 || lower > array.length-1) return (Double.NaN);
    int upper = (int) Math.ceil (index);
    if (upper < 0 || upper > array.length-1) return (Double.NaN);
    return (array[lower] + (index - lower)*(array[upper] - array[lower]));

  } // getIndex

  ////////////////////////////////////////////////////////////

  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    double latIndex = getIndex (latArray, earthLoc.lat);
    dataLoc.set (latLocIndex, latIndex);
    double lonIndex = getIndex (lonArray, earthLoc.lon);
    dataLoc.set (lonLocIndex, lonIndex);

  } // transformImpl

  ////////////////////////////////////////////////////////////

  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    earthLoc.lat = getValue (latArray, dataLoc.get (latLocIndex));
    earthLoc.lon = getValue (lonArray, dataLoc.get (lonLocIndex));

  } // transformImpl

  ////////////////////////////////////////////////////////////

} // GeoVectorProjection class

////////////////////////////////////////////////////////////////////////
