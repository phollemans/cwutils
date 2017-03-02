////////////////////////////////////////////////////////////////////////
/*
     FILE: SimpleColorChooser.java
  PURPOSE: To show a simple grid of colors to choose from.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/23
  CHANGES: 2005/09/12, PFH, added workaround for JRE popup menu bug
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

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
  private static final int SWATCH_SIZE = 10;

  /** The gap size. */
  private static final int GAP_SIZE = 1;

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

  /** Gets the color selected by this color chooser. */
  public Color getColor () { return (color); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the color selected by this color chooser.  The color may be
   * null, in which case this method has no effect.
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
    int rowSkip = 10 / rows;
    int colSkip = 32 / cols;

    // Create color subset
    // -------------------
    int[] rawValues = getRawValues();
    colors = new Color[numColors];
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        int index = (row*rowSkip)*31 + (col*colSkip);
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
      255, 255, 255, // first row
      204, 255, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      204, 204, 255,
      255, 204, 255,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 204, 204,
      255, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 255, 204,
      204, 204, 204,  // second row.
      153, 255, 255,
      153, 204, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      153, 153, 255,
      204, 153, 255,
      255, 153, 255,
      255, 153, 204,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 153, 153,
      255, 204, 153,
      255, 255, 153,
      204, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 153,
      153, 255, 204,
      204, 204, 204,  // third row
      102, 255, 255,
      102, 204, 255,
      102, 153, 255,
      102, 102, 255,
      102, 102, 255,
      102, 102, 255,
      102, 102, 255,
      102, 102, 255,
      153, 102, 255,
      204, 102, 255,
      255, 102, 255,
      255, 102, 204,
      255, 102, 153,
      255, 102, 102,
      255, 102, 102,
      255, 102, 102,
      255, 102, 102,
      255, 102, 102,
      255, 153, 102,
      255, 204, 102,
      255, 255, 102,
      204, 255, 102,
      153, 255, 102,
      102, 255, 102,
      102, 255, 102,
      102, 255, 102,
      102, 255, 102,
      102, 255, 102,
      102, 255, 153,
      102, 255, 204,
      153, 153, 153, // fourth row
      51, 255, 255,
      51, 204, 255,
      51, 153, 255,
      51, 102, 255,
      51, 51, 255,
      51, 51, 255,
      51, 51, 255,
      102, 51, 255,
      153, 51, 255,
      204, 51, 255,
      255, 51, 255,
      255, 51, 204,
      255, 51, 153,
      255, 51, 102,
      255, 51, 51,
      255, 51, 51,
      255, 51, 51,
      255, 102, 51,
      255, 153, 51,
      255, 204, 51,
      255, 255, 51,
      204, 255, 51,
      153, 244, 51,
      102, 255, 51,
      51, 255, 51,
      51, 255, 51,
      51, 255, 51,
      51, 255, 102,
      51, 255, 153,
      51, 255, 204,
      153, 153, 153, // fifth row
      0, 255, 255,
      0, 204, 255,
      0, 153, 255,
      0, 102, 255,
      0, 51, 255,
      0, 0, 255,
      51, 0, 255,
      102, 0, 255,
      153, 0, 255,
      204, 0, 255,
      255, 0, 255,
      255, 0, 204,
      255, 0, 153,
      255, 0, 102,
      255, 0, 51,
      255, 0 , 0,
      255, 51, 0,
      255, 102, 0,
      255, 153, 0,
      255, 204, 0,
      255, 255, 0,
      204, 255, 0,
      153, 255, 0,
      102, 255, 0,
      51, 255, 0,
      0, 255, 0,
      0, 255, 51,
      0, 255, 102,
      0, 255, 153,
      0, 255, 204,
      102, 102, 102, // sixth row
      0, 204, 204,
      0, 204, 204,
      0, 153, 204,
      0, 102, 204,
      0, 51, 204,
      0, 0, 204,
      51, 0, 204,
      102, 0, 204,
      153, 0, 204,
      204, 0, 204,
      204, 0, 204,
      204, 0, 204,
      204, 0, 153,
      204, 0, 102,
      204, 0, 51,
      204, 0, 0,
      204, 51, 0,
      204, 102, 0,
      204, 153, 0,
      204, 204, 0,
      204, 204, 0,
      204, 204, 0,
      153, 204, 0,
      102, 204, 0,
      51, 204, 0,
      0, 204, 0,
      0, 204, 51,
      0, 204, 102,
      0, 204, 153,
      0, 204, 204, 
      102, 102, 102, // seventh row
      0, 153, 153,
      0, 153, 153,
      0, 153, 153,
      0, 102, 153,
      0, 51, 153,
      0, 0, 153,
      51, 0, 153,
      102, 0, 153,
      153, 0, 153,
      153, 0, 153,
      153, 0, 153,
      153, 0, 153,
      153, 0, 153,
      153, 0, 102,
      153, 0, 51,
      153, 0, 0,
      153, 51, 0,
      153, 102, 0,
      153, 153, 0,
      153, 153, 0,
      153, 153, 0,
      153, 153, 0,
      153, 153, 0,
      102, 153, 0,
      51, 153, 0,
      0, 153, 0,
      0, 153, 51,
      0, 153, 102,
      0, 153, 153,
      0, 153, 153,
      51, 51, 51, // eigth row
      0, 102, 102,
      0, 102, 102,
      0, 102, 102,
      0, 102, 102,
      0, 51, 102,
      0, 0, 102,
      51, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 102,
      102, 0, 51,
      102, 0, 0,
      102, 51, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      102, 102, 0,
      51, 102, 0,
      0, 102, 0,
      0, 102, 51,
      0, 102, 102,
      0, 102, 102,
      0, 102, 102,
      0, 0, 0, // ninth row
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      0, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 51,
      51, 0, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      51, 51, 0,
      0, 51, 0,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      0, 51, 51,
      51, 51, 51 };

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

