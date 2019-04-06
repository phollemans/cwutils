////////////////////////////////////////////////////////////////////////
/*

     File: ResamplingMapFactory.java
   Author: Peter Hollemans
     Date: 2019/02/04

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.ResamplingMap;

/**
 * A <code>ResamplingMapFactory</code> is an interfaace for objects that
 * create {@link ResamplingMap} objects on-demand, based on the coordinates
 * that need to be remapped.  A factory instance should be thread-safe so
 * that map objects can be created simultaneously from multiple threads
 * if needed.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public interface ResamplingMapFactory {

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new resampling map that covers a specified set of destination
   * coordinates.
   *
   * @param start the starting rectangle coordinates in the destination
   * space as [row, col].
   * @param length the size of the rectangle in destination space as [rows, cols].
   *
   * @return a resampling object that covers the specified destination rectangle
   * or null if the destination rectangle has no corresponding coordinates
   * in the source space.
   */
  public ResamplingMap create (
    int[] start,
    int[] length
  );

  ////////////////////////////////////////////////////////////

} // ResamplingMap class

////////////////////////////////////////////////////////////////////////
