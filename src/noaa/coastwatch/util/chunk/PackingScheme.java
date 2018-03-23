////////////////////////////////////////////////////////////////////////
/*

     File: PackingScheme.java
   Author: Peter Hollemans
     Date: 2017/11/24

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

// Imports
// -------
import noaa.coastwatch.util.chunk.DataChunk.DataType;

/**
 * The <code>PackingScheme</code> interface is implemented by concrete classes that
 * have a strategy to pack floating point data to integer data and unpack integer
 * data to floating point data.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface PackingScheme {

  /**
   * Gets the unpacked data type that values in this packing scheme will
   * be unpacked to.
   */
  public DataType getUnpackedType();

  /**
   * Accepts a visitor in this scheme.
   *
   * @param visitor the visitor to accept.
   */
  public void accept (PackingSchemeVisitor visitor);
  
} // PackingScheme interface

////////////////////////////////////////////////////////////////////////

