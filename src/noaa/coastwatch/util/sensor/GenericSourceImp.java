////////////////////////////////////////////////////////////////////////
/*

     File: GenericSourceImp.java
   Author: Peter Hollemans
     Date: 2019/03/14

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
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.ResamplingSourceImp;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * <p>The <code>GenericSourceImp</code> helps resample generic sensor
 * satellite data.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class GenericSourceImp implements ResamplingSourceImp {

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
   * Creates a new generic resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for generic swath location
   * data.
   *
   * @return the new resampling helper.
   */
  public static GenericSourceImp getInstance (
    EarthTransform sourceTrans
  ) {
  
    return (new GenericSourceImp (sourceTrans));
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new Generic resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for Generic swath location
   * data.
   */
  protected GenericSourceImp (
    EarthTransform sourceTrans
  ) {

    this.sourceTrans = sourceTrans;

    sourceDims = sourceTrans.getDimensions();
    int rows = sourceDims[ROW];
    int cols = sourceDims[COL];

    EarthLocation earthLoc = new EarthLocation();
    DataLocation dataLoc = new DataLocation (2);
    double[] edgeECFCoord;
    double[] innerECFCoord = new double[3];

    // Compute inward pointing vectors along top and bottom edge
    // ---------------------------------------------------------
    topEdgeECFCoords = new double[cols][3];
    bottomEdgeECFCoords = new double[cols][3];
    topEdgeVectors = new double[cols][3];
    bottomEdgeVectors = new double[cols][3];
    for (int i = 0; i < cols; i++) {

      dataLoc.set (COL, i);

      // Top
      // ---
      dataLoc.set (ROW, 0);
      sourceTrans.transform (dataLoc, earthLoc);
      edgeECFCoord = topEdgeECFCoords[i];
      earthLoc.computeECF (edgeECFCoord);
      
      dataLoc.set (ROW, 1);
      sourceTrans.transform (dataLoc, earthLoc);
      earthLoc.computeECF (innerECFCoord);

      subtract (innerECFCoord, edgeECFCoord, topEdgeVectors[i]);

      // Bottom
      // ------
      dataLoc.set (ROW, rows-1);
      sourceTrans.transform (dataLoc, earthLoc);
      edgeECFCoord = bottomEdgeECFCoords[i];
      earthLoc.computeECF (edgeECFCoord);
      
      dataLoc.set (ROW, rows-2);
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

  } // GenericSourceImp constructor

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isValidLocation (DataLocation loc) { return (true); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getWindowSize() { return (3); }

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
    if (sourceRow == 0) {
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
    else if (sourceRow == sourceDims[ROW]-1) {
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

} // GenericSourceImp class

////////////////////////////////////////////////////////////////////////


