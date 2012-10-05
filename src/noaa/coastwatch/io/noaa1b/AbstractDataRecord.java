////////////////////////////////////////////////////////////////////////
/*
     FILE: AbstractDataRecord.java
  PURPOSE: Reads NOAA 1b format data records.
   AUTHOR: Peter Hollemans
     DATE: 2007/10/11
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
import java.nio.*;
import java.util.*;
import noaa.coastwatch.io.*;

/**
 * The <code>AbstractDataRecord</code> class reads NOAA 1b data
 * records.
 */
public abstract class AbstractDataRecord implements DataRecord {

  // Variables
  // ---------

  /** The binary reader for this class. */
  protected BinaryStreamReader reader;

  /** The data buffer to read. */
  protected ByteBuffer buffer;

  /** The header record for this data record. */
  protected DataHeader header;

  ////////////////////////////////////////////////////////////

  public int getScanLine() {  

    return (reader.getInt ("lineNumber", buffer));

  } // getScanLine

  ////////////////////////////////////////////////////////////

  public Date getDate() {

    int year = reader.getInt ("year", buffer);
    int day = reader.getInt ("day", buffer);
    long millisecond = reader.getLong ("timeUTC", buffer);
    
    return (NOAA1bServices.convertDate (year, day, millisecond));

  } // getDate

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new record.
   *
   * @param buffer the buffer to use for record data.
   * @param header the header record for this data record.
   *
   * @throws RuntimeException if the XML stream reader resource
   * file for the subclass cannot be found.
   */
  public AbstractDataRecord (
    ByteBuffer buffer,
    DataHeader header
  ) {

    // Initialize
    // ----------
    this.buffer = buffer;
    reader = BinaryStreamReaderFactory.getReader (getClass());
    this.header = header;

  } // AbstractDataRecord

  ////////////////////////////////////////////////////////////

} // AbstractDataRecord class

////////////////////////////////////////////////////////////////////////
