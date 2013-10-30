////////////////////////////////////////////////////////////////////////
/*
     FILE: TilingScheme.java
  PURPOSE: A class to support tiling of 2D data.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/06
  CHANGES: 2002/12/04, PFH, added Cloneable interface to TilePosition
           2004/10/20, PFH, modified TilePosition.contains() for better speed

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import java.awt.*;

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
 *              <---------->        /
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
 * truncated -->|  [2,0]  *  [2,1]  * [2,2]  |
 *   tile       +---------*---------*---+    v
 *
 *              <------------------------>
 *                    global column
 *                      dimension  
 * </pre>
 * In the example above, the tile count in both dimensions is 3, even
 * though some tiles are truncated due to the tile dimensions.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class TilingScheme {

  // Constants
  // ---------
  /** Index of rows dimension. */
  public final static int ROWS = Grid.ROWS;

  /** Index of columns dimension. */
  public final static int COLS = Grid.COLS;

  // Variables
  // ---------
  /** The global dimensions as [rows, columns]. */
  private int[] dims;

  /** The dimensions of each data tile as [rows, columns]. */
  private int[] tileDims;

  /** The tile count in each dimension as [rows, columns]. */
  private int[] tileCounts;

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
   */
  public int[] getTileDimensions (
    TilePosition pos
  ) {

    // Get tile coords and dimensions
    // ------------------------------
    int[] coords = pos.getCoords();
    int[] thisDims = (int[]) tileDims.clone();

    // Check for last tile
    // -------------------
    if ((coords[ROWS]+1) * tileDims[ROWS] > dims[ROWS])
      thisDims[ROWS] = dims[ROWS]%tileDims[ROWS];
    if ((coords[COLS]+1) * tileDims[COLS] > dims[COLS])
      thisDims[COLS] = dims[COLS]%tileDims[COLS];

    return (thisDims);

  } // getTileDimensions

  ////////////////////////////////////////////////////////////

  /** Gets the tile counts as [rows, columns]. */
  public int[] getTileCounts () { return ((int[]) tileCounts.clone()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new tile position from global coordinates.
   *
   * @param row the data row coordinate.
   * @param col the data column coordinate.
   */
  public TilePosition createTilePosition (
    int row,
    int col
  ) {

    return (new TilePosition (row/tileDims[ROWS], col/tileDims[COLS]));

  } // createTilePosition

  ////////////////////////////////////////////////////////////

  /**
   * A tile position stores the row and column location coordinates of
   * a tile in a tiling scheme.  The tile position is simply an int[2]
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

    /** Converts this tile position to a string. */
    public String toString () {

      return ("TilePosition[" + coords[ROWS] + "," + coords[COLS] + "]");

    } // toString

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new tile position from tile coordinates.
     * 
     * @param row the tile row coordinate.
     * @param col the tile column coordinate.
     */
    public TilePosition (
      int row,
      int col
    ) {

      // Initialize coordinates
      // ----------------------
      this.coords = new int[] {row, col};

      // Compute extents
      // ---------------
      start = new int[2];
      start[ROWS] = row*tileDims[ROWS];
      start[COLS] = col*tileDims[COLS];
      end = new int[2];
      end[ROWS] = start[ROWS] + tileDims[ROWS] - 1;
      end[COLS] = start[COLS] + tileDims[COLS] - 1;

    } // TilePosition constructor

    ////////////////////////////////////////////////////////

    /** Gets the position coordinates. */
    public int[] getCoords () { return ((int[]) coords.clone()); }

    ////////////////////////////////////////////////////////

    /** Gets the dimensions of the tile at this position. */
    public int[] getDimensions () { return (getTileDimensions (this)); }

    ////////////////////////////////////////////////////////

    /** Creates a hash code based on the tile position coordinates. */
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

    /** Checks for equality of tile position coordinates. */
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

    public Object clone () {

      return (new TilePosition (coords[ROWS], coords[COLS]));

    } // clone

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

    /** Converts this tile to a string. */
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

  } // Tile class

  ////////////////////////////////////////////////////////////

} // TilingScheme class

////////////////////////////////////////////////////////////////////////
