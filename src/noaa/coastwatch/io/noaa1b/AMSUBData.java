////////////////////////////////////////////////////////////////////////
/*

     File: AMSUBData.java
   Author: Peter Hollemans
     Date: 2007/11/22

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

// Import 
// ------
import java.io.IOException;
import noaa.coastwatch.io.noaa1b.AMSUB;
import noaa.coastwatch.io.noaa1b.AMSUBHeader;
import noaa.coastwatch.io.noaa1b.AMSUBRecord;
import terrenus.instrument.Instrument;
import terrenus.instrument.RadiometerCalibrator;
import terrenus.instrument.RadiometerData;
import terrenus.instrument.RadiometerCalibrator.CalibrationType;

/**
 * The <code>AMSUBData</code> class holds data from an AMSU-B
 * instrument on the NOAA KLMNN' series spacecrafts.
 *
 * @see AMSUB
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class AMSUBData
  implements RadiometerData {

  // Constants
  // ---------

  /** First radiation constant (mW/(m2.sr.cm-4)). */
  private static final double C1 = 1.1910427e-5;

  /** Second radiation constant (cm.K). */
  private static final double C2 = 1.4387752;

  // Variables
  // ---------

  /** The header record for this data. */
  private AMSUBHeader header;

  /** The data record for this data. */
  private AMSUBRecord record;

  /** The temporary count data array. */
  private int[] countData = new int[AMSUB.SAMPLES];

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new data object for the specified record.
   *
   * @param record the data record for source data.
   */
  public AMSUBData (
    AMSUBHeader header,
    AMSUBRecord record
  ) {

    this.header = header;
    this.record = record;

  } // AMSUBData constructor

  ////////////////////////////////////////////////////////////

  public Instrument getInstrument() { return (AMSUB.getInstance()); }

  ////////////////////////////////////////////////////////////

  public int[] getCountData (
    int channel, 
    int[] countData
  ) throws IOException {

    // Create count data array
    // -----------------------
    if (countData == null) countData = new int[AMSUB.SAMPLES];

    // Get count data
    // --------------
    short[] sensorData = record.getSensorData();
    for (int i = 0; i < AMSUB.SAMPLES; i++)
      countData[i] = sensorData[i*AMSUB.CHANNELS + (channel-1)];

    return (countData);

  } // getCountData

  ////////////////////////////////////////////////////////////

  public double[] getCalibratedData (
    int channel,
    CalibrationType calType,
    double[] calData
  ) throws IOException {

    // Create calibrated data array
    // ----------------------------
    if (calData == null) calData = new double[AMSUB.SAMPLES];

    // Get count and calibration data
    // ------------------------------
    getCountData (channel, countData);
    float[] calibration = record.getCalibration();
    float a0 = calibration[(channel-1)*3];
    float a1 = calibration[(channel-1)*3 + 1];
    float a2 = calibration[(channel-1)*3 + 2];
    
    // Check calibration type
    // ----------------------
    switch (calType) {
    case RADIANCE: 
    case CELSIUS: 
    case KELVIN: 
      break;
    default: 
      throw new IllegalArgumentException ("Invalid calibration type " +
        calType + " for thermal channel " + channel);
    } // switch

    // Initially calibrate to radiance
    // -------------------------------
    for (int j = 0; j < countData.length; j++) {
      int count = countData[j] & 0xffff;
      calData[j] = a0 + a1*count + a2*count*count;
    } // for

    // Calibrate to blackbody temperature
    // ----------------------------------
    if (calType != CalibrationType.RADIANCE) {
      
      // Get constants
      // -------------
      float[] radCalibration = header.getCalibration();
      double vc = radCalibration[3*(channel-1)];
      double b = radCalibration[3*(channel-1) + 1];
      double c = radCalibration[3*(channel-1) + 2];
      double c2_vc = C2*vc;
      double c1_vc3 = C1*vc*vc*vc;
      if (calType == CalibrationType.CELSIUS) b = b + 273.15*c;
      
      // Calibrate radiance to temperature
      // ---------------------------------
      for (int j = 0; j < countData.length; j++) {
        double tstar = c2_vc/Math.log (1 + (c1_vc3/calData[j]));
        calData[j] = (tstar - b)/c;
      } // for
      
    } // if

    return (calData);

  } // getCalibratedData

  ////////////////////////////////////////////////////////////

  public void getLocationData (
    double[] latitude,
    double[] longitude,
    double[] satZenith,
    double[] solZenith,
    double[] relAzimuth
  ) {

    float[] navigation = record.getNavigation();

    // Get solar zenith data
    // ---------------------
    if (solZenith != null) {
      for (int i = 0; i < AMSUB.SAMPLES; i++) {
        solZenith[i] = navigation[i*5];
      } // for
    } // if

    // Get satellite zenith data
    // -------------------------
    if (satZenith != null) {
      for (int i = 0; i < AMSUB.SAMPLES; i++) {
        satZenith[i] = navigation[i*5 + 1];
      } // for
    } // if

    // Get relative azimuth data
    // -------------------------
    if (relAzimuth != null) {
      for (int i = 0; i < AMSUB.SAMPLES; i++) {
        relAzimuth[i] = navigation[i*5 + 2];
      } // for
    } // if

    // Get latitude data
    // -----------------
    if (latitude != null) {
      for (int i = 0; i < AMSUB.SAMPLES; i++) {
        latitude[i] = navigation[i*5 + 3];
      } // for
    } // if

    // Get longitude data
    // -----------------
    if (longitude != null) {
      for (int i = 0; i < AMSUB.SAMPLES; i++) {
        longitude[i] = navigation[i*5 + 4];
      } // for
    } // if

  } // getLocationData

  ////////////////////////////////////////////////////////////

} // AMSUBData class

////////////////////////////////////////////////////////////////////////
