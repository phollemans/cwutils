////////////////////////////////////////////////////////////////////////
/*

     File: ChunkingScheme.java
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
import java.util.NoSuchElementException;
import java.util.Iterator;
import noaa.coastwatch.util.chunk.ChunkPosition;

/**
 * The <code>ChunkingScheme</code> class describes an overall set of chunks
 * that fill an n-dimensional space and allows for iteration over the set.
 * Chunks are all of equal size, except for some chunks at the edges of the
 * space which may be truncated due to the global dimensions.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ChunkingScheme implements Iterable<ChunkPosition> {

  // Variables
  // ---------
  
  /** The global dimensions of the space of chunks. */
  private long[] dims;
  
  /** The size of each chunk in the space (some chunks may be truncated). */
  private long[] chunkSize;
  
  /** The number of chunks in total, including truncated chunks. */
  private int chunkCount;
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new chunking scheme.
   *
   * @param dims the global dimensions of the n-dimensional chunk space.
   * @param chunkSize the size of each chunk in the space (some chunks may be
   * truncated).
   */
  public ChunkingScheme (
    long[] dims,
    long[] chunkSize
  ) {

    this.dims = dims;
    this.chunkSize = chunkSize;
    
    chunkCount = 1;
    for (int i = 0; i < dims.length; i++) {
      int thisChunkCount = (int) (dims[i] / chunkSize[i]);
      if (dims[i] % chunkSize[i] != 0) thisChunkCount++;
      chunkCount *= thisChunkCount;
    } // for

  } // ChunkingScheme constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the global dimensions of the chunking scheme.
   *
   * @return the global dimensions.
   */
  public long[] getDims() { return (dims); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the size of chunks in this scheme.
   *
   * @return the chunk size.
   */
  public long[] getChunkSize() { return (chunkSize); }

  ////////////////////////////////////////////////////////////

  @Override
  public Iterator<ChunkPosition> iterator() {

    return (new Iterator<ChunkPosition>() {

      private ChunkPosition pos, nextPos;

      {
        pos = null;
        nextPos = getFirst();
      }

      public boolean hasNext() { return (nextPos != null); }

      public ChunkPosition next() {
        if (nextPos == null) throw (new NoSuchElementException());
        pos = nextPos;
        nextPos = getNext (pos);
        return (pos);
      } // next

    });
  } // iterator

  ////////////////////////////////////////////////////////////

  /**
   * Gets the first chunk position in this scheme.
   *
   * @return the first position.
   */
  private ChunkPosition getFirst () {

    ChunkPosition pos = new ChunkPosition (dims.length);
    for (int i = 0; i < dims.length; i++) {
      pos.length[i] = Math.min (chunkSize[i], dims[i]);
    } // for
    return (pos);

  } // getFirst

  ////////////////////////////////////////////////////////////

  /**
   * Gets the next chunk position in this scheme from a specified
   * position.
   *
   * @param pos the position to get the next position for.
   *
   * @return the next position.
   */
  private ChunkPosition getNext (ChunkPosition pos) {
  
    ChunkPosition next = (ChunkPosition) pos.clone();
    boolean hasNext = increment (next);
    if (!hasNext) next = null;
    return (next);

  } // getNext

  ////////////////////////////////////////////////////////////

  /**
   * Increments the specified chunk position to the next one.
   *
   * @param pos the position to increment.
   *
   * @return true if the position could be incremented or false if not.
   */
  private boolean increment (ChunkPosition pos) {

    // Find the index within the chunk to increment
    // --------------------------------------------
    int index = dims.length-1;
    boolean isDone = false;
    while (!isDone && pos.start[index] + chunkSize[index] > dims[index]-1) {
      index--;
      if (index < 0) isDone = true;
    } // while
    
    // Increment the index and check for truncation
    // --------------------------------------------
    if (!isDone) {
      pos.start[index] += chunkSize[index];
      for (int i = index+1; i < dims.length; i++)
        pos.start[i] = 0;
      for (int i = index; i < dims.length; i++) {
        long end = pos.start[i] + chunkSize[i];
        if (end > dims[i]) end = dims[i];
        pos.length[i] = end - pos.start[i];
      } // for
    } // if

    return (!isDone);

  } // increment

  ////////////////////////////////////////////////////////////

  /**
   * Gets the total count of chunks in this scheme.
   *
   * @return the total number of chunks.
   */
  public int getChunkCount() { return (chunkCount); }

  ////////////////////////////////////////////////////////////

} // ChunkingScheme class

////////////////////////////////////////////////////////////////////////
