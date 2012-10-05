////////////////////////////////////////////////////////////////////////
/*
     FILE: NOAA1bFile.java
  PURPOSE: To read NOAA1b data format.
   AUTHOR: Peter Hollemans
     DATE: 2007/08/25
  CHANGES: 2007/12/11, PFH, fixed problem reading headerless data files

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
import terrenus.instrument.*;

/**
 * The <code>NOAA1bFile</code> is an interface for reading NOAA
 * 1b weather satellite data files.  The intention is to have a
 * uniform way of reading any NOAA 1b file format from any sensor
 * among the AVHRR and TOVS packages.
 */
public class NOAA1bFile {

  // Variables
  // ---------

  /** The input file channel. */
  private FileChannel inputChannel;

  /** The byte swapped flag, false by default. */
  private boolean isByteSwapped;

  /** The file archive header, possibly null. */
  private ArchiveHeader archiveHeader;

  /** The file data header. */
  private DataHeader dataHeader;

  ////////////////////////////////////////////////////////////

  /**
   * Opens a NOAA 1b data file for reading.  The byte order is
   * assumed to be the standard big endian order that most NOAA
   * 1b files are stored in.
   *
   * @param fileName the NOAA 1b filename.
   *
   * @throws IOException if an error occurred reading the file.
   */
  public NOAA1bFile (
    String fileName
  ) throws IOException {

    this (fileName, false);

  } // NOAA1bFile constructor

  ////////////////////////////////////////////////////////////

  /**
   * Opens a NOAA 1b data file for reading.
   *
   * @param fileName the NOAA 1b filename.
   * @param isByteSwapped the byte swapped flag, true if the data
   * is in little endian byte order.
   *
   * @throws IOException if an error occurred reading the file.
   */
  public NOAA1bFile (
    String fileName,
    boolean isByteSwapped
  ) throws IOException {

    this.isByteSwapped = isByteSwapped;
    inputChannel = new RandomAccessFile (fileName, "r").getChannel();
    archiveHeader = ArchiveHeaderFactory.create (inputChannel);
    if (archiveHeader != null) 
      inputChannel.position (archiveHeader.getHeaderSize());
    else
      inputChannel.position (0);
    dataHeader = DataHeaderFactory.create (inputChannel, isByteSwapped);

  } // NOAA1bFile constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the file format version number. 
   *
   * @return the format version in the range [1..n] where n is
   * the latest version being produced by NOAA.  The format
   * version is obtained from the data header record.
   */
  public int getFormatVersion () {

    return (dataHeader.getFormatVersion());

  } // getFormatVersion

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the file archive header.
   * 
   * @return the archive header or null if not available.
   */
  public ArchiveHeader getArchiveHeader () {

    return (archiveHeader);

  } // getArchiveHeader

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the file data header.
   * 
   * @return the data header.
   */
  public DataHeader getDataHeader () {

    return (dataHeader);

  } // getDataHeader

  ////////////////////////////////////////////////////////////

  /**
   * Gets an input buffer for record data with the correct byte
   * order.
   * 
   * @param length the length of the buffer.
   *
   * @return the buffer of byte values.
   */
  public ByteBuffer getInputBuffer (
    int length
  ) {

    ByteBuffer inputBuffer = ByteBuffer.allocate (length);
    if (isByteSwapped) inputBuffer.order (ByteOrder.LITTLE_ENDIAN);
    return (inputBuffer);

  } // getInputBuffer

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a file data record.
   * 
   * @param recordIndex the record to get in the range
   * [0..<code>getRecordCount()</code>-1].
   * @param readFull the full record flag, true to read a full data
   * record or false to only read the data record attributes (see
   * {@link DataHeader#getRecordAttSize}).
   * @param inputBuffer the input buffer to use for reading or null to
   * create a new one.
   * 
   * @return the data record.
   *
   * @throws IOException if an error occurred reading the record.
   */
  public DataRecord getDataRecord (
    int recordIndex,
    boolean readFull,
    ByteBuffer inputBuffer
  ) throws IOException {
    
    // Compute byte offset into file
    // -----------------------------
    int recordSize = dataHeader.getRecordSize();
    int offset = 
      (archiveHeader == null ? 0 : archiveHeader.getHeaderSize()) +
      recordSize*(dataHeader.getHeaderCount() + recordIndex);

    // Create new input buffer
    // -----------------------
    if (inputBuffer == null) { 
      inputBuffer = getInputBuffer (readFull ? recordSize :
        dataHeader.getRecordAttSize());
    } // if

    // Read file data
    // --------------
    inputChannel.read (inputBuffer, offset);
    return (dataHeader.getDataRecord (inputBuffer));

  } // getDataRecord

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a file data record.
   * 
   * @param recordIndex the record to get in the range
   * [0..<code>getRecordCount()</code>-1].
   * 
   * @return the data record.
   *
   * @throws IOException if an error occurred reading the record.
   */
  public DataRecord getDataRecord (
    int recordIndex
  ) throws IOException {
    
    return (getDataRecord (recordIndex, true, null));

  } // getDataRecord

  ////////////////////////////////////////////////////////////

  /** Gets the number of data records in this file. */
  public int getRecordCount () { return (dataHeader.getRecordCount()); }

  ////////////////////////////////////////////////////////////

  /** Gets the instrument whose data is recorded in this file. */
  public Instrument getInstrument() { return (dataHeader.getInstrument()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the instrument data for the specified record.
   *
   * @param recordIndex the record index for instrument data.
   *
   * @return the instrument data for the specified record.
   */
  public InstrumentData getData (
    int recordIndex
  ) throws IOException {

    return (getDataRecord (recordIndex).getData());

  } // getData

  ////////////////////////////////////////////////////////////

  /** Closes the file. */
  public void close () throws IOException { inputChannel.close(); }

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    NOAA1bFile file = new NOAA1bFile (argv[0]);

    ArchiveHeader archiveHeader = file.getArchiveHeader();
    if (archiveHeader != null) {
      System.out.println ("Archive header info:");
      System.out.println ("  Sensor word size = " + archiveHeader.getSensorWordSize());
      System.out.println ("  Header size = " + archiveHeader.getHeaderSize());
    } // if

    DataHeader dataHeader = file.getDataHeader();
    System.out.println ("Data header info:");
    System.out.println ("  Spacecraft = " + dataHeader.getSpacecraft());
    System.out.println ("  Instrument = " + dataHeader.getInstrument());
    System.out.println ("  Start date = " + dataHeader.getStartDate());
    System.out.println ("  End date = " + dataHeader.getEndDate());
    System.out.println ("  Record count = " + dataHeader.getRecordCount());
    System.out.println ("  Header count = " + dataHeader.getHeaderCount());
    System.out.println ("  Format version = " + dataHeader.getFormatVersion());
    System.out.println ("  Record size = " + dataHeader.getRecordSize());
    System.out.println ("  Record attribute size = " + dataHeader.getRecordAttSize());
    System.out.println ("  Dataset name = " + dataHeader.getDatasetName());
    System.out.println ("  Creation site = " + dataHeader.getCreationSite());

    for (int i = 0; i < dataHeader.getRecordCount(); i++) {
      DataRecord record = file.getDataRecord (i);
      System.out.println ("Record " + i + " info:");
      System.out.println ("  Scan line = " + record.getScanLine());
      System.out.println ("  Date = " + record.getDate());
      System.out.println ("  Sensor data usable = " + record.isSensorDataUsable());
      System.out.println ("  Sensor data = " + record.getSensorData());
      System.out.println ("  Calibration usable = " + record.isCalibrationUsable());
      System.out.println ("  Calibration data = " + record.getCalibration());
      System.out.println ("  Navigation usable = " + record.isNavigationUsable());
      System.out.println ("  Navigation data = " + record.getNavigation());
    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // NOAA1bFile class

////////////////////////////////////////////////////////////////////////
