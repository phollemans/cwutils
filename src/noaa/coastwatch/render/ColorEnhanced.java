////////////////////////////////////////////////////////////////////////
/*

     File: ColorEnhanced.java
   Author: Peter Hollemans
     Date: 2005/06/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.Palette;

/**
 * The <code>ColorEnhanced</code> interface gives methods for classes
 * whose data are enhanced with a color palette and enhancement
 * function.  A missing color may also be set which represents data
 * that is outside the enhancement range or otherwise missing.
 *
 * @author Peter Hollemans
 * @since 3.2.0
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
