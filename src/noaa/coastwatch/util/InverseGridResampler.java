////////////////////////////////////////////////////////////////////////
/*
     FILE: InverseGridResampler.java
  PURPOSE: A class to perform resampling of data from one projection 
           to another.
   AUTHOR: Peter Hollemans
     DATE: 2005/01/26
  CHANGES: n/a

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

    // TODO: This loop needs to be made more efficient.  One way would
    // be to eliminate the dynamic memory allocation performed when
    // creating and transforming each new DataLocation.  That would
    // require changes here, and in LocationEstimator, and possibly
    // EarthTransform by adding transform() methods that take two
    // arguments -- a source and destination for the transformed
    // point.

    // Loop over each destination location
    // -----------------------------------
    for (int i = 0; i < destDims[Grid.ROWS]; i++) {
      if (verbose && i%100 == 0)
        System.out.println (this.getClass() + ": Working on output row " + i);
      for (int j = 0; j < destDims[Grid.COLS]; j++) {

        // Get source location
        // -------------------
        DataLocation destLoc = new DataLocation (i, j);
        boolean destValid = destTrans.transform(destLoc).isValid();
        DataLocation sourceLoc = (destValid ? 
          estimator.getLocation (destLoc) : null);
        boolean sourceValid = (sourceLoc != null && 
          sourceLoc.isValid() && sourceLoc.isContained (sourceDims));

        // Copy data value
        // ---------------
        if (sourceValid) {
 
          // Get nearest neighbour source coordinate
          // ---------------------------------------
          int sourceRow = (int) Math.round (sourceLoc.get(Grid.ROWS));
          int sourceCol = (int) Math.round (sourceLoc.get(Grid.COLS));
                 
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
