////////////////////////////////////////////////////////////////////////
/*

     File: DoubleIterator.java
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

// Imports
// -------
import java.util.Iterator;

/**
 * The <code>DoubleIterator</code> class adds an extra interface
 * method to return the next double value.  This helps to save
 * allocating space for a new <code>Double</code> object every time
 * the next double value is needed.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public interface DoubleIterator
  extends Iterator<Double> {

  ////////////////////////////////////////////////////////////

  /*
   * Gets the next value.
   *
   * @return the next double value.
   */
  public double nextDouble();

  ////////////////////////////////////////////////////////////

} // DoubleIterator class

////////////////////////////////////////////////////////////////////////
