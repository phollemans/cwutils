/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.util.chunk;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import noaa.coastwatch.util.chunk.ChunkComputation;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.PoolProcessor;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>ChunkComputationHelper</code> class assists in running a chunk
 * computation using the greatest number of threads that can be used
 * given the current memory constraints.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class ChunkComputationHelper {

  private static final Logger LOGGER = Logger.getLogger (ChunkComputationHelper.class.getName());

  private static final int VM_MEMORY = 256;
  private static final int BPM = 1024*1024; // bytes per megabyte


  ////////////////////////////////////////////////////////////

  protected ChunkComputationHelper () { }

  ////////////////////////////////////////////////////////////

  public static ChunkComputationHelper getInstance () { return (new ChunkComputationHelper()); }

  ////////////////////////////////////////////////////////////

  /**
   * Runs a chunk computation either in serial or via a pool of threads.
   * 
   * @param op the chunk computation to perform.
   * @param scheme the chunking scheme to use for the the operation.
   * @param serial the serial flag, true to run the operation in serial.
   * @param maxOps the maximum number of threads to run the operation if
   * running in parallel mode.
   * @param verbose a verbose logger for printing informational messages.
   */
  public void run (
    ChunkComputation op,
    ChunkingScheme scheme,
    boolean serial,
    int maxOps,
    Logger verbose
  ) {

    // Debugging
    if (LOGGER.isLoggable (Level.FINE))
      op.setTracked (true);

    // Start by creating a list of all the chunk positions that need to
    // be processed.
    List<ChunkPosition> positions = new ArrayList<>();
    scheme.forEach (positions::add);

    // Compute an estimate of the memory needed to process each chunk
    // position.  Adjust the number of operations if needed based on 
    // available memory.
    var mem = op.getMemory (positions.get (0));
    verbose.info ("Estimated " + mem/BPM + " Mb per operation for data");
    long memNeeded = mem*(serial ? 1 : maxOps) + VM_MEMORY*BPM;
    long maxMemory = Runtime.getRuntime().maxMemory();
    if (memNeeded > maxMemory) {
      LOGGER.warning ("Estimated memory required per operation is " + mem/BPM + " Mb (data) + " + VM_MEMORY + " Mb (VM)");
      int newMaxOps = (int) ((maxMemory - VM_MEMORY*BPM) / mem);
      if (newMaxOps < 1) {
        throw new RuntimeException ("Estimated memory required exceeds available max of " + maxMemory/BPM + " Mb");
      } // if
      else {
        LOGGER.warning ("Adjusting max operations from " + maxOps + " to " + newMaxOps + " for available max of " + maxMemory/BPM + " Mb");
        maxOps = newMaxOps;
      } // else
    } // if

    int[] chunkSize = scheme.getChunkSize();
    verbose.info ("Processing " + positions.size() + " data chunks of size " + chunkSize[0] + "x" + chunkSize[1]);

    // Perform the computation in serial mode.
    if (serial) {
      positions.forEach (pos -> op.perform (pos));
    } // if

    // Perform the computation in parallel mode.
    else {
      PoolProcessor processor = new PoolProcessor();
      processor.init (positions, op);
      processor.setMaxOperations (maxOps);
      processor.start();
      processor.waitForCompletion();
    } // if

    // Debugging
    if (LOGGER.isLoggable (Level.FINE)) {
      StringBuilder types = new StringBuilder();
      StringBuilder times = new StringBuilder();
      op.getTrackingData().forEach ((type, time) -> {
        types.append ((types.length() == 0 ? "" : "/") + type);
        times.append ((times.length() == 0 ? "" : "/") + String.format ("%.3f", time));
      });
      LOGGER.fine ("Computation " + types + " = " + times + " s");
    } // if

  } // run

  ////////////////////////////////////////////////////////////

} // ChunkComputationHelper class
