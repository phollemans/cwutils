
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
import noaa.coastwatch.util.sensor.SensorIdentifier.Sensor;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * The <code>VIIRSSourceImp</code> helps resample VIIRS data using a specific
 * set of VIIRS sensor parameters.
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
   * The deletion pattern array of size [scanHeight][scanWidth], true for
   * locations deleted.
   */
  private boolean[][] isDeletedPixel;
  
  /** The true starting row of the source after any invalid rows. */
  private int validStartRow;
  
  /** The true ending row of the source before any invalid rows. */
  private int validEndRow;

  /** The VIIRS sensor-specific parameters for this resampling. */
  private VIIRSSensorParams sensorParams;

  ////////////////////////////////////////////////////////////

  /** Computes vector from b to a, result_k = a_k - b_k. */
  private static void subtract (double[] a, double[] b, double[] result) {
  
    for (int k = 0; k < 3; k++)
      result[k] = a[k] - b[k];

  } // subtract

  ////////////////////////////////////////////////////////////

  /** Computes dot product, result = sum_k (a_k * b_k). */
  private static double dot (double[] a, double[] b) {
  
    double result = 0;
    for (int k = 0; k < 3; k++)
      result += a[k] * b[k];

    return (result);
    
  } // dot

  ////////////////////////////////////////////////////////////

  /** Computes magnitude, result = sqrt (sum_k (a_k^2)). */
  private static double magnitude (double[] a) {

    double result = Math.sqrt (dot (a, a));
    return (result);

  } // magnitude

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new VIIRS resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for VIIRS swath location
   * data.
   * @param sensorParams the VIIRS sensor parameters to use.
   *
   * @return the new resampling helper.
   */
  public static VIIRSSourceImp getInstance (
    EarthTransform sourceTrans,
    VIIRSSensorParams sensorParams
  ) {

    // Check dimensions
    // ----------------
    int[] sourceDims = sourceTrans.getDimensions();
    if (sourceDims[ROW]%sensorParams.getScanHeight() != 0)
      throw new RuntimeException ("Invalid number of rows for VIIRS scan");
    if (sourceDims[COL] != sensorParams.getScanWidth())
      throw new RuntimeException ("Invalid number of columns for VIIRS scan");

    return (new VIIRSSourceImp (sourceTrans, sensorParams));
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new VIIRS resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for VIIRS swath location
   * data.
   * @param sensorParams the VIIRS sensor parameters to use.
   */
  private VIIRSSourceImp (
    EarthTransform sourceTrans,
    VIIRSSensorParams sensorParams
  ) {

    this.sourceTrans = sourceTrans;
    this.sensorParams = sensorParams;
    
    // Get dimensions
    // --------------
    sourceDims = sourceTrans.getDimensions();
    int rows = sourceDims[ROW];
    int cols = sourceDims[COL];

    // Get pixel deletion pattern
    // --------------------------
    isDeletedPixel = sensorParams.getDeletionPattern();

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

    if (validStartRow%sensorParams.getScanHeight() != 0)
      throw new RuntimeException ("First valid row not on full VIIRS scan boundary");
    if (invalidEndRows%sensorParams.getScanHeight() != 0)
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
      int topRow = sensorParams.getTopRowAtColumn (i) + validStartRow;

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
      int scanLine = i % sensorParams.getScanHeight();
      if (isDeletedPixel[scanLine][0] || isDeletedPixel[scanLine][sensorParams.getScanWidth()-1])
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
  
    int sourceScanLine = (int) loc.get (ROW) % sensorParams.getScanHeight();
    int sourceCol = (int) loc.get (COL);

    return (!isDeletedPixel[sourceScanLine][sourceCol]);
  
  } // isValidLocation

  ////////////////////////////////////////////////////////////

  @Override
  public int getWindowSize() { return ((int) (sensorParams.getScanHeight() * 1.2)); }

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
    int topRow = sensorParams.getTopRowAtColumn (sourceCol);
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

      // We add a little on here to take account of the edge pixel outer radius
      // d = source to dest vector
      // i = source to inside vector
      // d' = d + 1/2 i
      // d' . i > 0 when dest is inside outer boundary of edge pixel
      // Same goes for the corner case below
      dotProduct += 0.5 * dot (sourceToInsideVector, sourceToInsideVector);
      isInsideSource = (dotProduct > 0);

      // For a corner, check additional dot product
      // ------------------------------------------
      if (isInsideSource && sourceCornerToInsideVector != null) {
        dotProduct = dot (context.sourceToDestVector, sourceCornerToInsideVector);
        dotProduct += 0.5 * dot (sourceCornerToInsideVector, sourceCornerToInsideVector);
        isInsideSource = (dotProduct > 0);
      } // if

    } // if

    return (isInsideSource);

  } // isValidNearestLocation

 ////////////////////////////////////////////////////////////

} // VIIRSSourceImp class

////////////////////////////////////////////////////////////////////////

