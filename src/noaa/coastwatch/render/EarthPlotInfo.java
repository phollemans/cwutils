////////////////////////////////////////////////////////////////////////
/*

     File: EarthPlotInfo.java
   Author: Peter Hollemans
     Date: 2002/10/03

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
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.render.EarthContextElement;
import noaa.coastwatch.render.GraphicsServices;
import noaa.coastwatch.render.IconElement;
import noaa.coastwatch.render.Legend;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.tools.cwinfo;

/**
 * An earth plot information legend annotates an earth data view with
 * information about the data source, date, time, projection, and
 * earth location.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class EarthPlotInfo 
  extends Legend {

  // Constants
  // ---------
  /** The minimum picture element size. */
  private static final int MIN_PICTURE_SIZE = 50;

  /** The default date format. */
  private static final String DATE_FMT = cwinfo.DATE_FMT;

  /** The UTC time format. */
  private static final String TIME_FMT = cwinfo.TIME_FMT;

  /** The local time format. */
  private static final String LOCAL_TIME_FMT = "HH:mm:ss Z";

  // Variables
  // ---------
  /** The icon element for the top of the legend. */
  private IconElement icon;

  /** The earth data information for legend text. */
  private String[] labels;

  /** The earth context element for the bottom of the legend. */
  private EarthContextElement context;

  ////////////////////////////////////////////////////////////

  /**
   * Creates an earth plot information legend from the specified
   * parameters.  The font is set to the default font face, plain
   * style, 12 point, the preferred size to none, the foreground color
   * is set to black, and the background color to none.
   *
   * @param icon the icon element to use for the top of the legend.
   * @param info the earth data information for text annotations.
   * @param area the earth area for geographic bounds.
   * @param context the earth context for the bottom of the legend.
   */
  public EarthPlotInfo (
    IconElement icon,
    EarthDataInfo info,
    EarthArea area,    
    EarthContextElement context,
    EarthLocation center
  ) {

    this (icon, info, area, context, center, null, null, Color.BLACK, null);

  } // EarthPlotInfo constructor

  ////////////////////////////////////////////////////////////

  /**
   * Wraps and truncates a string value to fit on a specific number of lines
   * and line width.
   *
   * @param input the input string to work on.
   * @param indent the number of spaces of indent at the start of each line,
   * or 0 for no indenting.
   * @param maxLength the maximum length of each line.
   * @param maxLines the maximum number of lines in the output array.
   *
   * @return the output list of lines.
   */
  private static List<String> wrapTruncateValue (
    String input,
    int indent,
    int maxLength,
    int maxLines
  ) {

    // Create initial wrapped version of output
    // ----------------------------------------
    List<String> outputLines = new ArrayList<String>();
    outputLines.addAll (Arrays.asList (GUIServices.lineWrap (input, maxLength-indent).split ("\n")));

    // Truncate the number of lines
    // ----------------------------
    if (outputLines.size() > maxLines) {
      outputLines = outputLines.subList (0, maxLines);
      String lastLine = outputLines.get (outputLines.size()-1);
      if (lastLine.length() > maxLength-3) lastLine = lastLine.substring (0, maxLength-3);
      lastLine = lastLine + "...";
      outputLines.set (outputLines.size()-1, lastLine);
    } // if

    // Indent the lines
    // ----------------
    if (indent > 0) {
      String indentStr = "";
      for (int i = 0; i < indent; i++) indentStr += " ";
      for (int i = 0; i < outputLines.size(); i++)
        outputLines.set (i, indentStr + outputLines.get (i));
    } // if

    // Truncate each line in the middle
    // --------------------------------
    for (int i = 0; i < outputLines.size(); i++) {
      String line = outputLines.get (i);
      if (line.length() > maxLength) {
        line = line.substring (0, maxLength/2-2) + "..." +
          line.substring (line.length()-(maxLength/2-1), line.length());
        outputLines.set (i, line);
      } // if
    } // for

    return (outputLines);

  } // wrapTruncateValue

  ////////////////////////////////////////////////////////////

  /**
   * Creates an earth plot information legend from the specified
   * parameters.
   *
   * @param icon the icon element to use for the top of the legend.
   * @param info the earth data information for text annotations.
   * @param area the earth area for geographic bounds.
   * @param context the earth context for the bottom of the legend.
   * @param center the earth location of the plot center.
   * @param dim the preferred scale dimensions, or null for none.
   * @param font the font for variable name, units, and scale values, or 
   * null for the default font face, plain style, 12 point.
   * @param fore the foreground color for legend lines and annotations.
   * @param back the background color, or null for none.
   */
  public EarthPlotInfo (
    IconElement icon,
    EarthDataInfo info,
    EarthArea area,    
    EarthContextElement context,
    EarthLocation center,
    Dimension dim,
    Font font,
    Color fore,
    Color back
  ) {

    // Initialize pictures
    // -------------------
    super (dim, font, fore, back);
    this.icon = icon;
    this.context = context;

    // Create strings
    // --------------
    List<String> strings = new ArrayList<String>();

    // Add origin
    // ----------
    strings.add ("Data courtesy of:");
    String origin = MetadataServices.format (info.getOrigin(), " ");
    String[] originArray = GUIServices.lineWrap (origin, 20).split ("\n");
    strings.addAll (Arrays.asList (originArray));
    strings.add (" ");

    // Add data source
    // ---------------
    if (info instanceof SatelliteDataInfo) {
      SatelliteDataInfo satInfo = (SatelliteDataInfo) info;
      strings.add ("Satellite:");
      String sat = satInfo.getSatellite().toUpperCase();
      strings.addAll (wrapTruncateValue (MetadataServices.format (sat, ", "), 2, 20, 3));
      strings.add ("Sensor:");
      String sensor = satInfo.getSensor().toUpperCase();
      strings.addAll (wrapTruncateValue (MetadataServices.format (sensor, ", "), 2, 20, 3));
    } // if
    else {
      strings.add ("Data source:");
      String source = info.getSource().toUpperCase();
      strings.addAll (wrapTruncateValue (MetadataServices.format (source, ", "), 2, 20, 3));
    } // else

    // Add single time info
    // --------------------
    if (info.isInstantaneous()) {
      Date startDate = info.getStartDate();
      strings.add ("Date:");
      strings.add ("  " + DateFormatter.formatDate (startDate, DATE_FMT));
      strings.add ("Time:");
      strings.add ("  " + DateFormatter.formatDate (startDate, TIME_FMT));
      strings.add ("  " + DateFormatter.formatDate (startDate, LOCAL_TIME_FMT, 
        center));
      strings.add ("Scene time:");
      strings.add ("  " + info.getSceneTime (context.getUpperLeft(), 
        context.getLowerRight()).toUpperCase());
    } // if

    // Add time range info
    // -------------------
    else {
      Date startDate = info.getStartDate();
      Date endDate = info.getEndDate();
      String startDateString = DateFormatter.formatDate (startDate, DATE_FMT);
      String endDateString = DateFormatter.formatDate (endDate, DATE_FMT);
      String startTimeString = DateFormatter.formatDate (startDate, TIME_FMT);
      String endTimeString = DateFormatter.formatDate (endDate, TIME_FMT);
      if (startDateString.equals (endDateString)) {
        strings.add ("Date:");
        strings.add ("  " + startDateString);
        strings.add ("Start time:");
        strings.add ("  " + startTimeString);
        strings.add ("End time:");
        strings.add ("  " + endTimeString);
      } // if
      else {
        strings.add ("Start date:");
        strings.add ("  " + startDateString);
        strings.add ("Start time:");
        strings.add ("  " + startTimeString);
        strings.add ("End date:");
        strings.add ("  " + endDateString);
        strings.add ("End time:");
        strings.add ("  " + endTimeString);
      } // else
    } // else

    // Add projection type
    // -------------------
    EarthTransform trans = info.getTransform ();
    strings.add ("Projection type:");
    strings.add ("  " + (trans == null ? "unknown" : 
      trans.describe().toUpperCase()));

    // Add mapped projection attributes
    // --------------------------------
    if (trans instanceof MapProjection) {
      MapProjection map = (MapProjection) trans;
      strings.add ("Map projection:");
      if (map.getSystem() == GCTP.GEO) {
        DecimalFormat format = new DecimalFormat ("0.####");
        AffineTransform affine = map.getAffine();
        double[] matrix = new double[6];
        affine.getMatrix(matrix);
        strings.add ("  " + format.format (matrix[2]) + " deg/pixel");
      } // if
      else {
        DecimalFormat format = new DecimalFormat ("0.##");
        double res = map.getPixelSize();
        strings.add ("  " + format.format (res/1000) + " km/pixel");
      } // else
      String[] proj = map.getSystemName().toUpperCase().split(" ");
      for (int i = 0; i < proj.length; i++) 
        strings.add ("  " + proj[i]);
    } // if

    // Add geographic bounds
    // ---------------------
    int[] extremes = area.getExtremes();
    strings.add ("Latitude bounds:");
    String latBounds = 
      EarthLocation.formatSingle (extremes[1], EarthLocation.D, 
      EarthLocation.LAT) + " -> " +
      EarthLocation.formatSingle (extremes[0], EarthLocation.D, 
      EarthLocation.LAT);
    strings.add ("  " + latBounds);

    strings.add ("Longitude bounds:");
    String lonBounds = 
      EarthLocation.formatSingle (extremes[3], EarthLocation.D, 
      EarthLocation.LON) + " -> " +
      EarthLocation.formatSingle (extremes[2], EarthLocation.D, 
      EarthLocation.LON);
    strings.add ("  " + lonBounds);

    // Create labels array
    // -------------------
    labels = new String[strings.size()];
    strings.toArray (labels);

  } // EarthPlotInfo constructor

  ////////////////////////////////////////////////////////////

  public void render (
    Graphics2D g,
    int x,
    int y
  ) {

    // Initialize
    // ----------
    Dimension size = getSize(g);
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

    // Render icon
    // -----------
    int pictureSize = size.width - SPACE_SIZE*4;
    icon.setPreferredSize (new Dimension (pictureSize, pictureSize));
    Rectangle iconBounds = icon.getBounds(g);
    icon.setPosition (new Point (x + SPACE_SIZE*2 + pictureSize/2 -
      iconBounds.width/2, y + SPACE_SIZE*2 + pictureSize/2 - 
      iconBounds.height/2));
    icon.render (g, fore, back);

    // Render context
    // --------------
    context.setPreferredSize (new Dimension (pictureSize, pictureSize));
    Rectangle contextBounds = context.getBounds(g);
    Point contextPos = new Point (x + SPACE_SIZE*2 + 
      pictureSize/2 - contextBounds.width/2, (y + size.height - 1) - 
      SPACE_SIZE*2 - pictureSize/2 - contextBounds.height/2);
    context.setPosition (contextPos);
    Color highBack = null;
    if (back != null) {
      highBack = new Color (
        (int) Math.min (back.getRed()*1.05, 255),
        back.getGreen(),
        back.getBlue());
    } // if
    context.render (g, fore, highBack);
    g.setColor (fore);
    GraphicsServices.drawRect (g, new Rectangle (contextPos.x, contextPos.y, 
      contextBounds.width, contextBounds.height));

    // Loop over labels
    // ----------------
    Point2D.Float start = new Point2D.Float(x + SPACE_SIZE*2, 
      y + SPACE_SIZE*4 + pictureSize);
    boolean origin = true;
    FontRenderContext renderContext = g.getFontRenderContext();
    for (int i = 0; i < labels.length; i++) {

      // Create label
      // ------------
      TextLayout layout = new TextLayout (labels[i], font, renderContext);
      start.y += layout.getAscent();
      TextElement labelElement = new TextElement (labels[i], font,
        start, new double[] {0, 0}, 0);

      // Center origin
      // -------------
      if (labels[i] == " ") origin = false;
      if (origin) {
        labelElement.setBasePoint (new Point2D.Float (x + size.width/2.0f,
          start.y));
        labelElement.setAlignment (new double[] {0.5, 0}); 
      } // if

      // Render label and advance
      // ------------------------
      labelElement.render (g, fore, null);
      start.y += layout.getDescent() + layout.getLeading();

    } // for

  } // render

  ////////////////////////////////////////////////////////////

  public Dimension getSize (
    Graphics2D g
  ) {

    // Find labels dimensions 
    // ----------------------
    Point2D.Float start = new Point2D.Float();
    FontRenderContext renderContext = g.getFontRenderContext();
    float maxAdvance = 0;
    for (int i = 0; i < labels.length; i++) {
      TextLayout layout = new TextLayout (labels[i], font, renderContext);
      maxAdvance = Math.max (layout.getAdvance(), maxAdvance);
      start.y += layout.getAscent() + layout.getDescent() + 
        layout.getLeading();
    } // for
    int textWidth = (int) Math.ceil (maxAdvance);
    int textHeight = (int) Math.ceil (start.y);

    // Calculate width
    // ---------------
    Dimension size = new Dimension();
    size.width = SPACE_SIZE*4 + Math.max (textWidth, MIN_PICTURE_SIZE);

    // Calculate height
    // ----------------
    int requiredHeight = textHeight + (size.width - SPACE_SIZE*4)*2 + 
      SPACE_SIZE*8;
    if (preferredSize != null)
      size.height = Math.max (preferredSize.height, requiredHeight);
    else
      size.height = requiredHeight;

    return (size);

  } // getSize

  ////////////////////////////////////////////////////////////

} // EarthPlotInfo class

////////////////////////////////////////////////////////////////////////
