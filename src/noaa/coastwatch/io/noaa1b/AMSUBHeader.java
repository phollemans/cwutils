////////////////////////////////////////////////////////////////////////
/*

     File: AMSUBHeader.java
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

// Imports
// -------
import java.nio.ByteBuffer;
import noaa.coastwatch.io.BinaryStreamReader;
import noaa.coastwatch.io.noaa1b.AMSUB;
import noaa.coastwatch.io.noaa1b.AMSUBRecord;
import noaa.coastwatch.io.noaa1b.AbstractDataHeader;
import noaa.coastwatch.io.noaa1b.DataRecord;
import terrenus.instrument.Instrument;

/**
 * The <code>AMSUBHeader</code> class reads NOAA 1b data AMSU-B
 * header records.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class AMSUBHeader extends AbstractDataHeader {

  ////////////////////////////////////////////////////////////

  public Instrument getInstrument() { return (AMSUB.getInstance()); }

  ////////////////////////////////////////////////////////////

  public int getRecordSize() { return (3072); }

  ////////////////////////////////////////////////////////////

  public int getRecordAttSize() { return (32); }

  ////////////////////////////////////////////////////////////

  public DataRecord getDataRecord (
    ByteBuffer inputBuffer
  ) {

    return (new AMSUBRecord (inputBuffer, this));  

  } // getDataRecord

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new header.
   *
   * @param buffer the buffer to use for header data.
   */
  public AMSUBHeader (
    ByteBuffer buffer
  ) {

    super (buffer);

  } // AMSUBHeader

  ////////////////////////////////////////////////////////////

  /**
   * Gets the AMSU-B calibration data as tuplets of [v, b, c] for
   * each of the 15 thermal channels.  Temperature to radiance
   * conversion follows T* = b + c T where T is the target
   * temperature, then using the Planck relation, r = c1 v^3 /
   * (e^((c2 v)/T*) - 1) where c1 = 1.1910439 x 10^-5 mW/(m^2 sr
   * cm^-4), c2 = 1.4387686 cm K, and r is the radiance in
   * mW/(m^2 sr cm^-1).
   */
  public float[] getCalibration() {
 
    float[] calibration = new float[3*AMSUB.CHANNELS];
    for (int i = 0; i < AMSUB.CHANNELS; i++) {
      calibration[i*3] = (float) reader.getDouble ("ch" + (i+16) + 
        "CentralWave", buffer);
      calibration[i*3 + 1] = (float) reader.getDouble ("ch" + (i+16) + 
        "Constant1", buffer);
      calibration[i*3 + 2] = (float) reader.getDouble ("ch" + (i+16) + 
        "Constant2", buffer);
    } // for

    return (calibration);

  } // getCalibration

  ////////////////////////////////////////////////////////////

} // AMSUBHeader class

////////////////////////////////////////////////////////////////////////
