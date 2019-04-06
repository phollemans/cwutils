////////////////////////////////////////////////////////////////////////
/*

     File: Datum.java
   Author: Peter Hollemans
     Date: 2005/05/17

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.trans;

// Imports
// -------
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.SpheroidConstants;

/**
 * A <code>Datum</code> holds a geodetic datum name, spheroid name,
 * spheroid parameters, and WGS84-relative datum transformation
 * parameters.  The datum may be used to transform
 * <code>EarthLocation</code> objects to and from different datums.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class Datum 
  implements SpheroidConstants {

  // Constants
  // ---------

  /** The coordinate axis indices for ECF coordinates. */
  private static final int X = 0;
  private static final int Y = 1;
  private static final int Z = 2;

  // Variables
  // ---------

  /** The name of the datum. */
  private String datumName;

  /** The name of the spheroid. */
  private String spheroidName;

  /** The semi-major axis (meters). */
  private double axis;

  /** The flattening, f = (a-b)/a. */
  private double flat;

  /** The eccentricity squared, e2 = 2f-f^2. */
  private double e2;

  /** The x-axis shift (meters). */
  private double dx;

  /** The y-axis shift (meters). */
  private double dy;

  /** The z-axis shift (meters). */
  private double dz;

  /** The values needed for ECF computations. */
  private double rp, rp2, re, re2_over_rp2;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new geodetic datum based on user-specified spheroid
   * parameters.
   *
   * @param datumName the datum name.
   * @param spheroidName the spheroid name.
   * @param axis the spheroid semi-major axis (meters).
   * @param invFlat the spheroid inverse flattening.
   * @param dx the x-axis shift (meters).
   * @param dy the y-axis shift (meters).
   * @param dz the z-axis shift (meters).
   */
  public Datum (
    String datumName,
    String spheroidName,
    double axis,
    double invFlat,
    double dx,
    double dy,
    double dz
  ) {

    this.datumName = datumName;
    this.spheroidName = spheroidName;
    this.axis = axis;
    flat = 1.0/invFlat;
    e2 = 2*flat - flat*flat;
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;

    re = axis;
    rp = re*(1-flat);
    rp2 = rp*rp;
    re2_over_rp2 = (re*re)/rp2;

  } // Datum constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new geodetic datum based on a spheroid code.
   *
   * @param datumName the datum name.
   * @param spheroid the spheroid code for the datum.
   * @param dx the x-axis shift (meters).
   * @param dy the y-axis shift (meters).
   * @param dz the z-axis shift (meters).
   */
  public Datum (
    String datumName,
    int spheroid,
    double dx,
    double dy,
    double dz
  ) {

    this.datumName = datumName;
    this.spheroidName = SPHEROID_NAMES[spheroid];
    axis = SPHEROID_SEMI_MAJOR[spheroid];
    flat = 1.0/SPHEROID_INV_FLAT[spheroid];
    e2 = 2*flat - flat*flat;
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;

    re = axis;
    rp = re*(1-flat);
    rp2 = rp*rp;
    re2_over_rp2 = (re*re)/rp2;

  } // Datum constructor

  ////////////////////////////////////////////////////////////

  /** Gets the semi-major axis in meters. */
  public double getAxis () { return (axis); }

  ////////////////////////////////////////////////////////////

  /** Gets the flattening value. */
  public double getFlat () { return (flat); }

  ////////////////////////////////////////////////////////////

  /** Gets the eccentricity value squared. */
  public double getE2 () { return (e2); }

  ////////////////////////////////////////////////////////////

  /** Gets the datum name. */
  public String getDatumName() { return (datumName); }

  ////////////////////////////////////////////////////////////

  /**
   * Transforms an earth location from one datum to another.
   *
   * @param from the earth location to transform.
   * @param toDatum the new datum to transform to.
   * @param to the earth location to modify, or null to create a new
   * transformed earth location.  The <code>to</code> and
   * <code>from</code> locations may be the same, in which case the
   * location is modified in-place.
   *
   * @return the new or modified earth location.  The returned location will
   * have its datum set to the new datum.
   */
  public static EarthLocation transform (
    EarthLocation from,
    Datum toDatum,
    EarthLocation to
  ) {

    // Setup input values
    // ------------------
    Datum fromDatum = from.getDatum();
    double from_a = fromDatum.axis;
    double from_f = fromDatum.flat;
    double from_esq = fromDatum.e2;
    double da = toDatum.axis - fromDatum.axis;
    double df = toDatum.flat - fromDatum.flat;
    double dx = fromDatum.dx - toDatum.dx;
    double dy = fromDatum.dy - toDatum.dy;
    double dz = fromDatum.dz - toDatum.dz;

    // Compute lat/lon difference
    // --------------------------
    double slat = Math.sin (Math.toRadians (from.lat));
    double clat = Math.cos (Math.toRadians (from.lat));
    double slon = Math.sin (Math.toRadians (from.lon));
    double clon = Math.cos (Math.toRadians (from.lon));
    double ssqlat = slat * slat;
    double adb = 1.0 / (1.0 - from_f);
    double rn = from_a / Math.sqrt (1.0 - from_esq*ssqlat);
    double rm = from_a * (1.0 - from_esq) / Math.pow ((1.0 - from_esq*ssqlat),
      1.5);
    double dlat = (((((-dx*slat*clon - dy*slat*slon) + dz*clat)
      + (da * ((rn*from_esq*slat*clat) / from_a)))
      + (df * (rm*adb + rn/adb)*slat*clat))) / rm;
    double dlon = (-dx*slon + dy*clon) / (rn*clat);

    // Return new location
    // -------------------
    if (to == null) to = new EarthLocation();
    to.lat = from.lat + Math.toDegrees (dlat);
    to.lon = from.lon + Math.toDegrees (dlon);
    to.setDatum (toDatum);
    return (to);

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Converts an earth location to Earth-Centered-Fixed (ECF).  This
   * is a right-handed cartesian coordinate system centered at the center
   * of the Earth ellipsoid, with its x-axis pointing towards (0N, 0E), its
   * y-axis pointing towards (0N, 90E), and its z-axis pointing at the
   * north pole.
   *
   * @param lat the latitude in the range [-90..90].
   * @param lon the longitude in the range [-180..180].
   * @param ecf the output ECF coordinates in meters.
   *
   * @since 3.5.0
   */
  public void computeECF (
    double lat,
    double lon,
    double[] ecf
  ) {

    lat = Math.toRadians (lat);
    lon = Math.toRadians (lon);
    double cosLat = Math.cos (lat);
    double sinLat = Math.sin (lat);
    double cos2Lat = cosLat*cosLat;
    double sin2Lat = sinLat*sinLat;
    double beta = rp*Math.sqrt (re2_over_rp2 * cos2Lat + sin2Lat);
    double cosLon = Math.cos (lon);
    double sinLon = Math.sin (lon);
    ecf[X] = (re * cosLat * cosLon)/beta;
    ecf[Y] = (re * cosLat * sinLon)/beta;
    ecf[Z] = (rp2 * sinLat)/(re*beta);

  } // computeECF

  ////////////////////////////////////////////////////////////

  /**
   * Checks if this datum is equal to another.  For now, we assume
   * that datums all come from one place, in such a way as we can
   * compare object references for equality and be assured that
   * different references imply different datums.
   */
  public boolean equals (
    Object obj
  ) {

    return (this == obj);

  } // equals

  ////////////////////////////////////////////////////////////

  public String toString () {

    return ("Datum[datumName=" + datumName + ",spheroidName=" + 
      spheroidName + ",dx=" + dx + ",dy=" + dy + ",dz=" + dz + "]");

  } // toString

  ////////////////////////////////////////////////////////////

  public Object clone () {

    try {
      return (super.clone());
    } // try
    catch (CloneNotSupportedException e) {
      return (null);
    } // catch

  } // clone

  ////////////////////////////////////////////////////////////

} // Datum class

////////////////////////////////////////////////////////////////////////
