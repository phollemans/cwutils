////////////////////////////////////////////////////////////////////////
/*
     FILE: StallMonitor.java
  PURPOSE: To monitor a data transfer for a stall.
   AUTHOR: Peter Hollemans
     DATE: 2003/10/26
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2003, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.util.*;
import noaa.coastwatch.net.*;

/**
 * A stall monitor is a data transfer listener that helps to handle
 * stalled transfers.  When a stall period has expired and no new
 * transfer progress has been made, the stall monitor calls a
 * user-specified method.
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
