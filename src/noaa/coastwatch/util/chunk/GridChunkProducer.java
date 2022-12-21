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

import java.util.logging.Logger;

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

  private static final Logger LOGGER = Logger.getLogger (GridChunkProducer.class.getName());

  // Variables
  // ---------

  /** The grid to use as a source of data. */
  protected Grid grid;

  /** The packing scheme for new chunks. */
  protected PackingScheme packing;

  /** The scaling scheme for new chunks. */
  protected ScalingScheme scaling;

  /** The chunking scheme used or null for none. */
  protected ChunkingScheme scheme;

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

    // Create packing or scaling scheme
    // --------------------------------
    double[] scalingArray = grid.getScaling();

    // We previously assumed that an identity scaling array meant that the data 
    // needs no scaling or packing scheme.  But this causes integer data chunks 
    // to have a different external type than the scaling array, which causes
    // problems in expression parsing and preserving the correct missing 
    // values. 
//    if (scalingArray != null && !(scalingArray[0] == 1 && scalingArray[1] == 0)) {

    if (scalingArray != null) {
      boolean unity = (scalingArray[0] == 1 && scalingArray[1] == 0);
      var dataClass = grid.getDataClass();
      if (dataClass.equals (Float.TYPE) && !unity)
        scaling = new FloatScalingScheme ((float) scalingArray[0], (float) scalingArray[1]);
      else if (dataClass.equals (Double.TYPE) && !unity)
        scaling = new DoubleScalingScheme (scalingArray[0], scalingArray[1]);
      else
        packing = new DoublePackingScheme (scalingArray[0], scalingArray[1]);
    } // if

    // Create the prototype chunk
    // --------------------------
    Object data = Array.newInstance (grid.getDataClass(), 0);
    protoChunk = DataChunkFactory.getInstance().create (data,
      grid.getUnsigned(), grid.getMissing(), packing, scaling);

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

    Object data;
    synchronized (grid) {
      data = grid.getData (pos.start, pos.length);
    } // synchronized
    DataChunk chunk = DataChunkFactory.getInstance().create (data,
      grid.getUnsigned(), grid.getMissing(), packing, scaling);

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

