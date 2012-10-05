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
package noaa.coastwatch.render;

// Imports
// -------
import java.io.*;
import java.util.*;
import noaa.coastwatch.util.*;

/**
 * The <code>FeatureSource</code> interface provides methods
 * common the all feature sources; the methods allow for
 * selection of features based on a geographic area, iteration
 * over the matching features, and information on attributes.
 */
public interface FeatureSource {

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

  /** Gets the currently selected Earth area. */
  public EarthArea getArea ();

  ////////////////////////////////////////////////////////////

  /** Gets an iterator over all selected features. */
  public Iterator iterator ();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an attribute name for the attributes in each feature
   * from this source.
   * 
   * @param index the attribute index, starting at 0.
   *
   * @return the attribute name.
   * 
   * @throws IndexOutOfBoundsException if the index is out of range.
   */
  public String getAttributeName (
    int index
  );

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the number of attributes for each feature from this
   * source.
   */
  public int getAttributeCount();

  ////////////////////////////////////////////////////////////


} // FeatureSource class

////////////////////////////////////////////////////////////////////////
