////////////////////////////////////////////////////////////////////////
/*
     FILE: InverseGridResampler.java
  PURPOSE: A class to perform resampling of data from one projection 
           to another.
   AUTHOR: Peter Hollemans
     DATE: 2005/01/26
  CHANGES: 2014/02/12, PFH
           - Changes: Modified perform() method to minimize dynamic memory
             allocation.
           - Issue: The nested for loops in perform() were allocating a number
             of objects for every iteration.  We suspect the loop would run 
             more efficiently without the overhead of creating new objects
             every time through.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

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
 * The <code>InverseGridResampler</code> class performs generic data
 * resampling between 2D Earth transforms using an inverse location
 * lookup method.  The steps are as follows:
 * <ol>
 *
 *   <li>The destination grid is divided into rectangles of a bounded
 *   physical size.</li>
 * 
 *   <li>For each rectangle in the destination grid, a polynomial
 *   function is derived that maps from destination grid coordinates
 *   to source grid coordinates.</li>
 * 
 *   <li>For each coordinate in the destination grid, a source
 *   coordinate is computed and data from the source grid transferred
 *   to the destination.</li>
 * 
 * </ol>
 *
 * WARNING: This class is not thread-safe.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class InverseGridResampler 
  extends GridResampler {

  // Variables
  // ---------

  /** The polynomial size for the location estimation. */
  private double polySize;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new grid resampler from the specified source and
   * destination transforms.
   *
   * @param sourceTrans the source Earth transform.
   * @param destTrans the destination Earth transform.
   * @param polySize the estimation polynomial size in kilometers.
   *
   * @see LocationEstimator
   */
  public InverseGridResampler (
    EarthTransform sourceTrans,
    EarthTransform destTrans,
    double polySize 
  ) {
  
    super (sourceTrans, destTrans);
    this.polySize = polySize;

  } // InverseGridResampler constructor

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

    // Create location estimator
    // -------------------------
    int[] sourceDims = sourceArray[0].getDimensions();
    int[] destDims = destArray[0].getDimensions();
    if (verbose) 
      System.out.println (this.getClass() + ": Resampling to " + 
        destDims[Grid.ROWS] + "x" + destDims[Grid.COLS] + " from " +
        sourceDims[Grid.ROWS] + "x" + sourceDims[Grid.COLS]);
    AffineTransform sourceNav = sourceArray[0].getNavigation();
    if (sourceNav.isIdentity()) sourceNav = null;
    if (verbose) 
      System.out.println (this.getClass() + ": Creating location estimators");
    LocationEstimator estimator = new LocationEstimator (destTrans,
      destDims, sourceTrans, sourceDims, sourceNav, polySize);

    // Loop over each destination location
    // -----------------------------------
    DataLocation destLoc = new DataLocation (2);
    DataLocation sourceLoc = new DataLocation (2);
    EarthLocation earthLoc = new EarthLocation();
    for (int i = 0; i < destDims[Grid.ROWS]; i++) {
      if (verbose && i%100 == 0)
        System.out.println (this.getClass() + ": Working on output row " + i);
      for (int j = 0; j < destDims[Grid.COLS]; j++) {

        // Get source location
        // -------------------
        destLoc.set (Grid.ROWS, i);
        destLoc.set (Grid.COLS, j);
        destTrans.transform (destLoc, earthLoc);
        boolean isDestValid = earthLoc.isValid();
        boolean isSourceValid = false;
        if (isDestValid) {
          estimator.getLocation (destLoc, sourceLoc);
          isSourceValid = (sourceLoc.isValid() && sourceLoc.isContained (sourceDims));
        } // if

        // Copy data value
        // ---------------
        if (isSourceValid) {
 
          // Get nearest neighbour source coordinate
          // ---------------------------------------
          int sourceRow = (int) Math.round (sourceLoc.get (Grid.ROWS));
          int sourceCol = (int) Math.round (sourceLoc.get (Grid.COLS));
                 
          // Loop over each grid
          // -------------------
          for (int k = 0; k < grids; k++) {
            double val = sourceArray[k].getValue (sourceRow, sourceCol);
            destArray[k].setValue (i, j, val);
          } // for

        } // if

        // Set missing value
        // -----------------
        else {

          // Loop over each grid
          // -------------------
          for (int k = 0; k < grids; k++) {
            destArray[k].setValue (i, j, Double.NaN);
          } // for

        } // else

      } // for
    } // for

  } // perform

  ////////////////////////////////////////////////////////////

} // InverseGridResampler class

////////////////////////////////////////////////////////////////////////
