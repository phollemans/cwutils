////////////////////////////////////////////////////////////////////////
/*

     File: ArchiveHeader.java
   Author: Peter Hollemans
     Date: 2007/08/25

  CoastWatch Software Library and Utilities
  Copyright (c) 2007 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.io.noaa1b;

// Imports
// -------

/**
 * The <code>ArchiveHeader</code> interface is for reading NOAA
 * 1b archive header data.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public interface ArchiveHeader {

  /** Get the sensor channel data selection flags. */
  public boolean[] getChannelSelection();

  /** Gets the sensor word size in bits. */
  public int getSensorWordSize();

  /** Gets the header size in bytes. */
  public int getHeaderSize();

} // ArchiveHeader interface

////////////////////////////////////////////////////////////////////////
