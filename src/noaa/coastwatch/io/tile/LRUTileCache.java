////////////////////////////////////////////////////////////////////////
/*

     File: LRUTileCache.java
   Author: Peter Hollemans
     Date: 2014/07/09

  CoastWatch Software Library and Utilities
  Copyright (c) 2014 National Oceanic and Atmospheric Administration
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
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import noaa.coastwatch.io.tile.TileCache;
import noaa.coastwatch.io.tile.TileCacheKey;
import noaa.coastwatch.io.tile.TileSource;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The <code>LRUTileCache</code> is a tile cache that uses a 
 * least-recently-used rule to eliminate unused tiles.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class LRUTileCache
  extends LinkedHashMap<TileCacheKey, Tile>
  implements TileCache {

  // Variables
  // ---------
  
  /** The current cache size in bytes. */
  private int cacheSize;
  
  /** The maximum cache size in bytes. */
  private int maxCacheSize;

  ////////////////////////////////////////////////////////////

  public void setCacheSizeLimit (int bytes) { maxCacheSize = bytes; }

  ////////////////////////////////////////////////////////////

  public int getCacheSizeLimit() { return (maxCacheSize); }
  
  ////////////////////////////////////////////////////////////

  public int getCacheSize() { return (cacheSize); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the memory used by an array in bytes.
   *
   * @param array the array to inspect.
   *
   * @return the number of bytes used by the values in the array.
   *
   * @throws IllegalArgumentException if the array type is unknown.
   */
  private static int getBytesUsed (
    Object array
  ) {

    // Get the number of bytes per array value
    // ---------------------------------------
    Class arrayType = array.getClass();
    int bytesPerValue = 0;
    if (arrayType.equals (boolean[].class))
      bytesPerValue = 1;
    else if (arrayType.equals (byte[].class))
      bytesPerValue = 1;
    else if (arrayType.equals (short[].class))
      bytesPerValue = 2;
    else if (arrayType.equals (int[].class))
      bytesPerValue = 4;
    else if (arrayType.equals (long[].class))
      bytesPerValue = 8;
    else if (arrayType.equals (float[].class))
      bytesPerValue = 4;
    else if (arrayType.equals (double[].class))
      bytesPerValue = 8;
    else
      throw new IllegalArgumentException ("Unknown array type: " + arrayType);

    // Get the total bytes used
    // ------------------------
    int length = Array.getLength (array);
    int bytesUsed = length*bytesPerValue;

    return (bytesUsed);
    
  } // getBytesUsed

  ////////////////////////////////////////////////////////////

  @Override
  protected boolean removeEldestEntry (
    Map.Entry<TileCacheKey, Tile> eldest
  ) {
  
    // Remove entries if needed
    // ------------------------
    if (cacheSize > maxCacheSize) {
      Iterator<Map.Entry<TileCacheKey, Tile>> iterator = entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<TileCacheKey, Tile> entry = iterator.next();
        iterator.remove();
        cacheSize = cacheSize - getBytesUsed (entry.getValue().getData());
        if (cacheSize <= maxCacheSize) break;
      } // for
    } // if
  
    return (false);
    
  } // removeEldestEntry

  ////////////////////////////////////////////////////////////

  @Override
  public Tile remove (
    Object key
  ) {

    Tile removedValue = super.remove (key);
    if (removedValue != null)
      cacheSize = cacheSize - getBytesUsed (removedValue.getData());

    return (removedValue);

  } // remove
  
  ////////////////////////////////////////////////////////////

  @Override
  public Tile put (
    TileCacheKey key,
    Tile value
  ) {

    // Subtract the old tile size
    // --------------------------
    if (containsKey (key)) {
      Tile previousValue = get (key);
      cacheSize = cacheSize - getBytesUsed (previousValue.getData());
    } // if

    // Add the new tile size
    // ---------------------
    cacheSize += getBytesUsed (value.getData());

    // Perform the put
    // ---------------
    Tile previousValue = super.put (key, value);
    return (previousValue);

  } // put

  ////////////////////////////////////////////////////////////

  @Override
  public void clear() {
  
    super.clear();
    cacheSize = 0;
  
  } // clear

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new empty cache with maximum 16 Mb data.
   */
  public LRUTileCache () {

    super (16, 0.75f, true);
    cacheSize = 0;
    maxCacheSize = 1024*1024*16;

  } // LRUTileCache constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (LRUTileCache.class);

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

    logger.test ("getBytesUsed");
    assert (getBytesUsed (new boolean[10]) == 10);
    assert (getBytesUsed (new byte[20]) == 20);
    assert (getBytesUsed (new short[30]) == 60);
    assert (getBytesUsed (new int[40]) == 160);
    assert (getBytesUsed (new long[50]) == 400);
    assert (getBytesUsed (new float[60]) == 240);
    assert (getBytesUsed (new double[70]) == 560);
    try {
      getBytesUsed (new Object[80]);
      assert (false);
    } // try
    catch (IllegalArgumentException e) {
      assert (true);
    } // catch
    logger.passed();

    int[] globalDims = new int[] {100, 200};
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

    logger.test ("constructor");
    LRUTileCache cache = new LRUTileCache();
    assert (cache.getCacheSize() == 0);
    assert (cache.getCacheSizeLimit() == 1024*1024*16);
    logger.passed();

    logger.test ("setCacheSizeLimit");
    cache.setCacheSizeLimit (40*40*3);
    assert (cache.getCacheSizeLimit() == 40*40*3);
    logger.passed();
    
    logger.test ("put");

    TilePosition pos1 = scheme.getTilePositionForIndex (0, 0);
    Tile tile1 = source.readTile (pos1);
    TileCacheKey key1 = new TileCacheKey (source, pos1);

    TilePosition pos2 = scheme.getTilePositionForIndex (1, 1);
    Tile tile2 = source.readTile (pos2);
    TileCacheKey key2 = new TileCacheKey (source, pos2);

    TilePosition pos3 = scheme.getTilePositionForIndex (2, 2);
    Tile tile3 = source.readTile (pos3);
    TileCacheKey key3 = new TileCacheKey (source, pos3);

    TilePosition pos4 = scheme.getTilePositionForIndex (1, 3);
    Tile tile4 = source.readTile (pos4);
    TileCacheKey key4 = new TileCacheKey (source, pos4);

    cache.put (key1, tile1);
    assert (cache.containsKey (key1));
    assert (cache.get (key1) == tile1);
    assert (cache.getCacheSize() == 40*40);
    
    cache.put (key2, tile2);
    assert (cache.containsKey (key1));
    assert (cache.get (key1) == tile1);
    assert (cache.containsKey (key2));
    assert (cache.get (key2) == tile2);
    assert (cache.getCacheSize() == 40*40*2);

    cache.put (key3, tile3);
    assert (cache.containsKey (key1));
    assert (cache.get (key1) == tile1);
    assert (cache.containsKey (key2));
    assert (cache.get (key2) == tile2);
    assert (cache.containsKey (key3));
    assert (cache.get (key3) == tile3);
    assert (cache.getCacheSize() == 40*40*2 + 40*20);

    cache.put (key4, tile4);
    assert (!cache.containsKey (key1));
    assert (cache.containsKey (key2));
    assert (cache.get (key2) == tile2);
    assert (cache.containsKey (key3));
    assert (cache.get (key3) == tile3);
    assert (cache.containsKey (key4));
    assert (cache.get (key4) == tile4);
    assert (cache.getCacheSize() == 40*40*2 + 40*20);
    
    logger.passed();
    
    logger.test ("remove");
    cache.remove (key3);
    assert (!cache.containsKey (key1));
    assert (cache.containsKey (key2));
    assert (cache.get (key2) == tile2);
    assert (!cache.containsKey (key3));
    assert (cache.containsKey (key4));
    assert (cache.get (key4) == tile4);
    assert (cache.getCacheSize() == 40*40*2);
    logger.passed();

    logger.test ("clear");
    cache.clear();
    assert (cache.getCacheSize() == 0);
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // TileCache interface

////////////////////////////////////////////////////////////////////////

