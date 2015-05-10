////////////////////////////////////////////////////////////////////////
/*
     FILE: LineIterator.java
  PURPOSE: Implements a resettable iterator for line drawing.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/26
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.awt.Point;
import java.util.NoSuchElementException;
import noaa.coastwatch.util.ResettableIterator;

/**
 * The <code>LineIterator</code> class may be used to get the set of
 * (x,y) points required for drawing a line between start and end
 * points.  For convenience, the iterator is resettable and has a
 * <code>nextPoint()</code> method to save allocating a new point each
 * time.  The line drawing algorithm was taken from:
 * <blockquote>
 *   http://www.gamedev.net/reference/articles/article1275.asp
 * </blockquote>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class LineIterator
  implements ResettableIterator {

  // Variables
  // ---------

  /** The current pixel number, starting at 0. */
  private int curpixel;

  /** The number of pixels in the line. */
  private int numpixels;

  /** The x and y increments. */
  private int xinc1, xinc2, yinc1, yinc2;

  /** The numerator and denominator. */
  private int num, numadd, den;

  /** The x and y difference. */
  private int deltax, deltay;

  /** The current point. */
  private Point currentPoint;

  /** The start point. */
  private Point startPoint;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new line iterator. 
   * 
   * @param start the line starting point.
   * @param end the line ending point.
   */
  public LineIterator (
    Point start,
    Point end
  ) {

    // Find x, y diffs and start
    // -------------------------
    deltax = Math.abs (end.x - start.x);
    deltay = Math.abs (end.y - start.y);
    startPoint = (Point) start.clone();
    currentPoint = new Point();

    // The x values are increasing/decreasing
    // --------------------------------------
    if (end.x >= start.x) {
      xinc1 = 1;
      xinc2 = 1;
    } // if
    else {
      xinc1 = -1;
      xinc2 = -1;
    } // else

    // The y values are increasing/decreasing
    // --------------------------------------
    if (end.y >= start.y) {
      yinc1 = 1;
      yinc2 = 1;
    } // if
    else {
      yinc1 = -1;
      yinc2 = -1;
    } // else

    // At least one x for every y
    // --------------------------
    if (deltax >= deltay) {
      xinc1 = 0;
      yinc2 = 0;
      den = deltax;
      numadd = deltay;
      numpixels = deltax;
    } // if

    // At least one y for every x
    // --------------------------
    else {
      xinc2 = 0;
      yinc1 = 0;
      den = deltay;
      numadd = deltax;
      numpixels = deltay;
    } // else

    reset();

  } // LineIterator constructor

  ////////////////////////////////////////////////////////////

  /** Resets the line to the starting point. */
  public void reset () {

    if (deltax >= deltay) num = deltax / 2;
    else num = deltay / 2;
    currentPoint.setLocation (startPoint);
    curpixel = 0;

  } // reset

  ////////////////////////////////////////////////////////////

  /**
   * Gets the next point in the line.
   *
   * @param point the point to fill in with coordinates, or null to
   * allocate a new point.
   *
   * @return the next point.  The point is allocated only if the
   * passed point is null, otherwise the same point is returned.
   */
  public Point nextPoint (
    Point point
  ) {

    // Check if we are at the end of the line
    // --------------------------------------
    if (curpixel > numpixels) throw new NoSuchElementException();

    // Set point value
    // ---------------
    if (point == null) point = new Point();
    point.setLocation (currentPoint);

    // Increment to next point
    // -----------------------
    num += numadd;
    if (num >= den) {
      num -= den;
      currentPoint.x += xinc1;
      currentPoint.y += yinc1;
    } /* if */
    currentPoint.x += xinc2;
    currentPoint.y += yinc2;
    curpixel++;

    return (point);

  } // next

  ////////////////////////////////////////////////////////////

  /** Returns true if the line has another point, or false if not. */
  public boolean hasNext() { return (curpixel <= numpixels); }

  ////////////////////////////////////////////////////////////

  /** Returns the next point in the line. */
  public Object next() { return (nextPoint (null)); }

  ////////////////////////////////////////////////////////////
  
  /** Throws an error as removal is not supported. */
  public void remove () { throw new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    Point start = new Point (Integer.parseInt (argv[0]), 
      Integer.parseInt (argv[1]));
    Point end = new Point (Integer.parseInt (argv[2]), 
      Integer.parseInt (argv[3]));
    LineIterator iter = new LineIterator (start, end);
    while (iter.hasNext())
      System.out.println (iter.next());

  } // main

  ////////////////////////////////////////////////////////////

} // LineIterator class

////////////////////////////////////////////////////////////////////////
