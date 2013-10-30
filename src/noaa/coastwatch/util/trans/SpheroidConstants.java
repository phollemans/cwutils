////////////////////////////////////////////////////////////////////////
/*
     FILE: SpheroidConstants.java
  PURPOSE: Hold various Earth spheroid model constants.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/26
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

/** 
 * The <code>SpheroidConstants</code> class is an interface that
 * hold various Earth spheroid constants, including spheroid
 * index, names, and semi-major / semi-minor axes sizes.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public interface SpheroidConstants {

  /** Clarke 1866 (default) spheroid code. */
  public static final int CLARKE1866   = 0;

  /** Clarke 1880 spheroid code. */
  public static final int CLARKE1880   = 1;

  /** Bessel spheroid code. */
  public static final int BESSEL       = 2;

  /** International 1967 spheroid code. */
  public static final int INT1967      = 3;

  /** International 1909 spheroid code. */
  public static final int INT1909      = 4;

  /** WGS 72 spheroid code. */
  public static final int WGS72        = 5;

  /** Everest spheroid code. */
  public static final int EVEREST      = 6;

  /** WGS 66 spheroid code. */
  public static final int WGS66        = 7;

  /** GRS 1980 spheroid code. */
  public static final int GRS1980      = 8;

  /** Airy spheroid code. */
  public static final int AIRY         = 9;

  /** Modified Everest spheroid code. */
  public static final int MOD_EVEREST  = 10;

  /** Modified Airy spheroid code. */
  public static final int MOD_AIRY     = 11;

  /** WGS 84 spheroid code. */
  public static final int WGS84        = 12;

  /** SouthEast Asia spheroid code. */
  public static final int SE_ASIA      = 13;

  /** Australian National spheroid code. */
  public static final int AUS_NAT      = 14;

  /** Krassovsky spheroid code. */
  public static final int KRASS        = 15;

  /** Hough spheroid code. */
  public static final int HOUGH        = 16;

  /** Mercury 1960 spheroid code. */
  public static final int MERCURY1960  = 17;

  /** Modified Mercury 1968 spheroid code. */
  public static final int MOD_MER1968  = 18;

  /** Sphere of radius 6,370,997 metres spheroid code. */
  public static final int SPHERE       = 19;

  /** The total number of spheroid codes. */
  public static final int MAX_SPHEROIDS = 20;

  /** The list of spheroid code names. */
  public static final String[] SPHEROID_NAMES = {
    "Clarke 1866",
    "Clarke 1880",
    "Bessel",
    "International 1967",
    "International 1909",
    "WGS 72",
    "Everest",
    "WGS 66",
    "GRS 1980",
    "Airy",
    "Modified Everest",
    "Modified Airy",
    "WGS 84",
    "SouthEast Asia",
    "Australian National",
    "Krassovsky",
    "Hough",
    "Mercury 1960",
    "Modified Mercury 1968",
    "Sphere of radius 6370997 m"
  };

  /** Standard Earth radius in kilometers. */
  public static final double STD_RADIUS = 6370.997;

  /** Spheroid semi-major axes in meters. */
  public static final double[] SPHEROID_SEMI_MAJOR = new double[] {
    6378206.4,                // 0: Clarke 1866 (default) 
    6378249.145,              // 1: Clarke 1880 
    6377397.155,              // 2: Bessel 
    6378157.5,                // 3: International 1967 
    6378388.0,                // 4: International 1909 
    6378135.0,                // 5: WGS 72 
    6377276.3452,             // 6: Everest 
    6378145.0,                // 7: WGS 66 
    6378137.0,                // 8: GRS 1980 
    6377563.396,              // 9: Airy 
    6377304.063,              // 10: Modified Everest 
    6377340.189,              // 11: Modified Airy 
    6378137.0,                // 12: WGS 84 
    6378155.0,                // 13: Southeast Asia 
    6378160.0,                // 14: Australian National 
    6378245.0,                // 15: Krassovsky 
    6378270.0,                // 16: Hough 
    6378166.0,                // 17: Mercury 1960 
    6378150.0,                // 18: Modified Mercury 1968 
    6370997.0                 // 19: Sphere of Radius 6370997 meters 
  };

  /** Spheroid inverse flattening values. */
  public static final double[] SPHEROID_INV_FLAT = new double[] {
    294.9786982,              // 0: Clarke 1866 (default)
    293.465,                  // 1: Clarke 1880 
    299.1528128,              // 2: Bessel 
    298.249615390,            // 3: International 1967 
    297.0,                    // 4: International 1909 
    298.26,                   // 5: WGS 72 
    300.8017,                 // 6: Everest 
    298.25,                   // 7: WGS 66 
    298.257222101,            // 8: GRS 1980 
    299.3249646,              // 9: Airy 
    300.8017,                 // 10: Modified Everest 
    299.3249646,              // 11: Modified Airy 
    298.257223653,            // 12: WGS 84 
    298.3,                    // 13: Southeast Asia 
    298.25,                   // 14: Australian National 
    298.3,                    // 15: Krassovsky 
    297.0,                    // 16: Hough 
    298.3,                    // 17: Mercury 1960 
    298.3,                    // 18: Modified Mercury 1968 
    Double.POSITIVE_INFINITY  // 19: Sphere of Radius 6370997 meters 
  };

  /** Spheroid semi-minor axes in meters. */
  public static final double[] SPHEROID_SEMI_MINOR = new double[] {
    6356583.8,                // 0: Clarke 1866 (default) 
    6356514.86955,            // 1: Clarke 1880 
    6356078.96284,            // 2: Bessel 
    6356772.2,                // 3: International 1967 
    6356911.94613,            // 4: International 1909 
    6356750.519915,           // 5: WGS 72 
    6356075.4133,             // 6: Everest 
    6356759.769356,           // 7: WGS 66 
    6356752.31414,            // 8: GRS 1980 
    6356256.91,               // 9: Airy 
    6356103.039,              // 10: Modified Everest 
    6356034.448,              // 11: Modified Airy 
    6356752.314245,           // 12: WGS 84 
    6356773.3205,             // 13: Southeast Asia 
    6356774.719,              // 14: Australian National 
    6356863.0188,             // 15: Krassovsky 
    6356794.343479,           // 16: Hough 
    6356784.283666,           // 17: Mercury 1960 
    6356768.337303,           // 18: Modified Mercury 1968 
    6370997.0,                // 19: Sphere of Radius 6370997 meters   
  };

} // SpheroidConstants interface

////////////////////////////////////////////////////////////////////////
