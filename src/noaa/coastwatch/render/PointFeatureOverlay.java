////////////////////////////////////////////////////////////////////////
/*
     FILE: PointFeatureOverlay.java
  PURPOSE: Annotates a data view with point data.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/22
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
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.IOException;
import java.util.List;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.PointFeatureSymbol;
import noaa.coastwatch.render.PolygonOverlay;

/**
 * The <code>PointFeatureOverlay</code> class annotes a data view with
 * symbols using data from from a {@link PointFeatureSource}.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class PointFeatureOverlay 
  extends PolygonOverlay {

  // Variables
  // ---------

  /** The feature source. */
  private PointFeatureSource source;

  /** The feature symbol. */
  private PointFeatureSymbol symbol;

  /** The list of attribute names to use as labels. */
  private List<Integer> attList;

  ////////////////////////////////////////////////////////////

  /** Sets the list of attributes to use as point data labels. */
  public void setLabelAttributes (List<Integer> attList) {

    this.attList = attList;

  } // setLabelAttributes

  ////////////////////////////////////////////////////////////

  /** Gets the point symbol. */
  public PointFeatureSymbol getSymbol () { return (symbol); }

  ////////////////////////////////////////////////////////////

  /** Sets the point symbol. */
  public void setSymbol (PointFeatureSymbol symbol) { this.symbol = symbol; }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new overlay.  The overlay drawing and fill color are
   * determined from the symbol.
   * 
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param source the source for polygon data.
   * @param symbol the symbol to use for each point feature.
   */
  public PointFeatureOverlay (
    int layer,
    Stroke stroke,
    PointFeatureSource source,
    PointFeatureSymbol symbol
  ) { 

    super (symbol.getBorderColor(), layer, stroke, symbol.getFillColor());
    this.source = source;
    this.symbol = symbol;

  } // PointFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new overlay.  The layer number is initialized to 0
   * and the stroke to the default <code>BasicStroke</code>.  The
   * overlay drawing and fill color are determined from the symbol.
   * 
   * @param source the source for polygon data.
   * @param symbol the symbol to use for each point feature.
   */
  public PointFeatureOverlay (
    PointFeatureSymbol symbol,
    PointFeatureSource source
  ) { 

    super (symbol.getBorderColor());
    setFillColor (symbol.getFillColor());
    this.source = source;
    this.symbol = symbol;

  } // PointFeatureOverlay constructor

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

    // Draw symbol
    // -----------
    g.setStroke (getStroke());
    symbol.setBorderColor (getColorWithAlpha());
    symbol.setFillColor (getFillColorWithAlpha());
    source.render (g, view.getTransform(), symbol);

    // Draw labels
    // -----------
    if (attList != null) {
      for (Integer attIndex : attList) {
      

// TODO: The drawing of attribute values needs to be implemented!



      } // for
    } // if

  } // draw

  ////////////////////////////////////////////////////////////

} // PointFeatureOverlay class

////////////////////////////////////////////////////////////////////////
