////////////////////////////////////////////////////////////////////////
/*

     File: StallMonitor.java
   Author: Peter Hollemans
     Date: 2003/10/26

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
package noaa.coastwatch.io;

// Imports
// -------
import noaa.coastwatch.io.DataTransferAdapter;
import noaa.coastwatch.io.DataTransferEvent;
import noaa.coastwatch.net.Timeout;

/**
 * A stall monitor is a data transfer listener that helps to handle
 * stalled transfers.  When a stall period has expired and no new
 * transfer progress has been made, the stall monitor calls a
 * user-specified method.
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public class StallMonitor
  extends DataTransferAdapter {

  // Variables
  // ---------
  /** The timeout for measuring transfer stalls. */
  private Timeout timeout;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new stall monitor with the specified stall time.
   *
   * @param stallTime the number of milliseconds of transfer stall to
   * wait before running the action.
   * @param stallAction the runnable to invoke when a stall occurs.
   */
  public StallMonitor (
    int stallTime,
    Runnable stallAction
  ) {

    // Create timeout
    // --------------
    timeout = new Timeout (stallTime, stallAction);

  } // StallMonitor

  ////////////////////////////////////////////////////////////

  public void transferStarted (DataTransferEvent event) {

    // Start stall timeout
    // -------------------
    timeout.start();

  } // transferStarted

  ////////////////////////////////////////////////////////////

  public void transferProgress (DataTransferEvent event) {

    // Reset timeout
    // -------------
    timeout.reset();

  } // transferProgress

  ////////////////////////////////////////////////////////////

  public void transferEnded (DataTransferEvent event) {

    // Cancel timeout
    // --------------
    timeout.cancel();

  } // transferEnded

  ////////////////////////////////////////////////////////////

  public void transferError (DataTransferEvent event) { 

    // Cancel timeout
    // --------------
    timeout.cancel();

  } // transferError

  ////////////////////////////////////////////////////////////

} // StallMonitor class

////////////////////////////////////////////////////////////////////////
