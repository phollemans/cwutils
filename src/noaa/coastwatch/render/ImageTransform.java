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
           2014/04/02, PFH
           - Changes: Implemented orientation affine changes, and deprecated
             various constructors.
           - Issue: We added constructors to take the orientation affine, but
             didn't fully implement the changes needed to make use of the
             new affine.  The documentation for the scale factor had to be
             updated as well, since its desciption was inverse of the actual
             usage.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

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
 * The ImageTransform class translates between 2D data coordinates
 * and image coordinates in x and y.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
@noaa.coastwatch.test.Testable
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
   *
   * @deprecated As of 3.3.1, use the other constructors instead.
   */
  @Deprecated 
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
   * @deprecated As of 3.3.1, replaced by
   * {@link #ImageTransform(Dimension,Dimension,DataLocation,double)} or
   * {@link #ImageTransform(Dimension,Dimension,DataLocation,double,AffineTransform)}.
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
   * @param scale the ratio of data pixels to view image pixels.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   * 
   * @since 3.3.1
   */
  public ImageTransform (
    Dimension imageDims,
    DataLocation center,
    double scale
  ) throws NoninvertibleTransformException {

    // Create transformation to place world data coords into image viewport
    // --------------------------------------------------------------------
    AffineTransform affine = new AffineTransform();
    affine.preConcatenate (AffineTransform.getTranslateInstance (-center.get (Grid.ROWS), -center.get (Grid.COLS)));
    affine.preConcatenate (AffineTransform.getScaleInstance (1/scale, 1/scale));
    affine.preConcatenate (AffineTransform.getTranslateInstance (imageDims.height/2.0, imageDims.width/2.0));
    affine.preConcatenate (new AffineTransform (0, 1, 1, 0, 0 ,0));

    // Set affine transforms
    // ---------------------
    forward = affine.createInverse();
    inverse = affine;

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
   * @param dataDims the data dimensions.
   * @param center the center data location.  This location transforms
   * to the center of the image coordinates.
   * @param scale the ratio of data pixels to view image pixels.
   * @param orientationAffine the affine transform that orients
   * the data for display.  This may be used for when data row and column
   * coordinates do not correspond to the normal way that a data image is 
   * expected to be displayed.  The affine can be TYPE_FLIP,
   * TYPE_QUANDRANT_ROTATION, or TYPE_IDENTITY only.  The orientation affine
   * is used to rotate or flip the data about its center point in data
   * coordinate space (ie: world coordinates versus viewport coordinates).
   *
   * @throws NoninvertibleTransformException if the data to image
   * transform is not invertible.
   * @throws IllegalArgumentException if the orientation affine is not a flip,
   * quadrant rotation, or identity.
   * 
   * @since 3.3.1
   */
  public ImageTransform (
    Dimension imageDims,
    Dimension dataDims,
    DataLocation center,
    double scale,
    AffineTransform orientationAffine
  ) throws NoninvertibleTransformException {


    // Check orientation affine
    // ------------------------
    int affineType = orientationAffine.getType();
    switch (affineType) {
    case AffineTransform.TYPE_FLIP:
    case AffineTransform.TYPE_QUADRANT_ROTATION:
    case AffineTransform.TYPE_IDENTITY:
      break;
    default:
      throw new IllegalArgumentException ("Invalid orientation affine type");
    } // switch

    // Create transformation to place world data coords into image viewport
    // --------------------------------------------------------------------
    AffineTransform affine = new AffineTransform();
    affine.preConcatenate (AffineTransform.getTranslateInstance (-(dataDims.height-1)/2.0, -(dataDims.width-1)/2.0));
    affine.preConcatenate (orientationAffine);
    affine.preConcatenate (AffineTransform.getTranslateInstance ((dataDims.height-1)/2.0, (dataDims.width-1)/2.0));
    center = center.transform (affine);
    affine.preConcatenate (AffineTransform.getTranslateInstance (-center.get (Grid.ROWS), -center.get (Grid.COLS)));
    affine.preConcatenate (AffineTransform.getScaleInstance (1/scale, 1/scale));
    affine.preConcatenate (AffineTransform.getTranslateInstance (imageDims.height/2.0, imageDims.width/2.0));
    affine.preConcatenate (new AffineTransform (0, 1, 1, 0, 0 ,0));

    // Set affine transforms
    // ---------------------
    forward = affine.createInverse();
    inverse = affine;

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
    DataLocation point = loc.transform (inverse);
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

  /** Tests this class. */
  public static void main (String argv[]) throws Exception {

    System.out.print ("Testing constructors ... ");

    int rows = 100;
    int cols = 200;
    
    ImageTransform imageTrans = new ImageTransform (
      new Dimension (cols*10, rows*10),
      new DataLocation ((rows-1)/2.0, (cols-1)/2.0),
      0.1
    );

    double epsilon = 1e-5;

    Point2D upperLeft = imageTrans.transform (new DataLocation (-0.5, -0.5));
    assert (Math.abs (upperLeft.getX()) < epsilon);
    assert (Math.abs (upperLeft.getY()) < epsilon);
    Point2D lowerRight = imageTrans.transform (new DataLocation (rows-1+0.5, cols-1+0.5));
    assert (Math.abs (lowerRight.getX() - cols*10) < epsilon);
    assert (Math.abs (lowerRight.getY() - rows*10) < epsilon);

    imageTrans = new ImageTransform (
      new Dimension (cols*10, rows*10),
      new Dimension (cols, rows),
      new DataLocation ((rows-1)/2.0, (cols-1)/2.0),
      0.1,
      AffineTransform.getScaleInstance (-1, -1)
    );

    upperLeft = imageTrans.transform (new DataLocation (rows-1+0.5, cols-1+0.5));
    assert (Math.abs (upperLeft.getX()) < epsilon);
    assert (Math.abs (upperLeft.getY()) < epsilon);
    lowerRight = imageTrans.transform (new DataLocation (-0.5, -0.5));
    assert (Math.abs (lowerRight.getX() - cols*10) < epsilon);
    assert (Math.abs (lowerRight.getY() - rows*10) < epsilon);

    imageTrans = new ImageTransform (
      new Dimension (10, 10),
      new Dimension (cols, rows),
      new DataLocation (4.5, 4.5),
      1,
      AffineTransform.getScaleInstance (-1, -1)
    );
    
    DataLocation upperLeftLoc = imageTrans.transform (new Point (0, 0));
    assert (Math.abs (upperLeftLoc.get (Grid.ROWS) - 9.5) < epsilon);
    assert (Math.abs (upperLeftLoc.get (Grid.COLS) - 9.5) < epsilon);
    DataLocation lowerRightLoc = imageTrans.transform (new Point (10, 10));
    assert (Math.abs (lowerRightLoc.get (Grid.ROWS) - (-0.5)) < epsilon);
    assert (Math.abs (lowerRightLoc.get (Grid.COLS) - (-0.5)) < epsilon);

    System.out.println ("OK");

/*
    double[] coefficients = new double[6];
    for (int i = 0; i < argv.length; i++)
      coefficients[i] = Double.parseDouble (argv[i]);
    AffineTransform affine = new AffineTransform (coefficients);
    System.out.println ("affine = " + affine);
    
    int type = affine.getType();
    int[] types = new int[] {
      AffineTransform.TYPE_FLIP,
      AffineTransform.TYPE_GENERAL_ROTATION,
      AffineTransform.TYPE_GENERAL_SCALE,
      AffineTransform.TYPE_GENERAL_TRANSFORM,
      AffineTransform.TYPE_IDENTITY,
      AffineTransform.TYPE_MASK_ROTATION,
      AffineTransform.TYPE_MASK_SCALE,
      AffineTransform.TYPE_QUADRANT_ROTATION,
      AffineTransform.TYPE_TRANSLATION,
      AffineTransform.TYPE_UNIFORM_SCALE
    };
    String[] typeNames = new String[] {
      "TYPE_FLIP",
      "TYPE_GENERAL_ROTATION",
      "TYPE_GENERAL_SCALE",
      "TYPE_GENERAL_TRANSFORM",
      "TYPE_IDENTITY",
      "TYPE_MASK_ROTATION",
      "TYPE_MASK_SCALE",
      "TYPE_QUADRANT_ROTATION",
      "TYPE_TRANSLATION",
      "TYPE_UNIFORM_SCALE"
    };
    for (int i = 0; i < types.length; i++) {
      if ((type & types[i]) != 0)
        System.out.println ("type = " + typeNames[i]);
    } // for

*/

  } // main

  ////////////////////////////////////////////////////////////

} // ImageTransform class

////////////////////////////////////////////////////////////////////////
