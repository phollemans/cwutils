////////////////////////////////////////////////////////////////////////
/*
     FILE: Filter.java
  PURPOSE: To provide data value filtering.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/13
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

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
