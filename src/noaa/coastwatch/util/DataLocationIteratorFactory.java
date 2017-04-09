////////////////////////////////////////////////////////////////////////
/*

     File: DataLocationIteratorFactory.java
   Author: Peter Hollemans
     Date: 2017/03/31

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
import java.awt.Rectangle;
import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.DataLocationIterator;
import noaa.coastwatch.util.StrideLocationIterator;
import noaa.coastwatch.util.ConstrainedStrideLocationIterator;
import noaa.coastwatch.util.LineLocationIterator;

// Testing
import noaa.coastwatch.test.TestLogger;
import java.awt.Polygon;

/**
 * The <code>DataLocationIteratorFactory</code> class creates a 
 * {@link DataLocationIterator} instance based on a set of parameters that
 * specify the desired iterator behaviour.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class DataLocationIteratorFactory {

  // Variables
  // ---------

  /** The instance of this factory. */
  private static DataLocationIteratorFactory instance;

  ////////////////////////////////////////////////////////////

  /** Gets an instance of this factory. */
  public static DataLocationIteratorFactory getInstance () {

    if (instance == null) instance = new DataLocationIteratorFactory();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  private DataLocationIteratorFactory () { }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the optimal data iteration stride for the specified dimensions
   * based on a desired fraction and minimum number of values.
   *
   * @param start the starting data location.
   * @param end the ending data location.
   * @param fraction the iterator value count as a fraction of the total number
   * of values, in the range [0..1].
   * @param minCount the minimum desired number of values, or 0 to have no
   * minimum count.
   *
   * @return the optimal stride vector.  The number of values iterated over 
   * using the optimal stride is guaranteed to be bounded below by the
   * fraction value or the minimum count, whichever is greater.
   */
  private int[] getOptimalStride (
    DataLocation start,
    DataLocation end,
    double fraction,
    int minCount
  ) {

    // Calculate proposed stride
    // -------------------------
    int rank = start.getRank();
    int stride = (int) Math.floor (Math.pow (1/fraction, 1.0/rank));

    // Calculate actual number of sampled values
    // -----------------------------------------
    int sampleCount = 1;
    for (int i = 0; i < rank; i++) {
      sampleCount *= 1 + (int) Math.floor (Math.abs (start.get(i) - end.get(i))) / stride;
    } // for

    // Adjust stride if sampled values too low
    // ---------------------------------------
    if (sampleCount < minCount) {
      int totalCount = 1;
      for (int i = 0; i < rank; i++) {
        totalCount *= 1 + (int) Math.floor (Math.abs (start.get(i) - end.get(i)));
      } // for
      double newFraction = ((double) minCount) / totalCount;
      stride = (int) Math.floor (Math.pow (1/newFraction, 1.0/rank));
      if (stride < 1) stride = 1;
    } // if

    // Create stride array
    // -------------------
    int[] strideArray = new int[rank];
    Arrays.fill (strideArray, stride);

    return (strideArray);

  } // getOptimalStride

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new iterator.
   *
   * @param constraints the data location bounds and sparseness contraints.
   *
   * @return the data iterator.  The number of values iterated over is
   * determined by the constraint values.
   *
   * @throws IllegalArgumentException if inconsistencies are found in the
   * constraints.
   */
  public DataLocationIterator create (
    DataLocationConstraints constraints
  ) {

    int[] dims = constraints.dims;
    DataLocation start, end;
    double fraction = constraints.fraction;
    int minCount = constraints.minCount;

    // Get bounds from polygon
    // -----------------------
    Shape polygon = constraints.polygon;
    if (polygon != null) {

      // Check for fraction or stride
      // ----------------------------
      if (fraction == 0 && constraints.stride == null)
        throw new IllegalArgumentException ("No coverage or stride specified for polygon");

      // Check bounds
      // ------------
      DataLocation[] bounds = DataLocationConstraints.getShapeBounds (polygon);
      if (bounds == null)
        throw new IllegalArgumentException ("Polygon has zero area");
      start = bounds[0];
      end = bounds[1];

      // Modify fraction and min count for polygon
      // -----------------------------------------
      if (fraction != 0) {
        double polygonArea = DataLocationConstraints.getShapeArea (polygon);
        if (polygonArea != 0) {
          fraction = Math.min (fraction / polygonArea, 1);
          minCount = (int) (minCount / polygonArea);
        } // if
      } // if

    } // if

    // Get bounds from dims/start/end
    // ------------------------------
    else {

      // Check dims
      // ----------
      if ((constraints.start == null || constraints.start == null) && dims == null)
        throw new IllegalArgumentException ("No dimensions specified");

      // Get start
      // ---------
      start = constraints.start;
      if (start == null)
        start = new DataLocation (dims.length);
      if (!start.isValid())
        throw new IllegalArgumentException ("Starting data location is invalid");

      // Get end
      // -------
      end = constraints.end;
      if (end == null) {
        end = new DataLocation(dims.length);
        for (int i = 0; i < dims.length; i++) end.set (i, dims[i]-1);
      } // if
      if (!end.isValid())
        throw new IllegalArgumentException ("Ending data location is invalid");

    } // else

    // Get stride
    // ----------
    int[] stride = constraints.stride;
    boolean isLine = false;
    if (stride == null) {
    
      // Check for a line specification
      // ------------------------------
      if (fraction == 0) isLine = true;
      
      // Compute stride
      // --------------
      if (!isLine) stride = getOptimalStride (start, end, fraction, minCount);
    
    } // if

    // Create iterator
    // ---------------
    DataLocationIterator iterator;
    if (isLine)
      iterator = new LineLocationIterator (start, end);
    else if (polygon != null)
      iterator = new ConstrainedStrideLocationIterator (polygon, stride);
    else
      iterator = new StrideLocationIterator (start, end, stride);

    return (iterator);

  } // create

  ////////////////////////////////////////////////////////////
  
  /**
   * Counts the number of elements in an iterator.
   *
   * @param iter the iterator to get elements from.
   *
   * @return the number of elements left in the iterator.
   */
  private static int elementsInIterator (
    Iterator iter
  ) {

    int count = 0;
    while (iter.hasNext()) {
      count++;
      iter.next();
    } // while
    return (count);

  } // elementsInIterator

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (DataLocationIteratorFactory.class);

    // ------------------------->

    logger.test ("getInstance");
    DataLocationIteratorFactory factory = DataLocationIteratorFactory.getInstance();
    assert (factory != null);
    logger.passed();

    // ------------------------->

    logger.test ("create");

    DataLocationConstraints lc = new DataLocationConstraints();
    lc.dims = new int[] {100, 100};
    DataLocationIterator iterator = factory.create (lc);
    assert (elementsInIterator (iterator) == 100);

    lc.start = new DataLocation (5, 10);
    iterator = factory.create (lc);
    assert (elementsInIterator (iterator) == (99-5+1));

    lc.end = new DataLocation (50, 50);
    iterator = factory.create (lc);
    assert (elementsInIterator (iterator) == (50-5+1));

    lc.fraction = 0.01;
    lc.start = new DataLocation (10, 10);
    iterator = factory.create (lc);
    assert (elementsInIterator (iterator) == (5*5));

    lc.minCount = 100;
    iterator = factory.create (lc);
    int elements = elementsInIterator (iterator);
    int maxElements = (50-10+1)*(50-10+1);
    assert (elements >= 100 && elements <= maxElements);

    Polygon polygon = new Polygon();
    polygon.addPoint (0, 0);
    polygon.addPoint (0, 99);
    polygon.addPoint (49, 49);
    lc.polygon = polygon;
    iterator = factory.create (lc);
    elements = elementsInIterator (iterator);
    maxElements = 100*100/4;
    assert (elements >= 100 && elements <= maxElements);

    logger.passed();

    // ------------------------->

  } // main
  
  ////////////////////////////////////////////////////////////

} // DataLocationIteratorFactory class

////////////////////////////////////////////////////////////////////////
