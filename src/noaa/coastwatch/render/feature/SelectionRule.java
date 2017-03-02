////////////////////////////////////////////////////////////////////////
/*
     FILE: SelectionRule.java
  PURPOSE: Interface for selecting features.
   AUTHOR: Peter Hollemans
     DATE: 2016/07/04
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2016, USDOC/NOAA/NESDIS CoastWatch

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
