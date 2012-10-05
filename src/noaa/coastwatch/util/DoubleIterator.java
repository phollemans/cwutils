////////////////////////////////////////////////////////////////////////
/*
     FILE: DoubleIterator.java
  PURPOSE: Extends an iterator to return a double value.
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

// Imports
// -------
import java.util.*;

/**
 * The <code>DoubleIterator</code> class adds an extra interface
 * method to return the next double value.  This helps to save
 * allocating space for a new <code>Double</code> object every time
 * the next double value is needed.
 */
public interface DoubleIterator
  extends Iterator {

  ////////////////////////////////////////////////////////////

  /** Returns the next double value. */
  public double nextDouble();

  ////////////////////////////////////////////////////////////

} // DoubleIterator class

////////////////////////////////////////////////////////////////////////
