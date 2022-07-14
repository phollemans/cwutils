
////////////////////////////////////////////////////////////////////////
/*

     File: JEPParser.java
   Author: Peter Hollemans
     Date: 2017/11/07

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
import java.util.Stack;
import java.util.List;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import org.nfunk.jep.JEP;
import org.nfunk.jep.function.PostfixMathCommand;

import org.nfunk.jep.Node;
import org.nfunk.jep.ParserVisitor;
import org.nfunk.jep.ASTConstant;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.ASTStart;
import org.nfunk.jep.ASTVarNode;
import org.nfunk.jep.SimpleNode;

import noaa.coastwatch.util.expression.ExpressionParser;
import noaa.coastwatch.util.expression.ParseImp;
import noaa.coastwatch.util.expression.EvaluateImp;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * <p>The <code>JEPParser</code> class parses expressions using the syntax of the
 * Java Math Expression Parser (JEP) from http://singularsys.com/jep (we use
 * version 2.24 which is no longer supported since the product is now commercial).
 * This is the syntax that has been used by the CoastWatch Utilities
 * since 2003 and documented in the {@link noaa.coastwatch.tools.cwmath} tool.
 * Previous to this class existing, code would create a JEP instance and
 * use it directly for expression parsing.</p>
 *
 * <p>Note that this class is not thread-safe.</p>
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
@noaa.coastwatch.test.Testable
public class JEPParser implements ExpressionParser {

  // Variables
  // ---------

  /** The parse implementation to be used for variable indices and types. */
  private ParseImp parseImp;

  /** The JEP parser itself. */
  private JEP jepObj;

  /** The list of variables names in the expression. */
  private List<String> variableList;

  /** The array of variable types. */
  private Map<Integer,ResultType> typeMap;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the JEP object used for parsing.  This may be useful for classes
   * that need direct access to the parser.  The parser is only available after
   * the call to {@link #init}.
   *
   * @return the JEP parser.
   */
  public JEP getJEPObject () { return (jepObj); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isThreadSafe() { return (false); }

  ////////////////////////////////////////////////////////////

  @Override
  public void init (ParseImp parseImp) {

    this.parseImp = parseImp;

    // Set up JEP object
    // -----------------
    jepObj = new JEP();
    jepObj.setImplicitMul (true);
    jepObj.addStandardFunctions();
    jepObj.addStandardConstants();
    jepObj.setAllowUndeclared (true);
    jepObj.addFunction ("select", new Select());
    jepObj.addFunction ("hex", new Hex());
    jepObj.addFunction ("mask", new Mask());
    jepObj.addFunction ("and", new And());
    jepObj.addFunction ("or", new Or());
    jepObj.addFunction ("xor", new Xor());
    jepObj.addFunction ("not", new Not());
    jepObj.addVariable ("nan", Double.NaN);

  } // init

  ////////////////////////////////////////////////////////////

  @Override
  public void parse (String expr) {

    // Parse the expression
    // --------------------
    jepObj.parseExpression (expr);
    if (jepObj.hasError()) {
      throw new RuntimeException (jepObj.getErrorInfo());
    } // if

    // Get variables in expression
    // ---------------------------
    Hashtable symbols = jepObj.getSymbolTable();
    variableList = new ArrayList<String> ((Set<String>) symbols.keySet());
    variableList.remove ("e");
    variableList.remove ("pi");
    variableList.remove ("nan");

    // Check variable list
    // -------------------
    typeMap = new HashMap<>();
    variableList.forEach (varName -> {

      // Check index
      // -----------
      int index = parseImp.indexOfVariable (varName);
      if (index == -1) {
        throw new RuntimeException ("Invalid index found for variable " + varName);
      } // if

      // Check type
      // ----------
      String type = parseImp.typeOfVariable (varName);
      ResultType typeEnum;
      if (type == null) {
        throw new RuntimeException ("Unknown type for variable " + varName);
      } // if
      else if (type.equals ("Byte"))
        typeEnum = ResultType.BYTE;
      else if (type.equals ("Short"))
        typeEnum = ResultType.SHORT;
      else if (type.equals ("Integer"))
        typeEnum = ResultType.INT;
      else if (type.equals ("Long"))
        typeEnum = ResultType.LONG;
      else if (type.equals ("Float"))
        typeEnum = ResultType.FLOAT;
      else if (type.equals ("Double"))
        typeEnum = ResultType.DOUBLE;
      else
        throw new RuntimeException ("Unsupported type '" + type + "' for variable " + varName);
      typeMap.put (index, typeEnum);

    });

  } // parse

  ////////////////////////////////////////////////////////////

  /**
   * Builds a DOM tree by visiting the nodes in a JEP expression tree.
   * The elements will have the names constant, operator, function, and
   * variable.
   */
  private static class ParseTreeBuilder implements ParserVisitor {
  
    @Override
    public Object visit (ASTConstant node, Object data) {
    
      Document document = (Document) data;
      Element element = document.createElement ("constant");
      Object value = node.getValue();

      String valueStr;
      if (value instanceof String)
        valueStr = (String) value;
      else if (value instanceof Number) {
        Number numberValue = (Number) value;
        if (numberValue.doubleValue() == numberValue.longValue())
          valueStr = Long.toString (numberValue.longValue());
        else
          valueStr = numberValue.toString();
      } // else if
      else
        throw new RuntimeException ("Unknown constant type: " + value.getClass());
      element.setAttribute ("value", valueStr);

      return (element);
    
    } // visit
  
    @Override
    public Object visit (ASTFunNode node, Object data) {

      Document document = (Document) data;
      Element element;
      String name = node.getName();
      boolean isOperator = name.startsWith ("\"");

      if (isOperator) {
        element = document.createElement ("operator");
        element.setAttribute ("symbol", name.replaceAll ("\"", ""));
      } // if
      else {
        element = document.createElement ("function");
        element.setAttribute ("name", name);
      } // else

      int children = node.jjtGetNumChildren();
      for (int i = 0; i < children; i++) {
        element.appendChild ((Element) node.jjtGetChild (i).jjtAccept (this, data));
      } // for

      return (element);

    } // visit
  
    @Override
    public Object visit (ASTStart node, Object data) { throw new UnsupportedOperationException(); }
  
    @Override
    public Object visit (ASTVarNode node, Object data) {
    
      Document document = (Document) data;
      Element element = document.createElement ("variable");
      element.setAttribute ("name", node.getName());

      return (element);

    } // visit
  
    @Override
    public Object visit (SimpleNode node, Object data) { throw new UnsupportedOperationException(); }

  } // ParseTreeBuilder class

  ////////////////////////////////////////////////////////////

  @Override
  public Document getParseTree () {
  
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder loader;
    try { loader = factory.newDocumentBuilder(); }
    catch (ParserConfigurationException e) { throw new RuntimeException (e); }
    Document document = loader.newDocument();

    Node top = jepObj.getTopNode();
    Element root = document.createElement ("expression");
    document.appendChild (root);
    Element child = (Element) top.jjtAccept (new ParseTreeBuilder(), document);
    root.appendChild (child);
  
    return (document);
  
  } // getParseTree

  ////////////////////////////////////////////////////////////

  @Override
  public ResultType getResultType() { return (ResultType.DOUBLE); }

  ////////////////////////////////////////////////////////////

  @Override
  public List<String> getVariables() { return (variableList); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object evaluate (EvaluateImp evalImp) { return (evaluateToDouble (evalImp)); }

  ////////////////////////////////////////////////////////////

  @Override
  public double evaluateToDouble (EvaluateImp evalImp) {

    // Load variable values
    // --------------------
    boolean isInvalid = false;
    for (String varName : variableList) {
      int index = parseImp.indexOfVariable (varName);
      double value;
      switch (typeMap.get (index)) {
      case BYTE: value = evalImp.getByteProperty (index); break;
      case SHORT: value = evalImp.getShortProperty (index); break;
      case INT: value = evalImp.getIntegerProperty (index); break;
      case LONG: value = (double) evalImp.getLongProperty (index); break;
      case FLOAT: value = evalImp.getFloatProperty (index); break;
      case DOUBLE: value = evalImp.getDoubleProperty (index); break;
      default: throw new IllegalStateException();
      } // switch
      if (Double.isNaN (value)) {
        isInvalid = true;
        break;
      } // if
      jepObj.addVariable (varName, value);
    } // for

    // Compute expression value
    // ------------------------
    double output = (isInvalid ? Double.NaN : jepObj.getValue());

    return (output);

  } // evaluateToDouble

  ////////////////////////////////////////////////////////////

  /** Implements the Java ?: tertiary operator for JEP. */
  private static class Select extends PostfixMathCommand {

    public Select () { numberOfParameters = 3; }

    @Override
    public void run (Stack inStack) throws org.nfunk.jep.ParseException {

      // Check the stack
      // ---------------
      checkStack (inStack);
   
      // Get parameters
      // --------------
      Double condition, value1, value2;
      try {
        value2 = (Double) inStack.pop();
        value1 = (Double) inStack.pop();
        condition = (Double) inStack.pop();
      } // try
      catch (ClassCastException e) {
        throw new org.nfunk.jep.ParseException ("Invalid parameter type");
      } // catch

      // Perform selection
      // -----------------
      if (condition.doubleValue() == 0) inStack.push (value2);
      else inStack.push (value1);

    } // run
    
  } // Select class

  ////////////////////////////////////////////////////////////

  /** Implements a hexadecimal string decoder for JEP. */
  private static class Hex extends PostfixMathCommand {

    public Hex () { numberOfParameters = 1; }

    @Override
    public void run (Stack inStack) throws org.nfunk.jep.ParseException {

      // Check the stack
      // ---------------
      checkStack (inStack);
   
      // Get parameters
      // --------------
      String str;
      try {
        str = (String) inStack.pop();
      } // try
      catch (ClassCastException e) {
        throw new org.nfunk.jep.ParseException ("Invalid parameter type");
      } // catch

      // Perform hex decoding
      // --------------------
      Long val = Long.decode (str);
      inStack.push (val);

    } // run

  } // Hex class

  ////////////////////////////////////////////////////////////

  /** Implements a mask operation for JEP. */
  private static class Mask extends PostfixMathCommand {

    public Mask () { numberOfParameters = 3; }

    @Override
    public void run (Stack inStack) throws org.nfunk.jep.ParseException {

      // Check the stack
      // ---------------
      checkStack (inStack);
   
      // Get parameters
      // --------------
      Double value;
      Number maskData;
      Number maskValue;
      try {
        maskValue = (Number) inStack.pop();
        maskData = (Number) inStack.pop();
        value = (Double) inStack.pop();
      } // try
      catch (ClassCastException e) {
        throw new org.nfunk.jep.ParseException ("Invalid parameter type");
      } // catch

      // Perform masking
      // ---------------
      if ((maskData.longValue() & maskValue.longValue()) == 0)
        inStack.push (value);
      else
        inStack.push (Double.valueOf (Double.NaN));

    } // run

  } // Mask class

  ////////////////////////////////////////////////////////////

  /** Implements a bitwise or operation for JEP. */
  private static class Or extends PostfixMathCommand {

    public Or () { numberOfParameters = 2; }

    @Override
    public void run (Stack inStack) throws org.nfunk.jep.ParseException {

      // Check the stack
      // ---------------
      checkStack (inStack);
   
      // Get parameters
      // --------------
      Number value1, value2;
      try {
        value2 = (Number) inStack.pop();
        value1 = (Number) inStack.pop();
      } // try
      catch (ClassCastException e) {
        throw new org.nfunk.jep.ParseException ("Invalid parameter type");
      } // catch

      // Perform bitwise or
      // ------------------
      Long val = Long.valueOf (value1.longValue() | value2.longValue());
      inStack.push (val);

    } // run

  } // Or class

  ////////////////////////////////////////////////////////////

  /** Implements a bitwise and operation for JEP. */
  private static class And extends PostfixMathCommand {

    public And () { numberOfParameters = 2; }

    public void run (Stack inStack) throws org.nfunk.jep.ParseException {

      // Check the stack
      // ---------------
      checkStack (inStack);
   
      // Get parameters
      // --------------
      Number value1, value2;
      try {
        value2 = (Number) inStack.pop();
        value1 = (Number) inStack.pop();
      } // try
      catch (ClassCastException e) {
        throw new org.nfunk.jep.ParseException ("Invalid parameter type");
      } // catch

      // Perform bitwise and
      // ------------------
      Long val = Long.valueOf (value1.longValue() & value2.longValue());
      inStack.push (val);

    } // run

  } // And class

  ////////////////////////////////////////////////////////////

  /** Implements a bitwise xor operation for JEP. */
  private static class Xor extends PostfixMathCommand {

    public Xor () { numberOfParameters = 2; }

    @Override
    public void run (Stack inStack) throws org.nfunk.jep.ParseException {

      // Check the stack
      // ---------------
      checkStack (inStack);
   
      // Get parameters
      // --------------
      Number value1, value2;
      try {
        value2 = (Number) inStack.pop();
        value1 = (Number) inStack.pop();
      } // try
      catch (ClassCastException e) {
        throw new org.nfunk.jep.ParseException ("Invalid parameter type");
      } // catch

      // Perform bitwise xor
      // -------------------
      Long val = Long.valueOf (value1.longValue() ^ value2.longValue());
      inStack.push (val);

    } // run

  } // Xor class

  ////////////////////////////////////////////////////////////

  /** Implements a bitwise not operation for JEP. */
  private static class Not extends PostfixMathCommand {

    public Not () { numberOfParameters = 1; }

    @Override
    public void run (Stack inStack) throws org.nfunk.jep.ParseException {

      // Check the stack
      // ---------------
      checkStack (inStack);
   
      // Get parameters
      // --------------
      Number value1;
      try {
        value1 = (Number) inStack.pop();
      } // try
      catch (ClassCastException e) {
        throw new org.nfunk.jep.ParseException ("Invalid parameter type");
      } // catch

      // Perform bitwise not
      // -------------------
      Long val = Long.valueOf (~value1.longValue());
      inStack.push (val);

    } // run

  } // Not class

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (JEPParser.class);

    List<ExpressionTest> testList = new ArrayList<>();

    double x = 3;
    double y = 7;
    double z = 11;
    double a = 0.5;
    String[] variables = new String[] {"x", "y", "z", "a"};
    JEPParser parser = new JEPParser();
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

} // JEPParser class

////////////////////////////////////////////////////////////////////////
