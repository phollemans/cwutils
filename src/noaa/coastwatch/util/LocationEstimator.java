////////////////////////////////////////////////////////////////////////
/*
     FILE: LocationEstimator.java
  PURPOSE: A class to perform data location estimation using 
           two Earth transforms.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/13
  CHANGES: 2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/10/07, PFH
           - added documentation
           - modified to be more strict with partially covered partitions
           2006/10/02, PFH, updated for new EarthPartition constructor

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import java.awt.geom.*;
import noaa.coastwatch.util.trans.*;

/**
 * The <code>LocationEstimator</code> class uses a reference and
 * target Earth transform to estimate the data location of a target
 * point given a reference point.  The following diagram shows the two
 * reference frames, with an arrow showing the direction of estimation:
 * <pre>
 *   *----*----*      *----*----*
 *   |        /----------\      |
 *   |       L |      |   o     |
 *   |       o |      |         |   target = f(ref)
 *   | target  |      |  ref    |
 *   | point   |      |  point  |
 *   *----*----*      *----*----*
 * </pre>
 * An Earth partition is used to divide the reference into equal size
 * partitions, upon which a set of polynomials is calculated to
 * estimate locations.  Currently the location estimator class is
 * limited to 2D Earth transforms.
 * 
 * The location lookup operation has two modes: accurate and fast.  In
 * accurate mode, partitions with insufficient data coverage from the
 * target transform are queried by performing the data location
 * transform explicitly.  In fast mode, partitions with insufficient
 * coverage return an invalid data location for any query point.
 *
 * @see EarthTransform
 * @see EarthPartition
 */
public class LocationEstimator {

  // Constants
  // ---------
  /** The fast location query mode. */
  public static final int FAST = 0;

  /** The accurate location query mode. */
  public static final int ACCURATE = 1;

  /** The maximum number of data sampling intervals within one partition. */
  public static final int MAX_INTERVALS = 8;

  // Variables
  // ---------
  /** The reference Earth transform. */
  private EarthTransform refTrans;

  /** The reference Earth transform dimensions. */
  private int[] refDims;
 
  /** The target Earth transform. */
  private EarthTransform targetTrans;

  /** The target Earth transform dimensions. */
  private int[] targetDims;
 
  /** The target navigation transform. */
  private AffineTransform targetNav;

  /** The Earth partition used for estimation. */
  private EarthPartition partition;

  /** The array of child Earth partitions. */
  private EarthPartition[] parts;

  /** The location query mode. */
  private int queryMode;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the location query mode, <code>FAST</code> or
   * <code>ACCURATE</code>.
   */
  public void setQueryMode (int mode) { this.queryMode = mode; }

  ////////////////////////////////////////////////////////////

  /**
   * The partition data class holds information for estimating
   * locations on each partition.
   */
  private class PartitionData {

    // Variables
    // ---------
    /** The row estimator. */
    public Function rowEst;

    /** The column estimator. */
    public Function colEst;

    /** The validity flag, true if the estimators are valid. */
    public boolean valid;

    /** The coverage flag, true if the partition has some coverage. */
    public boolean coverage;

    ////////////////////////////////////////////////////////

    /** Creates a new partition data from the specified parameters. */
    public PartitionData (
      Function rowEst,
      Function colEst,
      boolean valid,
      boolean coverage
    ) {

      this.rowEst = rowEst;
      this.colEst = colEst;
      this.valid = valid;
      this.coverage = coverage;

    } // PartitionData constructor
 
    ////////////////////////////////////////////////////////

  } // PartitionData class

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of target locations for the specified reference
   * locations.
   *
   * @param the list of reference locations.  The list is modified
   * by removing any entries which did not transform to a valid target
   * location.
   *
   * @param the list of valid target locations.
   */
  private List getTargets (
    List refLocs
  ) {

    // Initialize target locations
    // ---------------------------
    List targetLocs = new ArrayList();

    // Loop over each reference location
    // ---------------------------------
    for (int i = 0; i < refLocs.size(); i++) {

      // Get target location
      // -------------------
      DataLocation targetLoc = targetTrans.transform (
        refTrans.transform ((DataLocation) refLocs.get(i)));
       if (targetNav != null) targetLoc = targetLoc.transform (targetNav);

      // Add target location to list
      // ---------------------------
      if (targetLoc.isValid())
        targetLocs.add (targetLoc);

      // Remove invalid reference location
      // ---------------------------------
      else {
        refLocs.remove (i);
        i--;
      } // else

    } // for

    return (targetLocs);

  } // getTargets

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of reference locations.
   *
   * @param the minimum reference location.
   * @param the maximum reference location.
   * @param intervals the number of intervals in each dimension.
   *
   * @return a list of reference locations.
   */
  private List getReferences (
    DataLocation min,
    DataLocation max,
    int intervals
  ) {

    // Create reference arrays
    // -----------------------
    double[] rows = new double[intervals+1];
    double[] cols = new double[intervals+1];
    for (int i = 0; i <= intervals; i++) {
      double s = (double) i / intervals;
      rows[i] = min.get(Grid.ROWS)*(1 - s) + max.get(Grid.ROWS)*s;
      cols[i] = min.get(Grid.COLS)*(1 - s) + max.get(Grid.COLS)*s;
    } // for

    // Create location list
    // --------------------
    List refLocs = new LinkedList();
    for (int i = 0; i <= intervals; i++) {
      for (int j = 0; j <= intervals; j++) {
        refLocs.add (new DataLocation (rows[i], cols[j]));
      } // for
    } // for

    return (refLocs);

  } // getReferences

  ////////////////////////////////////////////////////////////

  /**
   * Creates a set of row and column estimators for each child partition.
   *
   * @param targetNav the target navigation transform.  If not null,
   * this transformation is used to correct coordinate locations in
   * the target transform prior to calculating the polynomial
   * estimators.
   */
  private void createEstimators () {

    // Loop over each child partition
    // ------------------------------
    for (int k = 0; k < parts.length; k++) {

      // Get location bounds
      // -------------------
      DataLocation min = parts[k].getMin();
      DataLocation max = parts[k].getMax();

      // Loop until we have enough data
      // ------------------------------
      List refLocs = null;
      List targetLocs = null;
      int targets = 0;
      for (int intervals = 2; intervals <= MAX_INTERVALS; intervals *= 2) {
  
        // Get target location data
        // ------------------------
        refLocs = getReferences (min, max, intervals);
        targetLocs = getTargets (refLocs);

        // Stop if insufficient coverage
        // -----------------------------
        targets = targetLocs.size();
        if (intervals == 2 && targets < 6) break;

        // Stop if sufficient coverage
        // ---------------------------
        if (targets >= 9) break;

      } // for

      // Create valid partition
      // ----------------------
      if (targets >= 9) {

        // Get reference data
        // ------------------
        double[] x = new double[targets];
        double[] y = new double[targets];
        for (int i = 0; i < targets; i++) {
          DataLocation refLoc = (DataLocation) refLocs.get(i);
          x[i] = refLoc.get(Grid.ROWS);
          y[i] = refLoc.get(Grid.COLS);
        } // for

        // Get target data
        // ---------------
        double[] fRow = new double[targets];
        double[] fCol = new double[targets];
        for (int i = 0; i < targets; i++) {
          DataLocation targetLoc = (DataLocation) targetLocs.get(i);
          fRow[i] = targetLoc.get(Grid.ROWS);
          fCol[i] = targetLoc.get(Grid.COLS);
        } // for

        // Create estimator functions
        // --------------------------
        PartitionData data;
        try {
          Function rowEst = new BivariateEstimator (x, y, fRow, 2); 
          Function colEst = new BivariateEstimator (x, y, fCol, 2);
          data = new PartitionData (rowEst, colEst, true, true);
        } // try
        catch (RuntimeException e) {
          data = new PartitionData (null, null, false, true);
        } // catch
        parts[k].setData (data);

      } // if

      // Create partial partition
      // ------------------------
      else if (targets > 0) {
        parts[k].setData (new PartitionData (null, null, false, true));
      } // else if

      // Create empty partition
      // ----------------------
      else {
        parts[k].setData (new PartitionData (null, null, false, false));
      } // else

    } // for

  } // createEstimators

  ////////////////////////////////////////////////////////////

  /**
   * Creates a location estimator based on the specified Earth
   * transforms.  By default, the query mode is set to
   * <code>ACCURATE</code>.
   * 
   * @param refTrans the reference Earth transform.  This is the
   * transform against which data location queries will be made.
   * @param refDims the reference dimensions.  Queries are limited to
   * locations within these dimensions.
   * @param targetTrans the target Earth transform.  This is the
   * transform for which queries will return a data location.
   * @param targetDims the target dimensions.  Target points outside
   * these dimensions are considered out of bounds.
   * @param targetNav the target navigation transform.  If not null,
   * this transformation is used to correct coordinate locations in
   * the target transform prior to calculating the polynomial
   * estimators.
   * @param size the estimation polynomial size in kilometers.
   *
   * @throws UnsupportedOperationException if the dimension rank is not
   * supported.
   */
  public LocationEstimator (
    EarthTransform refTrans,
    int[] refDims,
    EarthTransform targetTrans,
    int[] targetDims,
    AffineTransform targetNav,
    double size
  ) throws UnsupportedOperationException {

    // Check rank
    // ----------
    if (refDims.length != 2 || targetDims.length != 2)
      throw new UnsupportedOperationException ("Unsupported dimension rank");

    // Initialize
    // ----------
    this.refTrans = refTrans;
    this.refDims = (int[]) refDims.clone();
    this.targetTrans = targetTrans;
    this.targetDims = (int[]) targetDims.clone();
    this.targetNav = (targetNav != null ? (AffineTransform) targetNav.clone() :
      null);
    this.queryMode = ACCURATE;

    // Create partition
    // ----------------
    DataLocation min = new DataLocation (0, 0);
    DataLocation max = new DataLocation (refDims[Grid.ROWS], 
      refDims[Grid.COLS]);
    partition = new EarthPartition (refTrans, min, max, size, 
      new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE});
    parts = partition.getPartitions();

    // Create estimators
    // -----------------
    createEstimators();

  } // LocationEstimator constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the target location for the specified reference location.
   * 
   * @param refLoc the reference location.   
   *
   * @return the target location.  The target location may be invalid
   * if an error occurred in calculation or if the specified reference
   * location has no valid target location.
   */
  public DataLocation getLocation (
    DataLocation refLoc
  ) {

    // Get child partition
    // -------------------
    EarthPartition part = partition.findPartition (refLoc);
    if (part == null) {
      return (new DataLocation (Double.NaN, Double.NaN));
    } // if

    // Get partition data
    // ------------------
    PartitionData data = (PartitionData) part.getData();

    // Handle invalid partition
    // ------------------------
    if (!data.valid) {

      // No valid location
      // -----------------
      if (!data.coverage || queryMode == FAST)
        return (new DataLocation (Double.NaN, Double.NaN));

      // Perform explicit location transform
      // -----------------------------------
      DataLocation targetLoc = targetTrans.transform (refTrans.transform (
        refLoc));   
      if (targetNav != null) targetLoc = targetLoc.transform (targetNav);
      return (targetLoc);

    } // if

    // Perform estimated location transform
    // ------------------------------------
    else {
      double[] refCoords = refLoc.getCoords();
      DataLocation targetLoc = new DataLocation (
        data.rowEst.evaluate(refCoords),
        data.colEst.evaluate(refCoords)
      );
      return (targetLoc);
    } // else

  } // getLocation

  ////////////////////////////////////////////////////////////

} // LocationEstimator class

////////////////////////////////////////////////////////////////////////
