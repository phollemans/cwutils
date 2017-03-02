////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthContextElement.java
  PURPOSE: A class to handle an earth context annotation element.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/30
  CHANGES: 2002/10/23, PFH, set default stroke to beveled joins
           2002/22/08, PFH, added minimum context resolution
           2002/12/03, PFH, modified for map projection changes
           2002/12/12, PFH, modified for earth vector transform() change
           2002/12/30, PFH, changed to use binned GSHHS reader
           2003/01/15, PFH, modified for global re-centerable views, grid
             lines, earth edge, and solar zenith terminator
           2003/03/29, PFH, added bounding box polygon check for swath
           2003/10/04, PFH, added handling for null coastline data
           2003/11/16, PFH
             - added setContextArea(EarthArea)
             - added get/set for context factor
             - added more documentation to class description
           2003/11/20, PFH, added labels to bounding boxes
           2003/11/21, PFH, added getBoundingBox(), getBoundingBoxes(),
             and getEarthImageTransform()
           2003/12/10, PFH, changed LineFeatureReader to LineFeatureSource
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/04/01, PFH, added render() exception for invalid transform
           2004/09/30, PFH, modified to use EarthTransform.getBoundingBox()
           2005/02/01, PFH, changed to output warnings to System.out
           2005/05/21, PFH, modified to handle earth location datum
           2005/05/27, PFH, modified to not draw discontinuous polygons
           2005/12/20, PFH, corrected label colors to match docs
           2006/05/26, PFH, modified to use SpheroidConstants
           2006/06/10, PFH, modified to use BinnedGSHHSReaderFactory
           2012/12/07, PFH, modified to use MapProjectionFactory
           2014/03/25, PFH
           - Changes: Updated to use new EarthImageTransform constructor

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.render.feature.BinnedGSHHSReader;
import noaa.coastwatch.render.feature.BinnedGSHHSReaderFactory;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.GraphicsServices;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.feature.LineFeature;
import noaa.coastwatch.render.feature.LineFeatureSource;
import noaa.coastwatch.render.PictureElement;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SolarZenith;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.trans.SpheroidConstants;
import noaa.coastwatch.util.trans.SwathProjection;

/**
 * The earth context element is a picture element that renders a
 * simplified view of the earth with coastlines and the bounding boxes
 * of a number of arbitrarily shaped areas.  The earth is shown in an
 * orthographic projection.  The picture element is designed similarly
 * to a National Geographic magazine map legend, which shows a little
 * version of the big map with extra context information around it in
 * the form of coastlines and grid lines in order to give the user
 * some idea of where on the earth the current map is located.  In a
 * context element, the "context area" is the area of interest,
 * usually the boundaries of some earth dataset.  By default the
 * largest dimension of the context area is about 15% of the element
 * size.  This size factor may be set, along with other properties
 * such as the center point and context area itself.<p>
 *
 * The context element may be set up to highlight not only the central
 * context area, but a set of individual areas via the specification
 * of polygon bounding boxes.  The box edges are rendered in a highlighted
 * color, and filled with a slightly darker, semi-transparent version 
 * of the same color.  The bounding boxes may optionally be annotated with 
 * text labels, which are centered on each box area and rendered in a
 * default 12 point font using the same color as the box edges.<p>
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class EarthContextElement
  extends PictureElement {

  // Constants
  // ---------
  /** The default picture size. */
  private final static int DEFAULT_SIZE = 100;  

  /** The number of segments for bounding box tracing. */
  private final static int SEGMENTS = 4;

  /** The multiplication factor for polygon fill color. */
  private final static double FILL_FACTOR = 0.65;

  /** The multiplication factor for polygon label color. */
  private final static double LABEL_FACTOR = 2.5;

  /** The alpha value for polygon label color. */
  private final static double LABEL_ALPHA = 1.0;

  /** The alpha value for polygon drawing. */
  private final static double ALPHA = 0.5;

  /** The context area to total area size factor. */
  private final static double SIZE_FACTOR = 0.15;

  /** The GSHHS minimum area cutoff. */
  private static final int AREA_CUTOFF = 100*100;

  /** The scale factor for non-alpha buffered image renderings. */
  private static final int BUFFER_SCALE = 4;

  /** The grid point increment. */
  private static final int GRID_DOT = 2;

  // Variables
  // ---------
  /** The coastline data for display. */
  private static LineFeatureSource coast = null;

  /** The list of polygons to draw for bounding boxes. */
  private List polygons;

  /** The list of polygon labels. */
  private List labels;

  /** The list of polygon colors. */
  private List colors;

  /** The earth image transform for geographic to image coordinates. */
  private EarthImageTransform earthImageTrans;

  /** The context earth transform. */
  private EarthTransform contextTrans;

  /** The context upper-left corner data coordinates. */
  private DataLocation contextUpperLeft;

  /** The context lower-right corner data coordinates. */
  private DataLocation contextLowerRight;

  /** The show grid flag, true for grid display. */
  private boolean showGrid;

  /** The show edge flag, true to show the edge of the Earth. */
  private boolean showEdge;

  /** The grid increment in degrees. */
  private int gridInc = 30;

  /** The solar zenith object. */
  private SolarZenith sz = null;

  /** The solar zenith colors. */
  private Color dayColor = null, nightColor = null;

  /** The context size to total size factor. */
  private double sizeFactor = SIZE_FACTOR;

  /** The font for drawing bounding box labels. */
  private Font labelFont = new Font (null, Font.PLAIN, 12);

  ////////////////////////////////////////////////////////////

  /** Gets the font for rendering bounding box labels. */
  public Font getLabelFont() { return (labelFont); }

  ////////////////////////////////////////////////////////////

  /** Sets the font for rendering bounding box labels. */
  public void setLabelFont (Font font) { labelFont = font; }

  ////////////////////////////////////////////////////////////

  /** Gets the context size to total size factor. */
  public double getSizeFactor () { return (sizeFactor); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the context size to total size factor.  This method must be
   * called prior to setting the context area in order to have any
   * effect.
   */
  public void setSizeFactor (double factor) { sizeFactor = factor; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the solar zenith fill colors.  If null, no filling is done
   * for the solar zenith day or night areas.
   *
   * @see #setSolarZenith
   */
  public void setSolarZenithFill (
    Color dayColor,
    Color nightColor
  ) {

    this.dayColor = dayColor;
    this.nightColor = nightColor;

  } // setSolarZenithFill

  ////////////////////////////////////////////////////////////

  /** Sets the edge flag to render the earth edges. */
  public void setEdge (boolean flag) { showEdge = flag; }

  ////////////////////////////////////////////////////////////

  /** Sets the grid flag to render a global grid. */
  public void setGrid (boolean flag) { showGrid = flag; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the grid increment in degrees.  By default, the grid
   * increment is 30.
   */
  public void setGridIncrement (int inc) { gridInc = inc; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the solar zenith object.  If null, no solar zenith line is
   * drawn. 
   */
  public void setSolarZenith (SolarZenith sz) { this.sz = sz; }

  ////////////////////////////////////////////////////////////

  /**
   * Adds a bounding box to this context element.  The new bounding box
   * is created by tracing the edges of the data window at regular
   * intervals to form a closed polygon.
   *
   * @param trans the earth transform for converting data to
   * geographic coordinates.
   * @param upperLeft the upper-left corner of the data window.
   * @param lowerRight the lower-right corner of the data window.
   * @param color the bounding box color.
   * @param label the bounding box label, or null for no label.
   */
  public void addBoundingBox (
    EarthTransform trans,
    DataLocation upperLeft,
    DataLocation lowerRight,
    Color color,
    String label
  ) {

    // Adjust corners for swath projection
    // -----------------------------------
    if (trans instanceof SwathProjection) {
      int[] dims = ((SwathProjection) trans).getDimensions();
      upperLeft = upperLeft.truncate (dims);
      lowerRight = lowerRight.truncate (dims);
    } // if

    // Add polygon and color
    // ---------------------
    LineFeature polygon = ((EarthTransform2D) trans).getBoundingBox (upperLeft, 
      lowerRight, SEGMENTS);
    addBoundingBox (polygon, color, label);

  } // addBoundingBox

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the upper-left corner of the context area.  This method is
   * only valid if a context area is defined.
   */
  public DataLocation getUpperLeft () { 

    return ((DataLocation) contextUpperLeft.clone()); 

  } // getUpperLeft

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the lower-right corner of the context area.  This method is
   * only valid if a context area is defined.
   */
  public DataLocation getLowerRight () {
 
    return ((DataLocation) contextLowerRight.clone()); 

  } // getLowerRight

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the earth transform used in the context area, or null if no
   * context area is defined. 
   */
  public EarthTransform getTransform() { return (contextTrans); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the area which should be used as the center of attention in
   * the context element using an earth area object.  The context area
   * apears centered and magnified.
   *
   * @param area the earth area to use for the context area.  The area
   * center point and context size are determined by examining the
   * extreme latitude and longitude values.
   *
   * @see #setContextArea(EarthTransform,DataLocation,DataLocation)
   */
  public void setContextArea (
    EarthArea area
  )  {

    // Set area parameters
    // -------------------
    contextTrans = null;
    contextUpperLeft = null;
    contextLowerRight = null;

    // Get context area center and size
    // --------------------------------
    int[] extremes = area.getExtremes();
    EarthLocation center = new EarthLocation ((extremes[0] + extremes[1])/2,
      (extremes[2] + extremes[3])/2);
    double contextSize = Math.max (
      new EarthLocation (center.lat, extremes[2]).distance (
        new EarthLocation (center.lat, extremes[3])),
      new EarthLocation (extremes[0], center.lon).distance (
        new EarthLocation (extremes[1], center.lon))
    );

    // Get new transform
    // -----------------
    earthImageTrans = getContextProjection (center, contextSize);

  } // setContextArea

  ////////////////////////////////////////////////////////////

  /**
   * Sets the area which should be used as the center of attention in
   * the context element using an earth transform and data location
   * bounds.  The context area appears centered and magnified.
   *
   * @param trans the earth transform for converting data to
   * geographic coordinates.
   * @param upperLeft the upper-left corner of the data window.
   * @param lowerRight the lower-right corner of the data window.
   *
   * @see #setContextArea(EarthArea)
   */
  public void setContextArea (
    EarthTransform trans,
    DataLocation upperLeft,
    DataLocation lowerRight
  ) {

    // Set area parameters
    // -------------------
    contextTrans = trans;
    contextUpperLeft = (DataLocation) upperLeft.clone();
    contextLowerRight = (DataLocation) lowerRight.clone();

    // Create new transform
    // --------------------
    earthImageTrans = getContextProjection (trans, upperLeft, lowerRight);

  } // setContextArea

  ////////////////////////////////////////////////////////////

  /**
   * Gets a local map projection appropriate for the specified context
   * area.
   *
   * @param center the context area center location.
   * @param contextSize the context area size in kilometers.
   *
   * @return the context map projection.
   */
  private EarthImageTransform getContextProjection (
    EarthLocation center,
    double contextSize
  ) {

    // Calculate resolution
    // --------------------
    Rectangle rect = getBounds (null);
    Dimension imageDims = new Dimension (rect.width, rect.height);
    int dataDims[] = new int[] {imageDims.height, imageDims.width};
    double maxRes = (SpheroidConstants.STD_RADIUS*2)/rect.width;
    double minRes = maxRes / 4;
    double res;
    if (Double.isNaN (contextSize)) 
      res = maxRes;
    else {
      res = contextSize/(rect.width * sizeFactor);
      if (res > maxRes) res = maxRes;
      else if (res < minRes) res = minRes;
    } // else

    // Create map projection
    // ---------------------
    double[] parameters = new double[15];
    parameters[4] = GCTP.pack_angle (center.lon);
    parameters[5] = GCTP.pack_angle (center.lat);
    MapProjection map;
    try {
      map = MapProjectionFactory.getInstance().create (
        ProjectionConstants.ORTHO,
        0,
        parameters,
        SpheroidConstants.SPHERE,
        dataDims,
        center,
        new double[] {res*1000, res*1000}
      );
    } catch (Exception e) { return (null); }

    // Create earth image transform
    // ----------------------------
    try { return (new EarthImageTransform (imageDims, center, 1, map)); }
    catch (Exception e) { return (null); }

  } // getContextProjection

  ////////////////////////////////////////////////////////////

  /**
   * Gets a local map projection appropriate for the specified context
   * area.
   *
   * @param trans the earth transform for converting data to
   * geographic coordinates.
   * @param upperLeft the upper-left corner of the data window.
   * @param lowerRight the lower-right corner of the data window.
   *
   * @return the context map projection.
   */
  private EarthImageTransform getContextProjection (
    EarthTransform trans,
    DataLocation upperLeft,
    DataLocation lowerRight
  ) {

    // Get context area center and size
    // --------------------------------
    DataLocation centerLoc = new DataLocation (
      (upperLeft.get(Grid.ROWS) + lowerRight.get(Grid.ROWS))/2,
      (upperLeft.get(Grid.COLS) + lowerRight.get(Grid.COLS))/2
    );
    EarthLocation center = trans.transform (centerLoc);
    double contextSize = Math.max (
      trans.distance (
        new DataLocation (centerLoc.get(Grid.ROWS), upperLeft.get(Grid.COLS)),
        new DataLocation (centerLoc.get(Grid.ROWS), lowerRight.get(Grid.COLS))
      ),
      trans.distance (
        new DataLocation (upperLeft.get(Grid.ROWS), centerLoc.get(Grid.COLS)),
        new DataLocation (lowerRight.get(Grid.ROWS), centerLoc.get(Grid.COLS))
      )
    );

    // Get projection
    // --------------
    return (getContextProjection (center, contextSize));
  
  } // getContextProjection

  ////////////////////////////////////////////////////////////

  /**
   * Gets a global map projection appropriate for the specified
   * context center.
   *
   * @param center the center earth location.
   *
   * @return the context map projection.
   */
  private EarthImageTransform getContextProjection (
    EarthLocation center
  ) {

    // Calculate resolution
    // --------------------
    Rectangle rect = getBounds (null);
    Dimension imageDims = new Dimension (rect.width, rect.height);
    int dataDims[] = new int[] {imageDims.height, imageDims.width};
    double res = (SpheroidConstants.STD_RADIUS*2)/rect.width;

    // Create map projection
    // ---------------------
    double[] parameters = new double[15];
    parameters[4] = GCTP.pack_angle (center.lon);
    parameters[5] = GCTP.pack_angle (center.lat);
    MapProjection map;
    try {
      map = MapProjectionFactory.getInstance().create (
        ProjectionConstants.ORTHO,
        0,
        parameters,
        SpheroidConstants.SPHERE,
        dataDims,
        center,
        new double[] {res*1000, res*1000}
      );
    } catch (Exception e) { return (null); }

    // Create earth image transform
    // ----------------------------
    try { return (new EarthImageTransform (imageDims, center, 1, map)); }
    catch (Exception e) { return (null); }

  } // getContextProjection

  ////////////////////////////////////////////////////////////

  /** Removes all polygon bounding boxes. */
  public void removeAllBoundingBoxes () {

    polygons.clear();
    colors.clear();
    labels.clear();

  } // removeAllBoundingBoxes

  ////////////////////////////////////////////////////////////

  /**
   * Adds a bounding box to this context element.
   *
   * @param polygon the bounding box earth location polygon points.
   * @param color the bounding box color.
   * @param label the bounding box label, or null for no label.
   */
  public void addBoundingBox (
    LineFeature polygon,
    Color color,
    String label
  ) {    

    polygons.add (polygon);
    colors.add (color);
    labels.add (label);

  } // addBoundingBox

  ////////////////////////////////////////////////////////////

  public void setPreferredSize (
    Dimension size
  ) {

    super.setPreferredSize (size);
    if (contextTrans != null)
      setContextArea (contextTrans, contextUpperLeft, contextLowerRight);
    else if (earthImageTrans != null) 
      setContextCenter (getCenter());

  } // setPreferredSize 

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new earth context element from the specified properties.
   *
   * @param position the top-left corner position of the picture.
   * @param size the preferred size of the picture (see {@link
   * PictureElement#setPreferredSize}).
   * @param trans the earth transform for converting data to
   * geographic coordinates.
   * @param upperLeft the upper-left corner of the context data window.
   * @param lowerRight the lower-right corner of the context data window.
   *
   * @see #setContextArea
   */
  public EarthContextElement ( 
    Point2D position,
    Dimension size,
    EarthTransform trans,
    DataLocation upperLeft,
    DataLocation lowerRight
  ) {

    // Initialize
    // ----------
    super (position, size);
    polygons = new ArrayList();
    colors = new ArrayList();
    labels = new ArrayList();
    if (coast == null) coast = getCoast();

    // Setup the context area
    // ----------------------
    setContextArea (trans, upperLeft, lowerRight);

  } // EarthContextElement constructor

  ////////////////////////////////////////////////////////////

  /** Gets the global coastline data. */
  private LineFeatureSource getCoast () {

    // Get global coastline data
    // -------------------------
    try { 
      BinnedGSHHSReader coast = 
        BinnedGSHHSReaderFactory.getInstance().getPolygonReader (
        BinnedGSHHSReaderFactory.getDatabaseName (
          BinnedGSHHSReaderFactory.COAST, BinnedGSHHSReaderFactory.CRUDE));
      coast.setMinArea (AREA_CUTOFF);
      EarthArea area = new EarthArea();
      area.addAll();
      coast.select (area); 
      return (coast);
    } // try

    // Print warning when not available
    // --------------------------------
    catch (IOException e) { 
      System.out.println (this.getClass() + ": Warning: " + e.toString());
      return (null); 
    } // catch

  } // getCoast

  ////////////////////////////////////////////////////////////

  /**
   * Sets the earth context center projection point.  This has the
   * effect of changing the context element to a global view centered
   * on the specified location.
   * 
   * @param center the new center location.
   */
  public void setContextCenter (
    EarthLocation center
  ) {
      
    // Set area parameters
    // -------------------
    contextTrans = null;
    contextUpperLeft = null;
    contextLowerRight = null;

    // Create new transform
    // --------------------
    earthImageTrans = getContextProjection (center);

  } // setContextCenter

  ////////////////////////////////////////////////////////////

  /** Gets the context element center earth location. */
  public EarthLocation getCenter () {

    Dimension imageDims = 
      earthImageTrans.getImageTransform().getImageDimensions();
    Point2D center = new Point2D.Double ((imageDims.width-1)/2.0,
      (imageDims.height-1)/2.0);
    return (earthImageTrans.transform (center));

  } // getCenter

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new earth context element showing the entire Earth.
   *
   * @see #setContextCenter
   *
   * @param center the initial center location.
   */
  public EarthContextElement ( 
    EarthLocation center
  ) {

    // Initialize
    // ----------
    super (new Point(), null);
    polygons = new ArrayList();
    colors = new ArrayList();
    labels = new ArrayList();
    if (coast == null) coast = getCoast();

    // Setup the context area
    // ----------------------
    setContextCenter (center);

  } // EarthContextElement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new earth context element from the specified properties.
   *
   * @param trans the earth transform for converting data to
   * geographic coordinates.
   * @param upperLeft the upper-left corner of the context data window.
   * @param lowerRight the lower-right corner of the context data window.
   *
   * @see #setContextArea
   */
  public EarthContextElement ( 
    EarthTransform trans,
    DataLocation upperLeft,
    DataLocation lowerRight
  ) {

    this (new Point(), null, trans, upperLeft, lowerRight);

  } // EarthContextElement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a shade and alpha modified color for polygon filling.
   *
   * @param color the original color.
   * @param factor the modification factor, [0..1] for a darker shade,
   * and >1 for a lighter shade.
   * @param alpha the alpha multiplication factor in the range [0..1].
   */
  private Color getModifiedColor (
    Color color,
    double factor,
    double alpha
  ) {

    float hsb[] = new float[3];

    Color.RGBtoHSB (color.getRed(), color.getGreen(), color.getBlue(), hsb);
    hsb[1] = (float) Math.min (Math.max ((1/factor)*hsb[1], 0), 1);
    hsb[2] = (float) Math.min (Math.max (factor*hsb[2], 0), 1);
    Color mod = Color.getHSBColor (hsb[0], hsb[1], hsb[2]);
    return (new Color (mod.getRed(), mod.getGreen(), mod.getBlue(), 
      Math.min ((int) (alpha * 255), 255)));


            /*

    return (
      Math.min ((int) (color.getRed()*factor), 255),
      Math.min ((int) (color.getGreen()*factor), 255),
      Math.min ((int) (color.getBlue()*factor), 255),
      Math.min ((int) (alpha * 255), 255)
    ));

            */


  } // getModifiedColor

  ////////////////////////////////////////////////////////////

  public void render (
    Graphics2D g,
    Color foreground,
    Color background
  ) {

    // Check for valid transform
    // -------------------------
    if (earthImageTrans == null) {
      throw new RuntimeException ("Cannot render with null earth transform");
    } // if

    // Initialize
    // ----------
    Rectangle rect = getBounds (g);

    // Check if we can draw with alpha
    // -------------------------------
    Graphics2D gSaved = null;
    BufferedImage buffer = null;
    boolean alpha = GraphicsServices.supportsAlpha (g);
    if (!alpha) {
      buffer = new BufferedImage (rect.width*BUFFER_SCALE,
        rect.height*BUFFER_SCALE, BufferedImage.TYPE_INT_ARGB);
      gSaved = g;
      g = buffer.createGraphics();
      g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
        RenderingHints.VALUE_ANTIALIAS_ON);
      g.scale (BUFFER_SCALE, BUFFER_SCALE);
      g.translate (-rect.x, -rect.y);
    } // if

    // Draw background
    // ---------------
    if (background != null) {
      g.setColor (background);
      g.fill (rect);
    } // if

    // Set clip
    // --------
    Rectangle clip = g.getClipBounds();
    g.setClip (rect);

    // Translate drawing frame
    // -----------------------
    AffineTransform saved = g.getTransform();
    g.translate (rect.x, rect.y);

    // Set drawing properties
    // ----------------------
    g.setColor (foreground);
    g.setStroke (new BasicStroke (1.0f, BasicStroke.CAP_SQUARE, 
      BasicStroke.JOIN_BEVEL));

    // Prepare for edge or terminator rendering
    // ----------------------------------------
    Point center = null;
    int radius = 0;
    if (showEdge || sz != null) {

      // Get center point
      // ----------------
      Dimension imageDims = 
        earthImageTrans.getImageTransform().getImageDimensions();
      center = new Point (imageDims.width/2, imageDims.height/2);

      // Get radius
      // ----------
      AffineTransform affine = 
        ((MapProjection) earthImageTrans.getEarthTransform()).getAffine();
      Point2D centerData = new Point (center.y, center.x);
      Point2D centerMap = affine.transform (centerData, null);
      Point2D edgeMap = new Point2D.Double (centerMap.getX() + 
        SpheroidConstants.STD_RADIUS*1000, 0);
      try { 
        Point2D edgeData = affine.inverseTransform (edgeMap, null); 
        radius = (int) Math.round (edgeData.getY() - centerData.getY());
        radius--;
      } catch (NoninvertibleTransformException e) { };

    } // if

    // Draw solar terminator
    // ---------------------
    if (sz != null) {

      // Create terminator ellipse
      // -------------------------
      LineFeature terminator = new LineFeature();
      for (double lat = -90; lat <= 90; lat += GRID_DOT) {
        EarthLocation loc = sz.getTerminator (lat, true);
        if (loc.isValid()) terminator.add (loc);
      } // for
      for (double lat = 90; lat >= -90; lat -= GRID_DOT) {
        EarthLocation loc = sz.getTerminator (lat, false);
        if (loc.isValid()) terminator.add (loc);
      } // for
      terminator.add (terminator.get (0));

      // Convert ellipse to image coordinates
      // ------------------------------------
      List points = new ArrayList();
      int start = 0;
      int npoints = terminator.size();
      for (int i = 0; i < npoints; i++) {
        points.add (earthImageTrans.transform (
          (EarthLocation) terminator.get(i)));
        if (i != 0 && (points.get(i-1) == null && points.get(i) != null))
          start = i;
      } // for

      // Convert ellipse to continuous path
      // ----------------------------------
      List path = new ArrayList();
      path.add (points.get (start));
      for (int i = start+1; i != start; i++) {
        if (i > npoints-1) i = 0;
        Point2D point = (Point2D) points.get (i);
        if (point != null) path.add (point);
      } // for

      // Convert ellipse to general path
      // -------------------------------
      GeneralPath genPath = new GeneralPath();
      for (int i = 0; i < path.size(); i++) {
        Point2D point = (Point2D) path.get (i);
        float x = (float) point.getX();
        float y = (float) point.getY();
        if (i == 0) genPath.moveTo (x, y);
        else genPath.lineTo (x, y);
      } // for

      // Render polygons
      // ---------------
      if (dayColor != null || nightColor != null) {

        // Get arc starting angle
        // ----------------------
        Point2D arcStart = (Point2D) path.get (path.size()-1);
        double startAngle = Math.toDegrees (Math.atan2 (- (arcStart.getY()
          - center.y), arcStart.getX() - center.x));

        // Render daytime polygon
        // ----------------------
        if (dayColor != null) {
          Arc2D arc = new Arc2D.Double (center.x - radius, center.y - radius,
            radius*2, radius*2, startAngle, 180, Arc2D.OPEN);
          GeneralPath dayPath = (GeneralPath) genPath.clone();
          dayPath.append (arc, true);
          dayPath.closePath();
          g.setColor (dayColor);
          g.fill (dayPath);
        } // if

        // Render nighttime polygon
        // ------------------------
        if (nightColor != null) {
          Arc2D arc = new Arc2D.Double (center.x - radius, center.y - radius,
            radius*2, radius*2, startAngle, -180, Arc2D.OPEN);
          GeneralPath nightPath = (GeneralPath) genPath.clone();
          nightPath.append (arc, true);
          nightPath.closePath();
          g.setColor (nightColor);
          g.fill (nightPath);
        } // if

      } // if

      // Render terminator path
      // ----------------------
      g.setColor (foreground);
      g.draw (genPath);

    } // if    

    // Draw Earth edge
    // ---------------
    if (showEdge) {
      g.drawOval (center.x - radius, center.y - radius, radius*2, radius*2);
    } // if

    // Draw coastlines
    // ---------------
    if (coast != null) coast.render (g, earthImageTrans);

    // Draw grid points
    // ----------------
    if (showGrid) {

      // Latitude lines
      // --------------
      Datum datum = earthImageTrans.getEarthTransform().getDatum();
      for (double lat = 0; lat <= 90; lat += gridInc)
        for (double lon = -180; lon < 180; lon += GRID_DOT)
          new EarthLocation (lat, lon, datum).render (g, earthImageTrans);
      for (double lat = -gridInc; lat >= -90; lat -= gridInc)
        for (double lon = -180; lon < 180; lon += GRID_DOT)
          new EarthLocation (lat, lon, datum).render (g, earthImageTrans);

      // Longitude lines
      // ---------------
      for (double lon = 0; lon < 180; lon += gridInc)
        for (double lat = -90; lat <= 90; lat += GRID_DOT)
          new EarthLocation (lat, lon, datum).render (g, earthImageTrans);
      for (double lon = -gridInc; lon >= -180; lon -= gridInc)
        for (double lat = -90; lat <= 90; lat += GRID_DOT)
          new EarthLocation (lat, lon, datum).render (g, earthImageTrans);

    } // if

    // Draw bounding polygons
    // ----------------------
    for (int i = 0; i < polygons.size(); i++) {

      // Get colors
      // ----------
      Color base = (Color) colors.get(i);
      Color fill = getModifiedColor (base, FILL_FACTOR, ALPHA);
      Color outline = getModifiedColor (base, 1, ALPHA);

      // Draw fill and outline
      // ---------------------
      LineFeature feature = (LineFeature) polygons.get (i);
      GeneralPath path = feature.getPath (earthImageTrans);
      if (!feature.isDiscontinuous()) {
        g.setColor (fill);
        g.fill (path);
        g.setColor (outline);
        g.draw (path);
      } // if

    } // for

    // Draw labels
    // -----------
    for (int i = 0; i < polygons.size(); i++) {
      String label = (String) labels.get (i);
      if (label != null) {
        Color labelColor = getModifiedColor ((Color) colors.get(i), 
          LABEL_FACTOR, LABEL_ALPHA);
        LineFeature feature = (LineFeature) polygons.get (i);
        GeneralPath path = feature.getPath (earthImageTrans);
        if (!feature.isDiscontinuous()) {
          Rectangle2D bounds = path.getBounds2D();
          Point2D labelCenter = new Point2D.Double (bounds.getCenterX(),
            bounds.getCenterY());
          TextElement element = new TextElement (label, labelFont, labelCenter,
            new double[] {0.5, 0.5}, 0);
          element.render (g, labelColor, Color.BLACK); 
        } // if
      } // if
    } // for

    // Restore drawing frame
    // ---------------------
    g.setTransform (saved);
 
    // Restore clip
    // ------------
    g.setClip (clip);

    // Render buffer to destination
    // ----------------------------
    if (!alpha) {
      gSaved.drawImage (buffer, rect.x, rect.y, rect.width, rect.height, 
        null);
    } // if

  } // render

  ////////////////////////////////////////////////////////////

  public Area getArea (
    Graphics2D g
  ) {

    // Create area
    // -----------
    int size;
    if (preferred == null) size = DEFAULT_SIZE; 
    else size = Math.min (preferred.width, preferred.height);
    return (new Area (new Rectangle ((int) Math.round (position.getX()), 
      (int) Math.round (position.getY()), size, size)));

  } // getArea

  ////////////////////////////////////////////////////////////

  /** Gets the current number of polygon bounding boxes. */
  public int getBoundingBoxes () { return (polygons.size()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a polygon bounding box.
   *
   * @param index the index of the desired polygon bounding box,
   * in the range [0..boxes-1].
   *
   * @return the bounding box as a list of earth locations.
   *
   * @see #getBoundingBoxes
   * @see #addBoundingBox
   */
  public LineFeature getBoundingBox (
    int index
  ) {

    return ((LineFeature) polygons.get (index));

  } // getBoundingBox

  ////////////////////////////////////////////////////////////

  /** Gets the current earth image transform for this element. */
  public EarthImageTransform getEarthImageTransform () { 

    return (earthImageTrans);

  } // getEarthImageTransform

  ////////////////////////////////////////////////////////////

} // EarthContextElement class

////////////////////////////////////////////////////////////////////////
