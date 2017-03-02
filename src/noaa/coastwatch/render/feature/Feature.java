////////////////////////////////////////////////////////////////////////
/*

     File: Feature.java
   Author: Peter Hollemans
     Date: 2005/04/29

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.util.Iterator;
import noaa.coastwatch.util.EarthLocation;

/*
 * A <code>Feature</code> represents any geographic object with a list
 * of points and a set of attributes.  The names and types of the
 * attributes are accessed through the feature source.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public interface Feature
  extends Iterable<EarthLocation> {

  /** Gets the value of the indexed attribute. */
  public Object getAttribute (int index);

  /** Gets an iterator over the points associated with this feature. */
  public Iterator<EarthLocation> iterator();

} // Feature interface

////////////////////////////////////////////////////////////////////////
