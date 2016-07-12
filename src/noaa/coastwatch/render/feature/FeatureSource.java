////////////////////////////////////////////////////////////////////////
/*
     FILE: FeatureSource.java
  PURPOSE: Provides feature data source methods.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/03
  CHANGES: 2008/04/29, PFH, added attribute related methods

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
   * @param area the Earth area for feature selection.
   * 
   * @throws IOException if an error occurred accessing the data source.
   */
  public void select (
    EarthArea area
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Selects a set of features from the data source based on the
   * list of selection rules.
   *
   * @param selectionRuleList the selection rules that the features must
   * satisfy.
   *
   * @throws IOException if an error occurred accessing the data source.
   */
  public void select (
    List<SelectionRule> selectionRuleList
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

} // FeatureSource class

////////////////////////////////////////////////////////////////////////
