////////////////////////////////////////////////////////////////////////
/*

     File: SensorScanProjection.java
   Author: Peter Hollemans
     Date: 2005/01/18, PFH

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
package noaa.coastwatch.util.trans;

/**
 * The <code>SensorScanProjection</code> is used to provide Earth
 * transform calculations for various types of satellite sensors that
 * cannot be represented by one of the standard map projections.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public abstract class SensorScanProjection 
  extends EarthTransform2D {

  // Constants
  // ---------

  /** Projection description string. */
  public static final String DESCRIPTION = "sensor_scan";  

  // Variables
  // ---------

  /** The sensor scan parameters. */
  protected double[] parameters;

  ////////////////////////////////////////////////////////////

  /** Provides a description of this class. */
  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  /** Gets a description of this sensor scan type. */
  public abstract String getSensorType ();

  ////////////////////////////////////////////////////////////

  /** Gets a code for this sensor scan type. */
  public abstract int getSensorCode ();

  ////////////////////////////////////////////////////////////

  /** Gets the parameters used to create this sensor scan projection. */
  public double[] getParameters () { return ((double[]) parameters.clone()); }

  ////////////////////////////////////////////////////////////

} // SensorScanProjection class

////////////////////////////////////////////////////////////////////////
