////////////////////////////////////////////////////////////////////////
/*
     FILE: ImageTransform.java
  PURPOSE: Class to set up image to data coordinate transformations.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/02
  CHANGES: 2002/09/04, PFH, added isValid
           2002/09/16, PFH, reverse coordinate order in affine
           2002/09/23, PFH, modified for reversed corner coordinates
           2002/10/08, PFH, changed to use center location
           2002/10/10, PFH, changed transform to return Point2D
           2002/12/12, PFH, added transform (Point2D)
           2003/05/28, PFH, corrected pixel shift for overlay lines
           2013/05/31, PFH, added new constructors and orientation affine

  CoastWatch Software Library and Utilities
  Copyright 1998-2003, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Import
// ------
import java.awt.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;

/**
 * The image transform class translates between 2D data coordinates
 * and image coordinates in x and y.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class ImageTransform {

  /** The image to data coordinate transform. */
  private AffineTransform forward;

  /** The data to image coordinate transform. */
  private AffineTransform inverse;

  /** The image to data location coordinate cache for rows. */
  private double[] rowCache;

  /** The image to data location coordinate cache for columns. */
  private double[] colCache;

  /** The image dimensions. */
  private Dimension imageDims;

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new default image transform set to unity.
   *
   * @param imageDims the image dimensions.
   */
  public ImageTransform (
    Dimension imageDims
  ) {

    // Initialize transforms
    // ---------------------
    forward = new AffineTransform (0, 1, 1, 0, 0.5, 0.5);
    inverse = new AffineTransform (0, 1, 1, 0, -0.5, -0.5);

    // Initialize coordinate caches
    // ----------------------------
    this.imageDims = (Dimension) imageDims.clone();
    rowCache = colCache = null;

  } // ImageTransform constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new image transform with the specified image
   * dimensions, center data location, and scaling factors.
   *
   * @param imageDims the image dimensions.
   * @param center the center data location.  This location transforms
   * to the center of the image coordinates.
   * @param scales the image to data scaling factors as [row, column].
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   *
   * @deprecated As of 3.3.0, replaced by 
   * {@link #ImageTransform(Dimension,DataLocation,double)} or
   * {@link #ImageTransform(Dimension,DataLocation,double,AffineTransform)}.
   */
  @Deprecated
  public ImageTransform (
    Dimension imageDims,
    DataLocation center,
    double[] scales
  ) throws NoninvertibleTransformException {

    // Calculate row, column shifts
    // ----------------------------
    double[] shifts = {
      center.get(Grid.ROWS) - (imageDims.height/2.0)*scales[Grid.ROWS],
      center.get(Grid.COLS) - (imageDims.width/2.0)*scales[Grid.COLS]
    };

    // Set affine transforms
    // ---------------------
    forward = new AffineTransform (0, scales[Grid.ROWS], scales[Grid.COLS], 0, 
      shifts[Grid.ROWS], shifts[Grid.COLS]);
    inverse = forward.createInverse();

    // Initialize coordinate caches
    // ----------------------------
    this.imageDims = (Dimension) imageDims.clone();
    rowCache = colCache = null;

  } // ImageTransform constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new image transform with the specified image
   * dimensions, center data location, and scaling factor.
   *
   * @param imageDims the image dimensions.
   * @param center the center data location.  This location transforms
   * to the center of the image coordinates.
   * @param scale the image to data scaling factor.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   * 
   * @since 3.3.0
   */
  public ImageTransform (
    Dimension imageDims,
    DataLocation center,
    double scale
  ) throws NoninvertibleTransformException {

    // Calculate row, column shifts
    // ----------------------------
    double[] shifts = {
      center.get(Grid.ROWS) - (imageDims.height/2.0)*scale,
      center.get(Grid.COLS) - (imageDims.width/2.0)*scale
    };

    // Set affine transforms
    // ---------------------
    forward = new AffineTransform (0, scale, scale, 0, 
      shifts[Grid.ROWS], shifts[Grid.COLS]);
    inverse = forward.createInverse();

    // Initialize coordinate caches
    // ----------------------------
    this.imageDims = (Dimension) imageDims.clone();
    rowCache = colCache = null;

  } // ImageTransform constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new image transform with the specified image
   * dimensions, center data location, scaling factors, and orientation 
   * affine.
   *
   * @param imageDims the image dimensions.
   * @param center the center data location.  This location transforms
   * to the center of the image coordinates.
   * @param scale the image to data scaling factor.
   * @param orientationAffine the affine transform that orients
   * the image for display.  This may be used for when data row and column
   * coordinates do not correspond to the normal way that a data image is 
   * expected to be displayed.  The affine must be TYPE_UNIFORM_SCALE
   * only.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   * @throws IllegalArgumentException if the oreitnation affine is not 
   * TYPE_UNIFORM_SCALE. 
   */
  public ImageTransform (
    Dimension imageDims,
    DataLocation center,
    double scale,
    AffineTransform orientationAffine
  ) throws NoninvertibleTransformException {

    // Calculate row, column shifts
    // ----------------------------
    double[] shifts = {
      center.get(Grid.ROWS) - (imageDims.height/2.0)*scale,
      center.get(Grid.COLS) - (imageDims.width/2.0)*scale
    };

    // Set affine transforms
    // ---------------------
    forward = new AffineTransform (0, scale, scale, 0, 
      shifts[Grid.ROWS], shifts[Grid.COLS]);
    inverse = forward.createInverse();




    // TODO: Actually implement the orientation affine.  This will fix
    // the issue of data appearing upside down when stored from south to
    // north.
    if (orientationAffine.getType() != AffineTransform.TYPE_UNIFORM_SCALE)
      throw new IllegalArgumentException ("Orientation affine not uniform scale");


    



    // Initialize coordinate caches
    // ----------------------------
    this.imageDims = (Dimension) imageDims.clone();
    rowCache = colCache = null;

  } // ImageTransform constructor

  ////////////////////////////////////////////////////////////

  /**
   * Converts data coordinates to image coordinates.
   *
   * @param loc the data location.
   *
   * @return the image coordinates, or null if the location is invalid.
   *
   * @see #transform(Point)
   */
  public Point2D transform (
    DataLocation loc
  ) {

    if (!loc.isValid()) return (null);
    DataLocation point = loc.transform(inverse);
    return (new Point2D.Double (point.get(0), point.get(1)));

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Converts arbitrary precision image coordinates to data coordinates.
   *
   * @param point the image coordinates.
   *
   * @return the data location.
   *
   * @see #transform(DataLocation)
   */
  public DataLocation transform (
    Point2D point
  ) {

    // Perform explicit transform
    // --------------------------
    DataLocation loc = new DataLocation (point.getX(), point.getY());
    return (loc.transform (forward));

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Converts integer image coordinates to data coordinates.
   *
   * @param point the image coordinates.
   *
   * @return the data location.
   *
   * @see #transform(DataLocation)
   */
  public DataLocation transform (
    Point point
  ) {

    // Compute image->data caches
    // --------------------------
    if (rowCache == null) computeCaches();

    // Perform explicit transform
    // --------------------------
    if (point.x < 0 || point.x > imageDims.width-1 ||
      point.y < 0 || point.y > imageDims.height-1) {
      DataLocation loc = new DataLocation (point.x, point.y);
      return (loc.transform (forward));
    } // if

    // Perform cached transform
    // ------------------------
    return (new DataLocation (rowCache[point.y], colCache[point.x]));

  } // transform

  ////////////////////////////////////////////////////////////

  /** Creates a set of caches to speed image->data transforms. */
  private void computeCaches () {

    // Create caches
    // -------------
    rowCache = new double[imageDims.height];
    colCache = new double[imageDims.width];

    // Calculate cache points
    // ----------------------
    for (Point p = new Point (0,0); p.x < imageDims.width; p.x++) {
      DataLocation loc = new DataLocation (p.x, p.y);
      loc = loc.transform (forward);
      colCache[p.x] = loc.get(Grid.COLS);
    } // for
    for (Point p = new Point (0,0); p.y < imageDims.height; p.y++) {
      DataLocation loc = new DataLocation (p.x, p.y);
      loc = loc.transform (forward);
      rowCache[p.y] = loc.get(Grid.ROWS);
    } // for

  } // computeCaches

  ////////////////////////////////////////////////////////////

  /** Gets the data to image coordinate transform. */
  public AffineTransform getAffine () { 
    return ((AffineTransform) inverse.clone()); 
  } // getAffine

  ////////////////////////////////////////////////////////////

  /** Gets the current image dimensions. */
  public Dimension getImageDimensions () {
    return ((Dimension) imageDims.clone()); 
  } // getImageDimensions

  ////////////////////////////////////////////////////////////

} // ImageTransform class

////////////////////////////////////////////////////////////////////////
