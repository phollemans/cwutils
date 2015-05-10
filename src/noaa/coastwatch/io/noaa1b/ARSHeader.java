////////////////////////////////////////////////////////////////////////
/*
     FILE: ARSHeader.java
  PURPOSE: Reads NOAA 1b format Archive Retrieval System header data.
   AUTHOR: Peter Hollemans
     DATE: 2007/08/27
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
import java.io.IOException;
import java.nio.ByteBuffer;
import noaa.coastwatch.io.noaa1b.AbstractArchiveHeader;
import noaa.coastwatch.io.noaa1b.NOAA1bServices;

/**
 * The <code>ARSHeader</code> class reads Archive Retrieval System header
 * data from NOAA 1b data files.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class ARSHeader extends AbstractArchiveHeader {

  // Constants
  // ---------

  /** The header size in bytes. */
  private static final int HEADER_SIZE = 512;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new header using the specified byte data.
   *
   * @param inputBuffer the buffer to read for byte data.
   *
   * @throws IOException if an error occurred checking the data.
   */
  public ARSHeader (
    ByteBuffer inputBuffer
  ) throws IOException {

    super (inputBuffer);

  } // ARSHeader constructor

  ////////////////////////////////////////////////////////////

  protected boolean isCompatible (ByteBuffer inputBuffer) {

    // Check for non-printable ASCII characters
    // ----------------------------------------
    boolean printable = true;
    for (int i = 0; i < HEADER_SIZE; i++) {
      if (!NOAA1bServices.isPrint (inputBuffer.get (i))) {
        printable = false;
        break;
      } // if
    } // for

    return (printable);

  } // isCompatible

  ////////////////////////////////////////////////////////////

  public int getHeaderSize() { return (HEADER_SIZE); }

  ////////////////////////////////////////////////////////////

} // ARSHeader class

////////////////////////////////////////////////////////////////////////
