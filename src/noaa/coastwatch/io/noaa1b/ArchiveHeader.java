////////////////////////////////////////////////////////////////////////
/*
     FILE: ArchiveHeader.java
  PURPOSE: Reads NOAA 1b format archive header data.
   AUTHOR: Peter Hollemans
     DATE: 2007/08/25
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2007, USDOC/NOAA/NESDIS CoastWatch

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
