////////////////////////////////////////////////////////////////////////
/*

     File: ChunkProducer.java
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

// Imports
// -------
import noaa.coastwatch.util.chunk.DataChunk.DataType;

/**
 * The <code>ChunkProducer</code> interface is implemented by all classes that
 * produce a data chunk on demand.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface ChunkProducer {

  /**
   * Gets the external type of chunks produced by this producer.
   *
   * @return the external data type.
   *
   * @see DataChunk#getExternalType
   */
  public DataType getExternalType();

  /**
   * Produces a data chunk.
   *
   * @param pos the position of the data chunk to produce.
   *
   * @return the data chunk.
   */
  public DataChunk getChunk (ChunkPosition pos);

  /**
   * Gets the native chunking scheme for chunks obtained from this producer.
   * Calls to {@link #getChunk} are optimized to use chunk positions obtained
   * from the native scheme.
   *
   * @return the native chunking scheme used by this producer, or null if
   * no native chunking scheme is used.
   *
   * @since 3.5.0
   */
  public ChunkingScheme getNativeScheme();

  /**
   * Gets a prototype chunk for this producer.  Chunks received from the
   * {@link #getChunk} method conform to the prototype.
   *
   * @return a prototype chunk for chunks produced by this producer.
   *
   * @since 3.5.0
   */
  public DataChunk getPrototypeChunk();

} // ChunkProducer interface

////////////////////////////////////////////////////////////////////////
