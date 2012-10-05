////////////////////////////////////////////////////////////////////////
/*
     FILE: HistogramPanel.java
  PURPOSE: To show a histogram graphically.
   AUTHOR: Peter Hollemans
     DATE: 2003/09/13
  CHANGES: 2005/03/21, PFH, added clone() calls

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;
import noaa.coastwatch.util.*;

/**
 * A histogram panel displays a histogram plot as a series of 
 * vertical columns.
 */
public class HistogramPanel
  extends JPanel {

  // Variables
  // ---------
  /** The histogram statistics. */
  private Statistics stats;

  /** The data value range. */
  private double[] range;

  ////////////////////////////////////////////////////////////

  /** Creates a new histogram panel with no data. */
  public HistogramPanel () {

    this.stats = null;
    this.range = null;

  } // HistogramPanel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new histogram panel with the specified data.
   *
   * @param stats the histogram statistics.
   */
  public HistogramPanel (
    Statistics stats
  ) {

    this.stats = stats;
    this.range = new double[] {stats.getMin(), stats.getMax()};

  } // HistogramPanel constructor

  ////////////////////////////////////////////////////////////

  public void paintComponent (
    Graphics g
  ) {

    // Check showing
    // -------------
    if (!isShowing ()) return;

    // Call super
    // ----------
    super.paintComponent (g);

    // Draw histogram bins
    // -------------------
    Dimension dims = getSize();
    g.setColor (getForeground());
    if (stats != null && range != null) {
      double scale = (double) (range[1] - range[0]) / (dims.width-1);
      for (int i = 0; i < dims.width; i++) {
        int columnHeight = (int) (stats.getNormalizedCount (range[0] + 
          i*scale) * dims.height);
        g.drawLine (i, dims.height-1, i, (dims.height-1) - columnHeight);
      } // for
    } // if

    // Draw border
    // -----------
    g.drawRect (0, 0, dims.width-1, dims.height-1);

  } // paintComponent

  ////////////////////////////////////////////////////////////

  /** Gets the current statistics. */
  public Statistics getStatistics () { return (stats); }

  ////////////////////////////////////////////////////////////

  /** Sets the current statistics. */
  public void setStatistics (Statistics stats) {

    this.stats = stats;
    repaint();

  } // setStatistics

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the data value range.  The range is used in combination with
   * the statistics to determine the position of the vertical column
   * shown in the histogram.  By default the range is determined from
   * the statistics.
   *
   * @param range the data value range as [min, max].
   */
  public void setRange (
    double[] range
  ) {

    this.range = (double[]) range.clone();
    repaint();

  } // setRange

  ////////////////////////////////////////////////////////////

} // HistogramPanel

////////////////////////////////////////////////////////////////////////
