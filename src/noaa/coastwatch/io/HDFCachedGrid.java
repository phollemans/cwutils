////////////////////////////////////////////////////////////////////////
/*

     File: HDFCachedGrid.java
   Author: Peter Hollemans
     Date: 2002/06/18

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFException;
import noaa.coastwatch.io.HDFLib;
import noaa.coastwatch.io.CachedGrid;
import noaa.coastwatch.io.HDFReader;
import noaa.coastwatch.io.HDFSD;
import noaa.coastwatch.io.HDFWriter;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.util.Grid;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The HDF cached grid class is a cached grid that understands how to
 * read variable data from HDF files.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class HDFCachedGrid
  extends CachedGrid {

  private static final Logger LOGGER = Logger.getLogger (HDFCachedGrid.class.getName());

  // Constants
  // ---------
  /** Default tile size in bytes. */
  public final static int DEFAULT_TILE_SIZE = 512*1024;

  /** Default cache size in bytes. */
  public final static int DEFAULT_CACHE_SIZE = (4*1024)*1024;

  // Variables
  // ---------
  
  /** HDF dataset. */
  private HDFSD dataset;

  /** HDF variable class. */
  private Class varClass;

  /** HDF variable index. */
  private int varIndex;

  /** HDF chunking flag. */
  private boolean chunked;

  /** HDF compression flag. */
  private boolean compressed;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data class.  This method overrides the parent because
   * the data class for an HDF cached grid is not available from the
   * data array when needed in some cases.
   *
   * @return the Java class of the data array.
   */
  public Class getDataClass() { return (varClass); }

  ////////////////////////////////////////////////////////////

  public Object getDataStream() { return (dataset); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new read-only HDF cached grid with the specified
   * properties.  Unchunked HDF variables will be given the default
   * tile size, otherwise the tile size is determined from the file.
   *
   * @param grid the grid to use for attributes.
   * @param reader the HDF reader data source.
   *
   * @throws IOException if a problem occurred accessing the HDF file.
   */
  public HDFCachedGrid (
    Grid grid, 
    HDFReader reader
  ) throws IOException {

    // Create cached grid
    // ------------------    
    super (grid, READ_ONLY);
    this.dataset = reader;

    // Get variable info
    // -----------------
    try {

      // Get variable index
      // ------------------
      varIndex = HDFLib.getInstance().SDnametoindex (dataset.getSDID(), getName()); 
      if (varIndex < 0) 
        throw new HDFException ("Cannot get index for variable " + getName());

      // Access variable
      // ---------------
      int sdsid = HDFLib.getInstance().SDselect (dataset.getSDID(), varIndex);
      if (sdsid < 0)
        throw new HDFException ("Cannot access variable " + getName());

      // Get variable type
      // -----------------
      String[] nameArr = new String[] {""};
      int dimArr[] = new int[HDFConstants.MAX_VAR_DIMS];
      int varInfo[] = new int[3];
      if (!HDFLib.getInstance().SDgetinfo (sdsid, nameArr, dimArr, varInfo))
        throw new HDFException ("Cannot get variable info for " + getName());
      int varType = varInfo[1];
      varClass = HDFReader.getClass (varType);
      setUnsigned (grid.getUnsigned());

      // Get chunk information
      // ---------------------
      int[] chunk_lengths;
      /** 
       * We try to read the chunk lengths here.  For an HDF file this
       * should always return normally.  But for a netCDF file, it
       * seems that this call fails.  If that's the case, we assume
       * that we are dealing with a netCDF file and so there are no
       * chunks.
       */
      try { chunk_lengths = HDFReader.getChunkLengths (sdsid); }
      catch (HDFException e) { chunk_lengths = null; }
      if (chunk_lengths == null)
        chunked = false;
      else {
        chunked = true;     
        super.setTileDims (chunk_lengths);
        super.setOptimizedCacheSize (DEFAULT_CACHE_SIZE);

        // super.setCacheSize (DEFAULT_CACHE_SIZE);
        // super.setDynamic (true);

      } // else

      // End access
      // ----------
      HDFLib.getInstance().SDendaccess (sdsid);

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

    // Set tile and cache sizes
    // ------------------------
    if (!chunked) {
      super.setTileSize (DEFAULT_TILE_SIZE);
      super.setOptimizedCacheSize (DEFAULT_CACHE_SIZE);

      // super.setCacheSize (DEFAULT_CACHE_SIZE);
      // super.setDynamic (true);

    } // if

  } // HDFCachedGrid constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new read-write HDF cached grid with the specified
   * properties.  Unchunked HDF files will be cached using the default
   * tile size, otherwise the tile size is determined from the file.
   *
   * @param grid the grid to use for attributes.
   * @param writer the HDF writer data destination.
   *
   * @throws IOException if a problem occurred accessing the HDF file.
   */
  public HDFCachedGrid (
    Grid grid, 
    HDFWriter writer
  ) throws IOException {

    // Create cached grid
    // ------------------    
    super (grid, READ_WRITE);
    this.dataset = writer;

    // Create variable
    // ---------------
    try {

      // Check if variable exists
      // ------------------------
      int index = -1;
      String varName = getName();
      int sdid = dataset.getSDID();
      try { index = HDFLib.getInstance().SDnametoindex (sdid, varName); }
      catch (Exception e) { }
      if (index != -1)
        throw new IOException ("Variable '" + varName + "' already exists");

      // Create variable
      // ---------------
      varClass = grid.getDataClass();
      boolean isUnsigned = grid.getUnsigned();
      setUnsigned (isUnsigned);
      int varType = HDFWriter.getType (varClass, isUnsigned);
      int sdsid = HDFLib.getInstance().SDcreate (sdid, varName, varType, dims.length, 
        dims);
      if (sdsid < 0)
        throw new HDFException ("Cannot create variable '" + varName + "'");
      writer.setVariableInfo (sdsid, grid);

      // Get variable index
      // ------------------
      varIndex = HDFLib.getInstance().SDnametoindex (sdid, varName);
      if (varIndex < 0) 
        throw new HDFException ("Cannot get index for variable " + varName);

      // Set chunking and compression
      // ----------------------------
      chunked = writer.getChunked();
      compressed = writer.getCompressed();
      if (chunked || compressed) {
        int[] tileDims = null;
        if (chunked) {
          tileDims = writer.getTileDims();
          if (tileDims == null)
            tileDims = CachedGrid.getTileDims (writer.getChunkSize(), grid);
          super.setTileDims (tileDims);
          super.setOptimizedCacheSize (DEFAULT_CACHE_SIZE);

          // super.setCacheSize (DEFAULT_CACHE_SIZE);
          // super.setDynamic (true);

        } // if
        HDFWriter.setChunkCompress (sdsid, compressed, tileDims);
      } // if

      // End access
      // ----------
      HDFLib.getInstance().SDendaccess (sdsid);

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

    // Set tile and cache sizes
    // ------------------------
    if (!chunked) {
      super.setTileSize (DEFAULT_TILE_SIZE);
      super.setOptimizedCacheSize (DEFAULT_CACHE_SIZE);

      // super.setCacheSize (DEFAULT_CACHE_SIZE);
      // super.setDynamic (true);

    } // if

    // Add this grid to the writer
    // ---------------------------
    writer.addVariable (this);

  } // HDFCachedGrid

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache where each tile has the specified dimensions.
   * Only unchunked HDF variables may have the tile dimensions set.
   * For chunked variables, the method has no effect.
   *
   * @param dims the tile dimensions as [rows, columns].
   */
  public void setTileDims (
    int[] dims
  ) {

    if (!chunked)
      super.setTileDims (dims);

  } // setTileDims

  ////////////////////////////////////////////////////////////

  protected Tile readTile (
    TilePosition pos
  ) throws IOException {

    try {

      // Access variable
      // ---------------
      int sdsid = HDFLib.getInstance().SDselect (dataset.getSDID(), varIndex);
      if (sdsid < 0)
        throw new HDFException ("Cannot access variable at index " + varIndex);

      // Read unchunked data
      // -------------------
      Object data;
      if (!chunked) {

        // Create data array
        // -----------------
        int[] dataDims = pos.getDimensions();
        int dataValues = dataDims[ROWS]*dataDims[COLS];
        data = Array.newInstance (varClass, dataValues);

        // Read data array
        // ---------------
        int[] tileCoords = pos.getCoords();
        int[] tileDims = tiling.getTileDimensions();
        int[] start = new int[] {tileCoords[ROWS]*tileDims[ROWS], 
          tileCoords[COLS]*tileDims[COLS]};
        int[] stride = new int[] {1, 1};
        int[] length = dataDims;
        if (!HDFLib.getInstance().SDreaddata (sdsid, start, stride, length, data))
          throw new HDFException ("Cannot read tile data for " + getName());

      } // if

      // Read chunked data
      // -----------------
      else {

        // Create data array
        // -----------------
        int[] tileDims = tiling.getTileDimensions();
        int values = tileDims[ROWS]*tileDims[COLS];
        data = Array.newInstance (varClass, values);

        // Read data chunk
        // ---------------
        int[] start = pos.getCoords();
        if (!HDFLib.getInstance().SDreadchunk (sdsid, start, data))
          throw new HDFException ("Cannot read tile data for " + getName());

        // Rearrange truncated tile data
        // -----------------------------
        int[] dataDims = pos.getDimensions();
        if (dataDims[ROWS] != tileDims[ROWS] || 
          dataDims[COLS] != tileDims[COLS]) {
          int dataValues = dataDims[ROWS] * dataDims[COLS];
          Object newData = Array.newInstance (getDataClass(), dataValues);
          Grid.arraycopy (data, tileDims, new int[] {0,0}, newData, 
            dataDims, new int[] {0,0}, dataDims);
          data = newData;
        } // if

      } // else

      // End access
      // ----------
      HDFLib.getInstance().SDendaccess (sdsid);

      // Return tile
      // -----------
      return (tiling.new Tile (pos, data));

    } // try

    catch (Exception e) {
      LOGGER.warning ("Error getting data tile at " + pos + " for " + getName());
      throw new IOException (e.getMessage());
    } // catch

  } // readTile

  ////////////////////////////////////////////////////////////

  protected void writeTile (
    Tile tile
  ) throws IOException {

    // Check access mode
    // -----------------
    if (accessMode == READ_ONLY)
      throw new IOException ("Cannot write tile to read-only dataset");

    try {

      // Access variable
      // ---------------
      int sdsid = HDFLib.getInstance().SDselect (dataset.getSDID(), varIndex);
      if (sdsid < 0)
        throw new HDFException ("Cannot access variable at index " + varIndex);

      // Write unchunked data
      // --------------------
      if (!chunked) {

        // Check for compression
        // ---------------------
        if (compressed)
          throw new IOException ("Cannot write tile to compressed, unchunked variable");

        // Write tile data
        // ---------------
        int[] tileCoords = tile.getPosition().getCoords();
        int[] tileDims = tiling.getTileDimensions();
        int[] start = new int[] {tileCoords[ROWS]*tileDims[ROWS], 
          tileCoords[COLS]*tileDims[COLS]};
        int[] stride = new int[] {1, 1};
        int[] count = tile.getDimensions();
        Object data = tile.getData();
        if (!HDFLib.getInstance().SDwritedata (sdsid, start, stride, count, data))
          throw new HDFException ("Cannot write tile data for " + getName());

      } // if

      // Write chunked data
      // ------------------
      else {

        // Get tile data
        // -------------
        int[] start = tile.getPosition().getCoords();
        int[] tileDims = tiling.getTileDimensions();
        int[] dataDims = tile.getDimensions();
        Object data = tile.getData();
        if (dataDims[Grid.ROWS] != tileDims[Grid.ROWS] || 
          dataDims[Grid.COLS] != tileDims[Grid.COLS]) {
          int values = tileDims[Grid.ROWS] * tileDims[Grid.COLS];
          Object newData = Array.newInstance (varClass, values);
          Grid.arraycopy (data, dataDims, new int[] {0,0}, newData, 
            tileDims, new int[] {0,0}, dataDims);
          data = newData;
        } // if

        // Write tile data
        // ---------------
        if (!HDFLib.getInstance().SDwritechunk (sdsid, start, data))
          throw new HDFException ("Cannot write tile data for " + getName());

      } // else

      // End access
      // ----------
      HDFLib.getInstance().SDendaccess (sdsid);
      tile.setDirty (false);

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

  } // writeTile

  ////////////////////////////////////////////////////////////

} // HDFCachedGrid class

////////////////////////////////////////////////////////////////////////
