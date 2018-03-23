////////////////////////////////////////////////////////////////////////
/*

     File: PoolProcessor.java
   Author: Peter Hollemans
     Date: 2017/12/05

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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * The <code>PoolProcessor</code> class is a <code>ParallelChunkOperation</code>
 * that operates using a pool of execution threads.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class PoolProcessor implements ParallelChunkOperation {

  // Variables
  // ---------

  /** The list of chunk positions to operate on. */
  private List<ChunkPosition> positions;

  /** The chunk operation to perform at each location. */
  private ChunkOperation op;

  /** The pool of execution threads to use for processing. */
  private ExecutorService pool;

  /** The list of futures to check for task completion or cancellation. */
  private List<Future<Void>> futureList;

  /** The number of completed tasks so far. */
  private int completedTasks;

  /** The maximum number of operations to run in parallel. */
  private int maxOperations = Runtime.getRuntime().availableProcessors();

  ////////////////////////////////////////////////////////////

  /**
   * Sets the maximum number of operations to run in parallel.  The default
   * is to use the number of available processors reported by the Java runtime.
   *
   * @param ops the maximum number of parallel operations.
   */
  public void setMaxOperations (
    int ops
  ) {

    this.maxOperations = ops;
  
  } // setMaxOperations
  
  ////////////////////////////////////////////////////////////

  /** Increments the number of complete tasks and shuts down when finished. */
  private synchronized void taskComplete() {
    completedTasks++;
    if (completedTasks == positions.size()) {
      pool.shutdown();
    } // if
  } // taskComplete

  ////////////////////////////////////////////////////////////

  /** Holds a unit of work in this parallel operation. */
  private class ChunkOperationTask implements Callable<Void> {
    private ChunkPosition pos;
    public ChunkOperationTask (ChunkPosition pos) { this.pos = pos; }
    public Void call () throws Exception {
      op.perform (pos);
      taskComplete();
      return (null);
    } // call
  } // ChunkOperationTask class

  ////////////////////////////////////////////////////////////

  @Override
  public void init (
    List<ChunkPosition> positions,
    ChunkOperation op
  ) {

    this.positions = positions;
    this.op = op;
  
  } // init

  ////////////////////////////////////////////////////////////

  @Override
  public void start() {

    completedTasks = 0;

    // Create and start execution pool
    // -------------------------------
    pool = Executors.newFixedThreadPool (maxOperations);
    futureList = new ArrayList<>();
    synchronized (futureList) {
      try {
        positions.forEach (pos -> {
          ChunkOperationTask task = new ChunkOperationTask (pos);
          futureList.add (pool.submit (task));
        });
      } // try
      catch (RejectedExecutionException e) { }
    } // synchronized

  } // start

  ////////////////////////////////////////////////////////////

  @Override
  public void cancel() {

    pool.shutdown();
    synchronized (futureList) {
      futureList.forEach (future -> future.cancel (false));
    } // synchronized

  } // cancel

  ////////////////////////////////////////////////////////////

  @Override
  public void waitForCompletion() {

    synchronized (futureList) {
      futureList.forEach (future -> {
        if (!future.isCancelled()) {
          try { future.get(); }
          catch (Exception e) { throw new RuntimeException (e); }
        } // if
      });
    } // synchronized

    pool.shutdown();

  } // waitForCompletion

  ////////////////////////////////////////////////////////////

} // PoolProcessor class

////////////////////////////////////////////////////////////////////////
