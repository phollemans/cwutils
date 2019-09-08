////////////////////////////////////////////////////////////////////////
/*

     File: PalettePanel.java
   Author: Peter Hollemans
     Date: 2003/09/13

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import javax.swing.JPanel;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.Palette;

/**
 * A palette panel displays a color palette graphically as a stripe
 * of colour.
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public class PalettePanel
  extends JPanel {

  // Variables
  // ---------
  /** The currently displayed palette. */
  private Palette palette;

  /** The enhancement function. */
  private EnhancementFunction func;

  /** The data value range. */
  private double[] range;

  ////////////////////////////////////////////////////////////

  /** Creates a new palette panel with no initial palette. */
  public PalettePanel () { 

    palette = null; 
    func = new LinearEnhancement (new double[] {0, 1});
    range = new double[] {0, 1};
    
  } // PalettePanel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new palette panel with the specified initial palette.
   */
  public PalettePanel (
    Palette palette
  ) {

    this();
    this.palette = palette;

  } // PalettePanel constructor

  ////////////////////////////////////////////////////////////

  public void paintComponent (
    Graphics g
  ) {

    // Check showing
    // -------------
    if (!isShowing ()) return;

    // Call super
    // ----------
    super.paintComponent (g);

    // Check palette
    // -------------
    if (palette == null || func == null || range == null) return;

    // Render buffered image
    // ---------------------
    Dimension dims = getSize();
    Image image = createImage (dims);
    g.drawImage (image, 0, 0, null);

  } // paintComponent

  ////////////////////////////////////////////////////////////

  /**
   * Creates an image of the palette for the panel.  This is made public
   * for use in rendering palette images in other contexts.
   *
   * @param dims the dimensions of the image to create.
   *
   * @return the palette image of the specified dimensions.
   *
   * @since 3.5.1
   */
  public Image createImage (
    Dimension dims
  ) {

    // Create buffered image
    // ---------------------
    IndexColorModel colorModel = palette.getModel();
    if (colorModel.getMapSize() > 256) {
      colorModel = palette.remap (256).getModel();
    } // if
    BufferedImage image = new BufferedImage (dims.width, dims.height,
      BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    WritableRaster raster = image.getRaster();

    // Draw colour stripe
    // ------------------
    byte[] byteRow = new byte[dims.width];
    int colors = colorModel.getMapSize();
    double scale = (double) (range[1] - range[0]) / (dims.width-1);
    for (int i = 0; i < dims.width; i++) {
      double norm = func.getValue (i*scale + range[0]);
      if (norm < 0) { byteRow[i] = 0; continue; }
      else if (norm > 1) norm = 1;
      byteRow[i] = (byte) Math.round (norm * (colors-1));
    } // for
    for (int j = 0; j < dims.height; j++)
      raster.setDataElements (0, j, dims.width, 1, byteRow);

    return (image);

  } // createImage

  ////////////////////////////////////////////////////////////

  /** Sets the current palette. */
  public void setPalette (Palette palette) {

    this.palette = palette;
    repaint();

  } // setPalette

  ////////////////////////////////////////////////////////////

  /**
   * Sets the enhancement function.  The enhancement function is used
   * to change the way that the palette is displayed.  Normally the
   * palette stripe is displayed as a simple left-to-right set of
   * colors showing the palette colors with index 0 to nColors-1 where
   * nColors is the number of colors in the palette.  When an
   * enhancement function is applied, the palette stripe is modified
   * so that stripe reflects how colors would be assigned to data
   * values in a ColorEnhancement view with the specified enhancement
   * and palette.  The left end of the stripe shows the color given to
   * the minimum value in the range, the right side the maximum value,
   * and the colors between reflect the shape of the enhancement
   * function.
   *
   * By default the enhancement function is a linear enhancement with
   * range [0..1].
   *
   * @param func the enhancement function.
   *
   * @see noaa.coastwatch.render.ColorEnhancement
   */   
  public void setFunction (
    EnhancementFunction func
  ) {

    this.func = (EnhancementFunction) func.clone();
    repaint();
    
  } // setFunction

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the data value range.  The range is used in combination with
   * the enhancement function to determine the colors shown in the
   * color stripe.  By default the range is determined from the
   * enhancement function itself.
   *
   * @param range the data value range as [min, max].
   */
  public void setRange (
    double[] range
  ) {

    this.range = (double[]) range.clone();
    repaint();

  } // setRange

  ////////////////////////////////////////////////////////////

} // PalettePanel

////////////////////////////////////////////////////////////////////////
