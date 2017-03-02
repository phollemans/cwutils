////////////////////////////////////////////////////////////////////////
/*

     File: HDFSD.java
   Author: Peter Hollemans
     Date: 2002/11/07

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
package noaa.coastwatch.io;

/**
 * The HDF scientific dataset (SD) interface sets the methods required
 * for all HDF SD classes.  Specifically, all HDF scientific datasets
 * must be able to return the SDID.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public interface HDFSD {

  /** Gets the HDF scientific dataset ID. */
  public int getSDID ();

  /** Gets the HDF scientific dataset file name. */
  public String getFilename ();

} // HDFSD interface

////////////////////////////////////////////////////////////////////////
