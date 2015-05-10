////////////////////////////////////////////////////////////////////////
/*
     FILE: Feature.java
  PURPOSE: Interface for all geographic features.
   AUTHOR: Peter Hollemans
     DATE: 2005/04/29
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.util.Iterator;

/*
 * A <code>Feature</code> represents any geographic object with a list
 * of points and a set of attributes.  The names and types of the
 * attributes are accessed through the feature source.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public interface Feature {

  /** Gets the value of the indexed attribute. */
  public Object getAttribute (int index);

  /** Gets an iterator over the points associated with this feature. */
  public Iterator iterator();

} // Feature interface

////////////////////////////////////////////////////////////////////////
