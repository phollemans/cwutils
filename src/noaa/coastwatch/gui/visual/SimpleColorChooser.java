////////////////////////////////////////////////////////////////////////
/*

     File: SimpleColorChooser.java
   Author: Peter Hollemans
     Date: 2004/02/23

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
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.im.InputContext;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import noaa.coastwatch.gui.TestContainer;

/**
 * The <code>SimpleColorChooser</code> class is a panel that displays
 * a simple grid of colors from which to choose, with a button to
 * display a full <code>JColorChooser</code> dialog.  When the user
 * selects a color, either from the simple grid or from the full Swing
 * color chooser, a property change event is fired whose property is
 * given by <code>COLOR_PROPERTY</code>.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class SimpleColorChooser
  extends JPanel {

  // Constants
  // ---------

  /** The color property. */
  public static String COLOR_PROPERTY = "color";

  /** The swatch size. */
  private static final int SWATCH_SIZE = 15;

  /** The gap size. */
  private static final int GAP_SIZE = 2;

  private static final int RAW_ROWS = 9;
  private static final int RAW_COLS = 13;

  // Variables
  // ---------

  /** The initial color chooser color. */
  private Color color;

  /** The swatch colors. */
  private Color[] colors;

  /** The number of swatch rows. */
  private int rows;

  /** The number of swatch columns. */
  private int cols;

  /** The button used for activating the JColorChooser. */
  protected JButton chooserButton;

  /** 
   * The full color chooser panel.  The panel is shared across all
   * instances of this chooser.
   */
  private static JColorChooser chooserPanel = new JColorChooser();

  /** The dialog used to display the full color chooser. */
  private JDialog chooserDialog;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new simple color chooser with the specified number of
   * swatch rows and columns.
   *
   * @param rows the number of swatch rows.
   * @param cols the number of swatch columns.
   * @param color the initial color for the main color chooser.
   */
  public SimpleColorChooser (
    int rows,
    int cols,
    Color color
  ) {

    super (new BorderLayout (2, 2));

    if (rows > RAW_ROWS)
      throw new IllegalArgumentException ("Rows " + rows + " greater than maximum " + RAW_ROWS);
    if (cols > RAW_COLS)
      throw new IllegalArgumentException ("Cols " + rows + " greater than maximum " + RAW_COLS);

    // Setup colors
    // ------------
    this.rows = rows;
    this.cols = cols;
    this.color = color;
    initColors();

    // Create swatch panel
    // -------------------
    JPanel swatchPanel = new SwatchPanel();
    this.add (swatchPanel, BorderLayout.CENTER);

    // Create button
    // -------------
    /**
     * We override getInputContext() here because of Java 1.5 bug
     * 5036146 that eats mouse events in popup windows so that the
     * action listener in the JButton is never called.
     */
    chooserButton = new JButton ("Other...") {
      public java.awt.im.InputContext getInputContext() { return (null); }
    };
    chooserButton.addActionListener (new ActionListener() {
      public void actionPerformed (ActionEvent event) {
        Color c = getColor();
        if (c != null) chooserPanel.setColor (c);
        chooserDialog = JColorChooser.createDialog (
          SimpleColorChooser.this, "Select a color", true, chooserPanel, 
          new ActionListener() {
            public void actionPerformed (ActionEvent event) {
              chooserDialog.dispose();
              setColor (chooserPanel.getColor());
            } // actionPerformed
          }, null);
        chooserDialog.setVisible (true);
      } // actionPerformed
    });
    this.add (chooserButton, BorderLayout.SOUTH);

  } // SimpleColorChooser

  ////////////////////////////////////////////////////////////

  /**
   * Gets the color selected by this color chooser.
   *
   *
   * @return the color selected by this color chooser.
   */
  public Color getColor () { return (color); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the color selected by this color chooser.  The color may be
   * null, in which case this method has no effect.
   *
   * @param newColor the color to select.
   *
   */
  public void setColor (Color newColor) { 

    if ((newColor == null && color != null) || 
      (newColor != null && !newColor.equals (color))) {
      color = newColor;
      firePropertyChange (COLOR_PROPERTY, null, color);
    } // if

  } // setColor

  ////////////////////////////////////////////////////////////

  /**
   * The <code>SwatchPanel</code> class shows a grid of colors and
   * allows the user to click on one of the colors to select it.
   */
  private class SwatchPanel 
    extends JPanel 
    implements MouseListener {

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new swatch panel and adds a mouse listener for click
     * events. 
     */
    public SwatchPanel () {

      addMouseListener (this);

    } // SwatchPanel

    ////////////////////////////////////////////////////////
 
    /** 
     * Gets the preferred panel size based on swatch and gap
     * sizes. 
     */
    public Dimension getPreferredSize () {

      int width = cols * (SWATCH_SIZE + GAP_SIZE) - 1;
      int height = rows * (SWATCH_SIZE + GAP_SIZE) - 1;
      return (new Dimension (width, height));

    } // getPreferredSize

    ////////////////////////////////////////////////////////

    /** Paints the swatch panel. */
    public void paintComponent (Graphics g) {

      // Fill background
      // ---------------
      g.setColor (getBackground());
      g.fillRect (0, 0, getWidth(), getHeight());

      // Draw small swatches
      // -------------------
      for (int row = 0; row < rows; row++) {
        for (int col = 0; col < cols; col++) {
          Color c = colors [row*cols + col];
          int x = col * (SWATCH_SIZE + GAP_SIZE);
          int y = row * (SWATCH_SIZE + GAP_SIZE);
          if (c != null) {
            g.setColor (c);
            g.fillRect (x, y, SWATCH_SIZE, SWATCH_SIZE);
          } // if
          else {
            g.setColor (getForeground());
            g.drawLine (x+SWATCH_SIZE-1, y, x, y+SWATCH_SIZE-1);
          } // else
          g.setColor (getForeground());
          g.drawLine (x+SWATCH_SIZE-1, y, x+SWATCH_SIZE-1, y+SWATCH_SIZE-1);
          g.drawLine (x, y+SWATCH_SIZE-1, x+SWATCH_SIZE-1, y+SWATCH_SIZE-1);
        } // for
      } // for

    } // paintComponent

    ////////////////////////////////////////////////////////

    /** 
     * Responds to a mouse click by setting the newly chosen color.
     */
    public void mouseClicked (MouseEvent event) { 

      // Get color
      // ---------
      int row = event.getY() / (SWATCH_SIZE + GAP_SIZE);
      int col = event.getX() / (SWATCH_SIZE + GAP_SIZE);
      if (row > rows-1 || col > cols-1) return;
      Color c = colors[row*cols + col];

      // Fire event
      // ----------
      setColor (c);

    } // mouseClicked

    ////////////////////////////////////////////////////////

    public void	mouseEntered (MouseEvent event) { }
    public void mouseExited (MouseEvent event) { }
    public void mousePressed (MouseEvent event) { }
    public void mouseReleased (MouseEvent event) { }

    ////////////////////////////////////////////////////////

  } // SwatchPanel class

  ////////////////////////////////////////////////////////////

  /** Initializes the swatch colors. */
  private void initColors () {

    // Get number of colors
    // --------------------
    int numColors = rows*cols;

    // Calculate row and column skip factors
    // -------------------------------------
    int rowSkip = (RAW_ROWS+1) / rows;
    int colSkip = (RAW_COLS+1) / cols;

    // Create color subset
    // -------------------
    int[] rawValues = getRawValues();
    colors = new Color[numColors];
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        int index = (row*rowSkip)*RAW_COLS + (col*colSkip);
        colors[row*cols + col] = new Color (rawValues[(index*3)], 
          rawValues[(index*3)+1], rawValues[(index*3)+2]);
      } // for
    } // for

    // Set null color
    // --------------
    colors[colors.length-1] = null;

  } // initColors

  ////////////////////////////////////////////////////////////
  
  /** Gets the raw color values. */
  private int[] getRawValues() {

    int[] rawValues = {

      // Row 1
      254, 254, 254,
      203, 235, 253,
      208, 221, 253,
      210, 194, 250,
      231, 196, 252,
      241, 206, 219,
      248, 215, 211,
      249, 223, 209,
      250, 233, 207,
      251, 240, 209,
      252, 251, 217,
      245, 248, 215,
      220, 234, 207,

      // Row 2
      224, 224, 224,
      154, 219, 250,
      164, 189, 251,
      165, 130, 248,
      212, 139, 249,
      230, 158, 183,
      243, 176, 166,
      245, 192, 163,
      247, 213, 161,
      248, 225, 162,
      252, 250, 180,
      239, 245, 178,
      201, 227, 175,

      // Row 3
      194, 194, 194,
      108, 204, 248,
      118, 155, 250,
      121, 72, 247,
      193, 86, 248,
      220, 110, 146,
      238, 136, 121,
      240, 160, 117,
      244, 194, 114,
      246, 213, 116,
      251, 248, 144,
      230, 239, 139,
      174, 214, 133,

      // Row 4
      164, 164, 164,
      79, 187, 247,
      76, 121, 248,
      86, 44, 226,
      171, 60, 235,
      208, 68, 110,
      235, 99, 75,
      237, 130, 70,
      240, 175, 68,
      243, 198, 70,
      250, 246, 108,
      223, 235, 102,
      148, 202, 93,

      // Row 5
      136, 136, 136,
      63, 147, 207,
      49, 84, 247,
      68, 33, 164,
      133, 46, 175,
      162, 52, 82,
      234, 75, 33,
      236, 106, 30,
      240, 166, 40,
      241, 194, 45,
      250, 250, 79,
      212, 232, 70,
      118, 176, 66,

      // Row 6
      108, 108, 108,
      53, 126, 168,
      42, 74, 203,
      50, 26, 133,
      104, 37, 144,
      131, 43, 69,
      203, 54, 21,
      196, 82, 24,
      192, 124, 31,
      193, 149, 35,
      238, 233, 52,
      187, 201, 49,
      99, 144, 54,

      // Row 7
      81, 81, 81,
      41, 96, 130,
      32, 57, 155,
      40, 14, 105,
      81, 28, 110,
      101, 33, 53, 
      157, 42, 16,
      151, 62, 19,
      149, 96, 24,
      148, 114, 27,
      184, 180, 40,
      144, 154, 37,
      75, 109, 41,

      // Row 8
      56, 56, 56,
      28, 66, 89,
      23, 40, 109,
      26, 13, 70,
      57, 19, 77,
      69, 23, 36,
      110, 30, 9,
      103, 42, 11,
      104, 67, 17,
      103, 79, 19,
      127, 123, 28,
      99, 106, 27,
      53, 76, 29,

      // Row 9
      0, 0, 0,
      20, 47, 64,
      16, 26, 75,
      19, 7, 50,
      38, 12, 52,
      48, 15, 25,
      75, 19, 6,
      73, 30, 6,
      73, 47, 9,
      72, 54, 12,
      89, 86, 19,
      69, 75, 18,
      37, 54, 20

      // 255, 255, 255, // first row
      // 204, 255, 255,
      // 204, 204, 255,
      // 204, 204, 255,
      // 204, 204, 255,
      // 204, 204, 255,
      // 204, 204, 255,
      // 204, 204, 255,
      // 204, 204, 255,
      // 204, 204, 255,
      // 204, 204, 255,
      // 255, 204, 255,
      // 255, 204, 204,
      // 255, 204, 204,
      // 255, 204, 204,
      // 255, 204, 204,
      // 255, 204, 204,
      // 255, 204, 204,
      // 255, 204, 204,
      // 255, 204, 204,
      // 255, 204, 204,
      // 255, 255, 204,
      // 204, 255, 204,
      // 204, 255, 204,
      // 204, 255, 204,
      // 204, 255, 204,
      // 204, 255, 204,
      // 204, 255, 204,
      // 204, 255, 204,
      // 204, 255, 204,
      // 204, 255, 204,

      // 204, 204, 204,  // second row.
      // 153, 255, 255,
      // 153, 204, 255,
      // 153, 153, 255,
      // 153, 153, 255,
      // 153, 153, 255,
      // 153, 153, 255,
      // 153, 153, 255,
      // 153, 153, 255,
      // 153, 153, 255,
      // 204, 153, 255,
      // 255, 153, 255,
      // 255, 153, 204,
      // 255, 153, 153,
      // 255, 153, 153,
      // 255, 153, 153,
      // 255, 153, 153,
      // 255, 153, 153,
      // 255, 153, 153,
      // 255, 153, 153,
      // 255, 204, 153,
      // 255, 255, 153,
      // 204, 255, 153,
      // 153, 255, 153,
      // 153, 255, 153,
      // 153, 255, 153,
      // 153, 255, 153,
      // 153, 255, 153,
      // 153, 255, 153,
      // 153, 255, 153,
      // 153, 255, 204,

      // 204, 204, 204,  // third row
      // 102, 255, 255,
      // 102, 204, 255,
      // 102, 153, 255,
      // 102, 102, 255,
      // 102, 102, 255,
      // 102, 102, 255,
      // 102, 102, 255,
      // 102, 102, 255,
      // 153, 102, 255,
      // 204, 102, 255,
      // 255, 102, 255,
      // 255, 102, 204,
      // 255, 102, 153,
      // 255, 102, 102,
      // 255, 102, 102,
      // 255, 102, 102,
      // 255, 102, 102,
      // 255, 102, 102,
      // 255, 153, 102,
      // 255, 204, 102,
      // 255, 255, 102,
      // 204, 255, 102,
      // 153, 255, 102,
      // 102, 255, 102,
      // 102, 255, 102,
      // 102, 255, 102,
      // 102, 255, 102,
      // 102, 255, 102,
      // 102, 255, 153,
      // 102, 255, 204,

      // 153, 153, 153, // fourth row
      // 51, 255, 255,
      // 51, 204, 255,
      // 51, 153, 255,
      // 51, 102, 255,
      // 51, 51, 255,
      // 51, 51, 255,
      // 51, 51, 255,
      // 102, 51, 255,
      // 153, 51, 255,
      // 204, 51, 255,
      // 255, 51, 255,
      // 255, 51, 204,
      // 255, 51, 153,
      // 255, 51, 102,
      // 255, 51, 51,
      // 255, 51, 51,
      // 255, 51, 51,
      // 255, 102, 51,
      // 255, 153, 51,
      // 255, 204, 51,
      // 255, 255, 51,
      // 204, 255, 51,
      // 153, 255, 51,
      // 102, 255, 51,
      // 51, 255, 51,
      // 51, 255, 51,
      // 51, 255, 51,
      // 51, 255, 102,
      // 51, 255, 153,
      // 51, 255, 204,

      // 153, 153, 153, // fifth row
      // 0, 255, 255,
      // 0, 204, 255,
      // 0, 153, 255,
      // 0, 102, 255,
      // 0, 51, 255,
      // 0, 0, 255,
      // 51, 0, 255,
      // 102, 0, 255,
      // 153, 0, 255,
      // 204, 0, 255,
      // 255, 0, 255,
      // 255, 0, 204,
      // 255, 0, 153,
      // 255, 0, 102,
      // 255, 0, 51,
      // 255, 0 , 0,
      // 255, 51, 0,
      // 255, 102, 0,
      // 255, 153, 0,
      // 255, 204, 0,
      // 255, 255, 0,
      // 204, 255, 0,
      // 153, 255, 0,
      // 102, 255, 0,
      // 51, 255, 0,
      // 0, 255, 0,
      // 0, 255, 51,
      // 0, 255, 102,
      // 0, 255, 153,
      // 0, 255, 204,

      // 102, 102, 102, // sixth row
      // 0, 204, 204,
      // 0, 204, 204,
      // 0, 153, 204,
      // 0, 102, 204,
      // 0, 51, 204,
      // 0, 0, 204,
      // 51, 0, 204,
      // 102, 0, 204,
      // 153, 0, 204,
      // 204, 0, 204,
      // 204, 0, 204,
      // 204, 0, 204,
      // 204, 0, 153,
      // 204, 0, 102,
      // 204, 0, 51,
      // 204, 0, 0,
      // 204, 51, 0,
      // 204, 102, 0,
      // 204, 153, 0,
      // 204, 204, 0,
      // 204, 204, 0,
      // 204, 204, 0,
      // 153, 204, 0,
      // 102, 204, 0,
      // 51, 204, 0,
      // 0, 204, 0,
      // 0, 204, 51,
      // 0, 204, 102,
      // 0, 204, 153,
      // 0, 204, 204, 

      // 102, 102, 102, // seventh row
      // 0, 153, 153,
      // 0, 153, 153,
      // 0, 153, 153,
      // 0, 102, 153,
      // 0, 51, 153,
      // 0, 0, 153,
      // 51, 0, 153,
      // 102, 0, 153,
      // 153, 0, 153,
      // 153, 0, 153,
      // 153, 0, 153,
      // 153, 0, 153,
      // 153, 0, 153,
      // 153, 0, 102,
      // 153, 0, 51,
      // 153, 0, 0,
      // 153, 51, 0,
      // 153, 102, 0,
      // 153, 153, 0,
      // 153, 153, 0,
      // 153, 153, 0,
      // 153, 153, 0,
      // 153, 153, 0,
      // 102, 153, 0,
      // 51, 153, 0,
      // 0, 153, 0,
      // 0, 153, 51,
      // 0, 153, 102,
      // 0, 153, 153,
      // 0, 153, 153,

      // 51, 51, 51, // eigth row
      // 0, 102, 102,
      // 0, 102, 102,
      // 0, 102, 102,
      // 0, 102, 102,
      // 0, 51, 102,
      // 0, 0, 102,
      // 51, 0, 102,
      // 102, 0, 102,
      // 102, 0, 102,
      // 102, 0, 102,
      // 102, 0, 102,
      // 102, 0, 102,
      // 102, 0, 102,
      // 102, 0, 102,
      // 102, 0, 51,
      // 102, 0, 0,
      // 102, 51, 0,
      // 102, 102, 0,
      // 102, 102, 0,
      // 102, 102, 0,
      // 102, 102, 0,
      // 102, 102, 0,
      // 102, 102, 0,
      // 102, 102, 0,
      // 51, 102, 0,
      // 0, 102, 0,
      // 0, 102, 51,
      // 0, 102, 102,
      // 0, 102, 102,
      // 0, 102, 102,

      // 0, 0, 0, // ninth row
      // 0, 51, 51,
      // 0, 51, 51,
      // 0, 51, 51,
      // 0, 51, 51,
      // 0, 51, 51,
      // 0, 0, 51,
      // 51, 0, 51,
      // 51, 0, 51,
      // 51, 0, 51,
      // 51, 0, 51,
      // 51, 0, 51,
      // 51, 0, 51,
      // 51, 0, 51,
      // 51, 0, 51,
      // 51, 0, 51,
      // 51, 0, 0,
      // 51, 51, 0,
      // 51, 51, 0,
      // 51, 51, 0,
      // 51, 51, 0,
      // 51, 51, 0,
      // 51, 51, 0,
      // 51, 51, 0,
      // 51, 51, 0,
      // 0, 51, 0,
      // 0, 51, 51,
      // 0, 51, 51,
      // 0, 51, 51,
      // 0, 51, 51,
      // 51, 51, 51 

    };

    return (rawValues);

  } // getRawValues

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    int width = Integer.parseInt (argv[0]);
    int height = Integer.parseInt (argv[1]);
    JPanel panel = new SimpleColorChooser (height, width, null);
    panel.addPropertyChangeListener (COLOR_PROPERTY, 
      new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          System.out.println ("new color = " + event.getNewValue());
        } // propertyChange
      });
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // SimpleColorChooser

////////////////////////////////////////////////////////////////////////
