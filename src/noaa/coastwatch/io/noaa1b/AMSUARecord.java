////////////////////////////////////////////////////////////////////////
/*

     File: AMSUARecord.java
   Author: Peter Hollemans
     Date: 2007/11/16

  CoastWatch Software Library and Utilities
  Copyright (c) 2007 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.io.noaa1b;

// Imports
// -------
import java.nio.ByteBuffer;
import java.util.Arrays;
import noaa.coastwatch.io.BinaryStreamReader;
import noaa.coastwatch.io.noaa1b.AMSUA;
import noaa.coastwatch.io.noaa1b.AMSUAData;
import noaa.coastwatch.io.noaa1b.AMSUAHeader;
import noaa.coastwatch.io.noaa1b.AbstractDataRecord;
import terrenus.instrument.InstrumentData;

/**
 * The <code>AMSUARecord</code> class reads NOAA 1b AMSU-A data
 * records.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class AMSUARecord extends AbstractDataRecord {

  // Constants
  // ---------

  /** The number of words in each AMSU-A1 sensor data record. */
  private static final int SENSOR_WORDS_A1 = 17;

  /** The number of words in each AMSU-A2 sensor data record. */
  private static final int SENSOR_WORDS_A2 = 4;

  ////////////////////////////////////////////////////////////

  public boolean isSensorDataUsable() {

    /**
     * What we're checking here is that the quality indicates
     * good sensor data:
     *
     * > bit 31: do not use scan for product generation
     * > bit 30: time sequence error detected within this scan (see below)
     * bit 29: data gap precedes this scan
     * bit 28: insufficient data for calibration (see below)
     * bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * > bit 25: instrument status changed with this scan
     * bits 24 - 4: <zero fill>
     * > bit 3: AMSU sync error detected
     * > bit 2: AMSU minor frame error detected
     * > bit 1: AMSU major frame error detected
     * > bit 0: AMSU parity error detected	
     */
    long quality = reader.getLong ("qualityIndicator", buffer);
    return ((quality & 0xc200000f) == 0);

  } // isSensorDataUsable

  ////////////////////////////////////////////////////////////
  
  public short[] getSensorData() {

    int[] rawSensorData1 = reader.getIntArray ("amsua1Data", buffer);
    int[] rawSensorData2 = reader.getIntArray ("amsua2Data", buffer);
    short[] sensorData = new short[AMSUA.SAMPLES * AMSUA.CHANNELS];
    for (int i = 0; i < AMSUA.SAMPLES; i++) {

      // Get AMSU-A2 channels 1-2 data
      // -----------------------------
      int offset = i*SENSOR_WORDS_A2 + 2;
      for (int j = 0; j < 2; j++) {
        sensorData[(AMSUA.SAMPLES-1-i)*AMSUA.CHANNELS + j] = 
          (short) (rawSensorData2[offset + j] & 0xffff);
      } // for    

      // Get AMSU-A1 channels 3-15 data
      // ------------------------------
      offset = i*SENSOR_WORDS_A1 + 4;
      for (int j = 2; j < AMSUA.CHANNELS; j++) {
        sensorData[(AMSUA.SAMPLES-1-i)*AMSUA.CHANNELS + j] = 
          (short) (rawSensorData1[offset + (j-2)] & 0xffff);
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
     * bit 29: data gap precedes this scan
     * > bit 28: insufficient data for calibration (see below)
     * bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * bit 25: instrument status changed with this scan
     * bits 24 - 4: <zero fill>
     * bit 3: AMSU sync error detected
     * bit 2: AMSU minor frame error detected
     * bit 1: AMSU major frame error detected
     * bit 0: AMSU parity error detected	
     *
     * Calibration problem:
     *
     * > bit 7: scan line was not calibrated because of bad time
     * bit 6: scan line was calibrated using fewer than the preferred number 
     * of scan lines because of proximity to start or end of data set 
     * or to a data gap
     * > bit 5: scan line was not calibrated because of bad or insufficient 
     * PRT data
     * bit 4: scan line was calibrated but with marginal PRT data
     * bit 3: some uncalibrated channels on this scan (see channel indicators)
     * > bit 2: uncalibrated due to instrument mode
     * bit 1: questionable calibration because of antenna position error 
     * of space view 
     * bit 0: questionable calibration because of antenna position error 
     * of blackbody view
     */
    long quality = reader.getLong ("qualityIndicator", buffer);
    short calProblem = reader.getShort ("calibrationProblem", buffer);
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
     * > bit 8: this scan line is either the last one before or the 
     * first one after a sudden, anomalous jump (or drop) in 
     * calibration counts
     * > bit 7: lunar contamination was detected in the space view 
     * counts of this channel
     * bit 6: the space view counts of this channel were corrected 
     * for lunar contamination when used in the calibration (only 
     * applicable if the previous flag [bit 7] is 1; otherwise, zero)
     * > bit 5: All bad blackbody view counts for scan line
     * > bit 4: All bad space view counts for scan line
     * > bit 3: All bad PRTs for this line
     * bit 2: Marginal blackbody view counts for this line
     * bit 1: Marginal space view counts for this line
     * bit 0: Marginal PRT temps on this line
     */
    int[] flagArray = reader.getIntArray ("calibrationQualityFlags", buffer);
    boolean[] isGoodCalibration = new boolean[AMSUA.CHANNELS];
    for (int i = 0; i < AMSUA.CHANNELS; i++) 
      isGoodCalibration[i] = ((flagArray[i] & 0x1b8) == 0);

    // Get calibration coefficients
    // ----------------------------
    float[] calibration = new float[3*AMSUA.CHANNELS];
    Arrays.fill (calibration, Float.NaN);
    for (int i = 0; i < AMSUA.CHANNELS; i++) {
      if (isGoodCalibration[i]) {
        calibration[i*3] = (float) reader.getDouble ("ch" + (i+1) + 
          "ZerothOrder", buffer);
        calibration[i*3 + 1] = (float) reader.getDouble ("ch" + (i+1) + 
          "FirstOrder", buffer);
        calibration[i*3 + 2] = (float) reader.getDouble ("ch" + (i+1) + 
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
     * bit 29: data gap precedes this scan
     * bit 28: insufficient data for calibration (see below)
     * > bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * bit 25: instrument status changed with this scan
     * bits 24 - 4: <zero fill>
     * bit 3: AMSU sync error detected
     * bit 2: AMSU minor frame error detected
     * bit 1: AMSU major frame error detected
     * bit 0: AMSU parity error detected	
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
    float[] navigation = new float[5*AMSUA.SAMPLES];
    for (int i = 0; i < AMSUA.SAMPLES; i++) {
      for (int j = 0; j < 3; j++)
        navigation[5*(AMSUA.SAMPLES-1-i) + j] = angles[3*i + j] * 1e-2f;
      for (int j = 0; j < 2; j++)
        navigation[5*(AMSUA.SAMPLES-1-i) + (j+3)] = locations[2*i + j] * 1e-4f;
    } // for

    return (navigation);

  } // getNavigation

  ////////////////////////////////////////////////////////////

  public InstrumentData getData() {

    return (new AMSUAData ((AMSUAHeader) header, this));

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
  public AMSUARecord (
    ByteBuffer buffer,
    AMSUAHeader header
  ) {
    
    super (buffer, header);

  } // AMSUARecord

  ////////////////////////////////////////////////////////////

} // AMSUARecord class

////////////////////////////////////////////////////////////////////////
