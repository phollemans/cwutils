////////////////////////////////////////////////////////////////////////
/*

     File: ChunkPosition.java
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
import java.util.Arrays;

/**
 * The <code>ChunkPosition</code> class marks a position within a
 * {@link ChunkingScheme}.  The position is specified by its starting
 * location and length along each dimension.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ChunkPosition {

  // Variables
  // ---------

  /** The start coordinates of the chunk within the chunk space. */
  public int[] start;

  /** The length of the chunk along each dimension. */
  public int[] length;

  ////////////////////////////////////////////////////////

  /**
   * Creates a new position with start and length initialized to
   * zero along each dimension.
   *
   * @param rank the number of dimensions of the chunk space.
   */
  public ChunkPosition (int rank) {
  
    start = new int[rank];
    length = new int[rank];

  } // ChunkPosition constructor

  ////////////////////////////////////////////////////////

  @Override
  public ChunkPosition clone () {

    ChunkPosition pos = new ChunkPosition (start.length);
    for (int i = 0; i < start.length; i++) {
      pos.start[i] = start[i];
      pos.length[i] = length[i];
    } // for
   return (pos);

  } // clone

  ////////////////////////////////////////////////////////

  @Override
  public String toString() {

    return (
      "ChunkPosition[" +
      "start=" + Arrays.toString (start) + "," +
      "length=" + Arrays.toString (length) +
      "]"
    );

  } // toString

  ////////////////////////////////////////////////////////

  } // ChunkPosition class

////////////////////////////////////////////////////////////////////////
