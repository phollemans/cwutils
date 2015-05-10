////////////////////////////////////////////////////////////////////////
/*
     FILE: TileCacheKey.java
   AUTHOR: Peter Hollemans
     DATE: 2014/07/15
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
import java.util.Arrays;
import noaa.coastwatch.io.tile.TileSource;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;

/**
 * A <code>TileCacheKey</code> object stores a pair of values: a tile source,
 * and a tile position, that can be used in a {@link TileCache} to store and recall
 * a specific tile.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class TileCacheKey {

  // Variables
  // ---------

  /** The tile source for this key. */
  private TileSource source;

  /** The tile position for this key. */
  private TilePosition pos;
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache key.
   *
   * @param source the source for the tiles.
   * @param pos the position of the tile in the scheme.
   */
  public TileCacheKey (
    TileSource source,
    TilePosition pos
  ) {
  
    this.source = source;
    this.pos = pos;
  
  } // TileCacheKey constructor

  ////////////////////////////////////////////////////////////

  @Override
  public boolean equals (Object obj) {
  
    boolean isEqual = false;
    if (obj instanceof TileCacheKey) {
      TileCacheKey key = (TileCacheKey) obj;
      isEqual = (this.source == key.source && this.pos.equals (key.pos));
    } // if

    return (isEqual);

  } // equals

  ////////////////////////////////////////////////////////////

  @Override
  public int hashCode() {
  
    return (source.hashCode()*1009 ^ pos.hashCode()*1013);
  
  } // hashCode
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the source for this key.
   *
   * @return the tile source.
   */
  TileSource getSource() { return (source); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the position for this key.
   *
   * @return the tile position in the scheme.
   */
  TilePosition getPosition() { return (pos); }

  ////////////////////////////////////////////////////////////

  @Override
  public String toString () {

    return ("TileCacheKey[source=" + source + ",pos=" + pos + "]");

  } // toString

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

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
    TilePosition pos = scheme.new TilePosition (2, 3);

    System.out.print ("Testing constructor ... ");
    TileCacheKey key = new TileCacheKey (source, pos);
    assert (key.getSource() == source);
    assert (key.getPosition() == pos);
    System.out.println ("OK");

    System.out.print ("Testing equals ... ");
    TilePosition pos2 = scheme.new TilePosition (2, 2);
    TileCacheKey key2 = new TileCacheKey (source, pos2);
    assert (!key.equals (key2));
    TilePosition pos3 = scheme.new TilePosition (2, 3);
    TileCacheKey key3 = new TileCacheKey (source, pos3);
    assert (key.equals (key3));
    
    TileSource source2 = new TileSource () {
      public Tile readTile (
        TilePosition pos
      ) throws IOException {
        Tile tile = null;
        int[] tileDims = pos.getDimensions();
        int count = tileDims[0] * tileDims[1];
        int[] data = new int[count];
        Arrays.fill (data, (int) pos.hashCode());
        tile = scheme.new Tile (pos, data);
        return (tile);
      } // readtile
      public Class getDataClass() { return (Integer.TYPE); }
      public TilingScheme getScheme() { return (scheme); }
    };
    TileCacheKey key4 = new TileCacheKey (source2, pos);
    assert (!key.equals (key4));
    assert (!key2.equals (key4));
    System.out.println ("OK");
    
    System.out.print ("Testing hashCode ... ");
    assert (key.hashCode() != key2.hashCode());
    assert (key.hashCode() == key3.hashCode());
    assert (key.hashCode() != key4.hashCode());
    System.out.println ("OK");

  } // main

  ////////////////////////////////////////////////////////////

} // TileCacheKey class

////////////////////////////////////////////////////////////////////////

