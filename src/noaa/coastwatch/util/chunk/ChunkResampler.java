////////////////////////////////////////////////////////////////////////
/*

     File: ChunkResampler.java
   Author: Peter Hollemans
     Date: 2019/02/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2018 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.ChunkConsumer;
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.ChunkDataFlagger;
import noaa.coastwatch.util.ResamplingMap;

/**
 * The <code>ChunkResampler</code> class performs a resampling of 2D chunk
 * data from a source coordinate system to a destination.  Each resampler
 * objects holds onto a coordinate {@link ResamplingMap} and the map is used to
 * resample data chunks from a {@link ChunkProducer} into a
 * {@link ChunkConsumer}.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class ChunkResampler {

  // Constants
  // ---------

  /** The row and column indices in coordinate values. */
  private static int ROW = 0;
  private static int COL = 1;

  // Variables
  // ---------

  /** The resampling map used for translating coordiantes. */
  private ResamplingMap map;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new chunk resampler that uses the specified map for
   * converting 2D coordinates.
   *
   * @param map the map to use for converting 2D coordinates.
   */
  public ChunkResampler (
    ResamplingMap map
  ) {
  
    this.map = map;
  
  } // ChunkResampler const

  ////////////////////////////////////////////////////////////

  /**
   * Resamples chunk data from a producer to a consumer at the specified chunk
   * position.
   *
   * @param producer the producer to use for requesting chunk data.
   * @param consumer the consumer to push resampled chunk data to.
   * @param pos the position within the chunking scheme of the consumer
   * to create and push a resampled chunk.
   *
   * @throws IllegalStateException if either the consumer or producer have no
   * valid chunking scheme.
   */
  public void resample (
    ChunkProducer producer,
    ChunkConsumer consumer,
    ChunkPosition pos
  ) {

    // Create destination chunk
    // ------------------------
    ChunkingScheme destScheme = consumer.getNativeScheme();
    if (destScheme == null)
      throw new IllegalStateException ("No chunking scheme found for destination");
    int[] size = destScheme.getChunkSize();
    int values = size[ROW] * size[COL];
    DataChunk protoChunk = consumer.getPrototypeChunk();
    DataChunk destChunk = protoChunk.blankCopyWithValues (values);

    // Create array of source chunks
    // -----------------------------
    ChunkingScheme sourceScheme = producer.getNativeScheme();
    if (sourceScheme == null)
      throw new IllegalStateException ("No chunking scheme found for source");
    int sourceTotalChunks = sourceScheme.getTotalChunks();
    DataChunk[] sourceChunks = new DataChunk[sourceTotalChunks];
    ChunkPosition[] sourcePositions = new ChunkPosition[sourceTotalChunks];
    int[] sourceChunkSize = sourceScheme.getChunkSize();
    int sourceChunkCols = sourceScheme.getChunkCount (COL);

    // Create array to store missing values
    // ------------------------------------
    boolean[] isMissingArray = new boolean[destChunk.getValues()];
    boolean isMissingUsed = false;

    // Loop over each coordinate in destination chunk
    // ----------------------------------------------
    int[] destCoords = new int[2];
    int[] sourceCoords = new int[2];
    ChunkDataCopier copier = new ChunkDataCopier();
    int destIndex = 0;

    for (int i = 0; i < pos.length[ROW]; i++) {
      for (int j = 0; j < pos.length[COL]; j++) {

        // Map dest to source
        // ------------------
        destCoords[ROW] = i + pos.start[ROW];
        destCoords[COL] = j + pos.start[COL];
        boolean isValid = map.map (destCoords, sourceCoords);

        if (isValid) {

          // Get source chunk
          // ----------------
          int sourceChunkRow = sourceCoords[ROW]/sourceChunkSize[ROW];
          int sourceChunkCol = sourceCoords[COL]/sourceChunkSize[COL];
          int sourceChunkIndex = sourceChunkRow*sourceChunkCols + sourceChunkCol;
          DataChunk sourceChunk = sourceChunks[sourceChunkIndex];

          // Read source chunk into cache if needed
          // --------------------------------------
          if (sourceChunk == null) {
            ChunkPosition sourcePos = sourceScheme.getPosition (sourceCoords);
            sourceChunk = producer.getChunk (sourcePos);
            sourceChunks[sourceChunkIndex] = sourceChunk;
            sourcePositions[sourceChunkIndex] = sourcePos;
          } // if

          // Copy data value into destination
          // --------------------------------
          ChunkPosition sourcePos = sourcePositions[sourceChunkIndex];
          int sourceIndex =
            (sourceCoords[ROW] - sourcePos.start[ROW])*sourcePos.length[COL] +
            (sourceCoords[COL] - sourcePos.start[COL]);
          copier.copyValue (sourceChunk, sourceIndex, destChunk, destIndex);

        } // if

        // Flag value as missing
        // ---------------------
        else {
          isMissingArray[destIndex] = true;
          isMissingUsed = true;
        } //else

        // Increment index to next data value
        // ----------------------------------
        destIndex++;

      } // for
    } // for

    // Flag missing values in chunk
    // ----------------------------
    if (isMissingUsed) {
      ChunkDataFlagger flagger = new ChunkDataFlagger();
      flagger.setMissingData (isMissingArray);
      destChunk.accept (flagger);
    } // if

    // Push the new reampled chunk to the consumer
    // -------------------------------------------
    consumer.putChunk (pos, destChunk);

  } // resample

  ////////////////////////////////////////////////////////////

} // ChunkResampler class

////////////////////////////////////////////////////////////////////////
