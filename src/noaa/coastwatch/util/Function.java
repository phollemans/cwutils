////////////////////////////////////////////////////////////////////////
/*
     FILE: Function.java
  PURPOSE: Interface to set the functionality of all function
           subclasses.
   AUTHOR: Peter Hollemans
     DATE: 2002/05/28
  CHANGES: 2003/11/22, PFH, fixed Javadoc comments

  CoastWatch Software Library and Utilities
  Copyright 1998-2003, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

/**
 * A function takes a number of variables as input and produces an
 * output value.  The function may be thought of as <code>f(X)</code>
 * where <code>X = [x<sub>1</sub>, x<sub>2</sub>,
 * ... x<sub>n</sub>]</code>.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class Function 
  implements Encodable {

  ////////////////////////////////////////////////////////////

  /**
   * Evalutes a function value with the specified inputs.
   *
   * @param variables the array of input variable values.
   *
   * @return the evaluated function value.  If an error occurred during
   * evaluation, the return value is <code>Double.NaN</code>.
   */
  public abstract double evaluate (
    double[] variables
  ); // evaluate

  ////////////////////////////////////////////////////////////

  /**
   * Gets this object encoding.  Override this function in the
   * subclass for actual object encoding.  This function simply
   * returns the object.
   */
  public Object getEncoding () { return (this); }

  ////////////////////////////////////////////////////////////

  /**
   * Uses the encoding to setup an instance of this object.  Override
   * this function in the subclass for actual object decoding.  This
   * function has no effect.
   */
  public void useEncoding (Object obj) { }

  ////////////////////////////////////////////////////////////

} // Function class

////////////////////////////////////////////////////////////////////////
