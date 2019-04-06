////////////////////////////////////////////////////////////////////////
/*

     File: DirectGridResampler.java
   Author: Peter Hollemans
     Date: 2017/01/19

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.trans.EarthTransform;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The <code>DirectGridResampler</code> class performs generic data
 * resampling between 2D Earth transforms using a direct location
 * lookup method.  The steps are as follows:</p>
 * <ol>
 *
 *   <li>For each pixel in the destination grid, compute the lat/lon using
 *   the destination Earth transform, then the source
 *   grid coordinates using the source Earth transform.</li>
 * 
 *   <li>Transfer the data from the source to the destination at the 
 *   specified location.</li>
 *
 * </ol>
 * <p>This class is best suited for working with source and destination 
 * {@link EarthTransform} objects that compute forward and inverse transforms
 * relatively quickly.</p>
 *
 * <p><b>WARNING: This class is not thread-safe.</b></p>
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class DirectGridResampler 
  extends GridResampler {

  private static final Logger LOGGER = Logger.getLogger (DirectGridResampler.class.getName());
  private static final Logger VERBOSE = Logger.getLogger (DirectGridResampler.class.getName() + ".verbose");

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new grid resampler from the specified source and
   * destination transforms.
   *
   * @param sourceTrans the source Earth transform.
   * @param destTrans the destination Earth transform.
   *
   * @see LocationEstimator
   */
  public DirectGridResampler (
    EarthTransform sourceTrans,
    EarthTransform destTrans
  ) {
  
    super (sourceTrans, destTrans);

  } // DirectGridResampler constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void perform (
    boolean verbose
  ) {

    if (verbose) VERBOSE.setLevel (Level.INFO);

    // Check grid count
    // ----------------
    int grids = sourceGrids.size();
    VERBOSE.info ("Found " + grids + " grid(s) for resampling");
    if (grids == 0) return;

    // Get grid arrays
    // ---------------
    Grid[] sourceArray = sourceGrids.toArray (new Grid[]{});
    Grid[] destArray = destGrids.toArray (new Grid[]{});
    int[] sourceDims = sourceArray[0].getDimensions();
    int[] destDims = destArray[0].getDimensions();

    // Loop over each destination location
    // -----------------------------------
    VERBOSE.info ("Resampling to " +
      destDims[Grid.ROWS] + "x" + destDims[Grid.COLS] + " from " +
      sourceDims[Grid.ROWS] + "x" + sourceDims[Grid.COLS]);
    DataLocation destLoc = new DataLocation (2);
    DataLocation sourceLoc = new DataLocation (2);
    EarthLocation earthLoc = new EarthLocation();
    for (int i = 0; i < destDims[Grid.ROWS]; i++) {

      // Print progress
      // --------------
      if ((i+1)%(destDims[Grid.ROWS]/10) == 0) {
        int percentComplete = (int) Math.round (((i+1)*100.0/destDims[Grid.ROWS]));
        VERBOSE.info (percentComplete + "% complete");
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
          sourceTrans.transform (earthLoc, sourceLoc);
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

} // DirectGridResampler class

////////////////////////////////////////////////////////////////////////
