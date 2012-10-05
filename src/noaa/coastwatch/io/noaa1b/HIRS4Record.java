////////////////////////////////////////////////////////////////////////
/*
     FILE: HIRS4Record.java
  PURPOSE: Reads NOAA 1b format HIRS/4 data records.
   AUTHOR: Peter Hollemans
     DATE: 2007/10/11
  CHANGES: 2007/11/21, PFH
           - added comments for quality flag interpretation
           - changed calibration code from byte to short value

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
 * The <code>HIRS4Record</code> class reads NOAA
 * 1b HIRS/4 data records.
 */
public class HIRS4Record extends AbstractDataRecord {

  // Constants
  // ---------

  /** Positions for channel data words in sensor data array. */
  private static final int[] POSITION = new int[] {
    0,2,3,5,15,14,9,10,19,12,7,17,4,13,16,18,1,6,8,11
  };

  /** The number of words in each sensor data record. */
  private static final int SENSOR_WORDS = 4 + HIRS4.CHANNELS;

  ////////////////////////////////////////////////////////////

  public boolean isSensorDataUsable() {

    /**
     * What we're checking here is that the sensor is pointed at
     * the earth, and the quality indicates good sensor data:
     *
     * > bit 31: do not use scan for product generation
     * > bit 30: time sequence error detected within this scan (see below)
     * bit 29: data gap precedes this scan
     * bit 28: calibration anomaly detected (see below)
     * bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * > bit 25: instrument status changed with this scan     
     */
    long quality = reader.getLong ("qualityIndicator", buffer);
    int scanType = reader.getInt ("scanType", buffer);
    return (scanType == 0 && (quality & 0xc2000000) == 0);

  } // isSensorDataUsable

  ////////////////////////////////////////////////////////////
  
  public short[] getSensorData() {

    short[] rawSensorData = reader.getShortArray ("hirsData", buffer);
    short[] sensorData = new short[HIRS4.SAMPLES * HIRS4.CHANNELS];
    for (int i = 0; i < HIRS4.SAMPLES; i++) {
      int offset = i*SENSOR_WORDS + 2;
      for (int j = 0; j < HIRS4.CHANNELS; j++) {
        sensorData[(HIRS4.SAMPLES-1-i)*HIRS4.CHANNELS + j] = 
          (short) (rawSensorData[offset + POSITION[j]] - 4096);
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
     * > bit 28: calibration anomaly detected (see below)
     * bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * bit 25: instrument status changed with this scan     
     *
     * Calibration problem:
     *
     * > bit 7: scan was not calibrated 
     * bit 6: scan contains marginal calibration in some of the IR channels
     * > bit 5: PRT quality test failed
     * bit 4: PRT data marginal, some readings were rejected
     * > bit 3: scan contains some uncalibrated channels
     * > bit 2: scan indicates that the normal HIRS calibration sequence 
     * is disabled
     * > bit 1: space view scan is lunar contaminated
     */
    long quality = reader.getLong ("qualityIndicator", buffer);
    short calProblem = reader.getShort ("calibrationProblem", buffer);
    return (
      (quality & 0x10000000) == 0 || 
      (calProblem & 0xae) == 0
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

    float[] calibration = new float[3*HIRS4.CHANNELS];
    for (int i = 0; i < HIRS4.CHANNELS; i++) {
      calibration[i*3] = (float) reader.getDouble ("ch" + (i+1) + 
        "ZerothOrder", buffer);
      calibration[i*3 + 1] = (float) reader.getDouble ("ch" + (i+1) + 
        "FirstOrder", buffer);
      calibration[i*3 + 2] = (float) reader.getDouble ("ch" + (i+1) + 
        "SecondOrder", buffer);
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
     * bit 28: calibration anomaly detected (see below)
     * > bit 27: earth location data not available (see below)
     * bit 26: first good time following a clock update (nominally 0)
     * bit 25: instrument status changed with this scan     
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
    float[] navigation = new float[5*HIRS4.SAMPLES];
    for (int i = 0; i < HIRS4.SAMPLES; i++) {
      for (int j = 0; j < 3; j++)
        navigation[5*(HIRS4.SAMPLES-1-i) + j] = angles[3*i + j] * 1e-2f;
      for (int j = 0; j < 2; j++)
        navigation[5*(HIRS4.SAMPLES-1-i) + (j+3)] = locations[2*i + j] * 1e-4f;
    } // for

    return (navigation);

  } // getNavigation

  ////////////////////////////////////////////////////////////

  public InstrumentData getData() {

    return (new HIRS4Data ((HIRS4Header) header, this));

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
  public HIRS4Record (
    ByteBuffer buffer,
    HIRS4Header header
  ) {
    
    super (buffer, header);

  } // HIRS4Record

  ////////////////////////////////////////////////////////////

} // HIRS4Record class

////////////////////////////////////////////////////////////////////////
