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
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DataVariableIterator;
import noaa.coastwatch.util.EarthDataSurvey;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.LineLocationIterator;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.EarthTransform;

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

  /** Gets a results report for the point survey. */
  public String getResults () {

    StringBuffer buffer = new StringBuffer();
    buffer.append ("Line survey of " + getVariableName() + "\n\n");

    EarthTransform trans = getTransform();
    DataLocation[] locs = getExtents();
    EarthLocation startLoc = trans.transform (locs[0]);
    EarthLocation endLoc = trans.transform (locs[1]);
    buffer.append ("Survey extents:\n");
    buffer.append ("  Distance: " + new DecimalFormat ("0.##").format (
      startLoc.distance (endLoc)) + " km\n");
    buffer.append ("  -- Start --\n");
    buffer.append ("  Row, Col: " + locs[0].format (true) + "\n");
    buffer.append ("  Lat, Lon: " + startLoc.format() + "\n");
    buffer.append ("  -- End --\n");
    buffer.append ("  Row, Col: " + locs[1].format (true) + "\n");
    buffer.append ("  Lat, Lon: " + endLoc.format() + "\n\n");

    Statistics stats = getStatistics();
    NumberFormat format = getVariableFormat();
    DecimalFormat decFormat = new DecimalFormat ("0.######");
    buffer.append ("Survey statistics:\n");
    buffer.append ("  Count:    " + stats.getValues() + "\n");
    int valid = stats.getValid();
    buffer.append ("  Valid:    " + valid + "\n");
    String min = (valid == 0 ? "NaN" : format.format (stats.getMin()));
    buffer.append ("  Min:      " + min + "\n");
    String max = (valid == 0 ? "NaN" : format.format (stats.getMax()));
    buffer.append ("  Max:      " + max + "\n");
    String mean = (valid == 0 ? "NaN" : decFormat.format (stats.getMean()));
    buffer.append ("  Mean:     " + mean + "\n");
    String stdev = (valid == 0 ? "NaN" : decFormat.format (stats.getStdev()));
    buffer.append ("  Stdev:    " + stdev + "\n");

    return (buffer.toString());

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
