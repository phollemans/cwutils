////////////////////////////////////////////////////////////////////////
/*
     FILE: MHSRecord.java
  PURPOSE: Reads NOAA 1b format MHS data records.
   AUTHOR: Peter Hollemans
     DATE: 2007/11/23
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io.noaa1b;

// Imports
// -------
import java.nio.ByteBuffer;
import java.util.Arrays;
import noaa.coastwatch.io.BinaryStreamReader;
import noaa.coastwatch.io.noaa1b.AbstractDataRecord;
import noaa.coastwatch.io.noaa1b.MHS;
import noaa.coastwatch.io.noaa1b.MHSData;
import noaa.coastwatch.io.noaa1b.MHSHeader;
import terrenus.instrument.InstrumentData;

/**
 * The <code>MHSRecord</code> class reads NOAA 1b MHS data
 * records.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class MHSRecord extends AbstractDataRecord {

  // Constants
  // ---------

  /** The number of words in each MHS sensor data record. */
  private static final int SENSOR_WORDS = 6;

  ////////////////////////////////////////////////////////////

  public boolean isSensorDataUsable() {

    /**
     * What we're checking here is that the quality indicates
     * good sensor data:
     *
     * > bit 31: do not use scan for product generation
     * > bit 30: time sequence error detected within this scan (see below)
     * bit 29: data gap precedes this scan (gap may be due to actual 
     * lost scans or scans in which the TIP or MIU are in non-nominal modes)
     * bit 28: insufficient data for calibration (see below)
     * bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * > bit 25: instrument status changed with this scan
     * bits 24 - 5: <zero fill>
     * > bit 4: transmitter status change occurred (see note 2)
     * > bit 3: MHS sync error detected
     * > bit 2: MHS minor frame error detected
     * > bit 1: MHS major frame error detected
     * > bit 0: MHS parity error detected
     */
    long quality = reader.getLong ("qualityIndicator", buffer);
    return ((quality & 0xc200001f) == 0);

  } // isSensorDataUsable

  ////////////////////////////////////////////////////////////
  
  public short[] getSensorData() {

    int[] rawSensorData = reader.getIntArray ("mhsData", buffer);
    short[] sensorData = new short[MHS.SAMPLES * MHS.CHANNELS];
    for (int i = 0; i < MHS.SAMPLES; i++) {
      int offset = i*SENSOR_WORDS + 1;
      for (int j = 0; j < MHS.CHANNELS; j++) {
        sensorData[(MHS.SAMPLES-1-i)*MHS.CHANNELS + j] = 
          (short) (rawSensorData[offset + j] & 0xffff);
      } // for    
    } // for

    return (sensorData);

  } // getSensorData

  ////////////////////////////////////////////////////////////

  public boolean isCalibrationUsable() {

    /**
     * For calibration usability, we need to check both the
     * quality indicator and specific calibration problem.  We're
     * willing to accept some minor calibration uncertainty:
     *
     * Quality indicator:
     * bit 31: do not use scan for product generation
     * bit 30: time sequence error detected within this scan (see below)
     * bit 29: data gap precedes this scan (gap may be due to actual      
     * lost scans or scans in which the TIP or MIU are in non-nominal modes)
     * > bit 28: insufficient data for calibration (see below)
     * bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * bit 25: instrument status changed with this scan
     * bits 24 - 5: <zero fill>
     * bit 4: transmitter status change occurred (see note 2)
     * bit 3: MHS sync error detected
     * bit 2: MHS minor frame error detected
     * bit 1: MHS major frame error detected
     * bit 0: MHS parity error detected
     *
     * Calibration problem:
     *
     * Word 1
     * bits 7-2: <zero fill>
     * bit 1: scan line contains one or more space views that are 
     * lunar contaminated
     * bit 0: lunar-contaminated scan line was able to be calibrated 
     * (only applicable if the previous flag [bit 1] is 1; otherwise, zero)
     *
     * Word 2
     * > bit 7: scan line was not calibrated because of bad time
     * bit 6: scan line was calibrated using fewer than the preferred 
     * number of scan lines because of proximity to start or end of 
     * data set or to a data gap
     * > bit 5: scan line was not calibrated because of bad or 
     * insufficient PRT data
     * bit 4: scan line was calibrated but with marginal PRT data
     * bit 3: some uncalibrated channels on this scan (see channel indicators)
     * > bit 2: uncalibrated due to instrument mode
     * bit 1: questionable calibration because of antenna position 
     * error of space view
     * bit 0: questionable calibration because of antenna position 
     * error of OBCT view
     */
    long quality = reader.getLong ("qualityIndicator", buffer);
    int calProblem = reader.getInt ("calibrationProblem", buffer);
    return (
      (quality & 0x10000000) == 0 || 
      (calProblem & 0xa4) == 0
    );

  } // isCalibrationUsable

  ////////////////////////////////////////////////////////////

  /**
   * Gets calibration data as tuplets of [a0, a1, a2]
   * coefficients for each channel.  Radiance in mW/(m^2 sr cm^-1)
   * may be computed from count value using the equation:
   * radiance = a0 + a1*count + a2*count^2.
   */
  public float[] getCalibration() {

    // Get calibration quality flags
    // -----------------------------
    /**
     * We're checking the individual channel calibration flags
     * here, according to the documentation:
     *
     * > bit 6: this scan line is either the last one before or the first 
     * one after a sudden, anomalous jump (or drop) in calibration counts
     * > bit 5: all bad OBCT view counts for scan line
     * > bit 4: all bad space view counts for scan line
     * > bit 3: all bad PRTs for this line
     * bit 2: marginal OBCT view counts for this line
     * bit 1: marginal space view counts for this line
     * bit 0: marginal PRT temps on this line
     */
    int[] flagArray = reader.getIntArray ("calibrationQualityFlags", buffer);
    boolean[] isGoodCalibration = new boolean[MHS.CHANNELS];
    for (int i = 0; i < MHS.CHANNELS; i++) 
      isGoodCalibration[i] = ((flagArray[i] & 0x78) == 0);

    // Get calibration coefficients
    // ----------------------------
    float[] calibration = new float[3*MHS.CHANNELS];
    Arrays.fill (calibration, Float.NaN);
    for (int i = 0; i < MHS.CHANNELS; i++) {
      if (isGoodCalibration[i]) {
        calibration[i*3] = (float) reader.getDouble ("chH" + (i+1) + 
          "ZerothOrder", buffer);
        calibration[i*3 + 1] = (float) reader.getDouble ("chH" + (i+1) + 
          "FirstOrder", buffer);
        calibration[i*3 + 2] = (float) reader.getDouble ("chH" + (i+1) + 
          "SecondOrder", buffer);
      } // if
    } // for

    return (calibration);

  } // getCalibration

  ////////////////////////////////////////////////////////////

  public boolean isNavigationUsable() {

    /**
     * For the navigation check, we're looking at the quality
     * indicator again:
     *
     * bit 31: do not use scan for product generation
     * bit 30: time sequence error detected within this scan (see below)
     * bit 29: data gap precedes this scan (gap may be due to actual
     * lost scans or scans in which the TIP or MIU are in non-nominal modes)
     * bit 28: insufficient data for calibration (see below)
     * > bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * bit 25: instrument status changed with this scan
     * bits 24 - 5: <zero fill>
     * bit 4: transmitter status change occurred (see note 2)
     * bit 3: MHS sync error detected 
     * bit 2: MHS minor frame error detected
     * bit 1: MHS major frame error detected
     * bit 0: MHS parity error detected
     */
    long quality = reader.getLong ("qualityIndicator", buffer);
    return ((quality & 0x08000000) == 0);

  } // isNavigationUsable

  ////////////////////////////////////////////////////////////

  /**
   * Gets navigation data as tuplets of [solar zenith, satellite
   * zenith, relative azimuth, latitude, longitude] for each
   * pixel.
   */
  public float[] getNavigation() {

    // Read navigation
    // ---------------
    short[] angles = reader.getShortArray ("angles", buffer);
    int[] locations = reader.getIntArray ("earthLocations", buffer);

    // Convert to degrees
    // ------------------
    float[] navigation = new float[5*MHS.SAMPLES];
    for (int i = 0; i < MHS.SAMPLES; i++) {
      for (int j = 0; j < 3; j++)
        navigation[5*(MHS.SAMPLES-1-i) + j] = angles[3*i + j] * 1e-2f;
      for (int j = 0; j < 2; j++)
        navigation[5*(MHS.SAMPLES-1-i) + (j+3)] = locations[2*i + j] * 1e-4f;
    } // for

    return (navigation);

  } // getNavigation

  ////////////////////////////////////////////////////////////

  public InstrumentData getData() {

    return (new MHSData ((MHSHeader) header, this));

  } // getData

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new record.
   *
   * @param buffer the buffer to use for record data.
   * @param header the header record for this data record.
   *
   * @throws RuntimeException if the XML stream reader resource
   * file for this class cannot be found.
   */
  public MHSRecord (
    ByteBuffer buffer,
    MHSHeader header
  ) {
    
    super (buffer, header);

  } // MHSRecord

  ////////////////////////////////////////////////////////////

} // MHSRecord class

////////////////////////////////////////////////////////////////////////
