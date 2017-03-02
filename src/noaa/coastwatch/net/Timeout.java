////////////////////////////////////////////////////////////////////////
/*

     File: Timeout.java
   Author: Peter Hollemans
     Date: 2003/09/03

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.net;

// Imports
// -------
import java.util.Timer;
import java.util.TimerTask;

/**
 * A timeout allows the user to perform a certain task after some amount
 * of time has passed.  A timeout may be used for example to take action
 * if a network stall occurs.<p>
 * 
 * When a timeout object is created, it is given a task to run and a
 * length of time after which to run the task.  However, the timeout
 * is created in an inactive state.  To start the timeout countdown,
 * the start() method must be called.  Once the timeout is started,
 * the timeout may be cancelled using the cancel() method, or reset
 * via reset().  The reset() method effectively resets the timeout as
 * if cancel() and then start() had been called, but does so in a way
 * that does not create any new objects or start any new threads, and
 * is thus safe to call frequently if needed, rather than calling
 * cancel() followed by start().
 *
 * @author Peter Hollemans
 * @since 3.1.5
 */
public class Timeout {

  // Constants
  // ---------
  /** The delay between time checks. */
  private final static int DELAY = 200;

  // Variables
  // ---------
  /** The timeout length. */
  private int length;
  
  /** The elapsed time so far. */
  private int elapsed;

  /** The timeout timer. */
  private Timer timer;

  /** The timeout task. */
  private Runnable task;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new timeout.
   *
   * @param length the timeout length in milliseconds.
   * @param task the runnable task to perform when the timeout occurs.
   */
  public Timeout (
    int length,
    Runnable task
  ) {

    // Initialize variables
    // --------------------
    this.length = length;
    this.task = task;
    timer = null; 

  } // Timeout constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the elapsed time back to zero and continues the timeout.
   */
  public synchronized void reset () { elapsed = 0; }

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the elapsed time and starts the timeout countdown. If the
   * countdown has already started and the cancel method has not been
   * called, this method has no effect.
   */
  public void start () {

    // Get a new timer
    // ---------------
    if (timer != null) return;
    timer = new Timer();

    // Schedule timer task
    // -------------------
    elapsed = 0;
    timer.scheduleAtFixedRate (new TimerTask () {
      public void run () { performIncrement(); }
    }, DELAY, DELAY);

  } // start

  ////////////////////////////////////////////////////////////

  /** 
   * Performs an increment of the elapsed time.  If the elapsed time
   * is greater then the timeout length, the timer is cancelled and
   * the timeout runnable task is called.
   */
  private synchronized void performIncrement () {

    // Check for active timer
    // ----------------------
    if (timer == null) return;

    // Increment elapsed time
    // ----------------------
    elapsed += DELAY;

    // Check for timeout
    // -----------------
    if (elapsed > length) {
      cancel();
      task.run();
    } // if

  } // performIncrement

  ////////////////////////////////////////////////////////////

  /** 
   * Cancels the timeout.  The timeout is stopped and the start method
   * must be called again.
   */
  public void cancel () {

    // Cancel the timer and discard
    // ----------------------------
    if (timer == null) return;
    timer.cancel();
    timer = null;

  } // cancel

  ////////////////////////////////////////////////////////////

} // Timeout class

////////////////////////////////////////////////////////////////////////
