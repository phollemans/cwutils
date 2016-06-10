////////////////////////////////////////////////////////////////////////
/*
     FILE: CWCFNCReader.java
  PURPOSE: Reads CoastWatch-style data through the NetCDF interface.
   AUTHOR: X. Liu
     DATE: 2012/06/27
  CHANGES: 2013/06/21, PFH, updated to use Variable.getShortName()
           2014/04/09, PFH
           - Changes: Removed use of setIsCFConventions in DataVariable.
           - Issue: The use of the method was never fully implemented in 
             DataVariable so rather than continuing its use, we decided
             to remove it and re-arrange the scaling and offset for CF
             conventions before passing into the Grid constructor.
           2016/02/10, PFH
           - Changes: Added more sophisticated detection of rows and columns
             dimensions for map transforms.  Previously the code was hard
             coded to detect a "latitude" variable.  Now it looks for the
             first 2D variable and uses that.
           - Issue: Files that had CoastWatch HDF-like metadata but no 
             "latitude" variable were not having a correct transform being
             created.
           2016/03/16, PFH
           - Changes: Updated getPreviewImpl method to read any rank of
             data, assuming that the rows and cols dimensions are the last 
             two.
           - Issue: The CoastWatch THREDDS server contained files that have
             a rank of 5, and this reader was balking at those variables.
  
  CoastWatch Software Library and Utilities
  Copyright 1998-2016, USDOC/NOAA/NESDIS CoastWatch

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
import ucar.nc2.dataset.NetcdfDataset;

/** 
 * The <code>CWCFNCReader</code> class reads Java NetCDF accessible
 * datasets and uses the CoastWatch HDF metadata conventions to parse
 * the attribute and variable data.
 *
 * @author Xiaoming Liu
 * @since 3.3.0
 */
public class CWCFNCReader
  extends NCReader {

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
    if (origin == null) origin = "unknown";
    String history = (String) getAttribute ("history");
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
          System.err.println (this.getClass() + 
            ": Warning: Problems encountered using earth location data");
          e.printStackTrace();
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
	      if (var.getRank() >= 2 && !var.getShortName().equals("time_bounds")) {
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
    variables = getVariableNames();
    info = getGlobalInfo();
    if(info == null) throw new RuntimeException("no global info found");

  } // CWCFNCReader constructor

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


