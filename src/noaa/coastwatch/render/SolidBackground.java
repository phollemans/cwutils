////////////////////////////////////////////////////////////////////////
/*
     FILE: SolidBackground.java
  PURPOSE: A class to view earth data overlays against a plain background.
   AUTHOR: Peter Hollemans
     DATE: 2002/12/01
  CHANGES: 2003/04/19, PFH, added rendering progress mode
           2005/05/30, PFH, added set/get background methods
           2014/03/25, PFH
           - Changes: Added a new constructor that only needs a color.
           - Issue: Changes in EarthDataView made it so that routines that
             called the constructor with null as the transform threw a
             NullPointerException.  So we created a new constructor
             and a default transform to use in these cases when the transform
             is not important.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

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
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjectionFactory;

/**
 * The solid background class is an earth data view that creates a
 * solid image of color to help view overlay graphics.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class SolidBackground 
  extends EarthDataView {

  // Variables
  // ---------

  /** The background color. */
  private Color color;

  /** The transform to use when null is passed in the constructor. */
  private static EarthTransform defaultTransform;

  ////////////////////////////////////////////////////////////

  static {

    /**
     * Here we create a geographic transform with 1 deg/pixel covering the
     * entire Earth, centered at 0N, 0E.  It will likely never be used by
     * the drawing code (because routines using the color-only constructor
     * were never adding any overlays), but you never know, maybe some day.
     */
    try {
      defaultTransform = MapProjectionFactory.getInstance().create (
        GCTP.GEO,
        0,
        new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        GCTP.WGS84,
        new int[] {180, 360},
        new EarthLocation (0, 0),
        new double[] {1, 1}
      );
    } // try
    catch (NoninvertibleTransformException e) {
      throw new ExceptionInInitializerError (e);
    } // catch
  
  } // static

  ////////////////////////////////////////////////////////////


  /** Sets the background color. */
  public void setBackground (
    Color color
  ) {

    this.color = color;
    updateColorModel();

  } // setBackground

  ////////////////////////////////////////////////////////////

  /**
   * Updates the index color model of the image if an image is
   * available.
   */
  private void updateColorModel () {

    if (image != null) {

      // Create color model
      // ------------------
      IndexColorModel colorModel = new IndexColorModel (1, 1, 
        new byte[] {(byte)color.getRed()}, 
        new byte[] {(byte)color.getGreen()},
        new byte[] {(byte)color.getBlue()});

      // Convert image to new color model
      // --------------------------------
      image = new BufferedImage (colorModel, image.getRaster(), false, null);
      changed = true;

    } // if

  } // updateColorModel

  ////////////////////////////////////////////////////////////

  /** Gets the background color. */
  public Color getBackground () { return (color); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new solid background with the specified color.  The
   * transform is set to an internal default transform.
   * 
   * @param color the background color.
   *
   * @throws NoninvertibleTransformException if the resulting image 
   * transform is not invertible.
   *
   * @since 3.3.1
   */
  public SolidBackground (
    Color color
  ) throws NoninvertibleTransformException {

    this (defaultTransform, defaultTransform.getDimensions(), color);

  } // SolidBackground constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new solid background with the specified color.
   * 
   * @param trans the view earth transform.
   * @param dims the grid dimensions to use for this view.
   * @param color the background color.
   *
   * @throws NoninvertibleTransformException if the resulting image 
   * transform is not invertible.
   */
  public SolidBackground (
    EarthTransform trans,
    int[] dims,
    Color color
  ) throws NoninvertibleTransformException {

    // Initialize variables
    // --------------------
    super (dims, trans);
    this.color = color;

  } // SolidBackground constructor

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g
  ) {

    // Create new image
    // ----------------
    IndexColorModel colorModel = new IndexColorModel (1, 1, 
      new byte[] {(byte)color.getRed()}, 
      new byte[] {(byte)color.getGreen()},
      new byte[] {(byte)color.getBlue()});
    image = new BufferedImage (imageDims.width, imageDims.height,
      BufferedImage.TYPE_BYTE_BINARY, colorModel);

  } // prepare

  ////////////////////////////////////////////////////////////

} // SolidBackground class

////////////////////////////////////////////////////////////////////////
