////////////////////////////////////////////////////////////////////////
/*
     FILE: GriddedPointGenerator.java
  PURPOSE: Generates point features from gridded data.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/26, PFH
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.util.*;
import java.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * A <code>GriddedPointGenerator</code> creates point features from a
 * set of co-located <code>Grid</code> objects.  The user selects an
 * area of interest, and the generator converts all data values in
 * that area into point features whose Earth locations are derived
 * from the grids' Earth transform and whose attributes are the data
 * values at the grid point.
 */
public class GriddedPointGenerator
  extends PointFeatureSource {

  // Variables
  // ---------
  
  /** The array of grids to use for feature attribute data. */
  private Grid[] gridArray;

  /** The Earth transform for the grid data. */
  private EarthTransform2D trans;
  
  /** The Earth area covered by the grids. */
  private EarthArea gridArea;

  /** The grid dimensions as [rows, cols]. */
  private int[] gridDims;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new generator.
   * 
   * @param gridArray the array of grids to use for data, each of the
   * same dimensions and Earth transform.
   * @param trans the Earth transform for the grids.
   */
  public GriddedPointGenerator (
    Grid[] gridArray,
    EarthTransform2D trans
  ) {

    // Set variables
    // -------------
    this.gridArray = gridArray;
    this.trans = trans;

    // Create grid area
    // ----------------
    gridDims = gridArray[0].getDimensions();
    gridArea = new EarthArea (trans, new DataLocation (0, 0),
      new DataLocation (gridDims[Grid.ROWS]-1, gridDims[Grid.COLS]-1));

  } // GriddedPointGenerator constructor

  ////////////////////////////////////////////////////////////

  protected void select () throws IOException {

    // Initialize feature list
    // -----------------------
    featureList.clear();

    // Create and check intersection area
    // ----------------------------------
    EarthArea intersection = area.intersection (gridArea);
    if (intersection.isEmpty()) return;

    // Setup arrays
    // ------------
    EarthLocation earthLoc = new EarthLocation (trans.getDatum());
    DataLocation[] corners = new DataLocation[4];
    for (int i = 0; i < 4; i++) corners[i] = new DataLocation (2);
    double[] min = new double[2];
    double[] max = new double[2];
    DataLocation dataLoc = new DataLocation (2);
    int[] start = new int[2];
    int[] end = new int[2];

    // Create visited set
    // ------------------
    Set visitedSet = new HashSet();

    // Iterate over each square
    // ------------------------
    for (Iterator iter = intersection.getIterator(); iter.hasNext(); ) {

      // Get square corners in data coordinates
      // --------------------------------------
      int[] ll = (int[]) iter.next();
      earthLoc.setCoords (ll[0], ll[1]); 
      trans.transform (earthLoc, corners[0]);
      earthLoc.setCoords (ll[0]+1, ll[1]); 
      trans.transform (earthLoc, corners[1]);
      earthLoc.setCoords (ll[0], ll[1]+1); 
      trans.transform (earthLoc, corners[2]);
      earthLoc.setCoords (ll[0]+1, ll[1]+1); 
      trans.transform (earthLoc, corners[3]);

      // Check for invalid corner data locations
      // ---------------------------------------
      boolean skipSquare = false;
      for (int i = 1; i < 4; i++) {
        if (corners[i].isInvalid() || !corners[i].isContained (gridDims)) { 
          skipSquare = true; 
          break; 
        } // if
      } // for        
      if (skipSquare) continue;
      
      // Get data coordinate extents
      // ---------------------------
      for (int j = 0; j < 2; j++) {
        min[j] = corners[0].get (j);
        max[j] = corners[0].get (j);
      } // for
      for (int i = 1; i < 4; i++) {
        for (int j = 0; j < 2; j++) {
          min[j] = Math.min (min[j], corners[i].get (j));
          max[j] = Math.max (max[j], corners[i].get (j));
        } // for
      } // for
      for (int i = 0; i < 2; i++)
        start[i] = (int) Math.floor (min[i]);
      for (int i = 0; i < 2; i++)
        end[i] = (int) Math.ceil (max[i]);

      // Loop over each data location
      // ----------------------------
      for (int i = start[Grid.ROWS]; i <= end[Grid.ROWS]; i++) {
        for (int j = start[Grid.COLS]; j <= end[Grid.COLS]; j++) {

          // Check if visited
          // ----------------
          dataLoc.set (Grid.ROWS, i);
          dataLoc.set (Grid.COLS, j);
          if (visitedSet.contains (dataLoc)) continue;
          else visitedSet.add (dataLoc.clone());
          
          // Check for containment
          // ---------------------
          dataLoc.set (Grid.ROWS, i);
          dataLoc.set (Grid.COLS, j);
          if (!dataLoc.isContained (gridDims)) continue;

          // Check Earth location
          // --------------------
          EarthLocation pointLoc = trans.transformToPoint (dataLoc, null);
          if (!pointLoc.isValid()) continue;

          // Create feature
          // --------------
          Object[] attributeArray = new Object[gridArray.length];
          for (int k = 0; k < attributeArray.length; k++)
            attributeArray[k] = new Double (gridArray[k].getValue (dataLoc));
          PointFeature feature = new PointFeature (pointLoc, attributeArray);
          featureList.add (feature);

        } // for
      } // for

    } // for

  } // select

  ////////////////////////////////////////////////////////////

} // GriddedPointGenerator class

////////////////////////////////////////////////////////////////////////
