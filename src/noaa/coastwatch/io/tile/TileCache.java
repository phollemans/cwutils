////////////////////////////////////////////////////////////////////////
/*

     File: TileCache.java
   Author: Peter Hollemans
     Date: 2014/07/01

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

