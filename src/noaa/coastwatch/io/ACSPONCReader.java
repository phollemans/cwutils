////////////////////////////////////////////////////////////////////////
/*
     FILE: ACSPONCReader.java
  PURPOSE: Reads ACSPO data through the NetCDF 4 interface.
   AUTHOR: X. Liu
     DATE: 2011/01/25
  CHANGES: 

  CoastWatch Software Library and Utilities
  Copyright 1998-2011, USDOC/NOAA/NESDIS CoastWatch

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
import ucar.nc2.dataset.*;
import ucar.ma2.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/** 
 * The <code>ACSPONCReader</code> class reads Java NetCDF accessible
 * datasets and uses the ACSPO metadata conventions to parse
 * the attribute and variable data.
 */
public class ACSPONCReader
  extends NCReader {

  // Constants
  // ---------

  /** The data format description. */
  private String DATA_FORMAT;
  
  /** Swath polynomial size in kilometers. */
  public static final double SWATH_POLY_SIZE = 100.0;

  /** Number of milliseconds in a day. */
  private static final long MSEC_PER_DAY = (1000L * 3600L * 24L);
  
  /** Number of milliseconds in an hour. */
  private static final long MSEC_PER_HOUR = (1000L * 3600L);

  /** The latitude variable name. */
  private static final String LAT_VAR = "latitude";

  /** The longitude variable name. */
  private static final String LON_VAR = "longitude";

  /** The prototype variable name. */
  private static final String PROTO_VAR = "latitude";

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
  
  /** Gets the Earth data info object. */
  private EarthDataInfo getGlobalInfo () {

    // Get simple attributes
    // ---------------------
    String sat = (String) getAttribute ("SATELLITE");
    String processor = (String) getAttribute ("PROCESSOR");
    String sensor = "AVHRR";
    String origin = "NOAA/NESDIS";
    DATA_FORMAT = processor + " NC4";
    String created = (String) getAttribute ("CREATED");
    String history = "[" + processor + "] " + created;

    // Get time period list and transform
    // ----------------------------------
    List periodList = getPeriodList();
    EarthTransform transform = getTransform();

    // Create info object
    // ------------------
    EarthDataInfo info = new SatelliteDataInfo (sat, sensor, periodList, transform, 
        origin, history); // else

    return (info);

  } // getGlobalInfo

  ////////////////////////////////////////////////////////////

  /** Gets the list of time periods. */
  private List getPeriodList () {

    // Read data
    // ---------
	int startYear = ((Number) getAttribute ("START_YEAR")).intValue();
	int startDay = ((Number) getAttribute ("START_DAY")).intValue();
	double startTime = 
	  ((Number) getAttribute ("START_TIME")).doubleValue();
	int endYear = ((Number) getAttribute ("END_YEAR")).intValue();
	int endDay = ((Number) getAttribute ("END_DAY")).intValue();
	double endTime = 
		((Number) getAttribute ("END_TIME")).doubleValue();
	
    // Create date objects
    // -------------------
    Calendar cal = new GregorianCalendar (startYear, 0, 1, 0, 0, 0);
    cal.setTimeZone (TimeZone.getTimeZone ("GMT"));
    Date startDate = new Date (cal.getTimeInMillis() + 
      (startDay-1)*MSEC_PER_DAY + (long) (startTime*MSEC_PER_HOUR));
    cal = new GregorianCalendar (endYear, 0, 1, 0, 0, 0);
    cal.setTimeZone (TimeZone.getTimeZone ("GMT"));
    Date endDate = new Date (cal.getTimeInMillis() + 
      (endDay-1)*MSEC_PER_DAY + (long) (endTime*MSEC_PER_HOUR));

    // Create period list
    // ------------------
    List periodList = new ArrayList();
    periodList.add (new TimePeriod (startDate, endDate.getTime() - 
      startDate.getTime()));

    return (periodList); 
	   
  } // getPeriodList

  ////////////////////////////////////////////////////////////

  /** Gets the Earth transform information. */
  private EarthTransform getTransform () {
	   
    // Create swath
    // ------------
    EarthTransform trans = null;
    DataVariable lat = null, lon = null;
    try {
      lat = getVariable (LAT_VAR);
      int cols = lat.getDimensions()[Grid.COLS];
      lon = getVariable (LON_VAR);
      if (dataProjection) {
        trans = new DataProjection (lat, lon);
      } // if
      else {
        trans = new SwathProjection (lat, lon, SWATH_POLY_SIZE, 
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

    return (trans);
	   
  } // getTransform

  ////////////////////////////////////////////////////////////

  /** Gets the list of variable names. */
  private String[] getVariableNames () {

    // Get variable names
    // ------------------
    List variableList = dataset.getReferencedFile().getVariables();
    List nameList = new ArrayList();
    //int[] dims = info.getTransform().getDimensions();
    for (Iterator iter = variableList.iterator(); iter.hasNext(); ) {
      Variable var = (Variable) iter.next();

      // Check for coordinate variable
      // -----------------------------
      if (var.isCoordinateVariable()) continue;

      // Check for correct dimensions
      // ----------------------------
      if (var.getRank() != 2) {
        //if (var.getDimension (0).getLength() != dims[0] ||
        //  var.getDimension (1).getLength() != dims[1]) 
    	continue;
    	  
      } // if

      // Add name to list
      // ----------------
      nameList.add (var.getName());

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
  public ACSPONCReader (
    String name
  ) throws IOException {

    super (name);
    variables = getVariableNames();
    info = getGlobalInfo();

  } // CWNCReader constructor

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
    String name = var.getName();
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

} // ACSPONCReader class

////////////////////////////////////////////////////////////////////////

