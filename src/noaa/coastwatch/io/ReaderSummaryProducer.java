/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.io;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.ReportFormatter;

import static noaa.coastwatch.util.MetadataServices.DATE_FMT;
import static noaa.coastwatch.util.MetadataServices.TIME_FMT;
import static noaa.coastwatch.util.MetadataServices.DATE_TIME_FMT;

import ucar.ma2.Array;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.units.DateUnit;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>ReaderSummaryProducer</code> class creates summary data for 
 * {@link EarthDataReader} objects.
 *
 * @author Peter Hollemans
 * @since 3.7.1
 */
public class ReaderSummaryProducer {

  private static final Logger LOGGER = Logger.getLogger (ReaderSummaryProducer.class.getName());

  private static ReaderSummaryProducer instance;

  /** Java primitve 1D array class types. */
  private static final Class[] JAVA_TYPES = {
    Boolean.TYPE,
    Byte.TYPE,
    Character.TYPE,
    Short.TYPE,
    Integer.TYPE,
    Long.TYPE,
    Float.TYPE,
    Double.TYPE
  };

  /** Primitive array class names. */
  private static final String[] JAVA_NAMES = {
    "boolean",
    "byte",
    "char",
    "short",
    "int",
    "long",
    "float",
    "double"
  };

  /////////////////////////////////////////////////////////////////

  protected ReaderSummaryProducer () {}

  /////////////////////////////////////////////////////////////////

  public static ReaderSummaryProducer getInstance() {

    if (instance == null) instance = new ReaderSummaryProducer();
    return (instance);

  } // getInstance

  /////////////////////////////////////////////////////////////////

  /** 
   * Holds summary information for a reader in the form of maps of properties
   * to values, and a variable information table.
   */
  public static class Summary {

    public String source;
    public Map<String, String> global;    
    public Map<String, String> transform;
    public List<Map<String, String>> coordinate;
    public SummaryTable variable;

  } // Summary class

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a summary of reader information of all types: global, variable,
   * transform, and coordinate.
   * 
   * @param reader the reader object to use.
   * 
   * @return the summary data for the reader.
   * 
   * @throws IOException if an error occurred using the reader.
   */
  public Summary create (
    EarthDataReader reader
  ) throws IOException {

    return (create (reader, false, EarthLocation.DDDD));

  } // create

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a report of a summary.
   * 
   * @param summary the summary produced for a reader.
   * @param report the report formatter to use.
   * @param global the global flag, true to report global information.
   * @param variable the variable flag, true to report variable information.
   * @param transform the transform flag, true to report transform information.
   * @param coordinate the coordinate flag, true to report coordinate information.
   */
  public void report (
    Summary summary,
    ReportFormatter report,
    boolean global,
    boolean variable,
    boolean transform,
    boolean coordinate
  ) { 

    report.start();

    report.title ("Contents of " + summary.source);

    if (global) {
      report.section ("Global information:");
      report.map (summary.global);
    } // if

    if (variable) {
      report.section ("Variable information:");
      report.table (summary.variable.columnNames, summary.variable.rowList);
    } // if

    if (transform) {
      report.section ("Earth location information:");
      report.map (summary.transform);
    } // if

    if (coordinate) {

      report.section ("Coordinate system information:");
      if (summary.coordinate.size() == 0)
        report.line ("** No NetCDF coordinate systems found **");
      else {
        var elements = summary.coordinate.size();
        for (int i = 0; i < elements; i++) {
          report.section ("Coordinate system [" + (i+1) + "/" + elements + "]");          
          report.map (summary.coordinate.get (i));
        } // for
      } // else

    } // if

    report.end();

  } // report

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a report of a summary.
   * 
   * @param summary the summary produced for a reader.
   * @param report the report formatter to use.
   */
  public void report (
    Summary summary,
    ReportFormatter report
  ) { 

    report (summary, report, true, true, true, true);

  } // report

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a summary of reader information of all types: global, variable,
   * transform, and coordinate.
   * 
   * @param reader the reader object to use.
   * @param useEdges true to use actual edges for location values in the 
   * transform summary, false to use center of edge pixels.
   * @param locFormat the Earth location format code,
   * see {@link EarthLocation#format}.
   * 
   * @return the summary data for the reader.
   * 
   * @throws IOException if an error occurred using the reader.
   */
  public Summary create (
    EarthDataReader reader,
    boolean useEdges,
    int locFormat
  ) throws IOException {

    var summary = new Summary();
    summary.source = reader.getSource();
    summary.global = getGlobalSummary (reader);    
    summary.transform = getTransformSummary (reader, useEdges, locFormat);
    summary.coordinate = getCoordinateSummary (reader);
    summary.variable = getVariableSummary (reader);

    return (summary);

  } // create

  /////////////////////////////////////////////////////////////////

  /**
   * Gets earth transform data from the specified reader.  The earth
   * transform data includes pixel resolution, total width and height,
   * and latitude and longitude data for selected locations.
   *
   * @param reader the reader object to use.
   * @param useEdges true to use actual edges for location values,
   * false to use center of edge pixels.
   * @param locFormat the Earth location format code,
   * see {@link EarthLocation#format}.
   * 
   * @return the map of information key to value.
   */
  public Map<String, String> getTransformSummary (
    EarthDataReader reader,
    boolean useEdges,
    int locFormat
  ) {

    // Get info
    // --------
    EarthDataInfo info = reader.getInfo();
    EarthTransform trans = info.getTransform();
    int[] dims = trans.getDimensions();
    int rows = dims[0];
    int cols = dims[1];

    // Create map
    // ----------
    var valueMap = new LinkedHashMap<String, String>();

    // Add distance info
    // -----------------
    double centerRow = (rows-1)/2.0;
    double centerCol = (cols-1)/2.0;
    double pixelWidth = trans.distance (
      new DataLocation (centerRow, centerCol-0.5),
      new DataLocation (centerRow, centerCol+0.5));
    valueMap.put ("Pixel width", String.format ("%.4f km", pixelWidth));
    double pixelHeight = trans.distance (
      new DataLocation (centerRow-0.5, centerCol),
      new DataLocation (centerRow+0.5, centerCol));
    valueMap.put ("Pixel height", String.format ("%.4f km", pixelHeight));
    double width = trans.distance (
      new DataLocation (centerRow, 0),
      new DataLocation (centerRow, cols-1));
    valueMap.put ("Total width", String.format ("%.4f km", width));
    double height = trans.distance (
      new DataLocation (0, centerCol),
      new DataLocation (rows-1, centerCol));
    valueMap.put ("Total height", String.format ("%.4f km", height));

    // Add location info
    // -----------------
    double corr = (useEdges ? 0.5 : 0);
    String pixelLoc = (useEdges ? "edge" : "center");
    valueMap.put ("Center",
      trans.transform (new DataLocation (centerRow, centerCol)).format (locFormat));
    valueMap.put ("Upper-left (pixel " + pixelLoc + ")",
      trans.transform (new DataLocation (0-corr, 0-corr)).format (locFormat));
    valueMap.put ("Upper-right (pixel " + pixelLoc + ")",
      trans.transform (new DataLocation (0-corr, cols-1+corr)).format (locFormat));
    valueMap.put ("Lower-left (pixel " + pixelLoc + ")",
      trans.transform (new DataLocation (rows-1+corr, 0-corr)).format (locFormat));
    valueMap.put ("Lower-right (pixel " + pixelLoc + ")",
      trans.transform (new DataLocation (rows-1+corr, cols-1+corr)).format (locFormat));

    return (valueMap);

  } // getTransformSummary

  /////////////////////////////////////////////////////////////////

  private Map<String, String> getCoordSystemSummary (
    EarthDataReader reader, 
    CoordinateSystem system
  ) throws IOException {                                        

    var valueMap = new LinkedHashMap<String, String>();

    // Get the axis names and types
    List<CoordinateAxis> axes = system.getCoordinateAxes();
    var buf = new StringBuffer();
    axes.forEach (axis -> {
      buf.append (axis.getShortName());
      var axisType = axis.getAxisType();
      if (axisType != null) buf.append ("(" + axisType.toString() + ")");
      buf.append (" ");
    });
    valueMap.put ("Axes", buf.toString());

    // Get each axis
    // -------------    
    for (CoordinateAxis axis : axes) {

      // Get basic axis info
      // -------------------
      var axisType = axis.getAxisType();
      var units = axis.getUnitsString();
      int size = (int) axis.getSize();
      var axisHeader = String.format ("%s (%s)", axis.getShortName(), units);

      // Create date unit if applicable
      // ------------------------------
      DateUnit dateUnit = null;
      if (axisType == AxisType.Time) {
        try { dateUnit = new DateUnit (units); }
        catch (Exception e) { }
      } // if

      // Detect geographic axis
      // ----------------------
      boolean isGeo = (
        axisType == AxisType.GeoX || 
        axisType == AxisType.GeoY ||
        axisType == AxisType.Lat ||
        axisType == AxisType.Lon ||
        axisType == AxisType.RadialAzimuth ||
        axisType == AxisType.RadialDistance ||
        axisType == AxisType.RadialElevation
      );

      // Get coordinates for simple axes
      // -------------------------------
      buf.setLength (0);
      buf.append (String.format ("Rank=%d ", axis.getRank()));

      if (axis.getRank() != 0)
        buf.append (String.format ("Shape=%s ", Arrays.toString (axis.getShape())));

      if (axis.getRank() == 1) {
        Array data = axis.read();

        for (int i : new int[] {0, size-1}) {

          Object value = data.getObject (i);
          if (dateUnit != null) {
            Date date = dateUnit.makeDate (((Number) value).doubleValue());
            value = DateFormatter.formatDate (date, DATE_TIME_FMT);
          } // if
          buf.append (String.format ("Coord[%d]=%s ", i, value));

        } // for
        
      } // if

      valueMap.put (axisHeader, buf.toString());

    } // for

    // Get variables
    // -------------
    buf.setLength (0);
    reader.getVariablesForSystem (system).forEach (name -> buf.append (name + " "));
    valueMap.put ("Variables", buf.toString());

    return (valueMap);

  } // getCoordSystemSummary

  /////////////////////////////////////////////////////////////////

  /**
   * Gets Common Data Model (CDM) style coordinate system information 
   * including level and time index information.  
   *
   * @param reader the earth data reader object to use.
   * 
   * @return the list of maps of information key to value, which may be 
   * empty if the file contains no CDM coordinate systems.
   * 
   * @throws IOException if an error occurred reading the coordinate systems.
   */
  public List<Map<String, String>> getCoordinateSummary (
    EarthDataReader reader
  ) throws IOException {

    var list = new ArrayList<Map<String, String>>();
    for (var system : reader.getCoordinateSystems()) 
      list.add (getCoordSystemSummary (reader, system));

    return (list);

  } // getCoordinateSummary

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the global file information.
   *
   * @param reader the reader object to use.
   *
   * @return the map of information key to value.
   */
  public Map<String, String> getGlobalSummary (
    EarthDataReader reader
  ) {

    var valueMap = new LinkedHashMap<String, String>();

    // Add data source info
    // --------------------
    EarthDataInfo info = reader.getInfo();
    if (info instanceof SatelliteDataInfo) {
      SatelliteDataInfo satInfo = (SatelliteDataInfo) info;
      valueMap.put ("Satellite", 
        MetadataServices.format (satInfo.getSatellite(), ", "));
      valueMap.put ("Sensor", 
        MetadataServices.format (satInfo.getSensor(), ", "));
    } // if
    else {
      valueMap.put ("Data source", 
        MetadataServices.format (info.getSource(), ", "));
    } // else

    // Add single time info
    // --------------------
    if (info.isInstantaneous()) {
      Date startDate = info.getStartDate();
      valueMap.put ("Date", DateFormatter.formatDate (startDate, DATE_FMT));
      valueMap.put ("Time", DateFormatter.formatDate (startDate, TIME_FMT));
      valueMap.put ("Scene time", reader.getSceneTime());
    } // if

    // Add time range info
    // -------------------
    else {
      Date startDate = info.getStartDate();
      Date endDate = info.getEndDate();
      String startDateString = DateFormatter.formatDate (startDate, DATE_FMT);
      String endDateString = DateFormatter.formatDate (endDate, DATE_FMT);
      String startTimeString = DateFormatter.formatDate (startDate, TIME_FMT);
      String endTimeString = DateFormatter.formatDate (endDate, TIME_FMT);
      if (startDateString.equals (endDateString)) {
        valueMap.put ("Date", startDateString);
        valueMap.put ("Start time", startTimeString);
        valueMap.put ("End time", endTimeString);
      } // if
      else {
        valueMap.put ("Start date", startDateString);
        valueMap.put ("Start time", startTimeString);
        valueMap.put ("End date", endDateString);
        valueMap.put ("End time", endTimeString);
      } // else
    } // else

    // Add earth transform info
    // ------------------------
    EarthTransform trans = info.getTransform();
    valueMap.put ("Projection type", (trans == null ? "Unknown" : 
      trans.describe()));
    valueMap.put ("Transform ident", (trans == null ? "null" :
      trans.getClass().getName()));



// TODO: It would be useful here if CDM grid map projections could also
// have their info printed, similar to the EarthPlotInfo legend.  Search the
// code for getSystemName() for all places that may need this functionality.



    // Add map projection info
    // -----------------------    
    if (trans instanceof MapProjection) {
      MapProjection map = (MapProjection) trans;
      valueMap.put ("Map projection", map.getSystemName());
      String affineVals = "";      
      DecimalFormat form = new DecimalFormat ("0.##");
      double[] matrix = new double[6];
      map.getAffine().getMatrix (matrix);
      for (int i = 0; i < 6; i++)
        affineVals += form.format (matrix[i]) + " ";
      valueMap.put ("Map affine", affineVals);
      valueMap.put ("Spheroid", map.getSpheroidName());
    } // if

    // Add other info
    // --------------
    valueMap.put ("Origin",
      MetadataServices.format (info.getOrigin(), ", "));
    valueMap.put ("Format", reader.getDataFormat());
    valueMap.put ("Reader ident", reader.getClass().getName());

    return (valueMap);

  } // getGlobalSummary

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the variable data type name.
   *
   * @param var the data variable.
   * 
   * @return a user-friendly type name.
   */
  private String getType (
    DataVariable var
  ) {

    // Find appropriate type name
    // --------------------------
    Class dataClass = var.getDataClass();
    String typeName = null;
    for (int i = 0; i < JAVA_TYPES.length && typeName == null; i++) {
      if (JAVA_TYPES[i].equals (dataClass))
        typeName = JAVA_NAMES[i];
    } // for

    // Check for unknown type
    // ----------------------
    if (typeName == null) return ("Unknown");

    // Add unsigned prefix
    // -------------------
    if (var.getUnsigned()) typeName = "u" + typeName;
    return (typeName);

  } // getType

  /////////////////////////////////////////////////////////////////

  /**
   * A <code>SummaryTable</code> holds the output of a summary 
   * that needs to be formatted as a series of rows and columns
   * in a table.
   */
  public static class SummaryTable {

    public String[] columnNames;
    public List<String[]> rowList;

  } // SummaryTable

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the variable information.
   *
   * @param reader the reader object to use.
   *
   * @return the summary table of variable information.
   *
   * @throws IOException if an error occurred reading the variable data.
   */
  public SummaryTable getVariableSummary (
    EarthDataReader reader
  ) throws IOException {

    var table = new SummaryTable();
    table.columnNames = new String[] {"Variable", "Type", "Dimensions", "Units", "Scale", "Offset"};
    table.rowList = new ArrayList<>();
    
    // Get variable info
    // -----------------
    int vars = reader.getVariables();
    for (int i = 0; i < vars; i++) {

      // Get preview
      // -----------
      DataVariable var = null;
      try { var = reader.getPreview (i); }
      catch (Exception e) {
        e.printStackTrace();
        continue;
      } // catch
      
      // Create new info array
      // ---------------------
      String[] infoArray = new String[6];
      int index = 0;
      infoArray[index++] = var.getName();
      infoArray[index++] = getType (var);
      
      // Get dimensions
      // --------------
      int[] dims = var.getDimensions();
      String dimValues = "";
      for (int dim = 0; dim < dims.length; dim++)
        dimValues += dims[dim] + (dim < dims.length-1 ? "x" : "");
      infoArray[index++] = dimValues;

      // Get units
      // ---------
      String units = var.getUnits ();
      if (units.equals ("")) units = "-";
      infoArray[index++] = units;

      // Get scale, offset
      // -----------------
      double[] scaling = var.getScaling();
      String scale, offset;
      if (scaling == null) {
        scale = "-";
        offset = "-";
      } // if
      else {
        DecimalFormat fmt = new DecimalFormat ("0.######");
        scale = fmt.format (scaling[0]);
        offset = fmt.format (scaling[1]);
      } // else
      infoArray[index++] = scale;
      infoArray[index++] = offset;

      table.rowList.add (infoArray);

    } // for

    return (table);

  } // getVariableSummary

  /////////////////////////////////////////////////////////////////

} // ReaderSummaryProducer class


