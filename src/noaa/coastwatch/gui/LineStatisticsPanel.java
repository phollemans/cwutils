////////////////////////////////////////////////////////////////////////
/*

     File: LineStatisticsPanel.java
   Author: Peter Hollemans
     Date: 2004/03/30

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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import noaa.coastwatch.gui.StatisticsPanel;
import noaa.coastwatch.gui.XYPlotPanel;
import noaa.coastwatch.render.DataColorScale;
import noaa.coastwatch.util.Statistics;

/**
 * The <code>LineStatisticsPanel</code> shows a simple data plot of
 * statistics as an x-y line plot of the statistics values.  The x
 * axis is labelled with the data value count, and the y axis with the
 * user-specified label.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class LineStatisticsPanel
  extends StatisticsPanel {

  // Variables
  // ---------

  /** The y axis label. */
  private String yLabel;

  ////////////////////////////////////////////////////////////

  protected String getXLabel() { return ("Value index"); }

  ////////////////////////////////////////////////////////////

  protected String getYLabel() { return (yLabel); }

  ////////////////////////////////////////////////////////////

  protected double[] getXRange() { 

    // Get min and max
    // ---------------
    Statistics stats = getStatistics();
    double min = 0;
    double max = stats.getValues()-1;

    // Nudge max
    // ---------
    double interval = DataColorScale.getLinearTickInterval (min, max, 
      AXIS_TICKS);
    max = Math.floor ((max - Double.MIN_VALUE + 
      interval)/interval)*interval;

    return (new double[] {min, max});

  } // getXRange

  ////////////////////////////////////////////////////////////

  protected double[] getYRange() { 

    // Get min and max
    // ---------------
    Statistics stats = getStatistics();
    double min = stats.getMin();
    double max = stats.getMax();

    // Nudge min and max
    // -----------------
    double interval = DataColorScale.getLinearTickInterval (min, max, 
      AXIS_TICKS);
    min = Math.ceil ((min + Double.MIN_VALUE - 
      interval)/interval)*interval;
    max = Math.floor ((max - Double.MIN_VALUE + 
      interval)/interval)*interval;

    return (new double[] {min, max});

  } // getYRange

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new panel using the specified data.
   *
   * @param stats the line statistics.
   * @param label the y-axis label.
   */
  public LineStatisticsPanel (
    Statistics stats,
    String label
  ) { 

    super (stats);
    yLabel = label;

  } // LineStatisticsPanel constructor

  ////////////////////////////////////////////////////////////

  public void paintComponent (
    Graphics g
  ) {

    // Paint super
    // -----------
    super.paintComponent (g);
    if (!getValid()) return;
    Graphics2D g2d = (Graphics2D) g;

    // Get plot data
    // -------------
    Rectangle bounds = getPlotBounds();
    AffineTransform affine = getPlotAffine();
    Statistics stats = getStatistics();

    // Set color
    // ---------
    g.setColor (getForeground());

    // Draw lines
    // ----------
    Point2D.Double plotPoint1 = new Point2D.Double();
    Point2D.Double dataPoint1 = new Point2D.Double();
    Point2D.Double plotPoint2 = new Point2D.Double();
    Point2D.Double dataPoint2 = new Point2D.Double();
    Line2D line = new Line2D.Double();
    int values = stats.getValues();
    for (int i = 0; i < values-1; i++) {
      double value1 = stats.getData(i);
      double value2 = stats.getData(i+1);
      if (!Double.isNaN (value1) && !Double.isNaN (value2)) {
        dataPoint1.setLocation (i, value1);
        affine.transform (dataPoint1, plotPoint1);
        dataPoint2.setLocation (i+1, value2);
        affine.transform (dataPoint2, plotPoint2);
        line.setLine (plotPoint1, plotPoint2);
        g2d.draw (line);
      } // if
    } // for

  } // paintComponent

  ////////////////////////////////////////////////////////////

} // LineStatisticsPanel class

////////////////////////////////////////////////////////////////////////
