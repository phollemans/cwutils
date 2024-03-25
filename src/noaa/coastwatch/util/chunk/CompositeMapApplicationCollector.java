/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.util.chunk;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkPosition;

import java.util.logging.Logger;

/**
 * The <code>CompositeMapApplicationCollector</code> class is a special type
 * of {@link ChunkCollector} optimized for use with a 
 * {@link CompositeMapApplicationFunction} object.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class CompositeMapApplicationCollector extends ChunkCollector {

  private static final Logger LOGGER = Logger.getLogger (CompositeMapApplicationCollector.class.getName());

  ////////////////////////////////////////////////////////////

  @Override
  public List<DataChunk> getChunks (ChunkPosition pos) {

    var chunks = new ArrayList<DataChunk>();

    // What we do here is get the coherent map data chunk and analyze it
    // to see which of the input chunks are actually needed.  Then we only
    // read those specific input chunks.
    var mapProducer = producerList.get (0);
    var mapChunk = mapProducer.getChunk (pos);
    chunks.add (mapChunk);
    short[] mapArray = (short[]) (mapChunk.getPrimitiveData());

    // Now create an array of all the possible short values that we could see 
    // in the map.  These will be used as objects in a set that we accumulate.
    // This helps to avoid the issue of creating millions of Short objects 
    // just to immediately discard most of them (flyweight pattern).
    int chunkCount = producerList.size() - 1;
    Short[] shortArray = new Short[chunkCount];
    for (int i = 0; i < chunkCount; i++) shortArray[i] = Short.valueOf ((short) i);

    // Use the short array of values to populate the set of values in the map
    // that are actually used.
    var chunkIndexSet = new HashSet<Short>();
    for (var i : mapArray) {
      if (i != -1) chunkIndexSet.add (shortArray[i]);
    } // for

    // Now read only the chunks that are used according to the set.  The chunks
    // that are not needed are added to the list as null values because
    // the map application function still expects to see the chunks in
    // their proper locations in the chunk list.
    LOGGER.fine ("Found " + chunkIndexSet.size() + " actual chunks required out of " + chunkCount + " possible chunk producers");
    for (int i = 0; i < chunkCount; i++) {
      if (chunkIndexSet.contains (shortArray[i]))
        chunks.add (producerList.get (i+1).getChunk (pos));
      else 
        chunks.add (null);
    } // for

    return (chunks);
  
  } // getChunks

  ////////////////////////////////////////////////////////////

} // ChunkCollector class

////////////////////////////////////////////////////////////////////////
