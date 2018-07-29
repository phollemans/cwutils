////////////////////////////////////////////////////////////////////////
/*

     File: MixedGridResampler.java
   Author: Peter Hollemans
     Date: 2005/01/26

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.List;
import noaa.coastwatch.util.BivariateEstimator;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.GridResampler;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.LocationFilter;

/**
 * The <code>MixedGridResampler</code> class performs generic data
 * resampling between 2D earth transforms using a mix of forward and
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

  /** The source location filter for pixel data. */
  protected LocationFilter sourceFilter;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the source location filter.  When set, the filter is used to
   * determine if a source grid location should be used in the registration.
   * By default, all source grid locations are used if they map to a
   * destination location.
   *
   * @param filter the source location filter, or null for none.
   */
  public void setSourceFilter (
    LocationFilter filter
  ) {
   
    sourceFilter = filter;

  } // setSourceFilter

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
   * @param sourceTrans the source earth transform.
   * @param destTrans the destination earth transform.
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

  /**
   * Computes the sign of the z component of the cross product between two
   * vectors given by three points in XY space.  Given points p1, p2, and p3,
   * the cross product p1p2 x p1p3 is computed, and the sign of its z component
   * is the result.
   *
   * @param x the x coordinate values of the points.
   * @param y the y coordinate values of the points.
   * @param p1 the index into x and y of p1's coordinates.
   * @param p2 the index into x and y of p2's coordinates.
   * @param p3 the index into x and y of p3's coordinates.
   *
   * @return the sign of the z component of the cross product as 0 for 
   * negative, 1 for positive.
   */
  private static int crossProductSign (
    double[] x,
    double[] y,
    int p1,
    int p2,
    int p3
  ) {
  
    double v1x = x[p2] - x[p1];
    double v1y = y[p2] - y[p1];
    double v2x = x[p3] - x[p1];
    double v2y = y[p3] - y[p1];
    double zComp = v1x*v2y - v1y*v2x;
    return (zComp < 0 ? 0 : 1);

  } // crossProductSign

  ////////////////////////////////////////////////////////////

  @Override
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
    DataLocation sourceLoc = new DataLocation (2);
    EarthLocation earthLoc = new EarthLocation();
    DataLocation destLoc = new DataLocation (2);

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
        if (verbose && rectangle%(rectangles/10) == 0) {
          int percentComplete = (int) Math.round (rectangle*100.0/rectangles);
          System.out.println (this.getClass() + ": " + percentComplete + "% complete");
        } // if

        // Set source coordinate sampling points (center of pixels)
        // --------------------------------------------------------
        /**
         * Here we sample the rectangle in the source transform using a 3x3
         * pattern from top-left to bottom-right as follows:
         * 
         *   0-----1-----2
         *   |     |     |
         *   |     |     |
         *   3-----4-----5
         *   |     |     |
         *   |     |     |
         *   6-----7-----8
         *
         */
        int rowMin = i;
        int rowMax = Math.min (i + rectHeight - 1, sourceDims[Grid.ROWS]-1);
        int colMin = j;
        int colMax = Math.min (j + rectWidth - 1, sourceDims[Grid.COLS]-1);

        sourceRows[0] = sourceRows[1] = sourceRows[2] = rowMin;
        sourceRows[3] = sourceRows[4] = sourceRows[5] = (rowMin+rowMax)/2;
        sourceRows[6] = sourceRows[7] = sourceRows[8] = rowMax;

        sourceCols[0] = sourceCols[3] = sourceCols[6] = colMin;
        sourceCols[1] = sourceCols[4] = sourceCols[7] = (colMin+colMax)/2;;
        sourceCols[2] = sourceCols[5] = sourceCols[8] = colMax;

        // Generate corresponding source/destination data locations
        // --------------------------------------------------------
        for (int k = 0; k < 9; k++) {
          sourceLoc.set (Grid.ROWS, sourceRows[k]);
          sourceLoc.set (Grid.COLS, sourceCols[k]);
          sourceTrans.transform (sourceLoc, earthLoc);
          if (!earthLoc.isValid()) continue rectLoop;
          destTrans.transform (earthLoc, destLoc);
          if (!destLoc.isValid()) continue rectLoop;
          destRows[k] = destLoc.get (Grid.ROWS);
          destCols[k] = destLoc.get (Grid.COLS);
        } // for

        // Perform cross product test on destination locations
        // ---------------------------------------------------
        /**
         * We perform a test here to determine if the source locations
         * have been translated to destination locations that make sense.
         * Given the rectangle sample points as shown above, we test that
         * the z-component of the cross products of various point pairs
         * all match in direction:
         *
         *   p0p1 x p0p3
         *   p1p2 x p1p4
         *   p3p4 x p3p6
         *   p4p5 x p4p7
         *   p8p7 x p8p5
         *
         * If the cross product vector directions do not match, we suspect
         * that there was a discontinuity in mapping the source rectangle to
         * destination and ignore this rectangle.
         */
        int sum =
          crossProductSign (destRows, destCols, 0, 1, 3) +
          crossProductSign (destRows, destCols, 1, 2, 4) +
          crossProductSign (destRows, destCols, 3, 4, 6) +
          crossProductSign (destRows, destCols, 4, 5, 7) +
          crossProductSign (destRows, destCols, 8, 7, 5);
        if (sum != 0 && sum != 5) {
          continue rectLoop;
        } // if

        // Create source polynomial estimators
        // -----------------------------------
        /**
         * Here, we generate polynomial estimators that will be used
         * to transform destination (row,col) coordinates back to the
         * source (row,col) coordinates.
         */
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
           * source transform gives us wonky earth locations.  So it's
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
            
            // Check filter function
            // ---------------------
            if (sourceFilter != null) {
              sourceLoc.set (Grid.ROWS, sourceRow);
              sourceLoc.set (Grid.COLS, sourceCol);
              if (!sourceFilter.useLocation (sourceLoc)) continue;
            } // if
            
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
