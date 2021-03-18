////////////////////////////////////////////////////////////////////////
/*

     File: FloatingPointValuedDataChunk.java
   Author: Peter Hollemans
     Date: 2021/03/07

  CoastWatch Software Library and Utilities
  Copyright (c) 2021 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.chunk;

/**
 * The <code>FloatingPointValuedDataChunk</code> interface is implemented by concrete
 * classes that hold data of a specific primitive floating point type (float, double).
 *
 * @author Peter Hollemans
 * @since 3.6.1
 */
public interface FloatingPointValuedDataChunk extends DataChunk {

  /**
   * Gets the scaling scheme.  A scaling scheme is used to alter the values of
   * floating point data using some scaling.
   *
   * @return the scaling scheme or null if the chunk data is not
   * scaled.
   */
  public ScalingScheme getScalingScheme();

} // FloatingPointValuedDataChunk interface

////////////////////////////////////////////////////////////////////////


