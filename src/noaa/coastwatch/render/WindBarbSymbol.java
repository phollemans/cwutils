////////////////////////////////////////////////////////////////////////
/*

     File: WindBarbSymbol.java
   Author: Peter Hollemans
     Date: 2005/05/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import noaa.coastwatch.render.DirectionSymbol;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.OrthographicProjection;

/**
 * A <code>WindBarbSymbol</code> is a <code>PointFeatureSymbol</code>
 * that renders a wind barb according to the WMO rules in:
 * <blockquote>
 *   Manual on the Global Data-Processing System, Edition 1992<br>
 *   http://www.wmo.ch/web/www/DPS/Manual/WMO485.pdf
 * </blockquote>
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class WindBarbSymbol
  extends DirectionSymbol {
  
  // Constants
  // ---------

  /** The speed units for knots. */
  public static final int SPEED_KNOTS = 0;

  /** The speed units for meters per second. */
  public static final int SPEED_METERS_PER_SECOND = 1;

  /** The index into speed quanta array of pennants. */
  private static final int PENNANT = 2;

  /** The index into speed quanta array of full barbs. */
  private static final int FULL_BARB = 1;

  /** The index into speed quanta array of half barbs. */
  private static final int HALF_BARB = 0;

  /** The angle of the barb with respect to the main line. */
  private static final double BARB_ANGLE = 70.0;

  /** The length of the main line. */
  private static final int LINE_LENGTH = 3;

  // Variables
  // ---------

  /** The feature attribute for wind speed. */
  private int speedAtt;

  /** The feature attribute for wind direction. */
  private int directionAtt;

  /** The units of speed. */
  private int speedUnits;

  /** The shapes of each speed quanta. */
  private static Shape[] shapesArray;

  /** The space between shapes. */
  private static double[] spaceArray;

  ////////////////////////////////////////////////////////////

  static {

    // Initialize shapes array
    // -----------------------
    shapesArray = new Shape[3];

    // Compute barb ends
    // -----------------
    float barbRise = (float) Math.sin (Math.toRadians (BARB_ANGLE));
    float barbRun = (float) Math.cos (Math.toRadians (BARB_ANGLE));

    // Create half barb
    // ----------------
    GeneralPath halfBarb = new GeneralPath();
    halfBarb.moveTo (0.0f, 0.0f);
    halfBarb.lineTo (barbRun/1.5f, barbRise/1.5f);
    shapesArray[HALF_BARB] = halfBarb;

    // Create full barb
    // ----------------
    GeneralPath fullBarb = new GeneralPath();
    fullBarb.moveTo (0.0f, 0.0f);
    fullBarb.lineTo (barbRun, barbRise);
    shapesArray[FULL_BARB] = fullBarb;

    // Create pennant
    // --------------
    GeneralPath pennant = new GeneralPath();
    pennant.moveTo (0.0f, 0.0f);
    pennant.lineTo (barbRun, barbRise);
    pennant.lineTo (barbRun*2, 0.0f);
    pennant.lineTo (0.0f, 0.0f);
    pennant.closePath();
    shapesArray[PENNANT] = pennant;

    // Initialize space array
    // ----------------------
    spaceArray = new double[] { barbRun, barbRun, barbRun*2 };

  } // static

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new wind barb symbol.
   *
   * @param speedAtt the feature attribute for wind speed.
   * @param directionAtt the feature attribute for wind direction.
   * @param speedUnits the units of speed, either
   * <code>SPEED_KNOTS</code> or <code>SPEED_METERS_PER_SECOND</code>.
   * @param trans the earth transform used for converting directions.
   */
  public WindBarbSymbol (
    int speedAtt,
    int directionAtt,
    int speedUnits,
    EarthTransform2D trans
  ) {

    super (trans);
    this.speedAtt = speedAtt;
    this.directionAtt = directionAtt;
    this.speedUnits = speedUnits;

  } // WindBarbSymbol constructor

  ////////////////////////////////////////////////////////////

  public void drawVector (
    Graphics gc, 
    int x, 
    int y,
    double magnitude,
    double direction
  ) {

    // TODO: Should we rearrange the drawing of speed quanta so that
    // they do not extend beyond the length of the line?

    // TODO: Should we worry about drawing the quanta on the other
    // side of the line in the southern hemisphere?

    // Check wind direction
    // --------------------
    /**
     * WMO rules say that if the wind direction is unknown, we plot
     * nothing.
     */
    if (Double.isNaN (direction)) return;

    // Check wind speed
    // ----------------
    /**
     * WMO rules say that if the wind speed is unknown, we plot an "x"
     * at the end of the barb.
     */
    boolean isInvalidSpeed = Double.isNaN (magnitude);
    
    // Round speed to nearest half barb unit
    // -------------------------------------
    if (speedUnits == SPEED_METERS_PER_SECOND) magnitude *= 2;
    int rSpeed = ((int) Math.round (magnitude/5)) * 5;
    boolean isCalm = (!isInvalidSpeed && rSpeed == 0);

    // Determine number of pennants, full, and half barbs
    // --------------------------------------------------
    int pennants = 0, fullBarbs = 0, halfBarbs = 0;
    if (!isInvalidSpeed && !isCalm) {
      pennants = rSpeed/50;
      int remainder = rSpeed - pennants*50;
      if (remainder > 0) {
        fullBarbs = remainder/10;
        remainder = remainder - fullBarbs*10;
        if (remainder > 0) halfBarbs = 1;
      } // if
    } // if

    // Transform, rotate, and scale origin
    // -----------------------------------
    Graphics2D g2d = (Graphics2D) gc;
    AffineTransform savedTransform = g2d.getTransform();
    g2d.translate (x, y);
    if (!isCalm) g2d.rotate (direction);
    int size = getSize();
    float scale = size/(float)LINE_LENGTH;
    g2d.scale (scale, scale);

    // Set drawing stroke
    // ------------------
    BasicStroke savedStroke = (BasicStroke) g2d.getStroke();
    int thick = (size <= 15 ? 1 : 2);
    g2d.setStroke (new BasicStroke (thick/scale, BasicStroke.CAP_BUTT, 
      BasicStroke.JOIN_BEVEL));

    // Set minimum barb spacing
    // ------------------------
    float minSpace = (thick*2+1)/scale;
    
    // Draw station
    // ------------
    if (size > 15) {
      float radius = (thick+1)/scale;
      g2d.fill (new Ellipse2D.Float (-radius, -radius, radius*2, radius*2));
    } // if

    // Draw circle if calm
    // -------------------
    if (isCalm) {
      float radius = (thick*3)/scale;
      if (size <= 15) radius /= 2;
      g2d.draw (new Ellipse2D.Float (-radius, -radius, radius*2, radius*2));
    } // if

    // Draw line and other symbols if not calm
    // ---------------------------------------
    else {
      
      // Draw line
      // ---------
      g2d.drawLine (0, 0, LINE_LENGTH, 0);
      g2d.translate (LINE_LENGTH, 0);

      // Draw only an "x" symbol if invalid
      // ----------------------------------
      if (isInvalidSpeed) {
        float radius = (thick+1)/scale;
        g2d.draw (new Line2D.Float (-radius, -radius, radius, radius));
        g2d.draw (new Line2D.Float (radius, -radius, -radius, radius));
      } // if

      // Draw actual wind barbs
      // ----------------------
      else {

        // Draw pennants
        // -------------
        for (int i = 0; i < pennants; i++) {
          g2d.translate (-spaceArray[PENNANT], 0);
          g2d.fill (shapesArray[PENNANT]);
        } // for
    
        // Draw full barbs
        // ---------------
        for (int i = 0; i < fullBarbs; i++) {
          if (i != 0 || pennants != 0) 
            g2d.translate (-Math.max (minSpace, spaceArray[FULL_BARB]), 0);
          g2d.draw (shapesArray[FULL_BARB]);
        } // for
    
        // Draw half barbs
        // ---------------
        for (int i = 0; i < halfBarbs; i++) {
          g2d.translate (-Math.max (minSpace, spaceArray[HALF_BARB]), 0);
          g2d.draw (shapesArray[HALF_BARB]);
        } // for

      } // else

    } // else

    // Restore context
    // ---------------
    g2d.setTransform (savedTransform);
    g2d.setStroke (savedStroke);

  } // drawVector

  ////////////////////////////////////////////////////////////

  public double getMagnitude () {

    double speed = 
      ((Number) feature.getAttribute (speedAtt)).doubleValue(); 
    return (speed);

  } // getMagnitude

  ////////////////////////////////////////////////////////////

  public double getDirection () {

    double direction = 
      ((Number) feature.getAttribute (directionAtt)).doubleValue(); 
    direction = -convertAngle (Math.toRadians (direction), feature.getPoint());
    if (!getDirectionIsFrom()) direction += Math.PI;
    return (direction);

  } // getDirection

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    double speed = Double.parseDouble (argv[0]);
    double dir = Double.parseDouble (argv[1]);
    final int size = Integer.parseInt (argv[2]);
    PointFeature feature = new PointFeature (new EarthLocation (0, 0), 
      new Object[] {new Double (speed), new Double (dir)});



    // FIXME: This will no longer work the same with the new OrthographicProjection
    // class!
    

    final WindBarbSymbol symbol = new WindBarbSymbol (0, 1, SPEED_KNOTS,
       new OrthographicProjection (new EarthLocation (0, 0), 
       new int[] {512,512}, new EarthLocation (0, 0), new double[] {1, 1}));
    symbol.setBorderColor (Color.BLACK); 
    symbol.setFeature (feature);
    symbol.setSize (size);

    JFrame frame = new JFrame();
    JPanel panel = new JPanel() {
        public void paintComponent (Graphics g) {
          super.paintComponent (g);
          g.setColor (getBackground());
          Dimension dims = getSize();
          g.fillRect (0, 0, dims.width, dims.height);
          ((Graphics2D) g).setRenderingHint (RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
          symbol.draw (g, size*3/2, size*3/2);
        } // paintComponent
      };
    panel.setPreferredSize (new Dimension (size*3, size*3));
    frame.getContentPane().add (panel, BorderLayout.CENTER);
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);    
    frame.pack();
    frame.setVisible (true);

  } // main

  ////////////////////////////////////////////////////////////

} // WindBarbSymbol class

////////////////////////////////////////////////////////////////////////
