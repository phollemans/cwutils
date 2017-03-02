////////////////////////////////////////////////////////////////////////
/*

     File: BinaryStreamReaderFactory.java
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
package noaa.coastwatch.io;

// Imports
// -------
import java.util.HashMap;
import java.util.Map;
import noaa.coastwatch.io.BinaryStreamReader;

/**
 * The <code>BinaryStreamReaderFactory</code> class handles
 * efficient storage and retrieval of stream readers for specific
 * classes.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class BinaryStreamReaderFactory {

  // Variables
  // ---------

  /** The map of classes to stream readers. */
  private static Map<Class,BinaryStreamReader> readerMap =
    new HashMap<Class,BinaryStreamReader>();

  ////////////////////////////////////////////////////////////

  /** 
   * Retrieves a stream reader for the specified class.
   *
   * @param readerClass the reader class to get a stream.
   *
   * @return the stream reader.
   *
   * @throws RuntimeException if no stream XML template could be
   * found for the specified class.
   */
  public static BinaryStreamReader getReader (
    Class readerClass
  ) {

    // Get existing reader
    // -------------------
    BinaryStreamReader reader = readerMap.get (readerClass);

    // Create new reader
    // -----------------
    if (reader == null) {
      String name = readerClass.getName();
      String resourceFile = name.substring (name.lastIndexOf (".") + 
        1).toLowerCase() + ".xml";
      try {
        reader = new BinaryStreamReader (readerClass.getResourceAsStream (
          resourceFile));
      } // try
      catch (Exception e) {
        throw new RuntimeException (e.toString());
      } // catch
      readerMap.put (readerClass, reader);
    } // if

    return (reader);
  
  } // getReader

  ////////////////////////////////////////////////////////////

} // BinaryStreamReaderFactory class

////////////////////////////////////////////////////////////////////////
