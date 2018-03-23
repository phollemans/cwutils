////////////////////////////////////////////////////////////////////////
/*

     File: ChunkCollector.java
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
import java.util.ArrayList;
import java.util.stream.Collectors;

import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkPosition;

/**
 * The <code>ChunkCollector</code> class assembles chunks from a list of
 * producers and allows them to be accessed in one operation.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ChunkCollector {

  // Variables
  // ---------

  /** The list of chunk producers. */
  private List<ChunkProducer> producerList = new ArrayList<>();

  ////////////////////////////////////////////////////////////

  /**
   * Adds a producer to the list of chunks to assemble.
   *
   * @param producer the producer to add to the list.
   */
  public void addProducer (ChunkProducer producer) { producerList.add (producer); }

  ////////////////////////////////////////////////////////////

  /**
   * Assembles a set of chunks from the producers.
   *
   * @param pos the chunk position to get.
   *
   * @return the list of chunks assembled.
   */
  public List<DataChunk> getChunks (ChunkPosition pos) {

    List<DataChunk> chunks = producerList.stream()
      .map (producer -> producer.getChunk (pos))
      .collect (Collectors.toList());

    return (chunks);
  
  } // getChunks

  ////////////////////////////////////////////////////////////

} // ChunkCollector class

////////////////////////////////////////////////////////////////////////
