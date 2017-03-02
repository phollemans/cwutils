////////////////////////////////////////////////////////////////////////
/*

     File: InverseGridResampler.java
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
import java.util.List;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.GridResampler;
import noaa.coastwatch.util.LocationEstimator;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>InverseGridResampler</code> class performs generic data
 * resampling between 2D earth transforms using an inverse location
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
   * @param sourceTrans the source earth transform.
   * @param destTrans the destination earth transform.
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
      System.out.println (this.getClass() + ": Creating location estimators with poly size " + polySize + " km");
    LocationEstimator estimator = new LocationEstimator (destTrans,
      destDims, sourceTrans, sourceDims, sourceNav, polySize);
    if (verbose) 
      System.out.println (this.getClass() + ": Location estimators complete, starting resampling");

    // Set up source location bounds
    // -----------------------------
    DataLocation sourceLocMin = new DataLocation (-0.5, -0.5);
    DataLocation sourceLocMax = new DataLocation (
      sourceDims[Grid.ROWS]-0.5, sourceDims[Grid.COLS]-0.5);

    // Loop over each destination location
    // -----------------------------------
    DataLocation destLoc = new DataLocation (2);
    DataLocation sourceLoc = new DataLocation (2);
    EarthLocation earthLoc = new EarthLocation();
    
    // TODO: In order to see how to improve the runtime of this routine,
    // we did a timing experiment and in one case, found that 40% of the
    // loop time was used for the source location calculation, and
    // 60% for copying the data values over to the new grids, when registering
    // 20 grids.  We could reduce these times by speeding up the location
    // estimator, using parallel computing, or by operating on tiles of data
    // so as to reduce the number of I/O calls.

    for (int i = 0; i < destDims[Grid.ROWS]; i++) {

      // Print progress
      // --------------
      if (verbose && (i+1)%(destDims[Grid.ROWS]/10) == 0) {
        int percentComplete = (int) Math.round (((i+1)*100.0/destDims[Grid.ROWS]));
        System.out.println (this.getClass() + ": " + percentComplete + "% complete");
      } // if

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
          isSourceValid = (sourceLoc.isValid() &&
            sourceLoc.isContained (sourceLocMin, sourceLocMax));
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
