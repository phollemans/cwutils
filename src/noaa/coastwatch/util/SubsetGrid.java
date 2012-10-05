////////////////////////////////////////////////////////////////////////
/*
     FILE: SubsetGrid.java
  PURPOSE: To act as a subset view of a grid.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/05
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.text.*;
import java.awt.geom.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * The <code>SubsetGrid</code> class uses an existing
 * <code>Grid</code> object to provide a subset view of its data.  For
 * example, if the original grid has dimensions (1024,1024) and the
 * subset is defined to show only a small section starting at
 * (100,100) of dimensions (512,512), then accessing location (0,0) in
 * the subset is the same as accessing location (100,100) in the
 * original grid.  All other properties of the original grid are the
 * same.
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
