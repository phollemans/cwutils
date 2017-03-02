////////////////////////////////////////////////////////////////////////
/*

     File: NOAA1bV5Reader.java
   Author: Peter Hollemans
     Date: 2006/11/15

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
import noaa.coastwatch.io.NOAA1bV4Reader;

/**
 * A NOAA 1b version 5 reader is the same as a 1b version 4
 * reader except that it accepts the version 5 in the archive and
 * data headers.  Version 5 started when an update to handle
 * MetOp data accidently also updated the version number.
 *
 * @see NOAA1bV4Reader
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class NOAA1bV5Reader
  extends NOAA1bV4Reader {

  // Constants
  // ---------

  /** The data format description. */
  private static final String READER_DATA_FORMAT = "NOAA 1b version 5";

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (READER_DATA_FORMAT); }

  ////////////////////////////////////////////////////////////

  /** Gets the data format version. */
  protected int getDataFormatVersion () { return (5); }

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a NOAA 1b version 5 reader from the specified file.
   *
   * @param file the file name to read.
   *
   * @throws IOException if an error occurred reading the file data.
   */
  public NOAA1bV5Reader (
    String file
  ) throws IOException {

    super (file);

  } // NOAA1bV5Reader constructor

  ////////////////////////////////////////////////////////////

} // NOAA1bV5Reader class

////////////////////////////////////////////////////////////////////////
