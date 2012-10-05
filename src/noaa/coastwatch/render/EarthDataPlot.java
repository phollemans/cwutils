////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataPlot.java
  PURPOSE: A class to set up a plot of Earth data with legends.
   AUTHOR: Peter Hollemans
     DATE: 2002/10/04
  CHANGES: 2002/10/14, PFH, added Renderable interface
           2003/11/21, PFH, modified for updates in EarthContextElement
           2004/09/09, PFH, changed SatelliteDataPlot to EarthDataPlot
           2004/10/08, PFH, fixed to initialize stroke before drawing
           2006/11/20, PFH, modified to use GraphicsServices.drawRect()

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * The Earth data plot class is used for rendering Earth data
 * with legends.  The plot uses an Earth data view and Earth data
 * information object to render a data plot showing the view, its
 * associated view legend (if any), and an Earth data information
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
 */
public class EarthDataPlot
  implements Renderable {

  // Constants
  // ---------
  /** The default color for the context bounding box. */
  private static final Color CONTEXT_BOUNDS_COLOR = Color.RED;

  // Variables
  // ---------
  /** The Earth data view for rendering. */
  private EarthDataView view;

  /** The view legend, or null for none. */
  private Legend viewLegend;

  /** The Earth plot information legend, or null for none. */
  private EarthPlotInfo infoLegend;

  /** The foreground drawing color. */
  private Color fore;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new Earth data plot from the specified parameters.
   *
   * @param view the data view for rendering in the main plot area.
   * @param info the info for the Earth data information legend.
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
    Dimension viewSize = view.getSize(null);
    viewLegend = view.getLegend();
    if (viewLegend != null) {
      viewLegend.setFont (font);
      viewLegend.setForeground (fore);
      viewLegend.setBackground (back);
    } // if

    // Create info legend
    // ------------------
    if (info != null) {

      // Create context element
      // ----------------------
      EarthImageTransform viewTrans = view.getTransform();
      ImageTransform imageTrans = viewTrans.getImageTransform();
      EarthTransform earthTrans = viewTrans.getEarthTransform();
      DataLocation upperLeft = imageTrans.transform (new Point (0,0));
      DataLocation lowerRight = imageTrans.transform (new Point (
        viewSize.width-1, viewSize.height-1));
      EarthContextElement context = new EarthContextElement (earthTrans,
        upperLeft, lowerRight);
      context.addBoundingBox (earthTrans, upperLeft, lowerRight, 
        CONTEXT_BOUNDS_COLOR, null);
      EarthLocation center = viewTrans.transform (new Point (
        (viewSize.width-1)/2, (viewSize.height-1)/2));

      // Create legend
      // -------------
      infoLegend = new EarthPlotInfo (icon, info, view.getArea(), 
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
        viewLegend.setPreferredSize (new Dimension (0, size.height));
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
