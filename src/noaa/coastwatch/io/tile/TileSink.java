////////////////////////////////////////////////////////////////////////
/*
     FILE: TileSink.java
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

