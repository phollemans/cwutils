////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorEnhanced.java
  PURPOSE: Interface for color enhanced classes.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/02
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
import java.awt.*;

/**
 * The <code>ColorEnhanced</code> interface gives methods for classes
 * whose data are enhanced with a color palette and enhancement
 * function.  A missing color may also be set which represents data
 * that is outside the enhancement range or otherwise missing.
 */
public interface ColorEnhanced {

  /** Gets the enhancement color palette. */
  public Palette getPalette ();

  /** Sets the enhancement color palette. */
  public void setPalette (Palette pal);

  /** Gets the enhancement function. */
  public EnhancementFunction getFunction ();

  /** Sets the enhancement function. */
  public void setFunction (EnhancementFunction func);

  /** Gets the missing value color. */
  public Color getMissingColor ();

  /** Sets the missing value color. */
  public void setMissingColor (Color missingColor);

} // ColorEnhanced

////////////////////////////////////////////////////////////////////////
