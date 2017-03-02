////////////////////////////////////////////////////////////////////////
/*

     File: AbstractDataRecord.java
   Author: Peter Hollemans
     Date: 2007/10/11

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
import java.nio.ByteBuffer;
import java.util.Date;
import noaa.coastwatch.io.BinaryStreamReader;
import noaa.coastwatch.io.BinaryStreamReaderFactory;
import noaa.coastwatch.io.noaa1b.DataHeader;
import noaa.coastwatch.io.noaa1b.DataRecord;
import noaa.coastwatch.io.noaa1b.NOAA1bServices;

/**
 * The <code>AbstractDataRecord</code> class reads NOAA 1b data
 * records.
 *
 * @author Peter Hollemans
 * @since 3.2.2
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
