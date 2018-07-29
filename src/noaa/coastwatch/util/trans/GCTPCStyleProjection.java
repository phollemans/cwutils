////////////////////////////////////////////////////////////////////////
/*

     File: GCTPCStyleProjection.java
   Author: Peter Hollemans
     Date: 2012/11/01

  CoastWatch Software Library and Utilities
  Copyright (c) 2012 National Oceanic and Atmospheric Administration
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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.PrintStream;
import java.util.Arrays;
import noaa.coastwatch.util.trans.GCTPStyleProjection;

/**
 * The <code>GCTPCStyleProjection</code> class provides method signatures
 * that correspond directly with the GCTPC code modules for ease of converting
 * GCTPC code to Java.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public abstract class GCTPCStyleProjection
  extends GCTPStyleProjection {

  // Constants
  // ---------

  /** The indices into return value array for forward transforms. */
  public static final int X = 0;
  public static final int Y = 1;

  /** The indices into return value array for inverse transforms. */
  public static final int LON = 0;
  public static final int LAT = 1;

  /** The return codes for GCTPC functions. */
  public static final int OK = 0;
  public static final int ERROR = -1;
  public static final int IN_BREAK = -2;

  // Variables
  // ---------

  /** The projection parameters used at initialization. */
  protected double[] params;
  
  /** The stream to output parameter messages. */
  private static PrintStream paramStream;

  /** The stream to output error messages. */
  private static PrintStream errorStream;
  
  ////////////////////////////////////////////////////////////

  /** 
   * Sets the error output stream for use by the error
   * reporting functions.  By default the error reporting
   * is disabled.
   *
   * @param stream the new stream for error reporting.
   */
  public static void setErrorStream (PrintStream stream) { errorStream = stream; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the parameter output stream for use by the parameter
   * reporting functions.  By default the parameter reporting
   * is disabled.
   *
   * @param stream the new stream for parameter reporting.
   */
  public static void setParamStream (PrintStream stream) { paramStream = stream; }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a GCTP C style projection.
   *
   * @param system the map projection system.
   * @param zone the map projection zone for State Plane and UTM
   * projections.
   * @param rMajor the semi-major axis in meters.
   * @param rMinor the semi-minor axis in meters.
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   */
  protected GCTPCStyleProjection (
    int system,
    int zone,
    double rMajor,
    double rMinor,
    int[] dimensions,
    AffineTransform affine
  ) throws NoninvertibleTransformException {

    super (system, zone, rMajor, rMinor, dimensions, affine);
    params = new double[15];
  
  } // GCTPCStyleProjection
  
  ////////////////////////////////////////////////////////////

  /**
   * Performs the actual forward calculation as specified in the
   * GCTP C code.  This method is as close as possible to the native
   * C function signature.
   * 
   * @param lat the latitude to convert.
   * @param lon the longitude to convert.
   * @param x the map coordinate x value (modified).
   * @param y the map coordinate y value (modified).
   *
   * @return OK on success, or not OK on failure.
   */
  abstract protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  );

  ////////////////////////////////////////////////////////////

  public void mapTransformFor (
    double[] lonLat,
    double[] xy
  ) {

    double x[] = new double[1];
    double y[] = new double[1];
    long result = projfor (lonLat[LAT], lonLat[LON], x, y);
    if (result == OK) {
      xy[X] = x[0];
      xy[Y] = y[0];
    } // if
    else {
      xy[X] = Double.NaN;
      xy[Y] = Double.NaN;
    } // else

  } // mapTransformFor

  ////////////////////////////////////////////////////////////

  /**
   * Performs the actual inverse calculation as specified in the
   * GCTP C code.  This method is as close as possible to the native
   * C function signature.
   * 
   * @param lat the latitude to convert.
   * @param lon the longitude to convert.
   * @param x the map coordinate x value (modified).
   * @param y the map coordinate y value (modified).
   *
   * @return OK on success, or not OK on failure.
   */
  abstract protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  );

  ////////////////////////////////////////////////////////////

  public void mapTransformInv (
    double[] xy,
    double[] lonLat
  ) {

    double lon[] = new double[1];
    double lat[] = new double[1];
    long result = projinv (xy[X], xy[Y], lon, lat);
    if (result == OK) {
      lonLat[LON] = lon[0];
      lonLat[LAT] = lat[0];
    } // if
    else {
      lonLat[LON] = Double.NaN;
      lonLat[LAT] = Double.NaN;
    } // else

  } // mapTransformInv

  ////////////////////////////////////////////////////////////

  /**
   * These are some dummy functions to satify the GCTPC code.  They
   * currently have no implementation, but may be updated in the future
   * to format and report GCTP operations.
   */

  protected static void p_error (char what[], char where[]) { }
  protected static void p_error (String what, String where) { }
  protected static void p_error (char what[], String where) { }
  protected static void sprintf (char buffer[], String format, Object... args) { }
  protected static void pblank () { }

  ////////////////////////////////////////////////////////////

  protected static void ptitle (String title) {
    if (paramStream != null) paramStream.printf ("\n%s PROJECTION PARAMETERS:\n\n", title);
  }

  protected void radius (double r) {
    if (paramStream != null)
      paramStream.printf ("   Radius of Sphere:     %f meters\n", r);
  }
  
  protected void radius2 (double rmaj, double  rmin) {
    if (paramStream != null) {
      paramStream.printf ("   Semi-Major Axis of Ellipsoid:     %f meters\n", rmaj);
      paramStream.printf ("   Semi-Minor Axis of Ellipsoid:     %f meters\n", rmin);
    }
  }
  
  protected void cenlon (double lon) {
    if (paramStream != null)
      paramStream.printf ("   Longitude of Center:     %f degrees\n", lon*R2D);
  }
  
  protected void cenlonmer (double lon) {
    if (paramStream != null)
      paramStream.printf("   Longitude of Central Meridian:     %f degrees\n", lon*R2D);
  }
  
  protected void cenlat (double lat) {
    if (paramStream != null)
      paramStream.printf ("   Latitude  of Center:     %f degrees\n", lat*R2D);
  }
  
  protected void origin (double lat) {
    if (paramStream != null)
      paramStream.printf ("   Latitude of Origin:     %f degrees\n", lat*R2D);
  }
  
  protected void stanparl (double lat1, double lat2) {
    if (paramStream != null) {
      paramStream.printf ("   1st Standard Parallel:     %f degrees\n", lat1*R2D);
      paramStream.printf ("   2nd Standard Parallel:     %f degrees\n", lat2*R2D);
    }
  }
  
  protected void stparl1 (double lat) {
    if (paramStream != null)
      paramStream.printf ("   Standard Parallel:     %f degrees\n", lat*R2D);
  }
  
  protected void offsetp (double fe, double fn) {
    if (paramStream != null) {
      paramStream.printf ("   False Easting:      %f meters \n", fe);
      paramStream.printf ("   False Northing:     %f meters \n", fn);
    }
  }
  
  protected static void genrpt (double a, String what) {
    if (paramStream != null)
      paramStream.printf("   %s %f\n", what, a);
  }

  protected static void genrpt_long (long a, String what) {
    if (paramStream != null)
      paramStream.printf("   %s %d\n", what, a);
  }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the GCTPC style parameter set.  This is the parameter set that will
   * be returned by {@link #getParameters}.
   *
   * @param parameters the parameters to set.
   */
  public void setParameters (
    double[] parameters
  ) {
  
    this.params = Arrays.copyOf (parameters, parameters.length);
  
  } // setParameters

  ////////////////////////////////////////////////////////////

  public double[] getParameters () { return (params); }

  ////////////////////////////////////////////////////////////

  /** Convenience function for phi1z. */
  public static double phi1z (
    double eccent,
    double qs,
    long flag[]
  ) {

    double result = phi1z (eccent, qs);
    flag[0] = Double.isNaN (result) ? 1 : OK;
    return (result);

  } // phi1z

  ////////////////////////////////////////////////////////////

  /** Convenience function for phi2z. */
  public static double phi2z (
    double eccent,
    double ts,
    long flag[]
  ) {

    double result = phi2z (eccent, ts);
    flag[0] = Double.isNaN (result) ? 2 : OK;
    return (result);

  } // phi2z

  ////////////////////////////////////////////////////////////

  public static double phi3z (
    double ml,
    double e0,
    double e1,
    double e2,
    double e3,
    long[] flag
  ) {

    double result = phi3z (ml, e0, e1, e2, e3);
    flag[0] = Double.isNaN (result) ? 3 : OK;
    return (result);
  
  } // phi3z

  ////////////////////////////////////////////////////////////

  public static long phi4z (
    double eccent,
    double e0,
    double e1,
    double e2,
    double e3,
    double a,
    double b,
    double c[],
    double phi[]
  ) {

    phi[0] = phi4z (eccent, e0, e1, e2, e3, a, b, c);
    return (Double.isNaN (phi[0]) ? 4 : OK);

  } // phi4z

  ////////////////////////////////////////////////////////////

  /**
   * Converts DMS packed angle into degrees.
   * 
   * @param ang the DMS packed angle.
   * @param iflg the error flag number (modified).
   *
   * @return the unpacked angle in degrees.
   */
  public static double paksz (
    double ang,
    long iflg[]
  ) {

    double fac;             /* sign flag                    */
    double deg;             /* degree variable              */
    double min;             /* minute variable              */
    double sec;             /* seconds variable             */
    double tmp;             /* temporary variable           */
    long i;                 /* temporary variable           */

    iflg[0] = 0;

    if (ang < 0.0)
      fac = -1;
    else
      fac = 1;

    /* find degrees
    -------------*/
    sec = Math.abs(ang);
    tmp = 1000000.0;
    i = (long) (sec/tmp);
    if (i > 360)
      {
      p_error("Illegal DMS field","paksz-deg");
      iflg[0] = 1116;
      return(-1);
      }
    else
      deg = i;

    /* find minutes
    -------------*/
    sec = sec - deg * tmp;
    tmp = 1000;
    i = (long) (sec / tmp);
    if (i > 60)
      {
      p_error("Illegal DMS field","paksz-min");
      iflg[0] = 1116;
      return(-1);
      }
    else
      min = i;

    /* find seconds
    -------------*/
    sec = sec - min * tmp;
    if (sec > 60)
      {
      p_error("Illegal DMS field","paksz-sec");
      iflg[0] = 1116;
      return(-1);
      }
    else
      sec = fac * (deg * 3600.0 + min * 60.0 + sec);
    deg = sec / 3600.0;
 
    return(deg);

  } // paksz

  ////////////////////////////////////////////////////////////
  
} // GCTPCStyleProjection

////////////////////////////////////////////////////////////////////////
