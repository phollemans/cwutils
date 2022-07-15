////////////////////////////////////////////////////////////////////////
/*

     File: PointSurvey.java
   Author: Peter Hollemans
     Date: 2004/03/26

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
 * The <code>PointSurvey</code> class holds survey information for a
 * single data location.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class PointSurvey 
  extends EarthDataSurvey {

  ////////////////////////////////////////////////////////////

  @Override
  public void getResults (ReportFormatter formatter) {

    formatter.start();

    formatter.title ("Point survey of " + getVariableName());

    EarthTransform trans = getTransform();
    DataLocation loc = getExtents()[0];
    EarthLocation earthLoc = trans.transform (loc);

    formatter.section ("Survey extents:");

    var map = new LinkedHashMap<String, String>();
    map.put ("Row, Col", loc.format (true));
    map.put ("Lat, Lon", earthLoc.format());
    formatter.map (map);

    Statistics stats = getStatistics();
    NumberFormat format = getVariableFormat();
    int valid = stats.getValid();

    formatter.section ("Survey statistics:");

    map.clear();
    map.put ("Valid", Integer.toString (valid));
    map.put ("Value", (valid == 0 ? "NaN" : format.format (stats.getMin())));
    formatter.map (map);

    formatter.end();

  } // getResults

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new point survey.
   * 
   * @param variable the survey variable.
   * @param trans the survey variable earth transform.
   * @param loc the survey data location.
   */
  public PointSurvey (
    DataVariable variable,
    EarthTransform trans,
    DataLocation loc
  ) { 

    // Compute statistics
    // ------------------
    DataLocationConstraints lc = new DataLocationConstraints();
    lc.start = loc;
    lc.end = loc;
    lc.fraction = 1;
    Statistics stats = VariableStatisticsGenerator.getInstance().generate (variable, lc);

    // Initialize
    // ----------
    init (
      variable.getName(),
      variable.getUnits(),
      variable.getFormat(),
      trans,
      stats,
      new DataLocation[] {loc, loc}
    );

  } // PointSurvey constructor

  ////////////////////////////////////////////////////////////

} // PointSurvey class

////////////////////////////////////////////////////////////////////////
