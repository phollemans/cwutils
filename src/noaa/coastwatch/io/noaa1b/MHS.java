////////////////////////////////////////////////////////////////////////
/*

     File: MHS.java
   Author: Peter Hollemans
     Date: 2007/11/23

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
 * The <code>MHS</code> is a radiometer for the NOAA Microwave Humidity
 * Sounder.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class MHS implements Radiometer {

  // Constants
  // ---------

  /** The number of channels for this sensor. */
  public static final int CHANNELS = 5;

  /** The number of sample per scan line for this sensor. */
  public static final int SAMPLES = 90;

  // Variables
  // ---------

  /** The singleton instance of this class. */
  private static MHS instance = new MHS();

  ////////////////////////////////////////////////////////////

  private MHS () {}

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static MHS getInstance() { return (instance); }

  ////////////////////////////////////////////////////////////

  public String getName() { return ("MHS"); }

  ////////////////////////////////////////////////////////////

  public String getLongName() { 

    return ("Microwave Humidity Sounder");

  } // getLongName

  ////////////////////////////////////////////////////////////

  public int getChannelCount() { return (CHANNELS); }

  ////////////////////////////////////////////////////////////

  public String getChannelName (int channel) {
    
    if (channel < 1 || channel > CHANNELS) 
      throw new IllegalArgumentException();
    return ("mhs_chH" + channel);

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

} // MHS class

////////////////////////////////////////////////////////////////////////
