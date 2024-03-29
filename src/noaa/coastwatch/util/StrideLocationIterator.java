////////////////////////////////////////////////////////////////////////
/*

     File: StrideLocationIterator.java
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

// Imports
// -------
import java.util.NoSuchElementException;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataLocationIterator;

/**
 * The <code>StrideLocationIterator</code> class iterates over data
 * locations using a constant step stride vector.  See {@link
 * noaa.coastwatch.util.DataLocation#increment(int[],int[])} for
 * details on how the locations are incremented with a stride.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class StrideLocationIterator
  implements DataLocationIterator {

  // Variables
  // ---------

  /** The starting data location. */
  private DataLocation start;

  /** The ending data location. */
  private DataLocation end;

  /** The data location stride. */
  private int[] stride;

  /** The current data location. */
  private DataLocation current;

  /** The next valid flag, indicating that there are more valid locations. */
  private boolean nextValid;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new stride location iterator.
   * 
   * @param start the starting data location.
   * @param end the ending data location.
   * @param stride the data location stride in each dimension.
   */
  public StrideLocationIterator (
    DataLocation start,
    DataLocation end,
    int[] stride
  ) {

    this.start = (DataLocation) start.clone();
    this.end = (DataLocation) end.clone();
    this.stride = (int[]) stride.clone();
    current = (DataLocation) start.clone();
    nextValid = true;

  } // StrideLocationIterator

  ////////////////////////////////////////////////////////////

  @Override
  public boolean hasNext () { return (nextValid); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataLocation nextLocation (DataLocation loc) {

    // Check for valid
    // ---------------
    if (!nextValid) throw new NoSuchElementException();

    // Copy current location
    // ---------------------
    if (loc == null) loc = (DataLocation) current.clone();
    else loc.setCoords (current);

    // Increment to next location
    // --------------------------
    nextValid = current.increment (stride, start, end);

    return (loc);

  } // nextLocation

  ////////////////////////////////////////////////////////////

  @Override
  public void remove () { throw new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

  @Override
  public void reset () {

    current.setCoords (start);
    nextValid = true;

  } // reset

  ////////////////////////////////////////////////////////////

  @Override
  public DataLocation next () { return (nextLocation (null)); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    DataLocation start = new DataLocation (Double.parseDouble (argv[0]), 
      Double.parseDouble (argv[1]));
    DataLocation end = new DataLocation (Double.parseDouble (argv[2]), 
      Double.parseDouble (argv[3]));
    int[] stride = new int[] {Integer.parseInt (argv[4]), 
      Integer.parseInt (argv[5])};
    StrideLocationIterator iter = new StrideLocationIterator (start, end, 
      stride);
    while (iter.hasNext())
      System.out.println (iter.next());

  } // main

  ////////////////////////////////////////////////////////////

} // StrideLocationIterator class

////////////////////////////////////////////////////////////////////////
