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
import java.util.Arrays;
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
  private int[] dims;
  
  /** The size of each chunk in the space (some chunks may be truncated). */
  private int[] chunkSize;

  /** The number of chunks along each dimension. */
  private int[] chunkCounts;

  /** The number of chunks in total, including truncated chunks. */
  private int totalChunks;
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new chunking scheme.
   *
   * @param dims the global dimensions of the n-dimensional chunk space.
   * @param chunkSize the size of each chunk in the space (some chunks may be
   * truncated).
   */
  public ChunkingScheme (
    int[] dims,
    int[] chunkSize
  ) {

    this.dims = dims;
    this.chunkSize = chunkSize;

    // Compute chunks along each dimension
    // -----------------------------------
    chunkCounts = new int[dims.length];
    for (int i = 0; i < dims.length; i++) {
      chunkCounts[i] = dims[i] / chunkSize[i];
      if (dims[i] % chunkSize[i] != 0) chunkCounts[i]++;
    } // for
    
    // Compute total chunks
    // --------------------
    totalChunks = 1;
    for (int i = 0; i < dims.length; i++)
      totalChunks *= chunkCounts[i];

  } // ChunkingScheme constructor

  ////////////////////////////////////////////////////////////

  /**
   * Determines if a chunk position is native to this chunking scheme.
   *
   * @param pos the position to check.
   *
   * @return true if the position is one in this chunking scheme or
   * false if not.
   *
   * @since 3.5.0
   */
  public boolean isNativePosition (
    ChunkPosition pos
  ) {

    boolean isNative;
    
    if (pos.start.length != dims.length || pos.length.length != dims.length)
      isNative = false;
    else {
      ChunkPosition testPos = getPosition (pos.start);
      isNative = Arrays.equals (pos.start, testPos.start) && Arrays.equals (pos.length, testPos.length);
    } // else

    return (isNative);

  } // isNativePosition

  ////////////////////////////////////////////////////////////

  /**
   * Gets the global dimensions of the chunking scheme.
   *
   * @return the global dimensions.
   */
  public int[] getDims() { return ((int[]) dims.clone()); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the size of chunks in this scheme.
   *
   * @return the chunk size.
   */
  public int[] getChunkSize() { return ((int[]) chunkSize.clone()); }

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
        int end = pos.start[i] + chunkSize[i];
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
   *
   * @since 3.5.0
   */
  public int getTotalChunks() { return (totalChunks); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the number of chunks along a dimension.
   *
   * @param index the index of the dimension to get the chunk count.
   *
   * @return the total number of chunks along the specified dimension,
   * including any truncated chunks.
   *
   * @since 3.5.0
   */
  public int getChunkCount (
    int index
  ) {
  
    return (chunkCounts[index]);
        
  } // getChunkCount

  ////////////////////////////////////////////////////////////

  /**
   * Gets the chunk position for a specified set of coordinates.
   *
   * @param coords the coordinates contained by the chunk.
   *
   * @return pos the chunk position that contains the specified coordinates.
   *
   * @since 3.5.0
   */
  public ChunkPosition getPosition (
    int[] coords
  ) {

    ChunkPosition pos = new ChunkPosition (dims.length);
    for (int i = 0; i < dims.length; i++) {
      pos.start[i] = (coords[i]/chunkSize[i]) * chunkSize[i];
      int end = pos.start[i] + chunkSize[i];
      if (end > dims[i]) end = dims[i];
      pos.length[i] = end - pos.start[i];
    } // for

    return (pos);

  } // getPosition

  ////////////////////////////////////////////////////////////

} // ChunkingScheme class

////////////////////////////////////////////////////////////////////////
