////////////////////////////////////////////////////////////////////////
/*

     File: Topology.java
   Author: Peter Hollemans
     Date: 2019/09/10

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * The <code>Topology</code> class contains a number of convenience methods
 * for working with coordinates in a topology.
 *
 * @author Peter Hollemans
 * @since 3.5.1
 */
public class Topology {

  // Constants
  // ---------
  
  /** The coordinate precision scaling factor. */
  private static final double SCALE = 1e6;
  
  /** The smallest increment that differentiates two coordinates. */
  public static final double EPSILON = 1e-6;

  // Variables
  // ---------

  /** The precision model to use for coordinates. */
  private static PrecisionModel model;

  /** The factory to use for generating geometries. */
  private static GeometryFactory factory;

  ////////////////////////////////////////////////////////////

  static {
  
    model = new PrecisionModel (SCALE);
    factory = new GeometryFactory (model);
  
  } // static
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the precision model for topologies.  The model has a precision
   * for locations given by the {@link #EPSILON} value.
   *
   * @return the precision model.
   */
  public static PrecisionModel getModel () { return (model); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the factory for topologies that uses the precision model
   * given by {@link #getModel}.
   *
   * @return the factory for creating topology objects.
   */
  public static GeometryFactory getFactory () { return (factory); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a coordinate for the specified values that conforms to
   * the precision model returned by {@link #getModel}.
   *
   * @param x the coordinate x value.
   * @param y the coordinate y value.
   *
   * @return the coordinate with values rounded to the precision model
   * accuracy.
   */
  public static Coordinate createCoordinate (
    double x,
    double y
  ) {

    Coordinate coord = new Coordinate (x, y);
    model.makePrecise (coord);
    
    return (coord);

  } // createCoordinate

  ////////////////////////////////////////////////////////////

} // Topology class

////////////////////////////////////////////////////////////////////////
