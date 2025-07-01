////////////////////////////////////////////////////////////////////////
/*

     File: GeoVectorProjection.java
   Author: Peter Hollemans
     Date: 2005/08/05

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
import java.util.Arrays;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform2D;

// Testing
import java.util.logging.Logger;
import noaa.coastwatch.test.TestLogger;

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
@noaa.coastwatch.test.Testable
public class GeoVectorProjection 
  extends EarthTransform2D {

  private static final Logger LOGGER = Logger.getLogger (GeoVectorProjection.class.getName());

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
    this.dims = new int[2];
    dims[latLocIndex] = latArray.length;
    dims[lonLocIndex] = lonArray.length;

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

    double result = -1;

    // Base case, there is no array to search
    if (array.length == 1) {
      if (array[0] == value)
        result = 0;
    } // if

    else {

      // Initialize the loop
      int low = 0;
      int high = array.length - 1;
      boolean isAscending = (array[0] < array[high]);

      // Perform binary search to find bracketing indices
      while ((high - low) > 1) {

        int mid = low + (high - low) / 2;

        // Check for an exact match at the midpoint
        if (array[mid] == value) {
          result = mid;
          break;
        } // if

        // Deal with ascending array
        if (isAscending) {
          if (array[mid] < value)
            low = mid;
          else
            high = mid;
        } // if

        // Deal with descending array
        else {
          if (array[mid] > value)
            low = mid;
          else
            high = mid;
        } // else

      } // while

      // Calculate fractional index if needed.  At the same time, we check
      // that the value is bracketed by low and high.
      if (result == -1) {
        double lowerValue = isAscending ? array[low] : array[high];
        double upperValue = isAscending ? array[high] : array[low];
        if (lowerValue <= value && value <= upperValue) {
          result = low + (upperValue - value) / (upperValue - lowerValue);
        } // if
      } // if

    } // if

    return (result);

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

  } // getValue

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

  @Override
  public boolean equals (
    Object obj
  ) {

    // Check object instance
    // ---------------------
    if (!(obj instanceof GeoVectorProjection)) return (false);

    // Check datum
    // -----------

    /**
     * We current don't need to check the datum here, because all GeoVectorProjection
     * instances use WGS84.
     */

    GeoVectorProjection otherObject = (GeoVectorProjection) obj;

    // Check lat/long arrays
    // ---------------------
    if (!Arrays.equals (this.latArray, otherObject.latArray)) return (false);
    if (!Arrays.equals (this.lonArray, otherObject.lonArray)) return (false);

    return (true);
    
  } // equals
  
  ////////////////////////////////////////////////////////////

  // Test helper function
  private static void testGetIndex (double[] array, double value, double expected) {

    double result = getIndex (array, value);
    LOGGER.fine ("Got value = " + result + ", expected value = " + expected);
    assert (!Double.isNaN (result));
    assert (Math.abs (result - expected) < 1e-6);

  } // testGetIndex

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (GeoVectorProjection.class);

    logger.test ("Ascending array, value between elements");
    testGetIndex (new double[]{10.0, 20.0, 30.0, 40.0, 50.0}, 35.0, 2.5);
    logger.passed();

    logger.test ("Descending array, value between elements");
    testGetIndex(new double[]{50.0, 40.0, 30.0, 20.0, 10.0}, 35.0, 1.5);
    logger.passed();

    logger.test ("Exact match in ascending array");
    testGetIndex(new double[]{10.0, 20.0, 30.0, 40.0, 50.0}, 20.0, 1.0);
    logger.passed();

    logger.test ("Exact match in descending array");
    testGetIndex(new double[]{50.0, 40.0, 30.0, 20.0, 10.0}, 40.0, 1.0);
    logger.passed();

    logger.test ("Value below/above the array range in ascending array");
    testGetIndex(new double[]{10.0, 20.0, 30.0, 40.0, 50.0}, 5.0, -1);
    testGetIndex(new double[]{10.0, 20.0, 30.0, 40.0, 50.0}, 60.0, -1);
    logger.passed();

    logger.test ("Value below/above the array range in descending array");
    testGetIndex(new double[]{50.0, 40.0, 30.0, 20.0, 10.0}, 5.0, -1);
    testGetIndex(new double[]{50.0, 40.0, 30.0, 20.0, 10.0}, 60.0, -1);
    logger.passed();

    logger.test ("Single-element array");
    testGetIndex(new double[]{10.0}, 10.0, 0.0);
    testGetIndex(new double[]{10.0}, 5.0, -1);
    testGetIndex(new double[]{10.0}, 15.0, -1);
    logger.passed();

    logger.test ("Two-element array, value between elements");
    testGetIndex(new double[]{10.0, 20.0}, 15.0, 0.5);
    testGetIndex(new double[]{20.0, 10.0}, 15.0, 0.5);
    logger.passed();

    logger.test ("Two-element array, value outside range");
    testGetIndex(new double[]{10.0, 20.0}, 5.0, -1);
    testGetIndex(new double[]{10.0, 20.0}, 25.0, -1);
    testGetIndex(new double[]{20.0, 10.0}, 5.0, -1);
    testGetIndex(new double[]{20.0, 10.0}, 25.0, -1);
    logger.passed();

    logger.test ("Negative numbers in the array");
    testGetIndex(new double[]{-50.0, -40.0, -30.0, -20.0, -10.0}, -35.0, 1.5);
    testGetIndex(new double[]{-10.0, -20.0, -30.0, -40.0, -50.0}, -35.0, 2.5);
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // GeoVectorProjection class

////////////////////////////////////////////////////////////////////////
