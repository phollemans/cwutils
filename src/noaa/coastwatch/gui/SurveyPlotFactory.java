////////////////////////////////////////////////////////////////////////
/*

     File: SurveyPlotFactory.java
   Author: Peter Hollemans
     Date: 2004/03/31

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
package noaa.coastwatch.gui;

// Imports
// -------
import javax.swing.JPanel;
import noaa.coastwatch.gui.HistogramStatisticsPanel;
import noaa.coastwatch.gui.LineStatisticsPanel;
import noaa.coastwatch.util.BoxSurvey;
import noaa.coastwatch.util.EarthDataSurvey;
import noaa.coastwatch.util.LineSurvey;
import noaa.coastwatch.util.PointSurvey;
import noaa.coastwatch.util.PolygonSurvey;

/** 
 * The <code>SurveyPlotFactory</code> creates data plot panels that
 * are appropriate for a given survey.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class SurveyPlotFactory {

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a plot panel for the specified survey.
   *
   * @param survey the survey to use.
   *
   * @return the newly created plot panel.
   * 
   * @throws IllegalArgumentException if the survey is not supported 
   * by any known property plot panel.
   */
  public static JPanel create (
    EarthDataSurvey survey
  ) {

    // Create panel
    // ------------
    JPanel panel;
    if (survey instanceof BoxSurvey || survey instanceof PolygonSurvey)
      panel = new HistogramStatisticsPanel (survey.getStatistics(), 
        createLabel (survey));
    else if (survey instanceof LineSurvey)
      panel = new LineStatisticsPanel (survey.getStatistics(), 
        createLabel (survey));
    else if (survey instanceof PointSurvey)
      panel = new JPanel();
    else {
      throw new IllegalArgumentException ("Unsupported survey class " +
        survey.getClass());
    } // else

    return (panel);

  } // create

  ////////////////////////////////////////////////////////////

  /** Creates a variable name/units label for the specified survey. */
  private static String createLabel (
    EarthDataSurvey survey
  ) {

    StringBuffer buffer = new StringBuffer();
    buffer.append (survey.getVariableName().toUpperCase());
    if (!survey.getVariableUnits().equals ("")) {
      buffer.append (" (");
      buffer.append (survey.getVariableUnits());
      buffer.append (")");
    } // if
    String label = buffer.toString().replace ('_', ' ');

    return (label);

  } // createLabel

  ////////////////////////////////////////////////////////////

} // SurveyPlotFactory class

////////////////////////////////////////////////////////////////////////
