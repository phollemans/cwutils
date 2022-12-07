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
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.DataChunkFactory;
import noaa.coastwatch.io.tile.TilingScheme;
import java.lang.reflect.Array;

/**
 * The <code>GridChunkConsumer</code> class consumes data chunks and places
 * the chunk data into a {@link noaa.coastwatch.util.Grid} object.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class GridChunkConsumer implements ChunkConsumer {

  // Variables
  // ---------

  /** The grid to use as a sink for data chunks. */
  private Grid grid;

  /** The chunking scheme used or null for none. */
  private ChunkingScheme scheme;

  /** The prototype chunk consumed. */
  private DataChunk protoChunk;

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

    // Create chunking scheme
    // ----------------------
    TilingScheme tiling = grid.getTilingScheme();
    if (tiling != null) {
      int[] chunkingDims = tiling.getDimensions();
      int[] chunkSize = tiling.getTileDimensions();
      scheme = new ChunkingScheme (chunkingDims, chunkSize);
    } // if

    // Create packing or scaling scheme
    // --------------------------------
    PackingScheme packing = null;
    ScalingScheme scaling = null;
    double[] scalingArray = grid.getScaling();
    if (scalingArray != null && !(scalingArray[0] == 1 && scalingArray[1] == 0)) {
      var dataClass = grid.getDataClass();
      if (dataClass.equals (Float.TYPE))
        scaling = new FloatScalingScheme ((float) scalingArray[0], (float) scalingArray[1]);
      else if (dataClass.equals (Double.TYPE))
        scaling = new DoubleScalingScheme (scalingArray[0], scalingArray[1]);
      else
        packing = new DoublePackingScheme (scalingArray[0], scalingArray[1]);
    } // if

    // Create the prototype chunk
    // --------------------------
    Object data = Array.newInstance (grid.getDataClass(), 0);
    protoChunk = DataChunkFactory.getInstance().create (data,
      grid.getUnsigned(), grid.getMissing(), packing, scaling);

  } // GridChunkConsumer constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void putChunk (ChunkPosition pos, DataChunk chunk) {

    // Check that the chunk passed in conforms to the prototype!
    if (!chunk.isCompatible (protoChunk))
      throw new IllegalArgumentException ("Chunk is incompatible with prototype");

    int[] start = new int[] {(int) pos.start[0], (int) pos.start[1]};
    int[] length = new int[] {(int) pos.length[0], (int) pos.length[1]};
    synchronized (grid) {
      grid.setData (chunk.getPrimitiveData(), start, length);
    } // synchronized

  } // putChunk

  ////////////////////////////////////////////////////////////

  @Override
  public ChunkingScheme getNativeScheme() { return (scheme); }
  
  ////////////////////////////////////////////////////////////
  
  @Override
  public DataChunk getPrototypeChunk() {

    return (protoChunk.blankCopy());

  } // getPrototypeChunk

  ////////////////////////////////////////////////////////////

} // GridChunkConsumer class

////////////////////////////////////////////////////////////////////////


