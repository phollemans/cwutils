////////////////////////////////////////////////////////////////////////
/*
     FILE: LongitudeFilter.java
  PURPOSE: To provide longitude value filtering.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/13
  CHANGES: 2003/04/10, PFH, corrected documentation

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import noaa.coastwatch.util.Filter;

/**
 * The longitude filter is a filter with special considerations for
 * longitude values.  In some cases, longitude values may cross the
 * 180 to -180 boundary in a computation that requires data values to
 * be continuous.  The longitude filter detects a boundary crossing
 * and adjusts the longitude values.  Values which are less than 0 are
 * shifted positive 360 degrees.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class LongitudeFilter
  implements Filter {

  ////////////////////////////////////////////////////////////

  /**
   * Detects a longitude boundary crossing and adjusts the values if
   * necessary.  The assumption is that if the minimum and maximum
   * longitudes differ by more than 180 degrees, then a boundary
   * crossing has occurred.
   *
   * @param values the longitude values for adjustment.
   */
  public void filter (
    double[] values
  ) {

    // Find min and max
    // ----------------
    double min = values[0];
    double max = values[0];
    for (int i = 1; i < values.length; i++) {
      if (values[i] > max) max = values[i];
      if (values[i] < min) min = values[i];
    } // for

    // Adjust longitudes
    // -----------------
    if ((max - min) > 180) {
      for (int i = 0; i < values.length; i++)
        if (values[i] < 0) values[i] += 360;
    } // if

  } // filter

  ////////////////////////////////////////////////////////////

} // LongitudeFilter class

////////////////////////////////////////////////////////////////////////
