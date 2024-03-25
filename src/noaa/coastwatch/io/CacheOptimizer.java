/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.io;

import java.util.Timer;
import java.util.TimerTask;
import java.lang.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

import java.util.logging.Logger;

/**
 * <p>The <code>CacheOptimizer</code> class is a helper for caches that
 * monitors cache access and miss rates and optimizes the cache size.  The
 * algorithm tries to maintain the cache miss rate (ie: page fault rate)
 * within an acceptable window specified by the caller.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class CacheOptimizer {

  private static final Logger LOGGER = Logger.getLogger (CacheOptimizer.class.getName());

  // Constants

  // The check timer period in milliseconds.
  private int TIMER_PERIOD = 200;

  // The inactive timer period maximum.
  private int INACTIVE_PERIOD_MAX = 5;

  // Variables
  
  // The bean used for timing cache operations.
  static private ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

  // The counter for the number of accesses to the cache.
  private int accessCount;

  // The counter for the number of misses in the cache.
  private int missCount;

  // The minimum allowed rate for cache misses.
  private double minMissRate;

  // The maximum allowed rate for cache misses.
  private double maxMissRate;

  // The task to run for increasing the cache size when the miss rate is
  // above the threshold.
  private Runnable growTask;

  // The task to run for decreasing the cache size when the miss rate is
  // below the threshold.
  private Runnable shrinkTask;

  // The timer used to check on cache usage.
  private Timer timer;

  // THe number timer periods that had no activity (ie: no accesses).
  private int inactivePeriodCount;

  ////////////////////////////////////////////////////////////

  /**
   * Times the execution of an operation that uses the cache to nanosecond
   * precision.
   * 
   * @param operation the operation to time.
   * 
   * @return the operation time in nanoseconds (ns).
   */
  static public long timeOperation (Runnable operation) {

    var startTime = mxBean.getCurrentThreadCpuTime();
    operation.run();
    var endTime = mxBean.getCurrentThreadCpuTime();
    long total = (endTime - startTime);

    return (total);

  } // timeOperation

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache optimizer.
   * 
   * @param minMissRate the minimum desired cache miss rate in the range [0..1].
   * @param maxMissRate the maximum desired cache miss rate in the range [0..1].
   * @param growTask the runnable to call to grow the cache size.
   * @param shrinkTask the runnable to call to shrink the cache size.
   */
  public CacheOptimizer (
    double minMissRate,
    double maxMissRate,
    Runnable growTask,
    Runnable shrinkTask
  ) {

    this.minMissRate = minMissRate;
    this.maxMissRate = maxMissRate;
    this.growTask = growTask;
    this.shrinkTask = shrinkTask;

  } // CacheOptimizer

  ////////////////////////////////////////////////////////////

  /** Starts the cache optimization timer. */
  private synchronized void start () {

    LOGGER.finer ("Starting cache optimization");

    timer = new Timer (true);
    var task = new TimerTask() { 
      public void run() { checkMissRate(); }
    };
    inactivePeriodCount = 0;
    timer.schedule (task, 0, TIMER_PERIOD);

  } // start

  ////////////////////////////////////////////////////////////

  /** Stops the cache optimization timer. */
  private synchronized void stop () {

    LOGGER.finer ("Stopping cache optimization");

    timer.cancel();
    timer = null;
    inactivePeriodCount = 0;

  } // stop

  ////////////////////////////////////////////////////////////

  /** Signals that the cache had an access (either hit or miss). */ 
  public synchronized void access () { 

    if (timer == null) start();
    accessCount++; 

  } // access

  ////////////////////////////////////////////////////////////

  /** Signals that the cache had a miss. */
  public synchronized void miss () { 

    if (timer == null) start();
    missCount++; 

  } // miss

  ////////////////////////////////////////////////////////////

  /** Checks the cache miss rate and changes the cache size if needed. */
  private synchronized void checkMissRate () {

    LOGGER.finer ("Cache access / miss = " + accessCount + " / " + missCount);
    if (accessCount > 0) {
      if (accessCount > 25) {
        double missRate = (double) missCount / accessCount;
        LOGGER.finer ("Cache miss rate = " + missRate);
        if (missRate < minMissRate) shrinkTask.run();
        else if (missRate > maxMissRate) growTask.run();
      } // if 
      inactivePeriodCount = 0;
    } // if
    else {
      inactivePeriodCount++;
    } // else

    accessCount = 0;
    missCount = 0;

    if (inactivePeriodCount > INACTIVE_PERIOD_MAX) stop();

  } // checkMissRate

  ////////////////////////////////////////////////////////////

} // CacheOptimizer class

