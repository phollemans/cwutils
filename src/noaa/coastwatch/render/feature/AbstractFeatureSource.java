////////////////////////////////////////////////////////////////////////
/*
     FILE: AbstractFeatureSource.java
  PURPOSE: Default implementation of some feature source methods.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/03
  CHANGES: 2005/05/26, PFH, changed vectorList to featureList

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

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
import noaa.coastwatch.render.feature.FeatureSource;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.util.EarthArea;

/**
 * The <code>AbstractFeatureSource</code> class supplies default
 * implementations for some <code>FeatureSource</code> methods.
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

  /** The currently selected Earth area. */
  protected EarthArea area;

  /** The list of attributes. */
  private List<Attribute> attributeList;

  ////////////////////////////////////////////////////////////

  /** Creates a new feature source with an empty list of features. */
  protected AbstractFeatureSource () { 

    featureList = new ArrayList<Feature>();
    area = new EarthArea(); 
    attributeList = new ArrayList<Attribute>();

  } // AbstractFeatureSource constructor

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
  public Iterator<Feature> iterator () { return (featureList.iterator()); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the list of attributes in this source.
   *
   * @param attributeList the list of attributes.
   */
  protected void setAttributes (
    List<Attribute> attributes
  ) {

    this.attributeList = new ArrayList<Attribute> (attributeList);

  } // setAttributes

  ////////////////////////////////////////////////////////////

} // AbstractFeatureSource class

////////////////////////////////////////////////////////////////////////
