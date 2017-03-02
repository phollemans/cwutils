////////////////////////////////////////////////////////////////////////
/*

     File: SubsetGrid.java
   Author: Peter Hollemans
     Date: 2004/05/05

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.Grid;

/**
 * The <code>SubsetGrid</code> class uses an existing
 * <code>Grid</code> object to provide a subset view of its data.  For
 * example, if the original grid has dimensions (1024,1024) and the
 * subset is defined to show only a small section starting at
 * (100,100) of dimensions (512,512), then accessing location (0,0) in
 * the subset is the same as accessing location (100,100) in the
 * original grid.  All other properties of the original grid are the
 * same.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class SubsetGrid
  extends Grid {

  // Variables
  // ---------

  /** The starting coordinates of the subset. */
  private int[] start;

  /** The underlying grid to use for data. */
  private Grid grid;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new subset based on the specified grid and parameters.
   *
   * @param grid the grid to subset.
   * @param start the starting data location for the subset as
   * <code>[row, column]</code>.
   * @param dims the subset dimensions as <code>[rows,
   * columns]</code>.
   */
  public SubsetGrid (
    Grid grid,
    int[] start,
    int[] dims
  ) {

    // Initialize
    // ----------
    super (grid, dims[0], dims[1]);
    this.grid = grid;
    this.start = (int[]) start.clone();

  } // SubsetGrid constructor

  ////////////////////////////////////////////////////////////

  public void setValue (
    int row,
    int col,
    double val
  ) {

    throw new UnsupportedOperationException ("Cannot set values in subset");

  } // setValue

  ////////////////////////////////////////////////////////////

  public double getValue (
    int row,
    int col
  ) {

    return (grid.getValue (row + start[0], col + start[1]));

  } // getValue

  ////////////////////////////////////////////////////////////

  public void setValue (
    DataLocation loc,
    double val
  ) {

    throw new UnsupportedOperationException ("Cannot set values in subset");

  } // setValue

  ////////////////////////////////////////////////////////////

  public double getValue (
    DataLocation loc
  ) {

    return (grid.getValue (loc.translate (start[0], start[1])));

  } // getValue

  ////////////////////////////////////////////////////////////

  public void setValue (
    int index,
    double val
  ) {

    throw new UnsupportedOperationException ("Cannot set values in subset");

  } // setValue

  ////////////////////////////////////////////////////////////

  public double getValue (
    int index
  ) {

    return (getValue (index/dims[COLS], index%dims[COLS]));

  } // getValue

  ////////////////////////////////////////////////////////////

  public double interpolate (
    DataLocation loc
  ) {

    return (grid.interpolate (loc.translate (start[0], start[1])));

  } // interpolate

  ////////////////////////////////////////////////////////////

  public Object getData () { return (getData (new int[] {0, 0}, dims)); }

  ////////////////////////////////////////////////////////////

  public Object getData (
    int[] start,
    int[] count
  ) {

    return (grid.getData (new int[] {start[0] + this.start[0], 
      start[1] + this.start[1]}, count));

  } // getData

  ////////////////////////////////////////////////////////////

} // SubsetGrid class

////////////////////////////////////////////////////////////////////////
