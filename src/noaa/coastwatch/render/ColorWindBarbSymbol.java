////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorWindBarbSymbol.java
  PURPOSE: Renders a point feature as a wind barb with color coding.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/26
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
import noaa.coastwatch.render.ColorEnhanced;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.WindBarbSymbol;
import noaa.coastwatch.util.trans.EarthTransform2D;

/**
 * A <code>ColorWindBarbSymbol</code> is similar to a normal wind
 * barb but it has the additional feature that barbs are colored
 * according to a color enhancement scheme based on the wind speed.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class ColorWindBarbSymbol
  extends WindBarbSymbol
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

  /** Gets the enhancement function. */
  public EnhancementFunction getFunction () { return (func); }

  ////////////////////////////////////////////////////////////

  /** Sets the enhancement color palette. */
  public void setPalette (Palette pal) { 

    this.pal = pal; 

  } // setPalette

  ////////////////////////////////////////////////////////////

  /** Sets the missing value color. */
  public void setMissingColor (Color missingColor) {

    this.missingColor = missingColor;

  } // setMissingColor

  ////////////////////////////////////////////////////////////

  /** Gets the missing value color. */
  public Color getMissingColor () { return (missingColor); }

  ////////////////////////////////////////////////////////////
  
  /** Sets the enhancement function. */
  public void setFunction (EnhancementFunction func) { 

    this.func = func; 

  } // setFunction

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new color wind barb symbol.
   *
   * @param speedAtt the feature attribute for wind speed.
   * @param directionAtt the feature attribute for wind direction.
   * @param speedUnits the units of speed, either
   * <code>SPEED_KNOTS</code> or <code>SPEED_METERS_PER_SECOND</code>.
   * @param trans the Earth transform used for converting directions.
   * @param pal the color palette for color enhancement.
   * @param func the enhancement function.
   */
  public ColorWindBarbSymbol (
    int speedAtt,
    int directionAtt,
    int speedUnits,
    EarthTransform2D trans,
    Palette pal,
    EnhancementFunction func
  ) {

    super (speedAtt, directionAtt, speedUnits, trans);
    this.pal = pal;
    this.func = func;
    missingColor = Color.BLACK;

  } // ColorWindBarbSymbol constructor

  ////////////////////////////////////////////////////////////

  public void draw (
    Graphics gc, 
    int x, 
    int y
  ) {

    // Set symbol color
    // ----------------
    double speed = getMagnitude();
    Color color;
    if (Double.isNaN (speed)) color = missingColor;
    else {
      double norm = func.getValue (speed);
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

} // ColorWindBarbSymbol class

////////////////////////////////////////////////////////////////////////
