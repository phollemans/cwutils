////////////////////////////////////////////////////////////////////////
/*
     FILE: LocationFilter.java
  PURPOSE: Interface for filtering the use of locations in computations.
   AUTHOR: Peter Hollemans
     DATE: 2015/10/30
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2015, USDOC/NOAA/NESDIS CoastWatch

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
