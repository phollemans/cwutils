////////////////////////////////////////////////////////////////////////
/*
     FILE: CachedGrid.java
  PURPOSE: A subclass of Grid that employs caching.
   AUTHOR: Peter Hollemans
     DATE: 2002/06/17
  CHANGES: 2002/07/20, PFH, added navigation copy to constructor
           2002/07/22, PFH, modified getRows for performance improvement
           2002/10/21, PFH, added getValue(int,int)
           2002/10/31, PFH, modified for tile and read/write caching
           2002/11/12, PFH, modified protected constructor
           2002/11/14, PFH, added getDataStream
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/06/10, PFH, changed resetCache() to protected access
           2004/07/10, PFH, added setOptimizedCacheSize()
           2013/02/12, PFH, added getMaxTiles()

  CoastWatch Software Library and Utilities
  Copyright 2004-2013, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.util.*;
import java.util.List;
import java.text.*;
import java.io.*;
import java.awt.*;
import java.lang.reflect.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.*;

/**
 * The cached grid class is a grid that uses caching to reduce the
 * memory needed to read and write data values in a grid.  The cache
 * uses a similar strategy to how modern operating systems cache
 * memory pages.  A data stream is kept open, and a number of tiles
 * (rectangular sections of data) are brought into memory as they are
 * needed.  The number of tiles in memory (the cache size) and the
 * size of each tile may be set by the user.  Tiles that are no longer
 * needed are swapped out to make room for new tiles in the cache.  In
 * order to avoid excessive data I/O, a least-recently-used rule is
 * used to determine which tile to remove from the cache when the
 * cache reaches its maximum capacity.<p>
 *
 * The standard data variable set method is supported by keeping a
 * dirty flag for each tile.  If the tile has been written to, it is
 * kept in the cache until, upon removal, it is written to the data
 * stream.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class CachedGrid
  extends Grid {

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

    int[] tileDims = tiling.getTileDimensions();
    int newMaxTiles = cacheSize / getTileSize (tileDims, this);
    if (tileDims[COLS] * newMaxTiles < dims[COLS])
      newMaxTiles = (int) Math.ceil ((double) dims[COLS] / tileDims[COLS]);
    setMaxTiles (newMaxTiles);

  } // setOptimizedCacheSize

  ////////////////////////////////////////////////////////////

  /**
   * Gets the cache data stream as an object.  This is useful for
   * classes that read and write cached grids.
   */
  public abstract Object getDataStream();

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
   * Writes the specified tile.
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

    // Loop ever each tile
    // -------------------
//    Iterator iter =  cache.values().iterator();

    for (Tile tile : cache.values()) {
//    while (iter.hasNext()) {
//      Tile tile = (Tile) iter.next();
      if (tile.getDirty()) 
        writeTile (tile);      
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

      e.printStackTrace();
      
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

  public void setValue (
    int index,
    double val
  ) {

    setValue (index/dims[COLS], index%dims[COLS], val);

  } // setValue

  ////////////////////////////////////////////////////////////

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

  public double getValue (
    int index
  ) {

    return (getValue (index/dims[COLS], index%dims[COLS]));

  } // getValue

  ////////////////////////////////////////////////////////////

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

  public Object getData () { return (getData (new int[] {0, 0}, dims)); }

  ////////////////////////////////////////////////////////////

  public Object getData (
    int[] start,
    int[] count
  ) {

    // Check subset
    // ------------
    if (!checkSubset (start, count))
      throw new IndexOutOfBoundsException ("Invalid subset");

    // Find required tiles
    // -------------------
    int[] minCoords = tiling.createTilePosition(start[ROWS], 
      start[COLS]).getCoords();
    int[] maxCoords = tiling.createTilePosition(start[ROWS]+count[ROWS]-1,
      start[COLS]+count[COLS]-1).getCoords();
    List tiles = new ArrayList();
    for (int i = minCoords[ROWS]; i <= maxCoords[ROWS]; i++)
      for (int j = minCoords[COLS]; j <= maxCoords[COLS]; j++)
        tiles.add (tiling.new TilePosition (i, j));

    // Create subset array
    // -------------------
    Object subset = Array.newInstance (getDataClass(), 
      count[ROWS]*count[COLS]);
    Rectangle subsetRect = new Rectangle (start[COLS], start[ROWS], 
      count[COLS], count[ROWS]);

    // Loop over each tile
    // -------------------
    Iterator iter = tiles.iterator();
    while (iter.hasNext()) {
      TilePosition pos = (TilePosition) iter.next();

      // Get tile
      // --------
      if (!cache.containsKey (pos)) cacheMiss (pos);
      Tile tile = (Tile) cache.get (pos);
      int[] thisTileDims = tile.getDimensions();
      Object tileData = tile.getData();

      // Get tile intersection
      // ---------------------
      Rectangle tileRect = tile.getRectangle();
      Rectangle intersect = subsetRect.intersection (tileRect);

      // Map tile data into subset data
      // ------------------------------
      for (int i = 0; i < intersect.height; i++) {
        System.arraycopy (
          tileData, (intersect.y-tileRect.y+i)*thisTileDims[COLS] + 
          (intersect.x-tileRect.x),
          subset, (intersect.y-subsetRect.y+i)*count[COLS] + 
          (intersect.x-subsetRect.x), intersect.width);
      } // for

    } // while
 
    return (subset);

  } // getData

  ////////////////////////////////////////////////////////////

} // CachedGrid class

////////////////////////////////////////////////////////////////////////
