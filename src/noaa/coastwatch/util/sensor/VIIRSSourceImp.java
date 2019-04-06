
////////////////////////////////////////////////////////////////////////
/*

     File: VIIRSSourceImp.java
   Author: Peter Hollemans
     Date: 2019/03/05

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
package noaa.coastwatch.util.sensor;

// Imports
// -------
import java.util.logging.Logger;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.ResamplingSourceImp;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * <p>The <code>VIIRSSourceImp</code> helps resample VIIRS data.  The VIIRS
 * scan is 3200 pixels wide by 16 pixels high and has some pixels deleted
 * on the top and bottom two rows which results in less overlap of the scan
 * than say MODIS.  The deletion pattern is as follows with the first and last
 * 1008 pixels deleted on the first line of the scan, and 640 on the second line,
 * similarly at the bottom of the scan:</p>
 * <pre>
 *     0    640       1008               2192      2560   3199
 *  0   ****************-------------------****************
 *  1   ******---------------------------------------******
 *  2   ---------------------------------------------------
 *  ..  ---------------------------------------------------
 *  13  ---------------------------------------------------
 *  14  ******---------------------------------------******
 *  15  ****************-------------------****************
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class VIIRSSourceImp implements ResamplingSourceImp {

  private static final Logger LOGGER = Logger.getLogger (VIIRSSourceImp.class.getName());

  // Variables
  // ---------

  /** The source transform used for locations. */
  private EarthTransform sourceTrans;

  /** The dimensions of the source transform. */
  private int[] sourceDims;

  /** The ECF coordinates of the edge locations. */
  private double[][] topEdgeECFCoords;
  private double[][] bottomEdgeECFCoords;
  private double[][] leftEdgeECFCoords;
  private double[][] rightEdgeECFCoords;

  /** The cached ECF vectors facing inwards from the edges of the transform. */
  private double[][] topEdgeVectors;
  private double[][] bottomEdgeVectors;
  private double[][] leftEdgeVectors;
  private double[][] rightEdgeVectors;

  /**
   * The VIIRS bow-tie deletion pattern array, true for locations
   * removed by bow-tie deletion.
   */
  private boolean[][] isBowtieDeleted;
  
  /** The true starting row of the source after any invalid rows. */
  private int validStartRow;
  
  /** The true ending row of the source before any invalid rows. */
  private int validEndRow;
  
  ////////////////////////////////////////////////////////////

  /** Computes vector from b to a, result_k = a_k - b_k. */
  private void subtract (double[] a, double[] b, double[] result) {
  
    for (int k = 0; k < 3; k++)
      result[k] = a[k] - b[k];

  } // subtract

  ////////////////////////////////////////////////////////////

  /** Computes dot product, result = sum_k (a_k * b_k). */
  private double dot (double[] a, double[] b) {
  
    double result = 0;
    for (int k = 0; k < 3; k++)
      result += a[k] * b[k];

    return (result);
    
  } // dot

  ////////////////////////////////////////////////////////////

  /**
   * Gets the top edge row of the swath at the specified column.
   *
   * @param col the column to query, [0..3199].
   *
   * @return the row within the data that forms the top of the non-deleted
   * part of the swath at the specified column.
   */
  private int getTopRowAtColumn (
    int col
  ) {

    int topRow;
    
    if (col >= 1008 && col < 2192)
      topRow = 0;
    else if (col >= 640 && col < 2560)
      topRow = 1;
    else topRow = 2;

    return (topRow);

  } // getTopRowAtColumn
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new VIIRS resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for VIIRS swath location
   * data.
   *
   * @return the new resampling helper.
   */
  public static VIIRSSourceImp getInstance (
    EarthTransform sourceTrans
  ) {
  
    // Check dimensions
    // ----------------
    int[] sourceDims = sourceTrans.getDimensions();
    if (sourceDims[ROW]%16 != 0)
      throw new RuntimeException ("Invalid number of rows for VIIRS scan");
    if (sourceDims[COL] != 3200)
      throw new RuntimeException ("Invalid number of columns for VIIRS scan");

    return (new VIIRSSourceImp (sourceTrans));
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new VIIRS resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for VIIRS swath location
   * data.
   */
  private VIIRSSourceImp (
    EarthTransform sourceTrans
  ) {

    this.sourceTrans = sourceTrans;

    // Get dimensions
    // --------------
    sourceDims = sourceTrans.getDimensions();
    int rows = sourceDims[ROW];
    int cols = sourceDims[COL];

    // Set up bow-tie deletion pattern
    // -------------------------------
    isBowtieDeleted = new boolean[16][3200];
    for (int i = 0; i < 16; i++) {
      for (int j = 0; j < 3200; j++) {
        if ((i == 0 || i == 15) && (j < 1008 || j >= 2192))
          isBowtieDeleted[i][j] = true;
        else if ((i == 1 || i == 14) && (j < 640 || j >= 2560))
          isBowtieDeleted[i][j] = true;
        else
          isBowtieDeleted[i][j] = false;
      } // for
    } // for

    // Check for invalid lines at the start and end of the source
    // ----------------------------------------------------------
    DataLocation dataLoc = new DataLocation (2);
    EarthLocation earthLoc = new EarthLocation();
    dataLoc.set (COL, cols/2);

    for (validStartRow = 0; validStartRow < rows; validStartRow++) {
      dataLoc.set (ROW, validStartRow);
      sourceTrans.transform (dataLoc, earthLoc);
      if (earthLoc.isValid()) break;
    } // for
      
    for (validEndRow = rows-1; validEndRow >= 0; validEndRow--) {
      dataLoc.set (ROW, validEndRow);
      sourceTrans.transform (dataLoc, earthLoc);
      if (earthLoc.isValid()) break;
    } // for

    LOGGER.fine ("Found " + validStartRow + " invalid rows at start of VIIRS scan");
    int invalidEndRows = rows-1 - validEndRow;
    LOGGER.fine ("Found " + invalidEndRows + " invalid rows at end of VIIRS scan");

    if (validStartRow%16 != 0)
      throw new RuntimeException ("First valid row not on full VIIRS scan boundary");
    if (invalidEndRows%16 != 0)
      throw new RuntimeException ("Last valid row not on full VIIRS scan boundary");

    // Compute inward pointing vectors along top and bottom edge
    // ---------------------------------------------------------
    double[] edgeECFCoord;
    double[] innerECFCoord = new double[3];

    topEdgeECFCoords = new double[cols][3];
    bottomEdgeECFCoords = new double[cols][3];
    topEdgeVectors = new double[cols][3];
    bottomEdgeVectors = new double[cols][3];
    for (int i = 0; i < cols; i++) {

      dataLoc.set (COL, i);

      // Top
      // ---
      int topRow = getTopRowAtColumn (i) + validStartRow;

      dataLoc.set (ROW, topRow);
      sourceTrans.transform (dataLoc, earthLoc);
      edgeECFCoord = topEdgeECFCoords[i];
      earthLoc.computeECF (edgeECFCoord);
      
      dataLoc.set (ROW, topRow+1);
      sourceTrans.transform (dataLoc, earthLoc);
      earthLoc.computeECF (innerECFCoord);

      subtract (innerECFCoord, edgeECFCoord, topEdgeVectors[i]);

      // Bottom
      // ------
      int bottomRow = validEndRow - topRow;

      dataLoc.set (ROW, bottomRow);
      sourceTrans.transform (dataLoc, earthLoc);
      edgeECFCoord = bottomEdgeECFCoords[i];
      earthLoc.computeECF (edgeECFCoord);
      
      dataLoc.set (ROW, bottomRow-1);
      sourceTrans.transform (dataLoc, earthLoc);
      earthLoc.computeECF (innerECFCoord);

      subtract (innerECFCoord, edgeECFCoord, bottomEdgeVectors[i]);

    } // for

    // Compute inwards pointing vectors along left and right edge
    // ----------------------------------------------------------
    leftEdgeECFCoords = new double[rows][3];
    rightEdgeECFCoords = new double[rows][3];
    leftEdgeVectors = new double[rows][3];
    rightEdgeVectors = new double[rows][3];
    for (int i = 0; i < rows; i++) {

      // Skip rows that start or end with deleted pixels
      // -----------------------------------------------
      int scanLine = i % 16;
      if (scanLine < 2 || scanLine > 13)
        continue;
      
      dataLoc.set (ROW, i);

      // Left
      // ----
      dataLoc.set (COL, 0);
      sourceTrans.transform (dataLoc, earthLoc);
      edgeECFCoord = leftEdgeECFCoords[i];
      earthLoc.computeECF (edgeECFCoord);
      
      dataLoc.set (COL, 1);
      sourceTrans.transform (dataLoc, earthLoc);
      earthLoc.computeECF (innerECFCoord);

      subtract (innerECFCoord, edgeECFCoord, leftEdgeVectors[i]);

      // Right
      // -----
      dataLoc.set (COL, cols-1);
      sourceTrans.transform (dataLoc, earthLoc);
      edgeECFCoord = rightEdgeECFCoords[i];
      earthLoc.computeECF (edgeECFCoord);
      
      dataLoc.set (COL, cols-2);
      sourceTrans.transform (dataLoc, earthLoc);
      earthLoc.computeECF (innerECFCoord);

      subtract (innerECFCoord, edgeECFCoord, rightEdgeVectors[i]);

    } // for

  } // VIIRSSourceImp constructor

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isValidLocation (DataLocation loc) {
  
    int sourceScanLine = (int) loc.get (ROW) % 16;
    int sourceCol = (int) loc.get (COL);

    return (!isBowtieDeleted[sourceScanLine][sourceCol]);
  
  } // isValidLocation

  ////////////////////////////////////////////////////////////

  @Override
  public int getWindowSize() { return (19); }

  ////////////////////////////////////////////////////////////

  /**
   * Holds some temporary data values used in evaluating the nearest location.
   * The purpose of this is to help reduce memory allocation in the method
   * itself and to allow for use from multiple threads.
   */
  private static class Context {

    public double[] destECFCoords = new double[3];
    public double[] sourceToDestVector = new double[3];

  } // Context class

  ////////////////////////////////////////////////////////////

  @Override
  public Object getContext() { return (new Context()); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isValidNearestLocation (
    EarthLocation earthLoc,
    DataLocation nearestDataLoc,
    Object contextObj
  ) {

    Context context = (Context) contextObj;

    // Determine if we're on an edge
    // -----------------------------
    int sourceRow = (int) nearestDataLoc.get (ROW);
    int sourceCol = (int) nearestDataLoc.get (COL);

    double[] sourceECFCoord = null;
    double[] sourceToInsideVector = null;
    double[] sourceCornerToInsideVector = null;
    boolean isEdgeLocation = true;

    // Top edge
    // --------
    int topRow = getTopRowAtColumn (sourceCol);
    int bottomRow = validEndRow - topRow;

    if (sourceRow == topRow) {
      sourceECFCoord = topEdgeECFCoords[sourceCol];
      sourceToInsideVector = topEdgeVectors[sourceCol];

      // Top-left corner
      // ---------------
      if (sourceCol == 0) {
        sourceCornerToInsideVector = leftEdgeVectors[sourceRow];
      } // if

      // Top-right corner
      // ----------------
      else if (sourceCol == sourceDims[COL]-1) {
        sourceCornerToInsideVector = rightEdgeVectors[sourceRow];
      } // else if

    } // if
    
    // Bottom edge
    // -----------
    else if (sourceRow == bottomRow) {
      sourceECFCoord = bottomEdgeECFCoords[sourceCol];
      sourceToInsideVector = bottomEdgeVectors[sourceCol];

      // Bottom-left corner
      // ------------------
      if (sourceCol == 0) {
        sourceCornerToInsideVector = leftEdgeVectors[sourceRow];
      } // if

      // Bottom-right corner
      // -------------------
      else if (sourceCol == sourceDims[COL]-1) {
        sourceCornerToInsideVector = rightEdgeVectors[sourceRow];
      } // else if

    } // else if
  
    // Left edge
    // ---------
    else if (sourceCol == 0) {
      sourceECFCoord = leftEdgeECFCoords[sourceRow];
      sourceToInsideVector = leftEdgeVectors[sourceRow];
    } // else if
  
    // Right edge
    // ----------
    else if (sourceCol == sourceDims[COL]-1) {
      sourceECFCoord = rightEdgeECFCoords[sourceRow];
      sourceToInsideVector = rightEdgeVectors[sourceRow];
    } // else if
  
    else {
      isEdgeLocation = false;
    } // else

    //  Check dest location near edge is inside source transform
    // ---------------------------------------------------------
    boolean isInsideSource = true;
    if (isEdgeLocation) {

      // Compute vector from source to dest
      // ----------------------------------
      earthLoc.computeECF (context.destECFCoords);
      subtract (context.destECFCoords, sourceECFCoord, context.sourceToDestVector);

      // Compute dot between source->dest and source->inside
      // ---------------------------------------------------
      double dotProduct = dot (context.sourceToDestVector, sourceToInsideVector);
      isInsideSource = (dotProduct > 0);

      // For a corner, check additional dot product
      // ------------------------------------------
      if (isInsideSource && sourceCornerToInsideVector != null) {
        dotProduct = dot (context.sourceToDestVector, sourceCornerToInsideVector);
        isInsideSource = (dotProduct > 0);
      } // if

    } // if

    return (isInsideSource);

  } // isValidNearestLocation

 ////////////////////////////////////////////////////////////

} // VIIRSSourceImp class

////////////////////////////////////////////////////////////////////////

