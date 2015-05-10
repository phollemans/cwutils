////////////////////////////////////////////////////////////////////////
/*
     FILE: StatisticsPanel.java
  PURPOSE: Shows statistics data in an XY plot panel.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/30
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
import noaa.coastwatch.gui.XYPlotPanel;
import noaa.coastwatch.util.Statistics;

/**
 * The <code>StatisticsPanel</code> is an abstract class that holds a
 * <code>Statistics</code> object and draws the axes and labels
 * desired for a plot of the statistics.  It is up to the child class
 * to draw the actual statistics data.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public abstract class StatisticsPanel
  extends XYPlotPanel {

  // Variables
  // ---------

  /** The histogram statistics. */
  private Statistics stats;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new panel using the specified statistics data.
   *
   * @param stats the data statistics.
   */
  protected StatisticsPanel (
    Statistics stats
  ) {

    this.stats = stats;

  } // StatisticsPanel constructor

  ////////////////////////////////////////////////////////////

  /** Gets the current statistics. */
  public Statistics getStatistics () { return (stats); }

  ////////////////////////////////////////////////////////////

} // StatisticsPanel class

////////////////////////////////////////////////////////////////////////
