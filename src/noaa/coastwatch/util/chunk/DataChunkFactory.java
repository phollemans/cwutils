////////////////////////////////////////////////////////////////////////
/*

     File: DataChunkFactory.java
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

// Imports
// -------
import java.lang.reflect.Array;

/**
 * The <code>DataChunkFactory</code> class create appropriate
 * instances of the {@link DataChunk} class using a primitive data array.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class DataChunkFactory {

  // Variables
  // ---------

  /** The singleton instance of this class. */
  private static DataChunkFactory instance = new DataChunkFactory();

  ////////////////////////////////////////////////////////////

  private DataChunkFactory () { }

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static DataChunkFactory getInstance() { return (instance); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk.
   *
   * @param data the primitive array of data values to be used in the chunk.
   * @param isUnsigned the flag to indicate if the integer primitive values are
   * actually unsigned values packed into a signed primitive (eg: byte value in
   * the range [0..255] packaged as a signed byte in the range [-128..127]).
   * @param missingValue the missing value used as a marker for invalid data,
   * or null for none.
   * @param packing the packing scheme for floating point data values packed as
   * integer values in the chunk, or null for none.
   *
   * @return the data chunk wrapping the primitive array.
   */
  public DataChunk create (
    Object data,
    boolean isUnsigned,
    Object missingValue,
    PackingScheme packing
  ) {

    return (create (data, isUnsigned, missingValue, packing, null));
    
  } // create

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new uninitialized data chunk.
   *
   * @param primitiveType the primitive data type for the chunk.
   * @param values the number of values in the data chunk.
   * @param isUnsigned the flag to indicate if the integer primitive values are
   * actually unsigned values packed into a signed primitive (eg: byte value in
   * the range [0..255] packaged as a signed byte in the range [-128..127]).
   * @param missingValue the missing value used as a marker for invalid data,
   * or null for none.
   * @param packing the packing scheme for floating point data values packed as
   * integer values in the chunk, or null for none.
   * @param scaling the scaling scheme for floating point data values or
   * null for none.  Either packing or scaling may be specified but not both.
   *
   * @return the data chunk wrapping the primitive array.
   *
   * @since 3.6.1
   */
  public <T extends Number> DataChunk create (
    Class<T> primitiveType,
    int values,
    boolean isUnsigned,
    Object missingValue,
    PackingScheme packing,
    ScalingScheme scaling
  ) {

    Object data = Array.newInstance (primitiveType, values);
    return (create (data, isUnsigned, missingValue, packing, scaling));

  } // create

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk.
   *
   * @param data the primitive array of data values to be used in the chunk.
   * @param isUnsigned the flag to indicate if the integer primitive values are
   * actually unsigned values packed into a signed primitive (eg: byte value in
   * the range [0..255] packaged as a signed byte in the range [-128..127]).
   * @param missingValue the missing value used as a marker for invalid data,
   * or null for none.
   * @param packing the packing scheme for floating point data values packed as
   * integer values in the chunk, or null for none.
   * @param scaling the scaling scheme for floating point data values or
   * null for none.  Either packing or scaling may be specified but not both.
   *
   * @return the data chunk wrapping the primitive array.
   *
   * @since 3.6.1
   */
  public DataChunk create (
    Object data,
    boolean isUnsigned,
    Object missingValue,
    PackingScheme packing,
    ScalingScheme scaling
  ) {

    Class primitiveType = data.getClass().getComponentType();
    DataChunk chunk;

    if (primitiveType.equals (Byte.TYPE)) {
      if (scaling != null) throw new IllegalArgumentException ("Scaling not supported for " + primitiveType);
      chunk = new ByteChunk ((byte[]) data, isUnsigned, (Byte) missingValue, packing);
    } // if
    else if (primitiveType.equals (Short.TYPE)) {
      if (scaling != null) throw new IllegalArgumentException ("Scaling not supported for " + primitiveType);
      chunk = new ShortChunk ((short[]) data, isUnsigned, (Short) missingValue, packing);
    } // else if
    else if (primitiveType.equals (Integer.TYPE)) {
      if (scaling != null) throw new IllegalArgumentException ("Scaling not supported for " + primitiveType);
      chunk = new IntChunk ((int[]) data, isUnsigned, (Integer) missingValue, packing);
    } // else if
    else if (primitiveType.equals (Long.TYPE)) {
      if (scaling != null) throw new IllegalArgumentException ("Scaling not supported for " + primitiveType);
      chunk = new LongChunk ((long[]) data, isUnsigned, (Long) missingValue, packing);
    } // else if
    else if (primitiveType.equals (Float.TYPE)) {
      if (isUnsigned) throw new IllegalArgumentException ("Unsigned data not supported for " + primitiveType);
      if (packing != null) throw new IllegalArgumentException ("Packing not supported for " + primitiveType);
      chunk = new FloatChunk ((float[]) data, (Float) missingValue, scaling);
    } // else if
    else if (primitiveType.equals (Double.TYPE)) {
      if (isUnsigned) throw new IllegalArgumentException ("Unsigned data not supported for " + primitiveType);
      if (packing != null) throw new IllegalArgumentException ("Packing not supported for " + primitiveType);
      chunk = new DoubleChunk ((double[]) data, (Double) missingValue, scaling);
    } // else if
    else
      throw new IllegalArgumentException ("No data chunk support for " + primitiveType);

    return (chunk);
    
  } // create

  ////////////////////////////////////////////////////////////

} // DataChunkFactory class

////////////////////////////////////////////////////////////////////////
