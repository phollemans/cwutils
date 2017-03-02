////////////////////////////////////////////////////////////////////////
/*

     File: HIRS4.java
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
import terrenus.instrument.Radiometer;

/**
 * The <code>HIRS4</code> is a radiometer for the NOAA High
 * Resolution Infrared Radiation Sounder
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class HIRS4 implements Radiometer {

  // Constants
  // ---------

  /** The number of channels for this sensor. */
  public static final int CHANNELS = 20;

  /** The number of sample per scan line for this sensor. */
  public static final int SAMPLES = 56;

  // Variables
  // ---------

  /** The singleton instance of this class. */
  private static HIRS4 instance = new HIRS4();

  ////////////////////////////////////////////////////////////

  private HIRS4 () {}

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static HIRS4 getInstance() { return (instance); }

  ////////////////////////////////////////////////////////////

  public String getName() { return ("HIRS"); }

  ////////////////////////////////////////////////////////////

  public String getLongName() { 

    return ("High Resolution Infrared Radiation Sounder");

  } // getLongName

  ////////////////////////////////////////////////////////////

  public int getChannelCount() { return (CHANNELS); }

  ////////////////////////////////////////////////////////////

  public String getChannelName (int channel) {
    
    if (channel < 1 || channel > CHANNELS) 
      throw new IllegalArgumentException();
    return ("hirs_ch" + channel);

  } // getChannelName

  ////////////////////////////////////////////////////////////

  public boolean isThermal (int channel) {

    if (channel < 1 || channel > CHANNELS) 
      throw new IllegalArgumentException();
    return (channel != 20);

  } // isThermal

  ////////////////////////////////////////////////////////////

  public int getSampleCount() { return (SAMPLES); }

  ////////////////////////////////////////////////////////////

} // HIRS4 class

////////////////////////////////////////////////////////////////////////
