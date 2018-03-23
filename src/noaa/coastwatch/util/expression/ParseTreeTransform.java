////////////////////////////////////////////////////////////////////////
/*

     File: ParseTreeTransform.java
   Author: Peter Hollemans
     Date: 2018/02/23

  CoastWatch Software Library and Utilities
  Copyright (c) 2018 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.expression;

// Imports
// -------
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A <code>ParseTreeTransform</code> stores a set of rules to transform a
 * DOM.  Each rule is an XPath expression which will be evaluated and the
 * resulting nodes run through a specified function to alter the DOM tree
 * in place.
 */
public class ParseTreeTransform {

  // Variables
  // ---------
  
  /** The map of xpath expression to rule for use on each matching node. */
  private Map<String, Consumer<Node>> ruleMap = new LinkedHashMap<>();

  ////////////////////////////////////////////////////////////

  /**
   * Adds a rule to the transform.
   *
   * @param xpath the XPath expression to use for producing a node list.
   * @param rule the function to apply to each node in the resulting list.
   */
  public void addRule (
    String xpath,
    Consumer<Node> rule
  ) {

    ruleMap.put (xpath, rule);

  } // addRule

  ////////////////////////////////////////////////////////////

  /**
   * Performs a transformation of the specified document in-place using the
   * rules in this transform.
   *
   * @param doc the document to transform.
   */
  public void transform (Document doc) {

    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();

    ruleMap.forEach ((xpathStr, rule) -> {
      try {
        XPathExpression expr = xpath.compile (xpathStr);
        NodeList nodes = (NodeList) expr.evaluate (doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) rule.accept (nodes.item (i));
      } // try
      catch (XPathExpressionException e) {
        throw new RuntimeException ("Error evaluating XPath expression " + xpathStr);
      } // catch
    });

  } // transform

  ////////////////////////////////////////////////////////////


} // ParseTreeTransform class

////////////////////////////////////////////////////////////////////////
