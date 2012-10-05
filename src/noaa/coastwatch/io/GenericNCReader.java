////////////////////////////////////////////////////////////////////////
/*
     FILE: GenericNCReader.java
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

  CoastWatch Software Library and Utilities
  Copyright 1998-2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.util.*;
import java.lang.reflect.Array;
import java.text.*;
import java.awt.geom.*;
import java.io.*;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.dataset.*;
import ucar.nc2.ft.*;
import ucar.nc2.dt.*;
import ucar.nc2.units.*;
import ucar.ma2.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.*;

/** 
 * The <code>GenericNCReader</code> class reads Java NetCDF API
 * accessible datasets and attempts to use any metadata and
 * geographic referencing information that the NetCDF layer
 * provides.  If a data variable is found that has two geographic
 * axes but also time and/or level axes, the variable data is
 * expanded to a series of 2D variables by extending the variable
 * name for each non-geographic axis.
 */
public class GenericNCReader 
  extends NCReader {

  // Constants
  // ---------

  /** The data format description. */
  private static final String DATA_FORMAT = "Java NetCDF/CDM";

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

    /** The grid set to access. */
    public GridDataset.Gridset gridset;
    
    /** The number of extra dimensions. */
    public int extraDims;
    
    /** The variable dimensions for each grid. */
    public int[] varDims;
    
    /** 
     * The expanded dimensions array, -1 for spatial axis, >=0 for
     * expanded axis. 
     */
    public int[] expandDims;

  } // VariableGroup class

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { 

    return (DATA_FORMAT);

  } // getDataFormat

  ////////////////////////////////////////////////////////////
  
  /** Gets the Earth data info object. */
  private EarthDataInfo getGlobalInfo () throws IOException { 

    // Create list of time periods
    // ---------------------------
    Set<Date> dateSet = new TreeSet<Date>();
    for (VariableGroup group : groupList) {
      GridCoordSystem coordSystem = group.gridset.getGeoCoordSystem();
      DateRange dateRange = coordSystem.getDateRange();
      if (dateRange != null) {
        dateSet.add (dateRange.getStart().getDate());
        dateSet.add (dateRange.getEnd().getDate());
      } // if
    } // for
    if (dateSet.size() == 0) dateSet.add (new Date(0));
    List periodList = new ArrayList();
    for (Date date : dateSet) periodList.add (new TimePeriod (date, 0));
    
    // Find the data origin and source
    // -------------------------------
    String origin = null, source = null;
    for (Attribute att : (List<Attribute>) dataset.getGlobalAttributes()) {
      if (!att.isString()) continue;
      String name = att.getName().toLowerCase();
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
    } // for
    if (source == null) source = "unknown";
    if (origin == null) origin = "unknown";

    // Create info
    // -----------
    EarthDataInfo info = new EarthDataInfo (source, periodList,
      getTransform(), origin, "");

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
    for (EarthTransform trans : transList) {
      if (!baseTrans.equals (trans))
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

    // Get coordinate system
    // ---------------------
    GridCoordSystem coordSystem = gridset.getGeoCoordSystem();

    /*
    int coordRank = coordSystem.getRankDomain();
    if (coordRank != 2) {
      throw new UnsupportedEncodingException (
        "Unsupported coordinate system, rank = " + coordRank);
    } // if
    */

    if (!coordSystem.isLatLon()) {
      throw new UnsupportedEncodingException (
        "Unsupported coordinate system, not lat/lon");
    } // if

    /*
    if (!coordSystem.isRegularSpatial()) {
      throw new UnsupportedEncodingException (
        "Coordinate system lat/lon axes are not regular");
    } // if
    */

    // Create map projection
    // ---------------------
    CoordinateAxis1D latAxis = (CoordinateAxis1D) coordSystem.getYHorizAxis();
    CoordinateAxis1D lonAxis = (CoordinateAxis1D) coordSystem.getXHorizAxis();
    int[] dims = new int[] {
      latAxis.getDimension(0).getLength(),
      lonAxis.getDimension(0).getLength()
    };
    double[] pixelDims = new double[] {
      (latAxis.getMaxValue() - latAxis.getMinValue())/(dims[0]-1),
      (lonAxis.getMaxValue() - lonAxis.getMinValue())/(dims[1]-1)
    };
    /*
    double[] pixelDims = new double[] {
      latAxis.getIncrement(),
      lonAxis.getIncrement()
    };
    */
    EarthLocation center = new EarthLocation (
      (latAxis.getMinValue() + latAxis.getMaxValue())/2,
      (lonAxis.getMinValue() + lonAxis.getMaxValue())/2
    );
    MapProjection proj;
    try {
      proj = MapProjectionFactory.getInstance().create (GCTP.GEO, 0, 
        new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, GCTP.WGS84,
        dims, center, pixelDims);
    } // try
    catch (NoninvertibleTransformException e) {
      throw new RuntimeException ("Invalid transform in map projection");
    } // catch

    return (proj);

  } // getTransform

  ////////////////////////////////////////////////////////////

  /** Gets the list of variable names. */
  private String[] getVariableNames () throws IOException {

    // Create list of all names
    // ------------------------
    groupMap = new HashMap<String,VariableGroup>();
    List<String> nameList = new ArrayList<String>();
    for (VariableGroup group : groupList) {
      String[] nameArray = getVariableNames (group);
      for (String name : nameArray) {
        nameList.add (name);
        groupMap.put (name, group);
      } // for
    } // for

    return (nameList.toArray (new String[0]));

  } // getVariableNames

  ////////////////////////////////////////////////////////////

  /** Gets the list of variable names for the specified group. */
  private String[] getVariableNames (VariableGroup group) throws IOException {

    // Create name list
    // ----------------
    List<GridDatatype> grids = group.gridset.getGrids();
    String[] variableNameArray = new String[grids.size()];
    for (int i = 0; i < grids.size(); i++)
      variableNameArray[i] = grids.get(i).getName();

    // Find coordinate axes for expansion
    // ----------------------------------
    GridCoordSystem coordSystem = group.gridset.getGeoCoordSystem();
    CoordinateAxis1D latAxis = (CoordinateAxis1D) coordSystem.getYHorizAxis();
    CoordinateAxis1D lonAxis = (CoordinateAxis1D) coordSystem.getXHorizAxis();
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
        group.expandDims[i] = 0;
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
          extension.append (group.expandDims[i] == 0 ? EXPAND : 
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

      // Expand variable names
      // ---------------------
      List<String> newVarNames = new ArrayList<String>();
      for (String name : variableNameArray) {
        for (String extension : varExtensions)
          newVarNames.add (name + START + extension + END);
      } // for
      variableNameArray = newVarNames.toArray (new String[]{});

      // Save variable dimensions
      // ------------------------
      int newRank = rank - group.extraDims;
      group.varDims = new int[newRank];
      int index = 0;
      for (int i = 0; i < rank; i++) {
        if (group.expandDims[i] == 0) 
          group.varDims[index++] = axes[i].getShape()[0];
      } // for

    } // if

    return (variableNameArray);

  } // getVariableNames

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new reader from the specified file. 
   * 
   * @param name the file name or URL to read.
   *
   * @throws IOException if an error occurred reading the metadata.
   */
  public GenericNCReader (
    String name
  ) throws IOException {

    super (name);

    // Get grid sets
    // -------------
    groupList = new ArrayList<VariableGroup>();
    Formatter errorLog = new Formatter();
    GridDataset gridDataset = (GridDataset) FeatureDatasetFactoryManager.wrap (
      FeatureType.GRID, dataset, null, errorLog);
    if (gridDataset == null) {
      throw new IOException ("Failed to find grids in " + name +", error is: "+
        errorLog);
    } // if
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
    info = getGlobalInfo();
    variables = getVariableNames();

  } // GenericNCReader constructor

  ////////////////////////////////////////////////////////////

  /** Gets the variable name minus any array section. */
  private String getVariableName (String name) {

    return (name.replaceFirst ("^(.*)" + START + ".*" + END + "$", "$1"));
    
  } // getVariableName

  ////////////////////////////////////////////////////////////

  /** Gets the array section from a variable name or null if none exists. */
  private String getArraySection (String name) {

    // Get section text
    // ----------------
    String section = name.replaceFirst ("^.*" + START + "(.*)" + END + "$", 
      "$1");
    if (section.equals (name)) return (null);

    // Expand text to full specification
    // ---------------------------------
    else {
      VariableGroup group = groupMap.get (name);
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
    Variable var = dataset.findVariable (getVariableName (variables[index]));
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
      boolean isUnsigned;
      if (varClass.equals (Double.TYPE) || varClass.equals (Float.TYPE))
        isUnsigned = false;
      else
        isUnsigned = var.isUnsigned();
    
      // Create fake data array
      // ----------------------
      Object data = Array.newInstance (varClass, 1);

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

      // TODO: The problem here is that if there are no format
      // strings or hints for what the accuracy of the data is,
      // the format reverts to integer.  Probably the type should
      // be used to determine the format.

      int digits = -1;
      if (!formatStr.equals ("")) {
        int dot = formatStr.indexOf ('.');
        digits = 0;
        if (dot != -1 && dot != formatStr.length()-1)
          digits = Character.digit (formatStr.charAt (dot+1), 10);
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
      if (rank == 1) {
        dataVar = new Line (name, longName, units, varDims[0], data, format,
          null, null);
      } // if
      else if (rank == 2) {
        dataVar = new Grid (name, longName, units, varDims[0], varDims[1], 
          data, format, null, null);
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
    DataVariable dataVar = getPreview (index);

    // Access variable
    // ---------------
    Variable var = dataset.findVariable (getVariableName (variables[index]));
    if (var == null)
      throw new IOException ("Cannot access variable at index " + index);
    VariableGroup group = groupMap.get (variables[index]);

    // Read data
    // ---------
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

    // Return variable
    // ---------------
    dataVar.setData (data);
    return (dataVar);

  } // getActualVariable

  ////////////////////////////////////////////////////////////

  public Grid getGridSubset (
    String varName,
    int[] start,
    int[] stride,
    int[] length
  ) throws IOException {

    // Check for mangled name
    // ----------------------
    String actualVarName = getVariableName (varName);
    if (actualVarName.equals (varName))
      return (super.getGridSubset (varName, start, stride, length));

    // Get a variable grid
    // -------------------
    Grid grid = new Grid ((Grid) getPreview (varName), length[Grid.ROWS],
      length[Grid.COLS]);
    
    // Access variable
    // ---------------
    String section = getArraySection (varName);
    VariableGroup group = groupMap.get (varName);
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
      if (group.expandDims[i] == 0) {
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

} // GenericNCReader class

////////////////////////////////////////////////////////////////////////

