////////////////////////////////////////////////////////////////////////
/*

     File: DirectResamplingMapFactory.java
   Author: Peter Hollemans
     Date: 2019/02/04

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
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * <p>The <code>DirectResamplingMapFactory</code> class creates a resampling
 * map by directly querying the source transform for the data location of
 * each earth location in the destination transform.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class DirectResamplingMapFactory implements ResamplingMapFactory {

  // Variables
  // ---------
  
  /** The source transform. */
  private EarthTransform sourceTrans;

  /** The destination transform. */
  private EarthTransform destTrans;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new resampling factory from the source and
   * destination transforms.
   *
   * @param sourceTrans the source earth transform.
   * @param destTrans the destination earth transform.
   */
  public DirectResamplingMapFactory (
    EarthTransform sourceTrans,
    EarthTransform destTrans
  ) {
  
    this.sourceTrans = sourceTrans;
    this.destTrans = destTrans;

  } // DirectResamplingMapFactory const

  ////////////////////////////////////////////////////////////

  @Override
  public ResamplingMap create (
    int[] start,
    int[] length
  ) {

    // Create row and column map arrays
    // --------------------------------
    int entries = length[ROW] * length[COL];
    int[] rowMap = new int[entries];
    int[] colMap = new int[entries];
    boolean isEmptyMap = true;

    // Loop over each dest location
    // ----------------------------
    DataLocation destLoc = new DataLocation (2);
    DataLocation sourceLoc = new DataLocation (2);
    int[] sourceDims = sourceTrans.getDimensions();
    EarthLocation earthLoc = new EarthLocation();
    int destIndex = 0;
    int[] end = new int[] {start[ROW] + length[ROW], start[COL] + length[COL]};

    for (int i = start[ROW]; i < end[ROW]; i++) {
      for (int j = start[COL]; j < end[COL]; j++) {

        // Get dest earth location
        // -----------------------
        destLoc.set (ROW, i);
        destLoc.set (COL, j);
        destTrans.transform (destLoc, earthLoc);

        // Save mapping of dest location
        // -----------------------------
        boolean isValidMapping = false;
        if (earthLoc.isValid()) {
          sourceTrans.transform (earthLoc, sourceLoc);
          if (sourceLoc.isValid()) {
            int sourceRow = (int) Math.round (sourceLoc.get (ROW));
            int sourceCol = (int) Math.round (sourceLoc.get (COL));
            if (
              sourceRow >= 0 &&
              sourceRow < sourceDims[ROW] &&
              sourceCol >= 0 &&
              sourceCol < sourceDims[COL]
            ) {
              rowMap[destIndex] = sourceRow;
              colMap[destIndex] = sourceCol;
              isValidMapping = true;
              isEmptyMap = false;
            } // if
          } // if
        } // if

        // Flag mapping as invalid
        // -----------------------
        if (!isValidMapping) {
          rowMap[destIndex] = Integer.MIN_VALUE;
          colMap[destIndex] = Integer.MIN_VALUE;
        } // if

        // Increment index to next data value
        // ----------------------------------
        destIndex++;

      } // for
    } // for

    // Create and return mapping
    // -------------------------
    ResamplingMap map;
    if (isEmptyMap)
      map = null;
    else
      map = new ResamplingMap (start, length, rowMap, colMap);
    
    return (map);

  } // create

  ////////////////////////////////////////////////////////////

} // DirectResamplingMapFactory class

////////////////////////////////////////////////////////////////////////

