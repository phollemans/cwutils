////////////////////////////////////////////////////////////////////////
/*

     File: DataReferenceOverlay.java
   Author: Peter Hollemans
     Date: 2006/12/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.util.Arrays;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.LabeledLineOverlay;
import noaa.coastwatch.render.lines.LineCollection;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.Grid;

/**
 * The <code>DataReferenceOverlay</code> class annotates an {@link
 * EarthDataView} object with row/column grid lines and labels.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class DataReferenceOverlay 
  extends LabeledLineOverlay {

  // Constants
  // ---------

  /** The serialization constant. */
  private static final long serialVersionUID = 3580402521822495401L;

  /** The allowed automatic grid increment base powers. */
  private static final int[] GRID_BASES = {1, 2, 5};

  /** The default number of desired grid lines. */
  private final static int GRID_LINES = 5;

  // Variables
  // ---------

  /** The grid reference row locations in pixels. */
  private double[] rows;

  /** The grid reference column locations in pixels. */
  private double[] cols;

  /** The manual lines flag. */
  private boolean manualLines;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the manual grid lines flag.  When off, the grid lines are
   * calculated automatically at rendering time for an optimal number
   * of grid lines in both the rows and columns directions.
   */
  public void setManualLines (boolean flag) { 

    if (manualLines != flag) {
      manualLines = flag; 
      prepared = false;
    } // if

  } // setManualIncrement

  ////////////////////////////////////////////////////////////

  /** Gets the manual grid increment mode flag. */
  public boolean getManualLines () { return (manualLines); }

  ////////////////////////////////////////////////////////////

  /** Sets the reference row lines. */
  public void setRows (double[] rows) {

    if (!Arrays.equals (this.rows, rows)) {
      this.rows = (double[]) rows.clone();
      if (manualLines) prepared = false;
    } // if

  } // setRows

  ////////////////////////////////////////////////////////////

  /** Gets the current reference row lines. */
  public double[] getRows () { return ((double[]) rows.clone()); }

  ////////////////////////////////////////////////////////////

  /** Sets the reference row lines. */
  public void setCols (double[] cols) {

    if (!Arrays.equals (this.cols, cols)) {
      this.cols = (double[]) cols.clone();
      if (manualLines) prepared = false;
    } // if

  } // setCols

  ////////////////////////////////////////////////////////////

  /** Gets the current reference row lines. */
  public double[] getCols () { return ((double[]) cols.clone()); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new grid overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param manualLines the manual lines flag.
   * @param rows the reference row line values.
   * @param cols the reference column line values.
   * @param drawLabels the grid labels flag, true if grid labels
   * should be drawn.
   * @param font the grid labels font.  The labels font may be
   * null if no labels are to be drawn.
   */
  public DataReferenceOverlay (
    Color color,
    int layer,
    Stroke stroke,
    boolean manualLines,
    double[] rows,
    double[] cols,
    boolean drawLabels,
    Font font
  ) { 

    super (color, layer, stroke, drawLabels, font);
    this.manualLines = manualLines;
    this.rows = (double[]) rows.clone();
    this.cols = (double[]) cols.clone();

  } // DataReferenceOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new grid overlay.  The layer number is initialized
   * to 0, the stroke to the default <code>BasicStroke</code>, the
   * manual lines flag to false, labels to true, and the font to the
   * default font face, plain style, 12 point.
   *
   * @param color the overlay color.
   */
  public DataReferenceOverlay (
    Color color
  ) { 

    super (color);
    this.manualLines = manualLines;
    this.rows = new double[0];
    this.cols = new double[0];

  } // DataReferenceOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Computes a grid increment value based on the data location
   * corners of the view.
   *
   * @param corners the data location corners of the view as
   * [upperLeft, lowerRight].
   */
  private static int getIncrementValue (
    DataLocation[] corners
  ) {

    // Get maximum geographic extents
    // ------------------------------
    double extent = Math.min (
      Math.abs (corners[0].get (Grid.ROWS) - corners[1].get (Grid.ROWS)),
      Math.abs (corners[0].get (Grid.COLS) - corners[1].get (Grid.COLS))
    );

    // Search for the best increment
    // -----------------------------
    int baseIndex = 0, power = 0;
    double lineDiff = 0, lastLineDiff, increment = 0, lastIncrement;
    int iter = 0;
    do {
      iter++;

      // Compute new increment
      // ---------------------
      lastIncrement = increment;
      increment = GRID_BASES[baseIndex]*Math.pow (10, power);

      // Compute difference with target line count
      // -----------------------------------------
      double lines = extent/increment;
      lastLineDiff = lineDiff;
      lineDiff = Math.abs (lines - GRID_LINES);

      // Increment base and power
      // ------------------------
      baseIndex++;
      if (baseIndex > GRID_BASES.length-1) {
        baseIndex = 0;
        power++;
      } // if

    } while (iter == 1 || lineDiff < lastLineDiff);

    return ((int) Math.round (lastIncrement));

  } // getIncrementValue

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the line values for a given location range and increment. 
   *
   * @param range the data location range bounds.
   * @param inc the data location increment.
   *
   * @return the line values.
   */
  private static double[] getLineValues (
    double[] range,
    int inc
  ) {

    int lines = (int) Math.ceil (Math.abs (range[0] - range[1])/inc);
    double[] values = new double[lines];
    int minValue = (int) Math.ceil (Math.min (range[0], range[1]));
    for (values[0] = minValue; values[0]%inc != 0; values[0]++);
    for (int i = 1; i < lines; i++) values[i] = values[0] + i*inc;

    return (values);

  } // getLineValues

  ////////////////////////////////////////////////////////////

  @Override
  protected LineCollection getLines (
    EarthDataView view
  ) {
 
    // Compute row and column lines
    // ----------------------------
    DataLocation[] corners = view.getBounds();
    double[] rows, cols;
    if (!manualLines) {
      int inc = getIncrementValue (corners);
      rows = getLineValues (new double[] {corners[0].get (Grid.ROWS),
        corners[1].get (Grid.ROWS)}, inc);
      cols = getLineValues (new double[] {corners[0].get (Grid.COLS),
        corners[1].get (Grid.COLS)}, inc);
    } // if

    // Use user-specified row and column lines
    // ---------------------------------------
    else {
      rows = this.rows;
      cols = this.cols;
    } // else

    // Compute bounds and mid
    // ----------------------
    double startCol = 
      Math.min (corners[0].get (Grid.COLS), corners[1].get (Grid.COLS));
    double endCol = 
      Math.max (corners[0].get (Grid.COLS), corners[1].get (Grid.COLS));
    double midCol = (startCol + endCol)/2;
    double startRow = 
      Math.min (corners[0].get (Grid.ROWS), corners[1].get (Grid.ROWS));
    double endRow =
      Math.max (corners[0].get (Grid.ROWS), corners[1].get (Grid.ROWS));
    double midRow = (startRow + endRow)/2;

    // Create row lines
    // ----------------
    EarthImageTransform trans = view.getTransform();
    LineCollection lines = new LineCollection();
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] < startRow || rows[i] > endRow) continue;
      int intRow = (int) rows[i];
      String label = "R=" + (rows[i] == intRow ? Integer.toString (intRow) :
        Double.toString (rows[i]));
      lines.addSegment (label, trans,
        new DataLocation (rows[i], startCol),
        new DataLocation (rows[i], midCol));
      lines.addSegment (label, trans,
        new DataLocation (rows[i], midCol),
        new DataLocation (rows[i], endCol));
    } // for

    // Create column lines
    // -------------------
    for (int i = 0; i < cols.length; i++) {
      if (cols[i] < startCol || cols[i] > endCol) continue;
      int intCol = (int) cols[i];
      String label = "C=" + (cols[i] == intCol ? Integer.toString (intCol) :
        Double.toString (cols[i]));
      lines.addSegment (label, trans,
        new DataLocation (startRow, cols[i]),
        new DataLocation (midRow, cols[i]));
      lines.addSegment (label, trans,
        new DataLocation (midRow, cols[i]),
        new DataLocation (endRow, cols[i]));
    } // for

    return (lines);

  } // getLines

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    // Get corners
    // -----------
    DataLocation topLeft = new DataLocation (
      Double.parseDouble (argv[0]),
      Double.parseDouble (argv[1])
    );
    DataLocation bottomRight = new DataLocation (
      Double.parseDouble (argv[2]),
      Double.parseDouble (argv[3])
    );

    // Get increment
    // -------------
    int inc = getIncrementValue (new DataLocation[] {topLeft, bottomRight});
    System.out.println ("inc = " + inc);

    // Get line values
    // ---------------
    double[] rows = getLineValues (new double[] {topLeft.get (Grid.ROWS),
      bottomRight.get (Grid.ROWS)}, inc);
    double[] cols = getLineValues (new double[] {topLeft.get (Grid.COLS),
      bottomRight.get (Grid.COLS)}, inc);
    for (int i = 0; i < rows.length; i++)
      System.out.println ("r = " + rows[i]);
    for (int i = 0; i < cols.length; i++)
      System.out.println ("c = " + cols[i]);

  } // main

  ////////////////////////////////////////////////////////////

} // DataReferenceOverlay class

////////////////////////////////////////////////////////////////////////
