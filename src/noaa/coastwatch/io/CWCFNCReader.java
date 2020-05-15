////////////////////////////////////////////////////////////////////////
/*

     File: CWCFNCReader.java
   Author: X. Liu
     Date: 2012/06/27

  CoastWatch Software Library and Utilities
  Copyright (c) 2012 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;

import java.util.logging.Logger;
import java.util.logging.Level;

/** 
 * <p>The <code>CWCFNCReader</code> class reads Java NetCDF accessible
 * datasets and uses the CoastWatch HDF metadata conventions to parse
 * the attribute and variable data.  Only map projected data is supported.
 * The presence of the following global attributes is used to recognize
 * the file as having CoastWatch-specific metadata (see the user's guide
 * metadata appendix for more details on each attribute):</p>
 *
 * <ul>
 *
 *   <li><code>cw:cwhdf_version</code> (string, optional) - The CoastWatch
 *   HDF metadata version number, defaults to 2.3.  This attribute is highly
 *   recommended so that the <code>cw:et_affine</code> attribute is interpreted
 *   correctly. If in doubt, use 3.4.</li>
 *
 *   <li><code>cw:satellite</code> (string, optional) - The satellite name,
 *   default is to use the CF <code>source</code> attribute.</li>
 *
 *   <li><code>cw:sensor</code> (string, optional) - The sensor name,
 *   default is to use the CF <code>source</code> attribute.</li>
 *
 *   <li><code>cw:origin</code> (string, optional) - The data origin, default
 *   is to use the CF <code>institution</code> attribute.</li>
 *
 *   <li><code>cw:pass_date</code> (int, REQUIRED) - The single value or array of dates
 *   of data recording (days since Jan 1, 1970).</li>
 *
 *   <li><code>cw:start_time</code> (double, REQUIRED) - The single value or array of
 *   start times of data recording (seconds since 00:00:00 UTC).</li>
 *
 *   <li><code>cw:temporal_extent</code> (double, optional) - The single
 *   value or array of durations in seconds of data recording.</li>
 *
 *   <li><code>cw:gctp_sys</code> (int, REQUIRED) - The GCTP projection
 *   system code.</li>
 *
 *   <li><code>cw:gctp_zone</code> (int, REQUIRED) - The GCTP UTM projection
 *   zone code (can be zero).</li>
 *
 *   <li><code>cw:gctp_parm</code> (double array, REQUIRED) - The array of
 *   GCTP projection parameters (15).</li>
 *
 *   <li><code>cw:gctp_datum</code> (int, REQUIRED) - The GCTP datum
 *   code.</li>
 *
 *   <li><code>cw:et_affine</code> (double array, REQUIRED) - The map projection
 *   affine transform.</li>
 *
 * </ul>
 *
 * <p>On a per-variable basis, the <code>cw:fraction_digits</code> and
 * <code>cw:nav_affine</code> attributes are also used if found, but are not
 * required.  Note that if any required or optional attributes are found to
 * have an incorrect data type, the file will not be recognized.</p>
 *
 * @author Xiaoming Liu
 * @since 3.3.0
 */
public class CWCFNCReader
  extends NCReader {

  private static final Logger LOGGER = Logger.getLogger (CWCFNCReader.class.getName());

  // Constants
  // ---------

  /** The data format description. */
  private static final String DATA_FORMAT = "CoastWatch HDF/NC/CF";

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
        dataset.findGlobalAttribute ("cw:cwhdf_version").getStringValue();
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
    String sat = (String) getAttribute ("cw:satellite");
    String sensor = (String) getAttribute ("cw:sensor");
    String source = (String) getAttribute ("source");
    String origin = (String) getAttribute ("cw:origin");
    String institution = (String) getAttribute ("institution");
    if (origin == null) origin = institution;
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
    int[] passDateArray = (int[]) getAttributeAsArray ("cw:pass_date");
    double[] startTimeArray = (double[]) getAttributeAsArray ("cw:start_time");
    int periods = passDateArray.length;
    double[] extentArray = (double[]) getAttributeAsArray ("cw:temporal_extent");
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
    //if (version >= 3) type = (String) getAttribute ("cw:projection");
    if (version >= 3) type = "mapped";
    else type = MapProjection.DESCRIPTION;

    // Read map projection
    // -------------------
    if (type.equals (MapProjection.DESCRIPTION)) {

      // Get GCTP parameters
      // -------------------
      int system = ((Integer) getAttribute ("cw:gctp_sys")).intValue();
      int zone = ((Integer) getAttribute ("cw:gctp_zone")).intValue();
      double[] parameters = (double[]) getAttribute ("cw:gctp_parm");
      int spheroid = ((Integer) getAttribute ("cw:gctp_datum")).intValue();

      // Get affine transform
      // --------------------
      AffineTransform affine;
      double[] matrix = (double[]) getAttribute ("cw:et_affine");
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

      // Create transform
      // ----------------
      EarthTransform2D trans = null;
      try {
      
          // Search for the first variable with rank 2
          // -----------------------------------------
          DataVariable var = null;
          int varCount = getVariables();
          for (int i = 0; i < varCount && var == null; i++) {
            DataVariable preview = getPreview (i);
            if (preview.getRank() == 2) var = preview;
          } // for
          if (var == null)
            throw new RuntimeException ("No prototype variable found for dimensions");

          // Create a map projection
          // -----------------------
          int[] dims = var.getDimensions();
	  trans = MapProjectionFactory.getInstance().create (system, zone,
            parameters, spheroid, dims, affine);

        } // try
      
        catch (Exception e) {
          LOGGER.log (Level.WARNING, "Problems encountered using earth location data", e);
        } // catch

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
    for (Iterator iter = variableList.iterator(); iter.hasNext(); ) {
      Variable var = (Variable) iter.next();

      // Check for coordinate variable
      // -----------------------------
      if (var.isCoordinateVariable()) continue;

      // Check for correct dimensions
      // ----------------------------
      String varName = var.getShortName();
      if (var.getRank() >= 2) {

        // Detect time bounds variable
        // ---------------------------
        List<Dimension> dims = var.getDimensions();
        Dimension dim0 = dims.get (0);
        Dimension dim1 = dims.get (1);
        boolean isTimeBounds = (
          var.getRank() == 2 &&
          (dim0.getLength() == 1 || dim1.getLength() == 1) &&
          (dim0.getShortName().matches (".*time.*") || dim1.getShortName().matches (".*time.*"))
        );

        // Skip time bounds variable in list
        // ---------------------------------
        if (!isTimeBounds)
          nameList.add (var.getShortName());

      } // if
      
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
  public CWCFNCReader (
    String name
  ) throws IOException {

    super (name);

  } // CWCFNCReader constructor

  ////////////////////////////////////////////////////////////

  @Override
  protected void initializeReader () throws IOException {
  
    variables = getVariableNames();
    info = getGlobalInfo();
    if (info == null) throw new RuntimeException ("No global info found");
  
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
    double[] scaling;
    try {
      Double scale = (Double) getAttribute (var, "scale_factor");
      Double offset = (Double) getAttribute (var, "add_offset");
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
      scaling = new double[] {1.0, 0.0};
    } // catch

    // Get missing value
    // -----------------
    Object missing;
    try {
      //missing = getAttribute (var, "_FillValue");
      missing = getAttribute (var, "missing_value");
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
      digits = ((Integer) getAttribute (var, "cw:fraction_digits")).intValue();
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
        double[] matrix  = (double[]) getAttribute (var, "cw:nav_affine");
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
    else if (rank >= 2) {
      dataVar = new Grid (name, longName, units, varDims[rank-2], varDims[rank-1],
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

} // CWCFNCReader class

////////////////////////////////////////////////////////////////////////


