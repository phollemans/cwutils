////////////////////////////////////////////////////////////////////////
/*

     File: SelectionRule.java
   Author: Peter Hollemans
     Date: 2016/07/04

  CoastWatch Software Library and Utilities
  Copyright (c) 2016 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.render.feature.Feature;

/*
 * A <code>SelectionRule</code> provides a selection mechanism for 
 * features.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public interface SelectionRule {

  /** 
   * Determines if a feature matches the rule.
   *
   * @param feature the feature to check.
   *
   * @return true if the feature matches this rule or false if not.
   */
  public boolean matches (Feature feature);

} // SelectionRule interface

////////////////////////////////////////////////////////////////////////
