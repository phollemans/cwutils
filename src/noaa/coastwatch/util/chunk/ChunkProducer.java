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

} // ChunkProducer interface

////////////////////////////////////////////////////////////////////////
