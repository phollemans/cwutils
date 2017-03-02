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
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataSurvey;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.EarthTransform;

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

  /** Gets a results report for the point survey. */
  public String getResults () {

    StringBuffer buffer = new StringBuffer();
    buffer.append ("Point survey of " + getVariableName() + "\n\n");

    EarthTransform trans = getTransform();
    DataLocation loc = getExtents()[0];
    EarthLocation earthLoc = trans.transform (loc);
    buffer.append ("Survey extents:\n");
    buffer.append ("  Row, Col: " + loc.format (true) + "\n");
    buffer.append ("  Lat, Lon: " + earthLoc.format() + "\n\n");

    Statistics stats = getStatistics();
    NumberFormat format = getVariableFormat();
    buffer.append ("Survey statistics:\n");
    int valid = stats.getValid();
    buffer.append ("  Valid:    " + valid + "\n");
    String value = (valid == 0 ? "NaN" : format.format (stats.getMin()));
    buffer.append ("  Value:    " + value + "\n");

    return (buffer.toString());

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

    super (
      variable.getName(),
      variable.getUnits(),
      variable.getFormat(),
      trans,
      variable.getStatistics (loc, loc, 1),
      new DataLocation[] {loc, loc}
    );

  } // PointSurvey constructor

  ////////////////////////////////////////////////////////////

} // PointSurvey class

////////////////////////////////////////////////////////////////////////
