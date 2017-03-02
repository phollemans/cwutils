////////////////////////////////////////////////////////////////////////
/*

     File: CoastOverlay.java
   Author: Peter Hollemans
     Date: 2002/07/25

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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import noaa.coastwatch.render.feature.BinnedGSHHSReader;
import noaa.coastwatch.render.feature.BinnedGSHHSReaderFactory;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.PolygonOverlay;
import noaa.coastwatch.util.EarthArea;

/**
 * A <code>CoastOverlay</code> annotates an earth data view with
 * coast lines and polygons.  The coast lines are rendered using
 * GSHHS coast line vector data (see {@link BinnedGSHHSReader}
 * and {@link BinnedGSHHSReaderFactory}).  The resolution of the
 * rendered earth vector data is based on the view resolution at
 * the time of rendering.  The resolution of the view is taken to
 * be the number of kilometers per pixel at the view center
 * point.  In order to help reduce the rendering time and keep
 * the view clear of unwanted coast line "dust", by default small
 * polygons such as the coasts of islands and lakes enclosing
 * less than 3x3 pixels are not rendered.<p>
 *
 * The overlay color and fill color are used to determine if coast
 * lines and/or polygons should be rendered.  If the overlay color is
 * set to null, the coast lines are not rendered.  Likewise if the
 * fill color is set to null, land polygons are not rendered.  By
 * default the fill color is set to null.<p>
 *
 * The default reader factory for coastline data is a standard {@link
 * BinnedGSHHSReaderFactory} unless the {@link #setReaderFactory} method
 * is called.<p>
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class CoastOverlay 
  extends PolygonOverlay {

  /** The serialization constant. */
  private static final long serialVersionUID = 2437261200101474557L;

  // Variables
  // ---------

  /** The binned GSHHS reader factory used for coastline data. */
  private transient BinnedGSHHSReaderFactory readerFactory =
    BinnedGSHHSReaderFactory.getInstance();

  /** The current GSHHS reader used for coastline data. */
  private transient BinnedGSHHSReader coast;

  /** The flag for full rendering including small polygons. */
  private boolean smallPolygons = false;

  ////////////////////////////////////////////////////////////

  /** Sets the read factory for new coastline readers. */
  public void setReaderFactory (BinnedGSHHSReaderFactory factory) {

    this.readerFactory = factory;

  } // setReaderFactory

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the small polygons flag which determines if polygons smaller
   * than 3x3 view pixels are rendered.  By default small polygons are
   * not rendered.
   */
  public void setSmallPolygons (boolean flag) { 

    if (smallPolygons != flag) {
      smallPolygons = flag;
      prepared = false;
    } // if

  } // setSmallPolygons

  ////////////////////////////////////////////////////////////

  /** Gets the small polygons flag. */
  public boolean getSmallPolygons () { return (smallPolygons); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new coast overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param fillColor the fill color to use for polygon fills.
   */
  public CoastOverlay (
    Color color,
    int layer,
    Stroke stroke,
    Color fillColor
  ) { 

    // Initialize
    // ----------
    super (color, layer, stroke, fillColor);
    coast = null;

  } // CoastOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new coast overlay.  The layer number is initialized
   * to 0, the stroke to the default <code>BasicStroke</code>, and the
   * fill color to null.
   *
   * @param color the overlay color.
   */
  public CoastOverlay (
    Color color
  ) { 

    super (color);
    setFillColor (null);
    coast = null;

  } // CoastOverlay constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the polygon fill color.  This method overrides the parent to
   * detect null/non-null changes in the polygon fill color.
   */
  public void setFillColor (Color newColor) {

    Color oldColor = getFillColor();
    if ((oldColor == null && newColor != null) ||
        (oldColor != null && newColor == null)) {
        prepared = false;
    } // if

    super.setFillColor (newColor);

  } // setFillColor

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Get required parameters
    // -----------------------
    EarthArea viewArea = view.getArea();
    double res = view.getResolution();
    /**
     * We do this next line because the de-serialized coast overlay
     * will have a null reader factory.
     */
    if (readerFactory == null)
      readerFactory = BinnedGSHHSReaderFactory.getInstance();
    String viewDatabase = readerFactory.getDatabaseName (
      BinnedGSHHSReaderFactory.COAST, 
      BinnedGSHHSReaderFactory.getDatabaseLevel (res)
    );

    // Check if coastline reload required
    // ----------------------------------
    if (coast != null && prepared) {
      EarthArea selectedArea = coast.getArea();
      String selectedDatabase = coast.getDatabase();
      if (viewDatabase.equals (selectedDatabase)) {
        if (viewArea.equals (selectedArea))
          return;
      } // if
      else coast = null;
    } // if

    // Load coastline data
    // -------------------
    try {
      if (coast == null) 
        coast = readerFactory.getPolygonReader (viewDatabase);
      coast.setMinArea ((smallPolygons ? -1 : Math.pow (3*res, 2)));
      coast.setPolygonRendering (getFillColor() != null);
      coast.select (viewArea);
    } // try
    catch (Exception e) { e.printStackTrace(); coast = null; }

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check coast
    // -----------
    if (coast == null) return;

    // Set rendering hint for stroke accuracy
    // --------------------------------------
    Object strokeHint = g.getRenderingHint (RenderingHints.KEY_STROKE_CONTROL);
    g.setRenderingHint (RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE);

    // Draw land polygons
    // ------------------
    EarthImageTransform trans = view.getTransform();
    Color fillColor = getFillColorWithAlpha();
    if (fillColor != null) {
      Object aliasHint = g.getRenderingHint (RenderingHints.KEY_ANTIALIASING);
      g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
        RenderingHints.VALUE_ANTIALIAS_OFF);
      g.setColor (fillColor);
      coast.renderPolygons (g, trans);
      if (aliasHint != null) 
        g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, aliasHint);
    } // if

    // Draw coastlines
    // ---------------
    Color color = getColorWithAlpha();
    if (color != null) {
      g.setColor (color);
      g.setStroke (getStroke());
      coast.render (g, trans);
    } // if

    // Restore rendering hint for stroke accuracy
    // ------------------------------------------
    if (strokeHint != null) 
      g.setRenderingHint (RenderingHints.KEY_STROKE_CONTROL, strokeHint);

  } // draw

  ////////////////////////////////////////////////////////////

} // CoastOverlay class

////////////////////////////////////////////////////////////////////////
