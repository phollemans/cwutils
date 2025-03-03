////////////////////////////////////////////////////////////////////////
/*

     File: CommonDataModelNCReader.java
   Author: Peter Hollemans
     Date: 2005/07/04

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
package noaa.coastwatch.io;

// Imports
// -------
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import noaa.coastwatch.io.NCReader;
import noaa.coastwatch.io.tile.TileSource;
import noaa.coastwatch.io.tile.TilingScheme;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.io.tile.NCTileSource;
import noaa.coastwatch.io.tile.TileCachedGrid;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.Line;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.CDMGridMappedProjection;
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.GeoVectorProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.SwathProjection;
import noaa.coastwatch.util.trans.EllipsoidPerspectiveProjection;

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.GridChunkProducer;
import noaa.coastwatch.util.chunk.DataChunkFactory;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.dataset.CoordinateAxisTimeHelper;
import ucar.nc2.time.Calendar;
import ucar.nc2.dataset.CoordinateAxis1DTime;

import java.util.logging.Logger;
import java.util.logging.Level;

/** 
 * The <code>CommonDataModelNCReader</code> class reads Java NetCDF API
 * accessible datasets and attempts to use any metadata and
 * geographic referencing information that the NetCDF layer
 * provides.  If a data variable is found that has two geographic
 * axes but also time and/or level axes, the variable data is
 * expanded to a series of 2D variables by extending the variable
 * name for each non-geographic axis.  Datasets must:
 * <ul>
 *   <li>have the same earth transform for all grids, and</li>
 *   <li>use either a geographic map projection with equally spaced lat/lon
 *   intervals, or a swath-style map projection with lat/lon data variables
 *   provided (since 3.3.1).</li>
 * </ul>
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class CommonDataModelNCReader 
  extends NCReader {

  private static final Logger LOGGER = Logger.getLogger (CommonDataModelNCReader.class.getName());

  // Constants
  // ---------

  /** The array section start specifier. */
  private static final String START = "__";

  /** The array section end specifier. */
  private static final String END = "";

  /** The array section expansion specifier. */
  private static final String EXPAND = "x";

  /** The array section dimension delimiter. */
  private static final String DELIMIT = "_";

  // Variables
  // ---------
  
  /** The list of variable groups to access. */
  private List<VariableGroup> groupList;

  /** The map of variable name to group. */
  private Map<String,VariableGroup> groupMap;

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>VariableGroup</code> class holds information from
   * one grid set in the file.
   */
  private static class VariableGroup {

    /** The grid set to access in the file. */
    public GridDataset.Gridset gridset;
    
    /** The number of extra dimensions, beyond geographical x/y dimensions. */
    public int extraDims;
    
    /** The variable dimensions for each grid. */
    public int[] varDims;
    
    /** 
     * The expanded dimensions array, -1 for spatial axis, >=0 for
     * expanded axis. 
     */
    public int[] expandDims;
    
    /** 
     * The variable name extension for all variables, or null if a single
     * extension cannot be used for all variables.  This would be the case
     * if one the expandeded dimensions has count value > 1.
     */
    public String varExtension;

  } // VariableGroup class

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { 

    return ("Java-NetCDF interface (" + dataset.getFileTypeId() + " " +
      dataset.getConventionUsed() + ")");

  } // getDataFormat

  ////////////////////////////////////////////////////////////
  
  /** Gets the earth data info object. */
  private EarthDataInfo getGlobalInfo () throws IOException { 

    // Detect time periods from time axis
    // ----------------------------------
    Set<Date> dateSet = new TreeSet<Date>();
    for (VariableGroup group : groupList) {
      GridCoordSystem coordSystem = group.gridset.getGeoCoordSystem();
      CalendarDateRange dateRange = coordSystem.getCalendarDateRange();

      // Use time axis directly
      // ----------------------
      if (dateRange == null) {
        if (coordSystem.hasTimeAxis()) {
          CoordinateAxis axis = coordSystem.getTimeAxis();
          if (axis.isScalar()) {
            double offset = axis.readScalarDouble();
            CoordinateAxisTimeHelper helper = new CoordinateAxisTimeHelper (Calendar.getDefault(), axis.getUnitsString());
            CalendarDate date = helper.makeCalendarDateFromOffset (offset);
            dateSet.add (new Date (date.getMillis()));


// There's an issue with detecting the time axis data the way it is.  Even
// if the time axis entries have bounds, the total extent of the time axis
// is not detected by just using getCalendarDateRange().  It only returns
// the values of the extreme time axis entries, not accounting for the bounds.
// Actually detecting the bounds is only accomplished by going into the time
// axis and requesting the bounds explicitly using getCoordBoundsDate().  But
// if there are no bounds, this returns some very odd values, namely the epoch
// start and end times.  Q: Is there a method that detects if time bounds have
// been specified?  The toolsUI seems to know when an interval is specified,
// so there must be a method.


//LOGGER.fine ("Set date from time axis");


          } // if
        } // if
      } // if
      
      // Use detected date range
      // -----------------------
      else {
        dateSet.add (new Date (dateRange.getStart().getMillis()));
        dateSet.add (new Date (dateRange.getEnd().getMillis()));




//LOGGER.fine ("Set date from date range");
//dateSet.forEach (val -> { LOGGER.fine ("value = " + val); });
//
//LOGGER.fine ("duration = " + dateRange.getDurationInSecs());
//var timeAxis = coordSystem.getTimeAxis();
//LOGGER.fine ("time axis = " + timeAxis);
//if (timeAxis instanceof CoordinateAxis1DTime) {
//  var time1DAxis = (CoordinateAxis1DTime) timeAxis;
//  for (int i = 0; i < time1DAxis.getSize(); i++) {
//    var bounds = time1DAxis.getCoordBoundsDate (i);
//    LOGGER.fine ("start bound = " + bounds[0]);
//    LOGGER.fine ("end bound = " + bounds[1]);
//  } // for
//
//  LOGGER.fine ("date range = " + time1DAxis.getCalendarDateRange());
//
//
//} // if


      } // else

    } // for

    // Test for time axis success
    // --------------------------
    boolean foundTimeAxis = (dateSet.size() != 0);

    // Find the data origin and source
    // -------------------------------
    String origin = null;
    String source = null;
    String platform = null;
    String sensor = null;
    String time_start = null;
    String time_end = null;
    for (Attribute att : (List<Attribute>) dataset.getGlobalAttributes()) {
      if (!att.isString()) continue;
      String name = att.getShortName().toLowerCase();
      if (origin == null) {
        if (name.indexOf ("institution") != -1 ||
            name.indexOf ("origin") != -1) {
          origin = att.getStringValue();
        } // if
      } // if
      if (source == null) {
        if (name.indexOf ("source") != -1) {
          source = att.getStringValue();
        } // if
      } // if
      if (platform == null) {
        if (name.equals ("platform") || name.equals ("platform_id")) {
          platform = att.getStringValue();
        } // if
      } // if
      if (sensor == null) {
        if (name.equals ("sensor") || name.equals ("instrument_id")) {
          sensor = att.getStringValue();
        } // if
      } // if
      if (time_start == null) {
        if (name.equals ("time_coverage_start")) {
          time_start = att.getStringValue();
        } // if
      } // if
      if (time_end == null) {
        if (name.equals ("time_coverage_end")) {
          time_end = att.getStringValue();
        } // if
      } // if
    } // for
    if (origin == null) origin = "Unknown";
    if (source == null) source = "Unknown";

    // Detect time periods from global attributes
    // ------------------------------------------
    if (!foundTimeAxis && (time_start != null && time_end != null)) {
      String calName = Calendar.getDefault().toString();
      try {
        CalendarDate start = CalendarDate.parseISOformat (calName, time_start);
        dateSet.add (new Date (start.getMillis()));
        CalendarDate end = CalendarDate.parseISOformat (calName, time_end);
        dateSet.add (new Date (end.getMillis()));
      } // try
      catch (Exception e) {
        LOGGER.log (Level.WARNING, "Failed to parse time metadata", e);
      } // catch
    } // if
    if (dateSet.size() == 0) dateSet.add (new Date(0));
    List<TimePeriod> periodList = new ArrayList<TimePeriod>();
    for (Date date : dateSet) periodList.add (new TimePeriod (date, 0));

    // Create satellite info
    // ---------------------
    EarthDataInfo info;
    EarthTransform trans = getTransform();
    if (platform != null && sensor != null) {
      info = new SatelliteDataInfo (platform, sensor, periodList, trans, origin, "");
    } // if

    // Create generic info
    // -------------------
    else {
      info = new EarthDataInfo (source, periodList, trans, origin, "");
    } // else
    
    return (info);

  } // getGlobalInfo

  ////////////////////////////////////////////////////////////

  /** Gets the earth transform information for the dataset. */
  private EarthTransform getTransform () throws IOException {

    // Get list of transforms
    // ----------------------
    List<EarthTransform> transList = new ArrayList<EarthTransform>();
    for (VariableGroup group : groupList) {
      EarthTransform trans = getTransform (group.gridset);
      transList.add (trans);
      
      // Log the list of grid names and the transform for each one here.
      // This is useful for debugging issues with the EarthTransform objects
      // not all matching.
      
      if (LOGGER.isLoggable (Level.FINE)) {
        List<GridDatatype> grids = group.gridset.getGrids();
        StringBuffer names = new StringBuffer();
        for (GridDatatype grid : grids)
          names.append ((names.length() != 0 ? "," : "") + grid.getName());
        LOGGER.fine ("Got transform = " + trans + " for grid names = " + names);
      } // if

    } // for

    // Check that all transforms are equal
    // -----------------------------------
    EarthTransform firstTrans = null;
    for (EarthTransform trans : transList) {
      if (firstTrans == null)
        firstTrans = trans;
      else if (!firstTrans.equals (trans)) {
        throw new IOException ("Earth transforms are not equal in all grids");
      } // else if
    } // for

    return (firstTrans);

  } // getTransform

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the earth transform information for the specified grid
   * set. 
   */
  private EarthTransform getTransform (
    GridDataset.Gridset gridset
  ) throws IOException {

/*

FIXME

There are several types of transforms:

- swath: earth locations given for each pixel in a pattern irregular enough
  to have no simple (lat,lon) --> (i,j) translation (often from a sensor)
 
- mapped: earth locations that can be translated to/from (i,j) using a map 
  projection calculation in both directions
  
- vector: earth locations that fall along 1D vectors of latitude and 
  longitude, not necessarily regularly spaced
  
- sensor: earth locations that have a complex relationship to their (i,j)
  given by a calculation that models the sensor and earth geometry

The issue is that the instanceof operator is used to determine which class
of transform is being used, and then act accordingly.  This could indicate an
issue with class design.  Transform class instanceof calls are used in the 
utilities when:

- writing ArcInfo compatible HDR and world files, to write the map projection
  coordinates of the corner points, and the resolution
  
- displaying general file information in GUI, command line, or image sidebar, 
  to print the map projection specifications and system

- writing NetCDF, HDF, or GeoTIFF metadata, to decide which metadata to write

- creating a subsampling of a map projection when viewing a GUI preview of a
  grid

- truncating top-left and bottom-right in swath to trace bounding box for
  GUI and plotting
  
- detection of swath in order to decide if coastlines should be plotted on 
  a preview image (ie: might be very slow rendering, or no (lat,lon) -> (i,j)
  translation available

*/

    EarthTransform trans = null;

    // Get detected coordinate system
    // ------------------------------
    GridCoordSystem coordSystem = gridset.getGeoCoordSystem();




    
    // Here we have a very nasty hack in order to get the transform information
    // correct for Himawari and GOES data.  The issue is that we cannot get the 
    // CDM class for geostationary data to produce the correct lat/lon 
    // transformation.  So we insert our own detection of Himawari and GOES 
    // geostationary parameters in the global metadata, and create an 
    // EllipsoidPerspectiveProjection in response. This is very similar 
    // to the code in the now deprecated ACSPONCReader.

    // First test to see if we even have an ACSPO file.  
    boolean isAcspo = false;
    try {
      var acspoVersion = (String) getAttribute ("ACSPO_VERSION");
      if (acspoVersion != null) {
        isAcspo = true;
        LOGGER.fine ("Found ACSPO data version " + acspoVersion);
      } // if
    } // try
    catch (Exception e) { }

    // If we have an ACSPO file, try reading various metadata versions of
    // geostationary attributes.
    if (isAcspo) {

      // One of the versions is 2.41b02 which had a series of lower case 
      // attribute names as follows:
   
      // :sub_lon = 140.7 ;
      // :dist_virt_sat = 42164. ;
      // :earth_radius_equator = 6378.137 ;
      // :earth_radius_polar = 6356.7523 ;
      // :cfac = 20466275 ;
      // :lfac = 20466275 ;
      // :coff = 2750.5f ;
      // :loff = 2750.5f ;

      try {

        LOGGER.fine ("Attempting to read ACSPO 2.41b02 geostationary style attributes");

        double subpointLon = ((Number) getAttribute ("sub_lon")).doubleValue();
        double satDist = ((Number) getAttribute ("dist_virt_sat")).doubleValue();
        int columnFactor = ((Number) getAttribute ("cfac")).intValue();
        int lineFactor = ((Number) getAttribute ("lfac")).intValue();
        double eqRadius = ((Number) getAttribute ("earth_radius_equator")).doubleValue();
        double polarRadius = ((Number) getAttribute ("earth_radius_polar")).doubleValue();
        double columnOffset = ((Number) getAttribute ("coff")).doubleValue();
        double lineOffset = ((Number) getAttribute ("loff")).doubleValue();
        CoordinateAxis xHorizAxis = coordSystem.getXHorizAxis();
        int[] dims = xHorizAxis.getShape();
        trans = new EllipsoidPerspectiveProjection (
          new double[] {
            0,
            subpointLon,
            satDist,
            Math.toRadians (65536.0/lineFactor),
            Math.toRadians (65536.0/columnFactor)
          },
          dims
        );
        return (trans);
      } // try
      catch (Exception e) { 
        LOGGER.log (Level.FINE, "Failed to read ACSPO 2.41b02 geostationary attributes", e);
      } // catch

      // One of the versions is 2.41b03 which had a series of mixed and upper 
      // case attribute names as follows:
 
      // :Sub_Lon = 140.7 ;
      // :Dist_Virt_Sat = 42164. ;
      // :Earth_Radius_Equator = 6378.137 ;
      // :Earth_Radius_Polar = 6356.7523 ;
      // :CFAC = 20466275 ;
      // :LFAC = 20466275 ;
      // :COFF = 2750.5f ;
      // :LOFF = 2750.5f ;
      // :sensor = "ABI" ;

      // Note that this version also includes support for both ABI and 
      // Himawari sensors which scan slightly differently.  We also allow 
      // for a different way of specifying the x/y coordinate axes sizes.
 
      try {

        LOGGER.fine ("Attempting to read ACSPO 2.41b03 geostationary style attributes");
        
        double subpointLon = ((Number) getAttribute ("Sub_Lon")).doubleValue();
        double satDist = ((Number) getAttribute ("Dist_Virt_Sat")).doubleValue();
        int columnFactor = ((Number) getAttribute ("CFAC")).intValue();
        int lineFactor = ((Number) getAttribute ("LFAC")).intValue();
        double eqRadius = ((Number) getAttribute ("Earth_Radius_Equator")).doubleValue();
        double polarRadius = ((Number) getAttribute ("Earth_Radius_Polar")).doubleValue();
        double columnOffset = ((Number) getAttribute ("COFF")).doubleValue();
        double lineOffset = ((Number) getAttribute ("LOFF")).doubleValue();
        String sensor = (String) getAttribute ("sensor");
        double isVertical = (sensor.equals ("ABI") ? 1 : 0);

        CoordinateAxis xHorizAxis = coordSystem.getXHorizAxis();
        int[] dims = xHorizAxis.getShape();

        if (dims.length == 1) {
          int[] xDims = dims;
          CoordinateAxis yHorizAxis = coordSystem.getYHorizAxis();
          int[] yDims = yHorizAxis.getShape();
          dims = new int[] {yDims[0], xDims[0]};
        } // if
        
        double rowStep = Math.toRadians (65536.0/lineFactor);
        double colStep = Math.toRadians (65536.0/columnFactor);
        trans = new EllipsoidPerspectiveProjection (
          new double[] {
            0,
            subpointLon,
            satDist,
            rowStep,
            colStep,
            isVertical
          },
          dims
        );
        
        return (trans);

      } // try
      catch (Exception e) {    
        LOGGER.log (Level.FINE, "Failed to read ACSPO 2.41b03 geostationary attributes", e);
      } // catch
          
      // Another versions is 2.90 which had a series of slightly different 
      // mixed and upper case attribute names as follows:
 
      // :Sub_Lon = 140.7 ;
      // :Dist_Virt_Sat = 42164. ;
      // :Earth_Radius_Equator = 6378.137 ;
      // :Earth_Radius_Polar = 6356.7523 ;
      // :CFAC = 20466275 ;
      // :LFAC = 20466275 ;
      // :COFF = 2750.5f ;
      // :LOFF = 2750.5f ;
      // :SENSOR = "ABI" ;

      // Note that this version also includes support for both ABI and 
      // Himawari sensors which scan slightly differently.  We also allow 
      // for a different way of specifying the x/y coordinate axes sizes.
 
      try {

        LOGGER.fine ("Attempting to read ACSPO 2.90 geostationary style attributes");
        
        double subpointLon = ((Number) getAttribute ("Sub_Lon")).doubleValue();
        double satDist = ((Number) getAttribute ("Dist_Virt_Sat")).doubleValue();
        int columnFactor = ((Number) getAttribute ("CFAC")).intValue();
        int lineFactor = ((Number) getAttribute ("LFAC")).intValue();
        double eqRadius = ((Number) getAttribute ("Earth_Radius_Equator")).doubleValue();
        double polarRadius = ((Number) getAttribute ("Earth_Radius_Polar")).doubleValue();
        double columnOffset = ((Number) getAttribute ("COFF")).doubleValue();
        double lineOffset = ((Number) getAttribute ("LOFF")).doubleValue();
        String sensor = (String) getAttribute ("SENSOR");
        double isVertical = (sensor.equals ("ABI") ? 1 : 0);

        CoordinateAxis xHorizAxis = coordSystem.getXHorizAxis();
        int[] dims = xHorizAxis.getShape();

        if (dims.length == 1) {
          int[] xDims = dims;
          CoordinateAxis yHorizAxis = coordSystem.getYHorizAxis();
          int[] yDims = yHorizAxis.getShape();
          dims = new int[] {yDims[0], xDims[0]};
        } // if
        
        double rowStep = Math.toRadians (65536.0/lineFactor);
        double colStep = Math.toRadians (65536.0/columnFactor);
        trans = new EllipsoidPerspectiveProjection (
          new double[] {
            0,
            subpointLon,
            satDist,
            rowStep,
            colStep,
            isVertical
          },
          dims
        );
        
        return (trans);

      } // try
      catch (Exception e) {    
        LOGGER.log (Level.FINE, "Failed to read ACSPO 2.90 geostationary attributes", e);
      } // catch
      
    } // if      
    





    // Check if grid mapped projection
    // -------------------------------
    if (CDMGridMappedProjection.isCompatibleSystem (coordSystem))
      trans = new CDMGridMappedProjection (coordSystem);

    else {
    
      // Check for latitude/longitude axes
      // ---------------------------------
      if (!coordSystem.isLatLon()) {
        throw new UnsupportedEncodingException ("Expected latitude/longitude based coordinate system");
      } // if
      CoordinateAxis latAxis = coordSystem.getYHorizAxis();
      if (latAxis.getAxisType() != AxisType.Lat)
        throw new UnsupportedEncodingException ("Expected Y horizontal axis type to be latitude");
      CoordinateAxis lonAxis = coordSystem.getXHorizAxis();
      if (lonAxis.getAxisType() != AxisType.Lon)
        throw new UnsupportedEncodingException ("Expected X horizontal axis type to be longitude");

      // Create geographic map projection
      // --------------------------------
      if (coordSystem.isProductSet()) {

        // Check for regular spacing
        // -------------------------
        CoordinateAxis1D latAxis1D = (CoordinateAxis1D) latAxis;
        CoordinateAxis1D lonAxis1D = (CoordinateAxis1D) lonAxis;
        double[] latValues = latAxis1D.getCoordValues();
        double[] lonValues = lonAxis1D.getCoordValues();
        boolean isRegular = (latAxis1D.isRegular() && lonAxis1D.isRegular());

        // Create regularly spaced geographic projection
        // ---------------------------------------------
        if (isRegular) {

          // Get latitude and longitude spacing and center
          // ---------------------------------------------
          int[] dims = new int[] {latValues.length, lonValues.length};
          double[] pixelDims = new double[] {
            (latValues[0] - latValues[latValues.length-1]) / (latValues.length-1),
            (lonValues[lonValues.length-1] - lonValues[0]) / (lonValues.length-1)
          };
          EarthLocation center = new EarthLocation (
            (latAxis.getMinValue() + latAxis.getMaxValue())/2,
            (lonAxis.getMinValue() + lonAxis.getMaxValue())/2
          );

          // Check if dimension order is correct: (lat, lon)
          // -----------------------------------------------
          Variable proto = gridset.getGrids().get (0).getVariable();
          int latIndex = proto.findDimensionIndex (latAxis.getShortName());
          int lonIndex = proto.findDimensionIndex (lonAxis.getShortName());
          boolean needsFlip = (latIndex > lonIndex);

          if (needsFlip) LOGGER.warning ("Geographic projection with dimension ordering (lon,lat)");

          // Create GEO projection
          // ---------------------
          try {
            MapProjection proj = MapProjectionFactory.getInstance().create (GCTP.GEO, 0,
              new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, GCTP.WGS84,
              dims, center, pixelDims);
            trans = proj;
          } // try
          catch (NoninvertibleTransformException e) {
            throw new RuntimeException ("Got non-invertible transform while creating geographic projection");
          } // catch

        } // if

        // Create generic cylindrical projection
        // -------------------------------------
        else {
          trans = new GeoVectorProjection (latValues, lonValues, Grid.ROWS, Grid.COLS);
        } // else
      
      } // if
    
      // Create swath projection
      // -----------------------
      else {
      
      
        // TODO: Is this a potential point of leaking memory, where the
        // swath transform will hold onto the nc tile cached grid variable?
        
      
        String latVarName = latAxis.getFullName();
        String lonVarName = lonAxis.getFullName();
        DataVariable lat = null, lon = null;
        try {
          lat = getVariable (latVarName);
          int cols = lat.getDimensions()[Grid.COLS];
          lon = getVariable (lonVarName);
          if (dataProjection) {
            trans = new DataProjection (lat, lon);
          } // if
          else {
            trans = new SwathProjection (lat, lon, 100, //SWATH_POLY_SIZE,
              new int[] {cols, cols});
          } // else
        } // try
        catch (Exception e) {
          LOGGER.log (Level.WARNING, "Problems encountered using earth location data", e);
          if (lat != null && lon != null) {
            LOGGER.warning ("No (lat,lon) --> (row,col) transform available");
            trans = new DataProjection (lat, lon);
          } // if
        } // catch
      } // else
    
    } // else
    
    return (trans);

  } // getTransform

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the list of variable names.
   *
   * @return the list of variable names, expanded so that all non-geographic
   * axis values are available. For example, an SST
   * field at time index 0 could be named sst__T0_x_x.
   */
  private String[] getVariableNames() throws IOException {

    // Initialize map of variable name to group
    // ----------------------------------------
    groupMap = new HashMap<String,VariableGroup>();
    
    // Loop over each group
    // --------------------
    Set<String> nameSet = new LinkedHashSet<String>();
    for (VariableGroup group : groupList) {

      // Add variable names from group to list
      // -------------------------------------
      String[] nameArray = getVariableNamesInGroup (group);
      for (String name : nameArray) {
        nameSet.add (name);
        groupMap.put (name, group);
      } // for
      
      // Add any coordinate variables to list
      // ------------------------------------
      List<CoordinateAxis> axes = group.gridset.getGeoCoordSystem().getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        String name = axis.getFullName();
        nameSet.add (name);
        groupMap.put (name, new VariableGroup());
      } // for
      
    } // for

    return (nameSet.toArray (new String[0]));

  } // getVariableNames

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the list of variable names for a group.
   * 
   * @param group the varible group to get the names (modified).
   *
   * @return the list of variable names in the group, expanded so that 
   * all non-geographic axis values are available.  For example, an SST
   * field at time index 0 could be named sst__T0_x_x.
   */
  private String[] getVariableNamesInGroup (
    VariableGroup group
  ) throws IOException {

    // Create name list
    // ----------------
    List<GridDatatype> grids = group.gridset.getGrids();
    String[] variableNameArray = new String[grids.size()];
    for (int i = 0; i < grids.size(); i++) {
      variableNameArray[i] = grids.get(i).getName();
    } // for
    GridDatatype prototypeGrid = grids.get (0);
    
    // Find coordinate axes for expansion
    // ----------------------------------
    GridCoordSystem coordSystem = group.gridset.getGeoCoordSystem();
    CoordinateAxis yAxis = coordSystem.getYHorizAxis();
    CoordinateAxis xAxis = coordSystem.getXHorizAxis();
    int rank = prototypeGrid.getRank();
    group.expandDims = new int[rank];
    CoordinateAxis[] axes = new CoordinateAxis[rank];
    boolean hasNonHorizontal = false;
    group.extraDims = 0;
    String[] axisPrefix = new String[rank];
    Variable var = prototypeGrid.getVariable();

    for (int i = 0; i < rank; i++) {
    
      // Find axis that uses dimension
      // -----------------------------

      // Note that we get the dimension from the variable at this point,
      // because we discovered that the GridDatatype has the dimensions
      // rearranged on canonical order: (runtime, ensemble, time, z, y, x),
      // even in the getDimension(int) method, not just in the list from
      // getDimensions().

      Dimension gridDimension = var.getDimension (i);
      
      List<CoordinateAxis> axesUsingDimension = new ArrayList<CoordinateAxis>();
      for (CoordinateAxis axis : coordSystem.getCoordinateAxes()) {
        if (axis.getDimensions().contains (gridDimension))
          axesUsingDimension.add (axis);
      } // for
      if (axesUsingDimension.size() == 0)
        throw new IOException ("Cannot find coordinate axes for dimension " + gridDimension);
      
      // Check if one of the axes for the dimension is horizontal
      // ---------------------------------------------------------
      boolean isDimensionHorizontal = (
        axesUsingDimension.contains (xAxis) ||
        axesUsingDimension.contains (yAxis)
      );
      axes[i] = axesUsingDimension.get (0);
      
      // Handle horizontal axis
      // ----------------------
      if (isDimensionHorizontal) {
        group.expandDims[i] = -1;
      } // if
      
      // Handle non-horizontal axis
      // --------------------------
      else {

        // Check for rank 1 axis
        // ---------------------
        if (axes[i].getRank() != 1)
          throw new IOException ("Unsupported coordinate axis rank");

        // Save info about axis
        // --------------------
        group.expandDims[i] = axes[i].getShape()[0];
        AxisType axisType = axes[i].getAxisType();
        if (axisType.equals (AxisType.Time)) axisPrefix[i] = "T";
        else if (axisType.equals (AxisType.GeoZ)) axisPrefix[i] = "Z";
        else if (axisType.equals (AxisType.Pressure)) axisPrefix[i] = "P";
        else if (axisType.equals (AxisType.Height)) axisPrefix[i] = "H";
        else if (axisType.equals (AxisType.RunTime)) axisPrefix[i] = "R";
        else if (axisType.equals (AxisType.Ensemble)) axisPrefix[i] = "E";
        else if (axisType.equals (AxisType.Spectral)) axisPrefix[i] = "S";
        else axisPrefix[i] = "I";
        hasNonHorizontal = true;
        group.extraDims++;
        
      } // else

    } // for

    if (hasNonHorizontal) {

      // Create name extensions
      // ----------------------
      List<String> varExtensions = new ArrayList<String>();
      int[] expandCount = new int[rank];
      boolean isDone = false;
      while (!isDone) {

        // Add expansion counter to variable extensions
        // --------------------------------------------
        StringBuffer extension = new StringBuffer();
        for (int i = 0; i < rank; i++) {
          if (i != 0) extension.append (DELIMIT);
          extension.append (group.expandDims[i] == -1 ? EXPAND :
            axisPrefix[i] + expandCount[i]);
        } // for
        varExtensions.add (extension.toString());

        // Increment expansion counter
        // ---------------------------
        for (int i = rank-1; i >= 0; i--) {
          if (expandCount[i] < (group.expandDims[i] - 1)) {
            expandCount[i]++;
            for (int j = i+1; j < rank; j++) expandCount[j] = 0;
            break;
          } // if              
          else if (i == 0) { 
            isDone = true; 
            break; 
          } // else if
        } // for

      } // while

      // Expand variable names (if needed)
      // ---------------------------------
      List<String> newVarNames = new ArrayList<String>();
      if (varExtensions.size() == 1) {
        group.varExtension = varExtensions.get(0);
      } // if
      else {
        for (String name : variableNameArray) {
          for (String extension : varExtensions)
            newVarNames.add (name + START + extension + END);
        } // for
        variableNameArray = newVarNames.toArray (new String[]{});
      } // else

      // Save variable dimensions
      // ------------------------
      int newRank = rank - group.extraDims;
      group.varDims = new int[newRank];
      int index = 0;
      int coordIndex = 0;
      for (int i = 0; i < rank; i++) {
        if (group.expandDims[i] == -1)
          group.varDims[index++] = axes[i].getShape()[coordIndex];
        if (axes[i].getRank() > 1) coordIndex++;
      } // for

    } // if

    return (variableNameArray);

  } // getVariableNamesInGroup

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new reader from the specified file. 
   * 
   * @param name the file name or URL to read.
   *
   * @throws IOException if an error occurred reading the metadata.
   */
  public CommonDataModelNCReader (
    String name
  ) throws IOException {

    super (name);

  } // CommonDataModelNCReader constructor

  ////////////////////////////////////////////////////////////

  @Override
  protected void initializeReader () throws IOException {
  
    // Get grid sets
    // -------------
    groupList = new ArrayList<VariableGroup>();
    Formatter errorLog = new Formatter();
    GridDataset gridDataset = (GridDataset) FeatureDatasetFactoryManager.wrap (
      FeatureType.GRID, dataset, null, errorLog);
    if (gridDataset == null)
      throw new IOException ("Failed to find grids in " + getFilename() + ", error is: " + errorLog);
    Collection gridsets = gridDataset.getGridsets();
    for (Iterator iter = gridsets.iterator(); iter.hasNext();) {
      VariableGroup group = new VariableGroup();
      group.gridset = (GridDataset.Gridset) iter.next();
      groupList.add (group);
    } // for
    if (groupList.size() == 0)
      throw new IOException ("Cannot locate any grid sets");
    
    // Get info and variables
    // ----------------------
    variables = getVariableNames();
    info = getGlobalInfo();
  
  } // initializeReader

  ////////////////////////////////////////////////////////////

  @Override
  public Map<String, Object> getRawMetadata (int index) throws IOException {

    // In this case, we need to override this method because the normal
    // getPreview() implementation returns a variable with no key/value pairs
    // in the metadata map, for historical reasons.  Also, this reader mangles
    // variable names to expand non-geographic axes, so we need to map back 
    // to the actual variable name.

    var baseName = getBaseVariableName (variables[index]);
    var variable = getReferencedFile().findVariable (baseName);
    if (variable == null) variable = dataset.findCoordinateAxis (baseName);
    if (variable == null) throw new IOException ("Cannot access variable '" + baseName + "' at index " + index);

    Map<String, Object> map = new LinkedHashMap<>();
    variable.attributes().forEach (att -> map.put (att.getShortName(), convertAttributeValue (att, false)));

    return (map);

  } // getRawMetadata

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the base variable name without any expanded axes values.
   *
   * @param name the variable name in expanded form, for example sst__T0_x_x, 
   * which would be sst with a time axis, index 0, and lat/lon axes.
   *
   * @return the root variable name before any "__" delimiter, or the input 
   * variable name if there is no delimiter.
   */
  private String getBaseVariableName (String name) {

    return (name.replaceFirst ("^(.*)" + START + "[TZPHRESI][0-9]+_.*" + END + "$", "$1"));
    
  } // getBaseVariableName

  ////////////////////////////////////////////////////////////

  /**
   * Gets the start index array for the expanded name.
   *
   * @param name the expanded variable name, for example sst__T0_x_x, which
   * would be sst with a time axis, index 0, and lat/lon axes.

   * @return start the starting coordinates to read data from for the expanded
   * name, the same rank as the NetCDF variable, with values
   * filled in for all dimensions _except_ row and column, which have
   * -1 as the value, or null if the expanded variable name contains no
   * array information.
   */
  private int[] getStartIndexArray (
    String name
  ) {
  
    int[] start = null;
  
    // Extend variable name if needed
    // ------------------------------
    VariableGroup group = groupMap.get (name);
    if (group.varExtension != null) name = name + START + group.varExtension + END;
    
    // Get section text
    // ----------------
    String section = name.replaceFirst ("^.*" + START + "(.*)" + END + "$", 
      "$1");

    // Expand text to start specification
    // ----------------------------------
    if (!section.equals (name)) {
      start = Arrays.copyOf (group.expandDims, group.expandDims.length);
      String[] sectionArray = section.split (DELIMIT);
      if (sectionArray.length != start.length)
        throw new RuntimeException ("Mismatch in array lengths");
      for (int i = 0; i < sectionArray.length; i++) {
        if (!sectionArray[i].equals (EXPAND))
          start[i] = Integer.parseInt (sectionArray[i].replaceAll ("[^0-9]", ""));
      } // for
    } // if

    return (start);

  } // getStartIndexArray

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the array section from a variable name or no array section is
   * appended to the variable name.
   *
   * @param name the expanded variable name, for example sst__T0_x_x, which
   * would be sst with a time axis, index 0, and lat/lon axes.
   *
   * @return the array section resulting from the expanded variable name,
   * for example "0:0,0:,0:" for the above example.
   */
  private String getArraySection (
    String name
  ) {

    // Extend variable name if needed
    // ------------------------------
    VariableGroup group = groupMap.get (name);
    if (group.varExtension != null) name = name + START + group.varExtension + END;
  
    // Get section text
    // ----------------
    String section = name.replaceFirst ("^.*" + START + "(.*)" + END + "$", 
      "$1");
    if (section.equals (name)) return (null);

    // Expand text to full specification
    // ---------------------------------
    else {
      StringBuffer buffer = new StringBuffer();
      String[] sectionArray = section.split (DELIMIT);
      int index = 0;
      for (int i = 0; i < sectionArray.length; i++) {
        if (sectionArray[i].equals (EXPAND)) {
          buffer.append ("0:");
          buffer.append (Integer.toString (group.varDims[index]-1));
          index++;
        } // if
        else {
          buffer.append (sectionArray[i].replaceAll ("[^0-9]", ""));
          buffer.append (":");
          buffer.append (sectionArray[i].replaceAll ("[^0-9]", ""));
        } // else
        if (i != sectionArray.length-1) buffer.append (",");
      } // for                                   
      return (buffer.toString());
    } // else
    
  } // getArraySection

  ////////////////////////////////////////////////////////////

  protected DataVariable getPreviewImpl (
    int index
  ) throws IOException { 

    // Access variable
    // ---------------
    String baseName = getBaseVariableName (variables[index]);
    Variable var = getReferencedFile().findVariable (baseName);
    if (var == null) var = dataset.findCoordinateAxis (baseName);
    if (var == null)
      throw new IOException ("Cannot access variable '" + baseName + "' at index " + index);
    VariableGroup group = groupMap.get (variables[index]);
    
    try {

      // Get variable info
      // -----------------
      int varDims[] = (group.varDims != null ? group.varDims : var.getShape());
      String name = variables[index];
      int rank = var.getRank() - group.extraDims;
      Class varClass = var.getDataType().getPrimitiveClassType();
      boolean isUnsigned = var.getDataType().isUnsigned();

      // Create fake data array
      // ----------------------
      Object data = Array.newInstance (varClass, 1);

      // Get calibration
      // ---------------
      double[] scaling;
      try {
        Number scale = (Number) getAttribute (var, "scale_factor");
        Number offset = (Number) getAttribute (var, "add_offset");
        if (offset == null) offset = 0;        
        scaling = new double[] {scale.doubleValue(), offset.doubleValue()};
        /**
         * We re-arrange the CF scaling conventions here into HDF:
         *
         *   y = a'x + b'      (CF)
         *   y = (x - b)*a     (HDF)
         *   => a = a'
         *      b = -b'/a'
         */
        scaling[1] = -scaling[1]/scaling[0];
      } // try
      catch (Exception e) {
        scaling = null;
      } // catch

      // Get missing value
      // -----------------
      Object missing;
      try {
        missing = getAttribute (var, "_FillValue");
        if (missing == null) missing = getAttribute (var, "_fillvalue");
        if (missing == null) missing = getAttribute (var, "missing_value");
      } // try
      catch (Exception e) {
        missing = null;
      } // catch

      // Convert missing value
      // ---------------------
      if (missing != null) {
        Class missingClass;
        try { 
          missingClass = (Class) missing.getClass().getField (
            "TYPE").get (missing);
        } // try
        catch (Exception e) {
          throw new RuntimeException ("Cannot get missing value class");
        } // catch
        if (!missingClass.equals (varClass)) {
          Number missingNumber = (Number) missing;
          if (varClass.equals (Byte.TYPE))
            missing = Byte.valueOf (missingNumber.byteValue());
          else if (varClass.equals (Short.TYPE))
            missing = Short.valueOf (missingNumber.shortValue());
          else if (varClass.equals (Integer.TYPE))
            missing = Integer.valueOf (missingNumber.intValue());
          else if (varClass.equals (Float.TYPE))
            missing = Float.valueOf (missingNumber.floatValue());
          else if (varClass.equals (Double.TYPE))
            missing = Double.valueOf (missingNumber.doubleValue());
          else
            throw new UnsupportedEncodingException ("Unsupported variable class");
        } // if
      } // if

      // Get data strings
      // ----------------
      String longName = var.getDescription();
      if (longName == null) longName = "";
      String units = var.getUnitsString();
      if (units == null) units = "";
      units = units.trim();
      String formatStr = (String) getAttribute (var, "format");
      if (formatStr == null) 
        formatStr = (String) getAttribute (var, "C_format");
      if (formatStr == null) 
        formatStr = "";

      // Try using format string
      // -----------------------         
      int digits = -1;
      if (!formatStr.equals ("")) {
        int dot = formatStr.indexOf ('.');
        digits = 0;
        if (dot != -1 && dot != formatStr.length()-1)
          digits = Character.digit (formatStr.charAt (dot+1), 10);
      } // if

      // Try guessing from scaling factor
      // --------------------------------
      if (digits == -1 && scaling != null) {
        double maxValue = 0;
        if (varClass.equals (Byte.TYPE)) 
          maxValue = Byte.MAX_VALUE*scaling[0];
        else if (varClass.equals (Short.TYPE)) 
          maxValue = Short.MAX_VALUE*scaling[0];
        else if (varClass.equals (Integer.TYPE)) 
          maxValue = Integer.MAX_VALUE*scaling[0];
        else if (varClass.equals (Float.TYPE))
          maxValue = Float.MAX_VALUE*scaling[0];
        else if (varClass.equals (Double.TYPE)) 
          maxValue = Double.MAX_VALUE*scaling[0];
        digits = DataVariable.getDecimals (Double.toString (maxValue));
      } // else if
      
      // Use full precision for floating point types
      // -------------------------------------------
      if (digits == -1 && scaling == null) {
        double maxValue = 0;
        if (varClass.equals (Float.TYPE))
          maxValue = Float.MAX_VALUE;
        else if (varClass.equals (Double.TYPE)) 
          maxValue = Double.MAX_VALUE;
        digits = DataVariable.getDecimals (Double.toString (maxValue));
      } // if

      // Set fractional digits
      // ---------------------
      String decFormat = "0";
      for (int i = 0; i < digits; i++) {
        if (i == 0) decFormat += ".";
        decFormat += "#";
      } // for
      NumberFormat format = new DecimalFormat (decFormat);

      // Create variable
      // ---------------
      DataVariable dataVar;
      if (rank == 0) {
        dataVar = new Line (name, longName, units, 1, data, format,
          scaling, missing);
      } // if
      else if (rank == 1) {
        dataVar = new Line (name, longName, units, varDims[0], data, format,
          scaling, missing);
      } // if
      else if (rank == 2) {
        dataVar = new Grid (name, longName, units, varDims[0], varDims[1], 
          data, format, scaling, missing);
      } // else if 
      else {
        throw new UnsupportedEncodingException ("Unsupported rank = " + rank +
          " for " + name);
      } // else
      dataVar.setUnsigned (isUnsigned);
      
      // Return the new variable
      // -----------------------
      return (dataVar);

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

  } // getPreviewImpl

  ////////////////////////////////////////////////////////////

  /** Produces data chunks directly from this reader. */
  private class NCChunkProducer extends GridChunkProducer {
  
    private TileSource source;
  
    public NCChunkProducer (TileCachedGrid grid) {

      super (grid);
      source = grid.getSource();

    } // NCChunkProducer

    public DataChunk getChunk (ChunkPosition pos) {

      DataChunk chunk;

      // Get a non-native chunk
      // ----------------------
      if (!scheme.isNativePosition (pos)) {
        chunk = super.getChunk (pos);
      } // if

      // Get a native chunk
      // ------------------
      else {

        try {

          // Read tile
          // ---------
          TilePosition tilePos = source.getScheme().getTilePositionForCoords (pos.start[ROW], pos.start[COL]);
          Tile tile = source.readTile (tilePos);

          LOGGER.fine ("Got chunk for " + grid.getName() + " at pos " + pos + " in file " +
            "0x" + Integer.toHexString (dataset.hashCode()));

          // Create chunk using tile data
          // ----------------------------
          chunk = DataChunkFactory.getInstance().create (tile.getData(),
            grid.getUnsigned(), grid.getMissing(), packing, scaling);

        } // try
        catch (IOException e) { throw new RuntimeException (e); }

      } // else

      return (chunk);

    } // getChunk

  } // NCChunkProducer

  ////////////////////////////////////////////////////////////

  @Override
  public ChunkProducer getChunkProducer (
    String name
  ) throws IOException {

    ChunkProducer producer;

    DataVariable var = getVariable (name);
    if (var instanceof TileCachedGrid)
      producer = new NCChunkProducer ((TileCachedGrid) var);
    else
      throw new IOException ("Chunk producer not available for variable " + name);

    return (producer);

  } // getChunkProducer

  ////////////////////////////////////////////////////////////

  @Override
  protected DataVariable getActualVariable (
    int index
  ) throws IOException {

    // Get a variable preview
    // ----------------------
    DataVariable varPreview = getPreview (index);

    // Access variable
    // ---------------
    String baseName = getBaseVariableName (variables[index]);
    Variable var = getReferencedFile().findVariable (baseName);
    if (var == null) var = dataset.findCoordinateAxis (baseName);
    if (var == null)
      throw new IOException ("Cannot access variable '" + baseName + "' at index " + index);
    VariableGroup group = groupMap.get (variables[index]);

    // Create tile cached grid
    // -----------------------
    DataVariable dataVar;
    if (varPreview instanceof Grid) {

      // Get row and column indices
      // --------------------------
      int rowIndex, colIndex;
      int[] start = getStartIndexArray (variables[index]);
      if (start != null) {
        rowIndex = colIndex = -1;
        for (int i = 0; i < start.length; i++) {
          if (start[i] == -1) {
            if (rowIndex == -1) rowIndex = i;
            else if (colIndex == -1) colIndex = i;
          } // if
        } // for
      } // if
      else {
        rowIndex = Grid.ROWS;
        colIndex = Grid.COLS;
      } // else
      
      // Create tile source
      // ------------------
      NCTileSource source = new NCTileSource (getReferencedFile(),
        baseName, rowIndex, colIndex, start);
      TileCachedGrid cachedGrid = new TileCachedGrid ((Grid) varPreview, source);
      dataVar = cachedGrid;

    } // if

    // Read full data
    // --------------
    else {
      Object data;
      if (group.extraDims == 0)
        data = var.read().getStorage();
      else {
        try {
          String section = getArraySection (variables[index]);
          data = var.read (section).getStorage();
        } // try
        catch (InvalidRangeException e) {
          throw new IOException ("Invalid array section in data read");
        } // catch
      } // else
      dataVar = varPreview;
      dataVar.setData (data);
    } // else

    // Return variable
    // ---------------
    return (dataVar);

  } // getActualVariable

  ////////////////////////////////////////////////////////////

  public Grid getGridSubset (
    String varName,
    int[] start,
    int[] stride,
    int[] length
  ) throws IOException {

    // Extend variable name if needed
    // ------------------------------
    VariableGroup group = groupMap.get (varName);
    if (group.varExtension != null) varName = varName + START + group.varExtension + END;

    // Check for mangled name
    // ----------------------
    String actualVarName = getBaseVariableName (varName);
    if (actualVarName.equals (varName))
      return (super.getGridSubset (varName, start, stride, length));

    // Get a variable grid
    // -------------------
    Grid grid = new Grid ((Grid) getPreview (varName), length[Grid.ROWS],
      length[Grid.COLS]);
    
    // Access variable
    // ---------------
    String section = getArraySection (varName);
    varName = actualVarName;
    Variable var = getReferencedFile().findVariable (varName);
    if (var == null)
      throw new IOException ("Cannot access variable " + varName);

    // Create combined section string
    // ------------------------------
    int[] end = new int[] {
      start[0] + stride[0]*length[0] - 1,
      start[1] + stride[1]*length[1] - 1
    };
    StringBuffer sectionBuf = new StringBuffer();
    int spatialIndex = 0;
    String[] sectionArray = section.split (",");
    for (int i = 0; i < sectionArray.length; i++) {
      if (group.expandDims[i] == -1) {
        sectionBuf.append (start[spatialIndex] + ":");
        sectionBuf.append (end[spatialIndex] + ":");
        sectionBuf.append (stride[spatialIndex] + "");
        spatialIndex++;
      } // if
      else
        sectionBuf.append (sectionArray[i]);
      if (i != sectionArray.length-1) sectionBuf.append (",");
    } // for                                                        
    section = sectionBuf.toString();

    // Read data
    // ---------
    Object data;
    try {
      data = var.read (section).getStorage();
    } // try
    catch (InvalidRangeException e) {
      throw new IOException ("Invalid section spec in grid subset read");
    } // catch

    // Return variable
    // ---------------
    grid.setData (data);
    return (grid);

  } // getGridSubset

  ////////////////////////////////////////////////////////////

} // CommonDataModelNCReader class

////////////////////////////////////////////////////////////////////////

