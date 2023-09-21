////////////////////////////////////////////////////////////////////////
/*

     File: DataColorScale.java
   Author: Peter Hollemans
     Date: 2002/09/26

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.IndexColorModel;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.GraphicsServices;
import noaa.coastwatch.render.Legend;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.LogEnhancement;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.StepEnhancement;
import noaa.coastwatch.render.TextElement;

import java.util.logging.Logger;

/**
 * A color scale annotates a data enhancement plot with a scale of
 * colors from a color palette and tick marks at regular intervals for
 * the data values.  The data variable name and units are also printed
 * along side the scale.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class DataColorScale 
  extends Legend {

  private static final Logger LOGGER = Logger.getLogger (DataColorScale.class.getName());

  // Constants
  // ---------
  
  /** The default number of desired tick marks. */
  private final static int TICKS = 10;

  /** The default scale width. */
  private final static int SCALE_WIDTH = 20;

  /** The default tick size. */
  private final static int TICK_SIZE = 5;

  // Variables
  // ---------
  
  /** The color palette used for the scale. */
  private Palette palette;

  /** The enhancement function for normalized values. */
  private EnhancementFunction function;

  /** The text to use for scale annotation. */
  private String annotation;

  /** An array of tick label strings. */
  private String[] labels;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the tick labels.
   * 
   * @param labels the new tick labels to use.  Each label is 
   * a number formatted to a string value.
   */
  public void setTickLabels (
    String[] labels
  ) {
  
    this.labels = (String[]) labels.clone();
    
  } // setTickLabels

  ////////////////////////////////////////////////////////////

  @Override
  public void render (
    Graphics2D g,
    int x,
    int y
  ) {

    // Initialize
    // ----------
    Dimension size = getSize (g);
    Dimension required = getRequiredSize (g);
    int xoff = (required.width < size.width ? (size.width - required.width)/2 : 0);

    int x1, y1, x2, y2;
    int scaleHeight = size.height - 4*SPACE_SIZE - 2;
    double[] range = new double[] {function.getInverse (0), 
      function.getInverse (1)};
    boolean reverse = (range[0] > range[1]);
    g.setStroke (DEFAULT_STROKE);

    // Draw background
    // ---------------
    if (back != null) {
      g.setColor (back);
      g.fillRect (x, y, size.width, size.height);
      g.setColor (fore);
      GraphicsServices.drawRect (g, new Rectangle (x, y, size.width, 
        size.height));
    } // if

    // Draw scale colors
    // -----------------
    x1 = x + xoff + SPACE_SIZE*2;
    x2 = x1 + SCALE_WIDTH;

    Dimension imageDims = new Dimension (SCALE_WIDTH, scaleHeight);
    BufferedImage image = new BufferedImage (imageDims.width, imageDims.height,
      BufferedImage.TYPE_INT_RGB);
    Graphics2D imageGraphics = image.createGraphics();
    imageGraphics.setStroke (DEFAULT_STROKE);

    IndexColorModel model = palette.getModel();
    int colors = model.getMapSize();
    for (int i = 0; i < scaleHeight; i++) {
      y1 = y2 = scaleHeight-1-i;
      double norm = (double) i / (scaleHeight-1);
      if (function instanceof StepEnhancement)
        norm = function.getValue (function.getInverse (norm));
      if (reverse) norm = 1-norm;
      int index = (int) Math.round (norm*(colors-1));
      imageGraphics.setColor (new Color (model.getRGB (index)));
      imageGraphics.drawLine (0, y1, imageDims.width-1, y2);
    } // for

    g.drawImage (image, x1, y + SPACE_SIZE*2, null);
    imageGraphics.dispose();

    // Draw scale border
    // -----------------
    g.setColor (fore);
    GraphicsServices.drawRect (g, new Rectangle (x + xoff + SPACE_SIZE*2, 
      y + SPACE_SIZE*2, SCALE_WIDTH, scaleHeight));

    // Draw scale ticks
    // ----------------
    x1 = x2 + 1;
    x2 = x1 + TICK_SIZE - 1;
    int maxx = x2 + SPACE_SIZE;

    if (Double.isFinite (range[0]) && Double.isFinite (range[1]) && (range[0] != range[1])) {

      boolean invert = (range[0] > range[1]);
      EnhancementFunction tickFunction;
      /** 
       * Note that this next statement may only appear to work in
       * testing because there is currently no way to set the reversal
       * flag for StepEnhancement objects in cdat (and cwrender
       * currently does not support step enhancements).
       */
      if (function instanceof StepEnhancement)
        tickFunction = new LinearEnhancement (function.getRange());
      else 
        tickFunction = function;
      for (int i = 0; i < labels.length; i++) {

        // Draw tick
        // ---------
        double val = Double.parseDouble (labels[i]);
        double norm = tickFunction.getValue (val);
        if (reverse) norm = 1-norm;
        y1 = y2 = (y + SPACE_SIZE*2 + scaleHeight) - 
          (int) (norm * (scaleHeight-1));
        g.drawLine (x1, y1, x2, y2);

        // Draw label
        // ----------
        TextElement labelElement = new TextElement (labels[i], font,
          new Point (x2 + SPACE_SIZE, y1), new double[] {0, 0.5}, 0);
        labelElement.render (g, fore, null);
        int endx = x2 + SPACE_SIZE + labelElement.getBounds(g).width;
        if (endx > maxx) maxx = endx;

        // Draw extra ticks
        // ----------------
        if (tickFunction instanceof LogEnhancement && i < labels.length-1) {
          double nextVal = Double.parseDouble (labels[i+1]);
          for (int j = 2; j < 10; j++) {
            double betweenVal = val * j;
            if (betweenVal >= nextVal) continue;
            norm = tickFunction.getValue (betweenVal);
            if (reverse) norm = 1-norm;
            y1 = y2 = (y + SPACE_SIZE*2 + scaleHeight) - 
              (int) (norm * (scaleHeight-1));
            g.drawLine (x1, y1, x2, y2);
          } // for
        } // if

      } //for

    } // if

    // Draw scale legend
    // -----------------
    x1 = maxx + SPACE_SIZE*2;
    y1 = y + size.height/2;
    TextElement annotationElement = new TextElement (annotation, font,
      new Point (x1, y1), new double[] {0.5, 1}, 90);
    annotationElement.render (g, fore, null);

  } // render

  ////////////////////////////////////////////////////////////

  /**
   * Gets the minimum size required by the color scale based on the label
   * lengths and scale width.
   * 
   * @param g the graphics context.
   * 
   * @return the minimum required size.
   */
  private Dimension getRequiredSize (
    Graphics2D g
  ) {

    // Find longest label
    // ------------------
    String label = " ";
    for (int i = 0; i < labels.length; i++) {
      if (labels[i].length() > label.length())
        label = labels[i];
    } // for

    // Create text bounds
    // ------------------
    TextElement labelElement = new TextElement (label, font, new Point(), 
      null, 0);
    Rectangle labelBounds = labelElement.getBounds(g);
    TextElement annotationElement = new TextElement (annotation, font, 
      new Point(), null, 90);
    Rectangle annotationBounds = annotationElement.getBounds(g);

    // Calculate size
    // --------------
    int requiredWidth = SPACE_SIZE*2 + 1 + SCALE_WIDTH + 1 + TICK_SIZE + SPACE_SIZE +
      labelBounds.width + SPACE_SIZE*2 + annotationBounds.width + SPACE_SIZE*2;
    int requiredHeight = Math.max (labelBounds.height * labels.length * 2,
      annotationBounds.height);
    requiredHeight += SPACE_SIZE*4;
    Dimension size = new Dimension (requiredWidth, requiredHeight);

    return (size);

  } // getRequiredSize

  ////////////////////////////////////////////////////////////

  @Override
  public Dimension getSize (
    Graphics2D g
  ) {

    Dimension size = getRequiredSize (g);

    if (preferredSize != null) {
      size.width = Math.max (preferredSize.width, size.width);
      size.height = Math.max (preferredSize.height, size.height);
    } // if
    
    return (size);

  } // getSize

  ////////////////////////////////////////////////////////////

  /**
   * Formats a log enhancement tick value for a label.
   *
   * @param exponent the exponent category for the tick as a
   * power of ten.
   * @param shortFormat the format flag, true to use exponential
   * notation.
   * @param value the value to format for the tick label.
   *
   * @return the formatted tick label string.
   */
  private static String formatLogValue (
    int exponent,
    boolean shortFormat,
    double value
  ) {

    String pattern = (shortFormat ? "0E0" : exponent >= 0 ? "0" : "0.");
    if (!shortFormat && exponent < 0) {
      for (int j = exponent; j <= 0; j++) pattern += "#";
    } // if
    DecimalFormat format = new DecimalFormat (pattern);
    return (format.format (value));

  } // formatLogValue

  ////////////////////////////////////////////////////////////

  /**
   * Gets an appropriate set of tick mark labels given the tick
   * specifications.  This method works well for log enhancement
   * scales.
   *
   * @param min the data value minimum.
   * @param max the data value maximum.
   *
   * @return an array of formatted numbers as strings for the tick
   * mark labels.
   */
  public static String[] getLogTickLabels (
    double min,
    double max
  ) {

    // Find min and max exponents
    // --------------------------
    int minExponent = (int) Math.floor (LogEnhancement.log10 (min));
    int maxExponent = (int) Math.ceil (LogEnhancement.log10 (max));
    boolean shortFormat = (minExponent < -3 || maxExponent > 3);

    LOGGER.fine ("Min exponent is " + minExponent);
    LOGGER.fine ("Max exponent is " + maxExponent);

    // Create labels
    // -------------
    int nticks = maxExponent - minExponent + 1;
    List<String> labelList = new LinkedList<>();
    for (int i = 0; i < nticks; i++) {
      int exponent = minExponent + i;
      double value = Math.pow (10, exponent);
      String label;
      if (value > max)
        label = formatLogValue (exponent+1, shortFormat, max);
      else if (value < min)
        label = formatLogValue (exponent-1, shortFormat, min);
      else
        label = formatLogValue (exponent, shortFormat, value);
      labelList.add (label);
    } // for
    String[] labels = labelList.toArray (new String[0]);

    if (labels.length == 0)
      LOGGER.fine ("No labels created!");
    else if (labels.length == 1)
      LOGGER.fine ("Created " + labels.length + " label, " + labels[0]);
    else if (labels.length >= 2)
      LOGGER.fine ("Created " + labels.length + " labels, [" + labels[0] + " .. " + labels[labels.length-1] + "]");

    return (labels);

  } // getLogTickLabels

  ////////////////////////////////////////////////////////////

  /**
   * Gets an appropriate tick mark interval given the tick
   * specifications.  This method works well for linear enhancement
   * scales.
   *
   * @param min the data value minimum.
   * @param max the data value maximum.
   * @param desired the approximate number of desired ticks.
   *
   * @return the tick interval.
   */
  public static double getLinearTickInterval (
    double min,
    double max,
    int desired
  ) {

    // Find best tick interval
    // -----------------------
    int[] bases = new int[] {1, 2, 5, 10, 20, 50};
    double range = max-min;
    int exp = (int) Math.floor (LogEnhancement.log10 (range)) - 1;
    double ticks = range / (bases[0]*Math.pow(10,exp)) + 1;
    int iticks = 0;
    for (int i = 1; i < bases.length; i++) {
      double newticks = range / (bases[i]*Math.pow(10,exp)) + 1;
      if (Math.abs (newticks - desired) < Math.abs (ticks - desired)) { 
        ticks = newticks;
        iticks = i;
      } // if
    } // for
    double interval = bases[iticks]*Math.pow(10,exp);

    return (interval);

  } // getLinearTickInterval

  ////////////////////////////////////////////////////////////

  /**
   * Gets an appropriate set of tick mark labels given the tick
   * specifications.  This method works well for linear enhancement
   * scales.
   *
   * @param min the data value minimum.
   * @param max the data value maximum.
   * @param desired the approximate number of desired ticks.
   *
   * @return an array of formatted numbers as strings for the tick
   * mark labels.
   */
  public static String[] getLinearTickLabels (
    double min,
    double max,
    int desired
  ) {

    // Find best tick interval
    // -----------------------
    double interval = getLinearTickInterval (min, max, desired);

    LOGGER.fine ("Tick interval is " + interval);

    // Create decimal format
    // ---------------------
    String pattern;
    double minExp = Math.abs (LogEnhancement.log10 (Math.abs (min)));
    if (!Double.isFinite (minExp)) minExp = 0;
    double maxExp = Math.abs (LogEnhancement.log10 (Math.abs (max)));
    if (!Double.isFinite (maxExp)) maxExp = 0;
    if (Math.max (minExp, maxExp) >= 3) {
      pattern = "0.##E0";
    } // if
    else {
      double logint = LogEnhancement.log10 (interval);
      int decimals = (int) (logint < 0 ? -Math.floor (logint) : 0);
      pattern = (decimals == 0 ? "0" : "0.");
      for (int i = 0; i < decimals; i++) pattern += "#";
    } // else
    DecimalFormat format = new DecimalFormat (pattern);

    LOGGER.fine ("Decimal pattern is " + pattern);

    // Nudge min and max
    // -----------------
    double newMin = Math.ceil ((min + Double.MIN_VALUE - 
      interval)/interval)*interval;
    double newMax = Math.floor ((max - Double.MIN_VALUE + 
      interval)/interval)*interval;

    LOGGER.fine ("Nudged min is " + newMin);
    LOGGER.fine ("Nudged max is " + newMax);    

    // Create labels
    // -------------
    String[] labels;
    if (Double.isFinite (newMin) && Double.isFinite (newMax)) {
      int nticks = (int) Math.round ((newMax - newMin)/interval) + 1;
      List<String> labelList = new ArrayList<>();
      for (int i = 0; i < nticks; i++) {
        double val = newMin + i*interval;
        if (val < min || val > max) continue;
        labelList.add (format.format (val));
      } // for
      labels = labelList.toArray (new String[0]);
    } // if
    else labels = new String[0];

    if (labels.length == 0)
      LOGGER.fine ("No labels created!");
    else if (labels.length == 1)
      LOGGER.fine ("Created " + labels.length + " label, " + labels[0]);
    else if (labels.length >= 2)
      LOGGER.fine ("Created " + labels.length + " labels, [" + labels[0] + " .. " + labels[labels.length-1] + "]");

    return (labels);

  } // getLinearTickLabels

  ////////////////////////////////////////////////////////////

  /**
   * Creates a data color scale with the specified parameters.  The
   * font is set to the default font face, plain style, 12 point, the
   * preferred size to none,  the foreground color is set to black,
   * and the background color to none.
   *
   * @param function the enhancement function for translating between
   * data values and normalized values.  The scale is drawn for
   * normalized values in the range [0..1].
   * @param palette the palette to use for scale colors.
   * @param name the data variable name.
   * @param units the data variable units.
   */
  public DataColorScale (
    EnhancementFunction function,
    Palette palette,
    String name,
    String units
  ) {

    this (function, palette, name, units, null, null, Color.BLACK, null);

  } // DataColorScale constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a data color scale from the specified parameters.
   *
   * @param function the enhancement function for translating between
   * data values and normalized values.  The scale is drawn for
   * normalized values in the range [0..1].
   * @param palette the palette to use for scale colors.
   * @param name the data variable name.
   * @param units the data variable units.
   * @param dim the preferred scale dimensions, or null for none.
   * @param font the font for variable name, units, and scale values, or 
   * null for the default font face, plain style, 12 point.
   * @param fore the foreground color for legend lines and annotations.
   * @param back the background color, or null for none.
   */
  public DataColorScale (
    EnhancementFunction function,
    Palette palette,
    String name,
    String units,
    Dimension dim,
    Font font,
    Color fore,
    Color back
  ) {

    // Initialize variables
    // --------------------
    super (dim, font, fore, back);
    this.palette = palette;
    this.function = function;

    // Create tick labels
    // ------------------
    double min = function.getInverse (0);
    double max = function.getInverse (1);
    if (min > max) { double tmp = min; min = max; max = tmp; }
    if (function instanceof LogEnhancement)
      labels = getLogTickLabels (min, max);
    else
      labels = getLinearTickLabels (min, max, TICKS);

    // Create annotation string
    // ------------------------
    StringBuffer buffer = new StringBuffer();
    buffer.append (name.toUpperCase());
    if (!units.equals ("")) {
      buffer.append (" (");
      buffer.append (units);
      buffer.append (")");
    } // if
    annotation = buffer.toString().replace ('_', ' ');

  } // DataColorScale constructor

  ////////////////////////////////////////////////////////////

} // DataColorScale class

////////////////////////////////////////////////////////////////////////
