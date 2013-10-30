////////////////////////////////////////////////////////////////////////
/*
     FILE: Datum.java
  PURPOSE: Handles geodetic datum transformations.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/17
  CHANGES: 2006/05/26, PFH, modified to implement SpheroidConstants
           2006/05/28, PFH, added various get methods
           2010/03/03, PFH, added getDatumName()

  CoastWatch Software Library and Utilities
  Copyright 1998-2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import noaa.coastwatch.util.*;

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
   * Transforms an Earth location from one datum to another.
   *
   * @param from the Earth location to transform.
   * @param toDatum the new datum to transform to.
   * @param to the Earth location to modify, or null to create a new
   * transformed Earth location.  The <code>to</code> and
   * <code>from</code> locations may be the same, in which case the
   * location is modified in-place.
   *
   * @return the new or modified Earth location.
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
