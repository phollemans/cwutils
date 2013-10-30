////////////////////////////////////////////////////////////////////////
/*
     FILE: PointFeatureSymbol.java
  PURPOSE: Supplies methods for all point feature symbols.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/22
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
import jahuwaldt.plot.PlotSymbol;

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
