////////////////////////////////////////////////////////////////////////
/*
     FILE: PointSurvey.java
  PURPOSE: Data survey for a single point.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/26
  CHANGES: 2005/02/07, PFH, modified to use default EarthLocation formatting

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.text.*;
import noaa.coastwatch.util.trans.*;

/**
 * The <code>PointSurvey</code> class holds survey information for a
 * single data location.
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
   * @param trans the survey variable Earth transform.
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
