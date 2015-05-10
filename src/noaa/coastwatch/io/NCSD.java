////////////////////////////////////////////////////////////////////////
/*
     FILE: NCSD.java
  PURPOSE: An interface for all NetCDF SD dataset classes.
   AUTHOR: Peter Hollemans
     DATE: 2013/03/10
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2013, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import ucar.nc2.dataset.NetcdfDataset;

/**
 * The NetCDF scientific dataset (SD) interface sets the methods required
 * for all NetCDF SD classes.  Specifically, all NetCDF scientific datasets
 * must be able to return the NetCDF dataset object.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public interface NCSD {

  /** Gets the NetCDF scientific dataset. */
  public NetcdfDataset getDataset ();

  /** Gets the NetCDF scientific dataset file name. */
  public String getFilename ();

} // NCSD interface

////////////////////////////////////////////////////////////////////////
