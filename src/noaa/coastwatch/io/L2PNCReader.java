////////////////////////////////////////////////////////////////////////
/*
     FILE: L2PNCReader.java
  PURPOSE: Reads L2P data through the NetCDF 4 interface.
   AUTHOR: X. Liu
     DATE: 2011/05/29
  CHANGES: 2013/04/13, PFH, updated and corrected for latest L2P examples
           2014/04/09, PFH
           - Changes: Removed use of setIsCFConventions in DataVariable.
           - Issue: The use of the method was never fully implemented in 
             DataVariable so rather than continuing its use, we decided
             to remove it and re-arrange the scaling and offset for CF
             conventions before passing into the Grid constructor.
           2015/02/27, PFH
           - Changes: Deprecated this class.
           - Issue: It seems more appropriate to have the CF parsing and 
             transformation data read in a generic way.
 
  CoastWatch Software Library and Utilities
  Copyright 1998-2015, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import noaa.coastwatch.io.NCCachedGrid;
import noaa.coastwatch.io.NCReader;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.L2PProjection;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/** 
 * The <code>L2PNCReader</code> class reads Java NetCDF accessible
 * datasets and uses the L2P metadata conventions to parse
 * the attribute and variable data.
 *
 * @author Xiaoming Liu
 * @since 3.3.0
 *
 * @deprecated As of 3.3.1, GHRSST L2P data for which this class was designed
 * is handled by the {@link CommonDataModelNCReader} class.
 */
@Deprecated
public class L2PNCReader
  extends NCReader {

  // Constants
  // ---------

  /** Swath polynomial size in kilometers. */
  public static final double SWATH_POLY_SIZE = 100.0;

  /** The latitude variable name. */
  private static final String LAT_VAR = "lat";

  /** The longitude variable name. */
  private static final String LON_VAR = "lon";

  /** ISO date format. */
  private static final String ISO_DATE_FMT = "yyyyMMdd'T'HHmmss'Z'";

  // Variables
  // ---------
  
  /** The data format description. */
  private String dataFormat;

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (dataFormat); }

  ////////////////////////////////////////////////////////////
  
  /** Gets the earth data info object. */
  private EarthDataInfo getGlobalInfo() throws IOException {

    // Check processing level
    // ----------------------
    String processing_level = (String) getAttribute ("processing_level");
    if (!processing_level.equals ("L2P"))
      throw new IOException ("Unsupported processing level: " + processing_level);

    // Get simple attributes
    // ---------------------
    String sat = (String) getAttribute ("platform");
    String sensor = (String) getAttribute ("sensor");
    String origin = (String) getAttribute ("institution");
    String conventions = (String) getAttribute ("Conventions");
    dataFormat = processing_level + " NetCDF 4 " + conventions;

    String project = (String) getAttribute ("project");
    String created = (String) getAttribute ("history");
    String history = "[" + project + "] " + created;

    // Get time period list and transform
    // ----------------------------------
    List periodList = getPeriodList();
    EarthTransform transform = getTransform();

    // Create info object
    // ------------------
    EarthDataInfo info = new SatelliteDataInfo (sat, sensor, periodList,
      transform, origin, history);

    return (info);

  } // getGlobalInfo

  ////////////////////////////////////////////////////////////

  /** Gets the list of time periods. */
  private List getPeriodList () throws IOException {

    // Read data
    // ---------
    String startCoverage = (String) getAttribute ("time_coverage_start");
    String endCoverage = (String) getAttribute ("time_coverage_end");
    
    // Create date objects
    // -------------------
    Date startDate, endDate;
    try {
      startDate = DateFormatter.parseDate (startCoverage, ISO_DATE_FMT);
      endDate = DateFormatter.parseDate (endCoverage, ISO_DATE_FMT);
    } // try
    catch (ParseException e) {
      throw new UnsupportedEncodingException ("Cannot parse start/end dates");
    } // catch
    
    // Create period list
    // ------------------
    List periodList = new ArrayList();
    periodList.add (new TimePeriod (startDate, endDate.getTime() - 
      startDate.getTime()));

    return (periodList); 
	   
  } // getPeriodList

  ////////////////////////////////////////////////////////////

  /** Gets the earth transform information. */
  private EarthTransform getTransform () {
	   
    // Create swath
    // ------------
    EarthTransform trans = null;
    DataVariable lat = null, lon = null;
    try {
      lat = getVariable (LAT_VAR);
      int cols = lat.getDimensions()[Grid.COLS];
      int rows = lat.getDimensions()[Grid.ROWS];
      lon = getVariable (LON_VAR);
      DataLocation loc = new DataLocation (2);
      for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++){
          loc.set (Grid.ROWS, i);
          loc.set (Grid.COLS, j);
          double val = lon.getValue (loc);
          if (val > 180) lon.setValue (loc, val-360.0);
        } // for
      } // for
      if (dataProjection) {
        trans = new DataProjection (lat, lon);
      } // if
      else {
        trans = new L2PProjection (lat, lon, 1000, new int[] {100, 100});
      } // else
    } // try
    catch (Exception e) {
      System.err.println (this.getClass() + 
        ": Warning: Problems encountered using earth location data");
      e.printStackTrace();
      if (lat != null && lon != null) {
        System.err.println (this.getClass() + 
          ": Warning: Falling back on data-only projection, " +
          "earth location reverse lookup will not function");
        trans = new DataProjection (lat, lon);
      } // if
    } // catch

    return (trans);
	   
  } // getTransform

  ////////////////////////////////////////////////////////////

  /** Gets the list of variable names. */
  private String[] getVariableNames () {

    // Create list of 2D non-coordinate variables
    // ------------------------------------------
    List<String> nameList = new ArrayList<String>();
    for (Variable var : dataset.getReferencedFile().getVariables()) {
      if (var.getRank() >= 2 && !var.isCoordinateVariable())
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
  public L2PNCReader (
    String name
  ) throws IOException {

	//check if it is a hdf5 file first?
    super (name);
    variables = getVariableNames();
    info = getGlobalInfo();
    if(info == null) throw new RuntimeException("no global info found");

  } // L2PNCReader constructor

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
    units = units.trim();

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
    } // try
    catch (Exception e) {
      missing = null;
    } // catch

    // Try guessing digits from scaling factor
    // ---------------------------------------
    int digits = -1;
    if (scaling != null) {
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

    // Try guessing digits from type
    // -----------------------------
    if (digits == -1) {
      if (varClass.equals (Float.TYPE))
        digits = 6;
      else if (varClass.equals (Double.TYPE))
        digits = 10;
    } // if
    
    // Set format from digits
    // ----------------------
    String decFormat = "0";
    for (int i = 0; i < digits; i++) {
      if (i == 0) decFormat += ".";
      decFormat += "#";
    } // for
    NumberFormat format = new DecimalFormat (decFormat);

    // Create variable
    // ---------------
    DataVariable dataVar;
    int rank = var.getRank();
    if (rank == 2) {
      dataVar = new Grid (name, longName, units, varDims[0], varDims[1], 
        data, format, scaling, missing);
    } // else if 
    else if (rank == 3) {
      dataVar = new Grid (name, longName, units, varDims[1], varDims[2], 
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

  } // getPreviewImpl

  ////////////////////////////////////////////////////////////

  protected DataVariable getActualVariable (
    int index
  ) throws IOException {

    // Get a variable preview
    // ----------------------
    DataVariable varPreview = getPreview (index);

    // Get NetCDF variable
    // -------------------    
    Variable var = dataset.getReferencedFile().findVariable (variables[index]);
    if (var == null)
      throw new IOException ("Cannot access variable at index " + index);

    // Create cached grid
    // ------------------
    DataVariable dataVar;
    if (varPreview instanceof Grid) {
      int rank = var.getRank();
      int[] start = new int[rank];
      Arrays.fill (start, -1);
      if (rank == 3) start[0] = 0;
      NCCachedGrid cachedGrid = new NCCachedGrid ((Grid) varPreview, this, start);
      if (cachedGrid.getMaxTiles() < 2) cachedGrid.setMaxTiles (2);
      dataVar = cachedGrid;
    } // if
    
    // Fill preview with data
    // ----------------------
    else {
      Object data = var.read().getStorage();
      varPreview.setData (data);
      dataVar = varPreview;
    } // else

    return (dataVar);

  } // getActualVariable

  ////////////////////////////////////////////////////////////

} // L2PNCReader class

////////////////////////////////////////////////////////////////////////

