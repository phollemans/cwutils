////////////////////////////////////////////////////////////////////////
/*

     File: Line.java
   Author: Peter Hollemans
     Date: 2002/06/06

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
package noaa.coastwatch.util;

// Imports
// -------
import java.text.NumberFormat;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;

/**
 * The 1D line class is a special form of data variable with
 * one dimension.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class Line
  extends DataVariable {

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new line with the specified properties.
   *
   * @see DataVariable
   */
  public Line (
    String name,
    String longName,
    String units,
    int length,
    Object data,
    NumberFormat format,
    double[] scaling,
    Object missing
  ) {

    // Chain to parent
    // ---------------
    super (name, longName, units, new int[] {length}, data, 
      format, scaling, missing);

  } // Line constructor

  ////////////////////////////////////////////////////////////

  public double interpolate (
    DataLocation loc
  ) {

    // Check containment
    // -----------------
    if (!loc.isContained (dims)) return (Double.NaN);

    // Get upper and lower data values
    // -------------------------------
    double a = getValue ((int) Math.floor (loc.get(0)));
    double b = getValue ((int) Math.ceil (loc.get(0)));

    // Perform interpolation
    // ---------------------
    double dx = loc.get(0) - Math.floor (loc.get(0));
    double val = (dx*b + (1-dx)*a);
    return (val);

  } // interpolate

  ////////////////////////////////////////////////////////////

} // Line class

////////////////////////////////////////////////////////////////////////
