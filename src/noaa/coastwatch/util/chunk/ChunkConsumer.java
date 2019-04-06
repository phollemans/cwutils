////////////////////////////////////////////////////////////////////////
/*

     File: ChunkConsumer.java
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
 * The <code>ChunkConsumer</code> interface is implemented by all classes that
 * consume data chunks.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface ChunkConsumer {

  /**
   * Consumes a data chunk.
   *
   * @param pos the position of the data chunk to consume.
   * @param chunk the data chunk to consume.
   */
  public void putChunk (ChunkPosition pos, DataChunk chunk);

  /**
   * Gets the native chunking scheme for chunks accepted by this producer.
   * Calls to {@link #putChunk} are optimized to use chunk positions obtained
   * from the native scheme.
   *
   * @return the native chunking scheme used by this consumer, or null if
   * no native chunking scheme is used.
   *
   * @since 3.5.0
   */
  public ChunkingScheme getNativeScheme();

  /**
   * Gets a prototype chunk for this consumer.  Chunks passed to the
   * {@link #putChunk} method must conform to the prototype.
   *
   * @return a prototype chunk for chunks accepted by this consumer.
   *
   * @since 3.5.0
   */
  public DataChunk getPrototypeChunk();

} // ChunkConsumer interface

////////////////////////////////////////////////////////////////////////

