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
import java.awt.Point;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Date;

import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.PointFeatureSymbol;
import noaa.coastwatch.render.PolygonOverlay;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.DateFormatter;

/**
 * The <code>PointFeatureOverlay</code> class annotes a data view with
 * symbols using data from from a {@link PointFeatureSource}.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class PointFeatureOverlay 
  extends PolygonOverlay {

  // Constants
  // ---------
  
  /** The date format for metadata info. */
  private static final String DATE_TIME_FMT = "yyyy/MM/dd HH:mm:ss 'UTC'";

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

  @Override
  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Select data from the source
    // ---------------------------
    EarthArea viewArea = view.getArea();
    if (!viewArea.equals (source.getArea())) {
      try { source.select (viewArea); }
      catch (IOException e) {
        throw new RuntimeException (e);
      } // catch
    } // if

  } // prepare

  ////////////////////////////////////////////////////////////

  @Override
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
/*
    if (attList != null) {
      for (Integer attIndex : attList) {
      

// TODO: The drawing of attribute values needs to be implemented!



      } // for
    } // if
*/


  } // draw

  ////////////////////////////////////////////////////////////

  @Override
  public boolean hasMetadata () { return (true); }

  ////////////////////////////////////////////////////////////

  @Override
  public Map<String, Object> getMetadataAtPoint (
    Point point
  ) {
   
    Map<String, Object> metadataMap = null;
    PointFeature feature = source.getFeatureAtPoint (point);
    if (feature != null) {

      // Create metadata map
      // -------------------
      metadataMap = new LinkedHashMap<String, Object>();
      List<Attribute> attList = source.getAttributes();

      // Loop over each attribute and add value if non-null
      // --------------------------------------------------
      for (int attIndex = 0; attIndex < attList.size(); attIndex++) {
        Object attValue = feature.getAttribute (attIndex);
        if (attValue != null) {
          String attName = attList.get (attIndex).getName();
          String attUnits = attList.get (attIndex).getUnits();
          StringBuilder valueStr = new StringBuilder();
          if (attValue instanceof Date)
            valueStr.append (DateFormatter.formatDate ((Date) attValue, DATE_TIME_FMT));
          else
            valueStr.append (attValue.toString());
          if (attUnits != null)
            valueStr.append (" (" + attUnits + ")");
          metadataMap.put (attName, valueStr.toString());
        } // if
      } // for
    
    } // if

    return (metadataMap);

  } // getMetadataAtPoint

  ////////////////////////////////////////////////////////////

} // PointFeatureOverlay class

////////////////////////////////////////////////////////////////////////
