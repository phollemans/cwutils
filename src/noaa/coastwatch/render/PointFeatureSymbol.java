////////////////////////////////////////////////////////////////////////
/*

     File: PointFeatureSymbol.java
   Author: Peter Hollemans
     Date: 2005/05/22

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
package noaa.coastwatch.render;

// Imports
// -------
import jahuwaldt.plot.PlotSymbol;
import noaa.coastwatch.render.feature.PointFeature;

/**
 * A <code>PointFeatureSymbol</code> is a <code>PlotSymbol</code> that
 * may change its rendered appearance slightly depending on the
 * attributes of the feature that it currently holds.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public abstract class PointFeatureSymbol
  extends PlotSymbol {

  // Variables
  // ---------

  /** The current point feature to use for rendering. */
  protected PointFeature feature;

  ////////////////////////////////////////////////////////////
  
  /** Sets the point feature to use for attributes. */
  public void setFeature (
    PointFeature feature
  ) {

    this.feature = feature;

  } // setFeature

  ////////////////////////////////////////////////////////////

} // PointFeatureSymbol class

////////////////////////////////////////////////////////////////////////
