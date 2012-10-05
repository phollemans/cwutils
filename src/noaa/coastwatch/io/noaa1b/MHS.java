////////////////////////////////////////////////////////////////////////
/*
     FILE: MHS.java
  PURPOSE: Implements the NOAA MHS radiometer.
   AUTHOR: Peter Hollemans
     DATE: 2007/11/23
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
 * The <code>MHS</code> is a radiometer for the NOAA Microwave Humidity
 * Sounder.
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
