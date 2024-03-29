////////////////////////////////////////////////////////////////////////
/*

     File: ChunkFunction.java
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
// --------
import java.util.List;
import noaa.coastwatch.util.chunk.DataChunk;

/**
 * The <code>ChunkFunction</code> interface is to be implemented by
 * any class that performs some type of processing on a set of input data
 * chunks to produce an output chunk.  The function is many-to-one.
 * A chunk function must be thread-safe to be used in a multi-threaded
 * application.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface ChunkFunction {

  /**
   * Applies some function to a list of chunks.
   *
   * @param pos the position to perform the function.
   * @param inputChunks the list of chunks as input to the function.
   *
   * @return the output chunk of the function, or null if no valid output
   * chunk could be computed.
   */
  public DataChunk apply (
    ChunkPosition pos,
    List<DataChunk> inputChunks
  );

  /**
   * Estimates the memory requirements for processing a chunk of 
   * data at the specified position, not including the input and output chunk
   * internal data arrays.
   * 
   * @param pos the position to estimate the memory usage.
   * @param chunks the number of input chunks that will be processed
   * at once.
   * 
   * @return the memory in bytes.
   * 
   * @throws UnsupportedOperationException if the memory calculation is not
   * implemented by this function.
   * 
   * @since 3.8.1
   */
  public default long getMemory (
    ChunkPosition pos, 
    int chunks
  ) { 

    throw new UnsupportedOperationException(); 

  } // getMemory

} // ChunkFunction interface

////////////////////////////////////////////////////////////////////////
