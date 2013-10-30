////////////////////////////////////////////////////////////////////////
/*
     FILE: DatumFactory.java
  PURPOSE: Creates geodetic datums.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/18
  CHANGES: 2006/05/26, PFH, modified to implement SpheroidConstants

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.io.*;
import java.util.*;
import noaa.coastwatch.util.*;

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
