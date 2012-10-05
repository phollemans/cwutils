////////////////////////////////////////////////////////////////////////
/*
     FILE: PalettePanel.java
  PURPOSE: To show a color palette graphically.
   AUTHOR: Peter Hollemans
     DATE: 2003/09/13
  CHANGES: 2003/11/22, PFH, fixed Javadoc comments
           2004/02/17, PFH, changed setEnhancement to setFunction
           2004/02/09, PFH, fixed color range problem
           2005/03/21, PFH, added clone() calls
           2005/10/09, PFH, added check for >256 color palette

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;
import noaa.coastwatch.render.*;

/**
 * A palette panel displays a color palette graphically as a stripe
 * of colour.
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

    // Create buffered image
    // ---------------------
    Dimension dims = getSize();
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
    g.drawImage (image, 0, 0, null);

  } // paintComponent

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
   * @see ColorEnhancement
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
