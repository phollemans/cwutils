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
  private Map<TilePosition, Tile> cache;

  /** The last tile retrieved from the cache. */
  private Tile lastTile;

  /** The maximum number of tiles in the cache. */
  private int maxTiles;

  /** The tiling scheme. */
  protected TilingScheme tiling;

  /** The access mode. */
  protected int accessMode;

  ////////////////////////////////////////////////////////////

  @Override
  public TilingScheme getTilingScheme () {
  
    return (tiling);
    
  } // getTilingScheme

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache with the specified size.  The cache size is
   * optimized so that the cache can contain a set of tiles which span
   * at least the entire width of the grid for more efficient caching.
   * Thus, the actual cache size may be slightly larger than that
   * requested.
   *
   * @param cacheSize the cache memory size in bytes.
   *
   * @see #setCacheSize
   */
  public void setOptimizedCacheSize (
    int cacheSize
  ) {

    // Compute tiles that fit into the cache
    // -------------------------------------
    int[] tileDims = tiling.getTileDimensions();
    int newMaxTiles = cacheSize / getTileSize (tileDims, this);

    // Compute tiles needed to span a diagonal line through the dataset
    // ----------------------------------------------------------------
    if (tileDims[ROWS] < dims[ROWS] && tileDims[COLS] < dims[COLS]) {
      int[] tileCounts = tiling.getTileCounts();
      int maxTileCount = Math.max (tileCounts[ROWS], tileCounts[COLS]);
      int spanningTiles = (int) Math.ceil (maxTileCount * 1.5);
      if (newMaxTiles < spanningTiles) newMaxTiles = spanningTiles;
    } // if

    setMaxTiles (newMaxTiles);

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

  /**
   * Clears the existing cache.  All tiles are removed and the cache set
   * to empty.  Use this method to release the memory used by the cache
   * when it won't be needed again.  The same notes in {@link #resetCache}
   * on using {@link #flush} apply here as well.
   *
   * @since 3.5.0
   */
  public void clearCache () {

    cache.clear();
    lastTile = null;
  
  } // clearCache

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

    // Create new cache
    // ----------------
    cache = new LinkedHashMap<TilePosition, Tile> (maxTiles+1, .75f, true) {
      public boolean removeEldestEntry (Map.Entry<TilePosition, Tile> eldest) {
 
        // Do not remove tile
        // ------------------
        if (size() <= maxTiles) return (false);

        // Remove tile but check for dirty
        // -------------------------------
        else {
          Tile tile = eldest.getValue();
          if (tile.getDirty()) { 
            try { writeTile (tile); }
            catch (IOException e) {
              throw new RuntimeException (e.getMessage());
            } // catch
            if (tile.getDirty())
              throw new IllegalStateException ("Written tile has getDirty() == true");
          } // if
          return (true);
        } // else

      } // removeEldestEntry
    }; 

    // Reset last tile retrieved
    // -------------------------
    lastTile = null;

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
   * Creates a new cache with a maximum number of tiles.
   *
   * @param tiles the maximum number of tiles.
   *
   * @see #setCacheSize
   */
  public void setMaxTiles (
    int tiles
  ) {

    // Check bounds
    // ------------
    if (tiles < 1) tiles = 1;

    // Create new cache
    // ----------------
    maxTiles = tiles;
    resetCache();

    LOGGER.fine ("Set max tiles to " + maxTiles + " for " + getName());

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
    
    // Initialize
    // ----------
    super (grid);

    // Create new cache
    // ----------------
    maxTiles = DEFAULT_MAX_TILES;
    setTileDims (new int[] {DEFAULT_TILE_DIMS, DEFAULT_TILE_DIMS});
    this.accessMode = accessMode;

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

    // Loop ever each tile and write if dirty
    // --------------------------------------
    for (Tile tile : cache.values()) {
      if (tile.getDirty()) {
        writeTile (tile);
        if (tile.getDirty())
          throw new IllegalStateException ("Written tile has getDirty() == true");
      } // if
    } // while

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

//      e.printStackTrace();
      
      throw new RuntimeException (e.getMessage());
    } // catch
    cache.put (pos, tile);

  } // cacheMiss

  ////////////////////////////////////////////////////////////

  /** Gets the tile for the specified coordinates. */
  private Tile getTile (
    int row,
    int col
  ) {

    // Check last tile
    // ---------------
    Tile tile;
    if (lastTile != null && lastTile.contains (row, col))
      tile = lastTile;

    // Get tile from cache
    // -------------------
    else {

      // TODO: This is not a very good thing to do -- we are
      // creating a tiny object for every call to this method
      // and then most likely deleting it again, unless it get used in
      // tile creation, and then we're cloning it for the Tile itself,
      // and putting it into the cache as a key.  At least we're checking
      // above if the last accessed tile contains it so that sequential
      // access is likely to not create/destroy many TilePosition
      // objects.  Is there some way that we can avoid this?  Flyweight?
      
      TilePosition pos = tiling.createTilePosition (row, col);
      if (!cache.containsKey (pos)) cacheMiss (pos);
      tile = (Tile) cache.get (pos);
      lastTile = tile;
    } // else

    return (tile);

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
    return (getValue (tile.getIndex (row, col), tile.getData()));

  } // getValue

  ////////////////////////////////////////////////////////////

  @Override
  public void setData (Object data) { setData (data, new int[] {0, 0}, dims); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getData () { return (getData (new int[] {0, 0}, dims)); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets a minimal list of tile positions that cover a subset of this
   * grid.
   *
   * @param start the subset starting [row, column].
   * @param count the subset dimension [rows, columns].
   *
   * @return the list of tile positions.
   *
   * @throws IndexOutOfBoundsException if the subset falls outside the
   * grid dimensions.
   */
  public List<TilePosition> getCoveringPositions (
    int[] start,
    int[] count
  ) {

    // Check subset
    // ------------
    if (!checkSubset (start, count))
      throw new IndexOutOfBoundsException ("Invalid subset");

    // Find required tiles
    // -------------------
    int[] minCoords = tiling.createTilePosition (start[ROWS], start[COLS]).getCoords();
    int[] maxCoords = tiling.createTilePosition (start[ROWS]+count[ROWS]-1, start[COLS]+count[COLS]-1).getCoords();
    List<TilePosition> tilePositions = new ArrayList<>();
    for (int i = minCoords[ROWS]; i <= maxCoords[ROWS]; i++)
      for (int j = minCoords[COLS]; j <= maxCoords[COLS]; j++)
        tilePositions.add (tiling.new TilePosition (i, j));

    return (tilePositions);

  } // getCoveringPositions

  ////////////////////////////////////////////////////////////

  @Override
  public void setData (
    Object subset,
    int[] start,
    int[] count
  ) {

    // Get list of tile positions
    // --------------------------
    List<TilePosition> tilePositions = getCoveringPositions (start, count);

    // Loop over each tile position
    // ----------------------------
    Rectangle subsetRect = new Rectangle (start[COLS], start[ROWS], count[COLS], count[ROWS]);
    for (TilePosition pos : tilePositions) {
    
      // Get tile
      // --------
      if (!cache.containsKey (pos)) cacheMiss (pos);
      Tile tile = (Tile) cache.get (pos);
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
    List<TilePosition> tilePositions = getCoveringPositions (start, count);

    // Create subset array
    // -------------------
    Object subset = Array.newInstance (getDataClass(), count[ROWS]*count[COLS]);
    Rectangle subsetRect = new Rectangle (start[COLS], start[ROWS], count[COLS], count[ROWS]);

    // Loop over each tile
    // -------------------
    for (TilePosition pos : tilePositions) {
    
      // Get tile
      // --------
      if (!cache.containsKey (pos)) cacheMiss (pos);
      Tile tile = (Tile) cache.get (pos);
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
