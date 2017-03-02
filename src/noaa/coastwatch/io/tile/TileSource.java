////////////////////////////////////////////////////////////////////////
/*

     File: TileSource.java
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
import java.io.IOException;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.io.tile.TilingScheme.Tile;

/**
 * The <code>TileSource</code> interface is for reading individual rectangles
 * of data from a data source.  Tiles can be read synchronously using the 
 * {@link #readTile} method or asynchronously using a
 * {@link TileDeliveryOperation} object.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public interface TileSource {

  ////////////////////////////////////////////////////////////

  /**
   * Reads the specified tile from the source.
   *
   * @param pos the tile position to read.
   *
   * @return the tile at the specified position.
   *
   * @throws IOException if an error occurred reading the tile data.
   * @throws IllegalArgumentException if the position tiling scheme does not
   * match this source.
   *
   * @see #getScheme
   */
  public Tile readTile (
    TilePosition pos
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the tiling scheme used to deliver tiles by this source.
   *
   * @return the tiling scheme.
   */
  public TilingScheme getScheme();

  ////////////////////////////////////////////////////////////

} // TileSource class

////////////////////////////////////////////////////////////////////////

