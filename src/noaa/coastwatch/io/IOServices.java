////////////////////////////////////////////////////////////////////////
/*

     File: IOServices.java
   Author: Peter Hollemans
     Date: 2003/12/28

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

/**
 * The IO services class defines various static methods relating
 * to input/output services.
 *
 * @author Peter Hollemans
 * @since 3.1.7
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
  
  /**
   * Prints a node and its children in XML syntax.
   * 
   * @param root the root of the node tree to print.
   */
  public static void printXML (
    Node root
  ) {
  
    printXML (root, 0);

  } // printXML
  
  ////////////////////////////////////////////////////////////
  
  /**
   * Gets an indent string.
   *
   * @param level the level of indent to get.
   */
  private static String getIndent (
    int level
  ) {

    StringBuffer buffer = new StringBuffer();
    String indent = "  ";
    for (int i = 0; i < level; i++) buffer.append (indent);
    return (buffer.toString());

  } // getIndent

  ////////////////////////////////////////////////////////////

  /**
   * Prints a node and its children in XML syntax.
   * 
   * @param node the root of the node tree to print.
   * @param level the indext level for the tree.
   */
  private static void printXML (
    Node node,
    int level
  ) {

    // Print node start
    // ----------------
    String nodeName = node.getNodeName();
    System.out.print (getIndent (level) + "<" + nodeName);

    // Print attributes
    // ----------------
    NamedNodeMap attrMap = node.getAttributes();
    if (attrMap != null) {
      int length = attrMap.getLength();
      for (int i = 0; i < length; i++) {
        Node attr = attrMap.item(i);
        System.out.print (" " + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"");
      } // for
    } // if

    // Print leaf node end
    // -------------------
    Node child = node.getFirstChild();
    if (child == null) System.out.println ("/>");

    // Print children
    // --------------
    else {
      System.out.println (">");
      while (child != null) {
        printXML (child, level+1);
        child = child.getNextSibling();
      } // while
      System.out.println (getIndent (level) + "</" + nodeName + ">");
    } // else
    
  } // printXML

  ////////////////////////////////////////////////////////////

  /**
   * Checks the setup of I/O services.
   *
   * @return an error string if any issues encountered, or null if none.
   */
  public static String checkSetup() {

    String ret = null;

    // Check the HDF library
    // ---------------------
    int[] intArray = new int[3];
    String[] strArray = new String[1];
    try {
      HDFLib.getInstance().Hgetlibversion (intArray, strArray);
      HDF5Lib.getInstance().H5get_libversion (intArray);
    } // try
    catch (Exception e) {
      ret =
        "The HDF library cannot be initialized.  On Windows this may indicate that\n" +
        "the prerequisite Visual C++ redistributable package has not been installed.";
    } // catch

    return (ret);
  
  } // checkSetup

  ////////////////////////////////////////////////////////////

  /**
   * Removes the leading group path from a variable name.
   *
   * @param name the full variable name.
   *
   * @return the modified variable name wothout leading group path.  If no
   * group path is found, the name is returned unmodified.
   * 
   * @since 3.8.0
   */
  public static String stripGroup (
    String name
  ) {
  
    int index = name.lastIndexOf ("/");
    String newName = (index == -1 ? name : name.substring (index+1));
    return (newName);

  } // stripGroup

  ////////////////////////////////////////////////////////////

  private IOServices () { }

  ////////////////////////////////////////////////////////////

} // IOServices class

////////////////////////////////////////////////////////////////////////
