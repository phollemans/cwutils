////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataPlot.java
   Author: Peter Hollemans
     Date: 2002/10/04

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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import noaa.coastwatch.render.EarthContextElement;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.EarthPlotInfo;
import noaa.coastwatch.render.GraphicsServices;
import noaa.coastwatch.render.IconElement;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.Legend;
import noaa.coastwatch.render.Renderable;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The earth data plot class is used for rendering earth data
 * with legends.  The plot uses an earth data view and earth data
 * information object to render a data plot showing the view, its
 * associated view legend (if any), and an earth data information
 * legend with icon and context map.  The plot is laid out as follows:
 * <pre>
 *   +------------------------------------+
 *   |               border               |
 *   |  +----------------+  +--+  +----+  |
 *   |  |                |  |  |  |    |  |
 *   |  |                |  |  |  |    |  |
 *   |  |                |  |  |  |    |  |
 *   |  |                |  View  |Info|  |
 *   |  |Earth data view | legend legend  |
 *   |  |                |  |  |  |    |  |
 *   |  |                |  |  |  |    |  |
 *   |  |                |  |  |  |    |  |
 *   |  |                |  |  |  |    |  |
 *   |  |                |  |  |  |    |  |
 *   |  +----------------+  +--+  +----+  |
 *   |               border               |
 *   +------------------------------------+
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class EarthDataPlot
  implements Renderable {

  private static final Logger LOGGER = Logger.getLogger (EarthDataPlot.class.getName());

  // Constants
  // ---------
  /** The default color for the context bounding box. */
  private static final Color CONTEXT_BOUNDS_COLOR = Color.RED;

  // Variables
  // ---------
  /** The earth data view for rendering. */
  private EarthDataView view;

  /** The view legend, or null for none. */
  private Legend viewLegend;

  /** The earth plot information legend, or null for none. */
  private EarthPlotInfo infoLegend;

  /** The foreground drawing color. */
  private Color fore;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new earth data plot from the specified parameters.
   *
   * @param view the data view for rendering in the main plot area.
   * @param info the info for the earth data information legend.
   * If null, no info legend is drawn.
   * @param icon the icon for the the info legend.  This parameter can
   * be null if info is null.
   * @param fore the foreground drawing color for legend annotations.
   * @param back the background drawing color for legends, or null for
   * no backgrounds.
   * @param font the font for legend annotations, or null for the default
   * legend fonts.
   *
   * @see DataColorScale
   * @see EarthPlotInfo
   */  
  public EarthDataPlot (
    EarthDataView view,
    EarthDataInfo info,
    IconElement icon,
    Color fore,
    Color back,
    Font font
  ) {

    // Initialize view and legend
    // --------------------------
    this.view = view;
    Dimension viewSize = view.getSize (null);
    viewLegend = view.getLegend();
    if (viewLegend != null) {
      viewLegend.setFont (font);
      viewLegend.setForeground (fore);
      viewLegend.setBackground (back);
    } // if

    // Create info legend
    // ------------------
    if (info != null) {

      // We only create the context image here if it takes up less than a
      // quarter of the whole earth area.  Otherwise, there tends to be
      // problems with showing the bounding box on an orthographic projection.
      var area = view.getArea();
      EarthImageTransform viewTrans = view.getTransform();
      EarthContextElement context = null;
      var coverage = area.getCoverage();
      LOGGER.fine ("View coverage is " + coverage);
      if (area.getCoverage() < 0.25) {

        ImageTransform imageTrans = viewTrans.getImageTransform();
        EarthTransform earthTrans = viewTrans.getEarthTransform();
        DataLocation upperLeft = imageTrans.transform (new Point (0,0));
        DataLocation lowerRight = imageTrans.transform (new Point (
          viewSize.width-1, viewSize.height-1));

        context = new EarthContextElement (earthTrans,
          upperLeft, lowerRight);
        context.addBoundingBox (earthTrans, upperLeft, lowerRight, 
          CONTEXT_BOUNDS_COLOR, null);

      } // if 

      // Create legend
      // -------------
      EarthLocation center = viewTrans.transform (new Point (
        (viewSize.width-1)/2, (viewSize.height-1)/2));
      infoLegend = new EarthPlotInfo (icon, info, area, 
        context, center, null, font, fore, back);

    } // if
    else infoLegend = null;

    // Set foreground color
    // --------------------    
    this.fore = fore;
 
  } // EarthDataPlot constructor

  ////////////////////////////////////////////////////////////

  /** Gets the rendered plot size. */
  public Dimension getSize (
    Graphics2D g
  ) {

    // Get maximum height
    // ------------------
    Dimension viewSize = view.getSize(null);
    int maxHeight = viewSize.height; 
    Dimension viewLegendSize = (viewLegend != null ? viewLegend.getSize(g) :
      new Dimension());
    maxHeight = Math.max (maxHeight, viewLegendSize.height);
    Dimension infoLegendSize = (infoLegend != null ? infoLegend.getSize(g) :
      new Dimension());
    maxHeight = Math.max (maxHeight, infoLegendSize.height);

    // Initialize
    // ----------
    Dimension size = new Dimension (viewSize.width, maxHeight);

    // Add view legend size
    // --------------------
    if (viewLegend != null) {
      size.width += Legend.SPACE_SIZE*2 + viewLegendSize.width;
      if (viewLegendSize.height != size.height)
        viewLegend.setPreferredSize (new Dimension (viewLegendSize.width, size.height));
    } // if
 
    // Add info legend size
    // --------------------
    if (infoLegend != null) {
      size.width += Legend.SPACE_SIZE*2 + infoLegendSize.width;
      if (infoLegendSize.height != size.height)
        infoLegend.setPreferredSize (new Dimension (0, size.height));
    } // if

    // Adjust view size
    // ----------------
    if (viewSize.height != size.height) {
      try { view.resizeHeight (size.height); }
      catch (NoninvertibleTransformException e) { }
      Dimension newViewSize = view.getSize(null);
      size.width += (-viewSize.width) + newViewSize.width;
    } // if

    // Add border size
    // ---------------
    size.width += Legend.SPACE_SIZE*4;
    size.height += Legend.SPACE_SIZE*4;

    return (size);

  } // getSize

  ////////////////////////////////////////////////////////////

  /**
   * Renders the plot to the graphics device.
   *
   * @param g the graphics device for rendering.
   */
  public void render (
    Graphics2D g 
  ) {

    // Initialize
    // ----------
    Dimension size = getSize(g);

    // Render view
    // -----------
    Dimension viewSize = view.getSize(null);
    Point topLeft = new Point (Legend.SPACE_SIZE*2, size.height/2 - 
      viewSize.height/2);
    Rectangle clip = g.getClipBounds();
    Rectangle viewRect = new Rectangle (topLeft.x, topLeft.y, viewSize.width,
      viewSize.height);
    g.setClip (viewRect);
    AffineTransform saved = g.getTransform();
    g.translate (topLeft.x, topLeft.y);
    view.render (g);
    g.setTransform (saved);
    g.setClip (clip);
    g.setColor (fore);
    g.setStroke (Legend.DEFAULT_STROKE);
    GraphicsServices.drawRect (g, viewRect);
    topLeft.x += viewSize.width + Legend.SPACE_SIZE*2;

    // Render view legend
    // ------------------
    if (viewLegend != null) {
      Dimension legendSize = viewLegend.getSize(g);
      topLeft.y = size.height/2 - legendSize.height/2;
      viewLegend.render (g, topLeft.x, topLeft.y);
      topLeft.x += legendSize.width + Legend.SPACE_SIZE*2;
    } // if

    // Render info legend
    // ------------------
    if (infoLegend != null) {
      Dimension legendSize = infoLegend.getSize(g);
      topLeft.y = size.height/2 - legendSize.height/2;
      infoLegend.render (g, topLeft.x, topLeft.y);
    } // if

  } // render

  ////////////////////////////////////////////////////////////

} // EarthDataPlot class

////////////////////////////////////////////////////////////////////////
