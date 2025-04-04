////////////////////////////////////////////////////////////////////////
/*

     File: ConstrainedStrideLocationIterator.java
   Author: Peter Hollemans
     Date: 2004/04/07

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
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.NoSuchElementException;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataLocationIterator;

/**
 * The <code>ConstrainedStrideLocationIterator</code> class iterates
 * over data locations using a constant step stride vector, but uses a
 * <code>java.awt.Shape</code> object to determine if each location is
 * contained within a shape boundary.  See {@link
 * noaa.coastwatch.util.DataLocation#increment(int[],int[])} for
 * details on how the locations are incremented with a stride.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ConstrainedStrideLocationIterator
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

  /** The constraining shape. */
  private Shape shape;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new constrained stride location iterator.
   * 
   * @param shape the shape to use for constraining the iterator
   * data locations.
   * @param stride the data location stride in each dimension.
   */
  public ConstrainedStrideLocationIterator (
    Shape shape,
    int[] stride
  ) {

    Rectangle2D bounds = shape.getBounds2D();
    this.start = new DataLocation (bounds.getMinX(), bounds.getMinY());
    this.end = new DataLocation (bounds.getMaxX(), bounds.getMaxY());
    this.stride = (int[]) stride.clone();
    this.shape = shape;
    current = new DataLocation (2);
    setFirst();

  } // ConstrainedStrideLocationIterator

  ////////////////////////////////////////////////////////////

  @Override
  public boolean hasNext () { return (nextValid); }

  ////////////////////////////////////////////////////////////

  /** Resets the location to the first valid location. */
  private void setFirst () {

    current.setCoords (start);
    Point2D point = new Point2D.Double (current.get(0), current.get(1));
    if (!shape.contains (point)) setNext();
    else nextValid = true;

  } // setFirst

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the next location.  The next location may be invalid, in
   * which case the valid flag will be set to false. 
   */
  private void setNext () {

    Point2D point = new Point2D.Double();
    while (nextValid = current.increment (stride, start, end)) {
      point.setLocation (current.get(0), current.get(1));
      if (shape.contains (point)) break;
    } // while

  } // setNext

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
    setNext();

    return (loc);

  } // nextLocation

  ////////////////////////////////////////////////////////////

  @Override
  public void remove () { throw new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

  @Override
  public void reset () {

    setFirst();

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

    Point2D.Float start = new Point2D.Float (Float.parseFloat (argv[0]), 
      Float.parseFloat (argv[1]));
    Point2D.Float end = new Point2D.Float (Float.parseFloat (argv[2]), 
      Float.parseFloat (argv[3]));
    int[] stride = new int[] {Integer.parseInt (argv[4]), 
      Integer.parseInt (argv[5])};

    GeneralPath path = new GeneralPath();
    path.moveTo ((start.x + end.x)/2, start.y);
    path.lineTo (end.x, (start.y + end.y)/2);
    path.lineTo ((start.x + end.x)/2, end.y);
    path.lineTo (start.x, (start.y + end.y)/2);
    path.closePath();

    ConstrainedStrideLocationIterator iter = 
      new ConstrainedStrideLocationIterator (path, stride);
    while (iter.hasNext())
      System.out.println (iter.next());

  } // main

  ////////////////////////////////////////////////////////////

} // ConstrainedStrideLocationIterator class

////////////////////////////////////////////////////////////////////////
