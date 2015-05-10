////////////////////////////////////////////////////////////////////////
/*
     FILE: GridContainerOverlay.java
  PURPOSE: An abstract overlay that uses grid data.
   AUTHOR: Peter Hollemans
     DATE: 2004/10/17
  CHANGES: 2006/07/10, PFH, added getGridList() and converted to interface
           2006/11/17, PFH, added setDataSource()
           2007/11/05, PFH, removed getGrid()

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.util.List;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.util.Grid;

/**
 * The <code>GridContainerOverlay</code> interface marks overlays
 * that use one or more {@link Grid} objects to display their
 * graphics.
 *
 * @author Peter Hollemans
 * @since 3.1.8
 */
public interface GridContainerOverlay {

  ////////////////////////////////////////////////////////////

  /** Gets the active list of grid variables. */
  public List<Grid> getGridList ();

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the data source for grid data.
   *
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   */
  public void setDataSource (
    EarthDataReader reader,
    List variableList
  );

  ////////////////////////////////////////////////////////////

} // GridContainerOverlay interface

////////////////////////////////////////////////////////////////////////
