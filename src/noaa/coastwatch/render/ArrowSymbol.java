////////////////////////////////////////////////////////////////////////
/*
     FILE: ArrowSymbol.java
  PURPOSE: Renders a point feature as an arrow.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/04
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

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
import java.awt.geom.GeneralPath;
import javax.swing.JFrame;
import javax.swing.JPanel;
import noaa.coastwatch.render.DirectionSymbol;
import noaa.coastwatch.render.PointFeature;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform2D;

/**
 * A <code>ArrowSymbol</code> is a <code>PointFeatureSymbol</code>
 * that renders an arrow pointing in the direction of a vector quantity.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class ArrowSymbol
  extends DirectionSymbol {
  
  // Constants
  // ---------

  /** The length of the main line. */
  private static final int LINE_LENGTH = 4;

  /** The size of the arrow head. */
  private static final int ARROW_SIZE = 1;

  // Variables
  // ---------

  /** The feature attributes for the vector components. */
  protected int[] componentAtts;

  /** 
   * The magnitude-direction flag, true if the feature attributes are
   * [magnitude, direction], false if they are [u, v].
   */
  protected boolean isMagDir;

  /** The shapes of the arrow. */
  private static Shape arrowShape;

  ////////////////////////////////////////////////////////////

  static {

    // Create arrow shape
    // ------------------
    GeneralPath arrowPath = new GeneralPath();
    arrowPath.moveTo (0.0f, 0.0f);
    arrowPath.lineTo (LINE_LENGTH, 0.0f);
    arrowPath.moveTo (LINE_LENGTH-ARROW_SIZE, -ARROW_SIZE);
    arrowPath.lineTo (LINE_LENGTH, 0.0f);
    arrowPath.lineTo (LINE_LENGTH-ARROW_SIZE, +ARROW_SIZE);
    arrowShape = arrowPath;

  } // static

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new arrow symbol based on U and V components.
   *
   * @param uComponentAtt the feature attribute for U component.
   * @param vComponentAtt the feature attribute for V component.
   */
  public ArrowSymbol (
    int uComponentAtt,
    int vComponentAtt
  ) {

    super (null);
    componentAtts = new int[] {uComponentAtt, vComponentAtt};

  } // ArrowSymbol constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new arrow symbol based on magnitude and direction components.
   *
   * @param magnitudeAtt the feature attribute for vector magnitude.
   * @param directionAtt the feature attribute for vector direction.
   * @param trans the earth transform used for converting directions.
   */
  public ArrowSymbol (
    int magnitudeAtt,
    int directionAtt,
    EarthTransform2D trans
  ) {

    super (trans);
    componentAtts = new int[] {magnitudeAtt, directionAtt};
    isMagDir = true;

  } // ArrowSymbol constructor

  ////////////////////////////////////////////////////////////

  public void drawVector (
    Graphics gc, 
    int x, 
    int y,
    double magnitude,
    double direction
  ) {

    // Check angle
    // -----------
    if (Double.isNaN (direction)) return;

    // Transform, rotate, and scale origin
    // -----------------------------------
    Graphics2D g2d = (Graphics2D) gc;
    AffineTransform savedTransform = g2d.getTransform();
    g2d.translate (x, y);
    g2d.rotate (direction);
    int size = getSize();
    float scale = size/(float)LINE_LENGTH;
    g2d.scale (scale, scale);

    // Set drawing stroke
    // ------------------
    BasicStroke savedStroke = (BasicStroke) g2d.getStroke();
    int thick = (size <= 15 ? 1 : 2);
    g2d.setStroke (new BasicStroke (thick/scale, BasicStroke.CAP_BUTT, 
      BasicStroke.JOIN_BEVEL));

    // Draw arrow
    // ----------
    g2d.draw (arrowShape);

    // Restore context
    // ---------------
    g2d.setTransform (savedTransform);
    g2d.setStroke (savedStroke);

  } // drawVector

  ////////////////////////////////////////////////////////////

  public double getMagnitude () {

    double magnitude;

    // Get magnitude directly
    // ----------------------
    if (isMagDir) {
      magnitude = 
        ((Number) feature.getAttribute (componentAtts[0])).doubleValue(); 
    } // if

    // Compute magnitude
    // -----------------
    else {
      double uComponent = 
        ((Number) feature.getAttribute (componentAtts[0])).doubleValue(); 
      double vComponent = 
        ((Number) feature.getAttribute (componentAtts[1])).doubleValue(); 
      magnitude = Math.sqrt (uComponent*uComponent + vComponent*vComponent);
    } // else

    return (magnitude);

  } // getMagnitude

  ////////////////////////////////////////////////////////////

  public double getDirection () {

    double direction;

    // Get direction and compute relative to transform
    // -----------------------------------------------
    if (isMagDir) {
      direction = 
        ((Number) feature.getAttribute (componentAtts[1])).doubleValue(); 
      direction = -convertAngle (Math.toRadians (direction), 
        feature.getPoint());
      if (!getDirectionIsFrom()) direction += Math.PI;
    } // if

    // Compute direction from components
    // ---------------------------------
    else {
      double uComponent = 
        ((Number) feature.getAttribute (componentAtts[0])).doubleValue(); 
      double vComponent = 
        ((Number) feature.getAttribute (componentAtts[1])).doubleValue(); 
      direction = -Math.atan2 (vComponent, uComponent);
    } // else

    return (direction);

  } // getDirection

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    double u = Double.parseDouble (argv[0]);
    double v = Double.parseDouble (argv[1]);
    final int size = Integer.parseInt (argv[2]);
    PointFeature feature = new PointFeature (new EarthLocation (0, 0), 
      new Object[] {new Double (u), new Double (v)});
    final ArrowSymbol symbol = new ArrowSymbol (0, 1);
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

} // ArrowSymbol class

////////////////////////////////////////////////////////////////////////
