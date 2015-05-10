////////////////////////////////////////////////////////////////////////
/*
     FILE: AlbersConicalEqualAreaProjection.java
  PURPOSE: Handles Albers Conical Equal Area map transformations.
   AUTHOR: Peter Hollemans
     DATE: 2012/11/02
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import noaa.coastwatch.util.trans.GCTPCStyleProjection;
import noaa.coastwatch.util.trans.GCTPStyleProjection;
import noaa.coastwatch.util.trans.ProjectionConstants;

/**
 * The <code>AlbersConicalEqualAreaProjection</code> class performs 
 * Albers Conical Equal Area map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class AlbersConicalEqualAreaProjection 
  extends GCTPCStyleProjection {

  // Variables
  // ---------

  private double c;                 // constant c
  private double e3;                // eccentricity
  private double es;                // eccentricity squared
  private double false_easting;     // x offset in meters
  private double false_northing;    // y offset in meters
  private double lon_center;        // center longitude
  private double ns0;               // ratio between meridians
  private double r_major;           // major axis
  private double r_minor;           // minor axis
  private double rh;                // height above ellipsoid

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param r_maj the major axis.
   * @param r_min the minor axis.
   * @param lat1 the first standard parallel.
   * @param lat2 the second standard parallel.
   * @param lon0 the center longitude.
   * @param lat0 the center latitude.
   * @param false_east the x offset in meters.
   * @param false_north the y offset in meters.
   *
   * @return OK on success, or not OK on failure.   
   */
  private long projinit (
    double r_maj,
    double r_min,
    double lat1,
    double lat2,
    double lon0,
    double lat0,
    double false_east,
    double false_north
  ) {

    double sin_po, cos_po;          // sin and cos values
    double con;                     // temporary variable
    double es, temp;                // eccentricity squared and temp var
    double ms1;                     // small m 1
    double ms2;                     // small m 2
    double qs0;                     // small q 0
    double qs1;                     // small q 1
    double qs2;                     // small q 2
    
    false_easting = false_east;
    false_northing = false_north;
    lon_center = lon0;
    if (Math.abs (lat1 + lat2) < EPSLN)
       {
       p_error ("Equal latitudes for St. Parallels on opposite sides of equator",
              "alber-forinit");
       return (31);
       }
    r_major = r_maj;
    r_minor = r_min;
    temp = r_minor / r_major;
    es = 1.0 - Math.pow (temp, 2);
    e3 = Math.sqrt (es);
    
    sin_po = Math.sin (lat1); cos_po = Math.cos (lat1);
    con = sin_po;
    
    ms1 = msfnz (e3, sin_po, cos_po);
    qs1 = qsfnz (e3, sin_po, cos_po);
    
    sin_po = Math.sin (lat2); cos_po = Math.cos (lat2);
    
    ms2 = msfnz (e3, sin_po, cos_po);
    qs2 = qsfnz (e3, sin_po, cos_po);
    
    sin_po = Math.sin (lat0); cos_po = Math.cos (lat0);
    
    qs0 = qsfnz (e3, sin_po, cos_po);
    
    if (Math.abs (lat1 - lat2) > EPSLN)
       ns0 = (ms1*ms1 - ms2*ms2)/ (qs2 - qs1);
    else
       ns0 = con;
    c = ms1*ms1 + ns0*qs1;
    rh = r_major*Math.sqrt (c - ns0*qs0)/ns0;
    
    /*Report parameters to the user
      -----------------------------*/
    ptitle ("ALBERS CONICAL EQUAL-AREA");
    radius2 (r_major, r_minor);
    stanparl (lat1, lat2);
    cenlonmer (lon_center);
    origin (lat0);
    offsetp (false_easting, false_northing);
    
    return (OK);

  } // projinit

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a map projection from the specified projection and
   * affine transform.   The {@link SpheroidConstants} and
   * {@link ProjectionConstants} class should be consulted for
   * valid parameter constants.
   *
   * @param rMajor the semi-major axis in meters.
   * @param rMinor the semi-minor axis in meters.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   * @param lat1 the first standard parallel.
   * @param lat2 the second standard parallel.
   * @param lon0 the center longitude.
   * @param lat0 the center latitude.
   * @param falseEast the false easting value.
   * @param falseNorth the false northing value.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public AlbersConicalEqualAreaProjection (
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine,
    double lat1,                    // first standard parallel
    double lat2,                    // second standard parallel
    double lon0,                    // center longitude
    double lat0,                    // center latitude
    double falseEast,
    double falseNorth
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (ALBERS, 0, rMajor, rMinor, dimensions, affine);
    setFalse (falseEast, falseNorth);
    long result = projinit (rMajor, rMinor, lat1, lat2, lon0, lat0, 
      falseEast, falseNorth);
    if (result != OK) 
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

  } // AlbersConicalEqualAreaProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    double sin_phi, cos_phi;        // sine and cos values
    double qs;                      // small q
    double theta;                   // angle
    double rh1;                     // height above ellipsoid
    
    sin_phi = Math.sin (lat); cos_phi = Math.cos (lat);
    qs = qsfnz (e3, sin_phi, cos_phi);
    rh1 = r_major*Math.sqrt (c - ns0*qs)/ns0;
    theta = ns0*adjust_lon (lon - lon_center);
    x[0] = rh1*Math.sin (theta) + false_easting;
    y[0] = rh - rh1*Math.cos (theta) + false_northing;
    
    return (OK);

  } // projfor

  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    double rh1;                     // height above ellipsoid
    double qs;                      // function q
    double con;                     // temporary sign value
    double theta;                   // angle
    long flag[];                    // error flag;
    
    flag = new long[1];
    x -= false_easting;
    y = rh - y + false_northing;;
    if (ns0 >= 0)
       {
       rh1 = Math.sqrt (x*x + y*y);
       con = 1.0;
       }
    else
       {
       rh1 = -Math.sqrt (x*x + y*y);
       con = -1.0;
       }
    theta = 0.0;
    if (rh1 != 0.0)
       theta = Math.atan2 (con*x, con*y);
    con = rh1*ns0 / r_major;
    qs = (c - con*con) / ns0;
    if (e3 >= 1e-10)
       {
       con = 1 - .5*(1.0 - es)*Math.log ((1.0 - e3) / (1.0 + e3))/e3;
       if (Math.abs (Math.abs (con) - Math.abs (qs)) > .0000000001 )
          {
          lat[0] = phi1z (e3, qs, flag);
          if (flag[0] != 0)
             return (flag[0]);
          }
       else
          {
          if (qs >= 0)
             lat[0] = .5*PI;
          else
             lat[0] = -.5*PI;
          }
       }
    else
       {
       lat[0] = phi1z (e3, qs, flag);
       if (flag[0] != 0)
          return (flag[0]);
       }
    
    lon[0] = adjust_lon (theta/ns0 + lon_center);
    
    return (OK);

  } // projinv

  ////////////////////////////////////////////////////////////

} // AlbersConicalEqualAreaProjection

////////////////////////////////////////////////////////////////////////
