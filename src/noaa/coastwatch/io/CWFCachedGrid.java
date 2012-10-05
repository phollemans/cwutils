////////////////////////////////////////////////////////////////////////
/*
     FILE: CWFCachedGrid.java
  PURPOSE: Reads CWF data using caching.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/30
  CHANGES: 2004/06/09, PFH, modified to store reader rather than cwid
           2004/10/07, PFH, modified to use setOptimizedCacheSize()

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.TilingScheme.*;

/**
 * The <code>CWFCachedGrid<code> class is a <code>CachedGrid</code>
 * that may be used in conjunction with a <code>CWFReader</code> to
 * read data from a CWF file.
 *
 * @see noaa.coastwatch.io.CWFReader
 * @see noaa.coastwatch.io.CachedGrid
 */
public class CWFCachedGrid
  extends CachedGrid {

  // Constants
  // ---------

  /** Default tile size in bytes. */
  public final static int DEFAULT_TILE_SIZE = 512*1024;

  /** Default cache size in bytes. */
  public final static int DEFAULT_CACHE_SIZE = 4096*1024;

  // Variables
  // ---------

  /** The CWF reader. */
  private CWFReader reader;

  /** The CWF variable index. */
  private int varIndex;

  /** The CWF variable type. */
  private int varType;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new read-only CWF cached grid with the specified
   * properties.  The cached grid is given the default tile size.
   *
   * @param grid the grid to use for attributes.
   * @param reader the CWF reader data source.
   * @param varIndex the CWF variable index.
   *
   * @throws IOException if a problem occurred accessing the HDF file.
   */
  public CWFCachedGrid (
    Grid grid,
    CWFReader reader,
    int varIndex
  ) throws IOException {

    // Create cached grid
    // ------------------    
    super (grid, READ_ONLY);
    this.reader = reader;

    // Get variable info
    // -----------------
    this.varIndex = varIndex;
    this.varType = CWF.inquire_variable_type (reader.getCWID(), varIndex);

    // Set tile and cache sizes
    // ------------------------
    setTileSize (DEFAULT_TILE_SIZE);
    setOptimizedCacheSize (DEFAULT_CACHE_SIZE);

  } // CWFCachedGrid

  ////////////////////////////////////////////////////////////

  /**
   * Reads the specified tile from the CWF dataset.
   *
   * @param pos the tile position to read.
   *
   * @return the tile at the specified position.
   *
   * @throws IOException if an error occurred reading the tile data.
   */
  protected Tile readTile (
    TilePosition pos
  ) throws IOException {

    // Create data array
    // -----------------
    int[] dataDims = tiling.getTileDimensions (pos);
    int dataValues = dataDims[ROWS]*dataDims[COLS];
    Object data = Array.newInstance (getDataClass(), dataValues);

    // Setup start and length
    // ----------------------
    int[] tileCoords = pos.getCoords();
    int[] tileDims = tiling.getTileDimensions();
    int[] start = new int[] {tileCoords[ROWS]*tileDims[ROWS], 
      tileCoords[COLS]*tileDims[COLS]};
    int[] length = dataDims;

    switch (varType) {

    // Read byte data
    // --------------
    case CWF.CW_BYTE:
      byte[][] fileByteData = CWF.get_variable_byte (reader.getCWID(), 
        varIndex, start, length);
      byte[] byteData = (byte[]) data;
      for (int i = 0; i < length[0]; i++) {
        int offset = i*length[1];
        for (int j = 0; j < length[1]; j++) {
          byteData[offset + j] = fileByteData[i][j];
        } // for
      } // for
      break;

    // Read float data
    // ---------------
    case CWF.CW_FLOAT:
      float[][] fileFloatData = CWF.get_variable_float (reader.getCWID(), 
        varIndex, start, length);
      short[] shortData = (short[]) data;
      short missing = ((Short) getMissing()).shortValue();
      double scale = getScaling()[0];
      for (int i = 0; i < length[0]; i++) {
        int offset = i*length[1];
        for (int j = 0; j < length[1]; j++) {
          if (fileFloatData[i][j] == CWF.CW_BADVAL) {
            shortData[offset + j] = missing;
          } // if
          else {
            shortData[offset + j] = (short) Math.round (fileFloatData[i][j] /
              scale);
          } // else
        } // for
      } // for
      break;

    // Throw exception
    // ---------------
    default:
      throw (new IOException ("Unsupported variable data type"));

    } // switch

    // Return tile
    // -----------
    return (tiling.new Tile (pos, data));

  } // readTile

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the CWF dataset ID used by this cached grid.
   *
   * @return the CWF dataset ID wrapped in an <code>Integer</code>
   * object.
   */
  public Object getDataStream() { return (new Integer (reader.getCWID())); }

  ////////////////////////////////////////////////////////////

  /** Throws an exception as writing is not allowed with CWF files. */
  protected void writeTile (
    Tile tile
  ) throws IOException {

    throw (new IOException ("CWF file writing is not supported"));

  } // writeTile

  ////////////////////////////////////////////////////////////

  /** 
   * Performs a data cache flush only, since navigation correction
   * for CWF data is handled at the data access layer, not by the Java
   * layer.  Users of CWF cached grids should set the global file
   * navigation using a call to the
   * <code>CWFReader.updateNavigation()</code> method, and then call
   * this routine to flush the cache and force the data to be re-read
   * from the file with the new navigation transform.
   */
  public void setNavigation (AffineTransform nav) { 
    
    super.setNavigation (null);
    resetCache();

  } // setNavigation

  ////////////////////////////////////////////////////////////

} // CWFCachedGrid

////////////////////////////////////////////////////////////////////////
