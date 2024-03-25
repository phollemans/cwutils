////////////////////////////////////////////////////////////////////////
/*

     File: BucketResamplingMapFactory.java
   Author: Peter Hollemans
     Date: 2019/02/07

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
import java.util.logging.Logger;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.EarthLocationSet;
import noaa.coastwatch.util.trans.SpheroidConstants;
import noaa.coastwatch.util.ResamplingSourceImp;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * <p>The <code>BucketResamplingMapFactory</code> class creates a resampling
 * map by placing the source transform earth locations into buckets and
 * searching the bucket contents for the closest data location to
 * each earth location in the destination transform.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class BucketResamplingMapFactory implements ResamplingMapFactory {

  private static final Logger LOGGER = Logger.getLogger (ResamplingMapFactory.class.getName());

  // Variables
  // ---------
  
  /** The source transform. */
  private EarthTransform sourceTrans;

  /** The destination transform. */
  private EarthTransform destTrans;

  /** The set of earth locations to search. */
  private EarthLocationSet<int[]> locationSet;
  
  /** The source area to search. */
  private EarthArea sourceArea;

  /** The implementation to use for source locations. */
  private ResamplingSourceImp sourceImp;

  /** The flag to perform a datum shift between source and destination. */
  private boolean isDatumShiftNeeded;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new resampling factory from the source and
   * destination transforms.
   *
   * @param sourceTrans the source earth transform.
   * @param destTrans the destination earth transform.
   * @param sourceImp the source-specific object to use for resampling queries.
   *
   * @throws IllegalStateException if the source transform had invalid earth
   * locations when trying to determine its resolution.
   */
  public BucketResamplingMapFactory (
    EarthTransform sourceTrans,
    EarthTransform destTrans,
    ResamplingSourceImp sourceImp
  ) {
  
    this.sourceTrans = sourceTrans;
    this.destTrans = destTrans;
    this.sourceImp = sourceImp;

    // Estimate source resolution at center
    // ------------------------------------
    int[] sourceDims = sourceTrans.getDimensions();
    DataLocation centerDataLoc = new DataLocation (sourceDims[ROW]/2, sourceDims[COL]/2);

    EarthLocation centerEarthLoc = sourceTrans.transform (centerDataLoc);
    EarthLocation leftEarthLoc = sourceTrans.transform (centerDataLoc.translate (0, -1));
    EarthLocation rightEarthLoc = sourceTrans.transform (centerDataLoc.translate (0, 1));
    EarthLocation topEarthLoc = sourceTrans.transform (centerDataLoc.translate (-1, 0));
    EarthLocation bottomEarthLoc = sourceTrans.transform (centerDataLoc.translate (1, 0));

    double horizRes = leftEarthLoc.distance (rightEarthLoc)/2;
    if (Double.isNaN (horizRes) || horizRes == 0) horizRes = -Double.MAX_VALUE;

    double vertRes = topEarthLoc.distance (bottomEarthLoc)/2;
    if (Double.isNaN (vertRes) || vertRes == 0) vertRes = -Double.MAX_VALUE;

    double res = Math.max (horizRes, vertRes);

    if (res == -Double.MAX_VALUE)
      throw new IllegalStateException ("Cannot determine source transform resolution");

    // Create location set with approx 5x5 locations per bin
    // -----------------------------------------------------
    double resInDegrees = Math.toDegrees (res / SpheroidConstants.STD_RADIUS);
    int binsPerDegree = (int) Math.round (1.0/(5*resInDegrees));
    if (binsPerDegree < 1) binsPerDegree = 1;
    this.locationSet = new EarthLocationSet<> (binsPerDegree);

    LOGGER.fine ("Resolution " + res + " km (" + resInDegrees + " deg)");
    LOGGER.fine ("Bin size " + binsPerDegree + " bins/deg");

    // Detect a difference in datums between source and destination
    // ------------------------------------------------------------
    isDatumShiftNeeded = !sourceTrans.getDatum().equals (destTrans.getDatum());

    if (isDatumShiftNeeded)
      LOGGER.fine ("Datum shift detected between source and destination transform");

    // Create an earth area for the destination transform
    // --------------------------------------------------
    int[] destDims = destTrans.getDimensions();
    EarthArea destArea = new EarthArea (destTrans,
      new DataLocation (0, 0),
      new DataLocation (destDims[ROW]-1, destDims[COL]-1));

    // Add source locations to set
    // ---------------------------
    EarthLocation earthLoc = new EarthLocation();
    DataLocation sourceLoc = new DataLocation (2);
    sourceArea = new EarthArea();

    for (int i = 0; i < sourceDims[ROW]; i++) {
      for (int j = 0; j < sourceDims[COL]; j++) {

        // Transform to earth location
        // ---------------------------
        sourceLoc.set (ROW, i);
        sourceLoc.set (COL, j);
        if (sourceImp.isValidLocation (sourceLoc)) {

          // Get earth location of source (i, j)
          // -----------------------------------
          sourceTrans.transform (sourceLoc, earthLoc);

          // If valid and in destination transform, add to locations
          // -------------------------------------------------------
          if (earthLoc.isValid()) {
            sourceArea.add (earthLoc);
            if (destArea.contains (earthLoc)) {
              locationSet.insert ((EarthLocation) earthLoc.clone(), new int[] {i, j});
            } // if
          } // if
  
        } // if
  
      } // for
    } // for


  } // BucketResamplingMapFactory const

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
    EarthLocation destEarthLoc = new EarthLocation();
    int destIndex = 0;
    int[] end = new int[] {start[ROW] + length[ROW], start[COL] + length[COL]};
    Object searchContext = locationSet.getContext();
    Object sourceContext = sourceImp.getContext();
    Datum sourceDatum = sourceTrans.getDatum();

    for (int i = start[ROW]; i < end[ROW]; i++) {
      for (int j = start[COL]; j < end[COL]; j++) {

        // Get dest earth location
        // -----------------------
        destLoc.set (ROW, i);
        destLoc.set (COL, j);
        destTrans.transform (destLoc, destEarthLoc);

        // Find and check mapping
        // ----------------------
        boolean isValidMapping = false;
        if (destEarthLoc.isValid()) {

          if (isDatumShiftNeeded) destEarthLoc.shiftDatum (sourceDatum);

          if (sourceArea.contains (destEarthLoc)) {
            EarthLocationSet<int[]>.Entry nearestSourceEntry = locationSet.nearest (destEarthLoc, searchContext);
            if (nearestSourceEntry != null) {
            
              int sourceRow = nearestSourceEntry.data[ROW];
              int sourceCol = nearestSourceEntry.data[COL];
              sourceLoc.set (ROW, sourceRow);
              sourceLoc.set (COL, sourceCol);

              if (sourceImp.isValidNearestLocation (destEarthLoc, sourceLoc, sourceContext)) {
                rowMap[destIndex] = sourceRow;
                colMap[destIndex] = sourceCol;
                isValidMapping = true;
                isEmptyMap = false;
              } // if

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

} // BucketResamplingMapFactory class

////////////////////////////////////////////////////////////////////////


