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
  
  /** The finished flag, true if this operation has finished delivery. */
  private boolean isFinished;

  /** The last exception from a failed attempt to read a tile from the source. */
  private IOException lastReadException;

  /** The worker thread used to perform the asyncronous operation. */
  private Thread worker;
  
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
    isFinished = false;
  
  } // TileDeliveryOperation constructor

  ////////////////////////////////////////////////////////////

  /**
   * Starts the operation.  Tiles will begin to be read in a parallel
   * thread, and observers notified when tiles are ready.  This method 
   * returns immediately, and may only be called once.
   *
   * @see #waitUntilFinished
   * @see #cancel
   * @see #isFinished
   */
  public void start() {

    // Check if already started
    // ------------------------
    if (!isStarted) {

      // Create worker thread
      // --------------------
      worker = new Thread() {
        public void run() {

          // Deliver each tile
          // -----------------
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
          
          // Set the finished flag
          // ---------------------
          synchronized (TileDeliveryOperation.this) {
            isFinished = true;
          } // synchronized
          
        }};
    
      // Start worker
      // ------------
      isStarted = true;
      worker.start();

    } // if
  
  } // start

  ////////////////////////////////////////////////////////////

  /**
   * Waits for the operation to finish.  This method should only be
   * called to synchronize the operation, and will exit when the delivery
   * operation is complete.  If no operation is in progress, the method
   * exits immediately.
   *
   * @throws InterruptedException if the current thread is interrupted waiting
   * for the operation to finish.
   */
  public void waitUntilFinished() throws InterruptedException {
  
    if (worker != null) worker.join();
  
  } // waitUntilFinished

  ////////////////////////////////////////////////////////////

  /**
   * Cancels this operation.  This method may only be called if
   * the {@link #start} method has been called.
   */
  public synchronized void cancel() {
  
    if (isStarted && !isFinished) isCancelled = true;
  
  } // cancel

  ////////////////////////////////////////////////////////////

  /**
   * Checks if this operation is finished.
   *
   * @return the finished flag, true if finished (either because the
   * operation was cancelled, or ran out of tiles to deliver), or
   * false if running or not started yet.
   *
   * @see #cancel
   * @see #start
   */
  public synchronized boolean isFinished() {
  
    return (isFinished);
  
  } // isFinished

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    /*
     *     0    1    2    3    4
     *   +----+----+----+----+----+  \
     * 0 |    |    |    |    |    |  |  40
     *   |    |    |    |    |    |  |
     *   +----+----+----+----+----+  X
     * 1 |    |    |    |    |    |  |  40
     *   |    |    |    |    |    |  |
     *   +----+----+----+----+----+  X
     * 2 |    |    |    |    |    |  |  20
     *   |    |    |    |    |    |  /
     *
     *   \----X----X----X----X----/
     *     40   40   40   40   40
     */
     
    int[] globalDims = new int[] {100, 200};
    int[] tileDims = new int[] {40, 40};
    final TilingScheme scheme = new TilingScheme (globalDims, tileDims);

    final boolean[] throwError = new boolean[] {false};
    TileSource source = new TileSource () {
      public Tile readTile (
        TilePosition pos
      ) throws IOException {
        Tile tile = null;
        if (throwError[0]) { throw new IOException ("Tile read failed"); }
        else {
          int[] tileDims = pos.getDimensions();
          int count = tileDims[0] * tileDims[1];
          byte[] data = new byte[count];
          Arrays.fill (data, (byte) pos.hashCode());
          tile = scheme.new Tile (pos, data);
          return (tile);
        } // else
      } // readtile
      public Class getDataClass() { return (Byte.TYPE); }
      public TilingScheme getScheme() { return (scheme); }
    };

    final List<TilePosition> positions = new ArrayList<TilePosition>();
    positions.add (scheme.new TilePosition (0, 1));
    positions.add (scheme.new TilePosition (2, 3));
    positions.add (scheme.new TilePosition (1, 0));

    System.out.print ("Testing constructor, getSource ... ");
    TileDeliveryOperation op = new TileDeliveryOperation (source, positions);
    assert (op.getSource() == source);
    assert (op.getLastReadException() == null);
    System.out.println ("OK");

    System.out.print ("Testing start, waitUntilFinished, getLastReadException, isFinished ... ");

    final int[] tilesObserved = new int[] {0};
    op.addObserver (new Observer() {
      public void update (Observable o, Object arg) {
        assert (o instanceof TileDeliveryOperation);
        TileDeliveryOperation op = (TileDeliveryOperation) o;
        assert (op.getLastReadException() == null);
        assert (arg instanceof Tile);
        Tile tile = (Tile) arg;
        TilePosition pos = tile.getPosition();
        assert (pos.equals (positions.get (tilesObserved[0])));
        tilesObserved[0]++;
        byte[] data = (byte[]) tile.getData();
        int[] tileDims = pos.getDimensions();
        assert (data.length == tileDims[0]*tileDims[1]);
        assert (data[0] == (byte) pos.hashCode());
      } // update
    });
    op.start();
    op.waitUntilFinished();
    assert (tilesObserved[0] == positions.size());

    throwError[0] = true;
    tilesObserved[0] = 0;
    op = new TileDeliveryOperation (source, positions);
    op.addObserver (new Observer() {
      public void update (Observable o, Object arg) {
        assert (o instanceof TileDeliveryOperation);
        TileDeliveryOperation op = (TileDeliveryOperation) o;
        assert (op.getLastReadException() != null);
        assert (arg == null);
        tilesObserved[0]++;
      } // update
    });
    op.start();
    op.waitUntilFinished();
    assert (tilesObserved[0] == positions.size());
    assert (op.isFinished());
    
    System.out.println ("OK");

    System.out.print ("Testing cancel ... ");
    throwError[0] = false;
    tilesObserved[0] = 0;
    op = new TileDeliveryOperation (source, positions);
    op.addObserver (new Observer() {
      public void update (Observable o, Object arg) {
        TileDeliveryOperation op = (TileDeliveryOperation) o;
        tilesObserved[0]++;
        if (tilesObserved[0] == positions.size()-1) op.cancel();
      } // update
    });
    op.start();
    op.waitUntilFinished();
    assert (tilesObserved[0] == positions.size()-1);
    System.out.println ("OK");
  
  } // main

  ////////////////////////////////////////////////////////////

} // TileDeliveryOperation class

////////////////////////////////////////////////////////////////////////

