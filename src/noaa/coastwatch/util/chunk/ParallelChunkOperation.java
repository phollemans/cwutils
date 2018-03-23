////////////////////////////////////////////////////////////////////////
/*

     File: ParallelChunkOperation.java
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
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.ChunkOperation;

/**
 * The <code>ParallelChunkOperation</code> interface is implemented by
 * classes that perform a {@link ChunkOperation} on a set of chunk positions
 * in parallel.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface ParallelChunkOperation {

  /**
   * Initializes the parallel operation.
   *
   * @param positions the list of chunk positions to operate on.
   * @param op the chunk operation to perform at each position.
   */
  public void init (
    List<ChunkPosition> positions,
    ChunkOperation op
  );

  /** Starts the parallel operation. */
  public void start();

  /**
   * Cancels a parallel operation in progress.  Some instances of chunk
   * operations in progress may take time to complete, but no more operations
   * are started after the call to this method.
   */
  public void cancel();

  /**
   * Waits for completion of a parallel operation.  If the parallel operation
   * has been cancelled, this method will also wait for any chunk operations
   * that were already in progress when {@link #cancel} was called.  Otherwise
   * it simply waits until all operations are complete and returns.
   */
  public void waitForCompletion();

} // ParallelChunkOperation interface

////////////////////////////////////////////////////////////////////////
