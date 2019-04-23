////////////////////////////////////////////////////////////////////////
/*

     File: MeanReduction.java
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
 * The <code>MeanReduction</code> reduces an array to a single mean value.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class MeanReduction implements ArrayReduction {
  
  public byte reduce (byte[] array, int from, int to) {
    double sum = 0;
    for (int i = from; i < to; i++) sum += array[i];
    return ((byte) Math.round (sum/(to-from)));
  } // reduce
  
  public short reduce (short[] array, int from, int to) {
    double sum = 0;
    for (int i = from; i < to; i++) sum += array[i];
    return ((short) Math.round (sum/(to-from)));
  } // reduce
  
  public int reduce (int[] array, int from, int to) {
    double sum = 0;
    for (int i = from; i < to; i++) sum += array[i];
    return ((int) Math.round (sum/(to-from)));
  } // reduce
  
  public long reduce (long[] array, int from, int to) {
    double sum = 0;
    for (int i = from; i < to; i++) sum += array[i];
    return ((long) Math.round (sum/(to-from)));
  } // reduce
  
  public float reduce (float[] array, int from, int to) {
    double sum = 0;
    for (int i = from; i < to; i++) sum += array[i];
    return ((float) (sum/(to-from)));
  } // reduce
  
  public double reduce (double[] array, int from, int to) {
    double sum = 0;
    for (int i = from; i < to; i++) sum += array[i];
    return (sum/(to-from));
  } // reduce
  
} // MeanReduction class

////////////////////////////////////////////////////////////////////////



