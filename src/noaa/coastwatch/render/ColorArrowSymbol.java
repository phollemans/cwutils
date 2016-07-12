////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorArrowSymbol.java
  PURPOSE: Renders a point feature as an arrow with color coding.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/06
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.IndexColorModel;
import noaa.coastwatch.render.ArrowSymbol;
import noaa.coastwatch.render.ColorEnhanced;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.util.trans.EarthTransform2D;

/**
 * A <code>ColorArrowSymbol</code> is similar to a normal arrow but it
 * has the additional feature that arrows are colored according to a
 * color enhancement scheme based on the vector magnitude.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class ColorArrowSymbol
  extends ArrowSymbol
  implements ColorEnhanced {
  
  // Variables
  // ---------

  /** The color palette to use for enhancement. */
  private Palette pal;

  /** The data enhancement function. */
  private EnhancementFunction func;
 
  /** The color to use for invalid or below range values. */
  private Color missingColor;

  ////////////////////////////////////////////////////////////

  /** Gets the enhancement color palette. */
  public Palette getPalette () { return (pal); }

  ////////////////////////////////////////////////////////////

  /** Sets the enhancement color palette. */
  public void setPalette (Palette pal) { this.pal = pal; }

  ////////////////////////////////////////////////////////////

  /** Gets the enhancement function. */
  public EnhancementFunction getFunction () { return (func); }

  ////////////////////////////////////////////////////////////
  
  /** Sets the enhancement function. */
  public void setFunction (EnhancementFunction func) { this.func = func; }

  ////////////////////////////////////////////////////////////

  /** Gets the missing value color. */
  public Color getMissingColor () { return (missingColor); }

  ////////////////////////////////////////////////////////////

  /** Sets the missing value color. */
  public void setMissingColor (Color color) { this.missingColor = color; }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new color arrow symbol based on U and V components.
   *
   * @param uComponentAtt the feature attribute for U component.
   * @param vComponentAtt the feature attribute for V component.
   * @param pal the color palette for color enhancement.
   * @param func the enhancement function.
   */
  public ColorArrowSymbol (
    int uComponentAtt,
    int vComponentAtt,
    Palette pal,
    EnhancementFunction func
  ) {

    super (uComponentAtt, vComponentAtt);
    this.pal = pal;
    this.func = func;
    missingColor = Color.BLACK;

  } // ColorArrowSymbol constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new color arrow symbol based on magnitude and direction
   * components.
   *
   * @param magnitudeAtt the feature attribute for vector magnitude.
   * @param directionAtt the feature attribute for vector direction.
   * @param trans the earth transform used for converting directions.
   * @param pal the color palette for color enhancement.
   * @param func the enhancement function.
   */
  public ColorArrowSymbol (
    int magnitudeAtt,
    int directionAtt,
    EarthTransform2D trans,
    Palette pal,
    EnhancementFunction func
  ) {

    super (magnitudeAtt, directionAtt, trans);
    this.pal = pal;
    this.func = func;
    missingColor = Color.BLACK;

  } // ColorArrowSymbol constructor

  ////////////////////////////////////////////////////////////

  public void draw (
    Graphics gc, 
    int x, 
    int y
  ) {

    // Set symbol color
    // ----------------
    double magnitude = getMagnitude();
    Color color;
    if (Double.isNaN (magnitude)) color = missingColor;
    else {
      double norm = func.getValue (magnitude);
      if (norm < 0) color = missingColor;
      else {
        IndexColorModel model = pal.getModel();
        int size = model.getMapSize();
        if (norm > 1) norm = 1;
        int index = (int) Math.round (norm*(size-1));
        color = new Color (model.getRGB (index));
      } // else
    } // else
    setBorderColor (color);

    // Draw symbol
    // -----------
    super.draw (gc, x, y);

  } // draw

  ////////////////////////////////////////////////////////////

} // ColorArrowSymbol class

////////////////////////////////////////////////////////////////////////
