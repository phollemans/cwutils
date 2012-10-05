////////////////////////////////////////////////////////////////////////
/*
     FILE: VariableEstimator.java
  PURPOSE: To estimate the value of a spatially smooth variable value.
   AUTHOR: Peter Hollemans
     DATE: 2002/06/03
  CHANGES: 2002/07/25, PFH, converted to location classes
           2002/09/13, PFH, added filter methods
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2006/10/02, PFH, modified to handle missing data values
           2007/04/10, PFH, corrected documentation of getEncoding()

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
import java.lang.reflect.*;
import noaa.coastwatch.util.trans.*;

/**
 * The <code>VariableEstimator</code> class provides an
 * approximation of smoothly changing variable data using
 * quadratic polynomials.  Currently only 1D and 2D data
 * variables are supported.  The estimator uses an {@link
 * EarthPartition} object and derives the polynomials
 * individually on each partition.  If the polynomials cannot be
 * derived for a given partition (due to small partition size or
 * missing data values), a <code>Double.NaN</code> value is
 * returned by {@link #getValue} for any location in the affected
 * partition.
 */
public class VariableEstimator
  implements Encodable, ValueSource {

  // Variables
  // ---------
  /** Earth partition used for estimation. */
  private EarthPartition partition;

  /** Array of child partitions. */
  private EarthPartition[] parts;

  /** Function index used for shared Earth partitions. */
  private int shareIndex;

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new set of estimators to the list for the specified
   * variable.
   * 
   * @param var the variable data values to add.  The variable must have
   * the same dimensions as the base estimator.
   * @param filter the data filtering function.  The filter is called
   * with the data value array for each partition prior to creating
   * the estimator object.  If the filter is null, no filtering is
   * performed.
   */
  private void addVariable (
    DataVariable var,
    Filter filter
  ) {

    // Loop over each child partition
    // ------------------------------
    int[] dims = var.getDimensions ();
    for (int k = 0; k < parts.length; k++) {

      // Get estimator list
      // ------------------
      List list = (List) parts[k].getData();

      // Get data coordinate bounds
      // --------------------------
      DataLocation min = parts[k].getMin();
      DataLocation max = parts[k].getMax();

      // Initialize function
      // -------------------
      Function func = null;

      // Create univariate estimator
      // ---------------------------
      estimator: if (dims.length == 1) {
        
        // Get coordinates
        // ---------------
        double[] x = new double[3];
        x[0] = Math.round (min.get(0));
        x[1] = Math.round ((min.get(0) + max.get(0)) / 2);
        x[2] = Math.round (max.get(0));
        if (x[0] == x[1] || x[1] == x[2]) break estimator;

        // Create estimator
        // ----------------
        double[] f = new double[x.length];
        for (int i = 0; i < x.length; i++) {
          f[i] = var.getValue (new DataLocation (x[i]));
          if (Double.isNaN (f[i])) break estimator;
        } // for
        if (filter != null) filter.filter (f);
        func = new UnivariateEstimator (x, f, 2);

      } // if

      // Create bivariate estimator
      // --------------------------
      else if (dims.length == 2) {

        // Get x coordinates
        // -----------------
        double[] x = new double[9];
        x[0] = x[3] = x[6] = Math.round (min.get(0));
        x[1] = x[4] = x[7] = Math.round ((min.get(0) + max.get(0)) / 2);
        x[2] = x[5] = x[8] = Math.round (max.get(0));
        if (x[0] == x[1] || x[1] == x[2]) break estimator;

        // Get y coordinates
        // -----------------
        double[] y = new double[9];
        y[0] = y[1] = y[2] = Math.round (min.get(1));
        y[3] = y[4] = y[5] = Math.round ((min.get(1) + max.get(1)) / 2);
        y[6] = y[7] = y[8] = Math.round (max.get(1));
        if (y[0] == y[3] || y[3] == y[6]) break estimator;

        // Create estimator
        // ----------------
        double[] f = new double[x.length];
        for (int i = 0; i < x.length; i++) {
          f[i] = var.getValue (new DataLocation (x[i], y[i]));
          if (Double.isNaN (f[i])) break estimator;
        } // for
        if (filter != null) filter.filter (f);
        func = new BivariateEstimator (x, y, f, 2);

      } // else if

      // Add function to list
      // --------------------
      list.add (func);

    } // for

  } // addVariable

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a variable estimator for the specified variable using
   * the same Earth transform and partitioning as the specified
   * variable estimator.
   * 
   * @param var the data variable for estimation.
   * @param est the variable estimator to use for Earth partitioning
   * information.
   */
  public VariableEstimator (
    DataVariable var,
    VariableEstimator est
  ) {

    this (var, null, est);

  } // VariableEstimator constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a variable estimator for the specified variable and
   * filter using the same Earth transform and partitioning as the
   * specified variable estimator.
   * 
   * @param var the data variable for estimation.
   * @param filter the data filtering function.  The filter is called
   * with the data value array for each partition prior to creating
   * the estimator object.
   * @param est the variable estimator to use for Earth partitioning
   * information.
   */
  public VariableEstimator (
    DataVariable var,
    Filter filter,
    VariableEstimator est
  ) {

    // Initialize variables
    // --------------------
    partition = est.partition;
    parts = est.parts;
    shareIndex = est.shareIndex + 1;
    est.addVariable (var, filter);

  } // VariableEstimator constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a variable estimator for the specified variable
   * and Earth transform.
   * 
   * @param var the data variable for estimation.
   * @param trans the Earth transform to use for partitioning.
   * @param maxSize the maximum polynomial partition size in
   * kilometers.
   * @param maxDims the maximum partition size in any dimension
   * in terms of data locations.
   *
   * @throws UnsupportedOperationException if the variable rank is not
   * supported.
   *
   * @see EarthPartition
   */
  public VariableEstimator (
    DataVariable var,
    EarthTransform trans,
    double maxSize,
    int[] maxDims
  ) throws UnsupportedOperationException {

    this (var, null, trans, maxSize, maxDims);

  } // VariableEstimator constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a variable estimator for the specified variable,
   * filter, and Earth transform.
   * 
   * @param var the data variable for estimation.
   * @param filter the data filtering function.  The filter is called
   * with the data value array for each partition prior to creating
   * the estimator object.
   * @param trans the Earth transform to use for partitioning.
   * @param maxSize the maximum polynomial partition size in
   * kilometers.
   * @param maxDims the maximum partition size in any dimension
   * in terms of data locations.
   *
   * @throws UnsupportedOperationException if the variable rank is not
   * supported.
   *
   * @see EarthPartition
   */
  public VariableEstimator (
    DataVariable var,
    Filter filter,
    EarthTransform trans,
    double maxSize,
    int[] maxDims
  ) throws UnsupportedOperationException {

    // Check variable dims
    // -------------------
    int[] dims = var.getDimensions();
    if (dims.length != 1 && dims.length != 2)
      throw new UnsupportedOperationException ("Unsupported variable rank");

    // Create partition
    // ----------------
    DataLocation min = new DataLocation (dims.length);
    DataLocation max = new DataLocation (dims.length);
    for (int i = 0; i < dims.length; i++)
      max.set (i, dims[i]-1);
    partition = new EarthPartition (trans, min, max, maxSize, maxDims);
    parts = partition.getPartitions();

    // Add empty estimators
    // --------------------
    for (int k = 0; k < parts.length; k++)
      parts[k].setData (new ArrayList());
    shareIndex = 0;

    // Add variable
    // ------------
    addVariable (var, filter);

  } // VariableEstimator constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a variable estimator from the specified encoding.
   * The encoding must be a valid encoding of the estimator as
   * created by <code>getEncoding</code>.
   * 
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public VariableEstimator (
    Object obj
  ) {

    useEncoding (obj);

  } // VariableEstimator constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a variable estimator from the specified encoding and
   * the same Earth transform and partitioning as the specified
   * variable estimator.
   * 
   * @param obj the variable estimator encoding.
   * @param est the variable estimator to use for Earth partitioning
   * information.
   */
  public VariableEstimator (
    Object obj,
    VariableEstimator est
  ) {

    // Initialize variables
    // --------------------
    partition = est.partition;
    parts = est.parts;
    shareIndex = est.shareIndex + 1;
    List coefs = (List) Array.get (obj, 2);
    est.useVariableEncoding (coefs);

  } // VariableEstimator constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets an estimated variable value.  The value is calculated using
   * the polynomial approximation function.
   *
   * @param loc the data value location.
   *
   * @return the estimated value, or Double.NaN if no value could be
   * estimated.
   */
  public double getValue (
    DataLocation loc
  ) {

    // Check containment
    // -----------------
    EarthPartition part = partition.findPartition (loc);
    if (part == null) return (Double.NaN);

    // Get function and evaluate
    // -------------------------
    Function f = (Function) ((List) part.getData()).get (shareIndex);
    if (f == null) return (Double.NaN);
    else return (f.evaluate (loc.getCoords()));

  } // getValue

  ////////////////////////////////////////////////////////////

  /**
   * Gets an encoded representation of this variable estimator. The
   * encoding may later be used to recreate the estimator without
   * using the original variable data.
   *
   * @return the object encoding.  The encoding object is an
   * <code>Object[3]</code> array containing:
   * <ul>
   *   <li>a <code>BitSet</code> object used for encoding partition
   *   structure information</li>
   *   <li>a <code>List</code> of <code>double[]</code> coordinates
   *   specifying partition boundaries as min and max (2 arrays per
   *   partition)</li>
   *   <li>a <code>List</code> of <code>double[]</code> arrays specifying 
   *   partition estimator coefficients</li>
   * </ul>
   *
   * @see #useEncoding
   */
  public Object getEncoding () {

    // Get Earth partition encoding
    // ----------------------------
    Object[] obj = (Object[]) partition.getEncoding();

    // Create coefficient list
    // -----------------------
    List coefs = new ArrayList();
    for (int i = 0; i < parts.length; i++) {
      Function f = (Function) ((List) parts[i].getData()).get (shareIndex);
      if (f == null) coefs.add (null);
      else coefs.add (f.getEncoding());
    } // for    

    // Return encoding
    // ---------------
    obj[2] = coefs;
    return (obj);

  } // getEncoding

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new set of estimators to the list using the specified
   * encoding.
   * 
   * @param coefs the list of coefficients to add.
   */
  private void useVariableEncoding (
    List coefs
  ) {

    // Loop over each child partition
    // ------------------------------
    for (int k = 0; k < parts.length; k++) {

      // Add to estimator list
      // ---------------------
      List list = (List) parts[k].getData();
      double[] coefsArray = (double[]) coefs.get (k);
      if (coefsArray == null)
        list.add (null);
      else if (coefsArray.length == 3)
        list.add (new UnivariateEstimator (coefsArray));
      else if (coefsArray.length == 9)
        list.add (new BivariateEstimator (coefsArray));
      else
        list.add (null);

    } // for

  } // useVariableEncoding

  ////////////////////////////////////////////////////////////

  /**
   * Constructs an estimator from the specified encoding.
   * The encoding must be a valid encoding of an estimator as created
   * by <code>getEncoding</code>.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public void useEncoding (
    Object obj
  ) {

    // Create data list
    // ----------------
    List data = new ArrayList();
    int length = ((List) Array.get (obj, 1)).size () / 2;
    for (int i = 0; i < length; i++)
      data.add (null);
    List coefs = (List) Array.get (obj, 2);
    Array.set (obj, 2, data);

    // Create Earth partition
    // ----------------------
    partition = new EarthPartition (obj);
    parts = partition.getPartitions ();

    // Add empty estimators
    // --------------------
    for (int k = 0; k < parts.length; k++)
      parts[k].setData (new ArrayList());
    shareIndex = 0;

    // Add variable estimators
    // -----------------------
    useVariableEncoding (coefs);

  } // useEncoding

  ////////////////////////////////////////////////////////////

} // VariableEstimator class

////////////////////////////////////////////////////////////////////////
