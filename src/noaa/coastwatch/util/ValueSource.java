////////////////////////////////////////////////////////////////////////
/*

     File: ValueSource.java
   Author: Peter Hollemans
     Date: 2006/09/27

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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

/**
 * The <code>ValueSource</code> interface may be implemented by
 * any class that provides data values for a set of {@link
 * DataLocation} objects.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public interface ValueSource {

  /** Gets a data value for the specified location. */
  public double getValue (DataLocation loc);

} // ValueSource class

////////////////////////////////////////////////////////////////////////
