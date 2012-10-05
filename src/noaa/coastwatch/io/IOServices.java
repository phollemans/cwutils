////////////////////////////////////////////////////////////////////////
/*
     FILE: IOServices.java
  PURPOSE: Performs various static IO-related functions.
   AUTHOR: Peter Hollemans
     DATE: 2003/12/28
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2003, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 * The IO services class defines various static methods relating
 * to input/output services.
 */
public class IOServices {

  ////////////////////////////////////////////////////////////

  /**
   * Gets a file path for a resource name.
   * 
   * @param resourceClass the class requesting the resource.
   * @param resourceName the resource name.
   *
   * @return the full path to the resource file.
   *
   * @throws IOException if the resource cannot be resolved to a
   * pathname, or the file is not readable.
   */ 
  public static String getFilePath (
    Class resourceClass,
    String resourceName
  ) throws IOException {

    // Get URL
    // -------
    URL url = resourceClass.getResource (resourceName);
    if (url == null)
      throw new IOException ("Cannot find resource '" + resourceName + "'");

    // Get file
    // --------
    File file;
    try { file = new File (new URI (url.toString())); }
    catch (URISyntaxException e) { throw new IOException (e.getMessage()); }

    // Check readable
    // --------------
    if (!file.canRead()) 
      throw new FileNotFoundException ("Cannot open '" + file + "'");

    return (file.getPath());

  } // getFilePath

  ////////////////////////////////////////////////////////////

  /** 
   * Converts octal escape sequences in the string to Java
   * characters.
   */
  public static String convertOctal (
    String value
  ) {

    // Create octal escape pattern matcher
    // -----------------------------------
    Pattern pattern = Pattern.compile ("\\\\(0[0-9]{2,3})");
    Matcher matcher = pattern.matcher (value);

    // Loop over each pattern match
    // ----------------------------
    StringBuffer newValue = new StringBuffer();
    int lastEnd = 0;
    while (matcher.find()) {

      // Append non-octal characters
      // ---------------------------
      if (lastEnd < matcher.start())
        newValue.append (value.substring (lastEnd, matcher.start()));
      lastEnd = matcher.end();

      // Append octal character
      // ----------------------
      newValue.append ((char) Byte.decode (matcher.group (1)).byteValue());

    } // while

    // Append final non-octal characters
    // ---------------------------------
    newValue.append (value.substring (lastEnd, value.length()));

    return (newValue.toString());

  } // convertOctal

  ////////////////////////////////////////////////////////////

  private IOServices () { }

  ////////////////////////////////////////////////////////////

} // IOServices class

////////////////////////////////////////////////////////////////////////
