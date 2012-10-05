////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorLookup.java
  PURPOSE: A class to look up colors based on their name.
   AUTHOR: Peter Hollemans
     DATE: 2002/10/11
  CHANGES: 2003/11/15, PFH, added convert method
           2004/05/17, PFH, changed to use XML format
           2004/06/17, PFH, added getInstance(), method
           2005/03/13, PFH, modified convert() to accept RGBA values
           2005/03/26, PFH, modified convert() to accept color:trans values

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import noaa.coastwatch.io.*;

/**
 * The color lookup class translates color names to red, green, blue
 * values based on a color names data file.  The data file has an
 * XML format as follows:
 * <pre>
 *   &lt;?xml version="1.0" encoding="ISO-8859-1"?&gt;
 *   &lt;!DOCTYPE colorlookup SYSTEM "http://coastwatch.noaa.gov/xml/colorlookup.dtd"&gt;
 *   
 *   &lt;colorlookup&gt;
 *     &lt;color name="snow" r="255" g="250" b="250" /&gt;
 *     &lt;color name="ghost white" r="248" g="248" b="255" /&gt;
 *     &lt;color name="GhostWhite" r="248" g="248" b="255" /&gt;
 *     &lt;color name="white smoke" r="245" g="245" b="245" /&gt;
 *     ...
 *   &lt;/colorlookup&gt;
 * </pre> 
 */
public class ColorLookup {

  // Constants
  // ---------

  /** The default color names file. */
  private static final String DEFAULT_FILE = "rgb.xml";

  /** The color lookup DTD URL. */
  private static final String DTD_URL = 
    "http://coastwatch.noaa.gov/xml/colorlookup.dtd";

  /** The color lookup DTD local resource. */
  private static final String DTD_RESOURCE = "colorlookup.dtd";

  // Variables
  // ---------

  /** The table mapping color names to color objects. */
  private Map colorMap;

  /** A static instance of this class. */
  private static ColorLookup instance;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a static instance of this class using the default
   * constructor. 
   *
   * @return the static instance, or null if one could not be
   * created.
   */
  public static ColorLookup getInstance () {

    if (instance == null) {
      try { instance = new ColorLookup(); }
      catch (IOException e) { }
    } // if
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new color lookup based on a predefined color names
   * file.
   *
   * @throws IOException if an error occurred reading the predefined
   * color names file.  Under normal circimstances, this should never
   * happen.
   */
  public ColorLookup () throws IOException {

    InputStream stream = getClass().getResourceAsStream (DEFAULT_FILE);
    if (stream == null)
      throw new IOException ("Cannot find resource '" + DEFAULT_FILE + "'");
    readLookup (stream);

  } // ColorLookup

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new color lookup from the specified file.
   *
   * @param file the file to read.
   *
   * @throws FileNotFoundException if the file is not valid.
   * @throws IOException if the file had input format errors.
   */
  public ColorLookup (
    File file
  ) throws FileNotFoundException, IOException {

    this (new FileInputStream (file));

  } // ColorLookup constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new color lookup from an input stream.
   *
   * @param stream the input stream to read palette data from.
   *
   * @throws IOException if the file had input format errors.
   */
  public ColorLookup (
    InputStream stream
  ) throws FileNotFoundException, IOException {

    readLookup (stream);

  } // ColorLookup constructor

  ////////////////////////////////////////////////////////////

  /** Reads a new lookup from the input stream. */
  private void readLookup (
    InputStream stream
  ) throws IOException {

    try {

      // Create parser
      // -------------
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating (true);
      SAXParser parser = factory.newSAXParser();
      ColorLookupHandler handler = new ColorLookupHandler();

      // Parse file
      // ----------
      parser.parse (stream, handler);

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

  } // readLookup

  ////////////////////////////////////////////////////////////

  /** Handles color lookup parsing events. */
  private class ColorLookupHandler 
    extends DefaultHandler {

    ////////////////////////////////////////////////////////

    public InputSource resolveEntity (
      String publicId, 
      String systemId
    ) {

      if (systemId.equals (DTD_URL)) {
         InputStream stream = getClass().getResourceAsStream (DTD_RESOURCE);
         return (new InputSource (stream));
      }  // if

      return (null);

    } // resolveEntity

    ////////////////////////////////////////////////////////

    public void error (
      SAXParseException e
    ) throws SAXException { 

      fatalError (e);

    } // error

    ////////////////////////////////////////////////////////

    public void fatalError (
      SAXParseException e
    ) throws SAXException {
 
      throw (new SAXException ("Line " + e.getLineNumber() + ": " + 
        e.getMessage())); 

    } // fatalError

    ////////////////////////////////////////////////////////

    public void warning (
      SAXParseException e
    ) throws SAXException { 

      fatalError (e);

    } // warning

    ////////////////////////////////////////////////////////

    public void startElement (
        String uri,
        String localName,
        String qName,
        Attributes attributes
    ) throws SAXException {

      // Start lookup
      // ------------
      if (qName.equals ("colorlookup")) {
        colorMap = new HashMap();
      } // if

      // Add color to map
      // ----------------
      else if (qName.equals ("color")) {
        String name = (String) attributes.getValue ("name");
        int red = Integer.parseInt (attributes.getValue ("r"));
        int green = Integer.parseInt (attributes.getValue ("g"));
        int blue = Integer.parseInt (attributes.getValue ("b"));
        colorMap.put (name, new Color (red, green, blue));
      } // else if

    } // startElement

    ////////////////////////////////////////////////////////

  } // ColorLookupHandler

  ////////////////////////////////////////////////////////////

  /**
   * Gets the color associated with a name.
   * 
   * @param name the color name.
   *
   * @return a color, or null if no color was found.
   */
  public Color getColor (
    String name
  ) {

    return ((Color) colorMap.get (name));

  } // getColor

  ////////////////////////////////////////////////////////////

  /**
   * Converts a color name or hexadecimal value to an object.
   * 
   * @param name the color name or hexadecimal value with "0x"
   * prepended.  If a color name is used, the name may be extended
   * with an optional colon ":" and transparency value in percent.  A
   * transparency of 0 is completely opaque and 100 is completely
   * transparent.
   *
   * @return the converted color with possible alpha component.
   *
   * @throws RuntimeException if the value cannot be converted.
   */
  public Color convert (
    String name
  ) {

    // Check for transparency value
    // ----------------------------
    int trans = 0;
    if (name.indexOf (":") != -1) {
      String[] nameArray = name.split (":");
      try { 
        trans = Integer.parseInt (nameArray[1]); 
        if (trans < 0) trans = 0;
        else if (trans > 100) trans = 100;
        name = nameArray[0];
      } // try
      catch (NumberFormatException e) { }
    } // if
    
    // Lookup color
    // ------------
    Color color = getColor (name);
    if (color != null && trans != 0) {
      int alpha = (int) Math.round (((100-trans)/100.0)*255);
      color = new Color (color.getRed(), color.getGreen(), color.getBlue(),
        alpha);
    } // if

    // Get color from hex value
    // ------------------------
    if (color == null) {
      try { 
        int colorValue = (int) Long.decode(name).longValue();
        boolean hasAlpha = ((0xff000000 & colorValue) != 0);
        color = new Color (colorValue, hasAlpha);
      } // try
      catch (NumberFormatException e) { }
    } // if

    // Report unrecognized color
    // -------------------------
    if (color == null) 
      throw new RuntimeException ("Cannot convert color '" + name + "'");

    return (color);

  } // convert

  ////////////////////////////////////////////////////////////

} // ColorLookup class

////////////////////////////////////////////////////////////////////////
