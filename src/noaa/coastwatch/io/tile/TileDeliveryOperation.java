////////////////////////////////////////////////////////////////////////
/*
     FILE: TileDeliveryOperation.java
   AUTHOR: Peter Hollemans
     DATE: 2014/07/01
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io.tile;

// Imports
// -------
import java.util.*;
import java.io.*;
import noaa.coastwatch.io.tile.TilingScheme.*;

/**
 * A <code>TileDeliveryOperation</code> represents an asynchronous process for
 * delivering tiles of data from a {@link TileSource} to a number of 
 * <code>Observer</code> objects.  Each observer receives a {@link TilingScheme.Tile}
 * object after each tile has been read from the source.  If an exception is
 * encountered while delivering tiles, the observer object will be null, and 
 * the {@link getLastReadException} method will return the latest exception
 * received.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class TileDeliveryOperation
  extends Observable {

  // Variables
  // ---------
  
  /** The source to be reading tile data from. */
  private TileSource source;
  
  /** The positions that tile data should be read from. */
  private Iterable<TilePosition> positions;
  
  /** The cancelled flag, true if this operation has been cancelled. */
  private boolean isCancelled;
  
  /** The started flag, true if this operation has been started. */
  private boolean isStarted;

  /** The last exception from a failed attempt to read a tile from the source. */
  private IOException lastReadException;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the source for this delivery operation.
   *
   * @return the tile source.
   */
  public TileSource getSource() { return (source); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the last exception received from reading tiles from the source.
   *
   * @return the last exception, or null if no exception has occurred.
   */
  public IOException getLastReadException() { return (lastReadException); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new delivery operation that reads tiles from a source.  The
   * operation is created but not started.
   *
   * @param source the source to read tile data from.
   * @param positions the positions to read.
   *
   * @see #start
   */
  public TileDeliveryOperation (
    TileSource source,
    Iterable<TilePosition> positions
  ) {
  
    this.source = source;
    this.positions = positions;
    isStarted = false;
    isCancelled = false;
  
  } // TileDeliveryOperation constructor

  ////////////////////////////////////////////////////////////

  /**
   * Starts the operation.  Tiles will begin to be read in a parallel
   * thread, and observers notified when tiles are ready.  This method 
   * returns immediately, and may only be called once.
   */
  public void start() {

    // Check if already started
    // ------------------------
    if (!isStarted) {

      // Create worker thread
      // --------------------
      Thread worker = new Thread() {
        public void run() {
          for (TilePosition pos : positions) {
        
            // Read tile
            // ---------
            Tile tile = null;
            try { tile = source.readTile (pos); }
            catch (IOException e) { lastReadException = e; }
            setChanged();
            notifyObservers (tile);
            
            // Check if cancelled
            // ------------------
            synchronized (TileDeliveryOperation.this) {
              if (isCancelled) break;
            } // synchronized
        
          } // for
        }};
    
      // Start worker
      // ------------
      isStarted = true;
      worker.start();

    } // if
  
  } // start

  ////////////////////////////////////////////////////////////

  /**
   * Cancels this operation.  This method may only be called if
   * the {@link #start} method has been called.
   */
  public synchronized void cancel() {
  
    if (isStarted) isCancelled = true;
  
  } // cancel

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    System.out.print ("Testing method ... ");


    assert (true);

    
    System.out.println ("OK");
  
  } // main

  ////////////////////////////////////////////////////////////

} // TileDeliveryOperation class

////////////////////////////////////////////////////////////////////////

