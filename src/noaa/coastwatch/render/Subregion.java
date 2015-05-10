////////////////////////////////////////////////////////////////////////
/*
     FILE: Subregion.java
  PURPOSE: Holds subregion information.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;

/*
 * A <code>Subregion</code> holds position and geographic extent
 * information about some small part of a larger geographic
 * region.  To be projection-independent, a subregion specifies a
 * center earth location and physical radius rather than using
 * latitude and longitude bounds.  Subregions may be used with
 * {@link EarthDataView} objects to center the view on a
 * subregion of interest.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class Subregion {

  // Constants
  // ---------

  /** The minimum data location increment for radius probes. */
  private static final double MIN_INC = 1e-6;

  // Variables
  // ---------
  
  /** The subregion center location. */
  private EarthLocation centerLoc;

  /** The physical radius of the subregion in kilometers. */
  private double radius;

  /** The name of the subregion. */
  private String name;

  /** The short name of the subregion. */
  private String shortName;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new subregion.
   *
   * @param centerLoc the subregion center location.
   * @param radius the subregino radius in kilometers.
   * @param name the common subregion name, for example "Chesapeake Bay".
   * @param shortName an abbreviated subregion name for use in
   * software, for example "cb".
   */
  public Subregion (
    EarthLocation centerLoc,
    double radius,
    String name,
    String shortName
  ) {

    this.centerLoc = centerLoc;
    this.radius = radius;
    this.name = name;
    this.shortName = shortName;

  } // Subregion constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new subregion with empty name and short name.
   *
   * @param centerLoc the subregion center location.
   * @param radius the subregino radius in kilometers.
   */
  public Subregion (
    EarthLocation centerLoc,
    double radius
  ) {

    this (centerLoc, radius, "", "");

  } // Subregion constructor

  ////////////////////////////////////////////////////////////

  /**
   * Finds the data location that is this region's radius away from
   * the center in a specific direction.
   *
   * @param trans the earth transform to use for coordinate transforms.
   * @param dataLoc the data location to use for probing (modified).
   * @param earthLoc the earth location to use for probing (modified).
   * @param direction the direction to use for each probe as a data location
   * offset.
   *
   * @return true if successful, false if Double.NaN was returned from
   * some distance calculation.
   */
  private boolean findDataLoc (
    EarthTransform trans,
    DataLocation dataLoc,
    EarthLocation earthLoc,
    int[] direction
  ) {

    // Initialize data probe
    // ---------------------
    trans.transform (centerLoc, dataLoc);
    trans.transform (dataLoc, earthLoc);
    double probeDist = Math.abs (centerLoc.distance (earthLoc) - radius);
    double lastProbeDist = Double.MAX_VALUE;

    // Set initial increment
    // ---------------------
    double[] inc = new double[] {direction[0], direction[1]};
    double incMagnitude = Math.max (Math.abs (inc[0]), Math.abs (inc[1]));

    // Loop until tolerance hit
    // ------------------------
    while (!Double.isNaN (probeDist) && incMagnitude > MIN_INC) {

      // Increment probe location and test distance
      // ------------------------------------------
      dataLoc.set (0, dataLoc.get (0) + inc[0]);
      dataLoc.set (1, dataLoc.get (1) + inc[1]);
      trans.transform (dataLoc, earthLoc);
      probeDist = Math.abs (centerLoc.distance (earthLoc) - radius);

      // Check to decrease increment
      // ---------------------------
      if (probeDist >= lastProbeDist) {
        inc[0] *= -0.5;
        inc[1] *= -0.5;
        incMagnitude = Math.max (Math.abs (inc[0]), Math.abs (inc[1]));
      } // if
      lastProbeDist = probeDist;

    } // while

    // Check final distance
    // --------------------
    if (Double.isNaN (probeDist)) return (false);
    else return (true);

  } // findDataLoc

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data location limits of this subregion relative to the
   * specified earth transform.
   *
   * @param trans the earth transform to use for computations.
   * @param start the starting data location for the rectangle that
   * minimally encloses this subregion (modified).
   * @param end the ending data location for the rectangle that
   * minimally encloses this subregion (modified).
   *
   * @return true if the limits were found and set, or false if
   * one of the coordinate transforms returned an invalid earth
   * or data location.  Usually this means that the limits are
   * past the edge of some transform discontinuity.
   */
  public boolean getLimits (
    EarthTransform trans, 
    DataLocation start,
    DataLocation end
  ) {

    // Create temporary locations
    // --------------------------
    DataLocation dataLoc = new DataLocation (2);
    EarthLocation earthLoc = new EarthLocation (trans.getDatum());

    // Find limits
    // -----------
    boolean found = findDataLoc (trans, dataLoc, earthLoc, new int[] {-1, 0});
    if (!found) return (false);
    double minRow = dataLoc.get (0);
    found = findDataLoc (trans, dataLoc, earthLoc, new int[] {1, 0});
    if (!found) return (false);
    double maxRow = dataLoc.get (0);
    found = findDataLoc (trans, dataLoc, earthLoc, new int[] {0, -1});
    if (!found) return (false);
    double minCol = dataLoc.get (1);
    found = findDataLoc (trans, dataLoc, earthLoc, new int[] {0, 1});
    if (!found) return (false);
    double maxCol = dataLoc.get (1);

    // Set coords
    // ----------
    start.set (0, minRow);
    start.set (1, minCol);
    end.set (0, maxRow);
    end.set (1, maxCol);
    return (true);

  } // getLimits

  ////////////////////////////////////////////////////////////
 
  /** Gets this subregion center location. */
  public EarthLocation getCenter () { return (centerLoc); }

  ////////////////////////////////////////////////////////////
 
  /** Gets this subregion radius in kilometers. */
  public double getRadius () { return (radius); }

  ////////////////////////////////////////////////////////////
 
  /** Gets this subregion name. */
  public String getName () { return (name); }

  ////////////////////////////////////////////////////////////
 
  /** Gets this subregion short name. */
  public String getShortName () { return (shortName); }

  ////////////////////////////////////////////////////////////
 
  /** Returns a string version of this subregion (the long name). */
  public String toString () { return (name); }

  ////////////////////////////////////////////////////////////

  /** Returns true if the subregions are equal. */
  public boolean equals (Object obj) {

    if (!(obj instanceof Subregion)) return (false);
    Subregion other = (Subregion) obj;
    return (
      this.centerLoc.equals (other.centerLoc) &&
      this.radius == other.radius &&
      this.shortName.equals (other.shortName) &&
      this.name.equals (other.name)
    );

  } // equals

  ////////////////////////////////////////////////////////////

} // Subregion class

////////////////////////////////////////////////////////////////////////
