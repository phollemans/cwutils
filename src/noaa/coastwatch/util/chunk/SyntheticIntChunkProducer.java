////////////////////////////////////////////////////////////////////////
/*

     File: SyntheticIntChunkProducer.java
   Author: Peter Hollemans
     Date: 2018/03/16

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
import java.util.function.IntBinaryOperator;

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.DataChunk.DataType;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.DataChunkFactory;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

import java.util.logging.Logger;

/**
 * A <code>SyntheticIntChunkProducer</code> object creates 2D integer data
 * chunks whose values are specified by a functional interface method that
 * returns an integer value for each row and column in the chunk.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class SyntheticIntChunkProducer implements ChunkProducer {

  private static final Logger LOGGER = Logger.getLogger (SyntheticIntChunkProducer.class.getName());

  // Variables
  // ---------
  
  /** The chunking scheme for this producer. */
  private ChunkingScheme scheme;
  
  /** The prototypes chunk for this producer. */
  private DataChunk prototypeChunk;
  
  /** The function used to compute the value for each chunk data entry. */
  private IntBinaryOperator function;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new producer.
   *
   * @param scheme the chunking scheme to use for this producer.
   * @param function the integer function for this producer that maps
   * (row, column) to an int value.
   */
  public SyntheticIntChunkProducer (
    ChunkingScheme scheme,
    IntBinaryOperator function
  ) {
  
    this.scheme = scheme;
    this.function = function;
    prototypeChunk = DataChunkFactory.getInstance().create (new int[0],
      false, Integer.MIN_VALUE, null);
  
  } // SyntheticIntChunkProducer constructor

  ////////////////////////////////////////////////////////////

  @Override
  public DataType getExternalType() { return (prototypeChunk.getExternalType()); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk getChunk (ChunkPosition pos) {

    DataChunk chunk;
    
    int values = pos.length[ROW]*pos.length[COL];
    int[] data = new int[values];
    
    for (int i = 0; i < pos.length[ROW]; i++) {
      int row = pos.start[ROW] + i;
      for (int j = 0; j < pos.length[COL]; j++) {
        int col = pos.start[COL] + j;
        data[i*pos.length[COL] + j] = function.applyAsInt (row, col);
      } // for
    } // for

    chunk = DataChunkFactory.getInstance().create (data, false, Integer.MIN_VALUE, null);
    return (chunk);

  } // getChunk

  ////////////////////////////////////////////////////////////

  @Override
  public ChunkingScheme getNativeScheme() { return (scheme); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk getPrototypeChunk() { return (prototypeChunk); }

  ////////////////////////////////////////////////////////////

} // SyntheticIntChunkProducer class

////////////////////////////////////////////////////////////////////////
