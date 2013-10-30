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
package noaa.coastwatch.render;

// Imports
// -------
import java.io.*;
import java.util.*;
import noaa.coastwatch.util.*;

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
  protected List featureList;

  /** The currently selected Earth area. */
  protected EarthArea area;

  /** The array of attribute names. */
  private String[] attNames;

  ////////////////////////////////////////////////////////////

  /** Creates a new feature source with an empty list of features. */
  protected AbstractFeatureSource () { 

    featureList = new ArrayList();
    area = new EarthArea(); 
    attNames = new String[0];

  } // AbstractFeatureSource constructor

  ////////////////////////////////////////////////////////////

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

  public EarthArea getArea () { return ((EarthArea) area.clone()); }

  ////////////////////////////////////////////////////////////

  public Iterator iterator () { return (featureList.iterator()); }

  ////////////////////////////////////////////////////////////

  public String getAttributeName (int index) { return (attNames[index]); }

  ////////////////////////////////////////////////////////////

  public int getAttributeCount() { return (attNames.length); }

  ////////////////////////////////////////////////////////////

  protected void setAttributeNames (
    String[] attNames
  ) {

    this.attNames = attNames;

  } // setAttributeNames

  ////////////////////////////////////////////////////////////

} // AbstractFeatureSource class

////////////////////////////////////////////////////////////////////////
