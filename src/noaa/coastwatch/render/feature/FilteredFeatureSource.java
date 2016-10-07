////////////////////////////////////////////////////////////////////////
/*
     FILE: FilteredFeatureSource.java
  PURPOSE: Allows features in a source to filtered by rules.
   AUTHOR: Peter Hollemans
     DATE: 2016/07/21
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
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import noaa.coastwatch.render.feature.FeatureSource;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.render.feature.SelectionRule;

// Testing
// -------
import noaa.coastwatch.test.TestLogger;
import java.util.Map;
import java.util.HashMap;
import noaa.coastwatch.util.EarthLocation;

/**
 * A <code>FilteredFeatureSource</code> is a feature source whose features
 * conform to a set of rules based on the attribute values of the features.
 * The rule set can be matched one of two ways: either all the rules must be
 * matched for a certain feature to be included, or any of the rules must be 
 * matched.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class FilteredFeatureSource

/*
 * Originally this class was designed to filter any feature source, but to fit
 * it into its intended place as a filter for point feature data without 
 * redesigning the class hirarchy, we had to change it to extend a 
 * PointFeatureSource so it could easily be used in PointFeatureOverlay objects.
 */

//  implements FeatureSource {
  extends PointFeatureSource {


  // Enumerations
  // ------------
  
  /** The mode that the filtering is operating under. */
  public enum FilterMode {
    MATCHES_ALL,
    MATCHES_ANY
  } // FilterNMode

  // Variables
  // ---------

  /** The feature source that this source is wrapping. */
  private FeatureSource innerSource;
  
  /** The list of rules to use for filtering. */
  private List<SelectionRule> ruleList;
  
  /** The filtering mode: either match any rules or all rules. */
  private FilterMode mode;

  ////////////////////////////////////////////////////////////

  @Override
  public void select (
    EarthArea area
  ) throws IOException {
  
    innerSource.select (area);
  
  } // select

  ////////////////////////////////////////////////////////////

  /*
   * This is implemented as a side-effect of extending an AbstractFeatureSource.
   * We wouldn't need this if we were using the original design.  It does
   * nothing because we never call it, we call the inner source's
   * select (EarthArea) method.
   */
  @Override
  protected void select () throws IOException { }

  ////////////////////////////////////////////////////////////

  @Override
  public EarthArea getArea() { return (innerSource.getArea()); }

  ////////////////////////////////////////////////////////////

  @Override
  public Iterator<Feature> iterator() {
  
    List<Feature> filteredList = new ArrayList<Feature>();

    // Detect filter mode
    // ------------------
    switch (mode) {
      
      // Filter by matching any rule
      // ---------------------------
      case MATCHES_ANY:
        for (Feature feature : innerSource) {
          boolean isMatching = false;
          for (SelectionRule rule : ruleList) {
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
        for (Feature feature : innerSource) {
          boolean isMatching = true;
          for (SelectionRule rule : ruleList) {
            if (!rule.matches (feature)) {
              isMatching = false;
              break;
            } // if
          } // for
          if (isMatching) filteredList.add (feature);
        } // for
        break;

    } // switch

    return (filteredList.iterator());
  
  } // iterator

  ////////////////////////////////////////////////////////////

  @Override
  public List<Attribute> getAttributes() { return (innerSource.getAttributes()); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the list of selection rules.  Changes made to the returned list will
   * be reflected in the iterator results.
   *
   * @return the rule list.
   */
  public List<SelectionRule> getRules() { return (ruleList); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the filtering mode.
   *
   * @param mode the filtering mode, either <code>MATCHES_ANY</code>
   * or <code>MATCHES_ALL</code>.  When set to 'any', if any single rule
   * matches a feature, the feature is included in the results.  When set to
   * 'all', the entire list of rules must match the feature for it to be
   * included.
   */
  public void setMode (FilterMode mode) { this.mode = mode; }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current filtering mode.
   *
   * @return the filtering mode, either <code>MATCHES_ANY</code> or
   * <code>MATCHES_ALL</code>.
   */
  public FilterMode getMode () { return (mode); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new filtered source using an existing source.  The iterator 
   * will return features from this source that match the filtering rules.
   * The initial rule set is empty and the filtering mode set to match any rule.
   *
   * @param source the feature source to use for filtering.  The iterator will 
   * return features from this source that match the filtering rules.
   */
  public FilteredFeatureSource (
    FeatureSource source
  ) {
  
    this.innerSource = source;
    this.ruleList = new ArrayList<SelectionRule>();
    this.mode = FilterMode.MATCHES_ANY;

  } // FilteredFeatureSource

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (FilteredFeatureSource.class);

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
    
    // feature source for the 4 features
    FeatureSource source = new FeatureSource() {
      public void select (EarthArea area) throws IOException {}
      public EarthArea getArea() { return (null); }
      public Iterator<Feature> iterator() { return (featureList.iterator()); }
      public List<Attribute> getAttributes() { return (null); }
    }; // FeatureSource

    List<SelectionRule> ruleList = new ArrayList<SelectionRule>();

    NumberRule rule = new NumberRule ("attribute0", nameMap, 1);
    rule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    ruleList.add (rule);

    rule = new NumberRule ("attribute1", nameMap, 1);
    rule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    ruleList.add (rule);

    rule = new NumberRule ("attribute2", nameMap, 1);
    rule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    ruleList.add (rule);

    rule = new NumberRule ("attribute3", nameMap, 1);
    rule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    ruleList.add (rule);

    logger.passed();

    logger.test ("constructor");
    FilteredFeatureSource filteredSource = new FilteredFeatureSource (source);
    filteredSource.getRules().addAll (ruleList);
    assert (filteredSource.getRules().size() == 4);
    assert (filteredSource.getMode() == FilterMode.MATCHES_ANY);
    logger.passed();

    logger.test ("iterator");

    int count = 0;
    for (Feature feature : filteredSource) count++;
    assert (count == 3);

    filteredSource.setMode (FilterMode.MATCHES_ALL);
    count = 0;
    for (Feature feature : filteredSource) count++;
    assert (count == 0);

    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // FilteredFeatureSource class

////////////////////////////////////////////////////////////////////////
