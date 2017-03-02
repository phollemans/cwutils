////////////////////////////////////////////////////////////////////////
/*

     File: TileSink.java
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
import noaa.coastwatch.io.tile.TilingScheme.Tile;

/**
 * The <code>TileSink</code> interface is for writing individual rectangles
 * of data to a data sink.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public interface TileSink {

  ////////////////////////////////////////////////////////////

  /**
   * Writes the specified tile to the sink.
   *
   * @param tile the tile to write.
   *
   * @throws IOException if an error occurred writing the tile data.
   */
  public void writeTile (
    Tile tile
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the tiling scheme used to write tiles to this sink.
   *
   * @return the tiling scheme.
   */
  public TilingScheme getScheme();

  ////////////////////////////////////////////////////////////

} // TileSink class

////////////////////////////////////////////////////////////////////////

