////////////////////////////////////////////////////////////////////////
/*

     File: NOAA1bV1Reader.java
   Author: Peter Hollemans
     Date: 2003/02/03

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import noaa.coastwatch.io.NOAA1bReader;

/**
 * A NOAA 1b version 1 reader is a NOAA 1b reader that reads NOAA
 * 1b format NOAA-A through -J GAC/LAC/HRPT data files available
 * from the NOAA/NESDIS Satellite Active Archive.  These files
 * are characterized by an optional 122 byte Terabit Memory (TBM)
 * header followed by data header and records of
 * 10688/14800/20928 bytes for 8/10/16-bit LAC or 2496/3220/4540
 * bytes for 8/10/16-bit GAC.<p>
 *
 * Note that currently, channel 3, 4, and 5 non-linear
 * corrections are applied to NOAA-14 data only.  Also,
 * conversion from radiance to brightness temperatures uses a
 * single central wavenumber for the range 270-310 K for all
 * satellites.  The calibration in this implementation may be
 * updated at a later date as required.  For more accurate
 * calibration results, use NOAA-KLM data stored in NOAA 1b
 * version 2 format datasets.
 *
 * @see NOAA1bV2Reader
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class NOAA1bV1Reader
  extends NOAA1bReader {

  // Constants
  // ---------

  /** The archive header size in bytes. */
  private static final int ARCHIVE_HEADER_SIZE = 122;

  /** The header and data record size for LAC in bytes. */
  private static final int LAC_8BIT_RECORD_SIZE = 10688;
  private static final int LAC_10BIT_RECORD_SIZE = 14800;
  private static final int LAC_16BIT_RECORD_SIZE = 20928;

  /** The offset for extra LAC solar zenith angle data. */
  private static final int LAC_EXTRA_NAVIGATION_OFFSET = 14104;

  /** The header and data record size for LAC in bytes. */
  private static final int GAC_8BIT_RECORD_SIZE = 2496;
  private static final int GAC_10BIT_RECORD_SIZE = 3220;
  private static final int GAC_16BIT_RECORD_SIZE = 4540;

  /** The offset for extra GAC solar zenith angle data. */
  private static final int GAC_EXTRA_NAVIGATION_OFFSET = 3176;

  /** The scan line attribute data size. */
  private static final int ATTRIBUTE_DATA_SIZE = 448;

  /** The dataset variables names. */
  private static final String[] VARIABLE_NAMES = new String[] {
    "avhrr_ch1",
    "avhrr_ch2",
    "avhrr_ch3",
    "avhrr_ch4",
    "avhrr_ch5",
    "sun_zenith",
    "latitude",
    "longitude",
    "scan_time"
  };

  /** The satellite name strings. */
  private static final String[] SATELLITE_NAMES = {
    "tiros-n",
    "noaa-6",
    "noaa-7",
    "noaa-8",
    "noaa-9",
    "noaa-10",
    "noaa-11",
    "noaa-12",
    "noaa-14"
  };

  /** The satellite indices. */
  private static final int TIROS_N = 0;
  private static final int NOAA_6 = 1;
  private static final int NOAA_7 = 2;
  private static final int NOAA_8 = 3;
  private static final int NOAA_9 = 4;
  private static final int NOAA_10 = 5;
  private static final int NOAA_11 = 6;
  private static final int NOAA_12 = 7;
  private static final int NOAA_14 = 8;

  /** The data format description. */
  private static final String DATA_FORMAT = "NOAA 1b version 1";

  // Variables
  // ---------

  /** The satellite index used for internal constants tables. */
  private int satIndex;

  /** The offset to additional solar zenith angle data. */
  private int extraSzOffset;

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (DATA_FORMAT); }

  ////////////////////////////////////////////////////////////

  protected String[] getVariableNames () { return (VARIABLE_NAMES); }

  ////////////////////////////////////////////////////////////

  /** 
   * Checks for an archive header and returns true if so.  This method
   * reads the TBM header which contains mostly printable ASCII
   * characters, except in a few locations.
   */
  protected boolean getArchiveFlag () throws IOException {

    // Check for non-printable ASCII characters
    // ----------------------------------------
    boolean printable = true;
    ByteBuffer inputBuffer = getDataBuffer (0, ARCHIVE_HEADER_SIZE);
    for (int i = 0; i < ARCHIVE_HEADER_SIZE; i++) {

      // Skip the dataset header (sometimes garbage)
      // -------------------------------------------
      if (i >= 30 && i <= 73) continue;

      // Skip the channels selected (in binary)
      // --------------------------------------
      else if (i >= 97 && i <= 116) continue;

      // Skip the rest of the header (all 0)
      // -----------------------------------
      else if (i > 118) continue;

      // Check for printable
      // -------------------
      if (!isPrint (inputBuffer.get (i))) {
        printable = false;
        break;
      } // if

    } // for

    return (printable);

  } // getArchiveFlag

  ////////////////////////////////////////////////////////////

  protected void checkFormat () throws IOException {

    // Initialize known constants
    // --------------------------
    archiveHeaderSize = ARCHIVE_HEADER_SIZE;
    dataHeaders = 1;
    attributeDataSize = ATTRIBUTE_DATA_SIZE;

    // Initialize sensor word size
    // ---------------------------
    sensorWordSize = -1;

    // Check archive header
    // --------------------
    if (archive) {
      ArchiveHeader header = getArchiveHeader();
    
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

    } // if

    // Get data header
    // ---------------
    recordSize = GAC_10BIT_RECORD_SIZE;
    DataHeader header = getDataHeader();

    // Check spacecraft
    // ----------------
    String spacecraft = (String) header.getAttribute (
      DataHeader.SPACECRAFT_ID);
    if (spacecraft.equals ("unknown")) 
      throw new IOException ("Unknown spacecraft ID");

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
      case 8: recordSize = LAC_8BIT_RECORD_SIZE; break;
      case 10: recordSize = LAC_10BIT_RECORD_SIZE; break;
      case 16: recordSize = LAC_16BIT_RECORD_SIZE; break;
      } // switch
      
      // Set offset to extra solar zenith data
      // -------------------------------------
      extraSzOffset = LAC_EXTRA_NAVIGATION_OFFSET;

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
      case 8: recordSize = GAC_8BIT_RECORD_SIZE; break;
      case 10: recordSize = GAC_10BIT_RECORD_SIZE; break;
      case 16: recordSize = GAC_16BIT_RECORD_SIZE; break;
      } // switch

      // Set offset to extra solar zenith data
      // -------------------------------------
      extraSzOffset = GAC_EXTRA_NAVIGATION_OFFSET;

    } // else

    // Report unsupported data type
    // ----------------------------
    else
      throw new IOException ("Unsupported data type: " + type);

  } // checkFormat

  ////////////////////////////////////////////////////////////

  public ArchiveHeader getArchiveHeader () throws IOException {

    return (new ArchiveHeaderV1());

  } // getArchiveHeader

  ////////////////////////////////////////////////////////////

  public DataHeader getDataHeader () throws IOException {

    return (new DataHeaderV1());

  } // getDataHeader

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a NOAA 1b version 1 reader from the specified file.
   *
   * @param file the file name to read.
   *
   * @throws IOException if an error occurred reading the file data.
   */
  public NOAA1bV1Reader (
    String file
  ) throws IOException {

    super (file);

    // Get the satellite index
    // -----------------------
    String satName = (String) header.getAttribute (DataHeader.SPACECRAFT_ID);
    satIndex = -1;
    for (int i = 0; i < SATELLITE_NAMES.length; i++)
      if (satName.equals (SATELLITE_NAMES[i])) satIndex = i;
    if (satIndex == -1) 
      throw new IOException ("Unsupported satellite: " + satName);    

  } // NOAA1bV1Reader constructor

  ////////////////////////////////////////////////////////////

  protected boolean isNavigationUsable (
    int record,
    ByteBuffer buffer
  ) throws IOException {

    buffer.clear().limit (4);
    inputChannel.read (buffer, getRecordStart (record) + 8);
    long quality = getUInt (buffer, 0);
    return ((quality & 0x04000000) == 0);

  } // isNavigationUsable

  ////////////////////////////////////////////////////////////

  public ScanLine getScanLine (
    int line,
    int start,
    int count
  ) throws IOException {

    ScanLine lineObject = scanLineCache.get (line);
    if (lineObject == null) {
      lineObject = new ScanLineV1 (line, start, count);
      scanLineCache.put (line, lineObject);
    } // if
    return (lineObject);

  } // getScanLine

  ////////////////////////////////////////////////////////////

  /**
   * The scan line version 1 class is a scan line that can read,
   * calibrate, and navigate NOAA-A through -J data.
   */
  public class ScanLineV1
    extends ScanLine {

    // Constants
    // ---------
    /** The radiation constant c1. */
    private static final float C1 = 1.1910659e-5f;

    /** The radiation constant c2. */
    private static final float C2 = 1.438833f;

    /** The channel 1 calibration slope attribute (Float). */
    public static final int CH1_SLOPE = 100;

    /** The channel 1 calibration intercept attribute (Float). */
    public static final int CH1_INTERCEPT = 101;

    /** The channel 2 calibration slope attribute (Float). */
    public static final int CH2_SLOPE = 102;

    /** The channel 2 calibration intercept attribute (Float). */
    public static final int CH2_INTERCEPT = 103;

    /** The channel 3 calibration slope attribute (Float). */
    public static final int CH3_SLOPE = 104;

    /** The channel 3 calibration intercept attribute (Float). */
    public static final int CH3_INTERCEPT = 105;

    /** The channel 4 calibration slope attribute (Float). */
    public static final int CH4_SLOPE = 106;

    /** The channel 4 calibration intercept attribute (Float). */
    public static final int CH4_INTERCEPT = 107;

    /** The channel 5 calibration slope attribute (Float). */
    public static final int CH5_SLOPE = 108;

    /** The channel 5 calibration intercept attribute (Float). */
    public static final int CH5_INTERCEPT = 109;

    /** The PRT reading 1 attribute (Integer). */
    public static final int PRT_READING_1 = 110;

    /** The PRT reading 2 attribute (Integer). */
    public static final int PRT_READING_2 = 111;
 
    /** The PRT reading 3 attribute (Integer). */
    public static final int PRT_READING_3 = 112;

    /** The scaling factor for calibration slopes. */
    private static final float SLOPE_SCALE = (float) 0x40000000L;

    /** The scaling factor for calibration intercepts. */
    private static final float INTER_SCALE = (float) 0x00400000L;

    /** The navigation data geometry offset. */
    private static final int NAVIGATION_GEOMETRY_OFFSET = 53;

    /** The navigation data location offset. */
    private static final int NAVIGATION_LOCATION_OFFSET = 104;

    /** The TIROS-N wavenumbers. */
    private static final float TIROS_N_CH3_WAVE = 2638.05f;
    private static final float TIROS_N_CH4_WAVE = 912.01f;

    /** The NOAA-6 wavenumbers. */
    private static final float NOAA_6_CH3_WAVE = 2658.05f;
    private static final float NOAA_6_CH4_WAVE = 912.14f;

    /** The NOAA-7 wavenumbers. */
    private static final float NOAA_7_CH3_WAVE = 2671.9f;
    private static final float NOAA_7_CH4_WAVE = 927.22f;
    private static final float NOAA_7_CH5_WAVE = 840.872f;

    /** The NOAA-8 wavenumbers. */
    private static final float NOAA_8_CH3_WAVE = 2639.18f;
    private static final float NOAA_8_CH4_WAVE = 914.305f;

    /** The NOAA-9 wavenumbers. */
    private static final float NOAA_9_CH3_WAVE = 2678.11f;
    private static final float NOAA_9_CH4_WAVE = 929.46f;
    private static final float NOAA_9_CH5_WAVE = 845.19f;

    /** The NOAA-10 wavenumbers. */
    private static final float NOAA_10_CH3_WAVE = 2660.35f;
    private static final float NOAA_10_CH4_WAVE = 909.52f;

    /** The NOAA-11 wavenumbers. */
    private static final float NOAA_11_CH3_WAVE = 2670.96f;
    private static final float NOAA_11_CH4_WAVE = 927.75f;
    private static final float NOAA_11_CH5_WAVE = 842.14f;

    /** The NOAA-12 wavenumbers. */
    private static final float NOAA_12_CH3_WAVE = 2639.61f;
    private static final float NOAA_12_CH4_WAVE = 921.0291f;
    private static final float NOAA_12_CH5_WAVE = 837.3641f;

    /** The NOAA-14 wavenumbers. */
    private static final float NOAA_14_CH3_WAVE = 2645.899f;
    private static final float NOAA_14_CH4_WAVE = 929.3323f;
    private static final float NOAA_14_CH5_WAVE = 835.1647f;

    /** The NOAA-14 non-linear correction coefficients. */
    private static final float NOAA_14_CH3_NONLINEAR_A = 1.00359f;
    private static final float NOAA_14_CH3_NONLINEAR_B = 0.0f;
    private static final float NOAA_14_CH3_NONLINEAR_D = -0.0031f;
    private static final float NOAA_14_CH4_NONLINEAR_A = 0.92378f;
    private static final float NOAA_14_CH4_NONLINEAR_B = 0.0003822f;
    private static final float NOAA_14_CH4_NONLINEAR_D = 3.72f;
    private static final float NOAA_14_CH5_NONLINEAR_A = 0.96194f;
    private static final float NOAA_14_CH5_NONLINEAR_B = 0.0001742f;
    private static final float NOAA_14_CH5_NONLINEAR_D = 2.00f;

    /** The HRPT telemetry offset. */
    private static final int HRPT_TELEMETRY_OFFSET = 308;

    ////////////////////////////////////////////////////////

    /** 
     * Throws an exception.  The NOAA 1b version 1 format does not
     * support the storage of cloud data.
     */
    public byte[] getCloud () { 
      
      throw new UnsupportedOperationException ();

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
      case QUALITY_INDICATOR: return (new Long (getUInt (data, 8)));
      case CH3_SELECT: return (new Integer (0));
      case SCAN_LINE_YEAR:
        int year = getUByte (data, 2) >>> 1;
        if (year > 70) year = 1900 + year;
        else year = 2000 + year;
        return (new Integer (year));
      case SCAN_LINE_DAY: return (new Integer (getUShort (data, 2) & 0x01ff));
      case SCAN_LINE_MILLISECOND: return (new Long (getUInt (data, 4)));

      // Format specific attributes
      // --------------------------
      case CH1_SLOPE: return (new Float (getInt (data, 12)/SLOPE_SCALE));
      case CH1_INTERCEPT: return (new Float (getInt (data, 16)/INTER_SCALE));
      case CH2_SLOPE: return (new Float (getInt (data, 20)/SLOPE_SCALE));
      case CH2_INTERCEPT: return (new Float (getInt (data, 24)/INTER_SCALE));
      case CH3_SLOPE: return (new Float (getInt (data, 28)/SLOPE_SCALE));
      case CH3_INTERCEPT: return (new Float (getInt (data, 32)/INTER_SCALE));
      case CH4_SLOPE: return (new Float (getInt (data, 36)/SLOPE_SCALE));
      case CH4_INTERCEPT: return (new Float (getInt (data, 40)/INTER_SCALE));
      case CH5_SLOPE: return (new Float (getInt (data, 44)/SLOPE_SCALE));
      case CH5_INTERCEPT: return (new Float (getInt (data, 48)/INTER_SCALE));
      case PRT_READING_1: return (new Integer ((int) (getUInt (data, 
        HRPT_TELEMETRY_OFFSET+20) & 0x03ff)));
      case PRT_READING_2: return (new Integer ((int) (getUInt (data, 
        HRPT_TELEMETRY_OFFSET+24) >>> 20)));
      case PRT_READING_3: return (new Integer ((int) ((getUInt (data, 
        HRPT_TELEMETRY_OFFSET+24) >>> 10) & 0x03ff)));

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
    public ScanLineV1 (
      int line,
      int start,
      int count
    ) throws IOException {

      super (line, start, count);

    } // ScanLineV1 constructor

    ////////////////////////////////////////////////////////

    public boolean isSensorDataUsable () {

      long quality = ((Long) getAttribute (QUALITY_INDICATOR)).longValue();
      return ((quality & 0xf9f8ffff) == 0);

    } // isSensorDataUsable 

    ////////////////////////////////////////////////////////

    public boolean isNavigationUsable () {

      long quality = ((Long) getAttribute (QUALITY_INDICATOR)).longValue();
      return ((quality & 0x04000000) == 0);

    } // isNavigationUsable

    ////////////////////////////////////////////////////////

    protected float[] getCalibration (
      int channel
    ) {

      float[] coefs = new float[2];
      switch (channel) {
      case 1: 
        coefs[0] = ((Float) getAttribute (CH1_SLOPE)).floatValue();
        coefs[1] = ((Float) getAttribute (CH1_INTERCEPT)).floatValue();
        break;
      case 2: 
        coefs[0] = ((Float) getAttribute (CH2_SLOPE)).floatValue();
        coefs[1] = ((Float) getAttribute (CH2_INTERCEPT)).floatValue();
        break;
      case 3: 
        coefs[0] = ((Float) getAttribute (CH3_SLOPE)).floatValue();
        coefs[1] = ((Float) getAttribute (CH3_INTERCEPT)).floatValue();
        break;
      case 4: 
        coefs[0] = ((Float) getAttribute (CH4_SLOPE)).floatValue();
        coefs[1] = ((Float) getAttribute (CH4_INTERCEPT)).floatValue();
        break;
      case 5: 
        coefs[0] = ((Float) getAttribute (CH5_SLOPE)).floatValue();
        coefs[1] = ((Float) getAttribute (CH5_INTERCEPT)).floatValue();
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

      // Create calibrated sample array
      // ------------------------------
      float[] sampleData = new float[countData.length];

      // Get calibration coefficients
      // ----------------------------
      float slope = 0, intercept = 0;
      switch (channel) {
      case 1: 
        slope = ((Float) getAttribute (CH1_SLOPE)).floatValue();
        intercept = ((Float) getAttribute (CH1_INTERCEPT)).floatValue();
        break;
      case 2: 
        slope = ((Float) getAttribute (CH2_SLOPE)).floatValue();
        intercept = ((Float) getAttribute (CH2_INTERCEPT)).floatValue();
        break;
      case 3: 
        slope = ((Float) getAttribute (CH3_SLOPE)).floatValue();
        intercept = ((Float) getAttribute (CH3_INTERCEPT)).floatValue();
        break;
      case 4: 
        slope = ((Float) getAttribute (CH4_SLOPE)).floatValue();
        intercept = ((Float) getAttribute (CH4_INTERCEPT)).floatValue();
        break;
      case 5: 
        slope = ((Float) getAttribute (CH5_SLOPE)).floatValue();
        intercept = ((Float) getAttribute (CH5_INTERCEPT)).floatValue();
        break;
      default:
        throw new IllegalArgumentException ("Invalid channel: " + channel);
      } // switch

      // Calibrate values
      // ----------------
      for (int i = 0; i < countData.length; i++) {
        sampleData[i] = countData[i]*slope + intercept;
      } // for

      // Convert thermal channels to temperature
      // ---------------------------------------
      switch (channel) {
      case 3: case 4: case 5:

        // Get calibration coefficients
        // ----------------------------
        float wave = 0, a = 0, b = 0, d = 0;
        boolean nonlinear = false;
        switch (satIndex) {

        case TIROS_N:
          switch (channel) {
          case 3: wave = TIROS_N_CH3_WAVE; break;
          case 4: wave = TIROS_N_CH4_WAVE; break;
          case 5: wave = TIROS_N_CH4_WAVE; break;
          } // switch
          break;

        case NOAA_6:
          switch (channel) {
          case 3: wave = NOAA_6_CH3_WAVE; break;
          case 4: wave = NOAA_6_CH4_WAVE; break;
          case 5: wave = NOAA_6_CH4_WAVE; break;
          } // switch
          break;

        case NOAA_7:
          switch (channel) {
          case 3: wave = NOAA_7_CH3_WAVE; break;
          case 4: wave = NOAA_7_CH4_WAVE; break;
          case 5: wave = NOAA_7_CH5_WAVE; break;
          } // switch
          break;

        case NOAA_8:
          switch (channel) {
          case 3: wave = NOAA_8_CH3_WAVE; break;
          case 4: wave = NOAA_8_CH4_WAVE; break;
          case 5: wave = NOAA_8_CH4_WAVE; break;
          } // switch
          break;

        case NOAA_9:
          switch (channel) {
          case 3: wave = NOAA_9_CH3_WAVE; break;
          case 4: wave = NOAA_9_CH4_WAVE; break;
          case 5: wave = NOAA_9_CH5_WAVE; break;
          } // switch
          break;

        case NOAA_10:
          switch (channel) {
          case 3: wave = NOAA_10_CH3_WAVE; break;
          case 4: wave = NOAA_10_CH4_WAVE; break;
          case 5: wave = NOAA_10_CH4_WAVE; break;
          } // switch
          break;

        case NOAA_11:
          switch (channel) {
          case 3: wave = NOAA_11_CH3_WAVE; break;
          case 4: wave = NOAA_11_CH4_WAVE; break;
          case 5: wave = NOAA_11_CH5_WAVE; break;
          } // switch
          break;

        case NOAA_12:
          switch (channel) {
          case 3: wave = NOAA_12_CH3_WAVE; break;
          case 4: wave = NOAA_12_CH4_WAVE; break;
          case 5: wave = NOAA_12_CH5_WAVE; break;
          } // switch
          break;

        case NOAA_14:
          switch (channel) {
          case 3: 
            wave = NOAA_14_CH3_WAVE; 
            a = NOAA_14_CH3_NONLINEAR_A;
            b = NOAA_14_CH3_NONLINEAR_B;
            d = NOAA_14_CH3_NONLINEAR_D;
            break;
          case 4: 
            wave = NOAA_14_CH4_WAVE; 
            a = NOAA_14_CH4_NONLINEAR_A;
            b = NOAA_14_CH4_NONLINEAR_B;
            d = NOAA_14_CH4_NONLINEAR_D;
            break;
          case 5: 
            wave = NOAA_14_CH5_WAVE; 
            a = NOAA_14_CH5_NONLINEAR_A;
            b = NOAA_14_CH5_NONLINEAR_B;
            d = NOAA_14_CH5_NONLINEAR_D;
            break;
          } // switch
          nonlinear = true;
          break;

        } // switch

        // Perform radiance-based non-linear correction
        // --------------------------------------------
        if (nonlinear) {
          for (int i = 0; i < sampleData.length; i++) {
            float rad = sampleData[i];
            sampleData[i] = (float) (a*rad + b*rad*rad + d);
          } // for
        } // if

        // Convert radiance to temperature 
        // -------------------------------
        double alpha = C2*wave;
        double beta = C1*Math.pow (wave, 3);
        for (int i = 0; i < sampleData.length; i++) {
          sampleData[i] = (float) (alpha/Math.log (1 + (beta/sampleData[i])));
        } // for
        break;

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
        for (int i = 0; i < ScanLine.NAVIGATION_VALUES; i++) {
          navData[i] = getUByte (data, NAVIGATION_GEOMETRY_OFFSET + i) / 2f;
        } // for           
        /**
         * We add the extra 3 bits of precision for each solar zenith
         * angle here.  We should not perform the extra bit reading
         * unless we're using 10-bit data -- this is due to a note in
         * the POD guide that says the 8-bit and 16-bit extraction
         * software doesn't keep the extra solar zenith bits.
         */
        if (sensorWordSize == 10) {
          for (int i = 0; i < ScanLine.NAVIGATION_VALUES; i++) {
            navData[i] += getNBit (data, extraSzOffset, i, 3) / 10f;
          } // for
        } // if
        break;

      // Retrieve location angle data
      // ----------------------------
      case LATITUDE:
      case LONGITUDE:

        // Get location data offset
        // ------------------------
        switch (variable) {
        case LATITUDE: offset = 0; break;
        case LONGITUDE: offset = 2; break;
        } // switch

        // Get data values
        // ---------------
        for (int i = 0; i < ScanLine.NAVIGATION_VALUES; i++) {
          navData[i] = getShort (data, 
            NAVIGATION_LOCATION_OFFSET + offset + 4*i) / 128f;
        } // for           

        break;

      default:
        throw new IllegalArgumentException ("Invalid variable: " + variable);

      } // switch

      return (navData);

    } // getRawNavigation

    ////////////////////////////////////////////////////////

  } // ScanLineV1 class

  ////////////////////////////////////////////////////////////

  /**
   * The data header version 1 class is a data header that can
   * retrieve NOAA-A through -J data header information.
   */
  public class DataHeaderV1
    extends DataHeader {

    // Constants
    // ---------
    /** The TIROS-N spacecraft ID code. */
    public static final int TIROS_N_ID = 1;

    /** The NOAA-6 spacecraft ID code. */
    public static final int NOAA_6_ID = 2;

    /** The NOAA-7 spacecraft ID code. */
    public static final int NOAA_7_ID = 4;

    /** The NOAA-8 spacecraft ID code. */
    public static final int NOAA_8_ID = 6;

    /** The NOAA-9 spacecraft ID code. */
    public static final int NOAA_9_ID = 7;

    /** The NOAA-10 spacecraft ID code. */
    public static final int NOAA_10_ID = 8;

    /** The NOAA-11 spacecraft ID code. */
    public static final int NOAA_11_ID = 1;

    /** The NOAA-12 spacecraft ID code. */
    public static final int NOAA_12_ID = 5;

    /** The NOAA-14 spacecraft ID code. */
    public static final int NOAA_14_ID = 3;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new dataset header.
     *
     * @throws IOException if an error occurred reading the file data.
     */
    public DataHeaderV1 () throws IOException {

      super();

    } // DataHeaderV1 constructor

    ////////////////////////////////////////////////////////

    public Object getAttribute (
      int index
    ) {

      switch (index) {

      // Standard attributes
      // -------------------
      case SPACECRAFT_ID: 
        short id = getUByte (data, 0);
        switch (id) {
        case TIROS_N_ID: 
          int year = ((Integer) getAttribute (START_YEAR)).intValue();
          if (year <= 1980) return (new String ("tiros-n"));
          else return (new String ("noaa-11"));
        case NOAA_6_ID: return (new String ("noaa-6"));
        case NOAA_7_ID: return (new String ("noaa-7"));
        case NOAA_8_ID: return (new String ("noaa-8"));
        case NOAA_9_ID: return (new String ("noaa-9"));
        case NOAA_10_ID: return (new String ("noaa-10"));
        case NOAA_12_ID: return (new String ("noaa-12"));
        case NOAA_14_ID: return (new String ("noaa-14"));
        default: return (new String ("unknown"));
        } // switch
      case DATA_TYPE_CODE: 
        int code = getUByte (data, 1) >>> 4;
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
        default: return (new String ("unknown"));
        } // switch
      case START_YEAR:
        int startYear = getUByte (data, 2) >>> 1;
        if (startYear > 70) startYear = 1900 + startYear;
        else startYear = 2000 + startYear;
        return (new Integer (startYear));
      case START_DAY: return (new Integer (getUShort (data, 2) & 0x01ff));
      case START_MILLISECOND: return (new Long (getUInt (data, 4)));
      case END_YEAR:
        int endYear = getUByte (data, 10) >>> 1;
        if (endYear > 70) endYear = 1900 + endYear;
        else endYear = 2000 + endYear;
        return (new Integer (endYear));
      case END_DAY: return (new Integer (getUShort (data, 10) & 0x01ff));
      case END_MILLISECOND: return (new Long (getUInt (data, 12)));
      case DATA_RECORDS: return (new Integer (getUShort (data, 8)));
      case DATA_GAPS: return (new Integer (getUShort (data, 24)));
      case DATASET_NAME: 
        try { return (new String (getBytes (data, 40, 44), "Cp500").trim()); }
        catch (UnsupportedEncodingException e) { 
          return (new String ("unknown"));
        } // catch

      // Format specific attributes
      // --------------------------








      default:
        throw new IllegalArgumentException ("Unsupported attribute index");

      } // switch

    } // getAttribute

    ////////////////////////////////////////////////////////

  } // DataHeaderV1 class

  ////////////////////////////////////////////////////////////

  /**
   * The archive header version 1 class is an archive header that can
   * read NOAA-A through -J style Terabit Memory (TBM) headers.
   *
   * @see NOAA1bReader#isArchive
   * @see NOAA1bReader#getArchiveHeader
   */
  public class ArchiveHeaderV1
    extends ArchiveHeader {

    // Constants
    // ---------










    ////////////////////////////////////////////////////////

    /**
     * Creates a new archive header.
     *
     * @throws IOException if an error occurred reading the file data.
     */
    public ArchiveHeaderV1 () throws IOException {

      super();

    } // ArchiveHeaderV1 constructor

    ////////////////////////////////////////////////////////

    public Object getAttribute (
      int index
    ) {

      switch (index) {

      // Standard attributes
      // -------------------
      case DATASET_NAME: return (new String (getBytes (data, 30, 44)).trim());
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








      default:
        throw new IllegalArgumentException ("Unsupported attribute index");

      } // switch

    } // getAttribute

    ////////////////////////////////////////////////////////

  } // ArchiveHeaderV1 class

  ////////////////////////////////////////////////////////////

} // NOAA1bV1Reader class

////////////////////////////////////////////////////////////////////////
