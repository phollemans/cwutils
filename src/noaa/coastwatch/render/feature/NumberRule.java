////////////////////////////////////////////////////////////////////////
/*
     FILE: NumberRule.java
  PURPOSE: Selects features based on a numerical attribute value.
   AUTHOR: Peter Hollemans
     DATE: 2016/07/12
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
 * A <code>NumberRule</code> provides a selection mechanism for
 * features based on the numercial value of one of the attributes.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class NumberRule
  extends AttributeRule<Number> {

  // Constants
  // ---------
  
  /** The operators for this class of rule. */
  public enum Operator {
    IS_GREATER_THAN,
    IS_LESS_THAN,
    IS_EQUAL_TO,
    IS_NOT_EQUAL_TO,
    CONTAINS_BITS_FROM,
    DOES_NOT_CONTAIN_BITS_FROM;
    @Override
    public String toString() {
      String value = super.toString();
      value = value.toLowerCase().replaceAll ("_", " ");
      return (value);
    } // toString
  } // Operator

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new number rule.
   *
   * @param attName the attribute name to use for matching.
   * @param nameMap the name to index mapping.
   * @param numberValue the number value to use for matching.
   */
  public NumberRule (
    String attName,
    Map<String, Integer> nameMap,
    Number numberValue
  ) {

    super (attName, nameMap, numberValue);
    setOperator (Operator.IS_GREATER_THAN);

  } // NumberRule

  ////////////////////////////////////////////////////////////

  @Override
  public Enum[] operators() { return (Operator.values()); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean matches (Feature feature) {

    // Get feature attribute value
    // ---------------------------
    int attIndex = nameMap.get (matchAttName);
    Number featureAttValue = (Number) feature.getAttribute (attIndex);

    // Compare to match value
    // ----------------------
    boolean isMatch = false;
    if (featureAttValue != null) {
      Operator numberOp = (Operator) operator;
      if (featureAttValue instanceof Float || featureAttValue instanceof Double) {
        switch (numberOp) {
        case IS_GREATER_THAN:
          isMatch = (featureAttValue.doubleValue() > matchAttValue.doubleValue());
          break;
        case IS_LESS_THAN:
          isMatch = (featureAttValue.doubleValue() < matchAttValue.doubleValue());
          break;
        case IS_EQUAL_TO:
          isMatch = (featureAttValue.doubleValue() == matchAttValue.doubleValue());
          break;
        case IS_NOT_EQUAL_TO:
          isMatch = (featureAttValue.doubleValue() != matchAttValue.doubleValue());
          break;
        default:
        } // switch
      } // if
      else {
        switch (numberOp) {
        case IS_GREATER_THAN:
          isMatch = (featureAttValue.longValue() > matchAttValue.longValue());
          break;
        case IS_LESS_THAN:
          isMatch = (featureAttValue.longValue() < matchAttValue.longValue());
          break;
        case IS_EQUAL_TO:
          isMatch = (featureAttValue.longValue() == matchAttValue.longValue());
          break;
        case IS_NOT_EQUAL_TO:
          isMatch = (featureAttValue.longValue() != matchAttValue.longValue());
          break;
        case CONTAINS_BITS_FROM:
          isMatch = ((featureAttValue.longValue() & matchAttValue.longValue()) != 0);
          break;
        case DOES_NOT_CONTAIN_BITS_FROM:
          isMatch = ((featureAttValue.longValue() & matchAttValue.longValue()) == 0);
          break;
        default:
        } // switch
      } // else
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
    logger.startClass (NumberRule.class);
    
    logger.test ("Framework");
    Map<String, Integer> nameMap = new HashMap<String, Integer>();
    nameMap.put ("attribute1", 0);
    nameMap.put ("attribute2", 1);
    nameMap.put ("attribute3", 2);
    assert (nameMap.containsKey ("attribute1"));
    assert (nameMap.containsKey ("attribute2"));
    assert (nameMap.containsKey ("attribute3"));
    Feature feature = new PointFeature (
      new EarthLocation (0, 0),
      new Object[] {1, 2, null}
    );
    assert (feature.getAttribute (0).equals (1));
    logger.passed();
    
    logger.test ("constructor");
    NumberRule rule = new NumberRule ("attribute1", nameMap, 1);
    assert (rule.getOperator().equals (Operator.IS_GREATER_THAN));
    assert (rule.getAttribute().equals ("attribute1"));
    assert (rule.getValue().equals (1));
    logger.passed();

    logger.test ("operators");
    List<Enum> operatorList = Arrays.asList (rule.operators());
    assert (operatorList.contains (Operator.IS_GREATER_THAN));
    assert (operatorList.contains (Operator.IS_LESS_THAN));
    assert (operatorList.contains (Operator.IS_EQUAL_TO));
    assert (operatorList.contains (Operator.IS_NOT_EQUAL_TO));
    assert (operatorList.contains (Operator.CONTAINS_BITS_FROM));
    assert (operatorList.contains (Operator.DOES_NOT_CONTAIN_BITS_FROM));
    assert (operatorList.size() == 6);
    logger.passed();

    logger.test ("matches");

    // Testing: attribute1 is greater than 1 etc.
    rule.setOperator (Operator.IS_GREATER_THAN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_LESS_THAN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_EQUAL_TO);
    assert (rule.matches (feature));
    rule.setOperator (Operator.IS_NOT_EQUAL_TO);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.CONTAINS_BITS_FROM);
    assert (rule.matches (feature));
    rule.setOperator (Operator.DOES_NOT_CONTAIN_BITS_FROM);
    assert (!rule.matches (feature));

    // Testing: attribute1 is greater than 2 etc.
    rule.setValue (2);
    rule.setOperator (Operator.IS_GREATER_THAN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_LESS_THAN);
    assert (rule.matches (feature));
    rule.setOperator (Operator.IS_EQUAL_TO);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_NOT_EQUAL_TO);
    assert (rule.matches (feature));
    rule.setOperator (Operator.CONTAINS_BITS_FROM);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.DOES_NOT_CONTAIN_BITS_FROM);
    assert (rule.matches (feature));

    // Testing: attribute1 is greater than 0 etc.
    rule.setValue (0);
    rule.setOperator (Operator.IS_GREATER_THAN);
    assert (rule.matches (feature));
    rule.setOperator (Operator.IS_LESS_THAN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_EQUAL_TO);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_NOT_EQUAL_TO);
    assert (rule.matches (feature));
    rule.setOperator (Operator.CONTAINS_BITS_FROM);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.DOES_NOT_CONTAIN_BITS_FROM);
    assert (rule.matches (feature));

    // Testing: attribute3 is greater than 0 etc. (null test)
    rule.setAttribute ("attribute3");
    rule.setOperator (Operator.IS_GREATER_THAN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_LESS_THAN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_EQUAL_TO);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_NOT_EQUAL_TO);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.CONTAINS_BITS_FROM);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.DOES_NOT_CONTAIN_BITS_FROM);
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
    rule.setValue (38);
    assert (rule.getValue().equals (38));
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // NumberRule class

////////////////////////////////////////////////////////////////////////
