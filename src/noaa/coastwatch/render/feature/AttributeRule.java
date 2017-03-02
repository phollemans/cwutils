////////////////////////////////////////////////////////////////////////
/*

     File: AttributeRule.java
   Author: Peter Hollemans
     Date: 2016/07/07

  CoastWatch Software Library and Utilities
  Copyright (c) 2016 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.render.feature.SelectionRule;
import java.util.Arrays;
import java.util.Map;

/**
 * An <code>AttributeRule</code> provides a selection mechanism for
 * features based on the value of one of the attributes.  If a feature has an
 * attribute value that matches, it is considered a matching
 * feature.  If the feature attribute value is null, the feature is 
 * non-matching no matter what operator is used.  The actual matching 
 * operation is left to the subclass depending on the attribute type.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public abstract class AttributeRule<T>
  implements SelectionRule, Cloneable {

  // Variables
  // ---------
  
  /** The attribute to use for matching. */
  protected String matchAttName;
  
  /** The attribute value to use for matching. */
  protected T matchAttValue;
  
  /** The map to use to translate name to index for attributes. */
  protected Map<String, Integer> nameMap;
  
  /** Thee rule operator currently being used. */
  protected Enum operator;

  ////////////////////////////////////////////////////////////

  @Override
  public Object clone () {

    AttributeRule copy;
    try { copy = (AttributeRule) super.clone(); }
    catch (CloneNotSupportedException e) { copy = null; }
    
    return (copy);

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new attribute rule.
   *
   * @param attName the attribute name to use for matching.
   * @param nameMap the name to index mapping for attributes.
   * @param attValue the attribute value to use for matching.
   *
   * @throws RuntimeException if the attribute name has no valid mapping
   * to an index.
   */
  protected AttributeRule (
    String attName,
    Map<String, Integer> nameMap,
    T attValue
  ) {

    this.nameMap = nameMap;
    setAttribute (attName);
    setValue (attValue);

  } // AttributeRule

  ////////////////////////////////////////////////////////////

  /**
   * Gets the attribute name for this rule.
   *
   * @return the attribute name used by this rule for matching.
   */
  public String getAttribute() { return (matchAttName); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the attribute name for this rule.
   *
   * @param attName the attribute name to use for matching.
   *
   * @throws RuntimeException if the attribute name has no valid mapping
   * to an index.
   */
  public void setAttribute (String attName) {
  
    if (!nameMap.containsKey (attName))
      throw new RuntimeException ("Unknown index for attribute '" + attName + "'");

    this.matchAttName = attName;
    
  } // setAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Gets the attribute value for this rule.
   *
   * @return the attribute value used by this rule for matching.
   */
  public T getValue() { return (matchAttValue); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the attribute value for this rule.
   *
   * @param attValue the attribute value to use for matching.
   */
  public void setValue (T attValue) { this.matchAttValue = attValue; }

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

  @Override
  public String toString () {

    return ("AttributeRule[" +
      "att=" + matchAttName + "," +
      "op=" + operator + "," +
      "value=" + matchAttValue +
      "]");

  } // toString

  ////////////////////////////////////////////////////////////

} // AttributeRule class

////////////////////////////////////////////////////////////////////////
