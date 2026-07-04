////////////////////////////////////////////////////////////////////////
/*

     File: ArrayReduction.java
   Author: Peter Hollemans
     Date: 2019/04/12

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util;

/**
 * An array reduction is an operator that reduces an array to a single
 * value.  Each function takes an array of values and from (inclusive) /
 * to (exclusive) pair similar to the <code>java.util.Arrays</code> methods and
 * returns a single value.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public interface ArrayReduction {

  ////////////////////////////////////////////////////////////

  /**
   * Reduces a range of byte values.
   *
   * @param array the array of values.
   * @param from the first index to use, inclusive.
   * @param to the index after the last value to use.
   *
   * @return the reduced value.
   */
  public byte reduce (byte[] array, int from, int to);

  /**
   * Reduces a range of short integer values.
   *
   * @param array the array of values.
   * @param from the first index to use, inclusive.
   * @param to the index after the last value to use.
   *
   * @return the reduced value.
   */
  public short reduce (short[] array, int from, int to);

  /**
   * Reduces a range of integer values.
   *
   * @param array the array of values.
   * @param from the first index to use, inclusive.
   * @param to the index after the last value to use.
   *
   * @return the reduced value.
   */
  public int reduce (int[] array, int from, int to);

  /**
   * Reduces a range of long integer values.
   *
   * @param array the array of values.
   * @param from the first index to use, inclusive.
   * @param to the index after the last value to use.
   *
   * @return the reduced value.
   */
  public long reduce (long[] array, int from, int to);

  /**
   * Reduces a range of single-precision floating-point values.
   *
   * @param array the array of values.
   * @param from the first index to use, inclusive.
   * @param to the index after the last value to use.
   *
   * @return the reduced value.
   */
  public float reduce (float[] array, int from, int to);

  /**
   * Reduces a range of double-precision floating-point values.
   *
   * @param array the array of values.
   * @param from the first index to use, inclusive.
   * @param to the index after the last value to use.
   *
   * @return the reduced value.
   */
  public double reduce (double[] array, int from, int to);

  ////////////////////////////////////////////////////////////

} // ArrayReduction interface

////////////////////////////////////////////////////////////////////////
