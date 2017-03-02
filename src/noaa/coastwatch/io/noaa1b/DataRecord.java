////////////////////////////////////////////////////////////////////////
/*

     File: DataRecord.java
   Author: Peter Hollemans
     Date: 2007/08/27

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
import java.util.Date;
import terrenus.instrument.InstrumentData;

/**
 * The <code>DataRecord</code> interface is for reading NOAA
 * 1b data records.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public interface DataRecord {

  /** Gets the scan line number. */
  public int getScanLine();

  /** Gets the record date. */
  public Date getDate();

  /** Determines if the sensor data is usable based on quality flags. */
  public boolean isSensorDataUsable();

  /** Gets the sensor data values as uncalibrated counts. */
  public short[] getSensorData();

  /** Determines if the calibration data is usable based on quality flags. */
  public boolean isCalibrationUsable();

  /** Gets the calibration data. */
  public float[] getCalibration();

  /** Determines if the navigation data is usable based on quality flags. */
  public boolean isNavigationUsable();

  /** Gets the navigation data. */
  public float[] getNavigation();

  /** Gets the calibrated and earth located sensor data. */
  public InstrumentData getData();

} // DataRecord interface

////////////////////////////////////////////////////////////////////////
