////////////////////////////////////////////////////////////////////////
/*
     FILE: TileCache.java
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
import java.util.Map;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.Tile;

/**
 * A <code>TileCache</code> object stores a set of {@link TilingScheme.Tile} 
 * objects and allows for their retrieval.  A tile cache may be used to speed
 * up access to frequently-used data tiles.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public interface TileCache
  extends Map<TileCacheKey, Tile> {

  ////////////////////////////////////////////////////////////

  /**
   * Sets the total cache size limit in bytes.
   *
   * @param bytes the number of bytes allowed in the cache.  The cache
   * must delete tiles if adding a tile would go over this limit.
   */
  public void setCacheSizeLimit (int bytes);

  ////////////////////////////////////////////////////////////

  /**
   * Gets the total cache size limit in bytes.
   *
   * @return the number of bytes allowed in the cache.  The cache
   * must delete tiles if adding a tile would go over this limit.
   */
  public int getCacheSizeLimit();
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the total cache size in bytes.
   *
   * @return the number of bytes currently in the cache.
   */
  public int getCacheSize();

  ////////////////////////////////////////////////////////////

} // TileCache interface

////////////////////////////////////////////////////////////////////////

