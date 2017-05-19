////////////////////////////////////////////////////////////////////////
/*

     File: ResettableIterator.java
   Author: Peter Hollemans
     Date: 2003/09/08

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
 * The <code>ResettableIterator</code> class has an interface to make iterators
 * go back to the first element again.
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public interface ResettableIterator {

  ////////////////////////////////////////////////////////////

  /**
   * Resets the iterator to the first element.
   */
  public void reset();

  ////////////////////////////////////////////////////////////

} // ResettableIterator class

////////////////////////////////////////////////////////////////////////
