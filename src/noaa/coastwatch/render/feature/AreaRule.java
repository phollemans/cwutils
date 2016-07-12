////////////////////////////////////////////////////////////////////////
/*
     FILE: AreaRule.java
  PURPOSE: Selects features based on an earth area.
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
import noaa.coastwatch.render.feature.SelectionRule;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;

/*
 * An <code>AreaRule</code> provides a selection mechanism for
 * features based on an earth area.  If a feature has at least one
 * location contained within the earth area, it is considered a matching
 * feature.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class AreaRule
  implements SelectionRule {

  // Variables
  // ---------
  
  /** The earth area to use for matching. */
  private EarthArea matchingArea;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new area rule.
   *
   * @param area the area to use for matching.
   */
  public AreaRule (
    EarthArea area
  ) {

    matchingArea = area;

  } // AreaRule

  ////////////////////////////////////////////////////////////

  @Override
  public boolean matches (Feature feature) {
  
    boolean isMatching = false;
    for (EarthLocation location : feature) {
      isMatching = matchingArea.contains (location);
      if (isMatching) break;
    } // for
    
    return (isMatching);
  
  } // matches

  ////////////////////////////////////////////////////////////

} // AreaRule class

////////////////////////////////////////////////////////////////////////
