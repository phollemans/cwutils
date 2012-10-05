////////////////////////////////////////////////////////////////////////
/*
     FILE: AbstractDataHeader.java
  PURPOSE: Reads NOAA 1b format data header records.
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
import java.nio.channels.*;
import java.util.*;
import noaa.coastwatch.io.*;

/**
 * The <code>AbstractDataHeader</code> class reads NOAA 1b data
 * header records.
 */
public abstract class AbstractDataHeader implements DataHeader {

  // Variables
  // ---------

  /** The binary reader for this class. */
  protected BinaryStreamReader reader;

  /** The data buffer to read. */
  protected ByteBuffer buffer;

  ////////////////////////////////////////////////////////////

  public String getSpacecraft() { 

    return (reader.getString ("spacecraft", buffer));

  } // getSpacecraft

  ////////////////////////////////////////////////////////////

  public Date getStartDate() {

    int year = reader.getInt ("startYear", buffer);
    int day = reader.getInt ("startDay", buffer);
    long millisecond = reader.getLong ("startTimeUTC", buffer);
    
    return (NOAA1bServices.convertDate (year, day, millisecond));

  } // getStartDate

  ////////////////////////////////////////////////////////////

  public Date getEndDate() {

    int year = reader.getInt ("endYear", buffer);
    int day = reader.getInt ("endDay", buffer);
    long millisecond = reader.getLong ("endTimeUTC", buffer);
    
    return (NOAA1bServices.convertDate (year, day, millisecond));

  } // getEndDate

  ////////////////////////////////////////////////////////////

  public int getRecordCount() {

    return (reader.getInt ("dataRecords", buffer));

  } // getRecordCount

  ////////////////////////////////////////////////////////////

  public int getHeaderCount() {

    return (reader.getInt ("headerRecords", buffer));

  } // getHeaderCount

  ////////////////////////////////////////////////////////////

  public int getFormatVersion() {

    return (reader.getInt ("formatVersion", buffer));

  } // getFormatVersion
    
  ////////////////////////////////////////////////////////////

  public String getDatasetName() {

    return (reader.getString ("datasetName", buffer));

  } // getDatasetName

  ////////////////////////////////////////////////////////////

  public String getCreationSite() {

    return (reader.getString ("creationSite", buffer));

  } // getCreationSite

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new header.
   *
   * @param buffer the buffer to use for header data.
   *
   * @throws RuntimeException if the XML stream reader resource
   * file for the subclass cannot be found.
   */
  public AbstractDataHeader (
    ByteBuffer buffer
  ) {

    // Initialize
    // ----------
    this.buffer = buffer;
    reader = BinaryStreamReaderFactory.getReader (getClass());

  } // AbstractDataHeader constructor

  ////////////////////////////////////////////////////////////

} // AbstractDataHeader class

////////////////////////////////////////////////////////////////////////
