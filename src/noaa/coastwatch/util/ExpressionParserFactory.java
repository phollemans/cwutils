////////////////////////////////////////////////////////////////////////
/*
     FILE: ExpressionParserFactory.java
  PURPOSE: Creates standard expression parsers.
   AUTHOR: Peter Hollemans
     DATE: 2006/07/11
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * The <code>ExpressionParserFactory</code> class create standard
 * instances of a Java Math Expression Parser (JEP) with
 * additional functions added.  See the {@link
 * noaa.coastwatch.tools.cwmath} documentation for details on the
 * added functions.
 */
public class ExpressionParserFactory {

  ////////////////////////////////////////////////////////////

  /**
   * Gets an instance of an expression parser with extra
   * useful functions added.  The functions are:
   * <ul>
   *   <li>select - Acts like the C language ?: operator.</li>
   *   <li>hex - Hexadecimal string decoder.</li>
   *   <li>mask - Masks to NaN based on a data value and bitmask.</li>
   *   <li>and - Bitwise AND.</li>
   *   <li>or - Bitwise OR.</li>
   *   <li>xor - Bitwise XOR.</li>
   *   <li>not - Bitwise complement.</li>
   * </ul>
   * as well as the "nan" variable which signifies double valued
   * Not-a-Number.
   *
   * @return the expression parser.
   */
  public static JEP getInstance () {

    // Create expression parser
    // ------------------------
    JEP parser = new JEP();
    parser.setImplicitMul (true);
    parser.addStandardFunctions();
    parser.addStandardConstants();
    parser.setAllowUndeclared (true);
    parser.addFunction ("select", new Select());
    parser.addFunction ("hex", new Hex());
    parser.addFunction ("mask", new Mask());
    parser.addFunction ("and", new And());
    parser.addFunction ("or", new Or());
    parser.addFunction ("xor", new Xor());
    parser.addFunction ("not", new Not());
    parser.addVariable ("nan", Double.NaN);

    return (parser);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** 
   * The select class implements a JEP custom function to act as
   * the Java ?: operator.
   */
  private static class Select
    extends PostfixMathCommand {

    ////////////////////////////////////////////////////////

    /** Creates a new selection instance. */
    public Select () { numberOfParameters = 3; }    

    ////////////////////////////////////////////////////////

    /** Runs the selection function. */
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

    ////////////////////////////////////////////////////////

  } // Select class

  ////////////////////////////////////////////////////////////

  /** 
   * The hex class implements a JEP custom function to act as
   * a hex value decoder.
   */
  private static class Hex
    extends PostfixMathCommand {

    ////////////////////////////////////////////////////////

    /** Creates a new hex instance. */
    public Hex () { numberOfParameters = 1; }    

    ////////////////////////////////////////////////////////

    /** Runs the hex function. */
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

    ////////////////////////////////////////////////////////

  } // Hex class

  ////////////////////////////////////////////////////////////

  /** 
   * The mask class implements a JEP custom function to act as
   * a bitmask.
   */
  private static class Mask
    extends PostfixMathCommand {

    ////////////////////////////////////////////////////////

    /** Creates a new mask instance. */
    public Mask () { numberOfParameters = 3; }    

    ////////////////////////////////////////////////////////

    /** Runs the mask function. */
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
        inStack.push (new Double (Double.NaN));

    } // run

    ////////////////////////////////////////////////////////

  } // Mask class

  ////////////////////////////////////////////////////////////

  /** 
   * The or class implements a JEP custom function to act as
   * a bitwise or.
   */
  private static class Or
    extends PostfixMathCommand {

    ////////////////////////////////////////////////////////

    /** Creates a new or instance. */
    public Or () { numberOfParameters = 2; }    

    ////////////////////////////////////////////////////////

    /** Runs the or function. */
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
      Long val = new Long (value1.longValue() | value2.longValue());
      inStack.push (val);

    } // run

    ////////////////////////////////////////////////////////

  } // Or class

  ////////////////////////////////////////////////////////////

  /** 
   * The and class implements a JEP custom function to act as
   * a bitwise and.
   */
  private static class And
    extends PostfixMathCommand {

    ////////////////////////////////////////////////////////

    /** Creates a new and instance. */
    public And () { numberOfParameters = 2; }    

    ////////////////////////////////////////////////////////

    /** Runs the and function. */
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
      Long val = new Long (value1.longValue() & value2.longValue());
      inStack.push (val);

    } // run

    ////////////////////////////////////////////////////////

  } // And class

  ////////////////////////////////////////////////////////////

  /** 
   * The Xor class implements a JEP custom function to act as
   * a bitwise Xor.
   */
  private static class Xor
    extends PostfixMathCommand {

    ////////////////////////////////////////////////////////

    /** Creates a new and instance. */
    public Xor () { numberOfParameters = 2; }    

    ////////////////////////////////////////////////////////

    /** Runs the xor function. */
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
      Long val = new Long (value1.longValue() ^ value2.longValue());
      inStack.push (val);

    } // run

    ////////////////////////////////////////////////////////

  } // Xor class

  ////////////////////////////////////////////////////////////

  /** 
   * The Not class implements a JEP custom function to act as
   * a bitwise Not.
   */
  private static class Not
    extends PostfixMathCommand {

    ////////////////////////////////////////////////////////

    /** Creates a new and instance. */
    public Not () { numberOfParameters = 1; }    

    ////////////////////////////////////////////////////////

    /** Runs the xor function. */
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
      Long val = new Long (~value1.longValue());
      inStack.push (val);

    } // run

    ////////////////////////////////////////////////////////

  } // Not class

  ////////////////////////////////////////////////////////////

} // ExpressionParserFactory class

////////////////////////////////////////////////////////////////////////
