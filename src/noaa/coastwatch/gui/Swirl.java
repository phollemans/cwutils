////////////////////////////////////////////////////////////////////////
/*
     FILE: Swirl.java
  PURPOSE: Shows a swirling progress icon.
   AUTHOR: Peter Hollemans
     DATE: 2006/04/08
  CHANGES: n/a

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
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The <code>Swirl</code> class shows a swirling progress icon in a
 * panel.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class Swirl
  extends JPanel {

  // Constants
  // ---------

  /** The swirl spoke colors. */
  private static final Color[] SPOKE_COLORS = new Color[] {
    new Color (0, 0, 0, 255),
    new Color (0, 0, 0, 223),
    new Color (0, 0, 0, 191),
    new Color (0, 0, 0, 159),
    new Color (0, 0, 0, 127),
    new Color (0, 0, 0, 95),
    new Color (0, 0, 0, 95),
    new Color (0, 0, 0, 95),
    new Color (0, 0, 0, 95),
    new Color (0, 0, 0, 95),
    new Color (0, 0, 0, 95),
    new Color (0, 0, 0, 95)
  };

  /** The number of spokes in the swirl. */
  private static final int SPOKES = SPOKE_COLORS.length;

  /** The rotation angle for each spoke. */
  private static final float ROTATION = (float) (Math.PI*2/SPOKES);

  // Variables
  // ---------

  /** The current rotation spoke count for the swirl highlight. */
  private int rotationSpokes;

  /** The timer for swirl actions. */
  private Timer timer;

  /** The current timeout for spoke rotation, determines the speed. */
  private int timeout;

  ////////////////////////////////////////////////////////////

  /** Creates a new swirl. */
  public Swirl () {

    setSpeed (2);

  } // Swirl constructor

  ////////////////////////////////////////////////////////////

  public void paintComponent (Graphics g1d) {

    // Paint the background
    // --------------------
    super.paintComponent (g1d);
    Graphics2D g = (Graphics2D) g1d;

    // Get size and shape
    // ------------------
    Dimension size = getSize();
    Point2D.Float center = new Point2D.Float (size.width/2.f, size.height/2.f);
    float radius = Math.min (size.width, size.height)/2.f;

    // Create spoke line
    // -----------------
    Point2D innerPoint = new Point2D.Float (center.x+radius/2.f, center.y);
    Point2D outerPoint = new Point2D.Float (center.x+radius*(5.f/6), center.y);
    Line2D line = new Line2D.Float (innerPoint, outerPoint);

    // Setup graphics
    // --------------
    g.setStroke (new BasicStroke (radius/6, BasicStroke.CAP_ROUND, 
      BasicStroke.JOIN_ROUND));
    g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
      RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint (RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE);

    // Draw spokes
    // -----------
    g.rotate (Math.PI*2*(0.75 + (float) rotationSpokes/SPOKES), center.x, 
      center.y);
    for (int i = 0; i < SPOKES; i++) {
      g.setColor (SPOKE_COLORS[i]);
      g.draw (line);
      g.rotate (-ROTATION, center.x, center.y);
    } // for

  } // paintComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the swirl speed, by default two rotations per second.
   * 
   * @param rotations the number of rotations per second in the
   * range [0.1 ... 10].
   */
  public void setSpeed (
    double rotations
  ) {

    if (rotations < 0.1) rotations = 0.1;
    else if (rotations > 10) rotations = 10;
    timeout = (int) (1000/SPOKES/rotations);

  } // setSpeed

  ////////////////////////////////////////////////////////////

  /** Starts the swirling motion to indicate progress. */
  public void start () {

    // Create timer
    // ------------
    if (timer == null) {
      ActionListener taskPerformer = new ActionListener() {
          public void actionPerformed (ActionEvent evt) {
            rotationSpokes = (rotationSpokes+1)%SPOKES;
            if (isShowing()) repaint();
          } // actionPerformed
        };
      timer = new Timer (timeout, taskPerformer);
    } // if

    // Start timer
    // -----------
    timer.start();

  } // start

  ////////////////////////////////////////////////////////////

  /** Stops the swirling motion. */
  public void stop () {
    
    if (timer != null) {
      timer.stop();
      rotationSpokes = 0;
      if (isShowing()) repaint();
    } // if      

  } // stop

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    Swirl swirl = new Swirl();
    swirl.setPreferredSize (new Dimension (100, 100));
    swirl.start();

    JFrame frame = new JFrame();
    frame.getContentPane().add (swirl, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible (true);
    
  } // main

  ////////////////////////////////////////////////////////////

} // Swirl class

////////////////////////////////////////////////////////////////////////
