////////////////////////////////////////////////////////////////////////
/*

     File: FeatureSource.java
   Author: Peter Hollemans
     Date: 2005/05/03

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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.feature.Attribute;

/**
 * The <code>FeatureSource</code> interface provides methods
 * common the all feature sources; the methods allow for
 * selection of features based on a geographic area, iteration
 * over the matching features, and information on attributes.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public interface FeatureSource
  extends Iterable<Feature> {

  ////////////////////////////////////////////////////////////

  /**
   * Selects a set of features from the data source based on the
   * specified area.
   *
   * @param area the earth area for feature selection.
   * 
   * @throws IOException if an error occurred accessing the data source.
   */
  public void select (
    EarthArea area
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected Earth area. */
  public EarthArea getArea();

  ////////////////////////////////////////////////////////////

  /** Gets an iterator over all selected features. */
  public Iterator<Feature> iterator();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the list of attributes for each feature from this source.
   *
   * @return the list of attributes.
   */
  public List<Attribute> getAttributes();

  ////////////////////////////////////////////////////////////

  /**
   * Gets the number of attributes in the attribute list for each feature.
   *
   * @return the attribute count.
   */
  public int getAttributeCount();

  ////////////////////////////////////////////////////////////

} // FeatureSource class

////////////////////////////////////////////////////////////////////////
