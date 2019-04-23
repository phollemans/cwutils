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

  public byte reduce (byte[] array, int from, int to);
  public short reduce (short[] array, int from, int to);
  public int reduce (int[] array, int from, int to);
  public long reduce (long[] array, int from, int to);
  public float reduce (float[] array, int from, int to);
  public double reduce (double[] array, int from, int to);

  ////////////////////////////////////////////////////////////

} // ArrayReduction interface

////////////////////////////////////////////////////////////////////////
