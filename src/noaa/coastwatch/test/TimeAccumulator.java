////////////////////////////////////////////////////////////////////////
/*

     File: TimeAccumulator.java
   Author: Peter Hollemans
     Date: 2018/01/13

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
package noaa.coastwatch.test;

// Imports
// -------
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * A <code>TimeAccumulator</code> object can be used for precise accumulation
 * of time used in a thread for performance testing.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class TimeAccumulator {

  // Variables
  // ---------

  /** The bean used for getting thread CPU time. */
  private static ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

  /** The total number of nanoseconds accumulated so far. */
  private long total = 0;

  /** The start CPU time from the last call to start(). */
  private long startTime;

  ////////////////////////////////////////////////////////////

  /** Marks the start of a CPU time recording session. */
  public void start () {
    startTime = mxBean.getCurrentThreadCpuTime();
  } // start
  
  ////////////////////////////////////////////////////////////

  /**
   * Marks the end of a CPU time recording session and adds the
   * accumulated time to the total.
   */
  public void end () {
    long endTime = mxBean.getCurrentThreadCpuTime();
    total += (endTime - startTime);
  } // end
  
  ////////////////////////////////////////////////////////////

  /**
   * Adds the time from another accumulator to this one.  This operation
   * is thread safe.
   *
   * @param acc the accumulator with time to add to this one.
   */
  public synchronized void add (
    TimeAccumulator acc
  ) {

    total += acc.total;

  } // add

  ////////////////////////////////////////////////////////////

  /**
   * Gets the total time accumulated.
   *
   * @return the time accumulated in seconds.
   */
  public double totalSeconds () { return (total/1000000*1e-3); }
  
  ////////////////////////////////////////////////////////////

  /** Resets the accumulated time. */
  public void reset () { total = 0; }

  ////////////////////////////////////////////////////////////

} // TimeAccumulator class

////////////////////////////////////////////////////////////////////////
