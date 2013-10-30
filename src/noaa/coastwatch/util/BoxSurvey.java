////////////////////////////////////////////////////////////////////////
/*
     FILE: BoxSurvey.java
  PURPOSE: Data survey for a rectangle of data.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/27
  CHANGES: 2005/02/07, PFH, modified to use default EarthLocation formatting
           2005/02/13, PFH, modified result formatting

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
  protected String getSurveyType () { return ("Box"); }

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

  /** Gets a results report for the point survey. */
  public String getResults () {

    StringBuffer buffer = new StringBuffer();
    buffer.append (getSurveyType() + " survey of " + getVariableName() + 
      "\n\n");

    EarthTransform trans = getTransform();
    DataLocation[] locs = getExtents();
    EarthLocation startLoc = trans.transform (locs[0]);
    EarthLocation endLoc = trans.transform (locs[1]);
    buffer.append ("Survey extents:\n");
    buffer.append ("  -- Upper-left --\n");
    buffer.append ("  Row, Col: " + locs[0].format (true) + "\n");
    buffer.append ("  Lat, Lon: " + startLoc.format() + "\n");
    buffer.append ("  -- Lower-right --\n");
    buffer.append ("  Row, Col: " + locs[1].format (true) + "\n");
    buffer.append ("  Lat, Lon: " + endLoc.format() + "\n\n");

    Statistics stats = getStatistics();
    NumberFormat format = getVariableFormat();
    DecimalFormat decFormat = new DecimalFormat ("0.######");
    buffer.append ("Survey statistics:\n");
    DecimalFormat pFormat = new DecimalFormat ("0.#");
    buffer.append ("  Sample:   " + new DecimalFormat ("0.#").format (
      getSampleSize()) + "%\n");
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

  /** Creates a new empty box survey. */
  protected BoxSurvey () { }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new box survey.
   * 
   * @param variable the survey variable.
   * @param trans the survey variable Earth transform.
   * @param start the box starting location.
   * @param end the box ending location.  The ending location must
   * have coordinates >= the starting location.
   */
  public BoxSurvey (
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
      variable.getStatistics (start, end, 
        variable.getOptimalStride (start, end, 0.01, 1000)),
      new DataLocation[] {start, end}
    );

  } // BoxSurvey constructor

  ////////////////////////////////////////////////////////////

} // BoxSurvey class

////////////////////////////////////////////////////////////////////////
