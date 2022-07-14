////////////////////////////////////////////////////////////////////////
/*

     File: EnhancementFunction.java
   Author: Peter Hollemans
     Date: 2002/07/21

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render;

// Imports
// -------
import java.io.Serializable;
import java.util.Arrays;
import noaa.coastwatch.util.Function;
import noaa.coastwatch.util.Statistics;

/**
 * <p>An enhancement function normalizes a data value to the range
 * [0..1].  The class is used in conjunction with a data enhancement.
 * For example, if a set of data is to be assigned colors based on the
 * data values, an enhancement function can be used to scale the data
 * values in the range [0..30] to the range [0..1] and then
 * multiplying by 255 would compute a byte value in the range
 * [0..255].  This would <i>color enhance</i> the data to a grayscale
 * color palette from black to white.</p>
 *
 * <p>Note that since this class implements <code>Cloneable</code>, all
 * concrete child classes must have a valid <code>clone()</code>
 * method.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class EnhancementFunction 
  extends Function 
  implements Cloneable, Serializable {

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
   * operation, but should be overridden by the subclass to set up
   * internal variables according to the new range values and reverse
   * flag.
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
