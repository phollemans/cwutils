////////////////////////////////////////////////////////////////////////
/*

     File: CachedGrid.java
   Author: Peter Hollemans
     Date: 2002/06/17

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.Grid;

// Testing
import noaa.coastwatch.test.TestLogger;

import java.util.logging.Logger;

/**
 * <p>A <code>CachedGrid</code> is a {@link Grid} that uses temporary caching
 * to reduce the overall memory footprint of gridded data.  The cache
 * uses a similar strategy to how modern operating systems cache
 * memory pages.  A data stream is kept open, and a number of tiles
 * (rectangular sections of data) are brought into memory as they are
 * needed.  The number of tiles in memory (the cache size) and the
 * size of each tile may be set by the user.  Tiles that are no longer
 * needed are swapped out to make room for new tiles in the cache.  In
 * order to avoid excessive data I/O, a least-recently-used rule is
 * used to determine which tile to remove from the cache when the
 * cache reaches its maximum capacity.</p>
 *
 * <p>The standard {@link noaa.coastwatch.util.DataVariable#setValue} method is
 * supported by keeping a dirty flag for each tile.  If the tile has been
 * written to, it is kept in the cache until, upon removal, it is written
 * to the data stream.</p>
 *
 * <p>The only methods that subclasses need to implement are {@link #readTile} to
 * retrieve tiles from the data source, {@link #writeTile} to update the
 * data with any changes made to tiles, and {@link #getDataStream} to retrieve
 * the object used to read and write data (only used to check for equality).</p>
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
@noaa.coastwatch.test.Testable
public abstract class CachedGrid
  extends Grid {

  private static final Logger LOGGER = Logger.getLogger (CachedGrid.class.getName());

  // Constants
  // ---------

  /** Default cache tiles. */
  public final static int DEFAULT_MAX_TILES = 8;

  /** Default tile dimensions. */
  public final static int DEFAULT_TILE_DIMS = 512;

  /** Read-only mode. */
  protected final static int READ_ONLY = 0;

  /** Read-write mode. */
  protected final static int READ_WRITE = 1;

  // Variables
  // ---------

  /** The cache of tile data. */
  private LinkedHashMap<TilePosition, Tile> cache;

  /** The last tile retrieved from the cache. */
  private Tile lastTile;

  /** The maximum number of tiles in the cache. */
  private int maxTiles;

  /** The tiling scheme. */
  protected TilingScheme tiling;

  /** The access mode. */
  protected int accessMode;

  // The helper used for tracking cache hits/misses and making dynamic adjustments.
  private CacheOptimizer optimizer;

  // The dynamic optimization flag, true if dynamic optimization has been requested.
  private boolean dynamic;

  // The maximum number of tiles requested before dynamic optimization.
  private int requestedMaxTiles;

  // An identifier string for this object.
  private String id;

  ////////////////////////////////////////////////////////////

  /** Gets an identifier for this object to help with debugging. */
  private String getID () { 

    if (id == null) id = getName() + "::" + Integer.toHexString (hashCode()); 
    return (id); 

  } // getID

  ////////////////////////////////////////////////////////////

  @Override
  public TilingScheme getTilingScheme () {
  
    return (tiling);
    
  } // getTilingScheme

  ////////////////////////////////////////////////////////////

  /**
   * Times an access to the cache at the specified coordinates.
   * 
   * @param row the data row to access.
   * @param col the data column to access.
   * 
   * @return the access time in nanoseconds (ns).
   */
  public long timeAccess (int row, int col) {

    var total = CacheOptimizer.timeOperation (() -> getValue (row, col));
    LOGGER.finer ("Access at (" + row + "," + col + ") required " + total + " ns");

    return (total);

  } // timeAccess

  ////////////////////////////////////////////////////////////

  /**
   * Tests the speed of the data access and caching mechanisms.  
   * 
   * @param fraction the fraction in the range [0..1] of the tiles to test.
   * 
   * @return the average [load, replace, access] times in nanoseconds.
   */
  private long[] testCacheSpeed (
    double fraction
  ) {

    // Start by setting the cache to one tile.  This way we 
    // can easily test the cache miss, cache hit, and tile replacement
    // times.
    int lastMaxTiles = maxTiles;   
    setMaxTiles (1);

    // Initialize times and counts for testing a random set of tile positions.
    var positionList = tiling.getAllPositions();
    int tilesTotal = positionList.size();
    int tilesToTest = Math.max (2, (int) Math.round (fraction*tilesTotal));
    int[] tileDims = tiling.getTileDimensions();

    long cacheLoadTime = 0;
    int cacheLoadCount = 0;

    long cacheReplaceTime = 0;
    int cacheReplaceCount = 0;

    long cacheAccessTime = 0;
    int cacheAccessCount = 0;

    LOGGER.finer ("Testing cache for " + getID() + " with [" + 
      tilesToTest + "/" + tilesTotal + "] " +
      tileDims[0] + "x" + tileDims[1] + " tiles");

    for (int tileIndex = 0; tileIndex < tilesToTest; tileIndex++) {

      if (positionList.size() == 0) break;

      // Choose a random tile to test and get its global start coordinates 
      // and dimensions.
      var pos = positionList.remove ((int) Math.round (Math.random()*(positionList.size()-1)));
      var start = pos.getStart();
      var dims = pos.getDimensions();

      LOGGER.finer ("Testing cache speed for " + getID() + " at " + pos);

      // If the cache currently has a tile in it, test the time to replace and 
      // access the new tile.
      if (cache.size() != 0) {
        cacheReplaceTime += timeAccess (start[ROW], start[COL]);
        cacheReplaceCount++;
      } // if

      // Now clear the cache and test the pure load time without any
      // tile replacement.
      clearCache();
      cacheLoadTime += timeAccess (start[ROW], start[COL]);
      cacheLoadCount++;

      // Finally for this tile, test the access time for a random selection
      // of locations.
      for (int i = 0; i < 10; i++) {
        int[] offset = new int[] { 
          (int) Math.round (Math.random()*(dims[ROWS]-1)),
          (int) Math.round (Math.random()*(dims[COLS]-1))
        };
        cacheAccessTime += timeAccess (start[ROW] + offset[ROW], start[COL] + offset[COL]);
        cacheAccessCount++;
      } // for

    } // for

    // Compute the average statistics and report.
    long cacheLoadAvg = cacheLoadTime/cacheLoadCount;
    long cacheReplaceAvg = cacheReplaceTime/cacheReplaceCount;
    long cacheAccessAvg = cacheAccessTime/cacheAccessCount;
    LOGGER.finer ("Cache for " + getID() + " load/replace/access times = " + 
      cacheLoadAvg + "/" + cacheReplaceAvg + "/" + cacheAccessAvg + " ns");

    // Restore the cache to its original settings.
    setMaxTiles (lastMaxTiles);

    return (new long[] {cacheLoadAvg, cacheReplaceAvg, cacheAccessAvg});

  } // testCacheSpeed

  ////////////////////////////////////////////////////////////

  /** Increases the cache size by some incremental amount. */
  private void growCache () {

    synchronized (cache) {
      int newMaxTiles = Math.min ((int) Math.round (maxTiles * 1.5), computeSpanningTiles());
      adjustMaxTiles (newMaxTiles);
    } // synchronized

  } // growCache

  ////////////////////////////////////////////////////////////

  /** Decreases the cache size by some incremental amount. */
  private void shrinkCache () {

    synchronized (cache) {
      int newMaxTiles = Math.max (maxTiles - 1, 1);
      adjustMaxTiles (newMaxTiles);
    } // synchronized

  } // shrinkCache

  ////////////////////////////////////////////////////////////

  /**
   * Turns on or off dynamic optimization mode.  In dynamic optimization,
   * the cache is monitored for access events and cache miss events and the
   * number of tiles in the cache is changed dynamically to maintain an
   * acceptable range of the cache miss rate.
   * 
   * @param flag the mode flag, true to perform dynamic optimization or
   * false to not.
   */
  public void setDynamic (boolean flag) {

    if (dynamic != flag) {

      // Turn on dynamic optimization here if it's not already running.
      if (flag) {
        dynamic = flag;
        requestedMaxTiles = maxTiles;
        LOGGER.fine ("Dynamic cache enabled for " + getID() + " with initial max " + 
          maxTiles + " tiles of size " + Arrays.toString (tiling.getTileDimensions()));
      } // if

      // Turn off dynamic optimization and return to normal operations.
      else {

        // TODO: We would need to turn off the optimizer here.  The issue
        // is that it might be in the middle of an optimization task, so
        // we would need to handle that.  For now we just throw an error.
        if (optimizer != null) 
          throw new UnsupportedOperationException ("Dynamic optimization stop not implemented while in use");

        dynamic = flag;
        maxTiles = requestedMaxTiles;
        LOGGER.fine ("Dynamic cache disabled for " + getID());
      } // else

    } // if

  } // setDynamic

  ////////////////////////////////////////////////////////////



  // TODO: This was implemented to help detect thrashing.  But what happens 
  // when the local set changes rapidly throughout the use of the data?  In
  // the case of CDAT using the data, the user controls the local set and if
  // zoomed in, this algorithm works quite well to limit the number of tiles
  // that are in the cache.  But in the case of cwrender, the local set changes
  // rapidly as the algorithm renders from top to bottom, so many tiles are
  // added to the cache unless we limit it to a diagonal line as we've done.
  // It seems that the page fault rate alone is not a good predictor of what
  // tiles the algorithm is using.



  /** Starts dynamic optimization. */
  private void startDynamic () {

    // Check if dynamic cache optimization is already being performed.
    if (optimizer != null) throw new IllegalStateException ("Cache optimization already being performed");

    // Test the speed of the current caching and compute a window of
    // acceptable cache miss rates.    
    dynamic = false;
    var speed = testCacheSpeed (0.1);
    dynamic = true;
    var mA = speed[2];
    var mLR = (speed[0] + speed[1]) / 2;
    double alphaMax = 1.20; // 20% slower
    double alphaMin = 1.05; // 5% slower
    double cacheMissRateMax = mA*(alphaMax-1) / mLR;
    double cacheMissRateMin = mA*(alphaMin-1) / mLR;
    LOGGER.finer ("Cache miss target range is [" + cacheMissRateMin + " .. " + cacheMissRateMax + "]");

    // Create a new cache optimizer that maintains the number of cache tiles
    // dynamically to guarantee the range of cache miss rates.
    optimizer = new CacheOptimizer (
      cacheMissRateMin, cacheMissRateMax, 
      () -> growCache(), () -> shrinkCache()
    );

  } // startDynamic

  ////////////////////////////////////////////////////////////

  /**
   * Computes an approximate number of tiles needed to span a diagonal line
   * through the tiling scheme.
   * 
   * @return the maximum number of tiles along a diagonal line in the range
   * of [1..tileCount] where tileCount is the number of tiles that cover the
   * entire tiling scheme.
   * 
   * @since 3.8.1
   */
  private int computeSpanningTiles () {

    int[] tileCounts = tiling.getTileCounts();
    int maxTileCount = Math.max (tileCounts[ROWS], tileCounts[COLS]);
    int spanningTiles = Math.min ((int) Math.ceil (maxTileCount * 1.5), tiling.getTileCount());

    return (spanningTiles);

  } // computeSpanningTiles

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache with the specified size.  The cache size is
   * optimized so that the cache can contain a set of tiles which span
   * at least the entire width of the grid for more efficient caching.
   * Thus, the actual cache size may be slightly larger than that
   * requested.  
   * 
   * As an alternative to this method, consider using dynamic caching
   * instead by calling {@link #setDynamic}.
   *
   * @param cacheSize the cache memory size in bytes.
   *
   * @see #setCacheSize
   */
  public void setOptimizedCacheSize (
    int cacheSize
  ) {

    if (optimizer != null) throw new IllegalStateException ("Dynamic optimization being performed");

    // Compute tiles that fit into the cache
    // -------------------------------------
    int[] tileDims = tiling.getTileDimensions();
    int newMaxTiles = cacheSize / getTileSize (tileDims, this);

    // Compute tiles needed to span a diagonal line through the dataset
    // ----------------------------------------------------------------
    if (tileDims[ROWS] < dims[ROWS] && tileDims[COLS] < dims[COLS]) {
      int spanningTiles = computeSpanningTiles();
      if (newMaxTiles < spanningTiles) newMaxTiles = spanningTiles;
    } // if

    setMaxTiles (newMaxTiles);

    LOGGER.fine ("Optimized cache created for " + getID() + " with " + 
      maxTiles + " tiles of size " + Arrays.toString (tileDims));

  } // setOptimizedCacheSize

  ////////////////////////////////////////////////////////////

  /**
   * Gets the cache data stream as an object.  This is useful for
   * classes that read and write cached grids.
   *
   * @return the data stream used for reading and writing data.
   */
  public abstract Object getDataStream();

  ////////////////////////////////////////////////////////////

  @Override
  public void dispose () {
  
    clearCache();
    super.dispose();
  
  } // dispose

  ////////////////////////////////////////////////////////////

  /**
   * Clears the existing cache.  All tiles are removed and the cache set
   * to empty.  Use this method to release the memory used by the cache
   * when it won't be needed again.  The same notes in {@link #resetCache}
   * on using {@link #flush} apply here as well.
   *
   * @since 3.5.0
   */
  public void clearCache () {

    synchronized (cache) {
      cache.clear();
      lastTile = null;
    } // synchronized
  
  } // clearCache

  ////////////////////////////////////////////////////////////

  /**
   * Handles a cache entry just prior to removal by writing any data that
   * needs to be synchronized.
   * 
   * @param entry the cache entry to handle.
   * 
   * @since 3.8.1
   */
  private void handleCacheEntryRemoval (
    Map.Entry<TilePosition, Tile> entry
  ) {

    Tile tile = entry.getValue();

    // If the tile is dirty, try writing it.  If that fails somehow, we have
    // a real problem.
    if (tile.getDirty()) { 
      try { 
        LOGGER.finer ("Writing tile for " + getID() + " at " + entry.getKey() + " (cache entry removal)");
        writeTile (tile); 
      } // try
      catch (IOException e) {
        throw new RuntimeException ("Error writing tile for " + getID() + " at " + entry.getKey() + ": " + e.getMessage());
      } // catch
      if (tile.getDirty())
        throw new IllegalStateException ("Written tile has getDirty() == true");
    } // if

    // If the tile being removed is the last accessed tile, we need to null out
    // the last tile so that it's not accessed anymore.
    if (tile == lastTile) lastTile = null;

  } // handleCacheEntryRemoval

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the tile cache to be empty.  This method should be used
   * with caution, since it does not perform a <code>flush()</code>
   * prior to resetting the cache.  Any tiles whose contents have been
   * modified but not written will lose those modifications.
   * Therefore, users of read/write mode tiles should explicitly call
   * <code>flush()</code> before this method.
   */
  protected void resetCache () {

    // This actually performs the same action as the clearCache()
    // method now, since we moved the cache creation to the constructor.
    // We did this so that we can now use the cache object reliably for 
    // serialization since it doesn't get reassigned.
    clearCache();

  } // resetCache

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache with the specified size.
   *
   * @param cacheSize the cache memory size in bytes.
   *
   * @see #setMaxTiles
   */
  public void setCacheSize (
    int cacheSize
  ) {

    if (optimizer != null) throw new IllegalStateException ("Dynamic optimization being performed");

    setMaxTiles (cacheSize / getTileSize (tiling.getTileDimensions(), this));

  } // setCacheSize

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current maximum number of tiles allowed in the cache.
   *
   * @return the maximum tiles allowed.
   */
  public int getMaxTiles () { return (maxTiles); }

  ////////////////////////////////////////////////////////////

  /**
   * Adjusts the maximum number of tiles while preserving the tiles
   * in the cache.
   * 
   * @param tiles the new maximum number of tiles.
   * 
   * @since 3.8.1
   */
  private void adjustMaxTiles (
    int tiles
  ) {

    // The minimum number of tiles in the cache should be 1.
    if (tiles < 1) tiles = 1;

    // If the caller is asking for more tiles in the cache, we just
    // increase the maximum tiles allowed.
    if (tiles > maxTiles) {
      var percent = String.format ("%.1f %%", ((float) tiles) / tiling.getTileCount() * 100);
      LOGGER.finer ("Increasing cache size for " + getID() + " from " + maxTiles + " tiles to " + tiles + " tiles (" + percent + ")");
      maxTiles = tiles;
    } // if

    // If the caller is asking for less tiles in the cache and some tiles
    // need to be removed, we remove each one and handle the cache entry
    // in case there are dirty tiles to write.
    else if (tiles < maxTiles) {
      var percent = String.format ("%.1f %%", ((float) tiles) / tiling.getTileCount() * 100);
      LOGGER.finer ("Decreasing cache size for " + getID() + " from " + maxTiles + " tiles to " + tiles + " tiles (" + percent + ")");
      if (cache.size() > tiles) {
        var iter = cache.entrySet().iterator();
        while (cache.size() > tiles) {
          var entry = iter.next();
          handleCacheEntryRemoval (entry);
          iter.remove();
        } // while
      } // if
      maxTiles = tiles;
    } // else if

  } // adjustMaxTiles

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache with a maximum number of tiles.
   *
   * @param tiles the maximum number of tiles.
   *
   * @see #setCacheSize
   */
  public void setMaxTiles (
    int tiles
  ) {

    if (optimizer != null) throw new IllegalStateException ("Dynamic optimization being performed");

    // Check bounds
    // ------------
    if (tiles < 1) tiles = 1;

    // Create new cache
    // ----------------
    maxTiles = tiles;
    resetCache();

  } // setMaxTiles

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new cache where each tile has the specified size.
   *
   * @param tileSize the tile memory size in bytes.
   */
  public void setTileSize (
    int tileSize
  ) {

    if (optimizer != null) throw new IllegalStateException ("Dynamic optimization being performed");

    setTileDims (getTileDims (tileSize, this));

  } // setTileSize

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache where each tile has the specified dimensions.
   *
   * @param tileDims the tile dimensions as [rows, columns].
   */
  public void setTileDims (
    int[] tileDims
  ) {

    if (optimizer != null) throw new IllegalStateException ("Dynamic optimization being performed");

    tiling = new TilingScheme (dims, tileDims);
    setMaxTiles (maxTiles);

  } // setTileDims

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new cached grid with the specified properties.  The
   * cache is created with a default size and maximum number of tiles.
   *
   * @param grid the grid to use for attributes.
   * @param accessMode the access mode, either <code>READ_ONLY</code>
   * or <code>READ_WRITE</code>.
   */
  protected CachedGrid (
    Grid grid,
    int accessMode
  ) {
    
    // Initialize the superclass with data from the specified grid.  Set the
    // access mode (not used here, but used in subclasses).
    super (grid);
    this.accessMode = accessMode;

    // We create a new cache here and make it ordered by last access, ie:
    // from least recently accessed to most recently.  That way we can use
    // it as a LRU cache for the tiles.
    cache = new LinkedHashMap<TilePosition, Tile> (maxTiles+1, .75f, true) {
      public boolean removeEldestEntry (Map.Entry<TilePosition, Tile> eldest) {
        boolean answer = false;
        if (size() > maxTiles) {
          handleCacheEntryRemoval (eldest);
          LOGGER.finer ("Removing cached tile for " + getID() + " at " + eldest.getKey());
          answer = true;
        } // if 
        return (answer);
      } // removeEldestEntry
    }; 

    // Set the initial maximum tiles and tile dimensions to the default 
    // values.
    maxTiles = DEFAULT_MAX_TILES;
    setTileDims (new int[] {DEFAULT_TILE_DIMS, DEFAULT_TILE_DIMS});

  } // CachedGrid constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Calculates the tile memory size based on dimensions.
   *
   * @param tileDims the tile dimensions as [rows, columns].
   * @param grid the grid for calculation.
   * 
   * @return the memory size used by each tile in bytes.
   */
  public static int getTileSize (
    int[] tileDims,
    Grid grid
  ) {

    // Calculate tile size
    // -------------------
    int values = tileDims[ROWS]*tileDims[COLS];
    int bytes = (values*DataVariable.getClassBits (grid.getDataClass())) / 8;

    return (bytes);

  } // getTileSize

  ////////////////////////////////////////////////////////////

  /** 
   * Calculates the tile dimensions required to fill the specified
   * memory.
   *
   * @param tileSize the tile memory size in bytes.
   * @param grid the grid for calculation.
   * 
   * @return the dimensions of the largest tile that fills the
   * memory without being larger than the grid dimensions.
   */
  public static int[] getTileDims (
    int tileSize, 
    Grid grid
  ) {

    // Calculate tile dimensions
    // -------------------------
    int values = (tileSize*8) / 
      DataVariable.getClassBits (grid.getDataClass());
    int tileDim = (int) Math.sqrt (values);
    if (tileDim < 1) tileDim = 1;
    int[] tileDims = new int[] {tileDim, tileDim};

    // Check tile dimensions
    // ---------------------
    int[] dims = grid.getDimensions();
    if (tileDims[ROWS] > dims[ROWS]) tileDims[ROWS] = dims[ROWS];
    if (tileDims[COLS] > dims[COLS]) tileDims[COLS] = dims[COLS];

    return (tileDims);

  } // getTileDims

  ////////////////////////////////////////////////////////////

  /**
   * Reads the specified tile.
   *
   * @param pos the tile position to read.
   *
   * @return the tile at the specified position.
   *
   * @throws IOException if an error occurred reading the tile data.
   */
  protected abstract Tile readTile (
    TilePosition pos
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Writes the specified tile.  The implementation of this method must set
   * the tile to be not dirty upon successfully writing it, or throw an error.
   *
   * @param tile the tile to write.
   *
   * @throws IOException if an error occurred writing the tile data.
   */
  protected abstract void writeTile (
    Tile tile
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Writes any unwritten tiles to the data stream.
   *
   * @throws IOException if an error occurred writing the tile data.
   */
  public void flush () throws IOException {

    synchronized (cache) {

      // For any tiles currently in the cache that have a dirty flag on,
      // ie: they've been modified but not written, we write the tiles
      // here.
      for (Tile tile : cache.values()) {
        if (tile.getDirty()) {
          LOGGER.fine ("Writing tile for " + getID() + " at " + tile.getPosition() + " (cache flush)");
          writeTile (tile);
          if (tile.getDirty())
            throw new IllegalStateException ("Written tile has getDirty() == true");
        } // if
      } // for

    } // synchronized

  } // flush

  ////////////////////////////////////////////////////////////

  /**
   * Handles a cache miss.  The specified tile is read into the cache.
   *
   * @param pos the tile position to read.
   */
  private void cacheMiss (
    TilePosition pos
  ) {

    Tile tile;
    try { tile = readTile (pos); }
    catch (IOException e) {
      throw new RuntimeException (e.getMessage());
    } // catch
    cache.put (pos, tile);

    LOGGER.finer ("Cache miss occurred for " + getID() + " at " + pos + ", " + 
      cache.size() + " tiles now cached");

  } // cacheMiss

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the tile for the specified coordinates.
   * 
   * @param row the data row coordinate.
   * @param col the data column coordinate.
   * 
   * @return the tile that contains the specified coordinates.
   */
  private Tile getTile (
    int row,
    int col
  ) {

    boolean isCacheMiss = false;
    Tile tile;

    synchronized (cache) {

      // If we're accessing a tile for the first time with dynamic optimization
      // turned on, we may need to start the optimizer here.  Delaying this 
      // helps to prevent the optimization of data that is not actually being
      // used.
      if (dynamic && optimizer == null) {
        startDynamic();
      } // if

      // Check if the tile needed is the last one accessed.  In that case,
      // we can speed things up a bit by not having to go to the cache.
      if (lastTile != null && lastTile.contains (row, col))
        tile = lastTile;

      // Otherwise we have to actually go to the cache, and we may find that
      // the tile isn't available.  In that case, we need to perform a cache
      // miss and get the tile into the cache.
      else {
        TilePosition pos = tiling.getTilePositionForCoords (row, col);
        if (!cache.containsKey (pos)) {
          cacheMiss (pos);
          isCacheMiss = true;
        } // if
        tile = cache.get (pos);
        lastTile = tile;
      } // else

    } // synchronized

    // Finally before returning the tile, inform the optimizer if needed
    // that we just accessed a tile, and possibly that we had a cache miss
    // event.
    if (optimizer != null) {
      optimizer.access();
      if (isCacheMiss) optimizer.miss();
    } // if

    return (tile);

  } // getTile

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the tile at the specified tile position.
   * 
   * @param pos the tile position.
   * 
   * @return the tile at the specified position.
   * 
   * @since 3.8.1
   */
  private Tile getTile (
    TilePosition pos
  ) {

    int[] start = pos.getStart();
    return (getTile (start[ROW], start[COL]));

  } // getTile

  ////////////////////////////////////////////////////////////

  @Override
  public void setValue (
    int index,
    double val
  ) {

    setValue (index/dims[COLS], index%dims[COLS], val);

  } // setValue

  ////////////////////////////////////////////////////////////

  @Override
  public void setValue (
    int row,
    int col,
    double val
  ) {

    if (row < 0 || row > dims[ROWS]-1 || col < 0 || col > dims[COLS]-1)
      return;

    Tile tile = getTile (row, col);
    tile.setDirty (true);
    setValue (tile.getIndex (row, col), tile.getData(), val);

  } // setValue

  ////////////////////////////////////////////////////////////

  @Override
  public double getValue (
    int index
  ) {

    return (getValue (index/dims[COLS], index%dims[COLS]));

  } // getValue

  ////////////////////////////////////////////////////////////

  @Override
  public double getValue (
    int row,
    int col
  ) {

    if (row < 0 || row > dims[ROWS]-1 || col < 0 || col > dims[COLS]-1)
      return (Double.NaN);

    Tile tile = getTile (row, col);
    double value = getValue (tile.getIndex (row, col), tile.getData());

    return (value);

  } // getValue

  ////////////////////////////////////////////////////////////

  @Override
  public void setData (Object data) { setData (data, new int[] {0, 0}, dims); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getData () { return (getData (new int[] {0, 0}, dims)); }

  ////////////////////////////////////////////////////////////

  @Override 
  public void setAccessType (
    AccessType type
  ) {

    if (type == AccessType.FULL_TILE) {
      LOGGER.fine ("Configuring " + getID() + " for full tile access type");
      setDynamic (false);
      setMaxTiles (1);
    } // if

  } // setAccessType

  ////////////////////////////////////////////////////////////

  @Override
  public void setData (
    Object subset,
    int[] start,
    int[] count
  ) {

    // Get list of tile positions
    // --------------------------
    List<TilePosition> tilePositions = tiling.getCoveringPositions (start, count);

    // Loop over each tile position
    // ----------------------------
    Rectangle subsetRect = new Rectangle (start[COLS], start[ROWS], count[COLS], count[ROWS]);
    for (TilePosition pos : tilePositions) {
    
      // Get tile
      // --------
      Tile tile = getTile (pos);
      int[] thisTileDims = tile.getDimensions();
      Object tileData = tile.getData();

      // Check for degenerate case -- subset is exactly one tile
      // -------------------------------------------------------
      Rectangle tileRect = tile.getRectangle();
      if (tileRect.equals (subsetRect)) {
        System.arraycopy (subset, 0, tileData, 0, thisTileDims[ROWS]*thisTileDims[COLS]);
      } // if
      
      // Map subset data into tile data
      // ------------------------------
      else {
        Rectangle intersect = subsetRect.intersection (tileRect);
        for (int i = 0; i < intersect.height; i++) {
          System.arraycopy (
            subset,
            (intersect.y-subsetRect.y+i)*count[COLS] + (intersect.x-subsetRect.x),
            tileData,
            (intersect.y-tileRect.y+i)*thisTileDims[COLS] + (intersect.x-tileRect.x),
            intersect.width);
        } // for
      } // else

      // Mark tile as needing to be written
      // ----------------------------------
      tile.setDirty (true);

    } // for
 
  } // setData

  ////////////////////////////////////////////////////////////

  @Override
  public Object getData (
    int[] start,
    int[] count
  ) {

    // Get list of tile positions
    // --------------------------
    List<TilePosition> tilePositions = tiling.getCoveringPositions (start, count);

    // Create subset array
    // -------------------
    Object subset = Array.newInstance (getDataClass(), count[ROWS]*count[COLS]);
    Rectangle subsetRect = new Rectangle (start[COLS], start[ROWS], count[COLS], count[ROWS]);

    // Loop over each tile
    // -------------------
    for (TilePosition pos : tilePositions) {
    
      // Get tile
      // --------
      Tile tile = getTile (pos);
      int[] thisTileDims = tile.getDimensions();
      Object tileData = tile.getData();

      // Check for degenerate case -- subset is exactly one tile
      // -------------------------------------------------------
      Rectangle tileRect = tile.getRectangle();
      if (tileRect.equals (subsetRect)) {
        System.arraycopy (tileData, 0, subset, 0, thisTileDims[ROWS]*thisTileDims[COLS]);
      } // if
      
      // Map tile data into subset data
      // ------------------------------
      else {
        Rectangle intersect = subsetRect.intersection (tileRect);
        for (int i = 0; i < intersect.height; i++) {
          System.arraycopy (
            tileData,
            (intersect.y-tileRect.y+i)*thisTileDims[COLS] + (intersect.x-tileRect.x),
            subset,
            (intersect.y-subsetRect.y+i)*count[COLS] + (intersect.x-subsetRect.x),
            intersect.width);
        } // for
      } // else

    } // for
 
    return (subset);

  } // getData

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (CachedGrid.class);

    // ------------------------->

    logger.test ("Framework");

    int[] testDims = new int[] {2000, 2000};
    int[] testData = new int[testDims[ROWS]*testDims[COLS]];
    for (int i = 0; i < testDims[ROWS]; i++) {
      for (int j = 0; j < testDims[COLS]; j++) {
        testData[i*testDims[COLS] + j] = i*testDims[COLS] + j;
      } // for
    } // for
    for (int index = 1; index < testData.length; index++) {
      assert (testData[index] != testData[index-1]);
    } // for

    Grid grid = new Grid (
      "test",
      "test data",
      "meters",
      testDims[ROWS],
      testDims[COLS],
      new int[0],
      new java.text.DecimalFormat ("000"),
      null,
      null);
    CachedGrid cached = new CachedGrid (grid, READ_WRITE) {

      protected Tile readTile (
        TilePosition pos
      ) throws IOException {

        int[] tileDims = pos.getDimensions();
        int[] tileStart = pos.getStart();
        int tileValues = tileDims[ROWS]*tileDims[COLS];
        int[] tileData = new int[tileValues];
        Grid.arraycopy (testData, testDims, tileStart, tileData, tileDims, new int[] {0, 0}, tileDims);
        return (tiling.new Tile (pos, tileData));

      } // readTile

      protected void writeTile (
        Tile tile
      ) throws IOException {
      
        int[] tileDims = tile.getDimensions();
        int[] tileStart = tile.getPosition().getStart();
        Object tileData = tile.getData();
        Grid.arraycopy (tileData, tileDims, new int[] {0, 0}, testData, testDims, tileStart, tileDims);
        tile.setDirty (false);
        
      } // writeTile

      public Object getDataStream() { return (null); }

    };
    int[] tileSize = new int[] {100, 100};
    cached.setTileDims (tileSize);

    logger.passed();

    // ------------------------->

    logger.test ("getData");

    List<int[]> startList = new ArrayList<>();
    List<int[]> countList = new ArrayList<>();

    startList.add (new int[] {30, 250});
    countList.add (new int[] {150, 90});

    startList.add (new int[] {tileSize[ROWS]*2, tileSize[COLS]*4});
    countList.add ((int[]) tileSize.clone());
    
    startList.add (new int[] {70, 130});
    countList.add (new int[] {200, 150});
    
    for (int testIndex = 0; testIndex < startList.size(); testIndex++) {
    
      int[] start = startList.get (testIndex);
      int[] count = countList.get (testIndex);

      int[] readTile = (int[]) cached.getData (start, count);

      for (int i = 0; i < count[ROWS]; i++) {
        for (int j = 0; j < count[COLS]; j++) {
          int globalRow = i+start[ROWS];
          int globalCol = j+start[COLS];
          assert (readTile[i*count[COLS] + j] == testData[globalRow*testDims[COLS] + globalCol]);
        } // for
      } // for

    } // for

    logger.passed();

    // ------------------------->

    logger.test ("setData");
    
    int[] savedTestData = (int[]) testData.clone();
    for (int testIndex = 0; testIndex < startList.size(); testIndex++) {

      int[] start = startList.get (testIndex);
      int[] count = countList.get (testIndex);

      int[] writeTile = new int[count[ROWS]*count[COLS]];

      for (int i = 0; i < count[ROWS]; i++) {
        for (int j = 0; j < count[COLS]; j++) {
          writeTile[i*count[COLS] + j] = (i*count[COLS] + j) + testData.length;
        } // for
      } // for

      cached.setData (writeTile, start, count);
      cached.flush();
    
      for (int i = 0; i < testDims[ROWS]; i++) {
        for (int j = 0; j < testDims[COLS]; j++) {
          int tileRow = i-start[ROWS];
          int tileCol = j-start[COLS];
          if (
            tileRow < 0 || tileRow > count[ROWS]-1 ||
            tileCol < 0 || tileCol > count[COLS]-1
          ) {
            assert (testData[i*testDims[COLS] + j] == (i*testDims[COLS] + j));
          } // if
          else {
            assert (testData[i*testDims[COLS] + j] == writeTile[tileRow*count[COLS] + tileCol]);
          } // else
        } // for
      } // for

      cached.setData (savedTestData);
      cached.flush();

    } // for

    logger.passed();

    // ------------------------->

  } // main

  ////////////////////////////////////////////////////////////

} // CachedGrid class

////////////////////////////////////////////////////////////////////////
