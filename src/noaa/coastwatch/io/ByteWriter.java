////////////////////////////////////////////////////////////////////////
/*

     File: ByteWriter.java
   Author: Mark Robinson
     Date: 2002/07/18

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
   * Creates a new binary file from the specified earth data info
   * and file name.
   *
   * @param info the earth data info object.
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
