////////////////////////////////////////////////////////////////////////
/*
     FILE: DateRule.java
  PURPOSE: Selects features based on a date attribute value.
   AUTHOR: Peter Hollemans
     DATE: 2016/07/17
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
 * A <code>DateRule</code> provides a selection mechanism for
 * features based on a date value of one of the attributes.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class DateRule
  extends AttributeRule {

  // Constants
  // ---------
  
  /** The operators for this class of rule. */
  public enum Operator {
    IS_BEFORE,
    IS_AFTER
  } // Operator
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new date rule.
   *
   * @param attName the attribute name to use for matching.
   * @param nameMap the name to index mapping.
   * @param dateValue the date value to use for matching.
   */
  public DateRule (
    String attName,
    Map<String, Integer> nameMap,
    Date dateValue
  ) {

    super (attName, nameMap, dateValue);
    setOperator (Operator.IS_BEFORE);

  } // DateRule

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
      Operator dateOp = (Operator) operator;
      Date dateValue = (Date) matchAttValue;
      switch (dateOp) {
      case IS_BEFORE:
        isMatch = featureAttValue.before (dateValue);
        break;
      case IS_AFTER:
        isMatch = featureAttValue.after (dateValue);
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
    logger.startClass (DateRule.class);
    
    logger.test ("Framework");
    Map<String, Integer> nameMap = new HashMap<String, Integer>();
    nameMap.put ("attribute1", 0);
    nameMap.put ("attribute2", 1);
    nameMap.put ("attribute3", 2);
    assert (nameMap.containsKey ("attribute1"));
    assert (nameMap.containsKey ("attribute2"));
    assert (nameMap.containsKey ("attribute3"));
    Date date1 = new Date (10000);
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
    Date date0 = new Date (0);
    DateRule rule = new DateRule ("attribute1", nameMap, date0);
    assert (rule.getOperator().equals (Operator.IS_BEFORE));
    assert (rule.getAttribute().equals ("attribute1"));
    assert (rule.getValue().equals (date0));
    logger.passed();

    logger.test ("operators");
    List<Enum> operatorList = Arrays.asList (rule.operators());
    assert (operatorList.contains (Operator.IS_BEFORE));
    assert (operatorList.contains (Operator.IS_AFTER));
    assert (operatorList.size() == 2);
    logger.passed();

    logger.test ("matches");

    // Testing: attribute1 is before date0 etc.
    rule.setOperator (Operator.IS_BEFORE);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_AFTER);
    assert (rule.matches (feature));

    // Testing: attribute1 is before date0 etc.
    rule.setOperator (Operator.IS_BEFORE);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_AFTER);
    assert (rule.matches (feature));

    // Testing: attribute1 is before date1 etc.
    rule.setValue (date1);
    rule.setOperator (Operator.IS_BEFORE);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_AFTER);
    assert (!rule.matches (feature));

    // Testing: attribute1 is before date2 etc.
    rule.setValue (date2);
    rule.setOperator (Operator.IS_BEFORE);
    assert (rule.matches (feature));
    rule.setOperator (Operator.IS_AFTER);
    assert (!rule.matches (feature));

    // Testing: attribute3 is before date2 etc. (null test)
    rule.setAttribute ("attribute3");
    rule.setOperator (Operator.IS_BEFORE);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_AFTER);
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
    rule.setValue (new Date (38));
    assert (rule.getValue().equals (new Date (38)));
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // DateRule class

////////////////////////////////////////////////////////////////////////
