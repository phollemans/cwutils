////////////////////////////////////////////////////////////////////////
/*
     FILE: NCTileSource.java
   AUTHOR: Peter Hollemans
     DATE: 2014/07/30
  CHANGES: 2014/09/09, PFH
           - Changes: Updated to use FileFormat.createInstance (String, int).
           - Issue: A user reported an issue on opening read-only files:
 
             "ncsa.hdf.hdf5lib.exceptions.HDF5Exception: Cannot write file, 
             try open as read-only"
 
             This was because we were using getInstance (String) followed by 
             open(), rather than createInstance.  getInstance defaults to
             read/write access, where as createInstance allows read-only
             to be specified.

  CoastWatch Software Library and Utilities
  Copyright 2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io.tile;

// Imports
// -------
import java.io.*;
import java.util.*;
import noaa.coastwatch.io.tile.TilingScheme.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.ma2.*;
import ncsa.hdf.object.*;
import ncsa.hdf.object.h5.*;

/**
 * The <code>NCTileSource</code> class provides tiles from a NetCDF 3 or 4
 * dataset variable.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class NCTileSource
  implements TileSource {

  // Constants
  // ---------
  
  /** The default tile size for unchunked data. */
  private static final int DEFAULT_UNCHUNKED_SIZE = 512;

  // Variables
  // ---------
  
  /** The NetCDF variable to read. */
  private Variable var;
  
  /** The index into the starting coordinates of the rows dimension. */
  private int rowIndex;
  
  /** The index into the starting coordinates of the columns dimension. */
  private int colIndex;
  
  /** The starting coordinates of the 2D slice. */
  private int[] start;

  /** The tiling scheme used by this source. */
  private TilingScheme scheme;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new NetCDF tile source to access the specified dataset
   * and variable.
   *
   * @param file the NetCDF file to access.
   * @param varName the NetCDF variable name to read.
   * @param rowIndex the dimension index to use as the row dimension of the 2D slice.
   * @param colIndex the dimension index to use as the column dimension of the 2D slice.
   * @param start the starting coordinates for the 2D slice, or null to start at
   * the origin.
   *
   * @throws IOException if a problem occurred accessing the NetCDF file.
   * @throws ArrayIndexOutOfBoundsException if the row or column index is
   * out of bounds with respect to the variable dimensions.
   * @throws IllegalArgumentException if the start coordinate dimensions do
   * not match the variable.
   */
  public NCTileSource (
    NetcdfFile file,
    String varName,
    int rowIndex,
    int colIndex,
    int[] start
  ) throws IOException {

    // Get NetCDF variable
    // -------------------
    var = file.findVariable (varName);
    if (var == null)
      throw new IOException ("Cannot access variable " + varName);

    // Create scheme dimensions
    // ------------------------
    this.rowIndex = rowIndex;
    this.colIndex = colIndex;
    int[] shape = var.getShape();
    int[] dims = new int[] {shape[rowIndex], shape[colIndex]};

    // Set start coordinates
    // ---------------------
    if (start == null)
      this.start = new int[shape.length];
    else {
      if (start.length != shape.length)
        throw new IllegalArgumentException ("Start coordinate array length does not match variable");
      this.start = (int[]) start.clone();
    } // else
    
    // Check for HDF 5 format
    // ----------------------
    FileFormat h5Format = FileFormat.getFileFormat (FileFormat.FILE_TYPE_HDF5);
    String location = file.getLocation();
    boolean isH5 = h5Format.isThisType (location);

    // Get chunking and compression data
    // ---------------------------------
    boolean isCompressed = false;
    long[] chunkSize = null;
    if (isH5) {

      try {

        // Open as HDF 5
        // -------------
        FileFormat hdfFile = h5Format.createInstance (location, FileFormat.READ);
        hdfFile.open();

        // Get chunk dimensions
        // --------------------
        Dataset hdfDataset = (Dataset) hdfFile.get (varName);
        hdfDataset.getMetadata();
        chunkSize = hdfDataset.getChunkSize();

        // Get compression
        // ---------------
        String compression = hdfDataset.getCompression();
        isCompressed = !compression.startsWith ("NONE");

        // Close file
        // ----------
        hdfFile.close();

      } // try
      catch (Exception e) {
        throw new IOException (e.toString());
      } // catch
      
    } // if
    
    // Set tile dimensions
    // -------------------
    int[] tileDims;
    if (chunkSize != null) {
    
      /** 
       * In this case we set the tile dimensions from the chunk size
       * directly.
       */
      tileDims = new int[] {
        (int) chunkSize[rowIndex],
        (int) chunkSize[colIndex]
      };
      
    } // if
    else {

      /**
       * What we've found here is a variable that is compressed in entirety
       * and not chunked.  So we have no choice but to decompress the whole 
       * 2D slice as one tile.
       */
      if (isCompressed) {
        tileDims = (int[]) dims.clone();
      } // if
      
      /**
       * In this other case, the data is not compressed and it's also not
       * chunked, so we choose an arbitrary tile size.
       */
      else {
        tileDims = new int[] {
          Math.min (dims[0], DEFAULT_UNCHUNKED_SIZE),
          Math.min (dims[1], DEFAULT_UNCHUNKED_SIZE)
        };
      } // else
    
    } // else
    
    // Create tiling scheme
    // --------------------
    scheme = new TilingScheme (dims, tileDims);

  } // NCCachedGrid constructor

  ////////////////////////////////////////////////////////////

  @Override
  public Tile readTile (
    TilePosition pos
  ) throws IOException {
  
    // Check tiling scheme
    // -------------------
    if (pos.getScheme() != scheme)
      throw new IllegalArgumentException ("Position tiling scheme does not match");

    // Create read start array
    // -----------------------
    int[] tileCoords = pos.getCoords();
    int[] schemeTileDims = scheme.getTileDimensions();
    int[] start = (int[]) this.start.clone();
    start[rowIndex] = tileCoords[TilingScheme.ROWS]*schemeTileDims[TilingScheme.ROWS];
    start[colIndex] = tileCoords[TilingScheme.COLS]*schemeTileDims[TilingScheme.COLS];

    // Create read length array
    // ------------------------
    int[] tileDims = pos.getDimensions();
    int[] length = new int[start.length];
    Arrays.fill (length, 1);
    length[rowIndex] = tileDims[TilingScheme.ROWS];
    length[colIndex] = tileDims[TilingScheme.COLS];

    // Read data
    // ---------
    Object data;
    try { data = var.read (start, length).getStorage(); }
    catch (InvalidRangeException e) {
      throw new IOException ("Invalid start/length reading tile");
    } // catch

    return (scheme.new Tile (pos, data));

  } // readTile

  ////////////////////////////////////////////////////////////

  @Override
  public TilingScheme getScheme() { return (scheme); }

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    /**
     * First we create a small NetCDF 3 dataset to test with.  Example
     * writing code with notes on chunking are found here (although compression
     * and chunking can actually only be done with NetCDF 4):
     *
     * http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/tutorial/NetcdfWriting.html
     * http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/netcdf4Clibrary.html
     *
     */

    String testNC3 = "/tmp/test.nc";
    String testNC4 = "/tmp/test.nc4";
    NetcdfFileWriter writer = NetcdfFileWriter.createNew (NetcdfFileWriter.Version.netcdf3, testNC3, null);
    int times = 2;
    int levels = 4;
    int rows = (int) (DEFAULT_UNCHUNKED_SIZE * 1.2);
    int cols = (int) (DEFAULT_UNCHUNKED_SIZE * 1.5);
    Dimension timeDim = writer.addDimension (null, "time", times);
    Dimension levelDim = writer.addDimension (null, "level", levels);
    Dimension rowDim = writer.addDimension (null, "row", rows);
    Dimension colDim = writer.addDimension (null, "col", cols);
    List<Dimension> dimsList = new ArrayList<Dimension>();
    dimsList.add (timeDim);
    dimsList.add (levelDim);
    dimsList.add (rowDim);
    dimsList.add (colDim);
    Variable var = writer.addVariable (null, "testField", DataType.DOUBLE, dimsList);
    writer.create();

    int[] shape = var.getShape();
    ArrayDouble varData = new ArrayDouble.D4 (shape[0], shape[1], shape[2], shape[3]);
    Index index = varData.getIndex();
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        for (int k = 0; k < shape[2]; k++) {
          for (int l = 0; l < shape[3]; l++) {
            varData.setDouble (index.set (i, j, k, l), (double) (k*1000000 + l*1000));
          } // for
        } // for
      } // for
    } // for
    int[] origin = new int[4];
    writer.write (var, origin, varData);
    writer.close();

    /**
     * What we do here is test NetCDF 4 format by converting the file we
     * created to NetCDF 4 using the external nccopy tool, which comes as
     * part of the NetCDF software distribution.  As of this note, we're 
     * using NetCDF 4.3.2 on Mac OS X for testing.
     */

    String command;
    command = "/opt/local/bin/nccopy";
    command += " -k 3";
    command += " -d 6";
    command += " -c time/1,level/1,row/" + (DEFAULT_UNCHUNKED_SIZE-2) + ",col/" + (DEFAULT_UNCHUNKED_SIZE-2);
    command += " " + testNC3 + " " + testNC4;
    Process process = Runtime.getRuntime().exec (command);
    process.waitFor();
    assert (process.exitValue() == 0);

    /**
     * Now we can do some actual testing.  This is what the scheme should look
     * like:
     *
     *     0    1 
     *   +-----+----  \
     * 0 |     |      |  512
     *   |     |      |
     *   +-----+----  x
     * 1 |     |      /  102
     *
     *   \-----x----/
     *     512   256
     */

    int chunkSize = DEFAULT_UNCHUNKED_SIZE;
    for (String location : new String[] {testNC3, testNC4}) {

      System.out.print ("Testing constructor, getScheme (" + location + ") ... ");
      NetcdfFile file = NetcdfFile.open (location);

      try {
        TileSource source = new NCTileSource (file, "testField2", 2, 3, null);
        assert (false);
      } // try
      catch (IOException e) { }

      try {
        TileSource source = new NCTileSource (file, "testField", 3, 4, null);
        assert (false);
      } // try
      catch (ArrayIndexOutOfBoundsException e) { }

      try {
        TileSource source = new NCTileSource (file, "testField", 2, 3, new int[] {0, 0, 0, 0, 0});
        assert (false);
      } // try
      catch (IllegalArgumentException e) { }

      TileSource source = new NCTileSource (file, "testField", 2, 3, new int[] {1, 1, 0, 0});
      TilingScheme scheme = source.getScheme();
      int[] dims = scheme.getDimensions();
      assert (dims[0] == rows);
      assert (dims[1] == cols);
      int[] tileDims = scheme.getTileDimensions();
      assert (tileDims[0] == chunkSize);
      assert (tileDims[1] == chunkSize);
      int[] posDims = scheme.new TilePosition (0, 0).getDimensions();
      assert (posDims[0] == chunkSize);
      assert (posDims[1] == chunkSize);
      posDims = scheme.new TilePosition (1, 1).getDimensions();
      assert (posDims[0] == (rows - chunkSize));
      assert (posDims[1] == (cols - chunkSize));
      System.out.println ("OK");
      
      System.out.print ("Testing readTile ...");

      Tile tile = source.readTile (scheme.new TilePosition (0, 0));
      Object data = tile.getData();
      int dataLength = java.lang.reflect.Array.getLength (data);
      assert (dataLength == chunkSize*chunkSize);
      int i = 0;
      int j = 0;
      assert (java.lang.reflect.Array.getDouble (data, 0) == (i*1000000 + j*1000));
      i = chunkSize-1;
      j = chunkSize-1;
      assert (java.lang.reflect.Array.getDouble (data, dataLength-1) == (i*1000000 + j*1000));
      
      tile = source.readTile (scheme.new TilePosition (1, 1));
      data = tile.getData();
      dataLength = java.lang.reflect.Array.getLength (data);
      assert (dataLength == (rows - chunkSize)*(cols - chunkSize));
      i = chunkSize;
      j = chunkSize;
      assert (java.lang.reflect.Array.getDouble (data, 0) == (i*1000000 + j*1000));
      i = rows - 1;
      j = cols - 1;
      assert (java.lang.reflect.Array.getDouble (data, dataLength-1) == (i*1000000 + j*1000));
      
      TilingScheme otherScheme = new TilingScheme (new int[] {10, 10}, new int[] {2, 2});
    
      try {
        tile = source.readTile (otherScheme.new TilePosition (1, 1));
        assert (false);
      } // try
      catch (IllegalArgumentException e) { }

      file.close();
      new File (location).delete();
      assert (!new File (location).exists());

      System.out.println ("OK");

      /**
       * Here we change the chunk size by a small amount so that we know we're
       * actually reading the chunks from the NetCDF 4 file and not using the
       * default value.
       */
      chunkSize = chunkSize - 2;

    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // NCTileSource class

////////////////////////////////////////////////////////////////////////