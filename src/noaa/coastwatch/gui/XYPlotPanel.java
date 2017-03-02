////////////////////////////////////////////////////////////////////////
/*

     File: XYPlotPanel.java
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import javax.swing.JPanel;
import javax.swing.UIManager;
import noaa.coastwatch.render.DataColorScale;
import noaa.coastwatch.render.TextElement;

/**
 * The <code>XYPlotPanel</code> is an abstract class that draws axes
 * and labels for an x-y plot.  It is up to the child class to draw
 * the actual x-y data.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public abstract class XYPlotPanel
  extends JPanel {

  // Constants
  // ---------
  
  /** The number of axis ticks. */
  public static final int AXIS_TICKS = 5;

  /** The tick size in pixels. */
  private static final int TICK_SIZE = 4;

  /** The space between annotation elements. */
  private static final int SPACE_SIZE = 4;

  // Variables
  // ---------

  /** The x-axis text element. */
  private TextElement xAxisLabel;

  /** The y-axis text element. */
  private TextElement yAxisLabel;

  /** The x-axis tick strings. */
  private TextElement[] xTickLabels;

  /** The y-axis text strings. */
  private TextElement[] yTickLabels;

  /** The plot bounds rectangle. */
  private Rectangle plotBounds;

  /** The plot affine transform. */
  private AffineTransform plotAffine;

  /** The valid flag. */
  private boolean isValid;

  ////////////////////////////////////////////////////////////

  /** Gets the x-axis label. */
  protected abstract String getXLabel();

  ////////////////////////////////////////////////////////////

  /** Gets the y-axis label. */
  protected abstract String getYLabel();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the x-axis value range as [min, max].  If the range has
   * invalid values or min==max, no plot is drawn.
   */
  protected abstract double[] getXRange();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the y-axis value range as [min, max].  If the range has
   * invalid values or min==max, no plot is drawn.
   */
  protected abstract double[] getYRange();

  ////////////////////////////////////////////////////////////

  /** Sets up the various labels for the axes and tick marks. */
  private void setupLabels () {

    // Check ranges
    // ------------
    double[] xRange = getXRange();
    double[] yRange = getYRange();
    if (xRange[0] == xRange[1] || 
      Double.isNaN (xRange[0]) || Double.isNaN (xRange[1]) ||
      yRange[0] == yRange[1] || 
      Double.isNaN (yRange[0]) || Double.isNaN (yRange[1])) return;
    isValid = true;

    // Create axis labels
    // ------------------
    Font font = UIManager.getFont ("Label.font").deriveFont (10.0f);
    Point2D base = new Point (0, 0);
    double[] align = new double[] {0.5, 0};
    xAxisLabel = new TextElement (getXLabel(), font, base, align, 0);
    align = new double[] {0.5, 1};
    yAxisLabel = new TextElement (getYLabel(), font, base, align, 90);

    // Create x-axis tick labels
    // -------------------------
    String[] xTickStrings = DataColorScale.getLinearTickLabels (xRange[0], 
      xRange[1], AXIS_TICKS);
    xTickLabels = new TextElement[xTickStrings.length];
    align = new double[] {0.5, 1};
    for (int i = 0; i < xTickStrings.length; i++) {
      xTickLabels[i] = new TextElement (xTickStrings[i], font, base, align, 
        0);
    } // for

    // Create y-axis tick labels
    // -------------------------
    String[] yTickStrings = DataColorScale.getLinearTickLabels (yRange[0], 
      yRange[1], AXIS_TICKS);
    yTickLabels = new TextElement[yTickStrings.length];
    align = new double[] {1, 0.5};
    for (int i = 0; i < yTickStrings.length; i++) {
      yTickLabels[i] = new TextElement (yTickStrings[i], font, base, align, 
        0);
    } // for

  } // setupLabels

  ////////////////////////////////////////////////////////////

  /** Gets the rectangle used to display the plot data. */
  protected Rectangle getPlotBounds() { return (plotBounds); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the affine transform for translating data coordinates to
   * plot coordinates.
   */
  protected AffineTransform getPlotAffine() { return (plotAffine); }

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if the plot is valid, or false if not.  If the plot
   * is invalid, there is no need to draw any data.
   */
  protected boolean getValid() { return (isValid); }

  ////////////////////////////////////////////////////////////

  /** Paints the x-y panel axes and labels. */
  public void paintComponent (
    Graphics g
  ) {

    super.paintComponent (g);

    // Set rendering hints
    // -------------------
    Graphics2D g2d = (Graphics2D) g;
    Object textHint = g2d.getRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING);
    g2d.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Setup labels
    // ------------
    if (xAxisLabel == null) setupLabels();

    if (isValid) {

      // Set color
      // ---------
      Color foreground = getForeground();
      g.setColor (foreground);
      Color faint = getBackground().darker();

      // Set origin y value
      // ------------------
      Point origin = new Point();
      Dimension panelDims = getSize();
      Rectangle xAxisBounds = xAxisLabel.getBounds (g2d);
      Rectangle xTickBounds = xTickLabels[0].getBounds (g2d);
      origin.y = panelDims.height-1 - SPACE_SIZE - xAxisBounds.height - 
        SPACE_SIZE - xTickBounds.height - SPACE_SIZE - TICK_SIZE;

      // Set origin x value
      // ------------------
      Rectangle yAxisBounds = yAxisLabel.getBounds (g2d);
      Rectangle yTickBounds = yTickLabels[0].getBounds (g2d);
      for (int i = 1; i < yTickLabels.length; i++) {
        Rectangle bounds = yTickLabels[i].getBounds (g2d);
        if (bounds.width > yTickBounds.width) yTickBounds = bounds;
      } // for
      origin.x = SPACE_SIZE + yAxisBounds.width + SPACE_SIZE + yTickBounds.width
       + SPACE_SIZE + TICK_SIZE;

      // Set plot maximum point
      // ----------------------
      xTickBounds = xTickLabels[xTickLabels.length-1].getBounds (g2d);
      yTickBounds = yTickLabels[yTickLabels.length-1].getBounds (g2d);
      Point maximum = new Point (
        panelDims.width-1 - SPACE_SIZE*2 - xTickBounds.width/2, 
        SPACE_SIZE*2 + yTickBounds.height/2);

      // Create affine transform
      // -----------------------
      AffineTransform affine = new AffineTransform();
      double[] xRange = getXRange();
      double[] yRange = getYRange();
      affine.preConcatenate (AffineTransform.getTranslateInstance (-xRange[0],
        -yRange[0]));
      affine.preConcatenate (AffineTransform.getScaleInstance (
        (maximum.x - origin.x) / (xRange[1] - xRange[0]),
        (maximum.y - origin.y) / (yRange[1] - yRange[0])));
      affine.preConcatenate (AffineTransform.getTranslateInstance (origin.x, 
        origin.y));

      // Draw x axis
      // -----------
      Point2D.Double dataPoint1 = new Point2D.Double();
      Point2D.Double plotPoint1 = new Point2D.Double();
      Point2D.Double dataPoint2 = new Point2D.Double();
      Point2D.Double plotPoint2 = new Point2D.Double();
      Line2D line = new Line2D.Double ();
      NumberFormat format = NumberFormat.getInstance();
      for (int i = 0; i < xTickLabels.length; i++) {

        // Draw tick
        // ---------
        double xValue = Double.parseDouble (xTickLabels[i].getText());
        dataPoint1.setLocation (xValue, 0);
        affine.transform (dataPoint1, plotPoint1);
        plotPoint1.y = origin.y;
        plotPoint2.setLocation (plotPoint1.x, plotPoint1.y + TICK_SIZE);      
        line.setLine (plotPoint1, plotPoint2);
        g2d.draw (line);

        // Draw label
        // ----------
        plotPoint2.y += TICK_SIZE;
        xTickLabels[i].setBasePoint (plotPoint2);
        xTickLabels[i].render (g2d, foreground, null);

        // Draw grid line
        // --------------
        plotPoint2.y = maximum.y;
        line.setLine (plotPoint1, plotPoint2);
        g2d.setColor (faint);
        g2d.draw (line);
        g2d.setColor (foreground);

      } // for

      // Draw y axis labels
      // ------------------
      for (int i = 0; i < yTickLabels.length; i++) {

        // Draw tick
        // ---------
        double yValue = Double.parseDouble (yTickLabels[i].getText());
        dataPoint1.setLocation (0, yValue);
        affine.transform (dataPoint1, plotPoint1);
        plotPoint1.x = origin.x;
        plotPoint2.setLocation (plotPoint1.x - TICK_SIZE, plotPoint1.y);
        line.setLine (plotPoint1, plotPoint2);
        g2d.draw (line);

        // Draw label
        // ----------
        plotPoint2.x += -TICK_SIZE;
        yTickLabels[i].setBasePoint (plotPoint2);
        yTickLabels[i].render (g2d, foreground, null);

        // Draw grid line
        // --------------
        plotPoint2.x = maximum.x;
        line.setLine (plotPoint1, plotPoint2);
        g2d.setColor (faint);
        g2d.draw (line);
        g2d.setColor (foreground);

      } // for

      // Draw axes
      // ---------
      xAxisLabel.setBasePoint (new Point ((origin.x + maximum.x)/2, 
        panelDims.height-1 - SPACE_SIZE));
      xAxisLabel.render (g2d, foreground, null);
      g.drawLine (origin.x, origin.y, maximum.x, origin.y);
      yAxisLabel.setBasePoint (new Point (SPACE_SIZE, (origin.y + maximum.y)/2));
      yAxisLabel.render (g2d, foreground, null);
      g.drawLine (origin.x, origin.y, origin.x, maximum.y);

      // Set plot bounds and affine
      // --------------------------
      plotBounds = new Rectangle (origin.x, maximum.y, 
        maximum.x-origin.x+1, origin.y-maximum.y+1);
      plotAffine = affine;

    } // if
    
    // Reset rendering hints
    // ---------------------
    g2d.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, textHint);

  } // paintComponent

  ////////////////////////////////////////////////////////////

} // XYPlotPanel class

////////////////////////////////////////////////////////////////////////
