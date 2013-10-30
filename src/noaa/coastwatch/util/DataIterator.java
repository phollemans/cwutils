////////////////////////////////////////////////////////////////////////
/*
     FILE: DataIterator.java
  PURPOSE: Combines double and resettable interfaces.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/27
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

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
