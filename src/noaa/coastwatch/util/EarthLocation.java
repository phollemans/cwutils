////////////////////////////////////////////////////////////////////////
/*

     File: EarthLocation.java
   Author: Peter Hollemans
     Date: 2002/07/25

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
package noaa.coastwatch.util;

// Imports
// -------
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.DatumFactory;
import noaa.coastwatch.util.trans.SpheroidConstants;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * An earth location represents a point using latitude and longitude
 * in degrees.  Unless otherwise specified, the latitude and longitude
 * are geodetic relative to the WGS84 datum.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
@noaa.coastwatch.test.Testable
public class EarthLocation
  implements Cloneable {

  // Constants
  // ---------

  /** Format style code for integer degrees: '124 W'. */
  public static final int D = 0;

  /** Format style code for 2-digit degrees: '124.36 W'. */
  public static final int DD = 1;

  /** Format style code for 4-digit degrees: '124.3600 W'. */
  public static final int DDDD = 4;

  /** Format style code for full precision degrees: '-124.360001453546473'. */
  public static final int RAW = 5;

  /** Format style code for degrees, minutes: '124 21.60 W'. */
  public static final int DDMM = 2;

  /** Format style code for degrees, minutes, seconds: '124 21 36.00 W'. */
  public static final int DDMMSS = 3;

  /** Selection code for latitude. */
  public static final int LAT = 0;

  /** Selection code for longitude. */
  public static final int LON = 1;

  /** The instance of WGS84. */
  private static final Datum WGS84 = 
    DatumFactory.create (SpheroidConstants.WGS84);

  // Variables
  // ---------
  /** 
   * The earth location latitude in degrees.  Latitudes have the range
   * [-90..90]. 
   */
  public double lat;

  /** 
   * The earth location longitude in degrees.  Longitudes have the range
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
   * Constructs a new earth location at the origin (0,0).  The datum
   * defaults to WGS84.
   */
  public EarthLocation () {

    this.lat = 0;
    this.lon = 0;

  } // EarthLocation constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new earth location from the specified parameters.
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
   * Constructs a new earth location at the origin (0,0) with the
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
   * Constructs a new earth location from the specified parameters.
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
   * Calculates the great circle distance between two locations A and B.
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
   * @param latA the latitude of point A in degrees.
   * @param lonA the longitude of point A in degrees.
   * @param latB the latitude of point B in degrees.
   * @param lonB the longitude of point B in degrees.
   *
   * @return the distance between points in kilometres.
   * 
   * @see #distance(EarthLocation)
   */
  public static double distance (
    double latA,
    double lonA,
    double latB,
    double lonB
  ) {

    // Convert to radians
    // ------------------
    double lat1 = Math.toRadians (latA);
    double lon1 = Math.toRadians (lonA);
    double lat2 = Math.toRadians (latB);
    double lon2 = Math.toRadians (lonB);

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
   * Calculates the great circle distance from this location to another.
   *
   * @param loc the location for which to calculate the distance.
   * 
   * @return the distance between points in kilometres.
   * 
   * @see #distance(double,double,double,double)
   */
  public double distance (
    EarthLocation loc
  ) {

    return (distance (this.lat, this.lon, loc.lat, loc.lon));
    
  } // distance

  ////////////////////////////////////////////////////////////

  /**
   * Translates an earth location by the specified increments.  If the
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
   * Formats a single coordinate from an earth location to a string.
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

    // Check for raw format
    // --------------------
    String str = null;
    if (style == RAW) {
      str = Double.toString (deg);
    } // if

    else {

      // Get hemisphere
      // --------------
      String hemisphere;
      if (select == LAT) hemisphere = (deg < 0 ? "S" : "N");
      else hemisphere = (deg < 0 ? "W" : "E");

      // Format value
      // ------------
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

    } // else
    
    return (str);

  } // formatSingle

  ////////////////////////////////////////////////////////////

  /**
   * Formats a single coordinate from this earth location to a string.
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
   * Formats a single coordinate from this earth location coordinate
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
   * Formats this earth location to a string.  Both latitude and
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
   * Formats this earth location to a string using the default format
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

  /** 
   * Checks if this earth location is valid.
   *
   * @return true if the location is valid or false if not.  An invalid 
   * earth location is normally used as a flag for a computation that has
   * failed.
   *
   * @see #markInvalid
   */
  public boolean isValid () {

    boolean locIsValid = true;
    if (Double.isNaN (lat))
      locIsValid = false;
    else if (Double.isNaN (lon))
      locIsValid = false;

    return (locIsValid);

  } // isValid

  ////////////////////////////////////////////////////////////

  /** 
   * Marks this location as invalid.  Subsequent calls to check
   * for validity will reflect the new state.
   *
   * @see #isValid
   *
   * @since 3.3.1
   */
  public void markInvalid () {
  
    this.lat = Double.NaN;
    this.lon = Double.NaN;
    
  } // markInvalid

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
   * Renders this earth location to a graphics context as a point.
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting Earth
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

  @Override
  public boolean equals (Object obj) {

    if (!(obj instanceof EarthLocation)) return (false);
    EarthLocation loc = (EarthLocation) obj;
    return (this.lat == loc.lat && this.lon == loc.lon && this.datum.equals (loc.datum));

  } // equals

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (EarthLocation.class);

    logger.test ("constructors");

    EarthLocation loc = new EarthLocation();
    assert (loc.lat == 0);
    assert (loc.lon == 0);
    Datum wgsDatum = DatumFactory.create (SpheroidConstants.WGS84);
    assert (loc.getDatum() == wgsDatum);
    
    loc = new EarthLocation (10, 20);
    assert (loc.lat == 10);
    assert (loc.lon == 20);
    assert (loc.getDatum() == wgsDatum);
    
    Datum grsDatum = DatumFactory.create (SpheroidConstants.GRS1980);
    loc = new EarthLocation (grsDatum);
    assert (loc.lat == 0);
    assert (loc.lon == 0);
    assert (loc.getDatum() == grsDatum);
  
    loc = new EarthLocation (10, 20, grsDatum);
    assert (loc.lat == 10);
    assert (loc.lon == 20);
    assert (loc.getDatum() == grsDatum);
    
    logger.passed();

    logger.test ("markInvalid, isValid");
    loc = new EarthLocation (0, 0);
    assert (loc.isValid());
    loc.markInvalid();
    assert (!loc.isValid());
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // EarthLocation class  

////////////////////////////////////////////////////////////////////////
