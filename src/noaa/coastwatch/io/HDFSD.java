////////////////////////////////////////////////////////////////////////
/*
     FILE: HDFSD.java
  PURPOSE: An interface for all HDF SD dataset classes.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/07
  CHANGES: 2003/04/21, PFH, added getFilename

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

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
