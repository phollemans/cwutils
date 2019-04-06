////////////////////////////////////////////////////////////////////////
/*

     File: ResamplingMap.java
   Author: Peter Hollemans
     Date: 2018/11/11

  CoastWatch Software Library and Utilities
  Copyright (c) 2018 National Oceanic and Atmospheric Administration
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
import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * A <code>ResamplingMap</code> object holds coordinate mapping data
 * between two 2D coordinate systems: the source and destination.
 * Coordinates from the destination coordinate space can be queried to map
 * to their corresponding nearest neighbour in the source coordinate
 * space.  This allows for a fast nearest neighbour resampling algorithms
 * to run using only the map.  A map covers a specific rectangular
 * extent of the destination coordinate extents.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class ResamplingMap {

  // Variables
  // ---------

  /** The destination space start coordinates for the map. */
  private int[] start;

  /** The destination space length dimensions for the map. */
  private int[] length;

  /** The array of source coordinate row values for each destination coordinate. */
  private int[] rowMap;
  
  /** The array of source coordinate column values for each destination coordinate. */
  private int[] colMap;

  ////////////////////////////////////////////////////////////

  /**
   * Maps the destination coordinates to their corresponding source coordinates.
   *
   * @param destCoords the destination coordinates to map as [row, col].
   * The coordinates must be within the destination space extents defined
   * by the map.
   * @param sourceCoords the source coordinates as [row, col] (modified).
   *
   * @return true if the mapping exists from destination coordinates, or false
   * if not.  If false, the values in the sourceCoords array are undefined.
   */
  public boolean map (
    int[] destCoords,
    int[] sourceCoords
  ) {

    int mapRow = destCoords[ROW] - start[ROW];
    int mapCol = destCoords[COL] - start[COL];
    int index = mapRow*length[COL] + mapCol;
    boolean isValidMapping = (rowMap[index] != Integer.MIN_VALUE);
    if (isValidMapping) {
      sourceCoords[ROW] = rowMap[index];
      sourceCoords[COL] = colMap[index];
    } // if

    return (isValidMapping);
  
  } // map

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new 2D resampling map.
   *
   * @param start the starting map coordinates in the destination space.
   * @param length the map length along each dimension in the destination space.
   * @param rowMap the source coordinate row mapping from destination
   * coordinates.  The source coordinate array can contain Integer.MIN_VALUE as
   * a flag to indicate that no mapping exists for a given destination coordinate.
   * @param colMap the source coordinate column mapping from destination
   * coordinates.  The source coordinate array can contain Integer.MIN_VALUE as
   * a flag to indicate that no mapping exists for a given destination coordinate.
   */
  public ResamplingMap (
    int[] start,
    int[] length,
    int[] rowMap,
    int[] colMap
  ) {
  
    this.start = start;
    this.length = length;
    this.rowMap = rowMap;
    this.colMap = colMap;

  } // ResamplingMap constructor

  ////////////////////////////////////////////////////////////

} // ResamplingMap class

////////////////////////////////////////////////////////////////////////
