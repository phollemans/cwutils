////////////////////////////////////////////////////////////////////////
/*

     File: DataVariableIterator.java
   Author: Peter Hollemans
     Date: 2004/03/27

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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

  @Override
  public boolean hasNext () { return (locationIter.hasNext()); }

  ////////////////////////////////////////////////////////////

  @Override
  public double nextDouble () {

    if (currentLocation == null)
      currentLocation = locationIter.nextLocation (null);
    else
      locationIter.nextLocation (currentLocation);
    double val = variable.getValue (currentLocation);
    return (val);

  } // nextDouble

  ////////////////////////////////////////////////////////////

  @Override
  public void remove () { throw new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

  @Override
  public void reset () {

    locationIter.reset();
    currentLocation = null;

  } // reset

  ////////////////////////////////////////////////////////////

  @Override
  public Double next () { return (Double.valueOf (nextDouble())); }

  ////////////////////////////////////////////////////////////

} // DataVariableIterator class

////////////////////////////////////////////////////////////////////////
