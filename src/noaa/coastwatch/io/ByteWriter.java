////////////////////////////////////////////////////////////////////////
/*
     FILE: ByteWriter.java
  PURPOSE: A class to write binary format files as a stream of 
           8-bit unsigned bytes.
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
 * A byte writer is a binary writer that writes data as a stream of
 * 8-bit unsigned bytes.
 *
 * @author Mark Robinson
 * @since 3.1.0
 */
public class ByteWriter
  extends BinaryWriter {

  ////////////////////////////////////////////////////////////

  public byte[] convertValue (
    Number value
  ) {

    byte byteValue = (byte) Math.round (value.doubleValue());
    return (new byte[] {byteValue});

  } // convertValue

  ////////////////////////////////////////////////////////////

  public double getTypeMin () { return (0.0); }

  ////////////////////////////////////////////////////////////

  public double getTypeMax () { return (255.0); }

  ////////////////////////////////////////////////////////////

  public double getTypeRange () { return (255.0); }

  ////////////////////////////////////////////////////////////

  public Number getDefaultMissing () { return (new Byte ((byte)0)); }

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
  public ByteWriter (
    EarthDataInfo info,
    String file
  ) throws IOException {

    super (info, file);

  } // ByteWriter constructor

  ////////////////////////////////////////////////////////////

} // ByteWriter class

////////////////////////////////////////////////////////////////////////
