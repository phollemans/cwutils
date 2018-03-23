////////////////////////////////////////////////////////////////////////
/*

     File: IntegerValuedDataChunk.java
   Author: Peter Hollemans
     Date: 2017/12/16

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
 * The <code>IntegerValuedDataChunk</code> interface is implemented by concrete
 * classes that hold data of a specific primitive integer type (byte, short,
 * int, long).
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface IntegerValuedDataChunk extends DataChunk {

  /**
   * Gets the unsigned flag.
   *
   * @return true if the primitive data values are unsigned.
   */
  public boolean isUnsigned();

  /**
   * Gets the packing scheme.  A packing scheme is used to reduce the size
   * of floating point data to store in an integer value.
   *
   * @return the packing scheme or null if the chunk data is not
   * packed.
   */
  public PackingScheme getPackingScheme();

} // IntegerValuedDataChunk interface

////////////////////////////////////////////////////////////////////////

