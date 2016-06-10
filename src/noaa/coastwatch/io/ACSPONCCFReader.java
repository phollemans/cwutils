////////////////////////////////////////////////////////////////////////
/*
     FILE: ACSPONCCFReader.java
  PURPOSE: Reads ACSPO data encoded in NetCDF with CF metadata.
   AUTHOR: Peter Hollemans
     DATE: 2013/01/25
  CHANGES: 2013/02/12, PFH, added support for cached grids to getActualVariable
           2013/05/13, PFH, fixed date parsing
           2014/01/25, PFH
           - Changes: added more strict check for ACSPO-specific attributes
           - Issue: Lucas Moxey reported that recognizing some types of CF
             data was failing, and it was because this reader was being used
             rather than the generic NetCDF reader.
           2014/08/08, PFH
           - Changes: Updated to use the TileCachedGrid and NCTileSource classes
             in getActualVariable().  Also fixed missing value for unsigned byte
             data variables.
           - Issue: The NCCachedGrid class uses a per-variable cache of limited
             flexibility, and we needed to be able to handle larger chunk sizes
             than it allowed for with tracking of total cache size across variables.
             The TileCachedGrid makes this possible.  We were getting chunk size
             too large messages when reading certain files.  Also, the missing value for
             byte data should only be applied to signed byte types, not unsigned
             according to Yury Kihai via email July 8, 2014.  We were seeing 
             issues with nighttime data where the cloud bitmask data for SST 
             was showing clear in situations where it should have been masked,
             because the value was being ignored as missing data.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

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
import java.util.Date;
import java.util.List;
import noaa.coastwatch.io.NCReader;
import noaa.coastwatch.io.tile.NCTileSource;
import noaa.coastwatch.io.tile.TileCachedGrid;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.SwathProjection;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/** 
 * The <code>ACSPONCCFReader</code> class reads Java NetCDF accessible
 * datasets and uses the ACSPO-CF metadata conventions to parse
 * the attribute and variable data.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class ACSPONCCFReader
  extends NCReader {

  // Constants
  // ---------

  /** Swath polynomial size in kilometers. */
  public static final double SWATH_POLY_SIZE = 100.0;

  /** The latitude variable name. */
  private static final String LAT_VAR = "latitude";

  /** The longitude variable name. */
  private static final String LON_VAR = "longitude";

  /** ISO date format. */
  private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ssX";

  // Variables
  // ---------
  
  /** The data format description. */
  private String dataFormat;

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (dataFormat); }

  ////////////////////////////////////////////////////////////
  
  /** Gets the earth data info object. */
  private EarthDataInfo getGlobalInfo () throws IOException {

    // Check that we have ACSPO data
    // -----------------------------
    String summary = (String) getAttribute ("summary");
    boolean isSummaryOK = (summary != null && summary.indexOf ("ACSPO") != -1);
    String title = (String) getAttribute ("title");
    boolean isTitleOK = (title != null && title.indexOf ("ACSPO") != -1);
    String inst = (String) getAttribute ("institution");
    boolean isInstOK = (inst != null && inst.indexOf ("NDE") != -1);
    if (!isSummaryOK && !isTitleOK && !isInstOK) {
      throw new IOException ("Cannot verify file is ACSPO data");
    } // if

    // Get simple attributes
    // ---------------------
    String sat = (String) getAttribute ("satellite_name");
    String sensor = (String) getAttribute ("instrument_name");
    String origin = (String) getAttribute ("institution");

    String processing_level = (String) getAttribute ("processing_level");
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
      if (var.getRank() == 2 && !var.isCoordinateVariable())
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
  public ACSPONCCFReader (
    String name
  ) throws IOException {

    super (name);
    variables = getVariableNames();
    info = getGlobalInfo();

  } // ACSPONCCFReader constructor

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
    if (units.equals ("none")) units = "";

    // Set missing value and print format
    // ----------------------------------
    Object missing;
    NumberFormat format;
    if (varClass.equals (Byte.TYPE)) {
      if (isUnsigned)
        missing = null;
      else {
        String missingStr = (String) getAttribute ("missing_value_int1");
        missing = new Byte ((byte) (Short.valueOf (missingStr) & 0xff));
      } // else
      format = new DecimalFormat ("0");
    } // if
    else if (varClass.equals (Float.TYPE)) {
      String missingStr = (String) getAttribute ("missing_value_real4");
      missing = Float.valueOf (missingStr);
      format = new DecimalFormat ("0.######");
    } // else if
    else {
      throw new UnsupportedEncodingException (
        "Unsupported variable class " + varClass + " for " + name);
    } // else
    
    // Create variable
    // ---------------
    DataVariable dataVar = new Grid (name, longName, units, varDims[0],
      varDims[1], data, format, null, missing);
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

    // Create cached grid
    // ------------------
    DataVariable dataVar;
    if (varPreview instanceof Grid) {
      Grid grid = (Grid) varPreview;
      NCTileSource source = new NCTileSource (dataset.getReferencedFile(),
        grid.getName(), Grid.ROWS, Grid.COLS, new int[] {0, 0});
      TileCachedGrid cachedGrid = new TileCachedGrid (grid, source);
      dataVar = cachedGrid;
    } // if
    
    // Fill preview with data
    // ----------------------
    else {
      Variable var = dataset.getReferencedFile().findVariable (variables[index]);
      if (var == null)
        throw new IOException ("Cannot access variable at index " + index);
      Object data = var.read().getStorage();
      varPreview.setData (data);
      dataVar = varPreview;
    } // else

    return (dataVar);

  } // getActualVariable

  ////////////////////////////////////////////////////////////

} // ACSPONCCFReader class

////////////////////////////////////////////////////////////////////////

