////////////////////////////////////////////////////////////////////////
/*

     File: MedianReduction.java
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

// Imports
// -------
import java.util.Arrays;

/**
 * The <code>MedianReduction</code> reduces an array to a single median
 * value.  Note that the median methods have a side effect that the input
 * array is sorted between the specified bounds after the call.  To
 * prevent this, pass in a copy of the data array.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class MedianReduction implements ArrayReduction {

  public byte reduce (byte[] array, int from, int to) {
    Arrays.sort (array, from, to);
    int values = to-from;
    double median;
    if (values%2 == 0)
      median = (array[values/2 - 1] + array[values/2]) / 2.0;
    else
      median = array[(values+1)/2 - 1];
    return ((byte) Math.round (median));
  } // reduce
  
  public short reduce (short[] array, int from, int to) {
    Arrays.sort (array, from, to);
    int values = to-from;
    double median;
    if (values%2 == 0)
      median = (array[values/2 - 1] + array[values/2]) / 2.0;
    else
      median = array[(values+1)/2 - 1];
    return ((short) Math.round (median));
  } // reduce
  
  public int reduce (int[] array, int from, int to) {
    Arrays.sort (array, from, to);
    int values = to-from;
    double median;
    if (values%2 == 0)
      median = (array[values/2 - 1] + array[values/2]) / 2.0;
    else
      median = array[(values+1)/2 - 1];
    return ((int) Math.round (median));
  } // reduce
  
  public long reduce (long[] array, int from, int to) {
    Arrays.sort (array, from, to);
    int values = to-from;
    double median;
    if (values%2 == 0)
      median = (array[values/2 - 1] + array[values/2]) / 2.0;
    else
      median = array[(values+1)/2 - 1];
    return ((long) Math.round (median));
  } // reduce
  
  public float reduce (float[] array, int from, int to) {
    Arrays.sort (array, from, to);
    int values = to-from;
    double median;
    if (values%2 == 0)
      median = (array[values/2 - 1] + array[values/2]) / 2.0;
    else
      median = array[(values+1)/2 - 1];
    return ((float) median);
  } // reduce
  
  public double reduce (double[] array, int from, int to) {
    Arrays.sort (array, from, to);
    int values = to-from;
    double median;
    if (values%2 == 0)
      median = (array[values/2 - 1] + array[values/2]) / 2.0;
    else
      median = array[(values+1)/2 - 1];
    return (median);
  } // reduce
  
} // MedianReduction class

////////////////////////////////////////////////////////////////////////





