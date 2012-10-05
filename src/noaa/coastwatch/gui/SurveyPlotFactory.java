////////////////////////////////////////////////////////////////////////
/*
     FILE: SurveyPlotFactory.java
  PURPOSE: Creates a plot appropriate for a survey.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/31
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import javax.swing.*;
import noaa.coastwatch.util.*;

/** 
 * The <code>SurveyPlotFactory</code> creates data plot panels that
 * are appropriate for a given survey.
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
