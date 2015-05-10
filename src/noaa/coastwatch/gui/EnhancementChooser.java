////////////////////////////////////////////////////////////////////////
/*
     FILE: EnhancementChooser.java
  PURPOSE: To select a data enhancement.
   AUTHOR: Peter Hollemans
     DATE: 2003/09/07
  CHANGES: 2004/01/15, PFH, updated Javadocs
           2004/02/17, PFH
             - changed enhancement property to function
             - added TabComponent interface
             - removed explicit property change support
           2004/02/19, PFH
             - added getRange()
             - fixed normalization out of slider range problem
           2004/05/22, PFH, modified to use GUIServices.getIcon()
           2005/02/02, PFH, modified to allow non-integer range bounds
           2005/02/08, PFH, corrected problems when min>max in setRange()
           2005/05/18, PFH, fixed normalize problem when no valid stdev
           2006/01/12, PFH, disabled norm button for log enhancements

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import noaa.coastwatch.gui.EnhancementFunctionPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.HistogramPanel;
import noaa.coastwatch.gui.PalettePanel;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.LogEnhancement;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.render.StepEnhancement;
import noaa.coastwatch.util.Statistics;

/**
 * An enhancement chooser is a panel that allows the user to select
 * the specifications of a data enhancement function.  An enhancement
 * function is typically used in conjunction with a 2D data variable
 * to normalize a set of data values to the range [0..1] for mapping
 * to a colour palette.<p>
 *
 * The enhancement chooser signals a change in the enhancement
 * function specifications by firing a
 * <code>PropertyChangeEvent</code> whose property name is
 * <code>EnhancementChooser.FUNCTION_PROPERTY</code>, and new value
 * contains an object of type {@link
 * noaa.coastwatch.render.EnhancementFunction}.
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public class EnhancementChooser
  extends JPanel
  implements TabComponent {

  // Constants
  // ---------
  /** The height of the palette stripe. */
  private static final int PALETTE_HEIGHT = 15;

  /** The minimum height of the histogram. */
  private static final int HISTOGRAM_HEIGHT = 100;

  /** The minimum height of the function. */
  private static final int FUNCTION_HEIGHT = 100;

  /** The enhancement function type linear. */
  private static final String FUNCTION_LINEAR = "Linear";

  /** The enhancement function type step. */
  private static final String FUNCTION_STEP = "Step";

  /** The enhancement function type log base 10. */
  private static final String FUNCTION_LOG = "Log10";

  /** The enhancement range commands. */
  private static final String NORMALIZE_COMMAND = "Norm";
  private static final String REVERSE_COMMAND = "Reverse";
  private static final String RESET_COMMAND = "Reset";

  /** The standard deviation units for normalization windows. */
  private static final double STDEV_UNITS = 1.5;

  /** The enhancement property. */
  public static final String FUNCTION_PROPERTY = "function";

  /** The enhancement tooltip. */
  private static final String ENHANCEMENT_TOOLTIP = "Color Enhancement";

  // Variables
  // ---------    
  /** The histogram panel. */
  private HistogramPanel histogramPanel;

  /** The palette panel. */
  private PalettePanel palettePanel;

  /** The enhancement function panel. */
  private EnhancementFunctionPanel enhancementFunctionPanel;

  /** The minimum value slider. */
  private JSlider minSlider;
 
  /** The maximum value slider. */
  private JSlider maxSlider;

  /** The enhancement function. */
  private EnhancementFunction func;

  /** The function combo box. */
  private JComboBox functionCombo;

  /** The steps spinner control. */
  private JSpinner stepsSpinner;

  /** The minimum text field. */
  private JTextField minField;

  /** The maximum text field. */
  private JTextField maxField;

  /** The range text fields. */
  private JTextField[] fields;

  /** The range sliders. */
  private JSlider[] sliders;

  /** The allowed enhancement range. */
  private double[] allowedRange;

  /** The last allowed enhancement range, saved for log enhancement. */
  private double[] lastAllowedRange;

  /** The actual enhancement range. */
  private double[] actualRange;

  /** The listener for text field action events. */
  private RangeFieldListener fieldListener;

  /** The listener for range slider change events. */
  private RangeSliderListener sliderListener;

  /** The listener for function combo events. */
  private FunctionComboListener functionComboListener;

  /** The listener for steps spinner events. */
  private StepsSpinnerListener stepsSpinnerListener;

  /** The normalize button. */
  private JButton normalizeButton;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new enhancement chooser panel.  If the system property
   * <code>debug</code> is set to <code>true</code>, the enhancement
   * chooser is setup with initial default properties to make testing
   * more interesting.
   */  
  public EnhancementChooser () {

    // Initialize
    // ----------
    super (new GridBagLayout());

    // Create histogram panel
    // ----------------------
    JPanel histogramPanelContainer = new JPanel (new BorderLayout (2, 0));
    histogramPanelContainer.setBorder (new TitledBorder (new EtchedBorder(), 
      "Histogram"));
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.BOTH,
      1, 1);
    this.add (histogramPanelContainer, gc);

    histogramPanel = new HistogramPanel();
    histogramPanel.setPreferredSize (new Dimension (HISTOGRAM_HEIGHT,
      HISTOGRAM_HEIGHT));
    histogramPanelContainer.add (histogramPanel, BorderLayout.CENTER);

    palettePanel = new PalettePanel();
    histogramPanelContainer.add (palettePanel, BorderLayout.SOUTH);
    palettePanel.setPreferredSize (new Dimension (PALETTE_HEIGHT, 
      PALETTE_HEIGHT));

    // Create enhancement range panel
    // ------------------------------
    JPanel enhancementRangeContainer = new JPanel (new GridBagLayout());
    enhancementRangeContainer.setBorder (new TitledBorder (new EtchedBorder(), 
      "Enhancement Range"));
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    this.add (enhancementRangeContainer, gc);

    minSlider = new JSlider();
    GUIServices.setConstraints (gc, 0, 0, 2, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    gc.insets = new Insets (2,0,2,0);
    enhancementRangeContainer.add (minSlider, gc);

    maxSlider = new JSlider();
    GUIServices.setConstraints (gc, 0, 1, 2, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    enhancementRangeContainer.add (maxSlider, gc);

    JLabel minLabel = new JLabel ("Minimum:");
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    enhancementRangeContainer.add (minLabel, gc);
    
    JLabel maxLabel = new JLabel ("Maximum:");
    GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    enhancementRangeContainer.add (maxLabel, gc);

    minField = new JTextField();
    GUIServices.setConstraints (gc, 0, 3, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    enhancementRangeContainer.add (minField, gc);

    maxField = new JTextField();
    GUIServices.setConstraints (gc, 1, 3, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    enhancementRangeContainer.add (maxField, gc);

    JPanel rangeButtonPanel = new JPanel (new GridLayout (1, 3));
    GUIServices.setConstraints (gc, 0, 4, 2, 1, GridBagConstraints.NONE,
      0, 0);
    gc.anchor = GridBagConstraints.WEST;
    enhancementRangeContainer.add (rangeButtonPanel, gc);
    gc.anchor = GridBagConstraints.CENTER;

    normalizeButton = new JButton (NORMALIZE_COMMAND);
    RangeButtonAction buttonAction = new RangeButtonAction();
    normalizeButton.addActionListener (buttonAction);
    rangeButtonPanel.add (normalizeButton);

    JButton reverseButton = new JButton (REVERSE_COMMAND);
    reverseButton.addActionListener (buttonAction);
    rangeButtonPanel.add (reverseButton);

    JButton resetButton = new JButton (RESET_COMMAND);
    resetButton.addActionListener (buttonAction);
    rangeButtonPanel.add (resetButton);

    // Create enhancement function panel
    // ---------------------------------
    JPanel enhancementFunctionContainer = new JPanel (new GridBagLayout());
    enhancementFunctionContainer.setBorder (new TitledBorder (
      new EtchedBorder(), "Enhancement Function"));
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.BOTH,
      1, 1);
    gc.insets = new Insets (0,0,0,0);
    this.add (enhancementFunctionContainer, gc);

    enhancementFunctionPanel = new EnhancementFunctionPanel();
    enhancementFunctionPanel.setPreferredSize (new Dimension (FUNCTION_HEIGHT,
      FUNCTION_HEIGHT));
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.BOTH,
      1, 1);
    gc.insets = new Insets (2,0,2,0);
    enhancementFunctionContainer.add (enhancementFunctionPanel, gc);

    JPanel functionButtonPanel = new JPanel (new GridBagLayout());
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.NONE,
      0, 0);
    gc.anchor = GridBagConstraints.WEST;
    enhancementFunctionContainer.add (functionButtonPanel, gc);
    gc.anchor = GridBagConstraints.CENTER;

    JLabel functionLabel = new JLabel ("Function:");
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    functionButtonPanel.add (functionLabel, gc);
    gc.insets = new Insets (2, 0, 2, 0);
    functionCombo = new JComboBox (new Object[] {
      FUNCTION_LINEAR, FUNCTION_STEP, FUNCTION_LOG});
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    functionButtonPanel.add (functionCombo, gc);
    
    JLabel stepsLabel = new JLabel ("Steps:");
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    functionButtonPanel.add (stepsLabel, gc);
    gc.insets = new Insets (2, 0, 2, 0);
   
    stepsSpinner = new JSpinner (new SpinnerNumberModel (10, 
      1, 100, 1));
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    functionButtonPanel.add (stepsSpinner, gc);

    // Save fields and sliders for easy processing
    // -------------------------------------------
    fields = new JTextField[] {minField, maxField};
    sliders = new JSlider[] {minSlider, maxSlider};

    // Create and activate listeners
    // -----------------------------
    fieldListener = new RangeFieldListener();
    sliderListener = new RangeSliderListener();
    functionComboListener = new FunctionComboListener();
    stepsSpinnerListener = new StepsSpinnerListener();
    activateListeners();

    // Set an predefined enhancement for debugging
    // -------------------------------------------
    if (System.getProperty ("debug", "false").equals ("true")) {
      setRange (new double[] {1, 100});
      setFunction (new LinearEnhancement (new double[] {1, 100}));
      setPalette (PaletteFactory.create ("HSL256"));
      setStatistics (Statistics.getTestData (333));
    } // if

  } // EnhancementChooser constructor

  ////////////////////////////////////////////////////////////

  /** Adds listeners to the slider and field components. */
  private void activateListeners () {

    for (int i = 0; i < 2; i++) {
      fields[i].addActionListener (fieldListener);
      sliders[i].addChangeListener (sliderListener);
    } // for
    functionCombo.addActionListener (functionComboListener);
    stepsSpinner.addChangeListener (stepsSpinnerListener);
    
  } // activateListeners

  ////////////////////////////////////////////////////////////

  /** Removes listeners from the slider and field components. */
  private void deactivateListeners () {

    for (int i = 0; i < 2; i++) {
      fields[i].removeActionListener (fieldListener);
      sliders[i].removeChangeListener (sliderListener);
    } // for
    functionCombo.removeActionListener (functionComboListener);
    stepsSpinner.removeChangeListener (stepsSpinnerListener);
    
  } // deactivateListeners

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new enhancement chooser panel with the specified
   * function.
   *
   * @param func the function to initialize the panel.
   */
  public EnhancementChooser (
    EnhancementFunction func
  ) {

    this();
    setFunction (func);

  } // EnhancementChooser constructor

  ////////////////////////////////////////////////////////////

  /** Handles slider change events. */
  private class RangeSliderListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {

      // Get slider value
      // ----------------
      JSlider slider = (JSlider) event.getSource();
      double value = slider.getValue();

      // Limit value to allowed range
      // ----------------------------
      boolean outOfRange = false;
      if (value < allowedRange[0]) {
        value = allowedRange[0];
        outOfRange = true;
      } // if
      else if (value > allowedRange[1]) {
        value = allowedRange[1];
        outOfRange = true;
      } // else if

      // Jump slider to actual value
      // ---------------------------
      if (outOfRange && !slider.getValueIsAdjusting()) {
        final JSlider fSlider = slider;
        final int fValue = (int) Math.round (value);
        SwingUtilities.invokeLater (new Runnable () {
            public void run () { 
              deactivateListeners();
              fSlider.setValue (fValue);
              activateListeners();
            } // run
          });
      } // if

      // Set text string and range values
      // --------------------------------
      String text = Double.toString (value);
      if (slider == minSlider) {
        actualRange[0] = value;
        minField.setText (text);
      } // if
      else if (slider == maxSlider) { 
        actualRange[1] = value;
        maxField.setText (text); 
      } // else if

      // Notify components of function change
      // ------------------------------------
      if (func != null) {
        func.setRange (actualRange);
        palettePanel.setFunction (func);
        enhancementFunctionPanel.setFunction (func);
        EnhancementChooser.this.firePropertyChange (FUNCTION_PROPERTY, null, 
          getFunction());
      } // if

    } // stateChanged
  } // RangeSliderListener class

  ////////////////////////////////////////////////////////////

  /** Handles function combo events. */
  private class FunctionComboListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      // Check for null function
      // -----------------------
      if (func == null) return;

      // Convert to linear function
      // --------------------------
      String functionType = (String) functionCombo.getSelectedItem();
      if (functionType == FUNCTION_LINEAR) {
        if (func instanceof LinearEnhancement && 
          !(func instanceof StepEnhancement)) return;
        if (lastAllowedRange != null) {
          allowedRange = lastAllowedRange;
          lastAllowedRange = null;
        } // if
        setFunction (new LinearEnhancement (actualRange));
      } // if

      // Convert to step function
      // ------------------------
      else if (functionType == FUNCTION_STEP) {
        if (func instanceof StepEnhancement) return;
        int steps = ((Integer) stepsSpinner.getValue()).intValue();
        if (lastAllowedRange != null) {
          allowedRange = lastAllowedRange;
          lastAllowedRange = null;
        } // if
        setFunction (new StepEnhancement (actualRange, steps));
      } // else if

      // Convert to log function
      // -----------------------
      else if (functionType == FUNCTION_LOG) {
        if (func instanceof LogEnhancement) return;
        if (allowedRange[0] <= 0 || allowedRange[1] <= 0) {
          lastAllowedRange = (double[]) allowedRange.clone();
          if (allowedRange[0] <= 0) allowedRange[0] = 1e-6;
          if (allowedRange[1] <= 0) allowedRange[1] = 1e-6;
        } // if
        actualRange[0] = Math.min (Math.max (actualRange[0], allowedRange[0]), 
          allowedRange[1]);
        actualRange[1] = Math.min (Math.max (actualRange[1], allowedRange[0]), 
          allowedRange[1]);
        setFunction (new LogEnhancement (actualRange));
      } // else if

      else
        throw new UnsupportedOperationException ("Unrecognized function type");

    } // actionPerformed
  } // FunctionComboListener class

  ////////////////////////////////////////////////////////////

  /** Handles steps spinner change events. */
  private class StepsSpinnerListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {

      // Check for null function
      // -----------------------
      if (func == null) return;

      // Create new function
      // -------------------
      if (!(func instanceof StepEnhancement)) return;
      int steps = ((Integer) stepsSpinner.getValue()).intValue();
      setFunction (new StepEnhancement (actualRange, steps));

    } // stateChanged
  } // StepsSpinnerListener class

  ////////////////////////////////////////////////////////////

  /** Handles range text field events. */
  private class RangeFieldListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      // Loop over each slider/field pair
      // --------------------------------
      deactivateListeners();
      for (int i = 0; i < 2; i++) {

        // Get field value
        // ---------------
        double value;
        try { value = Double.parseDouble (fields[i].getText()); }
        catch (NumberFormatException e) { value = actualRange[i]; }

        // Check field value
        // -----------------
        if (value < allowedRange[0]) value = allowedRange[0];
        else if (value > allowedRange[1]) value = allowedRange[1];

        // Set slider and field
        // --------------------
        sliders[i].setValue ((int) Math.round (value));
        fields[i].setText (Double.toString (value));
        
        // Set actual range
        // ----------------
        actualRange[i] = value;

      } // for
      activateListeners();

      // Notify components of function change
      // ------------------------------------
      if (func != null) {
        func.setRange (actualRange);
        palettePanel.setFunction (func);
        enhancementFunctionPanel.setFunction (func);
        EnhancementChooser.this.firePropertyChange (FUNCTION_PROPERTY, null, 
          getFunction());
      } // if

    } // actionPerformed
  } // RangeFieldListener

  ////////////////////////////////////////////////////////////

  /**
   * Gets the allowed slider range.
   * 
   * @return the slider range as [min, max].
   */
  public double[] getRange () { 

    return ((double[]) allowedRange.clone());

  } // getRange

  ////////////////////////////////////////////////////////////

  /** Handles enhancement range button actions. */
  private class RangeButtonAction extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Normalize enhancement range
      // ---------------------------
      if (command.equals (NORMALIZE_COMMAND)) {
        Statistics stats = histogramPanel.getStatistics();
        if (stats == null || Double.isNaN (stats.getStdev())) return;
        func.normalize (stats, STDEV_UNITS);
        double[] funcRange = func.getRange();
        funcRange[0] = Math.max (Math.floor (funcRange[0]), allowedRange[0]);
        funcRange[1] = Math.min (Math.ceil (funcRange[1]), allowedRange[1]);
        func.setRange (funcRange);
      } // if

      // Reverse enhancement range values
      // --------------------------------
      else if (command.equals (REVERSE_COMMAND)) {
        func.setRange (new double[] {actualRange[1], actualRange[0]});
      } // else if

      // Reset enhancement range to extremes
      // -----------------------------------
      else if (command.equals (RESET_COMMAND)) {
        func.setRange (allowedRange);
      } // else if

      // Update annotation elements
      // --------------------------
      setFunction (func);

    } // actionPerformed
  } // RangeButtonAction class

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the current enhancement function.
   *
   * @return the function.
   */
  public EnhancementFunction getFunction () {

    return ((EnhancementFunction) func.clone());

  } // getFunction

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the enhancement function.
   *
   * @param newFunc the enhancement function.
   */
  public void setFunction (
    EnhancementFunction newFunc
  ) { 
    
    // Set range
    // ---------
    actualRange = newFunc.getRange();

    // Set function
    // ------------
    func = (EnhancementFunction) newFunc.clone();
    palettePanel.setFunction (func);
    enhancementFunctionPanel.setFunction (func);

    // Adjust sliders
    // --------------    
    deactivateListeners();
    minSlider.setValue ((int) Math.round (actualRange[0]));
    maxSlider.setValue ((int) Math.round (actualRange[1]));

    // Adjust sliders
    // --------------    
    minField.setText (Double.toString (actualRange[0]));
    maxField.setText (Double.toString (actualRange[1]));

    // Adjust function parameters
    // --------------------------
    if (func instanceof StepEnhancement) {
      functionCombo.setSelectedItem (FUNCTION_STEP);
      stepsSpinner.setValue (new Integer (((StepEnhancement)func).getSteps()));
      stepsSpinner.setEnabled (true);
    } // if
    else if (func instanceof LinearEnhancement) {
      functionCombo.setSelectedItem (FUNCTION_LINEAR);
      stepsSpinner.setEnabled (false);
    } // else if
    else if (func instanceof LogEnhancement) {
      functionCombo.setSelectedItem (FUNCTION_LOG);
      stepsSpinner.setEnabled (false);
    } // else if
    else
      throw new UnsupportedOperationException ("Unrecognized function type");
    activateListeners();

    // Modify normalize button for log enhancement
    // -------------------------------------------
    if (func instanceof LogEnhancement)
      normalizeButton.setEnabled (false);
    else
      normalizeButton.setEnabled (true);

    // Fire change event
    // -----------------
    EnhancementChooser.this.firePropertyChange (FUNCTION_PROPERTY, null, 
      getFunction());

  } // setFunction

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the statistics for use in the histogram plot. 
   * 
   * @param stats the statistics, or null for no histogram display.
   */
  public void setStatistics (
    Statistics stats
  ) {

    histogramPanel.setStatistics (stats);

  } // setStatistics

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the palette for use in the palette stripe.
   * 
   * @param palette the palette, or null for no palette display.
   */
  public void setPalette (
    Palette palette
  ) {

    palettePanel.setPalette (palette);

  } // setPalette

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the range for use in the enhancement sliders.
   * 
   * @param range the slider range as [min, max].
   */
  public void setRange (
    double[] range
  ) {

    // Set actual and allowed range
    // ----------------------------
    allowedRange = (double[]) range.clone();
    if (allowedRange[0] > allowedRange[1]) {
      allowedRange[0] = range[1];
      allowedRange[1] = range[0];
    } // if
    actualRange = (double[]) allowedRange.clone();

    // Set palette and histogram extents
    // ---------------------------------
    histogramPanel.setRange (allowedRange);
    palettePanel.setRange (allowedRange);
    enhancementFunctionPanel.setRange (allowedRange);

    // Set slider extents
    // ------------------
    deactivateListeners();
    minSlider.setMinimum ((int) Math.floor (allowedRange[0]));
    minSlider.setMaximum ((int) Math.ceil (allowedRange[1]));
    maxSlider.setMinimum ((int) Math.floor (allowedRange[0]));
    maxSlider.setMaximum ((int) Math.ceil (allowedRange[1]));
    activateListeners();

  } // setRange

  ////////////////////////////////////////////////////////////

  /** 
   * Determines if the enhancement function is adjusting by
   * checking to see if the range sliders are adjusting
   * 
   * @return true if the range sliders are adjusting, or false if not.
   */
  public boolean getValueIsAdjusting () {
  
    return (minSlider.getValueIsAdjusting() || 
      maxSlider.getValueIsAdjusting());

  } // getValueIsAdjusting

  ////////////////////////////////////////////////////////////

  @Override
  public Icon getIcon () { return (GUIServices.getIcon ("enhancement.tab")); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getToolTip () { return (ENHANCEMENT_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getTitle () { return (null); }

  ////////////////////////////////////////////////////////////

} // EnhancementChooser class

////////////////////////////////////////////////////////////////////////
