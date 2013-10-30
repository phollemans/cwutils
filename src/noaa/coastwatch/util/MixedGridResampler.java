////////////////////////////////////////////////////////////////////////
/*
     FILE: MixedGridResampler.java
  PURPOSE: A class to perform resampling of data from one projection 
           to another.
   AUTHOR: Peter Hollemans
     DATE: 2005/01/26
  CHANGES: 2005/08/31, PFH, modified destination footprint computation
             to avoid pixel dropout between rectangles
           2006/07/13, PFH, added check for closer center point

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import java.awt.geom.*;
import noaa.coastwatch.util.trans.*;

/**
 * The <code>MixedGridResampler</code> class performs generic data
 * resampling between 2D Earth transforms using a mix of forward and
 * inverse resampling methods.  The steps are as follows:
 * <ol>
 *
 *   <li>The source grid is divided into rectangles of a
 *   user-specified size.</li>
 * 
 *   <li>For each rectangle in the source, a polynomial
 *   function is derived that maps from destination grid coordinates
 *   to source grid coordinates.</li>
 * 
 *   <li>For each coordinate in each rectangle in the destination
 *   grid, a source coordinate is computed and data from the source
 *   grid transferred to the destination.</li>
 * 
 * </ol>
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class MixedGridResampler
  extends GridResampler {

  // Constants
  // ---------

  /** The never overwrite mode. */
  public static final int OVERWRITE_NEVER = 0;

  /** The always overwrite mode. */
  public static final int OVERWRITE_ALWAYS = 1;

  /** The closer overwrite mode. */
  public static final int OVERWRITE_IF_CLOSER = 2;

  // Variables
  // ---------

  /** The source coordinate rectangle width. */
  protected int rectWidth;

  /** The source coordinate rectangle height. */
  protected int rectHeight;

  /** The overwrite mode. */
  protected int overwriteMode = OVERWRITE_ALWAYS;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the overwrite mode.  If during the resampling, more than one
   * source pixel is mapped to the same destination pixel, the
   * overwrite mode is used to determine if the old value should be
   * overwritten with the new value:
   * <ul>
   *
   *   <li><code>OVERWRITE_NEVER</code> - Keep the first data value
   *   and never overwrite it.</li>
   *
   *   <li><code>OVERWRITE_ALWAYS</code> - Always overwrite the data
   *   value with a new value.  This is the default.</li>
   * 
   *   <li><code>OVERWRITE_IF_CLOSER</code> - Only overwrite the data
   *   value if the new value is closer in physical location to the
   *   destination than the previous value.</li>
   * 
   * </ul>
   * 
   * @param mode the new overwrite mode value.
   */
  public void setOverwriteMode (
    int mode
  ) {

    overwriteMode = mode;

  } // setOverwriteMode

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new grid resampler from the specified source and
   * destination transforms.
   *
   * @param sourceTrans the source Earth transform.
   * @param destTrans the destination Earth transform.
   * @param rectWidth the source rectangle width.
   * @param rectHeight the source rectangle height.
   */
  public MixedGridResampler (
    EarthTransform sourceTrans,
    EarthTransform destTrans,
    int rectWidth,
    int rectHeight
  ) {
  
    super (sourceTrans, destTrans);
    this.rectWidth = rectWidth;
    this.rectHeight = rectHeight;

  } // MixedGridResampler constructor

  ////////////////////////////////////////////////////////////

  public void perform (
    boolean verbose
  ) {

    // Check grid count
    // ----------------
    int grids = sourceGrids.size();
    if (verbose) 
      System.out.println (this.getClass() + ": Found " + grids + 
        " grid(s) for resampling");
    if (grids == 0) return;

    // Get grid arrays
    // ---------------
    Grid[] sourceArray = (Grid[]) sourceGrids.toArray (new Grid[] {});
    Grid[] destArray = (Grid[]) destGrids.toArray (new Grid[] {});

    // Get source and destination dimensions
    // -------------------------------------
    int[] sourceDims = sourceArray[0].getDimensions();
    int[] destDims = destArray[0].getDimensions();
    if (verbose) 
      System.out.println (this.getClass() + ": Resampling to " + 
        destDims[Grid.ROWS] + "x" + destDims[Grid.COLS] + " from " +
        sourceDims[Grid.ROWS] + "x" + sourceDims[Grid.COLS]);

    // Get source navigation
    // ---------------------
    AffineTransform sourceNav = sourceArray[0].getNavigation();
    if (sourceNav.isIdentity()) sourceNav = null;

    // Create working arrays
    // ---------------------
    double[] sourceRows = new double[9];
    double[] sourceCols = new double[9];
    double[] destRows = new double[9];
    double[] destCols = new double[9];
    int[] destMin = new int[2];
    int[] destMax = new int[2];
    double[] destCoords = new double[2];
    double[] sourceCoords = new double[2];
    boolean[][] target = new boolean[destDims[Grid.ROWS]][destDims[Grid.COLS]];
    float[][] roundDist = null;
    if (overwriteMode == OVERWRITE_IF_CLOSER) 
      roundDist = new float[destDims[Grid.ROWS]][destDims[Grid.COLS]];
    double[] boxData = new double[8];
    double[] edgeSourceRows = new double[9];
    double[] edgeSourceCols = new double[9];

    // Loop over each source rectangle
    // -------------------------------
    int rectangles = 
      (int) Math.ceil ((float) sourceDims[Grid.ROWS]/rectHeight) * 
      (int) Math.ceil ((float) sourceDims[Grid.COLS]/rectWidth);
    int rectangle = 0;
    for (int i = 0; i < sourceDims[Grid.ROWS]; i += rectHeight) {
      rectLoop: for (int j = 0; j < sourceDims[Grid.COLS]; j += rectWidth) {

        // Print message
        // -------------
        rectangle++;
        if (verbose && rectangle%100 == 0) {
          System.out.println (this.getClass() + 
            ": Working on rectangle [" + rectangle + "/" + rectangles + "]");
        } // if

        // Set source coordinate sampling points (center of pixels)
        // --------------------------------------------------------
        int rowMin = i;
        int rowMax = Math.min (i + rectHeight - 1, sourceDims[Grid.ROWS]-1);
        int colMin = j;
        int colMax = Math.min (j + rectWidth - 1, sourceDims[Grid.COLS]-1);
        sourceRows[0] = rowMin; 
        sourceCols[0] = colMin;
        sourceRows[1] = sourceRows[0]; 
        sourceCols[1] = (colMin+colMax)/2;
        sourceRows[2] = sourceRows[0]; 
        sourceCols[2] = colMax;
        sourceRows[3] = (rowMin+rowMax)/2;
        sourceCols[3] = sourceCols[0];
        sourceRows[4] = sourceRows[3]; 
        sourceCols[4] = sourceCols[1];
        sourceRows[5] = sourceRows[3]; 
        sourceCols[5] = sourceCols[2];
        sourceRows[6] = rowMax; 
        sourceCols[6] = sourceCols[0];
        sourceRows[7] = sourceRows[6]; 
        sourceCols[7] = sourceCols[1];
        sourceRows[8] = sourceRows[6]; 
        sourceCols[8] = sourceCols[2];

        // Create source polynomial estimators
        // -----------------------------------
        /**
         * Here, we generate polynomial estimators that will be used
         * to transform destination (row,col) coordinates back to the
         * source (row,col) coordinates.
         */
        for (int k = 0; k < 9; k++) {
          DataLocation sourceLoc = new DataLocation (sourceRows[k], 
            sourceCols[k]);
          EarthLocation earthLoc = sourceTrans.transform (sourceLoc);
          if (!earthLoc.isValid()) continue rectLoop;
          DataLocation destLoc = destTrans.transform (earthLoc);
          if (!destLoc.isValid()) continue rectLoop;
          destRows[k] = destLoc.get (Grid.ROWS);
          destCols[k] = destLoc.get (Grid.COLS);
        } // for
        BivariateEstimator[] sourceEst;
        try {
          sourceEst = new BivariateEstimator[] {
            new BivariateEstimator (destRows, destCols, sourceRows, 2),
            new BivariateEstimator (destRows, destCols, sourceCols, 2)
          };
        } // try
        catch (RuntimeException e) {
          /**
           * At this point, we are catching a matrix that had no
           * inverse in the estimator constructor.  This only really
           * happens when the rectangle to convert is too small so
           * that the source data locations are repeated.  But it may
           * happen in other unknown cases as well, for example if the
           * source transform gives us wonky Earth locations.  So it's
           * best to catch the error here and just ignore the
           * offending rectangle.
           */
          continue rectLoop;
        } // catch

        // Create destination polynomial estimators
        // ----------------------------------------
        /**
         * Here, we generate polynomial estimators that transform
         * source to destination (row,col).  This is because in the
         * next step, we want to know the actual footprint that the
         * source rectangle has in the destination grid (out to the
         * corners of the edge pixels, not just to the center of the
         * edge pixels).
         */
        BivariateEstimator[] destEst;
        try {
          destEst = new BivariateEstimator[] {
            new BivariateEstimator (sourceRows, sourceCols, destRows, 2),
            new BivariateEstimator (sourceRows, sourceCols, destCols, 2)
          };
        } // try
        catch (RuntimeException e) {
          /** 
           * Same thing here, we catch just in case the estimator
           * matrix is singular.  See the note above.
           */
          continue rectLoop;          
        } // catch

        // Set source coordinate sampling points (edge of pixels)
        // ------------------------------------------------------
        edgeSourceRows[0] = rowMin - 0.5; 
        edgeSourceCols[0] = colMin - 0.5;
        edgeSourceRows[1] = edgeSourceRows[0]; 
        edgeSourceCols[1] = (colMin+colMax)/2;
        edgeSourceRows[2] = edgeSourceRows[0]; 
        edgeSourceCols[2] = colMax + 0.5;
        edgeSourceRows[3] = (rowMin+rowMax)/2;  
        edgeSourceCols[3] = edgeSourceCols[0];
        edgeSourceRows[4] = edgeSourceRows[3]; 
        edgeSourceCols[4] = edgeSourceCols[1];
        edgeSourceRows[5] = edgeSourceRows[3]; 
        edgeSourceCols[5] = edgeSourceCols[2];
        edgeSourceRows[6] = rowMax + 0.5; 
        edgeSourceCols[6] = edgeSourceCols[0];
        edgeSourceRows[7] = edgeSourceRows[6]; 
        edgeSourceCols[7] = edgeSourceCols[1];
        edgeSourceRows[8] = edgeSourceRows[6]; 
        edgeSourceCols[8] = edgeSourceCols[2];

        // Find destination bounds
        // -----------------------
        /**
         * We need to get the footprint of the current source
         * rectangle in the destination grid.  That way, we can loop
         * over all destination pixels and ask the estimators to
         * generate a source (row,col) for us.
         */
        destMin[Grid.ROWS] = destMin[Grid.COLS] = Integer.MAX_VALUE;
        destMax[Grid.ROWS] = destMax[Grid.COLS] = Integer.MIN_VALUE;
        for (int k = 0; k < 9; k++) {
          sourceCoords[Grid.ROWS] = edgeSourceRows[k];
          sourceCoords[Grid.COLS] = edgeSourceCols[k];
          double destRow = destEst[Grid.ROWS].evaluate (sourceCoords);
          double destCol = destEst[Grid.COLS].evaluate (sourceCoords);
          if (destRow < destMin[Grid.ROWS]) 
            destMin[Grid.ROWS] = (int) Math.floor (destRow);
          if (destCol < destMin[Grid.COLS]) 
            destMin[Grid.COLS] = (int) Math.floor (destCol);
          if (destRow > destMax[Grid.ROWS]) 
            destMax[Grid.ROWS] = (int) Math.ceil (destRow);
          if (destCol > destMax[Grid.COLS]) 
            destMax[Grid.COLS] = (int) Math.ceil (destCol);
        } // for
        destMin[Grid.ROWS]--; destMin[Grid.COLS]--;
        destMax[Grid.ROWS]++; destMax[Grid.COLS]++;

        // Compute intersection
        // --------------------
        /**
         * We compute the intersection here because the footprint of
         * the source rectangle may not fall entirely within the
         * bounds of the destination grid.  This is the final setup
         * before looping over all relevant destination pixels.
         */
        if (destMin[Grid.ROWS] > destDims[Grid.ROWS]-1 ||
            destMax[Grid.ROWS] < 0 ||
            destMin[Grid.COLS] > destDims[Grid.COLS]-1 ||
            destMax[Grid.COLS] < 0)
          continue;
        int interRowMin = Math.max (destMin[Grid.ROWS], 0);
        int interColMin = Math.max (destMin[Grid.COLS], 0);
        int interRowMax = Math.min (destMax[Grid.ROWS], destDims[Grid.ROWS]-1);
        int interColMax = Math.min (destMax[Grid.COLS], destDims[Grid.COLS]-1);

        // Loop over each destination intersection location
        // ------------------------------------------------
        for (int destRow = interRowMin; destRow <= interRowMax; destRow++) {
          for (int destCol = interColMin; destCol <= interColMax; destCol++) {

            // Check if write is needed
            // ------------------------
            if (target[destRow][destCol] && overwriteMode == OVERWRITE_NEVER) 
              continue;

            // Get source location
            // -------------------
            destCoords[Grid.ROWS] = destRow;
            destCoords[Grid.COLS] = destCol;
            double dSourceRow = sourceEst[Grid.ROWS].evaluate (destCoords);
            int sourceRow = (int) Math.round (dSourceRow);
            if (sourceRow < rowMin || sourceRow > rowMax) continue;
            double dSourceCol = sourceEst[Grid.COLS].evaluate (destCoords);
            int sourceCol = (int) Math.round (dSourceCol);
            if (sourceCol < colMin || sourceCol > colMax) continue;

            // Compute and check rounding distance
            // -----------------------------------
            if (overwriteMode == OVERWRITE_IF_CLOSER) {
              float deltaRow = (float) (dSourceRow - sourceRow);
              float deltaCol = (float) (dSourceCol - sourceCol);
              float d = deltaRow*deltaRow + deltaCol*deltaCol;
              if (target[destRow][destCol] && d >= roundDist[destRow][destCol])
                continue;
              roundDist[destRow][destCol] = d;
            } // if

            // Copy pixel values for each grid
            // -------------------------------
            for (int k = 0; k < grids; k++) {
              double val = sourceArray[k].getValue (sourceRow, sourceCol);
              destArray[k].setValue (destRow, destCol, val);
            } // for
            target[destRow][destCol] = true;
            
          } // for
        } // for

      } // for
    } // for

    // Correct single pixel resampling failures
    // ----------------------------------------
    if (verbose)
      System.out.println (this.getClass() + 
        ": Interpolating single pixel gaps");
    for (int i = 0; i < destDims[Grid.ROWS]; i++) {
      for (int j = 0; j < destDims[Grid.COLS]; j++) {
        if (!target[i][j]) {

          // Determine if pixel is surrounded
          // --------------------------------
          boolean singlePixel = true;
          for (int iOff = -1; iOff <= 1; iOff++) {
            for (int jOff = -1; jOff <= 1; jOff++) {
              if (iOff == 0 && jOff == 0) continue;
              try { if (!target[i+iOff][j+jOff]) { singlePixel = false; } }
              catch (ArrayIndexOutOfBoundsException e) { }
            } // for
          } // for

          // If so, fill with median value
          // -----------------------------
          if (singlePixel) {
            for (int k = 0; k < grids; k++) {
              int dataCount = 0;
              int nanCount = 0;
              for (int iOff = -1; iOff <= 1; iOff++) {
                for (int jOff = -1; jOff <= 1; jOff++) {
                  if (iOff == 0 && jOff == 0) continue;
                  boxData[dataCount] = destArray[k].getValue (i+iOff, j+jOff);
                  if (Double.isNaN (boxData[dataCount])) nanCount++;
                  dataCount++;
                } // for
              } // for
              Arrays.sort (boxData);
              double median = boxData[(8-nanCount)/2];
              destArray[k].setValue (i, j, median);
            } // for
          } // if

        } // if
      } // for
    } // for

  } // perform

  ////////////////////////////////////////////////////////////

} // MixedGridResampler class

////////////////////////////////////////////////////////////////////////
