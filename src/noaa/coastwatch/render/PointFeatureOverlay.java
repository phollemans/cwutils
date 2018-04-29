////////////////////////////////////////////////////////////////////////
/*

     File: PointFeatureOverlay.java
   Author: Peter Hollemans
     Date: 2005/05/22

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
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.Point;
import java.awt.Rectangle;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Date;

import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.feature.SelectionRuleFilter;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.PointFeatureSymbol;
import noaa.coastwatch.render.PolygonOverlay;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.DateFormatter;

/**
 * The <code>PointFeatureOverlay</code> class annotes a data view with
 * symbols using data from a {@link PointFeatureSource}.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class PointFeatureOverlay<T extends PointFeatureSymbol>
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
  private T symbol;

  /** The filter for the features, or null for no filtering. */
  private SelectionRuleFilter filter;
  
  /** The rectangle to point feature map from the last rendering. */
  private transient Map<Rectangle, PointFeature> rectToFeatureMap;

  ////////////////////////////////////////////////////////////

  @Override
  public Object clone () {

    PointFeatureOverlay<T> copy = (PointFeatureOverlay<T>) super.clone();
    copy.symbol = (T) symbol.clone();
    copy.filter = (SelectionRuleFilter) filter.clone();
    return (copy);

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Gets the feature filter being used in this overlay.
   *
   * @return the feature filter or null for no filtering.
   */
  public SelectionRuleFilter getFilter () { return (filter); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the feature filter to use in this overlay.
   *
   * @param filter the feature filter or null for no filtering.
   */
  public void setFilter (SelectionRuleFilter filter) { this.filter = filter; }

  ////////////////////////////////////////////////////////////

  /** Gets the point symbol. */
  public T getSymbol () { return (symbol); }

  ////////////////////////////////////////////////////////////

  /** Sets the point symbol. */
  public void setSymbol (T symbol) { this.symbol = symbol; }

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
    T symbol
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
    T symbol,
    PointFeatureSource source
  ) { 

    super (symbol.getBorderColor());
    setFillColor (symbol.getFillColor());
    this.source = source;
    this.symbol = symbol;

  } // PointFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the feature source for this overlay.
   *
   * @return the feature source.
   */
  public PointFeatureSource getSource() { return (source); }

  ////////////////////////////////////////////////////////////

  /**
   * Selects an earth area in the source by first checking to see if
   * the currently selected earth area is the same as the desired one.
   *
   * @param area the earth area to use for features.
   */
  private void selectAreaInSource (
    EarthArea area
  ) {

    if (!area.equals (source.getArea())) {
      try { source.select (area); }
      catch (IOException e) {
        throw new RuntimeException (e);
      } // catch
    } // if

  } // selectAreaInSource

  ////////////////////////////////////////////////////////////

  /**
   * Gets the matching point features in this overlay for the specified area.
   *
   * @param area the earth area to use for features.
   *
   * @return the list of features that will be drawn when the overlay is 
   * rendered with a view that shows the same earth area as that specified.
   *
   * @since 3.3.2
   */
  public List<Feature> getMatchingFeatures (
    EarthArea area
  ) {

    List<Feature> featureList;
    synchronized (source) {
      selectAreaInSource (area);
      source.setFilter (filter);
      featureList = new ArrayList<>();
      for (Feature feature : source) featureList.add (feature);
    } // synchronized

    return (featureList);

  } // getMatchingFeatures

  ////////////////////////////////////////////////////////////

  @Override
  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Do nothing here.  We used to prepare the source with the view earth area,
    // but that doesn't allow us to render in one thread and safely call
    // getMatchingFeatures() in another thread.  For thread safety we need
    // to prepare and draw in one step.  It helps that we check the earth area
    // in the view for a match, so that in general rendering multiple times
    // doesn't require that the source has to reselect the area every time.

  } // prepare

  ////////////////////////////////////////////////////////////

  @Override
  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    synchronized (source) {

      // Prepare the source
      // ------------------
      selectAreaInSource (view.getArea());

      // Prepare symbol
      // --------------
      g.setStroke (getStroke());
      symbol.setBorderColor (getColorWithAlpha());
      symbol.setFillColor (getFillColorWithAlpha());

      // Perform rendering
      // -----------------
      source.setFilter (filter);
      rectToFeatureMap = new LinkedHashMap<Rectangle, PointFeature>();
      source.render (g, view.getTransform(), symbol, rectToFeatureMap);

    } // synchronized

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
    if (rectToFeatureMap != null) {

      // Find feature
      // ------------
      PointFeature feature =
        rectToFeatureMap.entrySet()
        .stream()
        .filter (entry -> entry.getKey().contains (point))
        .map (entry -> entry.getValue())
        .findFirst()
        .orElse (null);

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
            if (attUnits != null) {
              attUnits = attUnits.trim();
              if (!attUnits.equals (""))
                valueStr.append (" (" + attUnits + ")");
            } // if
            metadataMap.put (attName, valueStr.toString());
          } // if
        } // for
      
      } // if    
    } // if

    return (metadataMap);

  } // getMetadataAtPoint

  ////////////////////////////////////////////////////////////

  @Override
  public String toString () {

    StringBuffer buffer = new StringBuffer();
    buffer.append ("PointFeatureOverlay[");
    buffer.append ("name=" + getName() + ",");
    buffer.append ("color=" + getColor() + ",");
    buffer.append ("fillColor=" + getFillColor() + ",");
    buffer.append ("\nfilter=" + filter + ",\n");
    buffer.append ("symbol=" + symbol);
    buffer.append ("]");

    return (buffer.toString());

  } // toString

  ////////////////////////////////////////////////////////////

} // PointFeatureOverlay class

////////////////////////////////////////////////////////////////////////
