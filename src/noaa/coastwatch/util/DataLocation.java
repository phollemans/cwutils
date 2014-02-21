////////////////////////////////////////////////////////////////////////
/*
     FILE: DataLocation.java
  PURPOSE: To define a data coordinate container with operations.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/25
  CHANGES: 2002/08/26, PFH, added index/dims constructor
           2002/09/06, PFH, changed rounding in getIndex
           2002/09/16, PFH, added getPoint
           2002/10/06, PFH, added increment
           2002/11/14, PFH, added isInvalid
           2002/12/04, PFH, added Cloneable interface
           2003/12/27, PFH, added hashCode()
           2004/01/10, PFH, added setCoords(int[])
           2004/03/27, PFH, added setCoords(DataLocation)
           2004/03/28, PFH, added format() method
           2014/02/11, PFH
           - Changes: Added markInvalid, transformInPlace, and 
             getCoords (double[]).
           - Issue: To help in implementing changes in LocationEstimator and
             reduce memory allocations needed in InverseGridResampler.
 
  CoastWatch Software Library and Utilities
  Copyright 2004-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.awt.geom.*;

/**
 * A data location represents a set of coordinates that uniquely
 * identify a data value position within some N-dimensional space.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class DataLocation
  implements Cloneable {

  // Variables
  // ---------
  /** The data location coordinates. */
  private double[] coords;

  /** The precalculated hash code. */
  private int hash;

  /** The hashed flag, true if the hash code is valid. */
  private boolean isHashed = false;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a formatted string for this location. 
   * 
   * @param doRound the rounding flag, true if rounding to the nearest
   * integer coordinate is desired.
   *
   * @return the formatted location string.
   */
  public String format (
    boolean doRound
  ) {

    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < coords.length; i++) {
      if (doRound)
        buffer.append (Integer.toString ((int) Math.round (coords[i])));
      else
        buffer.append (Double.toString (coords[i]));
      if (i != coords.length-1) 
        buffer.append (", ");
    } // for

    return (buffer.toString());

  } // format

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a data location with the specified rank.  All
   * coordinates are initialized to 0.
   *
   * @param rank the data location rank.
   */
  public DataLocation (
    int rank
  ) {

    this.coords = new double[rank];

  } // DataLocation constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a 1D data location from the specified coordinates.
   *
   * @param coord0 the 0 index coordinate value.
   */
  public DataLocation (
    double coord0
  ) {

    this.coords = new double[] {coord0};

  } // DataLocation constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a 2D data location from the specified coordinates.
   *
   * @param coord0 the 0 index coordinate value.
   * @param coord1 the 1 index coordinate value.
   */
  public DataLocation (
    double coord0,
    double coord1
  ) {

    this.coords = new double[] {coord0, coord1};

  } // DataLocation constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a data location from the specified coordinates.
   *
   * @param coords the data location coordinates.
   */
  public DataLocation (
    double[] coords
  ) {

    this.coords = (double[]) coords.clone();

  } // DataLocation constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the nearest data location with whole integer coordinates.
   *
   * @return the nearest data location.
   */
  public DataLocation round () {

    double[] round = new double[coords.length];
    for (int i = 0; i < coords.length; i++)
      round[i] = Math.round (coords[i]);
    return (create (round));

  } // round

  ////////////////////////////////////////////////////////////

  /**
   * Gets the nearest data location with whole integer coordinates
   * less than this location.
   *
   * @return the nearest lesser integer data location.
   */
  public DataLocation floor () {

    double[] floor = new double[coords.length];
    for (int i = 0; i < coords.length; i++)
      floor[i] = Math.floor (coords[i]);
    return (create (floor));

  } // floor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the nearest data location with whole integer coordinates
   * greater than this location.
   *
   * @return the nearest greater integer data location.
   */
  public DataLocation ceil () {

    double[] ceil = new double[coords.length];
    for (int i = 0; i < coords.length; i++)
      ceil[i] = Math.ceil (coords[i]);
    return (create (ceil));

  } // ceil

  ////////////////////////////////////////////////////////////

  public Object clone () {

    return (new DataLocation (coords));

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Gets one data location coordinate value.
   * 
   * @param index the dimension index.
   *
   * @return the coordinate value or Double.NaN if the index in invalid.
   */
  public double get (
    int index
  ) {

    if (index < 0 || index > coords.length-1) return (Double.NaN);
    return (coords[index]);

  } // get

  ////////////////////////////////////////////////////////////

  /**
   * Sets one data location coordinate value.
   * 
   * @param index the dimension index.
   * @param coord the coordinate value.
   */
  public void set (
    int index,
    double coord
  ) {

    if (index < 0 || index > coords.length-1) return;
    coords[index] = coord;
    isHashed = false;

  } // set

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data location coordinates as an array.
   *
   * @param coordsCopy the array of coordinates to fill, or null to create a
   * new array.
   *
   * @return the array of coordinate values, one per dimension.
   */
  public double[] getCoords (double[] coordsCopy) {

    if (coordsCopy == null)
      coordsCopy = (double[]) coords.clone();
    else {
      for (int i = 0; i < coords.length; i++)
        coordsCopy[i] = coords[i];
    } // else
    
    return (coordsCopy);

  } // getCoords

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data location coordinates as an array.
   *
   * @return the array of coordinate values, one per dimension.
   */
  public double[] getCoords () {

    return (getCoords (null));

  } // getCoords

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data location coordinates from an array.
   *
   * @param coords the array of coordinate values, one per dimension.
   * If the array does not match the location rank, no operation is
   * performed.
   */
  public void setCoords (double[] coords) {

    if (this.coords.length != coords.length) return;
    for (int i = 0; i < coords.length; i++)
      this.coords[i] = coords[i];
    isHashed = false;

  } // setCoords

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data location coordinates from an array.
   *
   * @param coords the array of coordinate values, one per dimension.
   * If the array does not match the location rank, no operation is
   * performed.
   */
  public void setCoords (int[] coords) {

    if (this.coords.length != coords.length) return;
    for (int i = 0; i < coords.length; i++)
      this.coords[i] = coords[i];
    isHashed = false;

  } // setCoords

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data location coordinates from another location.
   *
   * @param source the source coordinate data location.  If the source
   * location does not match in rank, no operation is performed.
   */
  public void setCoords (DataLocation source) {

    if (this.coords.length != source.coords.length) return;
    for (int i = 0; i < source.coords.length; i++)
      this.coords[i] = source.coords[i];
    isHashed = false;

  } // setCoords

  ////////////////////////////////////////////////////////////

  /** Gets the data location dimension rank. */
  public int getRank () { return (coords.length); }

  ////////////////////////////////////////////////////////////

  /**
   * Tests if this data location is contained between a minimum and a
   * maximum.  Containment means that each coordinate is bounded below
   * by the min and above by the max at each dimension.
   *
   * @param min the minimum location.
   * @param max the maximum location.
   *
   * @return true if the location is contained or false if not.  If
   * the min and max do not match rank with this location, false is
   * returned.
   */
  public boolean isContained (
    DataLocation min,
    DataLocation max
  ) {

    // Check ranks
    // -----------
    if (min.coords.length != coords.length || 
      max.coords.length != coords.length) 
      return (false);

    // Check containment
    // -----------------
    for (int i = 0; i < coords.length; i++)
      if (this.coords[i] < min.coords[i] || this.coords[i] > max.coords[i])
        return (false);

    // Conclude we are contained
    // -------------------------
    return (true);

  } // isContained

  ////////////////////////////////////////////////////////////

  /**
   * Tests if this data location is contained within the specified
   * dimensions.  The test checks if this location is in the range
   * [0..dims[i]-1] for all i.
   *
   * @param dims the dimensions for testing.
   *
   * @return true if the location is contained or false if not.  If
   * the dimensions do not match rank with this location, false is
   * returned.
   */
  public boolean isContained (
    int[] dims
  ) {

    // Check ranks
    // -----------
    if (dims.length != coords.length)
      return (false);

    // Check containment
    // -----------------
    for (int i = 0; i < coords.length; i++)
      if (this.coords[i] < 0 || this.coords[i] > dims[i]-1)
        return (false);

    // Conclude we are contained
    // -------------------------
    return (true);

  } // isContained

  ////////////////////////////////////////////////////////////

  /**
   * Truncates the data location to the nearest edge.  If the location
   * has any components outside [0..n-1] for each dimension size n,
   * then they are truncated to the nearest dimension bound.
   *
   * @param dims the dimension sizes.
   *
   * @return the truncated data location.
   */
  public DataLocation truncate (
    int[] dims
  ) {

    // Truncate to each dimension edge
    // -------------------------------
    double[] truncated = (double[]) coords.clone();
    for (int i = 0; i < coords.length; i++) {
      if (truncated[i] < 0) truncated[i] = 0;
      else if (truncated[i] > dims[i]-1) truncated[i] = dims[i]-1;
    } // for
    return (create (truncated));

  } // truncate

  ////////////////////////////////////////////////////////////

  /**
   * Creates a data location from an integer index and a set of
   * dimensions.  
   *
   * @param index the integer index.
   * @param dims the dimension lengths along each dimension.
   */
  public DataLocation (
    int index,
    int[] dims
  ) {

    // Initialize variables
    // --------------------
    int rank = dims.length;
    coords = new double[rank];
    int divisor = 1;
    for (int i = rank-1; i > 0; i--) divisor *= dims[i];

    // Calculate coordinates
    // ---------------------      
    for (int i = 0; i < rank; i++) {
      int coord = index/divisor;
      coords[i] = coord;
      index = index - coord*divisor;
      if (i != rank-1) divisor = divisor/dims[i+1];
    } // for

  } // DataLocation constructor

  ////////////////////////////////////////////////////////////

  /**
   * Translates a data location into an integer index.  If the data
   * location has fractional coordinates, they are rounded prior to
   * calculating the index.
   * 
   * @param dims the dimension lengths along each dimension.
   * 
   * @return a unique index.  If the data coordinate is out of bounds,
   * a -1 is returned.  If the dimensions do not match rank with this
   * location, -1 is returned.  
   */
  public int getIndex (
    int[] dims
  ) {

    // Check bounds
    // ------------
    DataLocation rounded = this.round();
    if (!rounded.isContained (dims))
      return (-1);

    // Calculate index
    // ---------------
    int multiplier = 1;
    int index = 0;
    for (int i = dims.length-1; i >= 0; i--) {
      index += ((int) rounded.coords[i]) * multiplier;
      multiplier *= dims[i];
    } // for    
    return (index);

  } // getIndex

  ////////////////////////////////////////////////////////////

  public String toString () {

    String str = "DataLocation[";
    for (int i = 0; i < coords.length; i++)
      str += Double.toString (coords[i]) + (i < coords.length-1 ? "," : "]");
    return (str);

  } // toString

  ////////////////////////////////////////////////////////////

  /**
   * Transforms this data location in place using an affine transform.  This
   * operation can only be applied to a 2D location.
   *
   * @param affine the affine transform to use.
   */
  public void transformInPlace (
    AffineTransform affine
  ) {

    if (coords.length == 2) {
      affine.transform (coords, 0, coords, 0, 1);
    } // if

  } // transformInPlace

////////////////////////////////////////////////////////////

  /**
   * Transforms this data location using an affine transform.  This
   * operation can only be applied to a 2D location.
   *
   * @param affine the affine transform to use.
   * 
   * @return the transformed data location.  If this location is not
   * 2D, no transformation is performed and a clone of this location
   * is returned.
   */
  public DataLocation transform (
    AffineTransform affine
  ) {

    if (coords.length != 2) return ((DataLocation) this.clone());
    double[] transformed = new double[2];
    affine.transform (coords, 0, transformed, 0, 1);
    return (create (transformed));

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Performs a 2D translation on this data location.
   *
   * @param trans0 the translation for index 0.
   * @param trans1 the translation for index 1.
   *
   * @return the translated data location.  If this location rank is
   * not 2, no operation is performed and a clone of this location is
   * returned.
   */
  public DataLocation translate (
    double trans0,
    double trans1
  ) {

    return (translate (new double[] {trans0, trans1}));

  } // translate

  ////////////////////////////////////////////////////////////

  /**
   * Performs a translation on this data location.
   *
   * @param trans the translation array.  Each component of the
   * translation array is added to the coordinate values.
   * 
   * @return the translated data location.  If the translation array
   * rank does not match this location rank, no operation is
   * performed and a clone of this location is returned.
   */
  public DataLocation translate (
    double[] trans
  ) {

    if (coords.length != trans.length) return ((DataLocation) this.clone());
    double[] translated = (double[]) coords.clone();
    for (int i = 0; i < coords.length; i++)
      translated[i] += trans[i];
    return (create (translated));

  } // translate

  ////////////////////////////////////////////////////////////

  public boolean equals (
    Object o
  ) {

    // Check compatibility
    // -------------------
    if (!(o instanceof DataLocation)) return (false);
    DataLocation loc = (DataLocation) o;
    if (this.coords.length != loc.coords.length) return (false);

    // Check each coordinate
    // ---------------------
    for (int i = 0; i < coords.length; i++)
      if (this.coords[i] != loc.coords[i])
        return (false);

    // Conclude we are equal
    // ---------------------
    return (true);

  } // equals

  ////////////////////////////////////////////////////////////

  /** Constructs a new data location. */
  protected DataLocation () { }

  ////////////////////////////////////////////////////////////

  /** Creates a new data location without cloning. */
  protected static DataLocation create (
    double[] coords
  ) {

    DataLocation created = new DataLocation();
    created.coords = coords;
    return (created);

  } // create

  ////////////////////////////////////////////////////////////

  /** 
   * Checks if this data location is valid. 
   *
   * @return true if the location is valid or false if not.  An invalid 
   * data location is normally used as a flag for a computation that has
   * failed.
   *
   * @see #markInvalid
   */
  public boolean isValid () {

    for (int i = 0; i < coords.length; i++)
      if (Double.isNaN (this.coords[i])) return (false);
    return (true);    

  } // isValid

  ////////////////////////////////////////////////////////////

  /** 
   * Checks if this data location is invalid.
   *
   * @return true if the location is invalid or false if not.  An invalid
   * data location is normally used as a flag for a computation that has
   * failed.
   *
   * @see #markInvalid
   */
  public boolean isInvalid () {

    for (int i = 0; i < coords.length; i++)
      if (Double.isNaN (this.coords[i])) return (true);
    return (false);

  } // isInvalid

  ////////////////////////////////////////////////////////////

  /** 
   * Marks this location as invalid.  Subsequent calls to check
   * for validity will reflect the new state.
   *
   * @see #isValid
   * @see #isInvalid
   */
  public void markInvalid () {
  
    for (int i = 0; i < coords.length; i++)
      this.coords[i] = Double.NaN;
    isHashed = false;
    
  } // markInvalid

  ////////////////////////////////////////////////////////////

  /**
   * Increments this location by the specified stride.  The data
   * location is incremented at only one dimension at a time until the
   * bounds are hit for that dimension, then the next dimension is
   * incremented.  This has the effect of subsampling the locations at
   * the specified stride intervals along each dimension.
   *
   * @param stride the data location stride in each dimension.
   * @param dims the dimension lengths along each dimension.
   *
   * @return true if the increment is successful, false otherwise.
   *
   * @see #increment(int[],DataLocation,DataLocation)
   */
  public boolean increment (
    int[] stride,
    int[] dims
  ) {

    // Find coordinate to increment
    // ----------------------------
    int i = coords.length-1;
    while (i >= 0 && coords[i]+stride[i] > dims[i]-1) {
      coords[i] = 0;
      i--;
    } // while

    // Increment coordinate
    // --------------------
    if (i < 0) return (false);
    coords[i] += stride[i];
    isHashed = false;
    return (true);

  } // increment

  ////////////////////////////////////////////////////////////

  /**
   * Increments this location by the specified stride.  The data
   * location is incremented at only one dimension at a time until the
   * bounds are hit for that dimension, then the next dimension is
   * incremented.  This has the effect of subsampling the locations at
   * the specified stride intervals along each dimension.  The
   * increment takes into account the data location window.
   *
   * @param stride the data location stride in each dimension.
   * @param start the data location window start.
   * @param end the data location window end.
   *
   * @return true if the increment is successful, false otherwise.  
   *
   * @see #increment(int[],int[])
   */
  public boolean increment (
    int[] stride,
    DataLocation start,
    DataLocation end
  ) {

    // Find coordinate to increment
    // ----------------------------
    int i = coords.length-1;
    while (i >= 0 && coords[i]+stride[i] > end.coords[i]) {
      coords[i] = start.coords[i];
      i--;
    } // while

    // Increment coordinate
    // --------------------
    if (i < 0) return (false);
    coords[i] += stride[i];
    isHashed = false;
    return (true);

  } // increment

  ////////////////////////////////////////////////////////////

  public int hashCode () {

    // Get cached hash code
    // --------------------
    if (isHashed) return (hash);

    // Calculate hash code
    // -------------------
    hash = 0;
    for (int i = 0; i < coords.length; i++) {
      long value = Double.doubleToLongBits (coords[i]);
      hash = hash ^ ((int) (value^(value >>> 32)));
    } // for

    // Set flag and return
    // -------------------
    isHashed = true;
    return (hash);

  } // hashCode

  ////////////////////////////////////////////////////////////

} // DataLocation class  

////////////////////////////////////////////////////////////////////////
