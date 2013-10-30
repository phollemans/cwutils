////////////////////////////////////////////////////////////////////////
/*
     FILE: NavigationOffsetEstimator.java
  PURPOSE: Estimates navigation errors using image data.
   AUTHOR: Peter Hollemans
     DATE: 2005/02/10
  CHANGES: 2005/05/08, PFH, added class fraction threshold and
           rearranged for expanded search space
           2005/05/11, PFH, added search sanity test

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.text.*;
import java.util.*;
import java.awt.geom.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.trans.*;

/**
 * The <code>NavigationOffsetEstimator</code> class uses image and
 * land mask data to estimate the navigation error in a small tile of
 * coastal image data.  Success of the estimation is largely dependent
 * on the separability of the image data into two classes: land and
 * water.  A good class separation is achieved when the pixels in each
 * class differ significantly, such as for a daytime scene of low
 * albedo water pixels and high albedo land pixels, or a high thermal
 * contrast scene at night.  The class separation is done using a
 * histogram splitting method, and then the navigation error is
 * determined by "shifting" the image data around to find the maximum
 * correlation with a precomputed land mask.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class NavigationOffsetEstimator {

  // Constants
  // ---------

  /** The maximum iterations for the theshold finding loop. */
  private static final int MAX_ITER = 20;

  // Variables
  // ---------

  /** The minimum allowed class separation distance in standard dev units. */
  private double minStdevDist = 2.5;

  /** The minimum allowed image/land class correlation. */
  private double minCorrelation = 0.95;

  /** The minimum class fraction. */
  private double minClassFraction = 0.05;

  /** The verbose flag, true to print verbose output during estimation. */
  private boolean verbose;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the minimum distance in standard deviation units for
   * successful histogram splitting.  The histogram splitting step
   * divides the image data into two classes, c1 and c2, with mean and
   * standard deviations (u1, s1) and (u2, s2) respectively.  The
   * threshold value t divides the classes, where all pixels in c1 are
   * less than t and all pixels in c2 are greater than t.  The measure
   * of class separation is given by the distance d(t) in standard
   * deviation units, d(t) = min ((t-u1)/s1, (u2-t)/s2).  The optimal
   * histogram splitting threshold topt is the value of t that
   * maximizes d(t), dmax = d(topt).  By default, histogram splitting
   * fails if dmax is less than 2.5, as this indicates that the
   * classes are not sufficiently "distinct" for a reliable navigation
   * offset estimation.
   *
   * @param dist the new minimum distance in standard deviation units.
   */
  public void setMinStdevDist (double dist) { minStdevDist = dist; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the minimum image correlation value for successful offset
   * estimation.  By default, image pixels separated into land and
   * water classes by the histogram splitting step must match the
   * precomputed land mask with a correlation of at least 0.95 at some
   * offset value in order to for the offset to be reported.
   *
   * @param corr the new minimum correlation value in the range
   * [0..1].
   */
  public void setMinCorrelation (double corr) { minCorrelation = corr; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the minimum class pixels as a fraction of total image
   * pixels.  Be default, image pixels in either the land or water
   * classes must make up at least 5% of the image pixels in order
   * for the navigation box to be used in the correlation.
   *
   * @param frac the new minimum class fraction in the range
   * [0..1].
   */
  public void setMinFraction (double frac) { minClassFraction = frac; }

  ////////////////////////////////////////////////////////////

  /** Sets the verbose flag to print verbose output during estimation. */
  public void setVerbose (boolean flag) { verbose = flag; }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the bimodal histogram statistics.
   * 
   * @param data the data array.
   * @param rows the data rows (first index of data array).
   * @param cols the data columns (second index of data array).
   * @param thresh the histogram splitting threshold.
   * @param stats the statistics array (modified) as [u1, s1, u2, s2].
   */
  /** Gets the bimodal histogram statistics. */
  private void getBimodalStats (
    double[][] data, 
    int rows, 
    int cols, 
    double thresh,
    double[] stats
  ) {

    // Compute mean of each class
    // --------------------------
    int[] n = new int[2];
    double[] meanSum = new double[2];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        int index = (data[i][j] < thresh ? 0 : 1);
        meanSum[index] += data[i][j];
        n[index]++;
      } // for
    } // for
    double[] mean = new double[] {meanSum[0]/n[0], meanSum[1]/n[1]};

    // Compute standard dev of each class
    // ----------------------------------
    double[] stdevSum1 = new double[2];
    double[] stdevSum2 = new double[2];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        int index = (data[i][j] < thresh ? 0 : 1);
        double diff = data[i][j] - mean[index];
        stdevSum1[index] += Math.pow (diff, 2);
        stdevSum2[index] += diff;
      } // for
    } // for
    double[] stdev = new double[] {
      Math.sqrt ((stdevSum1[0] - Math.pow (stdevSum2[0], 2)/n[0])/(n[0]-1)),
      Math.sqrt ((stdevSum1[1] - Math.pow (stdevSum2[1], 2)/n[1])/(n[1]-1))
    };

    // Fill stats array
    // ----------------
    stats[0] = mean[0];
    stats[1] = stdev[0];
    stats[2] = mean[1];
    stats[3] = stdev[1];

  } // getBimodalStats

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the class separation distance in standard deviation units.
   * 
   * @param data the data array.
   * @param rows the data rows (first index of data array).
   * @param cols the data columns (second index of data array).
   * @param thresh the histogram splitting threshold.
   * @param stats the statistics array (modified) as [u1, s1, u2, s2].
   * 
   * @return the class separation distance, d(t) = min ((t-u1)/s1,
   * (u2-t)/s2).
   */
  private double getStdevDist (
    double[][] data, 
    int rows, 
    int cols, 
    double thresh,
    double[] stats
  ) {  

    getBimodalStats (data, rows, cols, thresh, stats);
    double d1 = (thresh - stats[0])/stats[1];
    double d2 = (stats[2] - thresh)/stats[3];
    double d = Math.min (d1, d2);
    return (d);

  } // getStdevDist

  ////////////////////////////////////////////////////////////

  /**
   * Gets the specified pixel data from the grid.
   *
   * @param grid the grid to use for data values.
   * @param min the top-left data coordinates.
   * @param boxHeight the navigation box height in data dimensions.
   * @param boxWidth the navigation box width in data dimensions.
   *
   * @return the array of navigation box pixel data.
   */
  private double[][] getPixelData (
    Grid grid,
    int[] min,
    int boxHeight,
    int boxWidth
  ) {

    double[][] data = new double[boxHeight][boxWidth];
    DataLocation loc = new DataLocation (0, 0);
    for (int i = 0; i < boxHeight; i++) {
      for (int j = 0; j < boxWidth; j++) {
        loc.set (Grid.ROWS, i+min[0]);
        loc.set (Grid.COLS, j+min[1]);
        data[i][j] = grid.getValue (loc);
      } // for
    } // for

    return (data);

  } // getPixelData

  ////////////////////////////////////////////////////////////

  /**
   * Gets the specified land data from the landmask database.
   *
   * @param trans the Earth transform for data coordinates.
   * @param min the top-left data coordinates.
   * @param landHeight the land box height in data dimensions.
   * @param landWidth the land box width in data dimensions.
   *
   * @return the array of land box flag data.
   */
  private boolean[][] getLandData (
    EarthTransform trans,
    int[] min,
    int landHeight,
    int landWidth
  ) {

    LandMask landMask = LandMask.getInstance();
    boolean[][] land = new boolean[landHeight][landWidth];
    DataLocation loc = new DataLocation (0, 0);
    for (int i = 0; i < landHeight; i++) {
      for (int j = 0; j < landWidth; j++) {
        loc.set (Grid.ROWS, i+min[0]);
        loc.set (Grid.COLS, j+min[1]);
        land[i][j] = landMask.isLand (trans.transform (loc));
      } // for
    } // for

    return (land);

  } // getLandData

  ////////////////////////////////////////////////////////////

  /**
   * Gets the class separation threshold for the specified data.
   *
   * @param data the navigation box pixel data.
   * @param boxHeight the navigation box height in data dimensions.
   * @param boxWidth the navigation box width in data dimensions.
   *
   * @return the class threshold, or Double.NaN if the class
   * separation failed.
   */
  private double getClassThreshold (
    double[][] data,
    int boxHeight,
    int boxWidth
  ) {

    // Get initial stats
    // -----------------
    double[] stats = new double[4];
    getBimodalStats (data, boxHeight, boxWidth, Double.MAX_VALUE, stats);

    // Find approximate threshold
    // --------------------------
    double thresh = stats[0];
    double lastThresh = thresh*2;
    int n = 0;
    while (Math.abs ((thresh - lastThresh)/thresh) > 0.01 && n < MAX_ITER) {
      getBimodalStats (data, boxHeight, boxWidth, thresh, stats);
      lastThresh = thresh;
      thresh = (stats[0] + stats[2]) / 2;
      n++;
    } // while

    // Find optimal threshold
    // ----------------------
    double dist = 0;
    double lowerBound = stats[0];
    double upperBound = stats[2];
    lastThresh = thresh*2;
    n = 0;
    while (Math.abs ((thresh - lastThresh)/thresh) > 0.01 && n < MAX_ITER) {

      // Find max in bounds
      // ------------------
      double step = (upperBound - lowerBound) / 100;
      double maxDist = 0, maxT = 0;
      for (double t = lowerBound; t < upperBound; t += step) {
        double testDist = getStdevDist (data, boxHeight, boxWidth, t, stats);
        if (testDist > maxDist) {
          maxDist = testDist;
          maxT = t;
        } // if
      } // for

      // Adjust bounds
      // -------------
      double center = (lowerBound + upperBound)/2;
      if (maxT > center)
        lowerBound = center;
      else
        upperBound = center;

      // Compute new threshold
      // ---------------------
      lastThresh = thresh;
      thresh = maxT;
      dist = getStdevDist (data, boxHeight, boxWidth, thresh, stats);
      if (Double.isNaN (dist) || Double.isInfinite (dist)) break; 
      n++;

    } // while

    // Check for sufficient distance
    // -----------------------------
    DecimalFormat fmt = new DecimalFormat ("0.###");
    if (verbose) {
      System.out.println (this.getClass() + 
        ": Land/water class separation distance = " + fmt.format (dist));
    } // if
    if (Double.isNaN (dist) || Double.isInfinite (dist) || dist<minStdevDist) {
      if (verbose)
        System.out.println (this.getClass() + ": Insufficient separation");
      return (Double.NaN);
    } // if

    // Check for sufficient class fraction
    // -----------------------------------
    int[] classCount = new int[2];
    for (int i = 0; i < boxHeight; i++) {
      for (int j = 0; j < boxWidth; j++) {
        if (data[i][j] < thresh) classCount[0]++;
        else classCount[1]++;
      } // for
    } // for
    double[] classFraction = new double[2];
    int totalCount = boxWidth*boxHeight;
    classFraction[0] = (double) classCount[0] / totalCount;
    classFraction[1] = (double) classCount[1] / totalCount;
    if (verbose) {
      System.out.println (this.getClass() + 
        ": Class fraction = (" + fmt.format (classFraction[0]) + ", " + 
        fmt.format (classFraction[1]) + ")");
    } // if
    if (classFraction[0] < minClassFraction || classFraction[1] < 
      minClassFraction) {
      if (verbose)
        System.out.println (this.getClass() + ": Insufficient class fraction");
      return (Double.NaN);
    } // if

    return (thresh);

  } // getClassThreshold

  ////////////////////////////////////////////////////////////

  /**
   * Gets the image correlation offset for the specified data.
   * 
   * @param data the navigation box pixel data.
   * @param thresh the land/water class separation threshold.
   * @param boxHeight the navigation box height in data dimensions.
   * @param boxWidth the navigation box width in data dimensions.
   * @param land the land box flag data.
   * @param maxRowOffset maximum offset in the rows direction.
   * @param maxColOffset maximum offset in the columns direction.
   * @param correlation the correlation coefficient (modified) or
   * null.  If the correlation failed, the returned content is
   * undefined.
   *
   * @return the offset of maximum correlation, or null if correlation
   * failed.
   */
  private int[] getMaxCorrelationOffset (
    double[][] data,
    double thresh,
    int boxHeight,
    int boxWidth,
    boolean[][] land,
    int maxRowOffset,
    int maxColOffset,
    double[] correlation
  ) {

    // Find maximum image correlation
    // ------------------------------
    int[] maxCorr = new int[2];
    int[] maxCorrRowOffset = new int[2];
    int[] maxCorrColOffset = new int[2];
    for (int rowOffset = -maxRowOffset; rowOffset <= maxRowOffset; 
      rowOffset++) {
      for (int colOffset = -maxColOffset; colOffset <= maxColOffset; 
        colOffset++) {
        int[] corr = new int[2];
        for (int i = 0; i < boxHeight; i++) {
          for (int j = 0; j < boxWidth; j++) {
            boolean highIsLand = (data[i][j] > thresh);
            boolean isLand = 
              land[i-rowOffset+maxRowOffset][j-colOffset+maxColOffset];
            corr[0] += (highIsLand == isLand ? 1 : 0);
            corr[1] += (!highIsLand == isLand ? 1 : 0);
          } // for
        } // for
        for (int k = 0; k < 2; k++) {
          if (corr[k] > maxCorr[k]) {
            maxCorr[k] = corr[k];
            maxCorrRowOffset[k] = rowOffset;
            maxCorrColOffset[k] = colOffset;
          } // if
        } // for
      } // for
    } // for
    int index = (maxCorr[0] > maxCorr[1] ? 0 : 1);
    double finalCorr = (double) maxCorr[index] / (boxWidth*boxHeight);
    int finalRowOffset = maxCorrRowOffset[index];
    int finalColOffset = maxCorrColOffset[index];
    if (correlation != null) correlation[0] = finalCorr;

    // Check for max correlation at max offset
    // ---------------------------------------
    boolean maxOffsetProblem = (Math.abs (finalRowOffset) == maxRowOffset) || 
      (Math.abs (finalColOffset) == maxColOffset);

    // Check correlation threshold
    // ---------------------------
    DecimalFormat fmt = new DecimalFormat ("0.###");
    if (verbose) {
      System.out.println (this.getClass() + ": Image correlation = " + 
        fmt.format (finalCorr) + " at " + (maxOffsetProblem ? "max offset" : 
        "offset = (" + finalRowOffset + ", " + finalColOffset + ")"));
    } // if
    if (finalCorr > minCorrelation && !maxOffsetProblem)
      return (new int[] {finalRowOffset, finalColOffset});
    else {
      if (verbose) {
        if (maxOffsetProblem) 
          System.out.println (this.getClass() + ": Rejected max offset");
        else
          System.out.println (this.getClass() + ": Insufficient correlation");
      } // if
      return (null);
    } // else

  } // getMaxCorrelationOffset

  ////////////////////////////////////////////////////////////

  /**
   * Gets the offset estimate for the specified Earth location.
   *
   * @param grid the grid to use for data values.
   * @param trans the Earth transform for the data grid.
   * @param earthLoc the center location for the navigation box.
   * @param boxHeight the navigation box height in data dimensions.
   * @param boxWidth the navigation box width in data dimensions.
   * @param searchLevel the search level, starting from 0.  A search
   * level of 0 searches using only image data inside the navigation
   * box.  A search level of n increases the search area to (n+1)^2
   * times the size of the navigation box, with a corresponding
   * increase in algorithm run time.
   *
   * @return the offset estimate, or null if the class separation or
   * image correlation failed.
   */
  public int[] getOffset (
    Grid grid,
    EarthTransform trans,
    EarthLocation earthLoc,
    int boxHeight,
    int boxWidth,
    int searchLevel
  ) {

    // Prepare list of guess navigations
    // ---------------------------------
    List guessNavList = new ArrayList();
    List guessOffsetList = new ArrayList();
    AffineTransform gridNav = grid.getNavigation();
    for (int k = 0; k <= searchLevel; k++) {
      int minRowOffset = -(boxHeight/2)*k;
      int minColOffset = -(boxWidth/2)*k;
      for (int i = 0; i <= k; i++) {
        for (int j = 0; j <= k; j++) {
          if (i != 0 && i != k && j != 0 && j != k) continue;
          int[] guessOffset = new int[] {
            minRowOffset + i*boxHeight,
            minColOffset + j*boxWidth
          };
          AffineTransform guessNav = AffineTransform.getTranslateInstance (
            guessOffset[0], guessOffset[1]);
          guessNav.concatenate (gridNav);
          guessNavList.add (guessNav);
          guessOffsetList.add (guessOffset);
        } // for
      } // for
    } // for

    // Prepare pixel data constants
    // ----------------------------
    DataLocation dataLoc = trans.transform (earthLoc).round();
    int[] min = new int[] {(int)dataLoc.get(Grid.ROWS) - boxHeight/2, 
      (int)dataLoc.get(Grid.COLS) - boxWidth/2};
    double[][] data = null;

    // Prepare land data constants
    // ---------------------------
    int maxRowOffset = boxHeight/2;
    int maxColOffset = boxWidth/2;
    int landHeight = boxHeight + maxRowOffset*2;
    int landWidth = boxWidth + maxColOffset*2;
    int[] landMin = new int[] {min[0] - maxRowOffset, min[1] - maxColOffset};
    boolean[][] land = null;

    // Loop over each guess navigation
    // -------------------------------
    int[] bestOffset = null;
    double[] corr = new double[1];
    double maxCorr = 0;
    for (int i = 0; i < guessNavList.size(); i++) {

      // Get pixel data
      // --------------
      int[] guessOffset = (int[]) guessOffsetList.get (i);
      if (verbose)
        System.out.println (this.getClass() + ": Rough navigation offset = (" +
          guessOffset[0] + ", " + guessOffset[1] + ")");
      grid.setNavigation ((AffineTransform) guessNavList.get (i));
      data = getPixelData (grid, min, boxHeight, boxWidth);

      // Get class separation threshold
      // ------------------------------
      double thresh = getClassThreshold (data, boxHeight, boxWidth);
      if (Double.isNaN (thresh)) {
        if (verbose)
          System.out.println (this.getClass() + ": Class separation failed");
        continue;
      } // if
    
      // Get land data array
      // -------------------
      if (land == null) 
        land = getLandData (trans, landMin, landHeight, landWidth);
    
      // Find maximum image correlation offset
      // -------------------------------------
      int[] offset = getMaxCorrelationOffset (data, thresh, boxHeight, 
        boxWidth, land, maxRowOffset, maxColOffset, corr);
      if (offset == null) {
        if (verbose)
          System.out.println (this.getClass() + ": Image correlation failed");
        continue;
      } // if

      // Adjust offset for navigation guess
      // ----------------------------------
      if (corr[0] > maxCorr) {
        maxCorr = corr[0];
        offset[0] += guessOffset[0];
        offset[1] += guessOffset[1];
        bestOffset = offset;
      } // if

    } // for

    // Apply search sanity test
    // ------------------------
    if (bestOffset != null && searchLevel > 0) {
      AffineTransform testNav = AffineTransform.getTranslateInstance (
        bestOffset[0], bestOffset[1]);
      testNav.concatenate (gridNav);
      grid.setNavigation (testNav);
      boolean savedVerbose = verbose;
      verbose = false;
      int[] testOffset = getOffset (grid, trans, earthLoc, boxHeight, 
        boxWidth, 0);
      verbose = savedVerbose;
      if (testOffset == null) {
        if (verbose)
          System.out.println (this.getClass() + ": Search sanity test failed");
        bestOffset = null;
      } // if
    } // if

    // Reset grid navigation and return
    // --------------------------------
    grid.setNavigation (gridNav);
    return (bestOffset);

  } // getOffset

  ////////////////////////////////////////////////////////////

} // NavigationOffsetEstimator

////////////////////////////////////////////////////////////////////////
