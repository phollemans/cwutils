////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthImageTransform.java
  PURPOSE: Class to set up geographic to image coordinate transformations.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/02
  CHANGES: 2002/10/08, PFH, changed to use center location
           2002/10/10, PFH, changed transform to return Point2D
           2002/12/12, PFH, added getResolution, isDiscontinuous
           2003/05/26, PFH, changed discontinuous jump factor threshold to 10

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

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
import noaa.coastwatch.util.trans.*;

/**
 * The Earth image transform class translates between 2D geographic
 * coordinates in latitude, longitude and image coordinates in x, y.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class EarthImageTransform {

  /** The Earth transform for geo<-->data. */
  private EarthTransform earthTrans;

  /** The image transform for data<-->image. */
  private ImageTransform imageTrans;

  ////////////////////////////////////////////////////////////

  /** Gets the Earth transform. */
  public EarthTransform getEarthTransform () { return (earthTrans); }

  ////////////////////////////////////////////////////////////

  /** Gets the image transform. */
  public ImageTransform getImageTransform () { return (imageTrans); }

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs an Earth image transform with the specified parameters.
   *
   * @param earthTrans the Earth transform.
   * @param imageTrans the image transform.
   */
  public EarthImageTransform (
    EarthTransform earthTrans,
    ImageTransform imageTrans
  ) {

    this.earthTrans = earthTrans;
    this.imageTrans = imageTrans;

  } // EarthImageTransform constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new Earth image transform from the specified image
   * dimensions and geographic center.
   *
   * @param imageDims the image dimensions.
   * @param center the center Earth location.
   * @param scales the image to data scaling factors as [row, column].
   * @param earthTrans the Earth transform.
   *
   * @throws NoninvertibleTransformException if the image to data
   * transform is not invertible.
   */
  public EarthImageTransform (
    Dimension imageDims,
    EarthLocation center,
    double[] scales,
    EarthTransform earthTrans
  ) throws NoninvertibleTransformException {

    this.earthTrans = earthTrans;
    this.imageTrans = new ImageTransform (imageDims, 
      earthTrans.transform (center), scales);

  } // EarthImageTransform constructor

  ////////////////////////////////////////////////////////////

  /**
   * Converts geographic coordinates to image coordinates.
   *
   * @param geo the geographic location.
   *
   * @return the image coordinates, or null if the geographic point
   * could not be transformed.
   *
   * @see #transform(Point)
   */
  public Point2D transform (
    EarthLocation geo
  ) {

    return (imageTrans.transform (earthTrans.transform(geo)));

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Converts arbitrary precision image coordinates to geographic 
   * coordinates.
   *
   * @param point the image coordinates.
   *
   * @return the geographic location.
   *
   * @see #transform(EarthLocation)
   */
  public EarthLocation transform (
    Point2D point
  ) {

    return (earthTrans.transform (imageTrans.transform(point)));

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Converts integer image coordinates to geographic coordinates.
   *
   * @param point the image coordinates.
   *
   * @return the geographic location.
   *
   * @see #transform(EarthLocation)
   */
  public EarthLocation transform (
    Point point
  ) {

    return (earthTrans.transform (imageTrans.transform(point)));

  } // transform

  ////////////////////////////////////////////////////////////

  /**
   * Calculates an approximate resolution in km/pixel.
   *
   * @param point the image point.
   *
   * @return the resolution in km/pixel at the image point.
   */
  public double getResolution (
    Point2D point
  ) {

    // Calculate resolution in x and y direction
    // -----------------------------------------
    double dx = 
      transform (new Point2D.Double (point.getX()-1, point.getY())).distance (
      transform (new Point2D.Double (point.getX()+1, point.getY()))) / 2;
    double dy = 
      transform (new Point2D.Double (point.getX(), point.getY()-1)).distance (
      transform (new Point2D.Double (point.getX(), point.getY()+1))) / 2;

    // Return maximum resolution 
    // -------------------------
    return (Math.min (dx, dy));

  } // getResolution

  ////////////////////////////////////////////////////////////

  /**
   * Determines if a segment running from one Earth location to
   * another is discontinuous in the image space.  The calculation
   * uses a heuristic, so in some extreme cases it may not return the
   * correct answer.
   *
   * @param e1 the first Earth location.
   * @param e2 the second Earth location.
   * @param p1 the first image point (or null to calculate).
   * @param p2 the second image point (or null to calculate).
   *
   * @return true if the segment is discontinuous, or false if not or
   * if the Earth locations could not be resolved to image points.
   */
  public boolean isDiscontinuous (
    EarthLocation e1,
    EarthLocation e2,
    Point2D p1,
    Point2D p2
  ) {

    // Calculate image points
    // ----------------------
    if (p1 == null) p1 = transform (e1);
    if (p2 == null) p2 = transform (e2);
    if (p1 == null || p2 == null) return (false);
 
    // Check for jump using resolution
    // -------------------------------
    double earthDist = e1.distance (e2);
    double imageDist = p1.distance (p2);
    double res = getResolution (p1);
    double jumpFactor = (imageDist/earthDist) / (1/res);
    boolean jumped;
    if (Double.isNaN (jumpFactor)) jumped = true;
    else jumped = (jumpFactor > 10);

    return (jumped);

  } // isDiscontinuous

  ////////////////////////////////////////////////////////////

} // EarthImageTransform class

////////////////////////////////////////////////////////////////////////
