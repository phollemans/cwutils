////////////////////////////////////////////////////////////////////////
/*

     File: JEPEmulationParser.java
   Author: Peter Hollemans
     Date: 2017/11/11

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
package noaa.coastwatch.util.expression;

// Imports
// -------
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

/*

import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParserVisitor;
import org.nfunk.jep.ASTConstant;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.ASTStart;
import org.nfunk.jep.ASTVarNode;
import org.nfunk.jep.SimpleNode;
*/

import noaa.coastwatch.util.expression.ExpressionParser;
import noaa.coastwatch.util.expression.JEPParser;
import noaa.coastwatch.util.expression.JELParser;
import noaa.coastwatch.util.expression.ParseImp;
import noaa.coastwatch.util.expression.EvaluateImp;
import noaa.coastwatch.util.expression.ParseTreeTransform;

// Testing
import noaa.coastwatch.test.TestLogger;
import java.util.ArrayList;

/**
 * The <code>JEPEmulationParser</code> class emulates a JEP syntax
 * parser {@link JEPParser} using a high speed JEL parser {@link JELParser}
 * by translating the expression and emulating the output type
 * behaviour.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
@noaa.coastwatch.test.Testable
public class JEPEmulationParser implements ExpressionParser {

  // Variables
  // ---------

  /** The JEP parser used for parsing and parse tree translation. */
  private JEPParser jepParser;

  /** The JEL parser instance used for final parsing and evaluation. */
  private JELParser jelParser;

  ////////////////////////////////////////////////////////////

  /** Creates a new JEP emulation parser. */
  public JEPEmulationParser () {
  
    jepParser = new JEPParser();
    jelParser = new JELParser();

  } // JEPEmulationParser

  ////////////////////////////////////////////////////////////

  @Override
  public void init (ParseImp parseImp) {

    jepParser.init (parseImp);
    jelParser.init (parseImp);

  } // init

  ////////////////////////////////////////////////////////////

  /**
   * Replaces a node with another in the DOM.  After calling, the new node
   * is installed as a replacement child of the parent, and the old node's
   * children are transferred over to the new node.
   *
   * @param oldNode the old node to replace.
   * @param newNode the new node to replace with.
   */
  private void replaceNode (Node oldNode, Node newNode) {

    // Transfer children
    // -----------------
    Node child;
    while ((child = oldNode.getFirstChild()) != null)
      newNode.appendChild (child);

    // Replace in parent
    // -----------------
    oldNode.getParentNode().replaceChild (newNode, oldNode);

  } // replaceNode

  ////////////////////////////////////////////////////////////

  /** Convenience method for creating an operator element. */
  private Element newOperator (Document doc, String symbol) {
    Element op = doc.createElement ("operator");
    op.setAttribute ("symbol", symbol);
    return (op);
  } // newOperator
  
  ////////////////////////////////////////////////////////////

  /** Convenience method for creating a function element. */
  private Element newFunction (Document doc, String name) {
    Element func = doc.createElement ("function");
    func.setAttribute ("name", name);
    return (func);
  } // newFunction
  
  ////////////////////////////////////////////////////////////

  /** Convenience method for creating a constant element. */
  private Element newConstant (Document doc, String value) {
    Element con = doc.createElement ("constant");
    con.setAttribute ("value", value);
    return (con);
  } // newConstant
  
  ////////////////////////////////////////////////////////////

  /** Convenience method for creating a variable element. */
  private Element newVariable (Document doc, String name) {
    Element var = doc.createElement ("variable");
    var.setAttribute ("name", name);
    return (var);
  } // newVariable
  
  ////////////////////////////////////////////////////////////

  /** Convenience method for creating a cast operator element. */
  private Element newCast (Document doc, Node exp, String type) {
    Element op = newOperator (doc, "(" + type + ")");
    op.appendChild (exp);
    return (op);
  } // newCast

  ////////////////////////////////////////////////////////////

  /**
   * Transforms a DOM parse tree with JEP idioms into a JEL-compatible
   * parse tree.
   *
   * @param doc the DOM tree to transform in-place.
   */
  private void transformJEPToJEL (Document doc) {

    ParseTreeTransform trans = new ParseTreeTransform();

    trans.addRule ("//operator[@symbol='^']", node -> replaceNode (node, newFunction (doc, "pow")));

    // !var --> (var == 0)
    trans.addRule ("//operator[@symbol='!']", node -> {
      Element equals = newOperator (doc, "==");
      replaceNode (node, equals);
      equals.appendChild (newConstant (doc, "0"));
    });

    trans.addRule ("//function[@name='mod']", node -> replaceNode (node, newOperator (doc, "%")));
    trans.addRule ("//function[@name='select']", node -> replaceNode (node, newOperator (doc, "?:")));

    trans.addRule ("//function[@name='log']", node -> ((Element) node).setAttribute ("name", "log10"));
    trans.addRule ("//function[@name='ln']", node -> ((Element) node).setAttribute ("name", "log"));
    trans.addRule ("//function[@name='angle']", node -> ((Element) node).setAttribute ("name", "atan2"));
    trans.addRule ("//function[@name='rand']", node -> ((Element) node).setAttribute ("name", "random"));
    trans.addRule ("//function[@name='hex']", node -> ((Element) node).setAttribute ("name", ""));

    // mask (var, mask, bits) --> (mask & bits) == 0 ? var : NaN
    trans.addRule ("//function[@name='mask']", node -> {
      NodeList children = node.getChildNodes();
      Node var = children.item (0);
      Node mask = children.item (1);
      Node bits = children.item (2);
      Element and = newOperator (doc, "&");
      and.appendChild (newCast (doc, mask, "long"));
      and.appendChild (newCast (doc, bits, "long"));
      Element equals = newOperator (doc, "==");
      equals.appendChild (and);
      equals.appendChild (newConstant (doc, "0"));
      Element select = newOperator (doc, "?:");
      select.appendChild (equals);
      select.appendChild (var);
      select.appendChild (newVariable (doc, "NaN"));
      node.getParentNode().replaceChild (select, node);
    });

    Map<String, String> bitOperatorMap = new HashMap<>();
    bitOperatorMap.put ("and", "&");
    bitOperatorMap.put ("or", "|");
    bitOperatorMap.put ("xor", "^");
    bitOperatorMap.put ("not", "~");
    bitOperatorMap.forEach ((key, value) -> {
      trans.addRule ("//function[@name='" + key + "']", node -> {
        Element op = newOperator (doc, value);
        while (node.getChildNodes().getLength() != 0)
          op.appendChild (newCast (doc, node.getFirstChild(), "long"));
        node.getParentNode().replaceChild (op, node);
      });
    });

    // (cond ? x : y) && expression --> (cond ? x : y) != 0 && expression
    //         &&
    //      ?:    expression
    // cond
    trans.addRule ("//operator[@symbol='&&']/operator[@symbol='?:']", node -> {
      Element notEquals = newOperator (doc, "!=");
      node.getParentNode().replaceChild (notEquals, node);
      notEquals.appendChild (node);
      notEquals.appendChild (newConstant (doc, "0"));
    });
    
    // (cond ? x : y) || expression --> (cond ? x : y) != 0 || expression
    //         ||
    //      ?:    expression
    // cond
    trans.addRule ("//operator[@symbol='||']/operator[@symbol='?:']", node -> {
      Element notEquals = newOperator (doc, "!=");
      node.getParentNode().replaceChild (notEquals, node);
      notEquals.appendChild (node);
      notEquals.appendChild (newConstant (doc, "0"));
    });

    trans.addRule ("//variable[@name='e']", node -> ((Element) node).setAttribute ("name", "E"));
    trans.addRule ("//variable[@name='pi']", node -> ((Element) node).setAttribute ("name", "PI"));
    trans.addRule ("//variable[@name='nan']", node -> ((Element) node).setAttribute ("name", "NaN"));

    trans.transform (doc);

  } // transformJEPToJEL

  ////////////////////////////////////////////////////////////

  /**
   * Optimizes a JEL DOM parse tree.
   *
   * @param doc the DOM tree to optimize in-place.
   */
  private void optimizeTree (Document doc) {
  
    // It's useful at this point to have a little primer on the XPath syntax
    // used below because the online resources are scattered and bookmarks
    // become invalid (although try searching "xpath cheatsheet" or visit
    // https://devhints.io/xpath).  XPath is used to uniquely match specific
    // fragments of an XML document tree.  The tree contains a parsed expression
    // from the JEP parser that has been translated into JEL syntax and contains
    // nodes named 'operator', 'function', 'variable', and 'constant'.  The
    // goal is to correctly optimize an expression that's been translated
    // from JEP to JEL into a simpler version of the same expression, with the
    // effect of making it faster to evaluate.  For example, the expression:
    //
    //   (condition ? 0 : 1) != 0
    //
    // involves two operators (tertiary if/then/else and the equality test)
    // plus the condition evaluation and has a parse tree of:
    //
    //   <operator symbol='!='>
    //     <operator symbol='?!'>
    //       (condition tree)
    //       <constant value='0'/>
    //       <constant value='1'/>
    //     </operator>
    //     <constant value='0'/>
    //   </operator>
    //
    // and can be manipulated to transform it to:
    //
    //   <operator symbol='!'>
    //     (condition tree)
    //   </operator>
    //
    // which has just the condition evaluation and a not operator, which will
    // run faster.  Some examples of XPath fragments used:
    //
    // /step/step/step -- The general XPath syntax performs location steps
    // through the tree (default from parent to child) and at each step
    // specifies the step with the syntax: axisname::nodetest[predicate] where
    // omitting the axisname:: part means "look through the children of the
    // current node", ie: child::.
    //
    // //operator -- Look for a node named "operator" that is a descendent
    // anywhere in the tree.  The // means that the node can occur with any
    // line of parents from the current node which starts with the root node.
    //
    // [@symbol='*'] -- Match a node whose attribute "symbol" has the value "*",
    // ie: in our case the multiplication operator.
    //
    // .. -- Back up and start the next step at the parent of the current
    // node.
    //
    // parent::operator -- Select the parent of the current node that has the
    // name "operator" if it exists.
    //
    // following-sibling::constant[@value='1'] -- Select the sibling of this
    // node that is a constant with value 1.
    
    ParseTreeTransform trans = new ParseTreeTransform();

    // (var*0 == 0) --> !isNaN (var)
    //        ==
    //      *    0
    //  var   0
    trans.addRule ("//operator[@symbol='*']/constant[@value='0']/../parent::operator[@symbol='==']/constant[@value='0']/..//variable", node -> {
      Element isNaN = newFunction (doc, "isNaN");
      Node equals = node.getParentNode().getParentNode();
      isNaN.appendChild (node);
      Element not = newOperator (doc, "!");
      not.appendChild (isNaN);
      equals.getParentNode().replaceChild (not, equals);
    });

    // (condition ? 0 : 1) != 0 --> !condition
    //                      !=
    //               ?:             0
    //   condition   0   1
    trans.addRule ("//operator[@symbol='?:']/*[2]/self::constant[@value='0']/following-sibling::constant[@value='1']/../parent::operator[@symbol='!=']/constant[@value='0']/../operator[@symbol='?:']/*[1]", node -> {
      Element not = newOperator (doc, "!");
      Node notEqual = node.getParentNode().getParentNode();
      not.appendChild (node);
      notEqual.getParentNode().replaceChild (not, notEqual);
    });

    // (condition ? 1 : 0) != 0 --> condition
    //                      !=
    //               ?:             0
    //   condition   1   0
    trans.addRule ("//operator[@symbol='?:']/*[2]/self::constant[@value='1']/following-sibling::constant[@value='0']/../parent::operator[@symbol='!=']/constant[@value='0']/../operator[@symbol='?:']/*[1]", node -> {
      Node notEqual = node.getParentNode().getParentNode();
      notEqual.getParentNode().replaceChild (node, notEqual);
    });

    // !!condition --> condition
    //       !
    //       !
    //   condition
    trans.addRule ("//operator[@symbol='!']/operator[@symbol='!']", node -> {
      Node condition = node.getFirstChild();
      Node top = node.getParentNode().getParentNode();
      top.replaceChild (condition, node.getParentNode());
    });

    trans.transform (doc);

  } // optimizeTree

  ////////////////////////////////////////////////////////////

  /**
   * Prints a DOM parse tree to a string.
   *
   * @param node the root node for the DOM tree to print.
   */
  private String printTree (Node node) {

    String retValue = "";
    Element element = (Element) node;
    String tagName = element.getTagName();

    // Handle top level
    // ----------------
    if (tagName.equals ("expression"))
      retValue = printTree (node.getFirstChild());

    // Handle leaf tags
    // ----------------
    else if (tagName.equals ("variable"))
      retValue = element.getAttribute ("name");
    else if (tagName.equals ("constant"))
      retValue = element.getAttribute ("value");

    else {

      // Handle operators
      // ----------------
      NodeList children = node.getChildNodes();
      int childCount = children.getLength();
      if (tagName.equals ("operator")) {
    
        String symbol = element.getAttribute ("symbol");
        if (childCount == 1) {
          retValue = symbol + printTree (children.item (0));
        } // if

        else if (childCount == 2) {
          retValue = "(" +
            printTree (children.item (0)) + " " +
            symbol + " " +
            printTree (children.item (1)) + ")";
        } // else if

        else if (childCount == 3 && symbol.equals ("?:")) {
          retValue = "(" +
            printTree (children.item (0)) + " ? " +
            printTree (children.item (1)) + " : " +
            printTree (children.item (2)) + ")";
        } // else if
    
      } // if

      // Handle functions
      // ----------------
      else if (tagName.equals ("function")) {

        retValue = element.getAttribute ("name") + " (";
        for (int i = 0; i < childCount; i++) {
          String child = printTree (children.item (i));
          if (child.matches ("\\(.*\\)")) child = child.substring (1, child.length()-1);
          retValue += child;
          if (i != childCount-1) retValue += ", ";
        } // for
        retValue += ")";

      } // else if
      
    } // else

    return (retValue);

  } // printTree
  
  ////////////////////////////////////////////////////////////

  @Override
  public String translate (String expr) {

    // Parse expression in JEP
    // -----------------------
    jepParser.parse (expr);
    Document doc = jepParser.getParseTree();

    // Transform and optimize
    // ----------------------
    transformJEPToJEL (doc);
    optimizeTree (doc);
    String translatedExpr = printTree (doc.getFirstChild());

    return (translatedExpr);

  } // translate

  ////////////////////////////////////////////////////////////

  @Override
  public void parse (String expr) {

    jelParser.parse (translate (expr));

  } // parse

  ////////////////////////////////////////////////////////////

  @Override
  public ResultType getResultType() { return (ResultType.DOUBLE); }

  ////////////////////////////////////////////////////////////

  @Override
  public List<String> getVariables() { return (jelParser.getVariables()); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object evaluate (EvaluateImp evalImp) { return (evaluateToDouble (evalImp)); }

  ////////////////////////////////////////////////////////////

  @Override
  public double evaluateToDouble (EvaluateImp evalImp) {
 
    double resultValue;
    ResultType type = jelParser.getResultType();
    switch (type) {
    case BOOLEAN: resultValue = (jelParser.evaluateToBoolean (evalImp) ? 1.0 : 0.0); break;
    case BYTE: resultValue = jelParser.evaluateToByte (evalImp); break;
    case SHORT: resultValue = jelParser.evaluateToShort (evalImp); break;
    case INT: resultValue = jelParser.evaluateToInt (evalImp); break;
    case LONG: resultValue = (double) jelParser.evaluateToLong (evalImp); break;
    case FLOAT: resultValue = jelParser.evaluateToFloat (evalImp); break;
    case DOUBLE: resultValue = jelParser.evaluateToDouble (evalImp); break;
    default: throw new RuntimeException ("Unsupported expression result type: " + type);
    } // switch
    
    return (resultValue);

  } // evaluateToDouble

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (JEPEmulationParser.class);

    List<ExpressionTest> testList = new ArrayList<>();

    double x = 3;
    double y = 7;
    double z = 11;
    double a = 0.5;
    String[] variables = new String[] {"x", "y", "z", "a"};
    JEPEmulationParser parser = new JEPEmulationParser();
    double[] values = new double[] {x, y, z, a};
    testList.add (new ExpressionTest ("x ^ y", parser, variables, values, Math.pow (x, y)));
    testList.add (new ExpressionTest ("!x", parser, variables, values, 0));
    testList.add (new ExpressionTest ("+x", parser, variables, values, x));
    testList.add (new ExpressionTest ("-x", parser, variables, values, -x));
    testList.add (new ExpressionTest ("y % x", parser, variables, values, y % x));
    testList.add (new ExpressionTest ("y / x", parser, variables, values, y / x));
    testList.add (new ExpressionTest ("x * y", parser, variables, values, x*y));
    testList.add (new ExpressionTest ("x + y", parser, variables, values, x+y));
    testList.add (new ExpressionTest ("x + y * z", parser, variables, values, x + y*z));
    testList.add (new ExpressionTest ("x * y + z", parser, variables, values, x*y + z));
    testList.add (new ExpressionTest ("x - y", parser, variables, values, x-y));
    testList.add (new ExpressionTest ("x <= y", parser, variables, values, 1));
    testList.add (new ExpressionTest ("x >= y", parser, variables, values, 0));
    testList.add (new ExpressionTest ("x < y", parser, variables, values, 1));
    testList.add (new ExpressionTest ("x > y", parser, variables, values, 0));
    testList.add (new ExpressionTest ("x != y", parser, variables, values, 1));
    testList.add (new ExpressionTest ("x == y", parser, variables, values, 0));
    testList.add (new ExpressionTest ("x > y && y < z", parser, variables, values, 0));
    testList.add (new ExpressionTest ("x > y || y < z", parser, variables, values, 1));
    testList.add (new ExpressionTest ("sin (a)", parser, variables, values, Math.sin (a)));
    testList.add (new ExpressionTest ("cos (a)", parser, variables, values, Math.cos (a)));
    testList.add (new ExpressionTest ("tan (a)", parser, variables, values, Math.tan (a)));
    testList.add (new ExpressionTest ("asin (a)", parser, variables, values, Math.asin (a)));
    testList.add (new ExpressionTest ("acos (a)", parser, variables, values, Math.acos (a)));
    testList.add (new ExpressionTest ("atan (a)", parser, variables, values, Math.atan (a)));
    testList.add (new ExpressionTest ("asinh (x)", parser, variables, values, Math.log(x + Math.sqrt(x*x + 1.0))));
    testList.add (new ExpressionTest ("acosh (x)", parser, variables, values, Math.log(x + Math.sqrt(x*x - 1.0))));
    testList.add (new ExpressionTest ("atanh (x)", parser, variables, values, 0.5*Math.log( (x + 1.0) / (x - 1.0) )));
    testList.add (new ExpressionTest ("ln (x)", parser, variables, values, Math.log (x)));
    testList.add (new ExpressionTest ("log (x)", parser, variables, values, Math.log10 (x)));
    testList.add (new ExpressionTest ("angle (y, x)", parser, variables, values, Math.atan2 (y, x)));
    testList.add (new ExpressionTest ("abs (-x)", parser, variables, values, Math.abs (-x)));
    testList.add (new ExpressionTest ("rand()", parser, variables, values, 0).not());
    testList.add (new ExpressionTest ("mod (y, x)", parser, variables, values, y % x));
    testList.add (new ExpressionTest ("sqrt (x)", parser, variables, values, Math.sqrt (x)));
    testList.add (new ExpressionTest ("sum (x, y, z)", parser, variables, values, x+y+z));
    testList.add (new ExpressionTest ("select (x < y, z, a)", parser, variables, values, (x < y ? z : a)));
    testList.add (new ExpressionTest ("hex (\"0xffac\")", parser, variables, values, 0xffac));
    testList.add (new ExpressionTest ("mask (x, y, z)", parser, variables, values, ((int)y & (int)z) == 0 ? x : Double.NaN));
    testList.add (new ExpressionTest ("mask (x, y, 8)", parser, variables, values, ((int)y & (int)8) == 0 ? x : Double.NaN));
    testList.add (new ExpressionTest ("and (x, y)", parser, variables, values, ((int)x & (int)y)));
    testList.add (new ExpressionTest ("or (x, y)", parser, variables, values, ((int)x | (int)y)));
    testList.add (new ExpressionTest ("xor (x, y)", parser, variables, values, ((int)x ^ (int)y)));
    testList.add (new ExpressionTest ("not (x)", parser, variables, values, ~((int)x)));
    testList.add (new ExpressionTest ("e", parser, variables, values, Math.E));
    testList.add (new ExpressionTest ("pi", parser, variables, values, Math.PI));
    testList.add (new ExpressionTest ("nan", parser, variables, values, Double.NaN));

    testList.forEach (test -> {
      logger.test ("parse \"" + test + "\"");
      test.run();
      assert (test.isCorrect());
      logger.passed();
    });

  } // main

  ////////////////////////////////////////////////////////////

} // JEPEmulationParser class

////////////////////////////////////////////////////////////////////////
