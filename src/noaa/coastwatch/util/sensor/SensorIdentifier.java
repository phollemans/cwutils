////////////////////////////////////////////////////////////////////////
/*

     File: SensorIdentifier.java
   Author: Peter Hollemans
     Date: 2019/03/14

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.sensor;

// Imports
// -------
import java.util.List;
import java.util.ArrayList;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

import java.util.logging.Logger;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;

/**
 * <p>The <code>SensorIdentifier</code> class identifies scan data from the
 * supported sensors in order to help create sensor-specific concrete objects.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class SensorIdentifier {

  private static final Logger LOGGER = Logger.getLogger (SensorIdentifier.class.getName());

  /** Types of sensors. */
  public static enum Sensor {
    AVHRR,
    MODIS,
    VIIRS_MBAND_SDR,
    VIIRS_MBAND_EDR,
    VIIRS_IBAND_SDR,
    VIIRS_IBAND_EDR,
    UNKNOWN
  } // Sensor

  ////////////////////////////////////////////////////////////

  /**
   * Gets a sensor scan length by looking for discontinuities in geolocation
   * data.
   *
   * @param trans the earth transform from a sensor scan.
   *
   * @return the scan length.
   * 
   * @since 3.8.1
   */
  public static int getSensorScanLength (
    EarthTransform trans
  ) {

    int[] dims = trans.getDimensions();
    DataLocation dataLoc = new DataLocation (2);
    EarthLocation earthLoc = new EarthLocation();

    // Get edge latitudes
    // ------------------
    double[] lat = new double[dims[ROW]];
    dataLoc.set (COL, 0);
    for (int i = 0; i < dims[ROW]; i++) {
      dataLoc.set (ROW, i);
      trans.transform (dataLoc, earthLoc);
      lat[i] = earthLoc.lat;
    } // for
    
    // Detect skips in latitude
    // ------------------------
    List<Integer> scanList = new ArrayList<>();
    int start = -1;
    for (int i = 1; i < dims[ROW]-1; i++) {
      if (!Double.isNaN (lat[i-1]) && !Double.isNaN (lat[i]) && !Double.isNaN (lat[i+1])) {

        // Skip detected
        // -------------
        if ((lat[i] - lat[i-1])*(lat[i+1]-lat[i]) < 0) {
          if (start != -1) {
            int scan = i-start+1;
            scanList.add (scan);
          } // if
          i++;
          start = i;
        } // if
      
      } // if
    } // for

    LOGGER.fine ("Found " + scanList.size() + " skips in geolocation");

    // Find most probable scan length
    // ------------------------------
    int scanLength;
    if (scanList.size() < 2)
      scanLength = 1;
    else {
      scanList.sort (null);
      scanLength = scanList.get (scanList.size()/2);
    } // else

    return (scanLength);

  } // getSensorScanLength

  ////////////////////////////////////////////////////////////

  /**
   * Gets a sensor type using the earth transform to identify sensor-
   * specific scan patterns.
   *
   * @param trans the earth transform from a sensor scan.
   *
   * @return the matching sensor type or unknown.
   */
  public static Sensor getSensorFromScan (
    EarthTransform trans
  ) {
  
    int[] dims = trans.getDimensions();
    int scanLength = getSensorScanLength (trans);

    LOGGER.fine ("Detected scan length of " + scanLength + " rows per scan");
    LOGGER.fine ("Scan width is " + dims[COL] + " columns");

    Sensor sensor;
    if (scanLength == 1 && dims[COL] == 2048)
      sensor = Sensor.AVHRR;
    else if (scanLength == 10 && dims[COL] == 1354)
      sensor = Sensor.MODIS;
    else if (scanLength == 16 && dims[COL] == 3200)
      sensor = Sensor.VIIRS_MBAND_SDR;
    else if (scanLength == 32 && dims[COL] == 6400)
      sensor = Sensor.VIIRS_IBAND_SDR;
    else
      sensor = Sensor.UNKNOWN;

    LOGGER.fine ("Determined sensor is " + sensor);

    return (sensor);
    
  } // getSensorFromScan

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {
    
    EarthDataReader.setDataProjection (true);
    EarthDataReader reader = EarthDataReaderFactory.create (argv[0]);
    Sensor sensor = getSensorFromScan (reader.getInfo().getTransform());

  } // main

  ////////////////////////////////////////////////////////////

} // SensorIdentifier class

////////////////////////////////////////////////////////////////////////

