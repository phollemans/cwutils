////////////////////////////////////////////////////////////////////////
/*

     File: MultiPointFeatureOverlay.java
   Author: Peter Hollemans
     Date: 2017/02/11

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
import java.awt.Point;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.feature.SelectionRuleFilter;
import noaa.coastwatch.render.feature.FeatureGroupFilter;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.PointFeatureSymbol;
import noaa.coastwatch.render.feature.TimeWindow;
import noaa.coastwatch.util.EarthArea;

/**
 * The <code>MultiPointFeatureOverlay</code> class annotes a data view with
 * symbols using data from from multiple {@link PointFeatureOverlay} objects.
 * Global selection rule and group filters are used to filter point features 
 * for all overlays, while each overlay contains its own filter for features.  
 * In this way, a complex set of symbols can be created from a single data source.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class MultiPointFeatureOverlay<T extends PointFeatureSymbol>
  extends EarthDataOverlay {

  // Variables
  // ---------
  
  /** The global feature filter. */
  private SelectionRuleFilter globalFilter;
  
  /** The group filter. */
  private FeatureGroupFilter groupFilter;

  /** The list of overlays. */
  private List<PointFeatureOverlay<T>> overlayList;
  
  /** A hint for a TimeWindow object to use with this overlay. */
  private TimeWindow timeWindowHint;
  
  /** The group filter flag, true to use the group filter or false otherwise. */
  private boolean isGroupFilterActive = true;
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the group filter flag.
   *
   * @return the group filter flag.
   *
   * @see #setGroupFilterActive
   */
  public boolean getGroupFilterActive() { return (isGroupFilterActive); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the group filter flag.  By default the group filter is active, if
   * set to a non-null value.
   *
   * @param flag the filter flag, true to use the group filter or false to not
   * use the filter in drawing the overlay.
   */
  public void setGroupFilterActive (boolean flag) { isGroupFilterActive = flag; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the time window hint.  This hint may be used by choosers to 
   * present a reasonable default time and time window.
   *
   * @param window the time window to use for a hint.
   */
  public void setTimeWindowHint (
    TimeWindow window
  ) {
  
    this.timeWindowHint = window;
  
  } // setTimeWindowHint

  ////////////////////////////////////////////////////////////

  /**
   * Gets the time window hint.  
   *
   * @return the time window hint or null if there is no hint available.
   *
   * @see #setTimeWindowHint
   */
  public TimeWindow getTimeWindowHint() {
  
    return (timeWindowHint);
  
  } // getTimeWindowHint

  ////////////////////////////////////////////////////////////

  @Override
  public Object clone () {

    MultiPointFeatureOverlay<T> copy = (MultiPointFeatureOverlay<T>) super.clone();
    copy.globalFilter = (SelectionRuleFilter) globalFilter.clone();
    if (groupFilter != null) copy.groupFilter = (FeatureGroupFilter) groupFilter.clone();
    copy.overlayList = new ArrayList<PointFeatureOverlay<T>>();
    overlayList.forEach (overlay -> copy.overlayList.add ((PointFeatureOverlay<T>) overlay.clone()));
    return (copy);

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new overlay.  The global filter is initialized to be empty
   * for no filtering, and the overlay list to empty.  No group filter is set.
   */
  public MultiPointFeatureOverlay () {
  
    super (null);
    globalFilter = new SelectionRuleFilter();
    overlayList = new ArrayList<PointFeatureOverlay<T>>();
  
  } // MultiPointFeatureOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Sets the group filter.  The group filter is used to filter features
   * after the selection rule filter and before any individual point overlays 
   * are drawn.
   *
   * @param groupFilter the group filter or null to specify no group filtering.
   * Another way to specify no group filtering is to set a group filter, then
   * mark it as inactive using {@link #setGroupFilterActive}.
   */
  public void setGroupFilter (
    FeatureGroupFilter groupFilter
  ) {

    this.groupFilter = (groupFilter == null ? null : (FeatureGroupFilter) groupFilter.clone());
    
  } // setGroupFilter

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the group filter for the overlay.
   *
   * @return the group filter.
   *
   * @see #setGroupFilter
   */
  public FeatureGroupFilter getGroupFilter () {
  
    return (groupFilter == null ? null : (FeatureGroupFilter) groupFilter.clone());
  
  } // getGroupFilter

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the global filter for features in all overlays.
   *
   * @return the global filter.  The returned filter is not a copy, therefore
   * changes to the filter will be reflected in the overlay.
   */
  public SelectionRuleFilter getGlobalFilter () { return (globalFilter); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the list of overlays displayed.
   *
   * @return the list of overlays.
   */
  public List<PointFeatureOverlay<T>> getOverlayList () { return (overlayList); }

  ////////////////////////////////////////////////////////////

  @Override
  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    overlayList.forEach (overlay -> overlay.prepare (g, view));

  } // prepare
  
  ////////////////////////////////////////////////////////////

  /** 
   * Defines a filter that produces the intersection between the set
   * of features that satisfy the global filter and the set of features
   * that satisfy the overlay filter.  The intersection filter can be used
   * to filter a number of different overlays without inucrring the overhead
   * of re-filtering the base set of features each time.
   */
  private class IntersectionFilter extends SelectionRuleFilter {
  
    /** The result list for use by each intersection. */
    private List<Feature> preFilterResultList = null;

    /** The overlay filter for a specific overlay. */
    private SelectionRuleFilter overlayFilter;

    @Override
    public List<Feature> filter (List<Feature> features) {
      if (preFilterResultList == null) {

        // Apply global filter first
        // -------------------------
        preFilterResultList = features;
        if (globalFilter.size() != 0)
          preFilterResultList = globalFilter.filter (preFilterResultList);

        // Apply group filter second
        // -------------------------
        if (groupFilter != null && isGroupFilterActive)
          preFilterResultList = groupFilter.filter (preFilterResultList);

      } // if

      List<Feature> overlayResultList = overlayFilter.filter (preFilterResultList);
      return (overlayResultList);

    } // filter
  
    public void setOverlayFilter (SelectionRuleFilter filter) { this.overlayFilter = filter; }
  
  } // IntersectionFilter class

  ////////////////////////////////////////////////////////////

  /**
   * Gets the matching point features for the specified overlay.    
   *
   * @param overlay the overlay to get the list of matching features.  
   * These are the features that will be displayed if the overlay has 
   * its visibility turned on in this multi-point overlay.
   * @param area the earth area to use for feature matching.
   *
   * @return the list of features that will be drawn when the overlay is 
   * rendered with a view that shows the same earth area as that specified.
   *
   */
  public List<Feature> getMatchingFeatures (
    PointFeatureOverlay overlay,
    EarthArea area
  ) {

    // Create the intersection filter
    // ------------------------------
    IntersectionFilter intersectionFilter = new IntersectionFilter();
    SelectionRuleFilter overlayFilter = overlay.getFilter();
    intersectionFilter.setOverlayFilter (overlayFilter);
    overlay.setFilter (intersectionFilter);

    // Get matching features in overlay
    // --------------------------------
    List<Feature> featureList = overlay.getMatchingFeatures (area);
    overlay.setFilter (overlayFilter);

    return (featureList);

  } // getMatchingFeatures

  ////////////////////////////////////////////////////////////

  @Override
  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {
  
    // Create intersection filter
    // --------------------------
    IntersectionFilter intersectionFilter = new IntersectionFilter();

    // Draw each overlay using the intersection filter
    // -----------------------------------------------
    overlayList
      .stream()
      .filter (overlay -> overlay.getVisible())
      .forEach (overlay -> {
        SelectionRuleFilter overlayFilter = overlay.getFilter();
        intersectionFilter.setOverlayFilter (overlayFilter);
        overlay.setFilter (intersectionFilter);
        overlay.draw (g, view);
        overlay.setFilter (overlayFilter);
      });
    
  } // draw

  ////////////////////////////////////////////////////////////

  @Override
  public boolean hasMetadata () { return (true); }

  ////////////////////////////////////////////////////////////

  @Override
  public Map<String, Object> getMetadataAtPoint (
    Point point
  ) {

    Map<String, Object> metadata =
      overlayList
      .stream()
      .filter (overlay -> overlay.getVisible())
      .map (overlay -> overlay.getMetadataAtPoint (point))
      .filter (map -> map != null)
      .findFirst()
      .orElse (null);
    
    return (metadata);

  } // getMetadataAtPoint

  ////////////////////////////////////////////////////////////

  @Override
  public String toString () {

    StringBuffer buffer = new StringBuffer();
    buffer.append ("MultiPointFeatureOverlay[");
    buffer.append ("\nglobalFilter=" + globalFilter + ",");
    buffer.append ("\ngroupFilter=" + groupFilter + ",");
    buffer.append ("\noverlayList=[\n");
    for (int i = 0; i < overlayList.size(); i++) {
      buffer.append (overlayList.get(0).toString());
      if (i < overlayList.size()-1) buffer.append (",\n");
      else buffer.append ("\n");
    } // for
    buffer.append ("],\n");
    buffer.append ("timewindowHint=" + timeWindowHint);
    buffer.append ("]");

    return (buffer.toString());

  } // toString

  ////////////////////////////////////////////////////////////

} // MultiPointFeatureOverlay class

////////////////////////////////////////////////////////////////////////
