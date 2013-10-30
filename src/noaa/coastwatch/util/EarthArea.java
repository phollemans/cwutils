////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthArea.java
  PURPOSE: A class to work with areas on a sphere.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/10
  CHANGES: 2002/10/02, PFH, added addAll
           2002/10/03, PFH, added getExtremes
           2002/11/28, PFH, added equals method
           2002/12/06, PFH, added Cloneable interface
           2002/12/10, PFH, modified explore for iterative algorithm,
             added check for valid center location in constructor
           2002/12/17, PFH, fixed problem in singular center explore
           2003/11/16, PFH, added more documentation to getExtremes()
           2004/03/20, PFH, modified explore() to include start surroundings
           2005/05/27, PFH, added intersection() and isEmpty()

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * The Earth area class represents irregularly shaped areas on a
 * sphere.  In cases where simple north, south, east, and west bounds
 * are inadequate for describing an area, the Earth area class may be
 * used to set up and query if a certain Earth location is inside an
 * irregular shape.  The area information is maintained using a set of
 * 1x1 degree grid squares covering the entire globe.  Each grid
 * square is referenced by its lower-left corner -- for example the
 * grid square (10, 20) covers the area between 10N to 11N and 20E to
 * 21E.  All Earth locations must have a latitude range of [-90..90]
 * and longitude range of [-180..180).
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class EarthArea
  implements Cloneable {

  // Variables
  // ---------

  /** The bit set used to encode shape information. */
  private BitSet bits;

  ////////////////////////////////////////////////////////////

  /** 
   * Computes the intersection between this area and another.  The
   * resulting area includes only grid squares which occur both in this
   * area and in the other area.
   *
   * @param area the other Earth area to compute the intersection.
   *
   * @return a new Earth area containing only grid squares in common.
   */
  public EarthArea intersection (
    EarthArea area
  ) {

    EarthArea newArea = (EarthArea) this.clone();
    newArea.bits.and (area.bits);
    return (newArea);

  } // intersection

  ////////////////////////////////////////////////////////////

  /** Returns true if this Earth area contains no grid squares. */
  public boolean isEmpty () { return (bits.isEmpty()); }

  ////////////////////////////////////////////////////////////

  /**
   * The Earth area iterator is used to loop over all grid squares in
   * an Earth area.
   */
  public class EarthAreaIterator
    implements Iterator {

    // Variables
    // ---------
    private int index, nextIndex;

    //////////////////////////////////////////////////////////

    public EarthAreaIterator () {

      nextIndex = bits.nextSetBit(0);

    } // EarthAreaIterator

    //////////////////////////////////////////////////////////

    /** Returns true if the area has more grid squares. */
    public boolean hasNext() { 

      return (nextIndex != -1);

    } // hasNext

    //////////////////////////////////////////////////////////

    /** Returns the next grid square coordinates as [lat, lon]. */
    public Object next() { 

      if (nextIndex == -1) throw new NoSuchElementException();
      index = nextIndex;
      nextIndex = bits.nextSetBit(index+1);
      return (getSquare (index));

    } // next

    //////////////////////////////////////////////////////////

    /** Performs no operation. */
    public void remove() {

      throw new UnsupportedOperationException();

    } // remove

    //////////////////////////////////////////////////////////

  } // EarthAreaIterator class

  ////////////////////////////////////////////////////////////

  /** 
   * Returns an iterator over the grid square elements.  Each object
   * returned by the iterator contains the lower-left
   * <code>int[]</code> coordinates of the grid square as [lat, lon].
   */
  public Iterator getIterator () {

    return (new EarthAreaIterator ());

  } // getIterator

  ////////////////////////////////////////////////////////////

  /** 
   * Determines if an Earth location is contained in this area.
   *
   * @param loc the Earth location to search for.
   *
   * @return true if Earth location is in this area, or false if not.
   */
  public boolean contains (
    EarthLocation loc
  ) {

    int index = getIndex(loc);
    if (index == -1) return (false);
    return (bits.get (index));

  } // contains

  ////////////////////////////////////////////////////////////

  /** 
   * Determines if a grid square is contained in this area.
   *
   * @param square the grid square lower-left corner as [lat, lon].
   *
   * @return true if grid square is in this area, or false if not.
   */
  public boolean contains (
    int[] square
  ) {

    int index = getIndex (square[0], square[1]);
    if (index == -1) return (false);
    return (bits.get (index));

  } // contains

  ////////////////////////////////////////////////////////////

  /** 
   * Adds an Earth location to this area.
   *
   * @param loc the Earth location to add.
   */
  public void add (
    EarthLocation loc
  ) {

    int index = getIndex(loc);
    if (index == -1) return;
    bits.set (index);

  } // add

  ////////////////////////////////////////////////////////////

  /** 
   * Adds all Earth locations to this area.
   */
  public void addAll () {

    int locations = 360*180;
    for (int i = 0; i < locations; i++)
      bits.set (i);

  } // addAll

  ////////////////////////////////////////////////////////////

  /** 
   * Removes an Earth location from this area.
   *
   * @param loc the Earth location to remove.
   */
  public void remove (
    EarthLocation loc
  ) {

    int index = getIndex(loc);
    if (index == -1) return;
    bits.clear (index);

  } // remove

  ////////////////////////////////////////////////////////////

  /**
   * Computes an index based on a grid square.
   *
   * @param lat the grid square lower-left corner latitude.
   * @param lon the grid square lower-left corner longitude.
   *
   * @return an index into the bit set or -1 if the grid square is
   * invalid.
   */
  private int getIndex (
    int lat,
    int lon
  ) {

    if (lat < -90 || lat > 89) return (-1);
    if (lon < -180 || lon > 179) return (-1);
    return ((lat+90)*360 + (lon+180));

  } // getIndex

  ////////////////////////////////////////////////////////////

  /**
   * Computes an index based on an Earth location.
   *
   * @param loc the Earth location to convert.
   *
   * @return an index into the bit set or -1 if the Earth location is
   * invalid.
   */
  private int getIndex (
    EarthLocation loc
  ) {

    int lat = (int) Math.floor (loc.lat);
    if (lat == 90) lat = 89;
    int lon = (int) Math.floor (loc.lon);
    return (getIndex (lat, lon));

  } // getIndex

  ////////////////////////////////////////////////////////////

  /**
   * Computes an Earth location based on an index.  The Earth location
   * is the center of the box represented by the index.
   *
   * @param index the index to convert.
   *
   * @return the computed Earth location.
   */
  private EarthLocation getLocation (
    int index
  ) {

    int[] square = getSquare (index);
    return (new EarthLocation (square[0]+0.5, square[1]+0.5));

  } // getLocation

  ////////////////////////////////////////////////////////////

  /**
   * Computes a pair of grid square coordinates based on an index.
   * The grid square coordinates are the lower-left corner of the
   * grid square in degrees as [lat, lon].
   *
   * @param index the index to convert.
   *
   * @return the computed grid square coordinates.
   */
  private int[] getSquare (
    int index
  ) {

    int lat = index/360 - 90;
    int lon = index%360 - 180;
    return (new int[] {lat, lon});

  } // getSquare

  ////////////////////////////////////////////////////////////

  /** Creates an empty Earth area with no locations. */
  public EarthArea () {
  
    bits = new BitSet();

  } // EarthArea constructor

  ////////////////////////////////////////////////////////////

  /**
   * Expands the current area by 1 degree in all directions.  If the
   * current area is as follows:
   * <pre>
   *   . . . . . .
   *   . . . . . .
   *   . . * * . .
   *   . * * . . .
   *   . . * . . .
   *   . . . . . .
   *   . . . . . .
   * </pre>
   * then the expanded area includes the boundary points:
   * <pre>
   *   . . . . . .
   *   . x x x x .
   *   x x * * x .
   *   x * * x x .
   *   x x * x . .
   *   . x x x . .
   *   . . . . . .
   * </pre>
   */
  public void expand () {

    // Create base unexpanded copy
    // ---------------------------
    BitSet base = (BitSet) bits.clone();

    // Expand each box
    // ---------------
    for (int i = base.nextSetBit(0); i >= 0; i = base.nextSetBit(i+1)) {
      EarthLocation center = getLocation (i);
      add (center.translate (1, -1));
      add (center.translate (1, 0));
      add (center.translate (1, 1));
      add (center.translate (0, -1));
      add (center.translate (0, 1));
      add (center.translate (-1, -1));
      add (center.translate (-1, 0));
      add (center.translate (-1, 1));
    } // for

  } // expand

  ////////////////////////////////////////////////////////////

  /** 
   * Explores within data location boundaries using an Earth transform
   * to find all locations within an area.  The new grid squares are
   * added to the area.  The exploration terminates when squares that
   * are already part of the area are encountered, the data location
   * boundaries are hit, or the Earth transform returns an invalid
   * data location.
   * 
   * @param trans an Earth transform to use for exploration.
   * @param min the minimum data location to explore.
   * @param max the maximum data location to explore.
   * @param start the starting Earth location for exploration.
   */
  public void explore (
    EarthTransform trans,
    DataLocation min,
    DataLocation max,
    EarthLocation start
  ) {

    // Create exploration stack
    // ------------------------
    BitSet stack = new BitSet();
    int startIndex = getIndex (start);

    // Create probes
    // -------------
    int[] probeLat = new int[] {1, 0, -1, 0, 1, -1, -1, 1};
    int[] probeLon = new int[] {0, 1, 0, -1, 1, 1, -1, -1};

    // Add start and surrounding squares
    // ---------------------------------
    stack.set (startIndex);
    for (int i = 0; i < 8; i++) {
      stack.set (getIndex (start.translate (probeLat[i], probeLon[i])));
    } // for

    // While stack is not empty
    // ------------------------
    boolean done = false;
    while (!done) {

      // Retrieve location from stack
      // ----------------------------
      int index = stack.nextSetBit (0);
      if (index == -1) done = true;
 
      else {

        // Clear location from explore stack
        // ---------------------------------
        stack.clear (index);

        // Check location is valid
        // -----------------------
        if (!bits.get (index)) {
          EarthLocation earthLoc = getLocation (index);
          DataLocation dataLoc = trans.transform (earthLoc);
          if (dataLoc.isValid() && dataLoc.isContained (min, max)) {

            // Explore location and probe
            // --------------------------
            bits.set (index);
            for (int i = 0; i < 4; i++) {
              EarthLocation probe = earthLoc.translate (probeLat[i], 
                probeLon[i]);
              int probeIndex = getIndex (probe);
              if (!bits.get (probeIndex) && !stack.get (probeIndex)) 
                stack.set (probeIndex);
            } // for

          } // if

        } // if

      } // else

    } // while

    // Make sure start is explored
    // ---------------------------
    if (!bits.get (startIndex)) {
      DataLocation startLoc = trans.transform (start);
      if (startLoc.isValid() && startLoc.isContained (min, max))
        bits.set (startIndex);
    } // if

  } // explore

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new Earth area from the specified parameters.  The
   * parameters are used to perform an exploration of the area to
   * include all Earth locations.  The exploration is started at the
   * center of the data location boundaries.  After exploration, an
   * expansion of one grid square is performed.
   *
   * @param trans an Earth transform to use for exploration.
   * @param min the minimum data location to explore.
   * @param max the maximum data location to explore.  
   */
  public EarthArea (
    EarthTransform trans,
    DataLocation min,
    DataLocation max
  ) {

    // Initialize bits
    // ---------------
    bits = new BitSet();

    // Get center data location
    // ------------------------
    DataLocation center = new DataLocation ((min.get(0) + max.get(0))/2,
      (min.get(1) + max.get(1))/2);
    EarthLocation start = trans.transform (center);
    if (getIndex (start) == -1) 
      throw new RuntimeException ("Data center has no valid Earth location");

    // Explore Earth area and expand
    // -----------------------------
    explore (trans, min, max, start);
    expand();

  } // EarthArea constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the geographic extremes in this area.  If this Earth area
   * crosses the 180E/180W border, the east and west extremes are
   * adjusted so they both fall in the range [0..360].  Otherwise they
   * fall in the range [-180..180].
   *
   * @return the geographic extremes as [north, south, east, west].
   */
  public int[] getExtremes () {

    // Check for empty area
    // --------------------
    if (bits.nextSetBit(0) == -1) return (new int[] {0,0,0,0});

    // Initialize extremes
    // -------------------
    int i = bits.nextSetBit(0);
    int[] square = getSquare (i);
    int north = square[0];
    int south = square[0];
    int east = square[1];
    int west = square[1];
    int lonMod = (square[1] < 0 ? square[1]+360 : square[1]);
    int eastMod = lonMod;
    int westMod = lonMod;

    // Loop over each point
    // --------------------
    for (i = bits.nextSetBit(i+1); i >= 0; i = bits.nextSetBit(i+1)) {
      square = getSquare (i);
      north = Math.max (square[0], north);
      south = Math.min (square[0], south);
      east = Math.max (square[1], east);
      west = Math.min (square[1], west);
      lonMod = (square[1] < 0 ? square[1]+360 : square[1]);
      eastMod = Math.max (lonMod, eastMod);
      westMod = Math.min (lonMod, westMod);
    } // for

    // Check for boundary crossing
    // ---------------------------
    if (west == -180 && east == 179) {
      east = eastMod;
      west = westMod;
    } // if

    return (new int[] {north+1, south, east+1, west});    

  } // getExtremes

  ////////////////////////////////////////////////////////////

  /** Returns true if this area exactly equals another area. */
  public boolean equals (
    Object o
  ) {

    // Check compatibility
    // -------------------
    if (!(o instanceof EarthArea)) return (false);
    EarthArea area  = (EarthArea) o;

    // Check bit sets
    // --------------
    return (this.bits.equals (area.bits));

  } // equals

  ////////////////////////////////////////////////////////////

  /** Creates an independent copy of this area. */
  public Object clone () {

    // Create empty area
    // -----------------
    EarthArea area = new EarthArea();

    // Initialize variables
    // --------------------
    area.bits = (BitSet) bits.clone();

    return (area);

  } // clone

  ////////////////////////////////////////////////////////////

} // EarthArea class  

////////////////////////////////////////////////////////////////////////
