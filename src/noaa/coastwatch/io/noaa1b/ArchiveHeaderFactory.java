////////////////////////////////////////////////////////////////////////
/*

     File: ArchiveHeaderFactory.java
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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import noaa.coastwatch.io.noaa1b.ARSHeader;
import noaa.coastwatch.io.noaa1b.ArchiveHeader;
import noaa.coastwatch.io.noaa1b.TBMHeader;

/**
 * The <code>ArchiveHeaderFactory</code> creates archive header
 * objects using the byte data at the beginning of a NOAA 1b file.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class ArchiveHeaderFactory {

  ////////////////////////////////////////////////////////////
  
  /** 
   * Creates a new archive header.
   *
   * @param channel the channel of byte data to read.
   *
   * @return an archive header object, or null of no header could
   * be found that is appropriate for the data.
   *
   * @throws IOException if an error occurred reading the byte
   * data.
   */
  public static ArchiveHeader create (
    ByteChannel channel
  ) throws IOException {

    // Read data
    // ---------
    ByteBuffer inputBuffer = ByteBuffer.allocate (512);
    channel.read (inputBuffer);
    
    // Create archive header
    // ---------------------
    ArchiveHeader header = null;

    try { header = new TBMHeader (inputBuffer); }
    catch (Exception e) { }
    if (header != null) return (header);

    try { header = new ARSHeader (inputBuffer); }
    catch (Exception e) { }
    if (header != null) return (header);

    return (null);

  } // create

  ////////////////////////////////////////////////////////////

} // ArchiveHeaderFactory class

////////////////////////////////////////////////////////////////////////
