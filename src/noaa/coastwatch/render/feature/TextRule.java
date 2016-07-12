////////////////////////////////////////////////////////////////////////
/*
     FILE: AttributeRule.java
  PURPOSE: Selects features based on an attribute value.
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

/**
 * A <code>TextRule</code> provides a selection mechanism for
 * features based on the string value of one of the attributes.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class TextRule
  extends AttributeRule {

  // Constants
  // ---------
  
  /** The operators for this class of rule. */
  private enum Operator {
    CONTAINS,
    DOES_NOT_CONTAIN,
    BEGINS_WITH,
    ENDS_WITH,
    IS_EQUAL_TO
  } // Operator

  // Variables
  // ---------
  
  /** The text value to use for matching. */
  private String textValue;
  
  /** The operator to use for matching. */
  private Operator operator;
  
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

    super (attName, nameMap);
    setOperator (Operator.CONTAINS);
    this.textValue = textValue;

  } // TextRule

  ////////////////////////////////////////////////////////////

  @Override
  public Enum[] operators() { return (Operator.values()); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean matches (Feature feature) {

    boolean isMatch = false;
    int attIndex = nameMap.get (attName);
    String attValue = (String) feature.getAttribute (attIndex);
  
    Operator op = (Operator) operator;
    switch (op) {
    case CONTAINS:
      isMatch = (attValue.indexOf (textValue) != -1);
      break;
    case DOES_NOT_CONTAIN:
      isMatch = (attValue.indexOf (textValue) == -1);
      break;
    case BEGINS_WITH:
      isMatch = attValue.startsWith (textValue);
      break;
    case ENDS_WITH:
      isMatch = attValue.endsWith (textValue);
      break;
    case IS_EQUAL_TO:
      isMatch = attValue.equals (textValue);
      break;
    default:
    } // switch
    
    return (isMatch);

  } // matches

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) throws Exception {











  } // main

  ////////////////////////////////////////////////////////////

} // TextRule class

////////////////////////////////////////////////////////////////////////
