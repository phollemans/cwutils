////////////////////////////////////////////////////////////////////////
/*
     FILE: HIRS4Data.java
  PURPOSE: Reads HIRS/4 instrument data.
   AUTHOR: Peter Hollemans
     DATE: 2007/10/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io.noaa1b;

// Import 
// ------
import java.io.*;
import java.nio.*;
import java.util.*;
import terrenus.instrument.*;
import terrenus.instrument.RadiometerCalibrator.Record;
import terrenus.instrument.RadiometerCalibrator.CalibrationType;

/**
 * The <code>HIRS4Data</code> class holds data from an HIRS/4 instrument
 * on the NOAA NN' series spacecrafts. 
 *
 * @see HIRS4
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class HIRS4Data
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
  private HIRS4Header header;

  /** The data record for this data. */
  private HIRS4Record record;

  /** The temporary count data array. */
  private int[] countData = new int[HIRS4.SAMPLES];

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new data object for the specified record.
   *
   * @param record the data record for source data.
   */
  public HIRS4Data (
    HIRS4Header header,
    HIRS4Record record
  ) {

    this.header = header;
    this.record = record;

  } // HIRS4Data constructor

  ////////////////////////////////////////////////////////////

  public Instrument getInstrument() { return (HIRS4.getInstance()); }

  ////////////////////////////////////////////////////////////

  public int[] getCountData (
    int channel, 
    int[] countData
  ) throws IOException {

    // Create count data array
    // -----------------------
    if (countData == null) countData = new int[HIRS4.SAMPLES];

    // Get count data
    // --------------
    short[] sensorData = record.getSensorData();
    for (int i = 0; i < HIRS4.SAMPLES; i++)
      countData[i] = sensorData[i*HIRS4.CHANNELS + (channel-1)];

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
    if (calData == null) calData = new double[HIRS4.SAMPLES];

    // Get count and calibration data
    // ------------------------------
    getCountData (channel, countData);
    float[] calibration = record.getCalibration();
    float a0 = calibration[(channel-1)*3];
    float a1 = calibration[(channel-1)*3 + 1];
    float a2 = calibration[(channel-1)*3 + 2];
    
    // Check calibration type
    // ----------------------
    boolean isVisible = !HIRS4.getInstance().isThermal (channel);
    if (isVisible) {
      switch (calType) {
      case REFLECTANCE: 
      case ALBEDO: 
        break;
      default: 
        throw new IllegalArgumentException ("Invalid calibration type " +
          calType + " for visible channel " + channel);
      } // switch
    } // if
    else {
      switch (calType) {
      case RADIANCE: 
      case CELSIUS: 
      case KELVIN: 
        break;
      default: 
        throw new IllegalArgumentException ("Invalid calibration type " +
          calType + " for thermal channel " + channel);
      } // switch
    } // else

    // Calibrate visible data
    // ----------------------
    if (isVisible) {
      double scale = (calType == CalibrationType.ALBEDO ? 1 : 0.01);
      for (int j = 0; j < countData.length; j++) {
        calData[j] = scale*(a0 + a1*countData[j] + 
          a2*countData[j]*countData[j]);
      } // for
    } // if

    // Calibrate thermal data
    // ----------------------
    else {
      
      // Initially calibrate to radiance
      // -------------------------------
      for (int j = 0; j < countData.length; j++) {
        calData[j] = a0 + a1*countData[j] + a2*countData[j]*countData[j];
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

    } // else

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
      for (int i = 0; i < HIRS4.SAMPLES; i++) {
        solZenith[i] = navigation[i*5];
      } // for
    } // if

    // Get satellite zenith data
    // -------------------------
    if (satZenith != null) {
      for (int i = 0; i < HIRS4.SAMPLES; i++) {
        satZenith[i] = navigation[i*5 + 1];
      } // for
    } // if

    // Get relative azimuth data
    // -------------------------
    if (relAzimuth != null) {
      for (int i = 0; i < HIRS4.SAMPLES; i++) {
        relAzimuth[i] = navigation[i*5 + 2];
      } // for
    } // if

    // Get latitude data
    // -----------------
    if (latitude != null) {
      for (int i = 0; i < HIRS4.SAMPLES; i++) {
        latitude[i] = navigation[i*5 + 3];
      } // for
    } // if

    // Get longitude data
    // -----------------
    if (longitude != null) {
      for (int i = 0; i < HIRS4.SAMPLES; i++) {
        longitude[i] = navigation[i*5 + 4];
      } // for
    } // if

  } // getLocationData

  ////////////////////////////////////////////////////////////

} // HIRS4Data class

////////////////////////////////////////////////////////////////////////
