////////////////////////////////////////////////////////////////////////
/*
     FILE: SolidBackground.java
  PURPOSE: A class to view Earth data overlays against a plain background.
   AUTHOR: Peter Hollemans
     DATE: 2002/12/01
  CHANGES: 2003/04/19, PFH, added rendering progress mode
           2005/05/30, PFH, added set/get background methods

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * The solid background class is an Earth data view that creates a
 * solid image of color to help view overlay graphics.
 */
public class SolidBackground 
  extends EarthDataView {

  // Variables
  // ---------

  /** The background color. */
  private Color color;

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
   * Constructs a new solid background with the specified color.
   * 
   * @param trans the view Earth transform.
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
