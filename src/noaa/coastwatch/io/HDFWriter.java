////////////////////////////////////////////////////////////////////////
/*

     File: HDFWriter.java
   Author: Peter Hollemans
     Date: 2002/04/15

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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import hdf.hdflib.HDFChunkInfo;
import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFDeflateCompInfo;
import hdf.hdflib.HDFException;
import noaa.coastwatch.io.HDFLib;
import noaa.coastwatch.io.CachedGrid;
import noaa.coastwatch.io.EarthDataWriter;
import noaa.coastwatch.io.HDFSD;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.Line;
import noaa.coastwatch.util.MetadataServices;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An HDF writer is an earth data writer that writes HDF format
 * files using the HDF library class.  The HDF writer class is
 * abstract -- subclasses handle specific metadata variants.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class HDFWriter
  extends EarthDataWriter 
  implements HDFSD {

  private static final Logger LOGGER = Logger.getLogger (HDFWriter.class.getName());

  // Constants
  // ---------
  /** Default HDF chunk size in bytes. */
  public final static int DEFAULT_CHUNK_SIZE = 512*1024;

  // Variables
  // ---------
  /** HDF file id. */
  protected int sdid;

  /** HDF compression flag. */
  private boolean compressed;

  /** HDF variable chunked flag. */
  private boolean chunked;

  /** HDF variable chunk size in bytes. */
  private int chunkSize;

  /** Flag to signify that the file is closed. */
  protected boolean closed;

  /** The tile dimensions for 2D chunked variables. */
  private int[] tileDims;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the tile dimensions for grid variables passed to the
   * {@link #writeVariable} method. Setting the tile dimensions
   * explicitly overrides any chunk size set.
   *
   * @param tileDims the tile dimensions to use.
   *
   * @since 3.5.0
   */
  public void setTileDims (int[] tileDims) {
  
    this.tileDims = (tileDims == null ? null : (int[]) tileDims.clone());
    
  } // setTileDims

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current tile dimensions.
   *
   * @return the tile dimesions or null if the chunk size is being used
   * instead.
   *
   * @see #setChunkSize
   *
   * @since 3.5.0
   */
  public int[] getTileDims () { return (tileDims == null ? null : (int[]) tileDims.clone()); }

  ////////////////////////////////////////////////////////////

  public int getSDID () { return (sdid); }

  ////////////////////////////////////////////////////////////

  public String getFilename () { return (getDestination()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the HDF compression flag.  If compression is on, subsequent
   * calls to <code>flush</code> will cause variable data will be
   * written compressed.
   *
   * @param compressed the compression flag.
   *
   * @see #flush
   */
  public void setCompressed (boolean compressed) { 
    this.compressed = compressed; 
  } // setCompressed

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the HDF chunking flag.  If chunking is on, subsequent calls
   * to <code>flush</code> will cause variable data will be written in
   * a chunked form.
   *
   * @param chunked the chunking flag.
   *
   * @see #flush
   */
  public void setChunked (boolean chunked) { this.chunked = chunked; }

  ////////////////////////////////////////////////////////////

  /** Gets the HDF chunking flag. */
  public boolean getChunked () { return (chunked); }

  ////////////////////////////////////////////////////////////

  /** Gets the HDF compression flag. */
  public boolean getCompressed () { return (compressed); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the HDF type associated with a Java primitive class.
   *
   * @param c the Java primitive class.
   * @param isUnsigned the unsigned flag, true if unsigned type desired.
   *
   * @return the HDF data type.
   *
   * @throws ClassNotFoundException if a type cannot be found that
   * matches the Java class.
   */
  public static int getType (
    Class c,
    boolean isUnsigned
  ) throws ClassNotFoundException {    

    // Get unsigned type
    // -----------------
    if (isUnsigned) {
      if (c.equals (Byte.TYPE)) return (HDFConstants.DFNT_UINT8);
      else if (c.equals (Short.TYPE)) return (HDFConstants.DFNT_UINT16);
      else if (c.equals (Integer.TYPE)) return (HDFConstants.DFNT_UINT32);

      // The current HDF4 library doesn't support writing 64-bit integer 
      // data, even though it has data type constants that would indicate
      // otherwise.  The HDF5 library _does_ support 64-bit integer.
      
      // else if (c.equals (Long.TYPE)) return (HDFConstants.DFNT_UINT64);

      else throw new ClassNotFoundException (
        "Unsupported unsigned type class: " + c.getName());
    } // if

    // Get signed type
    // ---------------
    else {
      if (c.equals (Byte.TYPE)) return (HDFConstants.DFNT_INT8);
      else if (c.equals (Short.TYPE)) return (HDFConstants.DFNT_INT16);
      else if (c.equals (Integer.TYPE)) return (HDFConstants.DFNT_INT32);

      // See note above.
      // else if (c.equals (Long.TYPE)) return (HDFConstants.DFNT_INT64);

      else if (c.equals (Float.TYPE)) return (HDFConstants.DFNT_FLOAT32);
      else if (c.equals (Double.TYPE)) return (HDFConstants.DFNT_FLOAT64);
      else throw new ClassNotFoundException (
        "Unsupported signed type class: " + c.getName());
    } // else

    // TODO: Make sure that the reading software marks byte data as
    // unsigned if appropriate so that the old CDAT can handle it.

  } // getType

  ////////////////////////////////////////////////////////////

  /**
   * Sets an HDF attribute value.
   *
   * @param sdid the HDF scientific dataset ID.
   * @param name the attribute name.
   * @param value the attribute value.  If the attribute value is a
   * Java <code>String</code>, a character string is written.  If the
   * attribute is a Java primitive wrapped in an object, only one
   * value is written.  If the attribute value is a Java primitive
   * array, multiple values are written.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static void setAttribute (
    int sdid,
    String name,
    Object value
  ) throws HDFException, ClassNotFoundException {

    setAttribute (sdid, name, value, false);

  } // setAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Sets an HDF attribute value.
   *
   * @param sdid the HDF scientific dataset ID.
   * @param name the attribute name.
   * @param value the attribute value.  If the attribute value is a
   * Java <code>String</code>, a character string is written.  If the
   * attribute is a Java primitive wrapped in an object, only one
   * value is written.  If the attribute value is a Java primitive
   * array, multiple values are written.
   * @param isUnsigned the unsigned flag, true if the attribute is numeric
   * and should be written as unsigned, false otherwise.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   *
   * @since 3.6.1
   */
  public static void setAttribute (
    int sdid,
    String name,
    Object value,
    boolean isUnsigned
  ) throws HDFException, ClassNotFoundException {
  
    // Prepare string value
    // --------------------
    Object attValue;
    Class attClass;
    int attType;
    int attLength;
    if (value instanceof String) {
      attClass = value.getClass();
      attValue = ((String)value).getBytes();
      attType = HDFConstants.DFNT_CHAR8;
      attLength = ((String)value).length();
    } // if

    // Prepare numeric value
    // ---------------------
    else {
      if (!value.getClass().isArray())
        attValue = MetadataServices.toArray (value);
      else
        attValue = value;
      attClass = attValue.getClass().getComponentType();
      attType = getType (attClass, isUnsigned);
      attLength = Array.getLength (attValue);
    } // else

    // Check attribute length
    // ----------------------
    if (attLength == 0) return;
    
    // Determine attribute byte length
    // -------------------------------
    int typeBytes;
    switch (attType) {

      case HDFConstants.DFNT_CHAR8:
      case HDFConstants.DFNT_UINT8:
      case HDFConstants.DFNT_INT8:
        typeBytes = 1;
        break;

      case HDFConstants.DFNT_UINT16:
      case HDFConstants.DFNT_INT16:
        typeBytes = 2;
        break;

      case HDFConstants.DFNT_UINT32:
      case HDFConstants.DFNT_INT32:
      case HDFConstants.DFNT_FLOAT32:
        typeBytes = 4;
        break;

      case HDFConstants.DFNT_UINT64:
      case HDFConstants.DFNT_INT64:
      case HDFConstants.DFNT_FLOAT64:
        typeBytes = 8;
        break;

      default:
        throw new IllegalStateException ("Unknown length for HDF type " + attType);

    } // switch
    int attBytes = attLength*typeBytes;
    
    /**
     * We do the following check because the HDF library has a built
     * in limit of 65535 bytes for any attribute.  Currently there
     * is no sensible way to handle this except for trunction with a
     * warning.
     */
    if (attBytes > 65535) {
      attLength = 65535 / typeBytes;
      LOGGER.warning ("Writing attribute '" + name + "' requires " + attBytes + " bytes");
      LOGGER.warning ("HDF 4 library limit is 65535 bytes, truncating attribute to " + attLength + " values");
      LOGGER.warning ("Please note attribute truncation may cause data reading errors further downstream");
    } // if

    // Set attribute value
    // -------------------
    if (!HDFLib.getInstance().SDsetattr (sdid, name, attType, attLength, attValue)) {
      throw new HDFException ("Cannot set attribute value for '" + name + "'");
    } // if

  } // setAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Sets a number of attributes based on a map.
   *
   * @param sdid the HDF scientific dataset or variable ID for writing.
   * @param map the attribute map.
   * @param overwrite set to true to overwrite an attribute value that
   * already exists in the file, or false to leave existing attribute
   * values the same.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static void setAttributes (
    int sdid,
    Map map,
    boolean overwrite
  ) throws HDFException, ClassNotFoundException {

    // Loop over all attributes
    // ------------------------
    Object[] keys = map.keySet().toArray();
    for (int i = 0; i < keys.length; i++) {

      // Check for existing attribute value
      // ----------------------------------
      String attName = (String) keys[i];
      if (!overwrite) {
        int attIndex =  HDFLib.getInstance().SDfindattr (sdid, attName);
        if (attIndex != -1) {
          continue;
        } // if
      } // if

      // Set attribute value
      // -------------------
      Object attValue = map.get (attName);
      setAttribute (sdid, attName, attValue);

    } // for

  } // setAttributes

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the chunk size.  Future data writes will be performed by
   * writing chunks of the specified size.
   *
   * @param size the chunk size in bytes.
   */
  public void setChunkSize (int size) { chunkSize = size; }

  ////////////////////////////////////////////////////////////

  /** Gets the current chunk size in bytes. */
  public int getChunkSize () { return (chunkSize); }

  ////////////////////////////////////////////////////////////

  /**
   * Opens an existing HDF file for writing using the specified file
   * name.  Chunking and compression are off by default.  Note that a
   * writer created in this way has no <code>EarthDataInfo</code>
   * instance variable.
   *
   * @param file the HDF file name.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   */
  protected HDFWriter (
    String file
  ) throws HDFException {
    
    super (file);

    // Open file
    // ---------
    closed = true;
    sdid = HDFLib.getInstance().SDstart (file, HDFConstants.DFACC_WRITE);
    closed = false;

    // Set chunking and compression
    // ----------------------------
    chunkSize = DEFAULT_CHUNK_SIZE;
    chunked = false;
    compressed = false;

    // Set info
    // --------
    this.info = null;

  } // HDFWriter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new HDF file from the specified earth data info and
   * file name.  The HDF file is created but initial global metadata
   * not written.  Chunking and compression are off by default.
   *
   * @param info the earth data info object.
   * @param file the new HDF file name.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   *
   * @see #setGlobalInfo
   */
  protected HDFWriter (
    EarthDataInfo info,
    String file
  ) throws HDFException {

    super (file);

    // Create new file
    // ---------------
    closed = true;
    sdid = HDFLib.getInstance().SDstart (file, HDFConstants.DFACC_CREATE);
    closed = false;

    // Set chunking and compression
    // ----------------------------
    chunkSize = DEFAULT_CHUNK_SIZE;
    chunked = false;
    compressed = false;

    // Set info
    // --------
    this.info = info;

  } // HDFWriter

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the earth data info metadata.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred writing the file metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   * @throws UnsupportedEncodingException if the transform class encoding 
   * is not supported.
   */
  protected abstract void setGlobalInfo ()
    throws HDFException, IOException, ClassNotFoundException, 
    UnsupportedEncodingException;

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the data variable metadata.
   *
   * @param sdsid the variable HDF scientific dataset ID.
   * @param var the variable for which to write info.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred writing the file metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  protected abstract void setVariableInfo (
    int sdsid,
    DataVariable var
  ) throws HDFException, IOException, ClassNotFoundException;

  ////////////////////////////////////////////////////////////

  /**
   * Creates and writes an HDF variable.
   *
   * @param var the variable for writing.
   * @param chunked the chunking flag, true is chunking should be used.
   * @param compressed the compression flag, true if compression should
   * be used.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred writing the file metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  protected void writeVariable (
    DataVariable var,
    boolean chunked,
    boolean compressed
  ) throws HDFException, IOException, ClassNotFoundException {

    // Check for cached grid
    // ---------------------
    if (var instanceof CachedGrid) {
      CachedGrid cache = (CachedGrid) var;
      if (cache.getDataStream() == this) {
        cache.flush();
        return;
      } // if
    } // 

    // Check if variable exists
    // ------------------------
    int index = -1;
    String varName = var.getName ();
    try { index = HDFLib.getInstance().SDnametoindex (sdid, varName); }
    catch (Exception e) { }
    if (index != -1)
      throw new IOException ("Variable '" + varName + "' already exists");

    // Create variable
    // ---------------
    Class varClass = var.getDataClass();
    int varType = getType (varClass, var.getUnsigned());
    int[] dims = var.getDimensions();
    int sdsid = HDFLib.getInstance().SDcreate (sdid, varName, varType, dims.length, 
      dims);
    if (sdsid < 0)
      throw new HDFException ("Cannot create variable '" + varName + "'");
    setVariableInfo (sdsid, var);

    // Check rank
    // ----------
    if (!(var instanceof Line) && !(var instanceof Grid))
      throw new UnsupportedEncodingException (
        "Unsupported variable class for writing");
    if (chunked && !(var instanceof Grid))
      LOGGER.warning ("Chunking request ignored for '" + varName + "'");

    // Write chunked
    // -------------
    if (chunked && (var instanceof Grid)) {

      // Create tiling scheme
      // --------------------
      int[] tileDims = (this.tileDims != null ? this.tileDims : CachedGrid.getTileDims (chunkSize, (Grid) var));
      TilingScheme tiling = new TilingScheme (dims, tileDims);
      int values = tileDims[Grid.ROWS] * tileDims[Grid.COLS];

      // Set chunking
      // ------------
      setChunkCompress (sdsid, compressed, tileDims);

      // Loop over each tile
      // -------------------
      int[] tileCounts = tiling.getTileCounts();
      int tiles = tileCounts[Grid.ROWS]*tileCounts[Grid.COLS];
      for (int i = 0; i < tileCounts[Grid.ROWS]; i++) {
        for (int j = 0; j < tileCounts[Grid.COLS]; j++) {

          // Get tile data
          // -------------
          int[] dataStart = new int[] {i*tileDims[Grid.ROWS], j*tileDims[Grid.COLS]};
          int[] dataDims = tiling.getTilePositionForIndex (i, j).getDimensions();
          Object data = ((Grid) var).getData (dataStart, dataDims);
          if (dataDims[Grid.ROWS] != tileDims[Grid.ROWS] || 
            dataDims[Grid.COLS] != tileDims[Grid.COLS]) {
            Object newData = Array.newInstance (var.getDataClass(), values);
            Grid.arraycopy (data, dataDims, new int[] {0,0}, newData, 
              tileDims, new int[] {0,0}, dataDims);
            data = newData;
          } // if

          // Write tile data
          // ---------------
          int[] start = new int[] {i, j};
          if (!HDFLib.getInstance().SDwritechunk (sdsid, start, data))
            throw new HDFException ("Chunked write failed for '" +
              varName + "' at start = [" + start[0] + "," + start[1] + "]");

          // Set progress
          // ------------
          writeProgress = ((i*tileCounts[Grid.COLS] + (j+1))*100)/tiles;

          // Check for canceled
          // ------------------
          if (isCanceled) { 
            HDFLib.getInstance().SDendaccess (sdsid); 
            return;
          } // if

        } // for
      } // for

    } // if

    // Write compressed data
    // ---------------------
    else if (compressed) {

      // Set compression
      // ---------------
      setChunkCompress (sdsid, compressed, null);

      // Write data
      // ----------
      // NOTE: Have to write it all at once!
      Object data = var.getData();
      int[] start = new int[dims.length];
      Arrays.fill (start, 0);
      int[] stride = new int[dims.length];
      Arrays.fill (stride, 1);
      if (!HDFLib.getInstance().SDwritedata (sdsid, start, stride, dims, data))
        throw new HDFException ("Compressed write failed for '" + 
          varName + "'");

    } // else if

    // Write unchunked, uncompressed data
    // ----------------------------------
    else {

      // Write line data
      // ---------------
      if (var instanceof Line) {
        Object data = var.getData ();
        if (!HDFLib.getInstance().SDwritedata (sdsid, new int[] {0},
          new int[] {1}, new int[] {dims[0]}, data))
          throw new HDFException ("Write failed for '" + varName + "'");
      } // if

      // Write grid data
      // ---------------
      else if (var instanceof Grid) {
        int[] count = new int[] {1, dims[Grid.COLS]};
        int[] stride = new int[] {1, 1};

        // Loop over each row
        // ------------------
        for (int i = 0; i < dims[Grid.ROWS]; i++) {

          // Write row
          // ---------
          int[] start = new int[] {i, 0};
          Object data = ((Grid) var).getData (start, count);
          if (!HDFLib.getInstance().SDwritedata (sdsid, start, stride, count, data))
            throw new HDFException ("Write failed for '" +
              varName + "' at row " + i);

          // Set progress
          // ------------
          writeProgress = ((i+1)*100)/dims[Grid.ROWS];

          // Check for canceled
          // ------------------
          if (isCanceled) { 
            HDFLib.getInstance().SDendaccess (sdsid); 
            return;
          } // if

        } // for
      } // else if

    } // else

    // End access
    // ----------
    HDFLib.getInstance().SDendaccess (sdsid);

  } // writeVariable  

  ////////////////////////////////////////////////////////////
  
  public void flush () throws IOException {

    // Check for canceled
    // ------------------
    if (isCanceled) return;

    // Initialize progress counters
    // ----------------------------
    synchronized (this) {
      writeProgress = 0;
      writeVariables = 0;
    } // synchronized

    // Loop over each variable
    // -----------------------
    while (variables.size() != 0) {

      // Write variable
      // --------------
      DataVariable var = (DataVariable) variables.remove (0);
      writeVariableName = var.getName();
      try { writeVariable (var, chunked, compressed); }
      catch (Exception e) { 
        //        e.printStackTrace();
        throw new IOException (e.getMessage()); 
      } // catch

      // Update progress
      // ---------------
      synchronized (this) {
        writeProgress = 0;
        writeVariables++;
      } // synchronized

      // Check for canceled
      // ------------------
      if (isCanceled) return;

    } // while

  } // flush

  ////////////////////////////////////////////////////////////

  /**
   * Sets the chunking and compression for an SDS HDF variable.  
   * 
   * @param sdsid the HDF SDS variable ID.
   * @param compressed the compression flag, true if compression should
   * be used.  Compression is set to GZIP at level 6.
   * @param chunk_lengths the chunk lengths, one for each dimension.
   * If null, no chunking if performed.
   * 
   * @throws HDFException if an HDF error occurred.
   */
  public static void setChunkCompress (
    int sdsid,
    boolean compressed,
    int[] chunk_lengths
  ) throws HDFException {

    // Set up compression
    //  -----------------
    HDFDeflateCompInfo compInfo = null;
    if (compressed) {
      compInfo = new HDFDeflateCompInfo();
      compInfo.level = 6;
      if (chunk_lengths == null) {
        boolean success = HDFLib.getInstance().SDsetcompress (sdsid, HDFConstants.COMP_CODE_DEFLATE, compInfo);
        if (!success) throw new HDFException ("SDsetcompress call failed");
      } // if
    } // if
    
    // Set up chunking
    // ---------------
    if (chunk_lengths != null) {
      int flags;
      /**
       * We convert to the HDF library friendly version of chunk lengths here,
       * since the library expects to see a full array up to MAX_VAR_DIMS in
       * size, and can segfault in the native code when copying values if we
       * don't provide the expected length.
       */
      int[] libChunkLengths = Arrays.copyOf (chunk_lengths, HDFConstants.MAX_VAR_DIMS);
      HDFChunkInfo chunkInfo;
      if (!compressed) {
        flags = HDFConstants.HDF_CHUNK;
        chunkInfo = new HDFChunkInfo (libChunkLengths);
      } // if
      else {
        flags = HDFConstants.HDF_CHUNK | HDFConstants.HDF_COMP;
        chunkInfo = new HDFChunkInfo (libChunkLengths, HDFConstants.COMP_CODE_DEFLATE, compInfo);
      } // if
      boolean success = false;
      try  { success = HDFLib.getInstance().SDsetchunk (sdsid, chunkInfo, flags); }
      catch (Throwable e) { }
      if (!success) throw new HDFException ("SDsetchunk call failed");
    } // if

  } // setChunkCompress

  ////////////////////////////////////////////////////////////

  public void close () throws IOException {

    // Flush and close
    // ---------------
    if (closed) return;
    flush();
    try {
      if (!HDFLib.getInstance().SDend (sdid)) 
        throw new HDFException ("Failed to end access");
    } // try
    catch (HDFException e) { 
      throw new IOException (e.getMessage()); 
    } // catch
    closed = true;

  } // close

  ////////////////////////////////////////////////////////////

} // HDFWriter class

////////////////////////////////////////////////////////////////////////
