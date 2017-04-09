////////////////////////////////////////////////////////////////////////
/*

     File: PolygonSurvey.java
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
import java.awt.Rectangle;
import java.awt.Shape;
import noaa.coastwatch.util.BoxSurvey;
import noaa.coastwatch.util.ConstrainedStrideLocationIterator;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DataVariableIterator;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>PolygonSurvey</code> class holds survey information for a
 * polygon of data values.
 *
 * @author Peter Hollemans
 * @since 3.1.7
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
   * @param trans the survey variable earth transform.
   * @param shape the polygon shape.
   */
  public PolygonSurvey (
    DataVariable variable,
    EarthTransform trans,
    Shape shape
  ) { 

    this.shape = shape;

    // Compute statistics
    // ------------------
    DataLocationConstraints lc = new DataLocationConstraints();
    lc.polygon = shape;
    lc.fraction = 0.01;
    lc.minCount = 1000;
    Statistics stats = VariableStatisticsGenerator.getInstance().generate (variable, lc);

    // Initialize
    // ----------
    init (
      variable.getName(),
      variable.getUnits(),
      variable.getFormat(),
      trans,
      stats,
      DataLocationConstraints.getShapeBounds (shape)
    );
    areaFraction = DataLocationConstraints.getShapeArea (shape);

  } // PolygonSurvey constructor

  ////////////////////////////////////////////////////////////

} // PolygonSurvey class

////////////////////////////////////////////////////////////////////////
