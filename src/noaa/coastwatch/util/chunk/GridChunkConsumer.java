////////////////////////////////////////////////////////////////////////
/*

     File: GridChunkConsumer.java
   Author: Peter Hollemans
     Date: 2017/12/31

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
import noaa.coastwatch.util.Grid;

/**
 * The <code>GridChunkConsumer</code> class consumes data chunks into a
 * {@link noaa.coastwatch.util.Grid} object.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class GridChunkConsumer implements ChunkConsumer {

  // Variables
  // ---------

  /** The grid to use as a sink for data chunks. */
  private Grid grid;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new consumer.
   *
   * @param grid the grid to use for setting chunk data.
   */
  public GridChunkConsumer (
    Grid grid
  ) {
  
    this.grid = grid;

  } // GridChunkConsumer constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void putChunk (ChunkPosition pos, DataChunk chunk) {

    int[] start = new int[] {(int) pos.start[0], (int) pos.start[1]};
    int[] length = new int[] {(int) pos.length[0], (int) pos.length[1]};
    synchronized (grid) {
      grid.setData (chunk.getPrimitiveData(), start, length);
    } // synchronized

  } // putChunk

  ////////////////////////////////////////////////////////////

} // GridChunkConsumer class

////////////////////////////////////////////////////////////////////////


