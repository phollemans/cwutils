////////////////////////////////////////////////////////////////////////
/*

     File: DataLocationConstraints.java
   Author: Peter Hollemans
     Date: 2017/04/03

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
import java.awt.Point;
import java.awt.Rectangle;
import noaa.coastwatch.util.DataLocation;

/**
 * The <code>DataLocationConstraints</code> class holds a set of values
 * used to specify the bounds and sparseness of a continguous area of data 
 * locations.  The main purpose is to pass to the 
 * {@link DataLocationIteratorFactory} to create various types of iterators 
 * over locations within the area.  The class also has static methods useful
 * with polygons shapes.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class DataLocationConstraints {

  // Variables
  // ---------
  
  /** The full data location dimensions. */
  public int[] dims;
  
  /** 
   * The data location start and end bounds.  These can be specified as an
   * alternative to using the full bounds specified in dims.  If either
   * start or end is not specified, the bounds are taken from dims.
   */
  public DataLocation start, end;
  
  /** 
   * The polygon shape surrounding the locations.  The polygon can be 
   * specified as an alternative to the start and end bounds.  The (x,y) 
   * coordinate values of each point in the polygon are taken to be the
   * data location (row, column) values.
   */
  public Shape polygon;
  
  /** 
   * The data coverage fraction in the range [0..1].  This is used to compute
   * the stride if stride is not specified.  If neither the data coverage
   * fraction or the stride are specified, the start and end values are
   * taken to be endpoints of a 1D line rather than an area.
   */
  public double fraction;
  
  /** 
   * The data coverage minimum number of data locations.  This is used to
   * compute the stride in combination with the fraction.
   */
  public int minCount;

  /**
   * The data location stride in each data dimension.  If specified, this
   * stride overrides anything specified by the fraction and minCount.
   */
  public int[] stride;
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the rectangular bounds of the specified shape in data location 
   * coordinates.
   * 
   * @param shape the shape to get the bounds for.
   *
   * @return the bounds as [min, max] data locations or null if the shape
   * has no area.
   */
  public static DataLocation[] getShapeBounds (
    Shape shape
  ) {
  
    DataLocation[] bounds = null;
  
    Rectangle rect = shape.getBounds();
    if (rect.width != 0 || rect.height != 0) {
      int minX = rect.x;
      int minY = rect.y;
      int maxX = rect.x + rect.width - 1;
      int maxY = rect.y + rect.height - 1;
      DataLocation start = new DataLocation (minX, minY);
      DataLocation end = new DataLocation (maxX, maxY);
      bounds = new DataLocation[] {start, end};
    } // if

    return (bounds);

  } // getShapeBounds

  ////////////////////////////////////////////////////////////

  /**
   * Gets the area of a shape.
   *
   * @param shape the shape to get the area for.
   *
   * @return the shape area as a fractional value in the range [0..1] where 
   * 1 means that the shape fills up the entire rectangle enclosed by its
   * bounds.
   */
  public static double getShapeArea (
    Shape shape
  ) {
  
    Rectangle rect = shape.getBounds();
    int span = Math.min (rect.width, rect.height);
    int increment = (int) Math.round (Math.max (span / 20.0, 1));
    int totalPoints = 0;
    int sampledPoints = 0;
    Point point = new Point();
    int minX = rect.x;
    int minY = rect.y;
    int maxX = rect.x + rect.width - 1;
    int maxY = rect.y + rect.height - 1;
    for (point.x = minX; point.x < maxX; point.x += increment) {
      for (point.y = minY; point.y < maxY; point.y += increment) {
        totalPoints++;
        if (shape.contains (point)) sampledPoints++;
      } // for
    } // for
    double polygonArea = ((double) sampledPoints) / totalPoints;

    return (polygonArea);

  } // getShapeArea

  ////////////////////////////////////////////////////////////

} // DataLocationConstraints class

////////////////////////////////////////////////////////////////////////
