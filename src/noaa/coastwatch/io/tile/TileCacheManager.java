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

  // Constants
  // ---------
  
  /** The default tile maximum cache size property (specified in Mb). */
  public static final String DEFAULT_MAX_CACHE_SIZE_PROP = "cw.cache.size";

  // Variables
  // ---------
  
  /** The singleton instance of the manager. */
  private static TileCacheManager instance;

  /** The maximum cache size in bytes. */
  private static int maxCacheSize;

  /** The cache used by this manager. */
  private TileCache cache;

  ////////////////////////////////////////////////////////////

  static {

    maxCacheSize = Integer.parseInt (System.getProperty (DEFAULT_MAX_CACHE_SIZE_PROP, "128"))*1024*1024;

  } // static
  
  ////////////////////////////////////////////////////////////
  
  /**
   * Gets the cache managed by this instance.
   *
   * @return the managed tile cache.
   */
  TileCache getCache() { return (cache); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the singleton instance of this class using the default cache tile
   * class.
   *
   * @return the singleton instance.
   */
  public static TileCacheManager getInstance () {
  
    if (instance == null) {
    
    // TODO: Can we have some way of having a different tile cache by
    // default?
    
      TileCache cache = new LRUTileCache();
      cache.setCacheSizeLimit (maxCacheSize);
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

    TileCacheKey key = new TileCacheKey (source, pos);
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
   * <code>Object</code> for each tile (synchronously, before this method 
   * exits).  For the remaining tiles that are only available
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
      TileCacheKey key = new TileCacheKey (source, pos);
      Tile tile = cache.get (key);
      if (tile != null)
        observer.update (null, tile);
      else
        uncachedPositions.add (pos);
    } // for
    
    // Request remaining tiles
    // -----------------------
    TileDeliveryOperation op = new TileDeliveryOperation (source, uncachedPositions);
    final Observer requestObserver = observer;
    op.addObserver (new Observer() {
      public void update (Observable o, Object arg) {
        TileDeliveryOperation op = (TileDeliveryOperation) o;
        Tile tile = (Tile) arg;
        if (tile == null) {
          // print error??
        } // if
        else {
          TileCacheKey key = new TileCacheKey (op.getSource(), tile.getPosition());
          cache.put (key, tile);
        } // else
        requestObserver.update (op, tile);
      } // update
    });
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

    List<TileCacheKey> keysToRemove = new ArrayList<TileCacheKey>();
    for (TileCacheKey key : cache.keySet()) {
      if (key.getSource() == source) keysToRemove.add (key);
    } // for
    for (TileCacheKey key : keysToRemove) cache.remove (key);

  } // removeTilesForSource

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    System.out.print ("Testing getInstance, getCache ... ");
    TileCacheManager manager = TileCacheManager.getInstance();
    assert (manager != null);
    assert (manager.getCache() != null);
    System.out.println ("OK");

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
     
    final int[] globalDims = new int[] {100, 200};
    int[] tileDims = new int[] {40, 40};
    final TilingScheme scheme = new TilingScheme (globalDims, tileDims);

    TileSource source = new TileSource () {
      public Tile readTile (
        TilePosition pos
      ) throws IOException {
        int[] tileDims = pos.getDimensions();
        int count = tileDims[0] * tileDims[1];
        byte[] data = new byte[count];
        Arrays.fill (data, (byte) pos.hashCode());
        Tile tile = scheme.new Tile (pos, data);
        return (tile);
      } // readtile
      public Class getDataClass() { return (Byte.TYPE); }
      public TilingScheme getScheme() { return (scheme); }
    };
    final TilePosition pos = scheme.new TilePosition (2, 3);

    System.out.print ("Testing getTile ... ");
    Tile tile = manager.getTile (source, pos);
    assert (tile.getPosition().equals (pos));
    final TileCache cache = manager.getCache();
    assert (cache.containsKey (new TileCacheKey (source, pos)));
    assert (cache.containsValue (tile));
    System.out.println ("OK");

    System.out.print ("Testing requestTiles ... ");
    int[] start = new int[] {0, 0};
    int[] length = globalDims;
    final int[] tilesObserved = new int[] {0};
    final TileDeliveryOperation[] deliveryOp = new TileDeliveryOperation[] {null};
    final boolean[] isCovered = new boolean[globalDims[0]*globalDims[1]];
    manager.requestTiles (source, start, length, new Observer() {
      public void update (Observable o, Object arg) {
        TileDeliveryOperation op = (TileDeliveryOperation) o;
        Tile tile = (Tile) arg;
        assert (tile != null);
        if (op == null) {
          assert (tile.getPosition().equals (pos));
        } // if
        else {
          deliveryOp[0] = op;
          assert (!tile.getPosition().equals (pos));
          assert (cache.containsKey (new TileCacheKey (op.getSource(), pos)));
          assert (cache.containsValue (tile));
        } // else
        tilesObserved[0]++;
        java.awt.Rectangle tileRect = tile.getRectangle();
        int isCoveredCountBefore = 0;
        for (boolean val : isCovered) if (val) isCoveredCountBefore++;
        int[] dims = tile.getScheme().getDimensions();
        for (int row = 0; row < tileRect.height; row++) {
          for (int col = 0; col < tileRect.width; col++) {
            isCovered[(row + tileRect.y)*globalDims[1] + (col + tileRect.x)] = true;
          } // for
        } // for
        int isCoveredCountAfter = 0;
        for (boolean val : isCovered) if (val) isCoveredCountAfter++;
        assert ((isCoveredCountAfter - isCoveredCountBefore) == tileRect.width*tileRect.height);
      } // update
    });
    while (deliveryOp[0] == null) Thread.currentThread().sleep (1000);
    deliveryOp[0].waitUntilFinished();
    int[] tileCounts = scheme.getTileCounts();
    int tileCount = tileCounts[0]*tileCounts[1];
    assert (tilesObserved[0] == tileCount);
    for (boolean val : isCovered) assert (val);
    System.out.println ("OK");

    System.out.print ("Testing removeTilesForSource ... ");
    assert (cache.size() == tileCount);
    assert (cache.getCacheSize() == globalDims[0]*globalDims[1]);
    manager.removeTilesForSource (source);
    assert (cache.size() == 0);
    assert (cache.getCacheSize() == 0);
    System.out.println ("OK");
  
  } // main

  ////////////////////////////////////////////////////////////

} // TileCacheManager class

////////////////////////////////////////////////////////////////////////

