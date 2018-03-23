////////////////////////////////////////////////////////////////////////
/*

     File: ExpressionParserFactory.java
   Author: Peter Hollemans
     Date: 2006/07/11

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.expression.ExpressionParser;

/**
 * The <code>ExpressionParserFactory</code> class create standard
 * instances of a {@link ExpressionParser} that follow either the legacy
 * expression syntax or Java expression syntax.  See the {@link
 * noaa.coastwatch.tools.cwmath} documentation for details on the
 * legacy versus Java expression language features.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class ExpressionParserFactory {

  // Constants
  // ---------
  
  /** The various styles of parsers. */
  public enum ParserStyle {

    /** 
     * The pure legacy parser, using only JEP style syntax and evaluation.
     * Evaluation is done using JEP which has performance limitations and
     * is not thread-safe.
     */
    LEGACY,

    /** 
     * The emulated legacy parser, using JEP syntax translated into Java 
     * syntax with high speed Java bytecode evaluation.
     */
    LEGACY_EMULATED,

    /** 
     * The Java syntax parser, using ony Java language syntax and high speed
     * bytecode evaluation.
     */
    JAVA
    
  } // ParserStyle enum

  // Variables
  // ---------

  /** The singleton instance of this class. */
  private static ExpressionParserFactory instance = new ExpressionParserFactory();

  /** The default expression style. */
  private ParserStyle defaultStyle = ParserStyle.LEGACY;

  ////////////////////////////////////////////////////////////

  private ExpressionParserFactory () { }

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static ExpressionParserFactory getFactoryInstance() { return (instance); }

  ////////////////////////////////////////////////////////////

  @Deprecated
  public static org.nfunk.jep.JEP getInstance() {
    JEPParser parser = new JEPParser();
    parser.init (null);
    return (parser.getJEPObject());
  } // getInstance
  
  ////////////////////////////////////////////////////////////

  /**
   * Sets the default style of expression parser to create.
   * 
   * @param style the style of parser to create.
   */
  public void setDefaultStyle (ParserStyle style) { 
  
    this.defaultStyle = style;
  
  } // setDefaultStyle

  ////////////////////////////////////////////////////////////

  /** Creates a new expression parser in the default style. */
  public ExpressionParser create() {

    return (create (defaultStyle));
        
  } // create

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new expresion parser.
   * 
   * @param style the style of parser to create.
   * 
   * @return the parser configured with the specified style, or null if no
   * appropriate parser could be created.
   */
  public ExpressionParser create (ParserStyle style) {

    ExpressionParser parser;
    switch (style) {
    case LEGACY:
      parser = new JEPParser();
      break;
    case LEGACY_EMULATED:
      parser = new JEPEmulationParser();
      break;
    case JAVA:
      parser = new JELParser();
      break;
    default:
      parser = null;
    } // switch
    
    return (parser);
    
  } // create

  ////////////////////////////////////////////////////////////

} // ExpressionParserFactory class

////////////////////////////////////////////////////////////////////////
