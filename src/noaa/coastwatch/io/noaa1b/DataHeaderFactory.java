////////////////////////////////////////////////////////////////////////
/*

     File: DataHeaderFactory.java
   Author: Peter Hollemans
     Date: 2007/08/27

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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import noaa.coastwatch.io.noaa1b.AMSUAHeader;
import noaa.coastwatch.io.noaa1b.AMSUBHeader;
import noaa.coastwatch.io.noaa1b.DataHeader;
import noaa.coastwatch.io.noaa1b.HIRS4Header;
import noaa.coastwatch.io.noaa1b.MHSHeader;
import noaa.coastwatch.io.noaa1b.NOAA1bServices;

/**
 * The <code>DataHeaderFactory</code> creates data header
 * objects using the byte data at the beginning of a NOAA 1b file.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class DataHeaderFactory {

  ////////////////////////////////////////////////////////////
  
  /** 
   * Creates a new data header.
   *
   * @param channel the channel of byte data to read.
   * @param isByteSwapped the byte swapped flag, true if the data
   * is in little endian byte order.
   *
   * @return a data header object, or null of no header could be
   * found that is appropriate for the data.
   *
   * @throws IOException if an error occurred reading the byte
   * data.
   */
  public static DataHeader create (
    ByteChannel channel,
    boolean isByteSwapped
  ) throws IOException {

    // Read data
    // ---------
    ByteBuffer inputBuffer = ByteBuffer.allocate (22528);
    if (isByteSwapped) inputBuffer.order (ByteOrder.LITTLE_ENDIAN);
    channel.read (inputBuffer);
    
    // Try version 1 style header
    // --------------------------
    int craftCode = NOAA1bServices.getUByte (inputBuffer, 0);
    boolean isValidCraft = (craftCode >= 1 && craftCode <= 8);
    int typeCode = NOAA1bServices.getUByte (inputBuffer, 1) >>> 4;
    boolean isValidType = (typeCode >= 1 && typeCode <= 9);
    if (isValidCraft && isValidType) {
      switch (typeCode) {

      case 1: // LAC
        throw new IOException ("NOAA 1b version 1 LAC not implemented");

      case 2: // GAC
        throw new IOException ("NOAA 1b version 1 GAC not implemented");

      case 3: // HRPT
        throw new IOException ("NOAA 1b version 1 HRPT not implemented");

      case 4: // TIP
        throw new IOException ("NOAA 1b version 1 TIP not implemented");

      case 5: // HIRS
        throw new IOException ("NOAA 1b version 1 HIRS not implemented");

      case 6: // MSU
        throw new IOException ("NOAA 1b version 1 MSU not implemented");

      case 7: // SSU
        throw new IOException ("NOAA 1b version 1 SSU not implemented");

      case 8: // DCS
        throw new IOException ("NOAA 1b version 1 DCS not implemented");

      case 9: // SEM
        throw new IOException ("NOAA 1b version 1 SEM not implemented");

      } // switch
    } // if

    // Try version 2 style header
    // --------------------------
    craftCode = NOAA1bServices.getUShort (inputBuffer, 72);
    isValidCraft = (
      craftCode == 2 || 
      craftCode == 4 || 
      craftCode == 6 ||
      craftCode == 7 ||
      craftCode == 8 ||
      craftCode == 11 ||
      craftCode == 12
    );
    typeCode = NOAA1bServices.getUShort (inputBuffer, 76);
    isValidType = (typeCode >= 1 && typeCode <= 13);
    if (isValidCraft && isValidType) {
      switch (typeCode) {

      case 1: // LAC
        throw new IOException ("NOAA 1b version 2+ LAC not implemented");

      case 2: // GAC
        throw new IOException ("NOAA 1b version 2+ GAC not implemented");

      case 3: // HRPT
        throw new IOException ("NOAA 1b version 2+ HRPT not implemented");

      case 4: // TIP
        throw new IOException ("NOAA 1b version 2+ TIP not implemented");

      case 5: // HIRS
        return (new HIRS4Header (inputBuffer));

      case 6: // MSU
        throw new IOException ("NOAA 1b version 2+ MSU not implemented");

      case 7: // SSU
        throw new IOException ("NOAA 1b version 2+ SSU not implemented");

      case 8: // DCS
        throw new IOException ("NOAA 1b version 2+ DCS not implemented");

      case 9: // SEM
        throw new IOException ("NOAA 1b version 2+ SEM not implemented");

      case 10: // AMSU-A
        return (new AMSUAHeader (inputBuffer));

      case 11: // AMSU-B
        return (new AMSUBHeader (inputBuffer));

      case 12: // MHS
        return (new MHSHeader (inputBuffer));

      case 13: // FRAC
        throw new IOException ("NOAA 1b version 2+ FRAC not implemented");

      } // switch
    } // if

    throw new IOException ("Cannot determine data type");

  } // create

  ////////////////////////////////////////////////////////////

} // DataHeaderFactory class

////////////////////////////////////////////////////////////////////////
