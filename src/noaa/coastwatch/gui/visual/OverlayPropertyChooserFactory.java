////////////////////////////////////////////////////////////////////////
/*
     FILE: OverlayPropertyChooserFactory.java
  PURPOSE: Creates overlay property choosers for a given overlay.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/13
  CHANGES: 2006/11/02, PFH, added expression mask chooser
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import noaa.coastwatch.gui.visual.ExpressionMaskOverlayPropertyChooser;
import noaa.coastwatch.gui.visual.GenericOverlayPropertyChooser;
import noaa.coastwatch.gui.visual.MultilayerBitmaskOverlayPropertyChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooser;
import noaa.coastwatch.gui.visual.PointFeatureOverlayPropertyChooser;
import noaa.coastwatch.gui.visual.MultiPointFeatureOverlayPropertyChooser;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.ExpressionMaskOverlay;
import noaa.coastwatch.render.MultilayerBitmaskOverlay;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.MultiPointFeatureOverlay;
import noaa.coastwatch.render.SimpleSymbol;

/**
 * The <code>OverlayPropertyChooserFactory</code> class may be used to
 * create appropriate <code>OverlayPropertyChooser</code> objects for
 * a given overlay.
 *
 * @see noaa.coastwatch.render.EarthDataOverlay
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class OverlayPropertyChooserFactory {

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new property chooser panel for the specified overlay.
   *
   * @param overlay the overlay to use.
   *
   * @return the newly created chooser panel.
   * 
   * @throws IllegalArgumentException if the overlay is not supported 
   * by any known property chooser panel.
   */
  public static OverlayPropertyChooser create (
    EarthDataOverlay overlay
  ) {

    OverlayPropertyChooser chooser = null;

    // Create multilayer chooser
    // -------------------------
    if (overlay instanceof MultilayerBitmaskOverlay) {
      chooser = new MultilayerBitmaskOverlayPropertyChooser (
        (MultilayerBitmaskOverlay) overlay);
    } // if
    
    // Create multilayer chooser
    // -------------------------
    else if (overlay instanceof ExpressionMaskOverlay) {
      chooser = new ExpressionMaskOverlayPropertyChooser (
        (ExpressionMaskOverlay) overlay);
    } // else if
    
    // Create point data overlay chooser
    // ---------------------------------
    else if (overlay instanceof PointFeatureOverlay) {
      chooser = new PointFeatureOverlayPropertyChooser (
        (PointFeatureOverlay<SimpleSymbol>) overlay);
    } // else if
    
    // Create multi-point data overlay chooser
    // ---------------------------------------
    else if (overlay instanceof MultiPointFeatureOverlay) {
      chooser = new MultiPointFeatureOverlayPropertyChooser (
        (MultiPointFeatureOverlay<SimpleSymbol>) overlay);
    } // else if

    // Create generic chooser
    // ----------------------
    else {
      try { chooser = new GenericOverlayPropertyChooser (overlay); }
      catch (Exception e) { e.printStackTrace(); }
    } // try

    // Check for unsupported overlay
    // -----------------------------
    if (chooser == null) {
      throw new IllegalArgumentException ("Unsupported overlay class " +
        overlay.getClass());
    } // if

    return (chooser);

  } // create

  ////////////////////////////////////////////////////////////

  private OverlayPropertyChooserFactory () {}

  ////////////////////////////////////////////////////////////

} // OverlayPropertyChooserFactory

////////////////////////////////////////////////////////////////////////
