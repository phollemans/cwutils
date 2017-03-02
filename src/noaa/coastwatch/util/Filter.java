////////////////////////////////////////////////////////////////////////
/*

     File: Filter.java
   Author: Peter Hollemans
     Date: 2002/09/13

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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

/**
 * The filter interface provides a method for performing processing on
 * an array of data values.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public interface Filter {

  ////////////////////////////////////////////////////////////

  /**
   * Filters an array of data values.
   *
   * @param values the array of values for filtering.  The values are
   * modified in-situ.
   */
  public void filter (
    double[] values
  );

  ////////////////////////////////////////////////////////////

} // Filter class

////////////////////////////////////////////////////////////////////////
