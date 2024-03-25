////////////////////////////////////////////////////////////////////////
/*

     File: ChunkComputation.java
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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkOperation;
import noaa.coastwatch.util.chunk.ChunkCollector;
import noaa.coastwatch.util.chunk.ChunkConsumer;
import noaa.coastwatch.util.chunk.ChunkFunction;

import noaa.coastwatch.test.TimeAccumulator;

/**
 * The <code>ChunkComputation</code> class can be used to perform a computation
 * on a set of input data chunks using a function and a list of chunk
 * producers/consumers. The computation is an operation broken down into a
 * set of calls to the {@link #perform} method that performs the computation
 * on each chunk position.  To help with testing, the computation can optionally
 * track the time used by chunk collecting, processing, and consuming.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ChunkComputation implements ChunkOperation {

  private static final Logger LOGGER = Logger.getLogger (ChunkComputation.class.getName());

  /** The collector used as a source of chunks. */
  private ChunkCollector collector;

  /** The consumer to push chunks to. */
  private ChunkConsumer consumer;

  /** The many-to-one chunk function to compute. */
  private ChunkFunction function;

  /** The time tracking flag, true to track processing time. */
  private boolean isTracked;

  /** The time accumulator for chunk collecting. */
  private TimeAccumulator collectorTime;

  /** The time accumulator for chunk computation. */
  private TimeAccumulator functionTime;

  /** The time accumulator for chunk consuming. */
  private TimeAccumulator consumerTime;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new chunk computation from the specified components.
   *
   * @param collector the collector to use for source data.
   * @param consumer the consumer to push processed data to.
   * @param function the function to use for processing the source chunk data.
   */
  public ChunkComputation (
    ChunkCollector collector,
    ChunkConsumer consumer,
    ChunkFunction function
  ) {

    this.collector = collector;
    this.consumer = consumer;
    this.function = function;
  
  } // ChunkComputation constructor
  
  ////////////////////////////////////////////////////////////

  /**
   * Sets the tracking flag for this computation.
   *
   * @param isTracked the tracking flag, true to track the chunk computation
   * and collector/consumer times or false to not. By default the
   * computation timing is not tracked.
   */
  public void setTracked (
    boolean isTracked
  ) {

    this.isTracked = isTracked;
    if (isTracked) {
      collectorTime = new TimeAccumulator();
      functionTime = new TimeAccumulator();
      consumerTime = new TimeAccumulator();
    } // if

  } // setTracked
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the time tracking data for the computation.
   *
   * @return the map of tracked quantity to time in seconds.
   */
  public Map<String, Double> getTrackingData () {

    Map<String, Double> data = new LinkedHashMap<>();
    data.put ("collector", collectorTime.totalSeconds());
    data.put ("function", functionTime.totalSeconds());
    data.put ("consumer", consumerTime.totalSeconds());

    return (data);

  } // getTrackingData

  ////////////////////////////////////////////////////////////

  /**
   * Estimates the memory requirements for processing a chunk of 
   * data at the specified position.
   * 
   * @param pos the position to estimate the memory usage.
   * 
   * @return the memory in bytes.
   * 
   * @since 3.8.1
   */
  public long getMemory (ChunkPosition pos) {

    long mem = 0;

    // Add up the memory for the chunks that will be delivered by the various
    // chunk producers.  We assume that the data structures underlying the 
    // producers also hold onto a copy of at least one chunk of data in the 
    // native chunking scheme.
    int values = pos.getValues();
    int chunkCount = 0;
    long producerMem = 0;
    for (var producer : collector) {

      var proto = producer.getPrototypeChunk();
      producerMem += proto.valueBytes() * values;

      var nativeScheme = producer.getNativeScheme();
      var nativePos = nativeScheme.getPosition (pos.start);
      int nativeValues = nativePos.getValues();
      producerMem += proto.valueBytes() * nativeValues;

      chunkCount++;

    } // for
    mem += producerMem;
    LOGGER.fine ("Memory used by chunk producers approx " + (producerMem/1024) + " kb");

    // Add up the memory for the chunks that will be pushed to the consumer.
    var consumerMem = consumer.getPrototypeChunk().valueBytes() * values;
    mem += consumerMem;
    LOGGER.fine ("Memory used by chunk consumer approx " + (consumerMem/1024) + " kb");

    // Finally, add the memory needed inside the function.
    var functionMem = function.getMemory (pos, chunkCount);
    mem += functionMem;
    LOGGER.fine ("Memory used by function approx " + (functionMem/1024) + " kb");

    return (mem);

  } // getMemory

  ////////////////////////////////////////////////////////////

  @Override
  public void perform (ChunkPosition pos) {

    if (isTracked) {

      TimeAccumulator acc = new TimeAccumulator();
      acc.start();
      List<DataChunk> chunks = collector.getChunks (pos);
      acc.end();
      collectorTime.add (acc);

      acc.reset();
      acc.start();
      DataChunk result = function.apply (pos, chunks);
      acc.end();
      functionTime.add (acc);

      if (result != null) {
        acc.reset();
        acc.start();
        consumer.putChunk (pos, result);
        acc.end();
        consumerTime.add (acc);
      } // if

    } // if

    else {
      List<DataChunk> chunks = collector.getChunks (pos);
      DataChunk result = function.apply (pos, chunks);
      if (result != null) consumer.putChunk (pos, result);
    } // else

    LOGGER.fine ("Finished computation at pos = " + pos);

  } // perform

  ////////////////////////////////////////////////////////////

} // ChunkComputation class

////////////////////////////////////////////////////////////////////////
