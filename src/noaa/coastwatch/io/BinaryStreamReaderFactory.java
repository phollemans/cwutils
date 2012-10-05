////////////////////////////////////////////////////////////////////////
/*
     FILE: BinaryStreamReaderFactory.java
  PURPOSE: Creates binary stream reading objects.
   AUTHOR: Peter Hollemans
     DATE: 2007/10/11
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.util.*;

/**
 * The <code>BinaryStreamReaderFactory</code> class handles
 * efficient storage and retrieval of stream readers for specific
 * classes.
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
