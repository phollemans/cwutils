////////////////////////////////////////////////////////////////////////
/*
     FILE: GridResampler.java
  PURPOSE: A class to perform resampling of data from one projection 
           to another.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/11
  CHANGES: 2002/12/19, PFH, modified verbose message
           2002/12/20, PFH, added check for valid destination 
             earth location
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2005/01/26, PFH, moved main funtionality to child class

  CoastWatch Software Library and Utilities
  Copyright 2004-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>GridResampler</code> class performs generic data
 * resampling between 2D earth transforms.  The method that actually
 * performs the resampling is left to the child class.  The user must
 * provide source and destination earth transforms, and pairs of
 * source/destination grids (possibly more than one pair).  The
 * resampling is performed on all source / destination grid pairs
 * simultaneously.  Note that all source grids must have the same
 * dimensions and navigation transform, and all destination grids
 * must have the same dimensions.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public abstract class GridResampler {

  // Variables
  // ---------

  /** The source earth transform. */
  protected EarthTransform sourceTrans;

  /** The destination earth transform. */
  protected EarthTransform destTrans;

  /** The list of source grids. */
  protected List<Grid> sourceGrids;

  /** The list of destination grids. */
  protected List<Grid> destGrids;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new grid resampler from the specified source and
   * destination transforms.
   *
   * @param sourceTrans the source earth transform.
   * @param destTrans the destination earth transform.
   */
  public GridResampler (
    EarthTransform sourceTrans,
    EarthTransform destTrans
  ) {
  
    // Initialize
    // ----------  
    this.sourceTrans = sourceTrans;
    this.destTrans = destTrans;
    sourceGrids = new ArrayList();
    destGrids = new ArrayList();

  } // GridResampler constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a source and destination grid pair to the list of grids for
   * resampling.  The source grid data will be resampled into the
   * destination grid.  
   *
   * @param sourceGrid the source grid for resampling.
   * @param destGrid the destination grid for resampling.
   */
  public void addGrid (
    Grid sourceGrid,
    Grid destGrid
  ) {

    sourceGrids.add (sourceGrid);
    destGrids.add (destGrid);

  } // addGrid

  ////////////////////////////////////////////////////////////

  /**
   * Performs the resampling operation on all source / destination
   * pairs.
   *
   * @param verbose true for verbose resampling.
   */
  public abstract void perform (
    boolean verbose
  );

  ////////////////////////////////////////////////////////////

} // GridResampler class

////////////////////////////////////////////////////////////////////////
