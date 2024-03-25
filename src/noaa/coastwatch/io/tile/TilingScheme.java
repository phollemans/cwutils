////////////////////////////////////////////////////////////////////////
/*

     File: TilingScheme.java
   Author: Peter Hollemans
     Date: 2002/11/06

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
package noaa.coastwatch.io.tile;

// Imports
// -------
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The tiling scheme class helps support the tiling of 2D data.  A
 * tiling scheme consists of a set of global row and column dimensions
 * along with a set of tile row and column dimensions.  The tiling
 * scheme contains the <code>TilePosition</code> and <code>Tile</code>
 * classes to aid in the manipulation of tile coordinates and data.  A
 * general tiling in 2D looks as follows:
 * <pre>
 *              
 *              tile column
 *               dimension            tile position coordinates
 *              &lt;---------&gt;         /
 *                                 /
 *            ^ +---------*-------/-*---+    ^
 *            | |         *      /  *   |    | 
 *   tile row | |         *     v   *   |    |
 *  dimension | |  [0,0]  *  [0,1]  * [0,2]  |
 *            | |         *         *   |    |
 *            | |         *         *   |    |
 *            v *************************    |
 *              |         *         *   |    | global row dimension
 *              |         *         *   |    |
 *              |  [1,0]  *  [1,1]  * [1,2]  |
 *              |         *         *   |    | 
 *              |         *         *   |    |
 *              *************************    |
 *              |         *         *   |    |
 * truncated --&gt;|  [2,0]  *  [2,1]  * [2,2]  |
 *   tile       +---------*---------*---+    v
 *
 *              &lt;-----------------------&gt;
 *                    global column
 *                      dimension  
 * </pre>
 * In the example above, the tile count in both dimensions is 3, even
 * though some tiles are truncated due to the tile dimensions.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
@noaa.coastwatch.test.Testable
public class TilingScheme {

  // Constants
  // ---------
  /** Index of rows dimension. */
  public final static int ROWS = 0;

  /** Index of columns dimension. */
  public final static int COLS = 1;

  // Variables
  // ---------

  /** The global dimensions as [rows, columns]. */
  private int[] dims;

  /** The dimensions of each data tile as [rows, columns]. */
  private int[] tileDims;

  /** The tile count in each dimension as [rows, columns]. */
  private int[] tileCounts;

  /** The cache of all tile positions. */
  private Map<Integer, TilePosition> positionCache;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new tiling scheme from the specified dimensions.
   * 
   * @param dims the global dimensions as [rows, columns].
   * @param tileDims the tile dimensions as [rows, columns].
   */
  public TilingScheme (
    int[] dims,
    int[] tileDims
  ) {

    // Initialize
    // ----------
    this.dims = (int[]) dims.clone();
    this.tileDims = (int[]) tileDims.clone();

    // Calculate tile counts
    // ---------------------
    tileCounts = new int[] { 
      dims[ROWS] / tileDims[ROWS],
      dims[COLS] / tileDims[COLS]
    };
    if (tileCounts[ROWS]*tileDims[ROWS] < dims[ROWS]) tileCounts[ROWS]++;
    if (tileCounts[COLS]*tileDims[COLS] < dims[COLS]) tileCounts[COLS]++;

    // Create a cache of all tile positions to use for this scheme.
    positionCache = new LinkedHashMap<>();
    for (int tileRow = 0; tileRow < tileCounts[ROWS]; tileRow++) {
      for (int tileCol = 0; tileCol < tileCounts[COLS]; tileCol++) {
        var pos = new TilePosition (tileRow, tileCol);
        positionCache.put (pos.hashCode(), pos);
      } // for
    } // for

  } // TilingScheme constructor

  ////////////////////////////////////////////////////////////

  /** Gets the global dimensions as [rows, columns]. */
  public int[] getDimensions () { return ((int[]) dims.clone()); }

  ////////////////////////////////////////////////////////////

  /** Gets the tile dimensions as [rows, columns]. */
  public int[] getTileDimensions () { return ((int[]) tileDims.clone()); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the tile dimensions as [rows, columns] for the specified
   * tile position.  This method may return smaller dimensions than
   * <code>getTileDimensions()</code> for tiles at the truncated
   * positions.
   *
   * @param pos the tile position.
   *
   * @return the tile dimensions at the specified position.
   *
   * @deprecated As of 3.3.1, use {@link TilePosition#getDimensions}.
   */
  @Deprecated
  public int[] getTileDimensions (
    TilePosition pos
  ) {

    return (pos.getDimensions());

  } // getTileDimensions

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the total tile count in this scheme.
   *
   * @return the total tile count.
   * 
   * @since 3.8.1
   */
  public int getTileCount () { return (positionCache.size()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the tile counts along each dimension.
   * 
   * @return the tile counts as [rows, columns]. 
   */
  public int[] getTileCounts () { return ((int[]) tileCounts.clone()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new tile position from global coordinates.
   *
   * @param row the data row coordinate.
   * @param col the data column coordinate.
   *
   * @throws IndexOutOfBoundsException if the coordinates do not reference
   * a valid tile in the tiling scheme.
   * 
   * @deprecated As of 3.8.1, use {@link #getTilePositionForCoords}.
   */
  @Deprecated
  public TilePosition createTilePosition (
    int row,
    int col
  ) {

    // Check coordinates
    // -----------------
    if (row < 0 || row >= dims[ROWS])
      throw new IndexOutOfBoundsException ("Row index out of bounds: " + row);
    else if (col < 0 || col >= dims[COLS])
      throw new IndexOutOfBoundsException ("Column index out of bounds: " + col);
    
    return (new TilePosition (row/tileDims[ROWS], col/tileDims[COLS]));

  } // createTilePosition

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a shared instance of a tile position whose tile contains the
   * specified global coordinates.
   * 
   * @param row the data row coordinate.
   * @param col the data column coordinate.
   * 
   * @return the instance of the tile position containing the coordinates.
   *
   * @throws IndexOutOfBoundsException if the coordinates do not reference
   * a valid tile in the tiling scheme.
   * 
   * @since 3.8.1
   */
  public TilePosition getTilePositionForCoords (
    int row,
    int col
  ) {

    if (row < 0 || row > dims[ROWS]-1)
      throw new IndexOutOfBoundsException ("Row coordinate " + row + " out of bounds");
    else if (col < 0 || col > dims[COLS]-1)
      throw new IndexOutOfBoundsException ("Column coordinate " + col + " out of bounds");
    
    int tileRow = row/tileDims[ROWS];
    int tileCol = col/tileDims[COLS];
    int tileHash = tileRow*tileCounts[COLS] + tileCol;

    var pos = getTilePositionFromCache (tileRow, tileCol);
    return (pos);

  } // getTilePositionForCoords

  ////////////////////////////////////////////////////////////

  /**
   * Gets a tile position object using the tile position coordinates.
   * 
   * @param tileRow the tile row index.
   * @param tileCol the tile column index.
   * 
   * @return the instance of the tile position of the specified tile indices.
   * 
   * @throws IndexOutOfBoundsException if the indices do not reference
   * a valid tile in the tiling scheme.
   * 
   * @since 3.8.1
   * 
   * @see #getTileCounts
   */
  public TilePosition getTilePositionForIndex (
    int tileRow,
    int tileCol
  ) {

    if (tileRow < 0 || tileRow > tileCounts[ROWS]-1)
      throw new IndexOutOfBoundsException ("Tile row index " + tileRow + " out of bounds");
    else if (tileCol < 0 || tileCol > tileCounts[COLS]-1)
      throw new IndexOutOfBoundsException ("Tile column index " + tileCol + " out of bounds");

    return (getTilePositionFromCache (tileRow, tileCol));

  } // getTilePositionForIndex

  ////////////////////////////////////////////////////////////

  /**
   * Gets a tile position object from the cache using the tile position
   * coordinates.
   * 
   * @param tileRow the tile row coordinate.
   * @param tileCol the tile column coordinate.
   * 
   * @return the instance of the tile position of the specified tile indices.
   * 
   * @throws IllegalArgumentException if the indices do not match a position
   * in the cache.
   *
   * @since 3.8.1
   */
  private TilePosition getTilePositionFromCache (
    int tileRow,
    int tileCol
  ) {

    int tileHash = tileRow*tileCounts[COLS] + tileCol;
    var pos = positionCache.get (tileHash);
    if (pos == null) throw new IllegalArgumentException ("Tile position for [" + tileRow + "," + tileCol + "] is not available");

    return (pos);

  } // getTilePositionFromCache

  ////////////////////////////////////////////////////////////

  /**
   * Gets a minimal list of tile positions that cover a subset of the global
   * coordinates.
   *
   * @param start the data subset starting [row, column].
   * @param count the data subset dimension [rows, columns].
   *
   * @return the list of tile positions.
   *
   * @throws IndexOutOfBoundsException if the subset falls outside the
   * grid dimensions.
   * 
   * @since 3.8.1
   */
  public List<TilePosition> getCoveringPositions (
    int[] start,
    int[] count
  ) {

    // Check the we haven't been passed invalid sizes.  The start and end
    // coordinates will be checked automatically in the next step.
    if (count[ROWS] <= 0) throw new IllegalArgumentException ("Row count " + count[ROWS] + " invalid for subset");
    if (count[COLS] <= 0) throw new IllegalArgumentException ("Column count " + count[COLS] + " invalid for subset");

    // Get the tile positions coordinates for the two extreme corners, and then 
    // fill in the remaining ones between them.
    int[] minCoords = getTilePositionForCoords (start[ROWS], start[COLS]).getCoords();
    int[] maxCoords = getTilePositionForCoords (start[ROWS]+count[ROWS]-1, start[COLS]+count[COLS]-1).getCoords();
    List<TilePosition> tilePositions = new ArrayList<>();
    for (int i = minCoords[ROWS]; i <= maxCoords[ROWS]; i++) {
      for (int j = minCoords[COLS]; j <= maxCoords[COLS]; j++) {
        tilePositions.add (getTilePositionFromCache (i, j));
      } // for
    } // for

    return (tilePositions);

  } // getCoveringPositions

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of all tile positions for this tiling scheme.
   * 
   * @return the list of all tile positions.  Changes to the returned list
   * have no effect on the tiling scheme.
   * 
   * @since 3.8.1
   */
  public List<TilePosition> getAllPositions () { return (new ArrayList (positionCache.values())); }

  ////////////////////////////////////////////////////////////

  /**
   * A tile position stores the row and column location coordinates of
   * a tile in a tiling scheme.  The tile position is mainly an int[2]
   * array wrapped in an object so that it may be used as a key in a
   * hash map.
   */
  public class TilePosition
    implements Cloneable {

    // Variables
    // ---------

    /** The tile coordinates as [row, column]. */
    private int[] coords;

    /** The tile global starting coordinates. */
    private int[] start;

    /** The tile global ending coordinates. */
    private int[] end;

    ////////////////////////////////////////////////////////

    @Override
    public String toString () {

      return ("TilePosition[" + coords[ROWS] + "," + coords[COLS] + "]");

    } // toString

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new tile position from tile coordinates.
     * 
     * @param row the tile row coordinate.
     * @param col the tile column coordinate.
     *
     * @throws IndexOutOfBoundsException if the coordinates do not reference 
     * a valid tile in the tiling scheme.
     */
    protected TilePosition (
      int row,
      int col
    ) {

      // Check coordinates
      // -----------------
      if (row < 0 || row >= tileCounts[ROWS])
        throw new IndexOutOfBoundsException ("Row index out of bounds: " + row);
      else if (col < 0 || col >= tileCounts[COLS])
        throw new IndexOutOfBoundsException ("Column index out of bounds: " + col);

      // Initialize coordinates
      // ----------------------
      this.coords = new int[] {row, col};

      // Compute extents
      // ---------------
      start = new int[2];
      start[ROWS] = row*tileDims[ROWS];
      start[COLS] = col*tileDims[COLS];
      end = new int[2];
      int[] thisTileDims = getDimensions();
      end[ROWS] = start[ROWS] + thisTileDims[ROWS] - 1;
      end[COLS] = start[COLS] + thisTileDims[COLS] - 1;

    } // TilePosition constructor

    ////////////////////////////////////////////////////////

    /** Gets the position coordinates. */
    public int[] getCoords () { return ((int[]) coords.clone()); }

    ////////////////////////////////////////////////////////

    /** 
     * Gets the dimensions of the tile at this position.
     *
     * @return the dimensions of the tile at this position as [rows, columns].
     */
    public int[] getDimensions () {

      // Get tile coords and dimensions
      // ------------------------------
      int[] thisDims = (int[]) tileDims.clone();

      // Check for last tile
      // -------------------
      if ((coords[ROWS]+1) * tileDims[ROWS] > dims[ROWS])
        thisDims[ROWS] = dims[ROWS]%tileDims[ROWS];
      if ((coords[COLS]+1) * tileDims[COLS] > dims[COLS])
        thisDims[COLS] = dims[COLS]%tileDims[COLS];

      return (thisDims);

    } // getDimensions

    ////////////////////////////////////////////////////////

    /**
     * Gets the starting global coordinates of the tile at this position.
     *
     * @return the starting global coordinates at this position as [rows, columns].
     *
     * @since 3.3.1
     */
    public int[] getStart () {
    
      return ((int[]) start.clone());
    
    } // getStart

    ////////////////////////////////////////////////////////

    @Override
    public int hashCode () {

      return (coords[ROWS]*tileCounts[COLS] + coords[COLS]);

    } // hashCode

    ////////////////////////////////////////////////////////

    /** 
     * Determines if a global coordinate is contained in the tile at
     * this position.
     *
     * @param row the global row coordinate.
     * @param col the global column coordinate.
     *
     * @return true if the location is in this tile position, or false
     * if not.
     */
    public boolean contains (
      int row,
      int col 
    ) {

      if (row < start[ROWS] || row > end[ROWS]) return (false);
      else if (col < start[COLS] || col > end[COLS]) return (false);
      else return (true);

    } // contains

    ////////////////////////////////////////////////////////

    @Override
    public boolean equals (
      Object o
    ) {

      // Check compatibility
      // -------------------
      if (!(o instanceof TilePosition)) return (false);
      TilePosition pos = (TilePosition) o;

      // Check each coordinate
      // ---------------------
      if (this.coords[ROWS] != pos.coords[ROWS]) return (false);
      if (this.coords[COLS] != pos.coords[COLS]) return (false);

      // Conclude we are equal
      // ---------------------
      return (true);

    } // equals

    ////////////////////////////////////////////////////////

    @Override
    public Object clone () {

      return (new TilePosition (coords[ROWS], coords[COLS]));

    } // clone

    ////////////////////////////////////////////////////////

    /**
     * Gets the tiling scheme for this position.
     *
     * @return the tiling scheme associated with this position.
     */
    public TilingScheme getScheme () { return (TilingScheme.this); }

    ////////////////////////////////////////////////////////

  } // TilePosition class

  ////////////////////////////////////////////////////////////

  /**
   * A tile is a rectangular section of data with associated
   * attributes.  The tile position indicates its position in the
   * tiling scheme starting from [0,0].  The tile dimensions are the
   * tile rows and columns, which usually correspond to the tiling
   * scheme tile dimensions except when the global dimensions are not
   * a multiple of the tile dimensions.  The tile data is an array of
   * data values as an object reference.  A tile has a dirty flag to
   * indicate that data values in the tile have been set, but the tile
   * has not been written to a destination.  The dirty flag is for the
   * convenience of classes that use the tile for data reading and
   * writing.
   */
  public class Tile {

    // Variables
    // ---------
    /** The tile position in the grid. */
    private TilePosition pos;

    /** The tile dimensions as [rows, columns]. */
    private int[] dims;

    /** The tile data array as an object. */
    private Object data;

    /** The dirty flag, true if the tile is altered but not written. */
    private boolean dirty;

    ////////////////////////////////////////////////////////

    /**
     * Sets the tile data. Only the reference is copied, not the
     * contents.
     */
    public void setData (Object data) { this.data = data; }   

    ////////////////////////////////////////////////////////

    /** Gets the tile data.  Only the reference is returned. */
    public Object getData () { return (data); }

    ////////////////////////////////////////////////////////

    /** Gets the tile dimensions. */
    public int[] getDimensions () { return ((int[]) dims.clone()); }

    ////////////////////////////////////////////////////////

    /** Gets the tile position. */
    public TilePosition getPosition () { return ((TilePosition) pos.clone()); }

    ////////////////////////////////////////////////////////

    /** Gets the tile dirty flag. */
    public boolean getDirty () { return (dirty); }

    ////////////////////////////////////////////////////////

    /** Sets the tile dirty flag. */
    public void setDirty (boolean flag) { dirty = flag; }

    ////////////////////////////////////////////////////////

    @Override
    public String toString () {

      String str = "Tile[";
      str += "pos=" + pos + ",";
      str += "dims=[" + dims[ROWS] + "," + dims[COLS] + "],"; 
      str += "data=" + data + ",";
      str += "dirty=" + dirty + "]";
      str += "]";
      return (str);

    } // toString

    ////////////////////////////////////////////////////////

    /**
     * Creates a new tile with the specified parameters.  The dirty flag
     * is initialized to false.
     *
     * @param pos the tile position in the grid.
     * @param data the tile data.  
     */
    public Tile (
      TilePosition pos,
      Object data
    ) { 

      this.pos = (TilePosition) pos.clone();
      this.data = data;
      this.dims = pos.getDimensions();
      this.dirty = false;

    } // Tile constructor

    ////////////////////////////////////////////////////////

    /** 
     * Determines if a global coordinate is contained in this tile.
     *
     * @param row the global row coordinate.
     * @param col the global column coordinate.
     *
     * @return true if the location is in this tile, or false if not.
     */
    public boolean contains (
      int row,
      int col 
    ) {

      return (pos.contains (row, col));

    } // contains

    ////////////////////////////////////////////////////////

    /** 
     * Gets an index into the tile data based on a global data
     * coordinate.
     *
     * @param row the global row coordinate.
     * @param col the global column coordinate.
     *
     * @return the index into this tile's data.
     */
    public int getIndex (
      int row,
      int col
    ) {

      return ((row%tileDims[ROWS])*dims[COLS] + (col%tileDims[COLS]));

    } // getIndex

    ////////////////////////////////////////////////////////

    /**
     * Gets the tile rectangle.
     *
     * @return the rectangle bounding this tile.
     */
    public Rectangle getRectangle () {

      int[] coords = pos.getCoords();
      return (new Rectangle (coords[COLS]*tileDims[COLS],
        coords[ROWS]*tileDims[ROWS], dims[COLS], dims[ROWS]));

    } // getRectangle

    ////////////////////////////////////////////////////////

    /**
     * Compares the tile position to this tile for equality.
     * 
     * @param pos the tile position for comparison.
     *
     * @return true if the tile position is the same as this tile, or
     * false otherwise.
     */
    public boolean hasPosition (
      TilePosition pos
    ) {

      return (pos.equals (this.pos));

    } // hasPosition

    ////////////////////////////////////////////////////////

    /**
     * Gets the tiling scheme for this tile.
     *
     * @return the tiling scheme associated with this tile.
     */
    public TilingScheme getScheme () { return (TilingScheme.this); }

    ////////////////////////////////////////////////////////

  } // Tile class

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (TilingScheme.class);

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
    
    logger.test ("TilingScheme");

    int[] globalDims = new int[] {100, 200};
    int[] tileDims = new int[] {40, 40};
    TilingScheme scheme = new TilingScheme (globalDims, tileDims);
    
    int[] testGlobalDims = scheme.getDimensions();
    assert (testGlobalDims != globalDims);
    assert (Arrays.equals (testGlobalDims, globalDims));
    
    int[] testTileDims = scheme.getTileDimensions();
    assert (testTileDims != tileDims);
    assert (Arrays.equals (testTileDims, tileDims));

    int[] tileCounts = scheme.getTileCounts();
    assert (tileCounts[ROWS] == 3);
    assert (tileCounts[COLS] == 5);

    TilePosition pos = scheme.getTilePositionForCoords (85, 30);
    int[] posCoords = pos.getCoords();
    assert (posCoords[ROWS] == 2);
    assert (posCoords[COLS] == 0);

    boolean isException0 = false;
    try { scheme.getTilePositionForCoords (100, 0); }
    catch (IndexOutOfBoundsException e) { isException0 = true; }
    assert (isException0);

    logger.passed();

    logger.test ("TilePosition");

    TilePosition pos1 = scheme.getTilePositionForIndex (1, 2);
    int[] pos1Coords = pos1.getCoords();
    assert (pos1Coords[ROWS] == 1);
    assert (pos1Coords[COLS] == 2);
    
    int[] testTileDims2 = scheme.getTilePositionForIndex (0, 0).getDimensions();
    assert (testTileDims2[ROWS] == 40);
    assert (testTileDims2[COLS] == 40);

    int[] testTileDims3 = scheme.getTilePositionForIndex (2, 0).getDimensions();
    assert (testTileDims3[ROWS] == 20);
    assert (testTileDims3[COLS] == 40);

    int[] testTileDims4 = scheme.getTilePositionForIndex (0, 4).getDimensions();
    assert (testTileDims4[ROWS] == 40);
    assert (testTileDims4[COLS] == 40);

    TilePosition pos2 = scheme.getTilePositionForIndex (1, 2);
    TilePosition pos3 = scheme.getTilePositionForIndex (2, 2);
    assert (pos1.hashCode() == pos2.hashCode());
    assert (pos1.hashCode() != pos3.hashCode());
    
    assert (pos.contains (82, 5));
    assert (!pos.contains (79, 5));

    assert (pos1.equals (pos2));
    assert (!pos1.equals (pos3));
    TilePosition pos4 = (TilePosition) pos2.clone();
    assert (pos4.equals (pos2));
    
    assert (pos1.getScheme() == scheme);

    boolean isException = false;
    try { scheme.getTilePositionForIndex (0, 5); }
    catch (IndexOutOfBoundsException e) { isException = true; }
    assert (isException);    

    logger.passed();
    
    logger.test ("Tile");
    
    Object data = Integer.valueOf (38);
    Tile tile = scheme.new Tile (pos, data);
    assert (tile.getData().equals (data));
    tile.setData (null);
    assert (tile.getData() == null);
    assert (Arrays.equals (tile.getDimensions(), pos.getDimensions()));
    assert (tile.getPosition().equals (pos));
    assert (!tile.getDirty());
    tile.setDirty (true);
    assert (tile.getDirty());
    tile.setDirty (false);
    assert (!tile.getDirty());
    
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
     *
     *  (85, 30) --> tile (2, 0)
     *  In coords of tile (2, 0), (85, 30) - (80, 0) = (5, 30)
     *  Index of (5, 30) = 5*40 + 30 = 230
     *
     *  Rectangle of tile (2, 0) = (x, y, width, height) = (0, 80, 40, 20)
     */
    assert (tile.getIndex (85, 30) == 230);
    
    Rectangle rect = tile.getRectangle();
    assert (rect.x == 0);
    assert (rect.y == 80);
    assert (rect.width == 40);
    assert (rect.height == 20);

    assert (tile.hasPosition (pos));
    assert (!tile.hasPosition (pos1));
    
    assert (tile.getScheme() == scheme);
    
    logger.passed();
  
  } // main

  ////////////////////////////////////////////////////////////

} // TilingScheme class

////////////////////////////////////////////////////////////////////////
