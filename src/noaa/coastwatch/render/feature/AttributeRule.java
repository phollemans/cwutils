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
import noaa.coastwatch.render.feature.SelectionRule;
import java.util.Arrays;
import java.util.Map;

/**
 * An <code>AttributeRule</code> provides a selection mechanism for
 * features based on the value of one of the attributes.  If a feature has an
 * attribute value that matches, it is considered a matching
 * feature.  The actual matching operation is left to the subclass depending
 * on the attribute type.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public abstract class AttributeRule
  implements SelectionRule {

  // Variables
  // ---------
  
  /** The attribute to use for matching. */
  protected String attName;
  
  /** The map to use to translate name to index for attributes. */
  protected Map<String, Integer> nameMap;
  
  /** Thee rule operator currently being used. */
  protected Enum operator;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new attribute rule.
   *
   * @param attName the attribute name to use for matching.
   * @param nameMap the name to index mapping for attributes.
   */
  protected AttributeRule (
    String attName,
    Map<String, Integer> nameMap
  ) {

    this.attName = attName;
    this.nameMap = nameMap;

  } // AttributeRule

  ////////////////////////////////////////////////////////////

  /**
   * Gets the attribute name for this rule.
   *
   * @return the attribute name used by this rule for matching.
   */
  public String getAttribute() { return (attName); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets an array of operators that are valid for this rule.
   *
   * @return the array of valid operators.
   */
  public abstract Enum[] operators();

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current rule operator.
   *
   * @return the current operator.
   */
  public Enum getOperator() { return (operator); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the rule operator.
   * 
   * @param operator the new rule operator.
   */
  public void setOperator (Enum operator) {
  
    if (!Arrays.asList (operators()).contains (operator))
      throw new RuntimeException ("Operator '" + operator + "' not supported by this rule");

    this.operator = operator;
    
  } // setOperator

  ////////////////////////////////////////////////////////////

} // AttributeRule class

////////////////////////////////////////////////////////////////////////
