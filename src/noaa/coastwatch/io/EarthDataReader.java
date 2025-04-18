////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataReader.java
   Author: Peter Hollemans
     Date: 2002/04/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.GridChunkProducer;
import noaa.coastwatch.util.MetadataServices;

import ucar.nc2.dataset.CoordinateSystem;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An <code>EarthDataReader</code> obtains earth data from a data source and
 * provides it to the user in a consistent format.  A reader
 * should do the following:
 * <ul>
 *   <li> Construct from some type of file or data stream </li>
 *   <li> Read global information into a 
 *        {@link noaa.coastwatch.util.EarthDataInfo} object </li>
 *   <li> Report the number of
 *        {@link noaa.coastwatch.util.DataVariable} objects </li>
 *   <li> Read a "preview" of a <code>DataVariable</code>, which
 *        consists of all attributes but no actual data values </li>
 *   <li> Read a {@link DataVariable} object with data </li>
 *   <li> Close the source when no longer needed </li>
 * </ul>
 * The only methods that child classes must implement are:
 * <ul>
 *   <li>{@link #getDataFormat}</li>
 *   <li>{@link #getPreviewImpl(int)}</li>
 *   <li>{@link #getVariable(int)}</li>
 *   <li>{@link #close}</li>
 * </ul>
 * as well as a constructor that sets the protected {@link #info} and
 * {@link #variables} variables and fills the {@link #rawMetadataMap}
 * map.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class EarthDataReader {

  private static final Logger LOGGER = Logger.getLogger (EarthDataReader.class.getName());

  // Variables
  // ---------
  /** Earth data info object. */
  protected EarthDataInfo info;

  /** Earth data variable names. */
  protected String[] variables;

  /** The data source. */
  private String source;

  /** The map of variable name to statistics. */
  private Map<String, Statistics> statsMap;

  /** 
   * The data projection flag, true if reading explicit lat/lon data
   * should return a <code>DataProjection</code> rather than a
   * <code>SwathProjection</code>. 
   */
  protected static boolean dataProjection;

  /** The raw metadata map. */
  protected Map rawMetadataMap = new LinkedHashMap();

  /** The units map of variable names to preferred units. */
  private static Map unitsMap;

  ////////////////////////////////////////////////////////////
  
  /**
   * Sets the variable name to units map.  If set, data variables
   * accessed via {@link #getPreview} and {@link #getVariable} will
   * automatically have their units converted to the new units before
   * being passed to the caller.
   *
   * @param map the new map of variable name to units string.
   */
  public static void setUnitsMap (
    Map map
  ) {

    unitsMap = map;

  } // setUnitsMap

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data projection flag.  This is only applicable to
   * readers that deal with earth transforms stored as explicit earth
   * location data.  When true, the data projection flag forces the
   * reader to return a <code>DataProjection</code> object for the
   * earth transform when reading explicit latitude and longitude
   * data, rather than interpolating the data and returning a
   * <code>SwathProjection</code>.  The main difference is that a
   * <code>DataProjection</code> cannot be used to transform an
   * <code>EarthLocation</code> object into a
   * <code>DataLocation</code> object.  Generally, setting this flag
   * is only desirable if there are inherent problems with
   * interpolating the earth location data.  By default, the reader is
   * set to return a <code>SwathProjection</code>.
   *
   * @param flag the data projection flag.
   */
  public static void setDataProjection (
    boolean flag
  ) {

    dataProjection = flag;

  } // setDataProjection

  ////////////////////////////////////////////////////////////
  
  /** 
   * Gets the list of all variable names that have associated
   * statistics. 
   *
   * @return the list of variable names.
   */
  public List<String> getStatisticsVariables () {

    return (new ArrayList<String> (statsMap.keySet()));

  } // getStatisticsVariables

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the statistics data stored for the specified variable name.
   * 
   * @param name the variable name.
   * 
   * @return the statistics, or null if the variable name has no
   * associated statistics data.
   *
   * @see #putStatistics
   */
  public Statistics getStatistics (
    String name
  ) {

    return (statsMap.get (name));

  } // getStatistics

  ////////////////////////////////////////////////////////////

  /** 
   * Associates statistics with the specified variable name.
   * 
   * @param name the variable name.
   * @param stats the statistics data.
   */
  public void putStatistics (
    String name,
    Statistics stats
  ) {

    statsMap.put (name, stats);

  } // putStatistics

  ////////////////////////////////////////////////////////////

  /** Gets the earth data info object. */
  public EarthDataInfo getInfo () { return (info); }

  ////////////////////////////////////////////////////////////

  /** Gets the total count of data variables. */
  public int getVariables () { return (variables.length); }

  ////////////////////////////////////////////////////////////

  /** Gets the variable name at the specified index. */
  public String getName (int index) { return (variables[index]); }

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public abstract String getDataFormat ();

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new earth data reader.
   * 
   * @param source the data source.
   */
  protected EarthDataReader (
    String source
  ) {

    // Initialize
    // ----------
    this.source = source;
    this.statsMap = new LinkedHashMap<String, Statistics>();

  } // EarthDataReader constructor

  ////////////////////////////////////////////////////////////

  /** Get the earth data source. */
  public String getSource () { return (source); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the scene time based on the first grid variable.
   * 
   * @return the scene time or <code>unknown</code> if the scene time
   * cannot be determined.
   */
  public String getSceneTime () {

    // Find grid variable
    // ------------------
    DataVariable var = null;
    try {
      for (int i = 0; i < variables.length; i++)
        if ((var = getPreview (i)) instanceof Grid) break;
    } catch (IOException e) { }
    if (var == null || !(var instanceof Grid)) return ("Unknown");

    // Get scene time
    // --------------
    return (info.getSceneTime (var.getDimensions()));

  } // getSceneTime

  ////////////////////////////////////////////////////////////

  /**
   * Determines if a variable exists in the reader.
   *
   * @param name the variable name to search for.
   *
   * @return true if the variable exists or false if not.
   *
   * @since 3.5.0
   */
  public boolean containsVariable (
    String name
  ) {
  
    return (getIndex (name) != -1);
  
  } // containsVariable

  ////////////////////////////////////////////////////////////

  /**
   * Retrieves a variable index based on the name.
   * 
   * @param name the variable name to search for.
   * 
   * @return the variable index, or -1 if the variable could not be
   * found.
   */
  public int getIndex (
    String name
  ) {

    for (int i = 0; i < variables.length; i++)
      if (variables[i].equals (name)) return (i);
    return (-1);

  } // getIndex

  ////////////////////////////////////////////////////////////

  /**
   * Creates a data variable preview.  A preview contains all
   * metadata but no data value array.  The preview can be used in a
   * filtering loop to determine if the reading of the variable data
   * is desired, as I/O can be a time-intensive operation.
   *
   * @param index the index of the variable to preview.  Indexing
   * starts at 0.
   *
   * @return a data variable object with data value
   * array of length 1.
   *
   * @throws IOException if the data source had I/O errors.
   *
   * @see #getVariable
   */
  public DataVariable getPreview (
    int index
  ) throws IOException {

    // Get preview
    // -----------
    DataVariable preview = getPreviewImpl (index);
    String varName = preview.getName();

    // Try modifying the units if there's a units map.  We either look for the
    // variable name in the units map, or if the variable name has a group
    // prefix, we strip it and try looking for the base variable name in the 
    // units map.
    if (unitsMap != null) {

      String newUnits = (String) unitsMap.get (varName);
      if (newUnits == null) {
        String baseVarName = IOServices.stripGroup (varName);
        newUnits = (String) unitsMap.get (baseVarName);
      } // if

      if (newUnits != null) {
        try {
          preview.convertUnits (newUnits);
        } // try
        catch (IllegalArgumentException e) {
          LOGGER.warning (e.getMessage() + ", units conversion failed");
        } // catch
      } // if

    } // if

    return (preview);

  } // getPreview

  ////////////////////////////////////////////////////////////

  /**
   * Implementation for the subclass.
   *
   * @see #getPreview(int)
   */
  protected abstract DataVariable getPreviewImpl (
    int index
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a chunk producer for the specified variable.
   *
   * @param name the variable name.
   *
   * @return a chunk producer for the specified variable.
   *
   * @since 3.5.0
   */
  public ChunkProducer getChunkProducer (
    String name
  ) throws IOException {

    ChunkProducer producer;

    DataVariable var = getPreview (name);
    if (var instanceof Grid) {
      producer = new GridChunkProducer ((Grid) getVariable (name));
    } // if
    else {
      throw new IOException ("Chunk producer not available for variable " + name);
    } // else
  
    return (producer);
  
  } // getChunkProducer

  ////////////////////////////////////////////////////////////

  /**
   * Creates a data variable preview.  A preview contains all
   * metadata but no data value array.  The preview can be used in a
   * filtering loop to determine if the reading of the variable data
   * is desired, as I/O can be a time-intensive operation.
   *
   * @param name the name of the variable to get.
   *
   * @return a data variable object with data value
   * array of length 1.
   *
   * @throws IOException if the data source had I/O errors, or the
   * variable was not found.
   *
   * @see #getVariable
   */
  public DataVariable getPreview (
    String name
  ) throws IOException {

    // Check index
    // -----------
    int index = getIndex (name);
    if (index == -1) throw new IOException ("Variable not found: " + name);

    // Get preview
    // -----------
    return (getPreview (index));

  } // getPreview

  ////////////////////////////////////////////////////////////

  /**
   * Creates a data variable object.  The full data is read
   * into the object.
   *
   * @param index the index of the variable to get.  Indexing
   * starts at 0.
   *
   * @return a data variable object with full data value
   * array.
   *
   * @throws IOException if the data source had I/O errors.
   *
   * @see #getPreview
   */
  public abstract DataVariable getVariable (
    int index
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a data variable object.  The full data is read
   * into the object.
   *
   * @param name the name of the variable to get.
   *
   * @return a data variable object with full data value
   * array.
   *
   * @throws IOException if the data source had I/O errors, or the
   * variable was not found.
   *
   * @see #getPreview
   */
  public DataVariable getVariable (
    String name
  ) throws IOException {

    // Check index
    // -----------
    int index = getIndex (name);
    if (index == -1) throw new IOException ("Variable not found: " + name);

    // Get variable
    // ------------
    return (getVariable (index));

  } // getVariable

  ////////////////////////////////////////////////////////////

  /**
   * Closes the reader and frees any resources.
   *
   * @throws IOException if the data source had I/O errors.
   */
  public abstract void close () throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Closes the resources associated with the data source.
   */
  @Override
  protected void finalize () throws Throwable {

    try { close(); }
    finally { super.finalize(); }

  } // finalize

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of all 2D grid variable names in this reader.
   *
   * @return the list of grid variable names.
   *
   * @throws IOException if an error occurred getting the list of grids.
   */
  public List<String> getAllGrids () throws IOException {

    List<String> gridList = new LinkedList<>();
    for (int i = 0; i < variables.length; i++) {
      DataVariable variable = getPreview (i);
      if (variable instanceof Grid)
        gridList.add (variable.getName());
    } // for

    return (gridList);

  } // getAllGrids

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of all variable names in this reader.
   *
   * @return the list of variable names.
   */
  public List<String> getAllVariables () {

    return (Arrays.asList (variables));

  } // getAllVariables

  ////////////////////////////////////////////////////////////

  /**
   * Updates the navigation transform for the specified list of
   * variables (optional operation). Classes that override this method
   * should also override {@link #canUpdateNavigation}.
   *
   * @param variableNames the list of variable names to update.
   * @param affine the navigation transform to apply.  If null, the
   * navigation is reset to the identity.
   *
   * @throws IOException if an error occurred writing the file metadata.
   * @throws UnsupportedOperationException if the class does not
   * support navigation transform updates.
   *
   * @see #canUpdateNavigation
   */
  public void updateNavigation (
    List variableNames,
    AffineTransform affine
  ) throws IOException {

    throw new UnsupportedOperationException (getClass().getName());

  } // updateNavigation

  ////////////////////////////////////////////////////////////

  /**
   * Determines the ability of the file format to have its navigation
   * updated.  Classes that override {@link #updateNavigation} should
   * also override this method.
   *
   * @return true if the navigation can be updated, or false if not.
   *
   * @see #updateNavigation
   */
  public boolean canUpdateNavigation () {

    return (false);
  
  } // canUpdateNavigation

  ////////////////////////////////////////////////////////////

  /**
   * Gets the detailed raw metadata.  Each key is an attribute name
   * and each value the corresponding wrapped primitive, primitive
   * array, or string value.
   */
  public Map getRawMetadata () { return (rawMetadataMap); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the detailed raw metadata for a variable.
   * 
   * @param index the index of the variable for raw metadata.
   * 
   * @return the map of attribute name to attribute value.
   * 
   * @see #getRawMetadata
   * @since 3.7.1
   * 
   * @throws IOException if the data source had I/O errors.
   */
  public Map<String, Object> getRawMetadata (int index) throws IOException {

    return ((Map<String, Object>) getPreview (index).getMetadataMap());

  } // getRawMetadata

  ////////////////////////////////////////////////////////////

  /**
   * Gets the NetCDF CDM style coordinate systems accessed by
   * this reader.
   *
   * @return the list of coordinate systems, possibly empty.
   */
  public List<CoordinateSystem> getCoordinateSystems () {

    return (new ArrayList<CoordinateSystem>());

  } // getCoordinateSystems

  ////////////////////////////////////////////////////////////

  /**
   * Gets the variable names for the specified NetCDF CDM style
   * coordinate systems accessed by this reader.
   *
   * @return the list of variable names, possibly empty.
   */
  public List<String> getVariablesForSystem (
    CoordinateSystem system
  ) {

    return (new ArrayList<String>());

  } // getVariablesForSystem

  ////////////////////////////////////////////////////////////

  /**
   * Searches for a variable in a dataset using a set of search terms.
   * 
   * @param searchTerms the search terms to use.
   * @param minScore the minimum acceptable matching score in the range [0..1].
   * 
   * @return the variable name with the best match for the search terms, or 
   * null if no variables could be found.  Match quality is measured based on how
   * similar the variable name or its long name are to one of to search terms.
   * 
   * @since 3.8.1
   */
  public String findVariable (
    List<String> searchTerms,
    double minScore
  ) throws IOException {

    var variableList = this.getAllGrids();
    double highScore = 0;
    String match = null;
    for (var name : variableList) {

      var variable = this.getPreview (name);

      for (var term : searchTerms) {
        double score = MetadataServices.similarity (term, name);
        var longName = variable.getLongName();
        if (longName != null && !longName.isEmpty())
          score = Math.max (MetadataServices.similarity (term, longName), score);
        if (score > highScore) {
          highScore = score;
          match = name;
        } // if
      } // for

    } // for

    if (highScore < minScore) match = null;

    if (match != null) LOGGER.fine ("Found variable " + match + " with score " + highScore + " (minimum " + minScore + ")");

    return (match);

  } // findVariable

  ////////////////////////////////////////////////////////////

} // EarthDataReader class

////////////////////////////////////////////////////////////////////////
