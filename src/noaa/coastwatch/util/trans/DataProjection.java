////////////////////////////////////////////////////////////////////////
/*

     File: DataProjection.java
   Author: Peter Hollemans
     Date: 2002/05/31

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
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.util.Arrays;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.Statistics;

import java.util.logging.Logger;

/**
 * The <code>DataProjection</code> class implements earth transform
 * calculations for data coordinates with explicit latitude and
 * longitude data.  The only possible operation is translation from
 * data coordinates to geographic coordinates -- the reverse is not
 * implemented.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class DataProjection 
  extends EarthTransform2D {

  private static final Logger LOGGER = Logger.getLogger (DataProjection.class.getName());

  // Constants
  // ---------
  /** Projection description string. */
  public final static String DESCRIPTION = "data";  

  // Variables
  // ---------
  /** Latitude and longitude variables. */
  private DataVariable lat, lon;

  ////////////////////////////////////////////////////////////

  /** Gets the latitude variable used in this projection. */
  public DataVariable getLat() { return (lat); }

  ////////////////////////////////////////////////////////////

  /** Gets the longitude variable used in this projection. */
  public DataVariable getLon() { return (lon); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a data projection from the specified latitude and
   * longitude data.
   *
   * @param lat a data variable containing latitude data.
   * @param lon a data variable containing longitude data.
   */
  public DataProjection (
    DataVariable lat,
    DataVariable lon
  ) {

    // Initialize variables
    // --------------------
    this.lat = lat;
    this.lon = lon;
    this.dims = lat.getDimensions();

  } // DataProjection constructor

  ////////////////////////////////////////////////////////////

  @Override
  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    earthLoc.setCoords (lat.getValue (dataLoc), lon.getValue (dataLoc));

  } // transformImpl

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    dataLoc.set (Grid.ROWS, Double.NaN);
    dataLoc.set (Grid.COLS, Double.NaN);

  } // transformImpl

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isInvertible () { return (false); }

  ////////////////////////////////////////////////////////////

  /**
   * Compares the specified object with this data projection for
   * equality.  The latitudes and longitudes of the two data
   * projections are compared value by value.
   *
   * @param obj the object to be compared for equality.
   *
   * @return true if the data projections are equivalent, or false if
   * not.  
   */
  @Override
  public boolean equals (
    Object obj
  ) {

    // Check object instance
    // ---------------------
    if (!(obj instanceof DataProjection)) return (false);

    // Check each lat/lon value
    // ------------------------
    DataProjection data = (DataProjection) obj;

    // int n = this.lat.getValues ();
    // for (int i = 0; i < n; i++) {
    //   if (this.lat.getValue (i) != data.lat.getValue (i)) return (false);
    //   if (this.lon.getValue (i) != data.lon.getValue (i)) return (false);
    // } // for

    // We test here if the projections even have the same dimensions
    if (!Arrays.equals (this.dims, data.dims)) return (false);

    // Since this operation can take a very long time for large data 
    // projections, we calculate the statistics of 1% of the data instead,
    // then compare the results

    LOGGER.warning ("Comparing data projections using statistics, results may not be exact");

    boolean isEqual = 
      equalStats (this.lat.getStatistics (0.01), data.lat.getStatistics (0.01)) &&
      equalStats (this.lon.getStatistics (0.01), data.lon.getStatistics (0.01));

    return (isEqual);

  } // equals

  ////////////////////////////////////////////////////////////

  /**
   * Compares two stats objects for equality on the main statistics.
   * 
   * @param a the first stats object.
   * @param b the second stats object.
   * 
   * @return true if the stats are equal in valid, min, max, mean, and stdev.
   * 
   * @since 3.8.0
   */
  private boolean equalStats (Statistics a, Statistics b) {

    return (
      a.getValid() == b.getValid() &&
      equalDouble (a.getMin(), b.getMin()) &&
      equalDouble (a.getMax(), b.getMax()) &&
      equalDouble (a.getMean(), b.getMean()) &&
      equalDouble (a.getStdev(), b.getStdev())
    );

  } // equalStats

  ////////////////////////////////////////////////////////////

  /**
   * Compares two doubles for equality.
   * 
   * @param a the first double.
   * @param b the second double.
   *
   * @return true if the doubles are equal or both ar NaN, or false
   * if they are different.
   * 
   * @since 3.8.0
   */
  private boolean equalDouble (double a, double b) {

    var aNaN = Double.isNaN (a);
    var bNaN = Double.isNaN (b);
    return ((aNaN && bNaN) || (a == b));

  } // equalDouble

  ////////////////////////////////////////////////////////////

} // DataProjection class

////////////////////////////////////////////////////////////////////////
