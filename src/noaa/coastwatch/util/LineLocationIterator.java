////////////////////////////////////////////////////////////////////////
/*

     File: LineLocationIterator.java
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
import java.awt.Point;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataLocationIterator;
import noaa.coastwatch.util.LineIterator;

/**
 * The <code>LineLocationIterator</code> class iterates over data
 * locations along a line between start and end locations.  See {@link
 * noaa.coastwatch.util.LineIterator} for details on how the locations
 * are incremented along a line.  Currently, only 2D data locations
 * are supported.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class LineLocationIterator
  implements DataLocationIterator {

  // Variables
  // ---------

  /** The line iterator used for getting line locations. */
  private LineIterator iter;

  /** The current point. */
  private Point currentPoint;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new line location iterator.  The starting and ending
   * locations are rounded to the nearest integer coordinates prior to
   * deriving the line data locations.
   * 
   * @param start the starting data location.
   * @param end the ending data location.
   */
  public LineLocationIterator (
    DataLocation start,
    DataLocation end
  ) {

    // Create line iterator
    // --------------------
    Point startPoint = new Point (
      (int) Math.round (start.get(0)),
      (int) Math.round (start.get(1))
    );
    Point endPoint = new Point (
      (int) Math.round (end.get(0)),
      (int) Math.round (end.get(1))
    );
    iter = new LineIterator (startPoint, endPoint);

  } // LineLocationIterator

  ////////////////////////////////////////////////////////////

  /** Returns true if there are more data locations. */
  public boolean hasNext () { return (iter.hasNext()); }

  ////////////////////////////////////////////////////////////

  public DataLocation nextLocation (DataLocation loc) {

    // Get next point
    // --------------
    if (currentPoint == null)
      currentPoint = iter.nextPoint (null);
    else
      iter.nextPoint (currentPoint);

    // Copy current point
    // ------------------
    if (loc == null) loc = new DataLocation (2);
    loc.set (0, currentPoint.x);
    loc.set (1, currentPoint.y);

    return (loc);

  } // nextLocation

  ////////////////////////////////////////////////////////////

  /** Throws an exception, since removal is not supported. */
  public void remove () { throw new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

  /** Resets back to the first data variable value. */
  public void reset () { 

    iter.reset();
    currentPoint = null;

  } // reset

  ////////////////////////////////////////////////////////////

  /** Gets the next data location. */
  public Object next () { return (nextLocation (null)); }

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
    LineLocationIterator iter = new LineLocationIterator (start, end);
    while (iter.hasNext())
      System.out.println (iter.next());

  } // main

  ////////////////////////////////////////////////////////////

} // LineLocationIterator class

////////////////////////////////////////////////////////////////////////
