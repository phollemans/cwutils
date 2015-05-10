////////////////////////////////////////////////////////////////////////
/*
     FILE: ACSPOHDFReader.java
  PURPOSE: Reads HDF files from the NOAA/NESDIS ACSPO processing system.
   AUTHOR: Peter Hollemans
     DATE: 2007/06/20
  CHANGES: 2007/09/20, PFH, changed name to ACSPO
           2007/12/06, PFH, added variable filtering based on dimensions
           2008/04/02, PFH
           - updated to read new SATELLITE attribute
           - updated to use new HDF geolocation and prototype variables
           2010/03/30, PFH, modified constructor to close file on failure
           2010/11/01, XL, modified getVariable method to read non-chunked
             compressed files
           2010/12/02, XL, modified getGlobalInfo method to read satellite
             name info for ACSPO VIIRS files
           2013/11/18, PFH
           - Changes: updated getGlobalInfo() for Metop-B satellite and to
             properly read SATELLITE, L1B, INSTRUMENT_DATA, SENSOR attributes
             in succession to get satellite and sensor
           - Issue: reading AVHRR Metop-B files shows satellite "Unknown" and
             sensor "FRAC"
           2015/04/17, PFH
           - Changes: Wrapped all HDF library calls in HDFLib.getInstance().
           - Issue: The HDF library was crashing the VM due to multiple threads
             calling the library simultaneously and the library is not
             threadsafe.

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
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import noaa.coastwatch.io.HDFLib;
import noaa.coastwatch.io.HDFCachedGrid;
import noaa.coastwatch.io.HDFReader;
import noaa.coastwatch.io.tile.TilingScheme.Tile;
import noaa.coastwatch.io.tile.TilingScheme.TilePosition;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.SwathProjection;

/**
 * A <code>ACSPOHDFReader</code> reads HDF format data output
 * from the NOAA/NESDIS AVHRR Clear-Sky Processor for Oceans
 * (ACSPO) system.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class ACSPOHDFReader
  extends HDFReader {

  // Constants
  // ---------

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

  // Variables
  // ---------

  /** The data format description read from the file. */
  private String dataFormat;

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (dataFormat); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a ACSPOHDF reader from the specified file.
   * 
   * @param file the file name to read.
   *
   * @throws IOException if an error opening or reading the file
   * metadata.
   */  
  public ACSPOHDFReader (
    String file
  ) throws IOException {

    // Initialize
    // ----------
    super (file);

    try {

      // Get dimensions for prototype data variable
      // ------------------------------------------
      DataVariable protoVar = getPreview (PROTO_VAR);
      int[] protoDims = protoVar.getDimensions();

      // Mark invalid variables in list
      // ------------------------------
      boolean isFiltered = false;
      for (int i = 0; i < variables.length; i++) {
        
        // Get rank and dimensions
        // -----------------------
        int sdsid = HDFLib.getInstance().SDselect (sdid, HDFLib.getInstance().SDnametoindex (sdid, 
          variables[i]));
        if (sdsid < 0)
          throw new HDFException ("Cannot access variable " + variables[i]);
        String[] varName = new String[] {""};
        int varDims[] = new int[HDFConstants.MAX_VAR_DIMS];
        int varInfo[] = new int[3];
        if (!HDFLib.getInstance().SDgetinfo (sdsid, varName, varDims, varInfo))
          throw new HDFException ("Cannot get variable info for " + 
            variables[i]);
        int rank = varInfo[0];
      
        // Check rank and dimensions
        // -------------------------
        if (rank != 2 || 
            protoDims[0] != varDims[0] || 
            protoDims[1] != varDims[1]) {
          variables[i] = null;
          isFiltered = true;
        } // if

      } // for

      // Create new list
      // ---------------
      if (isFiltered) {
        List<String> validNames = new ArrayList<String>();
        for (String varName : variables) {
          if (varName != null) validNames.add (varName);
        } // for
        variables = validNames.toArray (new String[]{});
      } // if

    } // try

    // Catch exception and close file
    // ------------------------------
    catch (Exception e) {
      try { close(); }
      catch (IOException e2) { }
      throw new IOException (e);
    } // catch

  } // ACSPOHDFReader constructor

  ////////////////////////////////////////////////////////////

  protected boolean readAllMetadata () { return (true); }

  ////////////////////////////////////////////////////////////

  protected EarthDataInfo getGlobalInfo () throws HDFException, 
    IOException, ClassNotFoundException {

    // Get satellite name
    // ------------------
    String sat = null;
    try {
      sat = (String) getAttribute (sdid, "SATELLITE");
    } // try
    catch (Exception e) {}
    if (sat == null) {
      try {
        String l1bFile = (String) getAttribute (sdid, "L1B");
        String satCode = l1bFile.split ("\\.")[2];
        if (satCode.equals ("NK")) sat = "NOAA15";
        else if (satCode.equals ("NL")) sat = "NOAA16";
        else if (satCode.equals ("NM")) sat = "NOAA17";
        else if (satCode.equals ("NN")) sat = "NOAA18";
        else if (satCode.equals ("NP")) sat = "NOAA19";
        else if (satCode.equals ("M2")) sat = "METOPA";
        else if (satCode.equals ("M1")) sat = "METOPB";
      } // try
      catch (Exception e) {}
    } // if
    if (sat == null) sat = "Unknown";

    // Get sensor
    // ----------
    String sensor = null;
    try {
      sensor = (String) getAttribute (sdid, "SENSOR");
    } // try
    catch (Exception e) {}
    if (sensor == null) {
      try {
        sensor = (String) getAttribute (sdid, "INSTRUMENT_DATA");
      } // try
      catch (Exception e) {}
    } // if
    if (sensor == null)
      sensor = "AVHRR";

    // Get other simple attributes
    // ---------------------------
    String origin = "NOAA/NESDIS";
    String processor = (String) getAttribute (sdid, "PROCESSOR");
    dataFormat = processor + " HDF 4";
    String created = (String) getAttribute (sdid, "CREATED");
    String history = "[" + processor + "] " + created;

    // Get date and transform
    // ----------------------
    List periodList = getPeriodList();
    EarthTransform transform = getTransform();

    // Create info object
    // ------------------
    EarthDataInfo info = new SatelliteDataInfo (sat, sensor, periodList,
      transform, origin, history);

    return (info);

  } // getGlobalInfo

  //////////////////////////////////////////////////////////////////////

  public DataVariable getPreview (
    int index
  ) throws IOException {

    // Get preview
    // -----------
    DataVariable var = super.getPreview (index);

    // Set units
    // ---------
    String units = (String) var.getMetadataMap().get ("UNITS");
    if (units == null || units.equals ("none")) 
      units = "";
    if (units.equals ("") && var.getName().endsWith ("reflectance"))
      units = "percent";
    var.setUnits (units);

    // Set decimal format
    // ------------------
    Class dataClass = var.getDataClass();
    if (dataClass.equals (Float.TYPE) || dataClass.equals (Double.TYPE))
      var.setFormat (new DecimalFormat ("0.######"));

    // Set missing value
    // -----------------
    if (dataClass.equals (Float.TYPE)) 
      var.setMissing ((Float) rawMetadataMap.get ("MISSING_VALUE_REAL4"));

    return (var);

  } // getPreview

  //////////////////////////////////////////////////////////////////////

  /**
   * Reads the time period list.  The date and time metadata in the
   * HDF file are converted into the equivalent list of TimePeriod
   * objects.
   *
   * @return a list of TimePeriod objects from data in the HDF file
   * data.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   *
   * @see noaa.coastwatch.util.TimePeriod
   */
  private List getPeriodList () 
    throws HDFException, ClassNotFoundException {

    // Read the date and time
    // ----------------------
    int startYear = ((Number) getAttribute (sdid, "START_YEAR")).intValue();
    int startDay = ((Number) getAttribute (sdid, "START_DAY")).intValue();
    double startTime = 
      ((Number) getAttribute (sdid, "START_TIME")).doubleValue();
    int endYear = ((Number) getAttribute (sdid, "END_YEAR")).intValue();
    int endDay = ((Number) getAttribute (sdid, "END_DAY")).intValue();
    double endTime = 
      ((Number) getAttribute (sdid, "END_TIME")).doubleValue();

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

  //////////////////////////////////////////////////////////////////////

  /**
   * The <code>LongitudeGrid</code> class is a private class for
   * reading and interpolating longitude data in CLAVR-x files.
   * In areas close to the poles, the CLAVR-x files contain
   * longitude data with anomalies due to an incorrect
   * interpolation from the NOAA 1b data.  If a tile is requested
   * in one of these areas, we re-interpolate the data using the
   * same method as in the NOAA 1b reading code, ie: convert to
   * polar (x,y) map coordinates, then interpolate, then convert
   * back.
   */
  private class LongitudeGrid extends HDFCachedGrid {

    // Constants
    // ---------
    
    /** The maximum latitude for no resampling. */
    private static final int MAX_LATITUDE = 85;

    /** The starting sample for LAC navigation. */
    public static final int LAC_NAVIGATION_START = 24;
    
    /** The ending sample for LAC navigation. */
    public static final int LAC_NAVIGATION_END = 2024;
    
    /** The navigation step size for LAC navigation. */
    public static final int LAC_NAVIGATION_STEP = 40;
    
    /** The starting sample for GAC navigation. */
    public static final int GAC_NAVIGATION_START = 4;
    
    /** The ending sample for GAC navigation. */
    public static final int GAC_NAVIGATION_END = 404;
    
    /** The navigation step size for GAC navigation. */
    public static final int GAC_NAVIGATION_STEP = 8;
    
    /** The number of samples per line for LAC data. */
    public static final int LAC_SAMPLES = 2048;
    
    /** The number of samples per line for GAC data. */
    public static final int GAC_SAMPLES = 409;

    /** The total number of navigation data values. */
    public static final int NAVIGATION_VALUES = 51;

    // Variables
    // ---------

    /** The cache of raw latitude data by line. */
    private Map<Integer,float[]> latDataCache;

    /** The cache of raw longitude data by line. */
    private Map<Integer,float[]> lonDataCache;

    /** The starting sample for navigation. */
    private int navigationStart;
    
    /** The ending sample for navigation. */
    private int navigationEnd;
    
    /** The navigation step size for navigation. */
    private int navigationStep;

    //////////////////////////////////////////////////////////////////

    /**
     * Gets raw navigation data for the specified variable.
     *
     * @param var the variable name to read.
     * @param cache the cache to store data.
     * @param line the scan line number to read.
     *
     * @return the raw navigation data.
     */
    private float[] getRawNavigation (
      String var,
      Map<Integer,float[]> cache,
      int line
    ) throws IOException {

      try {

        // Read data into cache
        // --------------------
        float[] data = cache.get (line);
        if (data == null) {

          // Read raw data
          // -------------
          int index = HDFLib.getInstance().SDnametoindex (sdid, var);
          int sdsid = HDFLib.getInstance().SDselect (sdid, index);
          if (sdsid < 0)
            throw new HDFException ("Cannot access variable " + var);
          int[] start = new int[] {line, navigationStart};
          int[] stride = new int[] {1, navigationStep};
          int[] length = new int[] {1, NAVIGATION_VALUES};
          data = new float[NAVIGATION_VALUES];
          if (!HDFLib.getInstance().SDreaddata (sdsid, start, stride, length, data))
            throw new HDFException ("Cannot read navigation data for " + var);
          HDFLib.getInstance().SDendaccess (sdsid);

          // Insert into cache
          // -----------------
          cache.put (line, data);
          
        } // if

        // Return data
        // -----------
        return (data);

      } // try

      catch (Exception e) {
        throw new IOException (e.getMessage());
      } // catch
      
    } // getRawNavigation

    //////////////////////////////////////////////////////////////////

    /**
     * Creates a new longitude data grid for this reader.
     *
     * @throws IOException if an error occurred reading the
     * lat/lon data.
     */
    public LongitudeGrid () throws IOException {

      // Initialize
      // ----------
      super ((Grid) getPreview (LON_VAR), ACSPOHDFReader.this);
      latDataCache = new HashMap<Integer,float[]>();
      lonDataCache = new HashMap<Integer,float[]>();

      // Set navigation constants
      // ------------------------
      DataVariable var = getPreview (LAT_VAR);
      int[] dims = var.getDimensions();
      switch (dims[Grid.COLS]) {
      case LAC_SAMPLES:
        navigationStart = LAC_NAVIGATION_START;
        navigationEnd = LAC_NAVIGATION_END;
        navigationStep = LAC_NAVIGATION_STEP; 
        break;
      case GAC_SAMPLES:
        navigationStart = GAC_NAVIGATION_START;
        navigationEnd = GAC_NAVIGATION_END;
        navigationStep = GAC_NAVIGATION_STEP; 
        break;
      default: 
        throw new IOException ("Unsupport sample count: " + dims[Grid.COLS]);
      } // switch

    } // LongitudeGrid constructor

    //////////////////////////////////////////////////////////////////

    /*

    protected Tile readTile (
      TilePosition pos
    ) throws IOException {

      // Get longitude data tile
      // -----------------------
      Tile tile = super.readTile (pos);

      // Check for interpolation required
      // --------------------------------
      Rectangle rect = tile.getRectangle();
      boolean needsInterpolation = false;
      check: 
      for (int row = rect.y; row < rect.y+rect.height-1; row += 10) {
        for (int col = rect.x; col < rect.x+rect.width-1; col += 10) {
          double lat = latData.getValue (row, col);
          if (lat > MAX_LATITUDE) {
            needsInterpolation = true;
            break check;
          } // if
        } // for
      } // for
      
      // Perform interpolation
      // ---------------------
      if (needsInterpolation) {










      } // if

      return (tile);

    } // readTile

    */

    //////////////////////////////////////////////////////////////////

  } // LongitudeGrid class

  //////////////////////////////////////////////////////////////////////

  public DataVariable getVariable (
    String name
  ) throws IOException {

    // TODO: We need to complete this code at some point if the
    // CLAVR-x data continues to have longitude problems.  The
    // idea is to use the code from the NOAA1bReader class.  For
    // now we just pass back data from the superclass.

    /*
    if (name.equals (LON_VAR)) return (new LongitudeGrid());
    else return (super.getVariable (name));
    */
	/*
    final DataVariable var = super.getVariable (name);
    if (var instanceof HDFCachedGrid && (var.getName().equals (LAT_VAR) || 
      var.getName().equals (LON_VAR))) {
      ((HDFCachedGrid)var).setCacheSize (32*1024*1024);
    } // if
    */
	int index = getIndex (name);
	DataVariable var = getPreview (index);
	if (var instanceof Grid){	
		
		try{
			// Access variable
		    // ---------------
			int sdsid = HDFLib.getInstance().SDselect (sdid, HDFLib.getInstance().SDnametoindex (sdid, 
					variables[index]));
			if (sdsid < 0)
	        throw new HDFException ("Cannot access variable at index " + index);
	      
			int[] chunk_lengths = null;
	    	chunk_lengths = HDFReader.getChunkLengths (sdsid); 
	    	
	    	if (chunk_lengths != null){
	    		HDFCachedGrid cachedGrid = new HDFCachedGrid ((Grid) var, this);
	    		if ((var.getName().equals (LAT_VAR) || 
	    			      var.getName().equals (LON_VAR)))
	    			cachedGrid.setCacheSize (32*1024*1024);
	    		HDFLib.getInstance().SDendaccess (sdsid);
	    		return cachedGrid;
	    	}
	      
	    	// Read data
	    	// ---------
	    	Object data = Array.newInstance (var.getDataClass(), var.getValues());
	    	int[] dims = var.getDimensions();
	    	int[] start = new int[dims.length];
	    	Arrays.fill (start, 0);
	    	int[] stride = new int[dims.length];
	    	Arrays.fill (stride, 1);
	    	if (!HDFLib.getInstance().SDreaddata (sdsid, start, stride, dims, data))
	        throw new HDFException ("Cannot read data for " + var.getName());

	    	// End access
	    	// ----------
	    	HDFLib.getInstance().SDendaccess (sdsid);

	    	// Return variable
	    	// ---------------
	    	var.setData (data);
	    	return (var);

		} // try

		catch (Exception e) {
			throw new IOException (e.getMessage ());
		} // catch
  	}
	else{
		return super.getVariable (name);
	}

  } // getVariable

  //////////////////////////////////////////////////////////////////////

  /**
   * Reads the Earth transform information.  The projection
   * metadata and data in the HDF file is converted into the
   * equivalent {@link SwathProjection}.
   *
   * @return an earth transform based on the HDF file data.
   *
   * @throws IOException if there were errors reading the HDF data.
   * @throws HDFException if there were errors reading the HDF metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  private EarthTransform getTransform () 
    throws IOException, HDFException, ClassNotFoundException {

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

  //////////////////////////////////////////////////////////////////////

} // ACSPOHDFReader class

////////////////////////////////////////////////////////////////////////
