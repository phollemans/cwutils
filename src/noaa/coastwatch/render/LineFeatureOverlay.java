////////////////////////////////////////////////////////////////////////
/*
     FILE: LineFeatureOverlay.java
  PURPOSE: An overlay for vector polyline data.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/06
  CHANGES: 2002/11/29, PFH, added prepare, draw
           2003/12/10, PFH, changed LineFeatureReader to LineFeatureSource
           2005/03/21, PFH, added transparency handling
           2006/01/16, PFH, added check for null color

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
import java.util.*;
import java.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.*;

/**
 * The <code>LineFeatureOverlay</code> class annotates a data view
 * with line features from a line feature source.
 */
public class LineFeatureOverlay 
  extends LineOverlay {

  // Variables
  // ---------
  /** The Earth vector source. */
  private LineFeatureSource source;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new Earth vector overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param source the source for vector data.
   */
  public LineFeatureOverlay (
    Color color,
    int layer,
    Stroke stroke,
    LineFeatureSource source
  ) { 

    super (color, layer, stroke);
    this.source = source;

  } // LineFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new Earth vector overlay.  The layer number is
   * initialized to 0 and the stroke to the default
   * <code>BasicStroke</code>.
   * 
   * @param color the overlay color.
   * @param source the source for vector data.
   */
  public LineFeatureOverlay (
    Color color,
    LineFeatureSource source
  ) { 

    super (color);
    this.source = source;

  } // LineFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Select data from the source
    // ---------------------------
    try { source.select (view.getArea()); }
    catch (IOException e) { return; }

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check for null color
    // --------------------
    if (getColor() == null) return;

    // Initialize properties
    // ---------------------
    g.setColor (getColorWithAlpha());
    g.setStroke (getStroke());

    // Render the data
    // ---------------
    source.render (g, view.getTransform());

  } // draw

  ////////////////////////////////////////////////////////////

} // LineFeatureOverlay class

////////////////////////////////////////////////////////////////////////
