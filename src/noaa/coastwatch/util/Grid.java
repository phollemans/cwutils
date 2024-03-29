////////////////////////////////////////////////////////////////////////
/*

     File: Grid.java
   Author: Peter Hollemans
     Date: 2002/04/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util;

// Imports
// -------
import java.awt.geom.AffineTransform;
import java.lang.reflect.Array;
import java.text.NumberFormat;
import java.util.Map;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.io.tile.TilingScheme;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The 2D grid class is a special form of data variable with
 * two dimensions -- rows and columns.  The grid is associated with a
 * navigation correction in the form of an affine transform.  The
 * affine transform is used to translate between "desired" data
 * coordinates and "actual" data coordinates.  Suppose that the data
 * value at row, column coordinate <code>(r,c)</code> is desired.
 * Then the actual coordinate in the variable data array is
 * calculated as <code>(r',c') = affine ((r,c))</code> where
 * <code>affine()</code> is the application of the affine transform to
 * the data coordinate.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
@noaa.coastwatch.test.Testable
public class Grid
  extends DataVariable {

  // Constants
  // ---------

  /** Index of rows dimension. */
  public final static int ROWS = 0;

  /** Index of columns dimension. */
  public final static int COLS = 1;

  /** Index of rows dimension (singular). */
  public final static int ROW = 0;

  /** Index of columns dimension (singular). */
  public final static int COL = 1;

  // Variables
  // ---------
  /** Affine transform used for navigation correction. */
  private AffineTransform nav;

  /** Identity navigation flag. */
  private boolean identityNavigation;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the tiling scheme for this grid if one is available.
   *
   * @return the tiling scheme or null for none.
   *
   * @since 3.4.0
   */
  public TilingScheme getTilingScheme () { return (null); }

  ////////////////////////////////////////////////////////////

  /** Gets the navigation correction affine transform. */
  public AffineTransform getNavigation () { 
    return ((AffineTransform) nav.clone ());
  } // getNavigation

  ////////////////////////////////////////////////////////////

  /** Sets the navigation correction affine transform. */
  public void setNavigation (AffineTransform nav) { 

    this.nav = (nav == null ? new AffineTransform() : 
      (AffineTransform) nav.clone());
    identityNavigation = this.nav.isIdentity();

  } // setNavigation

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new 2D grid with the specified properties.  The
   * <code>rows</code> and <code>cols</code> specify the grid
   * dimensions.  The inital affine transform is set to the identity.
   *
   * @see DataVariable
   */
  public Grid (
    String name,
    String longName,
    String units,
    int rows, 
    int cols,
    Object data,
    NumberFormat format,
    double[] scaling,
    Object missing
  ) {

    // Chain to parent
    // ---------------
    super (name, longName, units, new int[] {rows, cols}, data, 
      format, scaling, missing);

    // Initialize navigation to identity
    // ---------------------------------
    setNavigation (null);

  } // Grid constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new 2D grid from the specified grid.  All properties
   * are copied, but the data array contains no actual data values.
   *
   * @param grid the grid to use for properties.
   */
  public Grid (
    Grid grid
  ) {

    this (grid.getName(), grid.getLongName(), grid.getUnits(),
      grid.dims[ROWS], grid.dims[COLS], 
      Array.newInstance (grid.getDataClass(), 1), grid.getFormat(),
      grid.getScaling(), grid.getMissing());
    setNavigation (grid.nav);
    setUnsigned (grid.getUnsigned());
    setLookup (grid.lookup);
    getMetadataMap().putAll (grid.getMetadataMap());

  } // Grid constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new 2D grid from the specified grid and dimensions.
   * All properties are copied, but the data array contains no actual
   * data values.
   *
   * @param grid the grid to use for properties.
   * @param rows the grid rows dimension.
   * @param cols the grid columns dimension.
   */
  public Grid (
    Grid grid,
    int rows,
    int cols
  ) {

    this (grid.getName(), grid.getLongName(), grid.getUnits(), rows, cols,
      Array.newInstance (grid.getDataClass(), 1), grid.getFormat(),
      grid.getScaling(), grid.getMissing());
    setNavigation (grid.nav);
    setUnsigned (grid.getUnsigned());
    getMetadataMap().putAll (grid.getMetadataMap());

  } // Grid constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a scaled data value with no navigation.  The data value is
   * scaled according to the scaling factor and offset and written to
   * the data array.  No navigation transform is applied to correct
   * the data location.
   *
   * @param row the data location row.
   * @param col the data location column.
   * @param val the data value as a double.  If the data value is
   * <code>Double.NaN</code> and the missing value is non-null, the
   * missing value is written to the array.
   *
   * @see #setValue(DataLocation,double)
   */
  public void setValue (
    int row,
    int col,
    double val
  ) {

    if (row < 0 || row > dims[ROWS]-1 || col < 0 || col > dims[COLS]-1)
      return;
    setValue (row*dims[COLS] + col, val);

  } // setValue

  ////////////////////////////////////////////////////////////

  /**
   * Reads a scaled data value with no navigation.  The data value is
   * read from the data array and scaled according to the scaling
   * factor and offset.  No navigation transform is applied to
   * correct the data location.
   *
   * @param row the data location row.
   * @param col the data location column.
   *
   * @return the scaled data value as a <code>double</code>.  The
   * <code>Double.NaN</code> value is used if the data value is
   * missing or data coordinate is not valid.
   *
   * @see #getValue(DataLocation)
   */
  public double getValue (
    int row,
    int col
  ) {

    if (row < 0 || row > dims[ROWS]-1 || col < 0 || col > dims[COLS]-1)
      return (Double.NaN);
    return (getValue (row*dims[COLS] + col));

  } // getValue

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a scaled data value with navigation.  The data value is
   * scaled according to the scaling factor and offset and written to
   * the data array.  The navigation transform is applied to correct
   * the data location prior to writing the data value.
   *
   * @param loc the data location.  If the location does not navigate
   * to a valid data coordinate, no value is written.
   * @param val the data value as a double.  If the data value is
   * <code>Double.NaN</code> and the missing value is non-null, the
   * missing value is written to the array.
   *
   * @see #setValue(int,int,double)
   */
  public void setValue (
    DataLocation loc,
    double val
  ) {

    // Apply navigation and check
    // --------------------------
    DataLocation actual = navigate (loc);
    if (!actual.isContained (dims)) return;

    // Set value
    // ---------
    super.setValue (actual, val);

  } // setValue

  ////////////////////////////////////////////////////////////

  /**
   * Reads a scaled data value with navigation.  The data value is
   * read from the data array and scaled according to the scaling
   * factor and offset.  The navigation transform is applied to
   * correct the data location prior to reading the data value.
   *
   * @param loc the data value location.
   *
   * @return the scaled data value as a <code>double</code>.  The
   * <code>Double.NaN</code> value is used if the data value is
   * missing or navigated data coordinate is not valid.
   *
   * @see #getValue(int,int)
   */
  public double getValue (
    DataLocation loc
  ) {

    // Apply navigation and check
    // --------------------------
    DataLocation actual = navigate (loc);
    if (!actual.isContained (dims)) return (Double.NaN);

    // Get value
    // ---------
    return (super.getValue (actual));

  } // getValue

  ////////////////////////////////////////////////////////////

  /**
   * Applies the navigation transform to the specified data coordinate.
   *
   * @param loc the data coordinate to navigate.
   *
   * @return a corrected data coordinate.
   */
  public DataLocation navigate (
    DataLocation loc
  ) {

    if (identityNavigation) return (loc);
    return (loc.transform (nav));

  } // navigate

  ////////////////////////////////////////////////////////////

  public double interpolate (
    DataLocation loc
  ) {

    // Apply navigation and check
    // --------------------------
    DataLocation actual = navigate (loc);
    if (!actual.isContained (dims)) return (Double.NaN);

    // Get data values in surrounding grid
    // -----------------------------------
    DataLocation upperLeft = loc.floor ();
    DataLocation lowerRight = loc.ceil ();
    double a = super.getValue (new DataLocation (
      upperLeft.get (ROWS),
      upperLeft.get (COLS)
    ));
    double b = super.getValue (new DataLocation (
      upperLeft.get (ROWS), 
      lowerRight.get (COLS)
    ));
    double c = super.getValue (new DataLocation (
      lowerRight.get (ROWS), 
      upperLeft.get (COLS)
    ));
    double d = super.getValue (new DataLocation (
      lowerRight.get (ROWS), 
      lowerRight.get (COLS)
    ));

    // Perform interpolation
    // ---------------------
    double dx = actual.get (COLS) - upperLeft.get (COLS);
    double dy = actual.get (ROWS) - upperLeft.get (ROWS);
    double val = (dx*b + (1-dx)*a) * (1-dy) + (dx*d + (1-dx)*c) * dy;
    return (val);

  } // interpolate

  ////////////////////////////////////////////////////////////

  /** 
   * Checks the subset specification for errors.
   *
   * @param start the starting [row, column].
   * @param count the subset dimension [rows, columns].
   *
   * @return true if the subset is good, false otherwise.
   */
  public boolean checkSubset (
    int[] start,
    int[] count
  ) {
  
    // Check start
    // -----------
    if (start[ROWS] < 0 || start[ROWS] > dims[ROWS]-1) return (false);
    if (start[COLS] < 0 || start[COLS] > dims[COLS]-1) return (false);

    // Check dimensions
    // ----------------
    if (count[ROWS] <= 0 || count[COLS] <= 0) return (false);

    // Check end
    // ---------
    int[] end = new int[] {
      start[ROWS] + count[ROWS] - 1, 
      start[COLS] + count[COLS] - 1
    }; 
    if (end[ROWS] < 0 || end[ROWS] > dims[ROWS]-1) return (false);
    if (end[COLS] < 0 || end[COLS] > dims[COLS]-1) return (false);

    return (true);

  } // checkSubset

  ////////////////////////////////////////////////////////////

  /**
   * Performs an array copy of 2D data stored in 1D arrays.  This
   * method is similar to <code>System.arraycopy</code> but works 
   * with 2D data stored in 1D arrays in row-major order.
   *
   * @param src the source array.
   * @param srcDims the source array dimensions as [rows, columns].
   * @param srcPos the source starting position as [row, column].
   * @param dest the destination array.
   * @param destDims the destination array dimensions as [rows, columns].
   * @param destPos the destination starting position as [row, column].
   * @param length the copy length as [rows, columns].
   */
  public static void arraycopy (
    Object src,
    int[] srcDims,
    int[] srcPos,
    Object dest,
    int[] destDims,
    int[] destPos,
    int[] length
  ) {

    // Loop over each row in the copy
    // ------------------------------
    for (int i = 0; i < length[ROWS]; i++) {
      int srcStartIndex = (srcPos[ROWS]+i)*srcDims[COLS] + srcPos[COLS];
      int destStartIndex = (destPos[ROWS]+i)*destDims[COLS] + destPos[COLS];
      System.arraycopy (src, srcStartIndex, dest, destStartIndex, length[COLS]);
    } // for

  } // arraycopy

  ////////////////////////////////////////////////////////////

  /**
   * Sets a subset of grid data values.  This method is similar to
   * {@link DataVariable#setData(Object)}, but sets only a subset of data
   * values in the raw, unscaled form.
   *
   * @param subset the subset array of unscaled data values.
   * @param start the subset starting [row, column].
   * @param count the subset dimensions [rows, columns].
   *
   * @throws IndexOutOfBoundsException if the subset falls outside the
   * grid dimensions.
   *
   * @since 3.4.0
   */
  public void setData (
    Object subset,
    int[] start,
    int[] count
  ) {

    // Check subset
    // ------------
    if (!checkSubset (start, count))
      throw new IndexOutOfBoundsException ("Invalid subset");

    // Modify data
    // -----------
    arraycopy (subset, count, new int[] {0,0}, data, dims, start, count);

  } // setData

  ////////////////////////////////////////////////////////////

  /**
   * Gets a subset of grid data values.  This method is similar to
   * {@link #getData}, but retrieves only a subset of data
   * values in the raw, unscaled form.
   *
   * @param start the subset starting [row, column].
   * @param count the subset dimension [rows, columns].
   *
   * @return an array containing the unscaled data values.
   *
   * @throws IndexOutOfBoundsException if the subset falls outside the
   * grid dimensions.
   *
   * @see DataVariable#getData
   */
  public Object getData (
    int[] start,
    int[] count
  ) {

    // Check subset
    // ------------
    if (!checkSubset (start, count))
      throw new IndexOutOfBoundsException ("Invalid subset");

    // Create subset
    // -------------
    Object subset = Array.newInstance (getDataClass(), count[ROWS]*count[COLS]);
    arraycopy (data, dims, start, subset, count, new int[] {0,0}, count);
    return (subset);

  } // getData

  ////////////////////////////////////////////////////////////
  
  /** 
   * Gets a new subset of this grid using the specified parameters.
   *
   * @param start the starting data location for the subset as
   * <code>[row, column]</code>.
   * @param dims the subset dimensions as <code>[rows,
   * columns]</code>.
   *
   * @return the new subset of this grid.
   */
  public Grid getSubset (
    int[] start,
    int[] dims
  ) {

    return (new SubsetGrid (this, start, dims));

  } // getSubset

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (Grid.class);

    // ------------------------->

    logger.test ("Framework");

    int[] dims = new int[] {40, 40};
    int[] data = new int[dims[ROWS]*dims[COLS]];
    for (int i = 0; i < dims[ROWS]; i++) {
      for (int j = 0; j < dims[COLS]; j++) {
        data[i*dims[COLS] + j] = i*dims[COLS] + j;
      } // for
    } // for
    for (int index = 1; index < data.length; index++) {
      assert (data[index] != data[index-1]);
    } // for

    Grid grid = new Grid (
      "test",
      "test data",
      "meters",
      dims[ROWS],
      dims[COLS],
      data,
      new java.text.DecimalFormat ("000"),
      null,
      null);

    logger.passed();

    // ------------------------->

    logger.test ("getData");
    
    int[] start = new int[] {3, 25};
    int[] count = new int[] {15, 9};
    int[] readTile = (int[]) grid.getData (start, count);

    for (int i = 0; i < count[ROWS]; i++) {
      for (int j = 0; j < count[COLS]; j++) {
        int globalRow = i+start[ROWS];
        int globalCol = j+start[COLS];
        assert (readTile[i*count[COLS] + j] == data[globalRow*dims[COLS] + globalCol]);
      } // for
    } // for

    logger.passed();

    // ------------------------->

    logger.test ("setData");
    
    start = new int[] {7, 13};
    count = new int[] {20, 15};
    int[] writeTile = new int[count[ROWS]*count[COLS]];
    for (int i = 0; i < count[ROWS]; i++) {
      for (int j = 0; j < count[COLS]; j++) {
        writeTile[i*count[COLS] + j] = (i*count[COLS] + j) + data.length;
      } // for
    } // for

    grid.setData (writeTile, start, count);
    
    for (int i = 0; i < dims[ROWS]; i++) {
      for (int j = 0; j < dims[COLS]; j++) {
        int tileRow = i-start[ROWS];
        int tileCol = j-start[COLS];
        if (
          tileRow < 0 || tileRow > count[ROWS]-1 ||
          tileCol < 0 || tileCol > count[COLS]-1
        ) {
          assert (data[i*dims[COLS] + j] == (i*dims[COLS] + j));
        } // if
        else {
          assert (data[i*dims[COLS] + j] == writeTile[tileRow*count[COLS] + tileCol]);
        } // else
      } // for
    } // for

    logger.passed();

    // ------------------------->

  } // main

  ////////////////////////////////////////////////////////////

} // Grid class

////////////////////////////////////////////////////////////////////////
