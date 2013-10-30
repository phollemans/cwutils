////////////////////////////////////////////////////////////////////////
/*
     FILE: EnhancementFunctionPanel.java
  PURPOSE: To show an enhancement function plot.
   AUTHOR: Peter Hollemans
     DATE: 2003/09/13
  CHANGES: 2004/02/17, PFH, changed enhancement property to function
           2005/03/21, PFH, added clone() calls

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
import javax.swing.*;
import javax.swing.event.*;
import noaa.coastwatch.render.*;

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
   */
  public EnhancementFunctionPanel (
    EnhancementFunction func
  ) {

    this.func = (EnhancementFunction) func.clone();
    this.range = new double[] {func.getInverse (0), func.getInverse (1)};

  } // EnhancementFunctionPanel constructor

  ////////////////////////////////////////////////////////////

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

  /** Sets the current enhancement function. */
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
