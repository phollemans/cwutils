////////////////////////////////////////////////////////////////////////
/*
     FILE: TimeWindowRule.java
  PURPOSE: Selects features based on a date attribute value.
   AUTHOR: Peter Hollemans
     DATE: 2016/07/18
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
import noaa.coastwatch.render.feature.AttributeRule;
import java.util.Map;
import java.util.Date;
import noaa.coastwatch.render.feature.TimeWindow;

// Testing
// -------
import java.util.HashMap;
import java.util.Arrays;
import noaa.coastwatch.test.TestLogger;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.util.EarthLocation;
import java.util.List;

/**
 * A <code>TimeWindowRule</code> provides a selection mechanism for
 * features based on a date value of one of the attributes and a window
 * of time around that date.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class TimeWindowRule
  extends AttributeRule {

  // Constants
  // ---------
  
  /** The operators for this class of rule. */
  public enum Operator {
    IS_WITHIN
  } // Operator
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new time window rule.
   *
   * @param attName the attribute name to use for matching.
   * @param nameMap the name to index mapping.
   * @param windowValue the time window value to use for matching.
   */
  public TimeWindowRule (
    String attName,
    Map<String, Integer> nameMap,
    TimeWindow windowValue
  ) {

    super (attName, nameMap, windowValue);
    setOperator (Operator.IS_WITHIN);

  } // TimeWindowRule

  ////////////////////////////////////////////////////////////

  @Override
  public Enum[] operators() { return (Operator.values()); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean matches (Feature feature) {

    // Get feature attribute value
    // ---------------------------
    int attIndex = nameMap.get (matchAttName);
    Date featureAttValue = (Date) feature.getAttribute (attIndex);

    // Compare to match value
    // ----------------------
    boolean isMatch = false;
    if (featureAttValue != null) {
      Operator windowOp = (Operator) operator;
      TimeWindow windowValue = (TimeWindow) matchAttValue;
      switch (windowOp) {
      case IS_WITHIN:
        isMatch = windowValue.isInWindow (featureAttValue);
        break;
      default:
      } // switch
    } // if
    
    return (isMatch);

  } // matches

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (TimeWindowRule.class);
    
    logger.test ("Framework");
    Map<String, Integer> nameMap = new HashMap<String, Integer>();
    nameMap.put ("attribute1", 0);
    nameMap.put ("attribute2", 1);
    nameMap.put ("attribute3", 2);
    assert (nameMap.containsKey ("attribute1"));
    assert (nameMap.containsKey ("attribute2"));
    assert (nameMap.containsKey ("attribute3"));
    Date date1 = new Date (13000);
    Date date2 = new Date (20000);
    Feature feature = new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {date1, date2, null}
    );
    assert (feature.getAttribute (0).equals (date1));
    assert (feature.getAttribute (1).equals (date2));
    assert (feature.getAttribute (2) == null);
    logger.passed();
    
    logger.test ("constructor");
    TimeWindow window = new TimeWindow (new Date (10000), 5000);
    TimeWindowRule rule = new TimeWindowRule ("attribute1", nameMap, window);
    assert (rule.getOperator().equals (Operator.IS_WITHIN));
    assert (rule.getAttribute().equals ("attribute1"));
    assert (rule.getValue().equals (window));
    logger.passed();

    logger.test ("operators");
    List<Enum> operatorList = Arrays.asList (rule.operators());
    assert (operatorList.contains (Operator.IS_WITHIN));
    assert (operatorList.size() == 1);
    logger.passed();

    logger.test ("matches");

    // Testing: attribute1 is within window
    rule.setOperator (Operator.IS_WITHIN);
    assert (rule.matches (feature));

    // Testing: attribute2 is within window
    rule.setAttribute ("attribute2");
    assert (!rule.matches (feature));

    // Testing: attribute3 is within window (null test)
    rule.setAttribute ("attribute3");
    assert (!rule.matches (feature));

    logger.passed();
    
    logger.test ("setAttribute, setValue");
    rule.setAttribute ("attribute2");
    assert (rule.getAttribute().equals ("attribute2"));
    boolean isError = false;
    try {
      rule.setAttribute ("attribute0");
    } // try
    catch (Exception e) { isError = true; }
    assert (isError);
    assert (rule.getAttribute().equals ("attribute2"));
    TimeWindow window1 = new TimeWindow (new Date (20000), 5000);
    assert (rule.getValue().equals (window));
    rule.setValue (window1);
    assert (rule.getValue().equals (window1));
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // TimeWindowRule class

////////////////////////////////////////////////////////////////////////
