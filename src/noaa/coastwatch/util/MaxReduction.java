////////////////////////////////////////////////////////////////////////
/*

     File: MaxReduction.java
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
 * The <code>MaxReduction</code> reduces an array to a single maximum value.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class MaxReduction implements ArrayReduction {
  
  public byte reduce (byte[] array, int from, int to) {
    byte maxValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] > maxValue) maxValue = array[i];
    return (maxValue);
  } // reduce
  
  public short reduce (short[] array, int from, int to) {
    short maxValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] > maxValue) maxValue = array[i];
    return (maxValue);
  } // reduce
  
  public int reduce (int[] array, int from, int to) {
    int maxValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] > maxValue) maxValue = array[i];
    return (maxValue);
  } // reduce
  
  public long reduce (long[] array, int from, int to) {
    long maxValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] > maxValue) maxValue = array[i];
    return (maxValue);
  } // reduce
  
  public float reduce (float[] array, int from, int to) {
    float maxValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] > maxValue) maxValue = array[i];
    return (maxValue);
  } // reduce
  
  public double reduce (double[] array, int from, int to) {
    double maxValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] > maxValue) maxValue = array[i];
    return (maxValue);
  } // reduce
  
} // MaxReduction class

////////////////////////////////////////////////////////////////////////


