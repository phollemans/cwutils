////////////////////////////////////////////////////////////////////////
/*

     File: DatumFactory.java
   Author: Peter Hollemans
     Date: 2005/05/18

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
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.SpheroidConstants;

/**
 * The <code>DatumFactory</code> class creates geodetic
 * <code>Datum</code> objects corresponding to various spheroid
 * codes.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class DatumFactory
  implements SpheroidConstants {

  // Constants
  // ---------

  /** The datum parameters file. */
  private static final String PROPERTIES_FILE = "datum.properties";

  // Variables
  // ---------

  /** The main array of datum objects. */
  private static Datum[] datumArray = new Datum[MAX_SPHEROIDS];

  /** The properties used for datum parameters. */
  private static Properties props;

  ////////////////////////////////////////////////////////////

  /** Initializes the datum properties file. */
  static {

    try {

      // Initialize properties
      // ---------------------
      props = new Properties();
      InputStream stream = DatumFactory.class.getResourceAsStream (
        PROPERTIES_FILE);
      if (stream == null) {
        throw new IOException ("Cannot find resource '" + 
          PROPERTIES_FILE + "'");
      } // if
      props.load (stream);
      stream.close();    

    } // try

    catch (IOException e) {
      throw new RuntimeException (e.getMessage());
    } // catch

  } // static

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a datum instance based on a spheroid code.  Since
   * <code>Datum</code> instances are immutable, the same instance is
   * returned each time for the same spheroid code.
   *
   * @param spheroid the spheroid code for the datum.
   * 
   * @return the datum object, or null if the specified spheroid has
   * no valid datum.
   */
  public static Datum create (
    int spheroid
  ) {

    // Check spheroid code
    // -------------------
    if (spheroid < 0 || spheroid > MAX_SPHEROIDS-1) return (null);

    // Check if datum is created
    // -------------------------
    if (datumArray[spheroid] == null) {
      String key = "GCTP_" + SPHEROID_NAMES[spheroid].replace (' ', '_');
      String value = props.getProperty (key);
      if (value != null) {
        String[] valueArray = value.split (",");
        datumArray[spheroid] = new Datum (valueArray[0], spheroid, 
          Double.parseDouble (valueArray[1]),
          Double.parseDouble (valueArray[2]),
          Double.parseDouble (valueArray[3]));
      } // if
    } // if

    // Return datum
    // ------------
    return (datumArray[spheroid]);
    
  } // create

  ////////////////////////////////////////////////////////////

} // DatumFactory class

////////////////////////////////////////////////////////////////////////
