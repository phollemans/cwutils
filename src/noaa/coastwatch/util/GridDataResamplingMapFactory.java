////////////////////////////////////////////////////////////////////////
/*

     File: GridDataResamplingMapFactory.java
   Author: Peter Hollemans
     Date: 2019/03/17

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.ResamplingMapFactory;
import noaa.coastwatch.util.ResamplingMap;
import noaa.coastwatch.util.Grid;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * <p>The <code>GridDataResamplingMapFactory</code> class creates a resampling
 * map by directly querying grid variables for the source and column mapping.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
 public class GridDataResamplingMapFactory implements ResamplingMapFactory {

  // Variables
  // ---------
  
  /** The grid supplying source row coordinates. */
  private Grid sourceRowGrid;

  /** The grid supplying source ciolumn coordinates. */
  private Grid sourceColGrid;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new map factory.
   *
   * @param sourceRowGrid the grid variable to use for retrieving row data
   * for the mapping.
   * @param sourceColGrid the grid variable to use for retrieving column data
   * for the mapping.
   */
  public GridDataResamplingMapFactory (
    Grid sourceRowGrid,
    Grid sourceColGrid
  ) {

    if (!sourceRowGrid.getDataClass().equals (Integer.TYPE))
      throw new IllegalArgumentException ("Source row variable must be 32-bit int type");
    this.sourceRowGrid = sourceRowGrid;

    if (!sourceColGrid.getDataClass().equals (Integer.TYPE))
      throw new IllegalArgumentException ("Source column variable must be 32-bit int type");
    this.sourceColGrid = sourceColGrid;

  } // GridDataResamplingMapFactory constructor
  
  ////////////////////////////////////////////////////////////

  @Override
  public ResamplingMap create (
    int[] start,
    int[] length
  ) {

    // Get the row and column data values
    // ----------------------------------
    int[] rowData;
    synchronized (sourceRowGrid) {
      rowData = (int[]) sourceRowGrid.getData (start, length);
    } // synchronized
    int rowMissing = (int) sourceRowGrid.getMissing();

    int[] colData;
    synchronized (sourceColGrid) {
      colData = (int[]) sourceColGrid.getData (start, length);
    } // synchronized
    int colMissing = (int) sourceColGrid.getMissing();

    // Check for all missing data
    // --------------------------
    boolean isEmptyMap = true;
    int values = length[ROW] * length [COL];

    for (int i = 0; i < values; i++) {
      if (rowData[i] != rowMissing || colData[i] != colMissing) {
        isEmptyMap = false;
        break;
      } // if
    } // for

    // Create and return mapping
    // -------------------------
    ResamplingMap map;
    if (isEmptyMap)
      map = null;
    else
      map = new ResamplingMap (start, length, rowData, colData);

    return (map);

  } // create

  ////////////////////////////////////////////////////////////

} // GridDataResamplingMapFactory class

////////////////////////////////////////////////////////////////////////
