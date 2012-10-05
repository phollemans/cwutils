////////////////////////////////////////////////////////////////////////
/*
     FILE: NOAA1bReader.java
  PURPOSE: A class to read NOAA 1b format files.
   AUTHOR: Peter Hollemans
     DATE: 2003/02/03
  CHANGES: 2003/03/28, PFH, added GAC and CLAVR cloud support
           2003/04/10, PFH, added longitude filtering for navigation 
             interpolation
           2003/11/22, PFH, fixed Javadoc comments
           2004/02/15, PFH, added super() call in constructor
           2004/06/17, PFH, added rows and cols settings in info for 
             cwcoverage error
           2004/09/11, PFH, modified geolocation data error message
           2004/09/28, PFH, modified automatic history append on read
           2004/10/05, PFH, removed rows and cols setting in metadata
           2004/11/29, PFH, added calibration lookup tables
           2005/02/01, PFH, added dataProjection flag
           2005/02/02, PFH, added fallback to use data-only projection
           2005/06/08, PFH, updated units strings
           2005/09/21, PFH, updated longitude interpolation near poles
           2006/08/28, PFH, updated to use java.nio classes
           2006/08/31, PFH, added support for 8 and 16 bit sensor data formats
           2006/09/21, PFH, added support for missing scan lines
           2006/11/03, PFH, changed getPreview(int) to getPreviewImpl(int)
           2006/12/27, PFH, added scan line time data reading
           2006/12/30, PFH, modified to find largest good line chunk
           2007/06/27, PFH, corrected GAC navigation start/end indices
           2007/12/13, PFH
           - added scan line caching
           - added extra code in close() to combat memory leaks
           2007/12/15, PFH, added scan line caching
           2007/12/20, PFH, corrected problem with start/count in getScanLine() 

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.text.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * A NOAA 1b reader is an Earth data reader that reads NOAA 1b
 * format GAC/LAC/HRPT data files available from the NOAA/NESDIS
 * Satellite Active Archive.  For file format details, see:
 * <blockquote>
 *   http://www.saa.noaa.gov
 * </blockquote>
 * The class may be used in a general way as an Earth data reader
 * to retrieve calibrated AVHRR variable data and dataset information,
 * or in a more specific way to retrieve raw scan line and header
 * information.  A selected subset of scan line and header attributes
 * are available as needed to implement the interface of an Earth
 * data reader.  Where no access to the underlying data is supplied,
 * the raw data may be used.
 */
public abstract class NOAA1bReader
  extends EarthDataReader {

  // Constants
  // ---------
  /** The swath polynomial size in kilometers. */
  public static final double SWATH_POLY_SIZE = 100.0;

  /** The number of scan lines for raw navigation interpolation. */
  private static final int NAVIGATION_SAMPLES = 4;

  /** The maximum scan line distance for raw navigation interpolation. */
  private static final int NAVIGATION_MAXDIST = 40;

  /** Short variable names. */
  private static final String[] SHORT_NAMES = {
    "avhrr_ch1",          
    "avhrr_ch2",            
    "avhrr_ch3",
    "avhrr_ch3a",
    "avhrr_ch4",          
    "avhrr_ch5",            
    "sat_zenith",           
    "sun_zenith",
    "rel_azimuth",
    "cloud",
    "latitude",
    "longitude",
    "scan_time"
  };

  /** Descriptive variable names. */
  private static final String[] LONG_NAMES = {
    "AVHRR channel 1",
    "AVHRR channel 2",
    "AVHRR channel 3",
    "AVHRR channel 3a",
    "AVHRR channel 4",         
    "AVHRR channel 5",
    "satellite zenith angle",   
    "solar zenith angle",
    "relative azimuth angle",
    "NESDIS CLAVR cloud mask",
    "latitude",
    "longitude",
    "scan time"
  };

  /** The starting sample for LAC navigation. */
  public static final int LAC_NAVIGATION_START = 24;

  /** The ending sample for LAC navigation. */
  public static final int LAC_NAVIGATION_END = 2024;

  /** The navigation step size for LAC navigation. */
  public static final int LAC_NAVIGATION_STEP = 40;

  /** The starting sample for GAC navigation. */
  public static final int GAC_NAVIGATION_START = 4;

  /** The ending sample for GAC navigation. */
  public static final int GAC_NAVIGATION_END = 404;

  /** The navigation step size for GAC navigation. */
  public static final int GAC_NAVIGATION_STEP = 8;

  /** The number of samples per line for LAC data. */
  public static final int LAC_SAMPLES = 2048;
  
  /** The number of samples per line for GAC data. */
  public static final int GAC_SAMPLES = 409;

  /** The size of each calibration lookup table. */
  public static final int LOOKUP_TABLE_SIZE = 1024;

  /** The maximum number of calibration lookup tables per channel. */
  public static final int MAX_LOOKUP_TABLES = 256;

  /** The maximum number of scan lines in the cache. */
  public static final int MAX_SCAN_LINES = 100;

  /** The filter to use for longitude values. */
  private static final Filter LON_FILTER = new LongitudeFilter();

  // Variables
  // ---------

  /** The input channel. */
  protected FileChannel inputChannel;

  /** The number of scan lines in this dataset. */
  protected int lines;

  /** 
   * The number of records in this dataset.  This is the number
   * of actual scan lines minus the number of missing lines.
   */
  protected int records;

  /** The scan line map array mapping scan line number to record number. */
  protected int[] scanLineMap;

  /** The number of samples in this dataset. */
  protected int samples;

  /** The archive header flag, true if we have an archive header. */
  protected boolean archive;

  /** The dataset header record, used for calibration. */
  protected DataHeader header;

  /** The number of data headers for this dataset. */
  protected int dataHeaders;

  /** The size of the archive header for this dataset in bytes. */
  protected int archiveHeaderSize;

  /** The size of the data header and records for this dataset in bytes. */
  protected int recordSize;

  /** The size of the scan line attributes section in bytes. */
  protected int attributeDataSize;

  /** The starting sample for navigation. */
  protected int navigationStart;

  /** The ending sample for navigation. */
  protected int navigationEnd;

  /** The navigation step size for navigation. */
  protected int navigationStep;

  /** The array of calibration lookup table maps for channels 1-5. */
  private HashMap[] lookupTableMaps;

  /** The array of lookup data count values. */
  private int[] lookupData;

  /** The sensor word size in bits. */
  protected int sensorWordSize;

  /** The scan line cache. */
  protected Map<Integer,ScanLine> scanLineCache;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the descriptive variable name.
   * 
   * @param name the short variable name.
   *
   * @return the descriptive variable name.
   */
  private static String getLongName (
    String name
  ) {

    // Find variable name in the list
    // ------------------------------
    for (int i = 0; i < SHORT_NAMES.length; i++)
      if (name.equals (SHORT_NAMES[i])) return (LONG_NAMES[i]);
    return ("unknown");

  } // getLongName

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the variable units.
   *
   * @param index the variable index.
   *
   * @return the variable units string.  If the variable has no known
   * units, an empty string is returned.
   */
  private static String getUnits (
    String name
  ) {

    // Create units string
    // -------------------
    if (name.matches ("^avhrr_ch(1|2|3a)$"))
      return ("percent");
    else if (name.matches ("^avhrr_ch[345]$"))
      return ("celsius");
    else if (name.matches ("^((sat|sun)_zenith|rel_azimuth)$"))
      return ("degrees");
    else if (name.matches ("^(latitude|longitude)$"))
      return ("degrees");
    else if (name.matches ("^scan_time$"))
      return ("milliseconds");
    else
      return ("");

  } // getUnits

  ////////////////////////////////////////////////////////////

  /** Gets the start of the record as a byte offset. */
  protected int getRecordStart (
    int record
  ) {

    int start = 0;
    if (archive) start += archiveHeaderSize;
    start += (dataHeaders + record)*recordSize;
    return (start);

  } // getRecordStart

  ////////////////////////////////////////////////////////////

  /** Gets the variable names in this dataset. */
  protected abstract String[] getVariableNames ();

  ////////////////////////////////////////////////////////////

  /** Checks for an archive header and returns true if so. */
  protected abstract boolean getArchiveFlag () throws IOException;

  ////////////////////////////////////////////////////////////

  /** Gets the number of scan lines in this dataset. */
  public int getLines () { return (lines); }

  ////////////////////////////////////////////////////////////

  /** Gets the number of samples per scan line in this dataset. */
  public int getSamples () { return (samples); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the archive header flag, true if the dataset has an archive
   * header.
   */
  public boolean isArchive () { return (archive); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the archive header if it exists.
   * 
   * @return the archive header.
   *
   * @throws IOException if an error occurred reading the file data.
   */
  public abstract ArchiveHeader getArchiveHeader () throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the data header.
   * 
   * @return the data header.
   *
   * @throws IOException if an error occurred reading the file data.
   */
  public abstract DataHeader getDataHeader () throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the scan line at the specified index.
   *
   * @param line the scan line index in the range [0..lines-1].
   * @param start the starting sensor data sample.
   * @param count the total number of sensor data samples.  If the
   * count is 0, no sensor data is read.
   *
   * @return the requested scan line.
   *
   * @throws IOException if an error occurred reading the file
   * data or the scan line is marked as missing.
   *
   * @see ScanLine
   * @see #getLines
   */
  public abstract ScanLine getScanLine (
    int line,
    int start,
    int count
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Checks the dataset format and throws an error if it is
   * incompatible with this reader.
   */
  protected abstract void checkFormat () throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Determines if the data is byte swapped.  Normally, NOAA 1b
   * format files are not byte swapped, so this method returns
   * false but may be overridden in a subclass for handling
   * strange formats.
   *
   * @return true if byte swapped, or false if not.
   */
  public boolean isByteSwapped () { return (false); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the navigation data usability flag.  This method is used to
   * determine if navigation data is usable for a certain scan line
   * without reading all the scan line data.
   *
   * @param record the record to check for navigation (note this is
   * not the same as the line number used in the scan line
   * constructor).
   * @param buffer the buffer to use for reading data.
   *
   * @return true if the line has usable navigation data, or false if
   * not.
   */
  protected abstract boolean isNavigationUsable (
    int record,
    ByteBuffer buffer
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the scan line number for a data record.
   * 
   * @param record the record to get the scan line number for.
   * @param buffer the buffer to use for reading data.
   * 
   * @return the scan line number for the record.
   */
  private int getScanLineNumber (
    int record,
    ByteBuffer buffer
  ) throws IOException {

    buffer.clear().limit (2);
    inputChannel.read (buffer, getRecordStart (record));
    int line = getUShort (buffer, 0);
    return (line);

  } // getScanLineNumber

  ////////////////////////////////////////////////////////////

  /**
   * Gets a mapping array of scan line numbers to record numbers.
   * The implementation of this method should try to avoid
   * reading all the data from every scan line in the file.
   *
   * @return the array of record numbers, one for each scan line.
   * If a scan line is missing, then the record number is -1.
   *
   * @throws IOException if an error occurred reading the file.
   */
  private int[] getScanLineMap () throws IOException {

    // Create map array
    // ----------------
    ByteBuffer inputBuffer = ByteBuffer.allocate (8);
    if (isByteSwapped()) inputBuffer.order (ByteOrder.LITTLE_ENDIAN);
    int lines = getScanLineNumber (records-1, inputBuffer);
    int[] lineMap = new int[lines];

    // Insert map values
    // -----------------
    Arrays.fill (lineMap, -1);
    for (int i = 0; i < records; i++) {
      int line = getScanLineNumber (i, inputBuffer);
      if (line < 1 || line > lines) continue;
      boolean navUsable = isNavigationUsable (i, inputBuffer);
      if (!navUsable) continue;
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

  } // getScanLineMap

  ////////////////////////////////////////////////////////////

  /**
   * Gets a data buffer for the file with data from the specified region.
   * 
   * @param start the starting byte offset.
   * @param length the length of the buffer.
   *
   * @return the buffer of byte values.
   *
   * @throws IOException if an error occurred reading the file.
   */
  protected ByteBuffer getDataBuffer (
    int start,
    int length
  ) throws IOException {

    ByteBuffer data = ByteBuffer.allocate (length);
    inputChannel.read (data, start);
    if (isByteSwapped()) data.order (ByteOrder.LITTLE_ENDIAN);
    return (data);

  } // getDataBuffer

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a NOAA 1b reader from the specified file.
   *
   * @param file the file name to read.
   *
   * @throws IOException if an error occurred reading the file data.
   */
  protected NOAA1bReader (
    String file
  ) throws IOException {

    super (file);

    // Open file, get archive flag, test format
    // ----------------------------------------
    inputChannel = new RandomAccessFile (file, "r").getChannel();
    archive = getArchiveFlag();
    checkFormat();

    // Create cache of scan lines
    // --------------------------
    scanLineCache = new LinkedHashMap<Integer,ScanLine> (MAX_SCAN_LINES, 
      .75f, true) {
      public boolean removeEldestEntry (Map.Entry eldest) {
        return (size() > MAX_SCAN_LINES);
      } // removeEldestEntry
    };

    // Set header and records
    // ----------------------
    header = getDataHeader();
    records = ((Number) header.getAttribute (
      DataHeader.DATA_RECORDS)).intValue();
    scanLineMap = getScanLineMap();
    lines = scanLineMap.length;

    // Set LAC-specific constants
    // --------------------------
    String type = (String) header.getAttribute (DataHeader.DATA_TYPE_CODE);
    if (type.equals ("hrpt") || type.equals ("lac")) {
      samples = LAC_SAMPLES;
      navigationStart = LAC_NAVIGATION_START;
      navigationEnd = LAC_NAVIGATION_END;
      navigationStep = LAC_NAVIGATION_STEP; 
    } // if

    // Set GAC-specific constants
    // --------------------------
    else if (type.equals ("gac")) {
      samples = GAC_SAMPLES;
      navigationStart = GAC_NAVIGATION_START;
      navigationEnd = GAC_NAVIGATION_END;
      navigationStep = GAC_NAVIGATION_STEP; 
    } // else

    // Report unsupported data type
    // ----------------------------
    else 
      throw new IOException ("Unsupported data type: " + type);

    // Get variable names
    // ------------------
    variables = getVariableNames();

    // Create info object
    // ------------------
    info = getGlobalInfo();

    // Create lookup table cache
    // -------------------------
    lookupTableMaps = new HashMap[5];
    for (int i = 0; i < 5; i++) {
      lookupTableMaps[i] = new LinkedHashMap (MAX_LOOKUP_TABLES, .75f, true) {
          public boolean removeEldestEntry (Map.Entry eldest) {
            return (size() > MAX_LOOKUP_TABLES);
          } // removeEldestEntry
        };
    } // for

    // Create lookup data count values table
    // -------------------------------------
    lookupData = new int[LOOKUP_TABLE_SIZE];
    for (int i = 0; i < LOOKUP_TABLE_SIZE; i++) lookupData[i] = i;

  } // NOAA1bReader constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Reads the satellite data info metadata.
   *
   * @return the satellite data info object.
   *
   * @throws IOException if an error occurred reading the file data.
   */  
  private SatelliteDataInfo getGlobalInfo () 
    throws IOException {

    // Get simple attributes
    // ---------------------
    String sat = (String) header.getAttribute (DataHeader.SPACECRAFT_ID);
    String sensor = "avhrr";
    String origin = "USDOC/NOAA/NESDIS";
    String history = (String) header.getAttribute (DataHeader.DATASET_NAME);

    // Get date and transform
    // ----------------------
    Date date = getDate();
    EarthTransform transform = getTransform();

    // Return info object
    // ------------------
    return (new SatelliteDataInfo (sat, sensor, date, transform,
      origin, history));

  } // getGlobalInfo

  ////////////////////////////////////////////////////////////

  /**
   * Reads the date and time.  The date and time metadata in the NOAA1b
   * file are converted into the equivalent <code>Date</code>.
   *
   * @return a new date based on the NOAA1b file data.
   */
  private Date getDate () {

    // Read the date and time
    // ----------------------
    int year = ((Integer) header.getAttribute (
      DataHeader.START_YEAR)).intValue();
    int day = ((Integer) header.getAttribute (
      DataHeader.START_DAY)).intValue();
    long millisecond = ((Long) header.getAttribute (
      DataHeader.START_MILLISECOND)).longValue();

    // Create date object
    // ------------------
    Calendar cal = new GregorianCalendar (year, 0, 0, 0, 0, 0);
    cal.set (Calendar.DAY_OF_YEAR, day);
    cal.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
    return (new Date (cal.getTimeInMillis() + millisecond));

  } // getDate

  ////////////////////////////////////////////////////////////

  /**
   * Reads the Earth transform information.  The projection metadata
   * in the NOAA1b file is converted into the equivalent {@link
   * SwathProjection}.
   *
   * @return an Earth transform based on the NOAA1b file data.
   */
  private EarthTransform getTransform () {

    // Create swath
    // ------------
    DataVariable lat = null, lon = null;
    EarthTransform trans = null;
    try {
      lat = getVariable ("latitude");
      int cols = lat.getDimensions()[Grid.COLS];
      ((CachedGrid) lat).setTileDims (new int[] {1, cols});
      lon = getVariable ("longitude");
      ((CachedGrid) lon).setTileDims (new int[] {1, cols});
      if (dataProjection) {
        trans = new DataProjection (lat, lon);
      } // if
      else {
        trans = new SwathProjection (lat, lon, SWATH_POLY_SIZE, 
          new int[] {cols, cols});
      } // else
    } // try
    catch (Exception e) {
      System.err.println (this.getClass() + 
        ": Warning: Problems encountered using Earth location data");
      e.printStackTrace();
      if (lat != null && lon != null) {
        System.err.println (this.getClass() + 
          ": Warning: Falling back on data-only projection, " +
          "Earth location reverse lookup will not function");
        trans = new DataProjection (lat, lon);
      } // if
    } // catch

    return (trans);

  } // getTransform

  ////////////////////////////////////////////////////////////

  /** Determines if a byte is a valid printable ASCII character. */
  public static boolean isPrint (
    byte b
  ) {

    if (b < 32 || b > 127) return (false);
    else return (true);

  } // isPrint

  ////////////////////////////////////////////////////////////

  /** Gets a byte array from the specified byte buffer. */
  public static byte[] getBytes (
    ByteBuffer data,
    int offset,
    int length
  ) {

    byte[] array = new byte[length];
    data.position (offset);
    data.get (array);
    return (array);

  } // getBytes

  ////////////////////////////////////////////////////////////

  /** Gets an unsigned 8-bit value from the specified byte buffer. */
  public static short getUByte (
    ByteBuffer data,
    int offset
  ) {
 
    return ((short) (data.get (offset) & 0xff));

  } // getUByte

  ////////////////////////////////////////////////////////////

  /** Gets an unsigned 16-bit value from the specified byte buffer. */
  public static int getUShort (
    ByteBuffer data,
    int offset
  ) {
 
    return (data.getShort (offset) & 0xffff);

  } // getUShort

  ////////////////////////////////////////////////////////////

  /** Gets a signed 16-bit value from the specified byte buffer. */
  public static short getShort (
    ByteBuffer data,
    int offset
  ) {
 
    return (data.getShort (offset));

  } // getShort

  ////////////////////////////////////////////////////////////

  /** Gets an unsigned 32-bit value from the specified byte buffer. */
  public static long getUInt (
    ByteBuffer data,
    int offset
  ) {
 
    return (data.getInt (offset) & 0xffffffffL);

  } // getUInt

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an n-bit value from the specified byte buffer.
   *
   * @param data the byte buffer to extract data from.
   * @param startOffset the starting offset into the byte buffer to count
   * n-bit data values from.
   * @param valueOffset the number of n-bit values into the array to
   * extract an integer from.
   * @param bits the number of bits in each value in the range [1..64].
   */
  public static long getNBit (
    ByteBuffer data,
    int startOffset,
    int valueOffset,
    int bits
  ) {
 
    // Get the starting byte and bit for the n-bit value
    // -------------------------------------------------
    int startByte = (valueOffset*bits) / 8;
    int startBit = (valueOffset*bits) - startByte*8;

    // Initialize the value
    // --------------------
    long value = 0L;
    
    // Loop over each bit and copy bit value into final value
    // ------------------------------------------------------
    int currentByte = startOffset + startByte;
    int currentBit = startBit;
    for (int i = 0; i < bits; i++) {
      byte mask = (byte) (0x80 >> currentBit);
      if ((data.get (currentByte) & mask) != 0)
        value = value | (0x01 << (bits-1-i));
      currentBit++;
      if (currentBit > 7) {
        currentBit = 0;
        currentByte++;
      } // if
    } // for

    return (value);

  } // getNBit

  ////////////////////////////////////////////////////////////

  /** Gets a signed 32-bit value from the specified byte buffer. */
  public static int getInt (
    ByteBuffer data,
    int offset
  ) {
 
    return (data.getInt (offset));

  } // getInt

  ////////////////////////////////////////////////////////////
  
  /** 
   * The <code>ScanLineCalibration</code> class holds calibration
   * coefficients for one AVHRR scan line so that they may be used as
   * a key in storing calibration lookup tables.
   */
  protected class ScanLineCalibration {

    // Variables
    // ---------
    
    /** The array of calibration coefficients. */
    private float[] coefs;

    /** 
     * The hash code created using data from the scan line
     * coefficients.
     */
    private int hash;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new <code>ScanLineCalibration</code> object with the
     * specified coefficients.
     *
     * @param coefs the coefficients for this scan line calibration.
     */
    public ScanLineCalibration (
      float[] coefs
    ) {

      this.coefs = (float[]) coefs.clone();
      this.hash = 0;
      for (int i = 0; i < coefs.length; i++) 
        hash ^= Float.floatToIntBits (coefs[i]);

    } // ScanLineCalibration constructor

    ////////////////////////////////////////////////////////

    public boolean equals (Object obj) {

      if (!(obj instanceof ScanLineCalibration)) return (false);
      return (Arrays.equals (((ScanLineCalibration) obj).coefs, this.coefs));

    } // equals

    ////////////////////////////////////////////////////////

    public int hashCode () {

      return (hash);

    } // hashCode

    ////////////////////////////////////////////////////////

  } // ScanLineCalibration

  ////////////////////////////////////////////////////////////           

  /**
   * The <code>LongitudeInterpolator</code> class performs longitude
   * interpolation using polar coordinates.  This helps to avoid
   * problems with longitude interpolation near the poles where
   * simple Lagrangian interpolation along the scan line causes
   * interpolation anomalies.
   */
  public static class LongitudeInterpolator 
    extends Function {

    // Variables
    // ---------
    
    /** The sample index to polar x coordinate interpolator. */
    private Function xInterp;

    /** The sample index to polar y coordinate interpolator. */
    private Function yInterp;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new interpolator.
     *
     * @param sampleIndex the array of sample indices.
     * @param latData the array of latitude data.
     * @param lonData the array of longitude data.
     */
    public LongitudeInterpolator (
      int[] sampleIndex,
      float[] latData,
      float[] lonData
    ) {       

      // Convert data to polar
      // ---------------------
      int samples = sampleIndex.length;
      double[] x = new double[samples];
      double[] y = new double[samples];
      double[] sample = new double[samples];
      for (int i = 0; i < samples; i++) {
        sample[i] = sampleIndex[i];
        double phi = Math.toRadians (Math.abs (latData[i]));
        double r = Math.tan (Math.PI/2 - phi);
        double theta = Math.toRadians (lonData[i]);
        x[i] = r*Math.cos (theta);
        y[i] = r*Math.sin (theta);
      } // for

      // Create interpolators
      // --------------------
      xInterp = new LagrangeInterpolator (sample, x);
      yInterp = new LagrangeInterpolator (sample, y);

    } // LongitudeInterpolator constructor

    ////////////////////////////////////////////////////////

    public double evaluate (
      double[] variables
    ) {

      double x = xInterp.evaluate (variables);
      double y = yInterp.evaluate (variables);
      double theta = Math.atan2 (y, x);
      return (Math.toDegrees (theta));

    } // evaluate

    ////////////////////////////////////////////////////////

  } // LongitudeInterpolator class

  ////////////////////////////////////////////////////////////

  // TODO: Should we add a static interpolation routine here to
  // be used by other classes?  It would take the navigation data
  // (possibly latitude data as well) and perform interpolation.

  ////////////////////////////////////////////////////////////

  /**
   * Interpolates navigation data using the specified raw navigation
   * data and sample range.  
   *
   * @param navData the navigation data.
   * @param lineIndex the scan line index in the range [0..lines-1].
   * @param variable the navigation variable to interpolate.  Special
   * actions may be taken depending on the variable.
   * @param start the starting sensor data sample.
   * @param count the total number of sensor data samples.
   *
   * @return an array of navigation angles, interpolated so that
   * each sample in the specified range.
   *
   * @see ScanLine#interpolateNavigation(float[],int)
   */
  public float[] interpolateNavigation (
    float[] navData,
    int lineIndex,
    int variable,
    int start,
    int count
  ) {

    // Setup for longitude interpolation
    // ---------------------------------
    float[] latData = null;
    Filter filter = null;
    if (variable == ScanLine.LONGITUDE) {

      // Get latitude data
      // -----------------
      ScanLine line;
      try { line = getScanLine (lineIndex, start, count); }
      catch (IOException e) { line = null; }
      if (line == null || !line.isNavigationUsable()) {
        /**
         * We allow this next call to throw a RuntimeException because
         * we should never get to the point of this method failing, so
         * if we did, there's some problem!  For example, think about
         * the case when a method calls this one to interpolate
         * longitude data but no latitude data is available for the
         * scan line -- that just doesn't happen because no longitude
         * data would be available either.
         */
        latData = interpolateRawNavigation (lineIndex, ScanLine.LATITUDE); 
      } // if
      else {
        latData = line.getRawNavigation (ScanLine.LATITUDE);
      } // else

      // Set special longitude filter
      // ----------------------------
      filter = LON_FILTER;

    } // if
    
    // Create navigation sample array
    // ------------------------------
    float[] sampleData = new float[count];

    // Create temporary index arrays
    // -----------------------------
    int[] rawIndex = new int[5];
    int[] sampleIndex = new int[5];
    
    // Setup initial interpolator
    // --------------------------
    Function interpolator = null;

    // Loop over each sample value
    // ---------------------------
    for (int i = 0; i < count; i++) {
      int sample = start + i;
      
      // Check for interpolator reset
      // ----------------------------
      if (interpolator == null) {
        int interpCount;

        // Create 5-point interpolator
        // ---------------------------
        if (sample < navigationStart || sample > navigationEnd) {
          interpCount = 5;

          // Use first 5 navigation values
          // -----------------------------
          if (sample < navigationStart) {
            for (int j = 0; j < 5; j++) {
              sampleIndex[j] = navigationStart + j*navigationStep;
              rawIndex[j] = j;
            } // for
          } // if

          // Use last 5 navigation values
          // ----------------------------
          else {
            for (int j = 0; j < 5; j++) {
              sampleIndex[j] = navigationEnd - j*navigationStep;
              rawIndex[j] = (ScanLine.NAVIGATION_VALUES - 1) - j;
            } // for
          } // else
          
        } // if

        // Create 3-point interpolator
        // ---------------------------
        else {
          interpCount = 3;
          
          // Get center navigation index
          // ---------------------------
          int centerIndex = ((sample - navigationStart) / navigationStep);
          int sampleDistance = (sample - navigationStart) - 
            centerIndex*navigationStep;
          if (sampleDistance > (navigationStep/2) - 1)
            centerIndex++;
          if (centerIndex == 0) 
            centerIndex = 1;
          else if (centerIndex == (ScanLine.NAVIGATION_VALUES - 1))
            centerIndex = ScanLine.NAVIGATION_VALUES - 2;

          // Get navigation values
          // ---------------------
          for (int j = 0; j < 3; j++) {
            sampleIndex[j] = navigationStart + 
              ((centerIndex - 1)+j)*navigationStep;
            rawIndex[j] = (centerIndex - 1)+j;
          } // for

        } // else
          
        // Check for polar longitude interpolation
        // ---------------------------------------
        boolean isPolar = false;
        if (variable == ScanLine.LONGITUDE) {
          for (int j = 0; j < interpCount; j++) {
            if (Math.abs (latData[rawIndex[j]]) > 87) {
               isPolar = true;
              break;
            } // if
          } // for
        } // if
        
        // Create polar longitude interpolator
        // -----------------------------------
        if (isPolar) {
          int[] sampleInterpData = new int[interpCount];
          float[] latInterpData = new float[interpCount];
          float[] lonInterpData = new float[interpCount];
          for (int j = 0; j < interpCount; j++) {
            sampleInterpData[j] = sampleIndex[j];
            latInterpData[j] = latData[rawIndex[j]];
            lonInterpData[j] = navData[rawIndex[j]];
          } // for
          interpolator = new LongitudeInterpolator (sampleInterpData,
            latInterpData, lonInterpData);
        } // if

        // Create regular Lagrangian interpolator
        // --------------------------------------
        else {
          double[] x = new double[interpCount];
          double[] y = new double[interpCount];
          for (int j = 0; j < interpCount; j++) {
            x[j] = sampleIndex[j];
            y[j] = navData[rawIndex[j]];
          } // for
          if (filter != null) filter.filter (y);
          interpolator = new LagrangeInterpolator (x, y);
        } // else

      } // if

      // Interpolate navigation data
      // ---------------------------
      sampleData[i] = (float) interpolator.evaluate (new double[] {sample});

      // Reset interpolator
      // ------------------
      if (sample == navigationStart-1 || sample == navigationEnd)
        interpolator = null;
      else if (sample > navigationStart && sample < navigationEnd) {
        int centerIndex = ((sample - navigationStart) / navigationStep);
        int sampleDistance = (sample - navigationStart) - 
          centerIndex*navigationStep;
        if (sampleDistance == (navigationStep/2) - 1)
          interpolator = null;
      } // else

    } // for

    return (sampleData);

  } // interpolateNavigation

  ////////////////////////////////////////////////////////////

  /**
   * The scan line class may be used to retrieve specific data from
   * one scan line of the dataset.
   */
  public abstract class ScanLine {

    // Constants
    // ---------

    /** The total number of navigation data values. */
    public static final int NAVIGATION_VALUES = 51;

    /** The AVHRR channel 1 variable. */
    public static final int AVHRR_CH1 = 1;

    /** The AVHRR channel 2 variable. */
    public static final int AVHRR_CH2 = 2;

    /** The AVHRR channel 3 variable. */
    public static final int AVHRR_CH3 = 3;

    /** The AVHRR channel 3a variable. */
    public static final int AVHRR_CH3A = 3;

    /** The AVHRR channel 4 variable. */
    public static final int AVHRR_CH4 = 4;

    /** The AVHRR channel 5 variable. */
    public static final int AVHRR_CH5 = 5;

    /** The solar zenith angle variable. */
    public static final int SOLAR_ZENITH = 6;

    /** The satellite zenith angle variable. */
    public static final int SATELLITE_ZENITH = 7;

    /** The relative azimuth angle variable. */
    public static final int RELATIVE_AZIMUTH = 8;

    /** The CLAVR cloud mask variable. */
    public static final int CLOUD = 9;

    /** The latitude angle variable. */
    public static final int LATITUDE = 10;

    /** The longitude angle variable. */
    public static final int LONGITUDE = 11;

    /** The scan time variable. */
    public static final int SCAN_TIME = 12;

    /** The scan line number attribute (Integer). */
    public static final int SCAN_LINE_NUMBER = 0;

    /** The quality indicator attribute (Long). */
    public static final int QUALITY_INDICATOR = 1;

    /** The scan line year (Integer). */
    public static final int SCAN_LINE_YEAR = 2;

    /** The scan line day of year, [1..366] (Integer). */
    public static final int SCAN_LINE_DAY = 3;

    /** The scan line UTC time in milliseconds (Long). */
    public static final int SCAN_LINE_MILLISECOND = 4;

    /** The ch3 select attribute: 0 = 3b, 1 = 3a, 2 = transition (Integer). */
    public static final int CH3_SELECT = 5;

    // Variables
    // ---------

    /** The scan line data. */
    protected ByteBuffer data;

    /** The sample start and count values. */
    protected int start, count;

    /** The scan line index (we don't trust the dataset stored value). */
    private int lineIndex;

    ////////////////////////////////////////////////////////

    /** Gets the scan line index, starting at 0. */
    public int getIndex () { return (lineIndex); }

    ////////////////////////////////////////////////////////

    /** Gets the raw scan line data. */
    public ByteBuffer getRawData () { return (data); }

    ////////////////////////////////////////////////////////

    /** Gets the sensor data usability flag. */
    public abstract boolean isSensorDataUsable ();

    ////////////////////////////////////////////////////////

    /** Gets the navigation data usability flag. */
    public abstract boolean isNavigationUsable ();

    ////////////////////////////////////////////////////////

    /**
     * Creates a new dataset scan line using the specified index.<p>
     *
     * Note that this code is now using the java.nio package and
     * individually allocated buffers for each scan line rather
     * than OS memory mapped buffers.  As such the start and
     * count parameters are ignored when the scan line is read,
     * and replaced with 0 and 2048 or 409 respectively.  This
     * should be remedied in a future release by removing the
     * start and count parameters entirely.
     *
     * @param line the scan line index in the range [0..lines-1].
     * @param start the starting sensor data sample.
     * @param count the total number of sensor data samples.  If the
     * count is 0, no sensor data is read.
     *
     * @throws IOException if an error occurred reading the file
     * data, or if a missing scan line was requested.  Generally,
     * users should not invoke this method, but use {@link
     * NOAA1bReader#getScanLine} instead.
     *
     * @see NOAA1bReader#getScanLine
     * @see NOAA1bReader#getLines
     */
    public ScanLine (
      int line,
      int start,
      int count
    ) throws IOException {

      // Check line index
      // ----------------
      if (line < 0 || line > lines-1)
        throw new IOException ("Requested scan line is out of range");

      // Check for missing line
      // ----------------------
      if (scanLineMap [line] == -1)
        throw new IOException ("Scan line " + line + " is missing");

      // Initialize
      // ----------
      this.data = getDataBuffer (getRecordStart (scanLineMap [line]), 
        recordSize);
      /*
      this.start = start;
      this.count = count;
      */
      if (start != 0 || (count != 0 && count != samples))
        throw new IllegalArgumentException (
          "Scan line subsampling has been deprecated (start = " + start +
          ", count = " + count + ")");
      this.start = 0;
      this.count = samples;
      this.lineIndex = line;

    } // ScanLine constructor

    ////////////////////////////////////////////////////////

    /** 
     * Gets the CLAVR cloud mask data.
     *
     * @return an array of cloud mask data values.
     */
    public abstract byte[] getCloud ();

    ////////////////////////////////////////////////////////

    /** 
     * Gets the scan line time data.
     *
     * @return an array of time data values in milliseconds.
     */
    public abstract long[] getScanTime ();

    ////////////////////////////////////////////////////////

    /** 
     * Gets a scan line attribute.
     *
     * @param index the attribute index.
     *
     * @return the attribute as an object.  Primitive types are wrapped in
     * their corresponding objects.
     */
    public abstract Object getAttribute (
      int index
    );

    ////////////////////////////////////////////////////////

    /**
     * Gets the uncalibrated sensor count data for the specified
     * channel.
     *
     * @param channel the channel to retrieve, [1..5].
     *
     * @return an array of uncalibrated sample count values.
     */
    public int[] getRawChannel (
      int channel
    ) {

      // Check for required data
      // -----------------------
      if (count == 0)
        throw new RuntimeException ("No sensor data available");

      // Create raw count data array
      // ---------------------------
      int[] countData = new int[count];
      int sampleIndex;
      switch (sensorWordSize) {

      // Read 8-bit sample values
      // ------------------------
      case 8:
        sampleIndex = start*5 + (channel-1);
        for (int i = 0; i < count; i++) {
          countData[i] = getUByte (data, attributeDataSize + sampleIndex)*4;
          sampleIndex += 5;
        } // for
        break;

      // Read 10-bit sample values
      // -------------------------
      case 10:
        int sampleOffset = ((start * 5) / 3) * 4;
        int samplePosition = (start * 5) % 3;
        samplePosition += (channel - 1);
        sampleOffset += (samplePosition / 3) * 4;
        samplePosition %= 3;
        for (int i = 0; i < count; i++) {

          // Get raw 10-bit value
          // --------------------
          int word = getInt (data, attributeDataSize + sampleOffset);
          switch (samplePosition) {
          case 0: countData[i] = (0x3ff00000 & word) >>> 20; break;
          case 1: countData[i] = (0x000ffc00 & word) >>> 10; break;
          case 2: countData[i] = (0x000003ff & word); break;
          } // switch

          // Increment sample indices
          // ------------------------
          samplePosition += 5;
          sampleOffset += (samplePosition / 3) * 4;
          samplePosition %= 3;

        } // for
        break;

      // Read 16-bit sample values
      // -------------------------
      case 16:
        sampleIndex = (start*5 + (channel-1))*2;
        for (int i = 0; i < count; i++) {
          countData[i] = getUShort (data, attributeDataSize + sampleIndex);
          sampleIndex += 10;
        } // for
        break;

      } // switch

      return (countData);

    } // getRawChannel

    ////////////////////////////////////////////////////////

    /** 
     * Calibrates channel count data to scientific units.  Visible
     * channel data is calibrated to percent albedo and thermal data
     * to degrees Kelvin.
     *
     * @param countData the count data as an integer array.
     * @param channel the channel to calibrate, [1..5].
     *
     * @return an array of calibrated sample values.
     *
     * @see #getRawChannel
     */
    public abstract float[] calibrateChannel (
      int[] countData,
      int channel
    );

    ////////////////////////////////////////////////////////

    /**
     * Gets the set of line-specific calibration coefficients for the
     * specified channel.  The order of coefficients is not important,
     * as the coefficients are simply used as a "calibration"
     * signature for use in creating count versus calibrated value
     * lookup tables.
     * 
     * @param channel the channel for calibration coefficients, [1..5].
     *
     * @return an array of calibration coefficients.
     */
    protected abstract float[] getCalibration (
      int channel
    );

    ////////////////////////////////////////////////////////
    
    /** 
     * Gets a lookup table for calibrating the specified channel.  A
     * cache of lookup tables is used, and a new lookup table is
     * computed only when needed.
     *
     * @param channel the channel lookup table to retrieve, [1..5].
     *
     * @return a lookup table to translate integer channel count
     * values to floating-point values in scientific units.
     */
    private float[] getLookup (
      int channel 
    ) {

      // Create new lookup table
      // -----------------------
      ScanLineCalibration cal = 
        new ScanLineCalibration (getCalibration (channel));
      float[] lookup = (float[]) lookupTableMaps[channel-1].get (cal);
      if (lookup == null) {
        lookup = calibrateChannel (lookupData, channel);
        lookupTableMaps[channel-1].put (cal, lookup);
      } // if

      return (lookup);

    } // getLookup

    ////////////////////////////////////////////////////////

    /**
     * Gets the calibrated sensor data for the specified channel.
     * Visible channel data is calibrated to percent albedo and
     * thermal data to degrees Kelvin.
     *
     * @param channel the channel to retrieve, [1..5].
     *
     * @return an array of calibrated sample values.
     *
     * @see #getRawChannel
     * @see #calibrateChannel
     */
    public float[] getChannel (
      int channel
    ) {

      // Get raw channel and calibrate
      // -----------------------------
      int[] countData = getRawChannel (channel);
      float[] sampleData = new float[countData.length];
      float[] lookup = getLookup (channel);
      for (int i = 0; i < countData.length; i++) 
        sampleData[i] = lookup[countData[i]];

      return (sampleData);

    } // getChannel

    ////////////////////////////////////////////////////////

    /**
     * Gets the raw uninterpolated navigation data for the specified
     * angle variable.  All angles are measured in degrees.
     *
     * @param variable the navigation variable.
     *
     * @return an array of raw uninterpolated navigation angles.
     */
    public abstract float[] getRawNavigation (
      int variable
    );

    ////////////////////////////////////////////////////////

    /**
     * Interpolates navigation data using the specified raw navigation
     * data and sample range specified when this scan line was
     * created.
     *
     * @param navData the navigation data.
     * @param variable the navigation variable to interpolate.  Special
     * actions may be taken depending on the variable.
     *
     * @return an array of navigation angles, interpolated so that
     * each sample in this scan line has a corresponding navigation
     * angle.
     *
     * @see NOAA1bReader#interpolateNavigation(float[],int,int,int,int)
     * @see #getRawNavigation
     */
    public float[] interpolateNavigation (
      float[] navData,
      int variable
    ) {

      return (NOAA1bReader.this.interpolateNavigation (navData, lineIndex, 
        variable, start, count));

    } // interpolateNavigation

    ////////////////////////////////////////////////////////

    /**
     * Gets the interpolated navigation data for the specified angle
     * variable.  All angles are measured in degrees.
     *
     * @param variable the navigation variable.
     *
     * @return an array of navigation angles, interpolated so that
     * each sample in this scan line has a corresponding navigation
     * angle.
     */
    public float[] getNavigation (
      int variable
    ) {

      // Get raw navigation and interpolate
      // ----------------------------------
      float[] navData = getRawNavigation (variable);
      float[] sampleData = interpolateNavigation (navData, variable);
      return (sampleData);

    } // getNavigation

    ////////////////////////////////////////////////////////

  } // ScanLine class

  ////////////////////////////////////////////////////////////

  /**
   * The data header class may be used to retrieve the header of the
   * dataset.
   */
  public abstract class DataHeader {

    // Constants
    // ---------
    /** The spacecraft ID attribute (String). */
    public static final int SPACECRAFT_ID = 0;

    /** The data type code attribute (String). */
    public static final int DATA_TYPE_CODE = 1;

    /** The dataset starting year attribute (Integer). */
    public static final int START_YEAR = 2;

    /** The dataset starting Julian day attribute: [1..366] (Integer). */
    public static final int START_DAY = 3;

    /** The dataset starting time in milliseconds attribute (Long). */
    public static final int START_MILLISECOND = 4;

    /** The number of data records attribute (Integer). */
    public static final int DATA_RECORDS = 5;

    /** The count of data gaps attribute (Integer). */
    public static final int DATA_GAPS = 6;

    /** The dataset name attribute (String). */
    public static final int DATASET_NAME = 7;

    // Variables
    // ---------

    /** The header data. */
    protected ByteBuffer data;

    ////////////////////////////////////////////////////////

    /** Gets the raw header data. */
    public ByteBuffer getRawData () { return (data); }

    ////////////////////////////////////////////////////////

    /**
     * Creates a new dataset header.
     *
     * @throws IOException if an error occurred reading the file data.
     */
    public DataHeader () throws IOException {

      // Initialize
      // ----------
      int position = (archive ? archiveHeaderSize : 0);
      this.data = getDataBuffer (position, recordSize);

    } // DataHeader constructor

    ////////////////////////////////////////////////////////

    /** 
     * Gets a header attribute.
     *
     * @param index the attribute index.
     *
     * @return the attribute as an object.  Primitive types are wrapped in
     * their corresponding objects.
     */
    public abstract Object getAttribute (
      int index
    );

    ////////////////////////////////////////////////////////

  } // DataHeader class

  ////////////////////////////////////////////////////////////

  /**
   * The archive header class may be used to retrieve specific data
   * from the archive header of the dataset. Note that not all
   * datasets have archive headers.
   *
   * @see NOAA1bReader#isArchive
   */
  public abstract class ArchiveHeader {

    /** The official archive dataset name attribute (String). */
    public static final int DATASET_NAME = 0;

    /** The dataset starting hour attribute: [0..23] (Integer). */
    public static final int START_HOUR = 1;

    /** The dataset starting minute attribute: [00..59] (Integer). */
    public static final int START_MINUTE = 2;

    /** The dataset duration in minutes attribute (Integer). */
    public static final int DURATION_MINUTES = 3;

    /** The sensor data word size attribute: 8, 10, or 16 bits (Integer). */
    public static final int SENSOR_DATA_WORD_SIZE = 4;

    // Variables
    // ---------

    /** The archive header data. */
    protected ByteBuffer data;

    ////////////////////////////////////////////////////////

    /** Gets the raw header data. */
    public ByteBuffer getRawData () { return (data); }

    ////////////////////////////////////////////////////////

    /**
     * Creates a new archive header.
     *
     * @throws IOException if an error occurred reading the file data.
     */
    public ArchiveHeader () throws IOException {

      // Check for archive header
      // ------------------------
      if (!archive) 
        throw new IOException ("No archive header detected");

      // Initialize
      // ----------
      this.data = getDataBuffer (0, archiveHeaderSize);

    } // ArchiveHeader constructor

    ////////////////////////////////////////////////////////

    /** 
     * Gets a header attribute.
     *
     * @param index the attribute index.
     *
     * @return the attribute as an object.  Primitive types are wrapped in
     * their corresponding objects.
     */
    public abstract Object getAttribute (
      int index
    );

    ////////////////////////////////////////////////////////

  } // ArchiveHeader class

  ////////////////////////////////////////////////////////////

  /**
   * Interpolates the raw navigation data values for the specified
   * scan line.  Generally, this is only required if continuous
   * navigation data is critical in some application.  The
   * interpolation employs a 4-point Lagrange method using the closest
   * lines with valid navigation both before and after the specified
   * line.  
   *
   * @param line the desired line for interpolation.  
   * @param variable the navigation variable for interpolation.  
   *
   * @return the array of interpolated raw navigation data values.
   *
   * @throws RuntimeException if not enough valid navigation lines are
   * available close to the specified line to perform an interpolation.
   *
   * @see ScanLine#getRawNavigation
   */
  public float[] interpolateRawNavigation (
    int line,
    int variable
  ) {

    // TODO: Should we really have this routine in here?  There are
    // problems with accuracy when we're trying to interpolate too far
    // away from known scan lines.

    //if (true) throw new RuntimeException();


    // Find closest lines
    // ------------------
    ScanLine[] scanLines = new ScanLine[NAVIGATION_SAMPLES];
    int count = 0;
    int radius = 1;
    while (count < NAVIGATION_SAMPLES && radius < lines) {
      for (int i = 0; i < 2; i++) {
        if (count == NAVIGATION_SAMPLES) break;
        ScanLine scanLine;
        try { scanLine = getScanLine (line + (i == 0 ? -1 : 1)*radius, 0, 0); }
        catch (IOException e) { scanLine = null; }
        if (scanLine != null && scanLine.isNavigationUsable()) {
          scanLines[count] = scanLine;
          count++;
        } // if
      } // for
      radius += 1;
    } // while

    // Check maximum line distance
    // ---------------------------
    int maxDistance = 0;
    for (int i = 0; i < NAVIGATION_SAMPLES; i++) {
      int distance = Math.abs (scanLines[i].getIndex() - line);
      if (distance > maxDistance) maxDistance = distance;
    } // for
    if (maxDistance > NAVIGATION_MAXDIST)
      throw new RuntimeException ("Maximum line distance tolerance exceeded");

    // Get navigation data samples
    // ---------------------------
    float[][] navDataSamples = new float[NAVIGATION_SAMPLES][];
    for (int i = 0; i < NAVIGATION_SAMPLES; i++) {
      navDataSamples[i] = scanLines[i].getRawNavigation (variable);
    } // for

    // Create raw navigation data
    // --------------------------
    float navData[] = new float[ScanLine.NAVIGATION_VALUES];

    // Set x values
    // ------------
    double[] x = new double[NAVIGATION_SAMPLES];
    for (int i = 0; i < NAVIGATION_SAMPLES; i++)
      x[i] = scanLines[i].getIndex();

    // Interpolate each navigation value
    // ---------------------------------
    for (int j = 0; j < ScanLine.NAVIGATION_VALUES; j++) {
      double[] y = new double[NAVIGATION_SAMPLES];
      for (int i = 0; i < NAVIGATION_SAMPLES; i++)
        y[i] = navDataSamples[i][j];
      LagrangeInterpolator interpolator = new LagrangeInterpolator (x, y);
      navData[j] = (float) interpolator.evaluate (new double[] {line});
    } // for

    return (navData);

  } // interpolateRawNavigation

  ////////////////////////////////////////////////////////////

  protected DataVariable getPreviewImpl (
    int index
  ) throws IOException {

    // Set string properties
    // ---------------------
    String varName = variables[index];
    String longName = getLongName (varName);
    String units = getUnits (varName);

    // Set numerical properties
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
    else if (varName.equals ("cloud")) {
      data = new byte[1];
      format = new DecimalFormat ("0");
      scaling = new double[] {1, 0};
      missing = new Byte ((byte)0);
    } // else if
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
 
    return (new Grid (varName, longName, units, lines, 
      samples, data, format, scaling, missing));

    // TODO: Should we worry about using unsigned bytes for cloud
    // data?

  } // getPreviewImpl

  ////////////////////////////////////////////////////////////

  public DataVariable getVariable (
    int index
  ) throws IOException {

    // Get a preview and convert to cached grid
    // ----------------------------------------
    DataVariable var = getPreview (index);
    return (new NOAA1bCachedGrid ((Grid) var, this));

  } // getVariable

  ////////////////////////////////////////////////////////////

  public void close () throws IOException { 

    inputChannel.close(); 
    scanLineCache.clear();
    header = null;
    for (HashMap map : lookupTableMaps) map.clear();

  } // close

  ////////////////////////////////////////////////////////////

} // NOAA1bReader class

////////////////////////////////////////////////////////////////////////
