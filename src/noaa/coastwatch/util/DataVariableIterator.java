////////////////////////////////////////////////////////////////////////
/*
     FILE: DataVariableIterator.java
  PURPOSE: An iterator that returns values from a data variable.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/27
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

/**
 * The <code>DataVariableIterator</code> class iterates over the
 * locations of a data variable and returns the data values found.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class DataVariableIterator
  implements DataIterator {

  // Variables
  // ---------

  /** The data variable from which to obtain values. */
  private DataVariable variable;

  /** The location iterator from which to obtain location values. */
  private DataLocationIterator locationIter;

  /** The current data location. */
  private DataLocation currentLocation;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new iterator.
   * 
   * @param variable the data variable from which to obtain values.
   * @param locationIter the location iterator from which to obtain
   * location values.
   */
  public DataVariableIterator (
    DataVariable variable,
    DataLocationIterator locationIter
  ) {

    this.variable = variable;
    this.locationIter = locationIter;

  } // DataVariableIterator

  ////////////////////////////////////////////////////////////

  /** Returns true if there are more data variable values. */
  public boolean hasNext () { return (locationIter.hasNext()); }

  ////////////////////////////////////////////////////////////

  /** Gets the next data variable value. */
  public double nextDouble () {

    if (currentLocation == null)
      currentLocation = locationIter.nextLocation (null);
    else
      locationIter.nextLocation (currentLocation);
    double val = variable.getValue (currentLocation);
    return (val);

  } // nextDouble

  ////////////////////////////////////////////////////////////

  /** Throws an exception, since removal is not supported. */
  public void remove () { throw new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

  /** Resets back to the first data variable value. */
  public void reset () { 

    locationIter.reset();
    currentLocation = null;

  } // reset

  ////////////////////////////////////////////////////////////

  /** Gets the next data variable value as a <code>Double</code>. */
  public Object next () { return (new Double (nextDouble())); }

  ////////////////////////////////////////////////////////////

} // DataVariableIterator class

////////////////////////////////////////////////////////////////////////
