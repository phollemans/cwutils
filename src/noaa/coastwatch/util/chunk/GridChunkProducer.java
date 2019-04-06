////////////////////////////////////////////////////////////////////////
/*

     File: GridChunkProducer.java
   Author: Peter Hollemans
     Date: 2017/12/11

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
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.util.chunk.DataChunk.DataType;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.PackingScheme;
import noaa.coastwatch.util.chunk.DoublePackingScheme;
import noaa.coastwatch.util.Grid;
import java.lang.reflect.Array;

/**
 * The <code>GridChunkProducer</code> class provides data chunks from
 * a {@link noaa.coastwatch.util.Grid} object.  The chunks produced will
 * be of an appropriate type for the grid primitive data, created using a
 * {@link DataChunkFactory} instance.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class GridChunkProducer implements ChunkProducer {

  // Variables
  // ---------

  /** The grid to use as a source of data. */
  private Grid grid;

  /** The packing scheme for new chunks. */
  private PackingScheme packing;
  
  /** The chunking scheme used or null for none. */
  private ChunkingScheme scheme;

  /** The prototype chunk produced. */
  private DataChunk protoChunk;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new producer.
   *
   * @param grid the grid to use for chunk data.
   */
  public GridChunkProducer (
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

    // Create packing scheme
    // ---------------------
    double[] scaling = grid.getScaling();
    if (scaling != null && !(scaling[0] == 1 && scaling[1] == 0)) {
      DoublePackingScheme doublePacking = new DoublePackingScheme();
      doublePacking.scale = scaling[0];
      doublePacking.offset = scaling[1];
      packing = doublePacking;
    } // if

    // Create the prototype chunk
    // --------------------------
    Object data = Array.newInstance (grid.getDataClass(), 0);
    protoChunk = DataChunkFactory.getInstance().create (data,
      grid.getUnsigned(), grid.getMissing(), packing);

  } // GridChunkProducer constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the grid used to produce data chunks for this producer.
   *
   * @return the grid for this producer.
   */
  public Grid getGrid () { return (grid); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataType getExternalType() {

    return (protoChunk.getExternalType());

  } // getExternalType

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk getChunk (ChunkPosition pos) {

    int[] start = new int[] {(int) pos.start[0], (int) pos.start[1]};
    int[] length = new int[] {(int) pos.length[0], (int) pos.length[1]};
    Object data;
    synchronized (grid) {
      data = grid.getData (start, length);
    } // synchronized
    DataChunk chunk = DataChunkFactory.getInstance().create (data,
      grid.getUnsigned(), grid.getMissing(), packing);

    return (chunk);

  } // getChunk

  ////////////////////////////////////////////////////////////

  @Override
  public ChunkingScheme getNativeScheme() { return (scheme); }
  
  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk getPrototypeChunk() {

    return (protoChunk.blankCopy());

  } // getPrototypeChunk

  ////////////////////////////////////////////////////////////

} // GridChunkProducer class

////////////////////////////////////////////////////////////////////////

