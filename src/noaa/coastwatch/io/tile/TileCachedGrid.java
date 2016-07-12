////////////////////////////////////////////////////////////////////////
/*
     FILE: TileCachedGrid.java
   AUTHOR: Peter Hollemans
     DATE: 2014/08/01
  CHANGES: 2014/08/26, PFH
           - Changes: Implemented dispose() method.
           - Issue: We added a dispose() method at the DataVariable level to
             better handle disposing of resources, rather than relying on 
             finalize() which is inherently unsafe because there is no guarantee
             that it will ever be called by the VM.  In this case we needed
             to remove tiles from the cache for this grid, when the tiles are 
             known to no longer be needed.
          2014/11/11, PFH
          - Changes: Changed to use weak references for last tile.
          - Issue: Holding onto strong references for the last tile was causing
            problems with memory management.
          2016/01/19, PFH
           - Changes: Updated to new logging API.

  CoastWatch Software Library and Utilities
  Copyright 2014-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io.tile;

// Imports
// -------
import java.awt.Rectangle;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import noaa.coastwatch.io.tile.TileCache;
import noaa.coastwatch.io.tile.TileCacheManager;
import noaa.coastwatch.io.tile.TileSource;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.util.Grid;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The <code>TileCachedGrid</code> class is a <code>Grid</code> whose data 
 * is supplied from a {@link TileSource} and cached via the 
 * {@link TileCacheManager}.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class TileCachedGrid
  extends Grid {

  // Variables
  // ---------

  /** The source to use for data. */
  private TileSource source;

  /** A weak reference to the last tile retrieved from the cache. */
  private WeakReference<Tile> lastTileRef;

  /** The class for the elements in the data array for this variable. */
  private Class dataClass;

  ////////////////////////////////////////////////////////////

  @Override
  public Class getDataClass() { return (dataClass); }

  ////////////////////////////////////////////////////////////

  @Override
  public void dispose () {
  
    TileCacheManager.getInstance().removeTilesForSource (source);
    super.dispose();
  
  } // dispose

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new grid from the specified tile source.
   *
   * @param source the tile source to use for tiles.
   */
  public TileCachedGrid (
    Grid grid,
    TileSource source
  ) {
    
    // Initialize
    // ----------
    super (grid);
    this.source = source;
    this.dataClass = grid.getDataClass();
    /**
     * We have to do this next line because we've overridden the getDataClass()
     * method, and it's used in the constructor for DataVariable to detect
     * the type of the array, so that an unsigned version of a type can
     * be assigned.  But the data class doesn't get assigned until after the
     * super constructor so we have to re-run the unsigned type setting here.
     * This may be an indicator that the class structure should be improved, for
     * example we could have a factory method create different classes for 
     * signed versus unsigned so that we don't need to have such complex logic
     * when getting data values in all cases, which would speed up value
     * conversion. (TODO)
     */
    this.setUnsigned (grid.getUnsigned());

  } // TileCachedGrid constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void setValue (
    int index,
    double val
  ) {

    throw new UnsupportedOperationException ("setValue() not supported");

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

    // Check bounds
    // ------------
    if (row < 0 || row > dims[ROWS]-1 || col < 0 || col > dims[COLS]-1)
      return (Double.NaN);

    // Get last tile
    // -------------
    Tile lastTile = null;
    if (lastTileRef != null) lastTile = lastTileRef.get();

    // Get tile
    // --------
    Tile tile;
    if (lastTile != null && lastTile.contains (row, col))
      tile = lastTile;
    else {
      TilePosition pos = source.getScheme().createTilePosition (row, col);
      try { tile = TileCacheManager.getInstance().getTile (source, pos); }
      catch (IOException e) {
        throw new RuntimeException ("Error getting tile: " + e.getMessage());
      } // catch
      lastTileRef = new WeakReference<Tile> (tile);
    } // else

    return (getValue (tile.getIndex (row, col), tile.getData()));

  } // getValue

  ////////////////////////////////////////////////////////////

  @Override
  public Object getData () { return (getData (new int[] {0, 0}, dims)); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getData (
    int[] start,
    int[] count
  ) {

    // Check subset
    // ------------
    if (!checkSubset (start, count))
      throw new IndexOutOfBoundsException ("Invalid subset: " +
      "start=" + Arrays.toString (start) +
      ", " +
      "count=" + Arrays.toString (count));

    // Find required tiles
    // -------------------
    TilingScheme scheme = source.getScheme();
    int[] minCoords = scheme.createTilePosition(start[ROWS],
      start[COLS]).getCoords();
    int[] maxCoords = scheme.createTilePosition(start[ROWS]+count[ROWS]-1,
      start[COLS]+count[COLS]-1).getCoords();
    List<TilePosition> tilePositionList = new ArrayList<TilePosition>();
    for (int i = minCoords[ROWS]; i <= maxCoords[ROWS]; i++)
      for (int j = minCoords[COLS]; j <= maxCoords[COLS]; j++)
        tilePositionList.add (scheme.new TilePosition (i, j));

    // Create subset array
    // -------------------
    Object subsetData = Array.newInstance (dataClass, count[ROWS]*count[COLS]);
    Rectangle subsetRect = new Rectangle (start[COLS], start[ROWS], 
      count[COLS], count[ROWS]);

    // Loop over each tile
    // -------------------
    for (TilePosition pos : tilePositionList) {

      // Get tile data
      // -------------
      Tile tile;
      try { tile = TileCacheManager.getInstance().getTile (source, pos); }
      catch (IOException e) {
        throw new RuntimeException ("Error getting tile: " + e.getMessage());
      } // catch
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
          subsetData, (intersect.y-subsetRect.y+i)*count[COLS] +
          (intersect.x-subsetRect.x), intersect.width);
      } // for

    } // while
 
    return (subsetData);

  } // getData

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (TileCachedGrid.class);

    /*
     *     0    1    2
     *   +-----+-----+---  \
     * 0 |     |     |     |  15
     *   |     |     |     |
     *   +-----+-----+---  X
     * 1 |     |     |     |  15
     *   |     |     |     |
     *   +-----+-----+---  X
     * 2 |     |     |     |  5
     *   |     |     |     /
     *
     *   \-----X-----X--/
     *     15   15   5
     */
    
    logger.test ("Framework");
    
    int[] globalDims = new int[] {40, 40};
    int[] tileDims = new int[] {15, 15};
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
        } // else
        return (tile);
      } // readtile
      public Class getDataClass() { return (Byte.TYPE); }
      public TilingScheme getScheme() { return (scheme); }
    };

    Grid grid = new Grid (
      "test",
      "test data",
      "meters",
      40,
      40,
      new byte[1],
      new java.text.DecimalFormat ("000"),
      null,
      null);
    grid.setUnsigned (true);

    logger.passed();

    logger.test ("constructor, getDataClass");
    TileCachedGrid cachedGrid = new TileCachedGrid (grid, source);
    assert (cachedGrid.getDataClass() == Byte.TYPE);
    assert (cachedGrid.getUnsigned());
    logger.passed();
    
    logger.test ("getValue");
    throwError[0] = true;
    try {
      cachedGrid.getValue (0, 0);
      assert (false);
    } // try
    catch (RuntimeException e) { }
    throwError[0] = false;
    assert (cachedGrid.getValue (0, 0) == 0);
    assert (cachedGrid.getValue (0, 15) == 1);
    assert (cachedGrid.getValue (0, 30) == 2);
    assert (cachedGrid.getValue (15, 0) == 3);
    assert (cachedGrid.getValue (15, 15) == 4);
    assert (cachedGrid.getValue (15, 30) == 5);
    assert (cachedGrid.getValue (30, 0) == 6);
    assert (cachedGrid.getValue (30, 15) == 7);
    assert (cachedGrid.getValue (30, 30) == 8);
    assert (Double.isNaN (cachedGrid.getValue (0, 40)));
    assert (Double.isNaN (cachedGrid.getValue (-1, -1)));
    logger.passed();
    
    logger.test ("setValue");
    try {
      cachedGrid.setValue (0, 0, 0);
      assert (false);
    } // try
    catch (UnsupportedOperationException e) { }
    logger.passed();

    logger.test ("getData");
    byte[] data;
    try {
      cachedGrid.getData (new int[] {-1, 0}, new int[] {40, 40});
      assert (false);
    } // try
    catch (IndexOutOfBoundsException e) { }
    try {
      cachedGrid.getData (new int[] {1, 0}, new int[] {40, 40});
      assert (false);
    } // try
    catch (IndexOutOfBoundsException e) { }
    data = (byte[]) cachedGrid.getData();
    for (int i = 0; i < 40; i++) {
      for (int j = 0; j < 40; j++) {
        int tileRow = i/15;
        int tileCol = j/15;
        assert (data[i*40 + j] == tileRow*3 + tileCol);
      } // for
    } // for
    logger.passed();
    
    logger.test ("dispose");
    assert (TileCacheManager.getInstance().getCache().getCacheSize() != 0);
    cachedGrid.dispose();
    assert (TileCacheManager.getInstance().getCache().getCacheSize() == 0);
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////


} // TileCachedGrid class

////////////////////////////////////////////////////////////////////////
