////////////////////////////////////////////////////////////////////////
/*

     File: EnhancementFunctionPanel.java
   Author: Peter Hollemans
     Date: 2003/09/13

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JPanel;
import noaa.coastwatch.render.EnhancementFunction;

/**
 * An enhancement function panel displays an enhancement function
 * graphically as a line plot.
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public class EnhancementFunctionPanel
  extends JPanel {

  // Variables
  // ---------
  /** The enhancement function. */
  private EnhancementFunction func;

  /** The data value range. */
  private double[] range;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new enhancement function panel with no initial
   * function.
   */
  public EnhancementFunctionPanel () { 

    func = null;
    range = null;
    
  } // EnhancementFunctionPanel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new enhancement function panel with the specified
   * initial function.
   *
   * @param func the initial function.
   */
  public EnhancementFunctionPanel (
    EnhancementFunction func
  ) {

    this.func = (EnhancementFunction) func.clone();
    this.range = new double[] {func.getInverse (0), func.getInverse (1)};

  } // EnhancementFunctionPanel constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void paintComponent (
    Graphics g
  ) {

    // Check showing
    // -------------
    if (!isShowing()) return;

    // Call super
    // ----------
    super.paintComponent (g);

    // Plot function
    // -------------
    Dimension dims = getSize();
    g.setColor (getForeground());
    if (func != null && range != null) {
      double scale = (double) (range[1] - range[0]) / (dims.width-1);
      Point last = null;
      for (int i = 0; i < dims.width; i++) {
        int x = i;
        int y = (dims.height-1) - (int) (func.getValue (range[0] + 
          i*scale)*dims.height);
        Point point = new Point (x, y);
        if (last != null) g.drawLine (last.x, last.y, point.x, point.y);
        last = point;
      } // for
    } // if

    // Draw border
    // -----------
    g.drawRect (0, 0, dims.width-1, dims.height-1);

  } // paintComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the current enhancement function.
   *
   * @param func the function to use.
   */
  public void setFunction (EnhancementFunction func) {

    this.func = (EnhancementFunction) func.clone();
    repaint();

  } // setFunction

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the data value range.  The range is used in combination with
   * the enhancement function to determine the line plot.  By default
   * the range is determined from the enhancement function itself.
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

} // EnhancementFunctionPanel

////////////////////////////////////////////////////////////////////////
