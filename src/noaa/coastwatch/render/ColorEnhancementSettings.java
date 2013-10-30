////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorEnhancementSettings.java
  PURPOSE: To store the palette and function for a color enhancement.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/19
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

/** 
 * The <code>ColorEnhancementSettings</code> class stores palette and
 * enhancement function information for a color enhancement of a data
 * variable.  The settings may be used to save and later restore 
 * a color enhancement.
 *
 * @see noaa.coastwatch.render.ColorEnhancement
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ColorEnhancementSettings 
  implements Cloneable {

  // Variables
  // ---------

  /** The variable used for data in the enhancement. */
  private String variableName;

  /** The enhancement palette. */
  private Palette palette;

  /** The enhancement function. */
  private EnhancementFunction function;
  
  ////////////////////////////////////////////////////////////

  /** Gets the variable name. */
  public String getName () { return (variableName); }

  ////////////////////////////////////////////////////////////

  /** Gets the color enhancement palette. */
  public Palette getPalette () { return (palette); }

  ////////////////////////////////////////////////////////////

  /** Sets the color enhancement palette. */
  public void setPalette (Palette palette) { this.palette = palette; }

  ////////////////////////////////////////////////////////////

  /** Gets the color enhancement function. */
  public EnhancementFunction getFunction () { return (function); }

  ////////////////////////////////////////////////////////////

  /** Gets the color enhancement function. */
  public void setFunction (
    EnhancementFunction function
  ) { 

    this.function = function; 

  } // setFunction

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new settings object.
   * 
   * @param variableName the data variable name.
   * @param palette the color enhancement palette.
   * @param function the color enhancement function.
   */
  public ColorEnhancementSettings (
    String variableName,
    Palette palette,
    EnhancementFunction function
  ) {

    this.variableName = variableName;
    this.palette = palette;
    this.function = function;

  } // ColorEnhancementSettings constructor

  ////////////////////////////////////////////////////////////

  /** Creates and returns a copy of this object. */
  public Object clone () {

    try {
      ColorEnhancementSettings settings = 
        (ColorEnhancementSettings) super.clone();
      settings.function = (EnhancementFunction) this.function.clone();
      return (settings);
    } // try
    catch (CloneNotSupportedException e) {
      return (null);
    } // catch

  } // clone

  ////////////////////////////////////////////////////////////

} // ColorEnhancementSettings

////////////////////////////////////////////////////////////////////////
