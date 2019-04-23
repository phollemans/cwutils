////////////////////////////////////////////////////////////////////////
/*

     File: MinReduction.java
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
 * The <code>MinReduction</code> reduces an array to a single minimum value.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class MinReduction implements ArrayReduction {
  
  public byte reduce (byte[] array, int from, int to) {
    byte minValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] < minValue) minValue = array[i];
    return (minValue);
  } // reduce
  
  public short reduce (short[] array, int from, int to) {
    short minValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] < minValue) minValue = array[i];
    return (minValue);
  } // reduce
  
  public int reduce (int[] array, int from, int to) {
    int minValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] < minValue) minValue = array[i];
    return (minValue);
  } // reduce
  
  public long reduce (long[] array, int from, int to) {
    long minValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] < minValue) minValue = array[i];
    return (minValue);
  } // reduce
  
  public float reduce (float[] array, int from, int to) {
    float minValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] < minValue) minValue = array[i];
    return (minValue);
  } // reduce
  
  public double reduce (double[] array, int from, int to) {
    double minValue = array[from];
    for (int i = from+1; i < to; i++)
      if (array[i] < minValue) minValue = array[i];
    return (minValue);
  } // reduce
  
} // MinReduction class

////////////////////////////////////////////////////////////////////////


