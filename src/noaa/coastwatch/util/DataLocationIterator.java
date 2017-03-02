////////////////////////////////////////////////////////////////////////
/*

     File: DataLocationIterator.java
   Author: Peter Hollemans
     Date: 2004/03/27

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
package noaa.coastwatch.util;

/**
 * The <code>DataLocationIterator</code> class adds an extra interface
 * method to return the next data location.  This helps to save
 * allocating space for a new <code>DataLocation</code> object every
 * time the next location is needed.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public interface DataLocationIterator
  extends ResettableIterator {

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the next data location.
   *
   * @param loc the location to fill in with coordinates, or null to
   * allocate a new location.
   *
   * @return the next location.  The location is allocated only if the
   * passed location is null, otherwise the same location is returned.
   */
  public DataLocation nextLocation (DataLocation loc);

  ////////////////////////////////////////////////////////////

} // DataLocationIterator class

////////////////////////////////////////////////////////////////////////
