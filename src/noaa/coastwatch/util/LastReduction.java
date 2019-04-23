////////////////////////////////////////////////////////////////////////
/*

     File: LastReduction.java
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
 * The <code>LastReduction</code> reduces an array to a single last value.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class LastReduction implements ArrayReduction {
  
  public byte reduce (byte[] array, int from, int to) {
    return (array[to-1]);
  } // reduce
  
  public short reduce (short[] array, int from, int to) {
    return (array[to-1]);
  } // reduce
  
  public int reduce (int[] array, int from, int to) {
    return (array[to-1]);
  } // reduce
  
  public long reduce (long[] array, int from, int to) {
    return (array[to-1]);
  } // reduce
  
  public float reduce (float[] array, int from, int to) {
    return (array[to-1]);
  } // reduce
  
  public double reduce (double[] array, int from, int to) {
    return (array[to-1]);
  } // reduce
  
} // LastReduction class

////////////////////////////////////////////////////////////////////////



