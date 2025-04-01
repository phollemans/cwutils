/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.io;

import java.util.List;
import java.util.Iterator;

import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform2D;

/**
 * <p>An <code>EarthGridSet</code> provides an interface for data to be modelled
 * as a set of 2D {@link noaa.coastwatch.util.Grid} objects, where each grid 
 * can be selected by specifying its unique position on a set of axes in a higher
 * dimensional space.  As an example, suppose that 
 * there are two axes that represent time and height, and that the time 
 * axis has 6 possible values:</p>
 * <ul>
 *   <li> [0] = Jan 1, 2015 </li>
 *   <li> [1] = Feb 1, 2015 </li>
 *   <li> [2] = Mar 1, 2015 </li>
 *   <li> [3] = Apr 1, 2015 </li>
 *   <li> [4] = May 1, 2015 </li>
 *   <li> [5] = Jun 1, 2015 </li>
 * </ul>
 * <p>and the height axis has 20 values:</p>
 * <ul> 
 *   <li> [0] = 0 feet </li>
 *   <li> [1] = 50 feet </li>
 *   <li> [2] = 100 feet </li>
 *   <li> ... </li>
 *   <li> [18] = 900 feet </li>
 *   <li> [19] = 1000 feet </li>
 * </ul>
 * <p>To select the 2D grid for May 1, 2015 on the time axis and 100 feet on
 * the height axis, the axis position would be time=2, height=2.</p>
 * 
 * @author Peter Hollemans
 * @since 4.0.1
 */
public interface EarthGridSet {

  /** 
   * An <code>Axis</code> holds information about one of the axes used to 
   * select a 2D grid in the set.
   */
  public class Axis<T> implements Iterable<T> {
    private String name;
    private String axisType;
    private String units;
    private List<T> values;
    private Class<T> dataType;

    /** 
     * Creates an axis from its properties and data.
     * 
     * @param name the axis name: time, altitude, etc.
     * @param axisType the axis type: Time, Height, Spectral, Pressure, etc.
     * @param units the axis units: seconds since ..., meters, millibars, etc.
     * @param values the list of axis coordinate values.
     * @param dataType the datatype class for the axis values.
     */
    public Axis (String name, String axisType, String units, List<T> values, Class<T> dataType) {
      this.name = name;
      this.axisType = axisType;
      this.units = units;
      this.values = values;
      this.dataType = dataType;
    } // Axis 

    public String getName() { return (name); }
    public String getAxisType() { return (axisType); }
    public String getUnits() { return (units); }
    @Override
    public Iterator<T> iterator() { return (values.iterator()); }
    public T getValue (int index) { return (values.get (index)); }
    public int getSize() { return (values.size()); }
    public Class<T> getDataType() { return (dataType); }
    public String toString () { return (name); }

  } // Axis class

  /**
   * Gets a list of all grid variable names in this set.
   * 
   * @return the variable names.
   */
  public List<String> getVariables();

  /**
   * Gets a list of axes used to specify the version of a grid needed
   * from the {@link #accessGrid} method.
   * 
   * @param varName the variable name for the axes.
   * 
   * @return the list of axes, possibly empty if the variable is just a single
   * grid and has no axes.
   */
  public List<Axis> getAxes (String varName);

  /**
   * Gets a version of a grid specified by a set of axes.
   * 
   * @param varName the variable name for the grid.
   * @param axisIndexList the list of axis indicies, one for each axis,
   * that specifies which grid to retrieve.  The list may be empty if the
   * variable has no axes.
   * 
   * @return the grid corresponding to the variable name and axes specified.
   */
  public Grid accessGrid (String varName, List<Integer> axisIndexList);

  /**
   * Gets a version of a grid specified by a set of axes and a subset 
   * specification.
   * 
   * @param varName the variable name for the grid.
   * @param axisIndexList the list of axis indicies, one for each axis,
   * that specifies which grid to retrieve.  The list may be empty if the
   * variable has no axes.
   * @param start the starting spatial data coordinates.
   * @param stride the spatial data stride.
   * @param length the total number of values to access in each spatial 
   * dimension.
   *
   * @return the grid corresponding to the variable name, axes, and subset
   * specified.
   * 
   * @since 4.1.0
   */
  default public Grid accessGridSubset (
    String varName,
    List<Integer> axisIndexList,
    int[] start,
    int[] stride,
    int[] length
  ) {

    throw new UnsupportedOperationException();

  } // accessGridSubset

  /**
   * Returns the status of support for calling the {@link #accessGridSubset} 
   * method.
   * 
   * @since 4.1.0
   */
  default public boolean gridSubsetSupported () { return (false); }

  /**
   * Releases the resources used by a grid accessed by this set.
   * 
   * @param grid the grid to release.
   */
  public void releaseGrid (Grid grid);

  /**
   * Gets the 2D earth transform for the grids in a variable.
   * 
   * @param varName the variable name for the earth transform.
   * 
   * @return the earth transform.
   */
  public EarthTransform2D getTransform (String varName);

  /**
   * Gets the institute/origin of the data in the dataset.
   * 
   * @return the data institute/origin.
   */
  public String getOrigin();

  /**
   * Gets the source/platform of the data in the dataset.
   * 
   * @return the data source/platform.
   */
  public String getSource();

  /** Disposes of the resources used by this grid set. */
  public void dispose();

} // EarthGridSet interface
