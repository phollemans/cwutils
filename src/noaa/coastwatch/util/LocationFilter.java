////////////////////////////////////////////////////////////////////////
/*

     File: LocationFilter.java
   Author: Peter Hollemans
     Date: 2015/10/30

  CoastWatch Software Library and Utilities
  Copyright (c) 2015 National Oceanic and Atmospheric Administration
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
 * The <code>LocationFilter</code> class allows for computations and other
 * classes to check if a specific location's data should be used in a 
 * computation.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public interface LocationFilter {

  ////////////////////////////////////////////////////////////

  /**
   * Determines if a data location should be used in the context of
   * some computation.
   *
   * @param loc the location to determine for use.
   *
   * @return true if the location should be used, or false if not.
   */
  public boolean useLocation (DataLocation loc);

  ////////////////////////////////////////////////////////////

} // LocationFilter class

////////////////////////////////////////////////////////////////////////
