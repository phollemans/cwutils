////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthLocation.java
  PURPOSE: To define an Earth location point and operations.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/25
  CHANGES: 2002/09/04, PFH, added isValid
           2002/09/09, PFH, added lonRange
           2002/09/11, PFH, added pole adjustments in translate
           2002/09/13, PFH, added lon range adjustment in constructor
           2002/09/19, PFH, added static format
           2002/12/04, PFH, added Cloneable interface
           2002/12/05, PFH, modified format for 2 decimals
           2002/01/15, PFH, added latRange, render
           2004/03/28, PFH, added additional format() method
           2004/09/20, PFH, added DDDD format style code
           2004/10/13, PFH, added equals()
           2005/02/07, PFH, added default format styles
           2005/05/16, PFH, added datum handling
           2005/05/18, PFH, modified clone() to call super.clone()
           2006/05/26, PFH, modified to use SpheroidConstants

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;
import java.text.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.trans.*;

/**
 * An Earth location represents a point using latitude and longitude
 * in degrees.  Unless otherwise specified, the latitude and longitude
 * are geodetic relative to the WGS84 datum.
 */
public class EarthLocation
  implements Cloneable {

  // Constants
  // ---------

  /** Format style code for integer degrees: '124 N'. */
  public final static int D = 0;

  /** Format style code for 2-digit degrees: '124.36 N'. */
  public final static int DD = 1;

  /** Format style code for 4-digit degrees: '124.3600 N'. */
  public final static int DDDD = 4;

  /** Format style code for degrees, minutes: '124 21.60 N'. */
  public final static int DDMM = 2;

  /** Format style code for degrees, minutes, seconds: '124 21 36.00 N'. */
  public final static int DDMMSS = 3;

  /** Selection code for latitude. */
  public final static int LAT = 0;

  /** Selection code for longitude. */
  public final static int LON = 1;

  /** The instance of WGS84. */
  private static final Datum WGS84 = 
    DatumFactory.create (SpheroidConstants.WGS84);

  // Variables
  // ---------
  /** 
   * The Earth location latitude in degrees.  Latitudes have the range
   * [-90..90]. 
   */
  public double lat;

  /** 
   * The Earth location longitude in degrees.  Longitudes have the range
   * [-180..180). 
   */
  public double lon;

  /** The default location format style. */
  private static int formatStyle = DDDD;

  /** The geodetic datum. */
  private Datum datum = WGS84;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the location geodetic datum.  No datum shift is performed.
   * 
   * @param newDatum the location geodetic datum.
   *
   * @see #shiftDatum
   */
  public void setDatum (Datum newDatum) { datum = newDatum; }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the location geodetic datum.
   *
   * @return the geodetic datum.
   */
  public Datum getDatum () { return (datum); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the location geodetic datum and shifts the latitude and
   * longitude coordinates so that they reflect the newly specified
   * datum.
   *
   * @param newDatum the new datum to convert to.
   */
  public void shiftDatum (
    Datum newDatum
  ) {

    Datum.transform (this, newDatum, this);

  } // shiftDatum

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the default format style used by location formatting
   * methods.  The default format style is DDDD.
   *
   * @param style the location format style to use.
   */
  public static void setFormatStyle (
    int style
  ) {

    formatStyle = style;

  } // setFormatStyle

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the default format style used by location formatting
   * routines.
   *
   * @return the current default location format style.
   *
   * @see #setFormatStyle
   */
  public static int getFormatStyle () { return (formatStyle); }

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new Earth location at the origin (0,0).  The datum
   * defaults to WGS84.
   */
  public EarthLocation () {

    this.lat = 0;
    this.lon = 0;

  } // EarthLocation constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new Earth location from the specified parameters.
   * If needed, the longitude value is adjusted to be in the range
   * [-180..180).  The datum defaults to WGS84.
   *
   * @param lat the latitude in degrees.
   * @param lon the longitude in degrees.  
   */
  public EarthLocation (
    double lat,
    double lon
  ) {

    this.lat = lat;
    this.lon = lonRange(lon);

  } // EarthLocation constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new Earth location at the origin (0,0) with the
   * specified datum.
   *
   * @param datum the geodetic datum.
   */
  public EarthLocation (
    Datum datum
  ) {

    this.lat = 0;
    this.lon = 0;
    this.datum = datum;

  } // EarthLocation constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new Earth location from the specified parameters.
   * If needed, the longitude value is adjusted to be in the range
   * [-180..180).  The datum defaults to WGS84.
   *
   * @param lat the latitude in degrees.
   * @param lon the longitude in degrees.  
   * @param datum the geodetic datum.
   */
  public EarthLocation (
    double lat,
    double lon,
    Datum datum
  ) {

    this.lat = lat;
    this.lon = lonRange(lon);
    this.datum = datum;

  } // EarthLocation constructor

  ////////////////////////////////////////////////////////////

  /** Gets the coordinates as [lat, lon]. */
  public double[] getCoords () { return (new double[] {lat, lon}); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the latitude and longitude coordinate values.  If needed,
   * the longitude value is adjusted to be in the range [-180..180).
   *
   * @param lat the latitude in degrees.
   * @param lon the longitude in degrees.  
   */
  public void setCoords (
    double lat,
    double lon
  ) {
    
    this.lat = lat;
    this.lon = lonRange(lon);

  } // setCoords

  ////////////////////////////////////////////////////////////

  /**
   * Calculates the great circle distance from this location to another.
   * The method uses a calculation for distance on a sphere.<p>
   *
   * Haversine Formula (from R.W. Sinnott, "Virtues of the Haversine",
   * Sky and Telescope, vol. 68, no. 2, 1984, p. 159):<p>
   *   
   * <pre>
   *   dlon = lon2 - lon1
   *   dlat = lat2 - lat1
   *   a = sin^2(dlat/2) + cos(lat1) * cos(lat2) * sin^2(dlon/2)
   *   c = 2 * arcsin(min(1,sqrt(a)))
   *   d = R * c
   * </pre>
   *
   * @param loc the location for which to calculate the distance.
   * 
   * @return the distance between points in kilometres.
   */
  public double distance (
    EarthLocation loc
  ) {

    // Convert to radians
    // ------------------
    double lat1 = Math.toRadians (this.lat);
    double lon1 = Math.toRadians (this.lon);
    double lat2 = Math.toRadians (loc.lat);
    double lon2 = Math.toRadians (loc.lon);

    // Calculate distance
    // ------------------
    double dlon = lon2 - lon1;
    double dlat = lat2 - lat1;
    double a = Math.pow (Math.sin (dlat/2), 2) + 
      Math.cos (lat1) * Math.cos (lat2) * Math.pow (Math.sin (dlon/2), 2);
    double c = 2 * Math.asin (Math.min (1, Math.sqrt (a)));
    double d = SpheroidConstants.STD_RADIUS * c;
    return (d);

  } // distance

  ////////////////////////////////////////////////////////////

  /**
   * Translates an Earth location by the specified increments.  If the
   * translation causes the latitude to be outside the range
   * [-90..90], the latitude and longitude are adjusted to travel over
   * the pole and down the other side.  If the translation causes the
   * longitude to be outside the range [-180..180), the longitude is
   * adjusted accordingly.
   * 
   * @param latInc the latitude increment.
   * @param lonInc the longitude increment.
   *
   * @return the translated location.
   */
  public EarthLocation translate (
    double latInc,
    double lonInc
  ) {

    EarthLocation loc = (EarthLocation) this.clone();
    loc.lat += latInc;
    if (loc.lat > 90) { loc.lat = 180 - loc.lat; loc.lon += 180; }
    else if (loc.lat < -90) { loc.lat = -180 - loc.lat; loc.lon += 180; }
    loc.lon = lonRange (loc.lon + lonInc);
    return (loc);

  } // translate

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if this location is north of the specified location.
   */
  public boolean isNorth (
    EarthLocation loc
  ) {

    return (this.lat > loc.lat);

  } // isNorth

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if this location is south of the specified location.
   */
  public boolean isSouth (
    EarthLocation loc
  ) {

    return (this.lat < loc.lat);

  } // isSouth

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if this location is east of the specified location. 
   */
  public boolean isEast (
    EarthLocation loc
  ) {

    double abs = Math.abs (this.lon - loc.lon);
    if (abs < 180) return (this.lon > loc.lon);
    else if (abs > 180) return (this.lon < loc.lon);
    else return (false);

  } // isEast

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if this location is west of the specified location. 
   */
  public boolean isWest (
    EarthLocation loc
  ) {

    double abs = Math.abs (this.lon - loc.lon);
    if (abs < 180) return (this.lon < loc.lon);
    else if (abs > 180) return (this.lon > loc.lon);
    else return (false);

  } // isWest

  ////////////////////////////////////////////////////////////

  /**
   * Formats a single coordinate from an Earth location to a string.
   *
   * @param deg the latitude or longitude value in degrees.
   * @param style the format style to use.
   * @param select the coordinate selection.  Use <code>LAT</code> for
   * latitude and <code>LON</code> for longitude.
   *
   * @return the formatted coordinate value.
   */
  public static String formatSingle (
    double deg,
    int style,
    int select
  ) {

    // Get hemisphere
    // --------------
    String hemisphere;
    if (select == LAT) hemisphere = (deg < 0 ? "S" : "N");
    else hemisphere = (deg < 0 ? "W" : "E");

    // Format value
    // ------------
    String str = null;
    deg = Math.abs (deg);
    DecimalFormat fmt = new DecimalFormat ("0.00");
    switch (style) {
    case D:
      str = ((int) deg) + " " + hemisphere;
      break;
    case DD:
      str = fmt.format(deg) + " " + hemisphere;
      break;
    case DDDD:
      str = new DecimalFormat ("0.0000").format (deg) + " " + hemisphere;
      break;
    case DDMM: {
      int dd = (int) deg;
      double mm = (deg - dd) * 60;
      str = dd + " " + fmt.format(mm) + " " + hemisphere;
      break; 
    } // case
    case DDMMSS: {
      int dd = (int) deg;
      int mm = (int) ((deg - dd)*60);
      double ss = (deg - dd - mm/60.0)*3600;
      str = dd + "d" + mm + "'" + fmt.format(ss) + "\"" + hemisphere;
      break;
    } // case
    default:
      break;
    } // switch 

    return (str);

  } // formatSingle

  ////////////////////////////////////////////////////////////

  /**
   * Formats a single coordinate from this Earth location to a string.
   *
   * @param style the format style to use.
   * @param select the coordinate selection.  Use <code>LAT</code> for
   * latitude and <code>LON</code> for longitude.
   *
   * @return the formatted coordinate value.
   */
  public String formatSingle (
    int style,
    int select
  ) {

    return (formatSingle ((select == LAT ? lat : lon), style, select));

  } // formatSingle

  ////////////////////////////////////////////////////////////

  /**
   * Formats a single coordinate from this Earth location coordinate
   * to a string using the default format style.
   *
   * @param select the coordinate selection.  Use <code>LAT</code> for
   * latitude and <code>LON</code> for longitude.
   *
   * @return the formatted coordinate value.
   */
  public String formatSingle (
    int select
  ) {

    return (formatSingle ((select == LAT ? lat : lon), formatStyle, select));

  } // formatSingle

  ////////////////////////////////////////////////////////////

  /**
   * Formats this Earth location to a string.  Both latitude and
   * longitude are formatted.
   *
   * @param style the format style to use.
   *
   * @return the formatted coordinate values.
   */
  public String format (
    int style
  ) {

    return (formatSingle (style, LAT) + ", " + formatSingle (style, LON));

  } // format

  ////////////////////////////////////////////////////////////

  /**
   * Formats this Earth location to a string using the default format
   * style.  Both latitude and longitude are formatted.
   *
   * @return the formatted coordinate values.
   */
  public String format () {

    return (formatSingle (formatStyle, LAT) + ", " + 
      formatSingle (formatStyle, LON));

  } // format

  ////////////////////////////////////////////////////////////

  public Object clone () {

    try {
      EarthLocation loc = (EarthLocation) super.clone();
      return (loc);
    } // try
    catch (CloneNotSupportedException e) {
      return (null);
    } // catch

  } // clone

  ////////////////////////////////////////////////////////////

  public String toString () {

    return ("EarthLocation[lat=" + lat + ",lon=" + lon + ",datum=" + datum + 
      "]");

  } // toString

  ////////////////////////////////////////////////////////////

  /** Checks if this Earth location is valid. */
  public boolean isValid () {

    if (Double.isNaN (lat)) return (false);
    if (Double.isNaN (lon)) return (false);
    return (true);    

  } // isValid

  ////////////////////////////////////////////////////////////

  /** Returns a latitude in the range [-90..90]. */
  public static double latRange (
    double lat
  ) {

    if (lat > 90) return (90);
    else if (lat < -90) return (-90);
    else return (lat);

  } // latRange

  ////////////////////////////////////////////////////////////

  /** Returns a longitude value in the range [-180..180). */
  public static double lonRange (
    double lon
  ) {

    if (lon < -180) return (lon + 360);
    else if (lon >= 180) return (lon - 360);
    else return (lon);

  } // lonRange

  ////////////////////////////////////////////////////////////

  /**
   * Renders this Earth location to a graphics context as a point.
   *
   * @param g the graphics context for drawing.
   * @param trans the Earth image transform for converting Earth
   * locations to image points.
   */
  public void render (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    // Create image point
    // ------------------
    Point2D point = trans.transform (this);
    if (point == null) return;
    int x = (int) Math.round (point.getX());
    int y = (int) Math.round (point.getY());

    // Render line
    // -----------
    g.drawLine (x, y, x, y);

  } // render

  ////////////////////////////////////////////////////////////

  /** Returns true if the specified location is identical to this one. */
  public boolean equals (Object obj) {

    if (!(obj instanceof EarthLocation)) return (false);
    EarthLocation loc = (EarthLocation) obj;
    return (this.lat == loc.lat && this.lon == loc.lon);

  } // equals

  ////////////////////////////////////////////////////////////

} // EarthLocation class  

////////////////////////////////////////////////////////////////////////
