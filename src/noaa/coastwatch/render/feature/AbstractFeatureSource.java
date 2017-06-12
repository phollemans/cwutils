////////////////////////////////////////////////////////////////////////
/*

     File: AbstractFeatureSource.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import noaa.coastwatch.render.feature.FeatureSource;
import noaa.coastwatch.render.feature.SelectionRuleFilter;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.util.EarthArea;

/**
 * The <code>AbstractFeatureSource</code> class supplies default
 * implementations for some <code>FeatureSource</code> methods.  The class
 * also adds the ability for any feature source to be filterable, by adding
 * a {@link SelectionRuleFilter}.  When a filter is set, the iterator
 * only returns features that match the filter.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public abstract class AbstractFeatureSource
  implements FeatureSource {

  // Variables
  // ---------

  /** The currently selected list of features. */
  protected List<Feature> featureList;

  /** The currently selected earth area. */
  protected EarthArea area;

  /** The list of attributes. */
  private List<Attribute> attributeList;

  /** The filter for the features, or null for no filtering. */
  private SelectionRuleFilter filter;

  /** The map of attribute name to index. */
  private Map<String, Integer> attNameMap;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the mapping from attribute name to index.  The index can be used to
   * retrieve the attribute value in each attribute of the features provided by
   * this source.
   *
   * @return the attribute name to index map.
   */
  public Map<String, Integer> getAttributeNameMap () {
  
    if (attNameMap == null) {
      attNameMap = new HashMap<String, Integer>();
      attributeList.forEach (att -> attNameMap.put (att.getName(), attributeList.indexOf (att)));
    } // if
    
    return (attNameMap);
    
  } // getAttributeNameMap

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new feature source with an empty list of features and a zero
   * length list of attributes.
   */
  protected AbstractFeatureSource () { 

    featureList = new ArrayList<Feature>();
    area = new EarthArea(); 
    attributeList = new ArrayList<Attribute>();

  } // AbstractFeatureSource constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the feature filter being used in this source.
   *
   * @return the feature filter or null for no filtering.
   */
  public SelectionRuleFilter getFilter () { return (filter); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the feature filter to use in this source.
   *
   * @param filter the feature filter or null for no filtering.
   */
  public void setFilter (SelectionRuleFilter filter) { this.filter = filter; }

  ////////////////////////////////////////////////////////////

  @Override
  public void select (
    EarthArea area
  ) throws IOException {

    this.area = (EarthArea) area.clone();
    select();

  } // select

  ////////////////////////////////////////////////////////////

  /**
   * Selects a set of features from the data source based on the
   * current area.
   *
   * @throws IOException if an error occurred accessing the data source.
   */
  protected abstract void select () throws IOException;

  ////////////////////////////////////////////////////////////

  @Override
  public EarthArea getArea () { return ((EarthArea) area.clone()); }

  ////////////////////////////////////////////////////////////

  @Override
  public Iterator<Feature> iterator () {
  
    Iterator<Feature> iterator;
    if (filter == null) {
      iterator = featureList.iterator();
    } // if
    else {
      List<Feature> filteredList = filter.filter (featureList);
      iterator = filteredList.iterator();
    } // else

    return (iterator);
    
  } // iterator

  ////////////////////////////////////////////////////////////

  @Override
  public List<Attribute> getAttributes() { return (new ArrayList<Attribute> (attributeList)); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getAttributeCount() { return (attributeList.size()); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the list of attributes in this source.
   *
   * @param attributeList the list of attributes.
   */
  protected void setAttributes (
    List<Attribute> attributeList
  ) {

    this.attributeList = new ArrayList<Attribute> (attributeList);
    this.attNameMap = null;

  } // setAttributes

  ////////////////////////////////////////////////////////////

} // AbstractFeatureSource class

////////////////////////////////////////////////////////////////////////
