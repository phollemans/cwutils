////////////////////////////////////////////////////////////////////////
/*
     FILE: ResettableIterator.java
  PURPOSE: Extends an iterator to be resettable.
   AUTHOR: Peter Hollemans
     DATE: 2003/09/08
  CHANGES: 2004/03/27, PFH, updated Javadocs

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;

/**
 * The <code>ResettableIterator</code> class adds an extra interface
 * method to <code>Iterator</code> to make it go back to the first
 * element again.
 */
public interface ResettableIterator
  extends Iterator {

  ////////////////////////////////////////////////////////////

  /**
   * Resets the iterator to the first element.
   */
  public void reset();

  ////////////////////////////////////////////////////////////

} // ResettableIterator class

////////////////////////////////////////////////////////////////////////
