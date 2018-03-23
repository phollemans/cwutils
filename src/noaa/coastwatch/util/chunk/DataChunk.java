////////////////////////////////////////////////////////////////////////
/*

     File: DataChunk.java
   Author: Peter Hollemans
     Date: 2017/11/01

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
 * The <code>DataChunk</code> interface is implemented by concrete classes that
 * hold data of a specific internal primitive type.  The internal type may be
 * different from the external type that the data values represent.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface DataChunk {

  /** The enumeration of the possible chunk external data types. */
  public enum DataType {
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE
  }; // DataType enum

  /**
   * Gets the external data type that values in this chunk are meant
   * to represent.  This is the type that the chunk unpacks to if values
   * are packed.
   *
   * @return the external data type.
   */
  public DataType getExternalType();

  /**
   * Accepts a visitor in this chunk.
   *
   * @param visitor the visitor to accept.
   */
  public void accept (ChunkVisitor visitor);

  /**
   * Gets the number of values held by this chunk.
   *
   * @return the number of values.
   */
  public int getValues();

  /**
   * Gets the primitive chunk data.
   *
   * @return the primitive data stored in this chunk.
   */
  public Object getPrimitiveData();

  /**
   * Creates a blank copy of this data chunk.
   *
   * @return the new blank data chunk.  The data values are uninitialized.
   */
  public DataChunk blankCopy();

  /**
   * Creates a blank copy of this data chunk with the specified number of
   * data values.
   *
   * @param values the number of data values in the new blank data chunk.
   *
   * @return the new blank data chunk.  The data values are uninitialized.
   */
  public DataChunk blankCopyWithValues (int values);

} // DataChunk interface

////////////////////////////////////////////////////////////////////////
