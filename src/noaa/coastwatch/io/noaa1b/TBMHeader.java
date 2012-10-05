////////////////////////////////////////////////////////////////////////
/*
     FILE: TBMHeader.java
  PURPOSE: Reads NOAA 1b format Terabit Memory header data.
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
import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * The <code>TBMHeader</code> class reads Terabit Memory header
 * data from NOAA 1b data files.
 */
public class TBMHeader extends AbstractArchiveHeader {

  // Constants
  // ---------

  /** The header size in bytes. */
  private static final int HEADER_SIZE = 122;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new header using the specified byte data.
   *
   * @param inputBuffer the buffer to read for byte data.
   *
   * @throws IOException if an error occurred checking the data.
   */
  public TBMHeader (
    ByteBuffer inputBuffer
  ) throws IOException {

    super (inputBuffer);

  } // TBMHeader constructor

  ////////////////////////////////////////////////////////////

  protected boolean isCompatible (ByteBuffer inputBuffer) {

    // Check for non-printable ASCII characters
    // ----------------------------------------
    boolean printable = true;
    for (int i = 0; i < HEADER_SIZE; i++) {

      // Skip the dataset header (sometimes garbage)
      // -------------------------------------------
      if (i >= 30 && i <= 73) continue;

      // Skip the channels selected (in binary)
      // --------------------------------------
      else if (i >= 97 && i <= 116) continue;

      // Skip the rest of the header (all 0)
      // -----------------------------------
      else if (i > 118) continue;

      // Check for printable
      // -------------------
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

} // TBMHeader class

////////////////////////////////////////////////////////////////////////
