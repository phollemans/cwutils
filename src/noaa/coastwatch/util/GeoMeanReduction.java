////////////////////////////////////////////////////////////////////////
/*

     File: GeoMeanReduction.java
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
 * The <code>GeoMeanReduction</code> reduces an array to a single geometric
 * mean value.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class GeoMeanReduction implements ArrayReduction {

  public byte reduce (byte[] array, int from, int to) {
    double sum = 0;
    int values = 0;
    for (int i = from; i < to; i++) {
      if (array[i] > 0) {
        sum += Math.log (array[i]);
        values++;
      } // if
    } // for
    double mean = (values == 0 ? 0 : Math.exp (sum/values));
    return ((byte) Math.round (mean));
  } // reduce
  
  public short reduce (short[] array, int from, int to) {
    double sum = 0;
    int values = 0;
    for (int i = from; i < to; i++) {
      if (array[i] > 0) {
        sum += Math.log (array[i]);
        values++;
      } // if
    } // for
    double mean = (values == 0 ? 0 : Math.exp (sum/values));
    return ((short) Math.round (mean));
  } // reduce
  
  public int reduce (int[] array, int from, int to) {
    double sum = 0;
    int values = 0;
    for (int i = from; i < to; i++) {
      if (array[i] > 0) {
        sum += Math.log (array[i]);
        values++;
      } // if
    } // for
    double mean = (values == 0 ? 0 : Math.exp (sum/values));
    return ((int) Math.round (mean));
  } // reduce
  
  public long reduce (long[] array, int from, int to) {
    double sum = 0;
    int values = 0;
    for (int i = from; i < to; i++) {
      if (array[i] > 0) {
        sum += Math.log (array[i]);
        values++;
      } // if
    } // for
    double mean = (values == 0 ? 0 : Math.exp (sum/values));
    return ((long) Math.round (mean));
  } // reduce
  
  public float reduce (float[] array, int from, int to) {
    double sum = 0;
    int values = 0;
    for (int i = from; i < to; i++) {
      if (array[i] > 0) {
        sum += Math.log (array[i]);
        values++;
      } // if
    } // for
    double mean = (values == 0 ? Float.NaN : Math.exp (sum/values));
    return ((float) mean);
  } // reduce
  
  public double reduce (double[] array, int from, int to) {
    double sum = 0;
    int values = 0;
    for (int i = from; i < to; i++) {
      if (array[i] > 0) {
        sum += Math.log (array[i]);
        values++;
      } // if
    } // for
    double mean = (values == 0 ? Double.NaN : Math.exp (sum/values));
    return (mean);
  } // reduce
  
} // GeoMeanReduction class

////////////////////////////////////////////////////////////////////////




