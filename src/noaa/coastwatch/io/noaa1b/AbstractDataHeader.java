////////////////////////////////////////////////////////////////////////
/*

     File: AbstractDataHeader.java
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
import noaa.coastwatch.io.noaa1b.NOAA1bServices;

/**
 * The <code>AbstractDataHeader</code> class reads NOAA 1b data
 * header records.
 *
 * @author Peter Hollemans
 * @since 3.2.2
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
