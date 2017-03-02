////////////////////////////////////////////////////////////////////////
/*

     File: GridContainerOverlay.java
   Author: Peter Hollemans
     Date: 2004/10/17

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
