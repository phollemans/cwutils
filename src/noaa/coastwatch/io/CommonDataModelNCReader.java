////////////////////////////////////////////////////////////////////////
/*
     FILE: CommonDataModelNCReader.java
  PURPOSE: Reads CoastWatch-style data through the NetCDF interface.
   AUTHOR: Peter Hollemans
     DATE: 2005/07/04
  CHANGES: 2006/05/28, PFH, modified to use MapProjectionFactory
           2006/11/03, PFH, changed getPreview(int) to getPreviewImpl(int)
           2007/02/13, PFH, added support for reading > 2D datasets using
             array sections (ugly!)
           2007/03/29, PFH, augmented to read multiple grid sets
           2008/02/16, PFH, modified to use new Java netCDF 2.2.22 library
           2010/02/14, PFH, modified to use new Java netCDF 4.1 library
           2013/01/06, PFH, added extra comments for class
           2013/05/24, PFH
            - updated to use GridCoordSystem.getCalendarDateRange()
            - modified latitude axis reading to handle south first reading
           2014/04/06, PFH
           - Changes: Updated to access the underlying NetCDF variable 
             rather than the enhanced VariableDS, and to use the NCCachedGrid.
             This means we have to do the metadata parsing ourselves for 
             missing value and scaling/offset since there do not appear
             to be any methods in VariableDS to access that info.
           - Issue: We were running out of heap space with large datasets
             (3600x7200) with float datatype, ~100 Mb per variable.  We need
             to reduce this data footprint on the heap, and also use caching.
             Accessing the underlying variable gave us access to the scaled
             data (ie: short rather than float) and combined with caching
             should control the heap usage.
           2014/04/09, PFH
           - Changes: Removed use of setIsCFConventions in DataVariable.
           - Issue: The use of the method was never fully implemented in 
             DataVariable so rather than continuing its use, we decided
             to remove it and re-arrange the scaling and offset for CF
             conventions before passing into the Grid constructor.
           2014/11/17, PFH
           - Changes: Renamed class to "CommonDataModel" from "Generic"
           - Issue: We needed a more accurate description of the reader class,
             since we'd like to expand its functionality in the future.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import noaa.coastwatch.io.NCReader;
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
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.trans.SwathProjection;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
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

/** 
 * The <code>CommonDataModelNCReader</code> class reads Java NetCDF API
 * accessible datasets and attempts to use any metadata and
 * geographic referencing information that the NetCDF layer
 * provides.  If a data variable is found that has two geographic
 * axes but also time and/or level axes, the variable data is
 * expanded to a series of 2D variables by extending the variable
 * name for each non-geographic axis.  Datasets must:
 * <ul>
 *   <li>have the same Earth transform for all grids, and</li>
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
  
  /** Gets the Earth data info object. */
  private EarthDataInfo getGlobalInfo () throws IOException { 

    // TODO: Does this correctly detect the time periods in the file?
    // We have a MODIS file that this doesn't seem to work with.

    // Create list of time periods
    // ---------------------------
    Set<Date> dateSet = new TreeSet<Date>();
    for (VariableGroup group : groupList) {
      GridCoordSystem coordSystem = group.gridset.getGeoCoordSystem();
      CalendarDateRange dateRange = coordSystem.getCalendarDateRange();



//System.out.println ("time axis = " + coordSystem.getTimeAxis());
//System.out.println ("time boundary ref = " + coordSystem.getTimeAxis1D().getBoundaryRef());



      if (dateRange != null) {
        dateSet.add (new Date (dateRange.getStart().getMillis()));


//System.out.println ("dateRange start = " + new Date (dateRange.getStart().getMillis()));


        dateSet.add (new Date (dateRange.getEnd().getMillis()));


//System.out.println ("dateRange end = " + new Date (dateRange.getEnd().getMillis()));
//System.out.println ("dateRange isPoint = " + dateRange.isPoint());
//System.out.println ("dateRange duration = " + dateRange.getDuration());





      } // if
    } // for
    if (dateSet.size() == 0) dateSet.add (new Date(0));
    List<TimePeriod> periodList = new ArrayList<TimePeriod>();
    for (Date date : dateSet) periodList.add (new TimePeriod (date, 0));
    
    // Find the data origin and source
    // -------------------------------
    String origin = null;
    String source = null;
    String platform = null;
    String sensor = null;
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
        if (name.equals ("platform")) {
          platform = att.getStringValue();
        } // if
      } // if
      if (sensor == null) {
        if (name.equals ("sensor")) {
          sensor = att.getStringValue();
        } // if
      } // if
    } // for
    if (origin == null) origin = "unknown";
    if (source == null) source = "unknown";

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

  /** Gets the Earth transform information for the dataset. */
  private EarthTransform getTransform () throws IOException {

    // Get list of transforms
    // ----------------------
    List<EarthTransform> transList = new ArrayList<EarthTransform>();
    for (VariableGroup group : groupList) {
      transList.add (getTransform (group.gridset));
    } // for

    // Check that all transforms are equal
    // -----------------------------------
    EarthTransform baseTrans = transList.get (0);
    for (int i = 1; i < transList.size(); i++) {
      if (!baseTrans.equals (transList.get (i)))
        throw new IOException ("Earth transforms are not equal in all grids");
    } // if

    return (baseTrans);

  } // getTransform

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the Earth transform information for the specified grid
   * set. 
   */
  private EarthTransform getTransform (
    GridDataset.Gridset gridset
  ) throws IOException {

    // Get and check coordinate system
    // -------------------------------
    GridCoordSystem coordSystem = gridset.getGeoCoordSystem();
    if (!coordSystem.isLatLon()) {
      throw new UnsupportedEncodingException (
        "Unsupported coordinate system, not latitude/longitude based");
    } // if
    CoordinateAxis latAxis = coordSystem.getYHorizAxis();
    if (latAxis.getAxisType() != AxisType.Lat)
      throw new UnsupportedEncodingException ("Expected Y horizontal axis type to be latitude");
    CoordinateAxis lonAxis = (CoordinateAxis) coordSystem.getXHorizAxis();
    if (lonAxis.getAxisType() != AxisType.Lon)
      throw new UnsupportedEncodingException ("Expected X horizontal axis type to be longitude");

    // Create map projection
    // ---------------------
    EarthTransform trans = null;
    if (coordSystem.isProductSet()) {

      // Get latitude and longitude spacing and center
      // ---------------------------------------------
      double[] latValues = ((CoordinateAxis1D) latAxis).getCoordValues();
      double[] lonValues = ((CoordinateAxis1D) lonAxis).getCoordValues();
      int[] dims = new int[] {latValues.length, lonValues.length};
      double[] pixelDims = new double[] {
        (latValues[0] - latValues[latValues.length-1]) / (latValues.length-1),
        (lonValues[lonValues.length-1] - lonValues[0]) / (lonValues.length-1)
      };
      EarthLocation center = new EarthLocation (
        (latAxis.getMinValue() + latAxis.getMaxValue())/2,
        (lonAxis.getMinValue() + lonAxis.getMaxValue())/2
      );

      // Create GEO projection
      // ---------------------
      try {
        trans = MapProjectionFactory.getInstance().create (GCTP.GEO, 0,
          new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, GCTP.WGS84,
          dims, center, pixelDims);
      } // try
      catch (NoninvertibleTransformException e) {
        throw new RuntimeException ("Got non-invertible transform creating map projection");
      } // catch

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
        System.err.println (this.getClass() + 
          ": Warning: Problems encountered using Earth location data");
        e.printStackTrace();
        if (lat != null && lon != null) {
          System.err.println (this.getClass() + 
            ": Warning: Falling back on data-only projection, " +
            "Earth location reverse lookup will not function");
          trans = new DataProjection (lat, lon);
        } // if
      } // catch
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
    List<String> nameList = new ArrayList<String>();
    for (VariableGroup group : groupList) {

      // Add variable names from group to list
      // -------------------------------------
      String[] nameArray = getVariableNamesInGroup (group);
      for (String name : nameArray) {
        nameList.add (name);
        groupMap.put (name, group);
      } // for
      
      // Add any coordinate variables to list
      // ------------------------------------
      List<CoordinateAxis> axes = group.gridset.getGeoCoordSystem().getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        String name = axis.getFullName();
        nameList.add (name);
        groupMap.put (name, new VariableGroup());
      } // for
      
    } // for

    return (nameList.toArray (new String[0]));

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
    
    // Find coordinate axes for expansion
    // ----------------------------------
    GridCoordSystem coordSystem = group.gridset.getGeoCoordSystem();
    CoordinateAxis latAxis = coordSystem.getYHorizAxis();
    CoordinateAxis lonAxis = coordSystem.getXHorizAxis();
    int rank = coordSystem.getDomain().size();
    group.expandDims = new int[rank];
    CoordinateAxis[] axes = (CoordinateAxis[]) 
      coordSystem.getCoordinateAxes().toArray (new CoordinateAxis[] {});
    boolean hasNonSpatial = false;
    group.extraDims = 0;
    String[] axisPrefix = new String[rank];
    for (int i = 0; i < rank; i++) {
      if (!axes[i].equals (latAxis) && !axes[i].equals (lonAxis)) {
        if (axes[i].getRank() != 1) 
          throw new IOException ("Unsupported coordinate axis rank");
        group.expandDims[i] = axes[i].getShape()[0];
        AxisType axisType = axes[i].getAxisType();
        if (axisType.equals (AxisType.Time)) axisPrefix[i] = "T";
        else if (axisType.equals (AxisType.GeoZ)) axisPrefix[i] = "Z";
        else if (axisType.equals (AxisType.Pressure)) axisPrefix[i] = "P";
        else if (axisType.equals (AxisType.Height)) axisPrefix[i] = "H";
        else axisPrefix[i] = "I";
        hasNonSpatial = true;
        group.extraDims++;
      } // if
      else
        group.expandDims[i] = -1;
    } // for

    if (hasNonSpatial) {

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

    // Get grid sets
    // -------------
    groupList = new ArrayList<VariableGroup>();
    Formatter errorLog = new Formatter();
    GridDataset gridDataset = (GridDataset) FeatureDatasetFactoryManager.wrap (
      FeatureType.GRID, dataset, null, errorLog);
    if (gridDataset == null)
      throw new IOException ("Failed to find grids in " + name + ", error is: " + errorLog);
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

  } // CommonDataModelNCReader constructor

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

    return (name.replaceFirst ("^(.*)" + START + ".*" + END + "$", "$1"));
    
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
    Variable var = dataset.getReferencedFile().findVariable (baseName);
    if (var == null)
      throw new IOException ("Cannot access variable at index " + index);
    VariableGroup group = groupMap.get (variables[index]);
    
    try {
    
      // Get variable info
      // -----------------
      int varDims[] = (group.varDims != null ? group.varDims : var.getShape());
      String name = variables[index];
      int rank = var.getRank() - group.extraDims;
      Class varClass = var.getDataType().getPrimitiveClassType();
      boolean isUnsigned = var.isUnsigned();

      // Create fake data array
      // ----------------------
      Object data = Array.newInstance (varClass, 1);

      // Get calibration
      // ---------------
      double[] scaling;
      try {
        Number scale = (Number) getAttribute (var, "scale_factor");
        Number offset = (Number) getAttribute (var, "add_offset");
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
            missing = new Byte (missingNumber.byteValue());
          else if (varClass.equals (Short.TYPE))
            missing = new Short (missingNumber.shortValue());
          else if (varClass.equals (Integer.TYPE))
            missing = new Integer (missingNumber.intValue());
          else if (varClass.equals (Float.TYPE))
            missing = new Float (missingNumber.floatValue());
          else if (varClass.equals (Double.TYPE))
            missing = new Double (missingNumber.doubleValue());
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
      if (rank == 1) {
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

  protected DataVariable getActualVariable (
    int index
  ) throws IOException {

    // Get a variable preview
    // ----------------------
    DataVariable varPreview = getPreview (index);

    // Access variable
    // ---------------
    String baseName = getBaseVariableName (variables[index]);
    Variable var = dataset.getReferencedFile().findVariable (baseName);
    if (var == null)
      throw new IOException ("Cannot access variable at index " + index);
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
      NCTileSource source = new NCTileSource (dataset.getReferencedFile(),
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
    Variable var = dataset.getReferencedFile().findVariable (varName);
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
