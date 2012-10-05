////////////////////////////////////////////////////////////////////////
/*
     FILE: AMSUA.java
  PURPOSE: Implements the NOAA AMSU-A radiometer.
   AUTHOR: Peter Hollemans
     DATE: 2007/11/16
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io.noaa1b;

// Imports
// -------
import terrenus.instrument.*;

/**
 * The <code>AMSUA</code> is a radiometer for the NOAA Advanced
 * Microwave Sounding Unit-A.
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
