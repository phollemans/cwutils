////////////////////////////////////////////////////////////////////////
/*

     File: HistogramStatisticsPanel.java
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
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import noaa.coastwatch.gui.StatisticsPanel;
import noaa.coastwatch.gui.XYPlotPanel;
import noaa.coastwatch.render.DataColorScale;
import noaa.coastwatch.util.Statistics;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>HistogramStatisticsPanel</code> shows a simple data plot
 * of statistics as a histogram.  The x axis is labelled with a
 * user-specified label, and the y axes of the plot are labelled with
 * the normalized count.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class HistogramStatisticsPanel
  extends StatisticsPanel {

  private static final Logger LOGGER = Logger.getLogger (HistogramStatisticsPanel.class.getName());

  // Variables
  // ---------

  /** The x axis label. */
  private String xLabel;

  ////////////////////////////////////////////////////////////

  protected String getXLabel() { return (xLabel); }

  ////////////////////////////////////////////////////////////

  protected String getYLabel() { return ("Normalized count"); }

  ////////////////////////////////////////////////////////////

  protected double[] getXRange() { 

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

  } // getXRange

  ////////////////////////////////////////////////////////////

  protected double[] getYRange() { return (new double[] {0, 1.001}); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new panel using the specified data.
   *
   * @param stats the histogram statistics.
   * @param label the x-axis label.
   */
  public HistogramStatisticsPanel (
    Statistics stats,
    String label
  ) { 

    super (stats);
    xLabel = label;

  } // HistogramStatisticsPanel constructor

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
    AffineTransform inverse;
    try { inverse = affine.createInverse(); }
    catch (NoninvertibleTransformException e) { 
      throw new RuntimeException (e); 
    } // catch
    Statistics stats = getStatistics();

    // Set color
    // ---------
    g.setColor (getForeground());

    // Draw histogram bars
    // -------------------
    Point2D.Double plotPoint1 = new Point2D.Double();
    Point2D.Double dataPoint1 = new Point2D.Double();
    Point2D.Double plotPoint2 = new Point2D.Double();
    Point2D.Double dataPoint2 = new Point2D.Double();
    Line2D line = new Line2D.Double();
    for (int x = bounds.x; x < bounds.x+bounds.width; x++) {
      plotPoint1.setLocation (x, bounds.y+bounds.height-1);
      inverse.transform (plotPoint1, dataPoint1);
      dataPoint2.setLocation (dataPoint1.x, 
        stats.getNormalizedCount (dataPoint1.x));
      affine.transform (dataPoint2, plotPoint2);
      plotPoint2.x = plotPoint1.x;
      line.setLine (plotPoint1, plotPoint2);
      g2d.draw (line);
    } // for

  } // paintComponent

  ////////////////////////////////////////////////////////////

} // HistogramStatisticsPanel constructor

////////////////////////////////////////////////////////////////////////
