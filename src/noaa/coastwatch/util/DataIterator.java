////////////////////////////////////////////////////////////////////////
/*

     File: DataIterator.java
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
 * The <code>DataIterator</code> class is intended to be used by
 * classes that need to iterate over a set of data values.  It
 * includes both the <code>ResettableIterator</code> interface and the
 * <code>DoubleIterator</code> interface.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public interface DataIterator
  extends ResettableIterator, DoubleIterator {

  // no extra methods are defined here

} // DataIterator class

////////////////////////////////////////////////////////////////////////
