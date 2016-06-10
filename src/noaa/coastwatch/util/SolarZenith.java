////////////////////////////////////////////////////////////////////////
/*
     FILE: SolarZenith.java
  PURPOSE: A class to calculate solar zenith angles.
   AUTHOR: Peter Hollemans
     DATE: 2003/01/15
  CHANGES: 2005/05/19, PFH, modified for datum shifting
           2006/05/26, PFH, modified to use SpheroidConstants

  CoastWatch Software Library and Utilities
  Copyright 1998-2003, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.DatumFactory;
import noaa.coastwatch.util.trans.SpheroidConstants;

/**
 * The solar zenith class may be used to calculate solar zenith angles
 * for any earth location at a given date and time.  The solar zenith
 * angle is the angle between two vectors: one vector normal to the
 * Earth's surface and the other pointing to the sun:
 * <pre>
 *           ^       -O-
 *           |       /|\ 
 *           |      _  
 *           |  sz  /|
 *           |--_  /
 *           |   \/
 *           |   /
 *           |  /
 *         --| /
 *        |  |/
 *   ======================
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class SolarZenith {

  // Constants
  // ---------

  /** The spheroid datum. */
  private static final Datum SPHERE = 
    DatumFactory.create (SpheroidConstants.SPHERE);

  // Variables
  // ---------

  /** The solar declination. */
  private double solarDec;

  /** The time in hours since 00:00 GMT. */
  private double tGmt;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new solar zenith object using the specified date.
   *
   * @param date the date.
   */
  public SolarZenith (
    Date date
  ) {

    // Calculate Julian day
    // --------------------
    Calendar cal = Calendar.getInstance();
    cal.setTime (date);
    cal.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
    int julianDay = cal.get (Calendar.DAY_OF_YEAR);

    // Calculate solar declination
    // ---------------------------
    double e = 2 * Math.PI * julianDay / 365;
    solarDec = 0.006918 - 0.399912*Math.cos (e) + 0.070257*Math.sin (e)
      - 0.006758*Math.cos (2*e) + 0.000907*Math.sin (2*e);    

    // Calculate hours since midnight
    // ------------------------------
    int hour = cal.get (Calendar.HOUR_OF_DAY);
    int minute = cal.get (Calendar.MINUTE);
    int second = cal.get (Calendar.SECOND);
    tGmt = (double) hour + minute/60.0 + second/3600.0;

  } // SolarZenith constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the solar zenith angle for a specified earth location.  
   * 
   * @param loc the earth location for the solar zenith calculation.
   *
   * @return the solar zenith angle in degrees.
   */
  public double getSolarZenith (
    EarthLocation loc
  ) {

    // Convert location to sphere
    // --------------------------
    if (!loc.getDatum().equals (SPHERE)) {
      loc = (EarthLocation) loc.clone();
      loc.shiftDatum (SPHERE);
    } // if

    // Calculate angle
    // ---------------
    double th = ((tGmt + loc.lon/15) - 12.0) * 15 * (2*Math.PI/360);
    double lat = loc.lat * Math.PI/180;
    double sz = Math.acos ( Math.sin (solarDec)*Math.sin (lat)
      + Math.cos (solarDec)*Math.cos (lat)*Math.cos (th) );
    return (Math.toDegrees (sz));

  } // getSolarZenith

  ////////////////////////////////////////////////////////////

  /**
   * Gets the position of the solar terminator for this data.  The
   * solar terminator is the point at which the solar zenith angle is
   * zero (the day to night boundary).
   *
   * @param lat the geocentric latitude of the desired terminator position.
   * @param positive the positive or negative solution flag.  The
   * solar terminator has two points at every latitude.  The solution
   * flag selects one of the two points.
   *
   * @return the earth location of the terminator.
   */   
  public EarthLocation getTerminator (
    double lat,
    boolean positive
  ) {

    // Calculate position
    // ------------------
    double th = Math.acos (- Math.tan (solarDec)*Math.tan (
      Math.toRadians (lat)));
    if (!positive) th *= -1;
    double tLocal = th * (1/15.0) * (180/Math.PI) + 12.0;
    double lon = 15*(tLocal - tGmt);
    return (new EarthLocation (lat, lon, SPHERE));

  } // getTerminator

  ////////////////////////////////////////////////////////////

} // SolarZenith class

////////////////////////////////////////////////////////////////////////
