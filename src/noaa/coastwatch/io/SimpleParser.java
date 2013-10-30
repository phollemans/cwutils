////////////////////////////////////////////////////////////////////////
/*
     FILE: SimpleParser.java
  PURPOSE: A class to perform simple text file parsing.
   AUTHOR: Peter Hollemans
     DATE: 2002/10/11
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;

/**
 * The simple parser class is a stream tokenizer with various methods
 * for simplifed parsing of text input streams.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class SimpleParser
  extends StreamTokenizer {

  ////////////////////////////////////////////////////////////

  /**
   * Gets a word from the tokenizer.
   *
   * @return the word token.
   *
   * @throws IOException if a word was not found at the next token.
   */
  public String getWord () throws IOException {

    if (this.nextToken() != StreamTokenizer.TT_WORD)
      throw new IOException ("Expected word at line " + this.lineno());
    return (this.sval);

  } // getWord

  ////////////////////////////////////////////////////////////

  /**
   * Gets a string from the tokenizer.
   *
   * @return the string token.
   *
   * @throws IOException if a string was not found at the next token.
   */
  public String getString () throws IOException {

    if (this.nextToken() != '"')
      throw new IOException ("Expected string at line " + this.lineno());
    return (this.sval);

  } // getString

  ////////////////////////////////////////////////////////////

  /**
   * Gets a number from the tokenizer.
   *
   * @return the number token.
   *
   * @throws IOException if a number was not found at the next token.
   */
  public double getNumber () throws IOException {

    if (this.nextToken() != StreamTokenizer.TT_NUMBER)
      throw new IOException ("Expected number at line " + this.lineno());
    return (this.nval);

  } // getNumber

  ////////////////////////////////////////////////////////////

  /**
   * Gets a key word from the tokenizer.  The key/value pair equals sign
   * is also parsed.
   *
   * @param expected the expected key or null of no specific key is
   * expected.
   *
   * @return the word token.
   *
   * @throws IOException if no key was found or the the expected key
   * was not found at the next token.
   */
  public String getKey (
    String expected
  ) throws IOException {

    // Read key word
    // -------------
    if (this.nextToken() != StreamTokenizer.TT_WORD) {
      if (expected == null)    
        throw new IOException ("Expected key/value pair at line " + 
          this.lineno());
      else
        throw new IOException ("Expected key '" + expected + "' at line " + 
          this.lineno());
    } // if
    String key = this.sval;
    if (expected != null && !key.equals (expected))
      throw new IOException ("Expected key '" + expected + "' at line " + 
        this.lineno());

    // Read '=' sign
    // -------------
    if (this.nextToken() != '=')
      throw new IOException ("Expected '=' after key at line " + 
        this.lineno());

    return (key);

  } // getKey

  ////////////////////////////////////////////////////////////

  /** 
   * Checks for the end-of-file token.
   * 
   * @return true if the next token is the end-of-file, or false
   * otherwise.  The state of the parser is not changed.
   *
   * @throws IOException if an error occurred reading the input stream.
   */
  public boolean eof () throws IOException {

    boolean eof = (this.nextToken() == StreamTokenizer.TT_EOF);
    this.pushBack();
    return (eof);

  } // eof

  ////////////////////////////////////////////////////////////

  /**
   * Creates a simple parser from a character stream.
   *
   * @param reader the reader object providing the input stream.
   */
  public SimpleParser (
    Reader reader
  ) {

    super (reader);

  } // SimpleParser

  ////////////////////////////////////////////////////////////

} // SimpleParser class

////////////////////////////////////////////////////////////////////////
