////////////////////////////////////////////////////////////////////////
/*

     File: NCCachedGrid.java
   Author: Peter Hollemans
     Date: 2013/02/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2013 National Oceanic and Atmospheric Administration
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.HObject;
import noaa.coastwatch.io.CachedGrid;
import noaa.coastwatch.io.NCReader;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.util.Grid;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * The <code>NCCachedGrid</code> class is a cached grid that understands how to
 * read variable data from NetCDF files.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class NCCachedGrid
  extends CachedGrid {

  // Constants
  // ---------

  /** Default tile size in bytes. */
  public final static int DEFAULT_TILE_SIZE = 512*1024;

  /** Default cache size in bytes. */
  public final static int DEFAULT_CACHE_SIZE = (4*1024)*1024;

  // Variables
  // ---------
  
  /** NetCDF dataset. */
  private NetcdfDataset dataset;

  /** NetCDF chunking flag. */
  private boolean isChunked;

  /** NetCDF compression flag. */
  private boolean isCompressed;
  
  /** NetCDF variable class. */
  private Class varClass;
  
  /** The starting dimension indices with -1 as place holders for row/col. */
  private int[] start;
  
  /** The index of the row dimension. */
  private int rowIndex;
  
  /** The index of the column dimension. */
  private int colIndex;

  /** The rank of the NetCDF variable to read from. */
  private int varRank;
  
  /** The name of the NetCDF variable to read data from. */
  private String ncVarName;
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the data class.  This method overrides the parent because
   * the data class for an NC cached grid is not available from the
   * data array when needed in some cases.
   *
   * @return the Java class of the data array.
   */
  public Class getDataClass() { return (varClass); }

  ////////////////////////////////////////////////////////////

  public Object getDataStream() { return (dataset); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new read-only NetCDF cached grid with the specified
   * properties.  Unchunked NetCDF variables will be given the default
   * tile size, otherwise the tile size is determined from the file.
   *
   * @param grid the grid to use for attributes.  In this constructor, the
   * grid name is not used as the NetCDF variable name.
   * @param reader the NetCDF reader data source.
   * @param ncVarName the NetCDF variable name to read data from.
   * @param start the starting coordinates to read data from.  This array
   * should have the same rank as the NetCDF variable, and have values
   * filled in for all dimensions _except_ row and column, which should have
   * -1 as the value.  The row and column dimensions are assumed to be
   * adjacent to each other and row to be the first dimension, and then 
   * column to be the second dimension.  For example if the dimensions are
   * [time, level, row, column], then the start array could be [0, 0, -1, -1]
   * to select the first time and level index.
   *
   * @throws IOException if a problem occurred accessing the NetCDF file.
   * 
   * @see #NCCachedGrid(Grid,NCReader,int[])
   */
  public NCCachedGrid (
    Grid grid, 
    NCReader reader,
    String ncVarName,
    int[] start
  ) throws IOException {

    // Create cached grid
    // ------------------    
    super (grid, READ_ONLY);
    this.dataset = reader.getDataset();
    this.ncVarName = ncVarName;
    varClass = grid.getDataClass();
    setUnsigned (grid.getUnsigned());

    // Setup start and dimensions
    // --------------------------
    this.start = (int[]) start.clone();
    rowIndex = colIndex = -1;
    for (int i = 0; i < start.length; i++) {
      if (start[i] == -1) {
        if (rowIndex == -1) rowIndex = i;
        else if (colIndex == -1) colIndex = i;
      } // if
    } // for

    // Get variable info
    // -----------------
    isChunked = false;
    isCompressed = false;
    try {

      // Open as HDF file (only works for NetCDF 4)
      // ------------------------------------------
      FileFormat hdfFile = FileFormat.getInstance (reader.getSource());
      hdfFile.open();

      // Check rank
      // ----------
      Dataset hdfDataset = (Dataset) hdfFile.get (ncVarName);
      hdfDataset.getMetadata();
      varRank = hdfDataset.getRank();
      if (varRank != start.length)
        throw new IOException ("Rank of HDF variable does not match start spec");

      // Get chunking and compression
      // ----------------------------
      long[] chunkSize = hdfDataset.getChunkSize();
      if (chunkSize != null) {
        isChunked = true;
        int[] tileSize = new int[] {
          (int)chunkSize[rowIndex],
          (int)chunkSize[colIndex]
        };
        super.setTileDims (tileSize);
        super.setOptimizedCacheSize (DEFAULT_CACHE_SIZE);
      } // if
      String compression = hdfDataset.getCompression();
      isCompressed = !compression.startsWith ("NONE");

      // Close file
      // ----------
      hdfFile.close();

    } // try
    catch (Exception e) {
      throw new IOException (e.toString());
    } // catch
      
    // Set default tile and cache sizes
    // --------------------------------
    if (!isChunked) {
      setTileSize (DEFAULT_TILE_SIZE);
      setOptimizedCacheSize (DEFAULT_CACHE_SIZE);
    } // if

    // Check compressed tile size
    // --------------------------
    



    // TODO: Why don't we adjust the cache size in the next line to accomodate
    // 8 tiles?  That seems like the obvious answer here.  Did we try that
    // and get thrashing?  See the TODO note in the readTile method for more.


    
    
    
    if (isCompressed && isChunked) {
      if (getMaxTiles() == 1 || Arrays.equals (tiling.getTileDimensions(), getDimensions()))
        throw new IOException ("Compressed data chunk size too large in variable " + ncVarName);
    } // if

  } // NCCachedGrid constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new read-only NetCDF cached grid with the specified
   * properties.  Unchunked NetCDF variables will be given the default
   * tile size, otherwise the tile size is determined from the file.
   *
   * @param grid the grid to use for attributes.
   * @param reader the NetCDF reader data source.
   * @param start the starting coordinates to read data from.  This array
   * should have the same rank as the NetCDF variable, and have values
   * filled in for all dimensions _except_ row and column, which should have
   * -1 as the value.  The row and column dimensions are assumed to be
   * adjacent to each other and row to be the first dimension, and then 
   * column to be the second dimension.  For example if the dimensions are
   * [time, level, row, column], then the start array could be [0, 0, -1, -1]
   * to select the first time and level index.
   *
   * @throws IOException if a problem occurred accessing the NetCDF file.
   */
  public NCCachedGrid (
    Grid grid, 
    NCReader reader,
    int[] start
  ) throws IOException {

    this (grid, reader, grid.getName(), start);

  } // NCCachedGrid constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new read-only NetCDF cached grid with the specified
   * properties.  Unchunked NetCDF variables will be given the default
   * tile size, otherwise the tile size is determined from the file.  The 
   * NetCDF variable named by the grid must have rank 2.
   *
   * @param grid the grid to use for attributes.
   * @param reader the NetCDF reader data source.
   *
   * @throws IOException if a problem occurred accessing the NetCDF file.
   */
  public NCCachedGrid (
    Grid grid, 
    NCReader reader
  ) throws IOException {

    this (grid, reader, new int[] {-1, -1});
    
  } // NCCachedGrid constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new cache where each tile has the specified dimensions.
   * Only unchunked NetCDF variables may have the tile dimensions set.
   * For chunked variables, the method has no effect.
   *
   * @param dims the tile dimensions as [rows, columns].
   */
  public void setTileDims (
    int[] dims
  ) {

    if (!isChunked)
      super.setTileDims (dims);

  } // setTileDims

  ////////////////////////////////////////////////////////////

  protected Tile readTile (
    TilePosition pos
  ) throws IOException {

    // Access variable
    // ---------------
    Variable var = dataset.getReferencedFile().findVariable (ncVarName);
    if (var == null)
      throw new IOException ("Cannot access variable " + ncVarName);

    // Set read start
    // --------------
    int[] tileCoords = pos.getCoords();
    int[] tileDims = tiling.getTileDimensions();
    int[] readStart = (int[]) start.clone();
    readStart[rowIndex] = tileCoords[ROWS]*tileDims[ROWS];
    readStart[colIndex] = tileCoords[COLS]*tileDims[COLS];

    // Set read stride
    // ---------------
    int[] stride = new int[varRank];
    Arrays.fill (stride, 1);

    // Set read length
    // ---------------
    int[] dataDims = pos.getDimensions();
    int[] length = new int[varRank];
    Arrays.fill (length, 1);
    length[rowIndex] = dataDims[ROWS];
    length[colIndex] = dataDims[COLS];

    // Read data
    // ---------
    int[] readEnd = new int[varRank];
    for (int i = 0; i < varRank; i++)
      readEnd[i] = readStart[i] + stride[i]*length[i] - 1;
    StringBuffer section = new StringBuffer();
    for (int i = 0; i < varRank; i++) {
      section.append (readStart[i] + ":" + readEnd[i] + ":" + stride[i]);
      if (i != varRank-1) section.append (",");
    } // for
    Object data;



    /**
     * TODO: When we have this statement in, we can see that there are
     * many more accesses to, say, the lon variable than are needed.  For
     * example in one test, a chunk was accessed 52 times, when really
     * there are only 4 chunks in the file.  This makes a case for having a
     * new algorithm that could detect a large number of re-reads and adjust
     * the chunk cache size accordingly.  Another option is to have a coordinated
     * chunk caching system, such that variables whose chunks haven't been
     * used lately are released, to make way for variables whose chunks are
     * active.  We assume that variables are not getting "cleaned up"
     * because of the usage pattern, but that's not been verified.
     */

    //System.out.println ("name = " + ncVarName + ", section = " + section);



    try {
      data = var.read (section.toString()).getStorage();
    } // try
    catch (InvalidRangeException e) {
      throw new IOException ("Invalid section spec reading tile");
    } // catch

    // Return tile
    // -----------
    return (tiling.new Tile (pos, data));

  } // readTile

  ////////////////////////////////////////////////////////////

  protected void writeTile (
    Tile tile
  ) throws IOException {
  
    throw new UnsupportedOperationException();

  } // writeTile
  
  ////////////////////////////////////////////////////////////

  /** 
   * TESTING: Prints an indent to System.out for the indentation 
   * specified level.
   */
  private static void indent (int level) {

    for (int i = 0; i < level*4; i++)
      System.out.print (" ");

  } // indent

  ////////////////////////////////////////////////////////////

  /** 
   * TESTING: Traverses an HDF tree structure and prints out the node names,
   * classes, dimensions, and compression/chunking information.
   */
  private static void traverse (
    TreeNode node,
    int level
  ) throws Exception {

    // Print node name and class
    // -------------------------
    HObject obj = (HObject) ((DefaultMutableTreeNode) node).getUserObject();
    indent (level);
    System.out.println ("o " + node + " [" + obj.getClass() + "]");

    // Print dataset information
    // -------------------------
    if (obj instanceof Dataset) {

      // Print type
      // ----------
      Dataset dataset = (Dataset) obj;
      dataset.getMetadata();
      Datatype type = dataset.getDatatype();
      indent (level+1);
      System.out.println ("Datatype: " + type.getDatatypeDescription() +
        (type.isUnsigned() ? "(unsigned)" : ""));

      // Print dimensions
      // ----------------
      indent (level+1);
      System.out.print ("Dimensions: ");
      for (long dim : dataset.getDims()) System.out.print (dim + " ");
      System.out.println();
  
      // Print chunk size
      // ----------------
      long[] chunkSize = dataset.getChunkSize();
      if (chunkSize != null) {
        indent (level+1);
        System.out.print ("Chunk size: ");
        for (long len : chunkSize) System.out.print (len + " ");
        System.out.println();
      } // if

      // Print compression
      // -----------------
      String compression = dataset.getCompression();
      if (compression != null) {
        indent (level+1);
        System.out.println ("Compression: " + compression);
      } // if

    } // if

    // Traverse children
    // -----------------
    for (TreeNode child : Collections.list ((Enumeration<TreeNode>)node.children()))
      traverse (child, level+1);

  } // traverse

  ////////////////////////////////////////////////////////////

  /** 
   * TESTING: Traverses an HDF file's structure and prints out dataset 
   * information.
   */
  public static void main (String argv[]) throws Exception {
  
    FileFormat file = FileFormat.getInstance (argv[0]);
    file.open();
    traverse (file.getRootNode(), 0);
  
  } // main

  ////////////////////////////////////////////////////////////

} // NCCachedGrid class

////////////////////////////////////////////////////////////////////////
