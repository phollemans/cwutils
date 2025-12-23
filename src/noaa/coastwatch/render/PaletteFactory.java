////////////////////////////////////////////////////////////////////////
/*

     File: PaletteFactory.java
   Author: Peter Hollemans
     Date: 2004/05/18

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.jar.JarFile;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import noaa.coastwatch.render.Palette;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The <code>PaletteFactory</code> class handles written tables of
 * predefined index color models.  Each model has a number of color entries
 * and is associated with a descriptive name.  The number of entries can vary
 * from 1 up to 65536, although in practice the palette will be remapped to
 * 256 colors for use with 8-bit images.  The palette file
 * has an XML format, as follows:</p>
 * <pre>
 *   &lt;?xml version="1.0" encoding="ISO-8859-1"?&gt;
 *   &lt;!DOCTYPE palette SYSTEM "http://coastwatch.noaa.gov/xml/palette.dtd"&gt;
 *   
 *   &lt;palette name="BW-Linear" length="256"&gt;
 *     &lt;color r="0" g="0" b="0" /&gt;
 *     &lt;color r="1" g="1" b="1" /&gt;
 *     &lt;color r="2" g="2" b="2" /&gt;
 *     ...
 *   &lt;/palette&gt;
 *</pre>
 *
 * <p>Users may generate files of this form and use them as input to the
 * palette constructor.  Alternately, palettes may be created by
 * specifying the name and index color model data.  A number of predefined
 * palettes are also available using the names returned by the {@link #getPredefined}
 * method.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class PaletteFactory { 

  private static final Logger LOGGER = Logger.getLogger (PaletteFactory.class.getName());

  // Constants

  /** The palettes resource prefix. */
  private static final String PREFIX = "palettes";

  /** The palette DTD URL. */
  private static final String DTD_URL = "http://coastwatch.noaa.gov/xml/palette.dtd";

  /** The palette DTD local resource. */
  private static final String DTD_RESOURCE = "palette.dtd";

  /** The palette file extension. */
  private static final String FILE_EXTENSION = ".xml";

  // Variables
  // ---------

  /** The map of predefined palettes. */
  private static Map<String, Palette> paletteMap;

  /** The list of predefined palette names. */
  private static List<String> predefinedList;

  ////////////////////////////////////////////////////////////

  /** Gets the list of predefined palette names. */
  public static List<String> getPredefined () { 

    return (List.copyOf (predefinedList));

  } // getPredefined

  ////////////////////////////////////////////////////////////

  /** 
   * Adds the specified palette to the list of predefined palettes.
   * 
   * @param palette the palette to add to the list.
   */
  public static void addPredefined (
    Palette palette
  ) { 

    var name = palette.getName();
    if (!predefinedList.contains (name)) {
      predefinedList.add (name);
      Collections.sort (predefinedList);
      paletteMap.put (name, palette);
    } // if
    else {
      LOGGER.fine ("User palette '" + name + "' ignored, already in predefined list");
    } // else

  } // addPredefined

  ////////////////////////////////////////////////////////////

  /** 
   * Adds the palettes in the specified directory to the list of
   * predefined palettes.  Palettes are identified as any file ending
   * in '.xml'.  Thus, palettes should be kept in a directory by
   * themselves to use this method.
   * 
   * @param paletteDir the palette directory to list.
   *
   * @throws IOException if an error occurred reading the directory or
   * parsing palette file contents.
   */
  public static void addPredefined (
    File paletteDir
  ) throws IOException { 

    File[] files = paletteDir.listFiles (new FilenameFilter () {
        public boolean accept (File dir, String name) {
          return (name.endsWith (FILE_EXTENSION));
        } // accept
      });
    for (int i = 0; i < files.length; i++) {
      try {
        Palette palette = create (new FileInputStream (files[i]));
        addPredefined (palette);
      } // try
      catch (IOException e) {
        LOGGER.log (Level.WARNING, "Error parsing palette file " + files[i], e);
      } // catch
    } // for

  } // addPredefined

  ////////////////////////////////////////////////////////////

  /** Initializes the palette hash map and list. */
  static {

    // Find and list the palettes in the jar file.

    var jar = PaletteFactory.class.getProtectionDomain().getCodeSource().getLocation();
    predefinedList = new ArrayList<>();
    try (var jarFile = new JarFile (new File (jar.toURI()))) {
      jarFile.stream()
        .filter (entry -> entry.getName().matches (".*/" + PREFIX + "/.*\\" + FILE_EXTENSION))
        .forEach (entry -> { 
          var path = entry.getName();
          var fileName = path.substring (path.lastIndexOf ("/") + 1);
          var baseName = fileName.replaceFirst ("\\.[^.]+$", "");
          predefinedList.add (baseName);
          LOGGER.fine ("Found palette file " + path);
        });
    } // try
    catch (Exception e) {
      LOGGER.log (Level.FINE, "Error getting jar entries", e);
    } // catch
    Collections.sort (predefinedList);

    paletteMap = new HashMap<>();

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new palette from the specified file.
   *
   * @param file the file to read.
   *
   * @return the new palette.
   *
   * @throws FileNotFoundException if the file is not valid.
   * @throws IOException if the file had input format errors.
   */
  public static Palette create (
    File file
  ) throws FileNotFoundException, IOException {

    return (create (new FileInputStream (file)));

  } // create

  ////////////////////////////////////////////////////////////

  /** Handles palette parsing events. */
  private static class PaletteHandler 
    extends DefaultHandler {

    // Variables
    // ---------

    /** The palette name. */
    private String name;

    /** The palette color model. */
    private IndexColorModel model;

    /** The buffer used to hold red data. */
    private byte[] red;

    /** The buffer used to hold green data. */
    private byte[] green;

    /** The buffer used to hold blue data. */
    private byte[] blue;

    /** The next color index to store. */
    private int index;

    ////////////////////////////////////////////////////////

    /** Gets the palette parsed by this handler. */
    public Palette getPalette () {

      return (new Palette (name, model));

    } // getPalette

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

      // Start palette
      // -------------
      if (qName.equals ("palette")) {
        name = attributes.getValue ("name");
        int length = Integer.parseInt (attributes.getValue ("length"));
        red = new byte[length];
        green = new byte[length];
        blue = new byte[length];
        index = 0;
      } // if

      // Add color to palette
      // --------------------
      else if (qName.equals ("color")) {
        red[index] = (byte) Integer.parseInt (attributes.getValue ("r"));
        green[index] = (byte) Integer.parseInt (attributes.getValue ("g"));
        blue[index] = (byte) Integer.parseInt (attributes.getValue ("b"));
        index++;
      } // else if

    } // startElement

    ////////////////////////////////////////////////////////////

    public void endElement (
        String uri,
        String localName,
        String qName
    ) throws SAXException {

      // Create color model
      // ------------------
      if (qName.equals ("palette")) {
        int bits = (red.length <= 256 ? 8 : 16);
        model = new IndexColorModel (bits, red.length, red, green, blue);
      } // if

    } // endElement

    ////////////////////////////////////////////////////////

  } // PaletteHandler

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new palette from an input stream.
   *
   * @param stream the input stream to read palette data from.
   *
   * @return the new palette.
   *
   * @throws IOException if the file had input format errors.
   */
  public static Palette create (
    InputStream stream
  ) throws IOException {

    try {

      // Create parser
      // -------------
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating (true);
      SAXParser parser = factory.newSAXParser();
      PaletteHandler handler = new PaletteHandler();

      // Parse file
      // ----------
      parser.parse (stream, handler);
      return (handler.getPalette());

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

  } // create

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new palette from a resource name.
   *
   * @param resource the resource name.
   *
   * @return the new palette.
   *
   * @throws RuntimeException if the palette specified by the resource 
   * had input format errors or the resource is invalid.
   * 
   * #since 3.8.1
   */
  private static Palette createFromResource (String resource) {

    Palette palette;

    try { 
      InputStream stream = PaletteFactory.class.getResourceAsStream (resource);
      if (stream == null) throw new IOException ("Resource not found");
      palette = create (stream);
    } // try
    catch (IOException e) {
      String error = "Error creating new palette instance from resource " + resource + ": " + e.getMessage();
      throw new RuntimeException (error);
    } // catch

    return (palette);

  } // createFromResource

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new palette from a predefined palette name.  This
   * method returns the same instance of the predefined palette when
   * called multiple times with the same name.  A hash map is kept of
   * predefined palettes, and reading of palette data only occurs when
   * required.
   *
   * @param name the predefined palette name.
   *
   * @return the new palette.
   *
   * @throws RuntimeException if the predefined palette had input
   * format errors or the predefined palette is invalid.
   */
  public static Palette create (
    String name
  ) {

    var palette = paletteMap.computeIfAbsent (name, key -> createFromResource (PREFIX + "/" + key + FILE_EXTENSION));
    return (palette);

  } // create

  ////////////////////////////////////////////////////////////

} // PaletteFactory class

////////////////////////////////////////////////////////////////////////
