////////////////////////////////////////////////////////////////////////
/*
     FILE: NCReader.java
  PURPOSE: Reads data through the NetCDF interface.
   AUTHOR: Peter Hollemans
     DATE: 2005/07/03
  CHANGES: 2006/11/03, PFH, changed getPreview(int) to getPreviewImpl(int)
           2010/03/15, PFH, added getCoordinateSystems() and 
             getVariablesForSystem() 

  CoastWatch Software Library and Utilities
  Copyright 1998-2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.util.*;
import java.io.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.ma2.*;
import noaa.coastwatch.util.*;

/** 
 * The <code>NCReader</code> class is the base class for readers that
 * use the Java NetCDF API to read and parse metadata.  Supported file
 * formats include NetCDF 3/4, HDF5, and OPeNDAP network connections.
 */
public abstract class NCReader 
  extends EarthDataReader
  implements GridSubsetReader {

  // Variables
  // ---------

  /** The NetCDF dataset for accessing data. */
  protected NetcdfDataset dataset;

  /** The cache of network datasets. */
  private static Map datasetCache = new HashMap();

  /** The cache of variables. */
  private Map variableCache = new HashMap();

  /** The network flag, true if this dataset is network-connected. */
  private boolean isNetwork;

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

    // Open network dataset
    // --------------------
    isNetwork = name.startsWith ("http://");
    if (isNetwork) {
      dataset = (NetcdfDataset) datasetCache.get (name);
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

    // Add raw metadata to map
    // -----------------------
    List attList = dataset.getGlobalAttributes();
    for (Iterator iter = attList.iterator(); iter.hasNext();) {
      Attribute att = (Attribute) iter.next();
      rawMetadataMap.put (att.getName(), convertAttributeValue (att, false));
    } // for

  } // NCReader constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Converts an attribute into a Java string, primitive array, or
   * wrapped primitive.
   * 
   * @param att the attribute to convert.
   * @param asArray the array flag, true to return single values as an
   * array, false to return as a wrapped primitive.
   */
  private static Object convertAttributeValue (
    Attribute att, 
    boolean asArray
  ) {

    if (att == null) return (null);
    Object value;
    if (asArray || att.isArray())
      value = att.getValues().copyTo1DJavaArray();
    else {
      if (att.isString()) {
        value = att.getStringValue().trim();
        if (((String) value).indexOf ("\\0") != -1) {
          value = IOServices.convertOctal ((String) value).trim();
        } // if
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
    DataVariable dataVar = (DataVariable) variableCache.get (variables[index]);

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

    // Don't close network datasets (they're cached)
    // ---------------------------------------------
    if (isNetwork) return;

    // But do close local datasets
    // ---------------------------
    dataset.close();

  } // close

  ////////////////////////////////////////////////////////////

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
    Variable var = dataset.getReferencedFile().findVariable (varName);
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

  public List<CoordinateSystem> getCoordinateSystems () {

    return (dataset.getCoordinateSystems());

  } // getCoordinateSystems

  ////////////////////////////////////////////////////////////

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
          varList.add (var.getName());
      } // if
    } // for

    return (varList);

  } // getVariablesForSystem

  ////////////////////////////////////////////////////////////

} // NCReader class

////////////////////////////////////////////////////////////////////////

