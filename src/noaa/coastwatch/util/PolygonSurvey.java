////////////////////////////////////////////////////////////////////////
/*
     FILE: PolygonSurvey.java
  PURPOSE: Data survey for a rectangle of data.
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

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;
import java.text.*;
import noaa.coastwatch.util.trans.*;

/**
 * The <code>PolygonSurvey</code> class holds survey information for a
 * polygon of data values.
 */
public class PolygonSurvey
  extends BoxSurvey {

  // Variables
  // ---------
  
  /** The polygon shape. */
  private Shape shape;

  /** The polygon area as a fraction of the total box area. */
  private double areaFraction;

  ////////////////////////////////////////////////////////////

  /** Gets the survey type for results reporting. */
  protected String getSurveyType () { return ("Polygon"); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the sample size (total values sampled divided by the total
   * values in the area) in percent. 
   */
  protected double getSampleSize () {

    DataLocation[] locs = getExtents();
    int totalCount =
      (1 + (int) Math.floor (Math.abs (locs[1].get(0) - locs[0].get(0)))) *
      (1 + (int) Math.floor (Math.abs (locs[1].get(1) - locs[0].get(1))));
    int count = getStatistics().getValues();
    double percent = (((double) count)/totalCount) * 100;
    if (areaFraction != 0) percent = percent / areaFraction;
    return (Math.min (percent, 100));

  } // getSampleSize

  ////////////////////////////////////////////////////////////

  /** Gets the shape defining this polygon. */
  public Shape getShape () { return (shape); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new polygon survey.
   * 
   * @param variable the survey variable.
   * @param trans the survey variable Earth transform.
   * @param shape the polygon shape.
   */
  public PolygonSurvey (
    DataVariable variable,
    EarthTransform trans,
    Shape shape
  ) { 

    // Initialize
    // ----------
    this.shape = shape;

    // Check survey bounds
    // -------------------
    Rectangle bounds = shape.getBounds();
    if (bounds.width == 0 && bounds.height == 0)
      throw new IllegalArgumentException ("Polygon has zero area");

    // Set start and end locations
    // ---------------------------
    int minX = bounds.x;
    int minY = bounds.y;
    int maxX = bounds.x + bounds.width - 1;
    int maxY = bounds.y + bounds.height - 1;
    DataLocation start = new DataLocation (minX, minY);
    DataLocation end = new DataLocation (maxX, maxY);

    // Compute area fraction
    // ---------------------
    int span = Math.min (bounds.width, bounds.height);
    int increment = (int) Math.round (Math.max (span / 20.0, 1));
    int totalPoints = 0;
    int sampledPoints = 0;
    Point point = new Point();
    for (point.x = minX; point.x < maxX; point.x += increment) {
      for (point.y = minY; point.y < maxY; point.y += increment) {
        totalPoints++;
        if (shape.contains (point)) sampledPoints++;
      } // for
    } // for
    areaFraction = ((double) sampledPoints) / totalPoints;

    // Get statistics
    // --------------
    double sampleFactor = 0.01;
    int minCount = 1000;
    if (areaFraction != 0) {
      sampleFactor = Math.min (sampleFactor / areaFraction, 1);
      minCount = (int) (minCount / areaFraction);
    } // if
    int[] stride = variable.getOptimalStride (start, end, sampleFactor, 
      minCount);
    DataVariableIterator iter = new DataVariableIterator (variable,
      new ConstrainedStrideLocationIterator (shape, stride));
    Statistics stats = new Statistics (iter);

    // Setup super
    // -----------
    init (
      variable.getName(),
      variable.getUnits(),
      variable.getFormat(),
      trans,
      stats,
      new DataLocation[] {start, end}
    );

  } // PolygonSurvey constructor

  ////////////////////////////////////////////////////////////

} // PolygonSurvey class

////////////////////////////////////////////////////////////////////////
