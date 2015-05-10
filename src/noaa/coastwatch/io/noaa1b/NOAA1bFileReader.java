////////////////////////////////////////////////////////////////////////
/*
     FILE: NOAA1bFileReader.java
  PURPOSE: To read NOAA1b data files.
   AUTHOR: Peter Hollemans
     DATE: 2007/10/22
  CHANGES: 2007/11/23, PFH, modified to handle per-channel calibration errors

  CoastWatch Software Library and Utilities
  Copyright 2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io.noaa1b;

// Imports
// -------
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import noaa.coastwatch.io.CachedGrid;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.noaa1b.DataHeader;
import noaa.coastwatch.io.noaa1b.DataRecord;
import noaa.coastwatch.io.noaa1b.NOAA1bFile;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.tools.cwinfo;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.SwathProjection;
import terrenus.instrument.Instrument;
import terrenus.instrument.Radiometer;
import terrenus.instrument.RadiometerCalibrator;
import terrenus.instrument.RadiometerCalibrator.CalibrationType;
import terrenus.instrument.RadiometerData;

/**
 * The <code>NOAA1bFileReader</code> class extends {@link
 * EarthDataReader} to handle NOAA 1b weather satellite data
 * files from a number of NOAA satellite sensors.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class NOAA1bFileReader extends EarthDataReader {

  // Constants
  // ---------

  /** Default cache size in bytes. */
  private static final int DEFAULT_CACHE_SIZE = (4*1024)*1024;

  /** The type of variable. */
  private enum VariableType {
    CHANNEL, NAVIGATION, SCAN_TIME
  };

  /** The navigation variable type. */
  private enum NavigationType {
    LATITUDE, LONGITUDE, SAT_ZENITH, SUN_ZENITH, REL_AZIMUTH
  };

  // Variables
  // ---------

  /** The NOAA 1b file being read. */
  private NOAA1bFile n1bFile;

  /** The set of extra non-channel data variables. */
  private static Set<String> extraVars;

  /** The units for non-channel data variables. */
  private static Map<String,String> extraVarUnits;

  /** The number of lines of data in the file. */
  private int lines;

  /** The number of samples per line. */
  private int samples;

  /** The mapping array of line number to record number. */
  private int[] lineMap;

  /** The number of radiometer channels. */
  private int channels;

  /** The radiometer instrument for this file. */
  private Radiometer radiometer;

  /** The map of variable name to cached grid. */
  private Map<String,CachedGrid> cachedGridMap;

  ////////////////////////////////////////////////////////////

  /** Create set of extra variables. */
  static {

    extraVars = new LinkedHashSet<String>();
    extraVars.add ("latitude");
    extraVars.add ("longitude");
    extraVars.add ("sat_zenith");
    extraVars.add ("sun_zenith");
    extraVars.add ("rel_azimuth");
    extraVars.add ("scan_time");

    extraVarUnits = new HashMap<String,String>();
    extraVarUnits.put ("latitude", "degrees");
    extraVarUnits.put ("longitude", "degrees");
    extraVarUnits.put ("sat_zenith", "degrees");
    extraVarUnits.put ("sun_zenith", "degrees");
    extraVarUnits.put ("rel_azimuth", "degrees");
    extraVarUnits.put ("scan_time", "milliseconds");

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Gets a mapping array of scan line numbers to record numbers.
   * The implementation of this method tries to avoid reading all
   * the data from every scan line in the file.
   *
   * @return the array of record numbers, one for each scan line.
   * If a scan line is missing, then the record number is -1.
   *
   * @throws IOException if an error occurred reading the file.
   */
  private int[] getLineMap () throws IOException {

    // Create buffer for record data
    // -----------------------------
    DataHeader header = n1bFile.getDataHeader();
    int attSize = header.getRecordAttSize();
    ByteBuffer buffer = n1bFile.getInputBuffer (attSize);

    // Create map array
    // ----------------
    int records = n1bFile.getRecordCount();
    buffer.clear();
    DataRecord lastRecord = n1bFile.getDataRecord (records-1, false, buffer);
    int lines = lastRecord.getScanLine();
    int[] lineMap = new int[lines];

    // Insert map values
    // -----------------
    Arrays.fill (lineMap, -1);
    for (int i = 0; i < records; i++) {
      buffer.clear();
      DataRecord record = n1bFile.getDataRecord (i, false, buffer);
      int line = record.getScanLine();
      if (line < 1 || line > lines) continue;
      if (!record.isNavigationUsable()) continue;
      lineMap[line-1] = i;
    } // for

    // Find chunks of valid lines
    // --------------------------
    TreeMap<Integer,Integer> chunkMap = new TreeMap<Integer,Integer>();
    int startLine = 0;
    while (startLine < lines && lineMap[startLine] == -1) startLine++;
    do {
      int endLine = startLine+1;
      while (endLine < lines && lineMap[endLine] != -1) endLine++;
      if (startLine < lines) {
        chunkMap.put (endLine-startLine, startLine);
        startLine = endLine;
        while (startLine < lines && lineMap[startLine] == -1) startLine++;
      } // if
    } while (startLine < lines);

    // Find largest valid chunk
    // ------------------------
    if (chunkMap.size() == 0) 
      throw new RuntimeException ("No valid scan lines found");
    int chunkLines = chunkMap.lastKey();
    int chunkStart = chunkMap.get (chunkLines);
    
    // Remap lines to only include largest valid chunk
    // -----------------------------------------------
    if (chunkStart != 0 || chunkLines != lines) {
      int[] newLineMap = new int[chunkLines];
      System.arraycopy (lineMap, chunkStart, newLineMap, 0, chunkLines);
      lineMap = newLineMap;
    } // if

    return (lineMap);

  } // getLineMap

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new reader assuming big endian byte order.
   *
   * @param fileName the NOAA 1b filename.
   *
   * @throws IOException if an error occurred reading the file.
   */
  public NOAA1bFileReader (
    String fileName
  ) throws IOException {

    this (fileName, false);

  } // NOAA1bFileReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new reader.
   *
   * @param fileName the NOAA 1b filename.
   * @param isByteSwapped the byte swapped flag, true if the data
   * is in little endian byte order.
   *
   * @throws IOException if an error occurred reading the file.
   */
  public NOAA1bFileReader (
    String fileName,
    boolean isByteSwapped
  ) throws IOException {

    super (fileName);

    // Open file
    // ---------
    n1bFile = new NOAA1bFile (fileName, isByteSwapped);

    // Get instrument information
    // --------------------------
    Instrument instrument = n1bFile.getInstrument();
    if (!(instrument instanceof Radiometer)) 
      throw new IOException ("Unknown instrument type, not a radiometer");
    radiometer = (Radiometer) instrument;
    samples = radiometer.getSampleCount();
    lineMap = getLineMap();
    lines = lineMap.length;
    channels = radiometer.getChannelCount();

    // Get variable names
    // ------------------
    variables = new String[channels + extraVars.size()];
    for (int i = 0; i < channels; i++) {
      variables[i] = radiometer.getChannelName (i+1);
    } // for
    int index = channels;
    for (String varName : extraVars) { variables[index] = varName; index++; }

    // Get time information
    // --------------------
    DataHeader header = n1bFile.getDataHeader();
    Date startDate = header.getStartDate();
    Date endDate = header.getEndDate();
    TimePeriod period = new TimePeriod (startDate, endDate.getTime() - 
      startDate.getTime());

    // Get data source information
    // ---------------------------
    String sat = header.getSpacecraft();
    String sensor = instrument.getName();
    String origin = header.getCreationSite();
    String history = header.getDatasetName();

    // Create map of cached grids
    // --------------------------
    cachedGridMap = new HashMap<String,CachedGrid>();

    // Get transform information
    // -------------------------
    DataVariable lat = getVariable ("latitude");
    DataVariable lon = getVariable ("longitude");
    EarthTransform trans;
    if (dataProjection) {
      trans = new DataProjection (lat, lon);
    } // if
    else {
      DataLocation d1 = new DataLocation (lines/2, samples/2);
      DataLocation d2 = new DataLocation (lines/2 + 1, samples/2 + 1);
      EarthLocation e1 = new EarthLocation (lat.getValue (d1), 
        lon.getValue (d1));
      EarthLocation e2 = new EarthLocation (lat.getValue (d2), 
        lon.getValue (d2));
      double dist = e1.distance (e2);
      double pixelSize = Math.sqrt ((dist*dist)/2);
      int minDim = Math.min (lines, samples);
      /**
       * Why use this swath polynomial size?  We want to have the
       * curvature of the earth on both sides of nadir accounted for
       * by separate polynomials.  At least three polynomial patches
       * per side accomplishes that, down to a minimum of 100x100
       * pixels.
       */
      double swathPolySize = Math.min (100, samples/4)*pixelSize;
      trans = new SwathProjection (lat, lon, swathPolySize,
        new int[] {minDim, minDim});
    } // else

    info = new SatelliteDataInfo (sat, sensor, 
      Arrays.asList (new TimePeriod[] {period}), trans, origin, history);

  } // NOAA1bFileReader

  ////////////////////////////////////////////////////////////

  public String getDataFormat() { 

    return (
      "NOAA 1b version " + 
      n1bFile.getFormatVersion() + " " + 
      n1bFile.getInstrument().getName()
    );

  } // getDataFormat

  ////////////////////////////////////////////////////////////

  protected DataVariable getPreviewImpl (int index) throws IOException {

    // Get variable name
    // -----------------
    String varName = variables[index];
    String longName = varName;

    // Get variable units
    // ------------------
    CalibrationType calType = getCalType (index);
    String varUnits = (calType != null ? varUnits = calType.getUnits() : 
      extraVarUnits.get (varName));

    // Get numerical properties
    // ------------------------
    Object data;
    NumberFormat format;
    double[] scaling;
    Object missing;
    if (varName.equals ("latitude") || varName.equals ("longitude")) {
      data = new float[1];
      format = new DecimalFormat ("0.####");
      scaling = new double[] {1, 0};
      missing = new Float (Float.NaN);
    } // if
    else if (varName.equals ("scan_time")) {
      data = new long[1];
      format = new DecimalFormat ("0");
      scaling = new double[] {1, 0};
      missing = new Long (-1L);
    } // else if
    else {
      data = new short[1];
      format = new DecimalFormat ("0.##");
      scaling = new double[] {0.01, 0};
      missing = new Short ((short) -32768);
    } // else
 
    return (new Grid (varName, longName, varUnits, lines, 
      samples, data, format, scaling, missing));

  } // getPreviewImpl

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the calibration type for the specified variable index
   * or null if the variable is not a channel.
   */
  private CalibrationType getCalType (
    int index
  ) {   

    if (index <= channels-1) {
      return (radiometer.isThermal (index+1) ? CalibrationType.CELSIUS :
        CalibrationType.ALBEDO);
    } // if
    else {
      return (null);
    } // else

  } // getCalType

  ////////////////////////////////////////////////////////////

  public DataVariable getVariable (int index) throws IOException {

    // Get a preview and convert to cached grid
    // ----------------------------------------
    if (!cachedGridMap.containsKey (variables[index])) {
      DataVariable var = getPreview (index);
      CachedGrid grid = new NOAA1bFileCachedGrid ((Grid) var, index);
      cachedGridMap.put (variables[index], grid);
    } // if

    return (cachedGridMap.get (variables[index]));

  } // getVariable

  ////////////////////////////////////////////////////////////

  public void close() throws IOException {

    n1bFile.close();
    cachedGridMap.clear();

  } // close

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>NOAA1bFileCachedGrid</code> reads and caches data
   * for a NOAA 1b file.
   */
  private class NOAA1bFileCachedGrid extends CachedGrid {

    // Variables
    // ---------
    
    /** The variable type. */
    private VariableType varType;
    
    /** The channel number. */
    private int channel;
    
    /** The navigation variable type. */
    private NavigationType navType;
    
    /** The variable class. */
    private Class varClass;

    ////////////////////////////////////////////////////////

    public Class getDataClass() { return (varClass); }

    ////////////////////////////////////////////////////////

    public Object getDataStream() { return (NOAA1bFileReader.this); }

    ////////////////////////////////////////////////////////

    /**
     * Constructs a new read-only NOAA 1b cached grid with the
     * specified properties.
     *
     * @param grid the grid to use for attributes.
     * @param index the variable index.
     *
     * @throws IOException if a problem occurred accessing the
     * NOAA 1b file.
     */
    public NOAA1bFileCachedGrid (
      Grid grid,
      int index
    ) throws IOException {

      // Create cached grid
      // ------------------    
      super (grid, READ_ONLY);

      // Set variable information
      // ------------------------
      if (index <= channels-1) {
        varType = VariableType.CHANNEL;
        channel = index+1;
      } // if
      else {
        String varName = grid.getName();
        if (varName.equals ("scan_time")) {
          varType = VariableType.SCAN_TIME;
        } // if
        else {
          varType = VariableType.NAVIGATION;
          navType = NavigationType.valueOf (NavigationType.class, 
            varName.toUpperCase());
        } // else
      } // else
      varClass = grid.getDataClass();

      // Set tile and cache sizes
      // ------------------------
      setTileDims (new int[] {1, dims[COLS]});
      setCacheSize (DEFAULT_CACHE_SIZE);

    } // NOAA1bFileCachedGrid constructor

    ////////////////////////////////////////////////////////

    protected Tile readTile (
      TilePosition pos
    ) throws IOException {

      // Create data arrays
      // ------------------
      int[] dataDims = pos.getDimensions();
      int dataValues = dataDims[ROWS]*dataDims[COLS];
      Object data = Array.newInstance (varClass, dataValues);
      short[] shortData = (varClass.equals (Short.TYPE) ? (short[]) data:null);
      float[] floatData = (varClass.equals (Float.TYPE) ? (float[]) data:null);
      long[] longData = (varClass.equals (Long.TYPE) ? (long[]) data : null);
      DataHeader header = n1bFile.getDataHeader();
      ByteBuffer buffer = n1bFile.getInputBuffer (header.getRecordSize());
      double[] calData = new double[samples];
      double[] navData = new double[samples];

      // Determine calibration type
      // --------------------------
      Radiometer radiometer = (Radiometer) n1bFile.getInstrument();
      CalibrationType calType = (varType == VariableType.CHANNEL ? 
        getCalType (channel-1) : null);

      // Loop over each line
      // -------------------
      int[] tileCoords = pos.getCoords();
      int[] tileDims = tiling.getTileDimensions();
      int[] start = new int[] {
        tileCoords[ROWS]*tileDims[ROWS], 
        tileCoords[COLS]*tileDims[COLS]
      };
      int[] length = dataDims;
      for (int i = 0; i < length[ROWS]; i++) {

        // Initialize line constants
        // -------------------------
        int lineIndex = start[ROWS] + i;
        int offset = i*length[COLS];

        DataRecord record = null;
        boolean isUsable = false;
        switch (varType) {

        // Read/calibrate channel data
        // ---------------------------
        case CHANNEL:

          // Read data record
          // ----------------
          if (lineMap[lineIndex] != -1) {
            buffer.clear();
            record = n1bFile.getDataRecord (lineMap[lineIndex], true, buffer);
            isUsable = record.isSensorDataUsable() && 
              record.isCalibrationUsable();
          } // if

          // Convert to 16-bit scaled
          // ------------------------
          if (isUsable) {
            RadiometerData radData = (RadiometerData) record.getData();
            radData.getCalibratedData (channel, calType, calData);
            for (int j = 0; j < length[COLS]; j++) {
              int calDataOffset = j + start[COLS];
              if (Double.isNaN (calData[calDataOffset]))
                shortData[offset + j] = (short) -32768;
              else
                shortData[offset + j] = 
                  (short) Math.round (calData[calDataOffset]*1e2);
            } // for
          } // if

          // Otherwise, fill with missing values
          // -----------------------------------
          else {
            Arrays.fill (shortData, offset, offset + length[COLS],
              (short) -32768);
          } // else

          break;

        // Read navigation data
        // --------------------
        case NAVIGATION:

          // Read data record
          // ----------------
          if (lineMap[lineIndex] != -1) {
            buffer.clear();
            record = n1bFile.getDataRecord (lineMap[lineIndex], true, buffer);
            isUsable = record.isNavigationUsable();
          } // if

          // Get navigation data and convert
          // -------------------------------
          if (isUsable) {
            RadiometerData radData = (RadiometerData) record.getData();
            switch (navType) {
            case LATITUDE:
              radData.getLocationData (navData, null, null, null, null);
              break;
            case LONGITUDE:
              radData.getLocationData (null, navData, null, null, null);
              break;
            case SAT_ZENITH:
              radData.getLocationData (null, null, navData, null, null);
              break;
            case SUN_ZENITH:
              radData.getLocationData (null, null, null, navData, null);
              break;
            case REL_AZIMUTH:
              radData.getLocationData (null, null, null, null, navData);
              break;
            } // switch
            switch (navType) {
            case LATITUDE:
            case LONGITUDE:
              for (int j = 0; j < length[COLS]; j++) {
                floatData[offset + j] = (float) navData[j + start[COLS]];
              } // for
              break;
            case SAT_ZENITH:
            case SUN_ZENITH:
            case REL_AZIMUTH:
              for (int j = 0; j < length[COLS]; j++) {
                shortData[offset + j] = 
                  (short) Math.round (navData[j + start[COLS]]*1e2);
              } // for
              break;
            } // switch
          } // if
          
          // Otherwise, fill with missing values
          // -----------------------------------
          else {
            switch (navType) {
            case LATITUDE:
            case LONGITUDE:
              Arrays.fill (floatData, offset, offset + length[COLS],
                Float.NaN);
              break;
            case SAT_ZENITH:
            case SUN_ZENITH:
            case REL_AZIMUTH:
              Arrays.fill (shortData, offset, offset + length[COLS],
                (short) -32768);
              break;
            } // switch
          } // else

          break;

        // Read scan time data
        // -------------------
        case SCAN_TIME:

          // Read data record
          // ----------------
          if (lineMap[lineIndex] != -1) {
            buffer.clear();
            record = n1bFile.getDataRecord (lineMap[lineIndex], false, buffer);
            isUsable = true;
          } // if

          // Copy time data
          // --------------
          if (isUsable) {
            long scanTime = record.getDate().getTime();
            Arrays.fill (longData, offset, offset + length[COLS], scanTime);
          } // if

          // Otherwise, fill with missing values
          // -----------------------------------
          else {
            Arrays.fill (longData, offset, offset + length[COLS], -1L);
          } // else

          break;

        } // switch

      } // for

      // Return tile
      // -----------
      return (tiling.new Tile (pos, data));

    } // readTile

    ////////////////////////////////////////////////////////

    protected void writeTile (
      Tile tile
    ) throws IOException {

      // Check access mode
      // -----------------
      if (accessMode == READ_ONLY)
        throw new IOException ("Cannot write tile to read-only dataset");

    } // writeTile

    ////////////////////////////////////////////////////////

  } // NOAA1bFileCachedGrid class

  ////////////////////////////////////////////////////////////
  
  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) throws Exception {

    NOAA1bFileReader reader = new NOAA1bFileReader (argv[0], false);
    noaa.coastwatch.tools.cwinfo.printInfo (reader, System.out);

  } // main  

  ////////////////////////////////////////////////////////////

} // NOAA1bFileReader class

////////////////////////////////////////////////////////////////////////
