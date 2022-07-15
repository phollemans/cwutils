////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataReaderFactory.java
   Author: Peter Hollemans
     Date: 2002/04/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.tools.ToolServices;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The earth data reader factory class creates an appropriate
 * earth data reader object based on a file name.  The default
 * list of readers is as follows:
 * <ul>
 *
 *   <li>HDF formats:<ul>
 *     <li> {@link noaa.coastwatch.io.CWHDFReader} </li>
 *     <li> {@link noaa.coastwatch.io.TSHDFReader} </li>
 *     <!-- <li> {@link noaa.coastwatch.io.ACSPOHDFReader} </li> -->
 *   </ul></li>
 *
 *   <li>NOAA 1b formats:<ul>
 *     <li> {@link noaa.coastwatch.io.NOAA1bV1Reader} </li>
 *     <li> {@link noaa.coastwatch.io.NOAA1bV2Reader} </li>
 *     <li> {@link noaa.coastwatch.io.NOAA1bV3Reader} </li>
 *     <li> {@link noaa.coastwatch.io.NOAA1bV4Reader} </li>
 *     <li> {@link noaa.coastwatch.io.NOAA1bV5Reader} </li>
 *     <li> {@link noaa.coastwatch.io.noaa1b.NOAA1bFileReader} </li>
 *   </ul></li>
 *
 *   <li>NetCDF formats:<ul>
 *     <!-- <li> {@link noaa.coastwatch.io.ACSPONCReader} </li> -->
 *     <!-- <li> {@link noaa.coastwatch.io.ACSPONCCFReader} </li> -->
 *     <li> {@link noaa.coastwatch.io.CWNCReader} </li>
 *     <li> {@link noaa.coastwatch.io.CWCFNCReader} </li>
 *     <li> {@link noaa.coastwatch.io.CommonDataModelNCReader} </li>
 *   </ul></li>
 
 * </ul>
 * Additional readers may be appended to the list using the
 * <code>addReader()</code> method, or by adding the class name
 * to the <code>reader.properties</code> resource file.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class EarthDataReaderFactory {

  private static final Logger LOGGER = Logger.getLogger (EarthDataReaderFactory.class.getName());
  private static final Logger VERBOSE = Logger.getLogger (EarthDataReaderFactory.class.getName() + ".verbose");

  // Constants
  // ---------

  /** The resource file for reader extensions. */
  private static final String READER_PROPERTIES = "reader.properties";

  // Variables
  // ---------

  /** The list of available reader classes as strings. */
  private static List<String> readerList;
  
  /** The default log level for the verbose logger. */
  private static Level defaultLevel;

  ////////////////////////////////////////////////////////////

  /** Sets up the initial set of readers. */
  static {

    // Create reader list
    // ------------------
    readerList = new ArrayList<>();
    String thisPackage = EarthDataReaderFactory.class.getPackage().getName();

    // Add HDF variants
    // ----------------
    readerList.add (thisPackage + ".CWHDFReader");
    readerList.add (thisPackage + ".TSHDFReader");

// Deprecated   readerList.add (thisPackage + ".ACSPOHDFReader");

    // Add NOAA 1b variants
    // --------------------
    readerList.add (thisPackage + ".NOAA1bV1Reader");
    readerList.add (thisPackage + ".NOAA1bV2Reader");
    readerList.add (thisPackage + ".NOAA1bV3Reader");
    readerList.add (thisPackage + ".NOAA1bV4Reader");
    readerList.add (thisPackage + ".NOAA1bV5Reader");
    readerList.add (thisPackage + ".noaa1b.NOAA1bFileReader");

    // Add NetCDF variants
    // -------------------
// Deprecated    readerList.add (thisPackage + ".ACSPONCReader");
// Deprecated    readerList.add (thisPackage + ".ACSPONCCFReader");
    readerList.add (thisPackage + ".CWNCReader");
    readerList.add (thisPackage + ".CWCFNCReader");
    readerList.add (thisPackage + ".CommonDataModelNCReader");

    // Add extension readers
    // ---------------------
    InputStream stream = ClassLoader.getSystemResourceAsStream (READER_PROPERTIES);
    if (stream != null) {
      Properties props = new Properties();
      try { 
        props.load (stream); 
        for (Iterator iter = props.keySet().iterator(); iter.hasNext();)
          readerList.add ((String) iter.next());
      } // try
      catch (IOException e) { LOGGER.log (Level.WARNING, "Error loading reader extension properties", e); }
      finally { 
        try { stream.close(); }
        catch (IOException e) { LOGGER.log (Level.WARNING, "Error closing reader extension properties", e); }
      } // finally
    } // if

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new reader to the list.  When the factory creates a new
   * reader from a file name, the new reader will be among those tried
   * when opening the file.  The reader should throw a
   * <code>java.io.IOException</code> if an error is encountered
   * reading the initial parts of the file, and/or if the file format
   * is not recognized.  By convention, the reader should have a
   * constructor that takes a single <code>String</code> parameter as
   * the file name.
   *
   * @param readerClass the new reader class to add to the list.
   */
  public static void addReader (
    Class readerClass
  ) {

    readerList.add (readerClass.getName());

  } // addReader

  ////////////////////////////////////////////////////////////

  /**
   * Sets the verbose mode.  If verbose mode is on, the errors
   * encountered while trying to create the reader object are
   * printed.
   *
   * @param flag the verbose flag.
   */
  public static void setVerbose (boolean flag) { 

    if (defaultLevel == null) defaultLevel = VERBOSE.getLevel();
    VERBOSE.setLevel (flag ? Level.INFO : defaultLevel);

  } // setVerbose

  ////////////////////////////////////////////////////////////

  /** 
   * Creates an earth data reader object.
   *
   * @param file the file name.  The file will be opened using the
   * different earth data reader classes in the list until the
   * correct constructor is found.
   * 
   * @return an earth data reader object specific to the file
   * format.
   *
   * @throws IOException if the reader could not be created.  Either
   * the file was not found, or the format was not recognized by any
   * reader.
   */
  public static EarthDataReader create (
    String file
  ) throws IOException {

    // Check for a network file
    // ------------------------
    boolean isNetwork = file.startsWith ("http://");

    // Check file exists and is readable
    // ---------------------------------
    if (!isNetwork) {
      File f = new File (file);
      if (!f.canRead ()) throw new FileNotFoundException ("Cannot open " + 
        file);
    } // if

    // Loop through each reader class
    // ------------------------------
    Class[] types = new Class[] {String.class};
    Object[] args = new Object[] {file};
    EarthDataReader reader = null;
    for (var readerName : readerList) {

      // Try to create a new reader object from the file name
      // ----------------------------------------------------
      try {
        Class readerClass = Class.forName (readerName);
        Constructor constructor = readerClass.getConstructor (types);
        reader = (EarthDataReader) constructor.newInstance (args);
      } // try

      // Handle error thrown by from reader constructor
      // ----------------------------------------------
      catch (InvocationTargetException targetException) {

        // If out of memory, stop now
        // --------------------------
        Throwable cause = targetException.getCause();
        if (cause instanceof OutOfMemoryError) {
          throw ((OutOfMemoryError) cause);
        } // if

        // Otherwise show a stack trace
        // ----------------------------
        VERBOSE.log (Level.INFO, "Error creating object " + readerName, ToolServices.shortTrace (cause, readerName));

      } // catch

      // Handle some other error
      // -----------------------
      catch (Exception otherException) {
        VERBOSE.log (Level.INFO, "Error creating object " + readerName, ToolServices.shortTrace (otherException, readerName));
      } // catch

      // Check if we've found the right reader
      // -------------------------------------
      if (reader != null) break;

    } // for
    
    // Give up and throw an error 
    // --------------------------
    if (reader == null) 
      throw new UnsupportedEncodingException ("Unable to recognize file format for " + file);      

    return (reader);

  } // create

  ////////////////////////////////////////////////////////////

} // EarthDataReaderFactory class

////////////////////////////////////////////////////////////////////////
