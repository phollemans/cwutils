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
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Array;
import java.lang.Math;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;

import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Group;

import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.render.feature.Attribute;

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

      // Create attribute list
      // ---------------------
      List<Attribute> attributeList = new ArrayList<Attribute>();
      int index = 0;
      fillValues = new Object[datasetCount];
      for (String datasetName : datasetNameList) {
        Dataset dataset = (Dataset) format.get (datasetName);
        dataset.init();
        String attName = datasetName.replaceFirst ("/", "");
        Class attType = getDataValue (index, 0).getClass();
        String attUnits = (String) getObjectAttribute (dataset, "units");
        fillValues[index] = getObjectAttribute (dataset, "_FillValue");
        attributeList.add (new Attribute (attName, attType, attUnits));
        index++;
      } // for
      setAttributes (attributeList);

    } // try
    
    catch (Exception e) {
      throw new IOException (e.getMessage());
    } //catch
    
    finally {
      try { format.close(); }
      catch (Exception e) { e.printStackTrace(); }
    } // finally

  } // IQuamNCReader
  
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

      // Access the dataset
      // ------------------
      Dataset dataset = (Dataset) format.get (datasetNameList.get (varIndex));
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
    
    } // if

    // Get value
    // ---------
    return (Array.get (chunkData[varIndex], valueIndex%chunkSizes[varIndex]));
  
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

  @Override
  protected void select () throws IOException {

    try {

      // Open file and get variable indices
      // ----------------------------------
      format.open();
      int latIndex = datasetNameList.indexOf (LAT_VAR);
      int lonIndex = datasetNameList.indexOf (LON_VAR);
      
      // Loop over each point in the file
      // --------------------------------
      EarthLocation earthLoc = new EarthLocation();
      int datasetCount = datasetNameList.size();
      for (int i = 0; i < pointCount; i++) {

        // Get earth location and check containment
        // ----------------------------------------
        double lat = ((Number) getDataValue (latIndex, i)).doubleValue();
        double lon = ((Number) getDataValue (lonIndex, i)).doubleValue();
        earthLoc.setCoords (lat, lon);
        if (area.contains (earthLoc)) {

          // Create point feature
          // --------------------
          Object[] values = new Object[datasetNameList.size()];
          for (int k = 0; k < values.length; k++) {
            values[k] = getDataValue (k, i);
            if (values[k].equals (fillValues[k])) values[k] = null;
          } // for
          featureList.add (new PointFeature ((EarthLocation) earthLoc.clone(), values));
      
        } // if

      } // for

    } // try

    catch (Exception e) {
      throw new IOException (e);
    } // catch

    finally {
      try { format.close(); }
      catch (Exception e) { e.printStackTrace(); }
    } // finally
    
  } // select

  ////////////////////////////////////////////////////////////

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

  } // main

  ////////////////////////////////////////////////////////////

} // IQuamNCReader class

////////////////////////////////////////////////////////////////////////
