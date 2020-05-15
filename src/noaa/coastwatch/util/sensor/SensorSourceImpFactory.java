////////////////////////////////////////////////////////////////////////
/*

     File: SensorSourceImpFactory.java
   Author: Peter Hollemans
     Date: 2019/03/15

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
import noaa.coastwatch.util.sensor.SensorIdentifier;
import noaa.coastwatch.util.sensor.SensorIdentifier.Sensor;
import noaa.coastwatch.util.sensor.AVHRRSourceImp;
import noaa.coastwatch.util.sensor.MODISSourceImp;
import noaa.coastwatch.util.sensor.VIIRSSourceImp;
import noaa.coastwatch.util.sensor.GenericSourceImp;
import noaa.coastwatch.util.ResamplingSourceImp;
import noaa.coastwatch.util.trans.EarthTransform;

import java.util.logging.Logger;

/**
 * The <code>SensorSourceImpFactory</code> creates concrete
 * {@link ResamplingSourceImp} objects for resampling sensor data that are
 * customized for each sensor.  The sensor type is automatically
 * detected from the earth transform data.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class SensorSourceImpFactory {

  private static final Logger LOGGER = Logger.getLogger (SensorSourceImpFactory.class.getName());

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new concrete resampling object for the specified transform.
   *
   * @param sourceTrans the source transform to use for location data.
   * @param sensorHint the hint to use for the sensor type, or null for
   * automatic detection.
   *
   * @return the new resampling helper specific to the sensor scan pattern.
   */
  public static ResamplingSourceImp create (
    EarthTransform sourceTrans,
    Sensor sensorHint
  ) {

    ResamplingSourceImp imp;

    Sensor sensor;
    if (sensorHint != null) {
      LOGGER.fine ("Using sensor hint " + sensorHint);
      sensor = sensorHint;
    } // if
    else {
      LOGGER.fine ("No sensor hint, running automatic sensor detection");
      sensor = SensorIdentifier.getSensorFromScan (sourceTrans);
    } // else

    switch (sensor) {
    case AVHRR:
      imp = AVHRRSourceImp.getInstance (sourceTrans);
      break;
    case MODIS:
      imp = MODISSourceImp.getInstance (sourceTrans);
      break;
    case VIIRS_MBAND_EDR:
      imp = VIIRSSourceImp.getInstance (sourceTrans, new VIIRSMBandEDRParams());
      break;
    case VIIRS_MBAND_SDR:
      imp = VIIRSSourceImp.getInstance (sourceTrans, new VIIRSMBandSDRParams());
      break;
    case VIIRS_IBAND_EDR:
      imp = VIIRSSourceImp.getInstance (sourceTrans, new VIIRSIBandEDRParams());
      break;
    case VIIRS_IBAND_SDR:
      imp = VIIRSSourceImp.getInstance (sourceTrans, new VIIRSIBandSDRParams());
      break;
    default:
      imp = GenericSourceImp.getInstance (sourceTrans);
    } // switch
    
    return (imp);
  
  } // create

  ////////////////////////////////////////////////////////////

} // SensorSourceImpFactory class

////////////////////////////////////////////////////////////////////////

