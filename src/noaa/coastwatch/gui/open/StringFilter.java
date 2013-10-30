////////////////////////////////////////////////////////////////////////
/*
     FILE: StringFilter.java
  PURPOSE: String filter interface.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/30
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

/**
 * The <code>StringFilter</code> interface specifies methods for
 * filtering an input string to an output string.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public interface StringFilter {

  /** Filters the input string. */
  public String filter (String input);

} // StringFilter interface

////////////////////////////////////////////////////////////////////////
