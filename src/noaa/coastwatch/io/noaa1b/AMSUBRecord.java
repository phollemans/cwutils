////////////////////////////////////////////////////////////////////////
/*
     FILE: AMSUBRecord.java
  PURPOSE: Reads NOAA 1b format AMSU-B data records.
   AUTHOR: Peter Hollemans
     DATE: 2007/11/22
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
import java.nio.*;
import java.util.*;
import terrenus.instrument.*;

/**
 * The <code>AMSUBRecord</code> class reads NOAA 1b AMSU-B data
 * records.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class AMSUBRecord extends AbstractDataRecord {

  // Constants
  // ---------

  /** The number of words in each AMSU-B sensor data record. */
  private static final int SENSOR_WORDS = 6;

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
     * bits 24 - 7: <zero fill>
     * bit 6: "new" bias status change (0 = no status change detected 
     * in preceding or following scan; 1 = "new" bias calibration uncertain)
     * bit 5: "new" bias status (1 = "new" bias correction on) 
     * > bit 4: transmitter status change occurred 
     * > bit 3: AMSU sync error detected
     * > bit 2: AMSU minor frame error detected
     * > bit 1: AMSU major frame error detected
     * > bit 0: AMSU parity error detected
     */
    long quality = reader.getLong ("qualityIndicator", buffer);
    return ((quality & 0xc200001f) == 0);

  } // isSensorDataUsable

  ////////////////////////////////////////////////////////////
  
  public short[] getSensorData() {

    int[] rawSensorData = reader.getIntArray ("amsubData", buffer);
    short[] sensorData = new short[AMSUB.SAMPLES * AMSUB.CHANNELS];
    for (int i = 0; i < AMSUB.SAMPLES; i++) {
      int offset = i*SENSOR_WORDS + 1;
      for (int j = 0; j < AMSUB.CHANNELS; j++) {
        sensorData[(AMSUB.SAMPLES-1-i)*AMSUB.CHANNELS + j] = 
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
     * bit 29: data gap precedes this scan
     * > bit 28: insufficient data for calibration (see below)
     * bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * bit 25: instrument status changed with this scan
     * bits 24 - 7: <zero fill>
     * bit 6: "new" bias status change (0 = no status change detected 
     * in preceding or following scan; 1 = "new" bias calibration uncertain)
     * bit 5: "new" bias status (1 = "new" bias correction on) 
     * bit 4: transmitter status change occurred 
     * bit 3: AMSU sync error detected
     * bit 2: AMSU minor frame error detected
     * bit 1: AMSU major frame error detected
     * bit 0: AMSU parity error detected
     *
     * Calibration problem:
     *
     * > bit 7: Scan line was not calibrated because of bad time.
     * bit 6: Scan line was calibrated using fewer than the preferred 
     * number of scan lines because of proximity to start or end of data 
     * set or to a data gap.
     * > bit 5: Scan line was not calibrated because of bad or insufficient 
     * PRT data.
     * bit 4: Scan line was calibrated but with marginal PRT data.
     * bit 3: Some uncalibrated channels on this scan. (See channel indicators.)
     * > bit 2: Uncalibrated due to instrument mode.
     * bit 1: Questionable calibration because of antenna position 
     * error of space view.
     * bit 0: Questionable calibration because of antenna position 
     * error of blackbody view.
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
     * > bit 5: All bad blackbody counts for scan line
     * > bit 4: All bad space view counts for scan line
     * > bit 3: All bad PRTs for this line
     * bit 2: Marginal blackbody view counts for this line
     * bit 1: Marginal space view counts for this line
     * bit 0: Marginal PRT temps on this line
     */
    int[] flagArray = reader.getIntArray ("calibrationQualityFlags", buffer);
    boolean[] isGoodCalibration = new boolean[AMSUB.CHANNELS];
    for (int i = 0; i < AMSUB.CHANNELS; i++) 
      isGoodCalibration[i] = ((flagArray[i] & 0x38) == 0);

    // Get calibration coefficients
    // ----------------------------
    float[] calibration = new float[3*AMSUB.CHANNELS];
    Arrays.fill (calibration, Float.NaN);
    for (int i = 0; i < AMSUB.CHANNELS; i++) {
      if (isGoodCalibration[i]) {
        calibration[i*3] = (float) reader.getDouble ("ch" + (i+16) + 
          "ZerothOrder", buffer);
        calibration[i*3 + 1] = (float) reader.getDouble ("ch" + (i+16) + 
          "FirstOrder", buffer);
        calibration[i*3 + 2] = (float) reader.getDouble ("ch" + (i+16) + 
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
     * bits 24 - 7: <zero fill>
     * bit 6: "new" bias status change (0 = no status change detected 
     * in preceding or following scan; 1 = "new" bias calibration uncertain)
     * bit 5: "new" bias status (1 = "new" bias correction on) 
     * bit 4: transmitter status change occurred 
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
    float[] navigation = new float[5*AMSUB.SAMPLES];
    for (int i = 0; i < AMSUB.SAMPLES; i++) {
      for (int j = 0; j < 3; j++)
        navigation[5*(AMSUB.SAMPLES-1-i) + j] = angles[3*i + j] * 1e-2f;
      for (int j = 0; j < 2; j++)
        navigation[5*(AMSUB.SAMPLES-1-i) + (j+3)] = locations[2*i + j] * 1e-4f;
    } // for

    return (navigation);

  } // getNavigation

  ////////////////////////////////////////////////////////////

  public InstrumentData getData() {

    return (new AMSUBData ((AMSUBHeader) header, this));

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
  public AMSUBRecord (
    ByteBuffer buffer,
    AMSUBHeader header
  ) {
    
    super (buffer, header);

  } // AMSUBRecord

  ////////////////////////////////////////////////////////////

} // AMSUBRecord class

////////////////////////////////////////////////////////////////////////
