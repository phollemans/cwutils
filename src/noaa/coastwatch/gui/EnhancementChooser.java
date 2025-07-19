////////////////////////////////////////////////////////////////////////
/*

     File: EnhancementChooser.java
   Author: Peter Hollemans
     Date: 2003/09/07

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import javax.swing.Action;
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
import javax.swing.Box;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import java.util.Collections;

import noaa.coastwatch.gui.EnhancementFunctionPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.HistogramPanel;
import noaa.coastwatch.gui.PalettePanel;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.LogEnhancement;
import noaa.coastwatch.render.GammaEnhancement;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.render.StepEnhancement;
import noaa.coastwatch.util.Statistics;

import java.util.logging.Logger;

/**
 * <p>An enhancement chooser is a panel that allows the user to select
 * the specifications of a data enhancement function.  An enhancement
 * function is typically used in conjunction with a 2D data variable
 * to normalize a set of data values to the range [0..1] for mapping
 * to a colour palette.</p>
 *
 * <p>The enhancement chooser signals a change in the enhancement
 * function specifications by firing a
 * <code>PropertyChangeEvent</code> whose property name is
 * <code>EnhancementChooser.FUNCTION_PROPERTY</code>, and new value
 * contains an object of type {@link
 * noaa.coastwatch.render.EnhancementFunction}.</p>
 * 
 * <p>The chooser alsp uses the <code>EnhancementChooser.SAVE_PROPERTY</code>
 * to signal that the preferences should be updated for the current variable
 * using the active palette and function.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public class EnhancementChooser
  extends JPanel
  implements TabComponent {

  private static final Logger LOGGER = Logger.getLogger (EnhancementChooser.class.getName());

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

  /** The enhancement function type gamma. */
  private static final String FUNCTION_GAMMA = "Gamma";

  /** The enhancement range commands. */
  private static final String NORMALIZE_COMMAND = "Norm";
  private static final String REVERSE_COMMAND = "Reverse";
  private static final String RESET_COMMAND = "Reset";

  /** The standard deviation units for normalization windows. */
  private static final double STDEV_UNITS = 1.5;

  /** The enhancement function property. */
  public static final String FUNCTION_PROPERTY = "function";

  /** The enhancement save preferences property. */
  public static final String SAVE_PROPERTY = "save";

  /** The enhancement tooltip. */
  private static final String ENHANCEMENT_TOOLTIP = "Color Enhancement";

  /** The min and msax constants for array access. */
  private static final int MIN = 0;
  private static final int MAX = 1;

  /** The log10 minimum when the user has specified <=0. */
  private static final double LOG10_MIN = 0.001;

  // Variables
  // ---------    
  /** The histogram panel. */
  private HistogramPanel histogramPanel;

  /** The palette panel. */
  private PalettePanel palettePanel;

  /** The enhancement function panel. */
  private EnhancementFunctionPanel enhancementFunctionPanel;

  /** The minimum value slider. */
//  private JSlider minSlider;
  private DoubleSlider minSlider;
 
  /** The maximum value slider. */
//  private JSlider maxSlider;
  private DoubleSlider maxSlider;

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
  private DoubleSlider[] sliders;

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

  /** Extends a slider to handle double-valued settings and ranges. */
  private class DoubleSlider extends JSlider {

    private static final int SLIDER_MIN = 0;
    private static final int SLIDER_MAX = 100;
    private double min, max;
    private double tick;
    private String format;

    public DoubleSlider () {

      super();
      setMinimum (SLIDER_MIN);
      setMaximum (SLIDER_MAX);

    } // DoubleSlider

    public void setDoubleRange (double min, double max) { 

      this.min = min;
      this.max = max;
      calculateTickSize (Math.abs (max - min));

      LOGGER.fine ("Slider range [" + min + "," + max + "] using tick size " + this.tick + " and format " + this.format);

    } // setDoubleRange

    public void calculateTickSize (double range) {

      double interval = range/(SLIDER_MAX - SLIDER_MIN);
      int power = (int) Math.floor (Math.log10 (interval));
      double mantissa = interval / Math.pow (10, power);
      if (mantissa < 2) this.tick = 2 * Math.pow (10, power);
      else if (mantissa < 5) this.tick = 5 * Math.pow (10, power);
      else this.tick = 10 * Math.pow (10, power);

      int decimals  = this.tick >= 1 ? 0 : Math.abs ((int) Math.floor (Math.log10 (this.tick)));
      this.format = (decimals == 0 ? "%.1f" : "%." + decimals + "f"); 

    } // calculateTickSize

    public void setDoubleValue (double value) { 

      setValue (SLIDER_MIN + (int) Math.round ((SLIDER_MAX - SLIDER_MIN)*(value - min)/(max - min))); 

    } // setDoubleValue

    public double getDoubleValue () { 

      int value = getValue();
      double doubleValue;
      if (value == SLIDER_MIN) doubleValue = min;
      else if (value == SLIDER_MAX) doubleValue = max;
      else {
        doubleValue = min + (value - SLIDER_MIN)/((double) SLIDER_MAX)*(max - min);
        doubleValue = ((int) Math.round (doubleValue / tick)) * tick;
      } // else

      return (doubleValue);

    } // getDoubleValue

    public String formatValue (double value) { return (String.format (format, value)); }

  } // DoubleSlider class

  ////////////////////////////////////////////////////////////

  /** Maps the tab kay for a text field to perform the same action as enter. */
  private void mapTabToEnterAction (JTextField textField) {

    // First inhibit the normal function of the TAB key
    textField.setFocusTraversalKeys (KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet());    

    // Next set the input map the perform an action when TAB is pressed
    var inputMap = textField.getInputMap (JComponent.WHEN_FOCUSED);
    inputMap.put (KeyStroke.getKeyStroke (KeyEvent.VK_TAB, 0, false), "performEnterAction");

    // Finally add an action to the map that posts an action event and then
    // moves focus
    var actionMap = textField.getActionMap();
    actionMap.put ("performEnterAction", new AbstractAction() {
      @Override
      public void actionPerformed (ActionEvent e) {
        textField.postActionEvent();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
      } // actionPerformed
    });

  } // mapTabToEnterAction

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
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    this.add (enhancementRangeContainer, gc);

    minSlider = new DoubleSlider();
    GUIServices.setConstraints (gc, 0, 0, 2, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (2,0,2,0);
    enhancementRangeContainer.add (minSlider, gc);

    maxSlider = new DoubleSlider();
    GUIServices.setConstraints (gc, 0, 1, 2, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    enhancementRangeContainer.add (maxSlider, gc);

    JLabel minLabel = new JLabel ("Minimum:");
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    enhancementRangeContainer.add (minLabel, gc);
    
    JLabel maxLabel = new JLabel ("Maximum:");
    GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    enhancementRangeContainer.add (maxLabel, gc);

    minField = new JTextField();
    mapTabToEnterAction (minField);
    GUIServices.setConstraints (gc, 0, 3, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    enhancementRangeContainer.add (minField, gc);

    maxField = new JTextField();
    mapTabToEnterAction (maxField);
    GUIServices.setConstraints (gc, 1, 3, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    enhancementRangeContainer.add (maxField, gc);

    Box rangeButtonPanel = Box.createHorizontalBox();
    GUIServices.setConstraints (gc, 0, 4, 2, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    gc.anchor = GridBagConstraints.WEST;
    enhancementRangeContainer.add (rangeButtonPanel, gc);
    gc.anchor = GridBagConstraints.CENTER;

    normalizeButton = GUIServices.getIconButton ("enhancement.normalize");
    GUIServices.setSquare (normalizeButton);
    normalizeButton.setToolTipText ("Normalize range around mean value");
    normalizeButton.addActionListener (event -> normalizeEvent());
    rangeButtonPanel.add (normalizeButton);

    JButton reverseButton = GUIServices.getIconButton ("enhancement.reverse");
    GUIServices.setSquare (reverseButton);
    reverseButton.setToolTipText ("Reverse minimum and maximum");
    reverseButton.addActionListener (event -> reverseEvent());
    rangeButtonPanel.add (reverseButton);

    JButton resetButton = GUIServices.getIconButton ("enhancement.reset");
    GUIServices.setSquare (resetButton);
    resetButton.setToolTipText ("Reset range to full extent");
    resetButton.addActionListener (event -> resetEvent());
    rangeButtonPanel.add (resetButton);

    rangeButtonPanel.add (Box.createHorizontalGlue());

    JButton configButton = GUIServices.getIconButton ("enhancement.config");
    GUIServices.setSquare (configButton);
    configButton.setToolTipText ("Reconfigure range extents");
    configButton.addActionListener (event -> configEvent());
    rangeButtonPanel.add (configButton);

    JButton saveButton = GUIServices.getIconButton ("enhancement.save");
    GUIServices.setSquare (saveButton);
    saveButton.setToolTipText ("Save enhancement to preferences");
    saveButton.addActionListener (event -> saveEvent());
    rangeButtonPanel.add (saveButton);

    // Create enhancement function panel
    // ---------------------------------
    JPanel enhancementFunctionContainer = new JPanel (new GridBagLayout());
    enhancementFunctionContainer.setBorder (new TitledBorder (
      new EtchedBorder(), "Enhancement Function"));
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.BOTH, 1, 1);
    gc.insets = new Insets (0,0,0,0);
    this.add (enhancementFunctionContainer, gc);

    enhancementFunctionPanel = new EnhancementFunctionPanel();
    enhancementFunctionPanel.setPreferredSize (new Dimension (FUNCTION_HEIGHT, FUNCTION_HEIGHT));
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.BOTH, 1, 1);
    gc.insets = new Insets (2,0,2,0);
    enhancementFunctionContainer.add (enhancementFunctionPanel, gc);

    JPanel functionButtonPanel = new JPanel (new GridBagLayout());
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.NONE, 0, 0);
    gc.anchor = GridBagConstraints.WEST;
    enhancementFunctionContainer.add (functionButtonPanel, gc);
    gc.anchor = GridBagConstraints.CENTER;

    JLabel functionLabel = new JLabel ("Function:");
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    functionButtonPanel.add (functionLabel, gc);
    gc.insets = new Insets (2, 0, 2, 0);
    functionCombo = new JComboBox (new Object[] {
      FUNCTION_LINEAR, FUNCTION_STEP, FUNCTION_LOG, FUNCTION_GAMMA});
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    functionButtonPanel.add (functionCombo, gc);
    
    JLabel stepsLabel = new JLabel ("Steps:");
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    functionButtonPanel.add (stepsLabel, gc);
    gc.insets = new Insets (2, 0, 2, 0);
   
    stepsSpinner = new JSpinner (new SpinnerNumberModel (10, 1, 100, 1));
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    functionButtonPanel.add (stepsSpinner, gc);

    // Save fields and sliders for easy processing
    // -------------------------------------------
    fields = new JTextField[] {minField, maxField};
    sliders = new DoubleSlider[] {minSlider, maxSlider};

    // Create and activate listeners
    // -----------------------------
    fieldListener = new RangeFieldListener();
    sliderListener = new RangeSliderListener();
    functionComboListener = new FunctionComboListener();
    stepsSpinnerListener = new StepsSpinnerListener();
    activateListeners();

    // Set an predefined enhancement for debugging
    // -------------------------------------------
    if (!System.getProperty ("cw.debug", "false").equals ("false")) {
      setRange (new double[] {1, 100});
      setFunction (new LinearEnhancement (new double[] {1, 100}));
      setPalette (PaletteFactory.create ("HSL256"));
      setStatistics (Statistics.getTestData (333));
    } // if

  } // EnhancementChooser constructor

  ////////////////////////////////////////////////////////////

  /** Adds listeners to the slider and field components. */
  private void activateListeners () {

    for (int i = MIN; i <= MAX; i++) {
      fields[i].addActionListener (fieldListener);
      sliders[i].addChangeListener (sliderListener);
    } // for
    functionCombo.addActionListener (functionComboListener);
    stepsSpinner.addChangeListener (stepsSpinnerListener);
    
  } // activateListeners

  ////////////////////////////////////////////////////////////

  /** Removes listeners from the slider and field components. */
  private void deactivateListeners () {

    for (int i = MIN; i <= MAX; i++) {
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
      var slider = (DoubleSlider) event.getSource();
      double value = slider.getDoubleValue();

      // Limit value to allowed range
      // ----------------------------
      boolean outOfRange = false;
      if (value < allowedRange[MIN]) {
        value = allowedRange[MIN];
        outOfRange = true;
      } // if
      else if (value > allowedRange[MAX]) {
        value = allowedRange[MAX];
        outOfRange = true;
      } // else if

      // Jump slider to actual value
      // ---------------------------
      if (outOfRange && !slider.getValueIsAdjusting()) {
        final double newValue = value;
        SwingUtilities.invokeLater (() -> {
          deactivateListeners();
          slider.setDoubleValue (newValue);
          activateListeners();
        });
      } // if

      // Set text string and range values
      // --------------------------------
      String text = (value > allowedRange[MIN] && value < allowedRange[MAX] ? 
        slider.formatValue (value) : Double.toString (value));
      if (slider == minSlider) {
        actualRange[MIN] = value;
        minField.setText (text);
      } // if
      else if (slider == maxSlider) { 
        actualRange[MAX] = value;
        maxField.setText (text); 
      } // else if

      // Notify components of function change
      // ------------------------------------
      if (func != null) {
        func.setRange (actualRange);
        palettePanel.setFunction (func);
        enhancementFunctionPanel.setFunction (func);

        LOGGER.fine ("Firing property change for function, new value = " + func + ", adjusting = " + getValueIsAdjusting());
        EnhancementChooser.this.firePropertyChange (FUNCTION_PROPERTY, null, getFunction());

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
        if (allowedRange[MIN] <= 0 || allowedRange[MAX] <= 0) {
          lastAllowedRange = (double[]) allowedRange.clone();
          if (allowedRange[MIN] <= 0) allowedRange[MIN] = LOG10_MIN;
          if (allowedRange[MAX] <= 0) allowedRange[MAX] = LOG10_MIN;
        } // if
        actualRange[MIN] = Math.min (Math.max (actualRange[MIN], allowedRange[MIN]), 
          allowedRange[MAX]);
        actualRange[MAX] = Math.min (Math.max (actualRange[MAX], allowedRange[MIN]), 
          allowedRange[MAX]);
        setFunction (new LogEnhancement (actualRange));
      } // else if

      // Convert to gamma function
      // -------------------------
      else if (functionType == FUNCTION_GAMMA) {
        if (func instanceof GammaEnhancement) return;
        if (lastAllowedRange != null) {
          allowedRange = lastAllowedRange;
          lastAllowedRange = null;
        } // if
        setFunction (new GammaEnhancement (actualRange));
      } // if

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
      for (int i = MIN; i <= MAX; i++) {

        // Get field value
        // ---------------
        double value;
        try { value = Double.parseDouble (fields[i].getText()); }
        catch (NumberFormatException e) { value = actualRange[i]; }

        // Check field value
        // -----------------
        if (value < allowedRange[MIN]) value = allowedRange[MIN];
        else if (value > allowedRange[MAX]) value = allowedRange[MAX];

        // Set slider and field
        // --------------------
        // sliders[i].setValue ((int) Math.round (value));
        // fields[i].setText (Double.toString (value));

        sliders[i].setDoubleValue (value);
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

        LOGGER.fine ("Firing property change for function, new value = " + func);
        EnhancementChooser.this.firePropertyChange (FUNCTION_PROPERTY, null, getFunction());

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

  // These are various event handlers in response to buttons.

  private void normalizeEvent() {
    Statistics stats = histogramPanel.getStatistics();
    if (stats == null || Double.isNaN (stats.getStdev())) return;
    func.normalize (stats, STDEV_UNITS);
    double[] funcRange = func.getRange();
    funcRange[MIN] = Math.max (Math.floor (funcRange[MIN]), allowedRange[MIN]);
    funcRange[MAX] = Math.min (Math.ceil (funcRange[MAX]), allowedRange[MAX]);
    func.setRange (funcRange);
    setFunction (func);
  } // normalizeEvent

  private void reverseEvent() {
    func.setRange (new double[] {actualRange[MAX], actualRange[MIN]});
    setFunction (func);
  } // reverseEvent

  private void resetEvent() {
    func.setRange (allowedRange);
    setFunction (func);
  } // resetEvent

  private void configEvent() {

    JPanel dimPanel = new JPanel();
    dimPanel.setLayout (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;

    double[] range = lastAllowedRange != null ? lastAllowedRange : allowedRange;

    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    dimPanel.add (new JLabel ("Minimum:        "), gc);
    gc.insets = new Insets (2, 0, 2, 0);
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    final JTextField minField = new JTextField();
    minField.setText (Double.toString (range[MIN]));
    minField.setEditable (true);
    minField.setColumns (8);
    dimPanel.add (minField, gc);
    
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    dimPanel.add (new JLabel ("Maximum:        "), gc);
    gc.insets = new Insets (2, 0, 2, 0);
    GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    final JTextField maxField = new JTextField();
    maxField.setText (Double.toString (range[MAX]));
    maxField.setEditable (true);
    maxField.setColumns (8);
    dimPanel.add (maxField, gc);

    final JDialog[] dialog = new JDialog[1];
    Action okAction = GUIServices.createAction ("OK", new Runnable() {
      public void run () {

        double[] newRange = new double[2];
        boolean update = false;
        try {
          newRange[MIN] = Double.parseDouble (minField.getText());
          newRange[MAX] = Double.parseDouble (maxField.getText());
          update = true;
        } // try
        catch (NumberFormatException e) {
          JOptionPane.showMessageDialog (dialog[0],
            "Error parsing text field:\n" + e.toString(),
            "Error", JOptionPane.ERROR_MESSAGE);
        } // catch

        if (update) {
          setRange (newRange);
          resetEvent();
          dialog[0].dispose();
        } // if

      } // run
    });

    Action cancelAction = GUIServices.createAction ("Cancel", null);

    dialog[0] = GUIServices.createDialog (
      EnhancementChooser.this, "Configure range extents", true, dimPanel,
      null, new Action[] {okAction, cancelAction}, new boolean[] {false, true}, 
      true);
    dialog[0].setVisible (true);

  } // configEvent

  private void saveEvent() {
    EnhancementChooser.this.firePropertyChange (SAVE_PROPERTY, null, null);
  } // saveEvent

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
    // minSlider.setValue ((int) Math.round (actualRange[MIN]));
    // maxSlider.setValue ((int) Math.round (actualRange[MAX]));

    minSlider.setDoubleValue (actualRange[MIN]);
    maxSlider.setDoubleValue (actualRange[MAX]);

    // Adjust fields
    // -------------    
    minField.setText (Double.toString (actualRange[MIN]));
    maxField.setText (Double.toString (actualRange[MAX]));

    // Adjust function parameters
    // --------------------------
    if (func instanceof StepEnhancement) {
      functionCombo.setSelectedItem (FUNCTION_STEP);
      stepsSpinner.setValue (Integer.valueOf (((StepEnhancement)func).getSteps()));
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
    else if (func instanceof GammaEnhancement) {
      functionCombo.setSelectedItem (FUNCTION_GAMMA);
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

    LOGGER.fine ("Firing property change for function, new value = " + func);

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
   * @param range the slider range as either [min, max] or [max,min].
   */
  public void setRange (
    double[] range
  ) {

    // Update the allowed range and swap if necessary.
    double[] newRange = (
      range[MIN] < range[MAX] ? 
      (double[]) range.clone() : 
      new double[] {range[MAX], range[MIN]}
    );
    allowedRange = (double[]) newRange.clone();  

    // Take account of log function mode by selectively modifying 
    // the range if zero or less.
    if (func instanceof LogEnhancement) {
      if (allowedRange[MIN] <= 0 || allowedRange[MAX] <= 0) {
        lastAllowedRange = (double[]) allowedRange.clone();
        if (allowedRange[MIN] <= 0) allowedRange[MIN] = LOG10_MIN;
        if (allowedRange[MAX] <= 0) allowedRange[MAX] = LOG10_MIN;
      } // if
      else lastAllowedRange = null;
    } // if

    // Set palette and histogram extents
    // ---------------------------------
    histogramPanel.setRange (newRange);
    palettePanel.setRange (newRange);
    enhancementFunctionPanel.setRange (newRange);

    // Set slider extents
    // ------------------
    deactivateListeners();
    // minSlider.setMinimum ((int) Math.floor (newRange[MIN]));
    // minSlider.setMaximum ((int) Math.ceil (newRange[MAX]));
    // maxSlider.setMinimum ((int) Math.floor (newRange[MIN]));
    // maxSlider.setMaximum ((int) Math.ceil (newRange[MAX]));

    minSlider.setDoubleRange (newRange[MIN], newRange[MAX]);
    maxSlider.setDoubleRange (newRange[MIN], newRange[MAX]);

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
  
    // FIXME: There is a known issue here on MacOS, that sometimes the
    // OS doesn't report a mouse release event when the mouse cursor
    // is outside the application window.  So the return value for
    // this method is true when it should be false.  This makes it so
    // that the enhancement function may not update to it's non-adjusting
    // state for users of this method.

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
