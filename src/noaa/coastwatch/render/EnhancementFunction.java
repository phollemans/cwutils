////////////////////////////////////////////////////////////////////////
/*
     FILE: EnhancementFunction.java
  PURPOSE: A class to set up the functionality of data enhancement
           functions.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/21
  CHANGES: 2002/09/25, PFH, added getInverse
           2002/10/10, PFH, added reverse
           2002/10/22, PFH, changed class structure
           2003/09/13, PFH, moved Statistics out of DataVariable
           2004/02/17, PFH, added equals()
           2004/02/18, PFH, added Cloneable interface
           2004/02/19, PFH, added getRange()
           2005/02/04, PFH, added getReverse()

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.io.Serializable;
import java.util.Arrays;
import noaa.coastwatch.util.Function;
import noaa.coastwatch.util.Statistics;

/**
 * An enhancement function normalizes a data value to the range
 * [0..1].  The class is used in conjunction with a data enhancement.
 * For example, if a set of data is to be assigned colors based on the
 * data values, an enhancement function can be used to scale the data
 * values in the range [0..30] to the range [0..1] and then
 * multiplying by 255 would compute a byte value in the range
 * [0..255].  This would <i>color enhance</i> the data to a grayscale
 * color palette from black to white.<p>
 *
 * Note that since this class implements <code>Cloneable</code>, all
 * concrete child classes must have a valid <code>clone()</code>
 * method.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class EnhancementFunction 
  extends Function 
  implements Cloneable,java.io.Serializable {

  // Variables
  // ---------
  /** The enhancement range as [min, max]. */
  protected double[] range;

  /** The enhancement reverse flag. */
  protected boolean reverse;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the enhancement reversal flag.  When an enhancement is
   * reversed, the mapping is to [1..0] rather than [0..1].
   *
   * @see #setReverse
   */
  public boolean getReverse() { return (reverse); }

  ////////////////////////////////////////////////////////////

  /** Creates and returns a copy of this object. */
  public Object clone () {

    try {
      EnhancementFunction func = (EnhancementFunction) super.clone();
      func.range = (double[]) this.range.clone();
      return (func);
    } // try
    catch (CloneNotSupportedException e) {
      return (null);
    } // catch

  } // clone

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the enhancement reversal flag.  When an enhancement is
   * reversed, the mapping is to [1..0] rather than [0..1].
   */
  public void setReverse (boolean flag) { 
    
    reverse = flag;
    reset();

  } // setReverse

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the enhancement range.
   * 
   * @param range the enhancement range as [min, max].
   */
  public void setRange (
    double[] range
  ) { 

    this.range = (double[]) range.clone();
    reset();

  } // setRange

  ////////////////////////////////////////////////////////////

  /** 
   * Creates an enhancement function with the specified range.  By
   * default the reversal flag is false.
   * 
   * @param range the enhancement range as [min, max].
   */
  protected EnhancementFunction (
    double[] range
  ) { 

    this.range = (double[]) range.clone();
    reverse = false;
    reset();

  } // EnhancementFunction constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the inverse enhancement value.
   *
   * @param normValue the normalized data value.
   *
   * @return the unnormalized data value.
   */
  public abstract double getInverse (
    double normValue
  );

  ////////////////////////////////////////////////////////////

  /**
   * Gets the enhancement value.
   *
   * @param dataValue the data value to convert.   
   *
   * @return the normalized data value.
   */
  public double getValue (
    double dataValue
  ) {

    return (evaluate (new double[] {dataValue}));

  } // getValue

  ////////////////////////////////////////////////////////////

  /**
   * Sets the range based on statistical data.  The mean and standard
   * deviation are used to compute a normalized data value range.
   *
   * @param stats the data statistics.
   * @param units the number of standard deviation units above and
   * below the mean for the data range.
   * 
   * @see noaa.coastwatch.util.DataVariable#getStatistics
   */
  public void normalize (
    Statistics stats,
    double units
  ) {
 
    double mean = stats.getMean();
    double diff = stats.getStdev()*units;
    setRange (new double[] {mean - diff, mean + diff});

  } // normalize

  ////////////////////////////////////////////////////////////

  /**
   * Resets the enhancement function.  This method performs no
   * operation, but should be overridden by the subclass.
   */
  protected void reset () { }

  ////////////////////////////////////////////////////////////

  /** Indicates whether some other object is "equal to" this one. */
  public boolean equals (
    Object obj
  ) { 

    if (!(obj.getClass().equals (this.getClass()))) return (false);
    EnhancementFunction funcObj = (EnhancementFunction) obj;
    if (!Arrays.equals (funcObj.range, this.range)) return (false);
    if (funcObj.reverse != this.reverse) return (false);
    return (true);

  } // equals 

  ////////////////////////////////////////////////////////////

  /** Gets the enhancement range. */
  public double[] getRange () { return ((double[]) range.clone()); }

  ////////////////////////////////////////////////////////////

  /** Gets a description of the function. */
  public abstract String describe();

  ////////////////////////////////////////////////////////////

} // EnhancementFunction class

////////////////////////////////////////////////////////////////////////
