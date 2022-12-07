////////////////////////////////////////////////////////////////////////
/*

     File: ResamplingOperation.java
   Author: Peter Hollemans
     Date: 2019/02/04

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
// --------
import java.util.List;
import java.util.logging.Logger;

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.ChunkConsumer;
import noaa.coastwatch.util.chunk.ChunkOperation;
import noaa.coastwatch.util.chunk.ChunkResampler;
import noaa.coastwatch.util.ResamplingMapFactory;
import noaa.coastwatch.util.ResamplingMap;

/**
 * The <code>ResamplingOperation</code> class performs a data resampling
 * operation between pairs of {@link ChunkProducer} instances and
 * {@link ChunkConsumer} instances.  A {@link ResamplingMapFactory} is used
 * to create {@link ResamplingMap} instances for a given chunk in the
 * consumers' chunking scheme.  Consumers are all assumed to have the same
 * chunking scheme, so that resampling can be performed on each chunk in the
 * consumers using the same resampling map.  If a given position in the consumer
 * scheme has no valid resampling map, no operation is performed.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class ResamplingOperation implements ChunkOperation {

  private static final Logger LOGGER = Logger.getLogger (ResamplingOperation.class.getName());

  /** The list of producers. */
  private List<ChunkProducer> producerList;

  /** The list of consumers. */
  private List<ChunkConsumer> consumerList;

  /** The factory for creating resampling maps. */
  private ResamplingMapFactory mapFactory;
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new chunk computation from the specified components.
   *
   * @param producerList the list of producers that supply chunks to remap.
   * @param consumerList the list of corresponding consumers that handle the
   * remapped chunks, must be the same length as the producer list.
   * @param mapFactory the factory that creates instances of maps for coordinate
   * mapping.
   *
   * @throws IllegalStateException if the consumer and producer lists have
   * different sizes.
   */
  public ResamplingOperation (
    List<ChunkProducer> producerList,
    List<ChunkConsumer> consumerList,
    ResamplingMapFactory mapFactory
  ) {

    if (consumerList.size() != producerList.size())
      throw new IllegalStateException ("Consumer and producer lists have different sizes");

    this.producerList = producerList;
    this.consumerList = consumerList;
    this.mapFactory = mapFactory;

  } // ResamplingOperation const
  
  ////////////////////////////////////////////////////////////

  @Override
  public void perform (ChunkPosition pos) {

    ResamplingMap map = mapFactory.create (pos.start, pos.length);

    if (map != null) {
      ChunkResampler resampler = new ChunkResampler (map);
      int count = producerList.size();
      for (int i = 0; i < count; i++) {
        resampler.resample (producerList.get (i), consumerList.get (i), pos);
      } // for

      LOGGER.fine ("Finished resampling at pos = " + pos);

    } // if
    
  } // perform

  ////////////////////////////////////////////////////////////

} // ResamplingOperation class

////////////////////////////////////////////////////////////////////////

