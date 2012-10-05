////////////////////////////////////////////////////////////////////////
/*
     FILE: DirectionSymbol.java
  PURPOSE: Renders a point feature as a direction vector symbol.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/05
  CHANGES: 2006/01/18, PFH, added check for null color

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * A <code>DirectionSymbol</code> is a <code>PointFeatureSymbol</code>
 * that renders a directional vector.
 */
public abstract class DirectionSymbol
  extends PointFeatureSymbol {

  // Variables
  // ---------

  /** The Earth transform used for converting directions. */
  private EarthTransform2D trans;  

  /** 
   * The direction-is-from flag, indicating that direction angles must
   * be flipped 180 degrees before drawing.
   */
  private boolean directionIsFrom;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the direction-is-from flag.  If true, the direction angle is
   * taken to be the angle from which the vector comes, not the angle
   * to which it points.  By default, the flag is false.
   *
   * @param flag the new direction-is-from flag value.
   */
  public void setDirectionIsFrom (boolean flag) { directionIsFrom = flag; }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the direction-is-from flag.
   *
   * @see #setDirectionIsFrom
   */
  public boolean getDirectionIsFrom () { return (directionIsFrom); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new direction symbol. 
   *
   * @param trans the Earth transform used for converting directions.
   */
  public DirectionSymbol (
    EarthTransform2D trans
  ) {

    this.trans = trans;

  } // DirectionSymbol

  ////////////////////////////////////////////////////////////

  /**
   * Draws a direction symbol with the specified properties.
   *
   * @param gc the graphics context for drawing.
   * @param x the x position of the base.
   * @param y the y position of the base.
   * @param magnitude the magnitude of the vector.
   * @param direction the direction angle of the vector in radians,
   * clockwise with respect to the graphics x-axis.
   */
  public abstract void drawVector (
    Graphics gc, 
    int x, 
    int y,
    double magnitude,
    double direction
  );

  ////////////////////////////////////////////////////////////

  /** Gets the magnitude of the vector feature. */
  public abstract double getMagnitude();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the direction angle of the vector feature.
   * 
   * @return the direction angle of the vector in radians, clockwise
   * with respect to the graphics x-axis.
   */
  public abstract double getDirection();

  ////////////////////////////////////////////////////////////

  /** Normalizes the 2D vector. */
  private void norm (double[] v) {

    double mag = Math.sqrt (v[0]*v[0] + v[1]*v[1]);
    if (mag != 0) {
      for (int i = 0; i < 2; i++) v[i] /= mag;
    } // if

  } // norm

  ////////////////////////////////////////////////////////////

  /** 
   * Converts a direction angle that is clockwise relative to north to
   * a direction angle that is counterclockwise relative to the x axis
   * of the Earth transform.  For example, suppose that the passed
   * direction is 45 deg WRT north, meaning directly northeast.  But
   * suppose that in the Earth transform, north is in the positive x
   * direction and east is in the negative y direction (positive x is
   * right and positive y is up).  Then the 45 deg WRT north direction
   * angle is converted to -45 deg WRT the Earth transform.
   *
   * @param inAngle the input direction angle in radians.
   * @param earthLoc the Earth location at which the direction applies.
   *
   * @return the converted angle, or <code>Double.NaN</code> if the
   * angle could not be converted.
   */
  public double convertAngle (
    double inAngle,
    EarthLocation earthLoc
  ) {

    // Get north and east vectors
    // --------------------------     
    DataLocation baseLoc = trans.transform (earthLoc.translate (-0.01, 0));
    DataLocation tipLoc = trans.transform (earthLoc.translate (0.01, 0));
    double[] northVector = new double[] {
      tipLoc.get (Grid.COLS) - baseLoc.get (Grid.COLS),
      -(tipLoc.get (Grid.ROWS) - baseLoc.get (Grid.ROWS))
    };
    norm (northVector);
    trans.transform (earthLoc.translate (0, -0.01), baseLoc);
    trans.transform (earthLoc.translate (0, 0.01), tipLoc);
    double[] eastVector = new double[] {
      tipLoc.get (Grid.COLS) - baseLoc.get (Grid.COLS),
      -(tipLoc.get (Grid.ROWS) - baseLoc.get (Grid.ROWS))
    };
    norm (eastVector);

    // Get direction vector
    // --------------------
    inAngle = Math.PI/2 - inAngle;
    double[] dirVector = new double[] {
      Math.cos (inAngle),
      Math.sin (inAngle)
    };

    // Create rotation matrix
    // ----------------------
    double[][] rot = new double[][] {
      {eastVector[0], northVector[0]},
      {eastVector[1], northVector[1]}
    };

    // Rotate direction vector
    // -----------------------
    double[] newDirVector = new double[2];
    newDirVector[0] = rot[0][0]*dirVector[0] + rot[0][1]*dirVector[1];
    newDirVector[1] = rot[1][0]*dirVector[0] + rot[1][1]*dirVector[1];

    // Compute new angle
    // -----------------
    double outAngle = Math.atan2 (newDirVector[1], newDirVector[0]);
    return (outAngle);

  } // convertAngle

  ////////////////////////////////////////////////////////////

  public void draw (
    Graphics gc, 
    int x, 
    int y
  ) {

    // Check for null color
    // --------------------
    Color borderColor = getBorderColor();
    if (borderColor == null) return;

    // Set color
    // ---------
    Color savedColor = gc.getColor();
    gc.setColor (borderColor);

    // Draw vector
    // -----------
    drawVector (gc, x, y, getMagnitude(), getDirection());
    
    // Restore color
    // -------------
    gc.setColor (savedColor);

  } // draw

  ////////////////////////////////////////////////////////////

  /** Test this class. */
  public static void main (String[] argv) throws Exception {

    double angle = Double.parseDouble (argv[0]);
    double lat = Double.parseDouble (argv[1]);
    double lon = Double.parseDouble (argv[2]);
    DirectionSymbol symbol = new DirectionSymbol (
      new OrthographicProjection (new EarthLocation (0, 0), new int[] {512,512},
      new EarthLocation (0, 0), new double[] {1, 1})) {
        public void drawVector (
          Graphics gc, 
          int x, 
          int y,
          double magnitude,
          double direction
        ) { }
        public void draw (
          Graphics gc, 
          int x, 
          int y
        ) { }
        public double getDirection () { return (0); }
        public double getMagnitude () { return (0); }
      };
    System.out.println ("Converted angle = " + 
      Math.toDegrees (symbol.convertAngle (Math.toRadians (angle), 
      new EarthLocation (lat, lon))));

  } // main

  ////////////////////////////////////////////////////////////

} // DirectionSymbol class

////////////////////////////////////////////////////////////////////////
