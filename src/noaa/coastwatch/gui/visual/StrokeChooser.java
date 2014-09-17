////////////////////////////////////////////////////////////////////////
/*
     FILE: StrokeChooser.java
  PURPOSE: To show a stroke chooser panel.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/25
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import noaa.coastwatch.gui.*;

/**
 * The <code>StrokeChooser</code> class is a panel that displays line
 * dash pattern and thickness controls to allow the user to select the
 * line stroke.  When the user selects a stroke, a property change
 * event is fired whose property is given by
 * <code>STROKE_PROPERTY</code>.<p>
 *
 * The chooser has some limitations in the line stroke.  Only
 * <code>Stroke</code> objects of type <code>BasicStroke</code> may be
 * used, and must have an integer line thicknesses and the dash
 * pattern given by an element of the <code>DASH_PATTERNS</code>
 * array.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class StrokeChooser
  extends JPanel {

  // Constants
  // ---------

  /** The stroke property. */
  public static String STROKE_PROPERTY = "stroke";

  /** The swatch size. */
  private static final int SWATCH_SIZE = 16;

  /** The line stroke dash patterns. */
  public static final float[][] DASH_PATTERNS = new float[][] {
    null,
    new float[] {4, 3},
    new float[] {1, 3},
    new float[] {4, 3, 1, 3},
    new float[] {4, 3, 1, 2, 1, 3},
    new float[] {4, 3, 1, 2, 1, 2, 1, 3}
  };

  // Variables
  // ---------

  /** The initial stroke chooser stroke. */
  private Stroke stroke;

  /** The dash style combo box. */
  private JComboBox dashStyleCombo;

  /** The line thickness spinner. */
  private JSpinner lineThicknessSpinner;

  /** The sample stroke pattern shown in the sample panel. */
  private StrokeSwatch sampleStroke;

  /** The sample stroke label shown in the sample panel. */
  private JLabel sampleLabel;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the dash pattern from this chooser that corresponds to the
   * dash pattern in the specified stroke.  Generally, this requires
   * that the stroke was obtained from this chooser.
   *
   * @param stroke the stroke to search.
   *
   * @return the dash pattern array, or null if not found.
   */
  public float[] getDashPattern (
    BasicStroke stroke
  ) {

    float[] strokeDashPattern = stroke.getDashArray();
    for (int i = 0; i < DASH_PATTERNS.length; i++) {
      if (Arrays.equals (strokeDashPattern, DASH_PATTERNS[i]))
        return (DASH_PATTERNS[i]);
    } // for

    throw (new IllegalArgumentException ("Invalid dash pattern"));

  } // getDashPattern

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new stroke chooser.  A simple line stroke with line
   * thickness 1 and no dashes is used initially.
   */
  public StrokeChooser () {

    this (null);

  } // StrokeChooser

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new stroke chooser with the specified initial stroke.
   *
   * @param stroke the initial stroke for the main stroke chooser. If
   * null, a simple line stroke with line thickness 1 and no dashes is
   * used.
   */
  public StrokeChooser (
    Stroke stroke
  ) {

    super (new GridBagLayout());

    // Get stroke 
    // ----------
    int lineThickness;
    float[] dashPattern;
    if (stroke == null) {
      lineThickness = 1;
      dashPattern = DASH_PATTERNS[0];
    } // if
    else {
      lineThickness = (int) Math.round (((BasicStroke) stroke).getLineWidth());
      dashPattern = getDashPattern ((BasicStroke) stroke);
    } // else
    this.stroke = getBasicStroke (lineThickness, dashPattern);

    // Create dash style combo
    // -----------------------
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 
      1, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Dash style:"), gc);
    gc.insets = new Insets (2, 0, 2, 0);

    dashStyleCombo = new JComboBox (DASH_PATTERNS);
    dashStyleCombo.setSelectedItem (dashPattern);
    dashStyleCombo.addActionListener (new DashStyleComboListener());
    dashStyleCombo.setRenderer (new DashStyleRenderer());
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.NONE, 
      0, 0);
    this.add (dashStyleCombo, gc);

    // Create line thickness spinner
    // -----------------------------
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 
      1, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Line thickness:"), gc);
    gc.insets = new Insets (2, 0, 2, 0);
   
    lineThicknessSpinner = new JSpinner (new SpinnerNumberModel (lineThickness,
      1, 10, 1));
    lineThicknessSpinner.addChangeListener (
      new LineThicknessSpinnerListener());
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.NONE, 
      0, 0);
    this.add (lineThicknessSpinner, gc);

    // Create sample panel
    // -------------------
    JPanel samplePanel = new JPanel();
    samplePanel.setBorder (new TitledBorder (new EtchedBorder(), "Sample"));
    sampleStroke = new StrokeSwatch (stroke, SWATCH_SIZE*6, SWATCH_SIZE);
    sampleLabel = new JLabel (sampleStroke);
    samplePanel.add (sampleLabel);
    GUIServices.setConstraints (gc, 0, 2, 2, 1, GridBagConstraints.HORIZONTAL, 
      1, 0);
    this.add (samplePanel, gc);

  } // StrokeChooser

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the current stroke value based on the dash pattern and
   * line thickness.
   */
  private void updateStroke () {

    int lineThickness = ((Integer) lineThicknessSpinner.getValue()).intValue();
    float[] dashPattern = (float[]) dashStyleCombo.getSelectedItem();
    setStroke (getBasicStroke (lineThickness, dashPattern));

  } // updateStroke

  ////////////////////////////////////////////////////////////

  /** Handles changes in the dash style selection. */
  private class DashStyleComboListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {
      updateStroke();
    } // actionPerformed
  } // DashStyleComboListener class

  ////////////////////////////////////////////////////////////

  /** Handles changes in the dash style selection. */
  private class LineThicknessSpinnerListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {
      updateStroke();
    } // stateChanged
  } // LineThicknessSpinnerListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new stroke object of the specified line width and dash
   * pattern index.  In order for the resulting stroke to be
   * compatible with the stroke chooser, the line width must be in the
   * range [1..10] and the dash pattern must be an entry in the
   * <code>DASH_PATTERNS</code> array.
   *
   * @param lineWidth the line width.
   * @param dashPattern a dash pattern from the
   * <code>DASH_PATTERNS</code> array.
   *
   * @return a new stroke object.
   */
  public static  Stroke getBasicStroke (
    int lineWidth,
    float[] dashPattern
  ) {

    return (new BasicStroke (lineWidth, BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));

  } // getBasicStroke

  ////////////////////////////////////////////////////////////

  /** Renders the dash styles as stroke swatches. */
  private class DashStyleRenderer
    extends JLabel
    implements ListCellRenderer {

    ////////////////////////////////////////////////////////

    /** Creates a new opaque dash style renderer. */
    public DashStyleRenderer () {

      setOpaque (true);

    } // DashStyleRenderer constructor

    ////////////////////////////////////////////////////////

    /** Sets this label to show a stroke swatch icon. */
    public Component getListCellRendererComponent (
      JList list,
      Object value,
      int index,
      boolean isSelected,
      boolean cellHasFocus
    ) {

      setIcon (new StrokeSwatch (getBasicStroke (2, (float[]) value),
        SWATCH_SIZE*3, SWATCH_SIZE));
      if (isSelected) {
        setBackground (list.getSelectionBackground());
        setForeground (list.getSelectionForeground());
      } // if
      else {
        setBackground (list.getBackground());
        setForeground (list.getForeground());
      }  // else
      return (this);

    } // getListCellRendererComponent

    ////////////////////////////////////////////////////////

  } // DashStyleRenderer class

  ////////////////////////////////////////////////////////////

  /** Gets the stroke selected by this stroke chooser. */
  public Stroke getStroke () { return (stroke); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the stroke selected by this stroke chooser.  The stroke may
   * be null, in which case this method has no effect.  The stroke
   * must be a stroke object obtained from the
   * <code>getBasicStroke()</code> method in order to be compatible
   * with the chooser.
   */
  public void setStroke (Stroke newStroke) { 

    if (newStroke != null && !newStroke.equals (stroke)) {

      // Set stroke
      // ----------
      int lineThickness = 
        (int) Math.round (((BasicStroke) newStroke).getLineWidth());
      float[] dashPattern = getDashPattern ((BasicStroke) newStroke);
      this.stroke = getBasicStroke (lineThickness, dashPattern);

      // Modify components
      // -----------------
      dashStyleCombo.setSelectedItem (dashPattern);
      lineThicknessSpinner.setValue (new Integer (lineThickness));

      // Update sample
      // -------------
      sampleStroke.setStroke (stroke);
      sampleLabel.repaint();

      // Fire property change
      // --------------------
      firePropertyChange (STROKE_PROPERTY, null, stroke);

    } // if

  } // setStroke

  ////////////////////////////////////////////////////////////
  
  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new StrokeChooser (new BasicStroke());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // StrokeChooser

////////////////////////////////////////////////////////////////////////

