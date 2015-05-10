////////////////////////////////////////////////////////////////////////
/*
     FILE: CWOpendapReader.java
  PURPOSE: Reads CoastWatch-style data through the OPeNDAP interface.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/08
  CHANGES: 2006/11/03, PFH, changed getPreview(int) to getPreviewImpl(int)
           2008/02/18, PFH, modified to use opendap.dap classes

  CoastWatch Software Library and Utilities
  Copyright 2006-2008, USDOC/NOAA/NESDIS CoastWatch

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
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import noaa.coastwatch.io.OpendapReader;
import noaa.coastwatch.tools.cwinfo;
import noaa.coastwatch.tools.cwstats;
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
import opendap.dap.BaseType;
import opendap.dap.DAS;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DConnect;
import opendap.dap.DDS;
import opendap.dap.DVector;
import opendap.dap.DataDDS;
import opendap.dap.NoSuchAttributeException;
import opendap.dap.NoSuchVariableException;
import opendap.dap.PrimitiveVector;
import opendap.dap.Server.InvalidParameterException;

/** 
 * The <code>CWOpendapReader</code> class reads OPeNDAP
 * accessible datasets and uses the CoastWatch HDF metadata
 * conventions to parse the attribute and variable data.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class CWOpendapReader 
  extends OpendapReader {

  // Constants
  // ---------

  /** The data format description. */
  private static final String DATA_FORMAT = "CoastWatch HDF/OPeNDAP";

  /** Number of milliseconds in a day. */
  private static final long MSEC_PER_DAY = (1000L * 3600L * 24L);
  
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
    String att = (String) rawMetadataMap.get ("cwhdf_version");
    if (att != null) return (Double.parseDouble (att));
    else return (2.3);

  } // getMetaVersion

  ////////////////////////////////////////////////////////////
  
  /** Gets the Earth data info object. */
  private EarthDataInfo getGlobalInfo () {

    // Get simple attributes
    // ---------------------
    String sat = (String) rawMetadataMap.get ("satellite");
    String sensor = (String) rawMetadataMap.get ("sensor");
    String source = (String) rawMetadataMap.get ("data_source");
    String origin = (String) rawMetadataMap.get ("origin");
    if (origin == null) origin = "unknown";
    String history = (String) rawMetadataMap.get ("history");
    if (history == null) history = "";

    // Get time period list and transform
    // ----------------------------------
    List periodList = getPeriodList();
    EarthTransform transform = getTransform();

    // Create info object
    // ------------------
    EarthDataInfo info;
    if (sat == null && sensor == null && source == null)
      source = "unknown";
    if (source != null) {
      info = new EarthDataInfo (source, periodList, transform, 
        origin, history);
    } // if
    else {
      if (sat == null) sat = "unknown";
      if (sensor == null) sensor = "unknown";
      info = new SatelliteDataInfo (sat, sensor, periodList, transform, 
        origin, history);
    } // else

    return (info);

  } // getGlobalInfo

  ////////////////////////////////////////////////////////////

  /** Gets the list of time periods. */
  private List getPeriodList () {

    // Get period data
    // ---------------
    int[] passDateArray = (int[]) rawMetadataMap.get ("pass_date");
    int periods = passDateArray.length;
    double[] startTimeArray = (double[]) rawMetadataMap.get ("start_time");
    double[] extentArray = (double[]) rawMetadataMap.get ("temporal_extent");
    if (extentArray == null) extentArray = new double[periods];
    if (startTimeArray.length != periods || extentArray.length != periods)
      throw new RuntimeException ("Date, time, and extent attributes have " + 
        "different lengths");

    // Create period list
    // ------------------
    List periodList = new ArrayList();
    for (int i = 0; i < periods; i++) {
      long msec = (long) passDateArray[i] * MSEC_PER_DAY;
      msec += (long) (startTimeArray[i] * 1000L);
      periodList.add (new TimePeriod (new Date (msec), 
        (long) extentArray[i] * 1000L));
    } // for

    return (periodList);

  } // getPeriodList

  ////////////////////////////////////////////////////////////

  /** Gets the Earth transform information. */
  private EarthTransform getTransform () {

    // Get projection type
    // -------------------
    double version = getMetaVersion();
    String type;
    if (version >= 3) type = (String) rawMetadataMap.get ("projection_type");
    else type = MapProjection.DESCRIPTION;

    // Read map projection
    // -------------------
    if (type.equals (MapProjection.DESCRIPTION)) {

      // Get GCTP parameters
      // -------------------
      int system = ((int[]) rawMetadataMap.get ("gctp_sys"))[0];
      int zone = ((int[]) rawMetadataMap.get ("gctp_zone"))[0];
      double[] parameters = (double[]) rawMetadataMap.get ("gctp_parm");
      int spheroid = ((int[]) rawMetadataMap.get ("gctp_datum"))[0];

      // Get affine transform
      // --------------------
      AffineTransform affine;
      double[] matrix = (double[]) rawMetadataMap.get ("et_affine");
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
      int rows = ((int[]) rawMetadataMap.get ("rows"))[0];
      int cols = ((int[]) rawMetadataMap.get ("cols"))[0];

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
    List nameList = new ArrayList();
    for (Enumeration en = dds.getVariables(); en.hasMoreElements();) {
      BaseType baseType = (BaseType) en.nextElement();
      if (baseType instanceof DVector)
        nameList.add (baseType.getName());
    } // for

    return ((String[]) nameList.toArray (new String[]{}));

  } // getVariableNames

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new reader using the specified URL.
   *
   * @param url the network location.
   *
   * @throws IOException if the an error occurred accessing the dataset.
   */
  public CWOpendapReader (
    String url
  ) throws IOException {

    super (url);
    info = getGlobalInfo();
    variables = getVariableNames();

  } // CWOpendapReader constructor

  ////////////////////////////////////////////////////////////

  protected DataVariable getPreviewImpl (
    int index
  ) throws IOException { 

    // Get variable metadata
    // ---------------------
    Map map;
    try { 
      map = getAttributeMap (das.getAttributeTable (variables[index]), null);
    } // try
    catch (NoSuchAttributeException e) {
      throw new IOException ("Error accessing metadata for variable " + 
        variables[index]);
    } // catch

    // Get variable info
    // -----------------
    DArray var;
    try { var = (DArray) dds.getVariable (variables[index]); }
    catch (NoSuchVariableException e) { 
      throw new IOException ("Error accessing variable " + variables[index]);
    } // catch
    String name = var.getName();
    Class varClass = getPrimitiveClassType (var);
    if (varClass == null)
      throw new IOException ("Cannot determine primitive class for " + name);
    boolean isUnsigned = isUnsigned (var);

    // Get dimension info
    // ------------------
    int rank = var.numDimensions();
    if (rank > 2) {
      throw new UnsupportedEncodingException ("Unsupported rank = " + rank +
        " for " + name);
    } // if
    int[] varDims = new int[rank];
    for (int i = 0; i < rank; i++) {
      try { varDims[i] = var.getDimension (i).getSize(); }
      catch (InvalidParameterException e) {
        throw new IOException ("Error accessing dimension size for " + name);
      } // catch
    } // for

    // Create fake data array
    // ----------------------
    Object data = Array.newInstance (varClass, 1);

    // Get data strings
    // ----------------
    String longName = (String) map.get ("long_name");
    if (longName == null) longName = "";
    String units = (String) map.get ("units");
    if (units == null) units = "";
    String formatStr = (String) map.get ("format");
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
      double scale = ((double[]) map.get ("scale_factor"))[0];
      double offset = ((double[]) map.get ("add_offset"))[0];
      scaling = new double[] {scale, offset};
    } // try
    catch (Exception e) {
      scaling = null;
    } // catch

    // Get missing value
    // -----------------
    Object missing;
    try {
      missing = Array.get (map.get ("_FillValue"), 0);
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
      digits = ((int[]) map.get ("fraction_digits"))[0];
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
        double[] matrix  = (double[]) map.get ("nav_affine");
        nav = new AffineTransform (matrix);
      } // try
      catch (Exception e) { }
    } // if

    // Create variable
    // ---------------
    DataVariable dataVar = null;
    if (rank == 1) {
      dataVar = new Line (name, longName, units, varDims[0], data, format,
        scaling, missing);
    } // if
    else if (rank == 2) {
      dataVar = new Grid (name, longName, units, varDims[0], varDims[1], 
        data, format, scaling, missing);
      if (nav != null) ((Grid) dataVar).setNavigation (nav);
    } // else if 
    dataVar.setUnsigned (isUnsigned);
      
    // Get attributes
    // --------------
    //getAttributes (dataVar.getMetadataMap(), false);

    // Return the new variable
    // -----------------------
    return (dataVar);

  } // getPreviewImpl

  ////////////////////////////////////////////////////////////

  public DataVariable getVariable (
    int index
  ) throws IOException {

    // Get a variable preview
    // ----------------------
    DataVariable var = getPreview (index);

    // Read data
    // ---------
    try {
      DataDDS dds = connect.getData ("?" + var.getName(), null);
      PrimitiveVector vector = 
        ((DVector) dds.getVariable (var.getName())).getPrimitiveVector();
      Object data = vector.getInternalStorage();
      var.setData (data);
    } // try
    catch (Exception e) {
      throw new IOException ("Error getting data: " + e.getMessage());
    } // catch

    // Return variable
    // ---------------
    return (var);

  } // getVariable

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    CWOpendapReader reader = new CWOpendapReader (argv[0]);
    noaa.coastwatch.tools.cwinfo.printInfo (reader, System.out);
    noaa.coastwatch.tools.cwinfo.printTransform (reader, System.out, false);
    noaa.coastwatch.tools.cwstats.printStats (reader, null, null, 0, 0.01, 
      null);

  } // main

  ////////////////////////////////////////////////////////////

} // CWOpendapReader class

////////////////////////////////////////////////////////////////////////

