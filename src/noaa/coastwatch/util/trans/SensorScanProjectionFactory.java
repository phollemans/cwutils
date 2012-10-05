////////////////////////////////////////////////////////////////////////
/*
     FILE: SensorScanProjectionFactory.java
  PURPOSE: Creates sensor scan projection objects.
   AUTHOR: Peter Hollemans
     DATE: 2005/01/18, PFH
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.util.*;

/**
 * The <code>SensorScanProjectionFactory</code> is used to generate 
 * <code>SensorScanProjection</code> objects.  The currently supported 
 * sensors are as follows:
 * <ul>
 *
 *   <li>GEOSTATIONARY - Simulates a geostationary satellite in orbit.
 *   Parameters are as follows:
 *   <ol>
 *     <li>Subpoint latitude in degrees (geocentric).</li>
 *     <li>Subpoint longitude in degrees.</li>
 *     <li>Distance of satellite from center of Earth in kilometers.</li>
 *     <li>Scan step angle in row direction in radians.</li>
 *     <li>Scan step angle in column direction in radians.</li>
 *   </ol></li>
 *
 * </ul>
 *
 */
public class SensorScanProjectionFactory {

  // Constants
  // ---------

  /** The geostationary satellite sensor type code. */
  public static final int GEOSTATIONARY = 
    EllipsoidPerspectiveProjection.SENSOR_CODE;

  ////////////////////////////////////////////////////////////

  /**
   * Creates an instance of a sensor scan projection.
   *
   * @param sensorCode the sensor type code.
   * @param parameters the array of sensor parameters, specific to the
   * sensor type.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   *
   * @return a projection object of the requested type.
   */
  public static SensorScanProjection create (
    int sensorCode,
    double[] parameters,
    int[] dimensions
  ) {

    SensorScanProjection proj = null;
    switch (sensorCode) {

    // Create perspective ellipsoid view
    // ---------------------------------
    case GEOSTATIONARY:
      proj = new EllipsoidPerspectiveProjection (parameters, dimensions);
      break;

    // Detected invalid sensor type
    // ----------------------------
    default:
      throw new RuntimeException ("Unsupported sensor type code");

    } // switch

    return (proj);

  } // create

  ////////////////////////////////////////////////////////////

} // SensorScanProjectionFactory class

////////////////////////////////////////////////////////////////////////
