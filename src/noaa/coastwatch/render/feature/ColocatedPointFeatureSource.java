////////////////////////////////////////////////////////////////////////
/*
     FILE: ColocatedPointFeatureSource.java
  PURPOSE: Wraps a source along with grids.
   AUTHOR: Peter Hollemans
     DATE: 2017/02/20
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2017, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.feature.SelectionRuleFilter;
import noaa.coastwatch.render.feature.Attribute;

// Testing
import noaa.coastwatch.test.TestLogger;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.trans.ProjectionConstants;
import noaa.coastwatch.util.trans.SpheroidConstants;
import java.util.Date;
import noaa.coastwatch.render.feature.SelectionRuleFilter.FilterMode;

/**
 * The <code>ColocatedPointFeatureSource</code> class combines the point 
 * features from a source with colocated values from a set of grids.  For
 * example if each point feature in the source has attributes named:
 * <ul>
 *   <li>observation_time</li>
 *   <li>windspeed</li>
 * </ul>
 * and the grids were named "sst" and "cloud", then the point data supplied
 * by this class would have attributes named:
 * <ul>
 *   <li>observation_time</li>
 *   <li>windspeed</li>
 *   <li>grid::sst</li>
 *   <li>grid::cloud</li>
 * </ul>
 * where the sst and cloud data values are taken from the grids at the geolocation
 * of the point data.  The "grid::" extension to the grid names prevents name 
 * collisions, if the attributes and grids happen to have some of the same names.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class ColocatedPointFeatureSource
  extends PointFeatureSource {

  // Variables
  // ---------

  /** The source of point feature data. */
  private PointFeatureSource source;

  /** The list of grids to use for data colocation. */
  private List<Grid> gridList;

  /** The list of grid types. */
  private List<Class> gridTypeList;

  /** The dimensions of the grids. */
  private int[] gridDims;

  /** The the earth transform for converting between geolocation and grid coordinates. */
  private EarthTransform trans;

  /** The number of attributes in the source point feature (before extension). */
  private int sourceAttCount;

  /** The mapping form primitive to wrapper class. */
  public final static Map<Class<?>, Class<?>> primitiveToWrapperMap;

  ////////////////////////////////////////////////////////////

  static {

    primitiveToWrapperMap = new HashMap<Class<?>, Class<?>>();
    primitiveToWrapperMap.put (Boolean.TYPE, Boolean.class);
    primitiveToWrapperMap.put (Byte.TYPE, Byte.class);
    primitiveToWrapperMap.put (Short.TYPE, Short.class);
    primitiveToWrapperMap.put (Character.TYPE, Character.class);
    primitiveToWrapperMap.put (Integer.TYPE, Integer.class);
    primitiveToWrapperMap.put (Long.TYPE, Long.class);
    primitiveToWrapperMap.put (Float.TYPE, Float.class);
    primitiveToWrapperMap.put (Double.TYPE, Double.class);

  } // static
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the data type that can hold a single value for the data in the
   * specified grid, once any scaling has been applied.
   *
   * @param grid the grid to get the unpacked type for.
   *
   * @return the unpacked type as a primitive class.
   */
  private Class getUnpackedType (
    Grid grid
  ) {

    Class type;
    
    if (grid.getScaling() != null) {
      type = Double.class;
    } // if
    else {
      Class primitiveType = grid.getDataClass();
      type = primitiveToWrapperMap.get (primitiveType);
      if (type == null) throw new RuntimeException ("Unknown primitive type: " + primitiveType);
      if (grid.getUnsigned()) {
        if (type.equals (Byte.class)) type = Short.class;
        else if (type.equals (Short.class)) type = Integer.class;
        else if (type.equals (Integer.class)) type = Long.class;
      } // if
    } // else

    return (type);

  } // getUnpackedType

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new colocated source using the specified source and grids.
   * 
   * @param source the point source to supply point feature data.
   * @param trans the earth transform for the grid data.
   * @param gridList the list of grids to append data values to the point 
   * feature attributes.
   */
  public ColocatedPointFeatureSource (
    PointFeatureSource source,
    EarthTransform trans,
    List<Grid> gridList
  ) {
  
    this.source = source;
    this.gridList = gridList;
    gridDims = gridList.get(0).getDimensions();
    this.trans = trans;
    
    // Create expanded attribute data
    // ------------------------------
    List<Attribute> attList = source.getAttributes();
    sourceAttCount = attList.size();
    gridTypeList = new ArrayList<Class>();
    for (Grid grid : gridList) {
      Class type = getUnpackedType (grid);
      gridTypeList.add (type);
      Attribute att = new Attribute ("grid::" + grid.getName(), type, grid.getUnits());
      attList.add (att);
    } // for
    setAttributes (attList);

  } // ColocatedPointFeatureSource constructor
  
  ////////////////////////////////////////////////////////////

  @Override
  protected void select () throws IOException {
  
    // Create colocated list
    // ---------------------
    source.select (area);
    featureList.clear();
    for (Feature feature : source) {
      featureList.add (new ColocatedPointFeature ((PointFeature) feature));
    } // for
    
  } // select
    
  ////////////////////////////////////////////////////////////

  /**
   * A colocated feature holds onto a point feature from the wrapped source
   * and allows access to its attributes, while extending the attribute access
   * to include the grid data.
   */
  private class ColocatedPointFeature extends PointFeature {
  
    // Variables
    // ---------
    
    /** The point feature to use for the initial set of attributes. */
    private PointFeature sourceFeature;
    
    ////////////////////////////////////////////////////
    
    @Override
    public Object getAttribute (int index) {

      Object dataValue = null;

      // Get data from source feature
      // ----------------------------
      if (index < sourceAttCount) {
        dataValue = sourceFeature.getAttribute (index);
      } // if
      
      // Alternatively, get data from a grid
      // -----------------------------------
      else {
        int gridIndex = index - sourceAttCount;
        DataLocation dataLoc = trans.transform (sourceFeature.getPoint());
        if (dataLoc.isValid() && dataLoc.isContained (gridDims)) {
          Grid grid = gridList.get (gridIndex);
          double dblValue = grid.getValue (dataLoc);
          if (!Double.isNaN (dblValue)) {
            Class type = gridTypeList.get (gridIndex);
            if (type.equals (Double.class))
              dataValue = dblValue;
            else if (type.equals (Float.class))
              dataValue = (float) dblValue;
            else if (type.equals (Long.class))
              dataValue = (long) dblValue;
            else if (type.equals (Integer.class))
              dataValue = (int) dblValue;
            else if (type.equals (Short.class))
              dataValue = (short) dblValue;
            else if (type.equals (Byte.class))
              dataValue = (byte) dblValue;
            else throw new RuntimeException ("Unsupported attribute type: " + type);
          } // if
        } // if
      } // else

      return (dataValue);

    } // getAttribute
  
    ////////////////////////////////////////////////////

    /**
     * Creates a new colocated object with the specified feature.
     *
     * @param sourceFeature the source point to extend.
     */
    public ColocatedPointFeature (
      PointFeature sourceFeature
    ) {
    
      super (null);
      this.sourceFeature = sourceFeature;

    } // ColocatedPointFeature constructor

    ////////////////////////////////////////////////////

    @Override
    public EarthLocation getPoint () { return (sourceFeature.getPoint()); }

    ////////////////////////////////////////////////////

    @Override
    public void setPoint (EarthLocation point) { throw new UnsupportedOperationException(); }

    ////////////////////////////////////////////////////
  
    @Override
    public String toString () {
    
      List<Object> attValues = new ArrayList<Object>();
      int attCount = getAttributeCount();
      for (int i = 0; i < attCount; i++) attValues.add (getAttribute (i));
      return ("ColocatedPointFeature[point=" + getPoint() + ",attributes=" + attValues + "]");
    
    } // toString

    ////////////////////////////////////////////////////
    
  } // ColocatedPointFeature

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (ColocatedPointFeatureSource.class);

    logger.test ("Framework");

    // Create point source
    // -------------------
    final List<Attribute> attList = new ArrayList<Attribute>();
    attList.add (new Attribute ("platform_type", Byte.class, ""));
    attList.add (new Attribute ("platform_id", String.class, ""));
    attList.add (new Attribute ("sst", Double.class, ""));
    attList.add (new Attribute ("quality_level", Byte.class, ""));
    attList.add (new Attribute ("time", Date.class, ""));

    final List<Feature> pointFeatureList = new ArrayList<Feature>();
    
    pointFeatureList.add (new PointFeature (
      new EarthLocation (46, -125),
      new Object[] {101, "FROBOZ", 10.5, 5, new Date()}
    ));
    pointFeatureList.add (new PointFeature (
      new EarthLocation (47, -125),
      new Object[] {101, "FROBOZ", 10.6, 5, new Date()}
    ));
    pointFeatureList.add (new PointFeature (
      new EarthLocation (48, -125),
      new Object[] {101, "FROBOZ", 10.7, 5, new Date()}
    ));
    pointFeatureList.add (new PointFeature (
      new EarthLocation (49, 125),
      new Object[] {101, "FROBOZ", 10.8, 5, new Date()}
    ));

    PointFeatureSource source = new PointFeatureSource () {
      {
        setAttributes (attList);
      }
      protected void select () throws IOException {
        this.featureList.clear();
        this.featureList.addAll (pointFeatureList);
      }
    };

    // Create transform
    // ----------------
    EarthTransform trans = MapProjectionFactory.getInstance().create (
      ProjectionConstants.MERCAT,
      0,
      new double[15],
      SpheroidConstants.WGS84,
      new int[] {512, 512},
      new EarthLocation (48, -125),
      new double[] {2000, 2000}
    );
    
    // Create variable
    // ---------------
    short[] data = new short[512*512];
    for (int i = 0; i < data.length; i++) data[i] = (short) i;
    Grid var = new Grid (
      "avhrr_ch4",
      "AVHRR channel 4",
      "degrees_Celsius",
      512, 512,
      data,
      new java.text.DecimalFormat ("0"),
      new double[] {0.01, 0},
      Short.MIN_VALUE
    );
    List<Grid> gridList = new ArrayList<>();
    gridList.add (var);

    List<Double> varValues = new ArrayList<>();
    for (Feature feature : pointFeatureList)
      varValues.add (var.getValue (trans.transform (((PointFeature) feature).getPoint())));

    logger.passed();

    // Start testing
    // -------------
    
    logger.test ("constructor");
    ColocatedPointFeatureSource colocatedSource =
      new ColocatedPointFeatureSource (source, trans, gridList);
    List<Attribute> colocatedAttList = colocatedSource.getAttributes();
    assert (colocatedAttList.size() == 6);
    assert (colocatedAttList.get (0).getName().equals ("platform_type"));
    assert (colocatedAttList.get (1).getName().equals ("platform_id"));
    assert (colocatedAttList.get (2).getName().equals ("sst"));
    assert (colocatedAttList.get (3).getName().equals ("quality_level"));
    assert (colocatedAttList.get (4).getName().equals ("time"));
    assert (colocatedAttList.get (5).getName().equals ("grid::avhrr_ch4"));
    logger.passed();

    logger.test ("select, getArea");
    EarthArea area = new EarthArea();
    area.addAll();
    colocatedSource.select (area);
    assert (colocatedSource.getArea().equals (area));
    int n = 0;
    for (Feature feature : colocatedSource) {
      assert (feature.getAttribute (0).equals (pointFeatureList.get (n).getAttribute (0)));
      assert (feature.getAttribute (1).equals (pointFeatureList.get (n).getAttribute (1)));
      assert (feature.getAttribute (2).equals (pointFeatureList.get (n).getAttribute (2)));
      assert (feature.getAttribute (3).equals (pointFeatureList.get (n).getAttribute (3)));
      assert (feature.getAttribute (4).equals (pointFeatureList.get (n).getAttribute (4)));
      if (n == 3) assert (feature.getAttribute (5) == null);
      else assert (feature.getAttribute (5).equals (varValues.get (n)));
      n++;
    } // for
    logger.passed();
    
    logger.test ("setFilter, getFilter");
    SelectionRuleFilter filter = new SelectionRuleFilter();
    filter.setMode (FilterMode.MATCHES_ALL);
    Map<String, Integer> attNameMap = colocatedSource.getAttributeNameMap();

    NumberRule sstRule = new NumberRule ("sst", attNameMap, 10.55);
    sstRule.setOperator (NumberRule.Operator.IS_GREATER_THAN);
    filter.add (sstRule);
    
    NumberRule gridRule = new NumberRule ("grid::avhrr_ch4", attNameMap, varValues.get (2));
    gridRule.setOperator (NumberRule.Operator.IS_NOT_EQUAL_TO );
    filter.add (gridRule);
  
    colocatedSource.setFilter (filter);
    assert (filter == colocatedSource.getFilter());
    colocatedSource.select (area);
    List<Feature> selected = new ArrayList<Feature>();
    for (Feature feature : colocatedSource) selected.add (feature);
    assert (selected.size() == 1);
    logger.passed();








  } // main

  ////////////////////////////////////////////////////////////

} // ColocatedPointFeatureSource class

////////////////////////////////////////////////////////////////////////
