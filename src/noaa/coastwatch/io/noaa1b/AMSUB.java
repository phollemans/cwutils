////////////////////////////////////////////////////////////////////////
/*
     FILE: AMSUB.java
  PURPOSE: Implements the NOAA AMSU-B radiometer.
   AUTHOR: Peter Hollemans
     DATE: 2007/11/22
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
 * The <code>AMSUB</code> is a radiometer for the NOAA Advanced
 * Microwave Sounding Unit-B.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class AMSUB implements Radiometer {

  // Constants
  // ---------

  /** The number of channels for this sensor. */
  public static final int CHANNELS = 5;

  /** The number of sample per scan line for this sensor. */
  public static final int SAMPLES = 90;

  // Variables
  // ---------

  /** The singleton instance of this class. */
  private static AMSUB instance = new AMSUB();

  ////////////////////////////////////////////////////////////

  private AMSUB () {}

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static AMSUB getInstance() { return (instance); }

  ////////////////////////////////////////////////////////////

  public String getName() { return ("AMSU-B"); }

  ////////////////////////////////////////////////////////////

  public String getLongName() { 

    return ("Advanced Microwave Sounding Unit-B");

  } // getLongName

  ////////////////////////////////////////////////////////////

  public int getChannelCount() { return (CHANNELS); }

  ////////////////////////////////////////////////////////////

  public String getChannelName (int channel) {
    
    if (channel < 1 || channel > CHANNELS) 
      throw new IllegalArgumentException();
    return ("amsub_ch" + (channel+15));

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

} // AMSUB class

////////////////////////////////////////////////////////////////////////
