////////////////////////////////////////////////////////////////////////
/*
     FILE: ProjectionConstants.java
  PURPOSE: Hold various map projection constants.
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
 * The <code>ProjectionConstants</code> class is an interface
 * that hold various map projection constants, including
 * projection indices and names.
 */
public interface ProjectionConstants {

  /** Geographic projection code. */
  public static final int GEO        = 0;

  /** Universal Transverse Mercator projection code. */
  public static final int UTM        = 1;

  /** State Plane Coordinates projection code. */
  public static final int SPCS       = 2;

  /** Albers Conical Equal Area projection code. */
  public static final int ALBERS     = 3;

  /** Lambert Conformal Conic projection code. */
  public static final int LAMCC      = 4;

  /** Mercator projection code. */
  public static final int MERCAT     = 5;

  /** Polar Stereographic projection code. */
  public static final int PS         = 6;

  /** Polyconic projection code. */
  public static final int POLYC      = 7;

  /** Equidistant Conic projection code. */
  public static final int EQUIDC     = 8;

  /** Transverse Mercator projection code. */
  public static final int TM         = 9;

  /** Stereographic projection code. */
  public static final int STEREO     = 10;

  /** Lambert Azimuthal Equal Area projection code. */
  public static final int LAMAZ      = 11;

  /** Azimuthal Equidistant projection code. */
  public static final int AZMEQD     = 12;

  /** Gnomonic projection code. */
  public static final int GNOMON     = 13;

  /** Orthographic projection code. */
  public static final int ORTHO      = 14;

  /** General Vertical Near-Side Perspective projection code. */
  public static final int GVNSP      = 15;

  /** Sinusoidal projection code. */
  public static final int SNSOID     = 16;

  /** Equirectangular projection code. */
  public static final int EQRECT     = 17;

  /** Miller Cylindrical projection code. */
  public static final int MILLER     = 18;

  /** Van der Grinten projection code. */
  public static final int VGRINT     = 19;

  /** Hotine Oblique Mercator projection code. */
  public static final int HOM        = 20;

  /** Robinson projection code. */
  public static final int ROBIN      = 21;

  /** Space Oblique Mercator (SOM) projection code. */
  public static final int SOM        = 22;

  /** Alaska Conformal projection code. */
  public static final int ALASKA     = 23;

  /** Interrupted Goode Homolosine projection code. */
  public static final int GOOD       = 24;

  /** Mollweide projection code. */
  public static final int MOLL       = 25;

  /** Interrupted Mollweide projection code. */
  public static final int IMOLL      = 26;

  /** Hammer projection code. */
  public static final int HAMMER     = 27;

  /** Wagner IV projection code. */
  public static final int WAGIV      = 28;

  /** Wagner VII projection code. */
  public static final int WAGVII     = 29;

  /** Oblated Equal Area projection code. */
  public static final int OBEQA      = 30;

  /** User defined projection code. */
  public static final int USDEF      = 99;

  /** The total number of projection codes. */
  public static final int MAX_PROJECTIONS = 31;

  /** The list of projection code names. */
  public static final String[] PROJECTION_NAMES = {
    "Geographic",
    "Universal Transverse Mercator",
    "State Plane Coordinates",
    "Albers Conical Equal Area",
    "Lambert Conformal Conic",
    "Mercator",
    "Polar Stereographic",
    "Polyconic",
    "Equidistant Conic",
    "Transverse Mercator",
    "Stereographic",
    "Lambert Azimuthal Equal Area",
    "Azimuthal Equidistant",
    "Gnomonic",
    "Orthographic",
    "General Vertical Near-side Perspective",
    "Sinusoidal",
    "Equirectangular",
    "Miller Cylindrical",
    "Van der Grinten",
    "Hotine Oblique Mercator",
    "Robinson",
    "Space Oblique Mercator",
    "Alaska Conformal",
    "Interrupted Goode Homolosine",
    "Mollweide",
    "Interrupted Mollweide",
    "Hammer",
    "Wagner IV",
    "Wagner VII",
    "Oblated Equal Area"
  };

} // ProjectionConstants interface

////////////////////////////////////////////////////////////////////////
