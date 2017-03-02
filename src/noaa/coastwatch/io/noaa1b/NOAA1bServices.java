////////////////////////////////////////////////////////////////////////
/*

     File: NOAA1bServices.java
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
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * The <code>NOAA1bServices</code> class performs various NOAA 1b
 * file related functions.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class NOAA1bServices {
  
  ////////////////////////////////////////////////////////////

  /** Determines if a byte is a valid printable ASCII character. */
  public static boolean isPrint (
    byte b
  ) {

    if (b < 32 || b > 127) return (false);
    else return (true);

  } // isPrint

  ////////////////////////////////////////////////////////////

  /** Gets a byte array from the specified byte buffer. */
  public static byte[] getBytes (
    ByteBuffer data,
    int offset,
    int length
  ) {

    byte[] array = new byte[length];
    data.position (offset);
    data.get (array);
    return (array);

  } // getBytes

  ////////////////////////////////////////////////////////////

  /** Gets an unsigned 8-bit value from the specified byte buffer. */
  public static short getUByte (
    ByteBuffer data,
    int offset
  ) {
 
    return ((short) (data.get (offset) & 0xff));

  } // getUByte

  ////////////////////////////////////////////////////////////

  /** Gets an unsigned 16-bit value from the specified byte buffer. */
  public static int getUShort (
    ByteBuffer data,
    int offset
  ) {
 
    return (data.getShort (offset) & 0xffff);

  } // getUShort

  ////////////////////////////////////////////////////////////

  /** Gets a signed 16-bit value from the specified byte buffer. */
  public static short getShort (
    ByteBuffer data,
    int offset
  ) {
 
    return (data.getShort (offset));

  } // getShort

  ////////////////////////////////////////////////////////////

  /** Gets an unsigned 32-bit value from the specified byte buffer. */
  public static long getUInt (
    ByteBuffer data,
    int offset
  ) {
 
    return (data.getInt (offset) & 0xffffffffL);

  } // getUInt

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an n-bit value from the specified byte buffer.
   *
   * @param data the byte buffer to extract data from.
   * @param startOffset the starting offset into the byte buffer to count
   * n-bit data values from.
   * @param valueOffset the number of n-bit values into the array to
   * extract an integer from.
   * @param bits the number of bits in each value in the range [1..64].
   */
  public static long getNBit (
    ByteBuffer data,
    int startOffset,
    int valueOffset,
    int bits
  ) {
 
    // Get the starting byte and bit for the n-bit value
    // -------------------------------------------------
    int startByte = (valueOffset*bits) / 8;
    int startBit = (valueOffset*bits) - startByte*8;

    // Initialize the value
    // --------------------
    long value = 0L;
    
    // Loop over each bit and copy bit value into final value
    // ------------------------------------------------------
    int currentByte = startOffset + startByte;
    int currentBit = startBit;
    for (int i = 0; i < bits; i++) {
      byte mask = (byte) (0x80 >> currentBit);
      if ((data.get (currentByte) & mask) != 0)
        value = value | (0x01 << (bits-1-i));
      currentBit++;
      if (currentBit > 7) {
        currentBit = 0;
        currentByte++;
      } // if
    } // for

    return (value);

  } // getNBit

  ////////////////////////////////////////////////////////////

  /** Gets a signed 32-bit value from the specified byte buffer. */
  public static int getInt (
    ByteBuffer data,
    int offset
  ) {
 
    return (data.getInt (offset));

  } // getInt

  ////////////////////////////////////////////////////////////

  /** 
   * Converts time in year, day, and milliseconds UTC to a Java
   * date.
   * 
   * @param year the four digit year.
   * @param day the day of the year starting at 1.
   * @param millisecond the millisecond of the day starting at
   * 00:00 UTC.
   *
   * @return a date object for the specified data.
   */
  public static Date convertDate (
    int year,
    int day,
    long millisecond
  ) {

    Calendar cal = new GregorianCalendar (year, 0, 0, 0, 0, 0);
    cal.set (Calendar.DAY_OF_YEAR, day);
    cal.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
    long time = cal.getTimeInMillis() + millisecond;

    return (new Date (time));

  } // convertDate

  ////////////////////////////////////////////////////////////

} // NOAA1bServices class

////////////////////////////////////////////////////////////////////////
