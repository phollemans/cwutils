////////////////////////////////////////////////////////////////////////
/*
     FILE: DataHeader.java
  PURPOSE: Reads NOAA 1b format data header records.
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
import java.nio.channels.*;
import java.util.*;
import terrenus.instrument.*;

/**
 * The <code>DataHeader</code> interface is for reading NOAA
 * 1b data header records.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public interface DataHeader {

  /** Gets the data spacecraft name. */
  public String getSpacecraft();

  /** Gets the data instrument. */
  public Instrument getInstrument();

  /** Gets the data start date. */
  public Date getStartDate();

  /** Gets the data end date. */
  public Date getEndDate();

  /** Gets the number of data records in the data file. */
  public int getRecordCount();

  /** Gets the number of header records in the data file. */
  public int getHeaderCount();

  /** 
   * Gets the file format version number. 
   *
   * @return the format version in the range [1..n] where n is
   * the latest version being produced by NOAA.
   */
  public int getFormatVersion();
    
  /** Gets the size of each header and data record in bytes. */
  public int getRecordSize();

  /** 
   * Creates a data record using the specified data.
   *
   * @param inputBuffer the buffer to read for byte data.
   */
  public DataRecord getDataRecord (
    ByteBuffer inputBuffer
  );

  /** 
   * Gets the size of the data record attributes in bytes.  The
   * attributes can be used to assess the data record contents without
   * reading the entire data record.  The attribute bytes must contain
   * enough data from the record to return the scan line, date, and
   * sensor, navigation, and calibration quality flags.
   */
  public int getRecordAttSize();

  /** Gets the calibration data in the header. */
  public float[] getCalibration();

  /** Gets the dataset name as stored in the header. */
  public String getDatasetName();

  /** Gets the dataset creation site. */
  public String getCreationSite();

} // DataHeader interface

////////////////////////////////////////////////////////////////////////
