////////////////////////////////////////////////////////////////////////
/*

     File: HIRS4Header.java
   Author: Peter Hollemans
     Date: 2007/10/11

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
import noaa.coastwatch.io.BinaryStreamReader;
import noaa.coastwatch.io.noaa1b.AbstractDataHeader;
import noaa.coastwatch.io.noaa1b.DataRecord;
import noaa.coastwatch.io.noaa1b.HIRS4;
import noaa.coastwatch.io.noaa1b.HIRS4Record;
import terrenus.instrument.Instrument;

/**
 * The <code>HIRS4Header</code> class reads NOAA 1b data HIRS/4
 * header records.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class HIRS4Header extends AbstractDataHeader {

  ////////////////////////////////////////////////////////////

  public Instrument getInstrument() { return (HIRS4.getInstance()); }

  ////////////////////////////////////////////////////////////

  public int getRecordSize() { return (4608); }

  ////////////////////////////////////////////////////////////

  public int getRecordAttSize() { return (36); }

  ////////////////////////////////////////////////////////////

  public DataRecord getDataRecord (
    ByteBuffer inputBuffer
  ) {

    return (new HIRS4Record (inputBuffer, this));  

  } // getDataRecord

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new header.
   *
   * @param buffer the buffer to use for header data.
   */
  public HIRS4Header (
    ByteBuffer buffer
  ) {

    super (buffer);

  } // HIRS4Header

  ////////////////////////////////////////////////////////////

  /**
   * Gets the HIRS/4 calibration data as tuplets of [v, b, c] for
   * each of the first 19 thermal channels, plus [solar
   * irradiance, filter width] for channel 20.  Temperature to
   * radiance conversion follows T* = b + c T where T is the
   * target temperature, then using the Planck relation, r = c1
   * v^3 / (e^((c2 v)/T*) - 1) where c1 = 1.1910439 x 10^-5
   * mW/(m^2 sr cm^-4), c2 = 1.4387686 cm K, and r is the
   * radiance in mW/(m^2 sr cm^-1).
   */
  public float[] getCalibration() {
 
    float[] calibration = new float[3*HIRS4.CHANNELS - 1];
    for (int i = 0; i < HIRS4.CHANNELS-1; i++) {
      calibration[i*3] = (float) reader.getDouble ("ch" + (i+1) + 
        "CentralWave", buffer);
      calibration[i*3 + 1] = (float) reader.getDouble ("ch" + (i+1) + 
        "Constant1", buffer);
      calibration[i*3 + 2] = (float) reader.getDouble ("ch" + (i+1) + 
        "Constant2", buffer);
    } // for
    calibration[calibration.length-2] = (float) reader.getDouble (
      "ch20SolarIrradiance", buffer);
    calibration[calibration.length-1] = (float) reader.getDouble (
      "ch20FilterWidth", buffer);

    return (calibration);

  } // getCalibration

  ////////////////////////////////////////////////////////////

} // HIRS4Header class

////////////////////////////////////////////////////////////////////////
