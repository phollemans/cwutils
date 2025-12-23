////////////////////////////////////////////////////////////////////////
/*

     File: BitmaskOverlay.java
   Author: Peter Hollemans
     Date: 2002/09/23

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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.GridContainerOverlay;
import noaa.coastwatch.render.MaskOverlay;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.Grid;

/**
 * A <code>BitmaskOverlay</code> annotates a data view using a
 * data grid and an integer bit mask.  The mask is computed by
 * logically anding the bit mask value with each integer-cast
 * data value in the grid.  If the result is non-zero at a given
 * location, the data is masked.  The bit mask is effectively a
 * selection mechanism for byte or integer valued data that
 * allows certain bits in the data values to act as overlay
 * graphics planes.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class BitmaskOverlay 
  extends MaskOverlay
  implements GridContainerOverlay {

  // Constants
  // ---------

  /** The serialization constant. */
  private static final long serialVersionUID = -1831067398269937724L;

  // Variables
  // ---------

  /** The data grid to use for masking. */
  private transient Grid grid;

  /** The bit mask value. */
  private int mask;

  /** The reader to use for data. */
  private transient EarthDataReader reader;

  /** The list of available data variables from the reader. */
  private transient List variableList;

  /** The name of the grid variable. */
  private String gridName;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data source for grid data.  The reader and variable list
   * must contain a data grid with the current grid name.
   *
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   */
  public void setDataSource (
    EarthDataReader reader,
    List variableList
  ) {

    this.reader = reader;
    this.variableList = variableList;
    setGridName (gridName);

  } // setDataSource

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the internal grid based on the grid name. This method may
   * only be used if the overlay was constructed using a reader and
   * grid name list.
   */
  private void setGrid () {

    // Check for valid list
    // --------------------
    if (variableList == null)
      throw new IllegalStateException ("Cannot set grid when list is null");

    // Set grid variable
    // -----------------
    try { this.grid = (Grid) reader.getVariable (gridName); } 
    catch (IOException e) { throw (new RuntimeException (e)); }

  } // setGrid

  ////////////////////////////////////////////////////////////

  /** Gets the bit mask value. */
  public int getMask () { return (mask); }

  ////////////////////////////////////////////////////////////

  /** Sets the bit mask value. */
  public void setMask (
    int mask
  ) { 

    this.mask = mask;
    invalidate();

  } // setMask

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the reader used to fetch the data for this bitmask, or null
   * if no reader was explicitly given to the constructor.
   */
  public EarthDataReader getReader () { return (reader); }

  ////////////////////////////////////////////////////////////

  /** Gets the grid variable name. */
  public String getGridName () { return (gridName); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the possible grid variable names, or null if no list was
   * explicitly given to the constructor.
   */
  public List getGridNameValues () { 

    return (variableList);

  } // getGridNameValues

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the grid variable based on the name.  This method may only
   * be used if the overlay was constructed using a reader and grid
   * name list.
   */
  public void setGridName (String name) { 

    // Check for valid list
    // --------------------
    if (variableList == null) 
      throw new IllegalStateException ("Cannot set grid name when variable list is null");

    // Check for valid grid
    // --------------------    
    boolean found = variableList.contains (name);
    if (!found) {

      // Do some extra work and try to find a base variable name that 
      // matches the requested name.  That means that we remove any leading
      // group names before we do the name comparison.
      for (var varName : (List<String>) variableList) {
        var baseVarName = varName.contains ("/") ? varName.substring (varName.lastIndexOf ("/") + 1) : varName;
        var baseNewName = name.contains ("/") ? name.substring (name.lastIndexOf ("/") + 1) : name;
        if (baseVarName.equals (baseNewName)) { name = varName; found = true; break; }
      } // for

    } // if
    if (!found) throw new IllegalArgumentException ("Cannot find variable " + name);

    // Set internals
    // -------------
    gridName = name;
    grid = null;
    invalidate();

  } // setGridName

  ////////////////////////////////////////////////////////////

  /** Gets the active grid variable. */
  public Grid getGrid () { 

    if (grid == null) setGrid();
    return (grid); 

  } // getGrid

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new bitmask overlay.  The layer number is
   * initialized to 0.  If this constructor is used, then the
   * {@link #setGridName} method performs no operation and {@link
   * #getGridNameValues} returns null.
   * 
   * @param color the overlay color.
   * @param grid the grid to use for data.
   * @param mask the bit mask value.
   */
  public BitmaskOverlay (
    Color color,
    Grid grid,
    int mask
  ) { 

    super (color);
    this.grid = grid;
    this.gridName = grid.getName();
    this.mask = mask;

  } // BitmaskOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new bitmask overlay.  The layer number is
   * initialized to 0.
   * 
   * @param color the overlay color.
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   * @param gridName the initial grid name from the list.
   * @param mask the bit mask value.
   */
  public BitmaskOverlay (
    Color color,
    EarthDataReader reader,
    List variableList,
    String gridName,
    int mask
  ) { 

    super (color);
    this.reader = reader;
    this.variableList = variableList;
    this.mask = mask;
    setGridName (gridName);

  } // BitmaskOverlay constructor

  ////////////////////////////////////////////////////////////

  @Override
  protected void prepareData () {

    // Check for valid grid
    // --------------------
    if (grid == null) setGrid();

  } // prepareData

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isMasked (
    DataLocation loc,
    boolean isNavigated
  ) {

    int value = (int) (isNavigated ? 
      grid.getValue ((int) loc.get (Grid.ROWS), (int) loc.get (Grid.COLS)) : 
      grid.getValue (loc)
    );
    return ((value & mask) != 0);

  } // isMasked

  ////////////////////////////////////////////////////////////

  @Override
  protected boolean isCompatible (
    EarthDataView view
  ) {

    return (view.hasCompatibleCaches (grid));

  } // isCompatible

  ////////////////////////////////////////////////////////////

  @Override
  public List<Grid> getGridList () { 

    return (Arrays.asList (new Grid[] {getGrid()})); 

  } // getGridList

  ////////////////////////////////////////////////////////////

} // BitmaskOverlay class

////////////////////////////////////////////////////////////////////////
