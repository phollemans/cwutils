////////////////////////////////////////////////////////////////////////
/*
     FILE: MultiPointFeatureOverlay.java
  PURPOSE: Annotates a data view with layers of point data.
   AUTHOR: Peter Hollemans
     DATE: 2017/02/11
  CHANGES: n/a
 
  CoastWatch Software Library and Utilities
  Copyright 2017, USDOC/NOAA/NESDIS CoastWatch

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
   */
  public void setGroupFilter (
    FeatureGroupFilter groupFilter
  ) {

    this.groupFilter = (groupFilter == null ? null : (FeatureGroupFilter) groupFilter.clone());
    
  } // setGroupFilter

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the group filter for the overlay
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
   * @return the global filter.
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
   * that satisfy the overlay filter.
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
        if (groupFilter != null)
          preFilterResultList = groupFilter.filter (preFilterResultList);

      } // if
      List<Feature> overlayResultList = overlayFilter.filter (preFilterResultList);

      return (overlayResultList);

    } // filter
  
    public void setOverlayFilter (SelectionRuleFilter filter) { this.overlayFilter = filter; }
  
  } // IntersectionFilter class

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

} // MultiPointFeatureOverlay class

////////////////////////////////////////////////////////////////////////
