////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataReader.java
  PURPOSE: Abstract class to set the functionality of all data
           reader subclasses.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/15
  CHANGES: 2002/05/14, PFH, added javadoc, package
           2002/06/07, PFH, added getIndex
           2002/07/22, PFH, added getVariable(String), getPreview(String)
           2002/11/12, PFH, added getSceneTime
           2004/02/15, PFH, added getSource() and related constructor
           2004/02/16, PFH, added better excpetion throwing to
             getPreview(String), getVariable (String)
           2004/03/11, PFH, added getAllGrids() method
           2004/04/10, PFH, added getDataFormat() method
           2004/05/07, PFH, added statistics map
           2004/06/08, PFH, added updateNavigation() method
           2004/09/09, PFH, renamed SatelliteDataReader to EarthDataReader
           2005/01/28, PFH, added setDataProjection()
           2005/04/22, PFH, added getAllVariables()
           2005/06/22, PFH, added getRawMetadata()
           2005/07/03, PFH, added extra docs for implementing classes
           2006/11/03, PFH, added setUnitsMap() for default units
           2010/03/15, PFH, added getCoordinateSystems()
           2012/12/04, PFH, added canUpdateNavigation()

  CoastWatch Software Library and Utilities
  Copyright 1998-2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.util.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;
import ucar.nc2.dataset.*;

/**
 * All Earth data readers obtain Earth data from a data source and
 * provide it to the user in a consistent format.  An Earth data reader
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
 *   <li>{@link #getPreview(int)}</li>
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

  // Variables
  // ---------
  /** Earth data info object. */
  protected EarthDataInfo info;

  /** Earth data variable names. */
  protected String[] variables;

  /** The data source. */
  private String source;

  /** The map of variable name to statistics. */
  private Map statsMap;

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
   * readers that deal with Earth transforms stored as explicit Earth
   * location data.  When true, the data projection flag forces the
   * reader to return a <code>DataProjection</code> object for the
   * Earth transform when reading explicit latitude and longitude
   * data, rather than interpolating the data and returning a
   * <code>SwathProjection</code>.  The main difference is that a
   * <code>DataProjection</code> cannot be used to transform an
   * <code>EarthLocation</code> object into a
   * <code>DataLocation</code> object.  Generally, setting this flag
   * is only desirable if there are inherent problems with
   * interpolating the Earth location data.  By default, the reader is
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
  public List getStatisticsVariables () {

    return (new ArrayList (statsMap.keySet()));

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

    return ((Statistics) statsMap.get (name));

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

  /** Gets the Earth data info object. */
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
   * Creates a new Earth data reader.
   * 
   * @param source the data source.
   */
  protected EarthDataReader (
    String source
  ) {

    // Initialize
    // ----------
    this.source = source;
    this.statsMap = new LinkedHashMap();

  } // EarthDataReader constructor

  ////////////////////////////////////////////////////////////

  /** Get the Earth data source. */
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
    if (var == null || !(var instanceof Grid)) return ("unknown");

    // Get scene time
    // --------------
    return (info.getSceneTime (var.getDimensions()));

  } // getSceneTime

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

    // Modify units
    // ------------
    if (unitsMap != null && unitsMap.containsKey (varName)) {
      try {
        preview.convertUnits ((String) unitsMap.get (varName));
      } // try
      catch (IllegalArgumentException e) {
        System.err.println (this.getClass() + 
          ": Warning: " + e.getMessage() + ", units conversion failed");
      } // catch
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
  protected void finalize () {

    try { close(); }
    catch (Exception e) { } 

  } // finalize

  ////////////////////////////////////////////////////////////

  /** Returns a list of all grid (2D) variable names in this reader. */
  public List getAllGrids () throws IOException {

    List gridList = new LinkedList();
    for (int i = 0; i < variables.length; i++) {
      DataVariable variable = getPreview (i);
      if (variable instanceof Grid)
        gridList.add (variable.getName());
    } // for

    return (gridList);

  } // getAllGrids

  ////////////////////////////////////////////////////////////

  /** Returns a list of all variable names in this reader. */
  public List getAllVariables () {

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

} // EarthDataReader class

////////////////////////////////////////////////////////////////////////
