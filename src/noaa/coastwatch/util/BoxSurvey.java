////////////////////////////////////////////////////////////////////////
/*

     File: BoxSurvey.java
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataSurvey;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.VariableStatisticsGenerator;
import noaa.coastwatch.util.ReportFormatter;

/**
 * The <code>BoxSurvey</code> class holds survey information for a
 * rectangle of data values.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class BoxSurvey 
  extends EarthDataSurvey {

  ////////////////////////////////////////////////////////////

  /** Gets the survey type for results reporting. */
  protected String getSurveyType () { return ("Rectangle"); }

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
    return (percent);

  } // getSampleSize

  ////////////////////////////////////////////////////////////

  @Override
  public void getResults (ReportFormatter formatter) {

    formatter.start();

    formatter.title (getSurveyType() + " survey of " + getVariableName());

    EarthTransform trans = getTransform();
    DataLocation[] locs = getExtents();
    EarthLocation startLoc = trans.transform (locs[0]);
    EarthLocation endLoc = trans.transform (locs[1]);

    formatter.section ("Survey extents:");

    var map = new LinkedHashMap<String, String>();
    formatter.line ("Upper-left corner:");
    map.put ("Row, Col", locs[0].format (true));
    map.put ("Lat, Lon", startLoc.format());
    formatter.map (map);

    formatter.line ("Lower-right corner:");
    map.put ("Row, Col", locs[1].format (true));
    map.put ("Lat, Lon", endLoc.format());
    formatter.map (map);

    Statistics stats = getStatistics();
    NumberFormat format = getVariableFormat();
    DecimalFormat decFormat = new DecimalFormat ("0.######");
    int valid = stats.getValid();

    formatter.section ("Survey statistics:");

    map.clear();
    map.put ("Sample", new DecimalFormat ("0.#").format (getSampleSize()) + "%");    
    map.put ("Count", Integer.toString (stats.getValues()));
    map.put ("Valid", Integer.toString (valid));
    map.put ("Min", (valid == 0 ? "NaN" : format.format (stats.getMin())));
    map.put ("Max", (valid == 0 ? "NaN" : format.format (stats.getMax())));
    map.put ("Mean", (valid == 0 ? "NaN" : decFormat.format (stats.getMean())));
    map.put ("Stdev", (valid == 0 ? "NaN" : decFormat.format (stats.getStdev())));
    formatter.map (map);

    formatter.end();

  } // getResults

  ////////////////////////////////////////////////////////////

  /** Creates a new empty box survey. */
  protected BoxSurvey () { }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new box survey.
   * 
   * @param variable the survey variable.
   * @param trans the survey variable earth transform.
   * @param start the box starting location.
   * @param end the box ending location.  The ending location must
   * have coordinates &gt;= the starting location.
   */
  public BoxSurvey (
    DataVariable variable,
    EarthTransform trans,
    DataLocation start,
    DataLocation end
  ) { 

    // Compute statistics
    // ------------------
    DataLocationConstraints lc = new DataLocationConstraints();
    lc.start = start;
    lc.end = end;
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
      new DataLocation[] {start, end}
    );

  } // BoxSurvey constructor

  ////////////////////////////////////////////////////////////

} // BoxSurvey class

////////////////////////////////////////////////////////////////////////
