////////////////////////////////////////////////////////////////////////
/*
     FILE: TileCacheManager.java
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
import org.javatuples.*;
import noaa.coastwatch.io.tile.TilingScheme.*;

/**
 * The <code>TileCacheManager</code> class provides convenient access to the 
 * default tile cache.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class TileCacheManager {

  // Variables
  // ---------
  
  /** The singleton instance of the manager. */
  private static TileCacheManager instance;
  
  /** The cache used by this manager. */
  private TileCache cache;

  ////////////////////////////////////////////////////////////

  static {
  
    // from environment variables:
    // set default cache class
    // set default cache size
    // set error stream
  
  
  } // static
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the singleton instance of this class.
   *
   * @return the singleton instance.
   */
  public static TileCacheManager getInstance () {
  
    if (instance == null) {
      TileCache cache = null; // do something else here
      // set cache size
      instance = new TileCacheManager (cache);
    } // if

   return (instance);
  
  } // getInstance
  
  ////////////////////////////////////////////////////////////
  
  /**
   * Creates a new manager backed by the specified cache.
   *
   * @param cache the cache to use for this manager.
   */
  protected TileCacheManager (
    TileCache cache
  ) {
  
    this.cache = cache;
    
  } // TileCacheManager constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a tile from the cache for the specified source and position.
   * If no tile is available in the cache with a matching source and
   * position, the tile is read from the source and cached for the next 
   * call to this method.
   *
   * @param source the source for the tile data.
   * @param pos the position of the tile in the scheme.
   *
   * @return the tile specified.
   *
   * @throws IOException if there was an error reading the tile from the 
   * source.
   */
  public Tile getTile (
    TileSource source,
    TilePosition pos
  ) throws IOException {

    Pair<TileSource, TilePosition> key = Pair.with (source, pos);
    Tile tile = cache.get (key);
    if (tile == null) {
      tile = source.readTile (pos);
      cache.put (key, tile);
    } // if
    
    return (tile);
  
  } // getTile

  ////////////////////////////////////////////////////////////

  /**
   * Starts a delivery operation for the specified tiles.  If any tiles are
   * not already in the cache, they are read asynchronously.
   *
   * @param source the tile source.
   * @param start the starting data coordinates as [row, column].
   * @param length the size of the data rectangle as [rows, columns].
   * @param observer the tile delivery observer to notify of tiles
   * becoming available.  If some tiles are available immediately with no
   * delay, <code>Observer.update (Observable, Object)</code> is called with
   * a null value for the <code>Observable</code>, and the tile for the 
   * <code>Object</code>.  For the remaining tiles that are only available
   * after a delay, <code>Observer.update (Observable, Object)</code> is called
   * with the {@link TileDeliveryOperation} as the Observable and the tile
   * for the <code>Object</code>.
   */
  public void requestTiles (
    TileSource source,
    int[] start,
    int[] length,
    Observer observer
  ) {
  
    // Create list of tile positions to request
    // ----------------------------------------
    TilingScheme scheme = source.getScheme();
    int[] startTilePos = scheme.createTilePosition (start[0], start[1]).getCoords();
    int[] endTilePos = scheme.createTilePosition (start[0]+length[0]-1,
      start[1]+length[1]-1).getCoords();
    List<TilePosition> positions = new ArrayList<TilePosition>();
    for (int row = startTilePos[0]; row <= endTilePos[0]; row++) {
      for (int col = startTilePos[1]; col <= endTilePos[1]; col++) {
        positions.add (scheme.new TilePosition (row, col));
      } // for
    } // for

    // Notify observer for tiles already available
    // -------------------------------------------
    List<TilePosition> uncachedPositions = new ArrayList<TilePosition>();
    for (TilePosition pos : positions) {
      Pair<TileSource, TilePosition> key = Pair.with (source, pos);
      Tile tile = cache.get (key);
      if (tile != null)
        observer.update (null, tile);
      else
        uncachedPositions.add (pos);
    } // for
    
    // Request remaining tiles
    // -----------------------
    TileDeliveryOperation op = new TileDeliveryOperation (source, uncachedPositions);
    op.addObserver (new Observer() {
      public void update (Observable o, Object arg) {
        Tile tile = (Tile) arg;
        if (arg == null) {
          // print error??
        } // if
        else {
          TileDeliveryOperation op = (TileDeliveryOperation) o;
          Pair<TileSource, TilePosition> key = Pair.with (op.getSource(), tile.getPosition());
          cache.put (key, tile);
        } // else
      } // update
    });
    op.addObserver (observer);
    op.start();
  
  } // requestTiles

  ////////////////////////////////////////////////////////////

  /**
   * Removes all the tiles in the cache from the specified source.
   *
   * @param source the tile source to remove all tiles.
   */
  public void removeTilesForSource (
    TileSource source
  ) {

    for (Pair<TileSource, TilePosition> key : cache.keySet()) {
      if (key.getValue0() == source) cache.remove (key);
    } // for
  
  } // removeTilesForSource

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    System.out.print ("Testing method ... ");


    assert (true);

    
    System.out.println ("OK");
  
  } // main

  ////////////////////////////////////////////////////////////

} // TileCacheManager class

////////////////////////////////////////////////////////////////////////

