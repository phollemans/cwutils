////////////////////////////////////////////////////////////////////////
/*
     FILE: ValueSource.java
  PURPOSE: Interface for source of data values.
   AUTHOR: Peter Hollemans
     DATE: 2006/09/27
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

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
