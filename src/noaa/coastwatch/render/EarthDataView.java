////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataView.java
  PURPOSE: A class to set up a graphical view of 2D earth locatable data.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/17
  CHANGES: 2002/07/21, PFH, added implementation
           2002/07/28, PFH, converted to location classes
           2002/09/03, PFH, renamed to EarthDataView and restructured
           2002/09/23, PFH, added upside down data detection
           2002/10/04, PFH, added getLegend, getSize, setSize, setMaxAspect,
             setWidth, setHeight
           2002/10/08, PFH, changed to use center location, added resize
             methods
           2002/10/21, PFH, added coordinate caches
           2002/10/22, PFH, added getCorners
           2002/11/19, PFH, added verbose mode, modified to render bitmask
             overlays correctly to destinations with no alpha support
           2002/12/06, PFH, modified getArea to return clone
           2002/12/10, PFH, added addOverlays and getOverlays, modified
             constructor to only flip swath projections
           2002/12/12, PFH, moved body of getResolution to EarthImageTransform
           2002/12/15, PFH, added isPrepared
           2002/12/16, PFH, added isChanged
           2003/04/19, PFH, added rendering progress mode
           2004/02/19, PFH, added getCenter(), magnify(factor)
           2004/02/20, PFH, updated documentation
           2004/02/21, PFH, added containsOverlay()
           2004/03/01, PFH, added handling for overlay visibility and name
           2004/03/03, PFH, added setChanged()
           2004/03/09, PFH, modified isPrepared() to check overlay visibility
           2004/03/11, PFH, added transform(Point,int[]), getProgress()
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/05/26, PFH, added setProperties() method
           2004/05/28, PFH, added stopRendering(), isRendering() methods
           2004/06/07, PFH, added image affine methods
           2004/10/17, PFH, added invalidate()
           2004/10/18, PFH, added hasCompatibleCaches()
           2005/02/14, PFH, fixed fill problem when imageAffine rendering
           2005/03/25, PFH
           - added clone()
           - modified to render all special overlays with alpha at once
           2005/07/31, PFH, modified to use SwathProjection.getNorthIsUp()
           2006/05/25, PFH, added call to setAccessHint() in computeCaches()
           2006/06/06, PFH, added showSubregion() and getSubregion()
           2006/10/07, PFH, modified to check for TransparentOverlay rather 
             than BitmaskOverlay in render()
           2006/12/14, PFH, added getImageAffine()
           2006/12/18, PFH, added getUpsideDown()
           2013/05/31, PFH, updated docs for getScale() and setCenterAndScale()
           2014/02/25, PFH
           - Changes: Updated to use orientation affine transform rather than
             upside-down flag for orienting the view correctly.
           - Issue: There were cases where a simple rotation of 180 deg wasn't
             correct to orient the view and the best generic orientation was
             expressed as an affine rather than using a negative scaling factor.
           2014/04/02. PFH
           - Changes: Corrected all documentation on scale factor.
           - Issue: The meaning of scale factor was described inverse to its
             implementation.
           2016/01/19, PFH
           - Changes: Updated to new logging API.

  CoastWatch Software Library and Utilities
  Copyright 1998-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.GraphicsServices;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.Legend;
import noaa.coastwatch.render.OrientationAffineFactory;
import noaa.coastwatch.render.Renderable;
import noaa.coastwatch.render.Subregion;
import noaa.coastwatch.render.TransparentOverlay;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.MapProjectionFactory;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The earth data view class sets up a correspondence between 2D Earth
 * locatable data variables (possibly more than one) and a visual
 * image.  A view has a certain size in width and height and a
 * translation between earth locations and image coordinates.  A view
 * may also have a number of data overlays for annotation.  For a
 * swath transform, the data view forces north to be in the upwards
 * direction, at the top of the image.  If the transform is such that
 * data locations with larger indices have a greater latitude, the
 * sense of the image transform is flipped so that north appears at
 * the top.<p>
 *
 * A view maintains its image to data scaling information using the
 * center data location and image scale factor.  All view
 * manipulations are relative to these quantities.<p>
 *
 * In order to help implement child classes, it may be of interest to
 * developers to know how view rendering works.  The main
 * <code>render()</code> method is called by any user of the view to
 * render the view to a graphics context.  In some cases, this
 * graphics context may be from a GUI panel on the screen, or in
 * others it may be an offscreen image buffer that will be copied to
 * the screen or saved to an image file.  In either case, the
 * <code>render()</code> method relies on a two-step process for
 * rendering the view.  It first renders the protected
 * <code>BufferedImage image</code> variable to the graphics context.
 * Secondly, it loops over each overlay in order and renders it to 
 * the graphics context.  This two-step process may involve delays,
 * both in rendering the main image and the overlays.  For example, a
 * delay may occur when translating data values into image colours, or
 * in translating overlay earth locations to image coordinates.<p>
 *
 * In order to help handle delays, two protected variables are used:
 * <code>changed</code> and <code>progress</code>.  The changed flag
 * indicates that something in the view has changed since the last
 * time the <code>render()</code> method was called.  If the view has
 * changed, then any saved image buffer that was used to capture the
 * results of the <code>render()</code> method may no longer be used
 * and should be discarded.  If the view has not changed, then
 * rendering again will produce the exact same output and there is no
 * need to render again if the output was saved.  The progress flag
 * indicates that the user wants to see progress updates during the
 * delay in which the image is being rendered.  In addition to these
 * flags, the two methods <code>prepare()</code> and
 * <code>isPrepared()</code> are used to determine how much of a delay
 * may be involed in rendering.  If the protected <code>image</code>
 * variable is not built or one of the overlays is not ready to be
 * immediately rendered in screen coordinates, then the view is "not
 * prepared".  The view may be explicitly prepared by calling the
 * <code>prepare()</code> method and passing it the graphics context
 * to which preparation progress should be written.  Once prepared,
 * the user may call <code>render()</code> and expect to have no
 * delay.<p>
 *
 * To handle interruptions in rendering so that a user-interface using
 * the view appears responsive, the <code>stopRendering()</code>
 * method may be used.  The method sets the protected
 * <code>stopRendering</code> flag to indicate to the child class that
 * it should stop inside any currently executing rendering loop and
 * return.  The <code>isRendering()</code> method may be used to
 * determine if a rendering loop is currently active.  Generally,
 * these methods are only useful in a multithreaded application in
 * which the rendering is running in a separate thread from the
 * user-interface.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
@noaa.coastwatch.test.Testable
public abstract class EarthDataView
  implements Renderable, Cloneable {

  // Constants
  // ---------

  /** 
   * The update frequency as a fraction of the total image size.  This
   * should be used by any rendering routine that wants to show the
   * progress of view construction while it is taking place.
   */
  public static final double UPDATE_FRACTION = 0.2;

  // Variables
  // ---------
  /** 
   * The view data window center point.  The center data location
   * appears at the center of any view graphics.
   */
  private DataLocation center;

  /** 
   * The view scaling factor.  This is the ratio of data pixels
   * to view image pixels.  A scale of 1 means that one pixel on the screen
   * is one pixel in the data.  A scale of two means one pixel on the
   * screen is two pixels in the data -- ie: that the data is
   * zoomed out.  Often, the scale will be > 1 so that the entire data
   * grid fits in the view image which may be an onscreen image.
   */
  private double scale;

  /** 
   * The earth to image coordinate transform. This is used to
   * translate between earth (lat,lon) and view image (x,y).  It holds
   * both the image transform for view image (x,y) to data (row,col)
   * and the earth transform for data (row,col) to earth (lat,lon).
   */
  protected EarthImageTransform trans;

  /** 
   * The view image dimensions.  This is the height and width of the
   * view graphics when delivered by the <code>render()</code> method.
   */
  protected Dimension imageDims;

  /** 
   * A number of data overlays.  The overlays are rendered in the view
   * image after sorting according to level.
   */
  protected LinkedList<EarthDataOverlay> overlays;

  /** 
   * The view area.  At any one time, the view shows some area of the
   * Earth.  Since the view may be using any one of a number Earth
   * transforms, the area may be an irregular shape.  This earth area
   * may be required and should only be created once each time the
   * view is modified since it can take time to create.
   */
  private EarthArea area;

  /** 
   * The buffered image used for rendering.  The buffered image is
   * used by child classes to render a specific image based on their
   * own variables.  This image is independent of the list of overlays
   * and should only contain graphics specific to the child.  It is
   * subsequently used by this class to assemble the complete view
   * graphics.
   */
  protected BufferedImage image;

  /**
   * The full data dimensions.  These are the dimensions of the full
   * data grid being viewed.  The data dimensions are required to set
   * up a default image transform when a view is first created, and to
   * subsequently reset the transform when required.
   */
  private int[] dataDims;

  /** 
   * The image to data location coordinate cache for rows.  In order
   * to speed up the translation of image y value to data row, the row
   * cache holds all the row values.  This can only work if the grid
   * has a simple navigation transform of the identity, or a
   * translation transform.  The row cache must be recomputed when the
   * view is modified.
   */
  int[] rowCache;

  /** 
   * The image to data location coordinate cache for columns.  Similar
   * to raw caching, the column cache speeds up the translation of
   * image x values to data column by holding all the column values.
   * The column cache is recomputed along with the row cache when the
   * view is modified.
   */
  int[] colCache;

  /** 
   * The verbose mode flag.  When true, the status of the main image
   * rendering is printed, along with each overlay rendering step.
   */
  protected boolean verbose;

  /** 
   * The view changed flag, true if the view changed after rendering.
   * The changed flag must be set if any change is made to the view
   * that would change the view graphics generated by a call to the
   * <code>render()</code> method.
   */
  protected boolean changed;

  /** 
   * The rendering progress flag, true to show rendering progress.
   * When rendering progress mode is on, the child class should
   * periodically update the view graphics in the
   * <code>prepare()</code> method at a frequency determined by
   * <code>UPDATE_FRACTION</code>.
   */
  protected boolean progress;

  /** 
   * The rendering stop flag, true to stop rendering in progress.
   * This flag is set to false at the start of any rendering call, and
   * should be used by child classes to break out of the rendering loop.
   */
  protected boolean stopRendering;

  /** 
   * The rendering flag, true if a rendering loop is in progress.
   * This flag would normally be used by a user object that wants to
   * know if a rendering loop is in progress so that if it needs to,
   * it can stop rendering and perform some other methods such as
   * altering the view characteristics and rendering again.
   */
  private boolean isRendering;

  /**
   * The image affine transform used to transform the image with
   * respect to the overlays.  Generally, this is only useful as a
   * temporary measure to perform an on-screen visual data navigation
   * with respect to the displayed overlays.
   */
  private AffineTransform imageAffine;

  /** 
   * The navigation transform used in computing coordinate caches.
   * Some classes may need to check this to ensure that the coordinate
   * cache is compatible with a certain grid.
   */
  private AffineTransform cacheNavigation;

  /**
   * The orientation affine that orients the image for display.  This
   * is used in such cases as when the image wouldn't normally show north
   * at the top of the screen.  This may be an identity transform if
   * no orientation should be performed.
   */
  private AffineTransform orientationAffine;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the upside-down flag.
   *
   * @return true if the view is being corrected from its normal 
   * orientation so that north is in the up direction, or false if not.
   *
   * @deprecated As of 3.3.1, use {@link #getOrientationAffine}.
   */
  @Deprecated
  public boolean getUpsideDown () {
  
    return (orientationAffine.getType() != AffineTransform.TYPE_IDENTITY);
    
  } // getUpsideDown

  ////////////////////////////////////////////////////////////

  /**
   * Gets the view orientation affine.  The orientation affine transforms
   * data coordinates for display, rotating or flipping as needed so that
   * the north direction is at the top of the screen.
   *
   * @return the orientation affine (possibly the identity).
   *
   * @since 3.3.1
   */
  public AffineTransform getOrientationAffine () {
  
    return ((AffineTransform) orientationAffine.clone());
    
  } // getOrientationAffine

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current image affine transform.
   *
   * @return the current transform or null for no transform.
   *
   * @see #setImageAffine
   */
  public AffineTransform getImageAffine () {

    return (imageAffine == null ? null : 
      (AffineTransform) imageAffine.clone());

  } // getImageAffine

  ////////////////////////////////////////////////////////////

  /**
   * Sets the image affine transform.  When the view is rendered,
   * the new image affine will be used to transform the image
   * coordinates prior to rendering.  Any overlays rendered will
   * be unaffected.
   *
   * @param affine the image affine transform, or null for no
   * transform.
   */
  public void setImageAffine (
    AffineTransform affine
  ) {
    
    imageAffine = (affine == null ? null : (AffineTransform) affine.clone());
    changed = true;

  } // setImageAffine

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the stop rendering flag to indicate that any current
   * rendering loop should stop and return.
   */
  public synchronized void stopRendering () { stopRendering = true; }

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if the view is currently being rendered, or false
   * if not.
   */
  public synchronized boolean isRendering () { return (isRendering); }

  ////////////////////////////////////////////////////////////

  /** Gets the current list of overlays. */
  public List<EarthDataOverlay> getOverlays () { return ((List<EarthDataOverlay>) overlays.clone()); }

  ////////////////////////////////////////////////////////////

  /** Adds a list of overlays to the view. */
  public void addOverlays (List overlays) {

    this.overlays.addAll (overlays);
    changed = true;

  } // addOverlays

  ////////////////////////////////////////////////////////////

  /** Sets the verbose mode flag. */
  public void setVerbose (boolean flag) { verbose = flag; }

  ////////////////////////////////////////////////////////////
  
  /** Gets the rendering progress mode flag. */
  public boolean getProgress () { return (progress); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the rendering progress mode flag.  When rendering progress
   * mode is on, a call to <code>render</code> shows the progress of
   * rendering by drawing successive images to the destination
   * graphics device.
   */
  public void setProgress (boolean flag) { progress = flag; }

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if this view has coordinate caches available.
   *
   * @see #transform(Point)
   */
  public boolean hasCoordinateCaches () { return (rowCache != null); }

  ////////////////////////////////////////////////////////////

  /**
   * Returns true if this view has coordinate caches that are
   * compatible with the specified grid navigation transform.
   * Returns false if the coordinate caches are incompatible or if
   * there are no coordinate caches.
   * 
   * @see #hasCoordinateCaches
   * @see #computeCaches
   */
  public boolean hasCompatibleCaches (
    Grid grid
  ) {

    if (!hasCoordinateCaches()) return (false);
    else return (grid.getNavigation().equals (cacheNavigation));

  } // hasCompatibleCaches

  ////////////////////////////////////////////////////////////

  /**
   * Transforms an image point to an integer data location using
   * precomputed coordinate caching.
   *
   * @param p the image point.
   *
   * @return the integer data location as <code>[row, column]</code>.
   * This view must have coordinate caches available.
   *
   * @see #hasCoordinateCaches
   */
  public int[] transform (
    Point p
  ) {
  
    return (new int[] {rowCache[p.y], colCache[p.x]});

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Transforms an image point to an integer data location using
   * precomputed coordinate caching.
   *
   * @param p the image point.
   * @param coords the output coordinates array.  The array entries
   * are modified to contain the integer data location as <code>[row,
   * column]</code>.
   *
   * @see #hasCoordinateCaches
   */
  public void transform (
    Point p,
    int[] coords
  ) {
    
    coords[0] = rowCache[p.y];
    coords[1] = colCache[p.x];

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Creates a set of caches to speed image-to-data transforms.  The
   * navigation transform of the specified grid is taken into
   * account in the cache.  Note that this is only useful when the
   * navigation is either the identity or a translation.
   *
   * @param grid the grid variable to use for navigation corrections.
   */
  protected void computeCaches (
    Grid grid
  ) {

    // Create caches
    // -------------
    rowCache = new int[imageDims.height];
    colCache = new int[imageDims.width];

    // Calculate cache points
    // ----------------------
    ImageTransform imageTrans = trans.getImageTransform();
    for (Point p = new Point (0,0); p.x < imageDims.width; p.x++) {
      DataLocation loc = grid.navigate (imageTrans.transform(p));
      colCache[p.x] = (int) Math.round (loc.get(Grid.COLS));
    } // for
    for (Point p = new Point (0,0); p.y < imageDims.height; p.y++) {
      DataLocation loc = grid.navigate (imageTrans.transform(p));
      rowCache[p.y] = (int) Math.round (loc.get(Grid.ROWS));
    } // for

    // Save cache navigation
    // ---------------------
    cacheNavigation = grid.getNavigation();

    // Compute access hint values
    // --------------------------
    int[] start = new int[] {rowCache[0], colCache[0]};
    int[] end = new int[] {
      rowCache[imageDims.height-1], 
      colCache[imageDims.width-1]
    };
    int[] stride = new int[] {
      (int) Math.floor (Math.max (1, (end[0] - start[0] + 1)/
        (float)imageDims.height)),
      (int) Math.floor (Math.max (1, (end[1] - start[1] + 1)/
        (float)imageDims.width))
    };

    // Check bounds of hints
    // ---------------------
    boolean useHint = true;
    for (int i = 0; i < 2; i++) {
      if (start[i] < 0) start[i] = 0;
      if (end[i] > dataDims[i]-1) end[i] = dataDims[i]-1;
      if (start[i] > end[i]) { useHint = false; break; }
    } // for

    // Set hints in grid
    // -----------------
    if (useHint) grid.setAccessHint (start, end, stride);

  } // computeCaches

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the changed flag.  This method should be called if some
   * existing overlay property has been updated, or some other change
   * made to the view that would require the view buffer to be
   * updated.  Normally, the change flag is set internally but in some
   * cases it may be necessary to "force" the view to acknowledge a
   * change.
   */
  public synchronized void setChanged() { changed = true; }

  ////////////////////////////////////////////////////////////

  /**
   * Invalidates the view.  This causes the view to be completely
   * reconstructed upon the next call to <code>render()</code>.
   */
  public void invalidate () {

    image = null;
    rowCache = null;
    colCache = null;
    changed = true;

  } // invalidate

  ////////////////////////////////////////////////////////////

  /** Adds an overlay to the view. */
  public void addOverlay (
    EarthDataOverlay overlay
  ) { 

    overlays.add (overlay); 
    changed = true;

  } // addOverlay

  ////////////////////////////////////////////////////////////

  /** Removes an overlay from the view. */
  public void removeOverlay (
    EarthDataOverlay overlay
  ) { 

    overlays.remove (overlay); 
    changed = true;

  } // removeOverlay

  ////////////////////////////////////////////////////////////

  /** Checks if this view contains the specified overlay. */
  public boolean containsOverlay (
    EarthDataOverlay overlay
  ) { 

    return (overlays.contains (overlay));

  } // containsOverlay

  ////////////////////////////////////////////////////////////

  /** Gets the earth image transform. */
  public EarthImageTransform getTransform() { return (trans); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the data corners of this view as [upperLeft, lowerRight]. 
   *
   * @deprecated As of 3.3.1, use {@link #getBounds}.
   */
  @Deprecated
  public DataLocation[] getCorners() {

    return (getBounds());

  } // getCorners

  ////////////////////////////////////////////////////////////

  /**
   * Gets the bounds of this view.
   *
   * @return the array of data location bounds as [minimum(row,col), 
   * maximum(row,col)].
   *
   * @since 3.3.1
   */
  public DataLocation[] getBounds() {

    // Get corner locations
    // --------------------
    DataLocation upperLeft = trans.getImageTransform().transform (
      new Point());
    DataLocation lowerRight = trans.getImageTransform().transform (
      new Point (imageDims.width-1, imageDims.height-1));

    // Compute mimimum and maximum locations
    // -------------------------------------
    DataLocation minLoc = new DataLocation (
      Math.min (upperLeft.get (Grid.ROWS), lowerRight.get (Grid.ROWS)),
      Math.min (upperLeft.get (Grid.COLS), lowerRight.get (Grid.COLS))
    );
    DataLocation maxLoc = new DataLocation (
      Math.max (upperLeft.get (Grid.ROWS), lowerRight.get (Grid.ROWS)),
      Math.max (upperLeft.get (Grid.COLS), lowerRight.get (Grid.COLS))
    );
    
    return (new DataLocation[] {minLoc, maxLoc});

  } // getBounds

  ////////////////////////////////////////////////////////////

  /** Gets the center data location of this view. */
  public DataLocation getCenter () { return ((DataLocation) center.clone()); }
  
  ////////////////////////////////////////////////////////////

  /** 
   * Gets the scale of this view.
   *
   * @return the ratio of data pixels to view image pixels.
   */
  public double getScale() { return (this.scale); }

  ////////////////////////////////////////////////////////////

  /** Gets the geographic area of this view. */
  public EarthArea getArea() { 

    // Create area if null
    // -------------------
    if (area == null) {
    
    
      // TODO: Could this be where the issue is when showing views that
      // contain the date line?  The grid and coastlines get cut off.
      
    
      DataLocation[] corners = getBounds();
      area = new EarthArea (trans.getEarthTransform(), corners[0], corners[1]);
    } // if

    return ((EarthArea) area.clone()); 

  } // getArea

  ////////////////////////////////////////////////////////////

  /**
   * Sets the view center to a new data location.  The image
   * dimensions and image scaling factor are unaffected.
   *
   * @param center the new center data location.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void setCenter (
    DataLocation center
  ) throws NoninvertibleTransformException {

    // Set center and reset
    // --------------------
    this.center = (DataLocation) center.clone();
    resetTransform();

  } // setCenter
  
  ////////////////////////////////////////////////////////////
  
  /**
   * Set the center and scale of this view
   * 
   * @param center the new center data location.
   * @param scale the new scaling factor for the view (desired ratio of
   * data pixels to image pixels).
   *
   * @throws NoninvertibleTransformException
   *
   * @see #getCenter
   */
  public void setCenterAndScale (
    DataLocation center,
    double scale
  ) throws NoninvertibleTransformException {

    // Set center and reset
    // --------------------
    this.center = (DataLocation) center.clone();
    this.scale = scale;
    resetTransform();

  } // setCenterAndScale

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the transform using the internal variables.  The Earth
   * area, image, and coordinate caches are also invalidated, and the
   * changed flag set to true.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  private void resetTransform () throws NoninvertibleTransformException {

    ImageTransform imageTrans = new ImageTransform (imageDims,
      new Dimension (dataDims[Grid.COLS], dataDims[Grid.ROWS]), center,
      scale, orientationAffine);
    trans = new EarthImageTransform (trans.getEarthTransform(), imageTrans);
    area = null;
    invalidate();

  } // resetTransform

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the properties of this view based on the specified view.
   * The two views should have compatible earth transforms and data
   * grids.  The overlays, earth transform, and verbose mode are all
   * copied.
   *
   * @param view the source view to copy properties from.
   */
  public void setProperties (
    EarthDataView view
  ) {

    this.center = (DataLocation) view.center.clone();
    this.scale = view.scale;
    this.trans = view.trans;
    this.imageDims = (Dimension) view.imageDims.clone();
    this.overlays = (LinkedList) view.overlays.clone();
    this.area = (view.area != null ? (EarthArea) view.area.clone() : null);
    this.image = null;
    this.orientationAffine = (AffineTransform) view.orientationAffine.clone();
    this.dataDims = (int[]) view.dataDims.clone();
    this.rowCache = (view.rowCache != null ? (int[]) view.rowCache.clone() : 
      null);
    this.colCache = (view.colCache != null ? (int[]) view.colCache.clone() : 
      null);
    this.verbose = view.verbose;
    this.changed = true;

  } // setProperties

  ////////////////////////////////////////////////////////////

  /**
   * Changes the view size to the specified maximum dimension.  The
   * view dimensions are changed, as well as the data to image scaling
   * so that approximately the same data is displayed in the resized
   * view.  The center data location is unaffected.
   *
   * @param imageSize the new image maximum dimension.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void resizeMaxAspect (
    int imageSize
  ) throws NoninvertibleTransformException {

    if (imageDims.height > imageDims.width) resizeHeight (imageSize);
    else resizeWidth (imageSize);

  } // resizeMaxAspect

  ////////////////////////////////////////////////////////////

  /**
   * Changes the view size to the specified width.  The view
   * dimensions are changed, as well as the data to image scaling so
   * that approximately the same data is displayed in the resized
   * view.  The center data location is unaffected.
   *
   * @param imageWidth the new image width.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void resizeWidth (
    int imageWidth
  ) throws NoninvertibleTransformException {

    double factor = (double) imageWidth / this.imageDims.width;
    resize (factor);

  } // resizeWidth

  ////////////////////////////////////////////////////////////

  /**
   * Changes the view size to the specified height.  The view
   * dimensions are changed, as well as the data to image scaling so
   * that approximately the same data is displayed in the resized
   * view.  The center data location is unaffected.
   *
   * @param imageHeight the new image height.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void resizeHeight (
    int imageHeight
  ) throws NoninvertibleTransformException {

    double factor = (double) imageHeight / this.imageDims.height;
    resize (factor);

  } // resizeHeight

  ////////////////////////////////////////////////////////////

  /**
   * Changes the view size to the specified dimensions.  The view
   * dimensions are changed, as well as the data to image scaling so
   * that approximately the same data is displayed in the resized
   * view.  The center data location is unaffected.
   *
   * @param imageDims the new image dimensions.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void resize (
    Dimension imageDims
  ) throws NoninvertibleTransformException {

    double factor = Math.min (
      (double) imageDims.width / this.imageDims.width,
      (double) imageDims.height / this.imageDims.height
    );
    resize (factor);

  } // resize

  ////////////////////////////////////////////////////////////

  /**
   * Changes the view size by the specified factor.  The view
   * dimensions are changed, as well as the data to image scaling so
   * that the same data is displayed in the resized view.  The center
   * data location is unaffected.
   *
   * @param factor the factor by which to change the view size.
   * Factors greater than 1 increase the view size and factors less
   * than 1 decrease the view size.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void resize (
    double factor
  ) throws NoninvertibleTransformException {

    // Modify the scaling and dimensions and reset
    // -------------------------------------------
    scale = scale / factor;
    imageDims.width = (int) (imageDims.width * factor);
    imageDims.height = (int) (imageDims.height * factor);
    resetTransform();

  } // resize

  ////////////////////////////////////////////////////////////

  /**
   * Magnifies this view around a center location.  The view
   * dimensions are unaffected.
   *
   * @param center the new center data location.
   * @param factor the factor by which to magnify.  Factors greater
   * than 1 increase the magnification and factors less than 1
   * decrease the magnification.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void magnify (
    DataLocation center,
    double factor
  ) throws NoninvertibleTransformException {

    Dimension savedDims = (Dimension) imageDims.clone();
    setCenter (center);
    resize (factor);
    setSize (savedDims);

  } // magnify

  ////////////////////////////////////////////////////////////

  /**
   * Magnifies this view around the current center location.  The view
   * dimensions are unaffected.
   *
   * @param factor the factor by which to magnify.  Factors greater
   * than 1 increase the magnification and factors less than 1
   * decrease the magnification.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   *
   * @see #magnify(DataLocation,double)
   */
  public void magnify (
    double factor
  ) throws NoninvertibleTransformException {

    Dimension savedDims = (Dimension) imageDims.clone();
    resize (factor);
    setSize (savedDims);

  } // magnify

  ////////////////////////////////////////////////////////////

  /**
   * Magnifies the specified area to occupy the entire view.  The
   * center data location is changed to match the center of the
   * specified rectangle.  The scaling factor is changed so that the
   * maximum dimension of the rectangle fits within the view
   * dimensions.
   *
   * @param upperLeft the upper-left corner point.
   * @param lowerRight the lower-right corner point.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void magnify (
    Point upperLeft,
    Point lowerRight
  ) throws NoninvertibleTransformException {

    // Set center point
    // ----------------
    ImageTransform imageTrans = trans.getImageTransform();
    DataLocation upperLeftLoc = imageTrans.transform (upperLeft);
    DataLocation lowerRightLoc = imageTrans.transform (lowerRight);
    DataLocation center = new DataLocation (
      (upperLeftLoc.get(Grid.ROWS) + lowerRightLoc.get(Grid.ROWS)) / 2,
      (upperLeftLoc.get(Grid.COLS) + lowerRightLoc.get(Grid.COLS)) / 2
    );
    setCenter (center);

    // Set view size
    // -------------
    Dimension savedDims = (Dimension) imageDims.clone();
    Dimension imageDims = new Dimension ( 
      Math.abs (upperLeft.x - lowerRight.x) + 1,
      Math.abs (upperLeft.y - lowerRight.y) + 1
    );
    setSize (imageDims);        

    // Resize view
    // -----------
    resize (savedDims);

  } // magnify

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the view to the full data resolution.  The center data
   * location is reset to the center of the data, the image:data
   * scaling factor to 1, and the image dimensions to the full data
   * dimensions.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void reset () throws NoninvertibleTransformException {

    // Set center point and scale
    // --------------------------
    center = new DataLocation ((dataDims[Grid.ROWS]-1) / 2.0,
      (dataDims[Grid.COLS]-1) / 2.0);
    scale = 1;

    // Initialize transform
    // --------------------
    imageDims = new Dimension (dataDims[Grid.COLS], dataDims[Grid.ROWS]);
    resetTransform();
  
  } // reset

  ////////////////////////////////////////////////////////////

  /**
   * Sets the view to a new size.  The data center location and image
   * scaling factor remain unaffected.
   *
   * @param imageDims the new image dimensions.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public void setSize (
    Dimension imageDims
  ) throws NoninvertibleTransformException {

    // Set image dims and reset
    // ------------------------
    this.imageDims = (Dimension) imageDims.clone();
    resetTransform();

  } // setSize

  ////////////////////////////////////////////////////////////

  /**
   * Calculates the view resolution at the center data location in
   * km/pixel.
   *
   * @return the resolution in km/pixel.
   */
  public double getResolution () {

    Point center = new Point ((imageDims.width-1)/2, (imageDims.height-1)/2);
    return (trans.getResolution (center));

  } // getResolution

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new earth data view using the specified parameters.
   * The view dimensions are initialized to the data dimensions, and
   * the ratio of data to image pixel count to 1.  A check is performed on the
   * transform to determine if it should be oriented differently for
   * display, and flipped or rotated if so.  The verbose mode and 
   * progress mode flags are initially set to false.
   *
   * @param dataDims the data dimensions as [rows, columns].
   * @param earthTrans the earth transform.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  protected EarthDataView (
    int[] dataDims,
    EarthTransform earthTrans
  ) throws NoninvertibleTransformException {

    // Get orientation affine
    // ----------------------
    /**
     * Here we determine the correct orientation for display.  If the 
     * orientation affine returned is a flip, we use it to orient the 
     * display.  Otherwise we honour the orientation hint from the transform.
     * If it cannot be determined, we use an identity transform.
     */
    EarthTransform2D trans2D = earthTrans.get2DVersion();
    AffineTransform affine = OrientationAffineFactory.create (trans2D);
    if (affine != null) {
      if (affine.getType() == AffineTransform.TYPE_FLIP || trans2D.isOrientable())
        orientationAffine = affine;
    } // if
    if (orientationAffine == null)
      orientationAffine = new AffineTransform();
    
    // Set data dimensions and reset
    // -----------------------------
    this.dataDims = (int[]) dataDims.clone();
    trans = new EarthImageTransform (earthTrans, null);
    reset();

    // Set empty overlays
    // ------------------
    overlays = new LinkedList();

    // Set verbose mode off
    // --------------------
    verbose = false;

    // Set progress mode off
    // ---------------------
    progress = false;

  } // EarthDataView constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Prepares this view for rendering using the specified graphics
   * object.  This method is called when any changes have occurred
   * that require the view to be completely reconstructed, such as a
   * change in view size or data window.
   */
  protected abstract void prepare (
    Graphics2D g
  );

  ////////////////////////////////////////////////////////////

  /**
   * Gets the status of view and overlay preparation. If the view is
   * prepared, a render call will return after almost no delay.  If
   * not, the render may require time to complete due to loading data
   * from disk or cache, converting earth locations to screen points,
   * and so on.
   */
  public synchronized boolean isPrepared () { 

    // Check base image preparation
    // ----------------------------
    if (image == null || (image != null && isRendering)) return (false);

    // Check overlay preparation
    // -------------------------
    for (Iterator iter = overlays.iterator(); iter.hasNext();) {
      EarthDataOverlay overlay = (EarthDataOverlay) iter.next();
      if (overlay.getVisible() && !overlay.isPrepared (this)) {
        return (false); 
      } // if
    } // while

    return (true);

  } // isPrepared

  ////////////////////////////////////////////////////////////

  /**
   * Gets the status of the view changed flag.  If the view is modified
   * after rendering by resizing or changing overlays, then the view
   * is changed and may require re-rendering.
   */
  public synchronized boolean isChanged () { return (changed); }

  ////////////////////////////////////////////////////////////

  /** Performs pre-rendering tasks. */
  private synchronized void preRendering () {

    isRendering = true;
    stopRendering = false;

  } // preRendering

  ////////////////////////////////////////////////////////////

  /** Performs post-rendering tasks. */
  private synchronized void postRendering (
    boolean eraseImage
  ) {

    isRendering = false;
    stopRendering = false;
    if (eraseImage) image = null;

  } // postRendering

  ////////////////////////////////////////////////////////////

  /** Renders this view using the graphics object. */
  public void render (
    Graphics2D g
  ) {

    // Set rendering flags
    // -------------------
    synchronized (this) {
      if (stopRendering) { postRendering (false); return; }
      preRendering();
    } // synchronized

    // Prepare data image
    // ------------------
    if (image == null) {
      if (verbose) System.out.println ("EarthDataView: Preparing data image");
      prepare (g);
      if (stopRendering) { postRendering (true); return; }
    } // if

    // Prepare overlays array
    // ----------------------
    ArrayList visibleOverlays = new ArrayList();
    for (Iterator iter = overlays.iterator(); iter.hasNext();) {
      EarthDataOverlay overlay = (EarthDataOverlay) iter.next();
      if (overlay.getVisible()) visibleOverlays.add (overlay);
    } // for
    EarthDataOverlay[] overlaysArray = 
      (EarthDataOverlay[]) visibleOverlays.toArray (new EarthDataOverlay[]{});
    Arrays.sort (overlaysArray);

    // Check if special alpha rendering is needed
    // ------------------------------------------
    boolean supportsAlpha = GraphicsServices.supportsAlpha (g);
    Set preDrawSet = new HashSet();
    if (!supportsAlpha) {

      // Check if any overlay has alpha
      // ------------------------------
      boolean hasAlpha = false;
      boolean alphaAfterNonAlpha = false;
      for (int i = 0; i < overlaysArray.length; i++) {
        if (overlaysArray[i] instanceof TransparentOverlay ||
            overlaysArray[i].getTransparency() != 0) {
          if (!hasAlpha && i != 0) alphaAfterNonAlpha = true;
          hasAlpha = true;
        } // if
      } // for

      // Create pre-draw set
      // -------------------
      if (hasAlpha) {
        for (int i = 0; i < overlaysArray.length; i++) {
          if (alphaAfterNonAlpha || 
            (overlaysArray[i] instanceof TransparentOverlay ||
             overlaysArray[i].getTransparency() != 0))
            preDrawSet.add (overlaysArray[i]);
        } // for
      } // if

      // Draw alpha overlays directly to image
      // -------------------------------------
      if (!preDrawSet.isEmpty()) {

        // Convert to RGB image
        // --------------------
        if (image.getColorModel() instanceof IndexColorModel) {
          BufferedImage newImage = new BufferedImage (image.getWidth(),
            image.getHeight(), BufferedImage.TYPE_INT_RGB);
          newImage.createGraphics().drawImage (image, 0, 0, null);
          image = newImage;               
        } // if

        // Pre-draw overlays
        // -----------------
        Graphics2D gImage = image.createGraphics();
        for (int i = 0; i < overlaysArray.length; i++) {
          if (preDrawSet.contains (overlaysArray[i])) {
            if (verbose) 
              System.out.println ("EarthDataView: Rendering overlay " +
                overlaysArray[i].getName());
            if (stopRendering) { postRendering (false); return; }
            overlaysArray[i].render (gImage, this);
          } // if
        } // for        

      } // if

    } // if

    // Render data image to destination
    // --------------------------------
    if (stopRendering) { postRendering (false); return; }
    if (imageAffine != null) {
      // TODO: We assume here that the missing color can be set to
      // black.  But maybe the missing color should be a property of
      // this class rather than ColorEnhancement so that we can use it
      // here.
      g.setColor (Color.BLACK);
      g.fillRect (0, 0, imageDims.width, imageDims.height);
      g.drawImage (image, imageAffine, null);
    } // if
    else
      g.drawImage (image, 0, 0, null);

    // Render overlays
    // ---------------
    for (int i = 0; i < overlaysArray.length; i++) {
      if (!preDrawSet.contains (overlaysArray[i])) {
        if (verbose) System.out.println ("EarthDataView: Rendering overlay " +
          overlaysArray[i].getName());
        if (stopRendering) { postRendering (false); return; }
        overlaysArray[i].render (g, this);
      } // if
    } // for

    // Set changed flag
    // ----------------
    changed = false;
    postRendering (false);

  } // render

  ////////////////////////////////////////////////////////////

  /**
   * Gets a legend for annotation of the data view.  This default
   * method returns null, but subclasses may override this method to
   * provide a legend.
   *
   * @return a legend or null if no legend is available.
   */
  public Legend getLegend () { return (null); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the rendered view dimensions.
   *
   * @param g the graphics context for rendering.  In this class, the
   * graphics context is ignored and may be null.
   *
   * @return the rendered view dimensions.
   */
  public Dimension getSize (
    Graphics2D g
  ) {

    return ((Dimension) imageDims.clone());

  } // getSize

  ////////////////////////////////////////////////////////////

  /** 
   * Creates and returns a copy of this object.  Child classes should
   * override this method if they contain any deep structure that
   * needs copying.
   */
  public Object clone () {

    // Create shallow copy
    // -------------------
    EarthDataView view = null;
    try {
      view  = (EarthDataView) super.clone();
    } // try
    catch (CloneNotSupportedException e) {
      return (null);
    } // catch

    // Copy deep structures
    // --------------------
    view.setProperties (this);

    return (view);

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Magnifies this view to show the specified subregion.  If the
   * limits of the subregion cannot be determined for the view,
   * no operation is performed.
   * 
   * @param subregion the subregion to show.
   *
   * @see #getSubregion
   */
  @Deprecated
  public void showSubregion (
    Subregion subregion
  ) {

    // Get view limits
    // ---------------
    DataLocation start = new DataLocation (2);
    DataLocation end = new DataLocation (2);
    boolean success = subregion.getLimits (trans.getEarthTransform(), 
      start, end);
    if (!success) return;

    // Magnify view
    // ------------
    Dimension size = this.getSize (null);
    DataLocation dataLoc = new DataLocation (
      (start.get (Grid.ROWS) + end.get (Grid.ROWS))/2,
      (start.get (Grid.COLS) + end.get (Grid.COLS))/2
    ) ;
    double factor = Math.min (
      size.height/(Math.abs (start.get (Grid.ROWS) - end.get (Grid.ROWS)) + 1),
      size.width/(Math.abs (start.get (Grid.COLS) - end.get (Grid.COLS)) + 1)
    );
    try {
      this.reset();
      this.setSize (size);
      this.magnify (dataLoc, factor);
    } // try
    catch (NoninvertibleTransformException e) { }

  } // showSubregion

  ////////////////////////////////////////////////////////////

  /**
   * Gets the subregion currently displayed by this view.
   *
   * @return the subregion displayed. 
   *
   * @see #showSubregion
   */
  @Deprecated
  public Subregion getSubregion () {

    // Get center earth location
    // -------------------------
    EarthTransform earthTrans = trans.getEarthTransform();
    DataLocation centerData = getCenter();
    EarthLocation centerEarth = earthTrans.transform (centerData);

    // Get diameter
    // ------------
    DataLocation[] corners = getBounds();
    DataLocation top = new DataLocation (
      corners[0].get (Grid.ROWS),
      center.get (Grid.COLS)
    ); 
    DataLocation bottom = new DataLocation (
      corners[1].get (Grid.ROWS),
      center.get (Grid.COLS)
    ); 
    DataLocation left = new DataLocation (
      center.get (Grid.ROWS),
      corners[0].get (Grid.COLS)
    ); 
    DataLocation right = new DataLocation (
      center.get (Grid.ROWS),
      corners[1].get (Grid.COLS)
    ); 
    double diameter = Math.min (
      earthTrans.distance (top, bottom),
      earthTrans.distance (left, right)
    );

    return (new Subregion (centerEarth, diameter/2));

  } // getSubregion

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (EarthDataView.class);

    // Create mapped info
    // ------------------
    logger.test ("Framework");
    int rows = 3;
    int cols = 4;
    EarthTransform trans = MapProjectionFactory.getInstance().create (
      GCTP.GEO,
      0,
      new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      GCTP.WGS84,
      new int[] {rows, cols},
      new EarthLocation (0, 0),
      new double[] {1, 1}
    );
    logger.passed();
    
    logger.test ("constructor");
    EarthDataView view = new EarthDataView (trans.getDimensions(), trans) {
      protected void prepare (Graphics2D g) { }
    };
    assert (view.getOrientationAffine().getType() == AffineTransform.TYPE_IDENTITY);
    EarthImageTransform eiTrans = view.getTransform();
    double epsilon = 1e-5;

    Point2D upperLeft = eiTrans.transform (new EarthLocation (rows/2.0, -cols/2.0));
    assert (Math.abs (upperLeft.getX()) < epsilon);
    assert (Math.abs (upperLeft.getY()) < epsilon);
    Point2D lowerRight = eiTrans.transform (new EarthLocation (-rows/2.0, cols/2.0));
    assert (Math.abs (lowerRight.getX() - cols) < epsilon);
    assert (Math.abs (lowerRight.getY() - rows) < epsilon);
  
    logger.passed();
  
    logger.test ("getBounds");
    DataLocation[] bounds = view.getBounds();
    assert (Math.abs (bounds[0].get (Grid.ROWS) - (-0.5)) < epsilon);
    assert (Math.abs (bounds[0].get (Grid.COLS) - (-0.5)) < epsilon);
    assert (Math.abs (bounds[1].get (Grid.ROWS) - (rows-1-0.5)) < epsilon);
    assert (Math.abs (bounds[1].get (Grid.COLS) - (cols-1-0.5)) < epsilon);
    logger.passed();

    logger.test ("resize");
    view.resize (new Dimension (cols*10, rows*10));
    bounds = view.getBounds();
    assert (Math.abs (bounds[0].get (Grid.ROWS) - (-0.5)) < epsilon);
    assert (Math.abs (bounds[0].get (Grid.COLS) - (-0.5)) < epsilon);
    assert (Math.abs (bounds[1].get (Grid.ROWS) - (rows-1+0.4)) < epsilon);
    assert (Math.abs (bounds[1].get (Grid.COLS) - (cols-1+0.4)) < epsilon);
    logger.passed();
  
  } // main

  ////////////////////////////////////////////////////////////

} // EarthDataView class

////////////////////////////////////////////////////////////////////////
