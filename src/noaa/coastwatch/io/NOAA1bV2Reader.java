////////////////////////////////////////////////////////////////////////
/*
     FILE: NOAA1bV2Reader.java
  PURPOSE: A class to read NOAA 1b format version 2 files.
   AUTHOR: Peter Hollemans
     DATE: 2003/02/03
  CHANGES: 2003/03/28, PFH, added GAC and CLAVR cloud support
           2004/04/10, PFH, added getDataFormat() method
           2004/11/29, PFH, added getCalibration()
           2005/02/15, PFH, modified to help extend for V3 reader
           2005/05/12, PFH, modified format description check for leniency
           2006/01/27, PFH, modified format description check for version
           2006/08/28, PFH, updated to use java.nio classes
           2006/06/21, PFH, added support for missing scan lines
           2006/12/27, PFH, added scan line time data reading
           2006/12/30, PFH, added isNavigationUsable() for constructor use
           2007/07/03, PFH, added check for NOAA-15 "verion 1" format problem
           2007/12/15, PFH, added scan line caching

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
import java.util.*;
import java.text.*;
import java.lang.reflect.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.NOAA1bReader.*;

/**
 * A NOAA 1b version 2 reader is a NOAA 1b reader that reads NOAA
 * 1b format KLM GAC/LAC/HRPT data files available from the
 * NOAA/NESDIS Satellite Active Archive.  These files are
 * characterized by an optional 512 byte Archive Retrieval System
 * (ARS) header followed by data header and records of
 * 12288/15872/22528 bytes for 8/10/16-bit LAC or 3584/4608/5632
 * bytes for 8/10/16-bit GAC.  The NOAA 1b version 2 format
 * provides more information than version 1 including more view
 * angles, orbital model information, an extra channel 3, a more
 * complete set of calibration parameters, and so on.
 *
 * @see NOAA1bV1Reader
 */
public class NOAA1bV2Reader
  extends NOAA1bReader {

  // Constants
  // ---------

  /** The archive header size in bytes. */
  private static final int ARCHIVE_HEADER_SIZE = 512;

  /** The header and data record size for LAC in bytes. */
  private static final int LAC_8BIT_RECORD_SIZE = 12288;
  private static final int LAC_10BIT_RECORD_SIZE = 15872;
  private static final int LAC_16BIT_RECORD_SIZE = 22528;

  /** The header and data record size for GAC in bytes. */
  private static final int GAC_8BIT_RECORD_SIZE = 3584;
  private static final int GAC_10BIT_RECORD_SIZE = 4608;
  private static final int GAC_16BIT_RECORD_SIZE = 5632;

  /** The scan line attribute data size. */
  private static final int ATTRIBUTE_DATA_SIZE = 1264;

  /** The scan line cloud data offset for LAC in bytes. */
  private static final int LAC_8BIT_CLOUD_DATA_OFFSET = 11560;
  private static final int LAC_10BIT_CLOUD_DATA_OFFSET = 14984;
  private static final int LAC_16BIT_CLOUD_DATA_OFFSET = 21800;;

  /** The scan line cloud data offset for GAC in bytes. */
  private static final int GAC_8BIT_CLOUD_DATA_OFFSET = 3360;
  private static final int GAC_10BIT_CLOUD_DATA_OFFSET = 4056;
  private static final int GAC_16BIT_CLOUD_DATA_OFFSET = 5408;

  /** The data format string supported by this class. */
  public static final String DATA_FORMAT = "NOAA Level 1b v2";

  /** The dataset variables names. */
  private static final String[] VARIABLE_NAMES = new String[] {
    "avhrr_ch1",
    "avhrr_ch2",
    "avhrr_ch3a",
    "avhrr_ch3",
    "avhrr_ch4",
    "avhrr_ch5",
    "sun_zenith",
    "sat_zenith",
    "rel_azimuth",
    "cloud",
    "latitude",
    "longitude",
    "scan_time"
  };

  /** The data format description. */
  private static final String READER_DATA_FORMAT = "NOAA 1b version 2";

  // Variables
  // ---------

  /** The cloud data offset. */
  protected int cloudDataOffset;

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (READER_DATA_FORMAT); }

  ////////////////////////////////////////////////////////////

  protected String[] getVariableNames () { return (VARIABLE_NAMES); }

  ////////////////////////////////////////////////////////////

  /** 
   * Checks for an archive header and returns true if so.  This method
   * reads the ARS header which contains only printable ASCII
   * characters.
   */
   protected boolean getArchiveFlag () throws IOException {

    // Check for non-printable ASCII characters
    // ----------------------------------------
    boolean printable = true;
    ByteBuffer inputBuffer = getDataBuffer (0, ARCHIVE_HEADER_SIZE);
    for (int i = 0; i < ARCHIVE_HEADER_SIZE; i++) {
      if (!isPrint (inputBuffer.get (i))) {
        printable = false;
        break;
      } // if
    } // for

    return (printable);

  } // getArchiveFlag

  ////////////////////////////////////////////////////////////

  /** Gets the data format version. */
  protected int getDataFormatVersion () { return (2); }

  ////////////////////////////////////////////////////////////

  /** Checks the data format description. */
  protected boolean isValidFormatDescription (String format) {

    String ver = Integer.toString (getDataFormatVersion());
    String lower = format.toLowerCase();
    if (lower.indexOf ("noaa") != -1 &&
      lower.indexOf ("1b") != -1 &&
      (lower.indexOf ("version " + ver) != -1 || 
      lower.indexOf ("v" + ver) != -1))
      return (true);
    else
      return (false);

  } // isValidFormatDescription

  ////////////////////////////////////////////////////////////

  protected void checkFormat () throws IOException {

    // Initialize known constants
    // --------------------------
    archiveHeaderSize = ARCHIVE_HEADER_SIZE;
    attributeDataSize = ATTRIBUTE_DATA_SIZE;

    // Initialize sensor word size and version kludge
    // ----------------------------------------------
    sensorWordSize = -1;
    boolean isActuallyVersion2 = false;

    // Check archive header
    // --------------------
    if (archive) {
      ArchiveHeader header = getArchiveHeader();

      // Get format string
      // -----------------
      /**
       * We have to insert a kludge here because some NOAA-15
       * files in the CLASS archive are marked as "NOAA 1b v1"
       * when really they should be "v2" since version 1 data
       * files have a completely different archive header and
       * record size.  So we check the file name and look for
       * "NK".  If we find it, we change the format string to
       * read "v2".
       */
      String fileName = (String) header.getAttribute (
        ArchiveHeader.DATASET_NAME);
      String format = ((String) header.getAttribute (
        ArchiveHeaderV2.DATA_FORMAT)).toLowerCase();
      isActuallyVersion2 = (format.contains ("v1") || 
        format.contains ("version 1")) && fileName.contains (".NK.");
      if (isActuallyVersion2)
        format = "NOAA 1b v2";
    
      // Check format string
      // -------------------
      if (!isValidFormatDescription (format))
        throw new IOException ("Unsupported data format: '" + format + "'");

      // Check sensor word size
      // ----------------------
      sensorWordSize = ((Integer) header.getAttribute (
        ArchiveHeader.SENSOR_DATA_WORD_SIZE)).intValue();
      switch (sensorWordSize) {
      case 8: case 10: case 16: break;
      default:
        throw new IOException ("Unsupported sensor word size: " + 
          sensorWordSize);
      } // switch

      // Check data record size
      // ----------------------
      int recordSize = ((Integer) header.getAttribute (
        ArchiveHeaderV2.RECORD_SIZE)).intValue();
      switch (recordSize) {
      case LAC_8BIT_RECORD_SIZE:
      case LAC_10BIT_RECORD_SIZE:
      case LAC_16BIT_RECORD_SIZE:
      case GAC_8BIT_RECORD_SIZE:
      case GAC_10BIT_RECORD_SIZE:
      case GAC_16BIT_RECORD_SIZE:
        break;
      default:
        throw new IOException ("Unsupported record size: " + recordSize);
      } // switch

    } // if

    // Get data header
    // ---------------
    recordSize = GAC_10BIT_RECORD_SIZE;
    DataHeader header = getDataHeader();

    // Get data header count
    // ---------------------
    dataHeaders = ((Integer) header.getAttribute (
      DataHeaderV2.HEADER_RECORDS)).intValue();

    // Check spacecraft
    // ----------------
    String spacecraft = (String) header.getAttribute (
      DataHeader.SPACECRAFT_ID);
    if (spacecraft.equals ("unknown")) 
      throw new IOException ("Unknown spacecraft ID");

    // Get format version
    // ------------------
    /**
     * We have to do a format version kludge for NOAA-15 here
     * too, see the note above.
     */
    int version = ((Integer) header.getAttribute (
      DataHeaderV2.FORMAT_VERSION)).intValue();
    if (!archive) {
      String fileName = (String) header.getAttribute (
        DataHeader.DATASET_NAME);
      isActuallyVersion2 = (version == 1 && fileName.contains (".NK."));
    } // if
    if (isActuallyVersion2) version = 2;

    // Check format version
    // --------------------
    if (version != getDataFormatVersion())
      throw new IOException ("Unsupported data format version: " + version);

    // Set constants for LAC/HRPT data
    // -------------------------------
    String type = (String) header.getAttribute (DataHeader.DATA_TYPE_CODE);
    if (type.equals ("hrpt") || type.equals ("lac")) {

      // Determine sensor word size
      // --------------------------
      if (sensorWordSize == -1) {
        int dataRecords = ((Number) header.getAttribute (
          DataHeader.DATA_RECORDS)).intValue();
        int fileRecordSize = (int) (inputChannel.size()/(dataRecords + 
          dataHeaders));
        switch (fileRecordSize) {
        case LAC_8BIT_RECORD_SIZE:
          sensorWordSize = 8; break;
        case LAC_10BIT_RECORD_SIZE:
          sensorWordSize = 10; break;
        case LAC_16BIT_RECORD_SIZE:
          sensorWordSize = 16; break;
        default:
          throw new IOException ("Unsupported record size: " + fileRecordSize);
        } // switch
      } // if

      // Set record size
      // ---------------
      switch (sensorWordSize) {
      case 8: 
        recordSize = LAC_8BIT_RECORD_SIZE; 
        cloudDataOffset = LAC_8BIT_CLOUD_DATA_OFFSET;
        break;
      case 10: 
        recordSize = LAC_10BIT_RECORD_SIZE;
        cloudDataOffset = LAC_10BIT_CLOUD_DATA_OFFSET;
        break;
      case 16: 
        recordSize = LAC_16BIT_RECORD_SIZE; 
        cloudDataOffset = LAC_16BIT_CLOUD_DATA_OFFSET;
        break;
      } // switch

    } // if

    // Set constants for GAC data
    // --------------------------
    else if (type.equals ("gac")) {

      // Determine sensor word size
      // --------------------------
      if (sensorWordSize == -1) {
        int dataRecords = ((Number) header.getAttribute (
          DataHeader.DATA_RECORDS)).intValue();
        int fileRecordSize = (int) (inputChannel.size()/(dataRecords + 
          dataHeaders));
        switch (fileRecordSize) {
        case GAC_8BIT_RECORD_SIZE:
          sensorWordSize = 8; break;
        case GAC_10BIT_RECORD_SIZE:
          sensorWordSize = 10; break;
        case GAC_16BIT_RECORD_SIZE:
          sensorWordSize = 16; break;
        default:
          throw new IOException ("Unsupported record size: " + fileRecordSize);
        } // switch
      } // if

      // Set record size
      // ---------------
      switch (sensorWordSize) {
      case 8: 
        recordSize = GAC_8BIT_RECORD_SIZE; 
        cloudDataOffset = GAC_8BIT_CLOUD_DATA_OFFSET;
        break;
      case 10: 
        recordSize = GAC_10BIT_RECORD_SIZE;
        cloudDataOffset = GAC_10BIT_CLOUD_DATA_OFFSET;
        break;
      case 16: 
        recordSize = GAC_16BIT_RECORD_SIZE; 
        cloudDataOffset = GAC_16BIT_CLOUD_DATA_OFFSET;
        break;
      } // switch

    } // else

    // Report unsupported data type
    // ----------------------------
    else
      throw new IOException ("Unsupported data type: " + type);

  } // checkFormat

  ////////////////////////////////////////////////////////////

  public ArchiveHeader getArchiveHeader () throws IOException {

    return (new ArchiveHeaderV2());

  } // getArchiveHeader

  ////////////////////////////////////////////////////////////

  public DataHeader getDataHeader () throws IOException {

    return (new DataHeaderV2());

  } // getDataHeader

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a NOAA 1b version 2 reader from the specified file.
   *
   * @param file the file name to read.
   *
   * @throws IOException if an error occurred reading the file data.
   */
  public NOAA1bV2Reader (
    String file
  ) throws IOException {

    super (file);

  } // NOAA1bV2Reader constructor

  ////////////////////////////////////////////////////////////

  protected boolean isNavigationUsable (
    int record,
    ByteBuffer buffer
  ) throws IOException {

    buffer.clear().limit (4);
    inputChannel.read (buffer, getRecordStart (record) + 24);
    long quality = getUInt (buffer, 0);
    if ((quality & 0x08000000) != 0) return (false);
    return (true);

  } // isNavigationUsable

  ////////////////////////////////////////////////////////////

  public ScanLine getScanLine (
    int line,
    int start,
    int count
  ) throws IOException {

    ScanLine lineObject = scanLineCache.get (line);
    if (lineObject == null) {
      lineObject = new ScanLineV2 (line, start, count);
      scanLineCache.put (line, lineObject);
    } // if
    return (lineObject);

    //    return (new ScanLineV2 (line, start, count));

  } // getScanLine

  ////////////////////////////////////////////////////////////

  /**
   * The scan line version 2 class is a scan line that can read,
   * calibrate, and navigate NOAA-KLM data.
   */
  public class ScanLineV2
    extends ScanLine {

    // Constants
    // ---------
    /** The radiation constant c1. */
    private static final float C1 = 1.1910427e-5f;

    /** The radiation constant c2. */
    private static final float C2 = 1.4387752f;

    /** The channel 1 calibration slope 1 attribute (Float). */
    public static final int CH1_SLOPE1 = 101;

    /** The channel 1 calibration intercept 1 attribute (Float). */
    public static final int CH1_INTERCEPT1 = 102;

    /** The channel 1 calibration slope 2 attribute (Float). */
    public static final int CH1_SLOPE2 = 103;

    /** The channel 1 calibration intercept 2 attribute (Float). */
    public static final int CH1_INTERCEPT2 = 104;

    /** The channel 1 calibration intersection attribute (Long). */
    public static final int CH1_INTERSECTION = 105;

    /** The channel 2 calibration slope 1 attribute (Float). */
    public static final int CH2_SLOPE1 = 106;

    /** The channel 2 calibration intercept 1 attribute (Float). */
    public static final int CH2_INTERCEPT1 = 107;

    /** The channel 2 calibration slope 2 attribute (Float). */
    public static final int CH2_SLOPE2 = 108;

    /** The channel 2 calibration intercept 2 attribute (Float). */
    public static final int CH2_INTERCEPT2 = 109;

    /** The channel 2 calibration intersection attribute (Long). */
    public static final int CH2_INTERSECTION = 110;

    /** The channel 3a calibration slope 1 attribute (Float). */
    public static final int CH3A_SLOPE1 = 111;

    /** The channel 3a calibration intercept 1 attribute (Float). */
    public static final int CH3A_INTERCEPT1 = 112;

    /** The channel 3a calibration slope 2 attribute (Float). */
    public static final int CH3A_SLOPE2 = 113;

    /** The channel 3a calibration intercept 2 attribute (Float). */
    public static final int CH3A_INTERCEPT2 = 114;

    /** The channel 3a calibration intersection attribute (Long). */
    public static final int CH3A_INTERSECTION = 115;

    /** The channel 3b calibration coefficient 1 attribute (Float). */
    public static final int CH3B_COEFFICIENT1 = 116;

    /** The channel 3b calibration coefficient 2 attribute (Float). */
    public static final int CH3B_COEFFICIENT2 = 117;

    /** The channel 3b calibration coefficient 3 attribute (Float). */
    public static final int CH3B_COEFFICIENT3 = 118;

    /** The channel 4 calibration coefficient 1 attribute (Float). */
    public static final int CH4_COEFFICIENT1 = 119;

    /** The channel 4 calibration coefficient 2 attribute (Float). */
    public static final int CH4_COEFFICIENT2 = 120;

    /** The channel 4 calibration coefficient 3 attribute (Float). */
    public static final int CH4_COEFFICIENT3 = 121;

    /** The channel 5 calibration coefficient 1 attribute (Float). */
    public static final int CH5_COEFFICIENT1 = 122;

    /** The channel 5 calibration coefficient 2 attribute (Float). */
    public static final int CH5_COEFFICIENT2 = 123;

    /** The channel 5 calibration coefficient 3 attribute (Float). */
    public static final int CH5_COEFFICIENT3 = 124;

    /** The scan line quality attribute (Long). */
    public static final int SCAN_LINE_QUALITY = 126;

    /** The calibration quality for channel 3b attribute (Integer). */
    public static final int CALIBRATION_QUALITY_CH3B = 127;

    /** The calibration quality for channel 4 attribute (Integer). */
    public static final int CALIBRATION_QUALITY_CH4 = 128;

    /** The calibration quality for channel 5 attribute (Integer). */
    public static final int CALIBRATION_QUALITY_CH5 = 129;

    /** The navigation status attribute (Long). */
    public static final int NAVIGATION_STATUS = 130;

    /** The spacecraft altitude attribute (Float). */
    public static final int SPACECRAFT_ALTITUDE = 131;

    /** The navigation data geometry offset. */
    protected static final int NAVIGATION_GEOMETRY_OFFSET = 328;

    /** The navigation data location offset. */
    protected static final int NAVIGATION_LOCATION_OFFSET = 640;

    ////////////////////////////////////////////////////////

    public byte[] getCloud () { 

      // Create cloud data array
      // -----------------------
      byte[] cloudData = new byte[count];

      // Loop over each sample
      // ---------------------
      int sampleOffset = (start / 8) * 2;
      int samplePosition = start % 8;
      for (int i = 0; i < count; i++) {

        // Get raw 16-bit value
        // --------------------
        int word = getUShort (data, cloudDataOffset + sampleOffset);

        // Get cloud samples
        // -----------------
        while (samplePosition < 8 && i < count) {
          int shift = (7 - samplePosition) * 2;
          cloudData[i] = (byte) (((0x3 << shift) & word) >>> shift);
          samplePosition++;
          i++;
        } // while
        i--;

        // Increment sample indices
        // ------------------------
        sampleOffset += 2;
        samplePosition = 0;

      } // for

      return (cloudData);

    } // getCloud

    ////////////////////////////////////////////////////////

    /** 
     * Gets the scan line time data.  This implementation simply
     * returns the same time for all data samples across the scan
     * line.  A more sophisticated method would compute separate times
     * for each sample.
     *
     * @return an array of time data values in milliseconds.
     */
    public long[] getScanTime () {

      // Read the date and time
      // ----------------------
      int year = ((Integer) getAttribute (SCAN_LINE_YEAR)).intValue();
      int day = ((Integer) getAttribute (SCAN_LINE_DAY)).intValue();
      long millisecond = ((Long) getAttribute (
        SCAN_LINE_MILLISECOND)).longValue();

      // Compute time in milliseconds
      // ----------------------------
      Calendar cal = new GregorianCalendar (year, 0, 0, 0, 0, 0);
      cal.set (Calendar.DAY_OF_YEAR, day);
      cal.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
      long time = cal.getTimeInMillis() + millisecond;

      // Create time sample array
      // ------------------------
      long[] timeArray = new long[count];
      Arrays.fill (timeArray, time);

      return (timeArray);

    } // getScanTime

    ////////////////////////////////////////////////////////

    public Object getAttribute (
      int index
    ) {

      switch (index) {

      // Standard attributes
      // -------------------
      case SCAN_LINE_NUMBER: return (new Integer (getUShort (data, 0)));
      case QUALITY_INDICATOR: return (new Long (getUInt (data, 24)));
      case CH3_SELECT: return (new Integer (getUShort (data, 12) & 0x0f));
      case SCAN_LINE_YEAR: return (new Integer (getUShort (data, 2)));
      case SCAN_LINE_DAY: return (new Integer (getUShort (data, 4)));
      case SCAN_LINE_MILLISECOND: return (new Long (getUInt (data, 8)));

      // Format specific attributes
      // --------------------------
      case CH1_SLOPE1: return (new Float (getInt (data, 48) * 1e-7));
      case CH1_INTERCEPT1: return (new Float (getInt (data, 52) * 1e-6));
      case CH1_SLOPE2: return (new Float (getInt (data, 56) * 1e-7));
      case CH1_INTERCEPT2: return (new Float (getInt (data, 60) * 1e-6));
      case CH1_INTERSECTION: return (new Long (getInt (data, 64)));
      case CH2_SLOPE1: return (new Float (getInt (data, 108) * 1e-7));
      case CH2_INTERCEPT1: return (new Float (getInt (data, 112) * 1e-6));
      case CH2_SLOPE2: return (new Float (getInt (data, 116) * 1e-7));
      case CH2_INTERCEPT2: return (new Float (getInt (data, 120) * 1e-6));
      case CH2_INTERSECTION: return (new Long (getInt (data, 124)));
      case CH3A_SLOPE1: return (new Float (getInt (data, 168) * 1e-7));
      case CH3A_INTERCEPT1: return (new Float (getInt (data, 172) * 1e-6));
      case CH3A_SLOPE2: return (new Float (getInt (data, 176) * 1e-7));
      case CH3A_INTERCEPT2: return (new Float (getInt (data, 180) * 1e-6));
      case CH3A_INTERSECTION: return (new Long (getInt (data, 184)));
      case CH3B_COEFFICIENT1: return (new Float (getInt (data, 228) * 1e-6));
      case CH3B_COEFFICIENT2: return (new Float (getInt (data, 232) * 1e-6));
      case CH3B_COEFFICIENT3: return (new Float (getInt (data, 236) * 1e-6));
      case CH4_COEFFICIENT1: return (new Float (getInt (data, 252) * 1e-6));
      case CH4_COEFFICIENT2: return (new Float (getInt (data, 256) * 1e-6));
      case CH4_COEFFICIENT3: return (new Float (getInt (data, 260) * 1e-6));
      case CH5_COEFFICIENT1: return (new Float (getInt (data, 276) * 1e-6));
      case CH5_COEFFICIENT2: return (new Float (getInt (data, 280) * 1e-6));
      case CH5_COEFFICIENT3: return (new Float (getInt (data, 284) * 1e-6));
      case SCAN_LINE_QUALITY: return (new Long (getUInt (data, 28)));
      case CALIBRATION_QUALITY_CH3B: return (new Integer (getUShort (data, 
        32)));
      case CALIBRATION_QUALITY_CH4: return (new Integer (getUShort (data, 
        34)));
      case CALIBRATION_QUALITY_CH5: return (new Integer (getUShort (data, 
        36)));
      case NAVIGATION_STATUS: return (new Long (getUInt (data, 312)));
      case SPACECRAFT_ALTITUDE: return (new Float (getUShort (data, 326)*0.1));

      default:
        throw new IllegalArgumentException ("Unsupported attribute index");

      } // switch

    } // getAttribute

    ////////////////////////////////////////////////////////

    /**
     * Creates a new dataset scan line using the specified index.
     *
     * @param line the scan line index in the range [0..lines-1].
     * @param start the starting sensor data sample.
     * @param count the total number of sensor data samples.  If the
     * count is 0, no sensor data is read.
     *
     * @throws IOException if an error occurred reading the file data.
     *
     * @see NOAA1bReader#getScanLine
     * @see NOAA1bReader#getLines
     */
    public ScanLineV2 (
      int line,
      int start,
      int count
    ) throws IOException {

      super (line, start, count);

    } // ScanLineV2 constructor

    ////////////////////////////////////////////////////////

    public boolean isSensorDataUsable () {

      long quality = ((Long) getAttribute (QUALITY_INDICATOR)).longValue();
      return (quality == 0);

    } // isSensorDataUsable 

    ////////////////////////////////////////////////////////

    public boolean isNavigationUsable () {

      // Check quality indicator bits
      // ----------------------------
      long quality = ((Long) getAttribute (QUALITY_INDICATOR)).longValue();
      if ((quality & 0x08000000) != 0) return (false);

      /**
       * A note from the programmer: There are a number of additional
       * tests that may be performed to detect bad navigation.  But
       * from code testing these tests seem to eliminate too many good
       * navigation lines, causing the swath projection to have a
       * degenerate partition and fail on construction.  For now, we
       * leave these tests out in the interests of data usability.

      // Check scan line quality flags
      // -----------------------------
      long scanQuality = ((Long) getAttribute (SCAN_LINE_QUALITY)).longValue();
      if ((scanQuality & 0xff) != 0) return (false);

      // Check navigation status
      // -----------------------
      long navStatus = ((Long) getAttribute (NAVIGATION_STATUS)).longValue();
      if ((navStatus & 0xffff) != 0) return (false);

      */

      return (true);

    } // isNavigationUsable 

    ////////////////////////////////////////////////////////

    protected float[] getCalibration (
      int channel
    ) {

      // Check for channel 3a
      // --------------------
      if (channel == 3) {
        int select = ((Integer) getAttribute (CH3_SELECT)).intValue();
        if (select == 1) channel = 6;
      } // if

      // Get calibration coefficients
      // ----------------------------
      float[] coefs;
      switch (channel) {
      case 1:
        coefs = new float[5];
        coefs[0] = ((Float) getAttribute (CH1_SLOPE1)).floatValue();
        coefs[1] = ((Float) getAttribute (CH1_INTERCEPT1)).floatValue();
        coefs[2] = ((Float) getAttribute (CH1_SLOPE2)).floatValue();
        coefs[3] = ((Float) getAttribute (CH1_INTERCEPT2)).floatValue();
        coefs[4] = ((Long) getAttribute (CH1_INTERSECTION)).intValue();
        break;
      case 2:
        coefs = new float[5];
        coefs[0] = ((Float) getAttribute (CH2_SLOPE1)).floatValue();
        coefs[1] = ((Float) getAttribute (CH2_INTERCEPT1)).floatValue();
        coefs[2] = ((Float) getAttribute (CH2_SLOPE2)).floatValue();
        coefs[3] = ((Float) getAttribute (CH2_INTERCEPT2)).floatValue();
        coefs[4] = ((Long) getAttribute (CH2_INTERSECTION)).intValue();
        break;
      case 6:
        coefs = new float[5];
        coefs[0] = ((Float) getAttribute (CH3A_SLOPE1)).floatValue();
        coefs[1] = ((Float) getAttribute (CH3A_INTERCEPT1)).floatValue();
        coefs[2] = ((Float) getAttribute (CH3A_SLOPE2)).floatValue();
        coefs[3] = ((Float) getAttribute (CH3A_INTERCEPT2)).floatValue();
        coefs[4] = ((Long) getAttribute (CH3A_INTERSECTION)).intValue();
        break;
      case 3:
        coefs = new float[3];
        coefs[0] = ((Float) getAttribute (CH3B_COEFFICIENT1)).floatValue();
        coefs[1] = ((Float) getAttribute (CH3B_COEFFICIENT2)).floatValue();
        coefs[2] = ((Float) getAttribute (CH3B_COEFFICIENT3)).floatValue();
        break;
      case 4:
        coefs = new float[3];
        coefs[0] = ((Float) getAttribute (CH4_COEFFICIENT1)).floatValue();
        coefs[1] = ((Float) getAttribute (CH4_COEFFICIENT2)).floatValue();
        coefs[2] = ((Float) getAttribute (CH4_COEFFICIENT3)).floatValue();
        break;
      case 5:
        coefs = new float[3];
        coefs[0] = ((Float) getAttribute (CH5_COEFFICIENT1)).floatValue();
        coefs[1] = ((Float) getAttribute (CH5_COEFFICIENT2)).floatValue();
        coefs[2] = ((Float) getAttribute (CH5_COEFFICIENT3)).floatValue();
        break;
      default:
        throw new IllegalArgumentException ("Invalid channel: " + channel);
      } // switch

      return (coefs);

    } // getCalibration

    ////////////////////////////////////////////////////////

    public float[] calibrateChannel (
      int[] countData,
      int channel
    ) {

      // Check for channel 3a
      // --------------------
      if (channel == 3) {
        int select = ((Integer) getAttribute (CH3_SELECT)).intValue();
        if (select == 1) channel = 6;
      } // if

      // Create calibrated sample array
      // ------------------------------
      float[] sampleData = new float[countData.length];
      switch (channel) {

      // Calibrate visible data
      // ----------------------
      case 1: case 2: case 6:

        // Get calibration coefficients
        // ----------------------------
        float slope1 = 0, intercept1 = 0, slope2 = 0, intercept2 = 0;
        int intersection = 0;
        switch (channel) {
        case 1:
          slope1 = ((Float) getAttribute (CH1_SLOPE1)).floatValue();
          intercept1 = ((Float) getAttribute (CH1_INTERCEPT1)).floatValue();
          slope2 = ((Float) getAttribute (CH1_SLOPE2)).floatValue();
          intercept2 = ((Float) getAttribute (CH1_INTERCEPT2)).floatValue();
          intersection = ((Long) getAttribute (CH1_INTERSECTION)).intValue();
          break;
        case 2:
          slope1 = ((Float) getAttribute (CH2_SLOPE1)).floatValue();
          intercept1 = ((Float) getAttribute (CH2_INTERCEPT1)).floatValue();
          slope2 = ((Float) getAttribute (CH2_SLOPE2)).floatValue();
          intercept2 = ((Float) getAttribute (CH2_INTERCEPT2)).floatValue();
          intersection = ((Long) getAttribute (CH2_INTERSECTION)).intValue();
          break;
        case 6:
          slope1 = ((Float) getAttribute (CH3A_SLOPE1)).floatValue();
          intercept1 = ((Float) getAttribute (CH3A_INTERCEPT1)).floatValue();
          slope2 = ((Float) getAttribute (CH3A_SLOPE2)).floatValue();
          intercept2 = ((Float) getAttribute (CH3A_INTERCEPT2)).floatValue();
          intersection = ((Long) getAttribute (CH3A_INTERSECTION)).intValue();
          break;
        } // switch

        // Calibrate values
        // ----------------
        for (int i = 0; i < countData.length; i++) {
          if (countData[i] < intersection) 
            sampleData[i] = countData[i]*slope1 + intercept1;
          else
            sampleData[i] = countData[i]*slope2 + intercept2;
        } // for
 
        break;

      // Calibrate thermal data
      // ----------------------
      case 3: case 4: case 5:

        // Get calibration coefficients
        // ----------------------------
        float coef1 = 0, coef2 = 0, coef3 = 0, wave = 0, const1 = 0, 
          const2 = 0;
        switch (channel) {
        case 3:
          coef1 = ((Float) getAttribute (CH3B_COEFFICIENT1)).floatValue();
          coef2 = ((Float) getAttribute (CH3B_COEFFICIENT2)).floatValue();
          coef3 = ((Float) getAttribute (CH3B_COEFFICIENT3)).floatValue();
          wave = ((Float)header.getAttribute (
            DataHeaderV2.CH3B_CENTRAL_WAVE)).floatValue();
          const1 = ((Float) header.getAttribute (
            DataHeaderV2.CH3B_CONSTANT1)).floatValue();
          const2 = ((Float) header.getAttribute (
            DataHeaderV2.CH3B_CONSTANT2)).floatValue();
          break;
        case 4:
          coef1 = ((Float) getAttribute (CH4_COEFFICIENT1)).floatValue();
          coef2 = ((Float) getAttribute (CH4_COEFFICIENT2)).floatValue();
          coef3 = ((Float) getAttribute (CH4_COEFFICIENT3)).floatValue();
          wave = ((Float) header.getAttribute (
            DataHeaderV2.CH4_CENTRAL_WAVE)).floatValue();
          const1 = ((Float) header.getAttribute (
            DataHeaderV2.CH4_CONSTANT1)).floatValue();
          const2 = ((Float) header.getAttribute (
            DataHeaderV2.CH4_CONSTANT2)).floatValue();
          break;
        case 5:
          coef1 = ((Float) getAttribute (CH5_COEFFICIENT1)).floatValue();
          coef2 = ((Float) getAttribute (CH5_COEFFICIENT2)).floatValue();
          coef3 = ((Float) getAttribute (CH5_COEFFICIENT3)).floatValue();
          wave = ((Float) header.getAttribute (
            DataHeaderV2.CH5_CENTRAL_WAVE)).floatValue();
          const1 = ((Float) header.getAttribute (
            DataHeaderV2.CH5_CONSTANT1)).floatValue();
          const2 = ((Float) header.getAttribute (
            DataHeaderV2.CH5_CONSTANT2)).floatValue();
          break;
        } // switch

        // Calibrate values
        // ----------------
        double alpha = C2*wave;
        double beta = C1*Math.pow (wave, 3);
        for (int i = 0; i < countData.length; i++) {
          double radiance = coef1 + coef2*countData[i] + 
            coef3*(countData[i]*countData[i]);
          double tstar = alpha/Math.log (1 + (beta/radiance));
          sampleData[i] = (float) (const1 + const2*tstar);
        } // for

        break;

      default:
        throw new IllegalArgumentException ("Invalid channel: " + channel);

      } // switch

      return (sampleData);

    } // calibrateChannel

    ////////////////////////////////////////////////////////

    public float[] getRawNavigation (
      int variable
    ) {

      // Create navigation data array
      // ----------------------------       
      float[] navData = new float[ScanLine.NAVIGATION_VALUES];
      int offset = 0;
      switch (variable) {

      // Retrieve geometry angle data
      // ----------------------------
      case SOLAR_ZENITH:
      case SATELLITE_ZENITH:
      case RELATIVE_AZIMUTH:

        // Get navigation data offset
        // --------------------------
        switch (variable) {
        case SOLAR_ZENITH: offset = 0; break;
        case SATELLITE_ZENITH: offset = 2; break;
        case RELATIVE_AZIMUTH: offset = 4; break;
        } // switch

        // Get data values
        // ---------------
        for (int i = 0; i < ScanLine.NAVIGATION_VALUES; i++) {
          navData[i] = getShort (data, 
            NAVIGATION_GEOMETRY_OFFSET + offset + 6*i) * 1e-2f;
        } // for           

        break;

      // Retrieve location angle data
      // ----------------------------
      case LATITUDE:
      case LONGITUDE:

        // Get location data offset
        // ------------------------
        switch (variable) {
        case LATITUDE: offset = 0; break;
        case LONGITUDE: offset = 4; break;
        } // switch

        // Get data values
        // ---------------
        for (int i = 0; i < ScanLine.NAVIGATION_VALUES; i++) {
          navData[i] = getInt (data, 
            NAVIGATION_LOCATION_OFFSET + offset + 8*i) * 1e-4f;
        } // for           

        break;

      default:
        throw new IllegalArgumentException ("Invalid variable: " + variable);

      } // switch

      return (navData);

    } // getRawNavigation

    ////////////////////////////////////////////////////////

  } // ScanLineV2 class

  ////////////////////////////////////////////////////////////

  /**
   * The data header version 2 class is a data header that can
   * retrieve NOAA-KLM data header information.
   */
  public class DataHeaderV2
    extends DataHeader {

    // Constants
    // ---------
    /** The dataset creation site attribute (String). */
    public static final int CREATION_SITE = 100;

    /** The NOAA 1b format version attribute (Integer). */
    public static final int FORMAT_VERSION = 101;

    /** The instrument status attribute (Long). */
    public static final int INSTRUMENT_STATUS = 102;

    /** The count of calibrated, Earth located lines attribute (Integer). */
    public static final int CALIBRATED_LINES = 103;

    /** The count of missing scan lines attribute (Integer). */
    public static final int MISSING_LINES = 104;

    /** The AVHRR channel 3b central wave number attribute (Float). */
    public static final int CH3B_CENTRAL_WAVE = 106;

    /** The AVHRR channel 3b constant 1 attribute (Float). */
    public static final int CH3B_CONSTANT1 = 107;

    /** The AVHRR channel 3b constant 2 attribute (Float). */
    public static final int CH3B_CONSTANT2 = 108;

    /** The AVHRR channel 4 central wave number attribute (Float). */
    public static final int CH4_CENTRAL_WAVE = 109;

    /** The AVHRR channel 4 constant 1 attribute (Float). */
    public static final int CH4_CONSTANT1 = 110;

    /** The AVHRR channel 4 constant 2 attribute (Float). */
    public static final int CH4_CONSTANT2 = 111;

    /** The AVHRR channel 5 central wave number attribute (Float). */
    public static final int CH5_CENTRAL_WAVE = 112;

    /** The AVHRR channel 5 constant 1 attribute (Float). */
    public static final int CH5_CONSTANT1 = 113;

    /** The AVHRR channel 5 constant 2 attribute (Float). */
    public static final int CH5_CONSTANT2 = 114;

    /** The reference ellipsoid model attribute (String). */
    public static final int ELLIPSOID = 115;

    /** The number of header records attribute (Integer). */
    public static final int HEADER_RECORDS = 116;

    /** The NOAA-15 spacecraft ID code. */
    public static final int NOAA_15_ID = 4;

    /** The NOAA-16 spacecraft ID code. */
    public static final int NOAA_16_ID = 2;

    /** The NOAA-17 spacecraft ID code. */
    public static final int NOAA_17_ID = 6;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new dataset header.
     *
     * @throws IOException if an error occurred reading the file data.
     */
    public DataHeaderV2 () throws IOException {

      super();

    } // DataHeaderV2 constructor

    ////////////////////////////////////////////////////////

    public Object getAttribute (
      int index
    ) {

      switch (index) {

      // Standard attributes
      // -------------------
      case SPACECRAFT_ID: 
        int id = getUShort (data, 72);


        //System.out.println ("id = " + id);


        switch (id) {
        case NOAA_15_ID: return (new String ("noaa-15"));
        case NOAA_16_ID: return (new String ("noaa-16"));
        case NOAA_17_ID: return (new String ("noaa-17"));
        default: return (new String ("unknown"));
        } // switch
      case DATA_TYPE_CODE: 
        int code = getUShort (data, 76);
        switch (code) {
        case 1: return (new String ("lac"));
        case 2: return (new String ("gac"));
        case 3: return (new String ("hrpt"));
        case 4: return (new String ("tip"));
        case 5: return (new String ("hirs"));
        case 6: return (new String ("msu"));
        case 7: return (new String ("ssu"));
        case 8: return (new String ("dcs"));
        case 9: return (new String ("sem"));
        case 10: return (new String ("amsu-a"));
        case 11: return (new String ("amsu-b"));
        default: return (new String ("unknown"));
        } // switch
      case START_YEAR: return (new Integer (getUShort (data, 84)));
      case START_DAY: return (new Integer (getUShort (data, 86)));
      case START_MILLISECOND: return (new Long (getUInt (data, 88)));
      case DATA_RECORDS: return (new Integer (getUShort (data, 128)));
      case DATA_GAPS: return (new Integer (getUShort (data, 134)));
      case DATASET_NAME: return (new String (getBytes (data, 22, 42)).trim());

      // Format specific attributes
      // --------------------------
      case CREATION_SITE: return (new String (getBytes (data, 0, 3)).trim());
      case FORMAT_VERSION: return (new Integer (getUShort (data, 4)));
      case INSTRUMENT_STATUS: return (new Long (getUInt (data, 116)));
      case CALIBRATED_LINES: return (new Integer (getUShort (data, 130)));
      case MISSING_LINES: return (new Integer (getUShort (data, 132)));
      case CH3B_CENTRAL_WAVE: return (new Float (getInt (data, 280)*1e-2));
      case CH3B_CONSTANT1: return (new Float (getInt (data, 284)*1e-5)); 
      case CH3B_CONSTANT2: return (new Float (getInt (data, 288)*1e-6)); 
      case CH4_CENTRAL_WAVE: return (new Float (getInt (data, 292)*1e-3));
      case CH4_CONSTANT1: return (new Float (getInt (data, 296)*1e-5)); 
      case CH4_CONSTANT2: return (new Float (getInt (data, 300)*1e-6)); 
      case CH5_CENTRAL_WAVE: return (new Float (getInt (data, 304)*1e-3));
      case CH5_CONSTANT1: return (new Float (getInt (data, 308)*1e-5)); 
      case CH5_CONSTANT2: return (new Float (getInt (data, 312)*1e-6)); 
      case ELLIPSOID: return (new String (getBytes (data, 328, 8)));
      case HEADER_RECORDS: return (new Integer (getUShort (data, 14)));

      default:
        throw new IllegalArgumentException ("Unsupported attribute index");

      } // switch

    } // getAttribute

    ////////////////////////////////////////////////////////

  } // DataHeaderV2 class

  ////////////////////////////////////////////////////////////

  /**
   * The archive header version 2 class is an archive header that can
   * read NOAA-KLM style Archive Retrieval System (ARS) headers.
   *
   * @see NOAA1bReader#isArchive
   * @see NOAA1bReader#getArchiveHeader
   */
  public class ArchiveHeaderV2
    extends ArchiveHeader {

    // Constants
    // ---------
    /** The archive order creation year attribute (Integer). */
    public static final int ORDER_CREATION_YEAR = 100;

    /** The archive order creation Julian day attribute (Integer). */
    public static final int ORDER_CREATION_DAY = 101;

    /** The archive processing site attribute (String). */
    public static final int PROCESSING_SITE = 102;

    /** The orbit type attribute: 'A', 'D', 'B' (String). */
    public static final int ORBIT_TYPE = 104;

    /** The data format name and version attribute (String). */
    public static final int DATA_FORMAT = 105;

    /** The data record size in bytes attribute (Integer). */
    public static final int RECORD_SIZE = 106;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new archive header.
     *
     * @throws IOException if an error occurred reading the file data.
     */
    public ArchiveHeaderV2 () throws IOException {

      super();

    } // ArchiveHeaderV2 constructor

    ////////////////////////////////////////////////////////

    public Object getAttribute (
      int index
    ) {

      switch (index) {

      // Standard attributes
      // -------------------
      case DATASET_NAME: return (new String (getBytes (data, 30, 42)).trim());
      case START_HOUR: 
        return (new Integer (Integer.parseInt (new String (getBytes (data, 89, 
          2)).trim())));
      case START_MINUTE: 
        return (new Integer (Integer.parseInt (new String (getBytes (data, 91, 
          2)).trim())));
      case DURATION_MINUTES: 
        return (new Integer (Integer.parseInt (new String (getBytes (data, 93, 
          3)).trim())));
      case SENSOR_DATA_WORD_SIZE: 
        return (new Integer (Integer.parseInt (new String (getBytes (data, 
          117, 2)).trim())));

      // Format specific attributes
      // --------------------------
      case ORDER_CREATION_YEAR: 
        return (new Integer (Integer.parseInt (new String (getBytes (data, 14, 
          4)).trim())));
      case ORDER_CREATION_DAY: 
        return (new Integer (Integer.parseInt (new String (getBytes (data, 18, 
          3)).trim())));
      case PROCESSING_SITE: return (new String (getBytes (data, 21, 1)));
      case ORBIT_TYPE: return (new String (getBytes (data, 146, 1)));
      case DATA_FORMAT: return (new String (getBytes (data, 161, 20)).trim());
      case RECORD_SIZE: 
        return (new Integer (Integer.parseInt (new String (getBytes (data, 
          181, 6)).trim())));

      default:
        throw new IllegalArgumentException ("Unsupported attribute index");

      } // switch

    } // getAttribute

    ////////////////////////////////////////////////////////

  } // ArchiveHeaderV2 class

  ////////////////////////////////////////////////////////////

} // NOAA1bV2Reader class

////////////////////////////////////////////////////////////////////////
