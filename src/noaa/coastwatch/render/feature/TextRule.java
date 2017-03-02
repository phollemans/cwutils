////////////////////////////////////////////////////////////////////////
/*
     FILE: TextRule.java
  PURPOSE: Selects features based on a text attribute value.
   AUTHOR: Peter Hollemans
     DATE: 2016/07/07
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
 * A <code>TextRule</code> provides a selection mechanism for
 * features based on the string value of one of the attributes.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class TextRule
  extends AttributeRule<String> {

  // Constants
  // ---------
  
  /** The operators for this class of rule. */
  public enum Operator {
    CONTAINS,
    DOES_NOT_CONTAIN,
    BEGINS_WITH,
    ENDS_WITH,
    IS_EQUAL_TO;
    @Override
    public String toString() {
      String value = super.toString();
      value = value.toLowerCase().replaceAll ("_", " ");
      return (value);
    } // toString    
  } // Operator

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new text rule.
   *
   * @param attName the attribute name to use for matching.
   * @param nameMap the name to index mapping.
   * @param textValue the text value to use for matching.
   */
  public TextRule (
    String attName,
    Map<String, Integer> nameMap,
    String textValue
  ) {

    super (attName, nameMap, textValue);
    setOperator (Operator.CONTAINS);

  } // TextRule

  ////////////////////////////////////////////////////////////

  @Override
  public Enum[] operators() { return (Operator.values()); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean matches (Feature feature) {

    // Get feature attribute value
    // ---------------------------
    int attIndex = nameMap.get (matchAttName);
    String featureAttValue = (String) feature.getAttribute (attIndex);
  
    // Compare to match value
    // ----------------------
    boolean isMatch = false;
    if (featureAttValue != null) {
      Operator textOp = (Operator) operator;
      switch (textOp) {
      case CONTAINS:
        isMatch = (featureAttValue.indexOf (matchAttValue) != -1);
        break;
      case DOES_NOT_CONTAIN:
        isMatch = (featureAttValue.indexOf (matchAttValue) == -1);
        break;
      case BEGINS_WITH:
        isMatch = featureAttValue.startsWith (matchAttValue);
        break;
      case ENDS_WITH:
        isMatch = featureAttValue.endsWith (matchAttValue);
        break;
      case IS_EQUAL_TO:
        isMatch = featureAttValue.equals (matchAttValue);
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
    logger.startClass (TextRule.class);

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
      new Object[] {"abcdef", "abc", null}
    );
    assert (feature.getAttribute (0).equals ("abcdef"));
    logger.passed();
    
    logger.test ("constructor");
    TextRule rule = new TextRule ("attribute1", nameMap, "abc");
    assert (rule.getOperator().equals (Operator.CONTAINS));
    assert (rule.getAttribute().equals ("attribute1"));
    assert (rule.getValue().equals ("abc"));
    logger.passed();

    logger.test ("operators");
    List<Enum> operatorList = Arrays.asList (rule.operators());
    assert (operatorList.contains (Operator.CONTAINS));
    assert (operatorList.contains (Operator.DOES_NOT_CONTAIN));
    assert (operatorList.contains (Operator.BEGINS_WITH));
    assert (operatorList.contains (Operator.ENDS_WITH));
    assert (operatorList.contains (Operator.IS_EQUAL_TO));
    assert (operatorList.size() == 5);
    logger.passed();

    logger.test ("matches");

    // Testing: attribute1 contains "abc" etc.
    rule.setOperator (Operator.CONTAINS);
    assert (rule.matches (feature));
    rule.setOperator (Operator.DOES_NOT_CONTAIN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.BEGINS_WITH);
    assert (rule.matches (feature));
    rule.setOperator (Operator.ENDS_WITH);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_EQUAL_TO);
    assert (!rule.matches (feature));

    // Testing: attribute1 contains "def" etc.
    rule.setValue ("def");
    rule.setOperator (Operator.CONTAINS);
    assert (rule.matches (feature));
    rule.setOperator (Operator.DOES_NOT_CONTAIN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.BEGINS_WITH);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.ENDS_WITH);
    assert (rule.matches (feature));
    rule.setOperator (Operator.IS_EQUAL_TO);
    assert (!rule.matches (feature));

    // Testing: attribute1 contains "abcdef" etc.
    rule.setValue ("abcdef");
    rule.setOperator (Operator.CONTAINS);
    assert (rule.matches (feature));
    rule.setOperator (Operator.DOES_NOT_CONTAIN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.BEGINS_WITH);
    assert (rule.matches (feature));
    rule.setOperator (Operator.ENDS_WITH);
    assert (rule.matches (feature));
    rule.setOperator (Operator.IS_EQUAL_TO);
    assert (rule.matches (feature));
    
    // Testing: attribute3 contains "abcdef" etc. (null test)
    rule.setAttribute ("attribute3");
    rule.setOperator (Operator.CONTAINS);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.DOES_NOT_CONTAIN);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.BEGINS_WITH);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.ENDS_WITH);
    assert (!rule.matches (feature));
    rule.setOperator (Operator.IS_EQUAL_TO);
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
    rule.setValue ("hello");
    assert (rule.getValue().equals ("hello"));
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // TextRule class

////////////////////////////////////////////////////////////////////////
