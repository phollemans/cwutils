////////////////////////////////////////////////////////////////////////
/*
     FILE: DateFormatter.java
  PURPOSE: A class to format dates.
   AUTHOR: Peter Hollemans
     DATE: 2004/07/02
  CHANGES: 2013/01/31, PFH, added parseDate method

  CoastWatch Software Library and Utilities
  Copyright 2004-2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import noaa.coastwatch.util.EarthLocation;

/**
 * The <code>DateFormatter</code> class contains a number of static
 * methods to format dates easily based on timezone and Earth
 * location.
 *
 * @author Peter Hollemans
 * @since 3.1.8
 */
public class DateFormatter {

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a string with the specified date formatting.  The date is
   * formatted as if it was in the GMT+0 timezone.
   *
   * @param date the date to format.
   * @param format a format string in the style of the Java
   * <code>SimpleDateFormat</code> class.
   *
   * @return a string with the date formatted.
   */ 
  public static String formatDate (
    Date date,
    String format
  ) {

    return (formatDate (date, format, TimeZone.getTimeZone ("GMT+0")));

  } // formatDate

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a string with the specified date formatting.  The date is
   * formatted as if it was in the specified time zone.
   *
   * @param date the date to format.
   * @param format a format string in the style of the Java
   * <code>SimpleDateFormat</code> class.
   * @param zone the time zone for formatting.
   *
   * @return a string with the date formatted.
   */ 
  public static String formatDate (
    Date date,
    String format,
    TimeZone zone
  ) {

    SimpleDateFormat dateFormat = new SimpleDateFormat (format);
    dateFormat.setTimeZone (zone);
    return (dateFormat.format (date));

  } // formatDate

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a string with the specified date formatting.  The date is
   * formatted as if it was in the time zone specified by the Earth
   * location longitude.
   *
   * @param date the date to format.
   * @param format a format string in the style of the Java
   * <code>SimpleDateFormat</code> class.
   * @param loc the earth location used to determine the appropriate
   * time zone.
   *
   * @return a string with the date formatted.
   */ 
  public static String formatDate (
    Date date,
    String format,
    EarthLocation loc
  ) {

    // Get time zone from longitude
    // ----------------------------
    double hours = (loc.lon/180) * 12;
    int hour = (int) Math.abs (Math.round (hours));
    String offset = (hours > 0 ? "+" : "-") + hour;
    TimeZone zone = TimeZone.getTimeZone ("GMT" + offset);

    // Get identified time zone from ID
    // --------------------------------
    /** 
     * Note: This doesn't really work very well, because different
     * latitudes in the same time zone use different conventions.  We
     * would need to account for latitude as well, for which there is
     * no easy conversion.
     */
//    String[] ids = TimeZone.getAvailableIDs (zone.getRawOffset());
//    zone = TimeZone.getTimeZone (ids[0]);

    // Format date with time zone
    // --------------------------
    return (formatDate (date, format, zone));

  } // formatDate

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a date from the specified string and format.
   *
   * @param input the input string to parse.
   * @param format a format string in the style of the Java
   * <code>SimpleDateFormat</code> class.
   *
   * @return a date object or null if the input string cannot be parsed.
   */ 
  public static Date parseDate (
    String input,
    String format
  ) throws ParseException {

    SimpleDateFormat dateFormat = new SimpleDateFormat (format);
    return (dateFormat.parse (input));

  } // parseDate

  ////////////////////////////////////////////////////////////

} // DateFormatter class

////////////////////////////////////////////////////////////////////////
