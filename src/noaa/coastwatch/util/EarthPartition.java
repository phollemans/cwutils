////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthPartition.java
  PURPOSE: To allow Earth-locatable data to be divided up into sections.
   AUTHOR: Peter Hollemans
     DATE: 2002/06/01
  CHANGES: 2002/07/25, PFH, converted to location classes
           2002/11/18, PFH, added last found partition
           2002/12/20, PFH, added check for invalid partition bounds
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/09/13, PFH, added test for valid resolution in constructor
           2005/09/19, PFH, added extra info in constructor exception
           2006/10/02, PFH, added new algorithm for missing location values
           2007/09/14, PFH, added new partition size function
           2007/12/14, PFH, added new constructor with isRoot flag

  CoastWatch Software Library and Utilities
  Copyright 2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Encodable;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>EarthPartition</code> class sets up a partitioning of
 * earth data such that no individual partition has physical size
 * exceeding a user-specified tolerance.  The physical size along each
 * dimension is measured in terms of the earth transform
 * <code>distance()</code> metric.  After construction, child
 * partitions may be retrieved and manipulated.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class EarthPartition
  implements Encodable {

  // Constants
  // ---------
  /* The index of the left child. */
  private static final int LEFT = 0;

  /* The index of the right child. */
  private static final int RIGHT = 1;

  // Variables
  // ---------
  /** Partition minimum coordinates. */
  private DataLocation min;

  /** Partition maximum coordinates. */
  private DataLocation max;

  /** Child partitions. */
  private EarthPartition[] children;

  /** Partition data. */
  private Object data;

  /** The last partition found. */
  private EarthPartition lastFound;

  ////////////////////////////////////////////////////////////

  // Simple functions
  // ----------------
  /** Gets the partition data. */
  public Object getData () { return (data); }

  /** Sets the partition data. */
  public void setData (Object data) { this.data = data; }

  /** Gets the partition minimum bounds. */
  public DataLocation getMin () { return ((DataLocation) min.clone()); }

  /** Gets the partition maximum bounds. */
  public DataLocation getMax () { return ((DataLocation) max.clone()); }

  /** Constructs an empty earth partition. */
  private EarthPartition () { }

  ////////////////////////////////////////////////////////////

  /**
   * Gets a partition size along a given dimension.
   *
   * @param min the partition minimum corner.
   * @param max the partition maximum corner.
   * @param dim the dimension along which to measure.
   * @param trans the earth transform for data to earth locations.
   * @param isRoot the root flag, true if this partition is the
   * root of a tree or false if not.
   *
   * @return the partition size in kilometers along the specified
   * dimension.
   */
  private double getPartitionSize (
    DataLocation min,
    DataLocation max,
    int dim,
    EarthTransform trans,
    boolean isRoot
  ) {

    // Compute using edge distance
    // ---------------------------
    double dataSize = max.get (dim) - min.get (dim);
    double size;
    if (isRoot && dataSize > 10) {
      DataLocation[] locations = new DataLocation[5];
      locations[0] = min;
      locations[1] = (DataLocation) min.clone();
      locations[1].set (dim, min.get (dim) + dataSize*0.25);
      locations[2] = (DataLocation) min.clone();
      locations[2].set (dim, min.get (dim) + dataSize*0.5);
      locations[3] = (DataLocation) min.clone();
      locations[3].set (dim, min.get (dim) + dataSize*0.75);
      locations[4] = max;
      size = 0;
      for (int i = 0; i < 4; i++) 
        size += trans.distance (locations[i], locations[i+1]);
    } // if
    else {
      DataLocation dimMax = (DataLocation) min.clone();
      dimMax.set (dim, max.get (dim));
      size = trans.distance (min, dimMax);
    } // else
    if (!Double.isNaN (size)) return (size);

    // Compute using endpoint resolution
    // ---------------------------------
    boolean minValid = trans.transform (min).isValid();
    boolean maxValid = trans.transform (max).isValid();
    if (minValid || maxValid) {
      DataLocation dataLoc = (minValid ? min : max);
      double[] res = trans.getResolution (dataLoc);
      size = res[dim]*dataSize;
      return (size);
    } // else if

    return (Double.NaN);

  } // getPartitionSize

  ////////////////////////////////////////////////////////////

  /**
   * Constructs an earth partitioning from the specified earth
   * transform, extents, and size tolerance.  The partition is
   * assumed to be the root, in which case a check is performed
   * for an earth wrapping swath.
   *
   * @param trans the earth transform to use. 
   * @param min the starting data location for partitioning.
   * @param max the ending data location for partitioning.
   * @param maxSize the maximum partition size in any dimension
   * in terms of the {@link
   * noaa.coastwatch.util.trans.EarthTransform#distance} metric.
   * @param maxDims the maximum partition size in any dimension
   * in terms of data locations.
   */
  public EarthPartition (
    EarthTransform trans,
    DataLocation min,
    DataLocation max,
    double maxSize,
    int[] maxDims
  ) {

    this (trans, min, max, maxSize, maxDims, true);

  } // EarthPartition constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs an earth partitioning from the specified earth
   * transform, extents, and size tolerance.
   *
   * @param trans the earth transform to use. 
   * @param min the starting data location for partitioning.
   * @param max the ending data location for partitioning.
   * @param maxSize the maximum partition size in any dimension
   * in terms of the {@link
   * noaa.coastwatch.util.trans.EarthTransform#distance} metric.
   * @param maxDims the maximum partition size in any dimension
   * in terms of data locations.
   * @param isRoot the root flag, true if this partition is the
   * root of a tree or false if not.
   */
  protected EarthPartition (
    EarthTransform trans,
    DataLocation min,
    DataLocation max,
    double maxSize,
    int[] maxDims,
    boolean isRoot
  ) {

    // Initialize variables
    // --------------------
    this.min = (DataLocation) min.clone();
    this.max = (DataLocation) max.clone();
    children = null;
    data = null;
    lastFound = null;

    // Create child partitions
    // -----------------------
    int rank = min.getRank();
    for (int i = 0; i < rank && children == null; i++) { 

      /**
       * This is the existing algorithm which doesn't handle missing
       * data particularly well.
       */

      // Check for degenerate partition
      // ------------------------------
      if (Math.abs (min.get(i) - max.get(i)) < 1) {
        throw new RuntimeException ("Degenerate partition detected between " + 
          min + " and " + max);
      } // if

      // Get partition size
      // ------------------
      double partSize = getPartitionSize (min, max, i, trans, isRoot);
      if (Double.isNaN (partSize)) {
        throw new RuntimeException ("Cannot determine partition size between "+
          min + " and " + max);
      } // if

      // Create bipartition
      // ------------------
      if (partSize > maxSize) {

        // Create first partition
        // ----------------------   
        children = new EarthPartition[2];
        double center = (min.get(i) + max.get(i)) / 2;
        DataLocation newMax = (DataLocation) max.clone();
        newMax.set (i, center);
        children[LEFT] = new EarthPartition (trans, min, newMax, maxSize, 
          maxDims, false);

        // Create second partition
        // -----------------------   
        DataLocation newMin = (DataLocation) min.clone();
        newMin.set (i, center);
        children[RIGHT] = new EarthPartition (trans, newMin, max, maxSize, 
          maxDims, false);

      } // if

      /**
       * This is the proposed new algorithm.

      // Check for degenerate partition
      // ------------------------------
      double dim = Math.abs (min.get(i) - max.get(i));
      if (dim < 1) {
        return;
      } // if

      // Initialize partition variables
      // ------------------------------
      boolean doPartition = false;
      double partitionLoc = (min.get(i) + max.get(i)) / 2;

      // Partition based on dimensions
      // -----------------------------
      if (dim > maxDims[i]) {
        doPartition = true;
      } // if

      // Partition based on physical size
      // --------------------------------
      else {

        // Get max data location along ith dimension
        // -----------------------------------------
        DataLocation dimMax = (DataLocation) min.clone();
        dimMax.set (i, max.get(i));

        // Get earth locations
        // -------------------
        EarthLocation minLoc = trans.transform (min);
        EarthLocation maxLoc = trans.transform (dimMax);
        boolean minValid = minLoc.isValid();
        boolean maxValid = maxLoc.isValid();

        // Partition based on size
        // -----------------------
        if (minValid && maxValid) {
          doPartition = (minLoc.distance (maxLoc) > maxSize);
        } // if

        // Partition based on nearest valid minimum location
        // -------------------------------------------------
        else if (!minValid) {
          DataLocation loc = (DataLocation) min.clone();
          while (!trans.transform (loc).isValid()) {
            doPartition = (loc.get (i) < dimMax.get (i));
            if (!doPartition) break;
            loc.set (i, loc.get (i) + 1);
          } // while
          if (doPartition) partitionLoc = loc.get (i) - 0.5;
        } // else if

        // Partition based on nearest valid maximum location
        // -------------------------------------------------
        else {
          DataLocation loc = (DataLocation) dimMax.clone();
          while (!trans.transform (loc).isValid()) {
            doPartition = (loc.get (i) > min.get (i));
            if (!doPartition) break;
            loc.set (i, loc.get (i) - 1);
          } // while
          if (doPartition) partitionLoc = loc.get (i) + 0.5;
        } // else if

      } // else

      // Create partition
      // ----------------
      if (doPartition) {

        // Create first partition
        // ----------------------   
        children = new EarthPartition[2];
        DataLocation newMax = (DataLocation) max.clone();
        newMax.set (i, Math.floor (partitionLoc));
        children[LEFT] = new EarthPartition (trans, min, newMax, maxSize, 
          maxDims);

        // Create second partition
        // -----------------------   
        DataLocation newMin = (DataLocation) min.clone();
        newMin.set (i, Math.ceil (partitionLoc));
        children[RIGHT] = new EarthPartition (trans, newMin, max, maxSize, 
          maxDims);

      } // if

      */

    } // for

  } // EarthPartition constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs an earth partitioning from the specified encoding.
   * The encoding must be a valid encoding of a partitioning as
   * created by <code>getEncoding</code>.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public EarthPartition (
    Object obj
  ) {

    useEncoding (obj);

  } // EarthPartition constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs an earth partitioning from the specified encoding.
   * The encoding must be a valid encoding of a partitioning as
   * created by <code>getEncoding</code>.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public void useEncoding (
    Object obj
  ) {

    // Get encoding objects
    // --------------------
    BitSet bits = (BitSet) Array.get (obj, 0);
    List coords = (List) Array.get (obj, 1);
    List data = (List) Array.get (obj, 2);

    // Create paritioning
    // ------------------
    preorderDecoding (bits, coords, data, 0);

  } // useEncoding

  ////////////////////////////////////////////////////////////

  /**
   * Creates a partitioning using a preorder traversal encoding
   * of the tree.
   *
   * @param bits the bitset object used for encoding partition
   * structure information.
   * @param coords the list of double[] coordinates specifying
   * partition boundaries.
   * @param data the data list specifying partition data.
   * @param index offset into the encoding to start.
   * 
   * @param return the index after decoding.
   */
  private int preorderDecoding (
    BitSet bits,
    List coords,
    List data,
    int index
  ) {

    // Check index
    // -----------
    if (index > data.size()-1) return (index);

    // Set variables
    // -------------
    min = new DataLocation ((double[]) coords.get (2*index));
    max = new DataLocation ((double[]) coords.get (2*index + 1));
    this.data = data.get (index);
    lastFound = null;

    // Check end of encoding
    // ---------------------
    children = null;
    if (index > bits.length()-1 || bits.get (index))
      return (index);

    // Create children
    // ---------------
    children = new EarthPartition[2];
    children[LEFT] = new EarthPartition ();  
    index = children[LEFT].preorderDecoding (bits, coords, data, index+1);
    children[RIGHT] = new EarthPartition ();  
    index = children[RIGHT].preorderDecoding (bits, coords, data, index+1);
    return (index);

  } // preorderDecoding

  ////////////////////////////////////////////////////////////

  /**
   * Gets the number of child partitions.
   */
  public int partitions () {
 
    if (children == null) 
      return (0);
    else
      return (children[LEFT].partitions() + children[RIGHT].partitions());

  } // partitions

  ////////////////////////////////////////////////////////////

  /**
   * Gets the total size of the tree.
   */
  private int size () {
 
    if (children == null) 
      return (1);
    else
      return (children[LEFT].size() + children[RIGHT].size());

  } // size

  ////////////////////////////////////////////////////////////

  /**
   * Creates an encoding of the partition information.  The encoding
   * may later be used to recreate the partitioning without using the
   * original earth transform data.
   *
   * @return obj the object encoding.  The encoding object is an
   * <code>Object[3]</code> array containing:
   * <ul>
   *   <li>a <code>BitSet</code> object used for encoding partition
   *   structure information</li>
   *   <li>a <code>List</code> of <code>double[]</code> coordinates
   *   specifying partition boundaries</li>
   *   <li>a <code>List</code> of <code>Object</code> specifying partition
   *   data</li>
   * </ul>
   *
   * @see #useEncoding
   */
  public Object getEncoding () {

    // Create encoding structures
    // --------------------------
    BitSet bits = new BitSet();
    List coords = new ArrayList();
    List data = new ArrayList();
    Object obj = new Object[] {bits, coords, data};

    // Perform encoding
    // ----------------
    preorderEncoding (bits, coords, data, 0);
    return (obj);

  } // getEncoding

  ////////////////////////////////////////////////////////////

  /**
   * Creates a preorder traversal encoding of the tree in the 
   * specified bitset object, coordinate list, and data list.
   *
   * @param bits the bitset object to use for encoding.  If the tree is
   * a leaf, no bit will be set.  If the tree has children, a false
   * will be used to signify a left child traversal and a true for a
   * right child traversal.  The encoding will begin at the end of the
   * bitset.
   * @param coords the coordinate list.  Partition bounds will be
   * stored in individual elements of the list as min, then max.
   * @param data the data list.  Partition data will be stored in
   * individual elements of the list.
   * @param index offset into the encoding to start.
   * 
   * @param return the index after encoding.
   */
  private int preorderEncoding (
    BitSet bits,
    List coords,
    List data,
    int index
  ) {

    // Add bounds and data
    // -------------------
    coords.add (min.getCoords());
    coords.add (max.getCoords());
    data.add (this.data);

    // Perform encoding of children
    // ----------------------------
    if (children != null) {
      bits.set (index, false);
      index = children[LEFT].preorderEncoding (bits, coords, data, index+1);
      bits.set (index, true);
      index = children[RIGHT].preorderEncoding (bits, coords, data, index+1);
    } // if
    return (index);

  } // preorderEncoding

  ////////////////////////////////////////////////////////////

  /**
   * Gets all child partitions as an array.
   *
   * @return the child partition array.
   */
  public EarthPartition[] getPartitions () {
    
    // Perform preorder traversal
    // --------------------------
    List list = new ArrayList();
    preorderLeaf (list);
    
    // Convert to array
    // ----------------
    Object[] array = list.toArray();
    EarthPartition[] parts = new EarthPartition[array.length];
    System.arraycopy (array, 0, parts, 0, array.length); 
    return (parts);

  } // getPartitions

  ////////////////////////////////////////////////////////////

  /**
   * Determines if the partition contains the specified data coordinate.
   *
   * @param loc the data location to check.
   * 
   * @return true if the partition contains the data location, or
   * false otherwise.
   */
  public boolean contains (
    DataLocation loc
  ) {

    return (loc.isContained (min, max));

  } // contains

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the partition containing the specified data location.
   *
   * @param loc the data location for searching.
   * 
   * @return the earth partition containing the data location, or
   * <code>null</code> if one cannot be found.
   */
  public EarthPartition findPartition (
    DataLocation loc
  ) { 

    // Check if contained locally
    // --------------------------
    if (!contains (loc)) return (null);
    if (children == null) return (this);

    // Check if contained in children
    // ------------------------------
    if (lastFound != null && lastFound.contains (loc))
      return (lastFound);
    EarthPartition part = children[LEFT].findPartition (loc);
    if (part == null) part = children[RIGHT].findPartition (loc);
    lastFound = part;
    return (part);

  } // findPartition

  ////////////////////////////////////////////////////////////

  /** 
   * Performs a preorder traversal of the partitions, adding any
   * childless (leaf) partitions to the specified list.
   *
   * @param list the list of childless partitions.
   */ 
  private void preorderLeaf (
    List list
  ) { 

    if (children == null)
      list.add (this);
    else {
      children[LEFT].preorderLeaf (list);
      children[RIGHT].preorderLeaf (list);
    } // else

  } // preorder

  ////////////////////////////////////////////////////////////

} // EarthPartition class

////////////////////////////////////////////////////////////////////////
