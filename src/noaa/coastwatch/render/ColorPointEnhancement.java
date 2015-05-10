////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorPointEnhancement.java
  PURPOSE: Plots color-enhanced point feature data.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/31
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
import java.awt.geom.NoninvertibleTransformException;
import noaa.coastwatch.render.ColorEnhanced;
import noaa.coastwatch.render.ColorEnhancementSettings;
import noaa.coastwatch.render.DataColorScale;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.Legend;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.SolidBackground;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>ColorPointEnhancement</code> class uses a {@link Palette}
 * and {@link EnhancementFunction} to render a {@link
 * PointFeatureOverlay} to an Earth view.
 *
 * @see ColorEnhanced
 * @see ColorEnhancement
 * @see PointFeatureOverlay
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class ColorPointEnhancement 
  extends SolidBackground {

  // Variables
  // ---------

  /** The name to use for the color bar legend. */
  private String name;

  /** The units to use for the color bar legend. */
  private String units;

  /** The overlay of color point data. */
  private PointFeatureOverlay overlay;

  ////////////////////////////////////////////////////////////

  /** Gets the enhancement color palette. */
  public Palette getPalette () { 

    return (((ColorEnhanced) overlay.getSymbol()).getPalette()); 

  } // getPalette

  ////////////////////////////////////////////////////////////

  /** Gets the enhancement function. */
  public EnhancementFunction getFunction () { 

    return (((ColorEnhanced) overlay.getSymbol()).getFunction()); 

  } // getFunction

  ////////////////////////////////////////////////////////////

  /** Sets the enhancement color palette. */
  public void setPalette (Palette pal) { 

    ((ColorEnhanced) overlay.getSymbol()).setPalette (pal); 

  } // setPalette

  ////////////////////////////////////////////////////////////

  /** Sets the missing value color. */
  public void setMissingColor (Color missingColor) { 

    ((ColorEnhanced) overlay.getSymbol()).setMissingColor (missingColor); 

  } // setMissingColor

  ////////////////////////////////////////////////////////////

  /** Gets the missing value color. */
  public Color getMissingColor () {

    return (((ColorEnhanced) overlay.getSymbol()).getMissingColor());

  } // getMissingColor

  ////////////////////////////////////////////////////////////
  
  /** Sets the enhancement function. */
  public void setFunction (EnhancementFunction func) { 

    ((ColorEnhanced) overlay.getSymbol()).setFunction (func);

  } // setFunction

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new point enhancement with white background.
   * 
   * @param overlay the overlay to use for rendering point data.
   * @param name the enhancement variable name used for color bar legend.
   * @param units the enhancement variable units used for color bar legend.
   * @param trans the view Earth transform.
   *
   * @throws NoninvertibleTransformException if the resulting image 
   * transform is not invertible.
   */
  public ColorPointEnhancement (
    PointFeatureOverlay overlay,
    String name,
    String units,
    EarthTransform trans
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (trans, trans.getDimensions(), Color.WHITE);
    this.name = name;
    this.units = units;
    this.overlay = overlay;

    // Create point overlay
    // --------------------
    overlay.setLayer (-1);
    addOverlay (overlay);

  } // ColorPointEnhancement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets a data color scale legend for annotation of this color
   * point enhancement.
   *
   * @return the data color scale legend.  The legend is constructed
   * with the default font and colors.
   *
   * @see DataColorScale
   */
  public Legend getLegend () { 

    return (new DataColorScale (getFunction(), getPalette(), 
      name, units));

  } // getLegend

  ////////////////////////////////////////////////////////////

  /**
   * Normalizes the enhancement function for this view using the
   * visible grid value statistics.
   *
   * @param units the number of standard deviation units above and
   * below the mean for the data range.
   */
  public void normalize (
    double units
  ) {

    /*

    // Calculate statistics
    // --------------------
    DataLocation[] corners = getCorners();
    Statistics stats = grid.getStatistics (corners[0],
      corners[1], 0.01);

    // Check for zero valid
    // --------------------    
    if (stats.getValid() == 0) {
      throw new ArithmeticException ("Variable '" + grid.getName() +
        "' has no valid data values, cannot normalize");
    } // if 

    // Modify function
    // ---------------
    func.normalize (stats, units);

    */

  } // normalize

  ////////////////////////////////////////////////////////////

  /** Saves and returns the current settings. */
  public ColorEnhancementSettings saveSettings () {

    return (new ColorEnhancementSettings (name, getPalette(), 
      getFunction()));

  } // saveSettings

  ////////////////////////////////////////////////////////////

  /** 
   * Restores the previously saved settings. 
   *
   * @param settings the settings to restore.  Only the palette and
   * function are used -- the variable name is igored.
   * 
   */
  public void restoreSettings (
    ColorEnhancementSettings settings
  ) {

    setPalette (settings.getPalette());
    setFunction (settings.getFunction());

  } // restoreSettings

  ////////////////////////////////////////////////////////////

} // ColorPointEnhancement class

////////////////////////////////////////////////////////////////////////
