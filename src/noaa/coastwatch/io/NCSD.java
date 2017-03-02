////////////////////////////////////////////////////////////////////////
/*

     File: NCSD.java
   Author: Peter Hollemans
     Date: 2013/03/10

  CoastWatch Software Library and Utilities
  Copyright (c) 2013 National Oceanic and Atmospheric Administration
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
