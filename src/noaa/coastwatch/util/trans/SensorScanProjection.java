////////////////////////////////////////////////////////////////////////
/*
     FILE: SensorScanProjection.java
  PURPOSE: Abstract parent class for all sensor scan projections.
   AUTHOR: Peter Hollemans
     DATE: 2005/01/18, PFH
  CHANGES: 2005/05/20, PFH, now extends 2D transform

  CoastWatch Software Library and Utilities
  Copyright 2005, USDOC/NOAA/NESDIS CoastWatch

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
