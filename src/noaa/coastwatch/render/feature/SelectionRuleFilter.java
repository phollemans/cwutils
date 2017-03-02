////////////////////////////////////////////////////////////////////////
/*

     File: SelectionRuleFilter.java
   Author: Peter Hollemans
     Date: 2017/01/26

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import noaa.coastwatch.render.feature.SelectionRule;

// Testing
// -------
import java.util.Map;
import java.util.HashMap;
import noaa.coastwatch.test.TestLogger;
import noaa.coastwatch.util.EarthLocation;

/**
 * A <code>SelectionRuleFilter</code> is a list of {@link SelectionRule} objects
 * together with an overall rule that determines how to filter a set of
 * {@link Feature} objects.  The filter list can be used for feature matching
 * in one of two ways: either all the rules must be matched for a certain 
 * feature to be included, or any of the rules can be matched (at least one).
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class SelectionRuleFilter
  extends ArrayList<SelectionRule> {

  // Enumerations
  // ------------
  
  /** The mode that the filtering is operating under. */
  public enum FilterMode {
    MATCHES_ALL,
    MATCHES_ANY;
    @Override
    public String toString() {
      String value = super.toString();
      value = value.toLowerCase().replaceAll ("_", " ");
      return (value);
    } // toString    
  } // FilterNMode

  // Variables
  // ---------

  /** The filtering mode: either match any rules or all rules. */
  private FilterMode mode;

  ////////////////////////////////////////////////////////////

  @Override
  public Object clone () {

    SelectionRuleFilter copy = new SelectionRuleFilter();
    copy.mode = mode;
    forEach (rule -> {
      Object ruleObj = (Object) rule;
      Object ruleCopy;
      try {
        Method method = ruleObj.getClass().getMethod ("clone");
        ruleCopy = method.invoke (ruleObj);
      } // try
      catch (Exception e) {
        throw new RuntimeException ("Error cloning object of type " + ruleObj.getClass());
      } // catch
      copy.add ((SelectionRule) ruleCopy);
    });
    return (copy);

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Filters a collection of features using the rules in this filter for
   * matching.
   * 
   * @param features the features to filter.
   *
   * @return the list of the matching features, possibly empty.
   */
  public List<Feature> filter (
    List<Feature> features
  ) {
  
    List<Feature> filteredList = new ArrayList<Feature>();

    // Detect filter mode
    // ------------------
    switch (mode) {
      
      // Filter by matching any rule
      // ---------------------------
      case MATCHES_ANY:
        for (Feature feature : features) {
          boolean isMatching = false;
          for (SelectionRule rule : this) {
            if (rule.matches (feature)) {
              isMatching = true;
              break;
            } // if
          } // for
          if (isMatching) filteredList.add (feature);
        } // for
        break;

      // Filter by matching all rules
      // ----------------------------
      case MATCHES_ALL:
        for (Feature feature : features) {
          boolean isMatching = true;
          for (SelectionRule rule : this) {
            if (!rule.matches (feature)) {
              isMatching = false;
              break;
            } // if
          } // for
          if (isMatching) filteredList.add (feature);
        } // for
        break;

    } // switch

    return (filteredList);
  
  } // filter

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the filtering mode.
   *
   * @param mode the filtering mode, either <code>MATCHES_ANY</code>
   * or <code>MATCHES_ALL</code>.  When set to 'any', if any single rule
   * matches a feature, the feature is included in the filter results.  When 
   * set to 'all', the entire list of rules must match the feature for it to 
   * be included.
   */
  public void setMode (FilterMode mode) { this.mode = mode; }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current filtering mode.
   *
   * @return the filtering mode, either <code>MATCHES_ANY</code> or
   * <code>MATCHES_ALL</code>.
   *
   * @see #setMode
   */
  public FilterMode getMode () { return (mode); }

  ////////////////////////////////////////////////////////////

  /** Creates a new empty filter with mode set to <code>MATCHES_ANY</code>. */
  public SelectionRuleFilter () {
  
    super();
    this.mode = FilterMode.MATCHES_ANY;

  } // SelectionRuleFilter

  ////////////////////////////////////////////////////////////

  @Override
  public String toString () {

    StringBuffer buffer = new StringBuffer();
    buffer.append ("SelectionRuleFilter[mode=" + mode + ", \n");
    this.forEach (rule -> buffer.append ("  " + rule.toString() + ",\n"));
    buffer.deleteCharAt (buffer.lastIndexOf (","));
    buffer.append ("]");

    return (buffer.toString());

  } // toString

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (SelectionRuleFilter.class);

    logger.test ("Framework");

    // 4 attributes: 0, 1, 2, 3
    Map<String, Integer> nameMap = new HashMap<String, Integer>();
    nameMap.put ("attribute0", 0);
    nameMap.put ("attribute1", 1);
    nameMap.put ("attribute2", 2);
    nameMap.put ("attribute3", 3);
    assert (nameMap.containsKey ("attribute0"));
    assert (nameMap.containsKey ("attribute1"));
    assert (nameMap.containsKey ("attribute2"));
    assert (nameMap.containsKey ("attribute3"));

    final List<Feature> featureList = new ArrayList<Feature>();
    
    // 4 features, each with 4 attributes
    featureList.add (new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {1, 2, 3, 4}
    ));
    featureList.add (new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {1, 1, 3, 4}
    ));
    featureList.add (new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {1, 1, 1, 4}
    ));
    featureList.add (new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {1, 1, 1, 1}
    ));

    logger.passed();
    
    logger.test ("constructor");
    SelectionRuleFilter filter = new SelectionRuleFilter();

    NumberRule rule = new NumberRule ("attribute0", nameMap, 1);
    rule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    filter.add (rule);

    rule = new NumberRule ("attribute1", nameMap, 1);
    rule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    filter.add (rule);

    rule = new NumberRule ("attribute2", nameMap, 1);
    rule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    filter.add (rule);

    rule = new NumberRule ("attribute3", nameMap, 1);
    rule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    filter.add (rule);

    assert (filter.size() == 4);
    assert (filter.getMode() == FilterMode.MATCHES_ANY);
    logger.passed();

    logger.test ("filter");

    int count = 0;
    for (Feature feature : filter.filter (featureList)) count++;
    assert (count == 3);

    filter.setMode (FilterMode.MATCHES_ALL);
    count = 0;
    for (Feature feature : filter.filter (featureList)) count++;
    assert (count == 0);

    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // SelectionRuleFilter class

////////////////////////////////////////////////////////////////////////
