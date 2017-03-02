////////////////////////////////////////////////////////////////////////
/*

     File: FeatureGroupFilter.java
   Author: Peter Hollemans
     Date: 2017/02/25

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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Collectors;
import java.util.function.BiFunction;
import java.util.Date;

// Testing
// -------
import java.util.HashMap;
import noaa.coastwatch.test.TestLogger;
import noaa.coastwatch.util.EarthLocation;

/**
 * A <code>FeatureGroupFilter</code> filters a set of {@link Feature} objects
 * using a grouping concept.  Features are grouped by an attribute value they
 * have in common -- in the case of point features that represent measurements
 * made over time, this could by the ID of the platform making the measurement.
 * The group is then filtered by only selecting the single feature in the
 * group whose attribute value is closest to a specified value.  For example,
 * suppose that we have features with "id" and "time" attributes as follows:
 * <ul>
 *   <li>id=BUOY1, time=1985-10-26T01:20:00</li>
 *   <li>id=BUOY1, time=1985-10-26T01:21:00</li>
 *   <li>id=BUOY1, time=1985-10-26T01:22:00</li>
 *   <li>id=BUOY2, time=1985-10-26T01:20:00</li>
 *   <li>id=BUOY2, time=1985-10-26T01:21:00</li>
 *   <li>id=BUOY2, time=1985-10-26T01:22:00</li>
 * </ul>
 * If the grouping attribute is "id", and the filtering attribute is "time"
 * with a value of 1985-10-26T01:21:10, then the results of the filter would
 * contain only two features:
 * <ul>
 *   <li>id=BUOY1, time=1985-10-26T01:21:00</li>
 *   <li>id=BUOY2, time=1985-10-26T01:21:00</li>
 * </ul>
 * The group filter would detect two groups: all features with "id" values
 * of "BUOY1" and all features with "BUOY2".  Then it would select the 
 * feature from each group with "time" attribute closest to
 * 1985-10-26T01:21:10.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class FeatureGroupFilter
  implements Cloneable {

  // Variables
  // ---------

  /** The name of the attribute to use for grouping common features. */
  private String groupAttName;
  
  /** The attribute to filter on. */
  private String filterAttName;
  
  /** The target attribute value to search for closest value. */
  private Object targetAttValue;
  
  /** The map to use to translate name to index for attributes. */
  protected Map<String, Integer> nameMap;
  
  ////////////////////////////////////////////////////////////

  @Override
  public Object clone () {

    FeatureGroupFilter copy;
    try { copy = (FeatureGroupFilter) super.clone(); }
    catch (CloneNotSupportedException e) { copy = null; }
    
    return (copy);

  } // clone

  ////////////////////////////////////////////////////////////

  /** Measures the distance between two dates. */
  private static class DateDistanceMeasure implements BiFunction<Object, Object, Comparable> {
    public Comparable apply (Object a, Object b) {
      return (Math.abs (((Date)a).getTime() - ((Date)b).getTime()));
    } // apply
  } // DateDistanceMeasure

  ////////////////////////////////////////////////////////////

  /** Measures the distance between two floating point numbers (float, double). */
  private static class FloatDistanceMeasure implements BiFunction<Object, Object, Comparable> {
    public Comparable apply (Object a, Object b) {
      return (Math.abs (((Number)a).doubleValue() - ((Number)b).doubleValue()));
    } // apply
  } // FloatDistanceMeasure

  ////////////////////////////////////////////////////////////

  /** Measures the distance between two integer numbers (byte, short, int, long). */
  private static class IntegerDistanceMeasure implements BiFunction<Object, Object, Comparable> {
    public Comparable apply (Object a, Object b) {
      return (Math.abs (((Number)a).longValue() - ((Number)b).longValue()));
    } // apply
  } // IntegerDistanceMeasure

  ////////////////////////////////////////////////////////////

  /**
   * Filters a collection of features using the algorithm in this filter for
   * matching.
   * 
   * @param features the features to filter.
   *
   * @return the list of the matching features.
   */
  public List<Feature> filter (
    List<Feature> features
  ) {
  
    List<Feature> filteredList = new ArrayList<Feature>();
    if (features.size() != 0) {

      // Find filtering attribute value type
      // -----------------------------------
      int attIndex = nameMap.get (filterAttName);
      Object attVal = features
        .stream()
        .map (feature -> feature.getAttribute (attIndex))
        .filter (obj -> obj != null)
        .findFirst().orElse (null);

      if (attVal != null) {

        // Get distance function
        // ---------------------
        BiFunction<Object, Object, Comparable> distFunc = null;
        if (attVal instanceof Date)
          distFunc = new DateDistanceMeasure();
        else if (attVal instanceof Double || attVal instanceof Float)
          distFunc = new FloatDistanceMeasure();
        else if (attVal instanceof Number)
          distFunc = new IntegerDistanceMeasure();
        else
          throw new RuntimeException ("Unsupported attribute type: " + attVal.getClass());

        // Create group lists
        // ------------------
        int groupIndex = nameMap.get (groupAttName);
        Map<Object, List<Feature>> featureLists = features.stream()
          .collect (Collectors.groupingBy (f -> f.getAttribute (groupIndex)));
      
        // Find minimum distance feature in each group
        // -------------------------------------------
        for (List<Feature> list : featureLists.values()) {
          Comparable minDist = null;
          Feature minDistFeature = null;
          for (Feature feature : list) {
            Object featureAttVal = feature.getAttribute (attIndex);
            if (featureAttVal != null) {
              Comparable dist = distFunc.apply (featureAttVal, targetAttValue);
              if (minDist == null || dist.compareTo (minDist) < 0) {
                minDist = dist;
                minDistFeature = feature;
              } // if
            } // if
          } // for
          if (minDistFeature != null) filteredList.add (minDistFeature);
        } // for
    
      } // if

    } // if

    return (filteredList);
  
  } // filter

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new group filter with the specified properties.
   * 
   * @param groupAttName the name of the attribute to use for creating groups
   * of features.
   * @param nameMap the name to index mapping.
   * @param filterAttName the name of the attribute to use for filtering.
   * @param targetAttValue the target attribute value to filter for.
   */
  public FeatureGroupFilter (
    String groupAttName,
    Map<String, Integer> nameMap,
    String filterAttName,
    Object targetAttValue
  ) {
  
    this.groupAttName = groupAttName;
    this.nameMap = nameMap;
    this.filterAttName = filterAttName;
    this.targetAttValue = targetAttValue;

  } // FeatureGroupFilter

  ////////////////////////////////////////////////////////////

  /**
   * Sets the grouping attribute.  During filtering, features will be grouped 
   * according to the value of their grouping attribute (according to 
   * Object.equals()) and then filtered by target value.
   *
   * @param groupAttName the name of the grouping attribute to use while filtering.
   */
  public void setGroupAttribute (String groupAttName) { this.groupAttName = groupAttName; }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the grouping attribute name.
   *
   * @return grouping attribute name.
   *
   * @see #setGroupAttribute
   */
  public String getGroupAttribute() { return (groupAttName); }
  
  ////////////////////////////////////////////////////////////

  /**
   * Sets the filtering attribute.  The filtering attribute is used to search 
   * for the feature in each group with closest attribute value to a target
   * value.
   *
   * @param filterAttName the name of the filtering attribute to use.
   */
  public void setFilterAttribute (String filterAttName) { this.filterAttName = filterAttName; }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the filter attribute name.
   *
   * @return filter attribute name.
   *
   * @see #setFilterAttribute
   */
  public String getFilterAttribute() { return (filterAttName); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the target attribute value  The target value is used to filter each
   * feature group.  Only the feature from each group with the closest value
   * to the target value is retained.
   *
   * @param targetAttValue the target value to use for measuring distance.
   */
  public void setTargetValue (Object targetAttValue) { this.targetAttValue = targetAttValue; }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the target value.
   *
   * @return target value object.
   *
   * @see #setTargetValue
   */
  public Object getTargetValue() { return (targetAttValue); }

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (FeatureGroupFilter.class);

    // ------------------------->

    logger.test ("Framework");

    // 5 attributes: id, time, sst, quality, wind
    Map<String, Integer> nameMap = new HashMap<String, Integer>();
    int n = 0;
    nameMap.put ("id", n++);
    nameMap.put ("time", n++);
    nameMap.put ("sst", n++);
    nameMap.put ("quality", n++);
    nameMap.put ("wind", n++);
    assert (nameMap.containsKey ("id"));
    assert (nameMap.containsKey ("time"));
    assert (nameMap.containsKey ("sst"));
    assert (nameMap.containsKey ("quality"));
    assert (nameMap.containsKey ("wind"));
    
    // 5 features, each with 5 attributes
    long now = new Date().getTime();
    long hour = 60*60*1000L;

    PointFeature feature1 = new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {"PLATFORM1", new Date (now+hour), 5.0, 5, 10.1}
    );
    
    PointFeature feature2 = new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {"PLATFORM1", new Date (now+2*hour), 5.0, 4, 10.2}
    );
    
    PointFeature feature3 = new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {"PLATFORM2", new Date (now-hour), 5.0, 3, 10.3}
    );
    
    PointFeature feature4 = new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {"PLATFORM2", null, 5.0, 2, 10.4}
    );
    
    PointFeature feature5 = new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {"PLATFORM3", null, 5.0, 1, 10.5}
    );

    List<Feature> featureList = new ArrayList<Feature>();
    featureList.add (feature1);
    featureList.add (feature2);
    featureList.add (feature3);
    featureList.add (feature4);
    featureList.add (feature5);

    logger.passed();

    // ------------------------->

    logger.test ("constructor");
    FeatureGroupFilter filter = new FeatureGroupFilter ("id", nameMap, "time", new Date (now));
    assert (filter.getGroupAttribute().equals ("id"));
    assert (filter.getFilterAttribute().equals ("time"));
    assert (filter.getTargetValue().equals (new Date (now)));
    logger.passed();

    // ------------------------->

    logger.test ("filter");

    List<Feature> filteredList = filter.filter (featureList);
    assert (filteredList.size() == 2);
    assert (filteredList.contains (feature1));
    assert (filteredList.contains (feature3));
    
    filter.setFilterAttribute ("wind");
    filter.setTargetValue (10.12);
    filteredList = filter.filter (featureList);
    assert (filteredList.size() == 3);
    assert (filteredList.contains (feature1));
    assert (filteredList.contains (feature3));
    assert (filteredList.contains (feature5));
    
    filter.setFilterAttribute ("quality");
    filter.setTargetValue (-1);
    filteredList = filter.filter (featureList);
    assert (filteredList.size() == 3);
    assert (filteredList.contains (feature2));
    assert (filteredList.contains (feature4));
    assert (filteredList.contains (feature5));
    
    logger.passed();

    // ------------------------->

    logger.test ("clone");

    FeatureGroupFilter filterCopy = (FeatureGroupFilter) filter.clone();

    assert (filterCopy.getGroupAttribute().equals (filter.getGroupAttribute()));
    filterCopy.setGroupAttribute ("time");
    assert (!filterCopy.getGroupAttribute().equals (filter.getGroupAttribute()));
    
    assert (filterCopy.getFilterAttribute().equals (filter.getFilterAttribute()));
    filterCopy.setFilterAttribute ("wind");
    assert (!filterCopy.getFilterAttribute().equals (filter.getFilterAttribute()));

    assert (filterCopy.getTargetValue().equals (filter.getTargetValue()));
    filterCopy.setTargetValue (new Date (now+10*hour));
    assert (!filterCopy.getTargetValue().equals (filter.getTargetValue()));

    logger.passed();

    // ------------------------->

  } // main

  ////////////////////////////////////////////////////////////

} // FeatureGroupFilter class

////////////////////////////////////////////////////////////////////////
