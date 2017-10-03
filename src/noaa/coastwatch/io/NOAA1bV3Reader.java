////////////////////////////////////////////////////////////////////////
/*

     File: NOAA1bV3Reader.java
   Author: Peter Hollemans
     Date: 2005/02/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import java.util.Map;
import noaa.coastwatch.io.NOAA1bReader;
import noaa.coastwatch.io.NOAA1bV2Reader;

/**
 * A NOAA 1b version 3 reader is a NOAA 1b reader that reads NOAA 1b
 * packed format KLMNN' GAC/LAC/HRPT data files available from the
 * NOAA/NESDIS Satellite Active Archive.  These files are very similar
 * to version 2 with some minor modifications and extensions for
 * NOAA-N and NOAA-N' data.
 *
 * @see NOAA1bV2Reader
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class NOAA1bV3Reader
  extends NOAA1bV2Reader {

  // Constants
  // ---------

  /** The data format description. */
  private static final String READER_DATA_FORMAT = "NOAA 1b version 3";

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (READER_DATA_FORMAT); }

  ////////////////////////////////////////////////////////////

  /** Gets the data format version. */
  protected int getDataFormatVersion () { return (3); }

  ////////////////////////////////////////////////////////////

  public DataHeader getDataHeader () throws IOException {

    return (new DataHeaderV3());

  } // getDataHeader

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a NOAA 1b version 3 reader from the specified file.
   *
   * @param file the file name to read.
   *
   * @throws IOException if an error occurred reading the file data.
   */
  public NOAA1bV3Reader (
    String file
  ) throws IOException {

    super (file);

  } // NOAA1bV3Reader constructor

  ////////////////////////////////////////////////////////////

  public ScanLine getScanLine (
    int line,
    int start,
    int count
  ) throws IOException {

    ScanLine lineObject = scanLineCache.get (line);
    if (lineObject == null) {
      lineObject = new ScanLineV3 (line, start, count);
      scanLineCache.put (line, lineObject);
    } // if
    return (lineObject);

    //    return (new ScanLineV3 (line, start, count));

  } // getScanLine

  ////////////////////////////////////////////////////////////

  /**
   * The scan line version 3 class is a scan line that can read,
   * calibrate, and navigate NOAA-KLMNN' data.
   */
  public class ScanLineV3
    extends ScanLineV2 {

    ////////////////////////////////////////////////////////

    public Object getAttribute (
      int index
    ) {

      switch (index) {
      case CH4_COEFFICIENT3: return (new Float (getInt (data, 260) * 1e-7));
      case CH5_COEFFICIENT3: return (new Float (getInt (data, 284) * 1e-7));
      default: return (super.getAttribute (index));
      } // switch

    } // getAttribute

    ////////////////////////////////////////////////////////

    /**
     * Creates a new dataset scan line using the specified index.
     *
     * @param line the scan line index in the range [0..lines-1].
     * @param start the starting sensor data sample.
     * @param count the total number of sensor data samples.  If the
     * count is 0, no sensor data is read.
     *
     * @throws IOException if an error occurred reading the file data.
     *
     * @see NOAA1bReader#getScanLine
     * @see NOAA1bReader#getLines
     */
    public ScanLineV3 (
      int line,
      int start,
      int count
    ) throws IOException {

      super (line, start, count);

    } // ScanLineV3 constructor

    ////////////////////////////////////////////////////////

  } // ScanLineV3 class

  ////////////////////////////////////////////////////////////

  /**
   * The data header version 3 class is a data header that can
   * retrieve NOAA-KLMNN' data header information.
   */
  public class DataHeaderV3
    extends DataHeaderV2 {

    // Constants
    // ---------

    /** The NOAA-18 spacecraft ID code. */
    public static final int NOAA_18_ID = 7;

    /** The NOAA-19 spacecraft ID code. */
    public static final int NOAA_19_ID = 8;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new dataset header.
     *
     * @throws IOException if an error occurred reading the file data.
     */
    public DataHeaderV3 () throws IOException {

      super();

    } // DataHeaderV2 constructor

    ////////////////////////////////////////////////////////

    public Object getAttribute (
      int index
    ) {

      switch (index) {

      // If spacecraft ID, return
      // ------------------------
      case SPACECRAFT_ID: 
        int id = getUShort (data, 72);
        switch (id) {
        case NOAA_15_ID: return (new String ("noaa-15"));
        case NOAA_16_ID: return (new String ("noaa-16"));
        case NOAA_17_ID: return (new String ("noaa-17"));
        case NOAA_18_ID: return (new String ("noaa-18"));
        case NOAA_19_ID: return (new String ("noaa-19"));
        default: return (new String ("Unknown"));
        } // switch

      // Otherwise, delegate to the parent
      // ---------------------------------
      default: return (super.getAttribute (index));

      } // switch

    } // getAttribute

    ////////////////////////////////////////////////////////

  } // DataHeaderV3 class

  ////////////////////////////////////////////////////////////

} // NOAA1bV3Reader class

////////////////////////////////////////////////////////////////////////
