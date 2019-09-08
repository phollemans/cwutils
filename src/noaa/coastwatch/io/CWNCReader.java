////////////////////////////////////////////////////////////////////////
/*

     File: CWNCReader.java
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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.io.NCReader;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.Line;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/** 
 * The <code>CWNCReader</code> class reads Java NetCDF accessible
 * datasets and uses the CoastWatch HDF metadata conventions to parse
 * the attribute and variable data.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class CWNCReader 
  extends NCReader {

  // Constants
  // ---------

  /** The data format description. */
  private static final String DATA_FORMAT = "CoastWatch HDF/NC";

  /** Number of milliseconds in a day. */
  private static final long MSEC_PER_DAY = (1000L * 3600L * 24L);

  /** List of extra map projection metadata. */
  private static final List MAP_METADATA = Arrays.asList (
    new String[] {"region_code", "region_name"});

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { 

    return (DATA_FORMAT + " version " + getMetaVersion());

  } // getDataFormat

  ////////////////////////////////////////////////////////////

  /**
   * Gets the CoastWatch HDF metadata version for this dataset.
   *
   * @return the metadata version.  If no metadata attribute can be found,
   * the metadata version is assumed to be 2.3.
   */
  public double getMetaVersion () {

    // Get metadata attribute
    // ----------------------
    try {
      String att = 
        dataset.findGlobalAttribute ("cwhdf_version").getStringValue();
      return (Double.parseDouble (att));
    } // try
    catch (Exception e) {
      return (2.3);
    } // catch

  } // getMetaVersion

  ////////////////////////////////////////////////////////////
  
  /** Gets the earth data info object. */
  private EarthDataInfo getGlobalInfo () {

    // Get simple attributes
    // ---------------------
    String sat = (String) getAttribute ("satellite");
    String sensor = (String) getAttribute ("sensor");
    String source = (String) getAttribute ("data_source");
    String origin = (String) getAttribute ("origin");
    if (origin == null) origin = "Unknown";
    String history = (String) getAttribute ("history");
    if (history == null) history = "";

    // Get time period list and transform
    // ----------------------------------
    List<TimePeriod> periodList = getPeriodList();
    EarthTransform transform = getTransform();

    // Create info object
    // ------------------
    EarthDataInfo info;
    if (sat == null && sensor == null && source == null)
      source = "Unknown";
    if (source != null) {
      info = new EarthDataInfo (source, periodList, transform, 
        origin, history);
    } // if
    else {
      if (sat == null) sat = "Unknown";
      if (sensor == null) sensor = "Unknown";
      info = new SatelliteDataInfo (sat, sensor, periodList, transform, 
        origin, history);
    } // else

    return (info);

  } // getGlobalInfo

  ////////////////////////////////////////////////////////////

  /** Gets the list of time periods. */
  private List<TimePeriod> getPeriodList () {

    // Read data
    // ---------
    int[] passDateArray = (int[]) getAttributeAsArray ("pass_date");
    double[] startTimeArray = (double[]) getAttributeAsArray ("start_time");
    int periods = passDateArray.length;
    double[] extentArray = (double[]) getAttributeAsArray ("temporal_extent");
    if (extentArray == null) extentArray = new double[periods];
    if (passDateArray.length != startTimeArray.length ||
      startTimeArray.length != extentArray.length)
      throw new RuntimeException ("Date, time, and extent attributes have " + 
        "different lengths");

    // Create period list
    // ------------------
    List<TimePeriod> periodList = new ArrayList<>();
    for (int i = 0; i < periods; i++) {
      long msec = (long) passDateArray[i] * MSEC_PER_DAY;
      msec += (long) (startTimeArray[i] * 1000L);
      periodList.add (new TimePeriod (new Date (msec), 
        (long) extentArray[i] * 1000L));
    } // for

    return (periodList);

  } // getPeriodList

  ////////////////////////////////////////////////////////////

  /** Gets the earth transform information. */
  private EarthTransform getTransform () {

    // Get projection type
    // -------------------
    double version = getMetaVersion();
    String type;
    if (version >= 3) type = (String) getAttribute ("projection_type");
    else type = MapProjection.DESCRIPTION;

    // Read map projection
    // -------------------
    if (type.equals (MapProjection.DESCRIPTION)) {

      // Get GCTP parameters
      // -------------------
      int system = ((Integer) getAttribute ("gctp_sys")).intValue();
      int zone = ((Integer) getAttribute ("gctp_zone")).intValue();
      double[] parameters = (double[]) getAttribute ("gctp_parm");
      int spheroid = ((Integer) getAttribute ("gctp_datum")).intValue();

      // Get affine transform
      // --------------------
      AffineTransform affine;
      double[] matrix = (double[]) getAttribute ("et_affine");
      if (version >= 3)
        affine = new AffineTransform (matrix);
      else {
        double[] newMatrix = new double[6];
        newMatrix[0] = matrix[1];
        newMatrix[1] = matrix[3];
        newMatrix[2] = matrix[0];
        newMatrix[3] = matrix[2];
        newMatrix[4] = matrix[0] + matrix[1] + matrix[4];
        newMatrix[5] = matrix[2] + matrix[3] + matrix[5];
        affine = new AffineTransform (newMatrix);
      } // else

      // Get dimensions
      // --------------
      int rows = ((Integer) getAttribute ("rows")).intValue();
      int cols = ((Integer) getAttribute ("cols")).intValue();

      // Create map projection
      // ---------------------
      EarthTransform2D trans;
      try {
        trans = MapProjectionFactory.getInstance().create (system, zone, 
          parameters, spheroid, new int[] {rows, cols}, affine);
      } // try
      catch (NoninvertibleTransformException e) {
        throw new RuntimeException ("Got noninvertible transform error " +
                                    "initializing map projection");
      } // catch

      // Attach metadata
      // ---------------
      //      getAttributes (MAP_METADATA, trans.getMetadataMap());

      return (trans);

    } // if

    // Unknown projection
    // ------------------
    else return (null);

  } // getTransform

  ////////////////////////////////////////////////////////////

  /** Gets the list of variable names. */
  private String[] getVariableNames () {

    // Get variable names
    // ------------------
    List variableList = dataset.getReferencedFile().getVariables();
    List nameList = new ArrayList();
    int[] dims = info.getTransform().getDimensions();
    for (Iterator iter = variableList.iterator(); iter.hasNext(); ) {
      Variable var = (Variable) iter.next();

      // Check for coordinate variable
      // -----------------------------
      if (var.isCoordinateVariable()) continue;

      // Check for correct dimensions
      // ----------------------------
      if (var.getRank() == 2) {
        if (var.getDimension (0).getLength() != dims[0] ||
          var.getDimension (1).getLength() != dims[1]) continue;
      } // if

      // Add name to list
      // ----------------
      nameList.add (var.getShortName());

    } // for

    return ((String[]) nameList.toArray (new String[]{}));

  } // getVariableNames

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new reader from the specified file. 
   * 
   * @param name the file name or URL to read.
   *
   * @throws IOException if an error occurred reading the metadata.
   */
  public CWNCReader (
    String name
  ) throws IOException {

    super (name);

  } // CWNCReader constructor

  ////////////////////////////////////////////////////////////

  @Override
  protected void initializeReader () throws IOException {
  
    info = getGlobalInfo();
    variables = getVariableNames();
  
  } // initializeReader

  ////////////////////////////////////////////////////////////

  protected DataVariable getPreviewImpl (
    int index
  ) throws IOException { 

    // Access variable
    // ---------------
    Variable var = dataset.getReferencedFile().findVariable (variables[index]);
    if (var == null)
      throw new IOException ("Cannot access variable at index " + index);

    // Get variable info
    // -----------------
    int varDims[] = var.getShape();
    String name = var.getShortName();
    int rank = var.getRank();
    Class varClass = var.getDataType().getPrimitiveClassType();
    boolean isUnsigned = var.isUnsigned();
    
    // Create fake data array
    // ----------------------
    Object data = Array.newInstance (varClass, 1);

    // Get data strings
    // ----------------
    String longName = var.getDescription();
    if (longName == null) longName = "";
    String units = var.getUnitsString();
    if (units == null) units = "";
    String formatStr = (String) getAttribute (var, "format");
    if (formatStr == null) formatStr = "";

    // Convert units
    // -------------
    units = units.trim();
    if (units.equals ("temp_deg_c")) units = "celsius";
    else if (units.equals ("albedo*100%")) units = "percent";
    else if (units.equals ("-")) units = "";

    // Get calibration
    // ---------------
    double[] calInfo = new double[4];
    int[] calType = new int[1];
    double[] scaling;
    try {
      Double scale = (Double) getAttribute (var, "scale_factor");
      Double offset = (Double) getAttribute (var, "add_offset");
      scaling = new double[] {scale.doubleValue(), offset.doubleValue()};
    } // try
    catch (Exception e) {
      scaling = null;
    } // catch

    // Get missing value
    // -----------------
    Object missing;
    try {
      missing = getAttribute (var, "_FillValue");
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
          throw new UnsupportedEncodingException("Unsupported variable class");
      } // if
    } // if

    // Try getting fraction digits attribute
    // -------------------------------------
    int digits = -1;
    try { 
      digits = ((Integer) getAttribute (var, "fraction_digits")).intValue();
    } // try
    catch (Exception e) { }

    // Try using format string
    // -----------------------         
    if (digits == -1 && !formatStr.equals ("")) {
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

    // Try getting navigation transform
    // --------------------------------
    AffineTransform nav = null;
    if (rank == 2) {
      try { 
        double[] matrix  = (double[]) getAttribute (var, "nav_affine");
        nav = new AffineTransform (matrix);
      } // try
      catch (Exception e) { }
    } // if

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
      if (nav != null) ((Grid) dataVar).setNavigation (nav);
    } // else if 
    else {
      throw new UnsupportedEncodingException ("Unsupported rank = " + rank +
        " for " + name);
    } // else
    dataVar.setUnsigned (isUnsigned);
      
    // Get attributes
    // --------------
    //getAttributes (dataVar.getMetadataMap(), false);

    // Return the new variable
    // -----------------------
    return (dataVar);

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
    Variable var = dataset.getReferencedFile().findVariable (variables[index]);
    if (var == null)
      throw new IOException ("Cannot access variable at index " + index);

    // Read data
    // ---------
    Object data = var.read().getStorage();

    // Return variable
    // ---------------
    dataVar.setData (data);
    return (dataVar);

  } // getActualVariable

  ////////////////////////////////////////////////////////////

} // CWNCReader class

////////////////////////////////////////////////////////////////////////

