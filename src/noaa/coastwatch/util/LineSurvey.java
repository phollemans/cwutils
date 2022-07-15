////////////////////////////////////////////////////////////////////////
/*

     File: LineSurvey.java
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DataVariableIterator;
import noaa.coastwatch.util.EarthDataSurvey;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.LineLocationIterator;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.ReportFormatter;

/**
 * The <code>LineSurvey</code> class holds survey information for a
 * line of data values from one point to another.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class LineSurvey 
  extends EarthDataSurvey {

  ////////////////////////////////////////////////////////////

  @Override
  public void getResults (ReportFormatter formatter) {

    formatter.start();

    formatter.title ("Line survey of " + getVariableName());

    EarthTransform trans = getTransform();
    DataLocation[] locs = getExtents();
    EarthLocation startLoc = trans.transform (locs[0]);
    EarthLocation endLoc = trans.transform (locs[1]);

    formatter.section ("Survey extents:");

    var map = new LinkedHashMap<String, String>();
    formatter.line ("Starting point:");
    map.put ("Row, Col", locs[0].format (true));
    map.put ("Lat, Lon", startLoc.format());
    formatter.map (map);

    map.clear();
    formatter.line ("Ending point:");
    map.put ("Row, Col", locs[1].format (true));
    map.put ("Lat, Lon", endLoc.format());
    map.put ("Distance", new DecimalFormat ("0.##").format (startLoc.distance (endLoc)) + " km");
    formatter.map (map);

    Statistics stats = getStatistics();
    NumberFormat format = getVariableFormat();
    DecimalFormat decFormat = new DecimalFormat ("0.######");
    int valid = stats.getValid();

    formatter.section ("Survey statistics:");

    map.clear();
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

  /** 
   * Creates a new line survey.
   * 
   * @param variable the survey variable.
   * @param trans the survey variable earth transform.
   * @param start the line starting location.
   * @param end the line ending location.
   */
  public LineSurvey (
    DataVariable variable,
    EarthTransform trans,
    DataLocation start,
    DataLocation end
  ) { 

    super (
      variable.getName(),
      variable.getUnits(),
      variable.getFormat(),
      trans,
      new Statistics (new DataVariableIterator (variable, 
        new LineLocationIterator (start, end)), true),
      new DataLocation[] {start, end}
    );

  } // LineSurvey constructor

  ////////////////////////////////////////////////////////////

} // LineSurvey class

////////////////////////////////////////////////////////////////////////
