////////////////////////////////////////////////////////////////////////
/*
     FILE: ShortWriter.java
  PURPOSE: A class to write binary format files as a stream of 
           16-bit unsigned integers.
   AUTHOR: Mark Robinson
     DATE: 2002/07/18
  CHANGES: 2002/08/25, PFH, rearranged

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import noaa.coastwatch.io.BinaryWriter;
import noaa.coastwatch.util.EarthDataInfo;

/**
 * A short writer is a binary writer that writes data as a stream of
 * 16-bit signed integers.
 *
 * @author Mark Robinson
 * @since 3.1.0
 */
public class ShortWriter
  extends BinaryWriter {

  ////////////////////////////////////////////////////////////

  public byte[] convertValue (
    Number value
  ) {

    short shortValue = (short) Math.round (value.doubleValue());
    return (getBytes (shortValue));

  } // convertValue

  ////////////////////////////////////////////////////////////

  public double getTypeMin () { return (-32768.0); }

  ////////////////////////////////////////////////////////////

  public double getTypeMax () { return (32767.0); }

  ////////////////////////////////////////////////////////////

  public double getTypeRange () { return (65535.0); }

  ////////////////////////////////////////////////////////////

  public Number getDefaultMissing () { return (new Short ((short)-32768)); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new binary file from the specified Earth data info
   * and file name.
   *
   * @param info the Earth data info object.
   * @param file the new binary file name.
   *
   * @throws IOException if an error occurred opening the file.
   *
   * @see BinaryWriter#BinaryWriter
   */
  public ShortWriter (
    EarthDataInfo info,
    String file
  ) throws IOException {

    super (info, file);

  } // ShortWriter constructor

  ////////////////////////////////////////////////////////////

} // ShortWriter class

////////////////////////////////////////////////////////////////////////
