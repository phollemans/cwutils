////////////////////////////////////////////////////////////////////////
/*

     File: NOAA1bV4Reader.java
   Author: Peter Hollemans
     Date: 2006/01/27

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.io.NOAA1bReader;
import noaa.coastwatch.io.NOAA1bV3Reader;

/**
 * A NOAA 1b version 4 reader is similar to the 1b version 3
 * reader except that it accepts the version 4 in the archive and
 * data headers.  Version 4 started when the CLAVR-x cloud mask
 * fill bytes were replaced with actual cloud mask information.
 * Version 4 also has support for the Metop series of satellites.
 *
 * @see NOAA1bV3Reader
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class NOAA1bV4Reader
  extends NOAA1bV3Reader {

  // Constants
  // ---------

  /** The data format description. */
  private static final String READER_DATA_FORMAT = "NOAA 1b version 4";

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (READER_DATA_FORMAT); }

  ////////////////////////////////////////////////////////////

  /** Gets the data format version. */
  protected int getDataFormatVersion () { return (4); }

  ////////////////////////////////////////////////////////////

  public DataHeader getDataHeader () throws IOException {

    return (new DataHeaderV4());

  } // getDataHeader

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a NOAA 1b version 4 reader from the specified file.
   *
   * @param file the file name to read.
   *
   * @throws IOException if an error occurred reading the file data.
   */
  public NOAA1bV4Reader (
    String file
  ) throws IOException {

    super (file);

  } // NOAA1bV4Reader constructor

  ////////////////////////////////////////////////////////////

  /**
   * The data header version 4 class is a data header that can
   * retrieve NOAA-KLMNN' and Metop data header information.
   */
  public class DataHeaderV4
    extends DataHeaderV3 {

    // Constants
    // ---------

    /** The Metop-1 spacecraft ID code. */
    public static final int METOP_1_ID = 11;

    /** The Metop-2 spacecraft ID code. */
    public static final int METOP_2_ID = 12;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new dataset header.
     *
     * @throws IOException if an error occurred reading the file data.
     */
    public DataHeaderV4 () throws IOException {

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
        case METOP_1_ID: return (new String ("metop-1"));
        case METOP_2_ID: return (new String ("metop-2"));
        default: return (new String ("Unknown"));
        } // switch

      // Also handle the new Metop FRAC code
      // -----------------------------------
      case DATA_TYPE_CODE: 
        int code = getUShort (data, 76);
        switch (code) {

        /** 
         * Note: We return LAC here for type code 13 even though
         * the Metop term is FRAC, as this avoids having to
         * change the version 2 reader class that handles LAC
         * versus GAC differently.  Does anyone care?  We don't
         * know.
         */
        case 1: case 13: return (new String ("lac"));

        case 2: return (new String ("gac"));
        case 3: return (new String ("hrpt"));
        case 4: return (new String ("tip"));
        case 5: return (new String ("hirs"));
        case 6: return (new String ("msu"));
        case 7: return (new String ("ssu"));
        case 8: return (new String ("dcs"));
        case 9: return (new String ("sem"));
        case 10: return (new String ("amsu-a"));
        case 11: return (new String ("amsu-b"));
        default: return (new String ("Unknown"));
        } // switch

      // Otherwise, delegate to the parent
      // ---------------------------------
      default: return (super.getAttribute (index));

      } // switch

    } // getAttribute

    ////////////////////////////////////////////////////////

  } // DataHeaderV4 class

  ////////////////////////////////////////////////////////////

} // NOAA1bV4Reader class

////////////////////////////////////////////////////////////////////////
