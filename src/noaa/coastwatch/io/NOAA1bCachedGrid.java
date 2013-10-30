////////////////////////////////////////////////////////////////////////
/*
     FILE: NOAA1bCachedGrid.java
  PURPOSE: A subclass of CachedGrid that works with NOAA1b files.
   AUTHOR: Peter Hollemans
     DATE: 2003/02/19
  CHANGES: 2003/04/10, PFH, added longitude filtering for navigation 
             interpolation
           2004/10/07, PFH, modified to use setOptimizedCacheSize()
           2005/07/28, PFH, modified to use line caching rather than 
             tile caching (speeds up subsampled views considerably)
           2005/09/21, PFH, moved longitude filtering to NOAA1bReader
           2006/06/21, PFH, added support for missing scan lines
           2006/12/27, PFH, added scan line time data reading

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.text.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.TilingScheme.*;
import noaa.coastwatch.io.NOAA1bReader.*;

/**
 * The NOAA1b cached grid class is a cached grid that understands how
 * to read variable data from NOAA1b files.
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class NOAA1bCachedGrid
  extends CachedGrid {

  // Constants
  // ---------

  /** Default cache size in bytes. */
  public final static int DEFAULT_CACHE_SIZE = (4*1024)*1024;

  /** Variable type for channels. */
  private final static int VARIABLE_TYPE_CHANNEL = 0;

  /** Variable type for navigation. */
  private final static int VARIABLE_TYPE_NAVIGATION = 1;

  /** Variable type for cloud. */
  private final static int VARIABLE_TYPE_CLOUD = 2;

  /** Variable type for scan time. */
  private final static int VARIABLE_TYPE_SCAN_TIME = 3;

  // Variables
  // ---------
  /** The NOAA1b reader. */
  private NOAA1bReader reader;

  /** The variable type flag. */
  private int variableType;

  /** The channel variable number. */
  private int channelVariable;

  /** The channel 3a flag. */
  private boolean channel3a;

  /** The navigation variable. */
  private int navigationVariable;

  /** The variables class. */
  private Class varClass;

  /** The IR channel flag. */
  private boolean isIRChannel;

  ////////////////////////////////////////////////////////////

  public Class getDataClass() { return (varClass); }

  ////////////////////////////////////////////////////////////

  public Object getDataStream() { return (reader); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new read-only NOAA1b cached grid with the specified
   * properties.
   *
   * @param grid the grid to use for attributes.
   * @param reader the HDF reader data source.
   *
   * @throws IOException if a problem occurred accessing the NOAA1b file.
   */
  public NOAA1bCachedGrid (
    Grid grid, 
    NOAA1bReader reader
  ) throws IOException {

    // Create cached grid
    // ------------------    
    super (grid, READ_ONLY);
    this.reader = reader;

    // Set variable information
    // ------------------------
    String varName = grid.getName();
    if (varName.equals ("avhrr_ch1")) {
      variableType = VARIABLE_TYPE_CHANNEL;
      channelVariable = 1;
      isIRChannel = false;
    } // if      
    else if (varName.equals ("avhrr_ch2")) {
      variableType = VARIABLE_TYPE_CHANNEL;
      channelVariable = 2;
      isIRChannel = false;
    } // else if      
    else if (varName.equals ("avhrr_ch3")) {
      variableType = VARIABLE_TYPE_CHANNEL;
      channelVariable = 3;
      channel3a = false;
      isIRChannel = true;
    } // else if      
    else if (varName.equals ("avhrr_ch3a")) {
      variableType = VARIABLE_TYPE_CHANNEL;
      channelVariable = 3;
      channel3a = true;
      isIRChannel = false;
    } // else if      
    else if (varName.equals ("avhrr_ch4")) {
      variableType = VARIABLE_TYPE_CHANNEL;
      channelVariable = 4;
      isIRChannel = true;
    } // else if      
    else if (varName.equals ("avhrr_ch5")) {
      variableType = VARIABLE_TYPE_CHANNEL;
      channelVariable = 5;
      isIRChannel = true;
    } // else if      
    else if (varName.equals ("sat_zenith")) {
      variableType = VARIABLE_TYPE_NAVIGATION;
      navigationVariable = ScanLine.SATELLITE_ZENITH;
    } // else if      
    else if (varName.equals ("sun_zenith")) {
      variableType = VARIABLE_TYPE_NAVIGATION;
      navigationVariable = ScanLine.SOLAR_ZENITH;
    } // else if      
    else if (varName.equals ("rel_azimuth")) {
      variableType = VARIABLE_TYPE_NAVIGATION;
      navigationVariable = ScanLine.RELATIVE_AZIMUTH;
    } // else if      
    else if (varName.equals ("cloud")) {
      variableType = VARIABLE_TYPE_CLOUD;
    } // else if
    else if (varName.equals ("scan_time")) {
      variableType = VARIABLE_TYPE_SCAN_TIME;
    } // else if
    else if (varName.equals ("latitude")) {
      variableType = VARIABLE_TYPE_NAVIGATION;
      navigationVariable = ScanLine.LATITUDE;
    } // else if      
    else if (varName.equals ("longitude")) {
      variableType = VARIABLE_TYPE_NAVIGATION;
      navigationVariable = ScanLine.LONGITUDE;
    } // else if
    else throw new IOException ("Invalid variable: " + varName);
    varClass = grid.getDataClass();

    // Set tile and cache sizes
    // ------------------------
    setTileDims (new int[] {1, dims[COLS]});
    setCacheSize (DEFAULT_CACHE_SIZE);
    
  } // NOAA1bCachedGrid constructor

  ////////////////////////////////////////////////////////////

  protected Tile readTile (
    TilePosition pos
  ) throws IOException {

    // Create data array
    // -----------------
    int[] dataDims = tiling.getTileDimensions(pos);
    int dataValues = dataDims[ROWS]*dataDims[COLS];
    Object data = Array.newInstance (varClass, dataValues);
    byte[] byteData = (varClass.equals (Byte.TYPE) ? (byte[]) data : null);
    short[] shortData = (varClass.equals (Short.TYPE) ? (short[]) data : null);
    float[] floatData = (varClass.equals (Float.TYPE) ? (float[]) data : null);
    long[] longData = (varClass.equals (Long.TYPE) ? (long[]) data : null);

    // Loop over each line
    // ------------------
    int[] tileCoords = pos.getCoords();
    int[] tileDims = tiling.getTileDimensions();
    int[] start = new int[] {tileCoords[ROWS]*tileDims[ROWS], 
      tileCoords[COLS]*tileDims[COLS]};
    int[] length = dataDims;
    float valueOffset = (isIRChannel ? -273.15f : 0);
    for (int i = 0; i < length[ROWS]; i++) {

      // Initialize line constants
      // -------------------------
      int lineIndex = start[ROWS] + i;
      int offset = i*length[COLS];
      ScanLine line;

      switch (variableType) {

      // Get channel data
      // ----------------
      case VARIABLE_TYPE_CHANNEL:

        // Read scan line 
        // --------------
        try { 
          line = reader.getScanLine (lineIndex, start[COLS], length[COLS]); 
        } // try
        catch (IOException e) { 
          line = null;
        } // catch

        // Check for usable sensor data
        // ----------------------------
        boolean valid = (line != null && line.isSensorDataUsable());

        // Check for channel 3 mismatch
        // ----------------------------
        if (valid && channelVariable == 3) {
          int ch3Select = ((Integer) line.getAttribute (
            ScanLine.CH3_SELECT)).intValue();
          if ((ch3Select == 2) || 
            (channel3a && ch3Select == 0) || 
            (!channel3a && ch3Select == 1))
            valid = false;
        } // if

        // Convert to 16-bit scaled
        // ------------------------
        if (valid) {
          float[] samples = line.getChannel (channelVariable);
          for (int j = 0; j < length[COLS]; j++) {
            shortData[offset + j] = (short) Math.round (
              (samples[j]+valueOffset) * 1e2);
          } // for
        } // if

        // Fill with missing values
        // ------------------------
        else {
          Arrays.fill (shortData, offset, offset + length[COLS],
            (short) -32768);
        } // else

        break;

      // Read navigation data
      // --------------------
      case VARIABLE_TYPE_NAVIGATION:

        // Read scan line 
        // --------------
        try { 
          line = reader.getScanLine (lineIndex, 0, 0);
        } // try
        catch (IOException e) { 
          line = null;
        } // catch

        // Get raw navigation
        // ------------------
        float[] navData;
        if (line == null || !line.isNavigationUsable()) {
          try { navData = reader.interpolateRawNavigation (lineIndex, 
            navigationVariable); }
          catch (RuntimeException e) { navData = null; }
        } // if
        else {
          navData = line.getRawNavigation (navigationVariable);
        } // else

        // Fill with missing values
        // ------------------------
        if (navData == null) {
          switch (navigationVariable) {
          case ScanLine.LATITUDE:
          case ScanLine.LONGITUDE:
            Arrays.fill (floatData, offset, offset + length[COLS], Float.NaN);
            break;
          default:
            Arrays.fill (shortData, offset, offset + length[COLS],
              (short) -32768);
            break;
          } // switch
        } // if

        else {

          // Interpolate navigation
          // ----------------------
          float[] samples = reader.interpolateNavigation (navData, lineIndex,
            navigationVariable, start[COLS], length[COLS]);

          switch (navigationVariable) {

          // Copy as 32-bit float
          // --------------------
          case ScanLine.LATITUDE:
          case ScanLine.LONGITUDE:
            System.arraycopy (samples, 0, data, offset, length[COLS]);
            break;

          // Convert to 16-bit scaled
          // ------------------------
          default:
            for (int j = 0; j < length[COLS]; j++) {
              shortData[offset + j] = (short) Math.round (samples[j] * 1e2);
            } // for
            break;

          } // switch

        } // else

        break;

      // Read cloud data
      // ---------------
      case VARIABLE_TYPE_CLOUD:

        // Read scan line 
        // --------------
        try { 
          line = reader.getScanLine (lineIndex, start[COLS], length[COLS]);
        } // try
        catch (IOException e) { 
          line = null;
        } // catch

        // Copy cloud data
        // ---------------
        if (line != null) {
          byte[] samples = line.getCloud();
          System.arraycopy (samples, 0, data, offset, length[COLS]);
        } // if
        else {
          Arrays.fill (byteData, offset, offset + length[COLS], (byte) 0);
        } // else

        break;

      // Read scan time data
      // -------------------
      case VARIABLE_TYPE_SCAN_TIME:

        // Read scan line 
        // --------------
        try { 
          line = reader.getScanLine (lineIndex, start[COLS], length[COLS]);
        } // try
        catch (IOException e) { 
          line = null;
        } // catch

        // Copy time data
        // --------------
        if (line != null) {
          long[] samples = line.getScanTime();
          System.arraycopy (samples, 0, data, offset, length[COLS]);
        } // if
        else {
          Arrays.fill (longData, offset, offset + length[COLS], (long) -1L);
        } // else

        break;

      } // switch

    } // for

    // Return tile
    // -----------
    return (tiling.new Tile (pos, data));

  } // readTile

  ////////////////////////////////////////////////////////////

  protected void writeTile (
    Tile tile
  ) throws IOException {

    // Check access mode
    // -----------------
    if (accessMode == READ_ONLY)
      throw new IOException ("Cannot write tile to read-only dataset");

  } // writeTile

  ////////////////////////////////////////////////////////////

} // NOAA1bCachedGrid class

////////////////////////////////////////////////////////////////////////
