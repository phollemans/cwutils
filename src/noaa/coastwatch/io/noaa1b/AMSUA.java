////////////////////////////////////////////////////////////////////////
/*

     File: AMSUA.java
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
import terrenus.instrument.Radiometer;

/**
 * The <code>AMSUA</code> is a radiometer for the NOAA Advanced
 * Microwave Sounding Unit-A.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class AMSUA implements Radiometer {

  // Constants
  // ---------

  /** The number of channels for this sensor. */
  public static final int CHANNELS = 15;

  /** The number of sample per scan line for this sensor. */
  public static final int SAMPLES = 30;

  // Variables
  // ---------

  /** The singleton instance of this class. */
  private static AMSUA instance = new AMSUA();

  ////////////////////////////////////////////////////////////

  private AMSUA () {}

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static AMSUA getInstance() { return (instance); }

  ////////////////////////////////////////////////////////////

  public String getName() { return ("AMSU-A"); }

  ////////////////////////////////////////////////////////////

  public String getLongName() { 

    return ("Advanced Microwave Sounding Unit-A");

  } // getLongName

  ////////////////////////////////////////////////////////////

  public int getChannelCount() { return (CHANNELS); }

  ////////////////////////////////////////////////////////////

  public String getChannelName (int channel) {
    
    if (channel < 1 || channel > CHANNELS) 
      throw new IllegalArgumentException();
    return ("amsua_ch" + channel);

  } // getChannelName

  ////////////////////////////////////////////////////////////

  public boolean isThermal (int channel) {

    if (channel < 1 || channel > CHANNELS) 
      throw new IllegalArgumentException();
    return (true);

  } // isThermal

  ////////////////////////////////////////////////////////////

  public int getSampleCount() { return (SAMPLES); }

  ////////////////////////////////////////////////////////////

} // AMSUA class

////////////////////////////////////////////////////////////////////////
