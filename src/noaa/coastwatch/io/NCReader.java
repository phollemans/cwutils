////////////////////////////////////////////////////////////////////////
/*

     File: NCReader.java
   Author: Peter Hollemans
     Date: 2005/07/03

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.GridSubsetReader;
import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.io.NCSD;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.cdm.EllipsoidMercatorBuilder;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.CoordTransBuilder;
import ucar.nc2.constants.CF;

import java.util.logging.Logger;

/** 
 * The <code>NCReader</code> class is the base class for readers that
 * use the Java NetCDF API to read and parse metadata.  Supported file
 * formats include NetCDF 3/4, HDF5, and OPeNDAP network connections.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public abstract class NCReader 
  extends EarthDataReader
  implements GridSubsetReader, NCSD {

  private static final Logger LOGGER = Logger.getLogger (NCReader.class.getName());

  // Variables
  // ---------

  /** The NetCDF dataset for accessing data. */
  protected NetcdfDataset dataset;

  /** The cache of network datasets. */
  private static Map<String, NetcdfDataset> datasetCache = new HashMap<String, NetcdfDataset>();

  /** The cache of variables. */
  private Map<String, DataVariable> variableCache = new HashMap<String, DataVariable>();

  /** The network flag, true if this dataset is network-connected. */
  private boolean isNetwork;

  /** Flag to signify that the file has been closed. */
  private boolean isClosed;

  ////////////////////////////////////////////////////////////

  static {

    // Override the default Mercator transform in order to support
    // generic ellipsoid projections.
    CoordTransBuilder.registerTransform (CF.MERCATOR, EllipsoidMercatorBuilder.class);

  } // static

  /////////////////////////////////////////////////////////////////

  @Override
  public NetcdfDataset getDataset () { return (dataset); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getFilename () { return (getSource()); }

  ////////////////////////////////////////////////////////////
  
  /**
   * Creates a new reader using the dataset at the specified location.
   *
   * @param name the file or network location.
   *
   * @throws IOException if an error occurred accessing the dataset.
   */
  protected NCReader (
    String name
  ) throws IOException {

    super (name);

    try {
    
      isClosed = true;

      // Open network dataset
      // --------------------
      isNetwork = name.startsWith ("http://");
      if (isNetwork) {
        dataset = datasetCache.get (name);
        if (dataset == null) {
          dataset = NetcdfDataset.openDataset (name);
          datasetCache.put (name, dataset);
        } // if
      } // if

      // Open local dataset
      // ------------------
      else {
        dataset = NetcdfDataset.openDataset (name);
      } // else

      isClosed = false;

      // Add raw metadata to map
      // -----------------------
      List attList = dataset.getGlobalAttributes();
      for (Iterator iter = attList.iterator(); iter.hasNext();) {
        Attribute att = (Attribute) iter.next();
        rawMetadataMap.put (att.getShortName(), convertAttributeValue (att, false));
      } // for

      // Finish initialization
      // ---------------------
      initializeReader();

    } // try

    // Catch exception and close file
    // ------------------------------
    catch (Exception e) {
      try { close(); }
      catch (IOException e2) { }
      throw new IOException (e);
    } // catch

  } // NCReader constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Performs reader initialization after the dataset has been opened.
   * 
   * @throws IOException if an error occurred on initialization.
   */
  protected void initializeReader () throws IOException {}

  ////////////////////////////////////////////////////////////

  /** 
   * Converts an attribute into a Java string, primitive array, or
   * wrapped primitive.
   * 
   * @param att the attribute to convert.
   * @param asArray the array flag, true to return single values as an
   * array, false to return as a wrapped primitive.
   */
  protected static Object convertAttributeValue (
    Attribute att, 
    boolean asArray
  ) {

    if (att == null) return (null);
    Object value;
    if (asArray || att.isArray())
      value = att.getValues().copyTo1DJavaArray();
    else {
      if (att.getDataType().isString()) {
        String strValue = att.getStringValue();
        if (strValue == null) strValue = "";
        strValue = strValue.trim();
        if (strValue.indexOf ("\\0") != -1) strValue = IOServices.convertOctal (strValue.trim());
        value = strValue;
      } // if
      else 
        value = att.getNumericValue();
    } // else
    return (value);

  } // convertAttributeValue

  ////////////////////////////////////////////////////////////

  /**
   * Gets a NetCDF global file attribute from this reader and converts
   * it to a Java string, primitive array, or wrapped primitive.
   *
   * @param name the name of the attribute.
   *
   * @return the attribute value or null if none exists.
   */
  public Object getAttribute (String name) {

    return (getAttribute (dataset, name));

  } // getAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Gets a NetCDF global file attribute from this reader and converts
   * it to a primitive array.  Strings are returned as an array of
   * bytes, and arrays of length 1 are returned as-is (and not
   * converted to a wrapped primitive).
   *
   * @param name the name of the attribute.
   *
   * @return the attribute value or null if none exists.
   */
  public Object getAttributeAsArray (String name) {

    return (getAttributeAsArray (dataset, name));

  } // getAttributeAsArray

  ////////////////////////////////////////////////////////////

  /**
   * Gets a NetCDF global file attribute and converts it to a Java
   * string, primitive array, or wrapped primitive.
   *
   * @param file the NetCDF file to access.
   * @param name the name of the attribute.
   *
   * @return the attribute value or null if none exists.
   */
  public static Object getAttribute (NetcdfFile file, String name) {

    return (convertAttributeValue (file.findGlobalAttribute (name), false));

  } // getAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Gets a NetCDF global file attribute and converts it to a
   * primitive array.  Strings are returned as an array of bytes, and
   * arrays of length 1 are returned as-is (and not converted to a
   * wrapped primitive).
   *
   * @param file the NetCDF file to access.
   * @param name the name of the attribute.
   *
   * @return the attribute value or null if none exists.
   */
  public static Object getAttributeAsArray (NetcdfFile file, String name) {

    return (convertAttributeValue (file.findGlobalAttribute (name), true));

  } // getAttributeAsArray

  ////////////////////////////////////////////////////////////

  /**
   * Gets a NetCDF variable attribute and converts it to a Java
   * string, primitive array, or wrapped primitive.
   *
   * @param var the variable to access.
   * @param name the name of the attribute.
   *
   * @return the attribute value or null if none exists.
   */
  public static Object getAttribute (Variable var, String name) {

    return (convertAttributeValue (var.findAttribute (name), false));

  } // getAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Gets a NetCDF variable attribute and converts it to a primitive
   * array.  Strings are returned as an array of bytes, and arrays of
   * length 1 are returned as-is (and not converted to a wrapped
   * primitive).
   *
   * @param var the variable to access.
   * @param name the name of the attribute.
   *
   * @return the attribute value or null if none exists.
   */
  public static Object getAttributeAsArray (Variable var, String name) {

    return (convertAttributeValue (var.findAttribute (name), true));

  } // getAttributeAsArray

  ////////////////////////////////////////////////////////////

  /** Returns true if this reader is network-connected. */
  public boolean isNetwork () { return (isNetwork); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the actual variable with data.  This method should be
   * implemented in the child class and is only called if the variable
   * is not already in the cache.  See {@link #getVariable} for the
   * required behaviour.
   */
  protected abstract DataVariable getActualVariable (
    int index
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  public DataVariable getVariable (
    int index
  ) throws IOException {

    // Get cached variable
    // -------------------
    DataVariable dataVar = variableCache.get (variables[index]);

    // TODO: There may be a better way to manage data variables from NetCDF
    // files.  As it is, we hold onto a strong reference here in a hash map,
    // so that when the file is closed, we can call dispose() on each data
    // variable.  That's because the rest of the code base doesn't take
    // ownership of a variable after calling getVariable(), and so variables
    // aren't ever disposed of.  It turns out that we needed to do this here
    // so that variables that cache some of their data can un-cache their data
    // when the file is closed in order to free up memory.

    // Insert into cache if not found
    // ------------------------------
    if (dataVar == null) {
      dataVar = getActualVariable (index);
      variableCache.put (variables[index], dataVar);
    } // if

    return (dataVar);

  } // getVariable

  ////////////////////////////////////////////////////////////
  
  /** Closes the reader. */
  public void close () throws IOException {

    // Close local datasets
    // --------------------
    if (!isNetwork) {
      if (!isClosed) {
        dataset.close();
        for (DataVariable dataVar : variableCache.values())
          dataVar.dispose();
        isClosed = true;
      } // if
    } // if

  } // close

  ////////////////////////////////////////////////////////////

  /**
   * Gets the NetCDF file referenced in this reader.
   *
   * @return the NetCDF file.
   *
   * @since 3.6.1
   */
  protected NetcdfFile getReferencedFile () {

    var ncFile = dataset.getReferencedFile();
    if (ncFile == null) ncFile = dataset;

    return (ncFile);
  
  } // getReferencedFile

  ////////////////////////////////////////////////////////////

  @Override
  public Grid getGridSubset (
    String varName,
    int[] start,
    int[] stride,
    int[] length
  ) throws IOException {

    // Get a variable grid
    // -------------------
    Grid grid = new Grid ((Grid) getPreview (varName), length[Grid.ROWS],
      length[Grid.COLS]);
    
    // Access variable
    // ---------------
    Variable var = getReferencedFile().findVariable (varName);
    if (var == null)
      throw new IOException ("Cannot access variable " + varName);

    // Read data
    // ---------
    int[] end = new int[] {
      start[0] + stride[0]*length[0] - 1,
      start[1] + stride[1]*length[1] - 1,
    };
    String section = 
      start[0] + ":" + end[0] + ":" + stride[0] + "," +
      start[1] + ":" + end[1] + ":" + stride[1];
    Object data;
    try {
      data = var.read (section).getStorage();
    } // try
    catch (InvalidRangeException e) {
      throw new IOException ("Invalid section spec in grid subset read");
    } // catch

    // Return variable
    // ---------------
    grid.setData (data);
    return (grid);

  } // getGridSubset

  ////////////////////////////////////////////////////////////

  @Override
  public List<CoordinateSystem> getCoordinateSystems () {

    return (dataset.getCoordinateSystems());

  } // getCoordinateSystems

  ////////////////////////////////////////////////////////////

  @Override
  public List<String> getVariablesForSystem (
    CoordinateSystem system
  ) {

    List<String> varList = new ArrayList<String>();
    for (Variable var : dataset.getVariables()) {
      List<CoordinateSystem> systems = 
        ((VariableDS) var).getCoordinateSystems();
      if (systems.size() != 0) {
        CoordinateSystem varSystem = systems.get (0);
        if (varSystem.equals (system))
          varList.add (var.getShortName());
      } // if
    } // for

    return (varList);

  } // getVariablesForSystem

  ////////////////////////////////////////////////////////////

  @Override
  public Map<String, Object> getRawMetadata (int index) throws IOException {

    // In this case, we need to override this method because the normal
    // getPreview() implementation returns a variable with no key/value pairs
    // in the metadata map, for historical reasons.

    var variable = getReferencedFile().findVariable (variables[index]);
    if (variable == null) variable = dataset.findCoordinateAxis (variables[index]);
    if (variable == null) throw new IOException ("Cannot access variable '" + variables[index] + "' at index " + index);

    Map<String, Object> map = new LinkedHashMap<>();
    variable.attributes().forEach (att -> map.put (att.getShortName(), convertAttributeValue (att, false)));

    return (map);

  } // getRawMetadata

  ////////////////////////////////////////////////////////////

} // NCReader class

////////////////////////////////////////////////////////////////////////

