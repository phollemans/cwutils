////////////////////////////////////////////////////////////////////////
/*
     FILE: ArchiveHeaderFactory.java
  PURPOSE: Create NOAA 1b archive headers from byte data.
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
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * The <code>ArchiveHeaderFactory</code> creates archive header
 * objects using the byte data at the beginning of a NOAA 1b file.
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
