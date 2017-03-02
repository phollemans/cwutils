////////////////////////////////////////////////////////////////////////
/*

     File: AMSUAHeader.java
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
import noaa.coastwatch.io.BinaryStreamReader;
import noaa.coastwatch.io.noaa1b.AMSUA;
import noaa.coastwatch.io.noaa1b.AMSUARecord;
import noaa.coastwatch.io.noaa1b.AbstractDataHeader;
import noaa.coastwatch.io.noaa1b.DataRecord;
import terrenus.instrument.Instrument;

/**
 * The <code>AMSUAHeader</code> class reads NOAA 1b data AMSU-A
 * header records.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class AMSUAHeader extends AbstractDataHeader {

  ////////////////////////////////////////////////////////////

  public Instrument getInstrument() { return (AMSUA.getInstance()); }

  ////////////////////////////////////////////////////////////

  public int getRecordSize() { return (2560); }

  ////////////////////////////////////////////////////////////

  public int getRecordAttSize() { return (32); }

  ////////////////////////////////////////////////////////////

  public DataRecord getDataRecord (
    ByteBuffer inputBuffer
  ) {

    return (new AMSUARecord (inputBuffer, this));  

  } // getDataRecord

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new header.
   *
   * @param buffer the buffer to use for header data.
   */
  public AMSUAHeader (
    ByteBuffer buffer
  ) {

    super (buffer);

  } // AMSUAHeader

  ////////////////////////////////////////////////////////////

  /**
   * Gets the AMSU-A calibration data as tuplets of [v, b, c] for
   * each of the 15 thermal channels.  Temperature to radiance
   * conversion follows T* = b + c T where T is the target
   * temperature, then using the Planck relation, r = c1 v^3 /
   * (e^((c2 v)/T*) - 1) where c1 = 1.1910439 x 10^-5 mW/(m^2 sr
   * cm^-4), c2 = 1.4387686 cm K, and r is the radiance in
   * mW/(m^2 sr cm^-1).
   */
  public float[] getCalibration() {
 
    float[] calibration = new float[3*AMSUA.CHANNELS];
    for (int i = 0; i < AMSUA.CHANNELS; i++) {
      calibration[i*3] = (float) reader.getDouble ("ch" + (i+1) + 
        "CentralWave", buffer);
      calibration[i*3 + 1] = (float) reader.getDouble ("ch" + (i+1) + 
        "Constant1", buffer);
      calibration[i*3 + 2] = (float) reader.getDouble ("ch" + (i+1) + 
        "Constant2", buffer);
    } // for

    return (calibration);

  } // getCalibration

  ////////////////////////////////////////////////////////////

} // AMSUAHeader class

////////////////////////////////////////////////////////////////////////
