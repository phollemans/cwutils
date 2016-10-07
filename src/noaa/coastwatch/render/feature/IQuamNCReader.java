////////////////////////////////////////////////////////////////////////
/*
     FILE: IQuamNCReader.java
  PURPOSE: To provide SST quality monitoring point data from the iQuam system.
   AUTHOR: Peter Hollemans
     DATE: 2016/06/20
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.lang.reflect.Array;
import java.lang.Math;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.awt.Color;

import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.Datatype;

import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.feature.FilteredFeatureSource;
import noaa.coastwatch.render.feature.FilteredFeatureSource.FilterMode;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.feature.TimeWindow;
import noaa.coastwatch.render.feature.TimeWindowRule;
import noaa.coastwatch.render.SimpleSymbol;
import noaa.coastwatch.render.PlotSymbolFactory;
import noaa.coastwatch.render.PointFeatureOverlay;

/**
 * The iQuam data reader reads NOAA iQuam (in-situ SST quality monitoring)
 * system data files and presents the data as point features.  Data files
 * are obtained from:
 * <blockquote>
 *   http://www.star.nesdis.noaa.gov/sod/sst/iquam/v2/index.html
 * </blockquote>
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class IQuamNCReader
  extends PointFeatureSource {

  // Constants
  // ---------
  
  /** The latitude variable name. */
  private final static String LAT_VAR = "/lat";
  
  /** The longitude variable name. */
  private final static String LON_VAR = "/lon";

  /** The time variable name. */
  private final static String TIME_VAR = "/time";

  /** The platform type variable name. */
  private final static String PLATFORM_TYPE_VAR = "/platform_type";

  /** The expected variables in the file. */
  private final static String[] EXPECTED_VAR_NAMES = {
    "/lat",
    "/lon",
    "/time",
    "/platform_id",
    "/platform_type"
  };

  /** The expected attribute name. */
  private final static String EXPECTED_ATT_NAME = "id";

  /** The expected attribute value. */
  private final static Object EXPECTED_ATT_VALUE = "STAR-L2i-iQuam-V2.00";
  
  /** The default chunk size for unchunked data. */
  private final static int DEFAULT_CHUNK_SIZE = 10000;
  
  /** The array of plot symbol names. */
  private static final String[] plotSymbolNames = new String[] {
    "X",             // unknown
    "Diamond",       // ship
    "Diamond",       // drifter
    "Triangle Up",   // t-mooring
    "Triangle Down", // c-mooring
    "Circle",        // argo
    "Square",        // hr-drifter
    "Square",        // imos
    "Circle"         // crw
  };
  
  /** The array of plot symbol colors. */
  private static final Color[] plotSymbolColors = new Color[] {
    new Color (255, 255, 255),    // unknown
    new Color (20, 150, 20),      // ship
    new Color (0, 0, 180),        // drifter
    new Color (240, 0, 0),        // t-mooring
    new Color (170, 0, 170),      // c-mooring
    new Color (0, 170, 170),      // argo
    new Color (0, 90, 240),       // hr-drifter
    new Color (0, 230, 0),        // imos
    new Color (145, 22, 22)       // crw
  };
  
  // Variables
  // ---------
  
  /** The HDF file to access for point data. */
  private FileFormat format;

  /** The list of dataset names. */
  private List<String> datasetNameList;

  /** The array of active data chunks. */
  private Object[] chunkData;

  /** The data chunk sizes. */
  private int[] chunkSizes;

  /** The current data chunk indexes. */
  private int[] chunkIndices;
  
  /** The number of point observations. */
  private int pointCount;

  /** The fill values for the datasets. */
  private Object[] fillValues;
  
  /** The reference time in milliseconds to compute proper date values. */
  private long refTime;

  /** The mapping of earth area square index to list of point features. */
  private Map<Integer, List<Feature>> squareToFeaturesMap;
  
  /** The latitude variable index. */
  private int latIndex;
  
  /** The latitude data array. */
  private float[] latData;

  /** The longitude variable index. */
  private int lonIndex;
  
  /** The longitude data array. */
  private float[] lonData;
  
  /** The time variable index. */
  private int timeIndex;
  
  /** The platform variable index. */
  private int platformTypeIndex;
  
  ////////////////////////////////////////////////////////

  /**
   * Gets an attribute value from a dataset.
   *
   * @param dataset the dataset to retrieve the attribute.
   * @param attName the attribute name to retrieve.
   *
   * @return the attribute value or null if the attribute doesn't exist
   * for the dataset.
   *
   * @throws Exception if an error occurred accessing the data file.
   */
  private Object getObjectAttribute (
    HObject object,
    String attName
  ) throws Exception {
  
    Object attValue = null;
    for (ncsa.hdf.object.Attribute att : (List<ncsa.hdf.object.Attribute>) object.getMetadata()) {
      if (att.getName().equals (attName))
        attValue = Array.get (att.getValue(), 0);
    } // for

    return (attValue);
  
  } // getObjectAttribute

  ////////////////////////////////////////////////////////

  /**
   * Gets the Java class for an HDF datatype.
   * 
   * @param type the HDF datatype.
   *
   * @return the Java class or null if datatype is unknown.
   */
  private static Class getClassForDatatype (
    Datatype type
  ) {

    Class retClass = null;
    String desc = type.getDatatypeDescription();

    if (desc.indexOf ("character") != -1)
      retClass = Byte.class;

    else if (desc.indexOf ("floating-point") != -1) {
      switch (type.getDatatypeSize()) {
      case 4: retClass = Float.class; break;
      case 8: retClass = Double.class; break;
      } // switch
    } // else if

    else if (desc.indexOf ("integer") != -1) {
      switch (type.getDatatypeSize()) {
      case 2: retClass = Short.class; break;
      case 4: retClass = Integer.class; break;
      case 8: retClass = Long.class; break;
      } // switch
    } // else if
    
    else if (desc.indexOf ("String") != -1) {
      retClass = String.class;
    } // else if
    
    return (retClass);

  } // getClassForDatatype

  ////////////////////////////////////////////////////////

  /**
   * Creates a new reader for an iQuam point data file.
   *
   * @param filename the name of the file.
   */
  public IQuamNCReader (
    String filename
  ) throws IOException {

    try {

      // Open the file
      // -------------
      format = FileFormat.getInstance (filename);
      if (format == null) throw new Exception ("Error opening file");
      format.open();

      // Check for expected attribute
      // ----------------------------
      HObject rootGroup = format.get ("/");
      rootGroup.open();
      Object attValue = getObjectAttribute (rootGroup, EXPECTED_ATT_NAME);
      if (attValue == null || !attValue.equals (EXPECTED_ATT_VALUE))
        throw new IOException ("Attribute '" + EXPECTED_ATT_NAME + "' value does not match expected");

      // Check for expected variables
      // ----------------------------
      datasetNameList = getDatasetsInTree (format.getRootNode(), "");
      for (String varName : EXPECTED_VAR_NAMES) {
        if (!datasetNameList.contains (varName))
          throw new Exception ("File is missing dataset '" + varName + "'");
      } // for
      
      // Set up chunk data
      // -----------------
      pointCount = getPointCount();
      int datasetCount = datasetNameList.size();
      chunkData = new Object[datasetCount];
      chunkSizes = new int[datasetCount];
      chunkIndices = new int[datasetCount];

      // Preload stored variables
      // ------------------------
      latIndex = datasetNameList.indexOf (LAT_VAR);
      preloadVariable (latIndex);
      latData = (float[]) chunkData[latIndex];

      lonIndex = datasetNameList.indexOf (LON_VAR);
      preloadVariable (lonIndex);
      lonData = (float[]) chunkData[lonIndex];

      timeIndex = datasetNameList.indexOf (TIME_VAR);
      preloadVariable (timeIndex);

      platformTypeIndex = datasetNameList.indexOf (PLATFORM_TYPE_VAR);
      preloadVariable (platformTypeIndex);

      // Create attribute list
      // ---------------------
      List<Attribute> attributeList = new ArrayList<Attribute>();
      int index = 0;
      fillValues = new Object[datasetCount];
      for (String datasetName : datasetNameList) {
        Dataset dataset = (Dataset) format.get (datasetName);
        dataset.init();
        String attName = datasetName.replaceFirst ("/", "");
        Attribute att;

        // Create date attribute
        // ---------------------
        if (attName.equals ("time")) {
          fillValues[index] = null;
          att = new Attribute (attName, Date.class, null);
        } // if

        // Create regular attribute
        // ------------------------
        else {

//          if (dataType.getDatatypeSign() == Datatype.SIGN_NONE) System.out.println ("Unsigned");

          Class attType = getClassForDatatype (dataset.getDatatype());
          String attUnits = (String) getObjectAttribute (dataset, "units");
          fillValues[index] = getObjectAttribute (dataset, "_FillValue");
          att = new Attribute (attName, attType, attUnits);
        } // else

        attributeList.add (att);
        index++;

      } // for
      setAttributes (attributeList);
      
    } // try
    
    catch (Exception e) {
      throw new IOException (e);
    } //catch
    
    finally {
      try { format.close(); }
      catch (Exception e) { e.printStackTrace(); }
    } // finally

    // Compute reference time for date computations
    // --------------------------------------------
    Calendar cal = Calendar.getInstance (TimeZone.getTimeZone ("GMT+0"));
    cal.set (1981, 0, 1, 0, 0, 0);      // 1980-01-01 00:00:00
    refTime = cal.getTimeInMillis();

  } // IQuamNCReader construtor
  
  ////////////////////////////////////////////////////////

  /**
   * An observation models a single observation in the data
   * file and allows access to it as if it was a PointFeature.
   */
  private class Observation extends PointFeature {

    // Variables
    // ---------
    
    /** The index of this observation in the data file. */
    private int obsIndex;
    
    ////////////////////////////////////////////////////
    
    @Override
    public Object getAttribute (int index) {

      // Get data value
      // --------------
      Object dataValue;
      try {
        dataValue = getDataValue (index, obsIndex);
      } // try
      catch (Exception e) {
        throw new RuntimeException (e);
      } // catch

      // Check for fill value
      // --------------------
      if (dataValue.equals (fillValues[index])) dataValue = null;

      return (dataValue);

    } // getAttribute
  
    ////////////////////////////////////////////////////

    /**
     * Creates a new observation object with the specified index.
     *
     * @param obsIndex the observation index into the data file.
     */
    public Observation (
      int obsIndex
    ) {
    
      super (new EarthLocation (latData[obsIndex], lonData[obsIndex]), null);
      this.obsIndex = obsIndex;

    } // Observation constructor

    ////////////////////////////////////////////////////
  
  } // Observation class

  ////////////////////////////////////////////////////////
  
  /**
   * Preloads an entire variable to speed up subsequent access.  The file is
   * assumed to be already open.
   *
   * @param varIndex the variable index to preload.
   */
  private void preloadVariable (
    int varIndex
  ) throws Exception {
    
    // Access the dataset
    // ------------------
    String datasetName = datasetNameList.get (varIndex);
    Dataset dataset = (Dataset) format.get (datasetName);
    dataset.init();

    // Read data
    // ---------
    long[] start = dataset.getStartDims();
    start[0] = 0;
    long[] length = dataset.getSelectedDims();
    length[0] = pointCount;
    chunkData[varIndex] = dataset.read();
    chunkIndices[varIndex] = 0;
    chunkSizes[varIndex] = pointCount;

  } // preloadVariable
  
  ////////////////////////////////////////////////////////
  
  /**
   * Gets a value from an array.  This is needed to help speed up array
   * access because the java.util.Array.get() method is know to be slower.
   *
   * @param array the array to get a data value from.
   * @param index the index of the element to get.
   *
   * @return the element in the array.
   */
  private static Object getFromArray (
    Object array,
    int index
  ) {

    Object returnValue;
    Class arrayClass = array.getClass();

    if (arrayClass == boolean[].class)
      returnValue = ((boolean[]) array)[index];

    else if (arrayClass == byte[].class)
      returnValue = ((byte[]) array)[index];

    else if (arrayClass == char[].class)
      returnValue = ((char[]) array)[index];

    else if (arrayClass == short[].class)
      returnValue = ((short[]) array)[index];

    else if (arrayClass == int[].class)
      returnValue = ((int[]) array)[index];

    else if (arrayClass == long[].class)
      returnValue = ((long[]) array)[index];

    else if (arrayClass == float[].class)
      returnValue = ((float[]) array)[index];

    else if (arrayClass == double[].class)
      returnValue = ((double[]) array)[index];

    else
      returnValue = Array.get (array, index);

    return (returnValue);
    
  } // getFromArray

  ////////////////////////////////////////////////////////

  /**
   * Gets a data variable value.
   *
   * @param varIndex the index of the variable in the dataset list.
   * @param valueIndex the index of the value within the variable.
   *
   * @return the data value.
   *
   * @throws Exception if an error occurred accessing the data file.
   */
  private Object getDataValue (
    int varIndex,
    int valueIndex
  ) throws Exception {
  
    // Check if we need to load a new chunk
    // ------------------------------------
    boolean isChunkAvailable = (chunkData[varIndex] != null);
    int chunkIndex = (isChunkAvailable ? valueIndex/chunkSizes[varIndex] : -1);
    boolean isCorrectChunk = (isChunkAvailable && chunkIndex == chunkIndices[varIndex]);
    
    // Load new chunk
    // --------------
    if (!isCorrectChunk) {

      try {

        // Access the dataset
        // ------------------
        format.open();
        String datasetName = datasetNameList.get (varIndex);
        Dataset dataset = (Dataset) format.get (datasetName);
        dataset.init();

        // Perform initial setup
        // ---------------------
        if (!isChunkAvailable) {
          long[] chunkSize = dataset.getChunkSize();
          if (chunkSize != null)
            chunkSizes[varIndex] = (int) chunkSize[0];
          else
            chunkSizes[varIndex] = Math.min (DEFAULT_CHUNK_SIZE, pointCount);
          chunkIndex = valueIndex/chunkSizes[varIndex];
        } // if

        // Read data
        // ---------
        long[] start = dataset.getStartDims();
        start[0] = chunkIndex*chunkSizes[varIndex];
        long[] length = dataset.getSelectedDims();
        length[0] = chunkSizes[varIndex];
        if ((start[0] + length[0] - 1) > (pointCount - 1)) length[0] = pointCount%chunkSizes[varIndex];
        chunkData[varIndex] = dataset.read();
        chunkIndices[varIndex] = chunkIndex;

      } // try

      finally {
        try { format.close(); }
        catch (Exception e) { e.printStackTrace(); }
      } // finally
      
    } // if

    // Get value
    // ---------
    Object value = getFromArray (chunkData[varIndex], valueIndex%chunkSizes[varIndex]);
    if (varIndex == timeIndex) {
      int intValue = ((Integer) value).intValue();
      value = new Date (refTime + (intValue & 0xffffffffL)*1000L);
    } // if
    
    return (value);
  
  } // getDataValue

  ////////////////////////////////////////////////////////

  /**
   * Gets the total number of points in the file.
   *
   * @return the number of point observations.
   *
   * @throws Exception if an error occurred accessing the data file.
   */
  private int getPointCount () throws Exception {
  
    Dataset dataset = (Dataset) format.get (datasetNameList.get (0));
    dataset.init();
    long[] dims = dataset.getDims();
    return ((int) dims[0]);
  
  } // getPointCount

   ////////////////////////////////////////////////////////

  /**
   * Performs a precaching read of all the point data in the file.  This will
   * have the effect of speeding up subsequent select operations significantly,
   * as the point data will be returned from memory and not read on-demand.
   * This operation may take some time to complete.
   */
  public void precache () throws IOException {
  
    // Select entire file
    // ------------------
    EarthArea area = new EarthArea();
    area.addAll();
    select (area);
    
    // Add features to cache
    // ---------------------
    squareToFeaturesMap = new HashMap<Integer, List<Feature>>();
    for (Feature feature : featureList) {
      EarthLocation location = ((PointFeature) feature).getPoint();
      int index = area.getIndex (location);
      List<Feature> squareFeatureList = squareToFeaturesMap.get (index);
      if (squareFeatureList == null) {
        squareFeatureList = new ArrayList<Feature>();
        squareToFeaturesMap.put (index, squareFeatureList);
      } // if
      squareFeatureList.add (feature);
    } // for

    // Clear feature list
    // ------------------
    featureList.clear();

  } // precache

  ////////////////////////////////////////////////////////

  @Override
  protected void select () throws IOException {
    
    // Initialize feature list
    // -----------------------
    featureList.clear();
    
    // Use data cache if available
    // ---------------------------
    if (squareToFeaturesMap != null) {
      for (int[] square : area) {
        int index = area.getIndex (square[0], square[1]);
        List<Feature> squareFeatureList = squareToFeaturesMap.get (index);
        if (squareFeatureList != null)
          featureList.addAll (squareFeatureList);
      } // for
    } // if

    // Read features from the file
    // ---------------------------
    else {
      
      // Loop over each point in the file
      // --------------------------------
      EarthLocation earthLoc = new EarthLocation();
      int datasetCount = datasetNameList.size();
      int lastPercentComplete = -1;
      for (int i = 0; i < pointCount; i++) {

        // Check containment
        // -----------------
        earthLoc.setCoords (latData[i], lonData[i]);
        if (area.contains (earthLoc)) {
          featureList.add (new Observation (i));
        } // if

      } // for

    } // else
    
  } // select

  ////////////////////////////////////////////////////////

  /**
   * Gets the datasets in the file starting from the specified node.
   *
   * @param node the root node to start returning datasets for.
   * @param path the path to the root node.
   *
   * @return the list of datasets in the tree rooted at the specified node.
   *
   * @throws Exception if an error occurred accessing the data file.
   */
  private List<String> getDatasetsInTree (
    TreeNode node,
    String path
  ) throws Exception {
  
    List<String> nameList = new ArrayList<String>();
  
    // Handle leaf case
    // ----------------
    if (node.isLeaf()) {
      HObject obj = format.get (path);
      if (obj instanceof Dataset) {
        nameList.add (obj.getFullName());
      } // if
    } // if

    // Handle children case
    // --------------------
    else {
      for (int i = 0; i < node.getChildCount(); i++) {
        TreeNode child = node.getChildAt (i);
        nameList.addAll (getDatasetsInTree (child, path + "/" + child));
      } // for
    } // else
  
    return (nameList);

  } // getDatasetsInTree

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of platform types in this file.
   *
   * @return the platform types as a set of key/value pairs where the
   * key is the numerical ID of the platform type and the value is the platform 
   * type description.
   *
   * @throws IOException if an error occurred accessing the data file.
   */
  private Map<Integer, String> getPlatformTypes() throws IOException {

    /*
     * In this method, we're parsing the comment string from the platform_type
     * dataset:
     *
     * string platform_type:comment = "0: Unknown; 1: Ship; 2: Drifting Buoy;
     * 3: Tropical Moored Buoy; 4: Coastal Moored Buoy; 5: Argo Float;
     * 6: High Resolution Drifter; 7: IMOS; 8: CRW Buoy; 9+: Reserved.
     * Note: only type 1,2,3,4,5,6,7,8 are QCed." ;
     *
     * On plots, we have the following names as synonyms for these values:
     * 
     * Argo -> Argo Float
     * Drifter -> Drifting Buoy
     * HR-Drifter -> High Resolution Drifter
     * T-Mooring -> Tropical Moored Buoy
     * C-Mooring -> Coastal Moored Buoy
     * CRW -> CRW Buoy
     * Ship -> Ship
     * IMOS -> IMOS
     */

    Map<Integer, String> platformTypeMap = new LinkedHashMap<Integer, String>();

    /*
     * For now rather than using the comment string, we will use our own 
     * abbreviated versions.  It turns out that the strings in the comment 
     * were too long to fit inside the text boxes in the list of layers.
     *
 
    // Get comment string
    // ------------------
    String comment = null;
    try {
      format.open();
      Dataset dataset = (Dataset) format.get ("/platform_type");
      dataset.init();
      comment = (String) getObjectAttribute (dataset, "comment");
    } // try
    catch (Exception e) {
      throw new IOException (e);
    } // catch

    finally {
      try { format.close(); }
      catch (Exception e) { e.printStackTrace(); }
    } // finally

    */
    
    String comment =
      "0: Unknown; " +
      "1: Ship; " +
      "2: Drifter; " +
      "3: T-Mooring; " +
      "4: C-Mooring; " +
      "5: Argo; " +
      "6: HR-Drifter; " +
      "7: IMOS; " +
      "8: CRW.";

    // Parse out the set of ID/value pairs
    // -----------------------------------
    comment = comment.substring (0, comment.indexOf ("."));
    String[] typeStrings = comment.split (";");
    for (String typeString : typeStrings) {
      String[] keyValuePair = typeString.split (":");
      Integer key;
      try { key = Integer.parseInt (keyValuePair[0].trim()); }
      catch (NumberFormatException e) { key = null; }
      if (key != null) {
        String value = keyValuePair[1].trim();
        platformTypeMap.put (key, value);
      } // if
    } // for
  
    return (platformTypeMap);

  } // getPlatformTypes
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the standard set of iQuam point data overlays for this data source.
   *
   * @param date the date to use for the set of overlays.  Point data in the
   * overlays will be filtered to fall within a certain time window centered
   * on the date.
   * 
   * @return a set of overlays that can be used to display iQuam data points.
   *
   * @throws IOException if an error occurred accessing the data file.
   */
  public List<EarthDataOverlay> getStandardOverlays (
    Date date
  ) throws IOException {

    List<EarthDataOverlay> overlayList = new ArrayList<EarthDataOverlay>();

    // Create time window rule
    // -----------------------
    Map<String, Integer> attNameMap = new HashMap<String, Integer>();
    List<Attribute> attList = getAttributes();
    for (int i = 0; i < attList.size(); i++)
      attNameMap.put (attList.get (i).getName(), i);
    TimeWindow window = new TimeWindow (date, 12*60*60*1000L);  // date +/- 12 hours
    TimeWindowRule timeRule = new TimeWindowRule ("time", attNameMap, window);

    // Loop over each platform type
    // ----------------------------
    for (Map.Entry<Integer, String> entry : getPlatformTypes().entrySet()) {

      // Create a filtered source for the platform
      // -----------------------------------------
      FilteredFeatureSource source = new FilteredFeatureSource (this);
      source.setMode (FilterMode.MATCHES_ALL);
      source.getRules().add (timeRule);
      int platformType = entry.getKey();
      NumberRule platformRule = new NumberRule ("platform_type", attNameMap, (byte) platformType);
      platformRule.setOperator (NumberRule.Operator.IS_EQUAL_TO);
      source.getRules().add (platformRule);

      // Get symbol and color for this overlay
      // -------------------------------------
      String symbolName;
      Color symbolColor;
      if (platformType >= 0 && platformType < plotSymbolNames.length) {
        symbolName = plotSymbolNames[platformType];
        symbolColor = plotSymbolColors[platformType];
      } // if
      else {
        symbolName = plotSymbolNames[0];
        symbolColor = plotSymbolColors[0];
      } // else
      SimpleSymbol symbol = new SimpleSymbol (PlotSymbolFactory.create (symbolName));
      symbol.setBorderColor (Color.WHITE);
      symbol.setFillColor (symbolColor);

      // Create overlay
      // --------------
      PointFeatureOverlay overlay = new PointFeatureOverlay (symbol, source);
      overlay.setName (entry.getValue());
      overlayList.add (overlay);

    } // for

    return (overlayList);

  } // getStandardOverlays

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    IQuamNCReader reader = new IQuamNCReader (argv[0]);
    EarthArea area = new EarthArea();
    area.add (new EarthLocation (50, -130));
    for (int i = 0; i < 2; i++) area.expand();
    reader.select (area);
    
    List<Attribute> attList = reader.getAttributes();
    int n = 0;
    for (Feature feature : reader) {
      System.out.println ("Feature[n=" + n + "]");
      for (int i = 0; i < attList.size(); i++) {
        Object attValue = feature.getAttribute (i);
        String attUnits = attList.get (i).getUnits();
        Object attName = attList.get (i).getName();
        if (attValue != null)
          System.out.println ("  " + attName + " = " + attValue + (attUnits != null ? " (" + attUnits + ")" : ""));
      } // for
      n++;
    } // for

    for (Map.Entry<Integer, String> entry : reader.getPlatformTypes().entrySet()) {
      System.out.println ("PlatformType[id=" + entry.getKey() +
        ", desc=" + entry.getValue() + "]");
    } // for

    reader.precache();

  } // main

  ////////////////////////////////////////////////////////////

} // IQuamNCReader class

////////////////////////////////////////////////////////////////////////
